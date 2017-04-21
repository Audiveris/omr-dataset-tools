//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    P a g e P r o c e s s o r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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

import static org.omrdataset.App.CONTEXT_HEIGHT;
import static org.omrdataset.App.CONTEXT_WIDTH;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.io.PrintWriter;

/**
 * Class {@code PageProcessor}
 *
 * @author Hervé Bitteur
 */
public class PageProcessor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PageProcessor.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final int pageWidth;

    private final int pageHeight;

    private final byte[] bytes;

    private final PageAnnotations pageInfo;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PageProcessor} object.
     *
     * @param bytes    DOCUMENT ME!
     * @param pageInfo DOCUMENT ME!
     */
    public PageProcessor (int pageWidth,
                          int pageHeight,
                          byte[] bytes,
                          PageAnnotations pageInfo)
    {
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.bytes = bytes;
        this.pageInfo = pageInfo;
    }

    //~ Methods ------------------------------------------------------------------------------------
    public void process (PrintWriter out)
            throws Exception
    {
        // Process each symbol definition
        for (SymbolInfo symbol : pageInfo.getSymbols()) {
            logger.info("{}", symbol);

            Rectangle box = symbol.bounds;

            // Symbol center
            int sCenterX = box.x + box.width / 2;
            int sCenterY = box.y + box.height / 2;

            // Top-left corner of context
            int left = sCenterX - CONTEXT_WIDTH / 2;
            int top = sCenterY - CONTEXT_HEIGHT / 2;

            // extract bytes from sub-image, beware of image borders
            // Layout: column by column (and not row by row!)
            for (int y = 0; y < CONTEXT_HEIGHT; y++) {
                int ay = top + y;

                for (int x = 0; x < CONTEXT_WIDTH; x++) {
                    int ax = left + x;
                    out.print(bytes[(ay * pageWidth) + ax] & 0xff);
                    out.print(",");
                }
            }

            // Add (OMR) shape index
            try {
                out.print(symbol.omrShape.ordinal());
            } catch (Exception ex) {
                logger.error("Missing shape {}", symbol);
                throw new RuntimeException("Missing shape for " + symbol);
            }
            out.println();
        }
    }
}
