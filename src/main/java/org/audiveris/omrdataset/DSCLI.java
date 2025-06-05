//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            D S C L I                                           //
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
package org.audiveris.omrdataset;

import org.audiveris.omr.util.IntArrayOptionHandler;
import org.audiveris.omrdataset.api.GeneralShape;

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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Class <code>DSCLI</code> parses and hold the command line parameters for DeepScores V2.
 *
 * @author Hervé Bitteur
 */
public class DSCLI
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DSCLI.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Help mode. */
    @Option(name = "-help",
            help = true,
            usage = "Display general help then stop")
    public boolean help;

    /** Data-set. */
    @Option(name = "-dataset",
            usage = "Define path to chosen dataset, dense or complete")
    public Path dataset;

    /** Categories. */
    @Option(name = "-categories",
            usage = "Generate categories.json file")
    public boolean gen_categories = false;

    /** Image map. */
    @Option(name = "-images",
            usage = "Generate image map of all complete archives")
    public boolean gen_image_map = false;

    /** Balanced. */
    @Option(name = "-balanced",
            usage = "Use balanced training set")
    public boolean balanced = false;

    /** Keras. */
    @Option(name = "-keras",
            usage = "Use Keras unmarshalling, mandatory for Python-created network")
    public boolean useKeras = false;

    /** Archives. */
    @Option(name = "-archives",
            usage = "Selected archives within complete dataset",
            handler = IntArrayOptionHandler.class)
    public ArrayList<Integer> archives = new ArrayList<>();

    /** Model Architecture. */
    @Option(name = "-archi",
            usage = "Chosen model architecture")
    public DSMain.Archi archi = DSMain.Archi.MobileNetV2;

    /** Nones. */
    @Option(name = "-nones",
            usage = "Generate none samples for the selected complete archives",
            handler = IntArrayOptionHandler.class)
    public ArrayList<Integer> nones = new ArrayList<>();

    /** Dense Nones. */
    @Option(name = "-dense_nones",
            usage = "Generate none samples for the dense dataset")
    public boolean dense_nones = false;

    /** Tallies. */
    @Option(name = "-tallies",
            usage = "Generate shape tallies on the selected complete archives",
            handler = IntArrayOptionHandler.class)
    public ArrayList<Integer> tallies = new ArrayList<>();

    /** Samples. */
    @Option(name = "-samples",
            usage = "Select image samples on the selected complete archives",
            handler = IntArrayOptionHandler.class)
    public ArrayList<Integer> samples = new ArrayList<>();

    /** Train. */
    @Option(name = "-train",
            usage = "Train model on the selected complete archives",
            handler = IntArrayOptionHandler.class)
    public ArrayList<Integer> train = new ArrayList<>();

    /** Dense train. */
    @Option(name = "-dense_train",
            usage = "Train model on the (unbalanced) dense dataset",
            handler = IntArrayOptionHandler.class)
    public ArrayList<Integer> dense_train = new ArrayList<>();

    /** Min Image index. */
    @Option(name = "-min_index",
            usage = "Define a minimum on image index")
    public int minIndex = 0;

    /** Max Image index. */
    @Option(name = "-max_index",
            usage = "Define a maximum on image index")
    public int maxIndex = 1_000_000;

    /** Epochs. */
    @Option(name = "-epochs",
            usage = "Define number of training epochs")
    public int epochs = 1;

    /** Mini-batch. */
    @Option(name = "-minibatch",
            usage = "Define size of training mini-batch")
    public int minibatch = 50;

    /** Test. */
    @Option(name = "-test",
            usage = "Test model on the selected images within the test dictionary",
            handler = IntArrayOptionHandler.class)
    public ArrayList<Integer> test = new ArrayList<>();

    /** Dense train. */
    @Option(name = "-dense_test",
            usage = "Test model on the (unbalanced) dense dataset",
            handler = IntArrayOptionHandler.class)
    public ArrayList<Integer> dense_test = new ArrayList<>();

    /** Target directory for output data. */
    @Option(name = "-output",
            usage = "Define output directory",
            metaVar = "<folder>")
    public Path outputFolder = Paths.get("data/output");

    /** Shapes for debugging. */
    @Option(name = "-shapes",
            usage = "Selected shapes for visual check")
    public ArrayList<GeneralShape> shapes = new ArrayList<>();

    /** Final arguments, with optional "--" separator. */
    @Argument
    @Option(name = "--",
            handler = StopOptionHandler.class)
    public List<Path> arguments = new ArrayList<Path>();

    //~ Constructors -------------------------------------------------------------------------------
    private DSCLI ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * Parse the CLI arguments and return the populated parameters structure.
     *
     * @param args the CLI arguments
     * @return the parsed parameters, or null if failed
     * @throws org.kohsuke.args4j.CmdLineException if command line is malformed
     */
    public static DSCLI create (final String... args)
            throws CmdLineException
    {
        logger.info("Args: {}", Arrays.toString(args));

        final DSCLI cli = new DSCLI();
        final Comparator<OptionHandler> noSorter = (OptionHandler o1, OptionHandler o2) -> 0;
        final ParserProperties props = ParserProperties.defaults()
                .withAtSyntax(true)
                .withUsageWidth(100)
                .withShowDefaults(true)
                .withOptionSorter(noSorter);
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

    //------------//
    // printUsage //
    //------------//
    /**
     * Print out the general syntax for the command line.
     */
    private static void printUsage (CmdLineParser parser)
    {
        StringBuilder buf = new StringBuilder();

        buf.append("\n");
        buf.append("\nSyntax:");
        buf.append("\n   [OPTIONS]");

        buf.append("\n@file:");
        buf.append("\n Content to be expanded in line");
        buf.append("\n");

        buf.append("\nOptions:\n");

        StringWriter writer = new StringWriter();
        parser.printUsage(writer, null);
        buf.append(writer.toString());

        buf.append("\n");
        logger.info(buf.toString());
    }
}
