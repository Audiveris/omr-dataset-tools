//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            N o r m s                                           //
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
package org.audiveris.omrdataset.math;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * Class {@code Norms} encapsulates the means and standard deviations of one or several
 * variables.
 * <p>
 * NOTA: Methods {@link #load} and {@link #store} are provided for completeness but Norms are not
 * additive.
 * A better practice is to load and store Populations which are additive and from which Norms are
 * derived by {@link Populations#toNorms} method.
 *
 * @author Hervé Bitteur
 */
public class Norms
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Norms.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** One row per variable, each row with 2 columns (mean, std). */
    private final INDArray vars;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code Norms} object.
     *
     * @param vars the underlying array
     */
    public Norms (INDArray vars)
    {
        this.vars = vars;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Report the mean value for provided variable index.
     *
     * @param index variable index
     * @return mean value
     */
    public double getMean (int index)
    {
        return vars.getDouble(index, 0);
    }

    /**
     * Report the standard deviation value for provided variable index.
     *
     * @param index variable index
     * @return standard deviation value
     */
    public double getStd (int index)
    {
        return vars.getDouble(index, 1);
    }

    /**
     * Load Norms data from the provided input file.
     *
     * @param root     the root path to file system
     * @param fileName file/entry name
     * @return the loaded Norms instance, or exception is thrown
     * @throws IOException in case of IO problem
     */
    public static Norms load (Path root,
                              String fileName)
            throws IOException
    {
        INDArray vars = null;

        final Path path = root.resolve(fileName);

        if (path != null) {
            InputStream is = Files.newInputStream(path); // READ by default
            DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
            vars = Nd4j.read(dis);
            dis.close();
        }

        if (vars != null) {
            logger.debug("{} read from {}", vars, path);

            return new Norms(vars);
        }

        throw new IllegalStateException("Norms were not found in " + fileName);
    }

    /**
     * Store Norms data to the provided output file.
     *
     * @param root     path to root of file system
     * @param fileName file/entry name
     * @throws IOException in case of IO problem
     */
    public void store (Path root,
                       String fileName)
            throws IOException
    {
        {
            Path path = root.resolve(fileName);
            DataOutputStream dos = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(path, CREATE)));
            Nd4j.write(vars, dos);
            dos.flush();
            dos.close();
            logger.debug("\n{} stored into {}", vars, path);
        }
    }

    @Override
    public String toString ()
    {
        return vars.toString();
    }
}
