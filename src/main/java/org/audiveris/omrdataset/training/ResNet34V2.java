//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       R e s N e t 3 4 V 2                                      //
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

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.CacheMode;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.distribution.TruncatedNormalDistribution;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInitDistribution;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.RmsProp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code ResNet34V2}
 *
 * @author Hervé Bitteur
 */
public class ResNet34V2
        extends ResNet18V2
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(ResNet34V2.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Factory for ResNet34V2 network instances.
     *
     * @param inputDepth  input number of channels (1 for gray, 3 for RGB)
     * @param inputWidth  input width
     * @param inputHeight input height
     * @param numClasses  number of classes to recognize
     */
    public ResNet34V2 (int inputDepth,
                       int inputWidth,
                       int inputHeight,
                       int numClasses)
    {
        super(inputDepth, inputWidth, inputHeight, numClasses);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * Create and initialize an instance of ResNet34V2 network.
     *
     * @return the initialized network
     */
    @Override
    public ComputationGraph create ()
    {
        // Define the graph configuration
        final WorkspaceMode workspaceMode = WorkspaceMode.ENABLED;
        final ComputationGraphConfiguration.GraphBuilder graph
                = new NeuralNetConfiguration.Builder()
                        .seed(1234)
                        .activation(Activation.IDENTITY)
                        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                        .updater(new RmsProp(0.1, 0.96, 0.001))
                        .weightInit(
                                new WeightInitDistribution(new TruncatedNormalDistribution(0.0, 0.5)))
                        .l1(1e-7)
                        .l2(5e-5)
                        .miniBatch(true)
                        .cacheMode(CacheMode.NONE)
                        .trainingWorkspaceMode(workspaceMode)
                        .inferenceWorkspaceMode(workspaceMode)
                        .cudnnAlgoMode(ConvolutionLayer.AlgoMode.PREFER_FASTEST)
                        .convolutionMode(ConvolutionMode.Truncate)
                        .graphBuilder();

        graph.setInputTypes(InputType.convolutionalFlat(inputHeight, inputWidth, inputDepth));
        graph.addInputs("resnet34_input");
        String last;

        // Stem (Stage 1)
        last = stemBlock(graph, "resnet34_input");

        // Stage 2
        last = firstIdentityBlock(graph, 64, "2", last); // Specific identity block
        last = identityBlock(graph, 64, "3", last);
        last = identityBlock(graph, 64, "4", last);

        // Stage 3
        last = convBlock(graph, 2, 128, "5", last);
        last = identityBlock(graph, 128, "6", last);
        last = identityBlock(graph, 128, "7", last);
        last = identityBlock(graph, 128, "8", last);

        // Stage 4
        last = convBlock(graph, 2, 256, "9", last);
        last = identityBlock(graph, 256, "10", last);
        last = identityBlock(graph, 256, "11", last);
        last = identityBlock(graph, 256, "12", last);
        last = identityBlock(graph, 256, "13", last);
        last = identityBlock(graph, 256, "14", last);

        // Stage 5
        last = convBlock(graph, 2, 512, "15", last);
        last = identityBlock(graph, 512, "16", last);
        last = identityBlock(graph, 512, "17", last);

        // Tail
        last = tailBlock(graph, last);

        graph.setOutputs(last);

        // Build the network with defined configuration
        final ComputationGraph network = new ComputationGraph(graph.build());
        network.init();

        return network;
    }

    public static void main (String[] args)
    {
        final int CONTEXT_DEPTH = 1;
        final int CONTEXT_WIDTH = 56;
        final int CONTEXT_HEIGHT = 112;
        final int NUM_CLASSES = 204;
        ComputationGraph network = new ResNet34V2(CONTEXT_DEPTH,
                                                  CONTEXT_WIDTH,
                                                  CONTEXT_HEIGHT,
                                                  NUM_CLASSES).create();

        System.out.println();
        System.out.println("*** ResNet34V2 ***");
        System.out.printf("CONTEXT_DEPTH:%d, CONTEXT_WIDTH:%d, CONTEXT_HEIGHT:%d, NUM_CLASSES:%d",
                          CONTEXT_DEPTH, CONTEXT_WIDTH, CONTEXT_HEIGHT, NUM_CLASSES);
        System.out.println();
        ///System.out.println(network.getConfiguration());
        InputType inputType = InputType.convolutionalFlat(CONTEXT_HEIGHT,
                                                          CONTEXT_WIDTH,
                                                          CONTEXT_DEPTH);
        System.out.println(network.summary(inputType));
        System.out.println(network.getConfiguration().getMemoryReport(inputType));
    }

    //~ Inner Classes ------------------------------------------------------------------------------
}
