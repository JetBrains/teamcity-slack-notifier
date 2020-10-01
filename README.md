# TeamCity Slack Notifier

[![official project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) 
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Hits-of-Code](https://hitsofcode.com/github/jetbrains/teamcity-slack-notifier?branch=master)](https://hitsofcode.com/view/github/jetbrains/teamcity-slack-notifier?branch=master)
[![CodeQL](https://github.com/JetBrains/teamcity-slack-notifier/workflows/CodeQL/badge.svg)](https://github.com/JetBrains/teamcity-slack-notifier/actions?query=workflow%3ACodeQL)


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
