//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 D e e p S c o r e s R e a d e r                                //
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

import org.audiveris.omrdataset.DSMain;
import org.audiveris.omrdataset.DSMain.ImageInfo;
import org.audiveris.omrdataset.api.GeneralShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.awt.Rectangle;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class <code>DeepScoresReader</code> can handle a perhaps very large DeepScores V2 data-set
 * descriptor, because it uses a reader rather than keeping the whole tree in memory.
 *
 * @author Hervé Bitteur
 */
public class DeepScoresReader
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DeepScoresReader.class);

    /** To sort rectangles on their abscissa value. */
    private static final Comparator<Rectangle> byAbscissa = (Rectangle r1, Rectangle r2) -> Integer
            .compare(r1.x, r2.x);

    public static final int NONES_PER_IMAGE = 50;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Selected archive ID. */
    private final int iArch;

    /** The underlying JSON file. */
    private final File jsonFile;

    private final JsonFactory factory = new MappingJsonFactory();

    /** Array of images. */
    protected JsonNode images;

    private final Random random = new Random();

    //~ Constructors -------------------------------------------------------------------------------
    public DeepScoresReader (int iArch,
                             File jsonFile)
    {
        this.iArch = iArch;
        this.jsonFile = jsonFile;
        logger.info("DeepScoresReader on {}", jsonFile);
    }

    //~ Methods ------------------------------------------------------------------------------------
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

    //-------------//
    // getImageMap //
    //-------------//
    public Map<Integer, ImageInfo> getImageMap ()
            throws Exception
    {
        final Map<Integer, ImageInfo> map = new HashMap<>();
        final JsonParser jp = factory.createParser(jsonFile);
        jp.nextToken(); // Skip opening JsonToken.START_OBJECT

        while (jp.nextToken() != JsonToken.END_OBJECT) {
            final String fieldName = jp.getCurrentName();
            jp.nextToken(); // move from field name to value start

            switch (fieldName) {
            case "images" -> {
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    // Read image into a tree model
                    final JsonNode img = jp.readValueAsTree();
                    map.put(img.get("id").asInt(),
                            new ImageInfo(img.get("filename").asText(),
                                          img.get("width").asInt(),
                                          img.get("height").asInt()));
                }
            }
            default ->
                jp.skipChildren();
            }
        }

        return map;
    }

    //---------------//
    // genShapeTally //
    //---------------//
    /**
     * Generate a tally for all samples found in the data-set.
     * <p>
     * For each (general) shape, we retrieve the list of imageId+annId.
     * This tally could then be used to drive a more balanced training.
     */
    public void genShapeTally (File targetFile)
            throws Exception
    {
        logger.info("=== Shape tally for archive {} ===", iArch);
        final Map<GeneralShape, Map<String, MiniAnn>> tally = new EnumMap(GeneralShape.class);
        final JsonParser jp = factory.createParser(jsonFile);
        jp.nextToken(); // Skip opening JsonToken.START_OBJECT

        while (jp.nextToken() != JsonToken.END_OBJECT) {
            final String fieldName = jp.getCurrentName();
            jp.nextToken(); // move from field name to value start

            switch (fieldName) {
            case "annotations" -> {
                System.out.println("Processing: " + fieldName);
                while (jp.nextToken() != JsonToken.END_OBJECT) {
                    final String annId = jp.getCurrentName();
                    jp.nextToken(); // move from field name to value start

                    // Read annotation into a tree model
                    final JsonNode ann = jp.readValueAsTree();
                    final GeneralShape gShape = DSMain.getGeneralShape(ann);

                    if (gShape != null) {
                        Map<String, MiniAnn> map = tally.get(gShape);
                        if (map == null) {
                            tally.put(gShape, map = new LinkedHashMap<>());
                        }

                        map.put(annId, new MiniAnn(ann.get("a_bbox"),
                                                   ann.get("cat_id"),
                                                   ann.get("img_id")));
                    }
                }
            }
            default ->
                jp.skipChildren();
            }
        }

        logger.info("Tally computed for archive {}.", iArch);
        Files.createDirectories(DSMain.tallyDir);
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        mapper.writeValue(targetFile, tally);
    }

    //---------------//
    // genCategories //
    //---------------//
    /**
     * Print the list of categories defined in the selected set.
     *
     * @param setName    either "deepscores" or "muscima++"
     * @param targetFile the output json file
     * @throws java.lang.Exception
     */
    public void genCategories (String setName,
                               File targetFile)
            throws Exception
    {
        logger.info("\n=== {} categories ===", setName);

        JsonNode categoriesTree = null;
        final JsonParser jp = factory.createParser(jsonFile);

        jp.nextToken(); // Skip opening JsonToken.START_OBJECT

        while (jp.nextToken() != JsonToken.END_OBJECT) {
            final String fieldName = jp.getCurrentName();
            jp.nextToken(); // move from field name to value start

            if (fieldName.equals("categories")) {
                System.out.println("Processing: " + fieldName);
                categoriesTree = jp.readValueAsTree();
            } else {
                jp.skipChildren();
            }
        }

        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.writeValue(targetFile, categoriesTree);
    }

    //-----------------//
    // printCategories //
    //-----------------//
    /**
     * Print the list of categories defined in the selected set.
     *
     * @param setName either "deepscores" or "muscima++"
     * @throws java.lang.Exception
     */
    public void printCategories (String setName)
            throws Exception
    {
        logger.info("\n=== {} categories ===", setName);

        final JsonParser jp = factory.createParser(jsonFile);
        int count = 0;

        jp.nextToken(); // Skip opening JsonToken.START_OBJECT

        while (jp.nextToken() != JsonToken.END_OBJECT) {
            final String fieldName = jp.getCurrentName();
            jp.nextToken(); // move from field name to value start

            switch (fieldName) {
            case "categories" -> {
                /// System.out.println("Processing: " + fieldName);
                while (jp.nextToken() != JsonToken.END_OBJECT) {
                    final String catId = jp.getCurrentName();
                    jp.nextToken(); // move from field name to value start

                    // Read category into a tree model,
                    // this moves the parsing position to the end of it
                    final JsonNode cat = jp.readValueAsTree();
                    final JsonNode setNode = cat.get("annotation_set");
                    final String set = setNode.textValue();

                    // Keep only the desired set
                    if (setName.equals(set)) {
                        count++;
                        System.out.println(String.format("key: %3s color: %3s name: %s",
                                                         catId, cat.get("color"), cat.get("name")));
                    }
                }
            }
            default ->
                jp.skipChildren();
            }
        }

        logger.info("{} categories in {}", count, setName);
    }

    //---------------------//
    // printShapeHistogram //
    //---------------------//
    /**
     * Print the count of instances per category.
     */
    public void printShapeHistogram ()
            throws Exception
    {
        logger.info("\n=== Shape Histogram for Archive: {} ===", iArch);

        int total = 0;
        final int[] counts = new int[DSMain.context.getNumClasses()];
        final JsonParser jp = factory.createParser(jsonFile);

        jp.nextToken(); // Skip opening JsonToken.START_OBJECT

        while (jp.nextToken() != JsonToken.END_OBJECT) {
            final String fieldName = jp.getCurrentName();
            jp.nextToken(); // move from field name to value start

            switch (fieldName) {
            case "annotations" -> {
                System.out.println("Processing: " + fieldName);
                while (jp.nextToken() != JsonToken.END_OBJECT) {
                    jp.nextToken(); // move from field name to value start

                    // Read annotation into a tree model
                    final JsonNode ann = jp.readValueAsTree();
                    final GeneralShape gShape = DSMain.getGeneralShape(ann);

                    if (gShape != null) {
                        counts[gShape.ordinal()]++;
                        total++;
                    }
                }
            }
            default ->
                jp.skipChildren();
            }
        }

        final GeneralShape[] values = GeneralShape.values();
        Files.createDirectories(DSMain.tallyDir);
        final ObjectMapper mapper = new ObjectMapper();
        final File file = DSMain.tallyDir.resolve("Counts-" + iArch + ".json").toFile();

        System.out.println(String.format("Total: %10d", total));
        final ArchiveCounts archCounts = new ArchiveCounts();
        archCounts.total = total;

        for (int i = 0; i < counts.length; i++) {
            final GeneralShape gShape = values[i];
            System.out.println(String.format("%3d : %7d %s", i, counts[i], gShape));
            archCounts.count_map.put(gShape, counts[i]);
        }

        mapper.writeValue(file, archCounts);
    }

    //----------//
    // genNones //
    //----------//
    public Map<Integer, ImageNones> genNones ()
            throws Exception
    {
        final Map<Integer, ImageNones> nones = new TreeMap<>();
        final JsonParser jp = factory.createParser(jsonFile);

        jp.nextToken(); // Skip opening JsonToken.START_OBJECT

        while (jp.nextToken() != JsonToken.END_OBJECT) {
            final String fieldName = jp.getCurrentName();
            jp.nextToken(); // move from field name to value start

            switch (fieldName) {
            case "images" -> {
                System.out.println("Reading: " + fieldName);
                images = jp.readValueAsTree();
            }
            case "annotations" -> {
                System.out.println("Processing: " + fieldName);
                final List<Rectangle> boxes = new ArrayList<>();
                int curImgId = -1;

                while (jp.nextToken() != JsonToken.END_OBJECT) {
                    jp.nextToken(); // move from field name to value start

                    // Read annotation into a tree model
                    final JsonNode ann = jp.readValueAsTree();
                    final int imgId = ann.get("img_id").asInt();

                    if (curImgId != -1) {
                        if (curImgId != imgId) {
                            // Process ended image
                            nones.put(curImgId, genImageNones(curImgId, boxes));

                            // Collect occupied boxes in starting image
                            curImgId = imgId;
                            boxes.clear();
                        }
                    } else {
                        curImgId = imgId;
                    }

                    if (DSMain.getGeneralShape(ann) != null) {
                        boxes.add(getBox(ann));
                    }
                }

                // Pending image?
                if (curImgId != -1) {
                    nones.put(curImgId, genImageNones(curImgId, boxes));
                }
            }
            default ->
                jp.skipChildren();
            }
        }

        return nones;
//        final GeneralShape[] values = GeneralShape.values();
//        final Path tallyDir = DSMain.OUTPUT_FOLDER.resolve("tally");
//        Files.createDirectories(tallyDir);
//        final ObjectMapper mapper = new ObjectMapper();
//        final File file = tallyDir.resolve("Counts-" + iArch + ".json").toFile();
//
//        System.out.println(String.format("Total: %10d", total));
//        final ArchiveCounts archCounts = new ArchiveCounts();
//        archCounts.total = total;
//
//        for (int i = 0; i < counts.length; i++) {
//            final GeneralShape gShape = values[i];
//            System.out.println(String.format("%3d : %7d %s", i, counts[i], gShape));
//            archCounts.count_map.put(gShape, counts[i]);
//        }
//
//        mapper.writeValue(file, archCounts);
//
        //        final int[] counts = new int[137];
        //        final JsonParser jp = factory.createParser(jsonFile);
        //
        //        jp.nextToken(); // Skip opening JsonToken.START_OBJECT
        //
        //        while (jp.nextToken() != JsonToken.END_OBJECT) {
        //            final String fieldName = jp.getCurrentName();
        //            jp.nextToken(); // move from field name to field value
        //
        //            if (fieldName.equals("annotations")) {
        //                System.out.println("Processing: " + fieldName);
        //                while (jp.nextToken() != JsonToken.END_OBJECT) {
        //                    final String annId = jp.getCurrentName();
        //                    jp.nextToken(); // move from field name to value start
        //                    ///System.out.println("annId: " + annId);
        //
        //                    // Read annotation into a tree model,
        //                    // this moves the parsing position to the end of it
        //                    final JsonNode ann = jp.readValueAsTree();
        //                    // And now we have random access to everything in the object
        //                    final JsonNode catidNode = ann.get("cat_id").get(0);
        //                    if (catidNode != null) {
        //                        final int catId = catidNode.asInt();
        //                        counts[catId]++;
        //                    }
        //                }
        //            } else {
        //                System.out.println("Skipping: " + fieldName);
        //                jp.skipChildren();
        //            }
        //        }
        //
        //        for (int i = 0; i < counts.length; i++) {
        //            System.out.println(String.format("%3d : %7d", i, counts[i]));
        //        }
    }

    //---------------//
    // genImageNones //
    //---------------//
    private ImageNones genImageNones (int imgId,
                                      List<Rectangle> boxes)
    {
        // Retrieve image dimensions
        final JsonNode image = getImage(imgId);
        final int sheetWidth = image.get("width").asInt();
        final int sheetHeight = image.get("height").asInt();

        // Allocate random none locations outside existing shape boxes
        Collections.sort(boxes, byAbscissa);
        final int toAdd = NONES_PER_IMAGE;
        final List<NoneLoc> added = new ArrayList<>();

        while (added.size() < toAdd) {
            final int x = random.nextInt(sheetWidth); // Random X
            final int y = random.nextInt(sheetHeight); // Random Y
            final Rectangle rect = new Rectangle(x, y, 1, 1);

            if (checkLocation(rect, boxes)) {
                final NoneLoc loc = new NoneLoc();
                loc.a_bbox.add(x);
                loc.a_bbox.add(y);
                loc.a_bbox.add(x);
                loc.a_bbox.add(y);
                added.add(loc);
            }
        }

        return new ImageNones(image.get("filename").asText(), added);
    }

    //---------------//
    // checkLocation //
    //---------------//
    /**
     * Check whether the provided rectangle (a none candidate) does not intersect any legal
     * shape box.
     *
     * @param rect  the none rectangle candidate
     * @param boxes the list of legal shape boxes, sorted by increasing abscissa
     * @return true if OK
     */
    private boolean checkLocation (Rectangle rect,
                                   List<Rectangle> boxes)
    {
        final int size = boxes.size();
        final int xMax = (rect.x + rect.width) - 1;

        // Theoretical insertion index in the sorted list
        final int result = Collections.binarySearch(boxes, rect, byAbscissa);
        final int index = (result >= 0) ? result : (-(result + 1));

        // Check for collision on right
        for (int i = index; i < size; i++) {
            Rectangle r = boxes.get(i);

            if (r.x > xMax) {
                break;
            } else if (r.intersects(rect)) {
                return false;
            }
        }

        // Check for collision on left
        for (int i = index - 1; i >= 0; i--) {
            Rectangle r = boxes.get(i);

            if (r.intersects(rect)) {
                return false;
            }
        }

        return true;
    }

    //----------//
    // getImage //
    //----------//
    private JsonNode getImage (int key)
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
    //---------//
    // MiniAnn //
    //---------//
    public static class MiniAnn
    {

        public final JsonNode a_bbox;

        public final JsonNode cat_id;

        public final JsonNode img_id;

        public MiniAnn (
                JsonNode a_bbox,
                JsonNode cat_id,
                JsonNode img_id)
        {
            this.a_bbox = a_bbox;
            this.cat_id = cat_id;
            this.img_id = img_id;
        }
    }

    public static class ArchiveCounts
    {

        public int total;

        public Map<GeneralShape, Integer> count_map = new EnumMap<>(GeneralShape.class);

    }

    //-----------//
    // ImageName //
    //-----------//
    public static class ImageName
    {

        public final String id;

        public final String filename;

        public final int width;

        public final int height;

        public ImageName (String id,
                          String filename,
                          int width,
                          int height)
        {
            this.id = id;
            this.filename = filename;
            this.width = width;
            this.height = height;
        }
    }

    //------------//
    // ImageNones //
    //------------//
    public static class ImageNones
    {

        public final String filename;

        public final List<NoneLoc> nones;

        public ImageNones (String filename,
                           List<NoneLoc> nones)
        {
            this.filename = filename;
            this.nones = nones;
        }
    }

    //---------//
    // NoneLoc //
    //---------//
    /**
     * A pseudo annotation, limited to a degenerated rectangle and always the none category.
     * <p>
     * Example:
     * {
     * "a_bbox": [1771.0, 1988.0, 1771.0, 1988.0],
     * "cat_id": ["0", "0"]
     * }
     */
    public static class NoneLoc
    {

        // Degenerated rectangle, since width and height are zero
        public List<Integer> a_bbox = new ArrayList<>();

        // Always the same "none" value
        public List<String> cat_id = Arrays.asList("0", "0");
    }
}
