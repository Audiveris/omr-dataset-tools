//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S h e e t P r o c e s s o r                                  //
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
import org.audiveris.omrdataset.api.SheetAnnotations;
import org.audiveris.omrdataset.api.SymbolInfo;
import static org.audiveris.omrdataset.train.App.*;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.stats.DistributionStats;
import org.nd4j.linalg.factory.Nd4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

/**
 * Class {@code SheetProcessor} processes a whole sheet (image + annotations) to extract
 * its features.
 * <p>
 * It can also draw the symbols boxes and the None symbols locations on top of sheet image for
 * visual check.
 *
 * @author Hervé Bitteur
 */
public class SheetProcessor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SheetProcessor.class);

    private static final DecimalFormat decimal = new DecimalFormat();

    static {
        decimal.setGroupingUsed(false);
        decimal.setMaximumFractionDigits(3); // For a maximum of 3 decimals
    }

    //~ Instance fields ----------------------------------------------------------------------------
    private final int sheetId;

    private final BufferedImage initialImg;

    private final SheetAnnotations annotations;

    private final boolean leaves;

    private final DistributionStats.Builder pixels;

    /** width/height gathered per shape. */
    private final Map<OmrShape, DistributionStats.Builder> dimMap;

    /** Image(s) gathered by interline value. */
    private final Map<Integer, BufferedImage> imgMap = new TreeMap<Integer, BufferedImage>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SheetProcessor} object.
     *
     * @param sheetId     sheet id (strictly positive)
     * @param initialImg  the initial image
     * @param annotations sheet annotations
     * @param leaves      true for using leaf symbols
     * @param pixels      pixels population
     * @param dimMap      population of symbol dims per shape
     */
    public SheetProcessor (int sheetId,
                           BufferedImage initialImg,
                           SheetAnnotations annotations,
                           boolean leaves,
                           DistributionStats.Builder pixels,
                           Map<OmrShape, DistributionStats.Builder> dimMap)
    {
        this.sheetId = sheetId;
        this.initialImg = initialImg;
        this.annotations = annotations;
        this.leaves = leaves;
        this.pixels = pixels;
        this.dimMap = dimMap;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Process the sheet (image / annotations) to append the extracted features
     * (the context sub-image for each symbol) to the out stream.
     * <p>
     * Nota: if a sub-image goes beyond image borders, we fill the related external pixels with
     * background value.
     *
     * @param features output to be populated by CSV records
     * @param journal  output to be populated by metadata
     * @param row      1-cell array for input/output of current row in features
     */
    public void extractFeatures (PrintWriter features,
                                 PrintWriter journal,
                                 int[] row)
    {
        // Process each symbol definition in the sheet
        processSymbols(annotations.getSymbols(), features, journal, row);
    }

    /**
     * Build a scaled version of an image.
     *
     * @param img   image to scale
     * @param ratio scaling ratio
     * @return the scaled image
     */
    public static BufferedImage scale (BufferedImage img,
                                       double ratio)
    {
        AffineTransform at = AffineTransform.getScaleInstance(ratio, ratio);
        AffineTransformOp atop = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        BufferedImage scaledImg = new BufferedImage(
                (int) Math.ceil(img.getWidth() * ratio),
                (int) Math.ceil(img.getHeight() * ratio),
                img.getType());

        return atop.filter(img, scaledImg);
    }

    /**
     * Draw symbols boxes and None symbols locations on control image.
     *
     * @param controlPath target path for control image
     * @throws java.io.IOException in case of IO problem
     */
    public void drawBoxes (Path controlPath)
            throws IOException
    {
        BufferedImage ctrl = new BufferedImage(
                initialImg.getWidth(),
                initialImg.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = ctrl.createGraphics();
        g.drawImage(initialImg, null, null);

        drawSymbols(annotations.getSymbols(), g);

        g.dispose();
        Files.createDirectories(controlPath.getParent());
        ImageIO.write(ctrl, OUTPUT_IMAGES_FORMAT, controlPath.toFile());
    }

    /**
     * Draw the boxes for the provided symbols (and recursively their inner symbols)
     *
     * @param symbols the collection of symbols to process
     * @param g       the graphic output
     */
    private void drawSymbols (List<SymbolInfo> symbols,
                              Graphics2D g)
    {
        for (SymbolInfo symbol : symbols) {
            logger.debug("{}", symbol);

            // Inner symbols?
            List<SymbolInfo> innerSymbols = symbol.getInnerSymbols();

            if (!innerSymbols.isEmpty()) {
                logger.debug("+++ Drawing inner symbols of {}", symbol);
                drawSymbols(innerSymbols, g);
                logger.debug("--- End of inner symbols of {}", symbol);
            }

            Rectangle2D box = symbol.getBounds();

            if (symbol.getOmrShape() != OmrShape.none) {
                // Draw outer rectangle, with line stroke of 1 pixel
                Rectangle2D b = new Rectangle2D.Double(
                        box.getX() - 1,
                        box.getY() - 1,
                        box.getWidth() + 1,
                        box.getHeight() + 1);

                if (IgnoredShapes.isIgnored(symbol.getOmrShape())) {
                    g.setColor(Color.GRAY);
                } else {
                    g.setColor(Color.GREEN);
                }

                g.draw(b);
            } else {
                double ratio = (double) INTERLINE / symbol.getInterline();
                int xMargin = (int) Math.rint(NONE_X_MARGIN / ratio);
                int yMargin = (int) Math.rint(NONE_Y_MARGIN / ratio);
                Rectangle b = box.getBounds();
                g.setColor(Color.RED);
                g.drawLine(b.x, b.y - yMargin, b.x, b.y + yMargin);
                g.drawLine(b.x - xMargin, b.y, b.x + xMargin, b.y);
            }
        }
    }

    /**
     * Generate the annotations related to the collection of provided symbols.
     *
     * @param symbols  the symbols to process
     * @param features features file
     * @param journal  journal file
     * @param row      in/out for current row in features
     */
    private void processSymbols (List<SymbolInfo> symbols,
                                 PrintWriter features,
                                 PrintWriter journal,
                                 int[] row)
    {
        for (SymbolInfo symbol : symbols) {
            final OmrShape symbolShape = symbol.getOmrShape();

            if (IgnoredShapes.isIgnored(symbolShape)) {
                continue;
            }

            logger.debug("{}", symbol);

            // Inner symbols?
            if (leaves) {
                List<SymbolInfo> innerSymbols = symbol.getInnerSymbols();

                if (!innerSymbols.isEmpty()) {
                    logger.debug("+++ Processing inner symbols of {}", symbol);
                    processSymbols(innerSymbols, features, journal, row);
                    logger.debug("--- End of inner symbols of {}", symbol);

                    if (!OmrShapes.TIME_COMBOS.contains(symbolShape)) {
                        continue;
                    }
                }
            }

            if (symbolShape != OmrShape.none) {
                if (symbolShape == null) {
                    logger.warn("Null shape {}", symbol);

                    continue;
                }
            }

            Rectangle2D box = symbol.getBounds();
            BufferedImage img = null;

            // Pick up image properly scaled
            final int interline = symbol.getInterline();
            final boolean rescale = interline != INTERLINE;
            final double ratio = (double) INTERLINE / interline;
            img = imgMap.get(interline);

            if (img == null) {
                imgMap.put(interline, img = rescale ? scale(initialImg, ratio) : initialImg);
            }

            // Cumulate symbol width/height in mean/std builder for proper shape
            DistributionStats.Builder whBuilder = dimMap.get(symbolShape);

            if (whBuilder == null) {
                dimMap.put(symbolShape, whBuilder = new DistributionStats.Builder());
            }

            INDArray wh = Nd4j.zeros(2);
            wh.putScalar(0, box.getWidth() * ratio);
            wh.putScalar(1, box.getHeight() * ratio);
            whBuilder.add(wh, null);

            // Symbol center
            double sCenterX = ratio * (box.getX() + (box.getWidth() / 2.0));
            double sCenterY = ratio * (box.getY() + (box.getHeight() / 2.0));

            // Top-left corner of context
            int left = (int) Math.rint(sCenterX - (CONTEXT_WIDTH / 2));
            int top = (int) Math.rint(sCenterY - (CONTEXT_HEIGHT / 2));
            logger.trace("left:{} top:{}", left, top);

            final int sheetWidth = img.getWidth();
            final int sheetHeight = img.getHeight();

            WritableRaster raster = img.getRaster();
            DataBuffer buffer = raster.getDataBuffer();
            DataBufferByte byteBuffer = (DataBufferByte) buffer;
            byte[] bytes = byteBuffer.getData();

            // Extract bytes from sub-image, paying attention to image limits
            // Target format is flattened format, row by row.
            // We also collect pixel values to populate mean/std pixels builder
            final int length = CONTEXT_HEIGHT * CONTEXT_WIDTH;
            double[] pixDoubles = new double[length];
            int index = 0;

            for (int y = 0; y < CONTEXT_HEIGHT; y++) {
                int ay = top + y; // Absolute y

                if ((ay < 0) || (ay >= sheetHeight)) {
                    // Fill with background value
                    for (int x = 0; x < CONTEXT_WIDTH; x++) {
                        features.print(BACKGROUND);
                        features.print(",");
                        pixDoubles[index++] = BACKGROUND;
                    }
                } else {
                    for (int x = 0; x < CONTEXT_WIDTH; x++) {
                        int ax = left + x; // Absolute x
                        int val = ((ax < 0) || (ax >= sheetWidth)) ? BACKGROUND
                                : (255 - (bytes[(ay * sheetWidth) + ax] & 0xff));
                        features.print(val);
                        features.print(",");
                        pixDoubles[index++] = val;
                    }
                }
            }

            // Cumulate pixels for mean/std
            INDArray pixVector = Nd4j.create(pixDoubles, new int[]{length, 1});
            pixels.add(pixVector, null);

            // Add (OMR) shape index
            try {
                features.print(symbolShape.ordinal());
            } catch (Exception ex) {
                logger.error("Missing shape {}", symbol);
                throw new RuntimeException("Missing shape for " + symbol);
            }

            features.println();

            // Add one line to the journal
            row[0]++;
            journal.print(row[0]);
            journal.print(",");
            journal.print(sheetId);
            journal.print(",");
            journal.print(symbol.getId());
            journal.print(",");
            journal.print(symbol.getInterline());
            journal.print(",");

            Rectangle2D bounds = symbol.getBounds();
            journal.print(decimal.format(bounds.getX()));
            journal.print(",");
            journal.print(decimal.format(bounds.getY()));
            journal.print(",");
            journal.print(decimal.format(bounds.getWidth()));
            journal.print(",");
            journal.print(decimal.format(bounds.getHeight()));
            journal.print(",");
            journal.print(symbol.getOmrShape().ordinal());
            journal.println();
        }
    }
}
