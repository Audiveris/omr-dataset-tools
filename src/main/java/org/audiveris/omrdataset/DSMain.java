//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           D S M a i n                                          //
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

import org.audiveris.omrdataset.api.Context;
import org.audiveris.omrdataset.api.DeepScoresShape;
import org.audiveris.omrdataset.api.GeneralContext;
import org.audiveris.omrdataset.api.GeneralShape;
import org.audiveris.omrdataset.extraction.DeepScoresReader;
import org.audiveris.omrdataset.extraction.DeepScoresReader.ArchiveCounts;
import org.audiveris.omrdataset.extraction.DeepScoresReader.ImageName;
import org.audiveris.omrdataset.training.MobileNetV2;
import org.audiveris.omrdataset.training.PixelPreProcessor;
import org.audiveris.omrdataset.training.ResNet18V2;
import org.audiveris.omrdataset.training.ResNet34V2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.datasets.iterator.INDArrayDataSetIterator;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.nn.modelimport.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.exceptions.UnsupportedKerasConfigurationException;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.model.stats.StatsListener;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.factory.Nd4j;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Class <code>DSMain</code> is the main class for DeepScores V2 dataset.
 * <p>
 * It processes a json descriptor, extract samples and train / test the selected model.
 *
 * @author Hervé Bitteur
 */
