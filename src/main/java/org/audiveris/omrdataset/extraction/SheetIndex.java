//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S h e e t I n d e x                                      //
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
package org.audiveris.omrdataset.extraction;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLStreamException;
import org.audiveris.omr.util.Jaxb;

/**
 * Class {@code SheetIndex} keeps an index of sheets processed, to allow any sheet to
 * be referenced via its ID in a feature line.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "SheetIndex")
public class SheetIndex
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SheetIndex.class);

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------
    @XmlElementWrapper(name = "map")
    private final TreeMap<Integer, String> int2Str = new TreeMap<>();

    private final Map<String, Integer> str2Int = new HashMap<>();

    private int lastId;

    //~ Constructors -------------------------------------------------------------------------------
    public SheetIndex ()
    {

    }

    //~ Methods ------------------------------------------------------------------------------------
//    public static void main (String[] args)
//            throws Exception
//    {
//        SheetIndex si = new SheetIndex();
//
//        for (int i = 1; i <= 5; i++) {
//            si.getId(Paths.get("path_" + i));
//        }
//
//        si.marshal(Paths.get("MySheetIndex.xml"));
//
//        SheetIndex newSi = SheetIndex.unmarshal(Paths.get("MySheetIndex.xml"));
//        logger.info("newSi.int2Str: {}", newSi.int2Str);
//        logger.info("newSi.str2Int: {}", newSi.str2Int);
//        logger.info("newSi.lastId: {}", newSi.lastId);
//
//    }
//
    public synchronized Integer getId (Path path)
    {
        final String str = path.toAbsolutePath().toString();
        Integer id = str2Int.get(str);

        if (id == null) {
            id = ++lastId;
            str2Int.put(str, id);
            int2Str.put(id, str);
        }

        return id;
    }

    public synchronized Path getPath (int id)
    {
        String str = int2Str.get(id);
        if (str == null) {
            return null;
        }

        return Paths.get(str);
    }

    public void marshal (Path outPath)
            throws JAXBException,
                   IOException,
                   XMLStreamException
    {
        Jaxb.marshal(this, outPath, getJaxbContext());
    }

    public static SheetIndex unmarshal (Path inPath)
            throws IOException,
                   JAXBException
    {
        return (SheetIndex) Jaxb.unmarshal(inPath, getJaxbContext());
    }

    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller m,
                                 Object parent)
    {
        // Populate reverse map str2Int and lastId
        for (Entry<Integer, String> entry : int2Str.entrySet()) {
            str2Int.put(entry.getValue(), lastId = entry.getKey());
        }
    }

    private static JAXBContext getJaxbContext ()
            throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(SheetIndex.class);
        }

        return jaxbContext;
    }
}
