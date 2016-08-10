CuXtomCam
=========
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-CuXtomCam-brightgreen.svg?style=flat-square)](http://android-arsenal.com/details/1/4085)

An open source camera for google glass. This is a an alternative to the default camera on Glass. CuXtom Cam provides you the default behaviour along with some special feature..

Special Features
--------------

* Use Swipe FORWARD and BACKWARD Gestures to perform Zoom In and Zoom out.
* During preview you can use TAP Gesture or Camera Button to take photo and end video recording.
* Take pictures, Record videos and save them in your own folder on Glass.
* There is no default 10 seconds video duration. Video can be recorded as long as the user wants. if user doesn't specify then default video recording length is 1 hour.


CuXtom Cam Glassware
--------------
Want to see CuXtomCam in action? Then check out [Google Glass Glassware] that is made using CuxtomCam library


Requirements
--------------
You can use cuXtom Cam library by adding it as library project. For a sample application see the [sample] app.

How To Use
--------------


**Start CuXtom Cam Activity like this**
```sh
private void startCuxtomCam() {
		String folder = Environment.getExternalStorageDirectory()
				+ File.separator + Environment.DIRECTORY_PICTURES
				+ File.separator + "MyFolder";
		Intent intent = new Intent(getApplicationContext(),
				CuxtomCamActivity.class);
		intent.putExtra(CuxtomIntent.CAMERA_MODE, CAMERA_MODE.VIDEO_MODE);
		intent.putExtra(CuxtomIntent.ENABLE_ZOOM, true);   // Enable zoom Gesture
		intent.putExtra(CuxtomIntent.FILE_NAME, "myvideo"); // No need for extensions
		intent.putExtra(CuxtomIntent.VIDEO_DURATION, 20);  // This duration is in seconds. Skipping this will record video for 1 hour
		intent.putExtra(CuxtomIntent.FOLDER_PATH, folder); // Set folder to save image and video
		startActivityForResult(intent, CUXTOM_CAM_REQUEST);

	}

```


**Recieve response from CuXtom Cam like this**
```sh
@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CUXTOM_CAM_REQUEST) {
			if (resultCode == RESULT_OK) {
				String path = data.getStringExtra(CuxtomIntent.FILE_PATH);
				int FIleType = data.getIntExtra(CuxtomIntent.FILE_TYPE,
						FILE_TYPE.PHOTO);
				if (FIleType == FILE_TYPE.PHOTO) {

				// Path points to Photo file
				} else {
				// Path points to Video file
				}

			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

```

**Don't forget to add the follwing lines in to your manifest.xml**
```sh
<uses-permission android:name="android.permission.CAMERA" />

    <uses-feature android:name="android.hardware.camera" />
    <uses-feature android:name="android.hardware.camera.autofocus" />

    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    
       <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:theme="@android:style/Theme.DeviceDefault" >
        <activity
            android:name="com.glass.cuxtomcam.CuxtomCamActivity"
            android:immersive="true" >
        </activity>
       </application>

```

For a detailed example check out the [sample] app 

Developed By
============

* Sheraz Ahmad Khilji - <sherazkhilji@gmail.com>


License
=======

    Copyright 2014 Sheraz Ahmad Khilji

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


**For those who use this library don't forget to tell me how was it.**

**HAPPY CODING ^_^**

[sample]:https://github.com/krazykira/CuXtomCam-App
[Google Glass Glassware]:https://glass.google.com/glassware/18006467403015279235

