#!/bin/sh
set -eu
BASEDIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
VERSION=3.9.9
MVN_HOME="${BASEDIR}/.mvn/apache-maven-${VERSION}"
if [ ! -x "${MVN_HOME}/bin/mvn" ]; then
  mkdir -p "${BASEDIR}/.mvn"
  ARCHIVE="/tmp/apache-maven-${VERSION}-bin.tar.gz"
  URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${VERSION}/apache-maven-${VERSION}-bin.tar.gz"
  echo "Downloading Maven ${VERSION}..."
  curl -fsSL "${URL}" -o "${ARCHIVE}"
  tar -xzf "${ARCHIVE}" -C "${BASEDIR}/.mvn"
fi
exec "${MVN_HOME}/bin/mvn" "$@"
