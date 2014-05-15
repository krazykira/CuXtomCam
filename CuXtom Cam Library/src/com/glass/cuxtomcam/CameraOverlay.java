package com.glass.cuxtomcam;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CameraOverlay extends View {
	public enum Mode {PLAIN, RECORDING, FOCUS, DISABLED}
	
	private Mode mode = Mode.PLAIN;
	public CameraOverlay(Context context) {
		super(context);
	}
	
	public CameraOverlay(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		// nothing gets drawn :(
		Paint p = new Paint();

		float midX = canvas.getWidth()/2;
		float midY = canvas.getHeight()/2;
		float reticleSizeX = canvas.getWidth()/6;
		float reticleSizeY = canvas.getHeight()/6;
		
		float leftX = midX - reticleSizeX;
		float rightX = midX + reticleSizeX;
		float topY = midY - reticleSizeY;
		float bottomY = midY + reticleSizeY;
		
		if (this.mode == Mode.RECORDING) {
			p.setColor(Color.RED);
		    p.setStyle(Paint.Style.FILL);
		    float radius = 20;
		    float offset = 40;
		    canvas.drawCircle(offset + radius, offset+radius, radius, p);
		} else if (this.mode == Mode.DISABLED){
			p.setColor(Color.RED);
			p.setStrokeWidth(3);
			
			//Rectangle with an X through it
			canvas.drawRect(new RectF(leftX,topY,rightX,bottomY), p);
			canvas.drawLines(new float[]{leftX, topY, rightX, bottomY,
					rightX, topY, leftX, bottomY}, p);
		} else {
			p.setColor(this.mode == Mode.FOCUS ? Color.GREEN : Color.WHITE);
			p.setStrokeWidth(3);
			
			//two angle brackets, one in the top left corner, one in the bottom right
			canvas.drawLines(new float[]{leftX, topY, midX, topY,
					leftX, topY, leftX, midY,
					rightX, bottomY, midX, bottomY,
					rightX, bottomY, rightX, midY}, p);
		}
	}
	
	public void setMode(Mode mode) {
		this.mode = mode;
		this.invalidate();
	}
}
