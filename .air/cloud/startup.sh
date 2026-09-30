#!/usr/bin/env bash
#
# Air cloud environment startup script for the TeamCity Slack Notifier plugin.
#
# The project is a Gradle/Kotlin TeamCity server plugin: there is no service to run, so
# this script only prepares a build toolchain - a JDK Gradle can run on, pointed at the
# environment's egress proxy. During warmup it additionally compiles the plugin and runs
# the whole test suite, so the snapshot ships with warm Gradle caches.
#
# It runs on every boot, not just during warmup: the proxy address is assigned per
# environment, so the Gradle configuration it writes has to be refreshed each time.
#
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_DIR"

# Gradle 8.14.1 + Kotlin 2.0.21 cannot run on the JDK 25 (JBR) shipped in the image, and
# the TeamCity test-support artifacts are compiled for Java 21, so build and test on
# Amazon Corretto 21. corretto.aws itself is not reachable through the egress proxy; the
# JetBrains cache redirector mirrors it.
JDK_VERSION="21.0.12.12.1"
JDK_SHA256="8785082c2fb999c024c8821e4a7c5391bda28f1667cceadafd78e2965b7669d2"
JDK_URL="https://cache-redirector.jetbrains.com/corretto.aws/downloads/resources/${JDK_VERSION}/amazon-corretto-${JDK_VERSION}-linux-x64.tar.gz"
JDK_ROOT="$HOME/.local/jdk"
BUILD_JAVA_HOME="$JDK_ROOT/amazon-corretto-${JDK_VERSION}-linux-x64"

# The TeamCity API to build against. build.gradle.kts defaults to 2023.11-SNAPSHOT, which
# no longer has everything the sources use (BuildPromotionEx.anchorBuildPromotion); CI
# passes the real version in as a parameter, so the environment has to do the same. This
# is the newest build published for all of server-api, oauth, web-openapi, internal:server,
# internal:web, internal:integration-test and tests-support.
TEAMCITY_VERSION="2026.2-23"

# Environment exported by this script dies with it, so it is persisted in a file that the
# login and interactive shells of the agent and the user source.
ENV_FILE="$HOME/.air-env/teamcity-slack-notifier.sh"
ENV_MARKER="# air-env: teamcity-slack-notifier"
# Delimits the block this script owns inside ~/.gradle/gradle.properties.
PROPS_MARKER="air-env: teamcity-slack-notifier"

# All egress goes through an HTTP proxy and direct DNS does not resolve. curl picks the
# proxy up from the environment but the JVM does not, so Gradle needs it as system
# properties - otherwise even the wrapper's distribution download fails with
# UnknownHostException.
NON_PROXY_HOSTS="localhost|127.0.0.1|::1"
PROXY_HOST=""
PROXY_PORT=""

log() { printf '[startup] %s\n' "$*"; }

detect_proxy() {
  local proxy="${HTTPS_PROXY:-${https_proxy:-${HTTP_PROXY:-${http_proxy:-}}}}"
  if [ -z "$proxy" ]; then
    log "No HTTP(S) proxy in the environment; Gradle will connect directly."
    return 0
  fi
  local hostport="${proxy#*://}"
  hostport="${hostport%%/*}"
  PROXY_HOST="${hostport%%:*}"
  if [ "$hostport" = "$PROXY_HOST" ]; then PROXY_PORT="80"; else PROXY_PORT="${hostport##*:}"; fi
  log "Egress proxy: $PROXY_HOST:$PROXY_PORT"
}

# JVM flags that route a JVM through the proxy. The nonProxyHosts values are quoted
# because gradlew `eval`s GRADLE_OPTS and '|' would otherwise be read as a pipe.
proxy_jvm_opts() {
  [ -n "$PROXY_HOST" ] || return 0
  printf -- '-Dhttp.proxyHost=%s -Dhttp.proxyPort=%s -Dhttps.proxyHost=%s -Dhttps.proxyPort=%s "-Dhttp.nonProxyHosts=%s" "-Dhttps.nonProxyHosts=%s"' \
    "$PROXY_HOST" "$PROXY_PORT" "$PROXY_HOST" "$PROXY_PORT" "$NON_PROXY_HOSTS" "$NON_PROXY_HOSTS"
}

install_jdk() {
  if [ -x "$BUILD_JAVA_HOME/bin/javac" ]; then
    log "Amazon Corretto $JDK_VERSION already present at $BUILD_JAVA_HOME"
    return 0
  fi

  log "Downloading Amazon Corretto $JDK_VERSION ..."
  local archive="/tmp/amazon-corretto-$JDK_VERSION.tar.gz"
  curl -fsSL -o "$archive" "$JDK_URL"
  echo "$JDK_SHA256  $archive" | sha256sum -c - >/dev/null

  log "Unpacking the JDK into $JDK_ROOT ..."
  mkdir -p "$JDK_ROOT"
  tar -xzf "$archive" -C "$JDK_ROOT"
  rm -f "$archive"

  if [ ! -x "$BUILD_JAVA_HOME/bin/javac" ]; then
    log "ERROR: expected a JDK at $BUILD_JAVA_HOME; $JDK_ROOT holds: $(ls "$JDK_ROOT")"
    return 1
  fi
  log "Installed $("$BUILD_JAVA_HOME/bin/java" -version 2>&1 | grep -m1 Runtime)"
}

