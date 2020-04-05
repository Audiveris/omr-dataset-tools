//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                              C L I                                             //
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
package org.audiveris.omrdataset;

import org.audiveris.omr.util.IntArrayOptionHandler;
import org.audiveris.omrdataset.training.Context.SourceType;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StopOptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class {@code CLI} parses and holds the command line parameters.
 *
 * @author Hervé Bitteur
 */
public class CLI
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(CLI.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Help mode. */
    @Option(name = "-help", help = true, usage = "Display general help then stop")
    public boolean help;

    /** 1/ Source. */
    @Option(name = "-filter", usage = "Step 1: Load and filter symbols according to source type")
    public SourceType source;

    /** 2/ Nones. */
    @Option(name = "-nones", usage
            = "Step 2: Generate none symbols (effective for control & features)")
    public boolean nones;

    /** 3/ Features. */
    @Option(name = "-features", usage = "Step 3: Generate sheet .csv (zipped) files")
    public boolean features;

    /** 4/ Split. */
    @Option(name = "-split", usage = "Step 4: Split all sheet .csv files into few csv bins")
    public boolean split;

    /** 5/ Shuffle. */
    @Option(name = "-shuffle", usage = "Step 5: Shuffle each csv bin in memory")
    public boolean shuffle;

    /** 6/ Train. */
    @Option(name = "-train", usage = "Step 6: Train model on selected bins",
            handler = IntArrayOptionHandler.class)
    public ArrayList<Integer> train;

    /** 7/ Test. */
    @Option(name = "-test", usage = "Step 7: Evaluate model from test csv bins")
    public boolean test;

    /** Sheet histogram of shapes. */
    @Option(name = "-histo", usage = "(optional) Print shape histogram per sheet")
    public boolean histo;

    /** Bin histogram of shapes. */
    @Option(name = "-binhisto", usage = "(optional) Print shape histogram for provided bins",
            handler = IntArrayOptionHandler.class)
    public ArrayList<Integer> binHisto;

    /** Control images. */
    @Option(name = "-control", usage = "(optional) Generate control images")
    public boolean control;

    /** Patch images. */
    @Option(name = "-patches", usage = "(optional) Generate patch images")
    public boolean patches;

    /** Shape names. */
    @Option(name = "-names", usage = "(optional) Print all possible shape names with their index")
    public boolean names;

    /** Parallel processing. */
    @Option(name = "-parallel", usage
            = "(recommanded) Use parallel processing (effective on steps 1 to 4)")
    public boolean parallel;

    /** Limit. */
    @Option(name = "-limit", usage = "(Deprecated) Limit samples per shape")
    public boolean limit;

    /** Target directory for output data. */
    @Option(name = "-output", usage = "(optional) Define output directory", metaVar = "<folder>")
    public Path outputFolder;

    /** Alternate target file for network model. */
    @Option(name = "-model", usage = "(optional) Define path to model", metaVar = "<.zip file>")
    public Path modelPath;

    /** The range of iterations to inspect. */
    @Option(name = "-inspect", usage = "(optional) Inspect a bin for a range of iterations",
            handler = IntArrayOptionHandler.class)
    public ArrayList<Integer> inspect;

    /** Final arguments, with optional "--" separator. */
    @Argument
    @Option(name = "--", handler = StopOptionHandler.class)
    public List<Path> arguments = new ArrayList<Path>();

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Parse the CLI arguments and return the populated parameters structure.
     *
     * @param args the CLI arguments
     * @return the parsed parameters, or null if failed
     * @throws org.kohsuke.args4j.CmdLineException if cmd line is malformed
     */
    public static CLI create (final String... args)
            throws CmdLineException
    {
        logger.info("CLI args: {}", Arrays.toString(args));

        CLI cli = new CLI();

        final CmdLineParser parser = new CmdLineParser(cli);

        parser.parseArgument(args);

        if (args.length == 0) {
            cli.help = true;
        }

        if (cli.help) {
            printUsage(parser);
        }

        return cli;
    }

    /**
     * Print out the general syntax for the command line.
     */
    private static void printUsage (CmdLineParser parser)
    {
        StringBuilder buf = new StringBuilder();

        buf.append("\n");
        buf.append("\nSyntax:");
        buf.append("\n   [OPTIONS] -- [INPUT_FILES]\n");

        buf.append("\n@file:");
        buf.append("\n Content to be extended in line");
        buf.append("\n");

        buf.append("\nOptions:\n");

        StringWriter writer = new StringWriter();
        parser.printUsage(writer, null);
        buf.append(writer.toString());

        buf.append("\nInput file extensions:");
        buf.append("\n .xml: annotations file");
        buf.append("\n");
        logger.info(buf.toString());
    }
}
