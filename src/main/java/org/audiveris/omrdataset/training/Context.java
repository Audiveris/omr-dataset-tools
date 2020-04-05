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
package org.audiveris.omrdataset.training;

import org.audiveris.omrdataset.api.OmrShape;
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

    /** Value used for background pixel feature: {@value}. */
    public static final int BACKGROUND = 0;

    /** Value used for foreground pixel feature: {@value}. */
    public static final int FOREGROUND = 255;

    /** Height for symbol context, in pixels. */
    public static final int CONTEXT_HEIGHT = 112;

    /** Width for symbol context, in pixels. */
    public static final int CONTEXT_WIDTH = 56;

    /** Number of classes handled. */
    public static final int NUM_CLASSES = OmrShape.unknown.ordinal();

    /** CSV index of Label: {@value}. */
    public static final int CSV_LABEL = CONTEXT_HEIGHT * CONTEXT_WIDTH;

    /** CSV index of Collection: {@value}. ZHAW:1 vs MuseScore:2. */
    public static final int CSV_COLLECTION = CSV_LABEL + 1;

    /** CSV index of SymbolId: {@value}. */
    public static final int CSV_SYMBOL_ID = CSV_COLLECTION + 1;

    /** CSV index of X: {@value}. */
    public static final int CSV_X = CSV_SYMBOL_ID + 1;

    /** CSV index of Y: {@value}. */
    public static final int CSV_Y = CSV_X + 1;

    /** CSV index of Width: {@value}. */
    public static final int CSV_WIDTH = CSV_Y + 1;

    /** CSV index of Height: {@value}. */
    public static final int CSV_HEIGHT = CSV_WIDTH + 1;

    /** CSV index of Interline: {@value}. */
    public static final int CSV_INTERLINE = CSV_HEIGHT + 1;

    /** CSV index of SheetId: {@value}. */
    public static final int CSV_SHEET_ID = CSV_INTERLINE + 1;

    //~ Constructors -------------------------------------------------------------------------------
    private Context ()
    {
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    /**
     * To remember the source training data comes from.
     */
    public enum SourceType
    {
        UNKNOWN,
        ZHAW,
        MUSESCORE;
    }
}
