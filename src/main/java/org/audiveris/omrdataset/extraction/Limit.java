//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            L i m i t                                           //
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

import org.audiveris.omrdataset.api.OmrShape;
import static org.audiveris.omrdataset.training.App.BINS_PATH;
import static org.audiveris.omrdataset.training.App.BIN_COUNT;
import static org.audiveris.omrdataset.training.Context.CSV_LABEL;
import static org.audiveris.omrdataset.training.Context.NUM_CLASSES;

import org.nd4j.linalg.api.ndarray.INDArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import org.audiveris.omr.util.ZipWrapper;

/**
 * Class {@code Limit} limits the content of bins so that each shape is limited to
 * a maximum of THRESHOLD samples.
 *
 * @author Hervé Bitteur
 */
public class Limit
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(Limit.class);

    private static final int THRESHOLD = 100;

    //~ Instance fields ----------------------------------------------------------------------------
    //~ Constructors -------------------------------------------------------------------------------
    //~ Methods ------------------------------------------------------------------------------------
    public void process ()
            throws Exception
    {

        for (int bin = 1; bin <= BIN_COUNT; bin++) {
            // Input: bin-NN.csv (within bin-NN.zip)
            final String name = String.format("bin-%02d.csv", bin);
            final Path fullPath = BINS_PATH.resolve(name);

            if (!ZipWrapper.exists(fullPath)) {
                logger.warn("Could not find (zipped) {}", fullPath);
                continue;
            }

            // Output: limited-bin-NN.csv (within limited-bin-NN.zip)
            final Path limitedPath = BINS_PATH.resolve("limited-" + name);
            logger.info("Limiting (zipped) {} to (zipped) {}", fullPath, limitedPath);

            ZipWrapper zin = ZipWrapper.open(fullPath);
            ZipWrapper zout = ZipWrapper.create(limitedPath);

            try (final InputStream is = zin.newInputStream();
                 final BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                 final OutputStream os = zout.newOutputStream();
                 final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"))) {

                final OmrShape[] shapes = OmrShape.values();
                final int[] counts = new int[shapes.length];
                String line;

                while ((line = br.readLine()) != null) {
                    final String[] cols = line.split(",");
                    final String indexStr = cols[CSV_LABEL];
                    final int iShape = Integer.parseInt(indexStr);
                    counts[iShape]++;

                    if (counts[iShape] <= THRESHOLD) {
                        bw.write(line);
                        bw.newLine();
                    }
                }

                bw.flush();
            }

            zin.close();
            zout.close();
        }
    }

    //----------//
    // getShape //
    //----------//
    /**
     * Report the shape name indicated in the labels vector.
     *
     * @param labels the labels vector (1.0 for a shape, 0.0 for the others)
     * @return the shape name
     */
    private OmrShape getShape (INDArray labels)
    {
        for (int c = 0; c < NUM_CLASSES; c++) {
            double val = labels.getDouble(c);

            if (val != 0) {
                return OmrShape.values()[c];
            }
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
}
