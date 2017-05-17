//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    P a g e P r o c e s s o r                                   //
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

import static org.omrdataset.App.*;
import org.omrdataset.util.Population;

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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

/**
 * Class {@code PageProcessor} processes a whole page (image + annotations) to extract
 * its features.
 * <p>
 * It can also draw the symbols boxes and the None symbols locations on top of page image for
 * visual check.
 *
 * @author Hervé Bitteur
 */
public class PageProcessor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PageProcessor.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final BufferedImage initialImg;

    private final PageAnnotations pageInfo;

    private final boolean leaves;

    private final Population pixelPop;

    private final Map<OmrShape, Population> widthPops;

    private final Map<OmrShape, Population> heightPops;

    /** Image(s) gathered by interline value. */
    private final Map<Integer, BufferedImage> imgMap = new TreeMap<Integer, BufferedImage>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PageProcessor} object.
     *
     * @param initialImg the initial image
     * @param pageInfo   page annotations
     * @param leaves     true for using leaf symbols
     * @param pixelPop   (output) pixels population
     * @param widthPops  population of symbol widths per shape
     * @param heightPops population of symbol heights per shape
     */
    public PageProcessor (BufferedImage initialImg,
                          PageAnnotations pageInfo,
                          boolean leaves,
                          Population pixelPop,
                          Map<OmrShape, Population> widthPops,
                          Map<OmrShape, Population> heightPops)
    {
        this.initialImg = initialImg;
        this.pageInfo = pageInfo;
        this.leaves = leaves;
        this.pixelPop = pixelPop;
        this.widthPops = widthPops;
        this.heightPops = heightPops;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Process the page (image / annotations) to append the extracted features
     * (the context sub-image for each symbol) to the out stream.
     * <p>
     * Nota: if a sub-image goes beyond image borders, we fill the related external pixels with
     * background value.
     *
     * @param out output to be populated by CSV records
     * @throws Exception
     */
    public void extractFeatures (PrintWriter out)
            throws Exception
    {
        // Process each symbol definition in the page
        processSymbols(pageInfo.getSymbols(), out);
    }

    private void processSymbols (List<SymbolInfo> symbols,
                                 PrintWriter out)
    {
        for (SymbolInfo symbol : symbols) {
            final OmrShape symbolShape = symbol.getOmrShape();
            logger.debug("{}", symbol);

            // Inner symbols?
            if (leaves) {
                List<SymbolInfo> innerSymbols = symbol.getInnerSymbols();

                if (!innerSymbols.isEmpty()) {
                    logger.debug("+++ Processing inner symbols of {}", symbol);
                    processSymbols(innerSymbols, out);
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

            widthPops.get(symbolShape).includeValue(box.getWidth() * ratio);
            heightPops.get(symbolShape).includeValue(box.getHeight() * ratio);

            // Symbol center
            double sCenterX = ratio * (box.getX() + (box.getWidth() / 2.0));
            double sCenterY = ratio * (box.getY() + (box.getHeight() / 2.0));

            // Top-left corner of context
            int left = (int) Math.rint(sCenterX - (CONTEXT_WIDTH / 2));
            int top = (int) Math.rint(sCenterY - (CONTEXT_HEIGHT / 2));
            logger.trace("left:{} top:{}", left, top);

            final int pageWidth = img.getWidth();
            final int pageHeight = img.getHeight();

            WritableRaster raster = img.getRaster();
            DataBuffer buffer = raster.getDataBuffer();
            DataBufferByte byteBuffer = (DataBufferByte) buffer;
            byte[] bytes = byteBuffer.getData();

            // Extract bytes from sub-image, paying attention to image limits
            // Target format is flattened format, row by row.
            for (int y = 0; y < CONTEXT_HEIGHT; y++) {
                int ay = top + y; // Absolute y

                if ((ay < 0) || (ay >= pageHeight)) {
                    // Fill with background value
                    for (int x = 0; x < CONTEXT_WIDTH; x++) {
                        out.print(BACKGROUND);
                        out.print(",");
                        pixelPop.includeValue(BACKGROUND);
                    }
                } else {
                    for (int x = 0; x < CONTEXT_WIDTH; x++) {
                        int ax = left + x; // Absolute x
                        int val = ((ax < 0) || (ax >= pageWidth)) ? BACKGROUND
                                : (255 - (bytes[(ay * pageWidth) + ax] & 0xff));
                        out.print(val);
                        out.print(",");
                        pixelPop.includeValue(val);
                    }
                }
            }

            // Add (OMR) shape index
            try {
                out.print(symbolShape.ordinal());
            } catch (Exception ex) {
                logger.error("Missing shape {}", symbol);
                throw new RuntimeException("Missing shape for " + symbol);
            }

            out.println();
        }
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
     * @throws java.io.IOException
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

        drawSymbols(pageInfo.getSymbols(), g);

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
                g.setColor(Color.GREEN);
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
}
