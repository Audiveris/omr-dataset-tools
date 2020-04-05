//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            U t i l s                                           //
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code Utils} gathers utility functions.
 *
 * @author Hervé Bitteur
 */
public abstract class Utils
{

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    //~ Constructors -------------------------------------------------------------------------------
    private Utils ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // getPrintWriter //
    //----------------//
    public static PrintWriter getPrintWriter (Path path)
            throws IOException
    {
        Files.createDirectories(path.getParent());

        final OutputStream os = Files.newOutputStream(path);
        final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

        return new PrintWriter(bw);
    }

    //---------------//
    // sansExtension //
    //---------------//
    /**
     * Remove the ending extension of the provided file name
     *
     * @param name file name such as "foo.ext"
     * @return radix such as "foo"
     */
    public static String sansExtension (String name)
    {
        int i = name.lastIndexOf('.');

        if (i >= 0) {
            return name.substring(0, i);
        } else {
            return name;
        }
    }
}
