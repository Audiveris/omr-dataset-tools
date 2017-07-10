# -*- coding: utf-8 -*-
__author__ = 'Pulimootil'

"""
This script is a part of the addNoise tool.
It performs operations related to coordinates.
"""

import numpy as np
import cv2

''' Process a single Bounding Box '''


def processBB(bbox, transform, mode):
    x = int(bbox[0])
    y = int(bbox[1])
    w = int(bbox[2])
    h = int(bbox[3])
    x, y, w, h = transformBB(x, y, w, h, transform, mode)
    return x, y, w, h


''' Return the distorted Coordinates '''


def getDistortedCoordinates(distCoefficients, points, center):
    # Finding the Euclidean distance between points and center
    r = np.linalg.norm(np.asarray(points) - np.asarray(center))
    radialFactor = 1 + distCoefficients[0] * r ** 2 + distCoefficients[1] * r ** 4 + distCoefficients[4] * r ** 6
    x = int(round(points[0] * radialFactor))
    y = int(round(points[1] * radialFactor))

    # points = np.append(points, 1)
    # points = points.reshape((1, 3))
    # ptsTransformed = np.matrix(transformMatrix) * np.matrix(points.T)
    # ptsTransformed = np.array([ptsTransformed[0, 0], ptsTransformed[1, 0]])
    # return ptsTransformed.astype(int)

    return x, y


''' Rotate a single bounding box '''


def transformBB(x, y, w, h, transform, mode):

    ## Making the coordinates
    coord1 = [x, y]
    coord2 = [x, y + h]
    coord3 = [x + w, y + h]
    coord4 = [x + w, y]
    ## to delete
    contour = np.array([coord1, coord2, coord4, coord3])
    ############

    if mode == 'rotation':

        ## Get the transformed Coordinates
        coord1 = getRotatedCoordinates(transform, coord1)
        coord2 = getRotatedCoordinates(transform, coord2)
        coord3 = getRotatedCoordinates(transform, coord3)
        coord4 = getRotatedCoordinates(transform, coord4)

    elif mode == 'distortion':
        def reshape(coord):
            return np.array((coord), dtype=np.float32, ndmin=3)

        coord1 = reshape(coord1)

        # Get the distorted Coordinates
        #coord1 = cv2.undistortPoints(coord1, transform[0], transform[1])

        pass

    ## to delete
    contour = [coord1, coord2, coord4, coord3]

    ############

    if mode == 'distortion':
        # print contour
        pass
    contour = np.array(contour).reshape((-1, 1, 2)).astype(np.int32)

    try:

        ## Finding the minimum area rectangle of the contour
        rect = cv2.minAreaRect(contour)
        box = cv2.boxPoints(rect)
        box = np.int0(box)
    except:

        print contour
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
