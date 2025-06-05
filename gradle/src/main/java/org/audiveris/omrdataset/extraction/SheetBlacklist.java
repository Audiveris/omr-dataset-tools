//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S h e e t B l a c k l i s t                                  //
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

import java.io.BufferedReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import org.audiveris.omr.util.FileUtil;
import static org.audiveris.omrdataset.training.App.SHEET_BLACKLIST_NAME;

/**
 * Class {@code SheetBlacklist} gathers at archive level the sheets to discard.
 *
 * @author Hervé Bitteur
 */
public class SheetBlacklist
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(SheetBlacklist.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** We just store the fileName with no extension at all. */
    private final TreeMap<Integer, String> int2Str = new TreeMap<>();
    //~ Constructors -------------------------------------------------------------------------------

    //~ Methods ------------------------------------------------------------------------------------
    public void marshal (Path outPath)
            throws IOException
    {
        try (PrintWriter pw = Utils.getPrintWriter(outPath)) {
            for (Map.Entry<Integer, String> entry : int2Str.entrySet()) {
                pw.println(entry.getKey() + "," + entry.getValue());
            }

            pw.flush();
        }
    }

    public synchronized boolean contains (int id)
    {
        if (int2Str.isEmpty()) {
            return false;
        }

        return int2Str.containsKey(id);
    }

    public synchronized boolean contains (Path path)
    {
        if (int2Str.isEmpty()) {
            return false;
        }

        String radix = FileUtil.avoidAnyExtension(path.getFileName().toString());
        return int2Str.containsValue(radix);
    }

    public synchronized void add (int id,
                                  Path path)
    {

        String radix = FileUtil.avoidAnyExtension(path.getFileName().toString());
        int2Str.put(id, radix);
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{size:").append(int2Str.size()).append('}');

        return sb.toString();
    }

    public static SheetBlacklist unmarshal (Path inPath)
            throws IOException
    {
        final SheetBlacklist sbl = new SheetBlacklist();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(Files.newInputStream(inPath), "UTF-8"))) {
            String line;

            while ((line = br.readLine()) != null) {
                final String[] tokens = line.split(",");
                final int id = Integer.parseInt(tokens[0]);
                final String str = tokens[1];
                sbl.int2Str.put(id, str);
            }
        }

        return sbl;
    }

    public static SheetBlacklist getSheetBlacklist (Path archiveFolder)
    {
        final Path blacklistPath = archiveFolder.resolve(SHEET_BLACKLIST_NAME);

        if (Files.exists(blacklistPath)) {
            try {
                return unmarshal(blacklistPath);
            } catch (IOException ex) {
                logger.warn("Error loading sheet blacklist {}", blacklistPath, ex);
                return null;
            }
        } else {
            return new SheetBlacklist(); // empty
        }
    }
    //~ Inner Classes ------------------------------------------------------------------------------
}
