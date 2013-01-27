package com.lolbro.anni.customs;

import org.andengine.engine.camera.Camera;
import org.andengine.util.Constants;

import com.lolbro.anni.views.MainActivity;

public class ChaseCamera extends Camera {

	/**
	 *  Offset camera in X axis
	 */
	private final static int OFFSET_X = 200;
	private final static int yAxis = MainActivity.CAMERA_HEIGHT / 2 + 150;
	
	public ChaseCamera(float pX, float pY, float pWidth, float pHeight) {
		super(pX, pY, pWidth, pHeight);
	}

	@Override
	public void updateChaseEntity() {
		if(mChaseEntity != null) {
			final float[] centerCoordinates = this.mChaseEntity.getSceneCenterCoordinates();
			this.setCenter(centerCoordinates[Constants.VERTEX_INDEX_X] + OFFSET_X, yAxis);
		}
	}
	
}