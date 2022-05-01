//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            P a t c h                                           //
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
package org.audiveris.omrdataset.api;

import org.audiveris.omr.util.Table;
import static org.audiveris.omrdataset.api.Context.INTERLINE;
import org.audiveris.omrdataset.api.Context.Metadata;
import static org.audiveris.omrdataset.api.Context.Metadata.HEIGHT;
import static org.audiveris.omrdataset.api.Context.Metadata.TRUE_SHAPE;
import static org.audiveris.omrdataset.api.Context.Metadata.WIDTH;
import static org.audiveris.omrdataset.api.Context.Metadata.WRONG_LABEL;
import static org.audiveris.omrdataset.api.Context.Metadata.X;
import static org.audiveris.omrdataset.api.Context.Metadata.Y;
import org.audiveris.omrdataset.extraction.SourceInfo.USymbolId;
import static org.audiveris.omrdataset.extraction.SourceInfo.parseSymbolId;
import static org.audiveris.omrdataset.extraction.Utils.setAbsoluteStroke;
import static org.audiveris.omrdataset.extraction.Utils.stringOf;
import static org.audiveris.omrdataset.training.App.BOX_COLOR;
import static org.audiveris.omrdataset.training.App.CROSS_COLOR;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
import java.awt.image.DataBufferByte;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Class {@code Patch} defines a patch in a given context (either {@link HeadContext} or
 * {@link GeneralContext}).
 *
 * @author Hervé Bitteur
 *
 * @param <S> precise Shape type
 * @param <C> precise Context type
 */
