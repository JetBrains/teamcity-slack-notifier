#!/usr/bin/env bash
#
# e2e/e2e.sh - local TeamCity + real Slack end-to-end environment for the
# Slack Notifier plugin. See e2e/README.md.
#
#   ./e2e/e2e.sh check                  preflight: secrets, Slack, docker, CLI
#   ./e2e/e2e.sh setup                  start + configure everything (idempotent)
#   ./e2e/e2e.sh plugin [--no-build]    rebuild the plugin and (re)install it
#   ./e2e/e2e.sh scenario <name>        run e2e/scenarios/<name>.sh
#   ./e2e/e2e.sh status | logs | logging | health | tc ... | slack ... | restart | down | destroy
#
# shellcheck source=lib/common.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/common.sh"

usage() { sed -n '3,11p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit "${1:-0}"; }

# ================================================================ preflight ==
ensure_cli() {
  if command -v teamcity >/dev/null 2>&1; then return 0; fi
  log "TeamCity CLI not found - installing v$TEAMCITY_CLI_VERSION into ~/.local/bin"
  local arch tmp
  case "$(uname -m)" in x86_64) arch=x86_64 ;; aarch64|arm64) arch=arm64 ;; *) die "unsupported arch $(uname -m)" ;; esac
  tmp=$(mktemp -d)
  local base="https://github.com/JetBrains/teamcity-cli/releases/download/v$TEAMCITY_CLI_VERSION"
  curl -fsSL -o "$tmp/cli.tar.gz" "$base/teamcity_${TEAMCITY_CLI_VERSION}_linux_${arch}.tar.gz"
  curl -fsSL -o "$tmp/checksums.txt" "$base/checksums.txt"
  (cd "$tmp" && grep "linux_${arch}.tar.gz" checksums.txt | sed 's#  .*#  cli.tar.gz#' | sha256sum -c - >/dev/null) || die "CLI checksum mismatch"
  tar -xzf "$tmp/cli.tar.gz" -C "$tmp" teamcity
  mkdir -p "$HOME/.local/bin" && install -m 755 "$tmp/teamcity" "$HOME/.local/bin/teamcity"
  rm -rf "$tmp"
  ok "installed $(teamcity --version)"
}

cmd_check() {
  log "checking Air project secrets"
  require_env spacePackagesToken SLACK_BOT_TOKEN SLACK_CLIENT_ID SLACK_CLIENT_SECRET
  ok "secrets present"

  log "checking Slack bot token (auth.test)"
  local auth; auth=$(slack_api auth.test)
  echo "$auth" | json 'assert d.get("ok"), d.get("error")' || die "auth.test failed: $auth"
  ok "Slack: $(echo "$auth" | json 'print("bot %s in workspace %s (%s)" % (d["user"], d["team"], d["url"]))')"

  log "checking Slack channel #$E2E_SLACK_CHANNEL"
  local chan
  if chan=$(slack_channel_id "$E2E_SLACK_CHANNEL"); then
    ok "channel #$E2E_SLACK_CHANNEL = $chan"
  else
    die "channel #$E2E_SLACK_CHANNEL is not visible to the bot. Channels it can see: $(slack_channel_names). Create/invite, or set E2E_SLACK_CHANNEL."
  fi

  log "checking docker"
  docker compose version >/dev/null || die "docker compose is not available"
  ok "$(docker compose version)"

  ensure_cli
  ok "TeamCity CLI: $(teamcity --version)"
  [ -n "${HTTPS_PROXY:-${https_proxy:-}}" ] && ok "egress proxy: ${HTTPS_PROXY:-$https_proxy} (passed to the server JVM)" || warn "no HTTPS_PROXY in the environment; the server JVM connects directly"
}

# ================================================================= server ===
ensure_state() {
  mkdir -p "$STATE_DIR"
  if [ ! -s "$STATE_DIR/superuser-token" ]; then
    python3 -c 'import secrets; print(secrets.randbelow(10**18) + 10**17)' > "$STATE_DIR/superuser-token"
    chmod 600 "$STATE_DIR/superuser-token"
  fi
  # Regenerated on every run: the proxy address differs per environment boot.
  {
    echo "TC_SUPERUSER_TOKEN=$(superuser_token)"
    echo "TC_PROXY_JVM_OPTS=$(proxy_jvm_opts)"
  } > "$COMPOSE_ENV"
}

