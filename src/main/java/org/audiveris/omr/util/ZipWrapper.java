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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import org.audiveris.omrdataset.extraction.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class {@code ZipWrapper} allows to wrap a file in a zip system rather transparently.
 * <p>
 * Motivation:
 * The original use-case deals with files like features.csv which can be enormous (25 GB),
 * because the CSV formatting is costly.
 * Zip compression for a typical CSV file, is about 95%, meaning the same information is now kept
 * in 5% (1/20th) of the uncompressed version.
 * <p>
 * Java offers the notion of
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
    //----------//
    // getInner //
    //----------//
    public Path getInner ()
    {
        return innerPath;
    }

    //----------------//
    // newPrintWriter //
    //----------------//
    public PrintWriter newPrintWriter ()
            throws IOException
    {
        return Utils.getPrintWriter(innerPath);
    }

    //-------------------//
    // newBufferedReader //
    //-------------------//
    public BufferedReader newBufferedReader ()
            throws IOException
    {
        return new BufferedReader(new InputStreamReader(newInputStream(), "UTF-8"));
    }

    //-------------------//
    // newBufferedWriter //
    //-------------------//
    public BufferedWriter newBufferedWriter ()
            throws IOException
    {
        return new BufferedWriter(new OutputStreamWriter(newOutputStream(), "UTF-8"));
    }

    //---------//
    // getRoot //
    //---------//
    public Path getRoot ()
    {
        return root;
    }

    //----------------//
    // newInputStream //
    //----------------//
    public InputStream newInputStream ()
            throws IOException
    {
        return Files.newInputStream(innerPath);
    }

    //-----------------//
    // newOutputStream //
    //-----------------//
    public OutputStream newOutputStream ()
            throws IOException
    {
        return Files.newOutputStream(innerPath);
    }

    //-------//
    // close //
    //-------//
    public void close ()
            throws IOException
    {
        root.getFileSystem().close();
    }

    //--------//
    // create //
    //--------//
    public static ZipWrapper create (Path virtualPath)
            throws IOException
    {
        final Path root = ZipFileSystem.create(toZip(virtualPath));

        return new ZipWrapper(root, root.resolve(virtualPath.getFileName().toString()));
    }

    //------//
    // open //
    //------//
    public static ZipWrapper open (Path virtualPath)
            throws IOException
    {
        final Path root = ZipFileSystem.open(toZip(virtualPath));

        return new ZipWrapper(root, root.resolve(virtualPath.getFileName().toString()));
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

    //-------//
    // toZip //
    //-------//
    public static Path toZip (Path virtualPath)
    {
        return virtualPath.resolveSibling(FileUtil.getNameSansExtension(virtualPath) + ".zip");
    }

    public static void main (String[] args)
            throws Exception
    {
        final Path where = Paths.get("data/tests/essai.csv");

        {
            logger.info("1");
            final ZipWrapper zw = ZipWrapper.create(where);
            final OutputStream os = zw.newOutputStream();
            final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

            bw.write("Une ligne");
            bw.newLine();
            bw.write("Une deuxième ligne");
            bw.newLine();
            bw.write("Une troisième ligne");
            bw.newLine();

            bw.flush();
            os.close();
        }

        {
            logger.info("2");
            final ZipWrapper zw = ZipWrapper.create(where);
            final InputStream is = zw.newInputStream();
            final BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            String line;
            while ((line = br.readLine()) != null) {
                logger.info("line: {}", line);
            }

            is.close();
            zw.close();
        }

        {
            logger.info("3");
            final ZipWrapper zw = ZipWrapper.create(where);
            final BufferedReader br = zw.newBufferedReader();

            String line;
            while ((line = br.readLine()) != null) {
                logger.info("line: {}", line);
            }

            br.close();
            zw.close();
        }

        {
            logger.info("4");
            final ZipWrapper zw2 = ZipWrapper.open(where);
            final InputStream is2 = zw2.newInputStream();
            final BufferedReader br2 = new BufferedReader(new InputStreamReader(is2, "UTF-8"));

            String line2;
            while ((line2 = br2.readLine()) != null) {
                logger.info("line: {}", line2);
            }

            is2.close();
            zw2.close();
        }

        logger.info("5");
        boolean deleted = ZipWrapper.deleteIfExists(where);
        logger.info("deleted: {}", deleted);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
}
