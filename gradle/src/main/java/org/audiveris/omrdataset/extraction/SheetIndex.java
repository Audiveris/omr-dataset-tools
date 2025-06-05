//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S h e e t I n d e x                                      //
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
import static org.audiveris.omrdataset.training.App.ANNOTATIONS_FOLDER_NAME;
import static org.audiveris.omrdataset.training.App.INFO_EXT;
import static org.audiveris.omrdataset.training.App.SHEET_INDEX_NAME;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Class {@code SheetIndex} keeps an index of all sheets in a given archive, to allow
 * any sheet to be referenced via its ID in a feature line.
 *
 * @author Hervé Bitteur
 */
public class SheetIndex
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SheetIndex.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final TreeMap<Integer, String> int2Str = new TreeMap<>();

    private final Map<String, Integer> str2Int = new HashMap<>();

    private int lastId;

    //~ Constructors -------------------------------------------------------------------------------
    public SheetIndex ()
    {

    }

    //~ Methods ------------------------------------------------------------------------------------
    public synchronized Integer getId (Path path)
    {
        final String str = path.toAbsolutePath().toString();
        Integer id = str2Int.get(str);

        if (id == null) {
            id = ++lastId;
            str2Int.put(str, id);
            int2Str.put(id, str);
        }

        return id;
    }

    public synchronized Path getPath (int id)
    {
        String str = int2Str.get(id);
        if (str == null) {
            return null;
        }

        return Paths.get(str);
    }

    public void marshal (Path outPath)
            throws IOException
    {
        try (PrintWriter pw = Utils.getPrintWriter(outPath)) {
            for (Entry<Integer, String> entry : int2Str.entrySet()) {
                pw.println(entry.getKey() + "," + entry.getValue());
            }

            pw.flush();
        }
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{size:").append(int2Str.size()).append('}');

        return sb.toString();
    }

    public static SheetIndex unmarshal (Path inPath)
            throws IOException
    {
        final SheetIndex si = new SheetIndex();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(Files.newInputStream(inPath), "UTF-8"))) {
            String line;

            while ((line = br.readLine()) != null) {
                final String[] tokens = line.split(",");
                final int id = Integer.parseInt(tokens[0]);
                final String str = tokens[1];
                si.str2Int.put(str, id);
                si.int2Str.put(id, str);
            }
        }

        return si;
    }

    /**
     * Report the sheet index for the provided archive.
     *
     * @param archiveFolder provided archive folder
     * @return the archive index
     */
    public static SheetIndex getSheetIndex (Path archiveFolder)
    {
        StopWatch watch = new StopWatch("getSheetIndex");
        // If index exists on disk, simply load it
        final Path indexPath = archiveFolder.resolve(SHEET_INDEX_NAME);

        try {
            watch.start("Existence");
            if (Files.exists(indexPath)) {
                watch.start("Unmarshalling");
                SheetIndex index = unmarshal(indexPath);
                ///watch.print();
                return index;
            }
        } catch (IOException ex) {
            logger.warn("Error loading sheet index {}", indexPath, ex);
        }

        // Build the index from scratch
        final Path annotationsFolder = archiveFolder.resolve(ANNOTATIONS_FOLDER_NAME);

        try {
            final SheetIndex index = new SheetIndex();
            logger.info("Browsing annotations");
            watch.start("Browsing annotations");
            Files.walkFileTree(
                    annotationsFolder,
                    new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile (Path path,
                                                  BasicFileAttributes attrs)
                        throws IOException
                {
                    String fn = path.getFileName().toString();
                    if (fn.endsWith(INFO_EXT)) {
                        index.getId(path); // This populates the index
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

            try {
                // Save index on disk
                logger.info("Marshalling index");
                watch.start("Marshalling index");
                index.marshal(indexPath);
            } catch (IOException ex) {
                logger.warn("Error saving sheet index {}", indexPath, ex);
                return null;
            }

            ///watch.print();
            return index;
        } catch (IOException ex) {
            logger.warn("Error browsing annotations folder {}", annotationsFolder, ex);
        }

        return null;
    }
//
//    public static void main (String[] args)
//            throws Exception
//    {
//        // Just for testing
//        Path archivePath = Paths.get("D:\\soft\\mscore-dataset\\MuseScore\\archive-0");
//        SheetIndex si = getSheetIndex(archivePath);
//        logger.info("si: {}", si);
//    }
}