# The First Start wizard is skipped when config/database.properties exists;
# seed the internal HSQLDB config into the (empty) data volume as tcuser.
ensure_db_seed() {
  if server_container_running; then return 0; fi
  compose run --rm --no-deps --entrypoint sh server -c '
    f="$TEAMCITY_DATA_PATH/config/database.properties"
    if [ ! -f "$f" ]; then
      mkdir -p "$(dirname "$f")"
      printf "# internal HSQLDB - seeded by e2e.sh for the unattended first start\nconnectionUrl=jdbc:hsqldb:file:\$TEAMCITY_SYSTEM_PATH/buildserver\n" > "$f"
      echo seeded
    fi' 2>/dev/null | grep -q seeded && ok "seeded database.properties (internal DB)" || true
}

cmd_up() {
  ensure_cli
  ensure_state
  ensure_db_seed
  log "starting containers (TeamCity $TC_VERSION)"
  compose up -d --quiet-pull 2>&1 | grep -v -e '^$' >&2 || true
  wait_for_server
}

cmd_restart() {
  log "restarting the TeamCity server"
  compose restart server >/dev/null
  wait_for_server
}

# ================================================================== setup ===
ensure_admin() {
  local pwd_file="$STATE_DIR/admin-password"
  if su_rest -o /dev/null -w '%{http_code}' "$TC_URL/app/rest/users/username:$E2E_ADMIN_USER" | grep -q '^200$'; then
    if [ ! -s "$pwd_file" ]; then
      warn "admin exists but its password is not in $STATE_DIR - resetting it via the super user"
      python3 -c 'import secrets; print(secrets.token_urlsafe(18))' > "$pwd_file"; chmod 600 "$pwd_file"
      su_rest -H 'Content-Type: application/json' -X PUT "$TC_URL/app/rest/users/username:$E2E_ADMIN_USER" \
        -d "{\"password\":\"$(cat "$pwd_file")\"}" -o /dev/null
    fi
    ok "admin user '$E2E_ADMIN_USER' exists"
  else
    log "creating admin user '$E2E_ADMIN_USER' with the super user token"
    python3 -c 'import secrets; print(secrets.token_urlsafe(18))' > "$pwd_file"; chmod 600 "$pwd_file"
    su_rest -H 'Content-Type: application/json' -X POST "$TC_URL/app/rest/users" -o /dev/null -f \
      -d "{\"username\":\"$E2E_ADMIN_USER\",\"name\":\"E2E Admin\",\"password\":\"$(cat "$pwd_file")\",\"roles\":{\"role\":[{\"roleId\":\"SYSTEM_ADMIN\",\"scope\":\"g\"}]}}" \
      || die "could not create the admin user"
    ok "created admin user '$E2E_ADMIN_USER'"
  fi
  # Idempotent: PUT of an existing role assignment is a no-op.
  su_rest -X PUT -o /dev/null -f "$TC_URL/app/rest/users/username:$E2E_ADMIN_USER/roles/SYSTEM_ADMIN/g" \
    || die "could not grant SYSTEM_ADMIN (is teamcity.authorization.simpleMode.roleAssignmentRestriction.enabled=false set?)"
}

ensure_admin_token() {
  if [ -s "$STATE_DIR/admin-token" ] && tc auth status >/dev/null 2>&1; then
    ok "CLI access token is valid"
    return 0
  fi
  log "creating a CLI access token for '$E2E_ADMIN_USER'"
  local resp
  resp=$(curl -sS --noproxy '*' -u "$E2E_ADMIN_USER:$(cat "$STATE_DIR/admin-password")" \
    -H 'Accept: application/json' -H 'Content-Type: application/json' \
    -X POST "$TC_URL/app/rest/users/current/tokens" -d "{\"name\":\"e2e-cli-$(date +%s)\"}")
  echo "$resp" | json 'print(d["value"])' > "$STATE_DIR/admin-token" 2>/dev/null || die "token creation failed: $resp"
  chmod 600 "$STATE_DIR/admin-token"
  tc auth status >/dev/null || die "the new token does not work"
  ok "CLI access token created"
}

