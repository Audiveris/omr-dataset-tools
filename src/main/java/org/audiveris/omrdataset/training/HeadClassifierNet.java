//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                H e a d C l a s s i f i e r N e t                               //
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.CacheMode;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.distribution.TruncatedNormalDistribution;
import org.deeplearning4j.nn.conf.graph.ElementWiseVertex;
import org.deeplearning4j.nn.conf.graph.GraphVertex;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ActivationLayer;
import org.deeplearning4j.nn.conf.layers.BatchNormalization;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DepthwiseConvolution2D;
import org.deeplearning4j.nn.conf.layers.Layer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.conf.layers.ZeroPaddingLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInitDistribution;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * Class <code>HeadClassifierNet</code> is derived from MobileNet V2 architecture to provide
 * support for head shapes.
 *
 * @author Hervé Bitteur
 */
public class HeadClassifierNet
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(HeadClassifierNet.class);

    //~ Instance fields ----------------------------------------------------------------------------
    protected final int inputHeight;

    protected final int inputWidth;

    protected final int inputDepth;

    protected final int numClasses;

    /**
     * alpha: Float, larger than zero, controls the width of the network.
     * This is known as the width multiplier in the MobileNetV2 paper, but the name is
     * kept for consistency with `applications.MobileNetV1` model in Keras.
     * - If `alpha` &lt; 1.0, proportionally decreases the number of filters in each layer.
     * - If `alpha` &gt; 1.0, proportionally increases the number of filters in each layer.
     * - If `alpha` = 1.0, default number of filters from the paper are used at each layer.
     */
    protected final double alpha = 1.0;

    /**
     * classifier_activation: The activation function to use on the "top" layer.
     * A `str` or callable.
     * Ignored unless `include_top=True`.
     * Set `classifier_activation=None` to return the logits of the "top" layer.
     * When loading pretrained weights, `classifier_activation` can only be `None` or `"softmax"`.
     */
    ///protected final String classifier_activation;
    ComputationGraphConfiguration.GraphBuilder builder;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Factory for HeadClassifierNet network instances.
     *
     * @param inputHeight input height
     * @param inputWidth  input width
     * @param inputDepth  input number of channels (1 for gray, 3 for RGB)
     * @param numClasses  number of classes to recognize
     */
    public HeadClassifierNet (int inputHeight,
                              int inputWidth,
                              int inputDepth,
                              int numClasses)
    {
        this.inputHeight = inputHeight;
        this.inputWidth = inputWidth;
        this.inputDepth = inputDepth;
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
        builder = new NeuralNetConfiguration.Builder()
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

        builder.setInputTypes(InputType.convolutionalFlat(inputHeight, inputWidth, inputDepth));
        String x = "input_1";
        builder.addInputs(x);

        //    img_input = layers.Input(shape=input_shape) ///???????????????????????????????
        //  first_block_filters = _make_divisible(32 * alpha, 8)
        final int first_block_filters = make_divisible((int) Math.floor(32 * alpha), 8);

        //  x = layers.Conv2D(
        //      first_block_filters,
        //      kernel_size=3,
        //      strides=(2, 2),
        //      padding='same',
        //      use_bias=False,
        //      name='Conv1')(img_input)
        x = addLayer("Conv1",
                     new ConvolutionLayer.Builder()
                             .nOut(first_block_filters)
                             .kernelSize(3, 3)
                             .stride(2, 2)
                             .padding(1, 1)
                             .hasBias(false)
                             .build(),
                     x);

        //  x = layers.BatchNormalization(
        //      axis=channel_axis, epsilon=1e-3, momentum=0.999, name='bn_Conv1')(x)
        x = addLayer("bn_Conv1",
                     new BatchNormalization(),
                     x);

        //  x = layers.ReLU(6., name='Conv1_relu')(x)
        x = addLayer("Conv1_relu",
                     new ActivationLayer.Builder().activation(Activation.RELU6).build(),
                     x);

        //                            block_id+
        //                           stride+  |
        //                       filters+  |  |
        //               in_channels+   |  |  |
        //             expansion+   |   |  |  |
        //              input+  |   |   |  |  |
        // Arguments:        |  |   |   |  |  |
        //-------------------+--+---+---+--+--+
        x = invertedResBlock(x, 1, 32, 16, 1, 0);

        x = invertedResBlock(x, 4.5, 16, 24, 2, 1);
        x = invertedResBlock(x, 3.6, 24, 24, 1, 2);

        x = invertedResBlock(x, 4, 24, 32, 2, 3);
        x = invertedResBlock(x, 6, 32, 32, 1, 4);
        x = invertedResBlock(x, 6, 32, 32, 1, 5);

        x = invertedResBlock(x, 3, 32, 64, 2, 6);
        x = invertedResBlock(x, 3, 64, 64, 1, 7);
        x = invertedResBlock(x, 6, 64, 64, 1, 8);
        x = invertedResBlock(x, 6, 64, 64, 1, 9);

        x = invertedResBlock(x, 6, 64, 96, 1, 10);
//        x = invertedResBlock(x, 6, 192, 192, 1, 11);
//        x = invertedResBlock(x, 6, 192, 192, 1, 12);
//
//        x = invertedResBlock(x, 6, 192, 160, 2, 13);
//        x = invertedResBlock(x, 6, 160, 160, 1, 14);
//        x = invertedResBlock(x, 6, 160, 160, 1, 15);
//
//        x = invertedResBlock(x, 6, 160, 320, 1, 16);
//
//        // no alpha applied to last conv as stated in the paper:
//        // if the width multiplier is greater than 1 we increase the number of output channels.
//        //  if alpha > 1.0:
//        //    last_block_filters = _make_divisible(1280 * alpha, 8)
//        //  else:
//        //    last_block_filters = 1280
//        final int last_block_filters = (alpha > 1)
//                ////? make_divisible((int) Math.floor(1280 * alpha), 8) : 1280;
//                ? make_divisible((int) Math.floor(640 * alpha), 8) : 640;
//
//        //  x = layers.Conv2D(last_block_filters, kernel_size=1, use_bias=False, name='Conv_1')(x)
//        x = addLayer("Conv_1",
//                     new ConvolutionLayer.Builder()
//                             .nOut(last_block_filters)
//                             .kernelSize(1, 1)
//                             .stride(1, 1)
//                             .hasBias(false)
//                             .build(),
//                     x);
//
//        //  x = layers.BatchNormalization(axis=channel_axis, epsilon=1e-3, momentum=0.999, name='Conv_1_bn')(x)
//        x = addLayer("Conv_1_bn",
//                     new BatchNormalization(),
//                     x);
//
//        //  x = layers.ReLU(6., name='out_relu')(x)
//        x = addLayer("out_relu",
//                     new ActivationLayer.Builder().activation(Activation.RELU6).build(),
//                     x);
//
//        //    x = layers.GlobalAveragePooling2D()(x)
//        x = addLayer("global_average_pooling2d",
//                     new GlobalPoolingLayer.Builder()
//                             .poolingType(PoolingType.AVG)
//                             .build(),
//                     x);
//        //    imagenet_utils.validate_activation(classifier_activation, weights)
//        //    x = layers.Dense(classes, activation=classifier_activation, name='predictions')(x)
//        x = addLayer("predictions",
//                     new DenseLayer.Builder()
//                             .activation(Activation.SOFTMAX)
//                             .nOut(250) // Why not?
//                             .build(),
//                     x);
////
////        x = addLayer("predictions_loss",
////                     new LossLayer.Builder(LossFunction.MCXENT)
////                             .build(),
////                     x);
////
        x = addLayer("avgpool_1",
                     new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.AVG)
                             ///.kernelSize(4, 2) ????
                             .kernelSize(2, 2)
                             .stride(2, 2)
                             .build(),
                     x);

        x = addLayer("predictions_loss",
                     new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                             .nOut(numClasses)
                             .activation(Activation.SOFTMAX)
                             .build(),
                     x);

        // This is the end...
        builder.setOutputs(x);

        // Build the network with defined configuration
        final ComputationGraph network = new ComputationGraph(builder.build());
        network.init();

        logger.info("Summary: {}", network.summary());

        return network;
    }

    //------------------//
    // invertedResBlock //
    //------------------//
    /**
     * Add an inverted residual block.
     *
     * @param input       name of input layer
     * @param expansion   expansion factor
     * @param in_channels number of input channels
     * @param stride      stride value
     * @param filters     number of filters
     * @param block_id    unique id for the block
     * @return the name of last layer
     */
    private String invertedResBlock (String input,
                                     double expansion,
                                     int in_channels,
                                     int filters,
                                     int stride,
                                     int block_id)
    {
        //  channel_axis = 1 if backend.image_data_format() == 'channels_first' else -1
        //
        //  in_channels = backend.int_shape(inputs)[channel_axis]

        //  pointwise_conv_filters = int(filters * alpha)
        final int pointwise_conv_filters = (int) Math.floor(filters * alpha);

        // Ensure the number of filters on the last 1x1 convolution is divisible by 8.
        //----------------------------------------------------------------------------
        //  pointwise_filters = _make_divisible(pointwise_conv_filters, 8)
        final int pointwise_filters = make_divisible(pointwise_conv_filters, 8);

        String prefix = "block_" + block_id + "_";
        String x = input;

        if (block_id != 0) {
            // Expand with a pointwise 1x1 convolution.
            //-----------------------------------------

            //    x = layers.Conv2D(
            //        expansion * in_channels,
            //        kernel_size=1,
            //        padding='same',
            //        use_bias=False,
            //        activation=None,
            //        name=prefix + 'expand')(x)
            x = addLayer(prefix + "expand",
                         new ConvolutionLayer.Builder()
                                 .nOut((int) Math.rint(expansion * in_channels))
                                 .kernelSize(1, 1)
                                 .stride(1, 1)
                                 .hasBias(false)
                                 .build(),
                         input);

            //    x = layers.BatchNormalization(
            //        axis=channel_axis,
            //        epsilon=1e-3,
            //        momentum=0.999,
            //        name=prefix + 'expand_BN')(x)
            x = addLayer(prefix + "expand_BN",
                         new BatchNormalization(),
                         x);

            //    x = layers.ReLU(6., name=prefix + 'expand_relu')(x)
            x = addLayer(prefix + "expand_relu",
                         new ActivationLayer.Builder().activation(Activation.RELU6).build(),
                         x);
        } else {
            // prefix = 'expanded_conv_'
            prefix = "expanded_conv_";
        }

        // Depthwise 3x3 convolution.
        //---------------------------
        //  if stride == 2:
        //    x = layers.ZeroPadding2D(
        //        padding=imagenet_utils.correct_pad(x, 3),
        //        name=prefix + 'pad')(x)
        if (stride == 2) {
            x = addLayer(prefix + "pad",
                         new ZeroPaddingLayer(2, 2), // Seems ok: k - 1
                         x);
        }

        //  x = layers.DepthwiseConv2D(
        //      kernel_size=3,
        //      strides=stride,
        //      activation=None,
        //      use_bias=False,
        //      padding='same' if stride == 1 else 'valid',
        //      name=prefix + 'depthwise')(x)
        x = addLayer(prefix + "depthwise",
                     new DepthwiseConvolution2D.Builder(
                             new int[]{3, 3}, // kernelSize
                             new int[]{stride, stride}, // stride
                             new int[]{1, 1}) // padding /// TO BE CHECKED
                             .hasBias(false)
                             .build(),
                     x);

        //  x = layers.BatchNormalization(
        //      axis=channel_axis,
        //      epsilon=1e-3,
        //      momentum=0.999,
        //      name=prefix + 'depthwise_BN')(x)
        x = addLayer(prefix + "depthwise_BN",
                     new BatchNormalization(),
                     x);

        //  x = layers.ReLU(6., name=prefix + 'depthwise_relu')(x)
        x = addLayer(prefix + "depthwise_relu",
                     new ActivationLayer.Builder().activation(Activation.RELU6).build(),
                     x);

        // Project with a pointwise 1x1 convolution.
        //------------------------------------------
        //  x = layers.Conv2D(
        //      pointwise_filters,
        //      kernel_size=1,
        //      padding='same',
        //      use_bias=False,
        //      activation=None, # Attention!
        //      name=prefix + 'project')(x)
        x = addLayer(prefix + "project",
                     new ConvolutionLayer.Builder()
                             .nOut(pointwise_filters)
                             .kernelSize(1, 1)
                             .stride(stride, stride)
                             .hasBias(false)
                             .build(),
                     x);

        //  x = layers.BatchNormalization(axis=channel_axis, epsilon=1e-3, momentum=0.999,
        //      name=prefix + 'project_BN')(x)
        x = addLayer(prefix + "project_BN",
                     new BatchNormalization(),
                     x);

        //  if in_channels == pointwise_filters and stride == 1:
        //    return layers.Add(name=prefix + 'add')([inputs, x])
        if (in_channels == pointwise_filters && stride == 1) {
            x = addVertex(prefix + "add",
                          new ElementWiseVertex(ElementWiseVertex.Op.Add),
                          input, x);
        }

        // NOTA: There is no final RELU in this block
        //
        return x;
    }

    //----------//
    // addLayer //
    //----------//
    /**
     * Convenient method to ease the linking of created layers.
     *
     * @param layerName   name for the layer to create
     * @param layer       layer implementation
     * @param layerInputs name(s) of layer input(s)
     * @return layerName, to ease further linking
     */
    protected String addLayer (String layerName,
                               Layer layer,
                               String... layerInputs)
    {
        builder.addLayer(layerName, layer, layerInputs);
        return layerName;
    }

    //-----------//
    // addVertex //
    //-----------//
    protected String addVertex (String vertexName,
                                GraphVertex vertex,
                                String... vertexInputs)
    {
        builder.addVertex(vertexName, vertex, vertexInputs);
        return vertexName;
    }

    //----------------//
    // make_divisible //
    //----------------//
    protected static int make_divisible (final int v,
                                         final int divisor)
    {
        return make_divisible(v, divisor, null);
    }

    //----------------//
    // make_divisible //
    //----------------//
    protected static int make_divisible (final int v,
                                         final int divisor,
                                         Integer min_value)
    {
        //  if min_value is None:
        //    min_value = divisor
        if (min_value == null) {
            min_value = divisor;
        }

        //  new_v = max(min_value, (int(v + divisor / 2) // divisor) * divisor)
        int new_v = Math.max(min_value,
                             Math.floorDiv(v + divisor / 2, divisor) * divisor);

        // Make sure that round down does not go down by more than 10%.
        //-------------------------------------------------------------
        //  if new_v < 0.9 * v:
        //    new_v += divisor
        if (new_v < 0.9 * v) {
            new_v += divisor;
        }

        //  return new_v
        logger.info("make_divisible, v:{} return {}", v, new_v);
        return new_v;
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
        final int CONTEXT_HEIGHT = 42; //100; //97; //224; //42;
        final int CONTEXT_WIDTH = 54; //60; //33; //224; //54;
        final int CONTEXT_DEPTH = 1;
        final int NUM_CLASSES = 12;
        ComputationGraph network = new HeadClassifierNet(CONTEXT_HEIGHT,
                                                         CONTEXT_WIDTH,
                                                         CONTEXT_DEPTH,
                                                         NUM_CLASSES).create();

        System.out.println();
        System.out.println("*** HeadClassifierNet ***");
        System.out.printf("CONTEXT_HEIGHT:%d, CONTEXT_WIDTH:%d, CONTEXT_DEPTH:%d, NUM_CLASSES:%d",
                          CONTEXT_HEIGHT, CONTEXT_WIDTH, CONTEXT_DEPTH, NUM_CLASSES);
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
// For alpha==1:
//block_id: 0 in_channels: 32
//block_id: 1 in_channels: 16
//block_id: 2 in_channels: 24
//block_id: 3 in_channels: 24
//block_id: 4 in_channels: 32
//block_id: 5 in_channels: 32
//block_id: 6 in_channels: 32
//block_id: 7 in_channels: 64
//block_id: 8 in_channels: 64
//block_id: 9 in_channels: 64
//block_id: 10 in_channels: 64
//block_id: 11 in_channels: 192
//block_id: 12 in_channels: 192
//block_id: 13 in_channels: 192
//block_id: 14 in_channels: 160
//block_id: 15 in_channels: 160
//block_id: 16 in_channels: 160
//
// For alpha==1.4:
//block_id: 0 in_channels: 48
//block_id: 1 in_channels: 24
//block_id: 2 in_channels: 32
//block_id: 3 in_channels: 32
//block_id: 4 in_channels: 48
//block_id: 5 in_channels: 48
//block_id: 6 in_channels: 48
//block_id: 7 in_channels: 88
//block_id: 8 in_channels: 88
//block_id: 9 in_channels: 88
//block_id: 10 in_channels: 88
//block_id: 11 in_channels: 272
//block_id: 12 in_channels: 272
//block_id: 13 in_channels: 272
//block_id: 14 in_channels: 224
//block_id: 15 in_channels: 224
//block_id: 16 in_channels: 224

//Model: "mobilenetv2_1.00_54"
//__________________________________________________________________________________________________
// Layer (type)                   Output Shape         Param #     Connected to
//==================================================================================================
// input_1 (InputLayer)           [(None, 54, 54, 1)]  0           []
//
// Conv1 (Conv2D)                 (None, 27, 27, 32)   288         ['input_1[0][0]']
//
// bn_Conv1 (BatchNormalization)  (None, 27, 27, 32)   128         ['Conv1[0][0]']
//
// Conv1_relu (ReLU)              (None, 27, 27, 32)   0           ['bn_Conv1[0][0]']
//
// expanded_conv_depthwise (Depth  (None, 27, 27, 32)  288         ['Conv1_relu[0][0]']
// wiseConv2D)
//
// expanded_conv_depthwise_BN (Ba  (None, 27, 27, 32)  128         ['expanded_conv_depthwise[0][0]']
// tchNormalization)
//
// expanded_conv_depthwise_relu (  (None, 27, 27, 32)  0           ['expanded_conv_depthwise_BN[0][0
// ReLU)                                                           ]']
//
// expanded_conv_project (Conv2D)  (None, 27, 27, 16)  512         ['expanded_conv_depthwise_relu[0]
//                                                                 [0]']
//
// expanded_conv_project_BN (Batc  (None, 27, 27, 16)  64          ['expanded_conv_project[0][0]']
// hNormalization)
//
// block_1_expand (Conv2D)        (None, 27, 27, 96)   1536        ['expanded_conv_project_BN[0][0]'
//                                                                 ]
//
// block_1_expand_BN (BatchNormal  (None, 27, 27, 96)  384         ['block_1_expand[0][0]']
// ization)
//
// block_1_expand_relu (ReLU)     (None, 27, 27, 96)   0           ['block_1_expand_BN[0][0]']
//
// block_1_pad (ZeroPadding2D)    (None, 29, 29, 96)   0           ['block_1_expand_relu[0][0]']
//
// block_1_depthwise (DepthwiseCo  (None, 14, 14, 96)  864         ['block_1_pad[0][0]']
// nv2D)
//
// block_1_depthwise_BN (BatchNor  (None, 14, 14, 96)  384         ['block_1_depthwise[0][0]']
// malization)
//
// block_1_depthwise_relu (ReLU)  (None, 14, 14, 96)   0           ['block_1_depthwise_BN[0][0]']
//
// block_1_project (Conv2D)       (None, 14, 14, 24)   2304        ['block_1_depthwise_relu[0][0]']
//
// block_1_project_BN (BatchNorma  (None, 14, 14, 24)  96          ['block_1_project[0][0]']
// lization)
//
// block_2_expand (Conv2D)        (None, 14, 14, 144)  3456        ['block_1_project_BN[0][0]']
//
// block_2_expand_BN (BatchNormal  (None, 14, 14, 144)  576        ['block_2_expand[0][0]']
// ization)
//
// block_2_expand_relu (ReLU)     (None, 14, 14, 144)  0           ['block_2_expand_BN[0][0]']
//
// block_2_depthwise (DepthwiseCo  (None, 14, 14, 144)  1296       ['block_2_expand_relu[0][0]']
// nv2D)
//
// block_2_depthwise_BN (BatchNor  (None, 14, 14, 144)  576        ['block_2_depthwise[0][0]']
// malization)
//
// block_2_depthwise_relu (ReLU)  (None, 14, 14, 144)  0           ['block_2_depthwise_BN[0][0]']
//
// block_2_project (Conv2D)       (None, 14, 14, 24)   3456        ['block_2_depthwise_relu[0][0]']
//
// block_2_project_BN (BatchNorma  (None, 14, 14, 24)  96          ['block_2_project[0][0]']
// lization)
//
// block_2_add (Add)              (None, 14, 14, 24)   0           ['block_1_project_BN[0][0]',
//                                                                  'block_2_project_BN[0][0]']
//
// block_3_expand (Conv2D)        (None, 14, 14, 144)  3456        ['block_2_add[0][0]']
//
// block_3_expand_BN (BatchNormal  (None, 14, 14, 144)  576        ['block_3_expand[0][0]']
// ization)
//
// block_3_expand_relu (ReLU)     (None, 14, 14, 144)  0           ['block_3_expand_BN[0][0]']
//
// block_3_pad (ZeroPadding2D)    (None, 15, 15, 144)  0           ['block_3_expand_relu[0][0]']
//
// block_3_depthwise (DepthwiseCo  (None, 7, 7, 144)   1296        ['block_3_pad[0][0]']
// nv2D)
//
// block_3_depthwise_BN (BatchNor  (None, 7, 7, 144)   576         ['block_3_depthwise[0][0]']
// malization)
//
// block_3_depthwise_relu (ReLU)  (None, 7, 7, 144)    0           ['block_3_depthwise_BN[0][0]']
//
// block_3_project (Conv2D)       (None, 7, 7, 32)     4608        ['block_3_depthwise_relu[0][0]']
//
// block_3_project_BN (BatchNorma  (None, 7, 7, 32)    128         ['block_3_project[0][0]']
// lization)
//
// block_4_expand (Conv2D)        (None, 7, 7, 192)    6144        ['block_3_project_BN[0][0]']
//
// block_4_expand_BN (BatchNormal  (None, 7, 7, 192)   768         ['block_4_expand[0][0]']
// ization)
//
// block_4_expand_relu (ReLU)     (None, 7, 7, 192)    0           ['block_4_expand_BN[0][0]']
//
// block_4_depthwise (DepthwiseCo  (None, 7, 7, 192)   1728        ['block_4_expand_relu[0][0]']
// nv2D)
//
// block_4_depthwise_BN (BatchNor  (None, 7, 7, 192)   768         ['block_4_depthwise[0][0]']
// malization)
//
// block_4_depthwise_relu (ReLU)  (None, 7, 7, 192)    0           ['block_4_depthwise_BN[0][0]']
//
// block_4_project (Conv2D)       (None, 7, 7, 32)     6144        ['block_4_depthwise_relu[0][0]']
//
// block_4_project_BN (BatchNorma  (None, 7, 7, 32)    128         ['block_4_project[0][0]']
// lization)
//
// block_4_add (Add)              (None, 7, 7, 32)     0           ['block_3_project_BN[0][0]',
//                                                                  'block_4_project_BN[0][0]']
//
// block_5_expand (Conv2D)        (None, 7, 7, 192)    6144        ['block_4_add[0][0]']
//
// block_5_expand_BN (BatchNormal  (None, 7, 7, 192)   768         ['block_5_expand[0][0]']
// ization)
//
// block_5_expand_relu (ReLU)     (None, 7, 7, 192)    0           ['block_5_expand_BN[0][0]']
//
// block_5_depthwise (DepthwiseCo  (None, 7, 7, 192)   1728        ['block_5_expand_relu[0][0]']
// nv2D)
//
// block_5_depthwise_BN (BatchNor  (None, 7, 7, 192)   768         ['block_5_depthwise[0][0]']
// malization)
//
// block_5_depthwise_relu (ReLU)  (None, 7, 7, 192)    0           ['block_5_depthwise_BN[0][0]']
//
// block_5_project (Conv2D)       (None, 7, 7, 32)     6144        ['block_5_depthwise_relu[0][0]']
//
// block_5_project_BN (BatchNorma  (None, 7, 7, 32)    128         ['block_5_project[0][0]']
// lization)
//
// block_5_add (Add)              (None, 7, 7, 32)     0           ['block_4_add[0][0]',
//                                                                  'block_5_project_BN[0][0]']
//
// block_6_expand (Conv2D)        (None, 7, 7, 192)    6144        ['block_5_add[0][0]']
//
// block_6_expand_BN (BatchNormal  (None, 7, 7, 192)   768         ['block_6_expand[0][0]']
// ization)
//
// block_6_expand_relu (ReLU)     (None, 7, 7, 192)    0           ['block_6_expand_BN[0][0]']
//
// block_6_pad (ZeroPadding2D)    (None, 9, 9, 192)    0           ['block_6_expand_relu[0][0]']
//
// block_6_depthwise (DepthwiseCo  (None, 4, 4, 192)   1728        ['block_6_pad[0][0]']
// nv2D)
//
// block_6_depthwise_BN (BatchNor  (None, 4, 4, 192)   768         ['block_6_depthwise[0][0]']
// malization)
//
// block_6_depthwise_relu (ReLU)  (None, 4, 4, 192)    0           ['block_6_depthwise_BN[0][0]']
//
// block_6_project (Conv2D)       (None, 4, 4, 64)     12288       ['block_6_depthwise_relu[0][0]']
//
// block_6_project_BN (BatchNorma  (None, 4, 4, 64)    256         ['block_6_project[0][0]']
// lization)
//
// block_7_expand (Conv2D)        (None, 4, 4, 384)    24576       ['block_6_project_BN[0][0]']
//
// block_7_expand_BN (BatchNormal  (None, 4, 4, 384)   1536        ['block_7_expand[0][0]']
// ization)
//
// block_7_expand_relu (ReLU)     (None, 4, 4, 384)    0           ['block_7_expand_BN[0][0]']
//
// block_7_depthwise (DepthwiseCo  (None, 4, 4, 384)   3456        ['block_7_expand_relu[0][0]']
// nv2D)
//
// block_7_depthwise_BN (BatchNor  (None, 4, 4, 384)   1536        ['block_7_depthwise[0][0]']
// malization)
//
// block_7_depthwise_relu (ReLU)  (None, 4, 4, 384)    0           ['block_7_depthwise_BN[0][0]']
//
// block_7_project (Conv2D)       (None, 4, 4, 64)     24576       ['block_7_depthwise_relu[0][0]']
//
// block_7_project_BN (BatchNorma  (None, 4, 4, 64)    256         ['block_7_project[0][0]']
// lization)
//
// block_7_add (Add)              (None, 4, 4, 64)     0           ['block_6_project_BN[0][0]',
//                                                                  'block_7_project_BN[0][0]']
//
// block_8_expand (Conv2D)        (None, 4, 4, 384)    24576       ['block_7_add[0][0]']
//
// block_8_expand_BN (BatchNormal  (None, 4, 4, 384)   1536        ['block_8_expand[0][0]']
// ization)
//
// block_8_expand_relu (ReLU)     (None, 4, 4, 384)    0           ['block_8_expand_BN[0][0]']
//
// block_8_depthwise (DepthwiseCo  (None, 4, 4, 384)   3456        ['block_8_expand_relu[0][0]']
// nv2D)
//
// block_8_depthwise_BN (BatchNor  (None, 4, 4, 384)   1536        ['block_8_depthwise[0][0]']
// malization)
//
// block_8_depthwise_relu (ReLU)  (None, 4, 4, 384)    0           ['block_8_depthwise_BN[0][0]']
//
// block_8_project (Conv2D)       (None, 4, 4, 64)     24576       ['block_8_depthwise_relu[0][0]']
//
// block_8_project_BN (BatchNorma  (None, 4, 4, 64)    256         ['block_8_project[0][0]']
// lization)
//
// block_8_add (Add)              (None, 4, 4, 64)     0           ['block_7_add[0][0]',
//                                                                  'block_8_project_BN[0][0]']
//
// block_9_expand (Conv2D)        (None, 4, 4, 384)    24576       ['block_8_add[0][0]']
//
// block_9_expand_BN (BatchNormal  (None, 4, 4, 384)   1536        ['block_9_expand[0][0]']
// ization)
//
// block_9_expand_relu (ReLU)     (None, 4, 4, 384)    0           ['block_9_expand_BN[0][0]']
//
// block_9_depthwise (DepthwiseCo  (None, 4, 4, 384)   3456        ['block_9_expand_relu[0][0]']
// nv2D)
//
// block_9_depthwise_BN (BatchNor  (None, 4, 4, 384)   1536        ['block_9_depthwise[0][0]']
// malization)
//
// block_9_depthwise_relu (ReLU)  (None, 4, 4, 384)    0           ['block_9_depthwise_BN[0][0]']
//
// block_9_project (Conv2D)       (None, 4, 4, 64)     24576       ['block_9_depthwise_relu[0][0]']
//
// block_9_project_BN (BatchNorma  (None, 4, 4, 64)    256         ['block_9_project[0][0]']
// lization)
//
// block_9_add (Add)              (None, 4, 4, 64)     0           ['block_8_add[0][0]',
//                                                                  'block_9_project_BN[0][0]']
//
// block_10_expand (Conv2D)       (None, 4, 4, 384)    24576       ['block_9_add[0][0]']
//
// block_10_expand_BN (BatchNorma  (None, 4, 4, 384)   1536        ['block_10_expand[0][0]']
// lization)
//
// block_10_expand_relu (ReLU)    (None, 4, 4, 384)    0           ['block_10_expand_BN[0][0]']
//
// block_10_depthwise (DepthwiseC  (None, 4, 4, 384)   3456        ['block_10_expand_relu[0][0]']
// onv2D)
//
// block_10_depthwise_BN (BatchNo  (None, 4, 4, 384)   1536        ['block_10_depthwise[0][0]']
// rmalization)
//
// block_10_depthwise_relu (ReLU)  (None, 4, 4, 384)   0           ['block_10_depthwise_BN[0][0]']
//
// block_10_project (Conv2D)      (None, 4, 4, 192)    73728       ['block_10_depthwise_relu[0][0]']
//
// block_10_project_BN (BatchNorm  (None, 4, 4, 192)   768         ['block_10_project[0][0]']
// alization)
//
// block_11_expand (Conv2D)       (None, 4, 4, 1152)   221184      ['block_10_project_BN[0][0]']
//
// block_11_expand_BN (BatchNorma  (None, 4, 4, 1152)  4608        ['block_11_expand[0][0]']
// lization)
//
// block_11_expand_relu (ReLU)    (None, 4, 4, 1152)   0           ['block_11_expand_BN[0][0]']
//
// block_11_depthwise (DepthwiseC  (None, 4, 4, 1152)  10368       ['block_11_expand_relu[0][0]']
// onv2D)
//
// block_11_depthwise_BN (BatchNo  (None, 4, 4, 1152)  4608        ['block_11_depthwise[0][0]']
// rmalization)
//
// block_11_depthwise_relu (ReLU)  (None, 4, 4, 1152)  0           ['block_11_depthwise_BN[0][0]']
//
// block_11_project (Conv2D)      (None, 4, 4, 192)    221184      ['block_11_depthwise_relu[0][0]']
//
// block_11_project_BN (BatchNorm  (None, 4, 4, 192)   768         ['block_11_project[0][0]']
// alization)
//
// block_11_add (Add)             (None, 4, 4, 192)    0           ['block_10_project_BN[0][0]',
//                                                                  'block_11_project_BN[0][0]']
//
// block_12_expand (Conv2D)       (None, 4, 4, 1152)   221184      ['block_11_add[0][0]']
//
// block_12_expand_BN (BatchNorma  (None, 4, 4, 1152)  4608        ['block_12_expand[0][0]']
// lization)
//
// block_12_expand_relu (ReLU)    (None, 4, 4, 1152)   0           ['block_12_expand_BN[0][0]']
//
// block_12_depthwise (DepthwiseC  (None, 4, 4, 1152)  10368       ['block_12_expand_relu[0][0]']
// onv2D)
//
// block_12_depthwise_BN (BatchNo  (None, 4, 4, 1152)  4608        ['block_12_depthwise[0][0]']
// rmalization)
//
// block_12_depthwise_relu (ReLU)  (None, 4, 4, 1152)  0           ['block_12_depthwise_BN[0][0]']
//
// block_12_project (Conv2D)      (None, 4, 4, 192)    221184      ['block_12_depthwise_relu[0][0]']
//
// block_12_project_BN (BatchNorm  (None, 4, 4, 192)   768         ['block_12_project[0][0]']
// alization)
//
// block_12_add (Add)             (None, 4, 4, 192)    0           ['block_11_add[0][0]',
//                                                                  'block_12_project_BN[0][0]']
//
// block_13_expand (Conv2D)       (None, 4, 4, 1152)   221184      ['block_12_add[0][0]']
//
// block_13_expand_BN (BatchNorma  (None, 4, 4, 1152)  4608        ['block_13_expand[0][0]']
// lization)
//
// block_13_expand_relu (ReLU)    (None, 4, 4, 1152)   0           ['block_13_expand_BN[0][0]']
//
// block_13_pad (ZeroPadding2D)   (None, 5, 5, 1152)   0           ['block_13_expand_relu[0][0]']
//
// block_13_depthwise (DepthwiseC  (None, 2, 2, 1152)  10368       ['block_13_pad[0][0]']
// onv2D)
//
// block_13_depthwise_BN (BatchNo  (None, 2, 2, 1152)  4608        ['block_13_depthwise[0][0]']
// rmalization)
//
// block_13_depthwise_relu (ReLU)  (None, 2, 2, 1152)  0           ['block_13_depthwise_BN[0][0]']
//
// block_13_project (Conv2D)      (None, 2, 2, 160)    184320      ['block_13_depthwise_relu[0][0]']
//
// block_13_project_BN (BatchNorm  (None, 2, 2, 160)   640         ['block_13_project[0][0]']
// alization)
//
// block_14_expand (Conv2D)       (None, 2, 2, 960)    153600      ['block_13_project_BN[0][0]']
//
// block_14_expand_BN (BatchNorma  (None, 2, 2, 960)   3840        ['block_14_expand[0][0]']
// lization)
//
// block_14_expand_relu (ReLU)    (None, 2, 2, 960)    0           ['block_14_expand_BN[0][0]']
//
// block_14_depthwise (DepthwiseC  (None, 2, 2, 960)   8640        ['block_14_expand_relu[0][0]']
// onv2D)
//
// block_14_depthwise_BN (BatchNo  (None, 2, 2, 960)   3840        ['block_14_depthwise[0][0]']
// rmalization)
//
// block_14_depthwise_relu (ReLU)  (None, 2, 2, 960)   0           ['block_14_depthwise_BN[0][0]']
//
// block_14_project (Conv2D)      (None, 2, 2, 160)    153600      ['block_14_depthwise_relu[0][0]']
//
// block_14_project_BN (BatchNorm  (None, 2, 2, 160)   640         ['block_14_project[0][0]']
// alization)
//
// block_14_add (Add)             (None, 2, 2, 160)    0           ['block_13_project_BN[0][0]',
//                                                                  'block_14_project_BN[0][0]']
//
// block_15_expand (Conv2D)       (None, 2, 2, 960)    153600      ['block_14_add[0][0]']
//
// block_15_expand_BN (BatchNorma  (None, 2, 2, 960)   3840        ['block_15_expand[0][0]']
// lization)
//
// block_15_expand_relu (ReLU)    (None, 2, 2, 960)    0           ['block_15_expand_BN[0][0]']
//
// block_15_depthwise (DepthwiseC  (None, 2, 2, 960)   8640        ['block_15_expand_relu[0][0]']
// onv2D)
//
// block_15_depthwise_BN (BatchNo  (None, 2, 2, 960)   3840        ['block_15_depthwise[0][0]']
// rmalization)
//
// block_15_depthwise_relu (ReLU)  (None, 2, 2, 960)   0           ['block_15_depthwise_BN[0][0]']
//
// block_15_project (Conv2D)      (None, 2, 2, 160)    153600      ['block_15_depthwise_relu[0][0]']
//
// block_15_project_BN (BatchNorm  (None, 2, 2, 160)   640         ['block_15_project[0][0]']
// alization)
//
// block_15_add (Add)             (None, 2, 2, 160)    0           ['block_14_add[0][0]',
//                                                                  'block_15_project_BN[0][0]']
//
// block_16_expand (Conv2D)       (None, 2, 2, 960)    153600      ['block_15_add[0][0]']
//
// block_16_expand_BN (BatchNorma  (None, 2, 2, 960)   3840        ['block_16_expand[0][0]']
// lization)
//
// block_16_expand_relu (ReLU)    (None, 2, 2, 960)    0           ['block_16_expand_BN[0][0]']
//
// block_16_depthwise (DepthwiseC  (None, 2, 2, 960)   8640        ['block_16_expand_relu[0][0]']
// onv2D)
//
// block_16_depthwise_BN (BatchNo  (None, 2, 2, 960)   3840        ['block_16_depthwise[0][0]']
// rmalization)
//
// block_16_depthwise_relu (ReLU)  (None, 2, 2, 960)   0           ['block_16_depthwise_BN[0][0]']
//
// block_16_project (Conv2D)      (None, 2, 2, 320)    307200      ['block_16_depthwise_relu[0][0]']
//
// block_16_project_BN (BatchNorm  (None, 2, 2, 320)   1280        ['block_16_project[0][0]']
// alization)
//
// Conv_1 (Conv2D)                (None, 2, 2, 640)    204800      ['block_16_project_BN[0][0]']
//
// Conv_1_bn (BatchNormalization)  (None, 2, 2, 640)   2560        ['Conv_1[0][0]']
//
// out_relu (ReLU)                (None, 2, 2, 640)    0           ['Conv_1_bn[0][0]']
//
// global_average_pooling2d (Glob  (None, 640)         0           ['out_relu[0][0]']
// alAveragePooling2D)
//
// predictions (Dense)            (None, 12)           7692        ['global_average_pooling2d[0][0]'
//                                                                 ]
//
//==================================================================================================
//Total params: 3,046,732
//Trainable params: 3,006,412
//Non-trainable params: 40,320
//__________________________________________________________________________________________________
//
