#!/bin/bash

./gradlew run --args="-parallel -context HEAD -nones -control -features -tally -bins -shuffle -- D:\soft\DeepScores\archive-0\xml_annotations" 2>&1 | tee data/material/D-0-shuffle.txt
./gradlew run --args="-parallel -context HEAD -nones -control -features -tally -bins -shuffle -- D:\soft\DeepScores\archive-1\xml_annotations" 2>&1 | tee data/material/D-1-shuffle.txt
./gradlew run --args="-parallel -context HEAD -nones -control -features -tally -bins -shuffle -- D:\soft\DeepScores\archive-2\xml_annotations" 2>&1 | tee data/material/D-2-shuffle.txt
./gradlew run --args="-parallel -context HEAD -nones -control -features -tally -bins -shuffle -- D:\soft\DeepScores\archive-3\xml_annotations" 2>&1 | tee data/material/D-3-shuffle.txt
./gradlew run --args="-parallel -context HEAD -nones -control -features -tally -bins -shuffle -- D:\soft\DeepScores\archive-4\xml_annotations" 2>&1 | tee data/material/D-4-shuffle.txt