//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       I n s p e c t i o n                                      //
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
import org.audiveris.omrdataset.Main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * Class {@code Inspection} inspects a range of iterations, by selecting the related
 * samples and generating their patches for visual check.
 *
 * @author Hervé Bitteur
 */
public class Inspection
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(Inspection.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final Path binPath;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a {@code Inspection} object on the specified bin number.
     *
     * @param bin bin number (1-based)
     */
    public Inspection (int bin)
    {
        binPath = Main.binPath(bin);
    }

    //~ Methods ------------------------------------------------------------------------------------
    public void process (List<Integer> iterations)
            throws Exception
    {
        if (iterations.isEmpty()) {
            logger.warn("No iterations to inspect");
            return;
        }

        StopWatch watch = new StopWatch("Inspection " + iterations);
        // Extract the related samples
        watch.start("slice");
        logger.info("Slice extraction for {} in {}", iterations, binPath);
        final int iterMin = iterations.get(0);
        final int iterMax = iterations.get(iterations.size() - 1);
        Path slicePath = new CsvSlicer().extract(binPath, iterMin, iterMax);

        // Generate the related patches
        watch.start("patches");
        new Patches(slicePath, slicePath.getParent()).process();

        watch.print();
    }
}
