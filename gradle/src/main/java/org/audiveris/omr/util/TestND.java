//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           T e s t N D                                          //
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
package org.audiveris.omr.util;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code TestND}
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
    public static void main (String[] args)
            throws Exception
    {
        logger.info("TestND start");

        INDArray a = Nd4j.linspace(1, 24, 24).reshape(4, 6);
        System.out.println("shape");
        System.out.println(a.shape());
        System.out.println("a");
        System.out.println(a);
//        System.out.println(a);

        INDArray b = a.get(NDArrayIndex.interval(1, 3), NDArrayIndex.interval(1, 4));
        System.out.println("b before");
        System.out.println(b);

        a.divi(2.0);

        System.out.println("b after");
        System.out.println(b);

        logger.info("TestND stop");
    }

    //~ Inner Classes ------------------------------------------------------------------------------
}
