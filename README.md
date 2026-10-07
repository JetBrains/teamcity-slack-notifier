# TeamCity Slack Notifier

[![official project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) 
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Hits-of-Code](https://hitsofcode.com/github/jetbrains/teamcity-slack-notifier?branch=master)](https://hitsofcode.com/view/github/jetbrains/teamcity-slack-notifier?branch=master)


The plugin is bundled in TeamCity and requires no manual installation.

This plugin allows you to configure notifications about various build events and global events to Slack.

## Documentation
See [official documentation](https://www.jetbrains.com/help/teamcity/notifications.html#Slack+Notifier)

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
