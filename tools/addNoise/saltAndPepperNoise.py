# -*- coding: utf-8 -*-


__author__ = 'Pulimootil'

"""
This script will handle salt and pepper Noise
"""

import numpy as np

''' Salt and pepper Noise '''


class SaltAndPepperNoise(object):
    def __init__(self, img, parameters):
        self.tag = "saltAndPepperNoise"
        self.img = img
        self.checkParameters(parameters)

    ''' Add distortion to the image'''

    def addDistortion(self):
        sp = float(self.parameters['saltVsPepperRatio'])  # salt and pepper ratio
        amount = float(self.parameters['amount'])
        self.distorted = self.img
        ## Salt mode
        numSalt = np.ceil(amount * self.img.size * sp)
        coords = [np.random.randint(0, i - 1, int(numSalt)) for i in self.img.shape]
        self.distorted[coords] = 255
        ## Pepper mode
        numPepper = np.ceil(amount * self.img.size * (1. - sp))
        coords = [np.random.randint(0, i - 1, int(numPepper)) for i in self.img.shape]
        self.distorted[coords] = 0
        return self.distorted

    ''' Check the passed parameters are valid'''

    def checkParameters(self, parameters):

        parameterStr = []

        if len(parameters) > 2:
            raise "Invalid number of Arguments"

        if len(parameters) == 2:
            parameterStr = [str(x) for x in parameters]
            if parameters[0] >1:
                raise ValueError('Salt vs Pepper ratio should be in the range of 0 and 1')
        elif len(parameters) == 1:
            parameterStr = [str(x) for x in parameters]
            parameterStr.append('0.01')
        else:
            parameterStr.append('0.5')
            parameterStr.append('0.01')

        self.parameters = {
            'saltVsPepperRatio': parameterStr[0],
            'amount': parameterStr[1]
        }
