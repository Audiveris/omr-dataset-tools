//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S h e e t S p l i t                                      //
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

import org.audiveris.omr.util.ZipWrapper;
import org.audiveris.omrdataset.Main;
import static org.audiveris.omrdataset.training.App.BIN_COUNT;
import static org.audiveris.omrdataset.training.App.CSV_EXT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class {@code SheetSplit} collects all sheet features and split them into a small number
 * of bin-xx.csv.zip files, so that each file can be separately fully loaded in memory.
 * <p>
 * Randomization is implemented at two levels:
 * <ol>
 * <li>Each feature line is dispatched to a randomly chosen bin.
 * <li>Each bin is shuffled (thus it needs to hold in memory).
 * </ol>
 *
 * @author Hervé Bitteur
 */
@Deprecated // Replaced by split from the shape tally (see TallySplit)
public class SheetSplit
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SheetSplit.class);

    private static final Pattern BIN_PATTERN = Pattern.compile("bin-[0-9]+\\.zip");

    //~ Instance fields ----------------------------------------------------------------------------
    /** List of all sheet features files. */
    private List<Path> allSheetFeatures;

    /** One writer per bin file. */
    private List<ZipPrintWriter> binWriters;

    /** Current index in sheets sequence. */
    private int nextSheetIndex;

    //~ Constructors -------------------------------------------------------------------------------
    public SheetSplit ()
    {

    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Process all sources to populate the features bins.
     * <ol>
     * <li>Allocate brand new bins.
     * <li>Retrieve the list of all sheets to be processed
     * <li>Save the global map of sheet IDs
     * <li>Then in parallel, for every sheet:
     * <ol>
     * <li>Assign a unique ID to the sheet
     * <li>Read every sheet features, append sheet ID and dispatch to bins
     * </ol>
     * <li>Shuffle every bin (or do this before any training?)
     * </ol>
     */
    public void process ()
            throws Exception
    {
        binWriters = allocateBins();

        allSheetFeatures = getAllSheetFeatures();

        ///saveSheetIndex();
        processSheets(); // Perhaps in parallel

        // Close all bins
        for (ZipPrintWriter zpw : binWriters) {
            zpw.pw.flush();
            zpw.pw.close();
            zpw.wrapper.close();
        }
    }

    //-------------------//
    // getNextSheetIndex //
    //-------------------//
    public synchronized int getNextSheetIndex ()
    {
        if (nextSheetIndex >= allSheetFeatures.size()) {
            return -1;
        }

        return nextSheetIndex++;
    }

    //--------------//
    // processSheet //
    //--------------//
    private void processSheet (int sheetId)
    {
        final Random random = new Random();
        final Path virtualPath = allSheetFeatures.get(sheetId - 1);
        logger.info("Split {} {}", sheetId, virtualPath);

        try {
            final ZipWrapper zin = ZipWrapper.open(virtualPath);
            try (InputStream is = zin.newInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {

                String line;

                while ((line = br.readLine()) != null) {
                    // Append sheet id
                    line = line + ',' + sheetId;

                    // Choose a random bin writer
                    int ib = random.nextInt(BIN_COUNT);
                    PrintWriter pw = binWriters.get(ib).pw;
                    pw.println(line);
                }
            }

            zin.close();
        } catch (Exception ex) {
            logger.warn("Error in processing {} {}", sheetId, ex.toString(), ex);
        }
    }

    //---------------//
    // processSheets //
    //---------------//
    /**
     * Process every sheet, perhaps in parallel.
     */
    private void processSheets ()
            throws Exception
    {
        Main.processAll(new Callable<Void>()
        {
            @Override
            public Void call ()
                    throws Exception
            {
                while (true) {
                    int idx = getNextSheetIndex();

                    if (idx == -1) {
                        break;
                    }

                    final int sheetId = idx + 1;
                    processSheet(sheetId);
                }

                return null;
            }
        });
    }

    //--------------//
    // allocateBins //
    //--------------//
    private List<ZipPrintWriter> allocateBins ()
            throws IOException
    {
        // Remove existing bins if any
        List<Path> allBins = getAllBins();

        for (Path path : allBins) {
            Files.delete(path);
        }

        // Allocate brand new files with their PrintWriter
        List<ZipPrintWriter> list = new ArrayList<>();

        for (int b = 1; b <= BIN_COUNT; b++) {
            ZipWrapper wrapper = ZipWrapper.create(Main.binPath(b));
            ZipPrintWriter zpw = new ZipPrintWriter(wrapper, wrapper.newPrintWriter());
            list.add(zpw);
        }

        logger.info("Created bins: {}", list.size());

        return list;
    }

    //---------------------//
    // getAllSheetFeatures //
    //---------------------//
    private List<Path> getAllSheetFeatures ()
            throws IOException
    {
        final List<Path> list = new ArrayList<>();

        // Scan the provided inputs (which can be simple files or folders)
        for (Path input : Main.cli.arguments) {
            if (!Files.exists(input)) {
                logger.warn("Could not find {}", input);
            } else {
                Files.walkFileTree(input,
                                   new SimpleFileVisitor<Path>()
                           {
                               @Override
                               public FileVisitResult visitFile (Path path,
                                                                 BasicFileAttributes attrs)
                                       throws IOException
                               {
                                   // We look for "SHEET.csv.zip"
                                   String fn = path.getFileName().toString();
                                   if (fn.endsWith(CSV_EXT)) {
                                       list.add(path);
                                   }

                                   return FileVisitResult.CONTINUE;
                               }
                           });
            }
        }

        logger.info("Browsed sheet features files: {}", list.size());

        return list;
    }

    //------------//
    // getAllBins //
    //------------//
    public static List<Path> getAllBins ()
            throws IOException
    {
        final List<Path> allBins = new ArrayList<>();

        if (Files.exists(Main.binsFolder)) {
            Files.walkFileTree(
                    Main.binsFolder,
                    new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile (Path path,
                                                  BasicFileAttributes attrs)
                        throws IOException
                {
                    // We look for files whose name matches BIN_PATTERN
                    final String fn = path.getFileName().toString();
                    final Matcher m = BIN_PATTERN.matcher(fn);

                    if (m.matches()) {
                        allBins.add(path);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return allBins;
    }

    //----------------//
    // ZipPrintWriter //
    //----------------//
    private static class ZipPrintWriter
    {

        public final ZipWrapper wrapper;

        public final PrintWriter pw;

        public ZipPrintWriter (ZipWrapper wrapper,
                               PrintWriter pw)
        {
            this.wrapper = wrapper;
            this.pw = pw;
        }
    }
}
