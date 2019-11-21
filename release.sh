#!/bin/bash
set -e

mvn -B help:evaluate -Dexpression=project.version | tee project_version

SEMANTIC_VERSION_WITH_SHA=$(cat project_version | grep -v INFO | grep -v WARNING)
SEMANTIC_VERSION_INCLUDING_PATCH=${SEMANTIC_VERSION_WITH_SHA%-*}
SEMANTIC_VERSION_EXCLUDING_PATCH=${SEMANTIC_VERSION_INCLUDING_PATCH%.*}
PATCH_VERSION=$(echo "$SEMANTIC_VERSION_INCLUDING_PATCH" | sed "s/$SEMANTIC_VERSION_EXCLUDING_PATCH.//")
NEW_PATCH_VERSION=$(($PATCH_VERSION+1))
COMMIT_SHA=$(git rev-parse --short=12 HEAD)
VERSION="$SEMANTIC_VERSION_EXCLUDING_PATCH.$NEW_PATCH_VERSION-$COMMIT_SHA"

# Update to semantic version with commit hash
echo "Setting release version: $VERSION"
mvn -B versions:set -DnewVersion="$VERSION"

echo "Running release"
mvn -B --settings maven-settings.xml deploy -Dmaven.wagon.http.pool=false

# Update to semantic SNAPSHOT version
VERSION="$SEMANTIC_VERSION_EXCLUDING_PATCH.$NEW_PATCH_VERSION-SNAPSHOT"
echo "Setting SNAPSHOT version: $VERSION"
mvn -B versions:set -DnewVersion="$VERSION"
