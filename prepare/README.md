# Dataset preparation

>[!WARNING] 
>**This is a work in progress, based on a preliminary use of DeepScores V2, DoReMi V1 and a Beethoven opus.**

## Goal
 
Starting from some dataset "source material", we have to prepare the files content and structure 
as expected by the YOLO model:

```
  + images/            // Images folder, split into train and val
  |  + train/
  |  | + aaa.png
  |  | + bbb.png
  |  | + ...
  |  + val/
  |    + xxx.png
  |    + yyy.png
  |    + ...
  + labels/            // Labels folder, split into train and val
     + train/
     | + aaa.txt       // Each row contains: classId, xc, yc, w, h (normalized values)
     | + bbb.txt
     | + ...
     + val/
       + xxx.txt
       + yyy.txt
       + ...
```
Plus
```
  data.yaml     // Processing configuration for YOLO
```

The `images` folder expects two sub-folders (`train` and `val`) where the images files must
be stored.

The sibling `labels` folder also expects two sub-folders (`train` and `val`)
where the symbols of each image must be described via a label file,
using the same name radix as the image file name and the `.txt` extension.  
For each symbol in the image, the label file should contain one line formatted as:
> class_id centerX  centerY  with  height

For example: 
```
...
 33 0.113010 0.056457 0.001020 0.040404
  5 0.065051 0.068543 0.021429 0.035714
  0 0.040306 0.097222 0.008673 0.082973
 25 0.217347 0.129690 0.008673 0.005772
 ...
```
Which defines (...,  a stem, a clefG, a brace, a noteheadBlackInSpace, ...)

| Item | Type | Comment |
| :--- | :--- | :--- |
| class_id | int   | class rank, counted from 0 |
| centerX | double | abscissa of symbol center, normalized by the image width |
| centerY | double | ordinate of symbol center, normalized by the image height |
| width   | double | symbol width, normalized by the image width |
| height  | double | symbol height, normalized by the image height |


In addition to the `images` and `labels` folders,
we need to write a `.yaml` data file, named as we like, to describe the dataset to Yolo and define training parameters.  
The file does not have to be located next to the `images` and `labels` folder, 
its path will be provided as an argument to the Yolo program.

For example:

```yaml
# Trying yolov11 on DeepScore V2 (dense subset), second campaign
train: /content/drive/MyDrive/YOLO/datasets/ds2_dense/images/train # 1305 train images
val: /content/drive/MyDrive/YOLO/datasets/ds2_dense/images/val     #  340 val images
test: # test images (optional)

# Augmentation settings
augmentations:
  degrees: 0.5  # Rotates image randomly within specified degree range (0.0 - 180)
  shear: 0.5    # Shears the image by a specified degree (-180 - +180)
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
  etc...
```

This configuration file is written manually,
using a copy/paste of the output of the operations (see below)
where the various labels are listed with their ordinal value.

## Operations

The main() method of the `org.audiveris.omrdataset.prepare.DataSetFactory` Java class launches the processing of DeepScores, DoReMi and Beethoven datasets in sequence:
```java
    public static void main (String... args)
        throws Exception
    {
        new DeepScores("yolo.yaml", "deepscores.yaml").process();
        new DoReMi("yolo.yaml", "doremi.yaml").process();
        new Beethoven("yolo.yaml", "beethoven.yaml").process();
    }
```
`DeepScores`, `DoReMi` and `Beethoven` classes inherit from the `DataSetFactory` abstract class.

These classes expect two parameters:
- The path to a file configuration for the Yolo target, here: [yolo.yaml](./yolo.yaml)
- The path to a file configuration for the source dataset, here [deepscores.yaml](./deepscores.yaml), [doremi.yaml](./doremi.yaml) and [beethoven.yaml](./beethoven.yaml) respectively.

All these files are located in the "prepare" sub-project.

### The `yolo.yaml` file

```yaml
#--------------------------------
# Configuration for Yolo dataset
#--------------------------------

# Folder where datasets for Yolo should be written
target: D:\\soft\\datasets\\yolo

# Should we print ID and name of YoloLabels?
print_labels: false
```

Explanations:
- The `target` path value should be adjusted to your local environment.
- The `print_labels` boolean governs the print out of the YoloLabel values (id and name).

### The `deepscores.yaml`, `doremi.yaml` and `beethoven.yaml` files

