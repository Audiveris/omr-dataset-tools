//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            S p l i t                                           //
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

import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.ZipWrapper;
import org.audiveris.omrdataset.Main;
import static org.audiveris.omrdataset.Main.executors;
import static org.audiveris.omrdataset.Main.processors;
import static org.audiveris.omrdataset.training.App.BINS_PATH;
import static org.audiveris.omrdataset.training.App.BIN_COUNT;
import static org.audiveris.omrdataset.training.App.FEATURES_ZIP;
import static org.audiveris.omrdataset.training.App.SHEETS_MAP_PATH;

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
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class {@code Split} collects all sheet features and split them into a small number of
 * bin-xx.zip csv files, so that each file can be separately fully loaded in memory.
 * <p>
 * Randomization is implemented at two levels:
 * <ol>
 * <li>Each feature line is dispatched to a randomly chosen bin.
 * <li>Each bin is shuffled (thus it needs to hold in memory).
 * </ol>
 *
 * @author Hervé Bitteur
 */
public class Split
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Split.class);

    private static final Pattern BIN_PATTERN = Pattern.compile("bin-[0-9]+\\.zip");

    //~ Instance fields ----------------------------------------------------------------------------
    /** List of all sheet features files. */
    private List<Path> allSheetFeatures;

    /** One writer per bin file. */
    private List<ZipPrintWriter> binWriters;

    /** Current index in sheets sequence. */
    private int nextSheetIndex;

    //~ Constructors -------------------------------------------------------------------------------
    public Split ()
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

        saveSheetIndex();

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
        logger.info("Split sheetId: {} (zipped) {}", sheetId, virtualPath);

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

        final Callable task = new Callable()
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
        };

        if (Main.cli.parallel) {
            final Collection<Callable<Void>> tasks = new ArrayList<>();

            for (int i = 0; i < processors; i++) {
                tasks.add(task);
            }

            logger.info("Start of parallel processing of sheets");
            executors.invokeAll(tasks);
            logger.info("End of parallel processing of sheets");
        } else {
            logger.info("Start of sequential processing of sheets");
            task.call();
            logger.info("End of sequential processing of sheets");
        }
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
            String radix = String.format("bin-%02d", b);
            ZipWrapper wrapper = ZipWrapper.create(BINS_PATH.resolve(radix + ".csv"));
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
                Files.walkFileTree(
                        input,
                        new SimpleFileVisitor<Path>()
                {
                    @Override
                    public FileVisitResult visitFile (Path path,
                                                      BasicFileAttributes attrs)
                            throws IOException
                    {
                        // We look for "SHEET.features.zip", and store "SHEET.features.csv"
                        String fn = path.getFileName().toString();
                        if (fn.endsWith(FEATURES_ZIP)) {
                            String newName = FileUtil.getNameSansExtension(path) + ".csv";
                            list.add(path.resolveSibling(newName));
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

        if (Files.exists(BINS_PATH)) {
            Files.walkFileTree(
                    BINS_PATH,
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
    // saveSheetIndex //
    //----------------//
    /**
     * From the list of feature files, build the index of sheets.
     *
     * @return the sheet index
     * @throws Exception if anything goes wrong
     */
    private SheetIndex saveSheetIndex ()
            throws Exception
    {
        SheetIndex index = new SheetIndex();

        for (Path path : allSheetFeatures) {
            index.getId(path);
        }

        index.marshal(SHEETS_MAP_PATH);

        logger.info("Global sheet map saved at {}", SHEETS_MAP_PATH);
        return index;
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
