//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   G e n e r a l C o n t e x t                                  //
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code GeneralContext}
 *
 * @author Hervé Bitteur
 */
public class GeneralContext
        extends Context<GeneralShape>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GeneralContext.class);

    /** Height for symbol context, in pixels: {@value}. */
    private static final int CONTEXT_HEIGHT = 55;

    /** Width for symbol context, in pixels: {@value}. */
    private static final int CONTEXT_WIDTH = 55;

    /** Number of pixels in a patch: {@value}. */
    private static final int NUM_PIXELS = CONTEXT_HEIGHT * CONTEXT_WIDTH;

    /** Number of classes handled: {@value}. */
    private static final int NUM_CLASSES = GeneralShape.values().length;

    /** Maximum <b>Compressed</b> size of a patch (in its .csv.zip file). */
    private static final int MAX_PATCH_COMPRESSED_SIZE = 500; // TODO: check this value

    private static final GeneralShape[] LABELS = GeneralShape.values();

    // Singleton
    public static final GeneralContext INSTANCE = new GeneralContext();

    //~ Instance fields ----------------------------------------------------------------------------
    //~ Constructors -------------------------------------------------------------------------------
    private GeneralContext ()
    {

    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public int getContextHeight ()
    {
        return CONTEXT_HEIGHT;
    }

    @Override
    public int getContextWidth ()
    {
        return CONTEXT_WIDTH;
    }

    @Override
    public Class<GeneralShape> getLabelClass ()
    {
        return GeneralShape.class;
    }

    @Override
    public GeneralShape getLabel (OmrShape omrShape)
    {
        return GeneralShape.toGeneralShape(omrShape);
    }

    @Override
    public GeneralShape getLabel (int ordinal)
    {
        return LABELS[ordinal];
    }

    @Override
    public GeneralShape[] getLabels ()
    {
        return LABELS;
    }

    @Override
    public List<String> getLabelList ()
    {
        final List<String> list = new ArrayList<>(LABELS.length);

        for (GeneralShape shape : LABELS) {
            list.add(shape.toString());
        }

        return list;
    }

    @Override
    public int getMaxPatchCompressedSize ()
    {
        return MAX_PATCH_COMPRESSED_SIZE;
    }

    @Override
    public int getNumClasses ()
    {
        return NUM_CLASSES;
    }

    @Override
    public int getNumPixels ()
    {
        return NUM_PIXELS;
    }

    @Override
    public GeneralShape getNone ()
    {
        return GeneralShape.none;
    }

    @Override
    public String toString ()
    {
        return "GENERAL";
    }

    //~ Inner Classes ------------------------------------------------------------------------------
}
