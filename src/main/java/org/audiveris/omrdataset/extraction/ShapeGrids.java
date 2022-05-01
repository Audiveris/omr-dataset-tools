//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S h a p e G r i d s                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omrdataset.extraction;

import org.audiveris.omr.util.StopWatch;
import org.audiveris.omrdataset.Main;
import org.audiveris.omrdataset.training.App;
import static org.audiveris.omrdataset.training.App.CSV_EXT;
import static org.audiveris.omrdataset.training.App.SHAPES_FOLDER_NAME;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Class {@code ShapeGrids} builds the patch grids for all shape samples in an archive.
 *
 * @author Hervé Bitteur
 */
public class ShapeGrids
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(ShapeGrids.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Current index in shapes. */
    private int nextShapeIndex = 0; ///1; // We skip the none shape

    //~ Constructors -------------------------------------------------------------------------------
    public ShapeGrids ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Process all tally files of archive to generate patch grids.
     */
    public void process ()
            throws Exception
    {
        StopWatch watch = new StopWatch("PatchGrid");

        watch.start("processAllShapes");
        processAllShapes();

        watch.print();
    }

    //-----------//
    // nextShape //
    //-----------//
    private synchronized Enum nextShape ()
    {
        final int numClasses = Main.context.getNumClasses();
        final Enum[] shapes = Main.context.getLabels();

        while (true) {
            if (nextShapeIndex >= numClasses) {
                return null;
            }

            Enum shape = shapes[nextShapeIndex++];

            logger.info("{}/{} PatchGrids for {}", nextShapeIndex, numClasses, shape);
            return shape;
        }
    }

    //------------------//
    // processAllShapes //
    //------------------//
    /**
     * Process all shapes, perhaps in parallel.
     */
    private void processAllShapes ()
            throws Exception
    {
        Main.processAll(new Callable<Void>()
        {
            @Override
            public Void call ()
                    throws Exception
            {
                while (true) {
                    Enum shape = nextShape();

                    if (shape == null) {
                        break;
                    }
                    processShape(shape);
                }
                return null;
            }
        });
    }

    //--------------//
    // processShape //
    //--------------//
    private void processShape (Enum shape)
    {
        final Path shapePath = Main.contextFolder.resolve(SHAPES_FOLDER_NAME)
                .resolve(shape + CSV_EXT);
        final Path gridsFolder = Main.contextFolder.resolve(App.SHAPE_GRIDS_FOLDER_NAME);

        try {
            new GridBuilder(shapePath, gridsFolder).process();
            logger.info("Completed {}", shape);
        } catch (Exception ex) {
            logger.warn("Error building grid for {}", shape, ex);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
}
