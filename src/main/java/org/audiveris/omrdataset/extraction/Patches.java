//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          P a t c h e s                                         //
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
import org.audiveris.omrdataset.api.OmrShape;
import static org.audiveris.omrdataset.training.App.IMAGES_EXT;
import static org.audiveris.omrdataset.training.App.IMAGES_FORMAT;
import static org.audiveris.omrdataset.training.Context.CONTEXT_HEIGHT;
import static org.audiveris.omrdataset.training.Context.CONTEXT_WIDTH;
import static org.audiveris.omrdataset.training.Context.CSV_HEIGHT;
import static org.audiveris.omrdataset.training.Context.CSV_INTERLINE;
import static org.audiveris.omrdataset.training.Context.CSV_LABEL;
import static org.audiveris.omrdataset.training.Context.CSV_SHEET_ID;
import static org.audiveris.omrdataset.training.Context.CSV_SYMBOL_ID;
import static org.audiveris.omrdataset.training.Context.CSV_WIDTH;
import static org.audiveris.omrdataset.training.Context.CSV_X;
import static org.audiveris.omrdataset.training.Context.CSV_Y;
import static org.audiveris.omrdataset.training.Context.INTERLINE;
import static org.audiveris.omrdataset.training.Context.NUM_CLASSES;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.InputStreamInputSplit;

import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/**
 * Class {@code Patches} takes a sheet features.csv file as input and regenerates patches
 * for visual checking.
 *
 * @author Hervé Bitteur
 */
public class Patches
{
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Patches.class);

    private static final int numClasses = OmrShape.unknown.ordinal();

    /** Color for overlapping cross. */
    private static final Color CROSS_COLOR = new Color(255, 0, 0, 150);

    /** Color for overlapping box. */
    private static final Color BOX_COLOR = new Color(0, 255, 0, 150);

    /** Index shift on DL4J features when separated from labels. */
    public static final int DID = -1; // -2; // -2 for old MuseScore bins HACK

    //~ Instance fields ----------------------------------------------------------------------------
    private final Path featuresPath;

    private final Path patchFolder;

    /** Sheet id, if any. */
    private Integer sheetId;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Patches builder for a sheet.
     *
     * @param sheetId      id of containing sheet (for information purpose only)
     * @param featuresPath (zipped) features.csv input file
     * @param patchFolder  dedicated patches folder
     */
    public Patches (Integer sheetId,
                    Path featuresPath,
                    Path patchFolder)
    {
        this.sheetId = sheetId;
        this.featuresPath = featuresPath;
        this.patchFolder = patchFolder;
        logger.info("sheetId:{} Generating patches in folder {}", sheetId, patchFolder);
    }

    public Patches (Path featuresPath,
                    Path patchFolder)
    {
        this.featuresPath = featuresPath;
        this.patchFolder = patchFolder;

        logger.info("Generating patches in folder {}", patchFolder);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Process the features data to generate one sub-image per features row.
     *
     * @throws Exception in case of IO problem or interruption
     */
    public void process ()
            throws Exception
    {
        // Rank in CSV file (1-based)
        int idx = 0;

        //First: get the dataset using the record reader. CSVRecordReader handles loading/parsing
        ZipWrapper zin = ZipWrapper.open(featuresPath);
        InputStream is = zin.newInputStream();

        RecordReader recordReader = new CSVRecordReader(0, ',');
        recordReader.initialize(new InputStreamInputSplit(is));

        //Second: the RecordReaderDataSetIterator handles conversion to DataSet objects,
        // ready for use in neural network
        int batchSize = 500;
        DataSetIterator iterator = new RecordReaderDataSetIterator(
                recordReader, batchSize, CSV_LABEL, NUM_CLASSES, -1);

        while (iterator.hasNext()) {
            DataSet dataSet = iterator.next();

            final INDArray features = dataSet.getFeatures();
            logger.debug("features rank:{} shape:{}", features.rank(), features.shape());
            logger.debug("features rows:{} cols:{}", features.rows(), features.columns());

            final INDArray labels = dataSet.getLabels();
            logger.debug("labels rows:{} cols:{}", labels.rows(), labels.columns());

            final int rows = features.rows();
            logger.debug("sheetId:{} Processing {} rows", sheetId, rows);

            for (int r = 0; r < rows; r++) {
                idx++;
                INDArray row = features.getRow(r);
                int id = (int) row.getDouble(CSV_SYMBOL_ID + DID);
                int x = (int) row.getDouble(CSV_X + DID);
                int y = (int) row.getDouble(CSV_Y + DID);
                int w = (int) row.getDouble(CSV_WIDTH + DID);
                int h = (int) row.getDouble(CSV_HEIGHT + DID);

                // Build image name prefix
                StringBuilder sb = new StringBuilder();

                if (sheetId == null) {
                    sb.append(idx).append("-");
                }

                if (row.columns() >= CSV_SHEET_ID) {
                    sb.append("Sh").append((int) row.getDouble(CSV_SHEET_ID + DID)).append("-");
                }

                sb.append(id).append("(")
                        .append("x").append(x)
                        .append("y").append(y)
                        .append("w").append(w)
                        .append("h").append(h)
                        .append(")");

                OmrShape shape = getShape(labels.getRow(r));
                savePatch(buildPatch(row, shape), sb.toString(), shape);
            }
        }

        is.close();
        zin.close();
    }

    //------------//
    // buildPatch //
    //------------//
    /**
     * Build the patch image that corresponds to the provided row of features.
     *
     * @param row      the flat row of pixel values
     * @param omrShape assigned shape
     * @return the bufferedImage
     */
    public static BufferedImage buildPatch (INDArray row,
                                            OmrShape omrShape)
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
        g.draw(new Line2D.Double(CONTEXT_WIDTH / 2.0, 0, CONTEXT_WIDTH / 2.0, CONTEXT_HEIGHT));
        g.draw(new Line2D.Double(0, CONTEXT_HEIGHT / 2.0, CONTEXT_WIDTH, CONTEXT_HEIGHT / 2.0));

        if (omrShape != OmrShape.none) {
            // Draw symbol bounding box, 1 pixel off of symbol
            double il = row.getDouble(CSV_INTERLINE + DID); // 20 for old MuseScore HACK
            double w = row.getDouble(CSV_WIDTH + DID);
            double h = row.getDouble(CSV_HEIGHT + DID);

            w *= INTERLINE / il;
            h *= INTERLINE / il;

            g.setColor(BOX_COLOR);
            Rectangle2D box = new Rectangle2D.Double((CONTEXT_WIDTH - w) / 2.0 - 1,
                                                     (CONTEXT_HEIGHT - h) / 2.0 - 1,
                                                     w + 2,
                                                     h + 2);
            g.draw(box);
        }

        g.dispose();

        return colorImg;
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

    //-----------//
    // savePatch //
    //-----------//
    /**
     * Save the patch to disk
     *
     * @param img    the patch image
     * @param prefix a unique prefix for file name
     * @param shape  the OMR shape
     */
    private void savePatch (BufferedImage img,
                            String prefix,
                            OmrShape shape)
    {
        try {
            Files.createDirectories(patchFolder);
            Path out = patchFolder.resolve(prefix + "-" + shape + IMAGES_EXT);
            ImageIO.write(img, IMAGES_FORMAT, out.toFile());
            logger.debug("Patch {}", out.toAbsolutePath());
        } catch (IOException ex) {
            logger.warn("Error saving patch to {}", patchFolder, ex);
        }
    }
}
