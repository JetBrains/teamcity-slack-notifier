# shellcheck shell=bash
# Shared helpers for the e2e environment. Sourced by e2e/e2e.sh and the scenarios.

set -euo pipefail

E2E_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_DIR="$(cd "$E2E_DIR/.." && pwd)"
STATE_DIR="$E2E_DIR/.state"
COMPOSE_FILE="$E2E_DIR/compose.yaml"
COMPOSE_ENV="$STATE_DIR/compose.env"
PLUGIN_ZIP="$REPO_DIR/build/distributions/slack.zip"

# Everything below can be overridden from the environment.
TC_VERSION="${TC_VERSION:-2026.2.1}"           # jetbrains/teamcity-server|agent image tag
TC_PORT="${TC_PORT:-8111}"                      # host port, bound to 127.0.0.1
TC_URL="http://127.0.0.1:$TC_PORT"
E2E_ADMIN_USER="${E2E_ADMIN_USER:-e2e-admin}"
E2E_AGENT_NAME="${E2E_AGENT_NAME:-e2e-agent}"
E2E_PROJECT_ID="${E2E_PROJECT_ID:-SlackE2E}"
E2E_JOB_ID="${E2E_JOB_ID:-SlackE2E_Notify}"
E2E_SLACK_CHANNEL="${E2E_SLACK_CHANNEL:-teamcity-e2e}"   # channel name, without '#'
TEAMCITY_CLI_VERSION="${TEAMCITY_CLI_VERSION:-1.5.0}"

export PATH="$HOME/.local/bin:$PATH"
export DO_NOT_TRACK=1                           # no CLI analytics
export TC_VERSION TC_PORT TC_AGENT_NAME="$E2E_AGENT_NAME"

# ---------------------------------------------------------------- logging ---
log()  { printf '\033[1;34m[e2e]\033[0m %s\n' "$*" >&2; }
ok()   { printf '\033[1;32m[e2e] ✓\033[0m %s\n' "$*" >&2; }
warn() { printf '\033[1;33m[e2e] !\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[e2e] ✗\033[0m %s\n' "$*" >&2; exit 1; }

