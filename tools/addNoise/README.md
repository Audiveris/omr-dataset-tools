# Add Noise tool
Add synthetic noise to the images for creating artificial data.

## Genesis
This tool was created as a part of a hack at Classical Music Hackday 2017, Salzburg. The main purpose of the hack was to set up an OMR dataset from synthetic score images generated from MuseScore symbolic data.

This tool is used for add artificial noise to the synthetic images generated from MuseScore containing musical symbols. Apart from the images, an xml file containing information about the symbols such as coordinates, name etc. will be provided.

## Required packages
- Python 2.7
- OpenCV 3.1
- Numpy (is a requirement for OpenCV)

### Problems with the installation ?
Following tutorials might be helpful in setting up OpenCV and python
- [Windows](http://docs.opencv.org/3.2.0/d5/de5/tutorial_py_setup_in_windows.html)
- [OS X](http://www.pyimagesearch.com/2015/06/15/install-opencv-3-0-and-python-2-7-on-osx/)
- [Ubuntu](http://www.pyimagesearch.com/2015/06/22/install-opencv-3-0-and-python-2-7-on-ubuntu/)
- [Fedora](http://docs.opencv.org/3.2.0/dd/dd5/tutorial_py_setup_in_fedora.html)


## How to use ?
In order to use this tool, the following command should be executed on the terminal. Make sure to provide the path to all the required files if they are not in working directory.

**python __main__.py** -i <*imageFile*\> -x <*xmlFile*\> -sp <*parameters*\> 
|-speckle <*parameters*\>
| -g <*parameters*\>
| -r <*parameters*\>.
<br>

##### Optional Arguments

- **-h** or **--help** show this help message and exit
- **-x XMLFILE** XML file which contain the annotaions
- **-o OUTPUTFOLDER** Output folder where the processed images should be saved

##### Required Arguments

1. **-i IMAGEFILE** Input image file to which noise should be added
2. One or more of the following arguments should be present:
    * **-sp** parameters: *saltVsPepperRatio, amount*.
    * **-speckle** parameters: *amount*
    * **-g** parameters: *mean, standardDeviation*
    * **-r** parameters: *angle*


**Example syntax** <br>
`python __main__.py -i path/imageFile.png -x path/xmlFile.xml -sp  0.1 0.05`
will take the input image `path/imageFile.png` and adds salt and pepper noise to it with 
*saltVsPepperRatio = 0.1* and *amount=0.05*. 
This image will be saved in the current working directory.
The noise information added to the image will be added in the `path/xmlFile.xml` 
<br><br>
`python __main__.py -i somePath/imageFile.png -sp -r 4`
will take the input image `somePath/imageFile.png` and adds salt and pepper noise to it with the default parameters.
This image will be saved in the current working directory.
An image rotated by *angle=4* will also be created in the working directory. 
The noise information added to the image will be added in the `somePath/xmlFile.xml`. 
<br><br>
Use `python __main__.py -h` to get the details about the parameters.

If both image file and the xml file have the same basename and are in the same directory,
 then you only need to specify the image file, i.e. `python __main__.py -i path/imageFile.png -sp`.