//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            C l e a n                                           //
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
package org.audiveris.omrdataset.extraction;

/**
 * Class {@code Clean} cleans up output data.
 *
 * @author Hervé Bitteur
 */
@Deprecated
public class Clean
{
//    //~ Static fields/initializers -----------------------------------------------------------------
//
//    private static final Logger logger = LoggerFactory.getLogger(Clean.class);
//
//    //~ Methods ------------------------------------------------------------------------------------
//    /**
//     * Direct entry point.
//     *
//     * @param args not used
//     * @throws IOException in case of IO problem
//     */
//    public static void main (String[] args)
//            throws IOException
//    {
//        new Clean().process();
//    }
//
//    /**
//     * Clean up the output folder.
//     *
//     * @throws IOException in case of IO problem
//     */
//    public void process ()
//            throws IOException
//    {
//        if (OUTPUT_PATH == null) {
//            logger.warn("No output defined.");
//
//            return;
//        }
//
//        if (!Files.exists(OUTPUT_PATH)) {
//            logger.info("{} does not exist.", OUTPUT_PATH);
//
//            return;
//        }
//
//        if (!Files.isDirectory(OUTPUT_PATH)) {
//            logger.warn("{} is not a directory.", OUTPUT_PATH);
//
//            return;
//        }
//
//        deleteDirectory(OUTPUT_PATH);
//        logger.info("Directory {} deleted.", OUTPUT_PATH);
//    }
//
//    /**
//     * Delete a directory with all its content in a recursive manner
//     *
//     * @param directory directory to delete
//     * @throws IOException in case deletion is unsuccessful
//     */
//    private static void deleteDirectory (Path directory)
//            throws IOException
//    {
//        if (!Files.exists(directory)) {
//            throw new IllegalArgumentException(directory + " does not exist");
//        }
//
//        if (!Files.isDirectory(directory)) {
//            throw new IllegalArgumentException(directory + " is not a directory");
//        }
//
//        Files.walkFileTree(
//                directory,
//                new SimpleFileVisitor<Path>()
//        {
//            @Override
//            public FileVisitResult visitFile (Path file,
//                                              BasicFileAttributes attrs)
//                    throws IOException
//            {
//                Objects.requireNonNull(file);
//                Objects.requireNonNull(attrs);
//                logger.debug("visitFile {}", file);
//
//                Files.delete(file);
//
//                return FileVisitResult.CONTINUE;
//            }
//
//            @Override
//            public FileVisitResult postVisitDirectory (Path dir,
//                                                       IOException exc)
//                    throws IOException
//            {
//                Objects.requireNonNull(dir);
//                logger.debug("postVisitDirectory {}", dir);
//
//                if (exc != null) {
//                    throw exc;
//                }
//
//                Files.delete(dir);
//
//                return FileVisitResult.CONTINUE;
//            }
//        });
//    }
}
