//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                P i x e l P r e P r o c e s s o r                               //
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
package org.audiveris.omrdataset.training;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;

/**
 * Class {@code PixelPreProcessor} normalizes pixel data from range 0..255 to range 0..1.
 *
 * @author Hervé Bitteur
 */
public class PixelPreProcessor
        implements DataSetPreProcessor
{

    //~ Constructors -------------------------------------------------------------------------------
    public PixelPreProcessor ()
    {

    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void preProcess (DataSet toPreProcess)
    {
        INDArray theFeatures = toPreProcess.getFeatures();
        preProcess(theFeatures);
    }

    public void preProcess (INDArray theFeatures)
    {
        theFeatures.divi(255.0);
    }
}
