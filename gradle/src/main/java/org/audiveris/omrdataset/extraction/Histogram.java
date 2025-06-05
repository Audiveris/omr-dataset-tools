//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        H i s t o g r a m                                       //
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
import org.audiveris.omrdataset.api.OmrShapes;
import org.audiveris.omrdataset.api.SheetAnnotations;
import org.audiveris.omrdataset.api.SymbolInfo;
import org.audiveris.omrdataset.extraction.SourceInfo.USheetId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Class {@code Histogram} handles the histogram of shapes for a given sheet.
 *
 * @author Hervé Bitteur
 */
public class Histogram
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Histogram.class);
    //~ Instance fields ----------------------------------------------------------------------------

    private final USheetId uSheetId;

    private final SheetAnnotations annotations;

    /** Histogram of shapes in sheet. */
    private final Map<OmrShape, Integer> histo = new EnumMap<>(OmrShape.class);

    //~ Constructors -------------------------------------------------------------------------------
    public Histogram (USheetId uSheetId,
                      SheetAnnotations annotations)
    {
        this.uSheetId = uSheetId;
        this.annotations = annotations;

        populate(annotations.getGoodSymbols());
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Print the proportion for each shape present in current sheet.
     */
    public void print ()
    {
        // Total
        int total = 0;
        for (Integer i : histo.values()) {
            total += i;
        }

        StringBuilder sb = new StringBuilder();
        // Non-empty buckets
        for (Map.Entry<OmrShape, Integer> entry : histo.entrySet()) {
            int count = entry.getValue();
            double ratio = count / (double) total;
            sb.append(String.format("%n%4d %.3f ", count, ratio)).append(entry.getKey());
        }

        sb.append(String.format("%n%4d 100.0", total));

        logger.info("{} histogram:{}", uSheetId, sb);
    }

    /**
     * Recursive building of shape histogram.
     *
     * @param symbols collection of symbols to analyze
     */
    private void populate (List<SymbolInfo> symbols)
    {
        final boolean leaves = true;

        for (SymbolInfo symbol : symbols) {
            final OmrShape symbolShape = symbol.getOmrShape();

            if (symbolShape == null) {
                logger.info("{} Skipping null shape {}", uSheetId, symbol);
                continue;
            }

            if (Main.context.ignores(symbolShape)) {
                continue;
            }

            logger.debug("{}", symbol);

            // Inner symbols?
            if (leaves) {
                List<SymbolInfo> innerSymbols = symbol.getInnerSymbols();

                if (!innerSymbols.isEmpty()) {
                    // Recursive processing of inner symbols
                    populate(innerSymbols);

                    if (!OmrShapes.TIME_COMBOS.contains(symbolShape)) {
                        continue;
                    }
                }
            }

            Integer count = histo.get(symbolShape);
            if (count == null) {
                histo.put(symbolShape, 1);
            } else {
                histo.put(symbolShape, count + 1);
            }
        }
    }
}
