//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B i n H i s t o g r a m                                    //
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

import org.audiveris.omr.util.ZipWrapper;
import org.audiveris.omrdataset.Main;
import org.audiveris.omrdataset.api.OmrShape;
import static org.audiveris.omrdataset.api.OmrShapes.OMR_SHAPES;
import static org.audiveris.omrdataset.api.Patch.parseLabel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Class {@code BinHistogram} computes a histogram of all shapes present in a collection
 * of .csv.zip files.
 *
 * @author Hervé Bitteur
 */
public class BinHistogram
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(BinHistogram.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final Class labelClass = Main.context.getLabelClass();

    private final Map<Object, Integer> histo = new EnumMap<>(labelClass);

    //~ Constructors -------------------------------------------------------------------------------
    public BinHistogram ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    public void process (List<Integer> bins)
            throws Exception
    {
        for (int bin : bins) {
            ///String name = String.format("limited-bin-%02d.csv.zip", bin);
            Path inPath = Main.binPath(bin);
            logger.info("Populating histogram with {}", inPath);

            if (!Files.exists(inPath)) {
                logger.warn("Could not find {}", inPath);
            } else {
                final ZipWrapper zin = ZipWrapper.open(inPath);
                final BufferedReader br = zin.newBufferedReader();

                String line;

                while ((line = br.readLine()) != null) {
                    final String[] cols = line.split(",");
                    final Enum shape = parseLabel(cols, Main.context);
                    final Integer count = histo.get(shape);

                    if (count == null) {
                        histo.put(shape, 1);
                    } else {
                        histo.put(shape, count + 1);
                    }
                }

                br.close();
                zin.close();
            }
        }
    }

    /**
     * Print the proportion for each shape present in histogram.
     */
    public void print ()
    {
        // Total
        int total = 0;
        for (Integer i : histo.values()) {
            total += i;
        }

        StringBuilder sb = new StringBuilder();
//        // Non-empty buckets
//        for (Map.Entry<OmrShape, Integer> entry : histo.entrySet()) {
//            int count = entry.getValue();
//            double ratio = count / (double) total;
//            sb.append(String.format("%n%7d %.3f ", count, ratio)).append(entry.getKey());
//        }
        // All buckets
        for (OmrShape shape : OMR_SHAPES) {
            Integer count = histo.get(shape);
            if (count == null) {
                sb.append("\n              ").append(shape);
                count = 0;
            } else {
                double ratio = count / (double) total;
                sb.append(String.format("%n%7d %.3f ", count, ratio)).append(shape);
            }
        }

        sb.append(String.format("%n%7d 100.0", total));

        logger.info("histogram:\n{}", sb);
    }
//~ Inner Classes ------------------------------------------------------------------------------
}
