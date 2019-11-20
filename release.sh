#!/bin/bash
set -e

mvn -B help:evaluate -Dexpression=project.version | tee project_version

if [ $? -gt 0 ]
 then
   echo "something fishy happened"
   exit 1;
fi

SEMANTIC_VERSION_WITH_SHA=$(cat project_version | grep -v INFO | grep -v WARNING)
SEMANTIC_VERSION_INCLUDING_PATCH=${SEMANTIC_VERSION_WITH_SHA%-*}
SEMANTIC_VERSION_EXCLUDING_PATCH=${SEMANTIC_VERSION_INCLUDING_PATCH%.*}
PATCH_VERSION=$(echo "$SEMANTIC_VERSION_INCLUDING_PATCH" | sed "s/$SEMANTIC_VERSION_EXCLUDING_PATCH.//")
NEW_PATCH_VERSION=$(($PATCH_VERSION+1))
COMMIT_SHA=$(git rev-parse --short=12 HEAD)
VERSION="$SEMANTIC_VERSION_EXCLUDING_PATCH.$NEW_PATCH_VERSION-$COMMIT_SHA"
echo "Setting version $VERSION"

mvn -B versions:set -DnewVersion="$VERSION"
mvn -B versions:commit

echo "Running release"
mvn -B --settings maven-settings.xml deploy -Dmaven.wagon.http.pool=false
