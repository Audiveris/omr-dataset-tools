# Model training
---

The `train` sub-project focuses on training a YOLO model on the prepared dataset(s).

## Description

- Input is a YOLO pre-trained model
- Data is a set of score images and labels
- Process is a series of training epochs, using a GPU on Google Colab
- Output is an ONNX-formatted model ready for inference time

## Configuration

Here is the content of the `dataset.yaml` configuration file,
that has been used to train a Yolo Model (`yolo12s.pt`) with the DeepScores + Beethoven dataset.

```
# Trying yolov12 on DeepScores and Beethoven

# Paths to the "train" and "val" folders
train: /content/drive/MyDrive/YOLO/datasets/medium/images/train # train images
val: /content/drive/MyDrive/YOLO/datasets/medium/images/val # val images

# Augmentation settings
augmentations:
  degrees: 0.25  # Rotates image randomly within specified degree range (0.0 - 180)
  shear: 0.1     # Shears the image by a specified degree (-180 - +180)
  flipud: 0.0    # Disables vertical flipping (0.0 is the default value)
  fliplr: 0.0    # Disables horizontal flipping
  mosaic: 0.0    # Combines four training images into one (default is 1.0)
  
# Classes
names:
  0: brace
  1: bracket
  2: ledgerLine
  3: barlineSingle
  4: barlineDouble
  5: barlineHeavy
  6: repeatLeft
  7: repeatRight
  8: repeatRightLeft
  9: segno
... (truncated)
  143: fingering3
  144: fingering4
  145: fingering5
```
## Training

This is done using Google Colab, with the `YoloOnScores.ipynb` notebook.
(the file is saved in this folder):

![YoloOnScores.ipynb](./YoloOnScores.ipynb)

The input is read from my Google drive and the resulting outputs (last.pt/best.pt and best.onnx) are copied back to my Google drive.

I trained the model incrementally on about 100 epochs.
Only 20 epochs at a time, because this takes about 4.5 hours and I get disconnected when trying longer sessions (although I pay Google credits...)

## Tests

See the `infer` sibling sub-project.