package com.lolbro.anni.views;

import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.AutoParallaxBackground;
import org.andengine.entity.scene.background.ParallaxBackground.ParallaxEntity;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;

import android.hardware.SensorManager;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.lolbro.anni.customs.ChaseCamera;
import com.lolbro.anni.customs.SwipeScene;
import com.lolbro.anni.customs.SwipeScene.SwipeListener;
import com.lolbro.anni.physicseditor.PhysicsEditorShapeLibrary;

public class MainActivity extends SimpleBaseGameActivity implements SwipeListener, IUpdateHandler {
	
	public static final int CAMERA_WIDTH = 720;
	public static final int CAMERA_HEIGHT = 480;
	
	private BitmapTextureAtlas mBackgroundTextureAtlas;
	private ITextureRegion mBackgroundLayerBack;
	
	private BitmapTextureAtlas mSegmentsTextureAtlas;
	private ITextureRegion mSegmentFloor1;
	
	private BitmapTextureAtlas mCharactersTextureAtlas;
	private TiledTextureRegion mPlayerTextureRegion;
	
	private Body mPlayerBody;

	private PhysicsEditorShapeLibrary physicsEditorShapeLibrary;
	private PhysicsEditorShapeLibrary levelShapeLibrary;
	private SwipeScene mScene;
	private ChaseCamera mCamera;
	private PhysicsWorld mPhysicsWorld;

    
	@Override
	public EngineOptions onCreateEngineOptions() {
		mCamera = new ChaseCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		//TODO Decide if we should use new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT) instead of
		//new FillResolutionPolicy(), to ensure same ratio on all devices
		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new FillResolutionPolicy(), mCamera);
	}

	@Override
	protected void onCreateResources() {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
		
		//Create atlas for background
		mBackgroundTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 1024, 512);
		mBackgroundLayerBack = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mBackgroundTextureAtlas, this, "scene_background.png", 0, 0); //720x480
		mBackgroundTextureAtlas.load();
		
		//Create atlas for level segments
		mSegmentsTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 1024, 256);
		mSegmentFloor1 = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mSegmentsTextureAtlas, this, "segment_floor_1.png", 0, 0); //640x119
		mSegmentsTextureAtlas.load();
		
		//Create texture atlas for mobs
		mCharactersTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 32, 64, TextureOptions.BILINEAR);
		mPlayerTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mCharactersTextureAtlas, this, "player.png", 0, 0, 1, 1); //32x64
		mCharactersTextureAtlas.load();
		
		//Create shape collision importer
		physicsEditorShapeLibrary = new PhysicsEditorShapeLibrary();
        physicsEditorShapeLibrary.open(this, "shapes/player.xml");
        
        levelShapeLibrary = new PhysicsEditorShapeLibrary();
        levelShapeLibrary.open(this, "shapes/segments.xml");
	}

	@Override
	protected Scene onCreateScene() {
		mEngine.registerUpdateHandler(new FPSLogger());
		
		final VertexBufferObjectManager vertexBufferObjectManager = getVertexBufferObjectManager();
		
		mScene = new SwipeScene();
		
		//Register for frame updates
		mScene.registerUpdateHandler(this);
		

		// =====================================================================
		// BACKGROUND AND WORLD
		// =====================================================================
		
		//Create background
		//Right now Parallax background is meaningless because we don't have a moving background
		final AutoParallaxBackground autoParallaxBackground = new AutoParallaxBackground(0, 0, 0, 5);
		autoParallaxBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, CAMERA_HEIGHT - mBackgroundLayerBack.getHeight(), mBackgroundLayerBack, vertexBufferObjectManager)));
		mScene.setBackground(autoParallaxBackground);
		
		//Create physic world and set gravity
		mPhysicsWorld = new PhysicsWorld(new Vector2(0, SensorManager.GRAVITY_NEPTUNE), false);
		
		
		
		// =====================================================================
		// SEGMENTS
		// =====================================================================
		
		//Create a sprite for the ground
		Sprite floorSegment = new Sprite(0, -mSegmentFloor1.getHeight(), mSegmentFloor1, vertexBufferObjectManager);
		
		//Create collision for the ground
		levelShapeLibrary.createBody("segment_floor_1", floorSegment, mPhysicsWorld);
		
		//Make the ground visible
		mScene.attachChild(floorSegment);
		
		
		
		// =====================================================================
		// PLAYER
		// =====================================================================
		
		//Create a sprite for our player
		AnimatedSprite playerSprite = new AnimatedSprite(0, -CAMERA_HEIGHT/2, mPlayerTextureRegion, this.getVertexBufferObjectManager());
		
		//Create the player body and set its collision
		mPlayerBody = physicsEditorShapeLibrary.createBody("player", playerSprite, mPhysicsWorld);
		
		// Place the player in the scene
		mScene.attachChild(playerSprite);		
		
		// Connect the player to follow laws of physics in the world
		mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(playerSprite, mPlayerBody, true, true));

		// Prevent player to rotate
		mPlayerBody.setFixedRotation(true);
		
		// Set camera to follow player
		mCamera.setChaseEntity(playerSprite);
		
		
		mScene.registerUpdateHandler(mPhysicsWorld);
		
		return mScene;
	}
	
	@Override
	public synchronized void onResumeGame() {
		super.onResumeGame();
		mScene.registerForGestureDetection(this, this);
	}

	@Override
	public void onUpdate(float pSecondsElapsed) {
		
	}
	
	private void jump() {
		mPlayerBody.setLinearVelocity(mPlayerBody.getLinearVelocity().x, -5);
	}

	//TODO Merge jump(), jumpLeft() and jumpRight() into jump(int direction)
	private void jumpLeft() {
		mPlayerBody.setLinearVelocity(-3.5f, -4f);
	}

	//TODO Merge jump(), jumpLeft() and jumpRight() into jump(int direction)
	private void jumpRight() {
		mPlayerBody.setLinearVelocity(3.5f, -4f);
	}
	
	@Override
	public void reset() {
		
	}

	@Override
	public void onSwipe(int direction) {
		if(Math.abs(mPlayerBody.getLinearVelocity().y) > 0.05f){
			return;
		}
		
		switch(direction){
		case SwipeListener.DIRECTION_UP:
			jump();
			break;
		case SwipeListener.DIRECTION_LEFT:
			jumpLeft();
			break;
		case SwipeListener.DIRECTION_RIGHT:
			jumpRight();
			break;
		}
	}
	
}