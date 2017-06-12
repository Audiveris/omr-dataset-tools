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
package org.audiveris.omrdataset.train;

import org.audiveris.omrdataset.api.OmrShape;
import org.audiveris.omrdataset.api.OmrShapes;
import org.audiveris.omrdataset.math.Norms;
import org.audiveris.omrdataset.math.Populations;
import static org.audiveris.omrdataset.train.App.*;

import org.datavec.api.records.Record;
import org.datavec.api.records.metadata.RecordMetaData;
import org.datavec.api.records.metadata.RecordMetaDataLine;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.api.writable.Writable;

import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.eval.meta.Prediction;
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
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.util.DataTypeUtil;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

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

    private static final OmrShape[] shapeValues = OmrShape.values();

    static {
        DataTypeUtil.setDTypeForContext(DataBuffer.Type.DOUBLE);
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Needed to point to origin of mistaken samples. */
    private final Journal journal = new Journal();

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Direct entry point.
     *
     * @param args not used
     * @throws Exception in case of problem encountered
     */
    public static void main (String[] args)
            throws Exception
    {
        new Training().process();
    }

    /**
     * Perform the training of the neural network.
     * <p>
     * Before training is launched, if the network model exists on disk it is reloaded, otherwise a
     * brand new one is created.
     *
     * @throws Exception in case of IO problem or interruption
     */
    public void process ()
            throws Exception
    {
        Files.createDirectories(MISTAKES_PATH);

        int nChannels = 1; // Number of input channels
        int batchSize = 64; // Batch size
        int nEpochs = 10; //2; // Number of training epochs
        int iterations = 2; //10; // Number of training iterations
        int seed = 123; //

        // Pixel norms
        final Norms pixelNorms = Populations.load(PIXELS_PATH).toNorms();

        // Get the dataset using the record reader. CSVRecordReader handles loading/parsing
        logger.info("Getting dataset...");

        int labelIndex = CONTEXT_WIDTH * CONTEXT_HEIGHT; // format: all cells then label
        int numLinesToSkip = 1; // Because of header comment line
        String delimiter = ",";

        RecordReader trainRecordReader = new CSVRecordReader(numLinesToSkip, delimiter);
        trainRecordReader.initialize(new FileSplit(FEATURES_PATH.toFile()));

        RecordReaderDataSetIterator trainIter = new RecordReaderDataSetIterator(
                trainRecordReader,
                batchSize,
                labelIndex,
                numClasses,
                -1);
        trainIter.setCollectMetaData(true); //Instruct the iterator to collect metadata, and store it in the DataSet objects

        RecordReader testRecordReader = new CSVRecordReader(numLinesToSkip, delimiter);
        testRecordReader.initialize(new FileSplit(FEATURES_PATH.toFile()));

        RecordReaderDataSetIterator testIter = new RecordReaderDataSetIterator(
                testRecordReader,
                batchSize,
                labelIndex,
                numClasses,
                -1);
        testIter.setCollectMetaData(true); //Instruct the iterator to collect metadata, and store it in the DataSet objects

        // Normalization
        DataSetPreProcessor preProcessor = new MyPreProcessor(pixelNorms);
        trainIter.setPreProcessor(preProcessor);
        testIter.setPreProcessor(preProcessor);

        if (false) {
            System.out.println("\n  +++++ Test Set Examples MetaData +++++");

            while (testIter.hasNext()) {
                DataSet ds = testIter.next();
                List<RecordMetaData> testMetaData = ds.getExampleMetaData(RecordMetaData.class);

                for (RecordMetaData recordMetaData : testMetaData) {
                    System.out.println(recordMetaData.getLocation());
                }
            }

            testIter.reset();
        }

        final MultiLayerNetwork model;

        if (Files.exists(MODEL_PATH)) {
            model = ModelSerializer.restoreMultiLayerNetwork(MODEL_PATH.toFile(), false);
            logger.info("Model restored from {}", MODEL_PATH.toAbsolutePath());
        } else {
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

            for (int epoch = 1; epoch <= nEpochs; epoch++) {
                Path epochFolder = MISTAKES_PATH.resolve("epoch#" + epoch);
                Files.createDirectories(epochFolder);

                long start = System.currentTimeMillis();
                model.fit(trainIter);

                long stop = System.currentTimeMillis();
                double dur = stop - start;
                logger.info(String.format("*** End epoch#%d, time: %.0f sec", epoch, dur / 1000));

                logger.info("Evaluating model...");

                Evaluation eval = new Evaluation(OmrShapes.NAMES);

                while (testIter.hasNext()) {
                    DataSet ds = testIter.next();
                    List<RecordMetaData> testMetaData = ds.getExampleMetaData(RecordMetaData.class);
                    INDArray output = model.output(ds.getFeatureMatrix(), false);
                    eval.eval(ds.getLabels(), output, testMetaData);
                }

                System.out.println(eval.stats());
                testIter.reset();

                //Get a list of prediction errors, from the Evaluation object
                //Prediction errors like this are only available after calling iterator.setCollectMetaData(true)
                List<Prediction> mistakes = eval.getPredictionErrors();
                logger.info("Epoch#{} Prediction Errors: {}", epoch, mistakes.size());

                //We can also load a subset of the data, to a DataSet object:
                //Here we load the raw data:
                List<RecordMetaData> predictionErrorMetaData = new ArrayList<RecordMetaData>();

                for (Prediction p : mistakes) {
                    predictionErrorMetaData.add(p.getRecordMetaData(RecordMetaData.class));
                }

                List<Record> predictionErrorRawData = testRecordReader.loadFromMetaData(
                        predictionErrorMetaData);

                for (int ie = 0; ie < mistakes.size(); ie++) {
                    Prediction p = mistakes.get(ie);
                    List<Writable> rawData = predictionErrorRawData.get(ie).getRecord();
                    saveMistake(p, rawData, epochFolder);
                }

                // Save model
                ModelSerializer.writeModel(model, MODEL_PATH.toFile(), false);
                logger.info("Model stored as {}", MODEL_PATH.toAbsolutePath());

                // To avoid long useless sessions...
                if (mistakes.isEmpty()) {
                    logger.info("No mistakes left, training stopped.");

                    break;
                }
            }
        } finally {
            // Stop monitoring
            if (uiServer != null) {
                uiServer.stop();
            }
        }

        logger.info("****************Example finished********************");
    }

    /**
     * Save to disk the image for a shape not correctly recognized.
     *
     * @param prediction the (wrong) prediction
     * @param rawData    pixels raw data
     * @param folder     target folder for current epoch
     * @throws Exception
     */
    private void saveMistake (Prediction prediction,
                              List<Writable> rawData,
                              Path folder)
            throws Exception
    {
        RecordMetaDataLine meta = prediction.getRecordMetaData(RecordMetaDataLine.class);
        final int line = meta.getLineNumber();
        final OmrShape predicted = shapeValues[prediction.getPredictedClass()];
        final OmrShape actual = shapeValues[prediction.getActualClass()];
        final Journal.Record record = journal.getRecord(line);
        System.out.println(record + " mistaken for " + predicted);

        // Generate subimage
        double[] pixels = new double[rawData.size()];

        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = rawData.get(i).toDouble();
        }

        INDArray row = Nd4j.create(pixels);
        BufferedImage img = SubImages.buildSubImage(row);

        // Save subimage to disk, with proper naming
        String name = actual + "-" + line + "-" + predicted + OUTPUT_IMAGES_EXT;
        ImageIO.write(img, OUTPUT_IMAGES_FORMAT, folder.resolve(name).toFile());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------------//
    // MyPreProcessor //
    //----------------//
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
