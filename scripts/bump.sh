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
--help                    This help message

Example: ./script/bump.sh --version 1.14.0-SNAPSHOT --camel 3.16.0
EOF

}

VERSION=""
CAMEL=""
CAMELQUARKUS=""
QUARKUS=""

main() {
  parse_args $@

  if [[ ! -z "$VERSION" ]]; then
    mvn versions:set -DnewVersion="$VERSION" -DgenerateBackupPoms=false
    mvn versions:set -DnewVersion="$VERSION" -f support/camel-k-runtime-bom/pom.xml -DgenerateBackupPoms=false
    echo "Camel K runtime project set to $VERSION"
  fi

  if [[ ! -z "$CAMEL" ]]; then
    mvn versions:set-property -Dproperty="camel-version" -DnewVersion="$CAMEL"
    echo "Camel version set to $CAMEL"
  fi

  if [[ ! -z "$CAMELQUARKUS" ]]; then
    mvn versions:set-property -Dproperty="camel-quarkus-version" -DnewVersion="$CAMELQUARKUS"
    echo "Camel Quarkus version set to $CAMELQUARKUS"
  fi

  if [[ ! -z "$QUARKUS" ]]; then
    mvn versions:set-property -Dproperty="quarkus-version" -DnewVersion="$QUARKUS"
    echo "Quarkus platform version set to $QUARKUS"
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
