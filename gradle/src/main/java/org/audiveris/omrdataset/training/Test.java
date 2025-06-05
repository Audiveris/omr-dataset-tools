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

import org.audiveris.omr.util.StopWatch;
import org.audiveris.omrdataset.Main;
import org.audiveris.omrdataset.api.Context;
import org.audiveris.omrdataset.api.OmrEvaluation;
import org.audiveris.omrdataset.api.OmrShape;
import static org.audiveris.omrdataset.api.OmrShapes.OMR_SHAPES;
import org.audiveris.omrdataset.api.Patch;
import static org.audiveris.omrdataset.api.Patch.UPatch.parseUPatch;
import org.audiveris.omrdataset.extraction.SourceInfo;
import org.audiveris.omrdataset.extraction.SourceInfo.UArchiveId;
import static org.audiveris.omrdataset.training.App.MISTAKES_FOLDER_NAME;
import static org.audiveris.omrdataset.training.App.MODEL_PATH;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipInputStream;

/**
 * Class {@code Test}
 *
 * @author Hervé Bitteur
 */
public class Test
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(Test.class);

    public static final double MIN_GRADE = 0.01;
    //~ Instance fields ----------------------------------------------------------------------------

    //~ Constructors -------------------------------------------------------------------------------
    //~ Methods ------------------------------------------------------------------------------------
    public void process (Path testPath)
            throws Exception
    {
        StopWatch watch = new StopWatch("Test");

        if (!Files.exists(MODEL_PATH)) {
            logger.warn("Could not find model at {}", MODEL_PATH);
            return;
        }

        watch.start("Restoring model");
        ///ComputationGraph model = ModelSerializer.restoreComputationGraph(MODEL_PATH.toFile(), false);
        MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(MODEL_PATH.toFile(),
                                                                           false);
        logger.info("Model restored from {}", MODEL_PATH.toAbsolutePath());

        watch.start("Testing");
        logger.info("Getting test dataset from {} ...", testPath);
        final ZipInputStream zis = new ZipInputStream(Files.newInputStream(testPath));
        zis.getNextEntry();

        UArchiveId uArchiveId = SourceInfo.lookupArchiveId(testPath);
        Path fileName = testPath.getFileName();
        Path mistakesFolder = SourceInfo.getPath(uArchiveId).resolve(MISTAKES_FOLDER_NAME);
        Files.createDirectories(mistakesFolder);
//
//        Path mistakesPath = mistakesFolder.resolve(fileName);
//        final ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(mistakesPath));
//        zos.putNextEntry(new ZipEntry(FileUtil.getNameSansExtension(mistakesPath)));

        try (final BufferedReader br = new BufferedReader(new InputStreamReader(zis, UTF_8)); //             final PrintWriter pw = new PrintWriter(
                //                     new BufferedWriter(new OutputStreamWriter(zos, UTF_8)))
                ) {
            final Context context = Main.context;
            String line;

            while ((line = br.readLine()) != null) {
                final Patch.UPatch uPatch = parseUPatch(line, context);
                final INDArray pixels = uPatch.pixelsArray();

                long start = System.currentTimeMillis();
                ///final INDArray preds = model.outputSingle(pixels);
                final INDArray preds = model.output(pixels);
                long stop = System.currentTimeMillis();
                System.out.println("model.outputSingle in " + (stop - start) + " ms");

                // Extract and sort evaluations
                final List<OmrEvaluation> evalList = new ArrayList<>();

                for (int iShape = 0; iShape < preds.length(); iShape++) {
                    OmrShape omrShape = OMR_SHAPES[iShape];
                    double grade = preds.getDouble(iShape);
                    evalList.add(new OmrEvaluation(omrShape, grade));
                }

                Collections.sort(evalList);

                final String topPreds = topPredictions(evalList);
                System.out.println(
                        String.format("%-15s %s ->%s", uPatch.uSymbolId, uPatch.label, topPreds));
//
//                final OmrShape inferred = evalList.get(0).omrShape;
//                if (inferred != uPatch.shape) {
//                    WrongPatch wPatch = new WrongPatch(inferred, uPatch);
//                    pw.println(wPatch.toCsv());
//                }
            }
        }

        watch.print();
    }

    //----------------//
    // topPredictions //
    //----------------//
    private String topPredictions (List<OmrEvaluation> evalList)
    {
        StringBuilder sb = new StringBuilder();

        for (OmrEvaluation ev : evalList) {
            if (ev.grade < 0.01) {
                break;
            }

            sb.append(String.format(" %.2f:", ev.grade)).append(ev.omrShape);
        }

        return sb.toString();
    }
    //~ Inner Classes ------------------------------------------------------------------------------
