//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       R e s N e t 1 8 V 2                                      //
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
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration.GraphBuilder;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.distribution.TruncatedNormalDistribution;
import org.deeplearning4j.nn.conf.graph.ElementWiseVertex;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ActivationLayer;
import org.deeplearning4j.nn.conf.layers.BatchNormalization;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInitDistribution;

import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code ResNet18V2} implements ResNet18 network, using V2 architecture.
 * <p>
 * Loosely derived from ResNet50 code found in model zoo of DL4J.
 * Refined according to Python implementation of Maxim Poliakovski.
 *
 * @author Hervé Bitteur
 */
public class ResNet18V2
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ResNet18V2.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final int inputDepth;

    private final int inputWidth;

    private final int inputHeight;

    private final int numClasses;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Factory for ResNet18V2 network instances.
     *
     * @param inputDepth  input number of channels (1 for gray, 3 for RGB)
     * @param inputWidth  input width
     * @param inputHeight input height
     * @param numClasses  number of classes to recognize
     */
    public ResNet18V2 (int inputDepth,
                       int inputWidth,
                       int inputHeight,
                       int numClasses)
    {
        this.inputDepth = inputDepth;
        this.inputWidth = inputWidth;
        this.inputHeight = inputHeight;
        this.numClasses = numClasses;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * Create and initialize an instance of ResNet18V2 network.
     *
     * @return the initialized network
     */
    public ComputationGraph create ()
    {
        // Define the graph configuration
        final WorkspaceMode workspaceMode = WorkspaceMode.ENABLED;
        final GraphBuilder graph
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
        graph.addInputs("resnet18_input");
        String last;

        // Stem (Stage 1)
        last = stemBlock(graph, "resnet18_input");

        // Stage 2
        last = firstIdentityBlock(graph, 64, "2", last); // Specific identity block
        last = identityBlock(graph, 64, "3", last);

        // Stage 3
        last = reductionBlock(graph, 2, 128, "4", last);
        last = identityBlock(graph, 128, "5", last);

        // Stage 4
        last = reductionBlock(graph, 2, 256, "6", last);
        last = identityBlock(graph, 256, "7", last);

        // Stage 5
        last = reductionBlock(graph, 2, 512, "8", last);
        last = identityBlock(graph, 512, "9", last);

        // Tail
        last = tailBlock(graph, last);

        graph.setOutputs(last);

        // Build the network with defined configuration
        final ComputationGraph network = new ComputationGraph(graph.build());
        network.init();

        return network;
    }

    //-----------//
    // component //
    //-----------//
    /**
     * Append a component made of 3 layers in sequence: Norm + Relu + Convolution.
     *
     * @param graph  the graph configuration being defined
     * @param stride stride value (same in x and y)
     * @param filter number of convolution filters
     * @param sbc    stage + block + component IDs
     * @param input  component input
     * @return the name of last layer
     */
    private String component (GraphBuilder graph,
                              int stride,
                              int filter,
                              String sbc,
                              String input)
    {
        final String normName = "bn_" + sbc;
        final String reluName = "relu_" + sbc;
        final String convName = "conv2d_" + sbc;

        graph
                .addLayer(normName,
                          new BatchNormalization(),
                          input)
                .addLayer(reluName,
                          new ActivationLayer.Builder().activation(Activation.RELU).build(),
                          normName)
                .addLayer(convName,
                          new ConvolutionLayer.Builder()
                                  .kernelSize(3, 3)
                                  .padding(1, 1)
                                  .stride(stride, stride)
                                  .nOut(filter)
                                  .build(),
                          reluName);

        return convName;
    }

    //--------------------//
    // firstIdentityBlock //
    //--------------------//
    /**
     * Append the first identity block made of 2 parallel branches merged at the end:
     * <ul>
     * <li>a sequence of 1 convolution and 1 component
     * <li>an identity shortcut
     * </ul>
     *
     * @param graph  the graph configuration being defined
     * @param filter number of convolution filters
     * @param sb     stage + block IDs
     * @param input  block input
     * @return the name of last layer
     */
    private String firstIdentityBlock (GraphBuilder graph,
                                       int filter,
                                       String sb,
                                       String input)
    {
        final String convName = "conv2d_" + sb + "a";

        graph.addLayer(convName,
                       new ConvolutionLayer.Builder()
                               .kernelSize(3, 3)
                               .stride(1, 1)
                               .nOut(filter)
                               .convolutionMode(ConvolutionMode.Same)
                               .build(),
                       input);

        String last = convName;

        last = component(graph, 1, filter, sb + "b", last);

        // shortcut
        final String shortcutName = "add_" + sb;
        graph.addVertex(shortcutName,
                        new ElementWiseVertex(ElementWiseVertex.Op.Add),
                        last, input);

        return shortcutName;
    }

    //---------------//
    // identityBlock //
    //---------------//
    /**
     * Append an identity block made of 2 parallel branches merged at the end:
     * <ul>
     * <li>a sequence of 2 components
     * <li>an identity shortcut
     * </ul>
     *
     * @param graph  the graph configuration being defined
     * @param filter number of convolution filters
     * @param sb     stage + block IDs
     * @param input  block input
     * @return the name of last layer
     */
    private String identityBlock (GraphBuilder graph,
                                  int filter,
                                  String sb,
                                  String input)
    {
        String last;
        last = component(graph, 1, filter, sb + "a", input);
        last = component(graph, 1, filter, sb + "b", last);

        // shortcut
        final String shortcutName = "add_" + sb;
        graph.addVertex(shortcutName,
                        new ElementWiseVertex(ElementWiseVertex.Op.Add),
                        last, input);

        return shortcutName;
    }

    //----------------//
    // reductionBlock //
    //----------------//
    /**
     * Append a reduction block made of 2 parallel reduction branches merged at the end:
     * <ul>
     * <li>a sequence of 2 components, the first one performing a reduction
     * <li>a reduction shortcut
     * </ul>
     *
     * @param graph       the graph configuration being defined
     * @param firstStride stride for first layer (same value in x and y)
     * @param filter      number of convolution filters
     * @param sb          stage + block IDs
     * @param input       block input
     * @return the name of last layer
     */
    private String reductionBlock (GraphBuilder graph,
                                   int firstStride,
                                   int filter,
                                   String sb,
                                   String input)
    {
        String last;
        last = component(graph, firstStride, filter, sb + "a", input);
        last = component(graph, 1, filter, sb + "b", last);

        // Projection shortcut
        final String convName = "conv2d_" + sb + "s";
        graph.addLayer(convName,
                       new ConvolutionLayer.Builder()
                               .kernelSize(1, 1)
                               .stride(firstStride, firstStride)
                               .nOut(filter)
                               .build(),
                       input);

        // Addition of the 2 branches
        final String shortcutName = "add_" + sb;
        graph.addVertex(shortcutName,
                        new ElementWiseVertex(ElementWiseVertex.Op.Add),
                        last, convName);

        return shortcutName;
    }

    //-----------//
    // stemBlock //
    //-----------//
    /**
     * Append the block of network first layers.
     *
     * @param graph the graph configuration being defined
     * @param input block input
     * @return the name of last layer
     */
    private String stemBlock (GraphBuilder graph,
                              String input)
    {
        // Stem (Stage 1)
        graph
                .addLayer("conv2d_1",
                          new ConvolutionLayer.Builder()
                                  .kernelSize(7, 7)
                                  .padding(3, 3)
                                  .stride(2, 2)
                                  .nOut(64)
                                  .build(),
                          input)
                .addLayer("bn_1",
                          new BatchNormalization(),
                          "conv2d_1")
                .addLayer("relu_1",
                          new ActivationLayer.Builder()
                                  .activation(Activation.RELU)
                                  .build(),
                          "bn_1")
                .addLayer("maxpool2d_1",
                          new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                                  .convolutionMode(ConvolutionMode.Same)
                                  .kernelSize(3, 3)
                                  .stride(2, 2)
                                  .build(),
                          "relu_1");

        return "maxpool2d_1";
    }

    //-----------//
    // tailBlock //
    //-----------//
    /**
     * Append the block of network last layers.
     *
     * @param graph the graph configuration being defined
     * @param input block input
     * @return the name of last layer
     */
    private String tailBlock (GraphBuilder graph,
                              String input)
    {
        graph
                .addLayer("bn_10",
                          new BatchNormalization(),
                          input)
                .addLayer("relu_10",
                          new ActivationLayer.Builder()
                                  .activation(Activation.RELU)
                                  .build(),
                          "bn_10")
                .addLayer("avgpool2d_1",
                          new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.AVG)
                                  .kernelSize(4, 2)
                                  .stride(2, 2)
                                  .build(),
                          "relu_10")
                .addLayer("fc_out",
                          new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                                  .nOut(numClasses)
                                  .activation(Activation.SOFTMAX)
                                  .build(),
                          "avgpool2d_1");

        return "fc_out";
    }

    //------//
    // main //
    //------//
    /**
     * Pseudo main method to easily print out summary end memory report.
     *
     * @param args unused
     */
    public static void main (String[] args)
    {
        ///network = new ResNet18V2(1, CONTEXT_WIDTH, CONTEXT_HEIGHT, NUM_CLASSES).create();
        ComputationGraph network = new ResNet18V2(1, 56, 112, 204).create(); // 197 vs 204

        System.out.println();
        System.out.println("*** ResNet18V2 ***");
        ///InputType inputType = InputType.convolutionalFlat(inputHeight, inputWidth, inputDepth);
        System.out.printf("inputHeight:%d, inputWidth:%d, inputDepth:%d, numClasses:%d",
                          56, 112, 1, 204); // 197 vs 204
        InputType inputType = InputType.convolutionalFlat(112, 56, 1);
        System.out.println(network.summary(inputType));
        System.out.println(network.getConfiguration().getMemoryReport(inputType));
    }
}
