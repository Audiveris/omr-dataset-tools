//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P o p u l a t i o n s                                     //
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
import java.util.Arrays;

/**
 * Class {@code Populations} encapsulates data from sample values of one or several
 * variables.
 * <p>
 * Populations can be added thanks to {@link #includePopulations} method.
 * <p>
 * Mean and standard deviation can be computed for all variables via {@link #toNorms} method.
 *
 * @author Hervé Bitteur
 */
public class Populations
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Populations.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** One row per variable, each row with 3 columns (n, s, s2). */
    private final INDArray vars;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code Populations} object of desired rows
     *
     * @param rows desired number of rows
     */
    public Populations (int rows)
    {
        vars = Nd4j.zeros(rows, 3);
    }

    /**
     * Creates a new {@code Populations} object from populated vars
     *
     * @param vars the underlying array
     */
    public Populations (INDArray vars)
    {
        this.vars = vars;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Get the number of cumulated measurements for provided index
     *
     * @param index variable index
     * @return this number
     */
    public int getCardinality (int index)
    {
        final INDArray row = vars.getRow(index);

        return (int) row.getDouble(0);
    }

    /**
     * Include a whole Populations instance to this one
     *
     * @param other the other Populations to include
     */
    public void includePopulations (Populations other)
    {
        // Check compatibility
        if (!Arrays.equals(vars.shape(), other.vars.shape())) {
            throw new IllegalArgumentException("Non compatible Populations");
        }

        vars.addi(other.vars);
    }

    /**
     * Add a measurement for variable of provided index
     *
     * @param index variable index
     * @param val   the measure value
     */
    public void includeValue (int index,
                              double val)
    {
        final INDArray row = vars.getRow(index);

        row.putScalar(0, row.getDouble(0) + 1); //n += 1;
        row.putScalar(1, row.getDouble(1) + val); //s += val;
        row.putScalar(2, row.getDouble(2) + (val * val)); //s2 += (val * val);
    }

    /**
     * Load Populations data from the provided input file.
     *
     * @param path path to input file
     * @return the loaded Populations instance, or exception is thrown
     * @throws IOException in case of IO problem
     */
    public static Populations load (Path path)
            throws IOException
    {
        INDArray vars = null;

        if (path != null) {
            InputStream is = Files.newInputStream(path); // READ by default
            DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
            vars = Nd4j.read(dis);
            dis.close();
        }

        if (vars != null) {
            logger.info("{} read from {}", vars, path);

            return new Populations(vars);
        }

        throw new IllegalStateException("Populations were not found in " + path);
    }

    /**
     * Store the Populations data to the provided output file.
     *
     * @param path path to output file
     * @throws IOException in case of IO problem
     */
    public void store (Path path)
            throws IOException
    {
        {
            DataOutputStream dos = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(path, CREATE)));
            Nd4j.write(vars, dos);
            dos.flush();
            dos.close();
            logger.info("\n{} stored into {}", vars, path);
        }
    }

    /**
     * Derive Norms from this Populations instance
     *
     * @return the derived norms
     */
    public Norms toNorms ()
    {
        final int rowNb = vars.rows();
        final INDArray ms = Nd4j.zeros(rowNb, 2); // For mean & std

        for (int ir = 0; ir < rowNb; ir++) {
            INDArray msRow = ms.getRow(ir);
            INDArray varsRow = vars.getRow(ir);
            double n = varsRow.getDouble(0);
            double s = varsRow.getDouble(1);
            double s2 = varsRow.getDouble(2);

            if (n > 0) {
                // Mean
                msRow.putScalar(0, s / n);

                // Standard deviation (unbiased)
                double variance = (n == 1) ? 0 : Math.max(0, ((s2 - ((s * s) / n)) / (n - 1)));
                msRow.putScalar(1, Math.sqrt(variance) + Nd4j.EPS_THRESHOLD);
            } else {
                msRow.putScalar(1, Nd4j.EPS_THRESHOLD); // Safer
            }
        }

        return new Norms(ms);
    }

    @Override
    public String toString ()
    {
        return vars.toString();
    }
}
