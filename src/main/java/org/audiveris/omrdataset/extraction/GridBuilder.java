//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      G r i d B u i l d e r                                     //
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

import org.audiveris.omr.util.FileUtil;
import org.audiveris.omrdataset.Main;
import org.audiveris.omrdataset.api.Patch.UPatch;
import static org.audiveris.omrdataset.api.Patch.UPatch.parseUPatch;
import org.audiveris.omrdataset.training.App;
import static org.audiveris.omrdataset.training.App.CSV_EXT;
import static org.audiveris.omrdataset.training.App.IMAGES_FORMAT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;

/**
 * Class {@code GridBuilder}
 *
 * @author Hervé Bitteur
 */
public class GridBuilder
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(GridBuilder.class);

    /** Number of columns: {@value}. */
    protected static final int GRID_COLUMNS = 20;

    /** Number of rows: {@value}. */
    protected static final int GRID_ROWS = 10;

    /** Border width: {@value}. */
    protected static final int BORDER_WIDTH = 10;

    /** Border height: {@value}. */
    protected static final int BORDER_HEIGHT = 20;

    /** Border color. */
    protected static final Color BORDER_COLOR = Color.GRAY;

    /** Text font for grid title. */
    protected static final Font TITLE_FONT = new Font("Sans Serif", Font.PLAIN, 16);

    /** Text font for each patch id. */
    protected static final Font PATCH_FONT = new Font("Sans Serif", Font.PLAIN, 10);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Zip source for patches. */
    protected final Path inPath;

    /** Target folder for grids. */
    protected final Path outFolder;

    protected final String radix;

    protected final int contextWidth = Main.context.getContextWidth();

    protected final int contextHeight = Main.context.getContextHeight();

    /** Total grid width. */
    protected final int GRID_WIDTH = BORDER_WIDTH + GRID_COLUMNS * (contextWidth + BORDER_WIDTH);

    /** Total grid height. */
    protected final int GRID_HEIGHT = BORDER_HEIGHT + GRID_ROWS * (contextHeight + BORDER_HEIGHT);

    //~ Constructors -------------------------------------------------------------------------------
    public GridBuilder (Path inPath,
                        Path outFolder)
    {
        this.inPath = inPath;
        this.outFolder = outFolder;

        radix = FileUtil.avoidExtensions(inPath, CSV_EXT).getFileName().toString();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    public void process ()
            throws Exception
    {
        final int indexMax = GRID_COLUMNS * GRID_ROWS - 1;

        final ZipInputStream zis = new ZipInputStream(Files.newInputStream(inPath));
        zis.getNextEntry();
        int gridId = 0;

        try (final BufferedReader br = new BufferedReader(new InputStreamReader(zis, UTF_8))) {
            BufferedImage grid = null; // Global image of several patches
            Graphics2D g = null;
            int index = -1; // Patch index
            String line;

            while ((line = br.readLine()) != null) {
                index++;
                final String[] cols = line.split(",");
                final UPatch uPatch = parseUPatch(cols, Main.context);

                if (grid == null) {
                    gridId++;
                    index = 0;
                    grid = new BufferedImage(GRID_WIDTH, GRID_HEIGHT, TYPE_4BYTE_ABGR);
                    g = grid.createGraphics();
                    g.setColor(BORDER_COLOR);
                    g.fillRect(0, 0, GRID_WIDTH, GRID_HEIGHT);

                    // ArchiveId + Shape + gridId as title
                    g.setFont(TITLE_FONT);
                    g.setColor(Color.YELLOW);
                    final String title = Main.uArchiveId + " " + radix + " #" + gridId;
                    g.drawString(title, BORDER_WIDTH / 2, (2 * BORDER_HEIGHT) / 3);

                    g.setFont(PATCH_FONT);
                }

                // Determine patch location within grid
                AffineTransform savedAT = g.getTransform();
                AffineTransform at = AffineTransform.getTranslateInstance(
                        BORDER_WIDTH + (index % GRID_COLUMNS) * (BORDER_WIDTH + contextWidth),
                        BORDER_HEIGHT + (index / GRID_COLUMNS)
                                                * (BORDER_HEIGHT + contextHeight));
                g.transform(at);

                // Draw patch
                uPatch.draw(g, true, true);

                // Write symbol id
                g.setColor(Color.YELLOW);
                String legend = uPatch.uSymbolId.toShortString();
                g.drawString(legend, 0, contextHeight + 8);

                // Hook for options
                drawOptions(g, cols, uPatch);

                // Restore location at origin
                g.setTransform(savedAT);

                if (index == indexMax) {
                    // Save to disk and reset data
                    saveGrid(grid, gridId);
                    index = -1;
                    grid = null;
                }
            }

            // Very last one, if started
            if (index != -1) {
                saveGrid(grid, gridId);
            }
        }

        logger.info("Completed {}", radix);

    }

    //----------//
    // saveGrid //
    //----------//
    protected void saveGrid (BufferedImage grid,
                             int gridId)
    {
        final Path folder = outFolder.resolve(radix);

        try {
            Files.createDirectories(folder);
            Path out = folder.resolve(gridId + App.IMAGE_EXT);
            ImageIO.write(grid, IMAGES_FORMAT, out.toFile());
            logger.debug("Grid {}", out.toAbsolutePath());
        } catch (IOException ex) {
            logger.warn("Error saving patch grid to {}", folder, ex);
        }
    }

    //-------------//
    // drawOptions //
    //-------------//
    protected void drawOptions (Graphics2D g,
                                String[] cols,
                                UPatch uPatch)
    {
        // Void by default
    }
    //~ Inner Classes ------------------------------------------------------------------------------
}