As of this writing, we have three configuration files:
- [deepscores.yaml](./deepscores.yaml)
- [doremi.yaml](./doremi.yaml)
- [beethoven.yaml](./beethoven.yaml)

They present a very similar structure.
Here below is an example (excerpt):

```yaml
# source folder
source: D:\\soft\\DoReMi_v1

# images sub-folder
images: Images

# annotations sub-folder
annotations: Parsed_by_page_omr_xml

# List of tasks to be actually performed
tasks:
  - histogram
  - checking
  - train
  - val

# Specifications for some visual checking
checking:
  output: drm-annotated-images          # where to store the annotated images
  draw_name: true                       # draw class name rather than class number?
  hidden_labels:                        # labels not to be drawn
    - kStaffLine
  required_labels:                  # limit to images containing any of these items
    - systemicBarline
    - timeSignatureComponent 
  use_train: false                      # select full train?
  use_val: false                        # select full val?
  selection:                            # list of selected images
    - 'Reger - Introduction-023.png'
    - 'Reger - Introduction-022.png'

# train pages (4175)
train:
  - 'Reger - Introduction-001.png'
  - 'Reger - Introduction-020.png'
  - 'Reger - Introduction-023.png'
  - 'Reger - Introduction-019.png'
  - 'Reger - Introduction-016.png'
  etc...
# val pages (109)
val:
  - 'beam stem weight notes 1-040.png'
  - 'beam groups 8 semiquavers-058.png'  
  etc...
```  
Explanations:
- The `source` parameter should be adjusted to your local environment,
to point to the location where the source dataset has been downloaded.
- The `images` parameter gives the folder of images under the source folder
- The `annotations` parameters gives the folder of annotations under the source folder
(This parameter does not exist for DeepScores)
- The `print_predefined_labels` boolean is specific to DeepScores which provides a list of predefined labels.
If set to true, these predefined labels are printed out.
- The `tasks` list gives the tasks to perform actually (any task can be commented out):
  - `histogram` prints out the counts per class on the whole dataset
  - `checking` draws selected images with their symbols
  - `train` prepares the train part (copy images files and write labels files)
  - `val` prepares the val part (idem)
- The `checking` parameter defines the list of images selected for a visual check.
- The `train` list defines the names of all images selected for training
- The `val` list gives the names of all images selected for validation

## DeepScores V2

Remark: For the first tests, we used the predefined list of classes (labels)
provided by the DeepScores V2 dataset.
We'll see later that this list of classes should be modified.

### Images

As recommended, we chose the `ds2_dense` subset of DeepScores V2.

Two `.json` files describe the symbols found in these images:
- `deepscores_train.json`
- `deepscores_test.json`

| Part | Images count | Min size | Max size | Most frequent size | % for most frequent |
| :--- |  ---: |   :---:  |   :---:  | :---:              | :---                |
| train | 1362 | 1597 x 2259 |  3842 x 5434 |1960 x 2772 | 75% |
| test  |  352 | 1597 x 2259 |  3842 x 5434 |1960 x 2772 | 72% |
| total | 1714 |

We initially used `deepscores_train.json` for the train part
and `deepscores_test.json` for the val part.

We have now discarded this separation, so that we can more easily define,
via the `deepscores.yaml` configuration file, which pages should go to which parts.

### Classes

A list of class names is predefined in the `.json` files of DeepScores.
We have thus been able to detect the class names for which no instance at all appear in the dataset.

Class instances in the DeepScores dataset, sorted alphabetically:

