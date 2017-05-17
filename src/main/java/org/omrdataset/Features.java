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
package org.omrdataset;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import static org.omrdataset.App.*;
import org.omrdataset.PageAnnotations.PageInfo;
import org.omrdataset.util.FileUtil;
import org.omrdataset.util.Norms;
import org.omrdataset.util.Population;

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

    private static final Logger logger = LoggerFactory.getLogger(Features.class);

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Process the available pairs (image + annotations) to extract the corresponding
     * symbols features, later used to train the classifier.
     *
     * @throws Exception
     */
    public void process ()
            throws Exception
    {
        try {
            // Output features file
            final OutputStream os = new FileOutputStream(CSV_PATH.toFile());
            final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            final PrintWriter out = new PrintWriter(bw);

            // Populations for norms computation
            Population pixelPop = new Population(); // For pixels
            Map<OmrShape, Population> widthPops = buildPopulationMap(); // For symbols widths
            Map<OmrShape, Population> heightPops = buildPopulationMap(); // For symbols heights

            // Scan the IMAGES_PATH folder (and its sub-folders)
            Files.walkFileTree(
                    IMAGES_PATH,
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

                            PageAnnotations annotations = PageAnnotations.unmarshal(path);
                            logger.info("{}", annotations);

                            if (annotations == null) {
                                logger.warn("Skipping {} with no annotations", path);

                                return FileVisitResult.CONTINUE;
                            }

                            // Rewrite annotations?
                            // annotations.marshall(
                            //         CONTROL_IMAGES_PATH.resolve(path.getFileName()));
                            //
                            PageInfo pageInfo = annotations.getPageInfo();

                            if (pageInfo == null) {
                                logger.warn("No Page information found");

                                return FileVisitResult.CONTINUE;
                            }

                            // Related image file
                            String uriStr = pageInfo.imageFileName;

                            if (uriStr == null) {
                                logger.warn("No image link found");

                                return FileVisitResult.CONTINUE;
                            }

                            // Make sure we can access the related image
                            URI uri = new URI(uriStr).normalize();
                            boolean isAbsolute = uri.isAbsolute();
                            logger.debug("uri={} isAbsolute={}", uri, isAbsolute);

                            Path imgPath = (isAbsolute) ? Paths.get(uri)
                                    : path.resolveSibling(Paths.get(uri.toString()));

                            logger.debug("imgPath={}", imgPath);

                            if (!Files.exists(imgPath)) {
                                logger.warn("Could not find image {}", uri);

                                return FileVisitResult.CONTINUE;
                            }

                            BufferedImage img = isAbsolute ? ImageIO.read(uri.toURL())
                                    : ImageIO.read(imgPath.toFile());
                            logger.info("Image {}", imgPath.toAbsolutePath());

                            if (img.getType() != BufferedImage.TYPE_BYTE_GRAY) {
                                logger.warn("Wrong image type={}", img.getType());

                                return FileVisitResult.CONTINUE;
                            }

                            // Augment annotations with None symbols
                            int nb = (int) Math.rint(
                                    NONE_RATIO * annotations.getSymbols().size());
                            logger.info("Creating {} None symbols", nb);
                            annotations.getSymbols().addAll(
                                    new NonesBuilder(annotations).insertNones(nb));
                            Collections.shuffle(annotations.getSymbols());

                            // Extract features for all symbols (valid or not)
                            PageProcessor processor = new PageProcessor(
                                    img,
                                    annotations,
                                    true, // leaves
                                    pixelPop,
                                    widthPops,
                                    heightPops);
                            processor.extractFeatures(out);

                            // Generate page image with valid symbol boxes and None locations
                            String radix = FileUtil.getNameSansExtension(imgPath);
                            Path controlPath = CONTROL_IMAGES_PATH.resolve(
                                    radix + OUTPUT_IMAGES_EXT);
                            logger.info("Generating control image {}", controlPath);
                            processor.drawBoxes(controlPath);
                        } catch (Exception ex) {
                            logger.warn("Exception " + ex, ex);
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
            out.flush();
            out.close();

            // Store norms
            storeShapeNorms(widthPops, WIDTHS_NORMS);
            storeShapeNorms(heightPops, HEIGHTS_NORMS);
            storePixelNorms(pixelPop);
        } catch (Throwable ex) {
            logger.warn("Error loading data from " + IMAGES_PATH + " " + ex, ex);
        }
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
        new Features().process();
    }

    //
    /**
     * Allocate the map of shape populations.
     *
     * @return the map of populations
     */
    private Map<OmrShape, Population> buildPopulationMap ()
    {
        Map<OmrShape, Population> map = new EnumMap<OmrShape, Population>(OmrShape.class);

        for (OmrShape shape : OmrShape.values()) {
            map.put(shape, new Population());
        }

        return map;
    }

    /**
     * Store the norms for all pixels.
     *
     * @param pixelPop population of pixels values
     * @throws IOException
     */
    private void storePixelNorms (Population pixelPop)
            throws IOException
    {
        INDArray pixels = Nd4j.create(
                new double[]{
                    pixelPop.getMeanValue(), pixelPop.getStandardDeviation() + Nd4j.EPS_THRESHOLD
                });
        logger.info("Pixels {}", pixelPop);
        new Norms(pixels).store(DATA_PATH, PIXELS_NORMS);
    }

    /**
     * Store the norms for symbols widths or heights.
     *
     * @param popMap   population of symbols sizes (widths or heights)
     * @param fileName target file name
     * @throws IOException
     */
    private void storeShapeNorms (Map<OmrShape, Population> popMap,
                                  String fileName)
            throws IOException
    {
        final int shapeCount = OmrShape.values().length;
        final INDArray vars = Nd4j.zeros(shapeCount, 2);

        for (Entry<OmrShape, Population> entry : popMap.entrySet()) {
            final int row = entry.getKey().ordinal();
            final Population pop = entry.getValue();
            final double mean = (pop.getCardinality() > 0) ? pop.getMeanValue() : 0;
            final double std = (pop.getCardinality() > 0) ? pop.getStandardDeviation() : 0;
            vars.putScalar(new int[]{row, 0}, mean);
            vars.putScalar(new int[]{row, 1}, std + Nd4j.EPS_THRESHOLD);
        }

        logger.debug("{} pop: {}", fileName, vars);
        new Norms(vars).store(DATA_PATH, fileName);
    }
}
