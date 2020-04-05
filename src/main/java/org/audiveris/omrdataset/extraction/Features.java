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
package org.audiveris.omrdataset.extraction;

import org.audiveris.omr.util.ZipWrapper;
import org.audiveris.omrdataset.api.OmrShape;
import org.audiveris.omrdataset.api.SheetAnnotations;
import org.audiveris.omrdataset.api.SymbolInfo;
import static org.audiveris.omrdataset.training.Context.BACKGROUND;
import static org.audiveris.omrdataset.training.Context.CONTEXT_HEIGHT;
import static org.audiveris.omrdataset.training.Context.CONTEXT_WIDTH;
import static org.audiveris.omrdataset.training.Context.INTERLINE;
import static org.audiveris.omrdataset.extraction.SheetProcessor.scale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code Features} reads input images data (collection of pairs: sheet image and
 * symbol descriptors) and produces the features CSV file meant for NN training.
 * <p>
 * Each page annotations are augmented with artificial None symbols.
 * For visual checking, a page image can be produced with initial image, true symbols boxes and
 * None symbols locations.
 * <p>
 * In the features CSV file, there must be one record per symbol, containing:
 * <ol>
 * <li>the (112*56=6272) pixels of the patch image centered on the symbol center
 * <li>(index of) shape name
 * <li>symbol id within sheet
 * <li>x
 * <li>y
 * <li>w
 * <li>h
 * <li>interline value (10) // Useful?
 * <li>sheet id (APPENDED WHEN BEING STORED IN bin-xx.csv)
 * </ol>
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

    private static final boolean leaves = true;

    //~ Instance fields ----------------------------------------------------------------------------
    private final int sheetId;

    private final SheetAnnotations annotations;

    /** Sheet image(s) gathered by interline value. */
    private final Map<Integer, BufferedImage> imgMap = new TreeMap<>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a {@code Features} object.
     *
     * @param sheetId     ID of related sheet
     * @param annotations sheet annotations
     */
    public Features (int sheetId,
                     SheetAnnotations annotations)
    {
        this.sheetId = sheetId;
        this.annotations = annotations;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // extract //
    //---------//
    /**
     * Extract the features for sheet good symbols.
     *
     * @param initialImg   whole sheet image
     * @param featuresPath path to sheet features compressed file (.zip)
     * @throws IOException if anything goes wrong
     */
    public void extract (BufferedImage initialImg,
                         Path featuresPath)
            throws IOException
    {
        List<SymbolInfo> symbols = annotations.getGoodSymbols();
        ZipWrapper zout = ZipWrapper.create(featuresPath);
        logger.info("sheetId:{} Extracting features to (zipped) {}", sheetId, featuresPath);

        try (PrintWriter features = zout.newPrintWriter()) {
            extractFeatures(initialImg, symbols, features);
            features.flush();
        }

        zout.close();
    }

    //-----------------//
    // extractFeatures //
    //-----------------//
    /**
     * Generate the features related to the collection of provided symbols.
     *
     * @param initialImg the initial image
     * @param symbols    the symbols to process
     * @param features   features output file
     */
    private void extractFeatures (BufferedImage initialImg,
                                  List<SymbolInfo> symbols,
                                  PrintWriter features)
    {
        for (SymbolInfo symbol : symbols) {
            final OmrShape symbolShape = symbol.getOmrShape();
            logger.debug("{}", symbol);

            // Inner symbols?
            if (leaves) {
                List<SymbolInfo> innerSymbols = symbol.getInnerSymbols();

                if (!innerSymbols.isEmpty()) {
                    extractFeatures(initialImg, SymbolInfo.getGoodSymbols(innerSymbols), features);
//
//                    if (!OmrShapes.TIME_COMBOS.contains(symbolShape)) {
//                        continue;
//                    }
                }
            }

            final StringBuilder sb = new StringBuilder(6500); // Largely presized
            final Rectangle2D box = symbol.getBounds();

            // Pick up image properly scaled
            final double interline = symbol.getInterline();
            final int roundedInterline = (int) Math.rint(interline);
            final boolean rescale = roundedInterline != INTERLINE;
            final double ratio = INTERLINE / interline;
            BufferedImage img = imgMap.get(roundedInterline);

            if (img == null) {
                imgMap.put(roundedInterline, img = rescale ? scale(initialImg, ratio) : initialImg);
            }

            final int imgWidth = img.getWidth();
            final int imgHeight = img.getHeight();

            // Symbol center
            final double sCenterX = ratio * (box.getX() + (box.getWidth() / 2.0));
            final double sCenterY = ratio * (box.getY() + (box.getHeight() / 2.0));

            // Top-left corner of context
            final int axMin = (int) Math.rint(sCenterX - (CONTEXT_WIDTH / 2));
            final int ayMin = (int) Math.rint(sCenterY - (CONTEXT_HEIGHT / 2));
            logger.trace("left:{} top:{}", axMin, ayMin);

            final WritableRaster raster = img.getRaster();
            final DataBuffer buffer = raster.getDataBuffer();
            final DataBufferByte byteBuffer = (DataBufferByte) buffer;
            final byte[] bytes = byteBuffer.getData();

            // Extract bytes from sub-image, paying attention to image limits
            // Target format is flattened format, row by row.
            for (int y = 0; y < CONTEXT_HEIGHT; y++) {
                int ay = ayMin + y; // Absolute y

                if ((ay < 0) || (ay >= imgHeight)) {
                    // Fill row with background value
                    for (int x = 0; x < CONTEXT_WIDTH; x++) {
                        sb.append(BACKGROUND).append(',');
                    }
                } else {
                    for (int x = 0; x < CONTEXT_WIDTH; x++) {
                        int ax = axMin + x; // Absolute x
                        int val = ((ax < 0) || (ax >= imgWidth)) ? BACKGROUND
                                : (255 - (bytes[(ay * imgWidth) + ax] & 0xff)); // Inversion!
                        sb.append(val).append(',');
                    }
                }
            }

            // Add OmrShape index
            try {
                sb.append(symbolShape.ordinal());
            } catch (Exception ex) {
                logger.error("sheetId:{} Missing shape {}", sheetId, symbol);
                throw new RuntimeException("Missing shape for " + symbol);
            }

            // Collection source. A bit tricky, to be improved! :-)
            final String src = annotations.getSource().toLowerCase();
            sb.append(',').append(src.contains("zhaw") ? 1 : (src.contains("musescore") ? 2 : 0));

            // Symbol id
            sb.append(',').append(symbol.getId());

            // Original symbol bounds (non scaled)
            sb.append(',').append(box.getX());
            sb.append(',').append(box.getY());
            sb.append(',').append(box.getWidth());
            sb.append(',').append(box.getHeight());

            // Original interline value for this symbol
            sb.append(',').append(symbol.getInterline());

// We do not include sheet ID at this point!
//            // Sheet id
//            sb.append(',').append(sheetId);
//
            // This is the end for this symbol...
            features.println(sb.toString());
        }
    }
}
