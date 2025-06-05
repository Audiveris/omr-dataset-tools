#!/bin/bash

OUT=data/material/ds-training.txt

#./gradlew run --args="@data/args.txt -train   1-100" 2>&1 | tee -a $OUT
./gradlew run --args="@data/args.txt -train 101-200" 2>&1 | tee -a $OUT
#./gradlew run --args="@data/args.txt -train 201-400" 2>&1 | tee -a $OUT
#./gradlew run --args="@data/args.txt -train 400-600" 2>&1 | tee -a $OUT
#./gradlew run --args="@data/args.txt -train 600-800" 2>&1 | tee -a $OUT

