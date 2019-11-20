#!/bin/bash
set -e

mvn -B help:evaluate -Dexpression=project.version | tee project_version

if [ $? -gt 0 ]
 then
   echo "something fishy happened"
   exit 1;
fi

MAJOR_VERSION_WITH_SHA=$(cat project_version | grep -v INFO | grep -v WARNING)
MAJOR_VERSION=${MAJOR_VERSION_WITH_SHA%-*}
COMMIT=$(git rev-parse --short=12 HEAD)
VERSION="$MAJOR_VERSION-$COMMIT"
echo "Setting version $VERSION"

mvn -B versions:set -DnewVersion="$VERSION"
mvn -B versions:commit

echo "Running release"
mvn -B --settings maven-settings.xml deploy -Dmaven.wagon.http.pool=false
