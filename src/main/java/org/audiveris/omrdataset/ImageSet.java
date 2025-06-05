//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         I m a g e S e t                                        //
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

import org.audiveris.omrdataset.api.DeepScoresShape;
import org.audiveris.omrdataset.api.GeneralShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Class <code>ImageSet</code> handles images with all their annotations, plus added 'none'
 * annotations, with no balance kept between shapes.
 * <p>
 * Inputs for archive N:
 * <ul>
 * <li>File "deepscores-complete-N_train.json"
 * <li>File "none2locs-N_train.json" for 'none' samples produced by '-nones' CLI option
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class ImageSet
        extends AbstractSampleSet
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ImageSet.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Array of images. */
    private JsonNode images;

    /** Selected archive ID. */
    private final int iArch;  // Not used?

    /** The underlying JSON file. */
    private final File jsonFile;

    private final JsonNode noneMap;

    private final JsonFactory factory = new MappingJsonFactory();

    private final List<Integer> desiredIndices;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an <code>ImageSet</code> object.
     *
     * @param iArch          the archive ID
     * @param jsonFile       the archive description file
     * @param noneFile       the archive none2locs file
     * @param desiredIndices the desired image indices
     */
    public ImageSet (int iArch,
                     File jsonFile,
                     File noneFile,
                     List<Integer> desiredIndices)
    {
        this.iArch = iArch;
        this.jsonFile = jsonFile;
        this.desiredIndices = desiredIndices;

        try {
            logger.info("noneFile: {}", noneFile);
            final JsonParser noneJp = factory.createParser(noneFile);
            noneMap = noneJp.readValueAsTree();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // iterator //
    //----------//
    @Override
    public Iterator<Pair<INDArray, INDArray>> iterator ()
    {
        return new CustomIterator();
    }

    //--------//
    // select //
    //--------//
    /**
     * Filter the user-desired image indices against the available population
     *
     * @param desiredIndices the list of image indices as desired by the end-user
     * @param size           the size of image population
     * @return the list of indices really selected
     */
    private List<Integer> select (List<Integer> desiredIndices,
                                  int size)
    {
        final List<Integer> kept = new ArrayList<>();
        for (int i : desiredIndices) {
            if (i >= 0 && i < size) {
                kept.add(i);
            }
        }

        logger.info("Selected indices: {} / {}", kept.size(), size);

        return kept;
    }

    //----------//
    // getImage //
    //----------//
    public JsonNode getImage (int key)
    {
        for (Iterator<JsonNode> it = images.elements(); it.hasNext();) {
            final JsonNode image = it.next();
            final int id = image.get("id").asInt();

            if (id == key) {
                return image;
            }
            if (id > key) {
                break;
            }

        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------------//
    // CustomIterator //
    //----------------//
    public class CustomIterator
            extends AbstractIterator
    {

        public CustomIterator ()

        {
            super(new ImageIterator());
            curPair = moveAhead();
        }
    }

    //---------------//
    // ImageIterator //
    //---------------//
    private class ImageIterator
            implements Iterator<Image>
    {

        private final JsonParser jp;

        /**
         * First annotation of a new image, when detecting this new image
         * and thus ending the previous one.
         */
        private JsonNode firstAnn = null;

        private Image curImage = null;

        public ImageIterator ()
        {
            try {
                jp = factory.createParser(jsonFile);
                curImage = init();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        private Image init ()
                throws Exception
        {
            jp.nextToken(); // Skip opening JsonToken.START_OBJECT

            while (jp.nextToken() != JsonToken.END_OBJECT) {
                final String fieldName = jp.getCurrentName();
                jp.nextToken(); // move from field name to value start

                switch (fieldName) {
                case "categories" -> {
                    logger.info("Reading: " + fieldName);
                    categories = jp.readValueAsTree();
                }
                case "images" -> {
                    logger.info("Reading: " + fieldName);
                    images = jp.readValueAsTree();
                    imageIndices = select(desiredIndices, images.size());
                    totalImageCount = imageIndices.size();
                }
                case "annotations" -> {
                    logger.info("Processing: " + fieldName);
                    return readImage(); // Normal method exit
                }
                default -> {
                    logger.info("Skipping: " + fieldName);
                    jp.skipChildren();
                }
                } // end switch
            } // end while

            return null; // Should not happen
        }

        private Image readImage ()
                throws Exception
        {
            final List<JsonNode> anns = new ArrayList<>();
            int curImgId;

            if (firstAnn != null) {
                curImgId = firstAnn.get("img_id").asInt();

                if (DSMain.getGeneralShape(firstAnn) != null) {
                    anns.add(firstAnn);
                }
            } else {
                curImgId = -1;
            }

            while (jp.nextToken() != JsonToken.END_OBJECT) {
                jp.nextToken(); // move from field name to value start

                // Read annotation into a tree model
                final JsonNode ann = jp.readValueAsTree();
                final int imgId = ann.get("img_id").asInt();
                logger.debug("ann:{} imgId: {}", ann, imgId);

                if (curImgId != -1) {
                    if (curImgId != imgId) {
                        // Detecting start of a different image
                        firstAnn = ann; // Annotation is put aside

                        // Process ended image
                        return new MyImage(curImgId, anns);
                    }
                } else {
                    curImgId = imgId;
                }

                if (DSMain.getGeneralShape(ann) != null) {
                    anns.add(ann);
                }
            }

            // This is the end of descriptor "annotations" object
            firstAnn = null;

            // Pending image?
            if (curImgId != -1) {
                return new MyImage(curImgId, anns);
            }

            return null;
        }

        @Override
        public boolean hasNext ()
        {
            return curImage != null;
        }

        @Override
        public Image next ()
        {
            final Image image = curImage;

            try {
                curImage = readImage();
            } catch (Exception ex) {
                logger.warn("Error in readImage", ex);
                curImage = null;
            }

            return image;
        }
    }

    //---------//
    // MyImage //
    //---------//
    private class MyImage
            implements AbstractSampleSet.Image
    {

        private final String imgId;

        private final String fileName;

        private final List<JsonNode> allAnns;

        public MyImage (int imgId,
                        List<JsonNode> selected_anns)
        {
            logger.debug("MyImage imgId:{} selected_anns size:{}", imgId, selected_anns.size());
            this.imgId = "" + imgId;
            allAnns = selected_anns;

            final JsonNode img = getImage(imgId);
            fileName = img.get("filename").asText();

            // Add 'none' samples
            final JsonNode imgNones = noneMap.get(this.imgId);
            int nones = 0;

            if (imgNones != null) {
                final JsonNode locs = imgNones.get("nones"); // An array of ~50 rectangles
                nones = locs.size();
                for (Iterator<JsonNode> it = locs.elements(); it.hasNext();) {
                    allAnns.add(it.next());
                }
            } else {
                logger.warn("No image {} in noneMap", this.imgId);
            }

            Collections.shuffle(allAnns);

            logger.debug("Image {} allAnns:{} including {} nones", imgId, allAnns.size(), nones);
        }

        @Override
        public int getAnnotationsCount ()
        {
            return allAnns.size();
        }

        @Override
        public Iterator<JsonNode> getAnnotationsIterator ()
        {
            return allAnns.iterator();
        }

        @Override
        public String getFileName ()
        {
            return fileName;
        }

        @Override
        public String getId ()
        {
            return imgId;
        }
    }
}