# Do not assume the admin is an admin: prove it with the operations we need.
verify_admin() {
  local roles
  roles=$(tc_api '/app/rest/users/current' | json 'print(" ".join("%s@%s" % (r["roleId"], r["scope"]) for r in d["roles"]["role"]))')
  echo "$roles" | grep -qw 'SYSTEM_ADMIN@g' || die "'$E2E_ADMIN_USER' lacks the global SYSTEM_ADMIN role (has: $roles)"
  local plugins
  plugins=$(tc_api '/app/rest/server/plugins' | json 'print(d["count"])') || die "'$E2E_ADMIN_USER' cannot list server plugins"
  [ "${plugins:-0}" -gt 0 ] || die "server plugin list is empty - not an admin?"
  tc agent list --json >/dev/null || die "'$E2E_ADMIN_USER' cannot list agents"
  ok "'$E2E_ADMIN_USER' is a system administrator (SYSTEM_ADMIN@g, lists $plugins plugins, sees agents)"
}

ensure_agent() {
  local waited=0 state
  while :; do
    state=$(tc_api "/app/rest/agents?locator=name:$E2E_AGENT_NAME,authorized:any&fields=agent(id,connected,authorized)" \
      | json 'a = d.get("agent", []); print("%s %s" % (a[0]["authorized"], a[0]["connected"]) if a else "absent")')
    [ "$state" != "absent" ] && break
    [ "$waited" -lt 240 ] || die "agent '$E2E_AGENT_NAME' did not register within 240s - check 'docker logs tc-slack-e2e-agent'"
    [ "$waited" -eq 0 ] && log "waiting for agent '$E2E_AGENT_NAME' to register ..."
    sleep 5; waited=$((waited + 5))
  done
  if [ "${state%% *}" != "True" ]; then
    tc agent authorize "$E2E_AGENT_NAME" >/dev/null || die "could not authorize the agent - the admin lacks permissions?"
    state=$(tc agent list --json=name,authorized | json 'print([a["authorized"] for a in d["agent"] if a["name"] == sys.argv[1]][0])' "$E2E_AGENT_NAME")
    [ "$state" = "True" ] || die "agent authorization did not stick"
    ok "authorized agent '$E2E_AGENT_NAME' (proves the admin can authorize agents)"
  else
    ok "agent '$E2E_AGENT_NAME' is authorized"
  fi
  [ "${state##* }" = "True" ] || warn "agent '$E2E_AGENT_NAME' is authorized but not connected yet"
}

ensure_plugin() {
  [ -f "$PLUGIN_ZIP" ] || cmd_plugin --no-install
  local want have
  want=$(plugin_zip_version "$PLUGIN_ZIP")
  have=$(loaded_plugin_versions)
  if echo "$have" | awk '{print $1}' | grep -qxF "$want"; then
    ok "plugin version $want is installed"
  else
    log "installed slackNotifier version(s): $(echo "$have" | awk '{print $1}' | paste -sd, -) - installing $want"
    install_plugin
  fi
}

# Upload + hot reload; the very first upload replaces the *bundled* copy,
# which only takes effect after a restart.
install_plugin() {
  local want out
  want=$(plugin_zip_version "$PLUGIN_ZIP")
  log "uploading $(basename "$PLUGIN_ZIP") ($want) with --hot-reload"
  if out=$(tc server plugin upload "$PLUGIN_ZIP" --hot-reload --json 2>&1) && echo "$out" | json 'assert d.get("hot_reloaded")' 2>/dev/null; then
    ok "hot-reloaded"
  else
    log "hot reload not possible ($(echo "$out" | json 'print(d.get("error", {}).get("message", "?"))' 2>/dev/null || echo "$out")) - restarting the server"
    cmd_restart
  fi
  local have; have=$(loaded_plugin_versions)
  echo "$have" | awk '{print $1}' | grep -qxF "$want" || die "server does not report plugin version $want after install; it has: $have"
  ok "server runs slackNotifier $want"
}

ensure_slack_channel() {
  local id
  id=$(slack_channel_id "$E2E_SLACK_CHANNEL") || die "Slack channel #$E2E_SLACK_CHANNEL not found. Visible channels: $(slack_channel_names). Set E2E_SLACK_CHANNEL to override."
  echo "$id" > "$STATE_DIR/slack-channel-id"
  ok "Slack channel #$E2E_SLACK_CHANNEL ($id)"
}

