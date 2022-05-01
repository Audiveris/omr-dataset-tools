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

import java.awt.Color;
import org.audiveris.omrdataset.Main;
import static org.audiveris.omrdataset.api.Context.INTERLINE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

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

    /** Definition of core rectangle of a good symbol. */
    public static final double CORE_RATIO = 0.8;

    /** Abscissa margin around a None symbol location. */
    public static final int NONE_X_MARGIN = (int) Math.rint(INTERLINE * 0.25);

    /** Ordinate margin around a None symbol location. */
    public static final int NONE_Y_MARGIN = (int) Math.rint(INTERLINE * 0.25);

    /** Ratio of None symbols created versus valid symbols found in page: {@value}. */
    public static final double NONE_RATIO = 0.5;

    /** Proportion of None symbols to be created close to good symbols: {@value}. */
    public static final double NONE_CLOSE_RATIO = 0.4;

    /** Proportion of None symbols to be created far from good/none symbols: {@value}. */
    public static final double NONE_FAR_RATIO = 0.2;

    /** Proportion of None symbols to be created out of ignored shapes: {@value}. */
    public static final double NONE_SHAPES_RATIO = 0.2;

    /** Proportion of None symbols to be created out of specific locations: {@value}. */
    public static final double NONE_LOCATIONS_RATIO = 0.2;

    /** Size of mini-batch during training. */
    public static final int BATCH_SIZE = 64;

    /** Number of training bins per archive (and per collection). */
    public static final int BIN_COUNT = 10;

    /** Max number of samples per shape and per archive: {@value}. */
    public static final int ARCHIVE_MAX_SHAPE_SAMPLES = 5000;

    /** Format for output images (patches and control-images): {@value}. */
    public static final String IMAGES_FORMAT = "png";

    // Colors
    //-------
    //
    /** Color for overlapping cross. */
    public static final Color CROSS_COLOR = new Color(255, 0, 0, 150);

    /** Color for overlapping box. */
    public static final Color BOX_COLOR = new Color(255, 0, 0, 150);

    /** Color for wrong shape. */
    public static final Color WRONG_COLOR = Color.CYAN;

    // File extensions
    //----------------
    //
    /** File extension for plain annotations: {@value}. */
    public static final String INFO_EXT = ".xml";

    /** File extension for output images: {@value}. */
    public static final String IMAGES_EXT = "." + IMAGES_FORMAT;

    /** File extension for omr: {@value}. */
    public static final String OMR_EXT = ".omr";

    /** File extension for tablatures: {@value}. */
    public static final String TABLATURES_EXT = ".tablatures.xml";

    /** File extension for filtered annotations: {@value}. */
    public static final String FILTERED_EXT = ".filtered.xml";

    /** File extension for page image: {@value}. */
    public static final String IMAGE_EXT = ".png";

    /** File extension for none locations: {@value}. */
    public static final String NONES_EXT = ".nones.csv";

    /** File extension for control-images: {@value}. */
    public static final String CONTROL_EXT = ".control.png";

    /** File extension for compressed CSV: {@value}. */
    public static final String CSV_EXT = ".csv.zip";

    // Archive Folders
    //----------------
    //
    /** Name pattern for archive folder. */
    public static final Pattern ARCHIVE_FOLDER_PATTERN = Pattern.compile("^archive-(?<num>[0-9]+)$");

    /** Name of annotations folder. */
    public static final String ANNOTATIONS_FOLDER_NAME = "xml_annotations";

    /** Name of images folder. */
    public static final String IMAGES_FOLDER_NAME = "gray_images_png";

    /** Name of omr folder. */
    public static final String OMR_FOLDER_NAME = "omr";

    /** Name of tablatures folder. */
    public static final String TABLATURES_FOLDER_NAME = "tablatures";

    /** Name of filtered folder. */
    public static final String FILTERED_FOLDER_NAME = "filtered";

    /** Name of nones folder. */
    public static final String NONES_FOLDER_NAME = "nones";

    /** Name of control folder. */
    public static final String CONTROL_FOLDER_NAME = "control";

    /** Name of features folder. */
    public static final String FEATURES_FOLDER_NAME = "features";

    /** Name of patches folder. */
    public static final String PATCHES_FOLDER_NAME = "patches";

    /** Name of shapes folder. */
    public static final String SHAPES_FOLDER_NAME = "shapes";

    /** Name of shape grids folder. */
    public static final String SHAPE_GRIDS_FOLDER_NAME = "shape_grids";

    /** Name of specific grids folder. */
    public static final String GRIDS_FOLDER_NAME = "grids";

    /** Name of mistakes folder. */
    public static final String MISTAKES_FOLDER_NAME = "mistakes";

    /** Name of bins folder. */
    public static final String BINS_FOLDER_NAME = "bins";

    /** File name of archive sheet index. {@value}. */
    public static final String SHEET_INDEX_NAME = "archive-sheet-index.csv";

    /** File name of archive blacklist. {@value}. */
    public static final String SHEET_BLACKLIST_NAME = "archive-blacklist.csv";

    // Resulting Model
    //----------------
    //
    /** Path to where the data is written. */
    public static final Path OUTPUT_PATH = (Main.cli.outputFolder != null) ? Main.cli.outputFolder
            : Paths.get("data/output");

    public static final Path ANALYSES_PATH = OUTPUT_PATH.resolve("verification");

    public static final Path TRAINING_PATH = OUTPUT_PATH.resolve("training");

    /** Path to neural network model. */
    public static final Path MODEL_PATH = (Main.cli.modelPath != null) ? Main.cli.modelPath
            : TRAINING_PATH.resolve("head-classifier.zip");

    //~ Constructors -------------------------------------------------------------------------------
    private App ()
    {
    }
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Forge the file name for a bin number.
     *
     * @param bin provided bin number
     * @return file name
     */
    public static String binName (int bin)
    {
        return String.format("bin-%02d" + CSV_EXT, bin);
    }
}
