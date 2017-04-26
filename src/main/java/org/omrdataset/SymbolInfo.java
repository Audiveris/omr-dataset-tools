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

import java.awt.geom.Rectangle2D;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code SymbolInfo} handles info about one symbol (name, bounding box).
 *
 * @author Hervé Bitteur
 */
public class SymbolInfo
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlAttribute(name = "shape")
    public final OmrShape omrShape;

    @XmlElement(name = "BoundingBox")
    @XmlJavaTypeAdapter(Jaxb.Rectangle2DAdapter.class)
    public final Rectangle2D bounds;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SymbolInfo} object.
     *
     * @param omrShape symbol OMR shape
     * @param bounds   symbol bounding box within containing image
     */
    public SymbolInfo (OmrShape omrShape,
                       Rectangle2D bounds)
    {
        this.omrShape = omrShape;
        this.bounds = bounds;
    }

    /**
     * Creates a new {@code SymbolInfo} object.
     */
    private SymbolInfo ()
    {
        this.omrShape = null;
        this.bounds = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Symbol{");
        sb.append(omrShape);
        sb.append(" ").append(bounds);
        sb.append("}");

        return sb.toString();
    }
}