ensure_project() {
  if tc_exists "/app/rest/projects/id:$E2E_PROJECT_ID"; then
    ok "project $E2E_PROJECT_ID exists"
  else
    tc project create "Slack E2E" --id "$E2E_PROJECT_ID" >/dev/null
    ok "created project $E2E_PROJECT_ID"
  fi
}

# Slack connection = project feature of type OAuthProvider / providerType=slackConnection
# (see src/kotlin-dsl/SlackConnection.xml). The CLI only creates GitHub/Docker
# connections, so this goes through `tc api`. TeamCity assigns the feature id
# itself (a requested id is ignored), so the connection is found by its
# properties and the id is recorded in .state for the job. Secrets are
# re-applied on every run.
CONNECTION_NAME="Slack sandbox (e2e)"
find_connection_ids() {
  tc_api "/app/rest/projects/id:$E2E_PROJECT_ID/projectFeatures?locator=type:OAuthProvider" | json '
import sys
for f in d.get("projectFeature", []):
    props = {p["name"]: p.get("value") for p in f["properties"]["property"]}
    if props.get("providerType") == "slackConnection" and props.get("displayName") == sys.argv[1]:
        print(f["id"])' "$CONNECTION_NAME"
}

ensure_connection() {
  local body ids id extra
  body=$(cat <<JSON
{"type":"OAuthProvider","properties":{"property":[
  {"name":"providerType","value":"slackConnection"},
  {"name":"displayName","value":"$CONNECTION_NAME"},
  {"name":"clientId","value":"$SLACK_CLIENT_ID"},
  {"name":"secure:clientSecret","value":"$SLACK_CLIENT_SECRET"},
  {"name":"secure:token","value":"$SLACK_BOT_TOKEN"}]}}
JSON
)
  ids=$(find_connection_ids)
  id=$(echo "$ids" | head -1)
  if [ -n "$id" ]; then
    for extra in $(echo "$ids" | tail -n +2); do
      tc_api "/app/rest/projects/id:$E2E_PROJECT_ID/projectFeatures/id:$extra" -X DELETE >/dev/null && warn "removed duplicate connection $extra"
    done
    echo "$body" | tc_api_json PUT "/app/rest/projects/id:$E2E_PROJECT_ID/projectFeatures/id:$id" >/dev/null || die "could not update the Slack connection"
    ok "Slack connection $id (updated)"
  else
    id=$(echo "$body" | tc_api_json POST "/app/rest/projects/id:$E2E_PROJECT_ID/projectFeatures" | json 'print(d["id"])') || die "could not create the Slack connection"
    ok "Slack connection $id (created)"
  fi
  echo "$id" > "$STATE_DIR/connection-id"
}

