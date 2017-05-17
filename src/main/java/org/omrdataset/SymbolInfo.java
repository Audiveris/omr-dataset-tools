//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S y m b o l I n f o                                      //
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
package org.omrdataset;

import org.omrdataset.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code SymbolInfo} handles info about one OMR symbol (name, bounding box).
 *
 * @author Hervé Bitteur
 */
public class SymbolInfo
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            SymbolInfo.class);

    //~ Instance fields ----------------------------------------------------------------------------
    @XmlAttribute(name = "interline")
    private final int interline;

    @XmlAttribute(name = "shape")
    @XmlJavaTypeAdapter(OmrShapeAdapter.class)
    private final OmrShape omrShape;

    @XmlElement(name = "Bounds")
    @XmlJavaTypeAdapter(Jaxb.Rectangle2DAdapter.class)
    private final Rectangle2D bounds;

    /** Inner symbols, if any. */
    @XmlElement(name = "Symbol")
    private List<SymbolInfo> innerSymbols;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SymbolInfo} object.
     *
     * @param omrShape  symbol OMR shape
     * @param interline related interline
     * @param bounds    symbol bounding box within containing image
     */
    public SymbolInfo (OmrShape omrShape,
                       int interline,
                       Rectangle2D bounds)
    {
        this.omrShape = omrShape;
        this.interline = interline;
        this.bounds = bounds;
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private SymbolInfo ()
    {
        this.omrShape = null;
        this.interline = 0;
        this.bounds = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Symbol{");
        sb.append(omrShape);

        if ((innerSymbols != null) && !innerSymbols.isEmpty()) {
            sb.append(" OUTER");
        }

        sb.append(" interline:").append(interline);
        sb.append(" ").append(bounds);

        sb.append("}");

        return sb.toString();
    }

    /**
     * Called after all the properties (except IDREF) are unmarshalled
     * for this object, but before this object is set to the parent object.
     */
    @PostConstruct
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        if (omrShape == null) {
            logger.warn("*** Null shape {}", this);
        }
    }

    /**
     * Report the inner symbols, if any
     *
     * @return un-mutable list of inner symbols, perhaps empty but never null
     */
    public List<SymbolInfo> getInnerSymbols ()
    {
        if (innerSymbols == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(innerSymbols);
    }

    /**
     * @return the interline
     */
    public int getInterline ()
    {
        return interline;
    }

    /**
     * @return the omrShape
     */
    public OmrShape getOmrShape ()
    {
        return omrShape;
    }

    /**
     * @return a COPY of the bounds
     */
    public Rectangle2D getBounds ()
    {
        Rectangle2D copy = new Rectangle2D.Double();
        copy.setRect(bounds);

        return copy;
    }

    /**
     * Add an inner symbol within this one.
     *
     * @param symbol the inner symbol to add
     */
    public void addInnerSymbol (SymbolInfo symbol)
    {
        if (innerSymbols == null) {
            innerSymbols = new ArrayList<SymbolInfo>();
        }

        innerSymbols.add(symbol);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------------//
    // OmrShapeAdapter //
    //-----------------//
    /**
     * We need a specific adapter to warn about unknown shape names.
     */
    public static class OmrShapeAdapter
            extends XmlAdapter<String, OmrShape>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public String marshal (OmrShape shape)
                throws Exception
        {
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
