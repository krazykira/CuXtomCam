package com.glass.cuxtomcam;

/**
 * This is invoked whenever camera is ready and its surface is created
 * 
 * @author Sheraz Ahmad Khilji <br>
 *         Developed by Virtual Force <br>
 *         {@link http://www.virtual-force.com/}<br>
 * <br>
 *         on Apr 1, 2014 at 3:23:10 PM
 */
interface CameraListener {
	/**
	 * This method is executed when the surfaceView of camera is created
	 */
	public void onCameraInit();

}
