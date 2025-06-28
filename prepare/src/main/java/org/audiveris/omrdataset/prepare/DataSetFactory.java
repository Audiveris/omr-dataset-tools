//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   D a t a S e t F a c t o r y                                  //
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
package org.audiveris.omrdataset.prepare;

import org.audiveris.omrdataset.prepare.DataSetConfig.Checking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.imageio.ImageIO;

/**
 * Class <code>DataSetFactory</code> handles the preparation of a YOLO train/val dataset
 * from different sources, for example DeepScores and DoReMi.
 * <p>
 * Expected arguments:
 * <ul>
 * <li>output: the yolo target folder
 * <li>input: the dataset .yaml description
 * </ul>
 *
 * @author Hervé Bitteur
 */
public abstract class DataSetFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DataSetFactory.class);

    //~ Instance fields ----------------------------------------------------------------------------

    protected final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    private Path trainImages;

    private Path valImages;

    private Path trainLabels;

    private Path valLabels;

    protected Path sourceDir; //  Path to dataset source directory

    protected Path imagesPath;

    protected DataSetConfig config;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a new <code>DataSetFactory</code> instance.
     *
     * @param yoloConfigPath path to the Yolo configuration file
     * @throws java.lang.Exception
     */
    public DataSetFactory (String yoloConfigPath)
            throws Exception
    {
        final Path path = Paths.get(yoloConfigPath);
        final YoloConfig yoloConfig = yamlMapper.readValue(path.toFile(), YoloConfig.class);
        logger.info("{}", yoloConfig);
        prepareYoloFolders(yoloConfig);
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * For each image in part, copy the image file and generate the label file for YOLO.
     *
     * @param part      train or val
     * @param outImages path for target images
     * @param outLabels path for target labels
     * @throws java.lang.Exception
     */
    public abstract void addPart (YoloPart part,
                                  Path outImages,
                                  Path outLabels)
        throws Exception;

    /**
     * Draw annotations on a selected image for visual checking.
     *
     * @param imgName the selected image name
     * @param g2d     graphic context of the annotated image
     * @throws java.lang.Exception
     */
    protected abstract void drawAnnotations (String imgName,
                                             Graphics2D g2d)
        throws Exception;

    /**
     * Print the histogram of classes and instances.
     *
     * @param map  (input/output) the map to be populated
     * @param part the part (train or val) to process
     * @throws Exception
     */
    protected abstract void partHistogram (TreeMap<String, Tuple> map,
                                           YoloPart part)
        throws Exception;

    //-------//
    // check //
    //-------//
    /**
     * Generate images annotated with their labels for visual inspection.
     *
     * @throws Exception
     */
    private void check ()
        throws Exception
    {
        final Path targetDirPath = Paths.get(config.checking.output);
        Files.createDirectories(targetDirPath);
        logger.info("Checking to {} ...", targetDirPath);

        // Collect all pages to check
        final Set<String> imgNames = new LinkedHashSet<>();
        if (config.checking.selection != null) {
            imgNames.addAll(config.checking.selection);
        }
        if (config.checking.use_train) {
            imgNames.addAll(config.train);
        }
        if (config.checking.use_val) {
            imgNames.addAll(config.val);
        }

        for (String imgName : imgNames) {
            final Path imgPath = imagesPath.resolve(imgName);

            final BufferedImage bufImg = ImageIO.read(imgPath.toFile());
            final BufferedImage off_Image = new BufferedImage(
                    bufImg.getWidth(),
                    bufImg.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g2d = off_Image.createGraphics();

            g2d.drawImage(bufImg, null, 0, 0);

            // Get and draw the related annotations
            final Font font = new Font("Arial", Font.PLAIN, 12);
            g2d.setFont(font);
            g2d.setColor(Color.MAGENTA);
            drawAnnotations(imgName, g2d);

            g2d.dispose();

            // Save the image with the boxes drawn on it
            final String fileName = imgPath.getFileName().toString();
            final String annotatedName = radixOf(fileName) + "-ann.png";
            final Path targetPath = targetDirPath.resolve(annotatedName);
            ImageIO.write(off_Image, "png", targetPath.toFile());
        }
    }

    //-----------//
    // histogram //
    //-----------//
    /**
     * Retrieve and print out the histogram of class instances in the source dataset.
     *
     * @throws Exception
     */
    private void histogram ()
        throws Exception
    {
        logger.info("Histogram task...");
        final TreeMap<String, Tuple> map = new TreeMap<>();
        partHistogram(map, YoloPart.train);
        partHistogram(map, YoloPart.val);

        System.out.println("Histogram:");
        System.out.format("| %-38s | %6s | %6s |%n", "Class", "Train", "Val");
        System.out.format("| %-38s | %6s | %6s |%n", ":---", "---:", "---:");
        map.entrySet().forEach(e -> {
            final Tuple tuple = e.getValue();
            System.out.format("| %-38s | %6d | %6d |%n", e.getKey(), tuple.train, tuple.val);
        });
        System.out.println();
    }

    //----------//
    // isHidden //
    //----------//
    /**
     * Report whether the provided class name should not be dran.
     *
     * @param className the provided class name
     * @return true if do
     */
    protected boolean isHidden (String className)
    {
        final List<String> hiddens = config.checking.hidden_labels;

        return ((hiddens != null) && hiddens.contains(className));
    }

    //--------------------//
    // prepareYoloFolders //
    //--------------------//
    /**
     * Prepare the target YOLO folders structure.
     *
     * @param the YOLO configuration
     * @throws Exception
     */
    private void prepareYoloFolders (YoloConfig config)
        throws Exception
    {
        final Path yoloDir = Paths.get(config.target);
        Files.createDirectories(yoloDir);

        final Path imagesDir = yoloDir.resolve("images");
        Files.createDirectories(imagesDir);

        trainImages = imagesDir.resolve("train");
        Files.createDirectories(trainImages);

        valImages = imagesDir.resolve("val");
        Files.createDirectories(valImages);

        final Path labelsDir = yoloDir.resolve("labels");
        Files.createDirectories(labelsDir);

        trainLabels = labelsDir.resolve("train");
        Files.createDirectories(trainLabels);

        valLabels = labelsDir.resolve("val");
        Files.createDirectories(valLabels);

        // Print out the YOLO labels id and name?
        if (config.print_labels) {
            System.out.println("\nYOLO labels ID and name:");
            for (YoloLabel yl : YoloLabel.values()) {
                System.out.println(String.format("  %d: %s", yl.ordinal(), yl));
            }
        }
    }

    //---------//
    // process //
    //---------//
    public void process ()
        throws Exception
    {
        if (config.tasks != null && config.tasks.contains("histogram")) {
            histogram();
        } else {
            logger.debug("No histogram task");
        }

        if (config.tasks != null && config.tasks.contains("checking")) {
            final Checking checking = config.checking;
            if (checking != null)
                check();
            else {
                logger.debug("No checking specified");
            }
        } else {
            logger.debug("No checking task");
        }

        for (YoloPart part : YoloPart.values()) {
            if (config.tasks != null && config.tasks.contains(part.name())) {
                addPart(
                        part,
                        (part == YoloPart.train) ? trainImages : valImages,
                        (part == YoloPart.train) ? trainLabels : valLabels);
            } else {
                logger.debug("No {} task", part);
            }
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------//
    // main //
    //------//
    public static void main (String... args)
        throws Exception
    {
        ///new DeepScores("yolo.yaml", "deepscores.yaml").process();
        new DoReMi("yolo.yaml", "doremi.yaml").process();
    }

    public static String radixOf (String fileName)
    {
        final int dot = fileName.lastIndexOf('.');
        return fileName.substring(0, dot);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-------//
    // Tuple // Used for populating histogram of instances
    //-------//
    protected static class Tuple
    {
        public int train = 0;

        public int val = 0;
    }

    //------------//
    // YoloConfig // Descriptor of a Yolo configuration
    //------------//
    private static class YoloConfig
    {
        public String target;

        public boolean print_labels;

        @Override
        public String toString ()
        {
            final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
            sb.append(" {")//
                    .append("\n  target:").append(target)//
                    .append("\n  print_labels:").append(print_labels);

            return sb.append("\n}").toString();
        }
    }

    //----------//
    // YoloPart //
    //----------//
    public static enum YoloPart
    {
        train, // Part for training
        val; // Part for validation
    }
}
