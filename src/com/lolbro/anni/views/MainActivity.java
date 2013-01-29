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
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.constants.PhysicsConstants;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;

import android.opengl.GLES20;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.lolbro.anni.customs.ChaseCamera;
import com.lolbro.anni.customs.SwipeScene;
import com.lolbro.anni.customs.SwipeScene.SwipeListener;
import com.lolbro.anni.debug.Box2dDebugRenderer;
import com.lolbro.anni.level.LevelSegmentManager;
import com.lolbro.anni.level.LevelSegmentManager.Segment;
import com.lolbro.anni.physicseditor.PhysicsEditorShapeLibrary;

public class MainActivity extends SimpleBaseGameActivity implements SwipeListener, IUpdateHandler, ContactListener {
	
	// =====================================================================
	// INSTANCE VARIABLES
	// =====================================================================
	
	public static final int CAMERA_WIDTH = 720;
	public static final int CAMERA_HEIGHT = 480;
	public static final float GRAVITY_VALUE = 60.00000f;
	
	public static final int JUMP_UP = 1;
	public static final int JUMP_DOWN = 2;
	public static final int JUMP_LEFT = 3;
	public static final int JUMP_RIGHT = 4;
	
	public static final String FLOOR_USERDATA = "segment_floor";
	
	private BitmapTextureAtlas mBackgroundTextureAtlas;
	private ITextureRegion mBackgroundLayerBack;
	
	private BitmapTextureAtlas mCharactersTextureAtlas;
	private TiledTextureRegion mPlayerTextureRegion;
	
	private AnimatedSprite mPlayerSprite;
	
	private LevelSegmentManager mSegmentManager;
	
	private Body mPlayerBody;
	private Body mCameraBody;
	
	private PhysicsEditorShapeLibrary mPhysicsEditorShapeLibrary;
	private SwipeScene mScene;
	private ChaseCamera mCamera;
	private PhysicsWorld mPhysicsWorld;
	
	private int mGroundContact = 0;
	private boolean swipeRight = false;
	private boolean swipeLeft = false;
	private short rollCounter = 0;
	
	private float mLastSegmentStartWorldPosition;
	private int mLastSegmentIndex;
	
	// =====================================================================
	// RANDOM SHIT
	// =====================================================================
	
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
		
