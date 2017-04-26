//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    P a g e P r o c e s s o r                                   //
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

import static org.omrdataset.App.*;
import org.omrdataset.util.Population;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Class {@code PageProcessor} processes a whole page (image + annotations) to extract
 * its features.
 *
 * @author Hervé Bitteur
 */
public class PageProcessor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PageProcessor.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final BufferedImage img;

    private final PageAnnotations pageInfo;

    private final Population pixelPop;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PageProcessor} object.
     *
     * @param img      the image
     * @param pageInfo page annotations
     * @param pixelPop (output) pixels population
     */
    public PageProcessor (BufferedImage img,
                          PageAnnotations pageInfo,
                          Population pixelPop)
    {
        this.img = img;
        this.pageInfo = pageInfo;
        this.pixelPop = pixelPop;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Process the page (image / annotations) to append the extracted features
     * (the context sub-image for each symbol) to the provided out stream.
     * <p>
     * Nota: if a sub-image goes beyond image borders, we fill the related external pixels with
     * background value.
     *
     * @param out        the output to append to
     * @param widthPops  population of symbol widths per shape
     * @param heightPops population of symbol heights per shape
     * @throws Exception
     */
    public void extractFeatures (PrintWriter out,
                                 Map<OmrShape, Population> widthPops,
                                 Map<OmrShape, Population> heightPops)
            throws Exception
    {
        final int pageWidth = img.getWidth();
        final int pageHeight = img.getHeight();

        WritableRaster raster = img.getRaster();
        DataBuffer buffer = raster.getDataBuffer();
        DataBufferByte byteBuffer = (DataBufferByte) buffer;
        byte[] bytes = byteBuffer.getData();

        // Process each symbol definition in the page
        for (SymbolInfo symbol : pageInfo.getSymbols()) {
            logger.debug("{}", symbol);

            Rectangle2D box = symbol.bounds;

            if (symbol.omrShape != OmrShape.None) {
                widthPops.get(symbol.omrShape).includeValue(box.getWidth());
                heightPops.get(symbol.omrShape).includeValue(box.getHeight());
            }

            // Symbol center
            double sCenterX = box.getX() + (box.getWidth() / 2.0);
            double sCenterY = box.getY() + (box.getHeight() / 2.0);

            // Top-left corner of context
            int left = (int) Math.rint(sCenterX - (CONTEXT_WIDTH / 2));
            int top = (int) Math.rint(sCenterY - (CONTEXT_HEIGHT / 2));
            logger.debug("left:{} top:{}", left, top);

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
                out.print(symbol.omrShape.ordinal());
            } catch (Exception ex) {
                logger.error("Missing shape {}", symbol);
                throw new RuntimeException("Missing shape for " + symbol);
            }

            out.println();
        }
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
                img.getWidth(),
                img.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = ctrl.createGraphics();
        g.drawImage(img, null, null);

        for (SymbolInfo symbol : pageInfo.getSymbols()) {
            logger.debug("{}", symbol);

            Rectangle2D box = symbol.bounds;

            if (symbol.omrShape != OmrShape.None) {
                g.setColor(Color.GREEN);
                g.draw(box);
            } else {
                Rectangle b = box.getBounds();
                g.setColor(Color.RED);
                g.drawLine(b.x, b.y - NONE_Y_MARGIN, b.x, b.y + NONE_Y_MARGIN);
                g.drawLine(b.x - NONE_X_MARGIN, b.y, b.x + NONE_X_MARGIN, b.y);
            }
        }

        g.dispose();
        ImageIO.write(ctrl, SUBIMAGE_FORMAT, controlPath.toFile());
    }
}
