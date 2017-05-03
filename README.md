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

To run all tasks in a row, use:

```
    gradle run
```

To run just one task (`Features`, `SubImages` or `Training`), specify the desired main class 
(using its fully qualified name), like:

```
    gradle run -PmainClass="org.omrdataset.Features"
```

Remark: the `Training` task lasts about 10 minutes.
To monitor the neural network being trained, simply open a browser on http://localhost:9000 url.

## Development

See the related [wiki][3] for more details.

[1]: http://yann.lecun.com/exdb/mnist/
[2]: http://imslp.org/
[3]: https://github.com/Audiveris/omr-dataset/wiki