ensure_job() {
  if tc_exists "/app/rest/buildTypes/id:$E2E_JOB_ID"; then
    ok "job $E2E_JOB_ID exists"
  else
    tc job create "Notify" --project "$E2E_PROJECT_ID" --id "$E2E_JOB_ID" >/dev/null
    ok "created job $E2E_JOB_ID"
  fi
  if ! tc job step list "$E2E_JOB_ID" --json | json 'assert any(s.get("name") == "hello" for s in d.get("step", []))' 2>/dev/null; then
    tc job step add "$E2E_JOB_ID" --type simpleRunner --name hello \
      --param use.custom.script=true --param 'script.content=echo "hello from the slack e2e build %build.number%"' >/dev/null
    ok "added build step"
  fi
  # Notifications build feature with the Slack notifier (see src/kotlin-dsl/SlackBuildFeatureAddon.xml).
  # Event names are NotificationRulesConstants (same as the integration tests use).
  # Do NOT add firstSuccessAfterFailure/firstFailureAfterSuccess here: TeamCity
  # treats them as "only the first ..." modifiers, which silently suppresses
  # the notification for every consecutive successful build.
  local body fid path connection
  connection=$(cat "$STATE_DIR/connection-id")
  body=$(cat <<JSON
{"type":"notifications","properties":{"property":[
  {"name":"notifier","value":"jbSlackNotifier"},
  {"name":"plugin:notificator:jbSlackNotifier:connection","value":"$connection"},
  {"name":"plugin:notificator:jbSlackNotifier:channel","value":"#$E2E_SLACK_CHANNEL"},
  {"name":"plugin:notificator:jbSlackNotifier:messageFormat","value":"verbose"},
  {"name":"plugin:notificator:jbSlackNotifier:addBuildStatus","value":"true"},
  {"name":"plugin:notificator:jbSlackNotifier:addBranch","value":"true"},
  {"name":"plugin:notificator:jbSlackNotifier:addChanges","value":"true"},
  {"name":"plugin:notificator:jbSlackNotifier:maximumNumberOfChanges","value":"10"},
  {"name":"buildStarted","value":"true"},
  {"name":"buildFinishedSuccess","value":"true"},
  {"name":"buildFinishedFailure","value":"true"},
  {"name":"buildFailedToStart","value":"true"}]}}
JSON
)
  path="/app/rest/buildTypes/id:$E2E_JOB_ID/features"
  fid=$(tc_api "$path" | json 'f = [f["id"] for f in d.get("feature", []) if f["type"] == "notifications"]; print(f[0] if f else "")')
  if [ -n "$fid" ]; then
    echo "$body" | tc_api_json PUT "$path/$fid" >/dev/null || die "could not update the notifications feature"
    ok "Slack notifications feature $fid (updated, connection $connection)"
  else
    fid=$(echo "$body" | tc_api_json POST "$path" | json 'print(d["id"])') || die "could not add the notifications feature"
    ok "Slack notifications feature $fid (created, connection $connection)"
  fi
}

cmd_setup() {
  cmd_check
  cmd_up
  ensure_admin
  ensure_admin_token
  verify_admin
  ensure_agent
  ensure_plugin
  ensure_slack_channel
  ensure_project
  ensure_connection
  ensure_job
  ok "setup complete - TeamCity UI: $TC_URL (login $E2E_ADMIN_USER / $(cat "$STATE_DIR/admin-password"))"
}

# ================================================================= plugin ===
cmd_plugin() {
  local build=1 install=1 a
  for a in "$@"; do case "$a" in --no-build) build=0 ;; --no-install) install=0 ;; *) die "unknown option $a" ;; esac; done
  if [ "$build" = 1 ]; then
    log "building the plugin (./gradlew serverPlugin)"
    (cd "$REPO_DIR" && ./gradlew serverPlugin --console=plain -q) || die "gradle build failed"
    ok "built $PLUGIN_ZIP ($(plugin_zip_version "$PLUGIN_ZIP"))"
  fi
  [ "$install" = 1 ] && install_plugin || true
}

# ================================================================= misc ====
cmd_status() {
  compose ps 2>/dev/null || true
  echo
  if [ -s "$STATE_DIR/admin-token" ]; then
    tc auth status || true
    echo "plugin versions on the server:"; loaded_plugin_versions | sed 's/^/  /'
    echo "built plugin: $([ -f "$PLUGIN_ZIP" ] && plugin_zip_version "$PLUGIN_ZIP" || echo '(not built)')"
    echo "agents:"; tc agent list --plain 2>/dev/null | sed 's/^/  /'
    echo "slack channel: #$E2E_SLACK_CHANNEL $(cat "$STATE_DIR/slack-channel-id" 2>/dev/null)"
    echo "UI: $TC_URL  login: $E2E_ADMIN_USER / $(cat "$STATE_DIR/admin-password" 2>/dev/null)"
  else
    echo "not set up yet - run: $0 setup"
  fi
}

cmd_logs() { # [-f] [grep-pattern]
  local follow="" pattern="" a
  for a in "$@"; do case "$a" in -f|--follow) follow=-f ;; *) pattern="$a" ;; esac; done
  if [ -n "$pattern" ]; then
    server_sh "grep -h -i -- '$pattern' /opt/teamcity/logs/teamcity-server.log /opt/teamcity/logs/teamcity-notifications.log | sort | tail -n 200"
  else
    compose exec server tail -n 200 $follow /opt/teamcity/logs/teamcity-server.log
  fi
}

