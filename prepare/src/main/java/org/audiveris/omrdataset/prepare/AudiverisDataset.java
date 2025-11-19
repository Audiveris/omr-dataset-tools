//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 A u d i v e r i s D a t a s e t                                //
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
//  </editor-fold>
package org.audiveris.omrdataset.prepare;

import org.audiveris.omr.util.Jaxb;
import static org.audiveris.omrdataset.prepare.DataSetFactory.yamlMapper;
import org.audiveris.omrdataset.prepare.api.OmrShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>AudiverisDataset</code> includes the Audiveris dataset into the YOLO dataset.
 * <p>
 * We are interested in these elements:
 * <ul>
 * <li>./ folder gathering images (such as foo#1.png)
 * <li>./ folder gathering XML annotations (such as foo#1.annotations.xml)
 * </ul>
 * An XML annotations file is organized as follows:
 *
 * <pre>
 *  &lt;?xml version="1.0" ?>
 *  &lt;Annotations version="1.0">
 *       &lt;Source>Audiveris 5.8.0&lt;/Source>
 *       &lt;Page>
 *           &lt;Image>Rigaudon Vladigerov - Viola#1.png&lt;/Image>
 *           &lt;Size w="2483" h="3508"/>
 *       &lt;/Page>
 *       &lt;Symbol interline="23" id="2" shape="barlineSingle">
 *           &lt;Bounds x="575" y="715" w="4" h="95"/>
 *       &lt;/Symbol>
 *      &lt;Symbol interline="23" id="223" shape="cClefAlto">
 *          &lt;Bounds x="246" y="715" w="64" h="93"/>
 *      &lt;/Symbol>
 *      ...
 *  &lt;/Annotations>
 * </pre>
 *
 * @author Hervé Bitteur
 */
public class AudiverisDataset
        extends DataSetFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AudiverisDataset.class);

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------

    private final Unmarshaller um;

    private final Path annotationsPath;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a new <code>AudiverisDataset</code> instance.
     *
     * @param sourceConfig path to AudiverisDataset .yaml config
     * @throws java.lang.Exception
     */
    public AudiverisDataset (String sourceConfig)
            throws Exception
    {
        logger.info("Audiveris dataset");

        config = yamlMapper.readValue(Paths.get(sourceConfig).toFile(), AudiverisConfig.class);
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
    protected void addPart (YoloPart part,
                            Path outImages,
                            Path outLabels)
        throws Exception
    {

        // For the count of Yolo labels
        final TreeMap<YoloLabel, Integer> labelCounts = new TreeMap<>();
        for (YoloLabel l : YoloLabel.values()) {
            labelCounts.put(l, 0);
        }

        // For images listing
        System.out.format("%nPart %s. Listing of images:%n", part);
        System.out.println("| Rank  | Width | Height | Instances | Image |");
        System.out.println("|  ---: |  ---: |   ---: |      ---: | :---  |");

        // For the count of Audiveris OmrShape ignored labels
        final TreeMap<OmrShape, Integer> ignoredCounts = new TreeMap<>();
        //        for (OmrShape l : OmrShape.values()) {
        //            if (BeethovenLabel.of(l) == null)
        //                ignoredCounts.put(l, 0);
        //        }

        final List<String> imgNames = (part == YoloPart.train) ? config.train : config.val;
        if (imgNames == null || imgNames.isEmpty()) {
            logger.info("No file names for part {}", part);
            return;
        }

        final int total = imgNames.size();
        int rank = 0;

        for (String imgName : imgNames) {
            final Path imgPath = imagesPath.resolve(imgName);

            // Copy image to target location
            Files.copy(imgPath, outImages.resolve(imgName), StandardCopyOption.REPLACE_EXISTING);

            // Get the related annotations
            final String xmlName = xmlNameOf(imgName);
            final Path annPath = annotationsPath.resolve(xmlName);
            final Annotations annotations = (Annotations) um.unmarshal(annPath.toFile());

            // Get image dimension
            final Dimension dim = annotations.sheetInfo.dim;
            final int imgWidth = dim.width;
            final int imgHeight = dim.height;

            // Line in images listing
            System.out.format(
                    "| %4d/%4d | %4d | %4d | %4d | %s |%n",
                    ++rank,
                    total,
                    imgWidth,
                    imgHeight,
                    annotations.symbols.size(),
                    imgName);

            // Generate the related label file
            final String labelFile = outLabels.resolve(radixOf(imgName) + ".txt").toString();

            try (PrintWriter writer = new PrintWriter(labelFile)) {
                //                for (Node node : page.Nodes) {
                //                    final String className = legalNameOf(node.name); // Spe
                //
                //                    // Find the corresponding YoloLabel, if any
                //                    final BeethovenLabel beeLabel = BeethovenLabel.valueOf(className);
                //                    final YoloLabel yoloLabel = BeethovenLabel.of(beeLabel);
                //
                //                    if (yoloLabel != null) {
                //                        labelCounts.put(yoloLabel, labelCounts.get(yoloLabel) + 1);
                //
                //                        writer.printf(
                //                                "%3d %f %f %f %f%n",
                //                                yoloLabel.ordinal(),
                //                                (node.bndbox.xmin + node.bndbox.xmax) / 2, // xc
                //                                (node.bndbox.ymin + node.bndbox.ymax) / 2, // yc
                //                                (node.bndbox.xmax - node.bndbox.xmin), // w
                //                                (node.bndbox.ymax - node.bndbox.ymin)); // h
                //                    } else {
                //                        ignoredCounts.put(beeLabel, ignoredCounts.get(beeLabel) + 1);
                //                    }
                //                }
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

        return false;
    }

    @Override
    protected void drawAnnotations (String imgName,
                                    Graphics2D g2d)
        throws Exception
    {
        final String xmlName = xmlNameOf(imgName);
        final Path annPath = annotationsPath.resolve(xmlName);
        final Annotations annotations = (Annotations) um.unmarshal(annPath.toFile());
        System.out.println(imgName + " " + annotations);

        for (SymbolInfo symbol : annotations.symbols) {
            final OmrShape className = symbol.omrShape;
            final Rectangle box = symbol.bounds;

            // Draw obj rectangle
            g2d.draw(box);

            // Draw class ID
            g2d.drawString(className.name(), box.x, box.y - 1);
        }
    }

    @Override
    protected void partHistogram (TreeMap<String, Tuple> map,
                                  YoloPart part)
        throws Exception
    {
        throw new UnsupportedOperationException("Not supported yet.");
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
            jaxbContext = JAXBContext.newInstance(Annotations.class);
        }

        return jaxbContext;
    }

    //-----------//
    // xmlNameOf //
    //-----------//
    /**
     * Generate the name of the .xml file that corresponds to the provided .png file.
     * <p>
     * Example:
     * -input : Autumn from The Four Seasons 1st Violin-#1.png
     * output : Autumn from The Four Seasons 1st Violin-#1.annotations.xml
     *
     * @param imgName name of the .png file
     * @return the corresponding xmlName
     */
    private static String xmlNameOf (String imgName)
    {
        return new StringBuilder() //
                .append(radixOf(imgName)) //
                .append(".annotations.xml") //
                .toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-------------//
    // Annotations //
    //-------------//
    @XmlAccessorType(XmlAccessType.NONE)
    @XmlRootElement(name = "Annotations")
    private static class Annotations
    {
        @XmlAttribute(name = "version")
        private String version;

        @XmlElement(name = "Source")
        private String source;

        @XmlElement(name = "Page")
        private SheetInfo sheetInfo;

        @XmlElement(name = "Symbol")
        private ArrayList<SymbolInfo> symbols = new ArrayList<>();

        @Override
        public String toString ()
        {
            return new StringBuilder().append('{')//
                    .append("width:").append(sheetInfo.dim.width) //
                    .append(" height:").append(sheetInfo.dim.height) //
                    .append(" symbols:").append(symbols.size()) //
                    .append('}').toString();
        }
    }

    //-----------------//
    // AudiverisConfig //
    //-----------------//
    private static class AudiverisConfig
            extends DataSetConfig
    {
        public AudiverisConfig ()
        {
        }
    }

    //------------------//
    // DimensionAdapter //
    //------------------//
    /**
     * Adapter for Dimension.
     */
    private static class DimensionAdapter
            extends XmlAdapter<DimensionAdapter.DimensionFacade, Dimension>
    {
        @Override
        public DimensionFacade marshal (Dimension dim)
            throws Exception
        {
            if (dim == null) {
                return null;
            }

            return new DimensionFacade(dim);
        }

        @Override
        public Dimension unmarshal (DimensionFacade facade)
            throws Exception
        {
            if (facade == null) {
                return null;
            }

            return facade.getDimension();
        }

        /**
         * Class <code>DimensionFacade</code> is a JAXB-compatible facade for predefined
         * {@link java.awt.geom.Dimension} class.
         */
        @XmlRootElement(name = "dimension")
        private static class DimensionFacade
        {
            /** The width dimension. */
            @XmlAttribute(name = "w")
            public int width;

            /** The height dimension. */
            @XmlAttribute(name = "h")
            public int height;

            /**
             * Needed for JAXB.
             */
            @SuppressWarnings("unused")
            private DimensionFacade ()
            {
            }

            /**
             * Creates a new <code>DimensionFacade</code> object.
             *
             * @param dimension the interfaced dimension
             */
            DimensionFacade (Dimension dimension)
            {
                width = dimension.width;
                height = dimension.height;
            }

            public Dimension getDimension ()
            {
                return new Dimension(width, height);
            }
        }
    }

    //-----------//
    // SheetInfo //
    //-----------//
    public static class SheetInfo
    {
        @XmlElement(name = "Image")
        public final String imageFileName;

        @XmlElement(name = "Size")
        @XmlJavaTypeAdapter(DimensionAdapter.class)
        public final Dimension dim;

        // No-argument constructor needed by JAXB
        private SheetInfo ()
        {
            this.imageFileName = null;
            this.dim = null;
        }

        public SheetInfo (String imageFileName,
                          Dimension dim)
        {
            this.imageFileName = imageFileName;
            this.dim = dim;
        }

        @Override
        public String toString ()
        {
            return "{" + imageFileName + " [width=" + dim.width + ",height=" + dim.height + "]}";
        }
    }

    //------------//
    // SymbolInfo //
    //------------//
    @XmlAccessorType(XmlAccessType.NONE)
    private static class SymbolInfo
    {
        //~ Static fields/initializers -----------------------------------------------------------------

        private static final Logger logger = LoggerFactory.getLogger(SymbolInfo.class);

        //~ Instance fields ----------------------------------------------------------------------------

        @XmlAttribute(name = "interline")
        private final int interline;

        @XmlAttribute(name = "id")
        private Integer id;

        @XmlAttribute(name = "shape")
        @XmlJavaTypeAdapter(OmrShapeAdapter.class)
        private OmrShape omrShape;

        @XmlElement(name = "Bounds")
        @XmlJavaTypeAdapter(Jaxb.RectangleAdapter.class)
        private final Rectangle bounds;

        /**
         * No-argument constructor needed for JAXB.
         */
        private SymbolInfo ()
        {
            omrShape = null;
            interline = 0;
            id = null;
            bounds = null;
        }

        /**
         * Creates a new <code>SymbolInfo</code> object.
         *
         * @param omrShape  symbol OMR shape
         * @param interline related interline
         * @param id        symbol id, if any
         * @param scale     ratio WRT standard symbol size, optional
         * @param bounds    symbol bounding box within containing image
         */
        public SymbolInfo (OmrShape omrShape,
                           int interline,
                           Integer id,
                           Rectangle bounds)
        {
            this.omrShape = omrShape;
            this.interline = interline;
            this.id = id;
            this.bounds = bounds;
        }

        /**
         * Called after all the properties (except IDREF) are unmarshalled
         * for this object, but before this object is set to the parent object.
         */
        @SuppressWarnings("unused")
        private void afterUnmarshal (Unmarshaller um,
                                     Object parent)
        {
            if (omrShape == null) {
                logger.warn("*** Null shape {}", this);
            }
        }

        /**
         * @return a COPY of the bounds
         */
        public Rectangle getBounds ()
        {
            Rectangle copy = new Rectangle();
            copy.setRect(bounds);

            return copy;
        }

        /**
         * Report symbol id (a positive integer)
         *
         * @return symbol id or 0
         */
        public int getId ()
        {
            if (id == null) {
                return 0;
            }

            return id;
        }

        /**
         * @return the interline
         */
        public double getInterline ()
        {
            return interline;
        }

        /**
         * @return the omrShape, perhaps null
         */
        public OmrShape getOmrShape ()
        {
            return omrShape;
        }

        /**
         * Assign ID value.
         *
         * @param id new ID value
         */
        public void setId (int id)
        {
            this.id = id;
        }

        /**
         * @param omrShape the omrShape to set
         */
        private void setOmrShape (OmrShape omrShape)
        {
            logger.debug("Renamed scaled {} as {}", this, omrShape);
            this.omrShape = omrShape;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("Symbol{");
            sb.append("shape:").append(omrShape);

            sb.append(" interline:").append(interline);

            if (id != null) {
                sb.append(" id:").append(id);
            }

            sb.append(" ").append(bounds);

            sb.append("}");

            return sb.toString();
        }

        //-----------------//
        // OmrShapeAdapter //
        //-----------------//
        /**
         * We need a specific adapter to warn about unknown shape names.
         */
        public static class OmrShapeAdapter
                extends XmlAdapter<String, OmrShape>
        {
            @Override
            public String marshal (OmrShape shape)
                throws Exception
            {
                if (shape == null) {
                    return null;
                }

                return shape.toString();
            }

            @Override
            public OmrShape unmarshal (String string)
                throws Exception
            {
                try {
                    return OmrShape.valueOf(string);
                } catch (IllegalArgumentException ex) {
                    logger.warn("*** Unknown shape name: {}", string);

                    return null;
                }
            }
        }
    }
}