| Class                                  |  Train |    Val |
| :---                                   |   ---: |   ---: |
| accidentalDoubleFlat                   |      6 |      0 |
| accidentalDoubleSharp                  |    169 |     80 |
| accidentalFlat                         |   4307 |   1163 |
| accidentalFlatSmall                    |      0 |      0 |
| accidentalNatural                      |   6099 |   1939 |
| accidentalNaturalSmall                 |      0 |      0 |
| accidentalSharp                        |   7480 |   2130 |
| accidentalSharpSmall                   |      0 |      0 |
| arpeggiato                             |    290 |     71 |
| articAccentAbove                       |   1535 |    521 |
| articAccentBelow                       |    829 |    274 |
| articMarcatoAbove                      |    168 |     88 |
| articMarcatoBelow                      |    197 |     70 |
| articStaccatissimoAbove                |    351 |     59 |
| articStaccatissimoBelow                |    174 |     89 |
| articStaccatoAbove                     |   6408 |   1193 |
| articStaccatoBelow                     |   1765 |    503 |
| articTenutoAbove                       |    666 |     82 |
| articTenutoBelow                       |     95 |     27 |
| augmentationDot                        |  18723 |   5525 |
| beam                                   |  69016 |  18841 |
| brace                                  |   2879 |    725 |
| caesura                                |    139 |    146 |
| clef15                                 |    219 |     76 |
| clef8                                  |   1101 |    305 |
| clefCAlto                              |   1015 |    255 |
| clefCTenor                             |    614 |    167 |
| clefF                                  |   5405 |   1488 |
| clefG                                  |   8332 |   2203 |
| clefUnpitchedPercussion                |      0 |      0 |
| coda                                   |    225 |     49 |
| dynamicCrescendoHairpin                |   1419 |    298 |
| dynamicDiminuendoHairpin               |    893 |    192 |
| dynamicF                               |   6065 |   1437 |
| dynamicM                               |   2330 |    533 |
| dynamicP                               |   3976 |   1096 |
| dynamicR                               |     29 |      4 |
| dynamicS                               |    469 |    127 |
| dynamicZ                               |    280 |     70 |
| fermataAbove                           |    649 |    184 |
| fermataBelow                           |    207 |     82 |
| fingering0                             |    158 |    140 |
| fingering1                             |    779 |    226 |
| fingering2                             |    730 |    138 |
| fingering3                             |    538 |    146 |
| fingering4                             |    480 |    131 |
| fingering5                             |    234 |      3 |
| flag128thDown                          |    255 |     42 |
| flag128thUp                            |    289 |     45 |
| flag16thDown                           |    691 |    335 |
| flag16thUp                             |    891 |    263 |
| flag32ndDown                           |     56 |    239 |
| flag32ndUp                             |    164 |     49 |
| flag64thDown                           |    165 |     42 |
| flag64thUp                             |    156 |     29 |
| flag8thDown                            |   8053 |   2281 |
| flag8thDownSmall                       |      0 |      0 |
| flag8thUp                              |   7136 |   1941 |
| flag8thUpSmall                         |      0 |      0 |
| graceNoteAcciaccaturaStemDown          |      0 |      0 |
| graceNoteAcciaccaturaStemUp            |      0 |      0 |
| graceNoteAppoggiaturaStemDown          |      0 |      0 |
| graceNoteAppoggiaturaStemUp            |      0 |      0 |
| keyFlat                                |  10954 |   3188 |
| keyNatural                             |    507 |    183 |
| keySharp                               |  12685 |   3478 |
| keyboardPedalPed                       |    203 |     93 |
| keyboardPedalUp                        |    496 |    144 |
| ledgerLine                             |  88316 |  23809 |
| noteheadBlackInSpace                   | 125410 |  33887 |
| noteheadBlackInSpaceSmall              |      0 |      0 |
| noteheadBlackOnLine                    | 125497 |  34743 |
| noteheadBlackOnLineSmall               |      0 |      0 |
| noteheadDoubleWholeInSpace             |     51 |     21 |
| noteheadDoubleWholeInSpaceSmall        |      0 |      0 |
| noteheadDoubleWholeOnLine              |    157 |     57 |
| noteheadDoubleWholeOnLineSmall         |      0 |      0 |
| noteheadHalfInSpace                    |  11297 |   2805 |
| noteheadHalfInSpaceSmall               |      0 |      0 |
| noteheadHalfOnLine                     |  10718 |   2876 |
| noteheadHalfOnLineSmall                |      0 |      0 |
| noteheadWholeInSpace                   |   3841 |   1008 |
| noteheadWholeInSpaceSmall              |      0 |      0 |
| noteheadWholeOnLine                    |   3453 |    865 |
| noteheadWholeOnLineSmall               |      0 |      0 |
| ornamentMordent                        |    205 |     81 |
| ornamentTrill                          |    230 |     52 |
| ornamentTurn                           |    210 |     71 |
| ornamentTurnInverted                   |    132 |     17 |
| ottavaBracket                          |    141 |     26 |
| repeatDot                              |   2786 |    876 |
| rest128th                              |    575 |     88 |
| rest16th                               |   2194 |    743 |
| rest32nd                               |    441 |    140 |
| rest64th                               |    548 |     93 |
| rest8th                                |   9565 |   2477 |
| restDoubleWhole                        |    221 |     77 |
| restHBar                               |     84 |     27 |
| restHNr                                |      0 |      0 |
| restHalf                               |   2515 |    677 |
| restQuarter                            |   7579 |   2092 |
| restWhole                              |  11239 |   3348 |
| segno                                  |    231 |     55 |
| slur                                   |   9937 |   2408 |
| staff                                  |  14496 |   3864 |
| stem                                   | 235695 |  65062 |
| stringsDownBow                         |    304 |     66 |
| stringsUpBow                           |    433 |     79 |
| tie                                    |  11034 |   3004 |
| timeSig0                               |    213 |     83 |
| timeSig1                               |    407 |    116 |
| timeSig2                               |    274 |    126 |
| timeSig3                               |    823 |    401 |
| timeSig4                               |   3492 |   1349 |
| timeSig5                               |    153 |    146 |
| timeSig6                               |    233 |    185 |
| timeSig7                               |    243 |     40 |
| timeSig8                               |    738 |    322 |
| timeSig9                               |    209 |     69 |
| timeSigCommon                          |    189 |     72 |
| timeSigCutCommon                       |    218 |     45 |
| tremolo1                               |     16 |     14 |
| tremolo2                               |     76 |     16 |
| tremolo3                               |     35 |     17 |
| tremolo4                               |     71 |     13 |
| tremolo5                               |      0 |      0 |
| tuplet1                                |      4 |      0 |
| tuplet2                                |      0 |      0 |
| tuplet3                                |   1985 |    354 |
| tuplet4                                |      6 |      0 |
| tuplet5                                |     21 |      5 |
| tuplet6                                |    332 |    214 |
| tuplet7                                |      5 |      0 |
| tuplet8                                |      5 |      0 |
| tuplet9                                |      2 |      1 |
| tupletBracket                          |    404 |     25 |

