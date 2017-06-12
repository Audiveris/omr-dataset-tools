//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          J o u r n a l                                         //
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
package org.audiveris.omrdataset.train;

import org.audiveris.omrdataset.api.OmrShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code Journal} handles the journal parallel to features, to provide easy use
 * of features meta-data.
 *
 * @author Hervé Bitteur
 */
public class Journal
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Journal.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final List<Record> records = new ArrayList<Record>();

    private final SheetIndex sheetIndex = new SheetIndex();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code Journal} object.
     */
    public Journal ()
    {
        load(App.JOURNAL_PATH);
    }

    //~ Methods ------------------------------------------------------------------------------------
    public Record getRecord (int line)
    {
        return records.get(line - 1);
    }

    private void load (Path path)
    {
        BufferedReader br = null;

        try {
            String line;
            br = new BufferedReader(new FileReader(path.toString()));
            br.readLine(); // Skip header line

            while ((line = br.readLine()) != null) {
                String[] f = line.split(",");
                records.add(
                        new Record(
                                Integer.decode(f[0]), // row
                                Integer.decode(f[1]), // sheetId
                                Integer.decode(f[2]), // symbolId
                                Integer.decode(f[3]), // interline
                                Integer.decode(f[4]), // x
                                Integer.decode(f[5]), // y
                                Integer.decode(f[6]), // w
                                Integer.decode(f[7]), // h
                                Integer.decode(f[8]))); // shapeId
            }

            logger.info("Journal records: {}", records.size());
        } catch (Exception ex) {
            logger.warn("Error loading {}", path, ex);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    logger.warn("Could not close {}", path, ex);
                }
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    public class Record
    {
        //~ Instance fields ------------------------------------------------------------------------

        // # row, sheetId, symbolId, interline, x, y, w, h, shapeId
        final int row;

        final int sheetId;

        final int symbolId;

        final int interline;

        final int x;

        final int y;

        final int w;

        final int h;

        final int shapeId;

        //~ Constructors ---------------------------------------------------------------------------
        public Record (int row,
                       int sheetId,
                       int symbolId,
                       int interline,
                       int x,
                       int y,
                       int w,
                       int h,
                       int shapeId)
        {
            this.row = row;
            this.sheetId = sheetId;
            this.symbolId = symbolId;
            this.interline = interline;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.shapeId = shapeId;
        }

        //~ Methods --------------------------------------------------------------------------------
        public OmrShape getOmrShape ()
        {
            return OmrShape.values()[shapeId];
        }

        public String getSheetName ()
        {
            return sheetIndex.getSheetName(sheetId);
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("Record{");
            sb.append("row:").append(row);
            sb.append(" sheet:").append(getSheetName());
            sb.append(" symbolId:").append(symbolId);
            sb.append(" interline:").append(interline);
            sb.append(" x:").append(x);
            sb.append(" y:").append(y);
            sb.append(" w:").append(w);
            sb.append(" h:").append(h);
            sb.append(" ").append(getOmrShape());

            sb.append("}");

            return sb.toString();
        }
    }

    //------------//
    // SheetIndex //
    //------------//
    private static class SheetIndex
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Map<Integer, String> map = new TreeMap<Integer, String>();

        //~ Constructors ---------------------------------------------------------------------------
        public SheetIndex ()
        {
            load(App.SHEETS_PATH);
        }

        //~ Methods --------------------------------------------------------------------------------
        public String getSheetName (int id)
        {
            return map.get(id);
        }

        private void load (Path path)
        {
            BufferedReader br = null;

            try {
                String line;
                br = new BufferedReader(new FileReader(path.toString()));
                br.readLine(); // Skip header line

                while ((line = br.readLine()) != null) {
                    String[] f = line.split(",");
                    map.put(Integer.decode(f[0]), f[1]);
                }

                logger.info("SheetIndex sheets: {}", map.size());
            } catch (Exception ex) {
                logger.warn("Error loading {}", path, ex);
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException ex) {
                        logger.warn("Could not close {}", path, ex);
                    }
                }
            }
        }
    }
}
