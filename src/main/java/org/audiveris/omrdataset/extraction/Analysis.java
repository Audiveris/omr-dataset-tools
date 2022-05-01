//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         A n a l y s i s                                        //
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

import java.awt.Color;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.audiveris.omr.util.ChartPlotter;
import org.audiveris.omr.util.DoubleFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code Analysis}
 *
 * @author Hervé Bitteur
 */
public class Analysis
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(Analysis.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //~ Constructors -------------------------------------------------------------------------------
    //~ Methods ------------------------------------------------------------------------------------
    public static void main (String[] args)
            throws Exception
    {
        new Analysis().process(8363, 11157);
    }

    public void process (int xMin,
                         int xMax)
            throws Exception
    {
        DoubleFunction df = new DoubleFunction(xMin, xMax);
        Path dataPath = Paths.get(
                "D:\\soft\\audiveris-github\\omr-dataset-tools\\data\\material\\iterations.txt");
        InputStream is = Files.newInputStream(dataPath);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;

            while ((line = br.readLine()) != null) {
                final String[] cols = line.split(" ");
                final int iter = Integer.parseInt(cols[0]);
                final double score = Double.parseDouble(cols[1]);
                df.addValue(iter, score);
            }
        }

        int argMax = df.argMax(xMin, xMax);
        logger.info("argMax:{} {}", argMax, df.getValue(argMax));

        ChartPlotter plotter = new ChartPlotter("Training", "Iteration", "Score");
        plotter.add(df.getDerivativeSeries(), Color.BLUE);
        plotter.add(df.getValueSeries(), Color.RED);
        plotter.add(df.getZeroSeries(), Color.WHITE);

        plotter.display(new Point(0, 0));

    }

    //~ Inner Classes ------------------------------------------------------------------------------
}
