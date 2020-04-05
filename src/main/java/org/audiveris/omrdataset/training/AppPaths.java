//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         A p p P a t h s                                        //
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

/**
 * Class {@code AppPaths} gathers global paths for Omr Dataset application.
 * <p>
 * NOTA: The other paths depends on the related sheet and are thus relative to sheet output folder,
 * defined as OUTPUT_PATH/sheetId.
 *
 * @author Hervé Bitteur
 */
@Deprecated
public class AppPaths
{
//
//    //~ Static fields/initializers -----------------------------------------------------------------
//    /** Path to where the data is written. */
//    public static final Path OUTPUT_PATH = (Main.cli.outputFolder != null) ? Main.cli.outputFolder
//            : Paths.get("data/output");
//
//    public static final Path FILTERED_PATH = OUTPUT_PATH.resolve("filtered");
//
//    public static final Path TRAINING_PATH = OUTPUT_PATH.resolve("training");
//
//    /** Path to global sheet map. {@value}. */
//    public static final Path SHEETS_MAP_PATH = FILTERED_PATH.resolve("global-sheets-map.xml");
//
//    /** Path to symbol dim populations. */
//    public static final Path DIMS_PATH = OUTPUT_PATH.resolve(DIMS_NAME);
//
//    /** Path to neural network model. */
//    public static final Path MODEL_PATH = (Main.cli.modelPath != null) ? Main.cli.modelPath
//            : OUTPUT_PATH.resolve(MODEL_NAME);
//
//    //~ Constructors -------------------------------------------------------------------------------
//    private AppPaths ()
//    {
//    }
}