//
//    //----------//
//    // getShape //
//    //----------//
//    /**
//     * Report the shape name indicated in the labels vector.
//     *
//     * @param labels the labels vector (1.0 for a shape, 0.0 for the others)
//     * @return the shape name
//     */
//    private OmrShape getShape (INDArray labels)
//    {
//        for (int c = 0; c < NUM_CLASSES; c++) {
//            double val = labels.getDouble(c);
//
//            if (val != 0) {
//                return OMR_SHAPES[c];
//            }
//        }
//
//        return null;
//    }

//    public void process (Path testPath)
//            throws Exception
//    {
//        StopWatch watch = new StopWatch("Test");
//
//        if (!Files.exists(MODEL_PATH)) {
//            logger.warn("Could not find model at {}", MODEL_PATH);
//            return;
//        }
//
//        watch.start("Restoring model");
//        ComputationGraph model = ModelSerializer.restoreComputationGraph(MODEL_PATH.toFile(), false);
//        logger.info("Model restored from {}", MODEL_PATH.toAbsolutePath());
//
//        watch.start("Creating iterator");
//        int maxNumBatches = -1;
//        logger.info("maxNumBatches: {}", maxNumBatches);
//
//        RecordReader testRecordReader = new CSVRecordReader(0, ',');
//        RecordReaderDataSetIterator testIter = new RecordReaderDataSetIterator(
//                testRecordReader, BATCH_SIZE, CSV_LABEL, NUM_CLASSES, maxNumBatches);
//
//        final ZipWrapper zinTest = ZipWrapper.open(testPath);
//        final InputStream testIs = zinTest.newInputStream();
//        testRecordReader.initialize(new InputStreamInputSplit(testIs));
//
//        //Instruct the iterator to collect metadata, and store it in the DataSet objects
//        testIter.setCollectMetaData(true);
//        logger.info("Getting test dataset from {} ...", testPath);
//
//        // Test sample per sample
//        while (testIter.hasNext()) {
//            DataSet testData = testIter.next();
//            final INDArray features = testData.getFeatures();
//            final INDArray labels = testData.getLabels();
//            final int rows = features.rows();
//
//            for (int r = 0; r < rows; r++) {
//                INDArray label = labels.getRow(r);
//                OmrShape expected = getShape(label);
//
//                INDArray row = features.getRow(r);
//                INDArray pixels = row.get(interval(0, NUM_PIXELS));
//                pixels.divi(255.0); // Normalize pixels
//                INDArray reshapedPixels = pixels.reshape(1, 1, CONTEXT_HEIGHT, CONTEXT_WIDTH);
//
//                INDArray preds = model.outputSingle(reshapedPixels);
//
//                // Extract and sort evaluations
//                List<OmrEvaluation> evalList = new ArrayList<>();
//
//                for (int iShape = 0; iShape < preds.length(); iShape++) {
//                    OmrShape omrShape = OMR_SHAPES[iShape];
//                    double grade = preds.getDouble(iShape);
//                    evalList.add(new OmrEvaluation(omrShape, grade));
//                }
//
//                Collections.sort(evalList);
//
//                // BEWARE: label column has been "removed" by testData.getFeatures()
//                INDArray meta = row.get(interval(CSV_LABEL, CSV_INTERLINE));
//                USymbolId uSymbolId = SourceInfo.parseSymbolId(meta);
//                String topPreds = topPredictions(evalList);
//                System.out.println(String.format("%-15s %s ->%s", uSymbolId, expected, topPreds));
//            }
//        }
//
//        testIs.close();
//        zinTest.close();
//
//        watch.print();
//    }
}
