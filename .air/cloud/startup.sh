#!/usr/bin/env bash
#
# Air cloud environment startup script for the TeamCity Slack Notifier plugin.
#
# The project is a Gradle/Kotlin TeamCity server plugin: there is no service to run,
# so this script prepares a working build toolchain and primes the Gradle caches, and
# `healthcheck` proves the plugin actually builds.
#
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_DIR"

# `warmup` is the snapshot-baking run (and the env-setup companion); `task` is a real task run.
if [ "${AIR_STARTUP_MODE:-}" = warmup ]; then WARMUP=1; else WARMUP=; fi

# Gradle 8.14.1 + Kotlin 2.0.21 cannot run on the JDK 25 (JBR) shipped in the image,
# and the plugin targets JVM 1.8, so build on Temurin 17.
JDK_DIR_NAME="jdk-17.0.13+11"
JDK_URL="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_linux_hotspot_17.0.13_11.tar.gz"
JDK_SHA256="8682892fc02965930b9022c066fa164dd6f458ef4a5dc262016aa28333b30f49"
JDK_ROOT="$HOME/.local/jdk"
BUILD_JAVA_HOME="$JDK_ROOT/$JDK_DIR_NAME"

# TeamCity API version to build against when the internal Space repository is not
# configured. The repository default (2023.11-SNAPSHOT) needs credentials for
# packages.jetbrains.team: org.jetbrains.teamcity:web-openapi is not published for it
# on any public repository. This version resolves without credentials.
PUBLIC_TEAMCITY_VERSION="2026.3-dsl6"

ENV_DIR="$HOME/.air-env"
ENV_FILE="$ENV_DIR/teamcity-slack-notifier.sh"
ENV_MARKER="# air-env: teamcity-slack-notifier"
BLOCK_MARKER="air-env: teamcity-slack-notifier"

log() { printf '[startup] %s\n' "$*"; }

install_jdk() {
  if [ -x "$BUILD_JAVA_HOME/bin/javac" ]; then
    log "Temurin $JDK_DIR_NAME already present at $BUILD_JAVA_HOME"
    return 0
  fi

  log "Downloading Temurin $JDK_DIR_NAME ..."
  mkdir -p "$JDK_ROOT"
  local archive="/tmp/temurin-jdk17.tar.gz"
  curl -fsSL -o "$archive" "$JDK_URL"

  log "Verifying archive checksum ..."
  echo "$JDK_SHA256  $archive" | sha256sum -c -

  log "Unpacking JDK into $JDK_ROOT ..."
  tar -xzf "$archive" -C "$JDK_ROOT"
  rm -f "$archive"

  if [ ! -x "$BUILD_JAVA_HOME/bin/javac" ]; then
    log "ERROR: expected JDK at $BUILD_JAVA_HOME after unpacking; got: $(ls "$JDK_ROOT")"
    return 1
  fi
  log "JDK installed: $("$BUILD_JAVA_HOME/bin/java" -version 2>&1 | head -1)"
}

# All egress goes through an HTTP proxy and direct DNS does not resolve. curl picks the
# proxy up from the environment, but the JVM does not, so Gradle needs it as system
# properties - otherwise even the Gradle distribution download fails with UnknownHostException.
PROXY_HOST=""
PROXY_PORT=""
NON_PROXY_HOSTS="localhost|127.0.0.1|::1"

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

# JVM proxy flags for the Gradle launcher (the wrapper download happens before any
# gradle.properties is read). The nonProxyHosts value is quoted because gradlew
# `eval`s GRADLE_OPTS and '|' would otherwise be a pipe.
proxy_gradle_opts() {
  [ -n "$PROXY_HOST" ] || return 0
  printf -- '-Dhttp.proxyHost=%s -Dhttp.proxyPort=%s -Dhttps.proxyHost=%s -Dhttps.proxyPort=%s "-Dhttp.nonProxyHosts=%s" "-Dhttps.nonProxyHosts=%s"' \
    "$PROXY_HOST" "$PROXY_PORT" "$PROXY_HOST" "$PROXY_PORT" "$NON_PROXY_HOSTS" "$NON_PROXY_HOSTS"
}

