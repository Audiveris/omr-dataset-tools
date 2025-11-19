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
import java.awt.Rectangle;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * <p>
 * DeepScores handles dynamic symbols only at the letter level.
 * For instance, an "sfp" (sforzandoPiano) dynamic symbol is not described as such,
 * but as 3 separate symbols: "dynamicS", "dynamicF" and "dynamicP".
 * In this preparation work, we rebuild upfront these compound symbols.
 *
 * @author Hervé Bitteur
 */
public class DeepScores
        extends DataSetFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DeepScores.class);

    /** Dynamics that cannot be extended. */
    private static final List<String> finalDynamics = Arrays.asList("mf", "mp");

    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * Original map: cat_id -> class name.
     * This map represents the <b>original</b> names as found in the .json files,
     * that is, for dynamics, just the 1-letter dynamic symbols.
     */
    private Map<Integer, String> orgClassMap;

    /** List of the 2 annotations JSON nodes. */
    private final List<JsonNode> annotationsList = new ArrayList<>();

    /** Map: image name -> image JSON node. */
    private final Map<String, JsonNode> imgMap = new HashMap<>();

    /** Cached symbols of the image being processed. */
    private List<Symbol> cachedSymbols;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a new <code>DeepScores</code> instance.
     *
     * @param sourceConfig path to DeepScores .yaml descriptor
     * @throws java.lang.Exception
     */
    public DeepScores (String sourceConfig)
            throws Exception
    {
        logger.info("DeepScores dataset");

        config = yamlMapper.readValue(Paths.get(sourceConfig).toFile(), DeepScoresConfig.class);
        logger.info("{}", config);

        sourceDir = Paths.get(config.source);
        imagesPath = sourceDir.resolve(config.images);

        final ObjectMapper jsonMapper = new ObjectMapper();

        // Preload both .json files, to map annotations and images nodes
        for (String jsonName : new String[] { "deepscores_train.json", "deepscores_test.json" }) {
            final File jsonFile = sourceDir.resolve(jsonName).toFile();

            // Preload the whole json file
            logger.debug("Loading {}...", jsonName);
            final long start = System.currentTimeMillis();
            final JsonNode wholeTree = jsonMapper.readTree(jsonFile);
            final long stop = System.currentTimeMillis();
            logger.debug("File {} loaded in {} ms ", jsonName, stop - start);

            // Map the original class names
            if (orgClassMap == null) {
                orgClassMap = new TreeMap<>();
                final JsonNode categories = wholeTree.get("categories");
                for (Map.Entry<String, JsonNode> entry : categories.properties()) {
                    final JsonNode value = entry.getValue();
                    if (value.get("annotation_set").asText().equals("deepscores")) {
                        orgClassMap.put(Integer.decode(entry.getKey()), value.get("name").asText());
                    }
                }

                if (((DeepScoresConfig) config).print_predefined_labels) {
                    System.out.println("\nDeepScores ORIGINAL labels ID and name:");
                    orgClassMap.entrySet().forEach(
                            e -> System.out.println(
                                    String.format("  %d: %s", e.getKey(), e.getValue())));
                }
            }

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
        System.out.println("| Rank  | Width | Height | Instances | Image |");
        System.out.println("|  ---: |  ---: |   ---: |      ---: | :---  |");

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

        final int total = imgNames.size();
        int rank = 0;

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
                    "| %4d/%4d | %4d | %4d | %4d | %s |%n",
                    ++rank,
                    total,
                    imgWidth,
                    imgHeight,
                    ann_ids.size(),
                    imgName);

            // Retrieve all symbols
            final List<Symbol> symbols = getImageSymbols(imgName);

            // Generate the related label file
            final String labelFile = outLabels.resolve(radixOf(imgName) + ".txt").toString();

            try (PrintWriter writer = new PrintWriter(labelFile)) {
                for (Symbol symbol : symbols) {
                    // Find the corresponding YoloLabel, if any
                    DeepScoresLabel dsLabel = DeepScoresLabel.valueOf(symbol.name);
                    final YoloLabel yoloLabel = DeepScoresLabel.of(dsLabel);

                    if (yoloLabel != null) {
                        labelCounts.put(yoloLabel, labelCounts.get(yoloLabel) + 1);
                        final Rectangle rect = symbol.rect;
                        final double xc = rect.x + rect.width / 2.0;
                        final double yc = rect.y + rect.height / 2.0;
                        writer.printf(
                                "%3d %f %f %f %f%n",
                                yoloLabel.ordinal(),
                                xc / imgWidth,
                                yc / imgHeight,
                                rect.width / (double) imgWidth,
                                rect.height / (double) imgHeight);
                    } else {
                        ///ignoredCounts.put(dsLabel, ignoredCounts.get(dsLabel) + 1);
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
    protected boolean conditionMet (String imgName)
        throws Exception
    {
        cachedSymbols = null;

        // Check at least one required label is present in this page
        if (config.checking.required_labels == null) {
            return true;
        }

        final List<Symbol> symbols = getImageSymbols(imgName);

        for (Symbol symbol : symbols) {
            if (config.checking.required_labels.contains(symbol.name)) {
                System.out.println(symbol.toString());
                cachedSymbols = symbols;
                return true;
            }
        }

        return false;
    }

    @Override
    protected void drawAnnotations (String imgName,
                                    Graphics2D g2d)
        throws Exception
    {
        final List<Symbol> symbols = cachedSymbols != null ? cachedSymbols
                : getImageSymbols(imgName);
        System.out.println(imgName + " symbols:" + symbols.size());

        symbols.forEach(symbol -> {
            if (!isHidden(symbol.name)) {
                g2d.draw(symbol.rect);
                g2d.drawString(symbol.name, symbol.rect.x, symbol.rect.y);
            }
        });
    }

    /**
     * Retrieve (and merge) all image symbols of an image.
     *
     * @param imgName the image name
     * @return the symbols ready to use
     */
    private List<Symbol> getImageSymbols (String imgName)
    {
        final JsonNode img = imgMap.get(imgName);
        final JsonNode ann_ids = img.get("ann_ids");

        final List<Symbol> standardSymbols = new ArrayList<>();
        final List<Symbol> dynamicSymbols = new ArrayList<>();

        for (Iterator<JsonNode> annIt = ann_ids.elements(); annIt.hasNext();) {
            final String id = annIt.next().asText();
            final JsonNode annotation = retrieveAnnotation(id);
            final int cat_id = annotation.get("cat_id").get(0).asInt();
            final String className = orgClassMap.get(cat_id);
            final JsonNode a_bbox = annotation.get("a_bbox");
            final String letter = getDynamicLetter(className);

            if (letter == null) {
                standardSymbols.add(new Symbol(className, a_bbox));
            } else {
                dynamicSymbols.add(new Symbol(letter, a_bbox));
            }
        }

        if (!dynamicSymbols.isEmpty()) {
            mergeDynamicSymbols(dynamicSymbols);
        }

        standardSymbols.addAll(dynamicSymbols);
        return standardSymbols;
    }

    /**
     * Merge the 1-letter dynamic symbols into compound dynamic symbols wherever possible.
     *
     * @param dynamicSymbols the collection of dynamic symbols
     */
    private void mergeDynamicSymbols (List<Symbol> dynamicSymbols)
    {
        Collections.sort(dynamicSymbols); // Sort the candidates by their starting abscissa

        if (logger.isDebugEnabled()) {
            System.out.println("Dynamic candidates:");
            dynamicSymbols.forEach(s -> System.out.format("   %s%n", s));
        }

        // Retrieve the minimum symbol width
        int minWidth = Integer.MAX_VALUE;
        for (Symbol s : dynamicSymbols) {
            minWidth = Math.min(minWidth, s.rect.width);
        }

        final int gap = 3; // Should be enough for intersecting a sibling symbol
        final int dblMax = 3;
        logger.debug("gap:{} dblMax:{}", gap, dblMax);

        // Cache: The last (letter) symbol merged
        Symbol last;

        for (int i = 0; i < dynamicSymbols.size(); i++) {
            final Symbol left = dynamicSymbols.get(i);
            Rectangle fatLeft = new Rectangle(left.rect);
            fatLeft.width += gap;
            last = left;
            logger.debug("i:{} left:{} fat:{}", i, left, fatLeft);

            for (int j = i + 1; j < dynamicSymbols.size(); j++) {
                final Symbol right = dynamicSymbols.get(j);
                logger.debug("   j:{} right:{}", j, right);

                if (right.rect.x > fatLeft.x + fatLeft.width) {
                    logger.debug("   end");
                    break; // since symbols are sorted by their starting abscissa
                }

                if (fatLeft.intersects(right.rect)) {
                    // Verify the baselines are consistent between last and right symbols
                    final int lastBl = getBaseline(last);
                    final int rightBl = getBaseline(right);
                    final int dbl = Math.abs(rightBl - lastBl);
                    logger.debug("   Baseline delta:{}", dbl);

                    if (dbl <= dblMax || mergeBoosted(left, right)) {
                        // Extend left with right
                        left.name = left.name + right.name;
                        left.rect = left.rect.union(right.rect);
                        fatLeft = new Rectangle(left.rect);
                        fatLeft.width += gap;
                        logger.debug("   Extended i:{} left:{} fat:{}", i, left, fatLeft);
                        dynamicSymbols.remove(j--);
                        last = right;

                        if (finalDynamics.contains(left.name)) {
                            logger.debug("   final symbol");
                            break;
                        }
                    } else {
                        logger.debug("   Incompatible baselines");
                    }
                }
            }
        }

        // Update all dynamic names, i.e. "mf" -> "dynamicMF"
        dynamicSymbols.forEach(s -> s.name = "dynamic" + s.name.toUpperCase());

        if (logger.isDebugEnabled()) {
            System.out.println();
            System.out.println("Dynamic results:");
            dynamicSymbols.forEach(s -> System.out.format("   %s%n", s));
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

        imgNames.forEach(img -> {
            getImageSymbols(img).forEach(symbol -> {
                Tuple tuple = map.get(symbol.name);

                if (tuple == null) {
                    map.put(symbol.name, tuple = new Tuple());
                }

                if (part == YoloPart.train) {
                    tuple.train++;
                } else {
                    tuple.val++;
                }
            });
        });
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

    //~ Static Methods -----------------------------------------------------------------------------

    /**
     * Report the letter of a (1-letter) dynamic name, when applicable.
     *
     * @param className any symbol name to test
     * @return the letter for a 1-letter dynamic symbol name, null otherwise
     */
    private static String getDynamicLetter (String className)
    {
        return switch (className) {
            case "dynamicP" -> "p";
            case "dynamicM" -> "m";
            case "dynamicF" -> "f";
            case "dynamicS" -> "s";
            case "dynamicZ" -> "z";
            case "dynamicR" -> "r";
            default -> null;
        };
    }

    /**
     * Report the normalized vertical offset of the baseline for the provided dynamic letter.
     * <p>
     * NOTA: These ratios are OK for a standard musical font, much less for a Jazz font.
     *
     * @param letter a dynamic letter in (p, m, f, s, z, r)
     * @return vertical offset of letter baseline, normalized by letter height
     */
    private static Double getYOffsetRatio (String letter)
    {
        return switch (letter) {
            case "p" -> 0.66;
            case "m" -> 0.96;
            case "f" -> 0.74;
            case "s" -> 0.96;
            case "z" -> 0.96;
            case "r" -> 1.00;
            default -> null;
        };
    }

    /**
     * Report the ordinate of the baseline for the provided (dynamic) symbol.
     *
     * @param symbol the 1-letter dynamic symbol
     * @return the baseline ordinate
     */
    private static int getBaseline (Symbol symbol)
    {
        final Double ratio = getYOffsetRatio(symbol.name);
        return symbol.rect.y + (int) Math.rint(ratio * symbol.rect.height);
    }

    /**
     * Some left/right combinations should survive a baseline offset.
     *
     * @param left  symbol on left
     * @param right symbol on right
     * @return true if they should be merged
     */
    private static boolean mergeBoosted (Symbol left,
                                         Symbol right)
    {
        // m+p
        if (left.name.equals("m") && right.name.equals("p")) {
            return true;
        }

        // m+f
        if (left.name.equals("m") && right.name.equals("f")) {
            return true;
        }

        // r+f
        if (left.name.equals("r") && right.name.equals("f")) {
            return true;
        }

        // rf+z
        if (left.name.equals("rf") && right.name.equals("z")) {
            return true;
        }

        // s+f
        if (left.name.equals("s") && right.name.equals("f")) {
            return true;
        }

        // sf+z
        if (left.name.equals("sf") && right.name.equals("z")) {
            return true;
        }

        // sff+z
        if (left.name.equals("sff") && right.name.equals("z")) {
            return true;
        }

        // sfz+p
        if (left.name.equals("sfz") && right.name.equals("p")) {
            return true;
        }

        return false;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //------------------//
    // DeepScoresConfig //
    //------------------//
    private static class DeepScoresConfig
            extends DataSetConfig
    {
        public boolean print_predefined_labels;
    }

    //--------//
    // Symbol //
    //--------//
    private static class Symbol
            implements Comparable<Symbol>
    {
        public String name;

        public Rectangle rect;

        public Symbol (String name,
                       JsonNode a_bbox)
        {
            this.name = name;
            final int x1 = a_bbox.get(0).asInt();
            final int y1 = a_bbox.get(1).asInt();
            final int x2 = a_bbox.get(2).asInt();
            final int y2 = a_bbox.get(3).asInt();
            rect = new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
        }

        @Override
        public int compareTo (Symbol that)
        {
            // first abscissa, then ordinate
            if (this.rect.x != that.rect.x)
                return Integer.signum(this.rect.x - that.rect.x);
            if (this.rect.y != that.rect.y)
                return Integer.signum(this.rect.y - that.rect.y);
            if (this.rect.width != that.rect.width)
                return Integer.signum(this.rect.width - that.rect.width);
            if (this.rect.height != that.rect.height)
                return Integer.signum(this.rect.height - that.rect.height);
            return 0;
        }

        @Override
        public String toString ()
        {
            return new StringBuilder() //
                    .append(name) //
                    .append(" [").append(rect.x) //
                    .append(", ").append(rect.y) //
                    .append(", ").append(rect.width) //
                    .append(", ").append(rect.height) //
                    .append(']').toString();
        }
    }
}
