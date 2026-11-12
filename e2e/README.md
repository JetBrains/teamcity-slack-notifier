# End-to-end environment: local TeamCity + real Slack

This directory gives an agent (or a human) a reproducible way to run the plugin
built from *this checkout* inside a real TeamCity server and watch it talk to a
real Slack workspace. Typical loop: reproduce a bug end to end, fix it, rebuild
and reinstall only the plugin, show the fix the same way.

No mocks: the server posts to the Slack Developer Sandbox workspace
`teamcity-e2e-tests.enterprise.slack.com`, where a Slack app is installed and
the bot is a member of the test channel. Verification reads the messages back
through the Slack Web API.

## TL;DR

```bash
./e2e/e2e.sh setup                          # once per session (~2 min first time); idempotent
./e2e/e2e.sh scenario build-notification    # run a build, assert the Slack message arrived

# after changing the plugin:
./e2e/e2e.sh plugin                         # gradle build + upload with hot reload (restart if needed)
./e2e/e2e.sh scenario build-notification

./e2e/e2e.sh tc run list                    # any TeamCity CLI command, pre-authenticated
./e2e/e2e.sh slack history 5                # last messages in the test channel
./e2e/e2e.sh logs slack                     # server log lines matching "slack"
./e2e/e2e.sh status
```

`setup` can be re-run at any time: on an already configured server it only
checks each item and fixes what is missing (it also refreshes the Slack
credentials in the connection).

## What `setup` does

1. **Preflight** (`check`): the Air secrets are present, `auth.test` succeeds with
   the bot token, the Slack channel is visible to the bot, Docker Compose works,
   the TeamCity CLI is installed (downloaded from GitHub releases if not).
2. **Server** (`up`): `jetbrains/teamcity-server` + `jetbrains/teamcity-agent`
   via `compose.yaml`. First start is unattended:
   - `config/database.properties` (internal HSQLDB) is seeded into the data
     volume, which makes TeamCity skip the First Start / database wizard;
   - `-Dteamcity.licenseAgreement.accepted=true` accepts the license;
   - `-Dteamcity.superUser.token=<random>` fixes the super-user token;
   - `-Dteamcity.authorization.simpleMode.roleAssignmentRestriction.enabled=false`
     allows granting `SYSTEM_ADMIN` through REST;
   - the egress proxy from `HTTPS_PROXY` is passed as JVM system properties,
     otherwise the plugin cannot reach `slack.com`.
3. **Admin**: with the super-user token (empty login, token as password) REST
   creates `e2e-admin` with the global `SYSTEM_ADMIN` role, then an access token
   is created for it. Everything from here on uses the TeamCity CLI with that token.
4. **Verification of the admin**: not assumed. The script checks the role list,
   lists the server plugins, lists agents, and authorizes the agent. Any failure aborts.
5. **Agent**: waits for `e2e-agent` to register and authorizes it (`teamcity agent authorize`).
6. **Plugin**: compares the version inside `build/distributions/slack.zip` with
   what the server reports (`/app/rest/server/plugins`) and installs it if they
   differ. Install = `teamcity server plugin upload --hot-reload`; the first
   upload replaces the *bundled* Slack Notifier and needs one server restart,
   which the script performs. Later uploads hot-reload in seconds.
7. **Project `SlackE2E`**: a Slack connection (project feature `OAuthProvider`,
   `providerType=slackConnection`, bot token + client id/secret from the
   secrets) and a job `SlackE2E_Notify` with one command-line step and a
   `notifications` build feature using `jbSlackNotifier` (verbose format,
   build started / finished / failed / failed-to-start events) targeting the
   test channel.

The result is reported with the TeamCity UI URL and admin credentials; the UI
is reachable at http://127.0.0.1:8111 from the environment.

## Commands

| Command | Purpose |
|---|---|
| `check` | preflight only |
| `up` | start (or recreate) the containers and wait for REST |
| `setup` | `check` + `up` + everything above, idempotent |
| `plugin [--no-build]` | build with Gradle and (re)install; verifies the loaded version |
| `scenario [name]` | run `scenarios/<name>.sh`; without a name lists scenarios |
| `tc <args>` | run the TeamCity CLI as the e2e admin (`tc api /app/rest/...` for raw REST) |
| `slack history [n]` / `slack post <text>` / `slack channel` | peek at or write to the test channel |
| `logs [-f] [pattern]` | server log; with a pattern, greps `teamcity-server.log` + `teamcity-notifications.log` |
| `logging <preset>` | switch the runtime logging preset, e.g. `debug-notifications` (plugin + notification engine at DEBUG in `teamcity-notifications.log`); `default` restores |
| `health` | TeamCity health items for the e2e job (the plugin reports Slack delivery failures there) |
| `status` | containers, auth, plugin versions, agents |
| `restart` | restart the server container and wait |
| `down` | stop containers, keep data |
| `destroy` | remove containers, volumes and `.state/` |