public class Patch<S extends Enum, C extends Context<S>>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Patch.class);

    private static final NumberFormat NF3 = NumberFormat.getNumberInstance(Locale.US);

    static {
        NF3.setGroupingUsed(false);
        NF3.setMaximumFractionDigits(3); // For a maximum of 3 decimals
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Chosen context. */
    protected final C context;

    /** Patch pixels. */
    public final Table.UnsignedByte pixels;

    //~ Constructors -------------------------------------------------------------------------------
    public Patch (Table.UnsignedByte pixels,
                  C context)
    {
        this.pixels = pixels;
        this.context = context;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // toCsv //
    //-------//
    /**
     * Report the CSV-formatted string of the patch.
     *
     * @return CSV string
     */
    public String toCsv ()
    {
        final int numPixels = context.getNumPixels();
        final StringBuilder sb = new StringBuilder(3 * numPixels); // Large enough

        for (int i = 0; i < numPixels; i++) {
            if (i > 0) {
                sb.append(',');
            }

            sb.append(pixels.getValue(i));
        }

        return sb.toString();
    }

    //---------//
    // toImage //
    //---------//
    /**
     * Report a gray BufferedImage filled with the <b>inverted</b> pixels values.
     *
     * @return the gray image
     */
    public BufferedImage toImage ()
    {
        final BufferedImage grayImg = new BufferedImage(
                context.getContextWidth(),
                context.getContextHeight(),
                TYPE_BYTE_GRAY);
        final DataBufferByte byteBuffer = (DataBufferByte) grayImg.getRaster().getDataBuffer();
        final int numPixels = context.getNumPixels();

        for (int i = 0; i < numPixels; i++) {
            byteBuffer.setElem(i, 255 - pixels.getValue(i)); // Pixel inversion
        }

        return grayImg;
    }

    //-------------//
    // pixelsArray //
    //-------------//
    /**
     * Report the INDArray of patch pixels, properly shaped and normalized.
     *
     * @return the pixels array, ready to be fed into neural network.
     */
    public INDArray pixelsArray ()
    {
        final int numPixels = context.getNumPixels();
        final double[] doubleArray = new double[numPixels];

        for (int i = 0; i < numPixels; i++) {
            doubleArray[i] = pixels.getValue(i) / 255.0;
        }

        return Nd4j.create(doubleArray, new int[]{1,
                                                  1,
                                                  context.getContextWidth(),
                                                  context.getContextHeight()});
    }

    //------//
    // draw //
    //------//
    /**
     * Draw the patch on the provided graphics environment.
     *
     * @param g         provided graphics context
     * @param withCross true for drawing cross pointing to patch center
     */
    public void draw (Graphics2D g,
                      boolean withCross)
    {
        // Image as background
        g.drawImage(toImage(), null, null);

        if (withCross) {
            final Stroke oldStroke = setAbsoluteStroke(g, 1.0f);
            g.setColor(CROSS_COLOR);
            g.draw(new Line2D.Double(0.5,
                                     0.5,
                                     context.getContextWidth() - 0.5,
                                     context.getContextHeight() - 0.5));
            g.draw(new Line2D.Double(0.5,
                                     context.getContextHeight() - 0.5,
                                     context.getContextWidth() - 0.5,
                                     0.5));
            g.setStroke(oldStroke);
        }
    }

    //-----------//
    // internals //
    //-----------//
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(context)
                .append('w').append(pixels.getWidth())
                .append('h').append(pixels.getHeight());

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('{').append(internals()).append('}');
        return sb.toString();
    }

    //-------------//
    // parsePixels //
    //-------------//
    public static <C extends Context> Table.UnsignedByte parsePixels (String[] cols,
                                                                      C context)
    {
        final Table.UnsignedByte pixels = new Table.UnsignedByte(context.getContextWidth(),
                                                                 context.getContextHeight());
        final int numPixels = context.getNumPixels();

        for (int i = 0; i < numPixels; i++) {
            pixels.setValue(i, Integer.parseInt(cols[i]));
        }

        return pixels;
    }

    //------------//
    // parsePatch //
    //------------//
    public static <S extends Enum, C extends Context<S>> Patch parsePatch (String line,
                                                                           C context)
    {
        return new Patch(parsePixels(line.split(","), context), context);
    }

    //------------//
    // parseLabel //
    //------------//
    public static <S extends Enum, C extends Context<S>> S parseLabel (String[] cols,
                                                                       C context)
    {
        return context.getLabel(Integer.parseInt(cols[context.getCsvLabel()]));
    }

    //----------------//
    // parseTrueShape //
    //----------------//
    public static <S extends Enum, C extends Context<S>> OmrShape parseTrueShape (String[] cols,
                                                                                  C context)
    {
        return OmrShapes.OMR_SHAPES[Integer.parseInt(cols[context.getCsv(TRUE_SHAPE)])];
    }

    //-----------------//
    // parseWrongLabel //
    //-----------------//
    public static <S extends Enum, C extends Context<S>> S parseWrongLabel (String[] cols,
                                                                            C context)
    {
        return context.getLabel(Integer.parseInt(cols[context.getCsv(WRONG_LABEL)]));
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------------//
    // LabeledPatch //
    //--------------//
    public static class LabeledPatch<S extends Enum, C extends Context<S>>
            extends Patch<S, C>
    {

        /** Assigned label (which may be different from trueShape). */
        public final S label;

        public LabeledPatch (S label,
                             Table.UnsignedByte pixels,
                             C context)
        {
            super(pixels, context);
            this.label = label;
        }

        @Override
        public String toCsv ()
        {
            return super.toCsv() + "," + label.ordinal();
        }

        public static <S extends Enum, C extends Context<S>>
                LabeledPatch<S, C> parseLabeledPatch (String[] cols,
                                                      C context)
        {
            return new LabeledPatch(parseLabel(cols, context), parsePixels(cols, context), context);
        }

        public static <S extends Enum, C extends Context<S>>
                LabeledPatch<S, C> parseLabeledPatch (String line,
                                                      C context)
        {
            return LabeledPatch.parseLabeledPatch(line.split(","), context);
        }

        @Override
        protected String internals ()
        {
            return super.internals() + "," + label;
        }
    }

    //--------//
    // UPatch //
    //--------//
    public static class UPatch<S extends Enum, C extends Context<S>>
            extends LabeledPatch<S, C>
    {

        /** True shape. */
        public final OmrShape trueShape;

        /** Link back to symbol origin. */
        public final USymbolId uSymbolId;

        /** Bounding box in original sheet. */
        public final Rectangle2D box;

        /** Symbol related interline value in original sheet. */
        public final double interline;

        public UPatch (OmrShape trueShape,
                       USymbolId uSymbolId,
                       Rectangle2D box,
                       double interline,
                       S label,
                       Table.UnsignedByte pixels,
                       C context)
        {
            super(label, pixels, context);

            this.trueShape = trueShape;
            this.uSymbolId = uSymbolId;
            this.box = box;
            this.interline = interline;
        }

        public UPatch (OmrShape trueShape,
                       USymbolId uSymbolId,
                       Rectangle2D box,
                       double interline,
                       LabeledPatch<S, C> lPatch)
        {
            this(trueShape, uSymbolId, box, interline, lPatch.label, lPatch.pixels, lPatch.context);
        }

        /**
         * Draw the patch on the provided graphics environment.
         *
         * @param g         provided graphics context
         * @param withCross true for drawing cross pointing to patch center
         * @param withBox   true for drawing symbol bounding box
         */
        public void draw (Graphics2D g,
                          boolean withCross,
                          boolean withBox)
        {
            super.draw(g, withCross);

            if (withBox && (label != context.getNone())) {
                g.setColor(BOX_COLOR);
                final Stroke oldStroke = setAbsoluteStroke(g, 1.0f);
                final double w = box.getWidth() * INTERLINE / interline;
                final double h = box.getHeight() * INTERLINE / interline;

                // Draw symbol bounding box, 1 pixel off of symbol
                g.draw(new Rectangle2D.Double((context.getContextWidth() - w) / 2.0 - 0.5,
                                              (context.getContextHeight() - h) / 2.0 - 0.5,
                                              w + 1,
                                              h + 1));
                g.setStroke(oldStroke);
            }
        }

        @Override
        protected String internals ()
        {
            StringBuilder sb = new StringBuilder(super.internals());

            sb.append(':').append(trueShape);
            sb.append(',').append(uSymbolId);
            sb.append(stringOf(box));
            sb.append(",il").append(String.format("%.0f", interline));

            return sb.toString();
        }

        @Override
        public String toCsv ()
        {
            StringBuilder sb = new StringBuilder(super.toCsv());

            // trueShape
            sb.append(',').append(trueShape.ordinal());

            // uSymbolId
            sb.append(',').append(uSymbolId.collectionId);
            sb.append(',').append(uSymbolId.archiveId);
            sb.append(',').append(uSymbolId.sheetId);
            sb.append(',').append(uSymbolId.symbolId);

            // Original symbol bounds
            sb.append(',').append(NF3.format(box.getX()));
            sb.append(',').append(NF3.format(box.getY()));
            sb.append(',').append(NF3.format(box.getWidth()));
            sb.append(',').append(NF3.format(box.getHeight()));

            // Original interline value for this symbol
            sb.append(',').append(NF3.format(interline));

            return sb.toString();
        }

        public static <S extends Enum, C extends Context<S>> double parseInterline (String[] cols,
                                                                                    C context)
        {
            return Double.parseDouble(cols[context.getCsv(Metadata.INTERLINE)]);
        }

        public static <S extends Enum, C extends Context<S>>
                Rectangle2D parseRectangle (String[] cols,
                                            C context)
        {
            return new Rectangle2D.Double(
                    Double.parseDouble(cols[context.getCsv(X)]),
                    Double.parseDouble(cols[context.getCsv(Y)]),
                    Double.parseDouble(cols[context.getCsv(WIDTH)]),
                    Double.parseDouble(cols[context.getCsv(HEIGHT)]));
        }

        public static <S extends Enum, C extends Context<S>>
                UPatch<S, C> parseUPatch (String[] cols,
                                          C context)
        {
            return new UPatch(parseTrueShape(cols, context),
                              parseSymbolId(cols, context),
                              parseRectangle(cols, context),
                              parseInterline(cols, context),
                              parseLabel(cols, context),
                              parsePixels(cols, context),
                              context);
        }

        public static <S extends Enum, C extends Context<S>> UPatch parseUPatch (String line,
                                                                                 C context)
        {
            return parseUPatch(line.split(","), context);
        }
    }

    //------------//
    // WrongPatch //
    //------------//
    public static class WrongPatch<S extends Enum, C extends Context<S>>
            extends UPatch<S, C>
    {

        public final S wrongLabel;

        public WrongPatch (S wrongLabel,
                           OmrShape trueShape,
                           USymbolId uSymbolId,
                           Rectangle2D box,
                           double interline,
                           S label,
                           Table.UnsignedByte pixels,
                           C context)
        {
            super(trueShape, uSymbolId, box, interline, label, pixels, context);
            this.wrongLabel = wrongLabel;
        }

        public WrongPatch (S wrongLabel,
                           UPatch<S, C> uPatch,
                           C context)
        {
            super(uPatch.trueShape, uPatch.uSymbolId, uPatch.box, uPatch.interline,
                  uPatch.label, uPatch.pixels, context);
            this.wrongLabel = wrongLabel;
        }

        @Override
        protected String internals ()
        {
            StringBuilder sb = new StringBuilder(super.internals());

            sb.append('p').append(wrongLabel);

            return sb.toString();
        }

        @Override
        public String toCsv ()
        {
            return super.toCsv() + "," + wrongLabel.ordinal();
        }

        public static <S extends Enum, C extends Context<S>>
                WrongPatch parseWrongPatch (String[] cols,
                                            C context)
        {
            return new WrongPatch(parseWrongLabel(cols, context),
                                  parseTrueShape(cols, context),
                                  parseSymbolId(cols, context),
                                  parseRectangle(cols, context),
                                  parseInterline(cols, context),
                                  parseLabel(cols, context),
                                  parsePixels(cols, context),
                                  context);
        }

        public static <S extends Enum, C extends Context<S>>
                WrongPatch parseWrongPatch (String line,
                                            C context)
        {
            return parseWrongPatch(line.split(","), context);
        }
    }
}
