#!/bin/bash

set -e

display_usage() {

cat <<EOF
Bump Camel K Runtime project synchronizing the dependency from Camel Quarkus

Usage: ./script/bump.sh --camel-quarkus <camel-quarkus-version>

--camel-quarkus           Bump Camel-Quarkus version
--help                    This help message

Example: ./script/bump.sh --camel-quarkus 2.16.0
EOF

}

CAMELQUARKUS=""

main() {
  parse_args $@

  if [[ ! -z "$CAMELQUARKUS" ]]; then
    mvn versions:set -DnewVersion="$CAMELQUARKUS-SNAPSHOT" -DgenerateBackupPoms=false
    mvn versions:set -DnewVersion="$CAMELQUARKUS-SNAPSHOT" -f support/camel-k-runtime-bom/pom.xml -DgenerateBackupPoms=false
    echo "Camel K runtime project set to $CAMELQUARKUS-SNAPSHOT"
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
        --camel-quarkus)
          shift
          CAMELQUARKUS="$1"
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
