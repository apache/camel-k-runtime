#!/bin/bash

set -e

SEMVER="^([[:digit:]]+)\.([[:digit:]]+)\.([[:digit:]]+)(-RC[[:digit:]]+|-SNAPSHOT)$"
DRY_RUN="false"
SKIP_VERSION_CHECK="false"

display_usage() {

cat <<EOF
Synch with the latest released version of Camel Quarkus.

Usage: ./script/sync_cq.sh

--dry-run                 Show the changes without applying them to the project
--help                    This help message
--skip-version-check      Use this parameter when starting a new minor or major upgrade from a local machine.

Example: ./script/sync_cq.sh --skip-version-check --dry-run
EOF

}

main() {
  parse_args $@

  VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
  if ! [[ $VERSION =~ $SEMVER ]]; then
    echo "❗ Version must match major.minor.patch[-RC<X>|[-SNAPSHOT] semantic version: $VERSION"
    exit 1
  fi
  VERSION_FULL="${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.${BASH_REMATCH[3]}"
  VERSION_MM="${BASH_REMATCH[1]}.${BASH_REMATCH[2]}"
  pushd /tmp
  rm -rf camel-quarkus
  git clone https://github.com/apache/camel-quarkus.git
  pushd camel-quarkus
  CQ_VERSION=$(git tag | grep $VERSION_MM | sort | tail -n 1)
  if [ "$SKIP_VERSION_CHECK" == "false" ] && [ "$CQ_VERSION" == "$VERSION_FULL" ]; then
    echo "INFO: there is no new version released, bye!"
    exit 0
  fi
  git checkout $CQ_VERSION
  # Get all variables in the new Camel Quarkus release
  CAMEL_MM=$(grep -oPm1 "(?<=<camel.major.minor>)[^<]+" pom.xml)
  CAMEL_P=$(grep -oPm1 "(?<=<camel.version>)[^<]+" pom.xml)
  CAMEL_VERSION="$CAMEL_MM${CAMEL_P#"\${camel.major.minor}"}"
  QUARKUS_VERSION=$(grep -oPm1 "(?<=<quarkus.version>)[^<]+" pom.xml)

  echo "Next Camel Quarkus version is $CQ_VERSION"
  echo "Next Camel version is $CAMEL_VERSION"
  echo "Next Quarkus version is $QUARKUS_VERSION"

  popd
  popd

  if [ "$DRY_RUN" == "true" ]; then
    exit 0
  fi

  mvn -ntp versions:set -DnewVersion="$CQ_VERSION-SNAPSHOT" -DgenerateBackupPoms=false
  mvn -ntp versions:set -DnewVersion="$CQ_VERSION-SNAPSHOT" -f support/camel-k-runtime-bom/pom.xml -DgenerateBackupPoms=false
  # We also need to align the following properties
  # camel-version
  mvn -ntp versions:update-parent "-DparentVersion=[$CAMEL_VERSION]" -DgenerateBackupPoms=false
  mvn -ntp versions:set-property -Dproperty="camel-version" -DnewVersion="$CAMEL_VERSION" -DgenerateBackupPoms=false
  # camel-quarkus-version
  mvn -ntp versions:set-property -Dproperty="camel-quarkus-version" -DnewVersion="$CQ_VERSION" -DgenerateBackupPoms=false
  # quarkus-version
  mvn -ntp versions:set-property -Dproperty="quarkus-version" -DnewVersion="$QUARKUS_VERSION" -DgenerateBackupPoms=false
  # quarkus-platform-version
  mvn -ntp versions:set-property -Dproperty="quarkus-platform-version" -DnewVersion="$QUARKUS_VERSION" -DgenerateBackupPoms=false
}

parse_args(){
  while [ $# -gt 0 ]
  do
      arg="$1"
      case $arg in
        -h|--help)
          display_usage
          exit 0
          ;;
        --dry-run)
          DRY_RUN="true"
          ;;
        --skip-version-check)
          SKIP_VERSION_CHECK="true"
          ;;
        *)
          echo "❗ unknown argument: $1"
          display_usage
          exit 1
          ;;
      esac
      shift
  done
}

main $*
