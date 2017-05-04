# -*- coding: utf-8 -*-
__author__ = 'Pulimootil'

"""
This script is a part of the addNoise tool.
It performs morphological Operations.
"""
import cv2
import numpy as np
import logging

''' Opening '''


def opening(img, SE='square', size=2):
    if SE == 'square':
        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (size, size))
    elif SE == 'ellipse':
        kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (size, size))
    elif SE == 'cross':
        kernel = cv2.getStructuringElement(cv2.MORPH_CROSS, (size, size))
    elif SE == 'vline':
        kernel = np.ones((size,1))
    elif SE == 'hline':
        kernel = np.ones((1,size))

    logging.info('Kernel used for opening: \n' + np.array_str(kernel))
    open = cv2.morphologyEx(img, cv2.MORPH_OPEN, kernel)
    return open
