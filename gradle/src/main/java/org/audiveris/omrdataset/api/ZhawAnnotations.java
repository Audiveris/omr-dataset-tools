//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  Z h a w A n n o t a t i o n s                                 //
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
package org.audiveris.omrdataset.api;

import org.audiveris.omr.util.Jaxb;
import org.audiveris.omrdataset.api.SheetAnnotations.SheetInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.audiveris.omr.util.FileUtil;

/**
 * Class {@code ZhawAnnotations} describes the ZHAW annotations available in their .xml
 * files.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "annotation")
public class ZhawAnnotations
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(ZhawAnnotations.class);

    public static final int ZHAW_INTERLINE = 23;

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------
    @XmlElement(name = "folder")
    public String folder;

    @XmlElement(name = "filename")
    public String filename;

    @XmlElement(name = "size")
    public ZhawSize size;

    @XmlElement(name = "segmented")
    public Integer segmented;

    /** List of symbols. NOTA: No nesting exists. */
    @XmlElement(name = "object")
    public ArrayList<ZhawObject> objects = new ArrayList<>();

    //~ Constructors -------------------------------------------------------------------------------
    public ZhawAnnotations ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------------//
    // toSheetAnnotations //
    //--------------------//
    public SheetAnnotations toSheetAnnotations ()
    {
        SheetAnnotations sa = new SheetAnnotations();

        sa.setSource("ZHAW");
        sa.setSheetInfo(new SheetInfo(filename, new Dimension(size.width, size.height)));

        for (int i = 0; i < objects.size(); i++) {
            ZhawObject obj = objects.get(i);
            SymbolInfo symbol = new SymbolInfo(
                    obj.omrShape,
                    ZHAW_INTERLINE,
                    i + 1,
                    null,
                    obj.bndbox.toRectangle2D(size));
            sa.addSymbol(symbol);
        }

        return sa;
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Load ZhawAnnotations from the annotations XML file.
     *
     * @param path to the XML input file.
     * @return the unmarshalled ZhawAnnotations object
     * @throws IOException   in case of IO problem
     * @throws JAXBException in case of JAXB problem
     */
    public static ZhawAnnotations unmarshal (Path path)
            throws IOException,
                   JAXBException
    {
        ZhawAnnotations za = (ZhawAnnotations) Jaxb.unmarshal(path, getJaxbContext());

        // Hack: replace .svg by .png as filename extension
        String radix = FileUtil.sansExtension(za.filename);
        za.filename = radix + ".png";

        return za;
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
            throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(ZhawAnnotations.class);
        }

        return jaxbContext;
    }

    public static void main (String[] args)
            throws Exception
    {
        Path dir = Paths.get("D:/soft/DeepScores/DeepScores_archive0/xml_annotations");
        Path input = dir.resolve("lg-46690-aug-gutenberg1939-.xml");

        ZhawAnnotations za = unmarshal(input);
        logger.info("za: {}", za);

        SheetAnnotations sa = za.toSheetAnnotations();
        logger.info("annot: {}", sa);

        for (SymbolInfo s : sa.getOuterSymbolsLiveList()) {
            logger.info("   {}", s);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    @XmlAccessorType(XmlAccessType.NONE)
    public static class ZhawSize
    {

        @XmlElement(name = "width")
        public int width;

        @XmlElement(name = "height")
        public int height;

        @XmlElement(name = "depth")
        public int depth;

        public ZhawSize ()
        {
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class ZhawBox
    {

        @XmlElement(name = "xmin")
        public double xmin;

        @XmlElement(name = "xmax")
        public double xmax;

        @XmlElement(name = "ymin")
        public double ymin;

        @XmlElement(name = "ymax")
        public double ymax;

        public ZhawBox ()
        {
        }

        public Rectangle2D toRectangle2D (ZhawSize size)
        {
            double x = xmin * size.width;
            double width = xmax * size.width - x;
            double y = ymin * size.height;
            double height = ymax * size.height - y;

            return new Rectangle2D.Double(x, y, width, height);
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class ZhawObject
    {

        @XmlElement(name = "name")
        @XmlJavaTypeAdapter(SymbolInfo.OmrShapeAdapter.class)
        public OmrShape omrShape;

        @XmlElement(name = "bndbox")
        public ZhawBox bndbox;

        public ZhawObject ()
        {
        }
    }
}
