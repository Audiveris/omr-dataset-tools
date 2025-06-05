//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S h e e t P r o c e s s i n g                                 //
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

import org.audiveris.omrdataset.Main;
import static org.audiveris.omrdataset.Main.processAll;
import static org.audiveris.omrdataset.training.App.INFO_EXT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Class {@code SheetProcessing} processes input sheets (annotation files) to filter
 * annotations, inject none samples and extract sample features.
 *
 * @author Hervé Bitteur
 */
public class SheetProcessing
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(SheetProcessing.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sequence of sheets to be processed. */
    private List<SourceInfo.USheetId> sheetInputList;

    /** Current index in sheet input sequence. */
    private int nextSheetInput;

    //~ Constructors -------------------------------------------------------------------------------
    //~ Methods ------------------------------------------------------------------------------------
    public void process ()
            throws Exception
    {
        logger.info("Building sheet list");
        sheetInputList = buildSheetList(); // Build list of source sheets (SHEET.xml)
        final SheetBlacklist sheetBlacklist = SheetBlacklist.getSheetBlacklist(Main.archiveFolder);

        logger.info("Processing sheet list size:{}", sheetInputList.size());
        processAll(new Callable<Void>()
        {
            @Override
            public Void call ()
            {
                while (true) {
                    final SourceInfo.USheetId uSheetId = nextSheetInput();
                    if (uSheetId == null) {
                        break;
                    }

                    // Check this sheet is not blacklisted
                    if (!sheetBlacklist.contains(uSheetId.sheetId)) {
                        new SheetProcessor(uSheetId).process();
                    } else {
                        logger.info("Skipping blacklisted {}", uSheetId);
                    }
                }

                return null;
            }
        });
    }

    //----------------//
    // nextSheetInput //
    //----------------//
    private synchronized SourceInfo.USheetId nextSheetInput ()
    {
        if (nextSheetInput >= sheetInputList.size()) {
            return null;
        }

        return sheetInputList.get(nextSheetInput++);
    }

    //----------------//
    // buildSheetList //
    //----------------//
    /**
     * Build the list of sheets to be processed.
     *
     * @return the list of universal sheet IDs to process
     */
    private List<SourceInfo.USheetId> buildSheetList ()
    {
        final List<SourceInfo.USheetId> list = new ArrayList<>();

        try {
            // Scan the provided inputs (which can be simple files or folders)
            for (Path input : Main.cli.arguments) {
                if (!Files.exists(input)) {
                    logger.warn("Could not find {}", input);
                } else {
                    // Look up the parent archive folder and its sheet index
                    SourceInfo.UArchiveId uArchiveId = SourceInfo.lookupArchiveId(input);
                    if (uArchiveId == null) {
                        logger.warn("Skipping {}", input);
                        continue;
                    }

                    SheetIndex sheetIndex = SourceInfo.getSheetIndex(uArchiveId);

                    // Then process the input (file or folder)
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
                                int sheetId = sheetIndex.getId(path);
                                list.add(new SourceInfo.USheetId(uArchiveId, sheetId));
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

    //~ Inner Classes ------------------------------------------------------------------------------
}
