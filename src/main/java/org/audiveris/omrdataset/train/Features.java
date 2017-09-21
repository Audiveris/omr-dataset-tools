//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         F e a t u r e s                                        //
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

import org.audiveris.omrdataset.Main;
import org.audiveris.omrdataset.api.OmrShape;
import org.audiveris.omrdataset.api.SheetAnnotations;
import org.audiveris.omrdataset.api.SheetAnnotations.SheetInfo;
import static org.audiveris.omrdataset.classifier.Context.*;
import static org.audiveris.omrdataset.train.App.*;
import static org.audiveris.omrdataset.train.AppPaths.*;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer;
import org.nd4j.linalg.dataset.api.preprocessor.stats.DistributionStats;
import org.nd4j.linalg.factory.Nd4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

/**
 * Class {@code Features} reads input images data (collection of pairs: sheet image and
 * symbol descriptors) and produces the features CSV file meant for NN training.
 * <p>
 * Each page annotations are augmented with artificial None symbols.
 * For visual checking, a page image can be produced with initial image, true symbols boxes and
 * None symbols locations.
 * <p>
 * In the CSV file, there must be one record per symbol, containing the pixels of the sub-image
 * centered on the symbol center, followed by the (index of) symbol name.
 * <p>
 * Beside CSV training file, we retrieve Norm (mean + stdDev) for: <ul>
 * <li>all pixel values whatever the shape
 * <li>symbol width per valid shape
 * <li>symbol height per valid shape
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class Features
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            Features.class);

    private static final int SHAPE_COUNT = OmrShape.values().length;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Cumulate values for mean/std of pixels. */
    private final DistributionStats.Builder pixels = new DistributionStats.Builder();

    /** Cumulate values for mean/std of width and height per shape. */
    private final Map<OmrShape, DistributionStats.Builder> dimMap = new EnumMap<OmrShape, DistributionStats.Builder>(
            OmrShape.class);

    private PrintWriter features; // For features.csv

    private PrintWriter journal; // For metadata

    private PrintWriter sheets; // For sheets table

    private int sheetId; // Sheet id (counted from 1)

    private final int[] rowBuffer = new int[]{0}; // For row index in features.csv file

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Direct entry point.
     *
     * @param args not used
     * @throws IOException in case of IO problem
     */
    public static void main (String[] args)
            throws IOException
    {
        new Features().process();
    }

    /**
     * Process the available pairs (image + annotations) to extract the corresponding
     * symbols features, later used to train the classifier.
     *
     * @throws IOException in case of IO problem
     */
    public void process ()
            throws IOException
    {
        if (Main.cli.arguments.isEmpty()) {
            logger.warn("No input specified for features. Exiting.");

            return;
        }

        try {
            features = getPrintWriter(FEATURES_PATH); // Output features file
            journal = getPrintWriter(JOURNAL_PATH); // Output journal file
            sheets = getPrintWriter(SHEETS_PATH); // Output sheets file

            // Header comment line for each CSV file
            final int numPixels = CONTEXT_WIDTH * CONTEXT_HEIGHT;
            features.println("# " + numPixels + " pixels, shapeId");
            journal.println("# row, sheetId, symbolId, interline, x, y, w, h, shapeId");
            sheets.println("# sheetId, sheetPath");

            // Scan the provided inputs (which can be simple files or folders)
            for (Path path : Main.cli.arguments) {
                if (!Files.exists(path)) {
                    logger.warn("Could not find {}", path);

                    continue;
                }

                if (Files.isDirectory(path)) {
                    // Process folder recusrsively
                    processFolder(path);
                } else {
                    // We look for "foo.xml" Annotations files
                    final String fileName = path.getFileName().toString();

                    if (fileName.endsWith(INFO_EXT)) {
                        processFile(path);
                    }
                }
            }

            features.flush();
            features.close();
            journal.flush();
            journal.close();
            sheets.flush();
            sheets.close();

            // Store dim stats per shape
            storeDims();

            // pixels.store(PIXELS_PATH);
            DistributionStats stats = pixels.build();
            NormalizerStandardize normalizer = new NormalizerStandardize(
                    stats.getMean(),
                    stats.getStd());
            NormalizerSerializer.getDefault().write(normalizer, PIXELS_PATH.toFile());
        } catch (Throwable ex) {
            logger.warn("Error loading data", ex);
        }
    }

    /**
     * Remove the ending extension of the provided file name
     *
     * @param name file name such as "foo.ext"
     * @return radix such as "foo"
     */
    private static String sansExtension (String name)
    {
        int i = name.lastIndexOf('.');

        if (i >= 0) {
            return name.substring(0, i);
        } else {
            return name;
        }
    }

    private PrintWriter getPrintWriter (Path path)
            throws IOException
    {
        Files.createDirectories(path.getParent());

        final OutputStream os = new FileOutputStream(path.toFile());
        final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

        return new PrintWriter(bw);
    }

    /**
     * Process one annotations file (and its related image file).
     *
     * @param path path to Annotations file
     */
    private void processFile (Path path)
    {
        try {
            logger.info("Processing file {}", path);

            SheetAnnotations annotations = SheetAnnotations.unmarshal(path);
            logger.info("{}", annotations);

            if (annotations == null) {
                logger.warn("Skipping {} with no annotations", path);

                return;
            }

            // Rewrite annotations?
            // annotations.marshall(
            //         CONTROL_IMAGES_PATH.resolve(path.getFileName()));
            //
            SheetInfo sheetInfo = annotations.getSheetInfo();

            if (sheetInfo == null) {
                logger.warn("No Page information found");

                return;
            }

            // Related image file
            String uriStr = sheetInfo.imageFileName;

            if (uriStr == null) {
                logger.warn("No image link found");

                return;
            }

            // Make sure we can access the related image
            URI uri = new URI(uriStr).normalize();
            boolean isAbsolute = uri.isAbsolute();
            logger.info("uri={} isAbsolute={}", uri, isAbsolute);

            Path imgPath = (isAbsolute) ? Paths.get(uri)
                    : path.resolveSibling(Paths.get(uri.toString()));

            logger.debug("imgPath={}", imgPath);

            if (!Files.exists(imgPath)) {
                logger.warn("Could not find image {}", uri);

                return;
            }

            BufferedImage img = isAbsolute ? ImageIO.read(uri.toURL())
                    : ImageIO.read(imgPath.toFile());
            logger.info("Image {}", imgPath.toAbsolutePath());

            if (img.getType() != BufferedImage.TYPE_BYTE_GRAY) {
                logger.warn("Wrong image type={}", img.getType());

                return;
            }

            if (Main.cli.nones) {
                // Augment annotations with none symbols
                int nb = (int) Math.rint(NONE_RATIO * annotations.getSymbols().size());
                logger.info("Creating {} none symbols", nb);
                annotations.getSymbols().addAll(new NonesBuilder(annotations).insertNones(nb));
            }

            // It's important for training to shuffle examples
            // Here we can shuffle symbols within the same sheet only...
            Collections.shuffle(annotations.getSymbols());

            // Append to sheets table
            sheets.print(++sheetId);
            sheets.print(",");
            sheets.print(imgPath);
            sheets.println();

            // Extract features for all symbols (valid or not)
            SheetProcessor processor = new SheetProcessor(
                    sheetId,
                    img,
                    annotations,
                    true, // leaves
                    pixels,
                    dimMap);
            processor.extractFeatures(features, journal, rowBuffer);

            if (Main.cli.controls) {
                // Generate page image with valid symbol boxes and None locations
                String radix = sansExtension(imgPath.getFileName().toString());
                Path controlPath = CONTROL_IMAGES_PATH.resolve(radix + OUTPUT_IMAGES_EXT);
                logger.info("Generating control image {}", controlPath);
                processor.drawBoxes(controlPath);
            }
        } catch (Throwable ex) {
            logger.warn("Error processing file {}", path, ex);
        }
    }

    /**
     * Process a folder (all its files, and recursively all its sub-folders).
     *
     * @param folder the folder to process
     */
    private void processFolder (Path folder)
    {
        try {
            logger.info("Processing folder {} ...", folder);
            Files.walkFileTree(
                    folder,
                    new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile (Path path,
                                                  BasicFileAttributes attrs)
                        throws IOException
                {
                    final String fileName = path.getFileName().toString();

                    // We look for "foo.xml" Annotations files
                    if (fileName.endsWith(INFO_EXT)) {
                        try {
                            logger.info("XML file {}", path);
                            processFile(path);
                        } catch (Exception ex) {
                            logger.warn("Exception " + ex, ex);
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Throwable ex) {
            logger.warn("Error processing folder {}", folder, ex);
        }
    }

    /**
     * Store the mean/std values for width/height of each (populated) omr shape.
     *
     * @throws IOException on IO error
     */
    private void storeDims ()
            throws IOException
    {
        INDArray dimStats = Nd4j.zeros(4, SHAPE_COUNT);
        logger.info("Symbol dimensions for populated shapes:");

        for (Entry<OmrShape, DistributionStats.Builder> entry : dimMap.entrySet()) {
            OmrShape shape = entry.getKey();
            DistributionStats.Builder builder = entry.getValue();
            DistributionStats stats = builder.build();
            INDArray means = stats.getMean();
            INDArray stds = stats.getStd();
            int index = shape.ordinal();
            double meanWidth = means.getDouble(0);
            double stdWidth = stds.getDouble(0);
            double meanHeight = means.getDouble(1);
            double stdHeight = stds.getDouble(1);
            dimStats.putScalar(new int[]{0, index}, meanWidth);
            dimStats.putScalar(new int[]{1, index}, stdWidth);
            dimStats.putScalar(new int[]{2, index}, meanHeight);
            dimStats.putScalar(new int[]{3, index}, stdHeight);
            logger.info(
                    String.format(
                            "%27s width{mean:%.2f std:%.2f} height{mean:%.2f std:%.2f}",
                            shape,
                            meanWidth,
                            stdWidth,
                            meanHeight,
                            stdHeight));
        }

        Nd4j.saveBinary(dimStats, DIMS_PATH.toFile());
    }
}
