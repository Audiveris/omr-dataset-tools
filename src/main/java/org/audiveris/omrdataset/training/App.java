//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                              A p p                                             //
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

import java.nio.file.Path;
import java.nio.file.Paths;
import org.audiveris.omrdataset.Main;
import static org.audiveris.omrdataset.training.Context.INTERLINE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code App} defines constants for the whole {@code OmrDataSet} application.
 *
 * @author Hervé Bitteur
 */
public abstract class App
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    /** Maximum symbol scale value to trigger a shape rename: {@value}. */
    public static final double MAX_SYMBOL_SCALE = 0.85;

    /** Abscissa margin around a None symbol location. */
    public static final int NONE_X_MARGIN = (int) Math.rint(INTERLINE * 0.5);

    /** Ordinate margin around a None symbol location. */
    public static final int NONE_Y_MARGIN = (int) Math.rint(INTERLINE * 0.5);

    /** Ratio of None symbols created versus valid symbols found in page: {@value}. */
    public static final double NONE_RATIO = 0.2;

    /** Format for output images (patches and control-images): {@value}. */
    public static final String IMAGES_FORMAT = "png";

    /** File extension for output images: {@value}. */
    public static final String IMAGES_EXT = "." + IMAGES_FORMAT;

    /** File extension for plain annotations: {@value}. */
    public static final String INFO_EXT = ".xml";

    /** Name of filtered folder. */
    public static final String FILTERED_FOLDER_NAME = "filtered";

    /** File extension for filtered annotations: {@value}. */
    public static final String FILTERED_EXT = ".filtered.xml";

    /** File extension for tablatures: {@value}. */
    public static final String TABLATURES_EXT = ".tablatures.xml";

    /** File extension for page image: {@value}. */
    public static final String IMAGE_EXT = ".png";

    /** Name of control folder. */
    public static final String CONTROL_FOLDER_NAME = "control";

    /** File extension for control-images: {@value}. */
    public static final String CONTROL_EXT = ".control.png";

    /** Name of features folder. */
    public static final String FEATURES_FOLDER_NAME = "features";

    /** File extension for sheet features: {@value}. */
    public static final String FEATURES_EXT = ".features.csv";

    /** File extension for compressed sheet features: {@value}. */
    public static final String FEATURES_ZIP = ".features.zip";

    /** Name of patches folder. */
    public static final String PATCHES_FOLDER_NAME = "patches";

    /** Path to where the data is written. */
    public static final Path OUTPUT_PATH = (Main.cli.outputFolder != null) ? Main.cli.outputFolder
            : Paths.get("data/output");

    public static final Path ANALYSES_PATH = OUTPUT_PATH.resolve("analyses");

    public static final Path TRAINING_PATH = OUTPUT_PATH.resolve("training");

    public static final Path BINS_PATH = TRAINING_PATH.resolve("bins");

    /** Path to global sheet map. {@value}. */
    public static final Path SHEETS_MAP_PATH = TRAINING_PATH.resolve("global-sheets-map.xml");

    /** Path to neural network model. */
    public static final Path MODEL_PATH = (Main.cli.modelPath != null) ? Main.cli.modelPath
            : TRAINING_PATH.resolve("patch-classifier.zip");

    /** Size of mini-batch during training. */
    public static final int BATCH_SIZE = 64;

    /** Number of features bins. */
    public static final int BIN_COUNT = 10;

    //~ Constructors -------------------------------------------------------------------------------
    private App ()
    {
    }
}
