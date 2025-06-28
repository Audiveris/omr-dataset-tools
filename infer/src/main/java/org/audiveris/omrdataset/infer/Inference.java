//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        I n f e r e n c e                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omrdataset.infer;

import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2RGB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.transform.ColorConversionTransform;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.onnxruntime.runner.OnnxRuntimeRunner;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Class <code>Inference</code> runs inference based on the trained model.
 *
 * @author Hervé Bitteur
 */
public class Inference
{
    //~ Static fields/initializers -----------------------------------------------------------------

    //    static {
    //        System.setProperty("ORT_LOG_LEVEL", "error");
    //        System.setProperty("ONNXRUNTIME_LOG_SEVERITY_LEVEL", "4");
    //    }

    private static final Logger logger = LoggerFactory.getLogger(Inference.class);

    private static final int CLASSES_OFFSET = 4; // Offset of classes in output rows (4: x,y,w,h)

    private static final int IMG_SIZE = 1984; // Image size chosen for YOLO

    private static final double detectionThreshold = 0.5; // Threshold for Detection

    private static final double nmsThreshold = 0.4; // Threshold for Non Max Suppresion

    //~ Instance fields ----------------------------------------------------------------------------

    private final OnnxRuntimeRunner onnxRuntimeRunner; // The model runtime

    private final Path targetDirPath; // Path to the directory where annotated images are stored

    private Path imgPath; // Path of the original image

    private BufferedImage orgImage; // The original image

    private int rowNb; // The number of rows: 4 + number of classes

    private int boxNb; // The number of columns (one for each box)

    private double wRatio; // original image width / yolo image width

