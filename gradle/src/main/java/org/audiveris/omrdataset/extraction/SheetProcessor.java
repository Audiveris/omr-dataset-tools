//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S h e e t P r o c e s s o r                                  //
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
package org.audiveris.omrdataset.extraction;

import java.awt.Graphics2D;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omrdataset.Main;
import org.audiveris.omrdataset.api.OmrShape;
import org.audiveris.omrdataset.api.SheetAnnotations;
import org.audiveris.omrdataset.api.SymbolInfo;
import org.audiveris.omrdataset.api.TablatureAreas;
import org.audiveris.omrdataset.api.ZhawAnnotations;
import org.audiveris.omrdataset.extraction.SourceInfo.Collection;
import org.audiveris.omrdataset.extraction.SourceInfo.XmlFormat;
import org.audiveris.omrdataset.extraction.SourceInfo.USheetId;
import static org.audiveris.omrdataset.training.App.CONTROL_EXT;
import static org.audiveris.omrdataset.training.App.CONTROL_FOLDER_NAME;
import static org.audiveris.omrdataset.training.App.CSV_EXT;
import static org.audiveris.omrdataset.training.App.FEATURES_FOLDER_NAME;
import static org.audiveris.omrdataset.training.App.FILTERED_EXT;
import static org.audiveris.omrdataset.training.App.FILTERED_FOLDER_NAME;
import static org.audiveris.omrdataset.training.App.IMAGES_FOLDER_NAME;
import static org.audiveris.omrdataset.training.App.INFO_EXT;
import static org.audiveris.omrdataset.training.App.MAX_SYMBOL_SCALE;
import static org.audiveris.omrdataset.training.App.NONE_RATIO;
import static org.audiveris.omrdataset.training.App.NONES_EXT;
import static org.audiveris.omrdataset.training.App.NONES_FOLDER_NAME;
import static org.audiveris.omrdataset.training.App.PATCHES_FOLDER_NAME;
import static org.audiveris.omrdataset.training.App.TABLATURES_EXT;
import static org.audiveris.omrdataset.training.App.TABLATURES_FOLDER_NAME;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Class {@code SheetProcessor} processes a whole sheet (image + annotations)
 * to filter its raw source and/or to extract its features.
 * <p>
 * Several instances, each on its dedicated sheet, can run in parallel according to CLI option.
 * It can also draw the symbols boxes and the None symbols locations on top of sheet image for
 * visual check.
 * <p>
 * It can also draw the patches built around each symbol.
 *
 * @author Hervé Bitteur
 */
