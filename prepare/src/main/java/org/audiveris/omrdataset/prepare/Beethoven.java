//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B e e t h o v e n                                       //
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

import static org.audiveris.omrdataset.prepare.DataSetFactory.radixOf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>Beethoven</code> includes the Beethoven dataset into the YOLO training dataset.
 * <p>
 * We are interested in these elements:
 * <ul>
 * <li>./ folder gathering 190 images
 * <li>./ folder gathering 190 XML annotations
 * </ul>
 * An XML annotation file is organized as follows:
 *
 * <pre>
 * &lt;annotation>
 *     &lt;folder>_local/beethoven&lt;/folder>
 *     &lt;filename>String_Quartet_No.4_Op.18_No.4__Ludwig_van_Beethoven.mei~Bravura~score_album_2.png&lt;/filename>
 *     &lt;font>Overpass&lt;/font>
 *     &lt;size>
 *         &lt;width>2480&lt;/width>
 *         &lt;height>3508&lt;/height>
 *         &lt;depth>1&lt;/depth>
 *     &lt;/size>
 *     &lt;segmented> &lt;/segmented>
 *     &lt;object>
 *         &lt;name>systemBoundingBox&lt;/name>
 *         &lt;superclass>SystemBoundingBox&lt;/superclass>
 *         &lt;uuid>se33py9&lt;/uuid>
 *         &lt;color>165,14,36&lt;/color>
 *         &lt;hexcolor>#A50E24&lt;/hexcolor>
 *         &lt;unique_color>122,195,19&lt;/unique_color>
 *         &lt;unique_hexcolor>#7AC313&lt;/unique_hexcolor>
 *         &lt;bndbox>
 *             &lt;xmin>0.01637&lt;/xmin>
 *             &lt;xmax>0.9757&lt;/xmax>
 *             &lt;ymin>0.01725&lt;/ymin>
 *             &lt;ymax>0.22434&lt;/ymax>
 *         &lt;/bndbox>
 *     &lt;/object>
 *     &lt;object>
 *          ...
 *     &lt;/object>
 * ...
 * </pre>
 *
 * @author Hervé Bitteur
 */