# Environment exported here dies with this script, so persist it in a file that the
# login and interactive shells of the agent/user source.
write_env_file() {
  mkdir -p "$ENV_DIR"
  cat > "$ENV_FILE" <<EOF
$ENV_MARKER
# Build the plugin with Temurin 17 (the image default JBR 25 is too new for Gradle 8.14.1).
if [ -d "$BUILD_JAVA_HOME" ]; then
  export JAVA_HOME="$BUILD_JAVA_HOME"
  case ":\$PATH:" in
    *":$BUILD_JAVA_HOME/bin:"*) ;;
    *) export PATH="$BUILD_JAVA_HOME/bin:\$PATH" ;;
  esac
fi

# Without credentials for packages.jetbrains.team the repository's default TeamCity
# version cannot be resolved, so build against the publicly published API instead.
# Fill the spacePackagesToken secret (or set teamcityVersion yourself) to override this.
if [ -z "\${teamcityVersion:-}" ] && [ -z "\${spacePackagesToken:-}" ] \\
   && [ -z "\${spacePackagesUsername:-}" ] && [ -z "\${spacePackagesPassword:-}" ]; then
  export teamcityVersion="$PUBLIC_TEAMCITY_VERSION"
fi

# The JVM ignores the HTTP(S)_PROXY variables, so hand Gradle the proxy explicitly.
# Resolved at shell start, because the proxy address is assigned per environment boot.
__air_proxy="\${HTTPS_PROXY:-\${https_proxy:-\${HTTP_PROXY:-\${http_proxy:-}}}}"
if [ -n "\$__air_proxy" ]; then
  case "\${GRADLE_OPTS:-}" in
    *proxyHost*) ;;
    *)
      __air_hostport="\${__air_proxy#*://}"
      __air_hostport="\${__air_hostport%%/*}"
      __air_host="\${__air_hostport%%:*}"
      if [ "\$__air_hostport" = "\$__air_host" ]; then __air_port=80; else __air_port="\${__air_hostport##*:}"; fi
      export GRADLE_OPTS="\${GRADLE_OPTS:-} -Dhttp.proxyHost=\$__air_host -Dhttp.proxyPort=\$__air_port -Dhttps.proxyHost=\$__air_host -Dhttps.proxyPort=\$__air_port \"-Dhttp.nonProxyHosts=$NON_PROXY_HOSTS\" \"-Dhttps.nonProxyHosts=$NON_PROXY_HOSTS\""
      unset __air_hostport __air_host __air_port
      ;;
  esac
fi
unset __air_proxy
EOF

  # A login shell reads only the first profile file that exists.
  local profile=""
  for candidate in "$HOME/.bash_profile" "$HOME/.bash_login" "$HOME/.profile"; do
    if [ -f "$candidate" ]; then profile="$candidate"; break; fi
  done
  if [ -z "$profile" ]; then
    profile="$HOME/.profile"
    touch "$profile"
  fi

  local source_line=". \"$ENV_FILE\"  $ENV_MARKER"
  for rc in "$profile" "$HOME/.bashrc"; do
    [ -f "$rc" ] || touch "$rc"
    if ! grep -qF "$ENV_MARKER" "$rc"; then
      printf '\n%s\n' "$source_line" >> "$rc"
      log "Hooked build environment into $rc"
    fi
  done

  # shellcheck source=/dev/null
  . "$ENV_FILE"
  log "JAVA_HOME=$JAVA_HOME, teamcityVersion=${teamcityVersion:-<repository default>}"
}

