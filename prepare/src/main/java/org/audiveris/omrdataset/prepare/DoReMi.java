//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           D o R e M i                                          //
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
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
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>DoReMi</code> includes the DoReMi dataset into the YOLO training dataset.
 * <p>
 * We are interested in these elements:
 * <ul>
 * <li>"Images" folder gathering 5218 images
 * <li>"Parsed_by_page_omr_xml" folder gathering 5218 XML annotations
 * </ul>
 * An XML annotation is organized as follows:
 *
 * <pre>
 * &lt;Page pageIndex="0">
 *	&lt;Nodes>
 *		&lt;Node>
 *			&lt;Id>103&lt;/Id>
 *			&lt;ClassName>timeSig4&lt;/ClassName>
 *			&lt;Top>709&lt;/Top>
 * 			&lt;Left>386&lt;/Left>
 *			&lt;Width>36&lt;/Width>
 *			&lt;Height>42&lt;/Height>
 *			&lt;Mask>0:11 1:16 ... &lt;/Mask>
 *		&lt;/Node>
 * ...
 * </pre>
 *
 * @author Hervé Bitteur
 */
public class DoReMi
        extends DataSetFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DoReMi.class);

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------

    private final Unmarshaller um;

    private final Path annotationsPath;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a new <code>DoReMi</code> instance.
     *
     * @param targetConfig path to Yolo .yaml config
     * @param sourceConfig path to DoReMi .yaml config
     * @throws java.lang.Exception
     */
    public DoReMi (String targetConfig,
                   String sourceConfig)
            throws Exception
    {
        super(targetConfig);

        logger.info("DoReMi dataset");

        config = yamlMapper.readValue(Paths.get(sourceConfig).toFile(), DoReMiConfig.class);
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

        // For the count of DoReMi ignored labels
        final TreeMap<DoReMiLabel, Integer> ignoredCounts = new TreeMap<>();
        for (DoReMiLabel l : DoReMiLabel.values()) {
            if (DoReMiLabel.of(l) == null)
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
            final Page page = (Page) um.unmarshal(annPath.toFile());

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
                    final String className = node.ClassName;

                    // Find the corresponding YoloLabel, if any
                    final DoReMiLabel drmLabel = DoReMiLabel.valueOf(className);
                    final YoloLabel yoloLabel = DoReMiLabel.of(drmLabel);

                    if (yoloLabel != null) {
                        labelCounts.put(yoloLabel, labelCounts.get(yoloLabel) + 1);

                        final int x = node.Left;
                        final int y = node.Top;
                        final int w = node.Width;
                        final int h = node.Height;

                        writer.printf(
                                "%3d %f %f %f %f%n",
                                yoloLabel.ordinal(),
                                (x + w / 2.0) / imgWidth,
                                (y + h / 2.0) / imgHeight,
                                w / (double) imgWidth,
                                h / (double) imgHeight);
                    } else {
                        ignoredCounts.put(drmLabel, ignoredCounts.get(drmLabel) + 1);
                    }
                }
            }
        }

        // Print out the histogram count for each YOLO label
        System.out.format("\nPart %s. Counts of YOLO labels:\n", part);
        labelCounts.entrySet().forEach(
                entry -> System.out.format("%6d : %s%n", entry.getValue(), entry.getKey()));

        // Print out the histogram count for the ignored DRM labels
        System.out.format("\nPart %s. Counts of DoReMi labels ignored:\n", part);
        ignoredCounts.entrySet().forEach(entry -> {
            if (entry.getValue() != null)
                System.out.format("%6d : %s%n", entry.getValue(), entry.getKey());
        });
    }

    @Override
    protected void drawAnnotations (String imgName,
                                    Graphics2D g2d)
        throws Exception
    {
        final String xmlName = xmlNameOf(imgName);
        final Path annPath = annotationsPath.resolve(xmlName);
        final Page page = (Page) um.unmarshal(annPath.toFile());
        System.out.println(imgName + " " + page);

        for (Node node : page.Nodes) {
            final String className = node.ClassName;
            if (isHidden(className)) {
                continue;
            }

            final int x = node.Left;
            final int y = node.Top;
            final int w = node.Width;
            final int h = node.Height;

            // Draw obj rectangle
            g2d.setColor(Color.RED);
            g2d.drawRect(x, y, w, h);

            // Draw class ID
            g2d.setColor(Color.MAGENTA);
            g2d.drawString(className, x, y);
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
            final Page page = (Page) um.unmarshal(annPath.toFile());

            for (Node node : page.Nodes) {
                final String className = node.ClassName;

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

    //~ Static Methods -----------------------------------------------------------------------------

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
        throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(Page.class);
        }

        return jaxbContext;
    }

    //-------------------//
    // trimLeadingZeroes //
    //-------------------//
    private static String trimLeadingZeroes (String str)
    {
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);

            if (c != '0') {
                return str.substring(i);
            }
        }

        return "0";
    }

    //-----------//
    // xmlNameOf //
    //-----------//
    /**
     * Generate the name of the .xml file that corresponds to the provided .png file.
     * <p>
     * Example:
     * input : accidental tucking-001.png
     * output : Parsed_accidental tucking-layout-0-muscima_Page_1.xml
     *
     * @param imgName name of the .png file
     * @return the corresponding xmlName
     */
    private static String xmlNameOf (String imgName)
    {
        final String radix = radixOf(imgName);
        final int dash = radix.lastIndexOf('-');
        final String name = radix.substring(0, dash);

        final String numStr = radix.substring(dash + 1);
        final String num = trimLeadingZeroes(numStr);

        return new StringBuilder("Parsed_") //
                .append(name) //
                .append("-layout-0-muscima_Page_") //
                .append(num) //
                .append(".xml") //
                .toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //------//
    // Page //
    //------//
    /**
     * Class <code>Page</code> is used to unmarshal the page XML description
     */
    @XmlAccessorType(XmlAccessType.NONE)
    @XmlRootElement(name = "Page")
    private static class Page
    {
        /** List of Node histogram. */
        @XmlElementWrapper(name = "Nodes")
        @XmlElement(name = "Node")
        public List<Node> Nodes = new ArrayList<>();

        private Page () // No-argument constructor meant for JAXB
        {
        }

        @Override
        public String toString ()
        {
            return new StringBuilder().append("Nodes.size:").append(Nodes.size()).toString();
        }
    }

    //------//
    // Node //
    //------//
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "Node")
    private static class Node
    {
        //public Integer Id; // Ignored

        public String ClassName;

        public Integer Top;

        public Integer Left;

        public Integer Width;

        public Integer Height;

        //public String Mask; // Ignored

        private Node () // No-argument constructor meant for JAXB
        {
        }
    }
}
