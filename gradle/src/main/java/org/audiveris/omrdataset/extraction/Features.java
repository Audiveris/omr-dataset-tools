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

import org.audiveris.omr.util.Table;
import org.audiveris.omr.util.ZipWrapper;
import org.audiveris.omrdataset.Main;
import org.audiveris.omrdataset.api.Context;
import org.audiveris.omrdataset.api.OmrShape;
import org.audiveris.omrdataset.api.Patch.UPatch;
import org.audiveris.omrdataset.api.SheetAnnotations;
import org.audiveris.omrdataset.api.SymbolInfo;
import org.audiveris.omrdataset.extraction.SourceInfo.USheetId;
import org.audiveris.omrdataset.extraction.SourceInfo.USymbolId;
import static org.audiveris.omrdataset.api.Context.BACKGROUND;
import static org.audiveris.omrdataset.api.Context.INTERLINE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
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
 * Class {@code Features} reads input data (image and annotations) and produces
 * a features CSV file meant for NN training.
 * <p>
 * In a features CSV file, there must be one record per sample.
 *
 * @author Hervé Bitteur
 */
public class Features
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Features.class);

    /** Should we process inner symbols?. */
    private static final boolean leaves = true;

    //~ Instance fields ----------------------------------------------------------------------------
    /** The sheet being processed. */
    private final USheetId uSheetId;

    /** Symbols (and nones) in this sheet. */
    private final SheetAnnotations annotations;

    /** Sheet image(s) indexed by interline value. */
    private final Map<Integer, BufferedImage> imgMap = new TreeMap<>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a {@code Features} object.
     *
     * @param uSheetId    ID of related sheet
     * @param annotations sheet annotations
     */
    public Features (USheetId uSheetId,
                     SheetAnnotations annotations)
    {
        this.uSheetId = uSheetId;
        this.annotations = annotations;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // extract //
    //---------//
    /**
     * Extract the features for sheet good symbols with respect to current context.
     *
     * @param initialImg   whole sheet image
     * @param featuresPath path to sheet features compressed output file (.zip)
     * @throws IOException if anything goes wrong
     */
    public void extract (BufferedImage initialImg,
                         Path featuresPath)
            throws IOException
    {
        final List<SymbolInfo> symbols = annotations.getGoodSymbols();
        final ZipWrapper zout = ZipWrapper.create(featuresPath);
        logger.info("{} Extracting features to {}", uSheetId, featuresPath);

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
     * @param originalImg the original image
     * @param symbols     the symbols to process
     * @param features    features output file
     */
    private void extractFeatures (BufferedImage originalImg,
                                  List<SymbolInfo> symbols,
                                  PrintWriter features)
            throws IOException
    {
        for (SymbolInfo symbol : symbols) {
            final OmrShape symbolShape = symbol.getOmrShape();

            // Inner symbols?
            if (leaves) {
                List<SymbolInfo> innerSymbols = symbol.getInnerSymbols();

                if (!innerSymbols.isEmpty()) {
                    extractFeatures(originalImg,
                                    SymbolInfo.getGoodSymbols(innerSymbols, Main.context),
                                    features);
                }
            }

            final Context context = Main.context;
            final UPatch uPatch = new UPatch(
                    symbolShape,
                    new USymbolId(uSheetId, symbol.getId()),
                    symbol.getBounds(),
                    symbol.getInterline(),
                    context.ignores(symbolShape) ? context.getNone() : context.getLabel(symbolShape),
                    getPixels(symbol, originalImg),
                    Main.context);

            ///System.out.println(uPatch.toString());
            features.println(uPatch.toCsv());
        }
    }

    //-----------//
    // getPixels //
    //-----------//
    /**
     * Build the pixel table for a symbol within its original image.
     *
     * @param symbol        the symbol to process
     * @param originalImage the original score image
     * @return the populated pixel table
     */
    private Table.UnsignedByte getPixels (SymbolInfo symbol,
                                          BufferedImage originalImage)
            throws IOException
    {
        // Pick up image properly scaled
        final double interline = symbol.getInterline();
        final int roundedInterline = (int) Math.rint(interline);
        final boolean rescale = roundedInterline != INTERLINE;
        final double ratio = INTERLINE / interline;
        BufferedImage img = imgMap.get(roundedInterline);

        if (img == null) {
            imgMap.put(roundedInterline, img = rescale
                       ? scale(originalImage, ratio) : originalImage);
        }

        final int imgWidth = img.getWidth();
        final int imgHeight = img.getHeight();

        // Symbol center
        final Rectangle2D box = symbol.getBounds();
        final double sCenterX = ratio * (box.getX() + (box.getWidth() / 2.0));
        final double sCenterY = ratio * (box.getY() + (box.getHeight() / 2.0));

        // Top-left corner of context
        final int contextWidth = Main.context.getContextWidth();
        final int contextHeight = Main.context.getContextHeight();
        final int axMin = (int) Math.rint(sCenterX - (contextWidth / 2));
        final int ayMin = (int) Math.rint(sCenterY - (contextHeight / 2));
        logger.trace("left:{} top:{}", axMin, ayMin);

        final WritableRaster raster = img.getRaster();
        final DataBuffer buffer = raster.getDataBuffer();
        final DataBufferByte byteBuffer = (DataBufferByte) buffer;
        final byte[] bytes = byteBuffer.getData();
        final Table.UnsignedByte pixels = new Table.UnsignedByte(contextWidth, contextHeight);

        // Extract bytes from sub-image, paying attention to image limits
        // Target format is flattened format, row by row.
        int idx = 0;
        for (int y = 0; y < contextHeight; y++) {
            int ay = ayMin + y; // Absolute y

            if ((ay < 0) || (ay >= imgHeight)) {
                // Fill row with background value
                for (int x = 0; x < contextWidth; x++) {
                    pixels.setValue(idx++, BACKGROUND);
                }
            } else {
                for (int x = 0; x < contextWidth; x++) {
                    int ax = axMin + x; // Absolute x
                    int val = ((ax < 0) || (ax >= imgWidth)) ? BACKGROUND
                            : (255 - (bytes[(ay * imgWidth) + ax] & 0xff)); // Inversion!
                    pixels.setValue(idx++, val);
                }
            }
        }

        return pixels;
    }

    //-------//
    // scale //
    //-------//
    /**
     * Build a scaled version of an image.
     *
     * @param img   image to scale
     * @param ratio scaling ratio
     * @return the scaled image
     */
    private BufferedImage scale (BufferedImage img,
                                 double ratio)
    {
        AffineTransform at = AffineTransform.getScaleInstance(ratio, ratio);
        AffineTransformOp atop = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        BufferedImage scaledImg = new BufferedImage(
                (int) Math.ceil(img.getWidth() * ratio),
                (int) Math.ceil(img.getHeight() * ratio),
                img.getType());

        BufferedImage scaled = atop.filter(img, scaledImg);

        return scaled;
    }
}
