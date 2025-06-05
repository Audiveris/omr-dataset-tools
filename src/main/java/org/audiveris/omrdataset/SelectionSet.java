//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S e l e c t i o n S e t                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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
package org.audiveris.omrdataset;

import org.audiveris.omrdataset.AbstractSampleSet.AbstractIterator;
import static org.audiveris.omrdataset.AbstractSampleSet.rgbToGray;
import org.audiveris.omrdataset.api.DeepScoresShape;
import org.audiveris.omrdataset.api.GeneralShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import javax.imageio.ImageIO;

/**
 * Class <code>SelectionSet</code> handles images with annotations selected to keep balance
 * between all symbol shapes, including the 'none' shape.
 * <p>
 * Input for archive N:
 * <ul>
 * <li>File "img2anns-N.json" produced by '-select' CLI option
 * <li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class SelectionSet
        extends AbstractSampleSet
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SelectionSet.class);

    private static final JsonFactory factory = new MappingJsonFactory();

    //~ Instance fields ----------------------------------------------------------------------------
    private final File selectionFile;
    //~ Constructors -------------------------------------------------------------------------------

    public SelectionSet (JsonNode categories,
                         File selectionFile)
    {
        this.categories = categories;
        this.selectionFile = selectionFile;
    }
    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // iterator //
    //----------//
    @Override
    public Iterator<Pair<INDArray, INDArray>> iterator ()
    {
        try {
            final JsonParser jp = factory.createParser(selectionFile);
            final JsonNode imgMap = jp.readValueAsTree();
            totalImageCount = imgMap.size();
            logger.info("Selection of {} images in {}", totalImageCount, selectionFile);

            return new CustomIterator(imgMap.fields());
        } catch (Exception ex) {
            logger.warn("Exception " + ex, ex);
            return null;
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------------//
    // CustomIterator //
    //----------------//
    public class CustomIterator
            extends AbstractIterator
    {

        public CustomIterator (Iterator<Entry<String, JsonNode>> imgIdIterator)
        {
            super(new ImageIterator(imgIdIterator));

            curPair = moveAhead();
        }
    }

    //---------------//
    // ImageIterator //
    //---------------//
    private static class ImageIterator
            implements Iterator<Image>
    {

        private final Iterator<Entry<String, JsonNode>> imgIdIterator;

        public ImageIterator (Iterator<Entry<String, JsonNode>> imgIdIterator)
        {
            this.imgIdIterator = imgIdIterator;
        }

        @Override
        public boolean hasNext ()
        {
            return imgIdIterator.hasNext();
        }

        @Override
        public Image next ()
        {
            final Entry<String, JsonNode> entry = imgIdIterator.next();
            return new MyImage(entry);
        }
    }

    //---------//
    // MyImage //
    //---------//
    private static class MyImage
            implements AbstractSampleSet.Image
    {

        private final Entry<String, JsonNode> entry;

        private final JsonNode selection;

        private final JsonNode selected_anns;

        public MyImage (Entry<String, JsonNode> entry)
        {
            this.entry = entry;
            selection = entry.getValue();
            selected_anns = selection.get("selected_anns");
        }

        @Override
        public int getAnnotationsCount ()
        {
            return selected_anns.size();
        }

        @Override
        public Iterator<JsonNode> getAnnotationsIterator ()
        {
            return selected_anns.elements();
        }

        @Override
        public String getFileName ()
        {
            return selection.get("filename").asText();
        }

        @Override
        public String getId ()
        {
            return entry.getKey();
        }
    }
}
