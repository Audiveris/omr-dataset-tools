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
package org.audiveris.omrdataset.training;

import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.ZipWrapper;
import org.audiveris.omrdataset.Main;
import static org.audiveris.omrdataset.training.App.BATCH_SIZE;
import static org.audiveris.omrdataset.training.App.BIN_COUNT;
import static org.audiveris.omrdataset.training.App.MODEL_PATH;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.InputStreamInputSplit;
import org.datavec.api.writable.Writable;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.api.NeuralNetwork;
import org.deeplearning4j.nn.graph.ComputationGraph;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code Training} performs the training of the classifier neural network based
 * on the features extracted from input images, organized in several bin files.
 *
 * @author Hervé Bitteur
 */
public class Training
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Training.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Normalization. */
    private final DataSetPreProcessor preProcessor = new PixelPreProcessor();

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Direct entry point for stand-alone run.
     *
     * @param args not used
     * @throws Exception in case of problem encountered
     */
    public static void main (String[] args)
            throws Exception
    {
        new Training().process(null);
    }

    /**
     * Perform the training of the neural network.
     * <p>
     * Before training is launched, if the network model exists on disk it is reloaded, otherwise a
     * brand new one is created.
     *
     * @param bins selected bin numbers
     * @throws Exception in case of IO problem or interruption
     */
    public void process (List<Integer> bins)
            throws Exception
    {
        // Check bins information
        // Either null: all bins
        // Or some bins, such as 1-9
        if (bins == null) {
            bins = new ArrayList<>();
            for (int i = 1; i <= BIN_COUNT; i++) {
                bins.add(i);
            }
        }

        logger.info("Archive:{} About to train on bins: {}", Main.uArchiveId, bins);

        final StopWatch watch = new StopWatch("Training");
        watch.start("Set iterators");

        // Model
        ///final ComputationGraph model;
        ///final MultiLayerNetwork model;
        final NeuralNetwork model;

        if (Files.exists(MODEL_PATH)) {
            watch.start("Restoring model");
            model = ModelSerializer.restoreComputationGraph(MODEL_PATH.toFile(), true);
            ///model = ModelSerializer.restoreMultiLayerNetwork(MODEL_PATH.toFile(), true);
            logger.info("Model restored from {}", MODEL_PATH.toAbsolutePath());

            // Backup model with time stamp before training
            final LocalDateTime now = LocalDateTime.now();
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
            Path backup = MODEL_PATH.resolveSibling(now.format(formatter) + "-model.zip");
            watch.start("Model backup");
            Files.copy(MODEL_PATH, backup);
            logger.info("Model backup as {}", backup.toAbsolutePath());
        } else {
            watch.start("Building model");
            logger.info("Building model from scratch");
            model = new ResNet18V2(1,
                                   Main.context.getContextWidth(), // 27
                                   Main.context.getContextHeight(), // 21
                                   Main.context.getNumClasses()).create();
//            model = new HeadModel(1,
//                                  Main.context.getContextWidth(),
//                                  Main.context.getContextHeight(),
//                                  Main.context.getNumClasses()).create();
        }

        try {
            if (true) {
                UIServer uiServer = UIServer.getInstance();

                //Configure where the network information (gradients, score vs. time etc) is to be stored. Here: store in memory.
                StatsStorage statsStorage = new InMemoryStatsStorage(); //Alternative: new FileStatsStorage(File), for saving and loading later
                uiServer.attach(statsStorage);

                if (model instanceof MultiLayerNetwork multiLayerNetwork) {
                    multiLayerNetwork.setListeners(new StatsListener(statsStorage),
                                                   new ScoreIterationListener(1));
                } else if (model instanceof ComputationGraph computationGraph) {
                    computationGraph.setListeners(new StatsListener(statsStorage),
                                                  new ScoreIterationListener(1));
                }
            } else {
                if (model instanceof MultiLayerNetwork multiLayerNetwork) {
                    multiLayerNetwork.setListeners(new ScoreIterationListener(1));
                } else if (model instanceof ComputationGraph computationGraph) {
                    computationGraph.setListeners(new ScoreIterationListener(1));
                }
            }

            logger.info("Training model...");
            int nEpochs = 1;

            for (int epoch = 1; epoch <= nEpochs; epoch++) {
                logger.info("*** Starting epoch {} on {} ***", epoch, LocalDateTime.now());
                watch.start("epoch " + epoch + " training");
                long start = System.currentTimeMillis();

                for (int bin : bins) {
                    final Path trainPath = Main.binPath(bin);
                    final ZipWrapper zinTrain = ZipWrapper.open(trainPath);
                    final InputStream is = zinTrain.newInputStream();

                    final RecordReader trainRecordReader = new MyCSVRecordReader();
                    trainRecordReader.initialize(new InputStreamInputSplit(is));
                    RecordReaderDataSetIterator trainIter = new RecordReaderDataSetIterator(
                            trainRecordReader,
                            BATCH_SIZE,
                            Main.context.getCsvLabel(),
                            Main.context.getNumClasses(),
                            -1);
                    trainIter.setCollectMetaData(true);
                    trainIter.setPreProcessor(preProcessor);
                    logger.info("{}", LocalDateTime.now());
                    logger.info("Training from {} ...", trainPath);

                    model.fit(trainIter); // Hours or days on this line ...

                    is.close();
                    zinTrain.close();
                }

                double duration = System.currentTimeMillis() - start;
                logger.info("{}", LocalDateTime.now());
                logger.info(String.format("*** End epoch #%d, duration: %.0f mn",
                                          epoch, duration / 60000));

                // Save model
                ModelSerializer.writeModel((Model) model, MODEL_PATH.toFile(), true);
                logger.info("Model stored as {}", MODEL_PATH.toAbsolutePath());

                // Evaluate model on test set
                watch.start("epoch " + epoch + " evaluation");
                logger.info("Evaluation for epoch {}", epoch);
                evaluate(model, 10);
            }

            logger.info("{}", LocalDateTime.now());
        } finally {
            watch.print();
            UIServer.stopInstance(); // Stop UI monitoring if needed
            logger.info("The end.");
        }
    }