### Remarks on this dataset

We made a first run of YOLO11n on the DS2 dataset, without any modification.

Then, we had a closer look at this dataset, and wrote down the following remarks,
in an attempt to better fit Audiveris expectations.

#### `*Small` labels
We can notice that none of the labels with the `Small` suffix are found in the dataset, whether in the training or the validation parts.

We'll see later during the inference, that they get recognized as "standard" labels,
even though their size is smaller. Therefore, we can safely remove these label names.

#### `restHNr` label
There is no instance of this `restHNr` label in the dataset.
I don't know what it is and looks like.
Perhaps the number (Nr) of measures concerned by a restHBar just below?  
We'll remove this label.

#### `staff` label
The `staff` label has 14496 instances in the training part, and 3864 instances in the validation part.  
First inference results have shown that this class is very poorly recognized.  
I think we should simply ignore this label.

#### Noteheads

The labels that refer to note heads are very detailed.

For a black note head for example, DeepScores defines:
- `noteheadBlackInSpace`
- `noteheadBlackInSpaceSmall`
- `noteheadBlackOnLine`
- `noteheadBlackOnLineSmall`

We have already seen that we can safely ignore the `*Small` labels.

What about the `*InSpace` and `*OnLine` labels?
The former represents a head located in a space and the latter represents
a head located right on a staff line or ledger.
And indeed, the surroundings of these two kinds of heads are not identical.

The problem is that other datasets (beginning with the DoReMi dataset) don't make this difference.

So, I think we should ignore this difference and "merge" these two labels.
The Yolo model should be able to learn this.

What is more annoying, is the case shown below, found in "lg-648395017847626711-aug-emmentaler--page-2.png":

![](./assets/CrossHeads%20in%20lg-648395017847626711-aug-emmentaler--page-2.png)

In this example (I stumbled upon it while reviewing some checking results), two cross-heads are labelled as `noteHeadBlack`.  
What should we do?

#### Dynamics

DeepScores defines only six "1-letter symbols" (f, m, p, r, s, z) as `dynamicF`, `dynamicM`, etc.
It has no notion of compound symbols like ppp, pp, mp, mf, ff, fff, fp, df, sfz, sfp...

Since other datasets do handle such compound symbols, we need a way to populate the Yolo dataset
with the dynamic compound symbols located in the DeepScores images.
For example, by detecting and building them on-the-fly during the preparation phase.

UPDATE: After having implemented this "merging" of compound dynamics, the histogram for dynamics names is now the following:

