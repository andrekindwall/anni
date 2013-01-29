package com.lolbro.anni.customs;

import org.andengine.entity.scene.Scene;
import org.andengine.input.touch.detector.SurfaceGestureDetector;

import android.content.Context;

public class SwipeScene extends Scene {

	public static final int MIN_SWIPE_DISTANCE = 50;
	
	private SwipeListener listener;
	
	public interface SwipeListener{
		public static final int DIRECTION_UP = 1;
		public static final int DIRECTION_DOWN = 2;
		public static final int DIRECTION_LEFT = 3;
		public static final int DIRECTION_RIGHT = 4;
		
		public void onSwipe(int direction);
	}
	
	public void registerForGestureDetection(Context context, SwipeListener listener) {
		this.listener = listener;
		SurfaceGestureDetector gestureDetector = new SurfaceGestureDetector(context, MIN_SWIPE_DISTANCE) {
			@Override
			protected boolean onSwipeUp() {
				SwipeScene.this.listener.onSwipe(SwipeListener.DIRECTION_UP);
				return false;
			}
			
			@Override
			protected boolean onSwipeDown() {
				SwipeScene.this.listener.onSwipe(SwipeListener.DIRECTION_DOWN);
				return false;
			}
			
			@Override
			protected boolean onSwipeLeft() {
				SwipeScene.this.listener.onSwipe(SwipeListener.DIRECTION_LEFT);
				return false;
			}
			
			@Override
			protected boolean onSwipeRight() {
				SwipeScene.this.listener.onSwipe(SwipeListener.DIRECTION_RIGHT);
				return false;
			}
			
			@Override
			protected boolean onSingleTap() {
				return false;
			}
			
			@Override
			protected boolean onDoubleTap() {
				return false;
			}
		};
		
		setOnSceneTouchListener(gestureDetector);
	}
	
}
