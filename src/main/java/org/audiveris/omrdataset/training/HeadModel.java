//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        H e a d M o d e l                                       //
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
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code HeadModel}
 *
 * @author Hervé Bitteur
 */
public class HeadModel
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(HeadModel.class);

    //~ Instance fields ----------------------------------------------------------------------------
    protected final int inputDepth;

    protected final int inputWidth;

    protected final int inputHeight;

    protected final int numClasses;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Factory for HeadModel network instances.
     *
     * @param inputDepth  input number of channels (1 for gray, 3 for RGB)
     * @param inputWidth  input width
     * @param inputHeight input height
     * @param numClasses  number of classes to recognize
     */
    public HeadModel (int inputDepth,
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
    public MultiLayerNetwork create ()
    {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(1234)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .convolutionMode(ConvolutionMode.Truncate)
                .list()
                .layer(0, new ConvolutionLayer.Builder(5, 5)
                       .nIn(1)
                       .stride(1, 1)
                       .nOut(20)
                       .activation(Activation.IDENTITY)
                       .convolutionMode(ConvolutionMode.Same)
                       .build())
                .layer(1, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                       .kernelSize(2, 2)
                       .stride(2, 2)
                       .build())
                .layer(2, new ConvolutionLayer.Builder(5, 5)
                       .stride(1, 1)
                       .nOut(50)
                       .activation(Activation.IDENTITY)
                       .convolutionMode(ConvolutionMode.Same)
                       .build())
                .layer(3, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                       .kernelSize(2, 2)
                       .stride(2, 2)
                       .build())
                .layer(4, new DenseLayer.Builder().activation(Activation.RELU)
                       ///.nIn(800)
                       .nOut(500)
                       .build())
                .layer(5, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                       ///.nIn(500)
                       .nOut(numClasses)
                       .activation(Activation.SOFTMAX)
                       .build())
                .setInputType(InputType.convolutionalFlat(inputHeight, inputWidth, inputDepth))
                .build();

        System.out.println("--- conf ---");
        System.out.println(conf);

        InputType inputType = InputType.convolutionalFlat(inputHeight, inputWidth, inputDepth);
        ///System.out.println(conf.getMemoryReport(inputType));

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        ///System.out.println(model.summary(inputType));
        System.out.println(model.summary());

        return model;
    }

    public static void main (String[] args)
    {
        new HeadModel(1, 21, 21, 12).create();
    }
    //~ Inner Classes ------------------------------------------------------------------------------

}
