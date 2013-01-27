package com.lolbro.anni.level;

import java.util.ArrayList;
import java.util.Random;

import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.constants.PhysicsConstants;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import android.content.Context;

import com.badlogic.gdx.physics.box2d.Body;
import com.lolbro.anni.physicseditor.PhysicsEditorShapeLibrary;
import com.lolbro.anni.views.MainActivity;


public class LevelSegmentManager {

	private PhysicsEditorShapeLibrary mLevelShapeLibrary;
	private ArrayList<Segment> mSegments;
	private Random mRandom;
	
	public LevelSegmentManager(Context context){
		mSegments = new ArrayList<Segment>();
		mRandom = new Random();
		
		mLevelShapeLibrary = new PhysicsEditorShapeLibrary();
        mLevelShapeLibrary.open(context, "shapes/segments.xml");
	}
	
	public void addSegment(String shapeName, ITextureRegion region) {
		mSegments.add(new Segment(shapeName, region, mSegments.size()));
	}
	
	public int size(){
		return mSegments.size();
	}
	
	public Segment get(int position){
		return mSegments.get(position);
	}
	
	public Segment getRandom() {
		int position = mRandom.nextInt(size());
		return get(position);
	}
	
	public Segment getRandom(int notPosition) {
		int position;
		while((position = mRandom.nextInt(size())) == notPosition);
		return get(position);
	}
	
	public void attachToWorld(VertexBufferObjectManager vertexBufferObjectManager, PhysicsWorld physicsWorld, Scene scene){
		for(int i=0; i<mSegments.size(); i++){
			Segment segment = mSegments.get(i);
			//Place all sprites on x=0 y=0, just below the camera
			Sprite sprite = new Sprite(0, 0, segment.getRegion(), vertexBufferObjectManager);
			Body body = mLevelShapeLibrary.createBody(segment.getShapeName(), sprite, physicsWorld);
			body.setUserData(MainActivity.FLOOR_USERDATA);
			scene.attachChild(sprite);
			
			segment.setSprite(sprite);
			segment.setBody(body);
		}
	}
	
	public class Segment{
		private Sprite sprite;
		private Body body;
		private String shapeName;
		private ITextureRegion region;
		private int index;
		
		private float worldCoordinatesWidth;
		
		public Segment(String shapeName, ITextureRegion region, int index){
			this.shapeName = shapeName;
			this.region = region;
			this.index = index;
			
			worldCoordinatesWidth = region.getWidth() / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT;
		}
		
		public void setBody(Body body) {
			this.body = body;
		}
		
		public void setSprite(Sprite sprite) {
			this.sprite = sprite;
		}
		
		public ITextureRegion getRegion() {
			return region;
		}
		
		public String getShapeName() {
			return shapeName;
		}
		
		public Body getBody() {
			return body;
		}
		
		public Sprite getSprite() {
			return sprite;
		}
		
		public int getIndex() {
			return index;
		}
		
		public float getWorldCoordinatesWidth() {
			return worldCoordinatesWidth;
		}
	}
	
	
}
