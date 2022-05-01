#!/bin/bash

./gradlew run --args="-parallel -context HEAD -filter -nones -control -features -tally -bins -shuffle -- D:\soft\DeepScores\archive-5\xml_annotations" 2>&1 | tee data/material/D-5-shuffle.txt
./gradlew run --args="-parallel -context HEAD -filter -nones -control -features -tally -bins -shuffle -- D:\soft\DeepScores\archive-6\xml_annotations" 2>&1 | tee data/material/D-6-shuffle.txt