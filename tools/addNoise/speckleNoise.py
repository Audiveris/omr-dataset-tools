# -*- coding: utf-8 -*-


__author__ = 'Pulimootil'

"""
This script will handle Speckle Noise
"""

import numpy as np
import cv2

''' Speckle Noise '''


class speckleNoise(object):
    def __init__(self, img, parameters):
        self.tag = "speckleNoise"
        self.img = img
        self.checkParameters(parameters)

    ''' Add distortion to the image'''

    def addDistortion(self):
        amount = float(self.parameters['amount'])
        row, col = self.img.shape
        gauss = np.random.randn(row, col)
        gauss = gauss.reshape(row, col)
        return cv2.add(self.img, self.img * gauss * amount, dtype=cv2.CV_8UC3)

    ''' Check the passed parameters are valid'''

    def checkParameters(self, parameters):

        parameterStr = []

        if len(parameters) > 1:
            raise "Invalid number of Arguments"

        if len(parameters) == 1:
            parameterStr = [str(x) for x in parameters]
        elif len(parameters) == 0:
            parameterStr.append('0.01')

        self.parameters = {
            'amount': parameterStr[0]
        }
