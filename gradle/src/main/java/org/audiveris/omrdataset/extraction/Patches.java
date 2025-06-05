//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          P a t c h e s                                         //
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

import org.audiveris.omr.util.ZipWrapper;
import org.audiveris.omrdataset.Main;
import org.audiveris.omrdataset.api.OmrShape;
import org.audiveris.omrdataset.api.Patch.UPatch;
import static org.audiveris.omrdataset.api.Patch.UPatch.parseUPatch;
import org.audiveris.omrdataset.extraction.SourceInfo.USheetId;
import static org.audiveris.omrdataset.training.App.IMAGES_EXT;
import static org.audiveris.omrdataset.training.App.IMAGES_FORMAT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/**
 * Class {@code Patches} takes a sheet features .csv file as input and regenerates patches
 * for visual checking.
 *
 * @author Hervé Bitteur
 */
public class Patches
{
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Patches.class);

    private static final int numClasses = OmrShape.unknown.ordinal();

    //~ Instance fields ----------------------------------------------------------------------------
    private final Path featuresPath;

    private final Path patchFolder;

    /** Sheet id, if any. */
    private USheetId uSheetId;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Patches builder for a sheet.
     *
     * @param uSheetId     id of containing sheet (for information purpose only)
     * @param featuresPath features .csv.zip input file
     * @param patchFolder  dedicated patches folder
     */
    public Patches (USheetId uSheetId,
                    Path featuresPath,
                    Path patchFolder)
    {
        this.uSheetId = uSheetId;
        this.featuresPath = featuresPath;
        this.patchFolder = patchFolder;
        logger.info("{} Generating patches in folder {}", uSheetId, patchFolder);
    }

    public Patches (Path featuresPath,
                    Path patchFolder)
    {
        this.featuresPath = featuresPath;
        this.patchFolder = patchFolder;

        logger.info("Generating patches in folder {}", patchFolder);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Process the features data to generate one patch per features row.
     *
     * @throws Exception in case of IO problem or interruption
     */
    public void process ()
            throws Exception
    {
        final ZipWrapper zin = ZipWrapper.open(featuresPath);

        try (final BufferedReader br = zin.newBufferedReader()) {
            String line;

            while ((line = br.readLine()) != null) {
                final UPatch uPatch = parseUPatch(line, Main.context);

                // Build image name prefix
                final Rectangle2D box = uPatch.box;
                final StringBuilder sb = new StringBuilder();
                sb.append(uPatch.uSymbolId).append("(")
                        .append("x").append(box.getX())
                        .append("y").append(box.getY())
                        .append("w").append(box.getWidth())
                        .append("h").append(box.getHeight())
                        .append(")");

                final Enum label = uPatch.label;
                savePatch(buildPatch(uPatch), sb.toString(), label);
            }
        }

        zin.close();
    }

    //------------//
    // buildPatch //
    //------------//
    /**
     * Build the patch image that corresponds to the provided patch.
     *
     * @param uPatch the patch to process
     * @return the bufferedImage
     */
    public static BufferedImage buildPatch (UPatch uPatch)
    {
        BufferedImage colorImg = new BufferedImage(
                Main.context.getContextWidth(),
                Main.context.getContextHeight(),
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = colorImg.createGraphics();
        uPatch.draw(g, true, true);
        g.dispose();

        return colorImg;
    }

    //-----------//
    // savePatch //
    //-----------//
    /**
     * Save the patch to disk.
     *
     * @param img    the patch image
     * @param prefix a unique prefix for file name
     * @param label  the OMR shape
     */
    private void savePatch (BufferedImage img,
                            String prefix,
                            Enum label)
    {
        try {
            Files.createDirectories(patchFolder);
            Path out = patchFolder.resolve(prefix + "-" + label + IMAGES_EXT);
            ImageIO.write(img, IMAGES_FORMAT, out.toFile());
            logger.debug("Patch {}", out.toAbsolutePath());
        } catch (IOException ex) {
            logger.warn("Error saving patch to {}", patchFolder, ex);
        }
    }
}
