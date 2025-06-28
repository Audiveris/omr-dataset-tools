//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           T e s t N D                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omrdataset.infer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

/**
 * Class <code>TestND</code>
 *
 * @author Hervé Bitteur
 */
public class TestND
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TestND.class);

    //~ Instance fields ----------------------------------------------------------------------------

    //~ Constructors -------------------------------------------------------------------------------

    //~ Methods ------------------------------------------------------------------------------------

    public void test ()
    {
        int nRows = 7;
        int nCols = 3;
        INDArray table = Nd4j.zeros(nRows, nCols);
        logger.info("table shape: {}", table.shape());

        int col = 0;
        table.putScalar(new int[] { 0, col }, 10);
        table.putScalar(new int[] { 1, col }, 11);
        table.putScalar(new int[] { 2, col }, 12);
        table.putScalar(new int[] { 3, col }, 13);
        table.putScalar(new int[] { 4, col }, 14);
        table.putScalar(new int[] { 5, col }, 15);
        table.putScalar(new int[] { 6, col }, 16);

        col = 1;
        table.putScalar(new int[] { 0, col }, 20);
        table.putScalar(new int[] { 1, col }, 21);
        table.putScalar(new int[] { 2, col }, 22);
        table.putScalar(new int[] { 3, col }, 23);
        table.putScalar(new int[] { 4, col }, 24);
        table.putScalar(new int[] { 5, col }, 25);
        table.putScalar(new int[] { 6, col }, 26);

        col = 2;
        table.putScalar(new int[] { 0, col }, 30);
        table.putScalar(new int[] { 1, col }, 31);
        table.putScalar(new int[] { 2, col }, 32);
        table.putScalar(new int[] { 3, col }, 33);
        table.putScalar(new int[] { 4, col }, 34);
        table.putScalar(new int[] { 5, col }, 35);
        table.putScalar(new int[] { 6, col }, 36);
        logger.info("table: \n{}", table);

        final INDArray values0 = table.get(NDArrayIndex.point(0), NDArrayIndex.all());
        logger.info("values0.shape: {}", values0.shape());
        logger.info("values0: {}", values0);

        final INDArray values1 = table.get(NDArrayIndex.point(1), NDArrayIndex.all());
        logger.info("values1.shape: {}", values1.shape());
        logger.info("values1: {}", values1);

        final INDArray values2 = table.get(NDArrayIndex.point(2), NDArrayIndex.all());
        logger.info("values2.shape: {}", values2.shape());
        logger.info("values2: {}", values2);

        // Gets the 3rd row
        INDArray rowSlice = table.get(NDArrayIndex.point(2));
        logger.info("rowSlice.shape: {}", rowSlice.shape());
        logger.info("rowSlice: {}", rowSlice);

        // Gets all rows in 2nd column
        INDArray columnSlice = table.get(NDArrayIndex.all(), NDArrayIndex.point(1));
        logger.info("columnSlice.shape: {}", columnSlice.shape());
        logger.info("columnSlice: {}", columnSlice);

        INDArray advancedRowSlice = table.get(NDArrayIndex.point(1), NDArrayIndex.interval(0, 2));
        logger.info("advancedRowSlice.shape: {}", advancedRowSlice.shape());
        logger.info("advancedRowSlice: {}", advancedRowSlice);

        INDArray advancedColSlice = table.get(
                NDArrayIndex.all(),
                NDArrayIndex.point(1),
                NDArrayIndex.interval(4, 7));
        logger.info("advancedColSlice.shape: {}", advancedColSlice.shape());
        logger.info("advancedColSlice: {}", advancedColSlice);

    }
    //~ Static Methods -----------------------------------------------------------------------------

    //~ Inner Classes ------------------------------------------------------------------------------

    public void main (String... args)
    {
        new TestND().test();
    }
}
