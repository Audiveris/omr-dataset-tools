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
import org.audiveris.omr.util.PathListOptionHandler;
import org.audiveris.omrdataset.api.Context.ContextType;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.StopOptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
    @Option(name = "-help",
            help = true,
            usage = "Display general help then stop")
    public boolean help;

    /** ContextType. */
    @Option(name = "-context",
            required = true,
            usage = "(mandatory) Specify which context kind to use")
    public ContextType contextType;

    /** 1/ Source. */
    @Option(name = "-filter",
            usage = "Step 1: Load and filter symbols")
    public boolean filter;

    /** 2/ Nones. */
    @Option(name = "-nones",
            usage = "Step 2: Generate none symbols"
                            + "\n   (effective for control & features)")
    public boolean nones;

    /** 3/ Features. */
    @Option(name = "-features",
            usage = "Step 3: Generate sheet .csv.zip files")
    public boolean features;

    /** 4/ Tally. */
    @Option(name = "-tally",
            usage = "Step 4: Dispatch features by shape")
    public boolean tally;

    /** 4b/ shape Grids. */
    @Option(name = "-shapeGrids",
            usage = "Step 4.b: Build patch grids by shape")
    public boolean shapeGrids;

    /** 4c/ specific Grids. */
    @Option(name = "-grids",
            usage = "Step 4.c: Build patch grids on selected inputs",
            handler = PathListOptionHandler.class)
    public ArrayList<Path> grids;

    /** 5/ Bins. */
    @Option(name = "-bins",
            usage = "Step 5: Split shape tally files into bins")
    public boolean bins;

    /** 6/ Shuffle. */
    @Option(name = "-shuffle",
            usage = "Step 6: Shuffle each bin in memory")
    public boolean shuffle;

    /** 7/ Train. */
    @Option(name = "-train",
            usage = "Step 7: Train model on selected bins",
            handler = IntArrayOptionHandler.class)
    public ArrayList<Integer> train;

    /** Test. */
    @Option(name = "-testPath",
            usage = "Evaluate model on the provided features file",
            metaVar = "XXX.csv.zip")
    public Path testPath;

    /** Parallel processing. */
    @Option(name = "-parallel",
            usage = "(recommended) Use parallel processing"
                            + "\n   (effective on steps 1 to 5)")
    public boolean parallel;

    /** Shape names. */
    @Option(name = "-names",
            usage = "(optional) Print context shapes names with their index")
    public boolean names;

    /** Sheet histogram of shapes. */
    @Option(name = "-histo",
            usage = "(optional) Print shape histogram per sheet")
    public boolean histo;

    /** Bin histogram of shapes. */
    @Option(name = "-binhisto",
            usage = "(optional) Print shape histogram of selected bins",
            handler = IntArrayOptionHandler.class)
    public ArrayList<Integer> binHisto;

    /** Control images. */
    @Option(name = "-control",
            usage = "(optional) Generate control image of each sheet")
    public boolean control;

    /** Patch images. */
    @Option(name = "-patches", usage = "(optional) Generate patch images")
    public boolean patches;

    /** The range of iterations to inspect. */
    @Option(name = "-inspect",
            usage = "(optional) Inspect a bin for a range of iterations",
            handler = IntArrayOptionHandler.class)
    public ArrayList<Integer> inspect;

    /** Target directory for output data. */
    @Option(name = "-output",
            usage = "(optional) Define output directory"
                            + "\n   (defaults to \"data/output\")",
            metaVar = "<folder>")
    public Path outputFolder;

    /** Alternate target file for network model. */
    @Option(name = "-model",
            usage = "(optional) Define path to model"
                            + "\n   (defaults to \"<output>/training/patch-classifier.zip\")",
            metaVar = "<.zip file>")
    public Path modelPath;

    /** Final arguments, with optional "--" separator. */
    @Argument
    @Option(name = "--",
            handler = StopOptionHandler.class)
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

        final CLI cli = new CLI();

        final Comparator<OptionHandler> noSsorter = new Comparator<>()
        {
            @Override
            public int compare (OptionHandler o1,
                                OptionHandler o2)
            {
                return 0;
            }
        };

        final ParserProperties props = ParserProperties.defaults()
                .withAtSyntax(true)
                .withUsageWidth(100)
                .withShowDefaults(false)
                .withOptionSorter(noSsorter);

        final CmdLineParser parser = new CmdLineParser(cli, props);

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
        buf.append("\n   [OPTIONS] -- [INPUT_ANNOTATION_FILES and/or INPUT_FOLDERS]\n");

        buf.append("\n@file:");
        buf.append("\n Content to be expanded in line");
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
