//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          T e s t Z i p                                         //
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code TestZip} tests the use of zip output/input.
 *
 * @author Hervé Bitteur
 */
public class TestZip
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(TestZip.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //~ Constructors -------------------------------------------------------------------------------
    //~ Methods ------------------------------------------------------------------------------------
    public static void main (String[] args)
            throws Exception
    {
        Path path = Paths.get("data/tests/essai.csv.zip");

        {
            System.out.println("create");

            OutputStream os = Files.newOutputStream(path);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            ZipOutputStream zos = new ZipOutputStream(bos);

            ZipEntry entry = new ZipEntry("essai.csv");
            zos.putNextEntry(entry);
//
//            PrintWriter pw = new PrintWriter(zos, true, Charset.forName("UTF-8"));
//            System.out.println("pw = " + pw);
////
////            pw.println("Ceci est la ligne #1");
////            pw.println("Ceci est la ligne #2");
////            pw.println("Ceci est la ligne #3");
//
//            pw.close();
            zos.close();
        }

        {
            System.out.println("read");

            InputStream is = Files.newInputStream(path);
            BufferedInputStream bis = new BufferedInputStream(is);
            ZipInputStream zis = new ZipInputStream(bis);
            ZipEntry entry = zis.getNextEntry();
            System.out.println("entry = " + entry);

            if (entry != null) {
                System.out.println("Reading entry " + entry);
                InputStreamReader reader = new InputStreamReader(zis);
                BufferedReader br = new BufferedReader(reader);

                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("line = " + line);
                }
            } else {
                System.out.println("No entry");
            }
        }

        {
            System.out.println("append");

            OutputStream os = Files.newOutputStream(path, APPEND, CREATE);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            ZipOutputStream zos = new ZipOutputStream(bos);

            // And now what? I can't append to an existing entry!
            /// zos.???
        }

    }
    //~ Inner Classes ------------------------------------------------------------------------------

}
