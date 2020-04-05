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

import org.audiveris.omr.util.StopWatch;
import org.audiveris.omrdataset.Main;
import org.audiveris.omrdataset.api.OmrShape;
import org.audiveris.omrdataset.api.SheetAnnotations;
import org.audiveris.omrdataset.api.SymbolInfo;
import org.audiveris.omrdataset.api.TablatureAreas;
import org.audiveris.omrdataset.api.ZhawAnnotations;
import org.audiveris.omrdataset.training.App;
import static org.audiveris.omrdataset.training.App.CONTROL_EXT;
import static org.audiveris.omrdataset.training.App.CONTROL_FOLDER_NAME;
import static org.audiveris.omrdataset.training.App.FEATURES_FOLDER_NAME;
import static org.audiveris.omrdataset.training.App.FILTERED_EXT;
import static org.audiveris.omrdataset.training.App.FILTERED_FOLDER_NAME;
import static org.audiveris.omrdataset.training.App.MAX_SYMBOL_SCALE;
import static org.audiveris.omrdataset.training.App.NONE_RATIO;
import static org.audiveris.omrdataset.training.App.PATCHES_FOLDER_NAME;
import static org.audiveris.omrdataset.training.App.TABLATURES_EXT;
import org.audiveris.omrdataset.training.Context.SourceType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
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
    /** Unique sheet id. */
    private final int sheetId;

    /** Path to sheet (SHEET.xml) annotations file. */
    private final Path infoPath;

    /** Path to sheet (SHEET.tablatures.xml) tablatures file, if any. */
    private final Path tablaturesPath;

    /** Path to sheet filtered (SHEET.filtered.xml) annotations file. */
    private final Path filteredPath;

    /** Sheet name radix (sans extension). */
    private final String radix;

    /** Sheet folder. */
    private final Path sheetFolder;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SheetProcessor} object.
     *
     * @param sheetId  sheet id (strictly positive)
     * @param infoPath path to sheet info file (.xml)
     */
    public SheetProcessor (int sheetId,
                           Path infoPath)
    {
        this.sheetId = sheetId;
        this.infoPath = infoPath;

        radix = Utils.sansExtension(infoPath.getFileName().toString());
        sheetFolder = infoPath.getParent();
        tablaturesPath = sheetFolder.resolve(radix + TABLATURES_EXT);
        filteredPath = sheetFolder.resolveSibling(FILTERED_FOLDER_NAME)
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

            // Run step #1 (source filtering)?
            //--------------------------------
            if (Main.cli.source != null) {
                watch.start("source");
                annotations = (Main.cli.source == SourceType.ZHAW)
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
                new Filter(sheetId, annotations, tablatures).process();

                // Save filtered annotations
                watch.start("save filtered");
                annotations.setSource(annotations.getSource() + " filtered");
                logger.info("{} Saving filtered annotations as {}", id(), filteredPath);
                annotations.marshall(filteredPath);
            }

            // From now on, we require the *filtered* annotations
            //
            // Run step #2 (nones)?
            //------------------------
            if (Main.cli.nones) {
                watch.start("nones");
                if (annotations == null) {
                    annotations = SheetAnnotations.unmarshal(filteredPath);
                }

                // Discard any none symbol already injected (potentially)
                removeNoneSymbols(annotations.getOuterSymbolsLiveList());

                // Augment annotations with none symbols
                int nb = (int) Math.rint(NONE_RATIO * annotations.getGoodSymbols().size());
                logger.info("{} Creating {} none symbols", id(), nb);
                annotations.getOuterSymbolsLiveList().addAll(
                        new NonesBuilder(sheetId, annotations).insertNones(nb));
                annotations.marshall(filteredPath);
            }

            // Histogram? (optional)
            if (Main.cli.histo) {
                watch.start("histo");
                if (annotations == null) {
                    annotations = SheetAnnotations.unmarshal(filteredPath);
                }

                // Print out shape histogram for each sheet
                new Histogram(sheetId, annotations).print();
            }

            // Control? (optional)
            if (Main.cli.control) {
                watch.start("control");
                if (annotations == null) {
                    annotations = SheetAnnotations.unmarshal(filteredPath);
                }

                // Generate control images for visual checking
                initialImg = getImage(getImagePath(infoPath, annotations.getSheetInfo()));
                Path controlPath = sheetFolder.resolveSibling(CONTROL_FOLDER_NAME)
                        .resolve(radix + CONTROL_EXT);
                new Control(sheetId, annotations).build(controlPath, initialImg);
            }

            // Run step #3 (features)?
            //------------------------
            if (Main.cli.features) {
                if (annotations == null) {
                    annotations = SheetAnnotations.unmarshal(filteredPath);
                }

                if (initialImg == null) {
                    initialImg = getImage(getImagePath(infoPath, annotations.getSheetInfo()));
                }

                // Extract features for each symbol definition in the sheet
                watch.start("features");
                final Path featuresPath = sheetFolder.resolveSibling(FEATURES_FOLDER_NAME)
                        .resolve(radix + App.FEATURES_EXT);
                new Features(sheetId, annotations).extract(initialImg, featuresPath);
            }

            // Patches? (optional)
            if (Main.cli.patches) {
                watch.start("patches");
                // Generate patch images for visual checking
                final Path featuresPath = sheetFolder.resolveSibling(FEATURES_FOLDER_NAME)
                        .resolve(radix + App.FEATURES_EXT);
                new Patches(sheetId,
                            featuresPath,
                            sheetFolder.resolveSibling(PATCHES_FOLDER_NAME).resolve(radix))
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
        return "sheetId:" + sheetId;
    }

    //-------//
    // scale //
    //-------//
    /**
     * Build a scaled version of an image.
     *
     * @param img   image to scale
     * @param ratio scaling ratio
     * @return the scaled image
     */
    public static BufferedImage scale (BufferedImage img,
                                       double ratio)
    {
        AffineTransform at = AffineTransform.getScaleInstance(ratio, ratio);
        AffineTransformOp atop = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        BufferedImage scaledImg = new BufferedImage(
                (int) Math.ceil(img.getWidth() * ratio),
                (int) Math.ceil(img.getHeight() * ratio),
                img.getType());

        return atop.filter(img, scaledImg);
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

        if (img.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            throw new RuntimeException("Wrong image type=" + img.getType() + " " + imgPath);
            // ZHAW png files use type TYPE_3BYTE_BGR instead of a gray image.
        }

        return img;
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

        final Path imgPath;
        if (Main.cli.source == SourceType.ZHAW) {
            Path imgDir = infoPath.getParent().resolveSibling("gray_images_png");
            imgPath = imgDir.resolve(fileName);
        } else {
            imgPath = infoPath.resolveSibling(fileName);
        }

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
