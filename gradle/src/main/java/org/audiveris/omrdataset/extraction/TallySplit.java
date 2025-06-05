//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       T a l l y S p l i t                                      //
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
import org.audiveris.omr.util.ZipWrapper;
import org.audiveris.omrdataset.Main;
import static org.audiveris.omrdataset.training.App.ARCHIVE_MAX_SHAPE_SAMPLES;
import static org.audiveris.omrdataset.training.App.BIN_COUNT;
import static org.audiveris.omrdataset.training.App.CSV_EXT;
import static org.audiveris.omrdataset.training.App.SHAPES_FOLDER_NAME;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Class {@code TallySplit} splits the tally of one archive to a fixed number of bins
 * suitable for batch training.
 *
 * @author Hervé Bitteur
 */
public class TallySplit
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(TallySplit.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** One ZipWrapper per bin file. */
    private final List<ZipWrapper> binZips = new ArrayList<>();

    /** One writer per bin file. */
    private final List<PrintWriter> binWriters = new ArrayList<>();

    /** Current index in shapes. */
    private int nextShapeIndex;

    //~ Constructors -------------------------------------------------------------------------------
    public TallySplit ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Process all tally files of archive to populate training bins.
     * <p>
     * Delete and reallocate all bin files.
     * Then, perhaps in parallel, for every shape:
     * <ol>
     * <li>Determine a "stride value" according to file size/line count of the shape file
     * <li>Read shape feature line on every stride
     * <li>Dispatch line to a bin randomly chosen
     * </ol>
     */
    public void process ()
            throws Exception
    {
        logger.info("Max shape samples: {} estimated max compressed bytes/Sample: {}",
                    ARCHIVE_MAX_SHAPE_SAMPLES, Main.context.getMaxPatchCompressedSize());
        StopWatch watch = new StopWatch("TallySplit");
        watch.start("allocateBins");
        allocateBins();

        watch.start("processAllShapes");
        processAllShapes();

        // Close writers
        watch.start("Close writers");
        for (Writer bw : binWriters) {
            bw.flush();
            bw.close();
        }

        // Close zips
        watch.start("Close zips");
        for (ZipWrapper wrapper : binZips) {
            wrapper.close();
        }

        watch.print();

    }

    //--------------//
    // allocateBins //
    //--------------//
    private void allocateBins ()
            throws IOException
    {
        for (int b = 1; b <= BIN_COUNT; b++) {
            final ZipWrapper wrapper = ZipWrapper.create(Main.binPath(b));
            binZips.add(wrapper);
            binWriters.add(wrapper.newPrintWriter());
        }
    }

    //-----------//
    // nextShape //
    //-----------//
    private synchronized Enum nextShape ()
    {
        final int numClasses = Main.context.getNumClasses();
        final Enum[] labels = Main.context.getLabels();

        while (true) {
            if (nextShapeIndex >= numClasses) {
                return null;
            }

            return labels[nextShapeIndex++];
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
        final Random random = new Random();
        final Path shapePath = Main.contextFolder.resolve(SHAPES_FOLDER_NAME)
                .resolve(shape + CSV_EXT);

        try {
            // We need an underestimate of the number of lines in this shape.csv.zip file
            final long size = Files.size(shapePath);
            final int patchCompSize = Main.context.getMaxPatchCompressedSize();
            final double gCount = Math.max(0, (size - 200) / (double) patchCompSize);
            final int stride = Math.max(1, (int) Math.rint(gCount / ARCHIVE_MAX_SHAPE_SAMPLES));
            final int offset = random.nextInt(stride);
            final ZipWrapper zin = ZipWrapper.open(shapePath);

            int idx = 0; // Current line index in input file
            int picked = 0; // Count of lines copied to bin so far

            try (BufferedReader br = zin.newBufferedReader()) {
                String line;

                while ((line = br.readLine()) != null) {
                    if ((idx - offset) % stride == 0) {
                        // OK, we pick up this line
                        // Now let us choose a bin writer randomly
                        int ib = random.nextInt(BIN_COUNT);
                        PrintWriter writer = binWriters.get(ib);
                        writer.println(line);

                        if (++picked >= ARCHIVE_MAX_SHAPE_SAMPLES) {
                            break; // Quorum reached
                        }
                    }

                    idx++;
                }
            }

            logger.info(
                    String.format(
                            "%30s size:%10d gCount:%8.0f stride:%4d offset:%4d idx:%8d picked:%5d %.0f",
                            shape, size, gCount, stride, offset, idx, picked, (double) size / idx));

            zin.close();
        } catch (Exception ex) {
            logger.warn("Error in processing {} {}", shapePath, ex.toString(), ex);
        }
    }
    //~ Inner Classes ------------------------------------------------------------------------------
}
