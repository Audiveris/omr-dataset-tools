#!/bin/bash

# Upload artifacts to bintray
# Prior to this, use: gradle clean build
# Credentials are in $HOME/.netrc

# (Manually modify the next line to desired version)
VERSION=1.0.0

TARGET=https://api.bintray.com/content/audiveris/omrdataset/omr-dataset-tools/$VERSION

for SUFFIX in "" -javadoc -sources
do
    curl -n -T build/libs/omrdataset-$VERSION$SUFFIX.jar $TARGET/omrdataset-$VERSION$SUFFIX.jar
    echo
done