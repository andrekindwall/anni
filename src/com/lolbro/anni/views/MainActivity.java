package com.lolbro.anni.views;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.CameraFactory;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.AutoParallaxBackground;
import org.andengine.entity.scene.background.ParallaxBackground.ParallaxEntity;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;

public class MainActivity extends SimpleBaseGameActivity {
	
	private static final int CAMERA_WIDTH = 720;
	private static final int CAMERA_HEIGHT = 480;
	
	private BitmapTextureAtlas mBackgroundTexture;
	private ITextureRegion mBackgroundLayerBack;
	
	@Override
	public EngineOptions onCreateEngineOptions() {
		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		
		//TODO Decide if we should use new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT) instead of
		//new FillResolutionPolicy(), to ensure same ratio on all devices
		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new FillResolutionPolicy(), camera);
	}

	@Override
	protected void onCreateResources() {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
		
		mBackgroundTexture = new BitmapTextureAtlas(this.getTextureManager(), 1024, 1024);
		mBackgroundLayerBack = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mBackgroundTexture, this, "scene_background.png", 0, 188);
		mBackgroundTexture.load();
	}

	@Override
	protected Scene onCreateScene() {
		mEngine.registerUpdateHandler(new FPSLogger());
		
		final Scene scene = new Scene();
		final AutoParallaxBackground autoParallaxBackground = new AutoParallaxBackground(0, 0, 0, 5);
		final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();
		autoParallaxBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, CAMERA_HEIGHT - mBackgroundLayerBack.getHeight(), mBackgroundLayerBack, vertexBufferObjectManager)));
		scene.setBackground(autoParallaxBackground);
		
		return scene;
	}
	
}