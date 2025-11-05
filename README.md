# omr-dataset-tools

This repository was created in 2017 as an attempt to manage a dataset specifically meant for
the training and evaluation of optical music recognition (OMR) tools.  
The original content is still available in the "**original**" branch.

In 2022, it was used to train and test a specific classifier called a "patch-classifier",
aiming at recognizing any musical symbol centered on a given image location.
The patch classifier was too slow to be effective on a full-page and was finally abandoned.  
The related content is still available in the "**patch-classifier**" branch.

In 2025, it was refocused on the training of a full-page detector/classifier based on 
latest architectures like YOLO.
This is the content of this "**development**" branch



## Work in progress

This repository is now focused on the training of a deep learning model suitable for Audiveris OMR.

It is based on:
- The YOLO architecture and tools provided by Ultralytics
- The datasets provided by DeepScores V2 and DoReMi
- Perhaps other datasets (such as those provided by Audiveris sampling)
- The use of ONNX format between YOLO (Python) and DeepLearning4j (Java)

## Structure

The repository is a Gradle project composed of 3 sub-projects, namely:
- [**prepare**](./prepare/README.md): to prepare the images and labels according to the YOLO expected input format.
- [**train**](./train/README.md): to train and validate the model on the prepared data-set.
- [**infer**](./infer/README.md): to use the model in detection mode on representative music scores.
