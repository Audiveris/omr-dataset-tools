//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  P a g e A n n o t a t i o n s                                 //
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

import org.omrdataset.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code PageAnnotations} represents the symbols information for a page.
 * It's a sequence of: {symbol name + symbol bounding box
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "Annotations")
public class PageAnnotations
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PageAnnotations.class);

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------
    @XmlAttribute(name = "version")
    private String version;

    @XmlElement(name = "Source")
    private String source;

    @XmlElement(name = "Page")
    private PageInfo pageInfo;

    @XmlElement(name = "Spatium")
    private Integer interline;

    @XmlElement(name = "Symbol")
    private ArrayList<SymbolInfo> symbols = new ArrayList<SymbolInfo>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PageInfo} object.
     */
    public PageAnnotations ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    public List<SymbolInfo> getSymbols ()
    {
        return symbols;
    }

    //-----------//
    // unmarshal //
    //-----------//
    public static PageAnnotations unmarshal (Path path)
            throws IOException
    {
        logger.debug("PageInfo unmarshalling {}", path);

        try {
            InputStream is = Files.newInputStream(path, StandardOpenOption.READ);
            Unmarshaller um = getJaxbContext().createUnmarshaller();
            PageAnnotations pageInfo = (PageAnnotations) um.unmarshal(is);
            logger.debug("Unmarshalled {}", pageInfo);
            is.close();

            return pageInfo;
        } catch (JAXBException ex) {
            logger.warn("Error unmarshalling " + path + " " + ex, ex);

            return null;
        }
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
            throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(PageAnnotations.class);
        }

        return jaxbContext;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Annotations{");
        sb.append("version:").append(version);

        if (source != null) {
            sb.append(" source:").append(source);
        }

        if (pageInfo != null) {
            sb.append(" page:").append(pageInfo);
        }

        if (interline != null) {
            sb.append(" interline:").append(interline);
        }

        sb.append(" symbols:").append(symbols.size());

        sb.append("}");

        return sb.toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // PageInfo //
    //----------//
    private static class PageInfo
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlElement(name = "Image")
        public final String imageFileName;

        @XmlElement(name = "Size")
        @XmlJavaTypeAdapter(Jaxb.DimensionAdapter.class)
        public final Dimension dim;

        //~ Constructors ---------------------------------------------------------------------------
        public PageInfo (String imageFileName,
                         Dimension dim)
        {
            this.imageFileName = imageFileName;
            this.dim = dim;
        }

        public PageInfo ()
        {
            this.imageFileName = null;
            this.dim = null;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return imageFileName + " [width=" + dim.width + ",height=" + dim.height + "]";
        }
    }
}
