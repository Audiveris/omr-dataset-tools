//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          C o n t r o l                                         //
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

import org.audiveris.omrdataset.Main;
import org.audiveris.omrdataset.api.OmrShape;
import org.audiveris.omrdataset.api.SheetAnnotations;
import org.audiveris.omrdataset.api.SymbolInfo;
import org.audiveris.omrdataset.extraction.SourceInfo.USheetId;
import static org.audiveris.omrdataset.api.Context.INTERLINE;
import static org.audiveris.omrdataset.training.App.NONE_X_MARGIN;
import static org.audiveris.omrdataset.training.App.NONE_Y_MARGIN;
import static org.audiveris.omrdataset.training.App.IMAGES_FORMAT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Class {@code Control} builds the control image of a sheet.
 * <p>
 * Over the initial image, paint excluded areas, draw symbols boxes and None symbols locations.
 *
 * @author Hervé Bitteur
 */
public class Control
{

    private static final Logger logger = LoggerFactory.getLogger(Control.class);

    private static final Color AREA_COLOR = new Color(255, 255, 0, 25);

    private final USheetId uSheetId;

    private final SheetAnnotations annotations;

    public Control (USheetId uSheetId,
                    SheetAnnotations annotations)
    {
        this.uSheetId = uSheetId;
        this.annotations = annotations;
    }

    /**
     * Build the control image.
     *
     * @param controlsPath output path for control image
     * @param initialImg   background image
     */
    public void build (Path controlsPath,
                       BufferedImage initialImg)
    {
        try {
            logger.info("{} Generating control image {}", uSheetId, controlsPath);
            BufferedImage ctrl = new BufferedImage(
                    initialImg.getWidth(),
                    initialImg.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = ctrl.createGraphics();
            g.drawImage(initialImg, null, null);

            paintExcludedAreas(g);
            drawSymbols(annotations.getOuterSymbolsLiveList(), g);

            g.dispose();
            Files.createDirectories(controlsPath.getParent());
            ImageIO.write(ctrl, IMAGES_FORMAT, controlsPath.toFile());
        } catch (IOException ex) {
            logger.warn("{} Error drawing boxes to {}", uSheetId, controlsPath, ex);
        }
    }

    private void paintExcludedAreas (Graphics g)
    {
        List<Rectangle> areas = annotations.getSheetInfo().excludedAreas;
        if (areas.isEmpty()) {
            return;
        }

        g.setColor(AREA_COLOR);

        for (Rectangle a : areas) {
            g.fillRect(a.x, a.y, a.width, a.height);
        }
    }

    //-------------//
    // drawSymbols //
    //-------------//
    /**
     * Draw the boxes for the provided symbols (and recursively their inner symbols)
     *
     * @param symbols the collection of symbols to process
     * @param g       the graphic output
     */
    private void drawSymbols (List<SymbolInfo> symbols,
                              Graphics2D g)
    {
        for (SymbolInfo symbol : symbols) {
            logger.debug("{}", symbol);

            // Inner symbols?
            List<SymbolInfo> innerSymbols = symbol.getInnerSymbols();

            if (!innerSymbols.isEmpty()) {
                drawSymbols(innerSymbols, g);
            }

            Rectangle2D box = symbol.getBounds();

            if (symbol.getOmrShape() == OmrShape.none) {
                // None symbol, draw a red cross
                double ratio = (double) INTERLINE / symbol.getInterline();
                int xMargin = (int) Math.rint(NONE_X_MARGIN / ratio);
                int yMargin = (int) Math.rint(NONE_Y_MARGIN / ratio);
                Rectangle b = box.getBounds();
                g.setColor(Color.RED);
                g.drawLine(b.x, b.y - yMargin, b.x, b.y + yMargin);
                g.drawLine(b.x - xMargin, b.y, b.x + xMargin, b.y);
            } else {
                // Draw outer rectangle, with line stroke of 1 pixel
                Rectangle2D b = new Rectangle2D.Double(
                        box.getX() - 1,
                        box.getY() - 1,
                        box.getWidth() + 1,
                        box.getHeight() + 1);

                if (Main.context.ignores(symbol.getOmrShape())) {
                    g.setColor(Color.BLUE);
                } else if (symbol.isInvalid()) {
                    g.setColor(Color.ORANGE);
                } else {
                    g.setColor(Color.GREEN);
                }

                g.draw(b);
            }
        }
    }
}
