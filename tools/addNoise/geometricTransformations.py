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
    logging.info('input shape =' + img.shape + 'output shape =' + dst.shape)
    return dst, [cameraMatrix, dist]


''' Tangential Distortion '''


def tangentialDistortion(img):
    logging.info('Adding Tangential Distortion')
    h, w = img.shape[:2]
    cameraMatrix = np.matrix([[w, 0, w / 2], [0, h, h / 2], [0, 0, 1]])
    dist = np.array([0, 0, 0.00, 0.05])  # Tangential
    dst = cv2.undistort(img, cameraMatrix, dist)
    return dst, [cameraMatrix, dist]


''' Rotation '''


def rotate(img, angle):
    logging.info('Rotating the image.')
    rows, cols, ch = img.shape
    M = cv2.getRotationMatrix2D((cols / 2, rows / 2), angle, 1)
    rot = cv2.warpAffine(img, M, (cols, rows), borderValue=[255, 255, 255])
    return rot, M

''' Perspective Transform '''
def perspective(img):
    rows, cols, ch = img.shape
    pts1 = np.float32([[0, 0], [1000, 0], [0, 300], [300, 300]])
    pts2 = np.float32([[0, 0], [1000, 0], [0, 300], [300, 301]])
    M = cv2.getPerspectiveTransform(pts1, pts2)
    dst = cv2.warpPerspective(img, M, (cols, rows))
    return dst, M

