<h1> Facemesh </h1>


<h2> Getting started</h2>

- Download the OpenCV Android sdk from [https://sourceforge.net/projects/opencvlibrary/files/opencv-android/3.2.0/](here).
- In the Android Studio project, from the menu select file -> new -> import existing module
- Select the OpenCV sdk location (OpenCV-android-sdk/sdk/java)
- Add the imported module as a dependency:
    - Open Project structure
    - Select the dependencies tab
    - Select the '+' button -> add module dependency -> select the opencv module.
- Repeat the same steps with the dlib library obtained from [https://github.com/tzutalin/dlib-android](here.)

<h2> Components </h2>


<h3>  Third Part components </h3>

Facemesh utilises two c++ computer vision libraries for facial detection and facial landmark identification.

The first library, OpenCV, is used for it's implementation of the Viola-Jones facial detection algorithm.

The second library, dlib, handles the facial landmark identifications.

The jni folder (src/main/jni) contains native build steps along with c++ wrapper code to interact with with native openCV and Dlib libraries
and return information about face bounding boxes and landmark points.


<h4> Pose Detection </h4>

- This class handles the pose estimation calculations, given the face detection/landmark results.

<h4> SphereView </h4>
The SphereView package handles the OpenGL mask renderering and


<h4> Android components </h4>
The FloatingCameraWindow is a hovering imageView which renders a a cropped section of
the camera preview. The rendered mask is overlayed over this view.


