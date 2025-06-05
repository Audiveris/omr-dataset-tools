#!/bin/bash

MODEL="build/headclassifierV3Small_h54_w54_s12-sub.h5"

# Done
#./gradlew run --args="-model $MODEL -context HEAD -train 1-9 -- D:\soft\mscore-dataset\MuseScore\archive-0\xml_annotations" 2>&1 | tee data/material/keras-training.txt

# To do
#./gradlew run --args="-model $MODEL -context HEAD -train 1-9 -- D:\soft\DeepScores\archive-0\xml_annotations" 2>&1 | tee -a data/material/training.txt
./gradlew run --args="-model $MODEL -context HEAD -train 1-9 -- D:\soft\DeepScores\archive-1\xml_annotations" 2>&1 | tee -a data/material/training.txt
./gradlew run --args="-model $MODEL -context HEAD -train 1-9 -- D:\soft\DeepScores\archive-2\xml_annotations" 2>&1 | tee -a data/material/training.txt
#./gradlew run --args="-model $MODEL -context HEAD -train 1-9 -- D:\soft\DeepScores\archive-3\xml_annotations" 2>&1 | tee -a data/material/training.txt
#./gradlew run --args="-model $MODEL -context HEAD -train 1-9 -- D:\soft\DeepScores\archive-4\xml_annotations" 2>&1 | tee -a data/material/training.txt
#./gradlew run --args="-model $MODEL -context HEAD -train 1-9 -- D:\soft\DeepScores\archive-5\xml_annotations" 2>&1 | tee -a data/material/training.txt

# Not yet available
#./gradlew run --args="-model $MODEL -context HEAD -train 1-9 -- D:\soft\DeepScores\archive-6\xml_annotations" 2>&1 | tee -a data/material/training.txt