# Make every Gradle invocation (CLI, IDE, tooling API) use the JDK we installed and the
# egress proxy, whatever JAVA_HOME or GRADLE_OPTS the caller happens to have. Rewritten on
# every boot because the proxy address is assigned per environment.
configure_gradle() {
  mkdir -p "$HOME/.gradle"
  local props="$HOME/.gradle/gradle.properties"
  [ -f "$props" ] || touch "$props"

  # Drop the block written by a previous run, then append the current one.
  sed -i "/^# >>> $BLOCK_MARKER >>>$/,/^# <<< $BLOCK_MARKER <<<$/d" "$props"
  {
    echo "# >>> $BLOCK_MARKER >>>"
    echo "org.gradle.java.home=$BUILD_JAVA_HOME"
    if [ -n "$PROXY_HOST" ]; then
      echo "systemProp.http.proxyHost=$PROXY_HOST"
      echo "systemProp.http.proxyPort=$PROXY_PORT"
      echo "systemProp.http.nonProxyHosts=$NON_PROXY_HOSTS"
      echo "systemProp.https.proxyHost=$PROXY_HOST"
      echo "systemProp.https.proxyPort=$PROXY_PORT"
      echo "systemProp.https.nonProxyHosts=$NON_PROXY_HOSTS"
    fi
    echo "# <<< $BLOCK_MARKER <<<"
  } >> "$props"

  # The Gradle wrapper downloads the distribution before reading gradle.properties.
  local opts
  opts="$(proxy_gradle_opts)"
  if [ -n "$opts" ]; then
    case "${GRADLE_OPTS:-}" in
      *proxyHost*) ;;
      *) export GRADLE_OPTS="${GRADLE_OPTS:-} $opts" ;;
    esac
  fi

  chmod +x ./gradlew || true
  log "Gradle configured to run on $BUILD_JAVA_HOME"
}

# Downloads the Gradle distribution, the Gradle plugins and all compile dependencies,
# and compiles the plugin. All of it lands in the snapshot, so real tasks start warm.
prime_build() {
  log "Priming Gradle caches and building the plugin (this is the slow, cached part) ..."
  if ./gradlew build --console=plain --stacktrace; then
    log "Initial build succeeded."
  else
    log "Initial build failed; healthcheck will retry and report."
  fi
}

# The produced plugin archive must carry the plugin descriptor and the server jars.
plugin_archive_is_valid() {
  local zip="$1"
  [ -s "$zip" ] || return 1
  python3 - "$zip" <<'PY'
import sys, zipfile
names = zipfile.ZipFile(sys.argv[1]).namelist()
missing = [n for n in ("teamcity-plugin.xml",) if n not in names]
has_server_jar = any(n.startswith("server/") and n.endswith(".jar") for n in names)
if missing or not has_server_jar:
    print(f"plugin archive incomplete: missing={missing} server_jar={has_server_jar}")
    sys.exit(1)
PY
}

# Asserts the environment can do what a real task needs: build the TeamCity plugin
# end to end and produce the distributable plugin archive.
healthcheck() {
  local plugin_zip="$REPO_DIR/build/distributions/slack.zip"
  local attempt=1

  while :; do
    log "healthcheck attempt #$attempt: java -version"
    if ! "$BUILD_JAVA_HOME/bin/java" -version; then
      log "healthcheck: JDK at $BUILD_JAVA_HOME is not runnable; retrying in 15s"
      sleep 15
      attempt=$((attempt + 1))
      continue
    fi

    log "healthcheck attempt #$attempt: ./gradlew build"
    if ./gradlew build --console=plain; then
      if plugin_archive_is_valid "$plugin_zip"; then
        log "healthcheck OK: $plugin_zip built ($(du -h "$plugin_zip" | cut -f1))"
        return 0
      fi
      log "healthcheck: build succeeded but $plugin_zip is missing or incomplete; retrying in 15s"
    else
      log "healthcheck: 'gradlew build' failed (see output above); retrying in 15s"
    fi

    sleep 15
    attempt=$((attempt + 1))
  done
}

log "Starting environment setup (mode=${AIR_STARTUP_MODE:-task}) in $REPO_DIR"
detect_proxy
install_jdk
write_env_file
configure_gradle

if [ -n "$WARMUP" ]; then
  prime_build
  healthcheck
  log "Warmup finished: the plugin builds and the environment is ready."
else
  log "Task run: toolchain ready, Gradle caches come from the snapshot."
fi
