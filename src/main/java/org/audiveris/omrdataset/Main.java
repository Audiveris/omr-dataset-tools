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
package org.audiveris.omrdataset;

import org.audiveris.omrdataset.api.OmrShapes;
import org.audiveris.omrdataset.train.Clean;
import org.audiveris.omrdataset.train.Features;
import org.audiveris.omrdataset.train.SubImages;
import org.audiveris.omrdataset.train.Training;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code Main} handles the whole processing from images and annotations inputs
 * to features, sub-images if desired, and classifier model.
 *
 * @author Hervé Bitteur
 */
public class Main
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /** CLI Parameters. */
    public static CLI cli;

    //~ Methods ------------------------------------------------------------------------------------
    public static void main (String[] args)
            throws Exception
    {
        cli = CLI.create(args);

        if (cli.help) {
            return; // Help has been printed by CLI itself
        }

        if (cli.outputFolder == null) {
            logger.warn("Output location not specified, please use -output option");

            return;
        }

        if (cli.names) {
            OmrShapes.printOmrShapes();
        }

        if (cli.clean) {
            new Clean().process();
        }

        if (cli.features) {
            // Extract features
            new Features().process();
        }

        if (cli.subimages) {
            // Extract subimages for visual check (not mandatory)
            new SubImages().process();
        }

        if (cli.training) {
            // Train the classifier
            new Training().process();
        }
    }
}
