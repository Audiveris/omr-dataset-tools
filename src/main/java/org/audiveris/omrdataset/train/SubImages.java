//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S u b I m a g e s                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omrdataset.train;

import org.audiveris.omrdataset.api.OmrShape;
import static org.audiveris.omrdataset.train.App.*;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;

import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.file.Files;

import javax.imageio.ImageIO;

/**
 * Class {@code SubImages} takes the .CSV file as input and regenerates sub-images for
 * visual checking.
 *
 * @author Hervé Bitteur
 */
public class SubImages
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SubImages.class);

    private static final int numClasses = OmrShape.values().length;

    /** Color for overlapping cross. */
    private static final Color CROSS_COLOR = new Color(255, 0, 0, 150);

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Build the sub-image that corresponds to the provided row of features.
     *
     * @param row the flat row of pixel values
     * @return the bufferedImage
     */
    public static BufferedImage buildSubImage (INDArray row)
    {
        // Build a gray image with vector values
        BufferedImage grayImg = new BufferedImage(
                CONTEXT_WIDTH,
                CONTEXT_HEIGHT,
                BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = grayImg.getRaster();
        DataBuffer buffer = raster.getDataBuffer();
        DataBufferByte byteBuffer = (DataBufferByte) buffer;

        for (int r = 0; r < CONTEXT_HEIGHT; r++) {
            int offset = r * CONTEXT_WIDTH;

            for (int c = 0; c < CONTEXT_WIDTH; c++) {
                int i = offset + c;
                int val = (int) Math.rint(row.getDouble(i));
                val = 255 - val; // Inversion
                byteBuffer.setElem(i, val);
            }
        }

        // Draw colored reference lines on top of image
        BufferedImage colorImg = new BufferedImage(
                CONTEXT_WIDTH,
                CONTEXT_HEIGHT,
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = colorImg.createGraphics();
        g.drawImage(grayImg, null, null);
        g.setColor(CROSS_COLOR);
        g.drawLine(CONTEXT_WIDTH / 2, 0, CONTEXT_WIDTH / 2, CONTEXT_HEIGHT);
        g.drawLine(0, CONTEXT_HEIGHT / 2, CONTEXT_WIDTH, CONTEXT_HEIGHT / 2);
        g.dispose();

        return colorImg;
    }

    /**
     * Direct entry point.
     *
     * @param args not used
     * @throws Exception in case of problem
     */
    public static void main (String[] args)
            throws Exception
    {
        new SubImages().process();
    }

    /**
     * Process the features data to generate one sub-image per features row.
     *
     * @throws Exception in case of IO problem or interruption
     */
    public void process ()
            throws Exception
    {
        int index = 0;

        //First: get the dataset using the record reader. CSVRecordReader handles loading/parsing
        int numLinesToSkip = 1;
        String delimiter = ",";
        RecordReader recordReader = new CSVRecordReader(numLinesToSkip, delimiter);
        recordReader.initialize(new FileSplit(FEATURES_PATH.toFile()));

        //Second: the RecordReaderDataSetIterator handles conversion to DataSet objects, ready for use in neural network
        int labelIndex = CONTEXT_WIDTH * CONTEXT_HEIGHT;
        int batchSize = 500;
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
                saveSubImage(buildSubImage(row), ++index, getShape(labels.getRow(r)));
            }
        }
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
     * Save the sub-image to disk
     *
     * @param img   the sub-image
     * @param index sample sequential index
     * @param shape the OMR shape
     * @throws IOException in case of IO problem
     */
    private void saveSubImage (BufferedImage img,
                               int index,
                               OmrShape shape)
            throws IOException
    {
        Files.createDirectories(SUB_IMAGES_PATH);
        ImageIO.write(
                img,
                OUTPUT_IMAGES_FORMAT,
                SUB_IMAGES_PATH.resolve(shape + "-" + index + OUTPUT_IMAGES_EXT).toFile());
    }
}
