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
        python main.py path/imageFile.png path/XMLFile.xml
        python main.py path/imageFile.png (If both the filenames are the same and are located in the same folder)
"""

import numpy as np
import cv2
import xml.etree.ElementTree as ET
import shutil
import sys
import os

''' Output Dir '''
outputDir = 'output'

''' Copy XML file '''
def copyXML(xmlinput,xmlFilename):
    shutil.copy2(xmlinput, xmlFilename)

''' Read the image '''
def callImage(imgFile, color):
    if color:
        img = cv2.imread(imgFile,1)
    else:
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

''' Guassian Noise '''
def guassian(img):
    #img = callImage(file,0)
    row,col= img.shape
    noise = np.zeros((row, col), np.int8)
    meanSD = cv2.meanStdDev(img)
    cv2.randn(noise, 0, meanSD[1]) # zero mean
    return cv2.add(img, noise, dtype = cv2.CV_8UC3)

''' Salt and pepper Noise '''
def saltAndPepper(img):
    sp = 0.5 # salt and pepper ratio
    amount = 0.01
    out = img
    ## Salt mode
    numSalt = np.ceil(amount * img.size * sp)
    coords = [np.random.randint(0, i - 1, int(numSalt)) for i in img.shape]
    out[coords] = 255
    ## Pepper mode
    numPepper = np.ceil(amount* img.size * (1. - sp))
    coords = [np.random.randint(0, i - 1, int(numPepper)) for i in img.shape]
    out[coords] = 0
    return img

''' Speckle Noise '''
def speckle(img):
    row,col = img.shape
    gauss = np.random.randn(row,col)
    gauss = gauss.reshape(row,col)
    return cv2.add(img, img * gauss * 0.4, dtype = cv2.CV_8UC3)

''' Rotation '''
def rotate(img, angle):
    rows, cols, ch = img.shape
    M = cv2.getRotationMatrix2D((cols/2, rows/2), angle, 1)
    rot = cv2.warpAffine(img, M, (cols, rows), borderValue=[255,255,255])
    return rot, M

''' Replace the coordinates of the XML file after transformation '''
def replace_XML(filename, transform):
    tree = ET.parse(filename)
    museScore = tree.getroot()
    bboxes = []
    for symbol in museScore:
        for bbox in symbol:
            if bbox.tag == "bbox":
                x = bbox.attrib["x"]
                y = bbox.attrib["y"]
                w = bbox.attrib["w"]
                h = bbox.attrib["h"]
                bboxes.append([x, y, w, h])
                ## Rotate the bounding box
                x, y, w, h = processBB([x, y, w, h], transform)
                ## Replacing the old data
                bbox.set("x",str(x))
                bbox.set("y",str(y))
                bbox.set("w",str(w))
                bbox.set("h",str(h))
    tree.write(filename)
    return bboxes

''' Process a single Bounding Box '''
def processBB(bbox, transform):
    x = int(bbox[0])
    y = int(bbox[1])
    w = int(bbox[2])
    h = int(bbox[3])
    x, y, w, h = transformBB(x, y, w, h, transform)
    return x, y, w, h

''' Process Bounding boxes for display '''
def processBBDisplay(bboxes, transform, orgImg, rotImg):
    img = orgImg
    for bbox in bboxes:
        x = int(bbox[0])
        y = int(bbox[1])
        w = int(bbox[2])
        h = int(bbox[3])
        cv2.rectangle(img, (x,y),(x+w,y+h),(255,100,0),1)
        x, y, w, h = transformBB(x, y, w, h, transform)
        cv2.rectangle(rotImg, (x, y), (x + w, y + h), (0, 0, 255), 1)
    Display(img, rotImg, "Rotated Image")


''' Rotate a single bounding box '''
def transformBB(x, y, w, h, transformation):
    ## Making the coordinates
    coord1 = x, y
    coord2 = x, y + h
    coord3 = x + w, y + h
    coord4 = x + w, y
    ## Get teh transformed Coordinates
    coord1 = getRotatedCoordinates(transformation, coord1)
    coord2 = getRotatedCoordinates(transformation, coord2)
    coord3 = getRotatedCoordinates(transformation, coord3)
    coord4 = getRotatedCoordinates(transformation, coord4)
    ## Making Countour from the transformed coordinates
    contour = [coord1, coord2, coord4, coord3]
    contour = np.array(contour).reshape((-1,1,2)).astype(np.int32)
    ## Finding the minimum area rectangle of the contour
    rect = cv2.minAreaRect(contour)
    box = cv2.boxPoints(rect)
    box = np.int0(box)
    ## Finding the Coordinates of the minimum area rectangle
    minx = min([pts[0] for pts in box])
    maxx = max([pts[0] for pts in box])
    miny = min([pts[1] for pts in box])
    maxy = max([pts[1] for pts in box])
    return minx, miny, maxx - minx, maxy - miny

''' Return the rotated Coordinates '''
def getRotatedCoordinates(transformMatrix, points):
    points = np.append(points, 1)
    points = points.reshape((1, 3))
    ptsTransformed = np.matrix(transformMatrix) * np.matrix(points.T)
    ptsTransformed = np.array([ptsTransformed[0,0], ptsTransformed[1,0]])
    return ptsTransformed.astype(int)

''' Radial Distortion '''
def radialDistortion(img):
    h, w = img.shape[:2]
    cameraMatrix = np.matrix([[w,0,w/2],[0,h,h/2],[0,0,1]])
    dist = np.array([0.1, 0.1, 0 , 0, 0]) # Radial
    dst = cv2.undistort(img, cameraMatrix, dist)
    return dst

''' Tangential Distortion '''
def tangentialDistortion(img):
    h, w = img.shape[:2]
    cameraMatrix = np.matrix([[w,0,w/2],[0,h,h/2],[0,0,1]])
    dist = np.array([0, 0, 0.00, 0.05])  # Tangential
    dst = cv2.undistort(img, cameraMatrix, dist)
    return dst

''' Main process '''
def main(argv):
    ## Processing arguments
    if len(argv) == 3:
        imgFile = argv[1]
        xmlFile = argv[2]

    elif len(argv) == 2:
        dirName = os.path.dirname(os.path.realpath(argv[1]))
        imgFile = dirName + os.path.sep + os.path.basename(argv[1])
        xmlFile = dirName + os.path.sep + os.path.splitext(os.path.basename(argv[1]))[0] + '.xml'

    else:
        raise 'Invalid input arguments'

    ## Checking for valid files
    if not os.path.isfile(imgFile):
        raise IOError('Invalid Image file: ' + imgFile)
    if not os.path.isfile(xmlFile):
        raise IOError('Invalid XML file: ' + xmlFile)

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
    gaussianDist = guassian(callImage(imgFile, 0))
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
    radDist = radialDistortion(callImage(imgFile,0))
    #Display(callImage(imgFile, True), radDist, "Radial Distortion")
    '''To do: Write the new coordinates to the xml file'''


    # Tangential distortion : occurs because image taking lense is not aligned perfectly parallel to the imaging plane.
    tangDist = tangentialDistortion(callImage(imgFile, 0))
    #Display(callImage(imgFile, True), tangDist, "Tangential Distortion")
    '''To do: Write the new coordinates to the xml file'''

    # Localvar: Zero-mean Gaussian white noise with an intensity-dependent variance
    # Skew: measure of the asymmetry of the probability distribution of a real-valued random variable about its mean.
    # Morphological Closing to connect the close objects.


if __name__ == "__main__":
    main(sys.argv)
