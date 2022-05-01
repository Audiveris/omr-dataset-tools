#!/bin/bash

./gradlew run --args="-context HEAD -train 1-9 -- D:\soft\mscore-dataset\MuseScore\archive-0\xml_annotations" 2>&1 | tee data/material/training.txt

./gradlew run --args="-context HEAD -train 1-9 -- D:\soft\DeepScores\archive-0\xml_annotations" 2>&1 | tee -a data/material/training.txt
./gradlew run --args="-context HEAD -train 1-9 -- D:\soft\DeepScores\archive-1\xml_annotations" 2>&1 | tee -a data/material/training.txt
./gradlew run --args="-context HEAD -train 1-9 -- D:\soft\DeepScores\archive-2\xml_annotations" 2>&1 | tee -a data/material/training.txt
./gradlew run --args="-context HEAD -train 1-9 -- D:\soft\DeepScores\archive-3\xml_annotations" 2>&1 | tee -a data/material/training.txt
./gradlew run --args="-context HEAD -train 1-9 -- D:\soft\DeepScores\archive-4\xml_annotations" 2>&1 | tee -a data/material/training.txt

./gradlew run --args="-context HEAD -train 1-9 -- D:\soft\DeepScores\archive-5\xml_annotations" 2>&1 | tee -a data/material/training.txt

# Not yet available
#./gradlew run --args="-context HEAD -train 1-9 -- D:\soft\DeepScores\archive-6\xml_annotations" 2>&1 | tee -a data/material/training.txt