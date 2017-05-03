# -*- coding: utf-8 -*-
__author__ = 'Pulimootil'

"""
This script is a part of the addNoise tool.
It performs morphological Operations.
"""
import cv2
import logging

''' Rotation '''


def rotate(img, angle):
    logging.info('Rotating the image.')
    rows, cols, ch = img.shape
    M = cv2.getRotationMatrix2D((cols / 2, rows / 2), angle, 1)
    rot = cv2.warpAffine(img, M, (cols, rows), borderValue=[255, 255, 255])
    return rot, M
