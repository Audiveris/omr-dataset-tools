# -*- coding: utf-8 -*-
__author__ = 'Pulimootil'

"""
This script is a part of the addNoise tool.
It performs operations related to Geometric Transformations.
"""

import numpy as np
import cv2
import logging

''' Radial Distortion '''


def radialDistortion(img):
    logging.info('Adding Radial Distortion')
    h, w = img.shape[:2]
    cameraMatrix = np.matrix([[w, 0, w / 2], [0, h, h / 2], [0, 0, 1]])
    dist = np.array([0.1, 0.1, 0, 0, 0])  # Radial
    dst = cv2.undistort(img, cameraMatrix, dist)
    return dst


''' Tangential Distortion '''


def tangentialDistortion(img):
    logging.info('Adding Tangential Distortion')
    h, w = img.shape[:2]
    cameraMatrix = np.matrix([[w, 0, w / 2], [0, h, h / 2], [0, 0, 1]])
    dist = np.array([0, 0, 0.00, 0.05])  # Tangential
    dst = cv2.undistort(img, cameraMatrix, dist)
    return dst
