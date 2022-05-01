//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    D i s p a t c h F i l e s                                   //
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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.audiveris.omr.util.FileUtil;
import static org.audiveris.omrdataset.training.App.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code DispatchFiles} dispatches every top-level file to its related dir
 * created on demand.
 * <p>
 * For example file foo-12.png is moved to foo/foo-12.png
 * <p>
 * NOTA: This class seems hard-coded for the MuseScore archive, perhaps to organize MuseScore
 * files in a manner similar to DeepScore archives.
 *
 * @author Hervé Bitteur
 */
public class DispatchFiles
{

    private static final Logger logger = LoggerFactory.getLogger(DispatchFiles.class);

    // HARD-CODED !!!!!!
    private static final Path FOLDER = Paths.get("D:\\soft\\mscore-dataset\\MuseScore\\archive-0"); // hard coded!

    private static final Path controlPath = FOLDER.resolve(CONTROL_FOLDER_NAME);

    private static final Path featuresPath = FOLDER.resolve(FEATURES_FOLDER_NAME);

    private static final Path filteredPath = FOLDER.resolve(FILTERED_FOLDER_NAME);

    private static final Path imagesPath = FOLDER.resolve(IMAGES_FOLDER_NAME);

    private static final Path omrPath = FOLDER.resolve(OMR_FOLDER_NAME);

    private static final Path tablaturesPath = FOLDER.resolve(TABLATURES_FOLDER_NAME);

    private static final Path annotationsPath = FOLDER.resolve(ANNOTATIONS_FOLDER_NAME);

    public static void main (String[] args)
            throws IOException
    {
        new DispatchFiles().process();
    }

    public void process ()
            throws IOException
    {
        Files.walkFileTree(
                FOLDER,
                new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory (Path dir,
                                                      BasicFileAttributes attrs)
                    throws IOException
            {
                if (dir.equals(FOLDER)) {
                    return FileVisitResult.CONTINUE;
                }

                final String fileName = dir.getFileName().toString();
                if (fileName.startsWith("370")) {
                    return FileVisitResult.CONTINUE;
                } else {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }

            @Override
            public FileVisitResult visitFile (Path file,
                                              BasicFileAttributes attrs)
                    throws IOException
            {
                logger.info("visitFile {}", file);

                final String fileName = file.getFileName().toString();
                if (fileName.endsWith(CONTROL_EXT)) {
                    Files.move(file, controlPath.resolve(fileName));
                } else if (fileName.endsWith(IMAGES_EXT)) {
                    Files.move(file, imagesPath.resolve(fileName));
                } else if (fileName.endsWith(".features.zip")) {
                    String radix = FileUtil.avoidExtensions(fileName, ".features.zip");
                    Files.move(file, featuresPath.resolve(radix + ".csv.zip"));
                } else if (fileName.endsWith(FILTERED_EXT)) {
                    Files.move(file, filteredPath.resolve(fileName));
                } else if (fileName.endsWith(INFO_EXT)) {
                    Files.move(file, annotationsPath.resolve(fileName));
                } else if (fileName.endsWith(OMR_EXT)) {
                    Files.move(file, omrPath.resolve(fileName));
                } else if (fileName.endsWith(TABLATURES_EXT)) {
                    Files.move(file, tablaturesPath.resolve(fileName));
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }
}
