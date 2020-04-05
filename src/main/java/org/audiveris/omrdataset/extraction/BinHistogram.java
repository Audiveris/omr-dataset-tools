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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.audiveris.omrdataset.api.OmrShape;
import static org.audiveris.omrdataset.training.App.BINS_PATH;
import static org.audiveris.omrdataset.training.Context.CSV_LABEL;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code BinHistogram} computes a histogram of all shapes present in a collection
 * of .csv files.
 *
 * @author Hervé Bitteur
 */
public class BinHistogram
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(BinHistogram.class);

    private static final int numClasses = OmrShape.unknown.ordinal();

    //~ Instance fields ----------------------------------------------------------------------------
    private final Map<OmrShape, Integer> histo = new EnumMap<>(OmrShape.class);

    //~ Constructors -------------------------------------------------------------------------------
    public BinHistogram ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    public void process (List<Integer> bins)
            throws Exception
    {
        for (int bin : bins) {
            String name = String.format("bin-%02d.csv", bin);
            Path featuresPath = BINS_PATH.resolve(name);
            logger.info("Populating histogram with {}", featuresPath);

            if (!Files.exists(featuresPath)) {
                logger.warn("Could not find {}", featuresPath);
            } else {
                int it = 0;
                RecordReader recordReader = new CSVRecordReader(0, ',');
                recordReader.initialize(new FileSplit(featuresPath.toFile()));
                int batchSize = 500;
                DataSetIterator iterator = new RecordReaderDataSetIterator(
                        recordReader, batchSize, CSV_LABEL, numClasses, -1);

                while (iterator.hasNext()) {
                    final DataSet dataSet = iterator.next();
                    final INDArray labels = dataSet.getLabels();
                    final int rows = labels.rows();
                    it++;
                    logger.info("   it:{} processing {} rows", it, rows);

                    for (int r = 0; r < rows; r++) {
                        final OmrShape shape = getShape(labels.getRow(r));
                        final Integer count = histo.get(shape);

                        if (count == null) {
                            histo.put(shape, 1);
                        } else {
                            histo.put(shape, count + 1);
                        }
                    }
                }
            }
        }
    }

    /**
     * Print the proportion for each shape present in current sheet.
     */
    public void print ()
    {
        // Total
        int total = 0;
        for (Integer i : histo.values()) {
            total += i;
        }

        StringBuilder sb = new StringBuilder();
        // Non-empty buckets
        for (Map.Entry<OmrShape, Integer> entry : histo.entrySet()) {
            int count = entry.getValue();
            double ratio = count / (double) total;
            sb.append(String.format("%n%7d %.3f ", count, ratio)).append(entry.getKey());
        }

        sb.append(String.format("%n%7d 100.0", total));

        logger.info(" histogram:\n{}", sb);
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
        for (int c = 0; c < numClasses; c++) {
            double val = labels.getDouble(c);

            if (val != 0) {
                return OmrShape.values()[c];
            }
        }

        return null;
    }

//~ Inner Classes ------------------------------------------------------------------------------
}
