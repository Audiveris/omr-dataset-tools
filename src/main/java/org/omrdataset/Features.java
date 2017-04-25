//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         F e a t u r e s                                        //
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

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import static org.omrdataset.App.*;
import org.omrdataset.util.FileUtil;
import org.omrdataset.util.Norms;
import org.omrdataset.util.Population;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

/**
 * Class {@code Features} reads input images data (collection of pairs: sheet image and
 * symbol descriptors) and produces the CSV file meant for NN training.
 * <p>
 * In the CSV file, there must be one record per symbol, containing the pixels of the sub-image
 * centered on the symbol center, followed by the (index of) symbol name.
 * <p>
 * Beside CSV training file, we need to retrieve Norm (mean + stdDev) for: <ul>
 * <li>all pixel values whatever the shape
 * <li>symbol width per shape
 * <li>symbol height per shape
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
        // Scan the data folder for pairs of files: foo.png and foo.xml
        try {
            final OutputStream os = new FileOutputStream(CSV_PATH.toFile());
            final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            final PrintWriter out = new PrintWriter(bw);

            Population pixelPop = new Population(); // For pixels
            Map<OmrShape, Population> widthPops = buildPopulationMap(); // For symbols widths
            Map<OmrShape, Population> heightPops = buildPopulationMap(); // For symbols heights

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

                    if (fileName.endsWith(IMAGE_EXT)) {
                        BufferedImage img = ImageIO.read(path.toFile());
                        logger.info("Image {}", path.toAbsolutePath());

                        WritableRaster raster = img.getRaster();

                        byte[] bytes = getBytes(img);

                        // Make sure we have the xml counterpart
                        // And unmarshal the xml information
                        String radix = FileUtil.getNameSansExtension(path);
                        String infoName = radix + App.INFO_EXT;
                        Path infoPath = path.resolveSibling(infoName);
                        PageAnnotations pageInfo = PageAnnotations.unmarshal(infoPath);
                        logger.info("{}", pageInfo);

                        PageProcessor processor = new PageProcessor(
                                raster.getWidth(),
                                raster.getHeight(),
                                bytes,
                                pageInfo,
                                pixelPop);

                        try {
                            processor.extractFeatures(out, widthPops, heightPops);
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
            storePixelNorms(pixelPop);
            storeShapeNorms(widthPops, WIDTHS_NORMS);
            storeShapeNorms(heightPops, HEIGHTS_NORMS);
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

    /**
     * Extract (and invert) the bytes array of the provided BufferedImage.
     *
     * @param bi the provided buffered image
     * @return the bytes array
     */
    private byte[] getBytes (BufferedImage bi)
    {
        if (bi.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            logger.warn("Wrong image type={}", bi.getType());
            throw new IllegalArgumentException("Image type != TYPE_BYTE_GRAY");
        }

        WritableRaster raster = bi.getRaster();
        DataBuffer buffer = raster.getDataBuffer();
        DataBufferByte byteBuffer = (DataBufferByte) buffer;
        byte[] bytes = byteBuffer.getData();

        // Invert bytes, so that black=0=background and white=255=foreground
        for (int i = bytes.length - 1; i >= 0; i--) {
            int val = bytes[i] & 0xFF;
            val = FOREGROUND - val;
            bytes[i] = (byte) val;
        }

        return bytes;
    }

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
        logger.info("Pixels pop: {}", pixelPop);
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
