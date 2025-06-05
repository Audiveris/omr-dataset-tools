//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t S a m p l e S e t                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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
package org.audiveris.omrdataset;

import org.audiveris.omrdataset.api.GeneralShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import com.fasterxml.jackson.databind.JsonNode;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.imageio.ImageIO;

/**
 * Class <code>AbstractSampleSet</code> provides the common features of
 * SelectionSet and ImageSet classes.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractSampleSet
        implements Iterable<Pair<INDArray, INDArray>>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AbstractSampleSet.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Map cat_id -> category. */
    protected JsonNode categories;

    /** The selected image indices. */
    protected List<Integer> imageIndices;

    /** Total number of images to be processed in this archive. */
    protected int totalImageCount;

    //~ Constructors -------------------------------------------------------------------------------
    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // getBox //
    //--------//
    public static Rectangle getBox (JsonNode annotation)
    {
        final JsonNode aBox = annotation.get("a_bbox");
        final int x1 = aBox.get(0).asInt();
        final int y1 = aBox.get(1).asInt();
        final int x2 = aBox.get(2).asInt();
        final int y2 = aBox.get(3).asInt();
        return new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
    }

    //--------//
    // getCsv //
    //--------//
    protected static String getCsv (float[] pixels)
    {
        final int lg = pixels.length;
        final StringBuilder sb = new StringBuilder(3 * lg); // Large enough

        for (int i = 0; i < lg; i++) {
            if (i > 0) {
                sb.append(',');
            }

            sb.append((int) pixels[i]);
        }

        return sb.toString();
    }

    //-----------//
    // rgbToGray //
    //-----------//
    /**
     * Take an RGB image and combine the R, G and B bands according to standard luminance
     * value to provide the output gray value.
     *
     * @param rgb input image with 3 bands RGB
     * @return a gray image
     */
    protected static BufferedImage rgbToGray (BufferedImage rgb)
    {
        ///logger.info("Converting RGB to gray ...");

        final int width = rgb.getWidth();
        final int height = rgb.getHeight();
        final Raster source = rgb.getData();
        final int numBands = rgb.getColorModel().getNumComponents();
        final int[] inLevels = new int[numBands];

        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final WritableRaster raster = img.getRaster();

        // We use luminance value based on standard RGB combination
        final double[] weights = {0.114d, 0.587d, 0.299d};

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                source.getPixel(x, y, inLevels);

                double val = 0;
                for (int i = 0; i < 3; i++) {
                    val += weights[i] * inLevels[i];
                }

                raster.setSample(x, y, 0, (int) Math.rint(val));
            }
        }

        return img;
    }

    //------------//
    // storeImage //
    //------------//
    protected void storeImage (int id,
                               GeneralShape gShape,
                               float[] pixels)
            throws IOException
    {
        final Path dir = DSMain.tallyDir.resolve("shapes");
        final Path shapeDir = Files.createDirectories(dir.resolve(gShape.toString()));
        final int height = DSMain.context.getContextHeight();
        final int width = DSMain.context.getContextWidth();

        final BufferedImage img
                = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final WritableRaster raster = img.getRaster();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 0, pixels[y * width + x]);
            }
        }

        final Path imgPath = shapeDir.resolve("" + id + ".png");
        ImageIO.write(img, "png", imgPath.toFile());
        logger.info(imgPath.toString());
    }

    //-----------//
    // dumpImage //
    //-----------//
    protected static void dumpImage (int id,
                                     GeneralShape gShape,
                                     float[] pixels)
    {
        System.out.println();
        System.out.println("---- " + gShape + " " + id);
        final int height = DSMain.context.getContextHeight();
        final int width = DSMain.context.getContextWidth();
        for (int y = 0; y < height; y++) {
            final StringBuilder sb = new StringBuilder();
            for (int x = 0; x < width; x++) {
                float f = pixels[y * width + x];
                int i = (int) f;
                sb.append(i == 0 ? ' ' : (i == 1 ? 'X' : '?'));
            }

            System.out.println(String.format("%2d %s", y, sb));
        }

        System.out.println("++++ end");
        System.out.println();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------------//
    // AbstractIterator //
    //------------------//
    public class AbstractIterator
            implements Iterator<Pair<INDArray, INDArray>>
    {

        //protected final Iterator<Map.Entry<String, JsonNode>> imgIterator; // Iterator on the images
        protected final Iterator<Image> imgIterator; // Iterator on the images

        protected byte[] imgBytes; // Pixels of current image

        protected int imgWidth; // Image width

        protected int imgHeight; // Image height

        protected Iterator<JsonNode> annIterator; // Iterator on relevant annotations in curImage

        protected Pair<INDArray, INDArray> curPair; // Current pair ready to pick up

        protected int imgIndex = -1; // Sequential image index

        protected PrintWriter csvWriter;

        protected int sequentialId = 0;

        ///public AbstractIterator (Iterator<Map.Entry<String, JsonNode>> imgIterator)
        public AbstractIterator (Iterator<Image> imgIterator)
        {
            this.imgIterator = imgIterator;

//            // CSV output
//            final Path selPath = selectionFile.toPath();
//            final String radix = FileUtil.getNameSansExtension(selPath);
//            try {
//                final Path csvPath = selPath.resolveSibling(radix + ".csv.zip");
//                csvWriter = Utils.getPrintWriter(csvPath);
//                logger.info("Writing CSV to {}", csvPath);
//            } catch (IOException ex) {
//                logger.warn("Error opening csvWriter ", ex);
//            }
        }

        @Override
        public boolean hasNext ()
        {
            final boolean hasNext = curPair != null;

            if (!hasNext) {
                if (csvWriter != null) {
                    csvWriter.flush();
                    csvWriter.close();
                }
            }

            return hasNext;
        }

        protected final Pair<INDArray, INDArray> moveAhead ()
        {
            Global:
            while (true) {
                // Within current image?
                while (annIterator != null && annIterator.hasNext()) {
                    final JsonNode annotation = annIterator.next();
                    final GeneralShape gShape = DSMain.getGeneralShape(annotation);
                    return genPair(annotation, gShape);
                }

                // End of image reached
                while (imgIterator.hasNext()) {
                    imgIndex++;
                    logger.debug("imgIndex:{}", imgIndex);

                    if (imgIndex > imageIndices.get(imageIndices.size() - 1)) {
                        logger.info("Maximum image index has been reached, stopping now.");
                        return null; // Enough!
                    }

                    final Image image = imgIterator.next();

                    if (imageIndices.contains(imgIndex)) {
                        if (imgIndex == imageIndices.get(0)) {
                            logger.info("Minimum image index reached, starting processing...");
                        }
                        final String id = image.getId();
                        final String filename = image.getFileName();
                        annIterator = image.getAnnotationsIterator();
                        logger.info("Processing image id:{} ({}->{}/{}) anns:{}",
                                    id, imgIndex,
                                    imageIndices.get(imageIndices.size() - 1),
                                    totalImageCount,
                                    image.getAnnotationsCount());

                        // Load image file to bytes
                        loadImageBytes(filename); // -> imgBytes, imgWidth, imgHeight
                        if (imgBytes == null) {
                            continue; // Image file unreadable
                        }

                        continue Global; // Move to first relevant annotation in image
                    }
                }

                // No more image
                return null;
            }
        }

        @Override
        public Pair<INDArray, INDArray> next ()
        {
            if (!hasNext()) {
                throw new NoSuchElementException("No next sample");
            }

            final Pair<INDArray, INDArray> cur = curPair;

            // Move to next pair if possible
            curPair = moveAhead();

            return cur;
        }

        protected void loadImageBytes (String fileName)
        {
            try {
                final BufferedImage inputImg = readImageFile(fileName);

                // Check if conversion rgb to gray is needed
                final int numBands = inputImg.getSampleModel().getNumBands(); // Including alpha?
                final BufferedImage img;
                switch (numBands) {
                case 1 ->
                    img = inputImg;
                case 3 ->
                    img = rgbToGray(inputImg);
                default ->
                    throw new IllegalArgumentException("Input images should have 1 or 3 bands, "
                                                               + fileName + " has " + numBands);
                }

                final WritableRaster raster = img.getRaster();
                final DataBuffer buffer = raster.getDataBuffer();
                final DataBufferByte byteBuffer = (DataBufferByte) buffer;
                imgBytes = byteBuffer.getData();
                imgWidth = img.getWidth();
                imgHeight = img.getHeight();
                //logger.info("Loaded image {}", fileName);
            } catch (IOException ex) {
                logger.warn("Error loading {}", fileName);
                imgBytes = null;
            }
        }

        protected BufferedImage readImageFile (String fileName)
                throws IOException
        {
            final Path path = DSMain.imagesFolder.resolve(fileName);
            return ImageIO.read(path.toFile());
        }

        protected Pair<INDArray, INDArray> genPair (JsonNode annotation,
                                                    GeneralShape gShape)
        {
            final int contextHeight = DSMain.context.getContextHeight();
            final int contextWidth = DSMain.context.getContextWidth();

            // Features
            float[] f = new float[contextHeight * contextWidth];
            final Rectangle annBox = getBox(annotation);
            ///logger.info("{} {}", gShape, box);
            /////////////////////////////////////////////
            final double ratio = 1.0;
            final int BACKGROUND = 0; // Background is black
            /////////////////////////////////////////////
            final double sCenterX = ratio * (annBox.getX() + (annBox.getWidth() / 2.0));
            final double sCenterY = ratio * (annBox.getY() + (annBox.getHeight() / 2.0));
            final int axMin = (int) Math.rint(sCenterX - (contextWidth / 2));
            final int ayMin = (int) Math.rint(sCenterY - (contextHeight / 2));

            int idx = 0;
            for (int y = 0; y < contextHeight; y++) {
                int ay = ayMin + y; // Absolute y
                if ((ay < 0) || (ay >= imgHeight)) {
                    // Fill row with background value
                    for (int x = 0; x < contextWidth; x++) {
                        f[idx++] = BACKGROUND;
                    }
                } else {
                    for (int x = 0; x < contextWidth; x++) {
                        int ax = axMin + x; // Absolute x
                        int val = ((ax < 0) || (ax >= imgWidth)) ? BACKGROUND
                                : (255 - (imgBytes[(ay * imgWidth) + ax] & 0xff)); // Inversion!
                        f[idx++] = val;
                    }
                }
            }

            // Copy as CSV
//            final String pixelsCsv = getCsv(f);
//            csvWriter.println(pixelsCsv + "," + gShape.ordinal());
//
            if (DSMain.cli.shapes.contains(gShape)) {
                // Gen image for visual check
                sequentialId++;
                dumpImage(sequentialId, gShape, f);

                try {
                    storeImage(sequentialId, gShape, f);
                } catch (Exception ex) {
                    logger.warn("Error in storeImage", ex);
                }
            }

            // Simple vector meant for InputType.convolutionalFlat()
            INDArray features = Nd4j.create(f);
            // Got rank 2 array as input to ConvolutionLayer (layer name = Conv1, layer index = 1)
            // with shape [50, 4608].
            // Expected rank 4 array with shape [minibatchSize, layerInputDepth, inputHeight, inputWidth].
            // (Wrong input type (see InputType.convolutionalFlat()) or wrong data type?)
            // (layer name: Conv1, layer index: 1, layer type: ConvolutionLayer)
            //
            //////features = features.reshape(new int[]{1, contextHeight, contextWidth, 1});

            // Labels (one-hot array)
            final INDArray labels = Nd4j.zeros(DSMain.context.getNumClasses())
                    .putScalar(new int[]{gShape.ordinal()}, 1f);

            return Pair.of(features, labels);
        }
    }

    //-------//
    // Image //
    //-------//
    public static interface Image
    {

        String getId ();

        String getFileName ();

        int getAnnotationsCount ();

        Iterator<JsonNode> getAnnotationsIterator ();
    }
}
