//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            T a l l y                                           //
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
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.ZipWrapper;
import org.audiveris.omrdataset.Main;
import static org.audiveris.omrdataset.api.Patch.parseLabel;
import org.audiveris.omrdataset.training.App;
import static org.audiveris.omrdataset.training.App.CSV_EXT;
import static org.audiveris.omrdataset.training.App.SHAPES_FOLDER_NAME;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Class {@code Tally} dispatch each input feature according to its shape field.
 * <p>
 * We build one tally per archive (as ZHAW did).
 * <p>
 * We CANNOT easily populate the tally incrementally, because of the use of Zip files with entry
 * size larger than Integer.MAX_VALUE.
 * The only way to append to such a large zip file would be to use temporary uncompressed files.
 * So, we have decided that building the tally is a one shot operation.
 * <p>
 * This operation can run in sequence, to ease the visual inspection of each shape.
 * If shuffling is needed, it can be done later on training bins.
 * <p>
 * It can also run in parallel.
 *
 * @author Hervé Bitteur
 */
public class Tally
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(Tally.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** List of all sheet features files. */
    private List<Path> allSheetFeatures;

    /** One writer per shape file. */
    private final Map<Object, PrintWriter> shapeWriters
            = new EnumMap<>(Main.context.getLabelClass());

    /** Current index in sheets sequence. */
    private int nextSheetIndex;

    //~ Constructors -------------------------------------------------------------------------------
    public Tally ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    public void process ()
            throws Exception
    {
        StopWatch watch = new StopWatch("Tally");
        watch.start("getAllSheetFeatures");
        allSheetFeatures = getAllSheetFeatures();

        watch.start("getTallyWriters");
        getTallyWriters();

        watch.start("processSheets");
        processAllSheets();

        // Close writers
        watch.start("Close writers");
        for (PrintWriter bw : shapeWriters.values()) {
            bw.flush();
            bw.close();
        }

        watch.print();
    }

    //------------------//
    // processAllSheets //
    //------------------//
    /**
     * Process all sheets in sequence.
     */
    private void processAllSheets ()
            throws Exception
    {
        Main.processAll(new Callable<Void>()
        {
            @Override
            public Void call ()
                    throws Exception
            {
                while (true) {
                    Path path = nextSheet();

                    if (path == null) {
                        break;
                    }
                    processSheet(path);
                }
                return null;
            }
        });
    }

    //-----------//
    // nextSheet //
    //-----------//
    private synchronized Path nextSheet ()
    {
        final int size = allSheetFeatures.size();

        while (true) {
            if (nextSheetIndex >= size) {
                return null;
            }

            Path path = allSheetFeatures.get(nextSheetIndex++);

            logger.info("{}/{} Tally for {}", nextSheetIndex, size, path);
            return path;
        }
    }

    //--------------//
    // processSheet //
    //--------------//
    private void processSheet (Path sheetFeaturesPath)
    {
        try {
            final ZipWrapper zin = ZipWrapper.open(sheetFeaturesPath);
            try (final BufferedReader br = zin.newBufferedReader()) {
                String line;

                while ((line = br.readLine()) != null) {
                    final String[] cols = line.split(",");
                    Enum label = parseLabel(cols, Main.context);
                    final PrintWriter writer = shapeWriters.get(label);
                    writer.println(line);
                }
            }

            zin.close();
        } catch (Exception ex) {
            logger.warn("Error in processing {}", sheetFeaturesPath, ex.toString(), ex);
        }
    }

    //-----------------//
    // getTallyWriters //
    //-----------------//
    private void getTallyWriters ()
            throws IOException
    {
        final Path shapesFolder = Main.contextFolder.resolve(SHAPES_FOLDER_NAME);
        Files.createDirectories(shapesFolder);

        for (Enum label : Main.context.getLabels()) {
            final Path shapePath = shapesFolder.resolve(label + CSV_EXT);
            final ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(shapePath));
            zos.putNextEntry(new ZipEntry(FileUtil.getNameSansExtension(shapePath)));
            PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(zos, UTF_8)));

            shapeWriters.put(label, pw);
        }
    }

    //---------------------//
    // getAllSheetFeatures //
    //---------------------//
    private List<Path> getAllSheetFeatures ()
            throws IOException
    {
        final List<Path> list = new ArrayList<>();

        // Detect the provided archive
        for (Path input : Main.cli.arguments) {
            if (!Files.exists(input)) {
                logger.warn("Could not find {}", input);
            }
        }

        final Path featuresFolder = Main.contextFolder.resolve(App.FEATURES_FOLDER_NAME);
        final SheetBlacklist sheetBlacklist = SheetBlacklist.getSheetBlacklist(Main.archiveFolder);
        logger.info("Building tally from {}", featuresFolder);

        Files.walkFileTree(featuresFolder,
                           new SimpleFileVisitor<Path>()
                   {
                       @Override
                       public FileVisitResult visitFile (Path path,
                                                         BasicFileAttributes attrs)
                               throws IOException
                       {
                           // We look for "SHEET.csv.zip"
                           final String fn = path.getFileName().toString();

                           if (fn.endsWith(App.CSV_EXT)) {
                               // Check sheet is not blacklisted
                               if (!sheetBlacklist.contains(path)) {
                                   list.add(path);
                               } else {
                                   logger.info("Skipped blacklisted {}", path);
                               }
                           }

                           return FileVisitResult.CONTINUE;
                       }
                   });

        logger.info("Browsed sheet features files: {}", list.size());

        return list;

    }
    //~ Inner Classes ------------------------------------------------------------------------------
}
