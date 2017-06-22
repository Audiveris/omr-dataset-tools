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

    private static final Logger logger = LoggerFactory.getLogger(
            CLI.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Help mode. */
    @Option(name = "-help", help = true, usage = "Displays general help then stops")
    public boolean help = false;

    /** Features. */
    @Option(name = "-features", usage = "Generates .csv and .dat files")
    public boolean features;

    /** Clean. */
    @Option(name = "-clean", usage = "Cleans up output")
    public boolean clean;

    /** Control images. */
    @Option(name = "-controls", usage = "Generates control images")
    public boolean controls;

    /** Sub-images. */
    @Option(name = "-subimages", usage = "Generates subimages")
    public boolean subimages;

    /** Names. */
    @Option(name = "-names", usage = "Prints all possible symbol names")
    public boolean names;

    /** Nones. */
    @Option(name = "-nones", usage = "Generates none symbols")
    public boolean nones;

    /** Training. */
    @Option(name = "-training", usage = "Trains classifier on features")
    public boolean training;

    /** Mistakes. */
    @Option(name = "-mistakes", usage = "Save mistake images")
    public boolean mistakes;

    /** Target directory for output data. */
    @Option(name = "-output", usage = "Defines output directory", metaVar = "<folder>")
    public Path outputFolder;

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

        buf.append("\nOptions:\n");

        StringWriter writer = new StringWriter();
        parser.printUsage(writer, null);
        buf.append(writer.toString());

        buf.append("\nInput file extensions:");
        buf.append("\n .xml: annotations file");
        buf.append("\n");

        buf.append("\n@file:");
        buf.append("\n content to be extended in line");
        buf.append("\n");
        logger.info(buf.toString());
    }
}
