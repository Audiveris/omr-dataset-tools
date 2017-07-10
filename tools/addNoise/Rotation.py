# -*- coding: utf-8 -*-


__author__ = 'Pulimootil'

"""
 Rotate the image
"""

import cv2

''' Rotation Class'''


class Rotation(object):
    def __init__(self, img, parameters):
        self.tag = "rotation"
        self.img = img
        self.checkParameters(parameters)

    ''' Add distortion to the image'''

    def addDistortion(self):
        angle = float(self.parameters['angle'])
        if len(self.img.shape) == 3:
            rows, cols, ch = self.img.shape
        else:
            rows, cols = self.img.shape
        M = cv2.getRotationMatrix2D((cols / 2, rows / 2), angle, 1)
        rot = cv2.warpAffine(self.img, M, (cols, rows), borderValue=[255, 255, 255])
        self.setTransformationMatrix(M)
        return rot

    ''' Check the passed parameters are valid'''

    def checkParameters(self, parameters):
        parameterStr = []
        if len(parameters) > 1:
            raise "Invalid number of Arguments"
        if len(parameters) == 1:
            parameterStr = [str(x) for x in parameters]
        elif len(parameters) == 0:
            parameterStr.append('1')
        self.parameters = {
            'angle': parameterStr[0]
        }

    '''Set the tranformation matrix '''

    def setTransformationMatrix(self, M):
        self.transformationMatrix = M