		//Create texture atlas for characters
		mCharactersTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 128, 64, TextureOptions.BILINEAR);
		mPlayerTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mCharactersTextureAtlas, this, "player_tile.png", 0, 0, 4, 1); //128x64
		mCharactersTextureAtlas.load();
		
		//Create atlas for level segments
		BitmapTextureAtlas segmentsTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 1024, 512);
		ITextureRegion segmentFloorShort1 = BitmapTextureAtlasTextureRegionFactory.createFromAsset(segmentsTextureAtlas, this, "segment_floor_short_1.png", 0, 0); //1024x128
		ITextureRegion segmentFloorShort2 = BitmapTextureAtlasTextureRegionFactory.createFromAsset(segmentsTextureAtlas, this, "segment_floor_short_2.png", 512, 0); //1024x128
		ITextureRegion segmentFloorShort3 = BitmapTextureAtlasTextureRegionFactory.createFromAsset(segmentsTextureAtlas, this, "segment_floor_short_3.png", 0, 256); //1024x128
		segmentsTextureAtlas.load();
		
		mSegmentManager = new LevelSegmentManager(this);
		mSegmentManager.addSegment("segment_floor_short_1", segmentFloorShort1);
		mSegmentManager.addSegment("segment_floor_short_2", segmentFloorShort2);
		mSegmentManager.addSegment("segment_floor_short_3", segmentFloorShort3);
		
		//Create shape collision importer
		mPhysicsEditorShapeLibrary = new PhysicsEditorShapeLibrary();
        mPhysicsEditorShapeLibrary.open(this, "shapes/player.xml");
	}
	
	@Override
	protected Scene onCreateScene() {
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
		mPhysicsWorld = new PhysicsWorld(new Vector2(0, GRAVITY_VALUE), false);
		
		//Register for contact changes. Needed to check if user stands on ground
		mPhysicsWorld.setContactListener(this);
		
		// =====================================================================
		// DEBUG TOOLS
		// =====================================================================
		
//		activateBox2dRenderDebugging(vertexBufferObjectManager);
		
		//Print fps
//		final FPSCounter fpsCounter = new FPSCounter();
//        this.mEngine.registerUpdateHandler(fpsCounter);
//		HUD hud=new HUD();
//		Font font = FontFactory.create(this.getFontManager(), this.getTextureManager(), 256, 256, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 32);
//		font.load();
//		final Text text = new Text(10, 10, font, "FPS: ", 20, new TextOptions(HorizontalAlign.CENTER), vertexBufferObjectManager);
//		hud.attachChild(text);
//		mCamera.setHUD(hud);
//		mScene.registerUpdateHandler(new TimerHandler(1 / 20.0f, true, new ITimerCallback() {
//		                @Override
//		                public void onTimePassed(final TimerHandler pTimerHandler) {
//		                         text.setText("FPS: " + fpsCounter.getFPS());
//		        }
//		}));
		
		// =====================================================================
		// LEVEL SEGMENT MANAGER
		// =====================================================================
		
		//Place all segments in the scene
		mSegmentManager.attachToWorld(vertexBufferObjectManager, mPhysicsWorld, mScene);
		
		//Get a random segment and place it as the first segment in the level
		Segment segment = mSegmentManager.getRandom();
		placeSegmentAtPosition(segment, 0, false);
		
		
		// =====================================================================
		// PLAYER
		// =====================================================================
		
		//Create a sprite for our player
		mPlayerSprite = new AnimatedSprite(0, -CAMERA_HEIGHT/2 - 50, mPlayerTextureRegion, vertexBufferObjectManager);
		mPlayerSprite.animate(65);
		mPlayerSprite.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		
		//Create the player body and set its collision
		mPlayerBody = mPhysicsEditorShapeLibrary.createBody("player", mPlayerSprite, mPhysicsWorld);
		
		// Place the player in the scene
		mScene.attachChild(mPlayerSprite);
		
		//Keep Sprite and Body in sync
		mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(mPlayerSprite, mPlayerBody, true, false));

		// Prevent player to rotate
		mPlayerBody.setFixedRotation(true);
		

		// =====================================================================
		// CAMERA SPRITE
		// =====================================================================
		
		Sprite cameraSprite = new Sprite(0, -CAMERA_HEIGHT/2, mPlayerTextureRegion, vertexBufferObjectManager);
		
		mCameraBody = mPhysicsEditorShapeLibrary.createBody("camera", cameraSprite, mPhysicsWorld);
		
		mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(cameraSprite, mCameraBody, true, false));
		
		mCameraBody.setGravityScale(0);
		
		// Set camera to follow camera sprite
		mCamera.setChaseEntity(cameraSprite);
		
		mScene.registerUpdateHandler(mPhysicsWorld);
		
		return mScene;
	}
	
	// =====================================================================
	// THINGS TO HAPPEN ON EACH UPDATE
	// =====================================================================
	
	@Override
	public void onUpdate(float pSecondsElapsed) {
		
		if (mCameraBody.getLinearVelocity().x < 5) {
			mCameraBody.setLinearVelocity(5, mCameraBody.getLinearVelocity().y);
		}
		
		if (swipeRight == true) {
			if (rollCounter < 13) {
				rollCounter ++;
				mPlayerBody.setLinearVelocity(25, mPlayerBody.getLinearVelocity().y);
			} else if (mPlayerBody.getPosition().x > mCameraBody.getPosition().x) {
				mPlayerBody.setLinearVelocity(-15, mPlayerBody.getLinearVelocity().y);
			} else {
				rollCounter = 0;
				swipeRight = false;
			}
		} else if (swipeLeft == true) {
			if (rollCounter < 13) {
				rollCounter ++;
				mPlayerBody.setLinearVelocity(-15, mPlayerBody.getLinearVelocity().y);
			} else if (mPlayerBody.getPosition().x < mCameraBody.getPosition().x) {
				mPlayerBody.setLinearVelocity(25, mPlayerBody.getLinearVelocity().y);
			} else {
				rollCounter = 0;
				swipeLeft = false;
			}
			
		} else if (mPlayerBody.getPosition().x != mCameraBody.getPosition().x) {
			mPlayerBody.setTransform(mCameraBody.getPosition().x, mPlayerBody.getPosition().y, 0);
		}
		
		if (mPlayerBody.getPosition().x > mLastSegmentStartWorldPosition){
			Segment segment = mSegmentManager.getRandom(mLastSegmentIndex);
			placeSegmentAtPosition(segment, mLastSegmentStartWorldPosition + segment.getWorldCoordinatesWidth(), true);
		}
	}
	
	// =====================================================================
	// METHODS
	// =====================================================================
	
	private void jump(int direction){
		switch(direction){
		case JUMP_UP:
			mPlayerBody.setLinearVelocity(mPlayerBody.getLinearVelocity().x, -25);
			break;
		case JUMP_DOWN:
			mPlayerBody.setLinearVelocity(mPlayerBody.getLinearVelocity().x, 20);
			break;
		case JUMP_LEFT:
			mPlayerBody.setLinearVelocity(-3.5f, mPlayerBody.getLinearVelocity().y);
			break;
		case JUMP_RIGHT:
			mPlayerBody.setLinearVelocity(3.5f, mPlayerBody.getLinearVelocity().y);
			break;
		}
	}
	
	private void placeSegmentAtPosition(Segment segment, float startPositionX, boolean isWorldPosition) {
		float segmentHeight = segment.getRegion().getHeight();

		float worldStartPositionX;
		float worldCenterPositionY = (-segmentHeight / 2) / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT;

		float scenePositionX;
		float scenePositionY = -segmentHeight;
		
		if(isWorldPosition){
			worldStartPositionX = startPositionX;
			scenePositionX = startPositionX * PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT;
		} else {
			worldStartPositionX = startPositionX / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT;
			scenePositionX = startPositionX;
		}
		float worldCenterPositionX = worldStartPositionX + segment.getWorldCoordinatesWidth() / 2;
		
		segment.getSprite().setX(scenePositionX);
		segment.getSprite().setY(scenePositionY);
		
		segment.getBody().setTransform(worldCenterPositionX, worldCenterPositionY, 0);
		
		mLastSegmentStartWorldPosition = Math.max(mLastSegmentStartWorldPosition, worldStartPositionX);
		mLastSegmentIndex = segment.getIndex();
	}
	
	@SuppressWarnings("unused")
	private void activateBox2dRenderDebugging(VertexBufferObjectManager vertexBufferObjectManager) {
		mScene.attachChild(new Box2dDebugRenderer(mPhysicsWorld, vertexBufferObjectManager));
	}

	@Override
	public synchronized void onResumeGame() {
		super.onResumeGame();
		mScene.registerForGestureDetection(this, this);
	}
	
	@Override
	public synchronized void onPauseGame() {
		super.onPauseGame();
		
	}
	
	@Override
	public void reset() {
		
	}
	
	private boolean touchingGround() {
		return mGroundContact > 0;
	}

	@Override
	public void onSwipe(int direction) {
		
		switch(direction){
		case SwipeListener.DIRECTION_UP:
			if(touchingGround()){				
				jump(JUMP_UP);
			}
			break;
		case SwipeListener.DIRECTION_LEFT:
			swipeLeft = true;
			jump(JUMP_LEFT);
			break;
		case SwipeListener.DIRECTION_RIGHT:
			swipeRight = true;
			jump(JUMP_RIGHT);
			break;
		case SwipeListener.DIRECTION_DOWN:
			if(touchingGround() == false){				
				jump(JUMP_DOWN);
			}
			break;
		}
		
	}

	@Override
	public void beginContact(Contact contact) {
		if(contact.getFixtureB().getBody() == mPlayerBody && contact.getFixtureA().getBody().getUserData().equals(FLOOR_USERDATA)){
			mGroundContact++;
		}
	}

	@Override
	public void endContact(Contact contact) {
		if(contact.getFixtureB().getBody() == mPlayerBody && contact.getFixtureA().getBody().getUserData().equals(FLOOR_USERDATA)){
			mGroundContact--;
		}
	}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
		
	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
		
	}
	
}