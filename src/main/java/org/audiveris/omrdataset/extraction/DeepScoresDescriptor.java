//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             D e e p S c o r e s D e s c r i p t o r                            //
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
package org.audiveris.omrdataset.extraction;

import org.audiveris.omr.util.StopWatch;
import org.audiveris.omrdataset.DSMain;
import org.audiveris.omrdataset.api.DeepScoresShape;
import org.audiveris.omrdataset.api.GeneralShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class <code>DeepScoresDescriptor</code> provides a convenient interface to a DeepScores V2
 * dataset Json descriptor.
 *
 * @author Hervé Bitteur
 */
public class DeepScoresDescriptor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DeepScoresDescriptor.class);

    //~ Enumerations -------------------------------------------------------------------------------
    //~ Instance fields ----------------------------------------------------------------------------
    /** The underlying Json file name. */
    protected final String jsonFileName;

    /** Whole: info, annotation_sets, categories, images, annotations. */
    protected final JsonNode wholeTree;

    /** Map cat_id -> category. */
    protected final JsonNode categories;

    /** Array of images. */
    protected final JsonNode images;

    /** Map ann_id -> annotation. */
    protected final JsonNode annotations;

    //~ Constructors -------------------------------------------------------------------------------
    public DeepScoresDescriptor (String jsonFileName,
                                 JsonNode wholeTree)
    {
        this.jsonFileName = jsonFileName;
        this.wholeTree = wholeTree;

        categories = wholeTree.get("categories");
        images = wholeTree.get("images");
        annotations = wholeTree.get("annotations");

    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // getAnnotation //
    //---------------//
    public JsonNode getAnnotation (String ann_id)
    {
        return annotations.get(ann_id);
    }

    //--------//
    // getBox //
    //--------//
    public Rectangle getBox (JsonNode annotation)
    {
        final JsonNode aBox = annotation.get("a_bbox");
        final int x1 = aBox.get(0).asInt();
        final int y1 = aBox.get(1).asInt();
        final int x2 = aBox.get(2).asInt();
        final int y2 = aBox.get(3).asInt();
        return new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
    }

    //----------//
    // getImage //
    //----------//
    public JsonNode getImage (String imgKey)
    {
        return getImage(Integer.decode(imgKey));
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

    //----------------//
    // getImagesCount //
    //----------------//
    public int getImagesCount ()
    {
        return images.size();
    }

    //-------------------//
    // getImagesIterator //
    //-------------------//
    public Iterator<JsonNode> getImagesIterator ()
    {
        return images.elements();
    }

    //----------//
    // getShape //
    //----------//
    public String getShape (int catId)
    {
        return DSMain.getShape("" + catId);
    }

    //-----------------//
    // printCategories //
    //-----------------//
    /**
     * Print the list of categories defined in the selected set.
     *
     * @param setName either "deepscores" or "muscima++"
     */
    public void printCategories (String setName)
    {
        logger.info("\n=== {} categories ===", setName);

        int count = 0;

        for (Iterator<Map.Entry<String, JsonNode>> it = categories.fields(); it.hasNext();) {
            final Map.Entry<String, JsonNode> entry = it.next();
            final JsonNode value = entry.getValue();
            final JsonNode setNode = value.get("annotation_set");
            final String set = setNode.textValue();

            // Keep only the desired set
            if (setName.equals(set)) {
                count++;
                System.out.println(
                        String.format("key: %3s color: %3s name: %s",
                                      entry.getKey(), value.get("color"), value.get("name")));
            }
        }

        logger.info("{} categories in {}", count, setName);
    }

    //-------------------//
    // printImageSamples //
    //-------------------//
    public void printImageSamples (int imgId)
    {
        final JsonNode image = getImage(imgId);
        if (image == null) {
            logger.info("Could not find image for id:{} in {}", imgId, jsonFileName);
            return;
        }

        System.out.println(String.format("=== %d: %s ===", imgId, image.get("filename")));

        final JsonNode ann_ids = image.get("ann_ids");
        for (JsonNode ann_id : ann_ids) {
            final JsonNode annotation = annotations.get(ann_id.textValue());
            final int cat = annotation.get("cat_id").get(0).asInt();
            System.out.println(String.format("%s", annotation.toPrettyString()));
            System.out.println(String.format("shape:%s box:%s",
                                             DSMain.getShape("" + cat), getBox(annotation)));
        }
    }

    //-----------//
    // printInfo //
    //-----------//
    public void printInfo ()
    {
        System.out.println();
        System.out.println(String.format("=== %s ===", this));
        System.out.println(String.format("images count: %d", images.size()));

        final StringBuilder sb = new StringBuilder();
        int count = 0;
        for (JsonNode image : images) {
            if (count++ % 20 == 0) {
                sb.append('\n');
            }
            sb.append(String.format("%5s", image.get("id")));
        }
        System.out.println(sb);
    }

    //--------------------------//
    // printSampleCountPerImage //
    //--------------------------//
    public void printSampleCountPerImage ()
    {
        int imgCount = 0;

        // Loop on images
        for (Iterator<JsonNode> it = images.elements(); it.hasNext();) {
            imgCount++;
            final JsonNode image = it.next();
            final JsonNode ann_ids = image.get("ann_ids");
            final int[] counts = new int[137];

            for (JsonNode ann_id : ann_ids) {
                final JsonNode annotation = annotations.get(ann_id.textValue());
                final int cat = annotation.get("cat_id").get(0).asInt();
                counts[cat - 1]++;
            }

            System.out.println(String.format("Image id:%5s size:%5d filename:%s",
                                             image.get("id"), ann_ids.size(), image.get("filename")));
            ///logger.info("   Counts:{}", filled(counts));
        }

        logger.info("{} images", imgCount);
    }

    //---------------------//
    // printShapeHistogram //
    //---------------------//
    public void printShapeHistogram ()
    {
        final EnumMap<DeepScoresShape, Integer> dsPercents = new EnumMap<>(DeepScoresShape.class);
        for (DeepScoresShape s : DeepScoresShape.values()) {
            dsPercents.put(s, 0);
        }

        final EnumMap<GeneralShape, Integer> gPercents = new EnumMap<>(GeneralShape.class);
        for (GeneralShape s : GeneralShape.values()) {
            gPercents.put(s, 0);
        }

        int totalDeepScore = 0;
        int totalGeneral = 0;

        for (Iterator<Map.Entry<String, JsonNode>> it = annotations.fields(); it.hasNext();) {
            final Map.Entry<String, JsonNode> entry = it.next();
            final JsonNode annotation = entry.getValue();
            final JsonNode catidNode = annotation.get("cat_id").get(0);

            if (catidNode != null) {
                final String shapeString = DSMain.getShape(catidNode.asText());

                final DeepScoresShape dsShape = DeepScoresShape.valueOf(shapeString);
                totalDeepScore++;
                dsPercents.put(dsShape, dsPercents.get(dsShape) + 1);

                final GeneralShape gShape = dsShape.toGeneralShape();
                if (gShape != null) {
                    totalGeneral++;
                    gPercents.put(gShape, gPercents.get(gShape) + 1);
                }
            }
        }

        System.out.println();
        System.out.println(String.format("=== %s DeepScoresShape samples: %d ===",
                                         jsonFileName, totalDeepScore));
        for (Entry<DeepScoresShape, Integer> entry : dsPercents.entrySet()) {
            final DeepScoresShape dsShape = entry.getKey();
            final int count = entry.getValue();
            final float freq = (float) count / totalDeepScore;
            System.out.println(String.format("ord:%3d count:%6d %9.7f%s %s",
                                             dsShape.ordinal(), 100 * count, freq, "%", dsShape));
        }

        final EnumMap<GeneralShape, Float> gFrequencies = new EnumMap<>(GeneralShape.class);
        System.out.println();
        System.out.println(String.format("=== %s GeneralShape samples: %d ===",
                                         jsonFileName, totalGeneral));
        for (Entry<GeneralShape, Integer> entry : gPercents.entrySet()) {
            final GeneralShape gShape = entry.getKey();
            final int count = entry.getValue();
            final float freq = (float) count / totalGeneral;
            System.out.println(String.format("ord:%3d count:%6d %9.7f%s %s",
                                             gShape.ordinal(), 100 * count, freq, "%", gShape));
            gFrequencies.put(gShape, freq);
        }

        // Marshal general frequencies
        try {
            final File file = DSMain.FREQUENCIES_PATH.toFile();
            final ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(file, gFrequencies);
            mapper.writeValueAsString(gFrequencies);

            // Check reading
            final JsonNode tree = mapper.readTree(file);
            System.out.println("Content of frequencies file:");
            System.out.println(tree.toPrettyString());
        } catch (IOException ex) {
            logger.warn("Error with frequencies file", ex);
        }
    }

    //------//
    // load //
    //------//
    /**
     * Load the descriptor from file.
     *
     * @param jsonFilePath path to the .json file to load
     * @return the loaded descriptor or null if load failed
     */
    public static DeepScoresDescriptor load (Path jsonFilePath)
    {
        final StopWatch watch = new StopWatch("Loading " + jsonFilePath);
        try {
            watch.start("load");
            final ObjectMapper mapper = new ObjectMapper();
            logger.debug("jsonFilePath={}", jsonFilePath);
            final JsonNode wholeTree = mapper.readTree(jsonFilePath.toFile());

            return new DeepScoresDescriptor(jsonFilePath.getFileName().toString(), wholeTree);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ///watch.print();
        }

        return null;
    }

    //--------//
    // filled //
    //--------//
    private static String filled (int[] counts)
    {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < counts.length; i++) {
            final int count = counts[i];

            if (count > 0) {
                sb.append(' ').append(i + 1).append(':').append(count);
            }
        }

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return getClass().getSimpleName() + " " + jsonFileName;
    }
}