require_env() {
  local missing=() v
  for v in "$@"; do [ -n "${!v:-}" ] || missing+=("$v"); done
  [ ${#missing[@]} -eq 0 ] || die "missing environment variable(s): ${missing[*]} (Air project secrets)"
}

# Run python3 with a JSON document on stdin; `d` is the parsed document and
# extra arguments are available as sys.argv[1..].
# Usage: ... | json 'print(d["count"])'
json() { python3 -c "import sys, json; d = json.load(sys.stdin); $1" "${@:2}"; }

# ----------------------------------------------------------------- docker ---
compose() {
  [ -f "$COMPOSE_ENV" ] || die "$COMPOSE_ENV is missing - run '$E2E_DIR/e2e.sh up' first"
  docker compose --env-file "$COMPOSE_ENV" -f "$COMPOSE_FILE" "$@"
}

server_container_running() {
  [ "$(docker inspect -f '{{.State.Running}}' tc-slack-e2e-server 2>/dev/null)" = "true" ]
}

# Shell inside the server container (as tcuser).
server_sh() { compose exec -T server sh -c "$1"; }

# Derive JVM proxy flags for the server from the host's proxy environment.
# The value has to carry its own quotes: Tomcat's catalina.sh `eval`s the
# options, so a bare '|' in nonProxyHosts would be read as a shell pipe.
proxy_jvm_opts() {
  local proxy="${HTTPS_PROXY:-${https_proxy:-${HTTP_PROXY:-${http_proxy:-}}}}"
  [ -n "$proxy" ] || return 0
  local hostport="${proxy#*://}"; hostport="${hostport%%/*}"
  local host="${hostport%%:*}" port="${hostport##*:}"
  [ "$host" != "$port" ] || port=80
  printf -- '-Dhttp.proxyHost=%s -Dhttp.proxyPort=%s -Dhttps.proxyHost=%s -Dhttps.proxyPort=%s "-Dhttp.nonProxyHosts=localhost|127.0.0.1|server|agent"' \
    "$host" "$port" "$host" "$port"
}

# -------------------------------------------------------- TeamCity access ---
superuser_token() { cat "$STATE_DIR/superuser-token"; }
admin_token()     { cat "$STATE_DIR/admin-token"; }

# Raw REST as the super user (empty login, token as password). Only the
# bootstrap uses this: the CLI cannot authenticate that way.
su_rest() {
  curl -sS --noproxy '*' -u ":$(superuser_token)" -H 'Accept: application/json' "$@"
}

# TeamCity CLI as the e2e admin. Everything after the bootstrap goes through
# it; `tc api` covers the REST calls the CLI has no command for.
tc() {
  [ -f "$STATE_DIR/admin-token" ] || die "no admin token yet - run '$E2E_DIR/e2e.sh setup'"
  TEAMCITY_URL="$TC_URL" TEAMCITY_TOKEN="$(admin_token)" \
    teamcity --no-input "$@" 2> >(grep -v -e 'Using insecure HTTP connection' -e 'Consider using HTTPS' >&2 || true)
}

# `tc api` returning only the body; non-2xx makes the CLI exit non-zero.
tc_api() { tc api "$@" --raw; }

# Does a REST resource exist? Swallows the error output of a 404.
tc_exists() { tc api "$1" --raw --silent >/dev/null 2>&1; }

# POST/PUT JSON from a heredoc on stdin.
tc_api_json() { # method path  (body on stdin)
  tc api "$2" -X "$1" -H 'Content-Type: application/json' -H 'Accept: application/json' --input - --raw
}

wait_for_server() { # [timeout-seconds]
  local timeout="${1:-420}" waited=0 code
  log "waiting for TeamCity at $TC_URL (up to ${timeout}s) ..."
  while :; do
    code=$(curl -s --noproxy '*' -o /dev/null -w '%{http_code}' -u ":$(superuser_token)" "$TC_URL/app/rest/server" || true)
    [ "$code" = "200" ] && { ok "TeamCity is up"; return 0; }
    if ! server_container_running; then
      docker logs --tail 30 tc-slack-e2e-server >&2 || true
      die "the server container is not running (see the log above)"
    fi
    [ "$waited" -lt "$timeout" ] || die "TeamCity did not come up within ${timeout}s (last HTTP status: $code)"
    sleep 5; waited=$((waited + 5))
  done
}

# Version string inside a plugin zip (teamcity-plugin.xml).
plugin_zip_version() {
  python3 - "$1" <<'PY'
import re, sys, zipfile
xml = zipfile.ZipFile(sys.argv[1]).read("teamcity-plugin.xml").decode()
print(re.search(r"<version>([^<]*)</version>", xml).group(1))
PY
}

# Versions of the slackNotifier plugin the server knows about: the one it
# loads plus, once a non-bundled copy exists, the shadowed bundled one.
loaded_plugin_versions() {
  tc_api '/app/rest/server/plugins' | json '
for p in d.get("plugin", []):
    if p["name"] == "slackNotifier":
        print(p.get("version", "?"), p.get("loadPath", ""))'
}

# -------------------------------------------------------------- Slack API ---
slack_api() { # method [curl -d args...]
  local method="$1"; shift
  curl -sS -X POST "https://slack.com/api/$method" -H "Authorization: Bearer $SLACK_BOT_TOKEN" "$@"
}

# Resolve a channel name to its ID (public + private channels the app can see).
slack_channel_id() { # name
  local name="${1#\#}" cursor="" id page
  while :; do
    page=$(slack_api conversations.list -d "types=public_channel,private_channel&exclude_archived=true&limit=200&cursor=$cursor")
    read -r id cursor < <(echo "$page" | json '
assert d.get("ok"), d.get("error")
name = sys.argv[1]
match = [c["id"] for c in d["channels"] if c["name"] == name]
print(match[0] if match else "-", d.get("response_metadata", {}).get("next_cursor", "") or "-")' "$name") || die "conversations.list failed"
    [ "$id" = "-" ] || { echo "$id"; return 0; }
    [ "$cursor" != "-" ] || return 1
  done
}

slack_channel_names() {
  slack_api conversations.list -d "types=public_channel,private_channel&exclude_archived=true&limit=200" \
    | json 'print(", ".join("#%s%s" % (c["name"], "" if c.get("is_member") else " (bot not a member)") for c in d.get("channels", [])))'
}
