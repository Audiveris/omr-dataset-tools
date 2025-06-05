//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              O m r C o m p u t a t i o n G r a p h                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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

import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.graph.ComputationGraph;

/**
 * Class <code>OmrComputationGraph</code> is a named ComputationGraph.
 *
 * @author Hervé Bitteur
 */
public class OmrComputationGraph
        extends ComputationGraph
{
    //~ Static fields/initializers -----------------------------------------------------------------
    //~ Instance fields ----------------------------------------------------------------------------

    /** A name assigned to this graph. */
    public final String name;

    //~ Constructors -------------------------------------------------------------------------------
    public OmrComputationGraph (String name,
                                ComputationGraphConfiguration configuration)
    {
        super(configuration);
        this.name = name;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String toString ()
    {
        return name + " - " + super.toString();
    }
}
