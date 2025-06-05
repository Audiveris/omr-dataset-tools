//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P a t c h G r i d s                                       //
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
import org.audiveris.omrdataset.api.Context;
import org.audiveris.omrdataset.api.Patch;
import static org.audiveris.omrdataset.api.Patch.parseWrongLabel;
import static org.audiveris.omrdataset.training.App.GRIDS_FOLDER_NAME;
import static org.audiveris.omrdataset.training.App.WRONG_COLOR;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import static org.audiveris.omrdataset.api.Patch.parseLabel;

/**
 * Class {@code PatchGrids} generates patch grids on provided input files.
 *
 * @author Hervé Bitteur
 */
public class PatchGrids
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(PatchGrids.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final List<Path> inPaths;

    private int inputIndex;

    //~ Constructors -------------------------------------------------------------------------------
    public PatchGrids (List<Path> inPaths)
    {
        this.inPaths = inPaths;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Process all input files.
     */
    public void process ()
            throws Exception
    {
        StopWatch watch = new StopWatch("PatchGrids");

        watch.start("processAllFiles");
        processAllInputs();

        watch.print();
    }

    //-----------//
    // nextInput //
    //-----------//
    private synchronized Path nextInput ()
    {
        final int size = inPaths.size();

        if (inputIndex >= size) {
            return null;
        }

        Path path = inPaths.get(inputIndex++);
        logger.info("{}/{} PatchGrids for {}", inputIndex, size, path);

        return path;
    }

    //------------------//
    // processAllInputs //
    //------------------//
    /**
     * Process all inputs, perhaps in parallel.
     */
    private void processAllInputs ()
            throws Exception
    {
        Main.processAll(new Callable<Void>()
        {
            @Override
            public Void call ()
                    throws Exception
            {
                while (true) {
                    Path path = nextInput();

                    if (path == null) {
                        break;
                    }
                    processInput(path);
                }
                return null;
            }
        });
    }

    //--------------//
    // processInput //
    //--------------//
    private void processInput (Path inPath)
            throws Exception
    {
        Path outFolder = inPath.getParent().resolveSibling(GRIDS_FOLDER_NAME);
        new MyGridBuilder(inPath, outFolder).process();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------------//
    // MyGridBuilder //
    //---------------//
    private static class MyGridBuilder
            extends GridBuilder
    {

        private static final int LABEL_DY = 16;

        public MyGridBuilder (Path inPath,
                              Path outFolder)
        {
            super(inPath, outFolder);
        }

        @Override
        protected void drawOptions (Graphics2D g,
                                    String[] cols,
                                    Patch.UPatch uPatch)
        {
            if (cols.length > Main.context.getCsv(Context.Metadata.WRONG_LABEL)) {
                // Draw name of wrong shape at bottom of patch
                Enum wrongLabel = parseWrongLabel(cols, Main.context);
                g.setColor(WRONG_COLOR);
                g.drawString(wrongLabel.toString(), 0, Main.context.getContextHeight() + LABEL_DY);
            } else {
                Enum label = parseLabel(cols, Main.context);
                String str = label.toString().replace("notehead", "-");
                g.drawString(str, 0, Main.context.getContextHeight() + LABEL_DY);
            }
        }
    }
}
