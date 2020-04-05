//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             T e s t                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import java.nio.file.Files;
import java.nio.file.Path;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omrdataset.api.OmrShape;
import org.audiveris.omrdataset.api.OmrShapes;
import static org.audiveris.omrdataset.training.App.BATCH_SIZE;
import static org.audiveris.omrdataset.training.App.BINS_PATH;
import static org.audiveris.omrdataset.training.App.MODEL_PATH;
import static org.audiveris.omrdataset.training.Context.CSV_LABEL;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code Test}
 *
 * @author Hervé Bitteur
 */
public class Test
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(Test.class);

    private static final int numClasses = OmrShape.unknown.ordinal();
    //~ Instance fields ----------------------------------------------------------------------------

    //~ Constructors -------------------------------------------------------------------------------
    //~ Methods ------------------------------------------------------------------------------------
    public void process ()
            throws Exception
    {
        StopWatch watch = new StopWatch("Test");

        if (!Files.exists(MODEL_PATH)) {
            logger.warn("Could not find model at {}", MODEL_PATH);
            return;
        }

        watch.start("Restoring model");
        ComputationGraph model = ModelSerializer.restoreComputationGraph(MODEL_PATH.toFile(), false);
        logger.info("Model restored from {}", MODEL_PATH.toAbsolutePath());

        watch.start("Creating iterator");
        Path testPath = BINS_PATH.resolve("bin-10.csv");
        RecordReader testRecordReader = new Training.MyCSVRecordReader(0, ',');
        testRecordReader.initialize(new FileSplit(testPath.toFile()));
        int maxNumBatches = 1;
        RecordReaderDataSetIterator testIter = new RecordReaderDataSetIterator(
                testRecordReader, BATCH_SIZE, CSV_LABEL, numClasses, maxNumBatches);
        testIter.setPreProcessor(new PixelPreProcessor());
        //Instruct the iterator to collect metadata, and store it in the DataSet objects
        testIter.setCollectMetaData(true);
        logger.info("Getting test dataset from {} ...", testPath);

        OmrShape[] shapes = OmrShape.values();

        // Test sample per sample
        while (testIter.hasNext()) {
            DataSet testData = testIter.next();
//
//            Evaluation multEval = new Evaluation(numClasses);
//            INDArray[] output = model.output(testData.getFeatures());
//            multEval.eval(testData.getLabels(), output);
//            logger.info(multEval.stats());
//
//            final INDArray features = dataSet.getFeatures();
//            logger.info("features rank:{} shape:{}", features.rank(), features.shape());
//            logger.info("features rows:{} cols:{}", features.rows(), features.columns());
//
//            final INDArray labels = dataSet.getLabels();
//            logger.info("labels rows:{} cols:{}", labels.rows(), labels.columns());
//
//            final int rows = features.rows();
//
//            for (int r = 0; r < rows; r++) {
//                INDArray row = features.getRow(r);
//                INDArray label = labels.getRow(r);
//                OmrShape expected = getShape(label);
//
//                INDArray reshapedRow = row.reshape(1, 1, CONTEXT_HEIGHT, CONTEXT_WIDTH);
//
//                INDArray preds = model.outputSingle(reshapedRow);
//
//                // Extract and sort evaluations
//                List<OmrEvaluation> evalList = new ArrayList<>();
//
//                for (int iShape = 0; iShape < preds.length(); iShape++) {
//                    OmrShape omrShape = shapes[iShape];
//                    double grade = preds.getDouble(iShape);
//                    evalList.add(new OmrEvaluation(omrShape, grade));
//                }
//
//                Collections.sort(evalList);
//
//                logger.info("Expected: {}", expected);
//                for (int guess = 1; guess <= 5; guess++) {
//                    logger.info("   {}: {}", guess, evalList.get(guess - 1));
//                }
//            }
        }
//
//        Evaluation multEval = new Evaluation(numClasses);
//        INDArray output = model.output(testData.getFeatures());
//        multEval.eval(testData.getLabels(), output);
//        logger.info(multEval.stats());

        // Global evaluation
        logger.info("Evaluation on {} batches max...", maxNumBatches);
        watch.start("Evaluation " + maxNumBatches);
        testIter.reset();
        Evaluation eval = model.evaluate(testIter);
        eval.setLabelsList(OmrShapes.NAMES);
        logger.info("eval: {}", eval.stats(false, true));

        watch.print();
    }

    //----------//
    // getShape //
    //----------//
    /**
     * Report the shape name indicated in the labels vector.
     *
     * @param labels the labels vector (1.0 for a shape, 0.0 for the others)
     * @return the shape name
     */
    private OmrShape getShape (INDArray labels)
    {
        for (int c = 0; c < numClasses; c++) {
            double val = labels.getDouble(c);

            if (val != 0) {
                return OmrShape.values()[c];
            }
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
}
