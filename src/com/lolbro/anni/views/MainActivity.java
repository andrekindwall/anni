package com.lolbro.anni.views;

import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.AutoParallaxBackground;
import org.andengine.entity.scene.background.ParallaxBackground.ParallaxEntity;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.input.touch.TouchEvent;
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
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.lolbro.anni.customs.ChaseCamera;

public class MainActivity extends SimpleBaseGameActivity implements IOnSceneTouchListener, IUpdateHandler {
	
	public static final int CAMERA_WIDTH = 720;
	public static final int CAMERA_HEIGHT = 480;
	
	/**
	 *  Physic laws constant, only used by player at the moment
	 */
	private static final FixtureDef FIXTURE_DEF = PhysicsFactory.createFixtureDef(1, 0f, 0.5f);
	
	private BitmapTextureAtlas mBackgroundTextureAtlas;
	private ITextureRegion mBackgroundLayerBack;
	
	private BitmapTextureAtlas mCharactersTextureAtlas;
	private TiledTextureRegion mPlayerTextureRegion;
	
	private Body mPlayerBody;
	
	private Scene mScene;
	private ChaseCamera mCamera;

	private PhysicsWorld mPhysicsWorld;
	
	private boolean mIsTouchingScene;
	private int mPlayerVelocity;
	
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
		mBackgroundLayerBack = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mBackgroundTextureAtlas, this, "scene_background.png", 0, 0);
		mBackgroundTextureAtlas.load();
		
		//Create texture atlas for mobs
		mCharactersTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 32, 64, TextureOptions.BILINEAR);
		mPlayerTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mCharactersTextureAtlas, this, "player.png", 0, 0, 1, 1);
		mCharactersTextureAtlas.load();
	}

	@Override
	protected Scene onCreateScene() {
		mEngine.registerUpdateHandler(new FPSLogger());
		
		final VertexBufferObjectManager vertexBufferObjectManager = getVertexBufferObjectManager();
		
		mScene = new Scene();
		
		//Register for touch events on the scene
		mScene.setOnSceneTouchListener(this);
		
		
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
		
		//Set the properties for our walls
		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0f, 0.5f);
		
		//Create a shape for the ground
		final Rectangle ground = new Rectangle(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH, 2, vertexBufferObjectManager);

		//Create physics/collision for the ground
		PhysicsFactory.createBoxBody(mPhysicsWorld, ground, BodyType.StaticBody, wallFixtureDef);
	
		//Make the ground visible in the scene
		mScene.attachChild(ground);

		// =====================================================================
		// PLAYER
		// =====================================================================
		
		//Create a sprite for our player
		AnimatedSprite playerSprite = new AnimatedSprite(CAMERA_WIDTH/2, CAMERA_HEIGHT/2, mPlayerTextureRegion, this.getVertexBufferObjectManager());
		
		//Create the player body and set its collision. We use BoxBody for now
		mPlayerBody = PhysicsFactory.createBoxBody(mPhysicsWorld, playerSprite, BodyType.DynamicBody, FIXTURE_DEF);	
		
		// Place the player in the scene
		mScene.attachChild(playerSprite);		
		

		// Connect the player to follow laws of physics in the world
		mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(playerSprite, mPlayerBody, true, true));

		
		// Set camera to follow player
		mCamera.setChaseEntity(playerSprite);
		
		
		mScene.registerUpdateHandler(mPhysicsWorld);
		
		return mScene;
	}

	@Override
	public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
		if(this.mPhysicsWorld != null) {
			switch(pSceneTouchEvent.getAction()){
			case TouchEvent.ACTION_DOWN:
			case TouchEvent.ACTION_MOVE:
				mIsTouchingScene = true;
				mPlayerVelocity = pSceneTouchEvent.getMotionEvent().getX() < mEngine.getSurfaceWidth() / 2 ? -2 : 2;
				break;
			default:
				mIsTouchingScene = false;
				break;
			}
		}
		return false;
	}

	@Override
	public void onUpdate(float pSecondsElapsed) {
		if(mIsTouchingScene){
			mPlayerBody.setLinearVelocity(mPlayerVelocity, mPlayerBody.getLinearVelocity().y);
		}
	}

	@Override
	public void reset() {
		
	}
	
}