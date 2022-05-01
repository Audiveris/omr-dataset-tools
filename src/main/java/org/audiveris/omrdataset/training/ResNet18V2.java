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

import org.deeplearning4j.nn.conf.graph.GraphVertex;
import org.deeplearning4j.nn.conf.layers.Layer;

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
    protected final int inputDepth;

    protected final int inputWidth;

    protected final int inputHeight;

    protected final int numClasses;

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
        last = convBlock(graph, 2, 128, "4", last);
        last = identityBlock(graph, 128, "5", last);

        // Stage 4
        last = convBlock(graph, 2, 256, "6", last);
        last = identityBlock(graph, 256, "7", last);

        // Stage 5
        last = convBlock(graph, 2, 512, "8", last);
        last = identityBlock(graph, 512, "9", last);

        // Tail
        last = tailBlock(graph, last);

        graph.setOutputs(last);

        // Build the network with defined configuration
        final ComputationGraph network = new ComputationGraph(graph.build());
        network.init();

        return network;
    }

    //----------//
    // addLayer //
    //----------//
    /**
     * Convenient method to ease the linking of created layers.
     *
     * @param graph       the graph being built
     * @param layerName   name for the layer to create
     * @param layer       layer implementation
     * @param layerInputs name(s) of layer input(s)
     * @return layerName, to ease further linking
     */
    protected static String addLayer (GraphBuilder graph,
                                      String layerName,
                                      Layer layer,
                                      String... layerInputs)
    {
        graph.addLayer(layerName, layer, layerInputs);
        return layerName;
    }

    //-----------//
    // addVertex //
    //-----------//
    protected static String addVertex (GraphBuilder graph,
                                       String vertexName,
                                       GraphVertex vertex,
                                       String... vertexInputs)
    {
        graph.addVertex(vertexName, vertex, vertexInputs);
        return vertexName;
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
    protected String component (GraphBuilder graph,
                                int stride,
                                int filter,
                                String sbc,
                                String input)
    {
        String last;

        last = addLayer(graph, "conv_" + sbc,
                        new ConvolutionLayer.Builder()
                                .kernelSize(3, 3)
                                .padding(1, 1)
                                .stride(stride, stride)
                                .nOut(filter)
                                .build(), input);

        last = addLayer(graph, "bn_" + sbc,
                        new BatchNormalization(), last);
        return last;
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
    protected String firstIdentityBlock (GraphBuilder graph,
                                         int filter, // = 64
                                         String sb,
                                         String input)
    {
        String left;
        left = addLayer(graph, "conv_" + sb + "a",
                        new ConvolutionLayer.Builder()
                                .kernelSize(3, 3)
                                .stride(1, 1)
                                .nOut(filter)
                                .convolutionMode(ConvolutionMode.Same)
                                .build(),
                        input);
        left = addLayer(graph, "bn_" + sb + "a", new BatchNormalization(), left);

        left = addLayer(graph, "relu_" + sb + "a",
                        new ActivationLayer.Builder().activation(Activation.RELU).build(), left);

        left = component(graph, 1, filter, sb + "b", left);

        // shortcut
        String both = addVertex(graph, "add_" + sb,
                                new ElementWiseVertex(ElementWiseVertex.Op.Add),
                                left, input);

        both = addLayer(graph, "relu_" + sb,
                        new ActivationLayer.Builder().activation(Activation.RELU).build(), both);

        return both;
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
    protected String identityBlock (GraphBuilder graph,
                                    int filter,
                                    String sb,
                                    String input)
    {
        String left;
        left = component(graph, 1, filter, sb + "a", input);
        left = addLayer(graph, "relu_" + sb + "a",
                        new ActivationLayer.Builder().activation(Activation.RELU).build(), left);

        left = component(graph, 1, filter, sb + "b", left);

        // shortcut
        String both;
        both = addVertex(graph, "add_" + sb,
                         new ElementWiseVertex(ElementWiseVertex.Op.Add),
                         left, input);

        both = addLayer(graph, "relu_" + sb,
                        new ActivationLayer.Builder().activation(Activation.RELU).build(), both);

        return both;
    }

    //-----------//
    // convBlock //
    //-----------//
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
    protected String convBlock (GraphBuilder graph,
                                int firstStride,
                                int filter,
                                String sb,
                                String input)
    {
        String left;
        left = component(graph, firstStride, filter, sb + "a", input);
        left = addLayer(graph, "relu_" + sb + "a",
                        new ActivationLayer.Builder().activation(Activation.RELU).build(), left);

        left = component(graph, 1, filter, sb + "b", left);

        // Projection shortcut
        String right = addLayer(graph, "conv_" + sb + "_skip",
                                new ConvolutionLayer.Builder()
                                        .kernelSize(1, 1)
                                        .stride(firstStride, firstStride)
                                        .nOut(filter)
                                        .build(),
                                input);
        right = addLayer(graph, "bn_" + sb + "_skip", new BatchNormalization(), right);

        // Both
        String both = addVertex(graph, "add_" + sb,
                                new ElementWiseVertex(ElementWiseVertex.Op.Add),
                                left, right);
        both = addLayer(graph, "relu_" + sb,
                        new ActivationLayer.Builder().activation(Activation.RELU).build(), both);

        return both;
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
    protected String stemBlock (GraphBuilder graph,
                                String input)
    {
        // Stem (Stage 1)
        String last;
        last = addLayer(graph, "conv_1",
                        new ConvolutionLayer.Builder()
                                .kernelSize(7, 7)
                                .padding(3, 3)
                                .stride(2, 2)
                                .nOut(64)
                                .build(),
                        input);
        last = addLayer(graph, "bn_1",
                        new BatchNormalization(),
                        last);
        last = addLayer(graph, "relu_1",
                        new ActivationLayer.Builder()
                                .activation(Activation.RELU)
                                .build(),
                        last);
        last = addLayer(graph, "maxpool_1",
                        new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                                .convolutionMode(ConvolutionMode.Same)
                                .kernelSize(3, 3)
                                .stride(2, 2)
                                .build(),
                        last);

        return last;
    }

    //-----------//
    // tailBlock //
    //-----------//
    /**
     * Append the block of network last layers.
     * <p>
     * NOTA: The SubsamplingLayer has been (temporarily) removed to cope with small input patches.
     *
     * @param graph the graph configuration being defined
     * @param input block input
     * @return the name of last layer
     */
    protected String tailBlock (GraphBuilder graph,
                                String input)
    {
        String last = input;

        last = addLayer(graph, "avgpool_1",
                        new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.AVG)
                                ///.kernelSize(4, 2) ????
                                .kernelSize(2, 2)
                                .stride(2, 2)
                                .build(),
                        last);

        last = addLayer(graph, "fc_out",
                        new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                                .nOut(numClasses)
                                .activation(Activation.SOFTMAX)
                                .build(),
                        last);

        return last;
    }

    //------//
    // main //
    //------//
    /**
     * Pseudo main method to easily print out summary and memory report.
     *
     * @param args unused
     */
    public static void main (String[] args)
    {
        final int CONTEXT_DEPTH = 1;
        final int CONTEXT_WIDTH = 54; //60; //33; //224; //54;
        final int CONTEXT_HEIGHT = 42; //100; //97; //224; //42;
        final int NUM_CLASSES = 12;
        ComputationGraph network = new ResNet18V2(CONTEXT_DEPTH,
                                                  CONTEXT_WIDTH,
                                                  CONTEXT_HEIGHT,
                                                  NUM_CLASSES).create();

        System.out.println();
        System.out.println("*** ResNet18V2 ***");
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
}
