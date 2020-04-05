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

import org.audiveris.omr.util.StopWatch;
import org.audiveris.omrdataset.api.OmrShapes;
import static org.audiveris.omrdataset.training.App.INFO_EXT;
import static org.audiveris.omrdataset.training.App.SHEETS_MAP_PATH;
import org.audiveris.omrdataset.extraction.BinHistogram;
import org.audiveris.omrdataset.extraction.Inspection;
import org.audiveris.omrdataset.extraction.Limit;
import org.audiveris.omrdataset.extraction.SheetProcessor;
import org.audiveris.omrdataset.extraction.Split;
import org.audiveris.omrdataset.extraction.ShuffleBins;
import org.audiveris.omrdataset.training.Test;
import org.audiveris.omrdataset.training.Training;
import org.audiveris.omrdataset.extraction.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class {@code Main} handles the whole processing from images and annotations inputs
 * to features, patches if desired, and classifier model.
 *
 * @author Hervé Bitteur
 */
public class Main
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /** CLI Parameters. */
    public static CLI cli;

    public static final int processors = Runtime.getRuntime().availableProcessors();

    public static ExecutorService executors;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sequence of sheets (SHEET.xml) to be processed. */
    private List<Path> sheetList;

    /** Current index in sheets sequence. */
    private int nextSheetIndex;

    //~ Constructors -------------------------------------------------------------------------------
    private Main ()
    {

    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // main //
    //------//
    public static void main (String[] args)
            throws Exception
    {
        logger.info("{}", LocalDateTime.now());
        cli = CLI.create(args);

        if (cli.help) {
            return; // Help has been printed by CLI itself
        }

        if (cli.names) {
            OmrShapes.printOmrShapes();
        }

        new Main().process();
        logger.info("{}", LocalDateTime.now());
    }

    //---------//
    // process //
    //---------//
    public void process ()
            throws Exception
    {
        final StopWatch watch = new StopWatch("Main");

        if (cli.parallel && ((cli.source != null) || cli.nones || cli.features || cli.split)) {
            logger.info("Processors: {}", processors);
            executors = Executors.newFixedThreadPool(processors);
        }

        if ((cli.source != null) || cli.nones || cli.features) {
            watch.start("Building sheet list");
            sheetList = buildSheetList(); // Build list of source sheets ([.filtered].xml)
            saveSheetList(); // For convenience, not really needed

            watch.start("Processing sheets");
            processSheets(); // Run one SheetProcessor per sheet, perhaps in parallel
        }

        if (cli.split) {
            watch.start("Split to bins");
            new Split().process(); // Split of sheet features into bins, perhaps in parallel
        }

        if (executors != null) {
            executors.shutdown(); // End of possible parallel processing
        }

        if (cli.shuffle) {
            watch.start("Shuffling bins");
            new ShuffleBins().process(); // Sequential because of memory constraints
        }

        if (cli.limit) {
            watch.start("Limit");
            new Limit().process();
        }

        if (cli.train != null) {
            watch.start("Training");
            new Training().process(cli.train); // Sequential classifier training from selected bins
        }

        if (cli.test) {
            watch.start("test");
            new Test().process();
        }

        if (cli.binHisto != null) {
            watch.start("binHisto");
            BinHistogram histo = new BinHistogram();
            histo.process(cli.binHisto);
            histo.print();
        }

        if (cli.inspect != null) {
            final int size = cli.inspect.size();

            if (size < 2) {
                logger.warn("Missing info for inspect, usage: -inspect, bin#, iter#");
            } else {
                watch.start("Inspection");
                new Inspection(cli.inspect.get(0)).process(cli.inspect.subList(1, size));
            }
        }

        watch.print();
    }

    //----------------//
    // buildSheetList //
    //----------------//
    /**
     * Build the list of sheets to be processed.
     * <p>
     * This is a list of paths to SHEET.xml files, which in turn allows to access the related SHEET
     * files such as SHEET.filtered.xml, SHEET.png, etc.
     *
     * @return the list of sheet paths
     */
    private List<Path> buildSheetList ()
    {
        final List<Path> list = new ArrayList<>();

        try {
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
                            // NOTA: We look for *plain* "foo.xml" files
                            // Not "foo.tablatures.xml"
                            // Not "foo.filtered.xml"
                            String fn = path.getFileName().toString();
                            if (fn.endsWith(INFO_EXT)
                                        && !fn.endsWith(".tablatures.xml")
                                        && !fn.endsWith(".filtered.xml")) {
                                list.add(path);
                            }

                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            }
        } catch (IOException ex) {
            logger.warn("Error building sheet list", ex);
        }

        return list;
    }

    //-------------------//
    // getNextSheetIndex //
    //-------------------//
    public synchronized int getNextSheetIndex ()
    {
        if (nextSheetIndex >= sheetList.size()) {
            return -1;
        }

        return nextSheetIndex++;
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
            {
                while (true) {
                    int idx = getNextSheetIndex();

                    if (idx == -1) {
                        break;
                    }

                    final Path sheetPath = sheetList.get(idx);
                    final int sheetId = idx + 1;

                    new SheetProcessor(sheetId, sheetPath).process();
                }

                return null;
            }

        };

        if (cli.parallel) {
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

    //---------------//
    // saveSheetList //
    //---------------//
    /**
     * Print sheet list to CSV file.
     */
    private void saveSheetList ()
            throws IOException
    {
        // Header comment line sheet CSV file
        try (PrintWriter sheetFile = Utils.getPrintWriter(SHEETS_MAP_PATH)) {
            // Header comment line sheet CSV file
            sheetFile.println("# sheetId, sheetPath");

            for (int i = 0; i < sheetList.size(); i++) {
                final Path path = sheetList.get(i);
                final int id = i + 1;
                sheetFile.print(id);
                sheetFile.print(",");
                sheetFile.print(path);
                sheetFile.println();
                logger.info("sheetId:{} {}", id, path);
            }

            sheetFile.flush();
        }
    }
}