## Scenario: `build-notification`

`scenarios/build-notification.sh` records the newest message in the channel,
starts `SlackE2E_Notify` with `teamcity run start --watch`, then polls
`conversations.history` until a message authored by our bot contains
`Notify #<build number>` and `is successful`. It prints the message text and
its permalink, or on failure the Slack-related server log lines and the
channel tail. Use it as a template for new scenarios: source `../lib/common.sh`
and you get `tc`, `tc_api`, `slack_api`, `json`, `log/ok/die`.

## Configuration

Environment variables (all optional, defaults in `lib/common.sh`):

| Variable | Default | Meaning |
|---|---|---|
| `E2E_SLACK_CHANNEL` | `teamcity-e2e` | channel name (no `#`) the bot posts to and the scenario reads |
| `TC_VERSION` | `2026.2.1` | TeamCity image tag (keep in line with `teamcityVersion` used for the build) |
| `TC_PORT` | `8111` | host port (bound to 127.0.0.1) |
| `E2E_PROJECT_ID` / `E2E_JOB_ID` | `SlackE2E` / `SlackE2E_Notify` | TeamCity ids (the connection id is assigned by TeamCity and stored in `.state/connection-id`) |
| `E2E_SLACK_TIMEOUT` | `90` | seconds the scenario waits for the Slack message |

Required secrets (Air project variables): `spacePackagesToken` (Gradle),
`SLACK_BOT_TOKEN`, `SLACK_CLIENT_ID`, `SLACK_CLIENT_SECRET`. `SLACK_URL` and
`testSlackPwd` are for logging in to the sandbox in a browser; the scripts do
not need them.

State lives in `e2e/.state/` (git-ignored): super-user token, admin password,
CLI access token, resolved channel id, compose env. Deleting it is safe;
`setup` recreates what it can (the admin password is reset through the super user).

## Debugging tips

- Plugin seems silent, no error anywhere: `./e2e/e2e.sh logging debug-notifications`,
  re-run the build, then `./e2e/e2e.sh logs jbSlackNotifier`. The notification
  engine logs which notificators were selected for each event; the plugin's own
  loggers (`...slackNotifier.*`) go to the same `teamcity-notifications.log`.
  `./e2e/e2e.sh health` shows failures the plugin reported (missing
  connection/token, Slack API errors).
- Notification rule semantics: `firstSuccessAfterFailure` and
  `firstFailureAfterSuccess` are *modifiers* of `buildFinishedSuccess` /
  `buildFinishedFailure` ("only the first ..."). With them set, consecutive
  successful builds produce no message at all. The e2e job deliberately leaves
  them out.

- Slack API errors from the plugin: `./e2e/e2e.sh logs slack` (the plugin logs
  under `jetbrains.buildServer.notification.slackNotifier`).
- Did the server JVM get the proxy? `./e2e/e2e.sh logs proxyHost` shows the JVM
  parameters line from startup.
- Super-user REST for emergencies:
  `curl --noproxy '*' -u ":$(cat e2e/.state/superuser-token)" http://127.0.0.1:8111/app/rest/server`.
- Hot reload failing with "New version wasn't found" means the loaded copy is
  the bundled one; `./e2e/e2e.sh restart` makes the uploaded copy active
  (`plugin` does this automatically).
- A *successful* hot reload is not enough when the change touches the notifier
  itself: the previously registered `SlackNotifier` instance keeps handling
  events (the registry is not cleaned on unload), so messages still come from
  the old code. If the messages do not reflect your change after
  `./e2e/e2e.sh plugin`, run `./e2e/e2e.sh restart`.
- Messages always say "No new changes" because the e2e job has no VCS root.
  To exercise changes/committers, serve a bare repository with `git daemon`
  inside the server container (`git://127.0.0.1:9418/<repo>.git`; local file
  URLs are rejected by TeamCity) and set the job's `checkoutMode` to `MANUAL`,
  because the agent container cannot reach the server's loopback interface.
- Host `curl` must bypass the egress proxy for 127.0.0.1 (`--noproxy '*'`),
  otherwise it answers "Blocked by network policy".
