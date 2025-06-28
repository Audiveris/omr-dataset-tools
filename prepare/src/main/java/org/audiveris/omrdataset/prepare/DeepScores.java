//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       D e e p S c o r e s                                      //
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
// </editor-fold>
package org.audiveris.omrdataset.prepare;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.Graphics2D;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class <code>DeepScores</code> includes the DeepScores dataset into the YOLO training dataset.
 * <p>
 * We are interested in its ds2_dense folder, containing:
 * <ul>
 * <li>"images" folder which gathers all images (1714)
 * <li>"deepscores_train.json" file
 * <li>"deepscores_test.json" file
 * </ul>
 * But we discard the separation between "train" and "test" JSON files, so that we can instead drive
 * the processing based on the provided configuration file which explicitly lists which
 * pages should be considered for train, and which pages for val.
 *
 * @author Hervé Bitteur
 */
public class DeepScores
        extends DataSetFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DeepScores.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** List of the 2 annotations JSON nodes. */
    private final List<JsonNode> annotationsList = new ArrayList<>();

    /** Map: image name -> image JSON node. */
    private final Map<String, JsonNode> imgMap = new HashMap<>();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a new <code>DeepScores</code> instance.
     *
     * @param targetConfig path to Yolo .yaml descriptor
     * @param sourceConfig path to DeepScores .yaml descriptor
     * @throws java.lang.Exception
     */
    public DeepScores (String targetConfig,
                       String sourceConfig)
            throws Exception
    {
        super(targetConfig);

        logger.info("DeepScores dataset");

        config = yamlMapper.readValue(Paths.get(sourceConfig).toFile(), DeepScoresConfig.class);
        logger.info("{}", config);

        sourceDir = Paths.get(config.source);
        imagesPath = sourceDir.resolve(config.images);

        final ObjectMapper jsonMapper = new ObjectMapper();
        boolean labelsPrinted = false;

        for (String jsonName : new String[] { "deepscores_train.json", "deepscores_test.json" }) {
            final File jsonFile = sourceDir.resolve(jsonName).toFile();

            // Preload the whole json file
            logger.debug("Loading {}...", jsonName);
            final long start = System.currentTimeMillis();
            final JsonNode wholeTree = jsonMapper.readTree(jsonFile);
            final long stop = System.currentTimeMillis();
            logger.debug("File {} loaded in {} ms ", jsonName, stop - start);

            // Remember annotations
            final JsonNode annotations = wholeTree.get("annotations");
            annotationsList.add(annotations);

            // Remember all images, to provide a direct access via the filename
            final JsonNode images = wholeTree.get("images");
            for (Iterator<JsonNode> imgIt = images.elements(); imgIt.hasNext();) {
                final JsonNode img = imgIt.next();
                final String filename = img.get("filename").asText();
                imgMap.put(filename, img);
            }

            if (((DeepScoresConfig) config).print_predefined_labels && !labelsPrinted) {
                printDSLabels(wholeTree);
                labelsPrinted = true;
            }
        }
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // addPart //
    //---------//
    @Override
    public void addPart (YoloPart part,
                         Path outImages,
                         Path outLabels)
        throws Exception
    {
        logger.info("Part {}. Start processing...", part);

        // For the count of Yolo labels
        final TreeMap<YoloLabel, Integer> labelCounts = new TreeMap<>();
        for (YoloLabel l : YoloLabel.values()) {
            labelCounts.put(l, 0);
        }

        // For images listing
        System.out.format("%nPart %s. Listing of images:%n", part);
        System.out.println("| Width | Height | Instances | Image |");
        System.out.println("|  ---: |   ---: |      ---: | :---  |");

        // For the count of DeepScores ignored labels
        final TreeMap<DeepScoresLabel, Integer> ignoredCounts = new TreeMap<>();
        for (DeepScoresLabel l : DeepScoresLabel.values()) {
            if (DeepScoresLabel.of(l) == null)
                ignoredCounts.put(l, 0);
        }

        final List<String> imgNames = (part == YoloPart.train) ? config.train : config.val;
        if (imgNames == null || imgNames.isEmpty()) {
            logger.info("No file names for part {}", part);
            return;
        }

        for (String imgName : imgNames) {
            final Path imgPath = imagesPath.resolve(imgName);

            // Copy image to target location
            Files.copy(imgPath, outImages.resolve(imgName), StandardCopyOption.REPLACE_EXISTING);

            // Get image dimension
            final JsonNode img = imgMap.get(imgName);
            final int imgWidth = img.get("width").asInt();
            final int imgHeight = img.get("height").asInt();

            // Get the related annotations
            final JsonNode ann_ids = img.get("ann_ids");

            // Line in images listing
            System.out.format(
                    "| %4d | %4d | %4d | %s |%n",
                    imgWidth,
                    imgHeight,
                    ann_ids.size(),
                    imgName);

            // Generate the related label file
            final String labelFile = outLabels.resolve(radixOf(imgName) + ".txt").toString();

            try (PrintWriter writer = new PrintWriter(labelFile)) {
                for (Iterator<JsonNode> annIt = ann_ids.elements(); annIt.hasNext();) {
                    final String id = annIt.next().asText();
                    final JsonNode annotation = retrieveAnnotation(id);
                    final int cat_id = annotation.get("cat_id").get(0).asInt();

                    // Find the corresponding YoloLabel, if any
                    final DeepScoresLabel dsLabel = DeepScoresLabel.values()[cat_id - 1];
                    final YoloLabel yoloLabel = DeepScoresLabel.of(dsLabel);

                    if (yoloLabel != null) {
                        labelCounts.put(yoloLabel, labelCounts.get(yoloLabel) + 1);

                        final JsonNode a_bbox = annotation.get("a_bbox");
                        final int x1 = a_bbox.get(0).asInt();
                        final int y1 = a_bbox.get(1).asInt();
                        final int x2 = a_bbox.get(2).asInt();
                        final int y2 = a_bbox.get(3).asInt();

                        writer.printf(
                                "%3d %f %f %f %f%n",
                                yoloLabel.ordinal(),
                                (x1 + x2) / (2.0 * imgWidth),
                                (y1 + y2) / (2.0 * imgHeight),
                                (x2 - x1 + 1) / (double) imgWidth,
                                (y2 - y1 + 1) / (double) imgHeight);
                    } else {
                        ignoredCounts.put(dsLabel, ignoredCounts.get(dsLabel) + 1);
                    }
                }
            }
        }

        // Print out the histogram count for each YOLO label
        System.out.format("\nPart %s. Counts of YOLO labels:\n", part);
        labelCounts.entrySet().forEach(
                entry -> System.out.format("%6d : %s%n", entry.getValue(), entry.getKey()));

        // Print out the histogram count for the ignored DS labels
        System.out.format("\nPart %s. Counts of DeepScores labels ignored:\n", part);
        ignoredCounts.entrySet().forEach(entry -> {
            if (entry.getValue() != null)
                System.out.format("%6d : %s%n", entry.getValue(), entry.getKey());
        });
    }

    @Override
    protected void drawAnnotations (String imgName,
                                    Graphics2D g2d)
        throws Exception
    {
        final JsonNode img = imgMap.get(imgName);
        final JsonNode ann_ids = img.get("ann_ids");
        System.out.println(imgName + " labels:" + ann_ids.size());

        for (Iterator<JsonNode> annIt = ann_ids.elements(); annIt.hasNext();) {
            final String id = annIt.next().asText();

            final JsonNode annotation = retrieveAnnotation(id);
            final int cat_id = annotation.get("cat_id").get(0).asInt();
            final String className = DeepScoresLabel.values()[cat_id - 1].name();
            if (isHidden(className)) {
                continue;
            }

            final JsonNode a_bbox = annotation.get("a_bbox");
            final int x = a_bbox.get(0).asInt();
            final int y = a_bbox.get(1).asInt();

            final int x2 = a_bbox.get(2).asInt();
            final int y2 = a_bbox.get(3).asInt();

            final int w = x2 - x + 1;
            final int h = y2 - y + 1;

            // Draw obj rectangle
            g2d.drawRect(x, y, w, h);

            // Draw class ID
            g2d.drawString(className, x, y);
        }
    }

    @Override
    protected void partHistogram (TreeMap<String, Tuple> map,
                                  YoloPart part)
        throws Exception
    {
        // DeepScores has a predefined list of classes
        // Make sure there is an entry for every class, even if no instance is ever found
        for (DeepScoresLabel label : DeepScoresLabel.values()) {
            if (map.get(label.name()) == null) {
                map.put(label.name(), new Tuple());
            }
        }

        final List<String> imgNames = (part == YoloPart.train) ? config.train : config.val;
        if (imgNames == null || imgNames.isEmpty()) {
            logger.info("No file names for part {}", part);
            return;
        }

        for (String imgName : imgNames) {
            final JsonNode img = imgMap.get(imgName);
            final JsonNode ann_ids = img.get("ann_ids");

            for (Iterator<JsonNode> annIt = ann_ids.elements(); annIt.hasNext();) {
                final String id = annIt.next().asText();

                final JsonNode annotation = retrieveAnnotation(id);
                final int cat_id = annotation.get("cat_id").get(0).asInt();
                final String className = DeepScoresLabel.values()[cat_id - 1].name();

                Tuple tuple = map.get(className);

                if (tuple == null) {
                    map.put(className, tuple = new Tuple());
                }

                if (part == YoloPart.train) {
                    tuple.train++;
                } else {
                    tuple.val++;
                }
            }
        }
    }

    private JsonNode retrieveAnnotation (String id)
    {
        // We have to search in both annotations nodes
        for (JsonNode annotations : annotationsList) {
            final JsonNode annotation = annotations.get(id);

            if (annotation != null) {
                return annotation;
            }
        }

        logger.error("No annotation found for ID: {}", id);
        return null;
    }

    //---------------//
    // printDSLabels //
    //---------------//
    /**
     * Print out the DeepScores labels id and name.
     *
     * @param wholeTree JSON tree
     */
    private void printDSLabels (JsonNode wholeTree)
    {
        final Map<Integer, String> cats = new TreeMap<>();
        final JsonNode categories = wholeTree.get("categories");

        categories.properties().forEach(entry -> {
            final int key = Integer.decode(entry.getKey());
            final JsonNode value = entry.getValue();
            final String name = value.get("name").asText();
            final String set = value.get("annotation_set").asText();

            if (set.equals("deepscores")) {
                cats.put(key - 1, name);
            }
        });

        System.out.println("\nDeepScores predefined labels ID and name:");

        cats.entrySet().forEach(
                e -> System.out.println(String.format("  %d: %s", e.getKey(), e.getValue())));
    }
}
