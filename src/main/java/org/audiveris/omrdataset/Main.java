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
import org.audiveris.omrdataset.api.Context;
import org.audiveris.omrdataset.api.Context.ContextType;
import org.audiveris.omrdataset.api.GeneralContext;
import org.audiveris.omrdataset.api.HeadContext;
import org.audiveris.omrdataset.extraction.BinHistogram;
import org.audiveris.omrdataset.extraction.Inspection;
import org.audiveris.omrdataset.extraction.PatchGrids;
import org.audiveris.omrdataset.extraction.SourceInfo;
import org.audiveris.omrdataset.extraction.SourceInfo.UArchiveId;
import org.audiveris.omrdataset.extraction.ShapeGrids;
import org.audiveris.omrdataset.extraction.SheetProcessing;
import org.audiveris.omrdataset.extraction.ShuffleBins;
import org.audiveris.omrdataset.extraction.SourceInfo.UCollectionId;
import org.audiveris.omrdataset.extraction.Tally;
import org.audiveris.omrdataset.extraction.TallySplit;
import org.audiveris.omrdataset.training.Test;
import org.audiveris.omrdataset.training.Training;
import static org.audiveris.omrdataset.training.App.BINS_FOLDER_NAME;
import static org.audiveris.omrdataset.training.App.binName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Class {@code Main} handles the whole processing from images and annotations inputs
 * to features, patches if desired, and classifier model.
 * <p>
 * The application is meant to run on one context and one archive.
 * <ul>
 * <li>A <b>context</b> defines the list of possible shapes and the patch dimension to use.
 * As of this writing, there exists only the HEAD context, focused on a dozen of possible head
 * shapes. It uses a patch of W27xH21 pixels.
 * <li>An <b>archive</b> is a manageable part of a collection.
 * The DeepScores collection of 200_000 sheets is thus divided in 20 archives of 10000 sheets each.
 * The MuseScore collection contains 4000 sheets, in a single archive.
 * </ul>
 * On the command line, the context must be explicitly provided, and the archive is derived from
 * the input paths.
 *
 * @author Hervé Bitteur
 */