public class SheetProcessor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SheetProcessor.class);

    private static final DecimalFormat decimal = new DecimalFormat();

    static {
        decimal.setGroupingUsed(false);
        decimal.setMaximumFractionDigits(3); // For a maximum of 3 decimals
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Universal sheet id. */
    private final USheetId uSheetId;

    /**
     * Path to raw sheet (SHEET.xml) annotations file.
     * Read from archive-N/<b>xml_annotations</b> folder.
     */
    private final Path infoPath;

    /**
     * Path to sheet (SHEET.tablatures.xml) tablatures file, if any.
     * Found in archive-N/<b>tablatures</b> folder.
     */
    private final Path tablaturesPath;

    /**
     * Path to sheet filtered (SHEET.filtered.xml) annotations file.
     * Written in archive-N/<b>filtered</b> folder
     */
    private final Path filteredPath;

    /** Sheet name radix (sans extension). */
    private final String radix;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SheetProcessor} object.
     *
     * @param uSheetId universal sheet id
     */
    public SheetProcessor (USheetId uSheetId)
    {
        this.uSheetId = uSheetId;
        this.infoPath = SourceInfo.getPath(uSheetId);

        radix = FileUtil.avoidExtensions(infoPath.getFileName().toString(), INFO_EXT);
        tablaturesPath = Main.archiveFolder.resolve(TABLATURES_FOLDER_NAME)
                .resolve(radix + TABLATURES_EXT);
        filteredPath = Main.archiveFolder.resolve(FILTERED_FOLDER_NAME)
                .resolve(radix + FILTERED_EXT);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Process the sheet (image / annotations) to append the extracted features
     * (the context patch for each symbol) to the out stream.
     * <p>
     * NOTA: if a patch goes beyond image borders, we fill the related external pixels with
     * background value.
     */
    public void process ()
    {
        try {
            logger.info("{} Processing {}", id(), infoPath);
            final StopWatch watch = new StopWatch("SheetProcessor " + infoPath);
            SheetAnnotations annotations = null;
            BufferedImage initialImg = null;

            if (Main.cli.filter) {
                // Run step #1 (source filtering)?
                //--------------------------------
                // This step is mandatory for the very first use of the (raw) sheet annotations
                // which are converted/checked and then saved as filtered sheet annotations.
                watch.start("source");
                Collection collection = uSheetId.getCollectionId().getCollection();
                annotations = (collection.getXmlFormat() == XmlFormat.ZHAW)
                        ? ZhawAnnotations.unmarshal(infoPath).toSheetAnnotations()
                        : SheetAnnotations.unmarshal(infoPath);
                logger.info("{} {}", id(), annotations);

                if (annotations == null) {
                    logger.warn("{} No Annotations structure in {}", id(), infoPath);
                    return;
                }

                if (annotations.getOuterSymbolsLiveList().isEmpty()) {
                    logger.info("{} No symbols found in {}", id(), infoPath);
                    return;
                }

                // Convert some scaled shapes
                convertScaledShapes(annotations);

                // Tablature informations?
                TablatureAreas tablatures = null;
                if (Files.exists(tablaturesPath)) {
                    logger.info("{} Tablatures areas found", id());
                    tablatures = TablatureAreas.unmarshal(tablaturesPath);
                }

                // More serious checks on symbols, which may discard some of them
                new Filter(uSheetId, annotations, tablatures).process();

                // Save filtered annotations
                watch.start("save filtered");
                annotations.setSource(annotations.getSource() + " filtered");
                logger.info("{} Saving filtered annotations as {}", id(), filteredPath);
                annotations.marshall(filteredPath);
            }

            // From now on, we require the *filtered* annotations
            //
            if (Main.cli.nones) {
                // Run step #2 (nones)?
                //---------------------
                // This step augments the filtered annotations with artificial "none" annotations
                watch.start("nones");
                if (annotations == null) {
                    annotations = SheetAnnotations.unmarshal(filteredPath);
                }

                // Discard any none symbol already injected (potentially)
                removeNoneSymbols(annotations.getOuterSymbolsLiveList());

                // Augment annotations with none symbols
                int nb = (int) Math.rint(NONE_RATIO * annotations.getGoodSymbols().size());
                logger.info("{} Creating {} none symbols", id(), nb);
                Path nonesPath = Main.contextFolder.resolve(NONES_FOLDER_NAME)
                        .resolve(radix + NONES_EXT);
                annotations.getOuterSymbolsLiveList().addAll(
                        new NonesBuilder(uSheetId, annotations, nonesPath).insertNones(nb));
                annotations.marshall(filteredPath);
            }

            if (Main.cli.histo) {
                // Histogram? (optional)
                watch.start("histo");
                if (annotations == null) {
                    annotations = SheetAnnotations.unmarshal(filteredPath);
                }

                // Print out shape histogram for each sheet
                new Histogram(uSheetId, annotations).print();
            }

            // Control? (optional)
            if (Main.cli.control) {
                watch.start("control");
                if (annotations == null) {
                    annotations = SheetAnnotations.unmarshal(filteredPath);
                }

                // Generate control images for visual checking
                // All symbols in sheet displayed with their bounds in blue
                // Plus the none locations if any, displayed as small red crosses
                initialImg = getImage(getImagePath(infoPath, annotations.getSheetInfo()));
                Path controlPath = Main.contextFolder.resolve(CONTROL_FOLDER_NAME)
                        .resolve(radix + CONTROL_EXT);
                new Control(uSheetId, annotations).build(controlPath, initialImg);
            }

            if (Main.cli.features) {
                // Run step #3 (features)?
                //------------------------
                if (annotations == null) {
                    annotations = SheetAnnotations.unmarshal(filteredPath);
                }

                if (initialImg == null) {
                    initialImg = getImage(getImagePath(infoPath, annotations.getSheetInfo()));
                }

                // Extract features in the sheet for each symbol definition
                // relevant for the current context (for example: just heads and nones).
                // Features are patch pixel values written in compressed CSV format
                watch.start("features");
                final Path featuresPath = Main.contextFolder.resolve(FEATURES_FOLDER_NAME)
                        .resolve(radix + CSV_EXT);
                new Features(uSheetId, annotations).extract(initialImg, featuresPath);
            }

            if (Main.cli.patches) {
                // Patches? (optional)
                // Generate patch images meant for visual checking
                watch.start("patches");
                final Path featuresPath = Main.contextFolder.resolve(FEATURES_FOLDER_NAME)
                        .resolve(radix + CSV_EXT);
                new Patches(uSheetId,
                            featuresPath,
                            Main.contextFolder.resolve(PATCHES_FOLDER_NAME).resolve(radix))
                        .process();
            }

            ///watch.print();
        } catch (Exception ex) {
            logger.warn("{} Error processing file {}", id(), infoPath, ex);
        }
    }

    //----//
    // id //
    //----//
    private String id ()
    {
        return uSheetId.toString();
    }

    //---------------------//
    // convertScaledShapes //
    //---------------------//
    /**
     * Some symbols have both a shape and a scale, often to indicate a smaller version.
     * <p>
     * For certain shapes with a lowering scale, we replace the original name by the small name.
     *
     * @param annotations
     */
    private void convertScaledShapes (SheetAnnotations annotations)
    {
        for (SymbolInfo symbol : annotations.getGoodSymbols()) {
            if ((symbol.getScale() != null) && (symbol.getScale() <= MAX_SYMBOL_SCALE)) {
                symbol.useSmallName();
            }
        }
    }

    //----------//
    // getImage //
    //----------//
    private BufferedImage getImage (Path imgPath)
            throws Exception
    {
        if (!Files.exists(imgPath)) {
            logger.warn("{} Could not find image {}", id(), imgPath);

            return null;
        }

        BufferedImage img = ImageIO.read(imgPath.toFile());
        logger.info("{} Loaded image {}", id(), imgPath);

        if (img.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            return img;
        }

        long start = System.currentTimeMillis();
        BufferedImage converted = new BufferedImage(img.getWidth(),
                                                    img.getHeight(),
                                                    BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = converted.createGraphics();
        g.drawImage(img, null, null);
        g.dispose();
        long dur = System.currentTimeMillis() - start;
        logger.info("Converting wrong image type {} {} in {} ms", img.getType(), imgPath, dur);

        return converted;
    }

    //--------------//
    // getImagePath //
    //--------------//
    private Path getImagePath (Path infoPath,
                               SheetAnnotations.SheetInfo sheetInfo)
    {
        // Related image file
        String fileName = sheetInfo.imageFileName;

        if (fileName == null) {
            logger.warn("{} No image link found", id());

            return null;
        }

        final Path imgDir = infoPath.getParent().resolveSibling(IMAGES_FOLDER_NAME);
        final Path imgPath = imgDir.resolve(fileName);

        return imgPath;
    }

    //-------------------//
    // removeNoneSymbols //
    //-------------------//
    private void removeNoneSymbols (List<SymbolInfo> symbols)
    {
        for (Iterator<SymbolInfo> it = symbols.iterator(); it.hasNext();) {
            if (it.next().getOmrShape() == OmrShape.none) {
                it.remove();
            }
        }
    }
}
