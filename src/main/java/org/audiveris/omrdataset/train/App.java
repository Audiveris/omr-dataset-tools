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
package org.audiveris.omrdataset.train;

import static org.audiveris.omrdataset.classifier.Context.INTERLINE;

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

    /** Abscissa margin around a None symbol location. */
    public static final int NONE_X_MARGIN = (int) Math.rint(INTERLINE * 0.5);

    /** Ordinate margin around a None symbol location. */
    public static final int NONE_Y_MARGIN = (int) Math.rint(INTERLINE * 0.5);

    /** Ratio of None symbols created versus valid symbols found in page: {@value}. */
    public static final double NONE_RATIO = 0.2; // 1.0;

    /** Format for output images (sub-images and control-images): {@value}. */
    public static final String OUTPUT_IMAGES_FORMAT = "png";

    /** File extension for output images: {@value}. */
    public static final String OUTPUT_IMAGES_EXT = "." + OUTPUT_IMAGES_FORMAT;

    /** File extension for page info: {@value}. */
    public static final String INFO_EXT = ".xml";

    /** Folder name for control-images: {@value}. */
    public static final String CONTROL_IMAGES_NAME = "control-images";

    /** Folder name for sub-images: {@value}. */
    public static final String SUB_IMAGES_NAME = "sub-images";

    /** Folder name for mistakes: {@value}. */
    public static final String MISTAKES_NAME = "mistakes";

    /** File name for features: {@value}. */
    public static final String FEATURES_NAME = "features.csv";

    /** FIle name for journal: {@value}. */
    public static final String JOURNAL_NAME = "journal.csv";

    /** File name for sheets: {@value}. */
    public static final String SHEETS_NAME = "sheets.csv";

    /** File name for pixel standards: {@value}. */
    public static final String PIXELS_NAME = "pixels.dat";
}
