#!/usr/bin/env bash
#
# Scenario: a successful build posts a notification to Slack.
#
# 1. Remember the newest message in the Slack channel.
# 2. Start the e2e job with the TeamCity CLI and wait for it to finish.
# 3. Read the channel back through the Slack API and find the message the
#    plugin posted for exactly this build (bot author + "Job #number" link).
#
# Exit code 0 = notification found; anything else = failure, with the context
# needed to debug (build status, server log lines about Slack, channel tail).
#
# shellcheck source=../lib/common.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib/common.sh"

TIMEOUT="${E2E_SLACK_TIMEOUT:-90}"   # seconds to wait for the Slack message

require_env SLACK_BOT_TOKEN
[ -s "$STATE_DIR/admin-token" ] || die "environment not set up - run ./e2e/e2e.sh setup"
chan=$(cat "$STATE_DIR/slack-channel-id" 2>/dev/null || slack_channel_id "$E2E_SLACK_CHANNEL") || die "Slack channel #$E2E_SLACK_CHANNEL not found"
bot_id=$(slack_api auth.test | json 'assert d.get("ok"), d.get("error"); print(d["bot_id"])')

log "scenario: build-notification (job $E2E_JOB_ID -> #$E2E_SLACK_CHANNEL, bot $bot_id)"
log "plugin on server: $(loaded_plugin_versions | head -1)"

# 1. high-water mark in the channel, so only new messages are considered
since=$(slack_api conversations.history -d "channel=$chan&limit=1" | json 'assert d.get("ok"), d.get("error"); print(d["messages"][0]["ts"] if d["messages"] else "0")')

# 2. run the build
log "starting build of $E2E_JOB_ID and waiting for it to finish"
run=$(tc run start "$E2E_JOB_ID" --watch --timeout 10m --json 2>/dev/null | python3 -c '
import sys, json
# --watch --json prints one JSON document; tolerate extra leading lines
text = sys.stdin.read()
doc = json.loads(text[text.index("{"):])
print(doc["id"], doc.get("number", "?"), doc.get("status", "?"), doc.get("state", "?"), doc.get("webUrl", ""))') || die "could not start/watch the build"
read -r build_id build_number status state web_url <<<"$run"
log "build id=$build_id number=$build_number state=$state status=$status $web_url"
[ "$status" = "SUCCESS" ] || { tc run log "$build_id" --raw --tail 40 2>/dev/null || true; die "build did not succeed (status=$status)"; }

# 3. read the message back from Slack
job_name=$(tc_api "/app/rest/buildTypes/id:$E2E_JOB_ID?fields=name" | json 'print(d["name"])')
needle="$job_name #$build_number"
log "waiting up to ${TIMEOUT}s for a message from bot $bot_id containing '$needle' ..."
waited=0
while :; do
  found=$(slack_api conversations.history -d "channel=$chan&oldest=$since&limit=50" | json '
import sys
assert d.get("ok"), d.get("error")
bot, needle = sys.argv[1], sys.argv[2]
for m in reversed(d["messages"]):
    blob = json.dumps(m)
    if m.get("bot_id") == bot and needle in blob and "is successful" in blob:
        print(m["ts"]); print(m.get("text") or ""); break' "$bot_id" "$needle")
  [ -n "$found" ] && break
  [ "$waited" -lt "$TIMEOUT" ] || {
    warn "no matching message. Last log lines mentioning slack (server + notifications logs):"
    server_sh "grep -h -i slack /opt/teamcity/logs/teamcity-server.log /opt/teamcity/logs/teamcity-notifications.log | sort | tail -n 20" >&2 || true
    warn "health items:"; "$E2E_DIR/e2e.sh" health >&2 || true
    warn "channel tail:"; "$E2E_DIR/e2e.sh" slack history 5 >&2 || true
    die "FAILED: no Slack notification for build #$build_number within ${TIMEOUT}s"
  }
  sleep 3; waited=$((waited + 3))
done
ts=$(echo "$found" | head -1)
text=$(echo "$found" | tail -n +2)
permalink=$(slack_api chat.getPermalink -d "channel=$chan&message_ts=$ts" | json 'print(d.get("permalink", ""))')
ok "PASSED: Slack message $ts for build #$build_number"
echo "  text:      $text"
echo "  permalink: $permalink"
echo "  build:     $web_url"
