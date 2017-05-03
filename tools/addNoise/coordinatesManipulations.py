# -*- coding: utf-8 -*-
__author__ = 'Pulimootil'

"""
This script is a part of the addNoise tool.
It performs operations related to coordinates (and also Bounding boxes).
"""

import numpy as np
import cv2

''' Process a single Bounding Box '''


def processBB(bbox, transform):
    x = int(bbox[0])
    y = int(bbox[1])
    w = int(bbox[2])
    h = int(bbox[3])
    x, y, w, h = transformBB(x, y, w, h, transform)
    return x, y, w, h


''' Rotate a single bounding box '''


def transformBB(x, y, w, h, transformation):
    ## Making the coordinates
    coord1 = x, y
    coord2 = x, y + h
    coord3 = x + w, y + h
    coord4 = x + w, y
    ## Get the transformed Coordinates
    coord1 = getRotatedCoordinates(transformation, coord1)
    coord2 = getRotatedCoordinates(transformation, coord2)
    coord3 = getRotatedCoordinates(transformation, coord3)
    coord4 = getRotatedCoordinates(transformation, coord4)
    ## Making Countour from the transformed coordinates
    contour = [coord1, coord2, coord4, coord3]
    contour = np.array(contour).reshape((-1, 1, 2)).astype(np.int32)
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
    ptsTransformed = np.array([ptsTransformed[0, 0], ptsTransformed[1, 0]])
    return ptsTransformed.astype(int)
