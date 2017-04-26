//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S u b i m a g e s                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package org.omrdataset;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;

import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import static org.omrdataset.App.CONTEXT_HEIGHT;
import static org.omrdataset.App.CONTEXT_WIDTH;
import static org.omrdataset.App.CSV_PATH;
import static org.omrdataset.App.SUBIMAGES_PATH;
import static org.omrdataset.App.SUBIMAGE_FORMAT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;

import javax.imageio.ImageIO;

/**
 * Class {@code Subimages} takes the .CSV file as input and regenerates sub-images for
 * visual checking.
 *
 * @author Hervé Bitteur
 */
public class Subimages
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Subimages.class);

    private static final int numClasses = OmrShape.values().length;

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Process the features data to generate one sub-image per features row.
     *
     * @throws Exception
     */
    public void process ()
            throws Exception
    {
        int index = 0;

        //First: get the dataset using the record reader. CSVRecordReader handles loading/parsing
        int numLinesToSkip = 0;
        String delimiter = ",";
        RecordReader recordReader = new CSVRecordReader(numLinesToSkip, delimiter);
        recordReader.initialize(new FileSplit(CSV_PATH.toFile()));

        //Second: the RecordReaderDataSetIterator handles conversion to DataSet objects, ready for use in neural network
        int labelIndex = CONTEXT_WIDTH * CONTEXT_HEIGHT;
        int batchSize = 50;
        logger.info(
                "labelIndex:{}, numClasses:{}, batchSize:{}",
                labelIndex,
                numClasses,
                batchSize);

        DataSetIterator iterator = new RecordReaderDataSetIterator(
                recordReader,
                batchSize,
                labelIndex,
                numClasses,
                -1);

        while (iterator.hasNext()) {
            DataSet dataSet = iterator.next();

            final INDArray features = dataSet.getFeatures();
            logger.debug("features rows:{} cols:{}", features.rows(), features.columns());

            final INDArray labels = dataSet.getLabels();
            logger.debug("labels rows:{} cols:{}", labels.rows(), labels.columns());

            final int rows = features.rows();
            logger.info("Processing {} rows", rows);

            for (int r = 0; r < rows; r++) {
                INDArray row = features.getRow(r);
                buildSubImage(row, ++index, getShape(labels.getRow(r)));
            }
        }
    }

    /**
     * Direct entry point.
     *
     * @param args not used
     * @throws Exception
     */
    public static void main (String[] args)
            throws Exception
    {
        new Subimages().process();
    }

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

    /**
     * Build the sub-image that corresponds to the provided row of features.
     *
     * @param row   the row within features array
     * @param index sample sequential index
     * @param shape the OMR shape
     * @throws Exception
     */
    private void buildSubImage (INDArray row,
                                int index,
                                OmrShape shape)
            throws Exception
    {
        BufferedImage img = new BufferedImage(
                CONTEXT_WIDTH,
                CONTEXT_HEIGHT,
                BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = img.getRaster();
        DataBuffer buffer = raster.getDataBuffer();
        DataBufferByte byteBuffer = (DataBufferByte) buffer;

        for (int r = 0; r < CONTEXT_HEIGHT; r++) {
            int offset = r * CONTEXT_WIDTH;
            for (int c = 0; c < CONTEXT_WIDTH; c++) {
                int i = offset + c;
                int val = (int) Math.rint(row.getDouble(i));
                ///val = 255 - val; // Inversion
                byteBuffer.setElem(i, val);
            }
        }

        ImageIO.write(
                img,
                SUBIMAGE_FORMAT,
                SUBIMAGES_PATH.resolve(shape + "-" + index + "." + SUBIMAGE_FORMAT).toFile());
    }
}
