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
    gradle clean build
```

To just display usage rules, use:

```
    gradle run
```   

this will display:  


    Syntax:
       [OPTIONS] -- [INPUT_FILES]

    Options:
     -clean           : Cleans up output
     -controls        : Generates control images
     -features        : Generates .csv and .dat files
     -help            : Displays general help then stops
     -names           : Prints all possible symbol names
     -nones           : Generates none symbols
     -output <folder> : Defines output directory
     -subimages       : Generates subimages
     -training        : Trains classifier on features

    Input file extensions:
     .xml: annotations file

    @file:
     content to be extended in line


To clean up output, use:
```
    gradle run -PcmdLineArgs="-output,data/output,-clean"
```

To generate features, with all options, using input from `data/input-images`, use:
```
    gradle run -PcmdLineArgs="-output,data/output,-features,-nones,-controls,-subimages,--,data/input-images"
```

To launch training on generated features, use:
```
    gradle run -PcmdLineArgs="-output,data/output,-training"
```

Remark: the training task lasts about 15 minutes (on the toy example `data/input-images` folder).
To monitor the neural network being trained, simply open a browser on http://localhost:9000 url.

## Development

See the related [wiki][3] for more details.

[1]: http://yann.lecun.com/exdb/mnist/
[2]: http://imslp.org/
[3]: https://github.com/Audiveris/omr-dataset/wiki
