//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             M a i n                                            //
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code Main} reads the CSV file and
 *
 * @author Hervé Bitteur
 */
public class Main
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    //~ Methods ------------------------------------------------------------------------------------
    public static void main (String[] args)
            throws Exception
    {
        OmrShapes.printOmrShapes();

        // Extract features
        new Features().process();

        // Extract subimages for visual check (not mandatory)
        new SubImages().process();

        // Train the classifier
        new Training().process();
    }
}
