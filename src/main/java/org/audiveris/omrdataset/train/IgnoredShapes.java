//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    I g n o r e d S h a p e s                                   //
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
import static org.audiveris.omrdataset.api.OmrShape.*;

import java.util.EnumSet;

/**
 * Class {@code IgnoredShapes} handles the OmrShape names currently ignored for
 * actual training.
 *
 * @author Hervé Bitteur
 */
public abstract class IgnoredShapes
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Symbol shapes to be ignored by training (for the time being). */
    private static final EnumSet<OmrShape> IGNORED_SHAPES = EnumSet.of(legerLine, stem);

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Report whether the provided shape is to be ignored for standard processing.
     *
     * @param shape the provided shape
     * @return true to ignore
     */
    public static boolean isIgnored (OmrShape shape)
    {
        return IGNORED_SHAPES.contains(shape);
    }
}
