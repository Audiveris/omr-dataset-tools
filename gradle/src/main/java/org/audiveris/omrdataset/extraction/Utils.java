//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            U t i l s                                           //
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
package org.audiveris.omrdataset.extraction;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code Utils} gathers utility functions.
 *
 * @author Hervé Bitteur
 */
public abstract class Utils
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    private static final NumberFormat NF1 = NumberFormat.getNumberInstance(Locale.US);

    private static final NumberFormat NF3 = NumberFormat.getNumberInstance(Locale.US);

    static {
        NF1.setGroupingUsed(false);
        NF1.setMaximumFractionDigits(1); // For a maximum of 1 decimal
        NF3.setGroupingUsed(false);
        NF3.setMaximumFractionDigits(3); // For a maximum of 3 decimals
    }

    //~ Constructors -------------------------------------------------------------------------------
    private Utils ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // getPrintWriter //
    //----------------//
    public static PrintWriter getPrintWriter (Path path,
                                              OpenOption... options)
            throws IOException
    {
        Files.createDirectories(path.getParent());

        final OutputStream os = Files.newOutputStream(path, options);
        final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

        return new PrintWriter(bw);
    }

    //-------------------//
    // setAbsoluteStroke //
    //-------------------//
    /**
     * Whatever the current scaling of a graphic context, set the stroke to the desired
     * absolute width, and return the saved stroke for later restore.
     *
     * @param g     the current graphics context
     * @param width the absolute stroke width desired
     * @return the previous stroke
     */
    public static Stroke setAbsoluteStroke (Graphics g,
                                            float width)
    {
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform AT = g2.getTransform();
        double ratio = AT.getScaleX();
        Stroke oldStroke = g2.getStroke();
        Stroke stroke = new BasicStroke(width / (float) ratio);
        g2.setStroke(stroke);

        return oldStroke;
    }

    public static String stringOf (Rectangle2D box)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("(x").append(NF1.format(box.getX()))
                .append(",y").append(NF1.format(box.getY()))
                .append(",w").append(NF1.format(box.getWidth()))
                .append(",h").append(NF1.format(box.getHeight()))
                .append(")");

        return sb.toString();
    }
}
