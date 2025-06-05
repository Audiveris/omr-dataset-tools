//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      M o b i l e N e t V 2                                     //
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
import org.deeplearning4j.nn.conf.graph.ElementWiseVertex;
import org.deeplearning4j.nn.conf.graph.GraphVertex;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ActivationLayer;
import org.deeplearning4j.nn.conf.layers.BatchNormalization;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.DepthwiseConvolution2D;
import org.deeplearning4j.nn.conf.layers.GlobalPoolingLayer;
import org.deeplearning4j.nn.conf.layers.Layer;
import org.deeplearning4j.nn.conf.layers.LossLayer;
import org.deeplearning4j.nn.conf.layers.PoolingType;
import org.deeplearning4j.nn.conf.layers.ZeroPaddingLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.impl.LossMCXENT;

/**
 * Class <code>MobileNetV2</code> is derived from python MobileNet V2 architecture to provide
 * support for a general patch classifier.
 *
 * @author Hervé Bitteur
 */
public class MobileNetV2
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(MobileNetV2.class);

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
     * Factory for MobileNetV2 network instances.
     *
     * @param inputHeight input height
     * @param inputWidth  input width
     * @param inputDepth  input number of channels (1 for gray, 3 for RGB)
     * @param numClasses  number of classes to recognize
     */
    public MobileNetV2 (int inputHeight,
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
     * Create and initialize an instance of network.
     *
     * @param lossWeights vector of loss weights to be used
     * @return the initialized network
     */
    public OmrComputationGraph create (INDArray lossWeights)
    {
        // Define the graph configuration
        final WorkspaceMode workspaceMode = WorkspaceMode.ENABLED;
        builder = new NeuralNetConfiguration.Builder()
                .seed(1234)
                .activation(Activation.IDENTITY)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                // RmsProp defaults: (learningRate:1e-1, rmsDecay:0.95, epsilon:1e-8)
                //.updater(new RmsProp(0.1, 0.96, 0.001))
                ///.updater(new RmsProp(0.05))
                .updater(new Nesterovs(0.1))
                .weightInit(WeightInit.XAVIER)
                ///.weightInit(new WeightInitDistribution(new TruncatedNormalDistribution(0.0, 0.5)))
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

        x = invertedResBlock(x, 6, 16, 24, 2, 1);
        x = invertedResBlock(x, 6, 24, 24, 1, 2);

        x = invertedResBlock(x, 6, 24, 32, 2, 3);
        x = invertedResBlock(x, 6, 32, 32, 1, 4);
        x = invertedResBlock(x, 6, 32, 32, 1, 5);

        x = invertedResBlock(x, 3, 32, 64, 2, 6);
        x = invertedResBlock(x, 3, 64, 64, 1, 7);
        x = invertedResBlock(x, 6, 64, 64, 1, 8);
        x = invertedResBlock(x, 6, 64, 64, 1, 9);

        x = invertedResBlock(x, 6, 64, 96, 1, 10);
        x = invertedResBlock(x, 6, 96, 96, 1, 11);
        x = invertedResBlock(x, 6, 96, 96, 1, 12);

        x = invertedResBlock(x, 6, 96, 160, 2, 13);
        x = invertedResBlock(x, 6, 160, 160, 1, 14);
        x = invertedResBlock(x, 6, 160, 160, 1, 15);

        x = invertedResBlock(x, 6, 160, 320, 1, 16);

        // no alpha applied to last conv as stated in the paper:
        // if the width multiplier is greater than 1 we increase the number of output channels.
        //  if alpha > 1.0:
        //    last_block_filters = _make_divisible(1280 * alpha, 8)
        //  else:
        //    last_block_filters = 1280
        final int last_block_filters = (alpha > 1)
                ? make_divisible((int) Math.floor(1280 * alpha), 8) : 1280;

        //  x = layers.Conv2D(last_block_filters, kernel_size=1, use_bias=False, name='Conv_1')(x)
        x = addLayer("Conv_1",
                     new ConvolutionLayer.Builder()
                             .nOut(last_block_filters)
                             .kernelSize(1, 1)
                             .stride(1, 1)
                             .hasBias(false)
                             .build(),
                     x);

        //  x = layers.BatchNormalization(axis=channel_axis, epsilon=1e-3, momentum=0.999, name='Conv_1_bn')(x)
        x = addLayer("Conv_1_bn",
                     new BatchNormalization(),
                     x);

        //  x = layers.ReLU(6., name='out_relu')(x)
        x = addLayer("out_relu",
                     new ActivationLayer.Builder().activation(Activation.RELU6).build(),
                     x);

        //    x = layers.GlobalAveragePooling2D()(x)
        x = addLayer("global_average_pooling2d",
                     new GlobalPoolingLayer.Builder()
                             .poolingType(PoolingType.AVG)
                             .build(),
                     x);
        //    imagenet_utils.validate_activation(classifier_activation, weights)
        //    x = layers.Dense(classes, activation=classifier_activation, name='predictions')(x)
        x = addLayer("predictions",
                     new DenseLayer.Builder()
                             .activation(Activation.SOFTMAX)
                             .nOut(numClasses)
                             .build(),
                     x);
//
//        x = addLayer("predictions_loss",
//                     new LossLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
//                             .build(),
//                     x);

        x = addLayer("predictions_loss",
                     new LossLayer.Builder()
                             .lossFunction(new LossMCXENT(lossWeights))
                             .build(),
                     x);

        // This is the end...
        builder.setOutputs(x);

        // Build the network with defined configuration
        final OmrComputationGraph network = new OmrComputationGraph(getClass().getSimpleName(),
                                                                    builder.build());
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
        final int CONTEXT_HEIGHT = 96;
        final int CONTEXT_WIDTH = 48;
        final int CONTEXT_DEPTH = 1;
        final int NUM_CLASSES = 115;
        final INDArray weights = Nd4j.ones(NUM_CLASSES); // Dummy weights
        final ComputationGraph network = new MobileNetV2(CONTEXT_HEIGHT,
                                                         CONTEXT_WIDTH,
                                                         CONTEXT_DEPTH,
                                                         NUM_CLASSES).create(weights);

        System.out.println();
        System.out.println("*** MobileNetV2 ***");
        System.out.printf("CONTEXT_HEIGHT:%d, CONTEXT_WIDTH:%d, CONTEXT_DEPTH:%d, NUM_CLASSES:%d",
                          CONTEXT_HEIGHT, CONTEXT_WIDTH, CONTEXT_DEPTH, NUM_CLASSES);
        System.out.println();
        ///System.out.println(network.getConfiguration());
        final InputType inputType = InputType.convolutionalFlat(CONTEXT_HEIGHT,
                                                                CONTEXT_WIDTH,
                                                                CONTEXT_DEPTH);
        System.out.println(network.summary(inputType));
        System.out.println(network.getConfiguration().getMemoryReport(inputType));
    }
}
