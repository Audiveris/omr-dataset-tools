# -*- coding: utf-8 -*-
__author__ = 'Pulimootil'

"""
This script is a part of the addNoise tool.
It performs operations related to XML reading and writing.
"""

import shutil
import logging
from xml.etree.ElementTree import ElementTree, Element
from coordinatesManipulations import processBB

''' Copy XML file '''


def copyXML(xmlinput, xmlFilename):
    shutil.copy2(xmlinput, xmlFilename)
    logging.info(xmlFilename + ' created')


''' Replace the coordinates of the XML file after transformation '''


def replaceBBoxXML(filename, transform, mode):
    tree = ElementTree.parse(filename)
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
    tree = ElementTree.parse(filename)
    museScore = tree.getroot()
    deterioration = museScore.find('deterioration')
    if deterioration is None:
        # Create the element
        museScore.append(Element('deterioration'))
        # deterioration = tree.Element('deterioration')
        # museScore.insert(0, deterioration)

    # Pass the mode and parameter to the element


    # for tag in museScore:
    #
    #     # Check if the root already contains the tag
    #     if deterioration is None:
    #         print 'Does not contain deterioration tag. Creating...'
    #         museScore.insert(1,'test')
    tree.write(filename)
    ElementTree(tree).write(filename, encoding='utf-8')
    print 'Writing..'
