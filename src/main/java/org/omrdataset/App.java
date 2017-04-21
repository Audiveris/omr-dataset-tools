//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                              A p p                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class {@code App} defines constants for the whole {@code OmrDataSet} application.
 *
 * @author Hervé Bitteur
 */
public abstract class App
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Path to where the data is found. */
    public static final Path DATA_PATH = Paths.get("data");

    /** Path to input images. */
    public static final Path IMAGES_PATH = DATA_PATH.resolve("input-images");

    /** Path to created sub-images. */
    public static final Path SUBIMAGES_PATH = DATA_PATH.resolve("subimages");

    /** Format for sub-images. */
    public static final String SUBIMAGE_FORMAT = "png";

    /** File extension for page images. */
    public static final String IMAGE_EXT = ".png";

    /** File extension for page info. */
    public static final String INFO_EXT = ".xml";

    /** Path to single CSV file. */
    public static final Path CSV_PATH = DATA_PATH.resolve("features.csv");

    /** Height in pixels for symbol context. */
    public static final int CONTEXT_HEIGHT = 96;

    /** Width in pixels for symbol context. */
    public static final int CONTEXT_WIDTH = 48;
}
