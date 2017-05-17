//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        O m r S h a p e s                                       //
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

import static org.omrdataset.OmrShape.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Class {@code OmrShapes} complements enum {@link OmrShape} with related features.
 *
 * @author Hervé Bitteur
 */
public abstract class OmrShapes
{
    //~ Static fields/initializers -----------------------------------------------------------------

    public static final List<String> NAMES = getNames();

    /** Predefined combos for time signatures. */
    public static final EnumSet<OmrShape> TIME_COMBOS = EnumSet.of(
            timeSig2over4,
            timeSig2over2,
            timeSig3over2,
            timeSig3over4,
            timeSig3over8,
            timeSig4over4,
            timeSig5over4,
            timeSig5over8,
            timeSig6over4,
            timeSig6over8,
            timeSig7over8,
            timeSig9over8,
            timeSig12over8);

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Report the list of OmrShape values, to be used by DL4J.
     *
     * @return OmrShape values, as a List
     */
    public static final List<String> getNames ()
    {
        List<String> list = new ArrayList<String>();

        for (OmrShape shape : OmrShape.values()) {
            list.add(shape.toString());
        }

        return list;
    }

    /**
     * Print out the omrShape ordinal and name.
     */
    public static void printOmrShapes ()
    {
        for (OmrShape shape : OmrShape.values()) {
            System.out.printf("%3d %s%n", shape.ordinal(), shape.toString());
        }
    }
}