| Class                                  |  Train |    Val |
| :---                                   |   ---: |   ---: |
| dynamicF                               |   1415 |    343 |
| dynamicFF                              |    452 |    110 |
| dynamicFFF                             |    251 |     62 |
| dynamicFFFF                            |    203 |     54 |
| dynamicFFFFF                           |     63 |     12 |
| dynamicFFFFFF                          |      5 |      0 |
| dynamicFP                              |    212 |     30 |
| dynamicFZ                              |      1 |      0 |
| dynamicM                               |    349 |    101 |
| dynamicMF                              |   1056 |    235 |
| dynamicMP                              |    909 |    196 |
| dynamicP                               |   1443 |    354 |
| dynamicPF                              |      0 |      0 |
| dynamicPP                              |    290 |     67 |
| dynamicPPP                             |    202 |     87 |
| dynamicPPPP                            |     38 |     22 |
| dynamicPPPPP                           |      4 |      6 |
| dynamicPPPPPP                          |      5 |      0 |
| dynamicRF                              |      0 |      0 |
| dynamicRFZ                             |     29 |      4 |
| dynamicSF                              |    199 |     58 |
| dynamicSFF                             |      4 |      1 |
| dynamicSFFZ                            |     18 |     15 |
| dynamicSFP                             |     12 |      2 |
| dynamicSFPP                            |      0 |      0 |
| dynamicSFZ                             |    232 |     51 |
| dynamicSFZP                            |      0 |      0 |

#### Scores scales

The interline value on the whole corpus has been measured as follows:
| Interline value | Scores count | % |
| ---: | ---: | ---: |
| 17 pixels | 1683 |98.2% |
| 16 pixels | 26 | 1.5% |
| no interline | 5 |0.3%|
| total: | 1714 | 100% |

Basically, all the scores in this dataset have the same interline value (16-17).
This results from the way these synthetic scores have been generated.

Could this lack of diversity have a negative impact on the model efficiency
when applied to real scores with a different scale?

Especially when, in the same score image, we are faced with staves with different spacing values.

#### Abnormal scores

We noticed a few scores in which the symbols overlap so much that these artificial images
cannot be considered as "real" scores.

For example, in file "lg-505588761689839126-aug-beethoven--page-1.png":

![](./assets/Collisions%20in%20lg-505588761689839126-aug-beethoven--page-1.png)

The best option is to get rid of these unrealistic scores.

As of this writing, we have discarded all the files starting from these radices:
- lg-113404515
- lg-16336832
  - except lg-16336832-aug-emmentaler--page-1.png
  - except lg-16336832-aug-gutenberg1939--page-1.png
- lg-202724943
- lg-505588761689839126
- lg-60516219
- lg-6677673
- lg-69678954220027474
- lg-815575830056765029
  - except lg-815575830056765029-aug-lilyjazz--page-6.png

## DoReMi V1

### Images

We have potentially 5218 images!

They all share the same dimensions: 2475 x 3504 pixels.

I very arbitrarily sorted the images by their file size, then took the first 80% for "train" and the remaining 20% for "val". Just to have something to start with...

### Classes

Class instances in the DoReMi dataset, sorted alphabetically:

