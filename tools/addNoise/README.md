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

**python main.py** <*imageFile*\> <*xmlFile*\>.
<br>
For example `python main.py path/imageFile.png path/XMLFile.xml`

If both image file and the xml file have the same basename and are in the same directory, then you only need to specify the image file, i.e. `python main.py path/imageFile.png`.

### Note
- Most of the parameters are hard coded for the purpose of Hackday (Will be changed soon).
- Some of the distortion functions are not yet complete.
- More meaningful noise functions will be added.
- Features to combine different distortions are not yet implemented.
