# -*- coding: utf-8 -*-


__author__ = 'Pulimootil'

"""
This script will handle Gaussian Noise
"""

import numpy as np
import cv2

''' Gaussian Noise'''


class GaussianNoise(object):
    def __init__(self, img, parameters):
        self.tag = "gaussianNoise"
        self.img = img
        self.checkParameters(parameters)

    ''' Add distortion to the image'''

    def addDistortion(self):
        print self.parameters
        mean = float(self.parameters['mean'])  # salt and pepper ratio
        std = float(self.parameters['standardDeviation'])
        row, col = self.img.shape
        noise = np.zeros((row, col), np.int8)
        cv2.randn(noise, mean, std)
        return cv2.add(self.img, noise, dtype=cv2.CV_8UC3)

    ''' Check the passed parameters are valid'''

    def checkParameters(self, parameters):
        # Compute the meand and std of the original image
        meanSD = cv2.meanStdDev(self.img)
        parameterStr = []
        if len(parameters) > 2:
            raise "Invalid number of Arguments"
        if len(parameters) == 2:
            parameterStr = [str(x) for x in parameters]
        elif len(parameters) == 1:
            # default std
            parameterStr = [str(x) for x in parameters]
            parameterStr.append(str(meanSD[1][0][0]))
        else:
            # Default mean and std
            parameterStr.append('0')
            parameterStr.append(str(meanSD[1][0][0]))

        self.parameters = {
            'mean': parameterStr[0],
            'standardDeviation': parameterStr[1]
        }