public class DSMain
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DSMain.class);

    /** General context. */
    public static final Context context = GeneralContext.INSTANCE;

    public static final Path FREQUENCIES_PATH = Paths.get("frequencies.json");

    /** Desired sample quorum for each shape within an archive. */
    public static final int SAMPLE_QUORUM = 60;

    /** DSCLI Parameters. */
    public static DSCLI cli;

    public static Path imagesFolder;

    public static Path tallyDir;

    public static JsonNode categories;

    //~ Enumerations -------------------------------------------------------------------------------
    public static enum Archi
    {
        ResNet18V2,
        ResNet34V2,
        MobileNetV2;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    private final ObjectMapper mapper = new ObjectMapper();

    private final Path modelPath;

    /** Normalization. */
    private final DataSetPreProcessor preProcessor = new PixelPreProcessor();

    //~ Constructors -------------------------------------------------------------------------------
    private DSMain ()
    {
        modelPath = cli.outputFolder.resolve(getDefaultModelFileName());
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // main //
    //------//
    public static void main (String[] args)
            throws Exception
    {
        logger.info("{}", LocalDateTime.now());
        cli = DSCLI.create(args);

        if (cli.help) {
            return; // Help has already been printed by DSCLI itself
        }

        if (cli.dataset == null) {
            logger.warn("-dataset not specified");
            return;
        }

        if (!Files.exists(cli.dataset)) {
            logger.warn("Dataset {} does not exist", cli.dataset);
            return;
        }

        tallyDir = cli.outputFolder.resolve("tally");
        imagesFolder = cli.dataset.resolve("images");

        new DSMain().process();
        logger.info("{}", LocalDateTime.now());

        logger.info("Calling System.exit");
        System.exit(0); // This is the end (despite still active threads?)
    }

    //---------//
    // process //
    //---------//
    public void process ()
            throws Exception
    {

        if (cli.gen_categories) {
            genCategories();
        }

        if (cli.gen_image_map) {
            genImageMap();
        }

        if (!cli.tallies.isEmpty()) {
            genShapeTallies();
        }

        if (cli.dense_nones) {
            genDenseNones();
        }

        if (!cli.dense_train.isEmpty()) {
            denseTrain();
        }

        if (!cli.nones.isEmpty()) {
            genNones();
        }

        if (!cli.samples.isEmpty()) {
            selectSamples();
        }

        if (!cli.train.isEmpty()) {
            train();
        }

        if (!cli.test.isEmpty()) {
            test();
        }

        if (!cli.dense_test.isEmpty()) {
            denseTest();
        }
    }

    //-------------//
    // genImageMap //
    //-------------//
    /**
     * Generate file 'tally/images.json', which gives image filename/width/height via its id.
     *
     * @throws Exception
     */
    public void genImageMap ()
            throws Exception
    {
        final SortedMap<Integer, ImageInfo> grandMap = new TreeMap<>();

        Files.walk(cli.dataset, 1)
                .forEach((Path p) -> {
                    System.out.println(p.toString());

                    // We take BOTH xxx_train.json and xxx_test.json to get ALL images in dataset
                    if (p.toString().endsWith(".json")) {
                        final DeepScoresReader reader = new DeepScoresReader(-1, p.toFile());
                        try {
                            grandMap.putAll(reader.getImageMap());
                        } catch (Exception ex) {
                            logger.warn("Error processing {}", p, ex);
                        }
                    }
                });

        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        mapper.writeValue(tallyDir.resolve("images.json").toFile(), grandMap);
    }

    //----------//
    // genNones //
    //----------//
    /**
     * Add "none" symbols for all images (there is no "none" symbol in DeepScores V2)
     * in the 'complete' data-set.
     * <p>
     * Rather than modifying the existing files ("deepscores-complete-N_test.json" and
     * "deepscores-complete-N_train.json"), we generate specific files ("none2locs-N_test.json" and
     * "none2locs-N_train.json".
     * <pre>
     * {
     * "7" : {
     *  "filename" : "lg-21117253-aug-emmentaler--page-2.png",
     *  "nones" : [ {
     *      "a_bbox" : [ 732, 843, 732, 843 ],
     *      "cat_id" : [ "0", "0" ]
     *      }, {
     *      "a_bbox" : [ 1098, 2045, 1098, 2045 ],
     *      "cat_id" : [ "0", "0" ]
     *      }, {
     *      "a_bbox" : [ 1196, 1123, 1196, 1123 ],
     *      "cat_id" : [ "0", "0" ]
     *      },
     *      ...
     *      ]},
     * ...
     * }
     * </pre>
     * <p>
     */
    private void genNones ()
            throws Exception
    {
        for (int iArch : cli.nones) {
            for (String suffix : new String[]{"_train", "_test"}) {
                final String descName = "deepscores-complete-" + iArch + suffix + ".json";
                final Path descPath = cli.dataset.resolve(descName);

                if (Files.exists(descPath)) {
                    logger.info("Generate nones for Archive: {}{}", iArch, suffix);
                    final DeepScoresReader reader = new DeepScoresReader(iArch, descPath.toFile());
                    final Map<Integer, DeepScoresReader.ImageNones> nones = reader.genNones();
                    final Path path = tallyDir.resolve("none2locs-" + iArch + suffix + ".json");
                    mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
                    mapper.writeValue(path.toFile(), nones);
                }
            }
        }
    }

    //-----------------//
    // genShapeTallies //
    //-----------------//
    private void genShapeTallies ()
            throws Exception
    {
        for (int iArch : cli.tallies) {
            final String descName = "deepscores-complete-" + iArch + "_train.json";
            final Path descPath = cli.dataset.resolve(descName);
            final DeepScoresReader reader = new DeepScoresReader(iArch, descPath.toFile());
            final Path targetPath = tallyDir.resolve("shape2anns-" + iArch + ".json");
            reader.genShapeTally(targetPath.toFile());
        }
    }

    //---------------//
    // genDenseNones //
    //---------------//
    /**
     * Add "none" symbols for all images in the 'dense' data-set.
     */
    public void genDenseNones ()
            throws Exception
    {
        for (String suffix : new String[]{"_train", "_test"}) {
            final String descName = "deepscores" + suffix + ".json";
            final Path descPath = cli.dataset.resolve(descName);

            if (Files.exists(descPath)) {
                logger.info("Generate Nones for dense: {}", suffix);
                final DeepScoresReader reader = new DeepScoresReader(-1, descPath.toFile());
                final Map<Integer, DeepScoresReader.ImageNones> nones = reader.genNones();
                final Path path = tallyDir.resolve("none2locs" + suffix + ".json");
                mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
                mapper.writeValue(path.toFile(), nones);
            }
        }
    }

    public void reformat ()
            throws Exception
    {
        final File file = tallyDir.resolve("CountPerShape.json").toFile();
        logger.info("Reading {}", file);
        final ArchiveCounts grandCounts = mapper.readValue(
                file,
                new TypeReference<ArchiveCounts>()
        {
        });

        final SortedArchiveCounts sorted = new SortedArchiveCounts();
        sorted.total = grandCounts.total;
        for (Entry<GeneralShape, Integer> entry : grandCounts.count_map.entrySet()) {
            sorted.counts.add(new ShapeCount(entry.getKey(), entry.getValue()));
        }

        Collections.sort(sorted.counts, (sc1, sc2) -> Integer.compare(sc1.count, sc2.count));

        final File sortedFile = tallyDir.resolve("SortedCountPerShape.json").toFile();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.writeValue(sortedFile, sorted);
    }

    //---------------//
    // selectSamples //
    //---------------//
    /**
     * Archive per archive, select image samples, while trying to keep balance between shapes.
     * <p>
     * Shapes ratios are provided by SortedCountPerShape.json
     * <p>
     * Per archive:
     * <ul>
     * <li>Inputs:
     * <ol>
     * <li>shape2anns-N.json, generated by
     * <li>none2locs-N_train.json, generated by '-nones' option
     * </ol>
     * <li>Output: img2anns-N.json
     * </ul>
     *
     * @throws Exception
     */
    public void selectSamples ()
            throws Exception
    {
        final int[] counts = new int[DeepScoresShape.values().length];
        final File sortedFile = tallyDir.resolve("SortedCountPerShape.json").toFile();
        final SortedArchiveCounts sorted = mapper.readValue(
                sortedFile,
                new TypeReference<SortedArchiveCounts>()
        {
        });

        final File imageFile = tallyDir.resolve("images.json").toFile();
        final Map<Integer, ImageInfo> imageMap = mapper.readValue(
                imageFile,
                new TypeReference<Map<Integer, ImageInfo>>()
        {
        });

        System.out.println("Total: " + sorted.total);
        for (ShapeCount sCount : sorted.counts) {
            System.out.println(String.format("%10d : %s", sCount.count, sCount.shape));
        }

        final JsonFactory factory = new MappingJsonFactory();

        for (int iArch : cli.samples) {
            logger.info("Processing archive {} ...", iArch);
            final File file = tallyDir.resolve("shape2anns-" + iArch + ".json").toFile();
            final JsonParser jp = factory.createParser(file);
            final JsonNode shapeMap = jp.readValueAsTree();

            final Map<String, Selection> img2anns = new HashMap<>();

            // Process shapes (per increasing count, but this is not really needed!)
            for (ShapeCount sCount : sorted.counts) {
                JsonNode mAnns = shapeMap.get(sCount.shape.toString());
                if (mAnns == null) {
                    logger.debug("No annotations for shape {} in archive {}", sCount.shape, iArch);
                    continue;
                }

                final int size = mAnns.size();
                final int step = Math.max(1, size / SAMPLE_QUORUM);
                logger.debug("shape {} size:{} step:{}", sCount.shape, size, step);
                int cnt = 0;
                for (Iterator<Entry<String, JsonNode>> it = mAnns.fields(); it.hasNext();) {
                    final Entry<String, JsonNode> entry = it.next();
                    if (++cnt != step) {
                        continue;
                    }

                    cnt = 0;
                    final JsonNode ma = entry.getValue();
                    final String imgId = ma.get("img_id").asText();

                    Selection selection = img2anns.get(imgId);
                    if (selection == null) {
                        img2anns.put(imgId, selection = new Selection());
                        selection.filename = imageMap.get(Integer.decode(imgId)).filename;
                    }

                    final BoxCat bc = new BoxCat();
                    bc.a_bbox = ma.get("a_bbox");
                    bc.cat_id = ma.get("cat_id");
                    counts[bc.cat_id.get(0).asInt()]++;
                    selection.selected_anns.add(bc);
                }
            }

            // Include a few nones to each selection
            final File noneFile = tallyDir.resolve("none2locs-" + iArch + "_train.json").toFile();
            logger.info("noneFile: {}", noneFile);
            final JsonParser noneJp = factory.createParser(noneFile);
            final JsonNode noneMap = noneJp.readValueAsTree();
            final Random random = new Random();
            for (Entry<String, Selection> entry : img2anns.entrySet()) {
                if (random.nextInt(50) != 0) {
                    continue; // A way to randomly pick up about 2% of img none symbols
                }

                final String imgId = entry.getKey();
                final Selection selection = entry.getValue();
                final JsonNode imgNones = noneMap.get(imgId);
                final JsonNode locs = imgNones.get("nones"); // An array of ~50 rectangles

                int toAdd = selection.selected_anns.size() / 2;
                for (Iterator<JsonNode> it = locs.elements(); it.hasNext();) {
                    final JsonNode loc = it.next();
                    final BoxCat bc = new BoxCat();
                    bc.a_bbox = loc.get("a_bbox");
                    bc.cat_id = loc.get("cat_id");
                    counts[bc.cat_id.get(0).asInt()]++;
                    selection.selected_anns.add(bc);

                    if (--toAdd <= 0) {
                        break;
                    }
                }

                Collections.shuffle(selection.selected_anns);
            }

            mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
            mapper.writeValue(tallyDir.resolve("img2anns-" + iArch + ".json").toFile(), img2anns);
        }

        // Print count of samples allocated per shape
        for (int i = 0; i < counts.length; i++) {
            System.out.println(String.format("cat:%3d count:%5d %s",
                                             i, counts[i], getShape("" + i)));
        }
    }

    //------------------//
    // computeAllCounts //
    //------------------//
    public void computeAllCounts ()
            throws Exception
    {
        final ArchiveCounts grandCounts = new ArchiveCounts();
        final int[] counts = new int[DSMain.context.getNumClasses()];

        for (int iArch : cli.archives) {
            final File file = tallyDir.resolve("Counts-" + iArch + ".json").toFile();
            logger.info("Reading {}", file);
            final ArchiveCounts archCounts = mapper.readValue(
                    file,
                    new TypeReference<ArchiveCounts>()
            {
            });

            grandCounts.total += archCounts.total;
            for (Entry<GeneralShape, Integer> entry : archCounts.count_map.entrySet()) {
                counts[entry.getKey().ordinal()] += entry.getValue();
            }
        }

        System.out.println("grandTotal: " + grandCounts.total);
        final GeneralShape[] values = GeneralShape.values();
        System.out.println("\nBy shape:");
        for (int i = 0; i < counts.length; i++) {
            final GeneralShape gShape = values[i];
            System.out.println(String.format("%3d : %7d %s", i, counts[i], gShape));
            grandCounts.count_map.put(gShape, counts[i]);
        }

        final File grandFile = tallyDir.resolve("CountPerShape.json").toFile();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        mapper.writeValue(grandFile, grandCounts);

        List<Entry<GeneralShape, Integer>> entries
                = new ArrayList<>(grandCounts.count_map.entrySet());

        // Print by increasing counts
        System.out.println("\nBy increasing count:");
        final Comparator<Entry<GeneralShape, Integer>> byCount = (e1, e2) -> Integer.compare(e1
                .getValue(), e2.getValue());
        Collections.sort(entries, byCount);
        for (Entry<GeneralShape, Integer> e : entries) {
            final int val = e.getValue();
            System.out.println(String.format("%10d : %7.3f%s %s",
                                             val, 100.0 * val / grandCounts.total, "%", e.getKey()));
        }
    }

    //-------//
    // train //
    //-------//
    /**
     * Train using the (balanced) SelectionSet of each selected archive.
     * <dl>
     * <dt>Inputs:</dt>
     * <dd>tally/<b>categories.json</b>, produced by <i>'-categories'</i> option</dd>
     * <dd>tally/<b>img2anns-N.json</b> for each selected archive N,
     * produced by <i>'-samples'</i> option</dd>
     * <dd>tally/<b>none2locs-N.json</b> for each selected archive N,
     * produced by <i>'-nones'</i> option</dd>
     * <dt>Outputs:</dt>
     * <dd>Model being trained</dd>
     * </dl>
     *
     * @throws Exception
     */
    public void train ()
            throws Exception
    {
        // 1. Restore model or create from scratch
        ComputationGraph model = restoreModel();
        if (model == null) {
            model = createModel();
            logger.info(model.summary());
        }

        // 2. Launch model training on train dataset
        UIServer uiServer = UIServer.getInstance();
        StatsStorage statsStorage = new InMemoryStatsStorage();
        uiServer.attach(statsStorage);
        model.setListeners(new StatsListener(statsStorage),
                           new ScoreIterationListener(1));

        logger.info("Training model on {} epochs...", cli.epochs);
        for (int epoch = 1; epoch <= cli.epochs; epoch++) {
            if (Files.exists(modelPath)) {
                backupModel(); // Backup model file with time stamp before training epoch
            }

            final long start = System.currentTimeMillis();

            for (int iArch : cli.train) {
                logger.info("Processing archive {}", iArch);
                final INDArrayDataSetIterator trainIter;

//                if (cli.balanced) {
                final File img2anns = tallyDir.resolve("img2anns-" + iArch + ".json").toFile();
                final SelectionSet selectionSet = new SelectionSet(getCategories(), img2anns);
                trainIter = new INDArrayDataSetIterator(selectionSet, cli.minibatch);
//                } else {
//                    final String descName = "deepscores-complete-" + iArch + "_train.json";
//                    final File descFile = cli.dataset.resolve(descName).toFile();
//                    final String noneName = "none2locs-" + iArch + "_train.json";
//                    final File noneFile = tallyDir.resolve(noneName).toFile();
//                    final ImageSet imageSet = new ImageSet(iArch, descFile, noneFile);
//                    trainIter = new INDArrayDataSetIterator(imageSet, cli.minibatch);
//                }

                trainIter.setPreProcessor(preProcessor);

                model.fit(trainIter);
            }

            double duration = System.currentTimeMillis() - start;
            logger.info("{}", LocalDateTime.now());
            logger.info(String.format("*** End epoch #%d/%d, duration: %.0f mn",
                                      epoch, cli.epochs, duration / 60000));

            // 3. Save model at each epoch end
            saveModel(model);
        }
    }

    //------------//
    // denseTrain //
    //------------//
    /**
     * Train on the (unbalanced) dense ImageSet.
     * cli.dense-train provides the desired image indices
     *
     * @throws Exception
     */
    private void denseTrain ()
            throws Exception
    {
        // 1. Restore model or create from scratch
        ComputationGraph model = restoreModel();
        if (model == null) {
            model = createModel();
            logger.info(model.summary());
        }

        // 2. Launch model training on train dataset
        UIServer uiServer = UIServer.getInstance();
        StatsStorage statsStorage = new InMemoryStatsStorage();
        uiServer.attach(statsStorage);
        model.setListeners(new StatsListener(statsStorage),
                           new ScoreIterationListener(1));

        logger.info("Training model on {} epochs...", cli.epochs);
        for (int epoch = 1; epoch <= cli.epochs; epoch++) {
            if (Files.exists(modelPath)) {
                backupModel(); // Backup model file with time stamp before training epoch
            }

            final long start = System.currentTimeMillis();

            logger.info("Processing dense dataset");
            final INDArrayDataSetIterator trainIter;

            final String descName = "deepscores_train.json";
            final File descFile = cli.dataset.resolve(descName).toFile();
            final String noneName = "none2locs_train.json";
            final File noneFile = tallyDir.resolve(noneName).toFile();
            final ImageSet imageSet = new ImageSet(-1, descFile, noneFile, cli.dense_train);
            trainIter = new INDArrayDataSetIterator(imageSet, cli.minibatch);
            trainIter.setPreProcessor(preProcessor);

            model.fit(trainIter);

            double duration = System.currentTimeMillis() - start;
            logger.info("{}", LocalDateTime.now());
            logger.info(String.format("*** End epoch #%d/%d, duration: %.0f mn",
                                      epoch, cli.epochs, duration / 60000));

            // 3. Save model at each epoch end
            saveModel(model);
        }
    }

    //------//
    // test //
    //------//
    private void test ()
            throws Exception
    {
        // 1. Restore model
        final ComputationGraph model = restoreModel();

        // 2. Run model evaluation on test dataset
        for (int iArch : cli.test) {
            final File img2anns = tallyDir.resolve("img2anns-" + iArch + ".json").toFile();
            final SelectionSet selectionSet = new SelectionSet(getCategories(), img2anns);
            final INDArrayDataSetIterator testIter = new INDArrayDataSetIterator(selectionSet,
                                                                                 cli.minibatch);
            testIter.setPreProcessor(preProcessor);

            final Evaluation eval = model.evaluate(testIter);

            eval.setLabelsList(DSMain.context.getLabelList());
            logger.info("{}", eval.stats(false, true));
        }
    }

    //-----------//
    // denseTest //
    //-----------//
    /**
     * Test on the (unbalanced) dense ImageSet.
     *
     * @throws Exception
     */
    private void denseTest ()
            throws Exception
    {
        // 1. Restore model
        final ComputationGraph model = restoreModel();

        // 2. Run model evaluation on test dataset
        logger.info("Processing dense dataset");
        final String descName = "deepscores_test.json";
        final File descFile = cli.dataset.resolve(descName).toFile();
        final String noneName = "none2locs_test.json";
        final File noneFile = tallyDir.resolve(noneName).toFile();
        final ImageSet imageSet = new ImageSet(-1, descFile, noneFile, cli.dense_test);
        final INDArrayDataSetIterator testIter = new INDArrayDataSetIterator(imageSet,
                                                                             cli.minibatch);
        testIter.setPreProcessor(preProcessor);

        final Evaluation eval = model.evaluate(testIter);

        eval.setLabelsList(DSMain.context.getLabelList());
        logger.info("{}", eval.stats(false, true));
    }

    //-------------//
    // createModel //
    //-------------//
    private ComputationGraph createModel ()
    {
        final INDArray lossWeights = getLossWeights();

        switch (cli.archi) {
        case ResNet18V2:
            return new ResNet18V2(context.getContextHeight(), context.getContextWidth(), 1,
                                  context.getNumClasses()).create(lossWeights);
        case ResNet34V2:
            return new ResNet34V2(context.getContextHeight(), context.getContextWidth(), 1,
                                  context.getNumClasses()).create(lossWeights);
        default:
        case MobileNetV2:
            return new MobileNetV2(context.getContextHeight(), context.getContextWidth(), 1,
                                   context.getNumClasses()).create(lossWeights);
        }
    }

    //--------------//
    // restoreModel //
    //--------------//
    private ComputationGraph restoreModel ()
    {
        if (!Files.exists(modelPath)) {
            logger.info("Model not found as {}", modelPath);
            return null;
        }

        try {
            logger.info("Restoring model from {}", modelPath);
            final ComputationGraph model = cli.useKeras
                    ? KerasModelImport.importKerasModelAndWeights(modelPath.toString())
                    : ModelSerializer.restoreComputationGraph(modelPath.toFile(), true);

            logger.info("Model restored.");
            ///logger.info(model.summary());

            return model;
        } catch (IOException |
                 InvalidKerasConfigurationException |
                 UnsupportedKerasConfigurationException ex) {
            logger.warn("Error restoring model {}", ex);
            return null;
        }
    }

    //-----------//
    // saveModel //
    //-----------//
    private void saveModel (ComputationGraph model)
    {
        try {
            ModelSerializer.writeModel(model, modelPath.toFile(), true);
            logger.info("Model saved as {}", modelPath.toAbsolutePath());
        } catch (IOException ex) {
            logger.warn("Error saving model {}", ex);
        }
    }

    //-------------//
    // backupModel //
    //-------------//
    private void backupModel ()
    {
        try {
            final LocalDateTime now = LocalDateTime.now();
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
            final Path backup = modelPath.resolveSibling(now.format(formatter)
                                                                 + "-" + modelPath.getFileName());
            Files.copy(modelPath, backup);
            logger.info("Model backup as {}", backup.toAbsolutePath());
        } catch (IOException ex) {
            logger.warn("Error in model backup {}", ex);

        }
    }

    //-------------------------//
    // getDefaultModelFileName //
    //-------------------------//
    /**
     * Report the default model file name, depending on selected model architecture and
     * on context values (patch height, patch width, number of output classes).
     *
     * @return the default model file name
     */
    private static String getDefaultModelFileName ()
    {
        return new StringBuilder(cli.archi + "_")
                .append('h').append(context.getContextHeight())
                .append('w').append(context.getContextWidth())
                .append('s').append(context.getNumClasses())
                .append(".h5")
                .toString();
    }

    //----------------//
    // getLossWeights //
    //----------------//
    /**
     * An attempt to train network with loss weights depending on sample shape.
     * <p>
     * The idea is to compensate for the very unbalanced training data-set where note head shapes
     * are over-represented.
     *
     * @return the vector of loss weights to apply
     */
    public static INDArray getLossWeights ()
    {
        System.out.println("LossWeights:");
        final GeneralShape[] values = GeneralShape.values();
        final int numClasses = values.length;

//        return Nd4j.ones(numClasses); // This means: same weight for every shape
        try {
            final double[] weights = new double[numClasses];
            final Map<GeneralShape, Float> map = new ObjectMapper().readValue(
                    FREQUENCIES_PATH.toFile(),
                    new TypeReference<Map<GeneralShape, Float>>()
            {
            });

            for (GeneralShape shape : values) {
                // BINGO - AWKFUL TRICK FOR "none" shape
                float f = (shape == GeneralShape.none) ? 0.30f : map.get(shape);
                final double w = Math.exp(-10 * f);
                final int ord = shape.ordinal();
                weights[ord] = w;
                System.out.println(String.format("    %3d %10.6f %s %f ", ord, w, shape, f));
            }

            return Nd4j.create(weights);
        } catch (IOException ex) {
            logger.warn("Error in getLossWeights " + ex, ex);
        }
        return null;
    }

    //---------------//
    // getCategories //
    //---------------//
    /**
     * Load categories once for all, from "categories.json" file.
     */
    private static JsonNode getCategories ()
    {
        if (categories == null) {
            final File file = tallyDir.resolve("categories.json").toFile();
            logger.info("Reading {}", file);

            try {
                categories = new ObjectMapper().readTree(file);
            } catch (IOException ex) {
                logger.warn("Error reading {} {}", file, ex);
            }
        }

        return categories;
    }

    //----------//
    // getShape //
    //----------//
    /**
     * Report the shape name for the given category id.
     *
     * @param catId provided category id in [0 .. 175]
     * @return shape name in [none, brace, ...]
     */
    public static String getShape (String catId)
    {
        if (catId.equals("0")) {
            return DeepScoresShape.none.toString();
        }

        final JsonNode cat = getCategories().get(catId);

        if (cat != null) {
            return cat.get("name").asText();
        } else {
            logger.info("Could not find category {}", catId);
            return null;
        }
    }

    //-----------------//
    // getGeneralShape //
    //-----------------//
    public static GeneralShape getGeneralShape (JsonNode ann)
    {
        final JsonNode catidNode = ann.get("cat_id").get(0);
        final String shapeString = getShape(catidNode.asText());
        final DeepScoresShape dsShape = DeepScoresShape.valueOf(shapeString);

        return dsShape.toGeneralShape();
    }

    //---------------//
    // genCategories //
    //---------------//
    /**
     * Take a descriptor file and write down the "tally/categories.json" file.
     * <p>
     * This routine works for either dense or complete data-set.
     */
    private void genCategories ()
    {
        final File targetFile = tallyDir.resolve("categories.json").toFile();

        for (String descName : new String[]{"deepscores_test.json",
                                            "deepscores-complete-0_test.json"}) {
            final Path descPath = cli.dataset.resolve(descName);

            if (Files.exists(descPath)) {
                final DeepScoresReader reader = new DeepScoresReader(-1, descPath.toFile());

                try {
                    reader.genCategories("deepscores", targetFile);
                    logger.info("File {} created.", targetFile);
                    return;
                } catch (Exception ex) {
                    logger.warn("Error in genCategories {}", ex);
                }
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // BoxCat //
    //--------//
    public static class BoxCat
    {

        public JsonNode a_bbox;

        public JsonNode cat_id;
    }

    //-----------//
    // ImageInfo //
    //-----------//
    public static class ImageInfo
    {

        public String filename;

        public int width;

        public int height;

        public ImageInfo ()
        {
        }

        public ImageInfo (String filename,
                          int width,
                          int height)
        {
            this.filename = filename;
            this.width = width;
            this.height = height;
        }
    }

    //-----------//
    // Selection //
    //-----------//
    public static class Selection
    {

        public String filename;

        public List<BoxCat> selected_anns = new ArrayList<>();
    }

    //------------//
    // ShapeCount //
    //------------//
    public static class ShapeCount
    {

        public GeneralShape shape;

        public int count;

        public ShapeCount ()
        {

        }

        public ShapeCount (GeneralShape shape,
                           int count)
        {
            this.shape = shape;
            this.count = count;
        }

    }

    //---------------------//
    // SortedArchiveCounts //
    //---------------------//
    public static class SortedArchiveCounts
    {

        public int total;

        public List<ShapeCount> counts = new ArrayList<>();
    }
}
