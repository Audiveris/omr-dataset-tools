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

import org.audiveris.omrdataset.api.OmrShape;
import org.audiveris.omrdataset.api.SheetAnnotations;
import org.audiveris.omrdataset.api.SymbolInfo;
import static org.audiveris.omrdataset.training.Context.INTERLINE;
import static org.audiveris.omrdataset.training.App.NONE_X_MARGIN;
import static org.audiveris.omrdataset.training.App.NONE_Y_MARGIN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class {@code NonesBuilder} generates None-shape symbols within a sheet.
 * <p>
 * We try to insert None-shape symbols at random locations in the sheet, provided that ordinate of
 * location center is within a valid symbol vertical range and the none-shape symbol does not
 * intersect another symbol rectangle.
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
    private final int sheetId;

    /** Annotations for this sheet. */
    private final SheetAnnotations annotations;

    /** We need the same interline value for the whole page. */
    private Integer roundedInterline;

    /** List of filled boxes, kept sorted on x. */
    private final List<Rectangle> filledBoxes = new ArrayList<>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code NoneSymbols} object.
     *
     * @param sheetId     id of containing sheet
     * @param annotations Annotations for the page
     */
    public NonesBuilder (int sheetId,
                         SheetAnnotations annotations)
    {
        this.sheetId = sheetId;
        this.annotations = annotations;
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
        if (!checkInterlineValue()) {
            logger.info("sheetId:{} Page has several interline values, "
                                + "no None symbol can be inserted.", sheetId);

            return Collections.emptyList();
        }

        // Adjust margin to sheet interline value
        final double ratio = (double) INTERLINE / roundedInterline;
        final int xMargin = (int) Math.rint(NONE_X_MARGIN / ratio);
        final int yMargin = (int) Math.rint(NONE_Y_MARGIN / ratio);

        int maxWidth = fillWithSymbols();
        maxWidth = Math.max(maxWidth, 2 * xMargin); // Safer

        // Created None symbols
        final List<SymbolInfo> createdSymbols = new ArrayList<>();
        final int sheetWidth = annotations.getSheetInfo().dim.width;
        final int sheetHeight = annotations.getSheetInfo().dim.height;

        // Ordinates occupied by standard symbols
        final boolean[] occupiedYs = getOccupiedYs(sheetHeight);

        // Put a reasonable limit on creation attempts
        for (int i = 50 * toAdd; i >= 0; i--) {
            if (createdSymbols.size() >= toAdd) {
                break; // Normal exit
            }

            // Make sure we pick a y within some valid symbol vertical range
            final int y = (int) Math.rint(Math.random() * sheetHeight);

            if ((y >= 0) && (y < sheetHeight) && occupiedYs[y]) {
                final int x = (int) Math.rint(Math.random() * sheetWidth);
                final Rectangle rect = new Rectangle(x, y, 0, 0);
                rect.grow(xMargin, yMargin);

                if (tryInsertion(rect, maxWidth)) {
                    createdSymbols.add(
                            new SymbolInfo(
                                    OmrShape.none,
                                    roundedInterline,
                                    annotations.nextSymbolId(),
                                    null,
                                    new Rectangle(x, y, 0, 0)));
                }
            }
        }

        logger.info("sheetId:{} Created: {}", sheetId, createdSymbols.size());
        return createdSymbols;
    }

    /**
     * Check this page contains a single interline value.
     *
     * @return true if OK
     */
    private boolean checkInterlineValue ()
    {
        for (SymbolInfo symbol : annotations.getGoodSymbols()) {
            if (roundedInterline == null) {
                if (symbol.getInterline() > 0) {
                    roundedInterline = (int) Math.rint(symbol.getInterline());
                }
            } else if (!roundedInterline.equals((int) Math.rint(symbol.getInterline()))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Populate the "filledBoxes" collection with the boxes of all good symbols.
     *
     * @return the maximum width across all symbols
     */
    private int fillWithSymbols ()
    {
        int maxWidth = 0;

        for (SymbolInfo symbol : annotations.getGoodSymbols()) {
            Rectangle r = symbol.getBounds().getBounds();
            maxWidth = Math.max(maxWidth, r.width);
            filledBoxes.add(r);
        }

        Collections.sort(filledBoxes, byAbscissa);

        return maxWidth;
    }

    /**
     * Return which ordinate values are occupied by a good symbol
     *
     * @param height page height
     * @return table of booleans indexed by y
     */
    private boolean[] getOccupiedYs (int height)
    {
        boolean[] occupied = new boolean[height];
        Arrays.fill(occupied, false);

        for (SymbolInfo symbol : annotations.getGoodSymbols()) {
            Rectangle r = symbol.getBounds().getBounds();

            for (int y = r.y; y < (r.y + r.height); y++) {
                if (y >= 0 && y < height) {
                    occupied[y] = true;
                }
            }
        }

        return occupied;
    }

    /**
     * Insert the provided rectangle if this does not result in a collision with any
     * existing rectangle (valid symbols plus already inserted artificial rectangles).
     *
     * @param rect     (input) the rectangle to insert
     * @param maxWidth (input) maximum symbol width
     * @return
     */
    private boolean tryInsertion (Rectangle rect,
                                  int maxWidth)
    {
        // Check we are far enough from valid symbol and artificial rectangle
        int index = checkFarEnough(rect, maxWidth);

        if (index == -1) {
            return false;
        }

        if (Math.random() < 0.9) {
            // Check we are close enough to a valid symbol
            Rectangle larger = new Rectangle(rect);
            larger.grow(rect.width / 2, rect.height / 2);

            if (!checkCloseEnough(larger, maxWidth)) {
                return false;
            }
        }

        // OK, insert rectangle at proper index
        filledBoxes.add(index, rect);
        logger.debug("Added None at {}", rect);

        return true;
    }

    private int checkFarEnough (Rectangle rect,
                                int maxWidth)
    {
        final int size = filledBoxes.size();
        final int xMax = (rect.x + rect.width) - 1;
        final int xMin = (rect.x - maxWidth) + 1;

        // Theoretical insertion index in the sorted list
        final int result = Collections.binarySearch(filledBoxes, rect, byAbscissa);
        final int index = (result >= 0) ? result : (-(result + 1));

        // Check for collisions on right
        for (int i = index; i < size; i++) {
            Rectangle r = filledBoxes.get(i);

            if (r.x > xMax) {
                break;
            } else if (r.intersects(rect)) {
                return -1;
            }
        }

        // Check for collisions on left
        for (int i = index - 1; i >= 0; i--) {
            Rectangle r = filledBoxes.get(i);

            if (r.x < xMin) {
                break;
            } else if (r.intersects(rect)) {
                return -1;
            }
        }

        return index;
    }

    private boolean checkCloseEnough (Rectangle rect,
                                      int maxWidth)
    {
        final int size = filledBoxes.size();
        final int xMax = (rect.x + rect.width) - 1;
        final int xMin = (rect.x - maxWidth) + 1;

        // Theoretical insertion index in the sorted list
        final int result = Collections.binarySearch(filledBoxes, rect, byAbscissa);
        final int index = (result >= 0) ? result : (-(result + 1));

        // Check for collisions on right
        for (int i = index; i < size; i++) {
            Rectangle r = filledBoxes.get(i);

            if (r.x > xMax) {
                break;
            } else if (r.intersects(rect)) {
                return true;
            }
        }

        // Check for collisions on left
        for (int i = index - 1; i >= 0; i--) {
            Rectangle r = filledBoxes.get(i);

            if (r.x < xMin) {
                break;
            } else if (r.intersects(rect)) {
                return true;
            }
        }

        return false;
    }
}
