# -*- coding: utf-8 -*-


__author__ = 'Pulimootil'

"""
This script is used to introduce noise to synthetic images.
This script was made as a part of Classical Music Hackday 2017 at Salzburg, Austria.
Most of the parameters are hardcoded for the purpose of hackday.
Some of the distortions are not complete.

input:
        imageFile (containing musical symbols)
        xmlFile (containing the coordinates of these symbols)

output:
        imageFile_distortion (distorted image)
        newxmlFile (containing changed coordinates of musical symbols)

usage:
        python mainT.py path/imageFile.png path/XMLFile.xml -sp -speckle
        python mainT.py path/imageFile.png (If both the filenames are the same and are located in the same folder) -sp
"""

import argparse
from argparse import RawTextHelpFormatter
from saltAndPepperNoise import *
from speckleNoise import *
from gaussianNoise import *
from Rotation import *
from FileOperations import *
from xmlOperations import *

''' Main '''


def main():
    # argument parser
    parser = argparse.ArgumentParser(description='Add noise to the image', formatter_class=RawTextHelpFormatter)

    # input arg
    # Salt and pepper arguments
    required = parser.add_argument_group('required arguments', '')
    required.add_argument('-i', dest='imageFile', help='Input image file to which noise should be added', required=True)

    # xml arg
    parser.add_argument('-x', dest='xmlFile', help='XML file which contain the annotaions', required=False)

    # output folder arg
    parser.add_argument('-o', dest='outputFolder', help='Output folder where the processed images should be saved',
                        required=False)

    # Salt and pepper arguments
    group1 = parser.add_argument_group('Salt and Pepper Noise', 'Add salt and pepper noise to the image')
    group1.add_argument('-sp',
                        nargs='*',
                        type=float,
                        help='parameters :'
                             '[saltVsPepperRatio, amount]'
                             '\nsaltVsPepperRatio should be in the range of 0 and 1'
                             '\n%(prog)s default=[0.5, 0.01] ',
                        required=False)

    # Speckle Noise arguments
    group2 = parser.add_argument_group('Speckle Noise', 'Add speckle noise to the image')
    group2.add_argument('-speckle',
                        nargs='*',
                        type=float,
                        # default=[0.5, 0.01],
                        help='parameter : [amount] '
                             '\n%(prog)s default=[0.1] ',
                        required=False)

    # Gaussian Noise arguments
    group2 = parser.add_argument_group('Gaussian Noise', 'Add Gaussian noise to the image')
    group2.add_argument('-g',
                        nargs='*',
                        type=float,
                        help='parameter : [mean, standardDeviation]'
                             '\nMean and standard deviation of the random noise that should be added to the image'
                             '\n%(prog)s by default= Zero mean with standard deviation of the original image is used for the noise',
                        required=False)

    # Rotation arguments
    group2 = parser.add_argument_group('Rotation', 'Rotate the image by the angle specified')
    group2.add_argument('-r',
                        nargs='*',
                        type=float,
                        help='parameter : angle'
                             '\nThe rotation angle in degrees can be specified as parameter'
                             '\n%(prog)s by default= 1°',
                        required=False)

    args, leftovers = parser.parse_known_args()

    # Object to do file operations
    if args.xmlFile is None:
        args.xmlFile = os.path.splitext(args.imageFile)[0] + '.xml'

    file = FileOperatoins(args.imageFile, args.xmlFile)

    # Check for output folder
    if args.outputFolder is not None:
        file.setOutput(args.outputFolder)

    # Salt and Pepper Argument
    if args.sp is not None:
        # Salt and pepper object
        sp = SaltAndPepperNoise(file.getImage(0), args.sp)
        # Adding distortion to the image
        file.setDistortedImage(sp.addDistortion())
        # Set the output filename
        file.setOutputImageName('_' + sp.tag)
        # Write the image
        file.writeImage()
        # Write XML annotation
        addDeteriorationInXML(args.xmlFile, sp.tag, sp.parameters)

    # Speckle Noise Argument
    if args.speckle is not None:
        # Speckle object
        speckle = speckleNoise(file.getImage(0), args.speckle)
        # Adding distortion to the image
        file.setDistortedImage(speckle.addDistortion())
        # Set the output filename
        file.setOutputImageName('_' + speckle.tag)
        # Write the image
        file.writeImage()
        # Write XML annotation
        addDeteriorationInXML(args.xmlFile, speckle.tag, speckle.parameters)

    # Gaussian Noise Argument
    if args.g is not None:
        # Gaussian object
        gaussian = GaussianNoise(file.getImage(0), args.g)
        # Adding distortion to the image
        file.setDistortedImage(gaussian.addDistortion())
        # Set the output filename
        file.setOutputImageName('_' + gaussian.tag)
        # Write the image
        file.writeImage()
        # Write XML annotation
        addDeteriorationInXML(args.xmlFile, gaussian.tag, gaussian.parameters)

    # Rotation Argument
    if args.r is not None:
        # Rotation object
        rotation = Rotation(file.getImage(0), args.r)
        # Adding distortion to the image
        file.setDistortedImage(rotation.addDistortion())
        # Set the output filename
        file.setOutputImageName('_' + rotation.tag)
        # Write the image
        file.writeImage()
        # Write XML annotation
        addDeteriorationInXML(args.xmlFile, rotation.tag, rotation.parameters)
        # Update the coordinate information
        replaceBBoxXML(args.xmlFile, rotation.transformationMatrix, mode='rotation')


if __name__ == "__main__":
    main()
