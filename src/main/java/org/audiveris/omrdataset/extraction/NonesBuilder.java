//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     N o n e s B u i l d e r                                    //
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

import org.audiveris.omrdataset.Main;
import org.audiveris.omrdataset.api.OmrShape;
import org.audiveris.omrdataset.api.SheetAnnotations;
import org.audiveris.omrdataset.api.SymbolInfo;
import org.audiveris.omrdataset.extraction.SourceInfo.USheetId;
import static org.audiveris.omrdataset.api.Context.INTERLINE;
import static org.audiveris.omrdataset.training.App.CORE_RATIO;
import static org.audiveris.omrdataset.training.App.NONE_CLOSE_RATIO;
import static org.audiveris.omrdataset.training.App.NONE_FAR_RATIO;
import static org.audiveris.omrdataset.training.App.NONE_LOCATIONS_RATIO;
import static org.audiveris.omrdataset.training.App.NONE_SHAPES_RATIO;
import static org.audiveris.omrdataset.training.App.NONE_X_MARGIN;
import static org.audiveris.omrdataset.training.App.NONE_Y_MARGIN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Class {@code NonesBuilder} generates None-shape symbols within a sheet.
 * <p>
 * We try to insert None-shape symbols at random locations in the sheet, making sure that
 * every none symbol is:
 * <ul>
 * <li>Close enough to one standard (good) symbol,
 * <li>Far enough from any other symbol, standard or none.
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class NonesBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(NonesBuilder.class);

    /** To sort rectangles on their abscissa value. */
    private static final Comparator<Rectangle> byAbscissa = new Comparator<Rectangle>()
    {
        @Override
        public int compare (Rectangle r1,
                            Rectangle r2)
        {
            return Integer.compare(r1.x, r2.x);
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sheet id. */
    private final USheetId uSheetId;

    /** Annotations for this sheet. */
    private final SheetAnnotations annotations;

    /** We need the same interline value for the whole page. */
    private final Integer roundedInterline;

    /** List of good symbols boxes, sorted on x. */
    private final List<Rectangle> goodBoxes = new ArrayList<>();

    /** List of core boxes, good symbols and none symbols so far, kept sorted on x. */
    private final List<Rectangle> coreBoxes = new ArrayList<>();

    /** Maximum width among all good symbols in sheet. */
    private int maxWidth;

    /** Horizontal margin around a none location. */
    private int xMargin;

    /** Vertical margin around a none location. */
    private int yMargin;

    /** None symbols created so far. */
    private final List<SymbolInfo> createdSymbols = new ArrayList<>();

    private final Path nonesPath;

    private final Random random = new Random();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code NoneSymbols} object.
     *
     * @param uSheetId    id of containing sheet
     * @param annotations Annotations for the page
     * @param nonesPath   path to potential nones locations
     */
    public NonesBuilder (USheetId uSheetId,
                         SheetAnnotations annotations,
                         Path nonesPath)
    {
        this.uSheetId = uSheetId;
        this.annotations = annotations;
        this.nonesPath = nonesPath;

        roundedInterline = getInterlineValue();
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Insert some None symbols in the page.
     *
     * @param toAdd desired number of None symbols to add
     * @return the list of inserted (None) symbols
     */
    public List<SymbolInfo> insertNones (int toAdd)
    {
        if (roundedInterline == null) {
            return Collections.emptyList();
        }

        // Adjust margin to sheet interline value
        final double ratio = (double) INTERLINE / roundedInterline;
        xMargin = (int) Math.rint(NONE_X_MARGIN / ratio);
        yMargin = (int) Math.rint(NONE_Y_MARGIN / ratio);

        maxWidth = Math.max(fillWithSymbols(), 2 * xMargin); // Safer

        // 1/ Close to good symbols
        final int targetClose = (int) Math.rint(toAdd * NONE_CLOSE_RATIO);
        if (targetClose > 0) {
            insertClose(targetClose);
        }

        // 2/ A few nones, far enough
        final int targetFar = (int) Math.rint(toAdd * NONE_FAR_RATIO);
        if (targetFar > 0) {
            insertFar(targetFar);
        }

        // 3/ Specific shapes, if any
        final int targetShapes = (int) Math.rint(toAdd * NONE_SHAPES_RATIO);
        if (targetShapes > 0) {
            insertSpecificShapes(targetShapes);
        }

        // 4/ Specific locations if any
        final int targetLocations = (int) Math.rint(toAdd * NONE_LOCATIONS_RATIO);
        if (targetLocations > 0) {
            insertSpecificLocations(targetLocations);
        }

        logger.info("{} nones: {}", uSheetId, createdSymbols.size());
        return createdSymbols;
    }

    /**
     * Check this sheet contains a single interline value.
     *
     * @return the sheet single interline value or null
     */
    private Integer getInterlineValue ()
    {
        Integer interline = null;

        for (SymbolInfo symbol : annotations.getGoodSymbols()) {
            if (interline == null) {
                if (symbol.getInterline() > 0) {
                    interline = (int) Math.rint(symbol.getInterline());
                }
            } else if (!interline.equals((int) Math.rint(symbol.getInterline()))) {
                logger.info("{} Sheet has several interline values, "
                                    + "no None symbol can be inserted.", uSheetId);
                return null;
            }
        }

        if (interline == null) {
            logger.info("{} No good symbols found for {} context", uSheetId, Main.context);
        }

        return interline;
    }

    /**
     * Populate the "filledBoxes" collection with the boxes of all good symbols, while
     * measuring the maximum width among all these good symbols.
     *
     * @return the maximum width across all good symbols
     */
    private int fillWithSymbols ()
    {
        int maxW = 0;

        for (SymbolInfo symbol : annotations.getGoodSymbols()) {
            // We shrink a bit the symbol, to keep just the core of it.
            final Rectangle2D r = symbol.getBounds();
            maxW = Math.max(maxW, (int) Math.ceil(r.getWidth()));

            final double w = r.getWidth() * CORE_RATIO;
            final double h = r.getHeight() * CORE_RATIO;
            r.setRect(r.getCenterX() - w / 2, r.getCenterY() - h / 2, w, h);
            coreBoxes.add(r.getBounds());
        }

        Collections.sort(coreBoxes, byAbscissa);

        goodBoxes.addAll(coreBoxes);

        return maxW;
    }

    /**
     * Insert the provided rectangle if this does not result in a collision with any
     * existing rectangle (good symbols plus already inserted none rectangles).
     *
     * @param rect (input) the rectangle to insert
     * @return true if insertion was successful
     */
    private void tryInsertion (Rectangle rect)
    {
        // Check we are far enough from any symbol or none rectangle
        int index = checkFarEnough(rect);

        if (index == -1) {
            return;
        }

        // Insert rectangle at proper index
        coreBoxes.add(index, rect);

        createdSymbols.add(
                new SymbolInfo(
                        OmrShape.none,
                        roundedInterline,
                        annotations.nextSymbolId(),
                        null,
                        new Rectangle(rect.x + rect.width / 2, rect.y + rect.height / 2, 0, 0)));

        logger.debug("Added None at {}", rect);
    }

    /**
     * Check if the proposed rectangle is far enough from any other symbol (good or none).
     *
     * @param rect the proposed rectangle
     * @return insertion index in list of rectangles (ordered by abscissa).
     *         If index == -1, insertion cannot be made.
     */
    private int checkFarEnough (Rectangle rect)
    {
        final int size = coreBoxes.size();
        final int xMax = (rect.x + rect.width) - 1;
        final int xMin = (rect.x - maxWidth) + 1;

        // Theoretical insertion index in the sorted list
        final int result = Collections.binarySearch(coreBoxes, rect, byAbscissa);
        final int index = (result >= 0) ? result : (-(result + 1));

        // Check for collisions on right
        for (int i = index; i < size; i++) {
            Rectangle r = coreBoxes.get(i);

            if (r.x > xMax) {
                break;
            } else if (r.intersects(rect)) {
                return -1;
            }
        }

        // Check for collisions on left
        for (int i = index - 1; i >= 0; i--) {
            Rectangle r = coreBoxes.get(i);

            if (r.x < xMin) {
                break;
            } else if (r.intersects(rect)) {
                return -1;
            }
        }

        return index;
    }

    private void insertClose (int target)
    {
        logger.debug("targetClose: {}", target);
        final int total = target + createdSymbols.size();

        for (int i = 0; i < 10 * target; i++) {
            logger.debug("i: {}", i);
            final List<Rectangle> goodTogo = new ArrayList<>(goodBoxes);

            // Choose randomly a good box, among the ones not yet processed
            while (!goodTogo.isEmpty()) {
                final int idx = random.nextInt(goodTogo.size());
                final Rectangle good = goodTogo.get(idx);
                goodTogo.remove(idx);

                final Rectangle fatGood = new Rectangle(good);
                fatGood.grow(3 * xMargin, 3 * yMargin);

                // Search a location in box vicinity
                final int x = fatGood.x + xMargin + random.nextInt(fatGood.width - 2 * xMargin);
                final int y = fatGood.y + yMargin + random.nextInt(fatGood.height - 2 * yMargin);
                final Rectangle rect = new Rectangle(x, y, 0, 0);
                rect.grow(xMargin, yMargin);
                tryInsertion(rect);

                if (createdSymbols.size() >= total) {
                    return;
                }
            }
        }
    }

    private void insertFar (int target)
    {
        logger.debug("targetFar: {}", target);
        final int total = createdSymbols.size() + target;

        final int sheetWidth = annotations.getSheetInfo().dim.width;
        final int sheetHeight = annotations.getSheetInfo().dim.height;

        if (target > 0) {
            for (int j = 0; j < 10 * target; j++) {

                final int x = random.nextInt(sheetWidth); // Random X
                final int y = random.nextInt(sheetHeight); // Random Y
                final Rectangle rect = new Rectangle(x, y, 0, 0);
                rect.grow(xMargin, yMargin);
                tryInsertion(rect);

                if (createdSymbols.size() >= total) {
                    return;
                }
            }
        }
    }

    private void insertSpecificShapes (int target)
    {
        logger.debug("targetShapes: {}", target);
        final int total = createdSymbols.size() + target;
        final List<SymbolInfo> shapes = Main.context.getNoneShapes(annotations);

        if (!shapes.isEmpty()) {
            for (int k = 0; k < 10 * target; k++) {
                if (!shapes.isEmpty()) {
                    int index = random.nextInt(shapes.size());
                    SymbolInfo symbol = shapes.get(index);
                    shapes.remove(index);
                    tryInsertion(symbol.getBounds().getBounds());

                    if (createdSymbols.size() >= total) {
                        return;
                    }
                }
            }
        }
    }

    private void insertSpecificLocations (int target)
    {
        logger.debug("targetLocations: {}", target);
        final int total = createdSymbols.size() + target;
        final List<Point> locations = Main.context.getNoneLocations(nonesPath);

        if (!locations.isEmpty()) {
            for (int k = 0; k < 10 * target; k++) {
                if (!locations.isEmpty()) {
                    final int index = random.nextInt(locations.size());
                    final Point point = locations.get(index);
                    locations.remove(index);

                    final Rectangle rect = new Rectangle(point);
                    rect.grow(xMargin, yMargin);
                    tryInsertion(rect);

                    if (createdSymbols.size() >= total) {
                        return;
                    }
                }
            }
        }
    }
}
