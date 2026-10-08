# TeamCity Slack Notifier

[![official project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) 
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Hits-of-Code](https://hitsofcode.com/github/jetbrains/teamcity-slack-notifier?branch=master)](https://hitsofcode.com/view/github/jetbrains/teamcity-slack-notifier?branch=master)


The plugin is bundled in TeamCity and requires no manual installation.

This plugin allows you to configure notifications about various build events and global events to Slack.

## Documentation
See [official documentation](https://www.jetbrains.com/help/teamcity/notifications.html#Slack+Notifier)

## Custom message templates

Besides the `Simple` and `Verbose` formats, a notification rule (build feature or
personal notification settings) can use the `Custom` format, where build
notifications are rendered from templates. A template is Slack mrkdwn text with:

- placeholders in curly braces, replaced with build data (see the table below);
- TeamCity parameter references such as `%build.number%`, `%teamcity.build.branch%`
  or `%env.TARGET%`, resolved against the build;
- any Slack markup, e.g. `*bold*`, `` `code` ``, `:emoji:`, `<!here>`, `<!subteam^S0123|@buildops>`.

A template is used for all build events (started, finished, failed, failed to
start, failing, probably hanging); separate templates can be set for successful
and for failed builds. Investigation and mute notifications keep the simple format.

| Placeholder | Value |
|---|---|
| `{build.emoji}` | emoji for the event, e.g. `:white_check_mark:` or `:x:` |
| `{build.event}` | `started`, `is successful`, `failed`, `failed to start`, `is failing`, `is probably hanging` |
| `{build.link}` | project name and a link to the build, e.g. `My Project / <url\|Build #42>` |
| `{build.url}` | URL of the build results page |
| `{build.number}` | build number |
| `{build.name}` | build configuration name |
| `{build.branch}` | branch name, empty if the build has no branch |
| `{build.status}` | status text, e.g. `Tests failed: 2 (1 new), passed: 50` |
| `{build.triggeredBy}` | who or what triggered the build |
| `{project.name}` | full project name |
| `{changes}` | changes in the build, one per line: committer and commit message (collapsed to one line) |
| `{changes.count}` | number of changes |
| `{changes.link}` | link to the changes tab of the build, empty when there are no changes |
| `{committers}` | comma-separated names of the committers |
| `{committers.mentions}` | `@mentions` of the committers who signed in to Slack in TeamCity, names for the others |
| `{tests.failed}` | failed tests, one per line |
| `{tests.failed.count}` | number of failed tests |
| `{problems}` | build problems other than failed tests, one per line |
| `{problems.count}` | number of build problems |

Example of a template for failed builds:

```
:rotating_light: {build.link} *failed* in branch `{build.branch}`, {committers.mentions} please have a look
{build.status}
*Failed tests:*
{tests.failed}
*Changes:*
{changes}
{changes.link}
```

In the Kotlin DSL the format is `messageFormat = customMessageFormat { template = "..."; failureTemplate = "..." }`.

## How to build
In root directory, run
```shell script
gradle build
```
Plugin zip will be located in `build/distributions` directory.

## End-to-end environment (local TeamCity + real Slack)

`e2e/` contains a scripted environment that runs the plugin built from this
checkout in a local TeamCity (Docker) wired to a real Slack sandbox workspace,
plus scenarios that verify notifications by reading them back from Slack:

```shell script
./e2e/e2e.sh setup                        # once per session, idempotent
./e2e/e2e.sh scenario build-notification  # build -> Slack message -> verified
./e2e/e2e.sh plugin                       # rebuild + reinstall the plugin after a change
```

See [e2e/README.md](e2e/README.md).
