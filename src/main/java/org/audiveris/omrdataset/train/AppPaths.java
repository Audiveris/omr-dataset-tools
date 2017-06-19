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
package org.audiveris.omrdataset.train;

import org.audiveris.omrdataset.Main;
import static org.audiveris.omrdataset.train.App.*;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class {@code AppPaths} gathers paths for Omr Dataset application
 *
 * @author Hervé Bitteur
 */
public class AppPaths
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Path to where the data is written. */
    public static final Path OUTPUT_PATH = (Main.cli.outputFolder != null) ? Main.cli.outputFolder
            : Paths.get("data/output");

    /** Path to created control-images. */
    public static final Path CONTROL_IMAGES_PATH = OUTPUT_PATH.resolve(CONTROL_IMAGES_NAME);

    /** Path to created sub-images. */
    public static final Path SUB_IMAGES_PATH = OUTPUT_PATH.resolve(SUB_IMAGES_NAME);

    /** Path to mistakes. */
    public static final Path MISTAKES_PATH = OUTPUT_PATH.resolve(MISTAKES_NAME);

    /** Path to single features file. */
    public static final Path FEATURES_PATH = OUTPUT_PATH.resolve(FEATURES_NAME);

    /** Path to single journal file. */
    public static final Path JOURNAL_PATH = OUTPUT_PATH.resolve(JOURNAL_NAME);

    /** Path to single sheets file. */
    public static final Path SHEETS_PATH = OUTPUT_PATH.resolve(SHEETS_NAME);

    /** Path to pixels populations. */
    public static final Path PIXELS_PATH = OUTPUT_PATH.resolve(PIXELS_NAME);

    /** Path to symbol dim populations. */
    public static final Path DIMS_PATH = OUTPUT_PATH.resolve(DIMS_NAME);

    /** Path to neural network model. */
    public static final Path MODEL_PATH = OUTPUT_PATH.resolve(MODEL_NAME);
}
