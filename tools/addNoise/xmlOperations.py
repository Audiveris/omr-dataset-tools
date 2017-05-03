# -*- coding: utf-8 -*-
__author__ = 'Pulimootil'

"""
This script is a part of the addNoise tool.
It performs operations related to XML reading and writing.
"""

import shutil
import xml.etree.ElementTree as ET
from coordinatesManipulations import processBB

''' Copy XML file '''


def copyXML(xmlinput, xmlFilename):
    shutil.copy2(xmlinput, xmlFilename)


''' Replace the coordinates of the XML file after transformation '''


def replace_XML(filename, transform):
    tree = ET.parse(filename)
    museScore = tree.getroot()
    bboxes = []
    for symbol in museScore:
        for bbox in symbol:
            if bbox.tag == "bbox":
                x = bbox.attrib["x"]
                y = bbox.attrib["y"]
                w = bbox.attrib["w"]
                h = bbox.attrib["h"]
                bboxes.append([x, y, w, h])
                ## Rotate the bounding box
                x, y, w, h = processBB([x, y, w, h], transform)
                ## Replacing the old data
                bbox.set("x", str(x))
                bbox.set("y", str(y))
                bbox.set("w", str(w))
                bbox.set("h", str(h))
    tree.write(filename)
    return bboxes
