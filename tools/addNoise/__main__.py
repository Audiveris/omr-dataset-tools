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
        python __main__.py path/imageFile.png path/XMLFile.xml
        python __main__.py path/imageFile.png (If both the filenames are the same and are located in the same folder)
"""

import sys
import os
import logging

from geometricTransformations import *
from morphologicalOperations import *
from xmlOperations import *
from coordinatesManipulations import transformBB

''' Output Dir '''
outputDir = 'output'

''' Read the image '''


def callImage(imgFile, color):
    if color:
        logging.info('Loading ' + imgFile + ' in Color mode.')
        img = cv2.imread(imgFile, 1)
    else:
        logging.info('Loading ' + imgFile + ' in Grayscale mode.')
        img = cv2.imread(imgFile, 0)
    return img


''' Display Image '''


def Display(orig, distorted, name):
    cv2.namedWindow("Original Image")
    cv2.imshow("Original Image", orig)

    cv2.namedWindow(name)
    cv2.imshow(name, distorted)

    cv2.waitKey(0)
    cv2.destroyAllWindows()


''' Gaussian Noise '''


def gaussian(img):
    logging.info('Adding Gaussian Noise')
    # img = callImage(file,0)
    row, col = img.shape
    noise = np.zeros((row, col), np.int8)
    meanSD = cv2.meanStdDev(img)
    cv2.randn(noise, 0, meanSD[1])  # zero mean
    return cv2.add(img, noise, dtype=cv2.CV_8UC3)


''' Salt and pepper Noise '''


def saltAndPepper(img):
    logging.info('Adding Salt and Pepper Noise')
    sp = 0.5  # salt and pepper ratio
    amount = 0.01
    out = img
    ## Salt mode
    numSalt = np.ceil(amount * img.size * sp)
    coords = [np.random.randint(0, i - 1, int(numSalt)) for i in img.shape]
    out[coords] = 255
    ## Pepper mode
    numPepper = np.ceil(amount * img.size * (1. - sp))
    coords = [np.random.randint(0, i - 1, int(numPepper)) for i in img.shape]
    out[coords] = 0
    return img


''' Speckle Noise '''


def speckle(img):
    logging.info('Adding Speckle Noise')
    row, col = img.shape
    gauss = np.random.randn(row, col)
    gauss = gauss.reshape(row, col)
    return cv2.add(img, img * gauss * 0.4, dtype=cv2.CV_8UC3)


''' Process Bounding boxes for display '''


def processBBDisplay(bboxes, transform, orgImg, rotImg):
    img = orgImg
    for bbox in bboxes:
        x = int(bbox[0])
        y = int(bbox[1])
        w = int(bbox[2])
        h = int(bbox[3])
        cv2.rectangle(img, (x, y), (x + w, y + h), (255, 100, 0), 1)
        x, y, w, h = transformBB(x, y, w, h, transform)
        cv2.rectangle(rotImg, (x, y), (x + w, y + h), (0, 0, 255), 1)
    Display(img, rotImg, "Rotated Image")


''' Main process '''


def main(argv):
    logging.basicConfig(filename='logFile.log',
                        level=logging.INFO,
                        format='%(asctime)s - %(levelname)s - %(funcName)s :: %(message)s',
                        filemode='w')  # Change filemode to append later

    ## Processing arguments
    if len(argv) == 3:
        imgFile = argv[1]
        xmlFile = argv[2]

    elif len(argv) == 2:
        dirName = os.path.dirname(os.path.realpath(argv[1]))
        imgFile = dirName + os.path.sep + os.path.basename(argv[1])
        xmlFile = dirName + os.path.sep + os.path.splitext(os.path.basename(argv[1]))[0] + '.xml'

    else:
        msg = 'Invalid number of Input arguments passed'
        logging.error(msg)
        raise msg

    logging.info('Detected Filename: ' + imgFile)
    logging.info('Detected Annotation File: ' + xmlFile)

    ## Checking for valid files
    if not os.path.isfile(imgFile):
        msg = 'Invalid Image file: ' + imgFile
        logging.error(msg)
        raise IOError(msg)
    if not os.path.isfile(xmlFile):
        msg = 'Invalid XML file: ' + xmlFile
        logging.error(msg)
        raise IOError(msg)

    ## Checking output directory
    if not os.path.exists(outputDir):
        os.makedirs(outputDir)

    ## Salt and Pepper Noise

    saltAndPepperDist = saltAndPepper(callImage(imgFile, 0))
    outputName = outputDir + os.path.sep + os.path.splitext(os.path.basename(argv[1]))[0] + '_salt_pepper'
    cv2.imwrite(outputName + '.png', saltAndPepperDist)
    copyXML(xmlFile, outputName + '.xml')
    # Display(callImage(imgFile, True), saltAndPepperDist, "Salt and Pepper Distortion")

    ## Gaussian Noise

    gaussianDist = gaussian(callImage(imgFile, 0))
    outputName = outputDir + os.path.sep + os.path.splitext(os.path.basename(argv[1]))[0] + '_gaussian'
    cv2.imwrite(outputName + '.png', gaussianDist)
    copyXML(xmlFile, outputName + '.xml')
    # Display(callImage(imgFile, True), gaussianDist, "Gaussian Distortion")

    ## Speckle: Multiplicative Noise

    speckleDist = speckle(callImage(imgFile, 0))
    outputName = outputDir + os.path.sep + os.path.splitext(os.path.basename(argv[1]))[0] + '_speckle'
    cv2.imwrite(outputName + '.png', speckleDist)
    copyXML(xmlFile, outputName + '.xml')
    # Display(callImage(imgFile, True), speckleDist, "Speckle Distortion")

    ## rotation: rotate by a specified angle
    rotImage, transform = rotate(callImage(imgFile, 1), 2)
    outputName = outputDir + os.path.sep + os.path.splitext(os.path.basename(argv[1]))[0] + '_rotate'
    copyXML(xmlFile, outputName + '.xml')
    cv2.imwrite(outputName + '.png', rotImage)
    bboxes = replace_XML(outputName + '.xml', transform)
    # processBBDisplay(bboxes, transform, callImage(imgFile, True), rotImage) # Display the rotated iamges with the coordinates

    # Radial distortion: straight lines will appear curved
    radDist = radialDistortion(callImage(imgFile, 0))
    # Display(callImage(imgFile, True), radDist, "Radial Distortion")
    '''To do: Write the new coordinates to the xml file'''

    # Tangential distortion : occurs because image taking lense is not aligned perfectly parallel to the imaging plane.
    tangDist = tangentialDistortion(callImage(imgFile, 0))
    # Display(callImage(imgFile, True), tangDist, "Tangential Distortion")
    '''To do: Write the new coordinates to the xml file'''

    # Localvar: Zero-mean Gaussian white noise with an intensity-dependent variance
    # Skew: measure of the asymmetry of the probability distribution of a real-valued random variable about its mean.

    # Morphological Closing to connect the close objects.
    # Opening us used instead of closing since the background is white and the foreground is black
    openImg = opening(callImage(imgFile, 0), size=3, SE='square')
    #Display(callImage(imgFile, True), openImg, "Opening Operation")
    outputName = outputDir + os.path.sep + os.path.splitext(os.path.basename(argv[1]))[0] + '_opening'
    cv2.imwrite(outputName + '.png', openImg)
    copyXML(xmlFile, outputName + '.xml')

    logging.info('----------------------------------------')


if __name__ == "__main__":
    main(sys.argv)
