//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       Z i p W r a p p e r                                      //
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
package org.audiveris.omr.util;

import org.audiveris.omrdataset.extraction.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Class {@code ZipWrapper} allows to wrap a file in a zip system rather transparently.
 * <p>
 * Motivation:
 * The original use-case deals with files like features.csv which can be enormous (25 GB),
 * because the CSV formatting is costly.
 * Zip compression for a typical CSV file, is about 95%, meaning the same information is now kept
 * in 5% (1/20th) of the uncompressed version.
 * <p>
 * Java offers the notion of zip file system, which is used here.
 * <p>
 * The ZipWrapper simply puts a Zip envelope around a standard file, in a manner as transparent as
 * possible.
 * The methods {@link #create(Path)}, {@link #open(Path)}, {@link #delete(Path)},
 * {@link #deleteIfExists(Path)} and {@link #exists(Path)} expect a "virtual path" as parameter.
 * They accept a virtual path both with and without a ".zip" extension:
 * <table>
 * <tr>
 * <th>Path provided</th>
 * <th>Zip external file</th>
 * <th>Zip internal entry</th>
 * </tr>
 * <tr>
 * <td>path/to/foo</td>
 * <td>path/to/foo.zip</td>
 * <td>foo</td>
 * <tr>
 * <td>path/to/foo.ext</td>
 * <td>path/to/foo.ext.zip</td>
 * <td>foo.ext</td>
 * <tr>
 * <td>path/to/foo.zip</td>
 * <td>path/to/foo.zip</td>
 * <td>foo</td>
 * <tr>
 * <td>path/to/foo.ext.zip</td>
 * <td>path/to/foo.ext.zip</td>
 * <td>foo.ext</td>
 * </tr>
 * </table>
 * It is recommended to use the last 2 cases because the physical path is the same as the
 * virtual path.
 *
 * @author Hervé Bitteur
 */
public class ZipWrapper
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ZipWrapper.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final Path root;

    private final Path innerPath;

    //~ Constructors -------------------------------------------------------------------------------
    private ZipWrapper (Path root,
                        Path innerPath)
    {
        this.root = root;
        this.innerPath = innerPath;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // close //
    //-------//
    public void close ()
            throws IOException
    {
        root.getFileSystem().close();
    }

    //----------//
    // getInner //
    //----------//
    public Path getInner ()
    {
        return innerPath;
    }

    //---------//
    // getRoot //
    //---------//
    public Path getRoot ()
    {
        return root;
    }

    //-------------------//
    // newBufferedReader //
    //-------------------//
    public BufferedReader newBufferedReader (OpenOption... options)
            throws IOException
    {
        return new BufferedReader(new InputStreamReader(newInputStream(options), "UTF-8"));
    }

    //-------------------//
    // newBufferedWriter //
    //-------------------//
    public BufferedWriter newBufferedWriter (OpenOption... options)
            throws IOException
    {
        return new BufferedWriter(new OutputStreamWriter(newOutputStream(options), "UTF-8"));
    }

    //----------------//
    // newInputStream //
    //----------------//
    public InputStream newInputStream (OpenOption... options)
            throws IOException
    {
        return Files.newInputStream(innerPath, options);
    }

    //-----------------//
    // newOutputStream //
    //-----------------//
    public OutputStream newOutputStream (OpenOption... options)
            throws IOException
    {
        return Files.newOutputStream(innerPath, options);
    }

    //----------------//
    // newPrintWriter //
    //----------------//
    public PrintWriter newPrintWriter (OpenOption... options)
            throws IOException
    {
        return Utils.getPrintWriter(innerPath, options);
    }

    @Override
    public String toString ()
    {
        return new StringBuilder()
                .append(getClass().getSimpleName()).append('{')
                .append(root.getFileSystem())
                .append(" inner:")
                .append(innerPath)
                .append('}').toString();
    }

    //--------//
    // create //
    //--------//
    /**
     * Create a new ZipWrapper at virtualPath location.
     *
     * @param virtualPath
     * @return the created wrapper
     * @throws IOException if IO error
     */
    public static ZipWrapper create (Path virtualPath)
            throws IOException
    {
        final Path root = ZipFileSystem.create(toZip(virtualPath));

        return new ZipWrapper(root, root.resolve(toInner(virtualPath)));
    }

    //--------//
    // delete //
    //--------//
    public static void delete (Path virtualPath)
            throws IOException
    {
        Files.delete(toZip(virtualPath));
    }

    //----------------//
    // deleteIfExists //
    //----------------//
    public static boolean deleteIfExists (Path virtualPath)
            throws IOException
    {
        return Files.deleteIfExists(toZip(virtualPath));
    }

    //--------//
    // exists //
    //--------//
    public static boolean exists (Path virtualPath)
            throws IOException
    {
        return Files.exists(toZip(virtualPath));
    }

    //------//
    // open //
    //------//
    public static ZipWrapper open (Path virtualPath)
            throws IOException
    {
        final Path root = ZipFileSystem.open(toZip(virtualPath));

        return new ZipWrapper(root, root.resolve(toInner(virtualPath)));
    }

    //--------//
    // toInner//
    //--------//
    public static String toInner (Path virtualPath)
    {
        final Path fileName = virtualPath.getFileName();
        final String extension = FileUtil.getExtension(fileName);

        if (extension.equals(".zip")) {
            return FileUtil.getNameSansExtension(virtualPath);
        } else {
            return fileName.toString();
        }
    }

    //-------//
    // toZip //
    //-------//
    public static Path toZip (Path virtualPath)
    {
        final String extension = FileUtil.getExtension(virtualPath);

        if (extension.equals(".zip")) {
            return virtualPath;
        } else {
            return virtualPath.resolveSibling(FileUtil.getNameSansExtension(virtualPath) + ".zip");
        }
    }

    public static void main (String[] args)
            throws Exception
    {
        final Path where = Paths.get("data/tests/essai.csv.zip");
        ///final Path where = Paths.get("data/tests/essai.csv");

        {
            logger.info("touch");
            final ZipWrapper zw = ZipWrapper.create(where);
            logger.info("zw:{}", zw);
            zw.close();
        }
//
//        {
//            logger.info("creation");
//            final ZipWrapper zw = ZipWrapper.create(where);
//            final BufferedWriter bw = zw.newBufferedWriter();
//
//            bw.write("Une ligne");
//            bw.newLine();
//            bw.write("Une deuxième ligne");
//            bw.newLine();
//            bw.write("Une troisième ligne");
//            bw.newLine();
//
//            bw.flush();
//            bw.close();
//            zw.close();
//        }

        {
            logger.info("read");
            final ZipWrapper zw = ZipWrapper.open(where);
            logger.info("zw:{}", zw);
            if (Files.exists(zw.getInner())) {
                try (BufferedReader br = zw.newBufferedReader()) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        logger.info("line: {}", line);
                    }
                }
            } else {
                logger.info("{} does not exist", zw.getInner());
            }
            zw.close();
        }

        {
            logger.info("append");
            final ZipWrapper zw = ZipWrapper.open(where);
            logger.info("zw:{}", zw);
            final OutputStream os = zw.newOutputStream(StandardOpenOption.CREATE,
                                                       StandardOpenOption.APPEND);
            final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

            bw.write("Une quatrième ligne");
            bw.newLine();
            bw.write("Une cinquième ligne");
            bw.newLine();
            bw.write("Une sixième ligne");
            bw.newLine();

            bw.flush();
            bw.close();
            zw.close();
        }

        {
            logger.info("append-bis");
            final ZipWrapper zw = ZipWrapper.open(where);
            final BufferedWriter bw = zw.newBufferedWriter(StandardOpenOption.CREATE,
                                                           StandardOpenOption.APPEND);

            bw.write("Une septième ligne");
            bw.newLine();
            bw.write("Une huitième ligne");
            bw.newLine();
            bw.write("Une neuvième ligne");
            bw.newLine();

            bw.flush();
            bw.close();
            zw.close();
        }

        {
            logger.info("read2");
            final ZipWrapper zw = ZipWrapper.open(where);
            logger.info("zw:{}", zw);
            final BufferedReader br = zw.newBufferedReader();

            String line;
            while ((line = br.readLine()) != null) {
                logger.info("line: {}", line);
            }

            br.close();
            zw.close();
        }

        logger.info("exists");
        boolean exists = ZipWrapper.exists(where);
        logger.info("exists:{}", exists);

        logger.info("deletion");
        boolean deleted = ZipWrapper.deleteIfExists(where);
        logger.info("deleted: {}", deleted);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
}