//
//    //----------//
//    // evaluate //
//    //----------//
//    /**
//     * Evaluate the model using a test data set among the bins.
//     * <p>
//     * NOTA: Because data set is hosted in a zip system, reset() can't be used for the iterator.
//     *
//     * @param model model to evaluate
//     * @param bin   bin to use for test (usually the last one: 10)
//     * @throws Exception if anything goes wrong
//     */
//    private void evaluate (ComputationGraph model,
//                           int bin)
//            throws Exception
//    {
//        final RecordReader testRecordReader = new MyCSVRecordReader();
//        final RecordReaderDataSetIterator testIter = new RecordReaderDataSetIterator(
//                testRecordReader,
//                BATCH_SIZE,
//                Main.context.getCsvLabel(),
//                Main.context.getNumClasses(),
//                -1);
//
//        final Path testPath = Main.binPath(bin);
//        final ZipWrapper zinTest = ZipWrapper.open(testPath);
//        final InputStream testIs = zinTest.newInputStream();
//        testRecordReader.initialize(new InputStreamInputSplit(testIs));
//        testIter.setCollectMetaData(true); //Instruct the iterator to collect metadata
//        testIter.setPreProcessor(preProcessor);
//        logger.info("Using test dataset {} ...", zinTest);
//
//        final Evaluation eval = model.evaluate(testIter);
//        eval.setLabelsList(Main.context.getLabelList());
//
//        testIs.close();
//        zinTest.close();
//        logger.info("{}", eval.stats(true));
//    }
//
    //----------//
    // evaluate //
    //----------//

    /**
     * Evaluate the model using a test data set among the bins.
     * <p>
     * NOTA: Because data set is hosted in a zip system, reset() can't be used for the iterator.
     *
     * @param model model to evaluate
     * @param bin   bin to use for test (usually the last one: 10)
     * @throws Exception if anything goes wrong
     */
    ///private void evaluate (MultiLayerNetwork model,
    private void evaluate (NeuralNetwork model,
                           int bin)
            throws Exception
    {
        final RecordReader testRecordReader = new MyCSVRecordReader();
        final RecordReaderDataSetIterator testIter = new RecordReaderDataSetIterator(
                testRecordReader,
                BATCH_SIZE,
                Main.context.getCsvLabel(),
                Main.context.getNumClasses(),
                -1);

        final Path testPath = Main.binPath(bin);
        final ZipWrapper zinTest = ZipWrapper.open(testPath);
        final InputStream testIs = zinTest.newInputStream();
        testRecordReader.initialize(new InputStreamInputSplit(testIs));
        testIter.setCollectMetaData(true); //Instruct the iterator to collect metadata
        testIter.setPreProcessor(preProcessor);
        logger.info("Using test dataset {} ...", zinTest);

        Evaluation eval = null;
        if (model instanceof MultiLayerNetwork multiLayerNetwork) {
            eval = multiLayerNetwork.evaluate(testIter);
        } else if (model instanceof ComputationGraph computationGraph) {
            eval = computationGraph.evaluate(testIter);
        }
        eval.setLabelsList(Main.context.getLabelList());

        testIs.close();
        zinTest.close();
        logger.info("{}", eval.stats(true));
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------------//
    // MyCSVRecordReader //
    //-------------------//
    /**
     * Remove all the CSV columns that lie beyond the label ID, to please DL4J.
     */
    public static class MyCSVRecordReader
            extends CSVRecordReader
    {

        private static final int labelIndex = Main.context.getCsvLabel();

        public MyCSVRecordReader ()
        {
            super(0, ",");
        }

        @Override
        protected List<Writable> parseLine (String line)
        {
            List<Writable> vals = super.parseLine(line);
            return vals.subList(0, labelIndex + 1);
        }
    }
}
