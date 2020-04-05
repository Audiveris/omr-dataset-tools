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

import org.audiveris.omr.util.ZipWrapper;
import static org.audiveris.omrdataset.training.App.BATCH_SIZE;
import static org.audiveris.omrdataset.training.Context.CSV_LABEL;
import static org.audiveris.omrdataset.training.Context.NUM_CLASSES;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.InputStreamInputSplit;
import org.datavec.api.writable.Writable;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omrdataset.api.OmrShapes;
import static org.audiveris.omrdataset.training.App.BINS_PATH;
import static org.audiveris.omrdataset.training.App.BIN_COUNT;
import static org.audiveris.omrdataset.training.App.MODEL_PATH;
import static org.audiveris.omrdataset.training.Context.CONTEXT_HEIGHT;
import static org.audiveris.omrdataset.training.Context.CONTEXT_WIDTH;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.evaluation.classification.Evaluation;

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

    /** Zip wrapper for test bin. */
    private ZipWrapper zinTest;

    /** Input stream from test bin. */
    private InputStream testIs;

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
        logger.info("About to train on bins: {}", bins);

        final StopWatch watch = new StopWatch("Training");
        watch.start("Set iterators");

        // For test, the limited bin is sufficient
        Path zipTestPath = BINS_PATH.resolve(String.format("limited-bin-%02d.zip", 10));
        String newName = FileUtil.getNameSansExtension(zipTestPath) + ".csv";
        Path testPath = zipTestPath.resolveSibling(newName);
        zinTest = ZipWrapper.open(testPath);

        // Model
        final ComputationGraph model;

        if (Files.exists(MODEL_PATH)) {
            watch.start("Restoring model");
            model = ModelSerializer.restoreComputationGraph(MODEL_PATH.toFile(), false);
            logger.info("Model restored from {}", MODEL_PATH.toAbsolutePath());
        } else {
            watch.start("Building model");
            logger.info("Building model from scratch");
            model = new ResNet18V2(1, CONTEXT_WIDTH, CONTEXT_HEIGHT, NUM_CLASSES).create();
        }

        UIServer uiServer = null;

        try {
            if (true) {
                // Prepare monitoring
                //Initialize the user interface backend
                uiServer = UIServer.getInstance();

                //Configure where the network information (gradients, score vs. time etc) is to be stored. Here: store in memory.
                StatsStorage statsStorage = new InMemoryStatsStorage(); //Alternative: new FileStatsStorage(File), for saving and loading later

                //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
                uiServer.attach(statsStorage);

                //Then add the StatsListener to collect this information from the network, as it trains
                model.setListeners(new StatsListener(statsStorage), new ScoreIterationListener(1));
            } else {
                model.setListeners(new ScoreIterationListener(1));
            }

            logger.info("Training model...");
            int nEpochs = 1;

            for (int epoch = 1; epoch <= nEpochs; epoch++) {
                logger.info("Starting epoch {} on {}", epoch, LocalDateTime.now());
                watch.start("epoch " + epoch);

                for (int bin : bins) {
                    final String name = String.format("bin-%02d.csv", bin);
                    final Path trainPath = BINS_PATH.resolve(name);
                    final ZipWrapper zinTrain = ZipWrapper.open(trainPath);
                    final InputStream is = zinTrain.newInputStream();

                    final RecordReader trainRecordReader = new MyCSVRecordReader(0, ',');
                    trainRecordReader.initialize(new InputStreamInputSplit(is));
                    RecordReaderDataSetIterator trainIter = new RecordReaderDataSetIterator(
                            trainRecordReader, BATCH_SIZE, CSV_LABEL, NUM_CLASSES, -1);
                    trainIter.setCollectMetaData(true); //Instruct the iterator to collect metadata, and store it in the DataSet objects (USEFUL???????)
                    trainIter.setPreProcessor(preProcessor);
                    logger.info("{}", LocalDateTime.now());
                    logger.info("Training from (zipped) {} ...", trainPath);

                    long start = System.currentTimeMillis();
                    model.fit(trainIter);

                    logger.info("{}", LocalDateTime.now());
                    double duration = System.currentTimeMillis() - start;
                    logger.info(String.format("*** End bin #%d, duration: %.0f mn",
                                              bin, duration / 60000));

                    // Save model
                    ModelSerializer.writeModel(model, MODEL_PATH.toFile(), true);
                    logger.info("Model stored as {}", MODEL_PATH.toAbsolutePath());

                    // Test model on test set
                    logger.info("Evaluation for bin {}", bin);
                    watch.start("Evaluation " + bin);
                    RecordReaderDataSetIterator testIter = getNewTestIter();
                    Evaluation eval = model.evaluate(testIter);
                    eval.setLabelsList(OmrShapes.NAMES);
                    testIs.close();
                    logger.info("{}", eval.stats(false, true));

                    is.close();
                    zinTrain.close();
                }
            }

            logger.info("{}", LocalDateTime.now());
        } finally {
            // Stop UI monitoring
            if (uiServer != null) {
                uiServer.stop();
            }
            watch.print();
        }

        zinTest.close();
        logger.info("The end.");
    }

    //----------------//
    // getNewTestIter //
    //----------------//
    /**
     * Get a new fresh iterator of test bin.
     * <p>
     * Because it is hosted in a zip system, the reset() can't be used.
     *
     * @return test iterator
     * @throws Exception
     */
    private RecordReaderDataSetIterator getNewTestIter ()
            throws Exception
    {
        testIs = zinTest.newInputStream();

        RecordReader testRecordReader = new MyCSVRecordReader(0, ',');
        testRecordReader.initialize(new InputStreamInputSplit(testIs));
        RecordReaderDataSetIterator testIter = new RecordReaderDataSetIterator(
                testRecordReader, BATCH_SIZE, CSV_LABEL, NUM_CLASSES, -1);
        testIter.setCollectMetaData(true); //Instruct the iterator to collect metadata
        testIter.setPreProcessor(preProcessor);
        logger.info("Getting test dataset from (zipped) {} ...", zinTest);

        return testIter;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------------//
    // MyCSVRecordReader //
    //-------------------//
    /**
     * Remove all the CSV columns that lie beyond the class ID, to please DL4J.
     */
    public static class MyCSVRecordReader
            extends CSVRecordReader
    {

        public MyCSVRecordReader (int numLinesToSkip,
                                  char delimiter)
        {
            super(numLinesToSkip, delimiter);
        }

        @Override
        protected List<Writable> parseLine (String line)
        {
            List<Writable> vals = super.parseLine(line);
            return vals.subList(0, CSV_LABEL + 1);
        }
    }

}
