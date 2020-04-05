//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        C s v S l i c e r                                       //
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
import java.io.BufferedWriter;
import static org.audiveris.omrdataset.training.App.ANALYSES_PATH;
import static org.audiveris.omrdataset.training.App.BATCH_SIZE;

import org.slf4j.Logger;

import java.nio.file.Path;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.ZipWrapper;
import org.slf4j.LoggerFactory;

/**
 * Class {@code CsvSlicer} gets a slice of a (zipped) CSV feature file, based on desired
 * iterations.
 *
 * @author Hervé Bitteur
 */
public class CsvSlicer
{

    private static final Logger logger = LoggerFactory.getLogger(CsvSlicer.class);

    //---------//
    // extract //
    //---------//
    /**
     * Extract a slice of the provided feature CSV file into a small CSV file named
     * according to the desired iterations
     *
     * @param csvPath path to the csv input file
     * @param iterMin first interesting iteration
     * @param iterMax last interesting iteration
     * @return the created slice file
     * @throws Exception should anything go wrong
     */
    public Path extract (Path csvPath,
                         int iterMin,
                         int iterMax)
            throws Exception
    {
        final String radix = "iter-" + iterMin + ((iterMax != iterMin) ? "-" + iterMax : "");
        final int idxMin = iterMin * BATCH_SIZE;
        final int idxMax = (iterMax + 1) * BATCH_SIZE - 1;
        final String name = FileUtil.getNameSansExtension(csvPath);
        final Path dir = ANALYSES_PATH.resolve(name + "-" + radix);
        final Path slicePath = dir.resolve(radix + ".csv");

        final ZipWrapper zin = ZipWrapper.open(csvPath);
        final ZipWrapper zout = ZipWrapper.create(slicePath);

        try (BufferedReader br = zin.newBufferedReader();
             BufferedWriter bw = zout.newBufferedWriter()) {

            String line;
            int idx = -1;

            while ((line = br.readLine()) != null) {
                idx++;

                if (idx > idxMax) {
                    break;
                }

                if (idx >= idxMin) {
                    bw.write(line);
                    bw.newLine();
                }
            }

            bw.close();
        }

        zin.close();
        zout.close();

        return slicePath;
    }
}