public class Beethoven
        extends DataSetFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Beethoven.class);

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------

    private final Unmarshaller um;

    private final Path annotationsPath;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a new <code>Beethoven</code> instance.
     *
     * @param targetConfig path to Yolo .yaml config
     * @param sourceConfig path to Beethoven .yaml config
     * @throws java.lang.Exception
     */
    public Beethoven (String targetConfig,
                      String sourceConfig)
            throws Exception
    {
        super(targetConfig);

        logger.info("Beethoven dataset");

        config = yamlMapper.readValue(Paths.get(sourceConfig).toFile(), BeethovenConfig.class);
        logger.info("{}", config);

        sourceDir = Paths.get(config.source);
        imagesPath = sourceDir.resolve(config.images);
        annotationsPath = sourceDir.resolve(config.annotations);

        um = getJaxbContext().createUnmarshaller();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // addPart //
    //---------//
    @Override
    public void addPart (YoloPart part,
                         Path outImages,
                         Path outLabels)
        throws Exception
    {
        logger.info("Part {}. Start processing...", part);

        // For the count of Yolo labels
        final TreeMap<YoloLabel, Integer> labelCounts = new TreeMap<>();
        for (YoloLabel l : YoloLabel.values()) {
            labelCounts.put(l, 0);
        }

        // For images listing
        System.out.format("%nPart %s. Listing of images:%n", part);
        System.out.println("| Width | Height | Instances | Image |");
        System.out.println("|  ---: |   ---: |      ---: | :---  |");

        // For the count of Beethoven ignored labels
        final TreeMap<BeethovenLabel, Integer> ignoredCounts = new TreeMap<>();
        for (BeethovenLabel l : BeethovenLabel.values()) {
            if (BeethovenLabel.of(l) == null)
                ignoredCounts.put(l, 0);
        }

        final List<String> imgNames = (part == YoloPart.train) ? config.train : config.val;
        if (imgNames == null || imgNames.isEmpty()) {
            logger.info("No file names for part {}", part);
            return;
        }

        for (String imgName : imgNames) {
            final Path imgPath = imagesPath.resolve(imgName);

            // Copy image to target location
            Files.copy(imgPath, outImages.resolve(imgName), StandardCopyOption.REPLACE_EXISTING);

            // Get image dimension
            final BufferedImage img = ImageIO.read(imgPath.toFile());
            final int imgWidth = img.getWidth();
            final int imgHeight = img.getHeight();

            // Get the related annotations
            final String xmlName = xmlNameOf(imgName);
            final Path annPath = annotationsPath.resolve(xmlName);
            final Annotation page = (Annotation) um.unmarshal(annPath.toFile());

            // Line in images listing
            System.out.format(
                    "| %4d | %4d | %4d | %s |%n",
                    imgWidth,
                    imgHeight,
                    page.Nodes.size(),
                    imgName);

            // Generate the related label file
            final String labelFile = outLabels.resolve(radixOf(imgName) + ".txt").toString();

            try (PrintWriter writer = new PrintWriter(labelFile)) {
                for (Node node : page.Nodes) {
                    final String className = legalNameOf(node.name); // Spe

                    // Find the corresponding YoloLabel, if any
                    final BeethovenLabel beeLabel = BeethovenLabel.valueOf(className);
                    final YoloLabel yoloLabel = BeethovenLabel.of(beeLabel);

                    if (yoloLabel != null) {
                        labelCounts.put(yoloLabel, labelCounts.get(yoloLabel) + 1);

                        writer.printf(
                                "%3d %f %f %f %f%n",
                                yoloLabel.ordinal(),
                                (node.bndbox.xmin + node.bndbox.xmax) / 2, // xc
                                (node.bndbox.ymin + node.bndbox.ymax) / 2, // yc
                                (node.bndbox.xmax - node.bndbox.xmin), // w
                                (node.bndbox.ymax - node.bndbox.ymin)); // h
                    } else {
                        ignoredCounts.put(beeLabel, ignoredCounts.get(beeLabel) + 1);
                    }
                }
            }
        }

        // Print out the histogram count for each YOLO label
        System.out.format("\nPart %s. Counts of YOLO labels:\n", part);
        labelCounts.entrySet().forEach(
                entry -> System.out.format("%6d : %s%n", entry.getValue(), entry.getKey()));

        // Print out the histogram count for the ignored DRM labels
        System.out.format("\nPart %s. Counts of Beethoven labels ignored:\n", part);
        ignoredCounts.entrySet().forEach(entry -> {
            if (entry.getValue() != null)
                System.out.format("%6d : %s%n", entry.getValue(), entry.getKey());
        });
    }

    @Override
    protected boolean conditionMet (String imgName)
        throws Exception
    {
        // Check at least one required label is present in this page
        if (config.checking.required_labels == null) {
            return true;
        }

        final String xmlName = xmlNameOf(imgName);
        final Path annPath = annotationsPath.resolve(xmlName);
        final Annotation page = (Annotation) um.unmarshal(annPath.toFile()); // Spe

        // Normalizing dimensions (Spe)
        final int imgWidth = page.size.width;
        final int imgHeight = page.size.height;

        for (Node node : page.Nodes) {
            final String className = legalNameOf(node.name); // Spe
            if (config.checking.required_labels.contains(className)) {
                final int x = (int) Math.rint(node.bndbox.xmin * imgWidth);
                final int y = (int) Math.rint(node.bndbox.ymin * imgHeight);
                final int w = (int) Math.rint((node.bndbox.xmax - node.bndbox.xmin) * imgWidth);
                final int h = (int) Math.rint((node.bndbox.ymax - node.bndbox.ymin) * imgHeight);
                System.out.format("%s at [x:%d, y:%d, w:%d, h:%d]%n", className, x, y, w, h);
                return true;
            }
        }

        return false;
    }

    @Override
    protected void drawAnnotations (String imgName,
                                    Graphics2D g2d)
        throws Exception
    {
        final String xmlName = xmlNameOf(imgName);
        final Path annPath = annotationsPath.resolve(xmlName);
        final Annotation page = (Annotation) um.unmarshal(annPath.toFile()); // Spe
        System.out.println(imgName + " " + page);

        // Normalizing dimensions (Spe)
        final int imgWidth = page.size.width;
        final int imgHeight = page.size.height;

        for (Node node : page.Nodes) {
            final String className = legalNameOf(node.name); // Spe
            if (isHidden(className)) {
                continue;
            }
            // Spe
            final int x = (int) Math.rint(node.bndbox.xmin * imgWidth);
            final int y = (int) Math.rint(node.bndbox.ymin * imgHeight);
            final int w = (int) Math.rint((node.bndbox.xmax - node.bndbox.xmin) * imgWidth);
            final int h = (int) Math.rint((node.bndbox.ymax - node.bndbox.ymin) * imgHeight);

            // Draw obj rectangle
            g2d.drawRect(x, y, w, h);

            // Draw class ID
            g2d.drawString(className, x, y - 1);
        }
    }

    @Override
    protected void partHistogram (TreeMap<String, Tuple> map,
                                  YoloPart part)
        throws Exception
    {
        final List<String> imgNames = (part == YoloPart.train) ? config.train : config.val;
        if (imgNames == null || imgNames.isEmpty()) {
            logger.info("No file names for part {}", part);
            return;
        }

        for (String imgName : imgNames) {
            final String xmlName = xmlNameOf(imgName);
            final Path annPath = annotationsPath.resolve(xmlName);
            final Annotation annotation = (Annotation) um.unmarshal(annPath.toFile());

            for (Node node : annotation.Nodes) {
                final String className = node.name;

                Tuple tuple = map.get(className);

                if (tuple == null) {
                    map.put(className, tuple = new Tuple());
                }

                if (part == YoloPart.train) {
                    tuple.train++;
                } else {
                    tuple.val++;
                }
            }
        }
    }

    private String legalNameOf (String name)
    {
        if (name.equals("dynam-f")) {
            return "dynamicF";
        }
        if (name.equals("dynam-p")) {
            return "dynamicP";
        }

        return name;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
        throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(Annotation.class);
        }

        return jaxbContext;
    }

    //-----------//
    // xmlNameOf //
    //-----------//
    /**
     * Generate the name of the .xml file that corresponds to the provided .png file.
     * <p>
     * Example 1:
     * -input : String_Quartet_No.4_Op.18_No.4__Ludwig_van_Beethoven.mei~Bravura~score_album_13.png
     * output : String_Quartet_No.4_Op.18_No.4__Ludwig_van_Beethoven.mei~Bravura~annotations_13.png
     * <p>
     * Example 2:
     * -input : String_Quartet_No.4_Op.18_No.4__Ludwig_van_Beethoven.mei~Bravura~score_page_13.png
     * output : String_Quartet_No.4_Op.18_No.4__Ludwig_van_Beethoven.mei~Bravura~annotations_13.png
     *
     * @param imgName name of the .png file
     * @return the corresponding xmlName
     */
    private static String xmlNameOf (String imgName)
    {
        final String radix = radixOf(imgName);

        final int tilde = radix.lastIndexOf('~');
        final String name = radix.substring(0, tilde);

        final int underscore = radix.lastIndexOf('_');
        final String numStr = radix.substring(underscore + 1);

        return new StringBuilder() //
                .append(name) //
                .append("~annotations_") //
                .append(numStr) //
                .append(".xml") //
                .toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //------------//
    // Annotation //
    //------------//
    @XmlRootElement(name = "annotation")
    private static class Annotation
    {
        /** Page dimensions. */
        public Size size;

        /** List of objects. */
        @XmlElement(name = "object")
        public List<Node> Nodes = new ArrayList<>();

        private Annotation () // No-argument constructor meant for JAXB
        {
        }

        @Override
        public String toString ()
        {
            return new StringBuilder() //
                    .append(" size:").append(size) //
                    .append(" Nodes.size:").append(Nodes.size()) //
                    .toString();
        }
    }

    //------//
    // Node //
    //------//
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class Node
    {
        public String name;

        //  superclass      ignored
        //  uuid            ignored
        //  color           ignored
        //  hexcolor        ignored
        //  unique_color    ignored
        //  unique_hexcolor ignored

        public BBox bndbox;

        private Node () // No-argument constructor meant for JAXB
        {
        }

        @Override
        public String toString ()
        {
            return new StringBuilder() //
                    .append("{name:").append(name) //
                    .append(" bndbox:").append(bndbox) //
                    .append('}').toString();
        }
    }

    //------//
    // BBox //
    //------//
    /**
     * Abscissa and ordinate values are normalized by the image width and height.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class BBox
    {
        public double xmin;

        public double xmax;

        public double ymin;

        public double ymax;

        private BBox () // No-argument constructor meant for JAXB
        {
        }

        @Override
        public String toString ()
        {
            return new StringBuilder() //
                    .append("{xmin:").append(xmin) //
                    .append(" xmax:").append(xmax) //
                    .append(" ymin:").append(ymin) //
                    .append(" ymax:").append(ymax) //
                    .append('}').toString();
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    private static class Size
    {
        public int width;

        public int height;

        // depth ignored
        private Size () // No-argument constructor meant for JAXB
        {
        }

        @Override
        public String toString ()
        {
            return new StringBuilder() //
                    .append("{width:").append(width) //
                    .append(" height:").append(height) //
                    .append('}').toString();
        }
    }
}
