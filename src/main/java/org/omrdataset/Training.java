//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         T r a i n i n g                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.omrdataset;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;

import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;

import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import static org.omrdataset.App.CONTEXT_HEIGHT;
import static org.omrdataset.App.CONTEXT_WIDTH;
import static org.omrdataset.App.CSV_PATH;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Class {@code Training} performs the training of the classifier neural network based
 * on the features extracted from input images.
 *
 * @author Hervé Bitteur
 */
public class Training
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Training.class);

    private static final int numClasses = OmrShape.values().length;

    private static final File modelFile = new File("data/cnn-model.zip");

    //~ Methods ------------------------------------------------------------------------------------
    public void process ()
            throws Exception
    {
        int nChannels = 1; // Number of input channels
        int batchSize = 64; // Batch size
        int nEpochs = 2; // Number of training epochs
        int iterations = 10; // Number of training iterations
        int seed = 123; //

        logger.info("Getting norms...");

        final Norms norms = null; ///loadNorms();

        // Get the dataset using the record reader. CSVRecordReader handles loading/parsing
        logger.info("Getting dataset...");

        int labelIndex = CONTEXT_WIDTH * CONTEXT_HEIGHT; // format: all cells then label
        int numLinesToSkip = 0;
        String delimiter = ",";

        RecordReader trainRecordReader = new CSVRecordReader(numLinesToSkip, delimiter);
        trainRecordReader.initialize(new FileSplit(CSV_PATH.toFile()));

        DataSetIterator trainIter = new RecordReaderDataSetIterator(
                trainRecordReader,
                batchSize,
                labelIndex,
                numClasses,
                -1);

        RecordReader testRecordReader = new CSVRecordReader(numLinesToSkip, delimiter);
        testRecordReader.initialize(
                ///new FileSplit(new File("../audiveris-ng/data/train/test.48x24.csv")));
                new FileSplit(CSV_PATH.toFile()));

        DataSetIterator testIter = new RecordReaderDataSetIterator(
                testRecordReader,
                batchSize,
                labelIndex,
                numClasses,
                -1);
        DataSetPreProcessor preProcessor = new MyPreProcessor(norms);
        trainIter.setPreProcessor(preProcessor);
        testIter.setPreProcessor(preProcessor);

        //        if (true) {
        //            printClasses(trainIter, "train");
        //            printClasses(testIter, "test");
        //
        //            ///System.exit(0);
        //        }
        //
        if (true) {
            // Debugging
            DataSet ds = trainIter.next();
            logger.info("ds:\n{}", ds);
            logger.info("numExamples:{}", ds.numExamples());

            INDArray labels = ds.getLabels();
            logger.info("labels rows:{} cols:{}", labels.rows(), labels.columns());
            logger.info("labels:\n{}", labels);

            INDArray features = ds.getFeatures();
            int rows = features.rows();
            int cols = features.columns();
            logger.info("features rows:{} cols:{}", rows, cols);
            logger.info("features:\n{}", features);

            INDArray r0 = features.getRow(0);
            logger.info("r0:\n{}", r0);
            logger.info("r0 rows:{} cols:{}", r0.rows(), r0.columns());

            for (int i = 0; i < r0.columns(); i++) {
                logger.info("i:{} val:{}", i, r0.getColumn(i));
            }

            trainIter.reset();
        }

        ///System.exit(0);
        MultiLayerNetwork model = null;

        if (modelFile.exists()) {
            model = ModelSerializer.restoreMultiLayerNetwork(modelFile, false);
            logger.info("Model restored from {}", modelFile.getAbsolutePath());
        } else {
            /*
             * Construct the neural network
             */
            logger.info("Building model from scratch");

            MultiLayerConfiguration.Builder builder = new NeuralNetConfiguration.Builder() //
                    .seed(seed) //
                    .iterations(iterations) //
                    .regularization(true) //
                    .l2(0.0005) //
                    .learningRate(.002) // HB: was .01 initially
                    //.biasLearningRate(0.02)
                    //.learningRateDecayPolicy(LearningRatePolicy.Inverse).lrPolicyDecayRate(0.001).lrPolicyPower(0.75)
                    .weightInit(WeightInit.XAVIER) //
                    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT) //
                    .updater(Updater.NESTEROVS).momentum(0.9) //
                    .list() //
                    .layer(
                            0,
                            new ConvolutionLayer.Builder(5, 5) //
                                    .name("C0") //
                                    .nIn(nChannels) //
                                    .stride(1, 1) //
                                    .nOut(20) //
                                    .activation(Activation.IDENTITY) //
                                    .build()) //
                    .layer(
                            1,
                            new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX) //
                                    .name("S1") //
                                    .kernelSize(2, 2) //
                                    .stride(2, 2) //
                                    .build()) //
                    .layer(
                            2,
                            new ConvolutionLayer.Builder(5, 5) //
                                    .name("C2") //
                                    .stride(1, 1) //
                                    .nOut(50) //
                                    .activation(Activation.IDENTITY) //
                                    .build()) //
                    .layer(
                            3,
                            new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX) //
                                    .name("S3") //
                                    .kernelSize(2, 2) //
                                    .stride(2, 2) //
                                    .build()) //
                    .layer(
                            4,
                            new DenseLayer.Builder() //
                                    .name("D4") //
                                    .nOut(500) //
                                    .activation(Activation.RELU) //
                                    .build()) //
                    .layer(
                            5,
                            new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD) //
                                    .name("O5") //
                                    .nOut(numClasses) //
                                    .activation(Activation.SOFTMAX) //
                                    .build()) //
                    .setInputType(InputType.convolutionalFlat(CONTEXT_HEIGHT, CONTEXT_WIDTH, 1));

            MultiLayerConfiguration conf = builder.build();
            model = new MultiLayerNetwork(conf);
            model.init();
        }

        /*
         * Exception in thread "main" org.deeplearning4j.exception.DL4JInvalidConfigException:
         * Invalid configuration for layer (idx=4, name=(not named), type=ConvolutionLayer) for
         * height dimension:
         * Invalid input configuration for kernel height. Require 0 < kH <= inHeight + 2*padH; got
         * (kH=5, inHeight=2, padH=0)
         * Input type = InputTypeConvolutional(h=2,w=9,d=50), kernel = [5, 5], strides = [1, 1],
         * padding = [0, 0], layer size (output depth) = 100, convolution mode = Truncate
         */
        // Prepare monitoring
        UIServer uiServer = null;

        try {
            if (true) {
                //Initialize the user interface backend
                uiServer = UIServer.getInstance();

                //Configure where the network information (gradients, score vs. time etc) is to be stored. Here: store in memory.
                StatsStorage statsStorage = new InMemoryStatsStorage(); //Alternative: new FileStatsStorage(File), for saving and loading later

                //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
                uiServer.attach(statsStorage);

                //Then add the StatsListener to collect this information from the network, as it trains
                model.setListeners(new StatsListener(statsStorage), new ScoreIterationListener(10));
            } else {
                model.setListeners(new ScoreIterationListener(10));
            }

            logger.info("Training model...");

            for (int i = 0; i < nEpochs; i++) {
                long start = System.currentTimeMillis();
                model.fit(trainIter);

                long stop = System.currentTimeMillis();
                double dur = stop - start;
                logger.info(
                        String.format("*** Completed epoch %d, time: %.0f sec ***", i, dur / 1000));

                logger.info("Evaluating model...");

                Evaluation eval = new Evaluation(OmrShapes.NAMES);

                while (testIter.hasNext()) {
                    DataSet ds = testIter.next();
                    INDArray output = model.output(ds.getFeatureMatrix(), false);
                    eval.eval(ds.getLabels(), output);
                }

                logger.info(eval.stats());
                testIter.reset();
            }

            // Save model
            ModelSerializer.writeModel(model, modelFile, false);
            logger.info("Model stored as {}", modelFile.getAbsoluteFile());
        } finally {
            // Stop monitoring
            if (uiServer != null) {
                uiServer.stop();
            }
        }

        logger.info("****************Example finished********************");
    }

    private static int argMax (INDArray row)
    {
        final int cols = row.columns();
        int best = 0;

        for (int i = 1; i < cols; i++) {
            if (row.getDouble(i) > row.getDouble(best)) {
                best = i;
            }
        }

        return best;
    }

    private static Norms loadNorms ()
            throws IOException
    {
        File normsFile = new File("../audiveris-ng/data/train/cnn-norms.zip");
        INDArray means = null;
        INDArray stds = null;
        FileInputStream is = new FileInputStream(normsFile);

        try {
            final ZipInputStream zis = new ZipInputStream(is);
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                switch (entry.getName()) {
                case "means.bin": {
                    DataInputStream dis = new DataInputStream(zis);
                    means = Nd4j.read(dis);
                }

                break;

                case "stds.bin": {
                    DataInputStream dis = new DataInputStream(zis);
                    stds = Nd4j.read(dis);
                }

                break;
                }

                zis.closeEntry();
            }

            zis.close();

            if ((means != null) && (stds != null)) {
                return new Norms(means, stds);
            } else {
                return null;
            }
        } finally {
            is.close();
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    private static class MyPreProcessor
            implements DataSetPreProcessor
    {
        //~ Instance fields ------------------------------------------------------------------------

        final double mean;

        final double std;

        //~ Constructors ---------------------------------------------------------------------------
        public MyPreProcessor (Norms norms)
        {
            /// mean:226.95 stdDev:69.19
            // mean:231.53 stdDev:62.85
            // mean:23.47 stdDev:62.85
            mean = 23.47; // norms.means.getDouble(0);
            std = 62.85; // norms.stds.getDouble(0);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void preProcess (org.nd4j.linalg.dataset.api.DataSet toPreProcess)
        {
            INDArray theFeatures = toPreProcess.getFeatures();
            preProcess(theFeatures);
        }

        public void preProcess (INDArray theFeatures)
        {
            theFeatures.subi(mean);
            theFeatures.divi(std);
        }
    }

    private static class Norms
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Features means. */
        final INDArray means;

        /** Features standard deviations. */
        final INDArray stds;

        //~ Constructors ---------------------------------------------------------------------------
        public Norms (INDArray means,
                      INDArray stds)
        {
            this.means = means;
            this.stds = stds;
        }
    }
}
//
//    private static void printClasses (DataSetIterator iter,
//                                      String name)
//    {
//        logger.info("");
//        logger.info("{} classes:", name);
//
//        // Map ShapeIndex -> Number of samples for that shape
//        final Map<Integer, Integer> testMap = new TreeMap<Integer, Integer>();
//        int total = 0;
//        int batches = 0;
//
//        while (iter.hasNext()) {
//            batches++;
//            DataSet ds = iter.next();
//            INDArray labels = ds.getLabels();
//
//            for (int row = 0; row < labels.rows(); row++) {
//                total++;
//
//                int index = argMax(labels.getRow(row));
//                Integer count = testMap.get(index);
//
//                if (count == null) {
//                    testMap.put(index, 1);
//                } else {
//                    testMap.put(index, count + 1);
//                }
//            }
//        }
//
//        for (Entry<Integer, Integer> entry : testMap.entrySet()) {
//            logger.info(
//                    String.format("%18s %3d", OmrShapes.NAMES.get(entry.getKey()), entry.getValue()));
//        }
//
//        logger.info("{} samples: {}", name, total);
//        logger.info("{} batches: {}", name, batches);
//
//        iter.reset();
//    }
//
