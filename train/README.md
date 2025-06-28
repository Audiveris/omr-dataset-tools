# Model training
---

The `train` sub-project focuses on training a YOLO model on the prepared dataset(s).

## Description

- Input is a YOLO pre-trained model
- Data is a set of score images and labels
- Process is a series of training epochs, using a GPU on Google Colab
- Output is an ONNX-formatted model ready for inference time

## Configuration

Here is the content of the `ds2_dense.yaml` configuration file,
that has been used to train a Yolo Model (`yolo11n.pt`) with the DeepScores dataset.

```
# Paths to the "train" and "val" folders
train: /content/drive/MyDrive/YOLO/datasets/ds2_dense/images/train  # 1362 train images
val: /content/drive/MyDrive/YOLO/datasets/ds2_dense/images/val      #  352 val images

# Augmentation settings
augmentations:
  degrees: 0.25 # Rotates image randomly within specified degree range (0.0 - 180)
  shear: 0.1    # Shears the image by a specified degree (-180 - +180)
  flipud: 0.0   # Disables vertical flipping (0.0 is the default value)
  fliplr: 0.0   # Disables horizontal flipping
  mosaic: 0.0   # Combines four training images into one (default is 1.0)
  
# Classes
names:
  0: brace
  1: ledgerLine
  2: repeatDot
  3: segno
  4: coda
  5: clefG
  6: clefCAlto
  7: clefCTenor
  8: clefF
  9: clefUnpitchedPercussion
  10: clef8
  11: clef15
  12: timeSig0
  13: timeSig1
  14: timeSig2
  15: timeSig3
  16: timeSig4
  17: timeSig5
  18: timeSig6
  19: timeSig7
  20: timeSig8
  21: timeSig9
  22: timeSigCommon
  23: timeSigCutCommon
  24: noteheadBlackOnLine
  25: noteheadBlackOnLineSmall
  26: noteheadBlackInSpace
  27: noteheadBlackInSpaceSmall
  28: noteheadHalfOnLine
  29: noteheadHalfOnLineSmall
  30: noteheadHalfInSpace
  31: noteheadHalfInSpaceSmall
  32: noteheadWholeOnLine
  33: noteheadWholeOnLineSmall
  34: noteheadWholeInSpace
  35: noteheadWholeInSpaceSmall
  36: noteheadDoubleWholeOnLine
  37: noteheadDoubleWholeOnLineSmall
  38: noteheadDoubleWholeInSpace
  39: noteheadDoubleWholeInSpaceSmall
  40: augmentationDot
  41: stem
  42: tremolo1
  43: tremolo2
  44: tremolo3
  45: tremolo4
  46: tremolo5
  47: flag8thUp
  48: flag8thUpSmall
  49: flag16thUp
  50: flag32ndUp
  51: flag64thUp
  52: flag128thUp
  53: flag8thDown
  54: flag8thDownSmall
  55: flag16thDown
  56: flag32ndDown
  57: flag64thDown
  58: flag128thDown
  59: accidentalFlat
  60: accidentalFlatSmall
  61: accidentalNatural
  62: accidentalNaturalSmall
  63: accidentalSharp
  64: accidentalSharpSmall
  65: accidentalDoubleSharp
  66: accidentalDoubleFlat
  67: keyFlat
  68: keyNatural
  69: keySharp
  70: articAccentAbove
  71: articAccentBelow
  72: articStaccatoAbove
  73: articStaccatoBelow
  74: articTenutoAbove
  75: articTenutoBelow
  76: articStaccatissimoAbove
  77: articStaccatissimoBelow
  78: articMarcatoAbove
  79: articMarcatoBelow
  80: fermataAbove
  81: fermataBelow
  82: caesura
  83: restDoubleWhole
  84: restWhole
  85: restHalf
  86: restQuarter
  87: rest8th
  88: rest16th
  89: rest32nd
  90: rest64th
  91: rest128th
  92: restHNr
  93: dynamicP
  94: dynamicM
  95: dynamicF
  96: dynamicS
  97: dynamicZ
  98: dynamicR
  99: graceNoteAcciaccaturaStemUp
  100: graceNoteAppoggiaturaStemUp
  101: graceNoteAcciaccaturaStemDown
  102: graceNoteAppoggiaturaStemDown
  103: ornamentTrill
  104: ornamentTurn
  105: ornamentTurnInverted
  106: ornamentMordent
  107: stringsDownBow
  108: stringsUpBow
  109: arpeggiato
  110: keyboardPedalPed
  111: keyboardPedalUp
  112: tuplet3
  113: tuplet6
  114: fingering0
  115: fingering1
  116: fingering2
  117: fingering3
  118: fingering4
  119: fingering5
  120: slur
  121: beam
  122: tie
  123: restHBar
  124: dynamicCrescendoHairpin
  125: dynamicDiminuendoHairpin
  126: tuplet1
  127: tuplet2
  128: tuplet4
  129: tuplet5
  130: tuplet7
  131: tuplet8
  132: tuplet9
  133: tupletBracket
  134: staff
  135: ottavaBracket
```