| Class                                  |  Train |    Val |
| :---                                   |   ---: |   ---: |
| accidentalDoubleFlat                   |    237 |      3 |
| accidentalDoubleSharp                  |    300 |      8 |
| accidentalFlat                         |   5946 |    110 |
| accidentalNatural                      |   4529 |     63 |
| accidentalQuarterToneFlatStein         |     73 |      0 |
| accidentalQuarterToneSharpStein        |     86 |      0 |
| accidentalSharp                        |   5965 |     87 |
| accidentalThreeQuarterTonesSharpStein  |     26 |      0 |
| articAccentAbove                       |    316 |      0 |
| articAccentBelow                       |    369 |      0 |
| articMarcatoAbove                      |    189 |      2 |
| articMarcatoBelow                      |     21 |      0 |
| articStaccatissimoAbove                |    452 |      0 |
| articStaccatissimoBelow                |    332 |      0 |
| articStaccatoAbove                     |   1339 |      0 |
| articStaccatoBelow                     |    867 |      0 |
| articTenutoAbove                       |    330 |      2 |
| articTenutoBelow                       |    284 |      0 |
| augmentationDot                        |    122 |     39 |
| barline                                |   3126 |   1056 |
| beam                                   |   8997 |   1359 |
| cClef                                  |    148 |      8 |
| dynamicFF                              |     63 |      0 |
| dynamicFFF                             |     18 |      0 |
| dynamicForte                           |    298 |      0 |
| dynamicFortePiano                      |     28 |      0 |
| dynamicForzando                        |     18 |      0 |
| dynamicMF                              |     96 |      0 |
| dynamicMP                              |     45 |      0 |
| dynamicPP                              |    140 |      1 |
| dynamicPPP                             |     14 |      0 |
| dynamicPiano                           |    351 |      0 |
| dynamicSforzato                        |    132 |      0 |
| dynamicText                            |     50 |      0 |
| fClef                                  |    924 |      3 |
| flag16thDown                           |    113 |      0 |
| flag16thUp                             |    159 |    364 |
| flag32ndDown                           |     12 |      0 |
| flag32ndUp                             |     76 |      0 |
| flag8thDown                            |   1010 |      0 |
| flag8thUp                              |   1346 |    471 |
| gClef                                  |   1885 |    329 |
| gradualDynamic                         |    874 |      1 |
| kStaffLine                             |  12595 |   1570 |
| noteheadBlack                          |  55887 |   5968 |
| noteheadHalf                           |   2128 |     10 |
| noteheadWhole                          |    483 |     10 |
| ornamentTrill                          |     34 |      0 |
| rest16th                               |    857 |    419 |
| rest32nd                               |     17 |      0 |
| rest8th                                |   2091 |    767 |
| restHalf                               |    227 |      6 |
| restQuarter                            |    927 |    117 |
| restWhole                              |    256 |     80 |
| slur                                   |   3585 |      1 |
| stem                                   |  40666 |   5945 |
| systemicBarline                        |    856 |      3 |
| tie                                    |   3321 |    133 |
| timeSig2                               |    286 |      7 |
| timeSig3                               |    292 |      2 |
| timeSig4                               |    552 |     19 |
| timeSig5                               |     77 |      0 |
| timeSig6                               |     32 |      6 |
| timeSig7                               |     37 |      0 |
| timeSig8                               |    509 |      5 |
| timeSig9                               |     25 |      0 |
| timeSigCommon                          |     34 |      0 |
| timeSigCutCommon                       |      8 |      0 |
| timeSignatureComponent                 |    114 |      1 |
| tupletBracket                          |    354 |    398 |
| tupletText                             |    747 |    268 |

### Remarks on this dataset

I have not yet submitted this dataset to the following training and inference stages.
So, these remarks are based only on the preparation work.

#### `accidental`

All sharp and flat signs in a key signature are labelled as `accidentalSharp` and `accidentalFlat`,
while they are not accidental and would be better named "keySharp" and "keyFlat" as in DeepScores.

See for example  "Haydn - Piano Trio 27 mvt I-011.png":

![](./assets/False%20accidentals%20in%20Haydn%20-%20Piano%20Trio%2027%20mvt%20I-011.png)

#### `beam`

The `beam` label seems to refer, not to a single beam, but to a group of beams.
There is thus no way to retrieve the individual beams.

See this example found in "Reger - Introduction-023.png":

![](./assets/Beam%20in%20Reger%20-%20Introduction-023.png)

#### `gradualDynamic`

First, this label is applied with no difference to both crescendo and diminuendo wedges.

Second, and more annoying, is the bounding box. The abscissa is OK, but the ordinate is very often wrong, as in the example below found in image "Reger - Introduction-023.png":

![](./assets/GradualDynamics%20in%20Reger%20-%20Introduction-023.png)

## Beethoven

124 images based on Beethoven string quartet N° 4, using Bravura and Leipzig musical fonts.

They all share the same dimensions: 2480 x 3508 pixels.

No train / val split has been decided yet.

### Classes

Here is the histogram of the class instances found in this corpus:

| Class                                  |  Train |    Val |
| :---                                   |   ---: |   ---: |
| accidentalFlat                         |   1064 |      0 |
| accidentalNatural                      |   2246 |      0 |
| accidentalSharp                        |    962 |      0 |
| articStaccato                          |   9624 |      0 |
| barlineDouble                          |      4 |      0 |
| barlineHeavy                           |     12 |      0 |
| barlineRepeatBoth                      |     18 |      0 |
| barlineRepeatEnd                       |     24 |      0 |
| barlineRepeatStart                     |      4 |      0 |
| barlineSingle                          |   2292 |      0 |
| beam                                   |   7116 |      0 |
| cClef                                  |    404 |      0 |
| charFullStop                           |    712 |      0 |
| digit0                                 |     64 |      0 |
| digit1                                 |    262 |      0 |
| digit2                                 |     94 |      0 |
| digit3                                 |    454 |      0 |
| digit4                                 |     72 |      0 |
| digit5                                 |     58 |      0 |
| digit6                                 |     90 |      0 |
| digit7                                 |    106 |      0 |
| digit8                                 |     82 |      0 |
| digit9                                 |     84 |      0 |
| dirDash                                |    846 |      0 |
| dot                                    |    990 |      0 |
| dynam-f                                |    412 |      0 |
| dynam-p                                |    660 |      0 |
| dynamicFF                              |     78 |      0 |
| dynamicFortePiano                      |     68 |      0 |
| dynamicPP                              |    262 |      0 |
| dynamicSforzando1                      |   1176 |      0 |
| dynamicSforzandoPiano                  |    116 |      0 |
| fClef                                  |    404 |      0 |
| fermataAbove                           |    112 |      0 |
| flag16thUp                             |     16 |      0 |
| flag8thDown                            |    684 |      0 |
| flag8thUp                              |    626 |      0 |
| gClef                                  |    808 |      0 |
| grpSymBracket                          |    404 |      0 |
| hairpinCrescendo                       |     10 |      0 |
| hairpinDiminuendo                      |     14 |      0 |
| keyFlat                                |   3776 |      0 |
| ledgerLine                             |   8530 |      0 |
| letter_a                               |      4 |      0 |
| letter_c                               |   1248 |      0 |
| letter_cA                              |      6 |      0 |
| letter_cP                              |      2 |      0 |
| letter_d                               |     66 |      0 |
| letter_e                               |    630 |      0 |
| letter_g                               |      6 |      0 |
| letter_i                               |     74 |      0 |
| letter_l                               |     12 |      0 |
| letter_m                               |     68 |      0 |
| letter_o                               |     12 |      0 |
| letter_p                               |      4 |      0 |
| letter_r                               |    634 |      0 |
| letter_s                               |    628 |      0 |
| letter_t                               |      2 |      0 |
| letter_z                               |      8 |      0 |
| noteheadBlack                          |  29366 |      0 |
| noteheadHalf                           |   1374 |      0 |
| noteheadWhole                          |    182 |      0 |
| octaveUp                               |      2 |      0 |
| ornamentTrill                          |      2 |      0 |
| ornamentTurn                           |     20 |      0 |
| rest16th                               |     80 |      0 |
| rest8th                                |   1372 |      0 |
| restHalf                               |    420 |      0 |
| restQuarter                            |   2942 |      0 |
| restWhole                              |    836 |      0 |
| slur                                   |   3226 |      0 |
| staffLine                              |   8080 |      0 |
| stem                                   |  29928 |      0 |
| system                                 |    404 |      0 |
| systemBoundingBox                      |    404 |      0 |
| tie                                    |    940 |      0 |
| timeSigCut                             |     32 |      0 |
| trillSig                               |     84 |      0 |
| voltaBracket                           |     20 |      0 |
| word                                   |   1124 |      0 |

### Remarks on this dataset

This is based on the preparation work only.

This small dataset is limited to a single opus, hence it may lack the diversity
we would need for an efficient training.

A manual review led to no detected anomaly, especially none of the problems encountered with DoReMi.
Hairpins boxes are OK, accidentals are not mixed with keys, beams are labelled individually, ...

The difference with DeepScores is the handling of dynamics:
- DeepScores handles the letters in say "sfp" as 3 dynamic symbols: "s", "f" and "p"
- Beethoven handles the symbol "sfp" as a whole: a `dynamicSforzandoPiano` symbol.

We will have to make a decision between these two approaches before training a model on these datasets.

## Conclusions

- The DoReMi dataset is discarded because we consider its defects as prohibitive for our training task.
- The DeepScores dataset has been modified to deliver compound dynamics instead of its original 1-letter dynamics and can then be used.
- The Beethoven dataset can be used to complement the DeepScores dataset.

## Upload to drive

When we are through with the preparation of these datasets, it's time to switch to the Yolo training.

To make this material available for a notebook on Google Colab, it is manually copied
from my PC to a target location on a Google drive.