write_env_file() {
  mkdir -p "$(dirname "$ENV_FILE")"
  {
    echo "$ENV_MARKER"
    echo "# Rewritten by .air/cloud/startup.sh on every boot; local edits are lost."
    echo "export JAVA_HOME=\"$BUILD_JAVA_HOME\""
    echo "case \":\$PATH:\" in"
    echo "  *\":$BUILD_JAVA_HOME/bin:\"*) ;;"
    echo "  *) export PATH=\"$BUILD_JAVA_HOME/bin:\$PATH\" ;;"
    echo "esac"
    echo
    echo "# build.gradle.kts picks this up from the environment; override it by exporting"
    echo "# teamcityVersion before the shell starts."
    echo "export teamcityVersion=\"\${teamcityVersion:-$TEAMCITY_VERSION}\""

    local opts
    opts="$(proxy_jvm_opts)"
    if [ -n "$opts" ]; then
      echo
      echo "# The Gradle wrapper downloads the distribution before it reads any"
      echo "# gradle.properties, so the launcher needs the proxy on its command line."
      echo "case \"\${GRADLE_OPTS:-}\" in"
      echo "  *proxyHost*) ;;"
      # The quotes inside the value have to survive the assignment that sets it.
      echo "  *) export GRADLE_OPTS=\"\${GRADLE_OPTS:-} ${opts//\"/\\\"}\" ;;"
      echo "esac"
    fi
  } > "$ENV_FILE"

  # A login shell reads only the first profile file that exists.
  local profile="$HOME/.profile"
  for candidate in "$HOME/.bash_profile" "$HOME/.bash_login" "$HOME/.profile"; do
    if [ -f "$candidate" ]; then profile="$candidate"; break; fi
  done

  for rc in "$profile" "$HOME/.bashrc"; do
    [ -f "$rc" ] || touch "$rc"
    if ! grep -qF "$ENV_MARKER" "$rc"; then
      printf '\n. "%s"  %s\n' "$ENV_FILE" "$ENV_MARKER" >> "$rc"
      log "Hooked the build environment into $rc"
    fi
  done

  # shellcheck source=/dev/null
  . "$ENV_FILE"
  log "JAVA_HOME=$JAVA_HOME, teamcityVersion=$teamcityVersion"
}

# Make every Gradle invocation (CLI, IDE, tooling API) use the JDK installed above and the
# egress proxy, whatever JAVA_HOME or GRADLE_OPTS the caller happens to have.
configure_gradle() {
  mkdir -p "$HOME/.gradle"
  local props="$HOME/.gradle/gradle.properties"
  [ -f "$props" ] || touch "$props"

  # Drop the block written by a previous boot, then append the current one.
  sed -i "/^# >>> $PROPS_MARKER >>>$/,/^# <<< $PROPS_MARKER <<<$/d" "$props"
  {
    echo "# >>> $PROPS_MARKER >>>"
    echo "org.gradle.java.home=$BUILD_JAVA_HOME"
    if [ -n "$PROXY_HOST" ]; then
      echo "systemProp.http.proxyHost=$PROXY_HOST"
      echo "systemProp.http.proxyPort=$PROXY_PORT"
      echo "systemProp.http.nonProxyHosts=$NON_PROXY_HOSTS"
      echo "systemProp.https.proxyHost=$PROXY_HOST"
      echo "systemProp.https.proxyPort=$PROXY_PORT"
      echo "systemProp.https.nonProxyHosts=$NON_PROXY_HOSTS"
    fi
    echo "# <<< $PROPS_MARKER <<<"
  } >> "$props"
  log "Gradle configured to run on $BUILD_JAVA_HOME"
}

# Downloads the Gradle distribution, the Gradle plugins and every compile and test
# dependency, compiles the plugin and runs the unit and integration test suites. All of
# that lands in the snapshot, so real task runs start warm - and a green build here proves
# the environment can do everything a task needs.
#
# The TeamCity artifacts come from packages.jetbrains.team and need the spacePackagesToken
# secret; build.gradle.kts reads it straight from the environment.
warm_up() {
  local attempt
  for attempt in 1 2; do
    log "Building the plugin and running all tests (attempt $attempt/2) ..."
    if ./gradlew build --console=plain; then
      log "Warmup finished: the plugin compiles and the full test suite passes."
      return 0
    fi
    log "Attempt $attempt failed."
  done
  log "ERROR: './gradlew build' does not pass in this environment (see the output above)."
  return 1
}

log "Starting environment setup (mode=${AIR_STARTUP_MODE:-task}) in $REPO_DIR"
detect_proxy
install_jdk
write_env_file
configure_gradle

# `warmup` is the snapshot-baking run; every other mode is a task run on top of a snapshot.
if [ "${AIR_STARTUP_MODE:-}" = warmup ]; then
  warm_up
else
  log "Task run: toolchain ready, Gradle caches come from the snapshot."
fi
