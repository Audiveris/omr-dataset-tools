//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          C o n t e x t                                         //
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
package org.audiveris.omrdataset.classifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code Context} gathers the needed definitions for using a context classifier
 * working on Omr Dataset.
 *
 * @author Hervé Bitteur
 */
public abstract class Context
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Context.class);

    /** Predefined interline value: {@value}. */
    public static final int INTERLINE = 10;

    /** Needed multiple for context dimensions: {@value}. */
    public static final int MULTIPLE = 4;

    /** Height for symbol context, in pixels. */
    public static final int CONTEXT_HEIGHT = toMultiple(INTERLINE * 9.6);

    /** Width for symbol context, in pixels. */
    public static final int CONTEXT_WIDTH = toMultiple(INTERLINE * 4.8);

    static {
        logger.info(
                "Context interline:{} width:{} height:{}",
                INTERLINE,
                CONTEXT_WIDTH,
                CONTEXT_HEIGHT);
    }

    /** Maximum symbol scale value to trigger a shape rename: {@value}. */
    public static final double MAX_SYMBOL_SCALE = 0.85;

    /** Value used for background pixel feature: {@value}. */
    public static final int BACKGROUND = 0;

    /** Value used for foreground pixel feature: {@value}. */
    public static final int FOREGROUND = 255;

    /** File name for symbol dimensions standards: {@value}. */
    public static final String DIMS_NAME = "dims.dat";

    /** File name for neural network model: {@value}. */
    public static final String MODEL_NAME = "img-classifier.zip";

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Report the integer value (as multiple of MULTIPLE).
     *
     * @return ceiling value, as multiple of MULTIPLE
     */
    private static int toMultiple (double val)
    {
        return MULTIPLE * (int) Math.ceil(val / MULTIPLE);
    }
}
