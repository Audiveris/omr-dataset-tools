# omr-dataset

## Vision

Inspired by the famous example of [MNIST][1] public database (60000 labelled images of hand-written digits), we acknowledge the need for a well-known and representative data set to help the development of applications in the specific domain of Optical Music Recognition.

## Purpose

+ OMR samples for the training and testing of symbol classifiers
+ Ground-truth material for the evaluation or comparison of OMR engines

## Organization

Ultimately, once data structuring and content are sufficiently validated, we think this reference should preferably be hosted by the International Music Score Library Project ([IMSLP][2]). 

Meanwhile, the purpose of this `omr-dataset` Github repository is to gather the material used to build preliminary versions of the target reference.

## Usage

This project is handled by gradle tool, and can be driven from an IDE or the command line.

\[NOTA: Noise addition tools are not yet included in this gradle build\]

From command line, for a full rebuild, use:

```
./gradlew clean build
```

To just display usage rules, use:

```
./gradlew run --args="-help"
```   

this will display:  

```
Syntax:
   [OPTIONS] -- [INPUT_ANNOTATION_FILES and/or INPUT_FOLDERS]

@file:
 Content to be expanded in line

Options:
 -help                     : Display general help then stop
 -context [HEAD | GENERAL] : (mandatory) Specify which context kind to use
 -filter                   : Step 1: Load and filter symbols
 -nones                    : Step 2: Generate none symbols
                                (effective for control & features)
 -features                 : Step 3: Generate sheet .csv.zip files
 -tally                    : Step 4: Dispatch features by shape
 -shapeGrids               : Step 4.b (optional): Build patch grids by shape
 -grids Path[]             : Step 4.c (optional): Build patch grids on selected inputs
 -bins                     : Step 5: Split shape tally files into bins
 -shuffle                  : Step 6: Shuffle each bin in memory
 -train int[]              : Step 7: Train model on selected bins
 -testPath XXX.csv.zip     : Evaluate model on the provided features file
 -parallel                 : (recommended) Use parallel processing
                                (effective on steps 1 to 5)
 -names                    : (optional) Print context shapes names with their index
 -histo                    : (optional) Print shape histogram per sheet
 -binhisto int[]           : (optional) Print shape histogram of selected bins
 -control                  : (optional) Generate control image of each sheet
 -patches                  : (optional) Generate patch images
 -inspect int[]            : (optional) Inspect a bin for a range of iterations
 -output <folder>          : (optional) Define output directory
                                (defaults to "data/output")
 -model <.zip file>        : (optional) Define path to model
                                (defaults to "<output>/training/head-classifier.zip")

Input file extensions:
 .xml: annotations file
```


To filter input and generate proper features for HEAD classifier, using input from `D:\soft\DeepScores\archive-6\xml_annotations` folder, use:
```
./gradlew run --args="-parallel -context HEAD -filter -nones -features -tally -bins -shuffle -- D:\soft\DeepScores\archive-6\xml_annotations"
```

To launch training on those generated features, use:
```
./gradlew run --args="-context HEAD -train 1-9 -- D:\soft\DeepScores\archive-6\xml_annotations"
```

The training results in the creation or update of file `data/output/training/head-classifier.zip`.

To monitor the neural network being trained, simply open a browser on this url: `http://localhost:9000/train/overview`

## Steps

Starting from the original material (sheet images and sheet annotations), the seven steps listed in the help section above are needed to get to the trained model.
The steps don't need to be run all at once, since they build one upon the previous one.

Purpose of this section is to provide more details on each of these steps

### Preliminary work

Folder `images_png` contains RGB images. A new folder `gray_images_png` is created with the initial RGB images converted to gray images.

### Tablatures detection

Purpose of this work is to detect tablatures in the input sheets.
This is done by running Audiveris OMR engine on each input sheet up to its GRID step, with the specific `RunTablatureCheck` addition.
If tablatures are detected in a given `<sheet>` , they result in the creation of a specific `<sheet>.tablatures.xml` file located in `tablatures` folder.

This file provides the bounding box of every detected tablature, to exclude the processing of these specific regions by the following steps.

This detection is performed on MuseScore set only.

### Step#1: filter
This step is run on `xml_annotations` folder of `<sheet>.xml` files and results in `filtered` folder of `<sheet>.filtered.xml`.
- Renaming of small version of shapes.
- Fix inner dots of a repeat symbol as `repeatDot` instead of wrong `augmentationDot`.
- Exclusion of symbols with zero widthh or zero height.
- Exclusion of tablatures areas (if any detected).
- Exclusion of specific areas.
- Exclusion of symbols not fully contained in sheet bounds
- Resolution of overlapping symbols (inner over outer, simple duplication, invalid overlap)
- Exclusion of inner symbols not contained in their outer symbol
- Exclusion of symbols too wide or high with respect to their shape

### Step#2: nones
Creation of artificial "none" symbols at selected random locations within every sheet.

### Step#3: features
Extraction of features for every relevant symbol (typically a head or a none in HEAD context).
This results into `HEAD/features/<sheet>.csv.zip` compressed files, where each line represents the features of one relevant symbol.
The features are the sequence of
1. The pixel values in the patch centered on symbol (patch dimension is defined in HEAD context)
2. The shape index
3. Metadata to trace the sample back to its containing sheet (and archive)

### Step#4: tally
In `HEAD/shapes` folder, this step creates one `<shape>.csv.zip` file per relevant shape (12 shapes for HEAD context)
This gathers all the samples for a given shape.

### Step#5: bins
In `HEAD/bins` folder, this steps creates exactly 10 files named `bin-NN.csv.zip`, where "NN" is the bin id, from 0 to 10.

The step reads the `HEAD/shapes` folder and for each shape dispatches its samples rather equally on the 10 bins.

### Step#6: shuffle
Each of the 10 bin files is shuffled in memory so that its samples appear randomly.

### Step#7: train
This step performs the model training on the selected bins, typically the bins 1 through 9.
The 10th bin being reserved for model testing.

## Development

There is a [wiki][3] for implementation informations, unfortunately it is several years old and not up-to-date.

[1]: http://yann.lecun.com/exdb/mnist/
[2]: http://imslp.org/
[3]: https://github.com/Audiveris/omr-dataset/wiki
