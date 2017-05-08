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
package org.omrdataset;

import static org.omrdataset.App.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class {@code NonesBuilder} generates None-shape symbols within a page.
 * <p>
 * We try to insert None-shape symbols at random locations in the page, provided that ordinate of
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
    /** Annotations for this page. */
    private final PageAnnotations annotations;

    /** We need the same interline value for the whole page. */
    private Integer interline;

    /** List of filled boxes, kept sorted on x. */
    private final List<Rectangle> filledBoxes = new ArrayList<Rectangle>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code NoneSymbols} object.
     *
     * @param annotations Annotations for the page
     */
    public NonesBuilder (PageAnnotations annotations)
    {
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
            logger.info("Page has several interline values, no None symbol can be inserted.");

            return Collections.emptyList();
        }

        int maxWidth = fillWithSymbols();
        maxWidth = Math.max(maxWidth, 2 * NONE_X_MARGIN); // Safer

        // Created None symbols
        final List<SymbolInfo> createdSymbols = new ArrayList<SymbolInfo>();
        final int pageWidth = annotations.getPageInfo().dim.width;
        final int pageHeight = annotations.getPageInfo().dim.height;

        // Ordinates occupied by valid symbols
        final boolean[] occupiedYs = getOccupiedYs(pageHeight);

        // Put a reasonable limit on creation attempts
        for (int i = 10 * toAdd; i >= 0; i--) {
            if (createdSymbols.size() >= toAdd) {
                break; // Normal exit
            }

            // Make sure we pick a y within some valid symbol vertical range
            final int y = (int) Math.rint(Math.random() * pageHeight);

            if ((y >= 0) && (y < pageHeight) && occupiedYs[y]) {
                final int x = (int) Math.rint(Math.random() * pageWidth);
                final Rectangle rect = new Rectangle(x, y, 0, 0);
                rect.grow(NONE_X_MARGIN, NONE_Y_MARGIN);

                if (tryInsertion(rect, maxWidth)) {
                    createdSymbols.add(
                            new SymbolInfo(OmrShape.none, interline, new Rectangle(x, y, 0, 0)));
                }
            }
        }

        return createdSymbols;
    }

    /**
     * Check this page contains a single interline value.
     *
     * @return true if OK
     */
    private boolean checkInterlineValue ()
    {
        for (SymbolInfo symbol : annotations.getSymbols()) {
            if ((interline == null) && (symbol.interline != 0)) {
                interline = symbol.interline;
            } else if (interline != symbol.interline) {
                return false;
            }
        }

        return true;
    }

    /**
     * Populate the "filledBoxes" collection with the boxes of all valid symbols.
     *
     * @return the maximum width across all symbols
     */
    private int fillWithSymbols ()
    {
        int maxWidth = 0;

        for (SymbolInfo symbol : annotations.getSymbols()) {
            Rectangle r = symbol.bounds.getBounds();
            maxWidth = Math.max(maxWidth, r.width);
            filledBoxes.add(r);
        }

        Collections.sort(filledBoxes, byAbscissa);

        return maxWidth;
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
                return false;
            }
        }

        // Check for collisions on left
        for (int i = index - 1; i >= 0; i--) {
            Rectangle r = filledBoxes.get(i);

            if (r.x < xMin) {
                break;
            } else if (r.intersects(rect)) {
                return false;
            }
        }

        // No collision found, insert rectangle at proper index
        filledBoxes.add(index, rect);
        logger.debug("Added None at {}", rect);

        return true;
    }

    /**
     * Return which ordinate values are occupied by a valid symbol
     *
     * @param height page height
     * @return table of booleans indexed by y
     */
    private boolean[] getOccupiedYs (int height)
    {
        boolean[] occupied = new boolean[height];
        Arrays.fill(occupied, false);

        for (SymbolInfo symbol : annotations.getSymbols()) {
            Rectangle r = symbol.bounds.getBounds();

            for (int y = r.y; y < (r.y + r.height); y++) {
                occupied[y] = true;
            }
        }

        return occupied;
    }
}
