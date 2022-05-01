//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S h u f f l e B i n s                                     //
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
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class {@code ShuffleBins} shuffles every bin, one after the other in memory.
 *
 * @author Hervé Bitteur
 */
public class ShuffleBins
{

    private static final Logger logger = LoggerFactory.getLogger(ShuffleBins.class);

    public void process ()
            throws Exception
    {
        if (!Files.exists(Main.binsFolder)) {
            logger.warn("Folder {} does not exist", Main.binsFolder);
            return;
        }

        final List<Path> allBins = getAllBins();
        final StopWatch watch = new StopWatch("ShuffleBins");

        // Shuffle every bin
        for (Path binPath : allBins) {
            logger.info("Shuffling {}", binPath);
            watch.start("Loading " + binPath);
            final List<String> lines = new ArrayList<>();

            final ZipWrapper zin = ZipWrapper.open(binPath);
            try (InputStream is = zin.newInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            }

            zin.close();

            watch.start("Shuffling " + binPath);
            Collections.shuffle(lines);

            ZipWrapper.delete(binPath); // Safer?

            watch.start("Rewriting " + binPath);
            final ZipWrapper zout = ZipWrapper.create(binPath);
            try (PrintWriter writer = zout.newPrintWriter()) {
                for (String line : lines) {
                    writer.println(line);
                }

                writer.flush();
            }

            zout.close();
        }

        watch.print();
    }

    //------------//
    // getAllBins //
    //------------//
    public static List<Path> getAllBins ()
            throws IOException
    {
        final Pattern BIN_PATTERN = Pattern.compile("bin-[0-9]+\\.csv\\.zip");
        final List<Path> allBins = new ArrayList<>();
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

        return allBins;
    }
}