public class Main
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /** CLI Parameters. */
    public static CLI cli;

    /** Mandatory specified context. */
    public static Context context;

    /** Top folder for input source files, organized as collections. */
    public static UCollectionId uCollectionId;

    /** Archive within collection. */
    public static UArchiveId uArchiveId;

    /** Folder for the selected archive. */
    public static Path archiveFolder;

    /** Top folder for all archive data related to chosen context. */
    public static Path contextFolder;

    /** Folder for archive training set bins. */
    public static Path binsFolder;

    /** Number of processors available. */
    public static final int processors = Runtime.getRuntime().availableProcessors();

    /** Services for parallel processing, if soo desired. */
    public static ExecutorService executors;

    //~ Instance fields ----------------------------------------------------------------------------
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

        // Which context (head vs general)?
        context = (cli.contextType == ContextType.HEAD) ? HeadContext.INSTANCE
                : (cli.contextType == ContextType.GENERAL ? GeneralContext.INSTANCE : null);

        if (context == null) {
            logger.warn("Context not specified: HEAD or GENERAL. Exiting.");
            return;
        }

        if (cli.names) {
            // Print the ordered list of shapes handled by the selected context
            context.printLabels();
        }

        // Which archive (and thus collection) are we processing?
        uCollectionId = detectCollection();

        contextFolder = archiveFolder.resolve(context.toString());
        binsFolder = contextFolder.resolve(BINS_FOLDER_NAME);

        if (cli.parallel) {
            // Allocate processors for parallel processing
            logger.info("Processors: {}", processors);
            executors = Executors.newFixedThreadPool(processors);
        }

        if (cli.filter
                    || cli.nones || cli.histo || cli.control
                    || cli.features || cli.patches) {
            // Processing of sheets xml_annotations, perhaps in parallel, to provide:
            // -filter: filtered annotations from archive-N/xml_annotations
            //          TO: archive-N/filtered/<sheet>.filtered.xml
            //
            // -nones: addition of none annotations to filtered annotations
            //
            // -histo: printout of shapes histogram for the sheet
            //
            // -control: images with symbols boxes in blue and nones locations in red
            //          TO: archive-N/<CONTEXT>/control/<sheet>.control.png
            //
            // -features: compressed CSV file (shape + pixels) for every symbol/none
            //          TO: archive-N/<CONTEXT>/features/<sheet>.csv.zip
            //
            // -patches: patch images for visual check of every symbol
            //          TO: archive-N/<CONTEXT>/patches/<sheet>/{symbol-details}.png
            watch.start("Sheet processing");
            new SheetProcessing().process();
        }

        if (cli.tally) {
            // Dispatch sheet features to shapes tally, perhaps in parallel
            // TO: archive-N/<CONTEXT>/shapes/<shape>.csv.zip
            watch.start("Populate shapes tally");
            new Tally().process();
        }

        if (cli.shapeGrids) {
            // Build patch grids for all shapes, for visual check, perhaps in parallel
            // TO: archive-N/<CONTEXT>/shape_grids/<shape>/<seq>.png
            watch.start("Buid shape grids");
            new ShapeGrids().process();
        }

        if (cli.grids != null) {
            // Build grids on selected input files, perhaps in parallel
            // TO: archive-N/<CONTEXT>/grids/<shape>/<seq>.png
            watch.start("Buid user-selected grids");
            new PatchGrids(cli.grids).process();
        }

        if (cli.bins) {
            // Dispatch shape samples rather equally into bins, perhaps in parallel
            watch.start("Split tally to bins");
            new TallySplit().process();
        }

        if (executors != null) {
            executors.shutdown(); // End of possible parallel processing

            try {
                // Wait a while for existing tasks to terminate
                if (!executors.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("Executors did not terminate");

                    // (Try to) cancel currently executing tasks.
                    executors.shutdownNow();
                }
            } catch (InterruptedException ie) {
                // (Re-)Try to cancel if current thread also got interrupted
                executors.shutdownNow();

                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }

            logger.debug("Executors closed.");
        }

        if (cli.shuffle) {
            // Shuffle all samples within every bin
            watch.start("Shuffling bins");
            new ShuffleBins().process(); // Sequential because of memory constraints
        }

        if (cli.train != null) {
            // Sequential classifier training from selected bins
            watch.start("Training");
            new Training().process(cli.train);
        }

        if (cli.testPath != null) {
            // Test the model on the provided samples file
            watch.start("test");
            new Test().process(cli.testPath);
        }

        if (cli.binHisto != null) {
            // Print histogram of shapes in provided bin number(s)
            watch.start("binHisto");
            BinHistogram histo = new BinHistogram();
            histo.process(cli.binHisto);
            histo.print();
        }

        if (cli.inspect != null) {
            // Generate for inspection the patches for a bin and its selected iterations range
            final int size = cli.inspect.size();

            if (size < 2) {
                logger.warn("Missing info for inspect, usage: -inspect, bin#, iter#");
            } else {
                watch.start("Inspection");
                new Inspection(cli.inspect.get(0)).process(cli.inspect.subList(1, size));
            }
        }

        watch.print();
        logger.info("Exiting...");
        System.exit(0);
    }

    //------------------//
    // detectCollection //
    //------------------//
    /**
     * Detect the collection being processed, based on the first CLI argument (input file).
     *
     * @return the universal collection ID, or null
     */
    private UCollectionId detectCollection ()
            throws Exception
    {
        for (Path input : Main.cli.arguments) {
            if (!Files.exists(input)) {
                logger.warn("Could not find {}", input);
            } else {
                // Look up the parent archive folder and its sheet index
                uArchiveId = SourceInfo.lookupArchiveId(input);
                if (uArchiveId == null) {
                    logger.warn("No archive found at or above {}", input);
                    return null;
                }

                archiveFolder = SourceInfo.getPath(uArchiveId);
                return uArchiveId.getCollectionId();
            }
        }

        return null;
    }

    //------------//
    // processAll //
    //------------//
    /**
     * Process the provided task, either alone or using all processors.
     *
     * @param task the task to perform
     * @throws Exception
     */
    public static void processAll (Callable<Void> task)
            throws Exception
    {
        if (Main.cli.parallel) {
            final Collection<Callable<Void>> tasks = new ArrayList<>();

            for (int i = 0; i < processors; i++) {
                tasks.add(task);
            }

            executors.invokeAll(tasks);
        } else {
            task.call();
        }
    }

    //---------//
    // binPath //
    //---------//
    /**
     * Report the path to a specific bin number.
     *
     * @param bin specific bin number
     * @return path to this bin
     */
    public static Path binPath (int bin)
    {
        return binsFolder.resolve(binName(bin));
    }
}