    private double hRatio; // original image height / yolo image height

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create an <code>Inference</code>.
     *
     * @param targetDir target directory
     */
    public Inference (String targetDir)
            throws Exception
    {
        logger.info("start");

        // Load the model
        final File f = new File("best (4).onnx");
        onnxRuntimeRunner = OnnxRuntimeRunner.builder().modelUri(f.getAbsolutePath()).build();
        logger.info("ONNX runtime loaded on model: {}", f.toString());

        // Make sure target dir is OK
        targetDirPath = Paths.get(targetDir);
        logger.info("Target directory: {}", targetDirPath);
        Files.createDirectories(targetDirPath);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // process //
    //---------//
    /**
     * Run the model on the provided image.
     *
     * @param imgPath path to the provided image
     * @throws Exception
     */
    public void process (Path imgPath)
        throws Exception
    {
        this.imgPath = imgPath;

        final File imageFile = imgPath.toFile();
        orgImage = ImageIO.read(imageFile);
        final int orgWidth = orgImage.getWidth();
        final int orgHeight = orgImage.getHeight();
        logger.info("orgImage width: {} height : {}", orgWidth, orgHeight);

        wRatio = (double) orgWidth / IMG_SIZE;
        hRatio = (double) orgHeight / IMG_SIZE;

        final NativeImageLoader loader = new NativeImageLoader(
                IMG_SIZE,
                IMG_SIZE,
                3,
                new ColorConversionTransform(COLOR_BGR2RGB));

        final INDArray image = loader.asMatrix(imageFile);
        logger.debug("image.shape: {}", image.shape());

        // Normalize pixels values to [0..1] range
        final ImagePreProcessingScaler scaler = new ImagePreProcessingScaler(0, 1);
        scaler.transform(image);

        final Map<String, INDArray> inputs = new LinkedHashMap<>();
        inputs.put("images", image);

        final Map<String, INDArray> results = onnxRuntimeRunner.exec(inputs);
        logger.debug("keySet: {}", results.keySet());
        final INDArray output = results.get("output0");
        logger.debug("output.shape: {}", output.shape());
        // 1:     image index (0 for a single image)
        // 125:   xc, yc, w, h, and then: c0, c1, ..., c120
        // 80724: boxes values
        rowNb = (int) output.shape()[1];
        boxNb = (int) output.shape()[2];
        logger.debug("rowNb: {} boxNb: {}", rowNb, boxNb);

        final int imgIdx = 0;
        final INDArray out = output.get(NDArrayIndex.point(imgIdx), NDArrayIndex.all());
        logger.debug("out Shape: {}", out.shape());

        final List<DetectedObject> objs = getPredictedObjects(out);
        logger.info("Detected {} objects", objs.size());

        ///YoloUtils.nms(objs, nmsThreshold);
        nms(objs, nmsThreshold); ///////////////////////////////////////////////////////
        logger.info("Kept {} objects", objs.size());

        //        printHistogram(out);
        drawObjects(objs);
        //        printClassBoxes(Classe.fermataAbove, out);
        //        printClassBoxes(Classe.fermataBelow, out);
        ///printClassBoxes(Classe.slur, out);
    }

    //---------------------//
    // getPredictedObjects //
    //---------------------//
    /**
     * Report only the predicted objects among all the candidates.
     *
     * @param out the model raw output
     * @return the list of candidates kept
     */
    private List<DetectedObject> getPredictedObjects (INDArray out)
    {
        final List<DetectedObject> objs = new ArrayList<>();

        for (int c = 0, clsNb = Classe.values().length; c < clsNb; c++) {
            final int row = CLASSES_OFFSET + c;
            final INDArray confs = out.get(NDArrayIndex.point(row), NDArrayIndex.all());

            for (int i = 0; i < boxNb; i++) {
                final double conf = confs.getDouble(i);
                if (conf < detectionThreshold) {
                    continue;
                }

                final double xc = out.getDouble(0, i);
                final double yc = out.getDouble(1, i);
                final double w = out.getDouble(2, i);
                final double h = out.getDouble(3, i);

                //                final INDArray preds = out.get(
                //                        NDArrayIndex.all(),
                //                        NDArrayIndex.point(i),
                //                        NDArrayIndex.interval(row, row + 1));

                DetectedObject obj = new DetectedObject(0, xc, yc, w, h, c, conf);
                objs.add(obj);
            }
        }

        return objs;
    }

    //----------------//
    // printHistogram //
    //----------------//
    /**
     * Print the histogram of all candidates found.
     *
     * @param out model raw output
     */
    private void printHistogram (INDArray out)
    {
        for (Classe c : Classe.values()) {
            final int row = c.ordinal() + 4;
            final INDArray values = out.get(NDArrayIndex.point(row), NDArrayIndex.all());
            final StringBuilder sb = new StringBuilder();

            for (int i = 0; i < boxNb; i++) {
                final double conf = values.getDouble(i);
                if (conf >= detectionThreshold) {
                    sb.append('X');
                }
            }

            if (sb.length() > 0) {
                System.out.println(String.format("%40s %s", c, sb));
            }
        }
    }

    //-----------------//
    // printClassBoxes //
    //-----------------//
    /**
     * Print the bounding box for each candidate of the desired class.
     *
     * @param cls the desired class
     * @param out model raw output
     */
    private void printClassBoxes (Classe cls,
                                  INDArray out)
    {
        System.out.println("\nBoxes for class: " + cls);
        final int row = cls.ordinal() + 4;
        final INDArray confs = out.get(NDArrayIndex.point(row), NDArrayIndex.all());

        for (int i = 0; i < boxNb; i++) {
            final double conf = confs.getDouble(i);
            if (conf >= detectionThreshold) {
                // Convert from xc,yc,w,h to x,y,w,h
                double xc = out.getDouble(0, i);
                double yc = out.getDouble(1, i);
                double w = out.getDouble(2, i);
                double h = out.getDouble(3, i);
                double x = xc - w / 2;
                double y = yc - h / 2;

                // Resize according to original image size
                int xx = (int) Math.rint(x * wRatio);
                int yy = (int) Math.rint(y * hRatio);
                int ww = (int) Math.rint(w * wRatio);
                int hh = (int) Math.rint(h * hRatio);
                System.out.println(
                        String.format("x: %d y: %d w: %d h: %d conf: %.5f", xx, yy, ww, hh, conf));
            }
        }

    }

    //-------------//
    // drawObjects //
    //-------------//
    /**
     * Draw, over the original image, the name and bounding box for each predicted object.
     *
     * @param objs the predicted objects
     * @throws Exception
     */
    private void drawObjects (List<DetectedObject> objs)
        throws Exception
    {
        final Classe[] classes = Classe.values();
        final BufferedImage off_Image = new BufferedImage(
                orgImage.getWidth(),
                orgImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d = off_Image.createGraphics();
        final Font font = new Font("Arial", Font.PLAIN, 12);
        g2d.setFont(font);
        g2d.drawImage(orgImage, null, 0, 0);

        objs.forEach(obj -> {
            // Resize according to original image size
            final double[] topLeft = obj.getTopLeftXY();
            int x = (int) Math.rint(wRatio * topLeft[0]);
            int y = (int) Math.rint(hRatio * topLeft[1]);
            int w = (int) Math.rint(wRatio * obj.getWidth());
            int h = (int) Math.rint(hRatio * obj.getHeight());

            // Draw obj rectangle
            g2d.setColor(Color.RED);
            g2d.drawRect(x, y, w, h);

            //            // Draw class name + conf
            //            g2d.setColor(Color.MAGENTA);
            //            final Classe cls = classes[obj.getPredictedClass()];
            //            final double conf = obj.getConfidence();
            //            g2d.drawString(cls.name() + String.format(" %.2f", conf), x, y);
            // Draw class ID
            g2d.setColor(Color.MAGENTA);
            g2d.drawString("" + obj.getPredictedClass(), x, y);
        });

        g2d.dispose();

        // Save the image with the rectangle drawn on it
        final String fileName = imgPath.getFileName().toString();
        final int dot = fileName.lastIndexOf('.');
        final String radix = fileName.substring(0, dot);
        final String annotatedName = radix + "-ann.png";
        final Path targetPath = targetDirPath.resolve(annotatedName);

        ImageIO.write(off_Image, "png", targetPath.toFile());
        logger.info("Objects printed in {}", targetPath);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    public static void main (String[] args)
        throws Exception
    {
        ///new TestND().test();
        ///
        final Inference inference = new Inference("../data/target");
        Visitor visitor = new Visitor(inference);

        //final Path source = Paths.get("../data/source");
        //        final Path source = Paths.get("D:\\soft\\cases");
        //
        //Files.walkFileTree(source, visitor);

        inference.process(Paths.get("../data/source/allegretto.png"));
        //        inference.process("carmen.png");
        // inference.process(Paths.get("../data/source/lg-617866567367835980-aug-beethoven-.png"));
        //        inference.process("cucaracha.png");
        //        inference.process("D0392410-1.256.png");
        //        inference.process("../data/source/Dichterliebe01-1.png");
        //        inference.process("../data/source/Dichterliebe01-2.png");
        //        inference.process("hove.png");
        //        inference.process("lg-94161796-aug-gonville--page-3.png");
        //        inference.process("lg-198200840-aug-beethoven--page-1.png");
    }

    //---------//
    // Visitor //
    //---------//
    private static class Visitor
            extends SimpleFileVisitor<Path>
    {
        private static final List<String> supported = Arrays.asList(".png", ".jpg");

        private final Inference inference;

        public Visitor (Inference inference)
        {
            this.inference = inference;
        }

        @Override
        public FileVisitResult visitFile (Path path,
                                          BasicFileAttributes attr)
        {
            final int dot = path.toString().lastIndexOf('.');
            final String ext = path.toString().substring(dot);

            if (supported.contains(ext)) {
                System.out.println("\nProcessing " + path);
                try {
                    inference.process(path);
                } catch (Exception ex) {
                    logger.warn("Error processing {} {}", path, ex);
                }
            }

            return CONTINUE;
        }
    }

    /**
     * Performs non-maximum suppression (NMS) on objects, using their IOU with threshold to match
     * pairs.
     */
    public static void nms (List<DetectedObject> objects,
                            double iouThreshold)
    {
        for (int i = 0; i < objects.size(); i++) {
            for (int j = 0; j < objects.size(); j++) {
                DetectedObject o1 = objects.get(i);
                DetectedObject o2 = objects.get(j);
                if (o1 != null && o2 != null && o1.getPredictedClass() == o2.getPredictedClass()
                        && o1.getConfidence() < o2.getConfidence() && iou(o1, o2) > iouThreshold) {
                    objects.set(i, null);
                }
            }
        }
        Iterator<DetectedObject> it = objects.iterator();
        while (it.hasNext()) {
            if (it.next() == null) {
                it.remove();
            }
        }
    }

    public static class DetectedObject
    {

        private final int exampleNumber;

        private final double centerX;

        private final double centerY;

        private final double width;

        private final double height;

        private final int predictedClass;

        private final double confidence;

        /**
         * @param exampleNumber  Index of the example in the current minibatch. For single images,
         *                       this is always 0
         * @param centerX        Center X position of the detected object
         * @param centerY        Center Y position of the detected object
         * @param width          Width of the detected object
         * @param height         Height of the detected object
         * @param predictedClass the predicted class
         */
        public DetectedObject (int exampleNumber,
                               double centerX,
                               double centerY,
                               double width,
                               double height,
                               int predictedClass,
                               double confidence)
        {
            this.exampleNumber = exampleNumber;
            this.centerX = centerX;
            this.centerY = centerY;
            this.width = width;
            this.height = height;
            this.predictedClass = predictedClass;
            this.confidence = confidence;
        }

        public double getConfidence ()
        {
            return confidence;
        }

        /**
         * Get the top left X/Y coordinates of the detected object
         *
         * @return Array of length 2 - top left X and Y
         */
        public double[] getTopLeftXY ()
        {
            return new double[] { centerX - width / 2.0, centerY - height / 2.0 };
        }

        /**
         * Get the bottom right X/Y coordinates of the detected object
         *
         * @return Array of length 2 - bottom right X and Y
         */
        public double[] getBottomRightXY ()
        {
            return new double[] { centerX + width / 2.0, centerY + height / 2.0 };
        }

        /**
         * Get the index of the predicted class (based on maximum predicted probability)
         *
         * @return Index of the predicted class (0 to nClasses - 1)
         */
        public int getPredictedClass ()
        {

            return predictedClass;
        }

        public double getCenterX ()
        {
            return centerX;
        }

        public double getCenterY ()
        {
            return centerY;
        }

        public double getHeight ()
        {
            return height;
        }

        public double getWidth ()
        {
            return width;
        }

        @Override
        public String toString ()
        {
            return "DetectedObject(exampleNumber=" + exampleNumber + ", centerX=" + centerX
                    + ", centerY=" + centerY + ", width=" + width + ", height=" + height
                    + ", confidence=" + confidence + ", predictedClass=" + getPredictedClass()
                    + ")";
        }
    }

    /** Returns intersection over union (IOU) between o1 and o2. */
    public static double iou (DetectedObject o1,
                              DetectedObject o2)
    {
        double x1min = o1.getCenterX() - o1.getWidth() / 2;
        double x1max = o1.getCenterX() + o1.getWidth() / 2;
        double y1min = o1.getCenterY() - o1.getHeight() / 2;
        double y1max = o1.getCenterY() + o1.getHeight() / 2;

        double x2min = o2.getCenterX() - o2.getWidth() / 2;
        double x2max = o2.getCenterX() + o2.getWidth() / 2;
        double y2min = o2.getCenterY() - o2.getHeight() / 2;
        double y2max = o2.getCenterY() + o2.getHeight() / 2;

        double ow = overlap(x1min, x1max, x2min, x2max);
        double oh = overlap(y1min, y1max, y2min, y2max);

        double intersection = ow * oh;
        double union = o1.getWidth() * o1.getHeight() + o2.getWidth() * o2.getHeight()
                - intersection;
        return intersection / union;
    }

    /** Returns overlap between lines [x1, x2] and [x3. x4]. */
    public static double overlap (double x1,
                                  double x2,
                                  double x3,
                                  double x4)
    {
        if (x3 < x1) {
            if (x4 < x1) {
                return 0;
            } else {
                return Math.min(x2, x4) - x1;
            }
        } else {
            if (x2 < x3) {
                return 0;
            } else {
                return Math.min(x2, x4) - x3;
            }
        }
    }

}
