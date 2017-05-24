# -*- coding: utf-8 -*-
__author__ = 'Pulimootil'

"""
This script is a part of the addNoise tool.
It performs operations related to XML reading and writing.
"""

import shutil
import logging
import xml.etree.ElementTree as ET
from coordinatesManipulations import processBB

''' Copy XML file '''


def copyXML(xmlinput, xmlFilename):
    shutil.copy2(xmlinput, xmlFilename)
    logging.info(xmlFilename + ' created')


''' Replace the coordinates of the XML file after transformation '''


def replaceBBoxXML(filename, transform, mode):
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
                x, y, w, h = processBB([x, y, w, h], transform, mode)
                ## Replacing the old data
                bbox.set("x", str(x))
                bbox.set("y", str(y))
                bbox.set("w", str(w))
                bbox.set("h", str(h))
    tree.write(filename)
    return bboxes


''' Edit/Add Deterioration information to the XML file'''


def deteriorationXML(filename, mode, parameters):
    # mode should contain the name of the noise string
    # Parameters should contain a dict of noise paramters

    tree = ET.parse(filename)
    museScore = tree.getroot()
    deterioration = museScore.find('deterioration')

    # Create deterioration node if not available in xml file
    if deterioration is None:
        print 'Not Found'
        child = ET.Element('deterioration')
        museScore.insert(2, child)

    # Adding parameters to deterioration
    det = museScore.find('deterioration')
    child = ET.Element(mode, attrib=parameters)
    det.insert(0, child)

    # Writing the new file
    tree.write(filename)
