//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         T r a i n i n g                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
import org.nd4j.linalg.lossfunctions.LossFunctions;
import static org.omrdataset.App.*;
import org.omrdataset.util.Norms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;

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

    //~ Methods ------------------------------------------------------------------------------------
    public void process ()
            throws Exception
    {
        int nChannels = 1; // Number of input channels
        int batchSize = 64; // Batch size
        int nEpochs = 3; // Number of training epochs
        int iterations = 10; // Number of training iterations
        int seed = 123; //

        // Pixel norms
        final Norms pixelNorms = Norms.load(DATA_PATH, PIXELS_NORMS);

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
        testRecordReader.initialize(new FileSplit(CSV_PATH.toFile()));

        DataSetIterator testIter = new RecordReaderDataSetIterator(
                testRecordReader,
                batchSize,
                labelIndex,
                numClasses,
                -1);
        DataSetPreProcessor preProcessor = new MyPreProcessor(pixelNorms);
        trainIter.setPreProcessor(preProcessor);
        testIter.setPreProcessor(preProcessor);

        //
        //        if (false) {
        //            // Debugging
        //            DataSet ds = trainIter.next();
        //            logger.info("ds:\n{}", ds);
        //            logger.info("numExamples:{}", ds.numExamples());
        //
        //            INDArray labels = ds.getLabels();
        //            logger.info("labels rows:{} cols:{}", labels.rows(), labels.columns());
        //            logger.info("labels:\n{}", labels);
        //
        //            INDArray features = ds.getFeatures();
        //            int rows = features.rows();
        //            int cols = features.columns();
        //            logger.info("features rows:{} cols:{}", rows, cols);
        //            logger.info("features:\n{}", features);
        //
        //            INDArray r0 = features.getRow(0);
        //            logger.info("r0:\n{}", r0);
        //            logger.info("r0 rows:{} cols:{}", r0.rows(), r0.columns());
        //
        //            for (int i = 0; i < r0.columns(); i++) {
        //                logger.info("i:{} val:{}", i, r0.getColumn(i));
        //            }
        //
        //            trainIter.reset();
        //        }
        //
        //        ///System.exit(0);
        //
        MultiLayerNetwork model = null;

        if (Files.exists(MODEL_PATH)) {
            model = ModelSerializer.restoreMultiLayerNetwork(MODEL_PATH.toFile(), false);
            logger.info("Model restored from {}", MODEL_PATH.toAbsolutePath());
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
            ModelSerializer.writeModel(model, MODEL_PATH.toFile(), false);
            logger.info("Model stored as {}", MODEL_PATH.toAbsolutePath());
        } finally {
            // Stop monitoring
            if (uiServer != null) {
                uiServer.stop();
            }
        }

        logger.info("****************Example finished********************");
    }

    /**
     * Direct entry point.
     *
     * @param args not used
     * @throws Exception
     */
    public static void main (String[] args)
            throws Exception
    {
        new Training().process();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    /**
     * Normalize pixel data on the fly.
     */
    private static class MyPreProcessor
            implements DataSetPreProcessor
    {
        //~ Instance fields ------------------------------------------------------------------------

        final double mean;

        final double std;

        //~ Constructors ---------------------------------------------------------------------------
        public MyPreProcessor (Norms norms)
        {
            mean = norms.getMean(0);
            std = norms.getStd(0);
            logger.info(String.format("Pixel pre-processor mean:%.2f std:%.2f", mean, std));
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
}