cmd_slack() { # history [n] | post <text> | channel
  local sub="${1:-history}"; shift || true
  local chan; chan=$(cat "$STATE_DIR/slack-channel-id" 2>/dev/null || slack_channel_id "$E2E_SLACK_CHANNEL")
  case "$sub" in
    channel) echo "#$E2E_SLACK_CHANNEL $chan" ;;
    history)
      slack_api conversations.history -d "channel=$chan&limit=${1:-10}" | json '
import datetime
assert d.get("ok"), d.get("error")
for m in reversed(d["messages"]):
    ts = datetime.datetime.fromtimestamp(float(m["ts"])).strftime("%H:%M:%S")
    print("%s [%s] %s" % (ts, m.get("bot_id") or m.get("user"), (m.get("text") or "").replace("\n", " | ")))' ;;
    post)
      slack_api chat.postMessage -d "channel=$chan" --data-urlencode "text=$*" | json 'assert d.get("ok"), d.get("error"); print("posted", d["ts"])' ;;
    *) die "usage: $0 slack history [n] | post <text> | channel" ;;
  esac
}

# Switch the server's runtime logging preset (Administration | Diagnostics).
# `debug-notifications` makes TeamCity and the plugin log at DEBUG into
# teamcity-notifications.log; `default` restores the normal configuration.
cmd_logging() { # <preset>|default|list
  local preset="${1:-}" csrf
  [ -n "$preset" ] || { echo "usage: $0 logging <debug-notifications|debug-general|debug-all|...|default>"; exit 1; }
  [ "$preset" = default ] && preset='<Default>'
  csrf=$(curl -sS --noproxy '*' -H "Authorization: Bearer $(admin_token)" "$TC_URL/authenticationTest.html?csrf")
  curl -sS --noproxy '*' -f -H "Authorization: Bearer $(admin_token)" -H "X-TC-CSRF-Token: $csrf" \
    -X POST "$TC_URL/admin/diagnostic.html" --data-urlencode "actionName=loadPreset" --data-urlencode "loggingPreset=$preset" \
    | grep -q '<errors */>' || die "could not switch the logging preset"
  ok "logging preset: $preset ($(server_sh 'grep -h "Switched to logging preset" /opt/teamcity/logs/teamcity-server.log | tail -1 | cut -c2-20'))"
}

# Server health items for the e2e job (the plugin reports delivery failures there).
cmd_health() {
  tc_api "/app/rest/health?locator=buildType:(id:$E2E_JOB_ID),count:100&fields=healthItem(identity,severity,healthCategory(id,name))" \
    | json 'items = d.get("healthItem", []); print("no health items for the job") if not items else [print(i["severity"], i["healthCategory"]["id"], "-", i["healthCategory"]["name"]) for i in items]'
}

cmd_scenario() {
  local name="${1:-}"; [ -n "$name" ] || { ls "$E2E_DIR/scenarios" | sed 's/\.sh$//'; exit 0; }
  shift
  local script="$E2E_DIR/scenarios/$name.sh"
  [ -f "$script" ] || die "no such scenario: $name (available: $(ls "$E2E_DIR/scenarios" | sed 's/\.sh$//' | paste -sd' ' -))"
  exec bash "$script" "$@"
}

cmd_down() { compose down; ok "containers stopped (data kept in docker volumes; 'destroy' removes everything)"; }
cmd_destroy() { [ -f "$COMPOSE_ENV" ] && compose down -v --remove-orphans || true; rm -rf "$STATE_DIR"; ok "removed containers, volumes and $STATE_DIR"; }

main() {
  local cmd="${1:-}"; shift || true
  case "$cmd" in
    check) cmd_check ;;
    up) cmd_up ;;
    setup) cmd_setup ;;
    plugin) cmd_plugin "$@" ;;
    restart) cmd_restart ;;
    status) cmd_status ;;
    logs) cmd_logs "$@" ;;
    logging) cmd_logging "$@" ;;
    health) cmd_health ;;
    tc) tc "$@" ;;
    slack) cmd_slack "$@" ;;
    scenario) cmd_scenario "$@" ;;
    down) cmd_down ;;
    destroy) cmd_destroy ;;
    -h|--help|help|"") usage 0 ;;
    *) die "unknown command '$cmd' (try: $0 help)" ;;
  esac
}
main "$@"
