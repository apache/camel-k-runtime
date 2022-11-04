#!/bin/bash

set -e

display_usage() {

cat <<EOF
Bump Camel K Runtime project Camel and Quarkus related dependencies

Usage: ./script/bump.sh [options]
--version                 Bump Camel K runtime version
--camel                   Bump Camel version
--camel-quarkus           Bump Camel-Quarkus version
--quarkus                 Bump Quarkus version
--quarkus-platform        Bump Quarkus platform version (could differ from quarkus core)
--help                    This help message

Example: ./script/bump.sh --version 1.14.0-SNAPSHOT --camel 3.16.0
EOF

}

VERSION=""
CAMEL=""
CAMELQUARKUS=""
QUARKUS=""
QUARKUSPLATFORM=""

main() {
  parse_args $@

  if [[ ! -z "$VERSION" ]]; then
    mvn versions:set -DnewVersion="$VERSION" -DgenerateBackupPoms=false
    mvn versions:set -DnewVersion="$VERSION" -f support/camel-k-runtime-bom/pom.xml -DgenerateBackupPoms=false
    echo "Camel K runtime project set to $VERSION"
  fi

  if [[ ! -z "$CAMEL" ]]; then
    mvn versions:update-parent "-DparentVersion=[$CAMEL]" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty="camel-version" -DnewVersion="$CAMEL" -DgenerateBackupPoms=false
    echo "Camel version set to $CAMEL"
  fi

  if [[ ! -z "$CAMELQUARKUS" ]]; then
    mvn versions:set-property -Dproperty="camel-quarkus-version" -DnewVersion="$CAMELQUARKUS" -DgenerateBackupPoms=false
    echo "Camel Quarkus version set to $CAMELQUARKUS"
  fi

  if [[ ! -z "$QUARKUS" ]]; then
    mvn versions:set-property -Dproperty="quarkus-version" -DnewVersion="$QUARKUS" -DgenerateBackupPoms=false
    echo "Quarkus version set to $QUARKUS"
  fi

    if [[ ! -z "$QUARKUSPLATFORM" ]]; then
    mvn versions:set-property -Dproperty="quarkus-platform-version" -DnewVersion="$QUARKUSPLATFORM" -DgenerateBackupPoms=false
    echo "Quarkus platform version set to $QUARKUSPLATFORM"
  fi
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
        --version)
          shift
          VERSION="$1"
          ;;
        --camel)
          shift
          CAMEL="$1"
          ;;
        --camel-quarkus)
          shift
          CAMELQUARKUS="$1"
          ;;
        --quarkus)
          shift
          QUARKUS="$1"
          ;;
        --quarkus-platform)
          shift
          QUARKUSPLATFORM="$1"
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
