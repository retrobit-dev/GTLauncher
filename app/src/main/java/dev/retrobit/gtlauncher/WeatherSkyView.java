package dev.retrobit.gtlauncher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/** Full-screen, non-scrolling pixel-art sky driven by local astronomy and weather data. */
public final class WeatherSkyView extends View {
    private static final int SCENE_WIDTH = 320;
    private static final int SCENE_HEIGHT = 200;
    // A 2x backing bitmap makes half-scene-unit clusters land on crisp source pixels.
    private static final int RENDER_SCALE = 2;
    private static final int RENDER_WIDTH = SCENE_WIDTH * RENDER_SCALE;
    private static final int RENDER_HEIGHT = SCENE_HEIGHT * RENDER_SCALE;
    private static final int ISLAND_GRID_SIZE = 9;
    private static final int ISLAND_TILE_WIDTH = 28;
    private static final String ISLAND_COLUMN_LABELS = "ABCDEFGHI";
    // Logical grid IDs are A1–I9: columns A–I left to right, rows 1–9 top to bottom.
    // Diamond compass frame: top=N, upper-right edge=NE, right=E, lower-right edge=SE,
    // bottom=S, lower-left edge=SW, left=W, and upper-left edge=NW.
    private static final float ISLAND_CAMERA_ELEVATION_DEGREES = 25.0f;
    // Keep a fixed 45° azimuth and project ground depth from the chosen camera elevation.
    private static final float ISLAND_TILE_HEIGHT = ISLAND_TILE_WIDTH
            * (float) Math.sin(Math.toRadians(ISLAND_CAMERA_ELEVATION_DEGREES));
    // Interior tile faces stay shallow; exposed perimeter faces extend through the grass band.
    private static final float ISLAND_TILE_SIDE_DEPTH = 2.5f;
    // Explicit side-wall bands: 8 units of grass above 16 units of exposed soil.
    private static final int ISLAND_GRASS_BAND_DEPTH = 8;
    private static final int ISLAND_SOIL_BAND_DEPTH = 16;
    // The green cap overhangs the narrower soil body along the front perimeter.
    private static final float ISLAND_GRASS_OVERHANG = 4.0f;
    private static final int ISLAND_DEPTH = ISLAND_GRASS_BAND_DEPTH + ISLAND_SOIL_BAND_DEPTH;
    private static final int ISLAND_BASE_BOTTOM_MARGIN = 8;
    private static final int ISLAND_VERTICAL_LIFT = 22;
    private static final int ISLAND_BOTTOM_MARGIN = ISLAND_BASE_BOTTOM_MARGIN + ISLAND_VERTICAL_LIFT;
    private static final float ISLAND_TOP = SCENE_HEIGHT - ISLAND_BOTTOM_MARGIN - ISLAND_DEPTH
            - ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT;
    // Cloud bases meet the island's top plane at the upper-half visual horizon.
    private static final double CLOUD_VISUAL_HORIZON_Y = SCENE_HEIGHT * 0.5;
    // A square-root curve gives low-altitude clouds more visible vertical separation.
    private static final double CLOUD_VISUAL_ALTITUDE_EXPONENT = 0.50;
    // Keep celestial centers in the upper third with enough margin for the Sun's glow.
    private static final float CELESTIAL_BAND_TOP = SCENE_HEIGHT * 0.10f;
    private static final float CELESTIAL_BAND_HEIGHT = SCENE_HEIGHT / 3.0f
            - CELESTIAL_BAND_TOP;
    // The Sun's outer halo reaches about 18 scene units from its center.
    private static final float SUN_GLOW_SAFE_INSET = 20.0f;
    // Temporarily hide sky objects while retaining their renderer for later design iterations.
    private static final boolean DRAW_SKY_OBJECTS = false;
    // The island's lowest point rests above this cloud sea, which fills the sky beneath it.
    // Three static sprite rows build the sea; side and front banks overlap the island.
    private static final float CLOUD_SEA_BASE = SCENE_HEIGHT - ISLAND_BOTTOM_MARGIN;
    private static final float CLOUD_SEA_MARGIN = 34.0f;
    private static final float CLOUD_SEA_TOP = CLOUD_SEA_BASE + 4.0f;
    private static final float CLOUD_HAZE_TOP = CLOUD_SEA_BASE - 54.0f;
    // Clouds are static for now: no drift and no drag parallax.
    // The sea rises until its crowns meet the island's underside, so the island reads as
    // resting on the clouds instead of hovering over a gap.
    private static final CloudLayer[] CLOUD_LAYERS = {
            new CloudLayer(CLOUD_SEA_BASE + 8.0f, 28.0f, 0.90f, 0.55f,
                    Color.rgb(182, 200, 218), 11, 1.0f),
            new CloudLayer(CLOUD_SEA_BASE + 22.0f, 36.0f, 1.0f, 0.26f,
                    Color.rgb(201, 217, 231), 27, 1.04f),
            new CloudLayer(CLOUD_SEA_BASE + 38.0f, 46.0f, 1.0f, 0.0f,
                    Color.rgb(221, 233, 243), 43, 1.10f)
    };
    private static final int CLOUD_NEAR_TINT = Color.rgb(238, 244, 250);
    // Nothing is an exact mirror of its opposite: heights, widths and tints all vary, so the
    // cloud walls read as natural puffs rather than a symmetrical pattern.
    private static final CloudBank[] CLOUD_GAP_FILLERS = {
            new CloudBank(0.335f, 148.0f, 46.0f, 0.06f, true),
            new CloudBank(0.672f, 144.0f, 52.0f, 0.04f, false),
            new CloudBank(0.418f, 154.0f, 44.0f, 0.03f, false),
            new CloudBank(0.600f, 150.0f, 40.0f, 0.05f, true)
    };
    // Each flank is a stack of overlapping clouds rather than one scaled blob, so the lobes
    // stay visible and the wall of cloud still reads as many separate puffs.
    private static final CloudBank[] CLOUD_SIDE_BANKS = {
            new CloudBank(0.045f, 132.0f, 84.0f, 0.12f, false),
            new CloudBank(0.118f, 116.0f, 92.0f, 0.07f, true),
            new CloudBank(0.172f, 142.0f, 68.0f, 0.05f, true),
            new CloudBank(0.072f, 102.0f, 78.0f, 0.16f, false),
            new CloudBank(0.958f, 128.0f, 80.0f, 0.11f, true),
            new CloudBank(0.877f, 122.0f, 86.0f, 0.06f, false),
            new CloudBank(0.812f, 146.0f, 64.0f, 0.04f, true),
            new CloudBank(0.936f, 108.0f, 72.0f, 0.15f, true)
    };
    // The cradle holds the island: these crowns sit directly under its soil slab and hide
    // the lowest tip, which is what makes the island look settled on the cloud tops.
    // The cradle holds the island: every crest sits a fixed gap under the slab underside,
    // so the cloud silhouette parallels the two lower edges and the tip looks wedged in
    // cloud instead of pasted on. Heights and tints vary so it is not a copied outline.
    // A continuous ridge of crowns with bridge puffs between them, so the clouds that touch
    // the slab read as one mass instead of a row of separate balls. Heights follow a smooth
    // arc and the whole row sits four units lower than a bare hug would, which keeps the
    // exposed soil band at its original width.
    private static final CloudBank[] CLOUD_CRADLE_BANKS = {
            new CloudBank(0.235f, 118.0f, 30.0f, 0.10f, true),
            new CloudBank(0.293f, 125.9f, 33.4f, 0.10f, false),
            new CloudBank(0.351f, 133.8f, 36.4f, 0.10f, true),
            new CloudBank(0.409f, 142.0f, 38.7f, 0.10f, false),
            new CloudBank(0.467f, 150.3f, 39.8f, 0.10f, true),
            new CloudBank(0.525f, 151.5f, 39.8f, 0.10f, false),
            new CloudBank(0.583f, 143.2f, 38.7f, 0.10f, true),
            new CloudBank(0.641f, 135.0f, 36.4f, 0.10f, false),
            new CloudBank(0.699f, 127.1f, 33.4f, 0.10f, true),
            new CloudBank(0.757f, 119.2f, 30.0f, 0.10f, false),
            new CloudBank(0.264f, 124.4f, 35.4f, 0.10f, false),
            new CloudBank(0.322f, 132.7f, 38.0f, 0.10f, false),
            new CloudBank(0.380f, 141.1f, 40.1f, 0.10f, false),
            new CloudBank(0.438f, 149.6f, 41.5f, 0.10f, false),
            new CloudBank(0.496f, 158.2f, 42.0f, 0.10f, false),
            new CloudBank(0.554f, 150.8f, 41.5f, 0.10f, false),
            new CloudBank(0.612f, 142.3f, 40.1f, 0.10f, false),
            new CloudBank(0.670f, 133.9f, 38.0f, 0.10f, false),
            new CloudBank(0.728f, 125.6f, 35.4f, 0.10f, false),
    };
    // Only this frontmost group drifts: a very slow sway and bob, so the scene breathes without
    // the island's framing shifting around.
    // Only this group drifts. Bridge puffs between the crowns keep the foreground from
    // reading as separate blobs while the sway and bob stay exactly as they were.
    private static final CloudBank[] CLOUD_FRONT_BANKS = {
            new CloudBank(0.100f, 150.0f, 74.0f, 0.07f, false, 2.6f, 0.8f, 0.075f, 0.4f),
            new CloudBank(0.895f, 156.0f, 66.0f, 0.07f, true, 2.2f, 0.7f, 0.062f, 2.1f),
            new CloudBank(0.497f, 160.0f, 52.8f, 0.07f, true, 2.2f, 0.7f, 0.062f, 3.1f),
            new CloudBank(0.238f, 162.0f, 58.0f, 0.07f, true, 3.1f, 0.9f, 0.088f, 4.0f),
            new CloudBank(0.567f, 166.0f, 46.4f, 0.07f, false, 3.1f, 0.9f, 0.088f, 5.0f),
            new CloudBank(0.782f, 168.0f, 62.0f, 0.07f, false, 2.4f, 0.6f, 0.070f, 5.3f),
            new CloudBank(0.510f, 172.0f, 49.6f, 0.07f, true, 2.4f, 0.6f, 0.070f, 6.3f),
            new CloudBank(0.516f, 174.0f, 46.0f, 0.07f, false, 3.4f, 1.0f, 0.055f, 1.2f),
            new CloudBank(0.649f, 178.0f, 36.8f, 0.07f, false, 3.4f, 1.0f, 0.055f, 2.2f),
    };
    // Distant clouds behind the island, hovering a little above the middle of the screen. The
    // island covers their centre, so they show as a scattered far layer in the gaps beside it.
    // Far layer: crests echo the island's upper edges (highest under its tip, sinking
    // toward the frame edges) with uneven jitter, so the island reads as resting inside a
    // cloud basin instead of sitting in open sky. These drift very slowly as well: far
    // clouds move far less than the front ones. The last value is the bank's alpha.
    private static final CloudBank[] CLOUD_BACK_BANKS = {
            new CloudBank(0.005f, 104.0f, 42.0f, 0.46f, true,
                    1.00f, 0.40f, 0.010f, 0.0f, 0.64f),
            new CloudBank(0.050f, 96.0f, 32.0f, 0.43f, false,
                    1.25f, 0.55f, 0.011f, 0.7f, 0.64f),
            new CloudBank(0.095f, 75.0f, 36.0f, 0.39f, true,
                    1.50f, 0.70f, 0.012f, 1.4f, 0.64f),
            new CloudBank(0.145f, 67.0f, 28.0f, 0.46f, false,
                    1.75f, 0.40f, 0.014f, 2.1f, 0.64f),
            new CloudBank(0.200f, 50.0f, 32.0f, 0.43f, true,
                    1.00f, 0.55f, 0.015f, 2.8f, 0.50f),
            new CloudBank(0.265f, 52.0f, 26.0f, 0.39f, false,
                    1.25f, 0.70f, 0.010f, 3.5f, 0.50f),
            new CloudBank(0.335f, 44.0f, 30.0f, 0.46f, true,
                    1.50f, 0.40f, 0.011f, 4.2f, 0.50f),
            new CloudBank(0.410f, 49.0f, 26.0f, 0.43f, false,
                    1.75f, 0.55f, 0.012f, 4.9f, 0.50f),
            new CloudBank(0.490f, 42.0f, 28.0f, 0.39f, true,
                    1.00f, 0.70f, 0.014f, 5.6f, 0.50f),
            new CloudBank(0.565f, 48.0f, 26.0f, 0.46f, false,
                    1.25f, 0.40f, 0.015f, 6.3f, 0.50f),
            new CloudBank(0.640f, 43.0f, 30.0f, 0.43f, true,
                    1.50f, 0.55f, 0.010f, 7.0f, 0.50f),
            new CloudBank(0.710f, 52.0f, 26.0f, 0.39f, false,
                    1.75f, 0.70f, 0.011f, 7.7f, 0.50f),
            new CloudBank(0.775f, 53.0f, 32.0f, 0.46f, true,
                    1.00f, 0.40f, 0.012f, 8.4f, 0.50f),
            new CloudBank(0.835f, 69.0f, 28.0f, 0.43f, false,
                    1.25f, 0.55f, 0.014f, 9.1f, 0.50f),
            new CloudBank(0.895f, 75.0f, 36.0f, 0.39f, true,
                    1.50f, 0.70f, 0.015f, 9.8f, 0.64f),
            new CloudBank(0.950f, 94.0f, 32.0f, 0.46f, false,
                    1.75f, 0.40f, 0.010f, 10.5f, 0.64f),
            new CloudBank(0.995f, 104.0f, 44.0f, 0.43f, true,
                    1.00f, 0.55f, 0.011f, 11.2f, 0.64f),
    };
    // Blanket crowns that climb high enough to cover the outermost tiles on every side, so the
    // cloud really does embrace the ground slab instead of stopping at its edge.
    private static final CloudBank[] CLOUD_BLANKET_BANKS = {
            new CloudBank(0.020f, 118.0f, 92.0f, 0.14f, false),
            new CloudBank(0.098f, 126.0f, 84.0f, 0.08f, true),
            new CloudBank(0.262f, 152.0f, 72.0f, 0.05f, true),
            new CloudBank(0.986f, 122.0f, 88.0f, 0.12f, false),
            new CloudBank(0.906f, 132.0f, 80.0f, 0.07f, false),
            new CloudBank(0.742f, 156.0f, 68.0f, 0.04f, true)
    };
    private static final String CLOUD_BANK_SPRITE = "cloud-bank";
    private static final String CLOUD_MASS_SPRITE = "cloud-mass";
    // Night cools the clouds, storm cover deepens them, and low sun warms the crests.
    private static final int CLOUD_NIGHT_TINT = Color.rgb(51, 67, 106);
    private static final int CLOUD_STORM_TINT = Color.rgb(101, 117, 140);
    private static final int CLOUD_WARM_TINT = Color.rgb(255, 226, 189);
    private static final int CLOUD_TINT_CACHE_LIMIT = 12;
    // This stylized 9 x 9 tile surface represents a 2.25 km square ground patch.
    private static final double ISLAND_TILE_METERS = 250.0;
    private static final float ISLAND_CENTER_Y = ISLAND_TOP + ISLAND_TILE_HEIGHT / 2.0f
            + (ISLAND_GRID_SIZE - 1) * ISLAND_TILE_HEIGHT / 2.0f;
    private static final double CLOUD_BASE_MIN_METERS = 800.0;
    private static final int CLOUD_BASE_RANGE_METERS = 9201;
    private static final double CLOUD_MIN_BASE_ALTITUDE_GAP_METERS = 200.0;
    private static final double CLOUD_MIN_SCREEN_ALTITUDE_GAP = 2.55;
    private static final int CLOUD_ALTITUDE_SAMPLE_ATTEMPTS = 256;
    private static final double CLOUD_WIDTH_MIN_METERS = 1200.0;
    private static final int CLOUD_WIDTH_RANGE_METERS = 4401;
    private static final double CLOUD_FIELD_MIN_RADIUS_METERS = 500.0;
    private static final double CLOUD_FIELD_RADIUS_RANGE_METERS = 44500.0;
    private static final double CLOUD_FIELD_HALF_LENGTH_METERS = 60000.0;
    private static final int CLOUD_RADIUS_RANDOM_SALT = 34;
    private static final int CLOUD_ALTITUDE_RANDOM_SALT = 2;
    private static final int CLOUD_WIDTH_RANDOM_SALT = 3;
    private static final int CLOUD_DEPTH_RANDOM_SALT = 4;
    private static final int CLOUD_BEARING_RANDOM_SALT = 1;
    private static final int CLOUD_SHAPE_RANDOM_SALT = 0;
    private static final int CLOUD_ART_SIZE_RANDOM_SALT = 5;
    // Rendering advances cloud advection at 24 simulated seconds per animation second.
    private static final double CLOUD_SIMULATION_RATE = 24.0;
    private static final double CLOUD_VERTICAL_ART_EXAGGERATION = 1.5;
    private static final double CLOUD_HORIZONTAL_ART_EXAGGERATION = 3.0;
    private static final float CLOUD_MIN_SCREEN_WIDTH = 52.0f;
    private static final float CLOUD_MAX_SCREEN_WIDTH = 64.0f;
    private static final float CLOUD_LAYOUT_GAP = 5.0f;
    private static final float CLOUD_MIN_SCREEN_HEIGHT = 18.0f;
    private static final float CLOUD_MAX_SCREEN_HEIGHT = 23.0f;
    private static final float CLOUD_SCREEN_TOP_MARGIN = 8.0f;
    private static final double CLOUD_SHADOW_FOOTPRINT_SCALE = 0.52;
    private static final double CLOUD_SHADOW_MAX_SPRITE_RATIO = 0.90;
    private static final double ISLAND_BODY_SHADOW_SCALE = 2.0;
    private static final double ISLAND_BODY_SHADOW_OPACITY_SCALE = 0.40;
    private static final float ISLAND_BODY_SHADOW_RISE = 3.0f;
    private static final double CLOUD_SHADOW_MIN_SUN_ALTITUDE = 3.0;
    private static final double CLOUD_SHADOW_CLEAR_OPACITY = 0.10;
    private static final double CLOUD_SHADOW_COVER_OPACITY = 0.08;
    private static final double CLOUD_SHADOW_STORM_BONUS = 0.02;
    private static final double CLOUD_SHADOW_MAX_OPACITY = 0.20;
    private static final double CLOUD_SHADOW_FALLBACK_MIN_OPACITY = 0.16;
    private static final int[] NIGHT_SKY = colors("#111a35", "#1d2948", "#35415d", "#514c65");
    private static final int[] DAWN_SKY = colors("#425783", "#7183a7", "#c79aa5", "#f0bd88");
    private static final int[] SUNSET_SKY = colors("#39486e", "#6e6f91", "#d38d84", "#f5b573");
    private static final int[] DAY_SKY = colors("#4c7fa5", "#73a5b8", "#abc7c1", "#e5d4b6");
    private static final SkyAtmosphere NIGHT_ATMOSPHERE =
            new SkyAtmosphere(Color.rgb(70, 91, 132), 0.18);
    private static final SkyAtmosphere TWILIGHT_ATMOSPHERE =
            new SkyAtmosphere(Color.rgb(205, 148, 136), 0.11);
    private static final SkyAtmosphere OVERCAST_ATMOSPHERE =
            new SkyAtmosphere(Color.rgb(119, 143, 154), 0.12);
    private static final SkyAtmosphere DAY_ATMOSPHERE =
            new SkyAtmosphere(Color.rgb(218, 226, 207), 0.07);
    // Tall cumulus silhouettes: a broad base, with one or two higher rounded lobes.
    private static final String[][] CLOUD_SHAPES = {
            {"000000111000000", "000001111100000", "000011111110000", "000111111111000",
                    "001111111111100", "011111111111110", "111111111111111", "111111111111111",
                    "111111111111111", "111111111111111"},
            {"000111000000000", "001111100000000", "011111110000000", "011111111100000",
                    "111111111111000", "111111111111110", "111111111111111", "111111111111111",
                    "111111111111111", "111111111111111"},
            {"000000000111000", "000000001111100", "000000011111110", "000001111111110",
                    "000111111111111", "011111111111111", "111111111111111", "111111111111111",
                    "111111111111111", "111111111111111"},
            {"000111000111000", "001111101111100", "011111111111110", "111111111111111",
                    "111111111111111", "111111111111111", "111111111111111", "111111111111111",
                    "111111111111111", "111111111111111"},
            {"000001110000000", "000011111000000", "000111111100000", "001111111110000",
                    "011111111111000", "111111111111100", "111111111111111", "111111111111111",
                    "111111111111111", "111111111111111"}
    };

    private final Paint paint = new Paint();
    private final Paint islandPolyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint renderBitmapPaint = new Paint();
    private final Path shapePath = new Path();
    private final Path cloudShadowPath = new Path();
    private final Path islandSurfacePath = new Path();
    private final Path islandPolyPath = new Path();
    private final Path islandBodyPath = new Path();
    private final float[] islandBodyVertices = new float[12];
    private final RectF renderedSceneBounds = new RectF();
    private final long animationStartMillis = System.currentTimeMillis();
    private Bitmap sceneBitmap;
    private Canvas sceneCanvas;
    private WeatherSnapshot weather;
    private double latitude = -6.7097;
    private double longitude = 111.3476;
    private float headingDegrees;
    private boolean animationEnabled = true;
    private Raindrop[] raindrops = new Raindrop[0];
    private final SparseArray<Bitmap> cloudSprites = new SparseArray<>();
    private final Map<String, Bitmap> cloudTintCache = new LinkedHashMap<>();
    private final Paint cloudPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Matrix cloudMatrix = new Matrix();

    public WeatherSkyView(Context context) {
        super(context);
        initialize();
    }

    public WeatherSkyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public WeatherSkyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        paint.setStyle(Paint.Style.FILL);
        islandPolyPaint.setAntiAlias(true);
        islandPolyPaint.setDither(true);
        islandPolyPaint.setStrokeJoin(Paint.Join.ROUND);
        islandPolyPaint.setStrokeCap(Paint.Cap.ROUND);
        renderBitmapPaint.setAntiAlias(false);
        renderBitmapPaint.setFilterBitmap(false);
        renderBitmapPaint.setDither(false);
        sceneBitmap = Bitmap.createBitmap(RENDER_WIDTH, RENDER_HEIGHT, Bitmap.Config.ARGB_8888);
        sceneCanvas = new Canvas(sceneBitmap);
        setContentDescription("Langit pixel art dan pulau terapung yang mengikuti waktu dan cuaca");
    }

    public void setWeatherSnapshot(WeatherSnapshot snapshot) {
        if (snapshot == null) return;
        weather = snapshot;
        latitude = snapshot.latitude;
        longitude = snapshot.longitude;
        rebuildWeatherParticles(snapshot);
        postInvalidateOnAnimation();
    }

    public void setLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        weather = null;
        raindrops = new Raindrop[0];
        postInvalidateOnAnimation();
    }

    public void setHeadingDegrees(float headingDegrees) {
        this.headingDegrees = (float) normalizeDegrees(headingDegrees);
        postInvalidateOnAnimation();
    }

    public void setAnimationEnabled(boolean enabled) {
        animationEnabled = enabled;
        if (enabled) postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() <= 0 || getHeight() <= 0) return;

        long now = System.currentTimeMillis();
        double elapsedSeconds = (now - animationStartMillis) / 1000.0;
        double motionSeconds = animationEnabled ? elapsedSeconds : 0.0;
        SkyMath.Position sun = SkyMath.sun(now, latitude, longitude);
        SkyMath.Position moon = DRAW_SKY_OBJECTS ? SkyMath.moon(now, latitude, longitude) : null;
        double cloudCover = weather == null ? 42.0 : weather.cloudCoverPercent;
        int code = weather == null ? 3 : weather.weatherCode;
        CloudState[] cloudStates = DRAW_SKY_OBJECTS
                ? createCloudStates(motionSeconds, code) : new CloudState[0];

        Canvas scene = sceneCanvas;
        int save = scene.save();
        scene.scale(RENDER_SCALE, RENDER_SCALE);
        drawSky(scene, sun, cloudCover, code);
        int upperHalfSave = scene.save();
        scene.clipRect(0, 0, SCENE_WIDTH, SCENE_HEIGHT * 0.5f);
        if (DRAW_SKY_OBJECTS) {
            drawSun(scene, sun, motionSeconds);
            drawClouds(scene, cloudStates, sun.altitudeDegrees, code);
            drawStars(scene, now, sun.altitudeDegrees, cloudCover, motionSeconds);
            drawMoon(scene, moon, sun.altitudeDegrees, cloudCover, motionSeconds);
        }
        drawFog(scene, code);
        drawStormFlash(scene, elapsedSeconds, code);
        scene.restoreToCount(upperHalfSave);
        drawFloatingIsland(scene, sun.altitudeDegrees, sun.azimuthDegrees,
                cloudCover, code, cloudStates);
        if (DRAW_SKY_OBJECTS) {
            drawSunOverIsland(scene, sun, motionSeconds);
            drawCloudsOverIsland(scene, cloudStates, sun.altitudeDegrees, code);
            drawCelestialsOverIsland(scene, now, sun, moon, cloudCover, motionSeconds);
        }
        drawPrecipitation(scene, elapsedSeconds);
        scene.restoreToCount(save);

        renderedSceneBounds.set(0, 0, getWidth(), getHeight());
        canvas.drawBitmap(sceneBitmap, null, renderedSceneBounds, renderBitmapPaint);

        if (animationEnabled && getVisibility() == VISIBLE) {
            postInvalidateDelayed(40);
        }
    }

    private void drawSky(Canvas canvas, SkyMath.Position sun, double cloudCover, int code) {
        double sunAltitude = sun.altitudeDegrees;
        double sunAzimuth = sun.azimuthDegrees;
        int[] palette;
        if (sunAltitude < -12.0) {
            palette = NIGHT_SKY;
        } else if (sunAltitude < 2.0) {
            palette = sunAzimuth > 180.0 ? SUNSET_SKY : DAWN_SKY;
        } else {
            palette = DAY_SKY;
        }

        double daylight = clamp((sunAltitude + 14.0) / 28.0, 0.0, 1.0);
        if (sunAltitude < -12.0) daylight = 0.0;
        else if (sunAltitude > 2.0) daylight = 1.0;

        int bandHeight = 2;
        for (int y = 0; y < SCENE_HEIGHT; y += bandHeight) {
            float amount = y / (float) SCENE_HEIGHT;
            int upper = paletteColor(palette, amount);
            int overcast = Color.rgb(
                    interpolate(55, 130, amount),
                    interpolate(68, 140, amount),
                    interpolate(84, 151, amount));
            double coverMix = clamp(cloudCover / 100.0 * 0.58, 0.0, 0.58);
            if (code >= 95) coverMix = Math.max(coverMix, 0.38);
            int skyColor = blend(upper, overcast, coverMix);
            if (daylight < 0.35 && code >= 61) {
                skyColor = blend(skyColor, Color.rgb(27, 34, 53), 0.16);
            }
            paint.setColor(skyColor);
            canvas.drawRect(0, y, SCENE_WIDTH, y + bandHeight, paint);
        }

        if (DRAW_SKY_OBJECTS && sunAltitude > -5 && sunAltitude < 12) {
            float horizonX = mapSunToUpperEdgeX(sun.hourAngleDegrees);
            float horizonY = mapSunToUpperEdgeY(sun.hourAngleDegrees);
            horizonX = Math.round(horizonX * RENDER_SCALE) / (float) RENDER_SCALE;
            horizonY = Math.round(horizonY * RENDER_SCALE) / (float) RENDER_SCALE;
            paint.setColor(Color.argb(32, 255, 189, 131));
            canvas.drawRect(horizonX - 42, horizonY - 7, horizonX + 42, horizonY + 7, paint);
            paint.setColor(Color.argb(22, 255, 211, 156));
            canvas.drawRect(horizonX - 70, horizonY - 3, horizonX + 70, horizonY + 3, paint);
        }
    }

    private void drawPixelRing(Canvas canvas, float centerX, float centerY,
                               float radius, float thickness, int color) {
        float cell = 1.0f / RENDER_SCALE;
        float outerRadius = radius + thickness / 2.0f;
        float innerRadius = Math.max(0.0f, radius - thickness / 2.0f);
        float snappedX = Math.round(centerX * RENDER_SCALE) / (float) RENDER_SCALE;
        float snappedY = Math.round(centerY * RENDER_SCALE) / (float) RENDER_SCALE;
        int rows = (int) Math.ceil(outerRadius / cell);
        paint.setColor(color);
        for (int row = -rows; row <= rows; row++) {
            float sampleY = (row + 0.5f) * cell;
            if (Math.abs(sampleY) >= outerRadius) continue;
            float outerHalf = (float) Math.sqrt(outerRadius * outerRadius - sampleY * sampleY);
            float outerLeft = (float) Math.floor((snappedX - outerHalf) / cell) * cell;
            float outerRight = (float) Math.ceil((snappedX + outerHalf) / cell) * cell;
            float top = snappedY + row * cell;
            if (innerRadius > 0.0f && Math.abs(sampleY) < innerRadius) {
                float innerHalf = (float) Math.sqrt(innerRadius * innerRadius - sampleY * sampleY);
                float innerLeft = (float) Math.floor((snappedX - innerHalf) / cell) * cell;
                float innerRight = (float) Math.ceil((snappedX + innerHalf) / cell) * cell;
                if (innerLeft > outerLeft) {
                    canvas.drawRect(outerLeft, top, innerLeft, top + cell, paint);
                }
                if (outerRight > innerRight) {
                    canvas.drawRect(innerRight, top, outerRight, top + cell, paint);
                }
            } else {
                canvas.drawRect(outerLeft, top, outerRight, top + cell, paint);
            }
        }
    }

    private void drawGlowPulse(Canvas canvas, float centerX, float centerY,
                               double elapsedSeconds, double phaseOffset,
                               float maxRadius, float intensity, int color) {
        if (intensity <= 0.0f) return;
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        for (int pulseIndex = 0; pulseIndex < 2; pulseIndex++) {
            float phase = wrap(elapsedSeconds * 0.13 + phaseOffset + pulseIndex * 0.5, 1.0);
            float radius = 0.75f + phase * maxRadius;
            float alpha = intensity * (float) Math.pow(1.0f - phase, 1.5);
            if (alpha < 0.008f) continue;
            int pulseColor = Color.argb(Math.round(alpha * 255.0f), red, green, blue);
            drawPixelRing(canvas, centerX, centerY, radius, 1.25f, pulseColor);
        }
    }

    private void drawSun(Canvas canvas, SkyMath.Position sun, double elapsedSeconds) {
        if (sun.altitudeDegrees <= -1.0) return;
        int centerX = Math.round(mapSunToUpperEdgeX(sun.hourAngleDegrees));
        int centerY = Math.round(mapSunToUpperEdgeY(sun.hourAngleDegrees));
        float pulse = 0.5f + 0.5f * (float) Math.sin(elapsedSeconds * 0.34
                + Math.toRadians(sun.azimuthDegrees));
        // Preserve the existing warm disc glow and its safe-inset footprint.
        drawPixelDisc(canvas, centerX, centerY, 16, 0.5f,
                Color.argb(Math.round(12 + pulse * 4), 255, 216, 152));
        drawPixelDisc(canvas, centerX, centerY, 12, 0.5f,
                Color.argb(Math.round(24 + pulse * 8), 255, 226, 170));
        drawGlowPulse(canvas, centerX, centerY, elapsedSeconds, 0.0,
                17.0f, 0.30f, Color.rgb(255, 225, 158));

        double sunsetWarmth = clamp((8.0 - sun.altitudeDegrees) / 20.0, 0.0, 1.0);
        int sunBase = blend(Color.rgb(241, 183, 104), Color.rgb(226, 135, 69), sunsetWarmth * 0.45);
        int sunLight = blend(Color.rgb(255, 225, 157), Color.rgb(255, 205, 144), sunsetWarmth * 0.35);
        int sunShade = blend(Color.rgb(202, 114, 45), Color.rgb(185, 84, 50), sunsetWarmth * 0.40);
        for (int ray = 0; ray < 8; ray++) {
            double angle = -Math.PI / 2.0 + ray * Math.PI / 4.0;
            int rayColor = Math.sin(angle) < 0.0
                    ? blend(sunBase, sunLight, 0.58) : blend(sunBase, sunShade, 0.45);
            drawSunRay(canvas, centerX, centerY, angle,
                    ray % 2 == 0 ? 3.2f : 2.5f, 1.25f, 0.75f, rayColor);
        }
        drawPixelDisc(canvas, centerX, centerY, 9, 0.5f, sunBase);
        // Two flat facets give the sun a chunky, low-poly bevel without a face.
        drawIslandPolygon(canvas,
                centerX - 5.5f, centerY - 4.0f,
                centerX - 1.5f, centerY - 7.0f,
                centerX + 1.0f, centerY - 2.0f,
                centerX - 2.0f, centerY + 1.0f,
                sunLight, 0);
        drawIslandPolygon(canvas,
                centerX - 1.0f, centerY + 1.0f,
                centerX + 3.5f, centerY - 1.0f,
                centerX + 7.0f, centerY + 2.0f,
                centerX + 4.0f, centerY + 6.0f,
                sunShade, 0);
    }

    private void drawSunRay(Canvas canvas, float centerX, float centerY, double angle,
                            float length, float baseHalfWidth, float tipHalfWidth, int color) {
        double directionX = Math.cos(angle);
        double directionY = Math.sin(angle);
        double perpendicularX = -directionY;
        double perpendicularY = directionX;
        double innerRadius = 8.0;
        double outerRadius = innerRadius + length;
        float x1 = snapCelestialCoordinate((float) (centerX + directionX * innerRadius
                + perpendicularX * baseHalfWidth));
        float y1 = snapCelestialCoordinate((float) (centerY + directionY * innerRadius
                + perpendicularY * baseHalfWidth));
        float x2 = snapCelestialCoordinate((float) (centerX + directionX * outerRadius
                + perpendicularX * tipHalfWidth));
        float y2 = snapCelestialCoordinate((float) (centerY + directionY * outerRadius
                + perpendicularY * tipHalfWidth));
        float x3 = snapCelestialCoordinate((float) (centerX + directionX * outerRadius
                - perpendicularX * tipHalfWidth));
        float y3 = snapCelestialCoordinate((float) (centerY + directionY * outerRadius
                - perpendicularY * tipHalfWidth));
        float x4 = snapCelestialCoordinate((float) (centerX + directionX * innerRadius
                - perpendicularX * baseHalfWidth));
        float y4 = snapCelestialCoordinate((float) (centerY + directionY * innerRadius
                - perpendicularY * baseHalfWidth));
        drawIslandPolygon(canvas, x1, y1, x2, y2, x3, y3, x4, y4, color, 0);
    }

    private float snapCelestialCoordinate(float coordinate) {
        return Math.round(coordinate * RENDER_SCALE) / (float) RENDER_SCALE;
    }

    private void drawMoon(Canvas canvas, SkyMath.Position moon, double sunAltitude,
                          double cloudCover, double elapsedSeconds) {
        if (moon.altitudeDegrees <= 0.0 || sunAltitude > -3.0 || cloudCover > 96.0
                || moon.illumination < 0.04) return;
        int centerX = Math.round(mapAzimuthToX(moon.azimuthDegrees));
        int centerY = Math.round(mapAltitudeToCelestialBand(moon.altitudeDegrees));
        float pulse = 0.5f + 0.5f * (float) Math.sin(elapsedSeconds * 0.30
                + Math.toRadians(moon.azimuthDegrees + 120.0));
        drawGlowPulse(canvas, centerX, centerY, elapsedSeconds, 0.37,
                14.0f, 0.23f, Color.rgb(244, 231, 194));
        drawPixelDisc(canvas, centerX, centerY, 12, 0.5f,
                Color.argb(Math.round(9 + pulse * 6), 221, 231, 216));
        drawPixelDisc(canvas, centerX, centerY, 10, 0.5f,
                Color.argb(Math.round(17 + pulse * 7), 232, 236, 220));
        int moonBase = Color.rgb(232, 222, 193);
        drawPixelDisc(canvas, centerX, centerY, 8, 0.5f, moonBase);
        // Warm light and cool shade echo the island's beveled, faceted surfaces.
        drawIslandPolygon(canvas,
                centerX - 5.0f, centerY - 4.0f,
                centerX - 1.0f, centerY - 7.0f,
                centerX + 2.0f, centerY - 2.0f,
                centerX - 2.0f, centerY + 1.0f,
                Color.rgb(250, 243, 218), 0);
        drawIslandPolygon(canvas,
                centerX - 1.0f, centerY + 1.0f,
                centerX + 3.0f, centerY - 1.0f,
                centerX + 6.0f, centerY + 3.0f,
                centerX + 2.0f, centerY + 6.0f,
                Color.rgb(178, 194, 205), 0);
        if (moon.illumination < 0.98) {
            int skyAtMoon = colorForSkyHeight(centerY / (float) SCENE_HEIGHT,
                    sunAltitude, cloudCover);
            float shadowOffset = (float) (moon.illumination * 13.0);
            drawPixelDisc(canvas, Math.round(centerX + shadowOffset), centerY, 8, 0.5f, skyAtMoon);
        }
    }

    private void drawStars(Canvas canvas, long now, double sunAltitude, double cloudCover,
                           double elapsedSeconds) {
        if (sunAltitude > -6.0 || cloudCover > 88.0) return;
        float cloudVisibility = (float) clamp((100.0 - cloudCover) / 48.0, 0.12, 1.0);
        int index = 0;
        for (SkyMath.Star star : SkyMath.BRIGHT_STARS) {
            SkyMath.Position position = SkyMath.star(star, now, latitude, longitude);
            if (position.altitudeDegrees <= 0.0) {
                index++;
                continue;
            }
            float x = mapAzimuthToX(position.azimuthDegrees);
            float y = mapAltitudeToCelestialBand(position.altitudeDegrees);
            float twinkle = 0.80f + 0.12f
                    * (float) Math.sin(elapsedSeconds * 0.55 + index * 1.7);
            int starColor = index % 3 == 0 ? Color.rgb(255, 235, 187) : Color.rgb(203, 216, 239);
            float glowStrength = (float) (0.18 * (0.55 + star.brightness * 0.45) * cloudVisibility);
            drawGlowPulse(canvas, x, y, elapsedSeconds, index * 0.137,
                    5.0f, glowStrength, starColor);
            paint.setColor(starColor);
            paint.setAlpha((int) (255 * cloudVisibility * star.brightness * twinkle));
            float pixelX = Math.round(x * RENDER_SCALE) / (float) RENDER_SCALE;
            float pixelY = Math.round(y * RENDER_SCALE) / (float) RENDER_SCALE;
            float size = star.brightness > 0.7 ? 1.5f : 1.0f;
            canvas.drawRect(pixelX, pixelY, pixelX + size, pixelY + size, paint);
            if (star.brightness > 0.75) {
                canvas.drawRect(pixelX + 0.5f, pixelY - 0.5f,
                        pixelX + 1.0f, pixelY, paint);
                canvas.drawRect(pixelX - 0.5f, pixelY + 0.5f,
                        pixelX, pixelY + 1.0f, paint);
            }
            index++;
        }
        paint.setAlpha(255);
    }

    private CloudState[] createCloudStates(double elapsedSeconds, int code) {
        // WMO 0/1 means clear or mainly clear; every other condition gets one cloud.
        int count = code == 0 || code == 1 ? 0 : 1;
        CloudState[] result = new CloudState[count];
        if (count == 0) return result;

        double windSpeedKmh = weather == null ? 4.0 : weather.windSpeedKmh;
        double windFromDegrees = weather == null ? 90.0 : weather.windDirectionDegrees;
        // Open-Meteo reports the bearing the wind comes from; use the downwind ENU vector.
        double windBearing = Math.toRadians(normalizeDegrees(windFromDegrees + 180.0));
        double alongEast = Math.sin(windBearing);
        double alongNorth = Math.cos(windBearing);
        double crossEast = -alongNorth;
        double crossNorth = alongEast;
        double simulatedSeconds = elapsedSeconds * CLOUD_SIMULATION_RATE;
        double windSpeedMetersPerSecond = Math.max(0.0, windSpeedKmh) / 3.6;
        int cloudSeed = (int) Math.floor(latitude * 37.0 + longitude * 17.0);

        double[] assignedBaseAltitudes = new double[count];
        double[] assignedScreenCenterY = new double[count];
        for (int index = 0; index < count; index++) {
            String[] shape = CLOUD_SHAPES[(int) (cloudRandom(cloudSeed, index,
                    CLOUD_SHAPE_RANDOM_SALT) * CLOUD_SHAPES.length)];
            // Independent hashed bearings vary cloud positions but stay stable between frames.
            double initialBearing = cloudRandom(cloudSeed, index, CLOUD_BEARING_RANDOM_SALT)
                    * Math.PI * 2.0;
            double initialRadius = CLOUD_FIELD_MIN_RADIUS_METERS
                    + cloudRandom(cloudSeed, index, CLOUD_RADIUS_RANDOM_SALT) * CLOUD_FIELD_RADIUS_RANGE_METERS;
            double initialEast = Math.sin(initialBearing) * initialRadius;
            double initialNorth = Math.cos(initialBearing) * initialRadius;
            double along = initialEast * alongEast + initialNorth * alongNorth;
            double across = initialEast * crossEast + initialNorth * crossNorth;
            // Reflect at either end of the track instead of wrapping to the far edge.
            // The triangular-wave path reverses direction without a position jump.
            along = reflectCentered(along + windSpeedMetersPerSecond * simulatedSeconds,
                    CLOUD_FIELD_HALF_LENGTH_METERS);
            double cloudEast = along * alongEast + across * crossEast;
            double cloudNorth = along * alongNorth + across * crossNorth;

            double rangeMeters = Math.max(1.0, Math.hypot(cloudEast, cloudNorth));
            double widthMeters = CLOUD_WIDTH_MIN_METERS
                    + cloudRandom(cloudSeed, index, CLOUD_WIDTH_RANDOM_SALT) * CLOUD_WIDTH_RANGE_METERS;
            double depthRatio = 0.60 + cloudRandom(cloudSeed, index, CLOUD_DEPTH_RANDOM_SALT) * 0.35;
            double depthMeters = widthMeters * depthRatio;
            // Choose altitude from the cloud's stable initial range, not its changing distance.
            // Re-sampling every frame can switch candidates and visibly teleport the cloud vertically.
            double baseAltitude = sampleCloudBaseAltitude(cloudSeed, index, initialRadius,
                    depthMeters, shape, assignedBaseAltitudes, assignedScreenCenterY, index);
            double azimuth = normalizeDegrees(Math.toDegrees(Math.atan2(cloudEast, cloudNorth)));
            double baseElevation = Math.toDegrees(Math.atan2(baseAltitude, rangeMeters));
            double topElevation = Math.toDegrees(Math.atan2(baseAltitude + depthMeters, rangeMeters));
            double angularWidth = 2.0 * Math.atan2(widthMeters * 0.5, rangeMeters);
            double artSizeMix = cloudRandom(cloudSeed, index, CLOUD_ART_SIZE_RANDOM_SALT);
            float maxSpriteWidth = Math.min(
                    CLOUD_MAX_SCREEN_WIDTH * (0.82f + (float) artSizeMix * 0.18f),
                    SCENE_WIDTH / (float) count - CLOUD_LAYOUT_GAP);
            float spriteWidth = (float) clamp(
                    Math.toDegrees(angularWidth) * CLOUD_HORIZONTAL_ART_EXAGGERATION
                            * SCENE_WIDTH / 360.0,
                    CLOUD_MIN_SCREEN_WIDTH, maxSpriteWidth);
            float unitX = spriteWidth / shape[0].length();
            float baseY = mapCloudAltitudeToY(baseElevation);
            float topY = mapCloudAltitudeToY(topElevation);
            float spriteHeight = (float) clamp(
                    Math.max(shape.length * 0.7f,
                            (baseY - topY) * (float) CLOUD_VERTICAL_ART_EXAGGERATION),
                    CLOUD_MIN_SCREEN_HEIGHT, CLOUD_MAX_SCREEN_HEIGHT);
            float unitY = spriteHeight / shape.length;
            float spriteTopY = Math.max(CLOUD_SCREEN_TOP_MARGIN, baseY - spriteHeight);
            assignedBaseAltitudes[index] = baseAltitude;
            assignedScreenCenterY[index] = cloudSpriteCenterY(baseAltitude, initialRadius,
                    depthMeters, shape.length);
            result[index] = new CloudState(
                    shape, cloudEast, cloudNorth, baseAltitude, widthMeters, depthMeters,
                    alongEast, alongNorth, crossEast, crossNorth,
                    (float) mapAzimuthToX(azimuth), spriteTopY, unitX, unitY);
        }
        separateOverlappingClouds(result);
        return result;
    }

    /** Randomly sample cloud heights, rejecting physical or projected near-duplicates. */
    private double sampleCloudBaseAltitude(int seed, int index, double rangeMeters,
                                           double depthMeters, String[] shape,
                                           double[] assignedAltitudes, double[] assignedCenterY,
                                           int assignedCount) {
        double bestCandidate = CLOUD_BASE_MIN_METERS
                + cloudRandom(seed, index, CLOUD_ALTITUDE_RANDOM_SALT + 8192)
                * CLOUD_BASE_RANGE_METERS;
        double bestScore = -1.0;
        for (int attempt = 0; attempt < CLOUD_ALTITUDE_SAMPLE_ATTEMPTS; attempt++) {
            int salt = CLOUD_ALTITUDE_RANDOM_SALT + attempt * 37;
            double candidate = CLOUD_BASE_MIN_METERS
                    + cloudRandom(seed, index, salt) * CLOUD_BASE_RANGE_METERS;
            double centerY = cloudSpriteCenterY(candidate, rangeMeters, depthMeters, shape.length);
            double score = altitudeSpacingScore(candidate, centerY,
                    assignedAltitudes, assignedCenterY, assignedCount);
            if (score > bestScore) {
                bestScore = score;
                bestCandidate = candidate;
            }
            if (score >= 1.0) return candidate;
        }

        // Rare fallback: scan a seed-rotated altitude lattice for a valid gap before relaxing it.
        int latticeSize = 4096;
        int start = (int) (cloudRandom(seed, index, CLOUD_ALTITUDE_RANDOM_SALT + 16384)
                * latticeSize);
        for (int offset = 0; offset < latticeSize; offset++) {
            int slot = (start + offset) % latticeSize;
            double candidate = CLOUD_BASE_MIN_METERS
                    + (slot + 0.5) / latticeSize * CLOUD_BASE_RANGE_METERS;
            double centerY = cloudSpriteCenterY(candidate, rangeMeters, depthMeters, shape.length);
            double score = altitudeSpacingScore(candidate, centerY,
                    assignedAltitudes, assignedCenterY, assignedCount);
            if (score > bestScore) {
                bestScore = score;
                bestCandidate = candidate;
            }
            if (score >= 1.0) return candidate;
        }
        return bestCandidate;
    }

    private double cloudSpriteCenterY(double baseAltitudeMeters, double rangeMeters,
                                      double depthMeters, int shapeHeight) {
        double baseElevation = Math.toDegrees(Math.atan2(baseAltitudeMeters, rangeMeters));
        double topElevation = Math.toDegrees(Math.atan2(baseAltitudeMeters + depthMeters, rangeMeters));
        double baseY = mapCloudAltitudeToY(baseElevation);
        double topY = mapCloudAltitudeToY(topElevation);
        double spriteHeight = clamp(
                Math.max(shapeHeight * 0.7,
                        (baseY - topY) * CLOUD_VERTICAL_ART_EXAGGERATION),
                CLOUD_MIN_SCREEN_HEIGHT, CLOUD_MAX_SCREEN_HEIGHT);
        double spriteTopY = Math.max(CLOUD_SCREEN_TOP_MARGIN, baseY - spriteHeight);
        return spriteTopY + spriteHeight * 0.5;
    }

    private double altitudeSpacingScore(double altitudeMeters, double screenCenterY,
                                        double[] assignedAltitudes, double[] assignedScreenCenterY,
                                        int assignedCount) {
        if (assignedCount == 0) return Double.POSITIVE_INFINITY;
        double score = Double.POSITIVE_INFINITY;
        for (int previous = 0; previous < assignedCount; previous++) {
            double altitudeGap = Math.abs(altitudeMeters - assignedAltitudes[previous]);
            double screenGap = Math.abs(screenCenterY - assignedScreenCenterY[previous]);
            score = Math.min(score, altitudeGap / CLOUD_MIN_BASE_ALTITUDE_GAP_METERS);
            score = Math.min(score, screenGap / CLOUD_MIN_SCREEN_ALTITUDE_GAP);
        }
        return score;
    }

    /** Spread only intersecting sprites; preserve range and keep the ENU shadow source aligned. */
    private void separateOverlappingClouds(CloudState[] clouds) {
        for (int iteration = 0; iteration < 16; iteration++) {
            boolean moved = false;
            for (int first = 0; first < clouds.length; first++) {
                CloudState a = clouds[first];
                float widthA = a.unitX * a.shape[0].length();
                float heightA = a.unitY * a.shape.length;
                float centerYA = a.screenY + heightA * 0.5f;
                for (int second = first + 1; second < clouds.length; second++) {
                    CloudState b = clouds[second];
                    float widthB = b.unitX * b.shape[0].length();
                    float heightB = b.unitY * b.shape.length;
                    float centerYB = b.screenY + heightB * 0.5f;
                    if (Math.abs(centerYA - centerYB) >= (heightA + heightB) * 0.5f) continue;
                    double deltaX = wrapCentered(b.screenCenterX - a.screenCenterX, SCENE_WIDTH);
                    double requiredGap = (widthA + widthB) * 0.5 + CLOUD_LAYOUT_GAP;
                    double overlap = requiredGap - Math.abs(deltaX);
                    if (overlap <= 0.0) continue;
                    double direction = deltaX < 0.0 ? -1.0 : 1.0;
                    float push = (float) (overlap * 0.5);
                    a.screenCenterX = wrap(a.screenCenterX - direction * push, SCENE_WIDTH);
                    b.screenCenterX = wrap(b.screenCenterX + direction * push, SCENE_WIDTH);
                    moved = true;
                }
            }
            if (!moved) break;
        }

        // Rotate ENU coordinates to the separated bearing without changing cloud distance.
        for (CloudState cloud : clouds) {
            double range = Math.hypot(cloud.eastMeters, cloud.northMeters);
            double azimuth = normalizeDegrees(cloud.screenCenterX / SCENE_WIDTH * 360.0
                    + headingDegrees - 180.0);
            cloud.eastMeters = Math.sin(Math.toRadians(azimuth)) * range;
            cloud.northMeters = Math.cos(Math.toRadians(azimuth)) * range;
        }
    }

    private void drawClouds(Canvas canvas, CloudState[] clouds, double sunAltitude, int code) {
        int baseColor;
        int shadeColor;
        int lightColor;
        if (sunAltitude < -5.0) {
            baseColor = Color.rgb(100, 119, 150);
            shadeColor = Color.rgb(61, 76, 105);
            lightColor = Color.rgb(174, 190, 218);
        } else if (sunAltitude < 8.0) {
            baseColor = Color.rgb(213, 185, 199);
            shadeColor = Color.rgb(142, 126, 157);
            lightColor = Color.rgb(249, 224, 197);
        } else {
            baseColor = Color.rgb(215, 227, 243);
            shadeColor = Color.rgb(128, 150, 180);
            lightColor = Color.rgb(255, 250, 239);
        }
        if (code >= 95) {
            baseColor = Color.rgb(142, 155, 174);
            shadeColor = Color.rgb(77, 94, 118);
            lightColor = Color.rgb(203, 212, 224);
        }

        for (CloudState cloud : clouds) {
            float width = cloud.unitX * cloud.shape[0].length();
            float x = cloud.screenCenterX - width * 0.5f;
            drawCloud(canvas, x, cloud.screenY, cloud.unitX, cloud.unitY,
                    cloud.shape, baseColor, shadeColor, lightColor);
            if (x < 0.0f) {
                drawCloud(canvas, x + SCENE_WIDTH, cloud.screenY, cloud.unitX, cloud.unitY,
                        cloud.shape, baseColor, shadeColor, lightColor);
            } else if (x + width > SCENE_WIDTH) {
                drawCloud(canvas, x - SCENE_WIDTH, cloud.screenY, cloud.unitX, cloud.unitY,
                        cloud.shape, baseColor, shadeColor, lightColor);
            }
        }
    }

    private void appendCloudRows(Path path, float x, float y, float unitX, float unitY,
                                 String[] shape, int firstRow, int lastRow, float offsetY) {
        for (int row = firstRow; row < lastRow; row++) {
            int column = 0;
            while (column < shape[row].length()) {
                while (column < shape[row].length() && shape[row].charAt(column) != '1') column++;
                int firstColumn = column;
                while (column < shape[row].length() && shape[row].charAt(column) == '1') column++;
                if (column == firstColumn) continue;
                float left = snapCelestialCoordinate(x + firstColumn * unitX);
                float right = snapCelestialCoordinate(x + column * unitX);
                float top = snapCelestialCoordinate(y + row * unitY + offsetY);
                float bottom = snapCelestialCoordinate(y + (row + 1) * unitY + offsetY);
                if (right > left && bottom > top) {
                    path.addRect(left, top, right, bottom, Path.Direction.CW);
                }
            }
        }
    }

    private void drawCloud(Canvas canvas, float x, float y, float unitX, float unitY,
                           String[] shape, int base, int shade, int light) {
        int middleRow = shape.length / 2;
        int bottomRow = shape.length - 1;
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);

        // Offset lower blocks form a compact shadow bevel beneath the broad base.
        shapePath.reset();
        appendCloudRows(shapePath, x, y, unitX, unitY, shape,
                middleRow, shape.length, unitY * 0.34f);
        paint.setColor(shade);
        canvas.drawPath(shapePath, paint);

        // Joined row-runs retain the rounded cumulus silhouette as chunky pixel steps.
        shapePath.reset();
        appendCloudRows(shapePath, x, y, unitX, unitY, shape, 0, shape.length, 0.0f);
        paint.setColor(base);
        canvas.drawPath(shapePath, paint);

        // A narrow lower facet separates the cloud's face from its underside.
        shapePath.reset();
        for (int row = Math.max(middleRow, bottomRow - 1); row <= bottomRow; row++) {
            int column = 0;
            while (column < shape[row].length()) {
                while (column < shape[row].length() && shape[row].charAt(column) != '1') column++;
                int firstColumn = column;
                while (column < shape[row].length() && shape[row].charAt(column) == '1') column++;
                if (column == firstColumn) continue;
                float left = snapCelestialCoordinate(x + firstColumn * unitX + unitX * 0.06f);
                float right = snapCelestialCoordinate(x + column * unitX - unitX * 0.06f);
                float top = snapCelestialCoordinate(y + (row + 0.66f) * unitY);
                float bottom = snapCelestialCoordinate(y + (row + 1.0f) * unitY);
                if (right > left && bottom > top) {
                    shapePath.addRect(left, top, right, bottom, Path.Direction.CW);
                }
            }
        }
        paint.setColor(blend(base, shade, 0.48));
        canvas.drawPath(shapePath, paint);

        // Small flat facets catch light along the stepped crowns and left shoulders.
        shapePath.reset();
        for (int row = 0; row < middleRow; row++) {
            for (int column = 0; column < shape[row].length(); column++) {
                if (shape[row].charAt(column) != '1') continue;
                boolean exposedTop = row == 0 || shape[row - 1].charAt(column) != '1';
                boolean exposedLeft = column == 0 || shape[row].charAt(column - 1) != '1';
                float left = x + column * unitX;
                float right = left + unitX;
                float top = y + row * unitY;
                float bottom = top + unitY;
                if (exposedTop) {
                    shapePath.moveTo(snapCelestialCoordinate(left + unitX * 0.08f),
                            snapCelestialCoordinate(top + unitY * 0.08f));
                    shapePath.lineTo(snapCelestialCoordinate(right - unitX * 0.08f),
                            snapCelestialCoordinate(top + unitY * 0.08f));
                    shapePath.lineTo(snapCelestialCoordinate(left + unitX * 0.56f),
                            snapCelestialCoordinate(top + unitY * 0.48f));
                    shapePath.lineTo(snapCelestialCoordinate(left + unitX * 0.08f),
                            snapCelestialCoordinate(top + unitY * 0.48f));
                    shapePath.close();
                } else if (exposedLeft) {
                    shapePath.moveTo(snapCelestialCoordinate(left + unitX * 0.08f),
                            snapCelestialCoordinate(top + unitY * 0.08f));
                    shapePath.lineTo(snapCelestialCoordinate(left + unitX * 0.42f),
                            snapCelestialCoordinate(top + unitY * 0.25f));
                    shapePath.lineTo(snapCelestialCoordinate(left + unitX * 0.42f),
                            snapCelestialCoordinate(bottom - unitY * 0.08f));
                    shapePath.lineTo(snapCelestialCoordinate(left + unitX * 0.08f),
                            snapCelestialCoordinate(bottom - unitY * 0.08f));
                    shapePath.close();
                }
            }
        }
        paint.setColor(light);
        canvas.drawPath(shapePath, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawFloatingIsland(Canvas canvas, double sunAltitude, double sunAzimuth,
                                    double cloudCover, int code, CloudState[] clouds) {
        IslandPalette palette = islandPalette(sunAltitude, cloudCover, code);
        IslandSunlight sunlight = islandSunlight(sunAltitude, cloudCover, code);
        float centerX = SCENE_WIDTH / 2.0f;
        float leftX = centerX - ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH / 2.0f;
        float rightX = centerX + ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH / 2.0f;
        float topY = ISLAND_TOP;
        float middleY = topY + ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT / 2.0f;
        float bottomY = topY + ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT;
        float lowerY = bottomY + ISLAND_DEPTH;
        float grassBandDepth = ISLAND_GRASS_BAND_DEPTH;
        float soilFrontInsetY = ISLAND_GRASS_OVERHANG / 2.0f;
        float soilFrontY = bottomY - soilFrontInsetY;
        float soilLeftX = leftX + ISLAND_GRASS_OVERHANG;
        float soilRightX = rightX - ISLAND_GRASS_OVERHANG;

        // The cloud sea acts as the ground plane, so the island's shadow lands on its tops.
        // Far layers first: haze and the distant skyline must sit behind the sea and island.
        drawDistantCloudHaze(canvas, sunAltitude, cloudCover, code);
        drawBackCloudBanks(canvas, sunAltitude, cloudCover, code);
        drawCloudSea(canvas, sunAltitude, cloudCover, code);
        drawIslandShadowOnClouds(canvas, centerX, sunAltitude, cloudCover, code);

        // One rounded, uninterrupted soil slab sits inside the wider grass cap.
        drawIslandSoilBody(canvas, soilLeftX, centerX, soilRightX,
                middleY, soilFrontY, grassBandDepth, palette);
        // A side-shaded average of the two tile hues fills rounded joints without a third green.
        int grassTileJoin = blend(palette.grassTileSoftFern, palette.grassTileFernSage, 0.5);
        int leftBandLight = blend(grassTileJoin, Color.BLACK, 0.08);
        int leftBandShade = blend(grassTileJoin, Color.BLACK, 0.22);
        int rightBandLight = blend(grassTileJoin, Color.BLACK, 0.16);
        int rightBandShade = blend(grassTileJoin, Color.BLACK, 0.30);
        drawSmoothIslandQuad(canvas,
                leftX, middleY, centerX, bottomY,
                centerX, bottomY + grassBandDepth, leftX, middleY + grassBandDepth,
                leftBandLight, leftBandShade, 0, 0.0f);
        drawSmoothIslandQuad(canvas,
                centerX, bottomY, rightX, middleY,
                rightX, middleY + grassBandDepth, centerX, bottomY + grassBandDepth,
                rightBandLight, rightBandShade, 0, 0.0f);

        // A darker underside makes the overhang read as a distinct grass platform above the soil.
        int grassLedgeLight = blend(palette.grassWall, palette.grassDark, 0.16);
        int grassLedgeDark = blend(palette.grassWall, palette.grassDark, 0.32);
        drawSmoothIslandQuad(canvas,
                leftX, middleY + grassBandDepth,
                centerX, bottomY + grassBandDepth,
                centerX, soilFrontY + grassBandDepth,
                soilLeftX, middleY + grassBandDepth,
                grassLedgeLight, grassLedgeDark, 0, 0.0f);
        drawSmoothIslandQuad(canvas,
                centerX, bottomY + grassBandDepth,
                rightX, middleY + grassBandDepth,
                soilRightX, middleY + grassBandDepth,
                centerX, soilFrontY + grassBandDepth,
                grassLedgeLight, grassLedgeDark, 0, 0.0f);

        // A soft mid-green underlay closes hairline gaps without drawing dark grid seams.
        int tileSeam = blend(grassTileJoin, palette.grass, 0.10);
        drawSmoothIslandQuad(canvas,
                centerX, topY, rightX, middleY, centerX, bottomY, leftX, middleY,
                tileSeam, tileSeam, 0, 0.0f);
        for (int diagonal = 0; diagonal < ISLAND_GRID_SIZE * 2 - 1; diagonal++) {
            for (int row = 0; row < ISLAND_GRID_SIZE; row++) {
                int column = diagonal - row;
                if (column < 0 || column >= ISLAND_GRID_SIZE) continue;
                char columnLabel = ISLAND_COLUMN_LABELS.charAt(column);
                int rowLabel = row + 1;
                boolean lightTile = ((columnLabel - 'A') + rowLabel - 1) % 2 == 0;
                float tileX = centerX + (column - row) * ISLAND_TILE_WIDTH / 2.0f;
                float tileY = topY + ISLAND_TILE_HEIGHT / 2.0f
                        + (column + row) * ISLAND_TILE_HEIGHT / 2.0f;
                drawGrassTile(canvas, tileX, tileY, lightTile,
                        column, row, palette, sunlight);
            }
        }
        drawIslandCloudShadows(canvas, clouds, sunAltitude, sunAzimuth, cloudCover, code);
        int topRim = blend(palette.grassSeam, palette.grassDark, 0.32);
        drawSmoothIslandLine(canvas, leftX, middleY, centerX, bottomY, topRim, 0.60f);
        drawSmoothIslandLine(canvas, centerX, bottomY, rightX, middleY, topRim, 0.60f);
        // Side and front banks overlap the island's tiles, so they draw after it.
        drawCloudBanks(canvas, sunAltitude, cloudCover, code);
        drawCloudContactShade(canvas, sunAltitude, cloudCover, code);
    }

    private void drawCloudsOverIsland(Canvas canvas, CloudState[] clouds,
                                      double sunAltitude, int code) {
        if (clouds.length == 0) return;
        setIslandSurfacePath();
        int save = canvas.save();
        canvas.clipPath(islandSurfacePath);
        canvas.clipRect(0, 0, SCENE_WIDTH, SCENE_HEIGHT * 0.5f);
        // Only the part of a cloud's projection over the surface is redrawn in front.
        drawClouds(canvas, clouds, sunAltitude, code);
        canvas.restoreToCount(save);
    }

    private void drawSunOverIsland(Canvas canvas, SkyMath.Position sun, double elapsedSeconds) {
        setIslandSurfacePath();
        int save = canvas.save();
        canvas.clipPath(islandSurfacePath);
        canvas.clipRect(0, 0, SCENE_WIDTH, SCENE_HEIGHT * 0.5f);
        drawSun(canvas, sun, elapsedSeconds);
        canvas.restoreToCount(save);
    }

    private void drawCelestialsOverIsland(Canvas canvas, long now,
                                          SkyMath.Position sun, SkyMath.Position moon,
                                          double cloudCover, double elapsedSeconds) {
        setIslandSurfacePath();
        int save = canvas.save();
        canvas.clipPath(islandSurfacePath);
        canvas.clipRect(0, 0, SCENE_WIDTH, SCENE_HEIGHT * 0.5f);
        drawStars(canvas, now, sun.altitudeDegrees, cloudCover, elapsedSeconds);
        drawMoon(canvas, moon, sun.altitudeDegrees, cloudCover, elapsedSeconds);
        canvas.restoreToCount(save);
    }

    private void drawIslandCloudShadows(Canvas canvas, CloudState[] clouds,
                                        double sunAltitude, double sunAzimuth,
                                        double cloudCover, int code) {
        if (clouds.length == 0 || sunAltitude <= CLOUD_SHADOW_MIN_SUN_ALTITUDE) return;
        setIslandSurfacePath();
        boolean physicalShadowMayHit = drawCloudShadowsOnSurface(canvas, clouds,
                sunAltitude, sunAzimuth, cloudCover, 0.0, islandSurfacePath, code);
        if (!physicalShadowMayHit) {
            drawIslandCloudShadowCue(canvas, clouds, sunAltitude, sunAzimuth,
                    cloudCover, islandSurfacePath, code);
        }
    }

    private void setIslandSurfacePath() {
        float centerX = SCENE_WIDTH / 2.0f;
        float halfWidth = ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH / 2.0f;
        float halfHeight = ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT / 2.0f;
        islandSurfacePath.reset();
        islandSurfacePath.moveTo(centerX, ISLAND_CENTER_Y - halfHeight);
        islandSurfacePath.lineTo(centerX + halfWidth, ISLAND_CENTER_Y);
        islandSurfacePath.lineTo(centerX, ISLAND_CENTER_Y + halfHeight);
        islandSurfacePath.lineTo(centerX - halfWidth, ISLAND_CENTER_Y);
        islandSurfacePath.close();
    }

    /**
     * Projects cloud silhouettes onto a horizontal receiver plane. The clip path and receiver
     * elevation make this reusable for future object tops; the current scene only has the island.
     */
    private boolean drawCloudShadowsOnSurface(Canvas canvas, CloudState[] clouds,
                                               double sunAltitude, double sunAzimuth,
                                               double cloudCover, double receiverElevationMeters,
                                               Path receiverMask, int code) {
        if (sunAltitude <= CLOUD_SHADOW_MIN_SUN_ALTITUDE) return false;
        double altitudeRadians = Math.toRadians(sunAltitude);
        double azimuthRadians = Math.toRadians(sunAzimuth);
        double sineAltitude = Math.sin(altitudeRadians);
        double horizontalSun = Math.cos(altitudeRadians);
        double sunEast = horizontalSun * Math.sin(azimuthRadians);
        double sunNorth = horizontalSun * Math.cos(azimuthRadians);
        double directLight = clamp((sineAltitude - Math.sin(Math.toRadians(3.0)))
                / (Math.sin(Math.toRadians(18.0)) - Math.sin(Math.toRadians(3.0))), 0.0, 1.0);
        double coverFactor = clamp(cloudCover / 100.0, 0.0, 1.0);
        double opacity = (CLOUD_SHADOW_CLEAR_OPACITY
                + CLOUD_SHADOW_COVER_OPACITY * coverFactor) * directLight;
        if (code >= 95) {
            opacity = Math.min(CLOUD_SHADOW_MAX_OPACITY, opacity + CLOUD_SHADOW_STORM_BONUS);
        }
        int shadowColor = Color.argb((int) Math.round(opacity * 255.0), 17, 26, 39);

        boolean physicalShadowMayHit = false;
        int save = canvas.save();
        canvas.clipPath(receiverMask);
        for (CloudState cloud : clouds) {
            double relativeCloudHeight = cloud.baseAltitudeMeters - receiverElevationMeters;
            if (relativeCloudHeight <= 0.0) continue;
            double groundShiftEast = -relativeCloudHeight * sunEast / sineAltitude;
            double groundShiftNorth = -relativeCloudHeight * sunNorth / sineAltitude;
            double footprintScale = cloudShadowFootprintScale(cloud);
            double shadowCenterEast = cloud.eastMeters + groundShiftEast;
            double shadowCenterNorth = cloud.northMeters + groundShiftNorth;
            if (cloudShadowIntersectsIsland(cloud, groundShiftEast, groundShiftNorth,
                    footprintScale)) {
                physicalShadowMayHit = true;
            }
            buildCloudShadowPath(cloud, groundShiftEast, groundShiftNorth, footprintScale);
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(255);
            paint.setColor(shadowColor);
            canvas.drawPath(cloudShadowPath, paint);
        }
        canvas.restoreToCount(save);
        return physicalShadowMayHit;
    }

    /** If every real shadow misses, add a soft, island-local cue from the nearest cloud. */
    private void drawIslandCloudShadowCue(Canvas canvas, CloudState[] clouds,
                                          double sunAltitude, double sunAzimuth,
                                          double cloudCover, Path receiverMask, int code) {
        double altitudeRadians = Math.toRadians(sunAltitude);
        double azimuthRadians = Math.toRadians(sunAzimuth);
        double sineAltitude = Math.sin(altitudeRadians);
        if (sineAltitude <= 0.0) return;
        double horizontalSun = Math.cos(altitudeRadians);
        double sunEast = horizontalSun * Math.sin(azimuthRadians);
        double sunNorth = horizontalSun * Math.cos(azimuthRadians);
        double directLight = clamp((sineAltitude - Math.sin(Math.toRadians(3.0)))
                / (Math.sin(Math.toRadians(18.0)) - Math.sin(Math.toRadians(3.0))), 0.0, 1.0);
        double coverFactor = clamp(cloudCover / 100.0, 0.0, 1.0);
        double opacity = (CLOUD_SHADOW_CLEAR_OPACITY
                + CLOUD_SHADOW_COVER_OPACITY * coverFactor) * directLight;
        if (code >= 95) {
            opacity = Math.min(CLOUD_SHADOW_MAX_OPACITY, opacity + CLOUD_SHADOW_STORM_BONUS);
        }
        double cueOpacity = Math.max(CLOUD_SHADOW_FALLBACK_MIN_OPACITY * directLight,
                opacity * 0.72);

        CloudState nearest = null;
        double nearestEast = 0.0;
        double nearestNorth = 0.0;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (CloudState cloud : clouds) {
            double relativeCloudHeight = cloud.baseAltitudeMeters;
            if (relativeCloudHeight <= 0.0) continue;
            double shiftEast = -relativeCloudHeight * sunEast / sineAltitude;
            double shiftNorth = -relativeCloudHeight * sunNorth / sineAltitude;
            double centerEast = cloud.eastMeters + shiftEast;
            double centerNorth = cloud.northMeters + shiftNorth;
            double distance = Math.hypot(centerEast, centerNorth);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = cloud;
                nearestEast = centerEast;
                nearestNorth = centerNorth;
            }
        }
        if (nearest == null) return;

        // Preserve the projected direction but gently pull the cue onto the island's inner surface.
        double cueRadiusMeters = ISLAND_GRID_SIZE * ISLAND_TILE_METERS * 0.20;
        double maxAxisDistance = Math.max(Math.abs(nearestEast), Math.abs(nearestNorth));
        double pull = maxAxisDistance > cueRadiusMeters ? cueRadiusMeters / maxAxisDistance : 1.0;
        double cueEast = nearestEast * pull;
        double cueNorth = nearestNorth * pull;
        double cueShiftEast = cueEast - nearest.eastMeters;
        double cueShiftNorth = cueNorth - nearest.northMeters;
        buildCloudShadowPath(nearest, cueShiftEast, cueShiftNorth,
                cloudShadowFootprintScale(nearest));

        int save = canvas.save();
        canvas.clipPath(receiverMask);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        paint.setColor(Color.argb((int) Math.round(cueOpacity * 255.0), 17, 26, 39));
        canvas.drawPath(cloudShadowPath, paint);
        canvas.restoreToCount(save);
    }

    /** Exact convex-quad checks against the square ENU footprint of the island top. */
    private boolean cloudShadowIntersectsIsland(CloudState cloud,
                                                double shadowEast, double shadowNorth,
                                                double footprintScale) {
        double cellAlong = cloud.widthMeters * footprintScale / cloud.shape[0].length();
        double cellAcross = cloud.depthMeters * footprintScale / cloud.shape.length;
        double halfExtent = ISLAND_GRID_SIZE * ISLAND_TILE_METERS / 2.0;
        for (int row = 0; row < cloud.shape.length; row++) {
            String pixels = cloud.shape[row];
            int column = 0;
            while (column < pixels.length()) {
                while (column < pixels.length() && pixels.charAt(column) != '1') column++;
                int runStart = column;
                while (column < pixels.length() && pixels.charAt(column) == '1') column++;
                if (runStart == column) continue;
                double alongStart = (runStart - pixels.length() * 0.5) * cellAlong;
                double alongEnd = (column - pixels.length() * 0.5) * cellAlong;
                double acrossStart = (row - cloud.shape.length * 0.5) * cellAcross;
                double acrossEnd = (row + 1 - cloud.shape.length * 0.5) * cellAcross;
                double[] east = new double[4];
                double[] north = new double[4];
                setShadowVertex(cloud, alongStart, acrossStart, shadowEast, shadowNorth, east, north, 0);
                setShadowVertex(cloud, alongEnd, acrossStart, shadowEast, shadowNorth, east, north, 1);
                setShadowVertex(cloud, alongEnd, acrossEnd, shadowEast, shadowNorth, east, north, 2);
                setShadowVertex(cloud, alongStart, acrossEnd, shadowEast, shadowNorth, east, north, 3);
                if (shadowQuadIntersectsIsland(east, north, halfExtent)) return true;
            }
        }
        return false;
    }

    private void setShadowVertex(CloudState cloud, double alongMeters, double acrossMeters,
                                 double shadowEast, double shadowNorth,
                                 double[] east, double[] north, int index) {
        east[index] = cloud.eastMeters + alongMeters * cloud.alongEast
                + acrossMeters * cloud.crossEast + shadowEast;
        north[index] = cloud.northMeters + alongMeters * cloud.alongNorth
                + acrossMeters * cloud.crossNorth + shadowNorth;
    }

    private boolean shadowQuadIntersectsIsland(double[] east, double[] north, double halfExtent) {
        double minEast = east[0];
        double maxEast = east[0];
        double minNorth = north[0];
        double maxNorth = north[0];
        for (int index = 1; index < 4; index++) {
            minEast = Math.min(minEast, east[index]);
            maxEast = Math.max(maxEast, east[index]);
            minNorth = Math.min(minNorth, north[index]);
            maxNorth = Math.max(maxNorth, north[index]);
        }
        if (minEast > halfExtent || maxEast < -halfExtent
                || minNorth > halfExtent || maxNorth < -halfExtent) return false;

        // Separating-axis checks for the four edges of the projected parallelogram.
        for (int edge = 0; edge < 4; edge++) {
            int next = (edge + 1) % 4;
            double axisX = -(north[next] - north[edge]);
            double axisY = east[next] - east[edge];
            double polygonMin = Double.POSITIVE_INFINITY;
            double polygonMax = Double.NEGATIVE_INFINITY;
            for (int vertex = 0; vertex < 4; vertex++) {
                double projection = east[vertex] * axisX + north[vertex] * axisY;
                polygonMin = Math.min(polygonMin, projection);
                polygonMax = Math.max(polygonMax, projection);
            }
            double squareRadius = halfExtent * (Math.abs(axisX) + Math.abs(axisY));
            if (polygonMin > squareRadius || polygonMax < -squareRadius) return false;
        }
        return true;
    }

    private void buildCloudShadowPath(CloudState cloud, double shadowEast, double shadowNorth,
                                      double footprintScale) {
        double cellAlong = cloud.widthMeters * footprintScale / cloud.shape[0].length();
        double cellAcross = cloud.depthMeters * footprintScale / cloud.shape.length;
        cloudShadowPath.reset();
        for (int row = 0; row < cloud.shape.length; row++) {
            String pixels = cloud.shape[row];
            int column = 0;
            while (column < pixels.length()) {
                while (column < pixels.length() && pixels.charAt(column) != '1') column++;
                int runStart = column;
                while (column < pixels.length() && pixels.charAt(column) == '1') column++;
                if (runStart == column) continue;
                double alongStart = (runStart - pixels.length() * 0.5) * cellAlong;
                double alongEnd = (column - pixels.length() * 0.5) * cellAlong;
                double acrossStart = (row - cloud.shape.length * 0.5) * cellAcross;
                double acrossEnd = (row + 1 - cloud.shape.length * 0.5) * cellAcross;
                appendCloudShadowVertex(cloudShadowPath, cloud, alongStart, acrossStart,
                        shadowEast, shadowNorth, true);
                appendCloudShadowVertex(cloudShadowPath, cloud, alongEnd, acrossStart,
                        shadowEast, shadowNorth, false);
                appendCloudShadowVertex(cloudShadowPath, cloud, alongEnd, acrossEnd,
                        shadowEast, shadowNorth, false);
                appendCloudShadowVertex(cloudShadowPath, cloud, alongStart, acrossEnd,
                        shadowEast, shadowNorth, false);
                cloudShadowPath.close();
            }
        }
    }

    /** Keep the projected ground shadow smaller than its rendered cloud sprite. */
    private double cloudShadowFootprintScale(CloudState cloud) {
        double modelWidth = cloud.widthMeters;
        double modelDepth = cloud.depthMeters;
        double projectedWidthAtScaleOne = (modelWidth * Math.abs(cloud.alongEast - cloud.alongNorth)
                + modelDepth * Math.abs(cloud.crossEast - cloud.crossNorth))
                * ISLAND_TILE_WIDTH / (2.0 * ISLAND_TILE_METERS);
        double projectedHeightAtScaleOne = (modelWidth * Math.abs(cloud.alongEast + cloud.alongNorth)
                + modelDepth * Math.abs(cloud.crossEast + cloud.crossNorth))
                * ISLAND_TILE_HEIGHT / (2.0 * ISLAND_TILE_METERS);
        double spriteWidth = cloud.unitX * cloud.shape[0].length();
        double spriteHeight = cloud.unitY * cloud.shape.length;
        double widthLimit = spriteWidth / Math.max(1.0e-6, projectedWidthAtScaleOne);
        double heightLimit = spriteHeight / Math.max(1.0e-6, projectedHeightAtScaleOne);
        return Math.min(CLOUD_SHADOW_FOOTPRINT_SCALE,
                CLOUD_SHADOW_MAX_SPRITE_RATIO * Math.min(widthLimit, heightLimit));
    }

    private void appendCloudShadowVertex(Path path, CloudState cloud,
                                         double alongMeters, double acrossMeters,
                                         double shadowEast, double shadowNorth,
                                         boolean first) {
        double east = cloud.eastMeters + alongMeters * cloud.alongEast
                + acrossMeters * cloud.crossEast + shadowEast;
        double north = cloud.northMeters + alongMeters * cloud.alongNorth
                + acrossMeters * cloud.crossNorth + shadowNorth;
        float x = (float) (SCENE_WIDTH / 2.0
                + (east - north) * ISLAND_TILE_WIDTH / (2.0 * ISLAND_TILE_METERS));
        float y = (float) (ISLAND_CENTER_Y
                + (east + north) * ISLAND_TILE_HEIGHT / (2.0 * ISLAND_TILE_METERS));
        x = Math.round(x * RENDER_SCALE) / (float) RENDER_SCALE;
        y = Math.round(y * RENDER_SCALE) / (float) RENDER_SCALE;
        if (first) path.moveTo(x, y);
        else path.lineTo(x, y);
    }

    private void drawIslandShadow(Canvas canvas, float centerX, float lowerY,
                                  double sunAltitude, double cloudCover, int code) {
        double directLight = clamp((sunAltitude + 8.0) / 28.0, 0.0, 1.0);
        double cloudFilter = 1.0 - clamp(cloudCover / 100.0 * 0.45, 0.0, 0.45);
        if (code >= 95) cloudFilter *= 0.82;
        double opacity = (0.08 + directLight * 0.25) * cloudFilter
                * ISLAND_BODY_SHADOW_OPACITY_SCALE;
        float shadowCenterY = lowerY - ISLAND_DEPTH - ISLAND_BODY_SHADOW_RISE;
        float halfWidth = ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH / 4.0f
                * (float) ISLAND_BODY_SHADOW_SCALE;
        float halfHeight = ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT / 4.0f
                * (float) ISLAND_BODY_SHADOW_SCALE;
        int shadowColor = Color.argb((int) (255.0 * opacity), 12, 23, 40);
        drawIslandPolygon(canvas,
                centerX, shadowCenterY - halfHeight,
                centerX + halfWidth, shadowCenterY,
                centerX, shadowCenterY + halfHeight,
                centerX - halfWidth, shadowCenterY,
                shadowColor, 0);
    }

    private SkyAtmosphere skyAtmosphere(double sunAltitude, double cloudCover, int code) {
        if (sunAltitude < -8.0) return NIGHT_ATMOSPHERE;
        if (sunAltitude < 5.0) return TWILIGHT_ATMOSPHERE;
        boolean overcastOrWet = cloudCover >= 65.0 || code == 45 || code == 48
                || (code >= 51 && code <= 99);
        return overcastOrWet ? OVERCAST_ATMOSPHERE : DAY_ATMOSPHERE;
    }

    private IslandPalette islandPalette(double sunAltitude, double cloudCover, int code) {
        double daylight = clamp((sunAltitude + 12.0) / 24.0, 0.0, 1.0);
        double twilight = clamp(1.0 - Math.abs(sunAltitude + 1.0) / 9.0, 0.0, 1.0);
        double weatherShade = clamp((cloudCover - 25.0) / 100.0 * 0.34, 0.0, 0.26);
        if (code >= 95) weatherShade = Math.max(weatherShade, 0.34);
        SkyAtmosphere atmosphere = skyAtmosphere(sunAltitude, cloudCover, code);

        int grass = blend(Color.rgb(56, 83, 68), Color.rgb(90, 115, 101), daylight);
        grass = blend(grass, Color.rgb(195, 157, 96), twilight * 0.25);
        grass = blend(grass, Color.rgb(120, 131, 111), weatherShade);
        boolean wet = (code >= 51 && code <= 67) || (code >= 80 && code <= 82);
        if (wet) grass = blend(grass, Color.rgb(98, 120, 109), 0.12);
        grass = blend(grass, atmosphere.tint, atmosphere.islandMix);

        int grassDark = blend(grass, Color.rgb(45, 73, 56), 0.24);
        double grassTileMix = (0.18 + daylight * 0.77) * (1.0 - weatherShade * 0.30);
        int grassTileSoftFern = blend(grass, Color.rgb(168, 199, 149), grassTileMix);
        int grassTileFernSage = blend(grass, Color.rgb(152, 186, 135), grassTileMix);
        int grassSeam = blend(grass, Color.rgb(49, 77, 55), 0.08);
        int grassWall = blend(grass, Color.rgb(35, 72, 44), 0.28);

        int soil = blend(Color.rgb(50, 37, 28), Color.rgb(154, 74, 15), daylight);
        soil = blend(soil, Color.rgb(190, 116, 45), twilight * 0.24);
        soil = blend(soil, Color.rgb(91, 72, 55), weatherShade * 0.45);
        soil = blend(soil, atmosphere.tint, atmosphere.islandMix * 0.22);
        int soilLight = blend(soil, Color.rgb(190, 106, 26), 0.20);
        int soilDark = blend(soil, Color.rgb(58, 27, 7), 0.50);
        return new IslandPalette(grass, grassDark, grassTileSoftFern, grassTileFernSage,
                grassSeam, grassWall, soilLight, soilDark);
    }

    private IslandSunlight islandSunlight(double sunAltitude, double cloudCover, int code) {
        if (sunAltitude <= 0.0) return new IslandSunlight(0.0);
        double altitudeRadians = Math.toRadians(clamp(sunAltitude, 0.0, 90.0));
        double strength = clamp(Math.sin(altitudeRadians) / Math.sin(Math.PI / 3.0),
                0.0, 1.0) * (1.0 - clamp(cloudCover / 100.0, 0.0, 1.0));
        if (code >= 95 && code <= 99) strength *= 0.55;
        return new IslandSunlight(strength);
    }

    private void drawGrassTile(Canvas canvas, float centerX, float centerY, boolean lightTile,
                               int column, int row, IslandPalette palette,
                               IslandSunlight sunlight) {
        int fill = lightTile ? palette.grassTileSoftFern : palette.grassTileFernSage;
        // Keep adjacent top tiles nearly touching while retaining rounded corners.
        float halfWidth = ISLAND_TILE_WIDTH / 2.0f - 0.05f;
        float halfHeight = ISLAND_TILE_HEIGHT / 2.0f - 0.025f;
        float cornerRadius = 1.4f;

        boolean leftPerimeterFace = row == ISLAND_GRID_SIZE - 1;
        boolean rightPerimeterFace = column == ISLAND_GRID_SIZE - 1;
        float leftSideDepth = leftPerimeterFace
                ? ISLAND_GRASS_BAND_DEPTH : ISLAND_TILE_SIDE_DEPTH;
        float rightSideDepth = rightPerimeterFace
                ? ISLAND_GRASS_BAND_DEPTH : ISLAND_TILE_SIDE_DEPTH;
        // Perimeter side faces meet at the full grid vertices; top caps stay rounded and inset.
        drawGrassTileSide(canvas, centerX, centerY,
                leftPerimeterFace ? ISLAND_TILE_WIDTH / 2.0f : halfWidth,
                leftPerimeterFace ? ISLAND_TILE_HEIGHT / 2.0f : halfHeight,
                leftPerimeterFace ? 0.0f : cornerRadius,
                leftSideDepth, fill, true);
        drawGrassTileSide(canvas, centerX, centerY,
                rightPerimeterFace ? ISLAND_TILE_WIDTH / 2.0f : halfWidth,
                rightPerimeterFace ? ISLAND_TILE_HEIGHT / 2.0f : halfHeight,
                rightPerimeterFace ? 0.0f : cornerRadius,
                rightSideDepth, fill, false);

        // Each top stays solid; direct sunlight adds only a uniform, subtle color lift.
        drawSunlitIslandDiamond(canvas, centerX, centerY, halfWidth, halfHeight,
                cornerRadius, fill, sunlight);
    }

    private void drawGrassTileSide(Canvas canvas, float centerX, float centerY,
                                   float halfWidth, float halfHeight, float cornerRadius,
                                   float depth, int fill, boolean leftSide) {
        float cutX = Math.min(cornerRadius, halfWidth * 0.34f);
        float cutY = cutX * halfHeight / halfWidth;
        float splitY = centerY + halfHeight - cutY * 0.5f;
        float sideShade = depth > ISLAND_TILE_SIDE_DEPTH
                ? (leftSide ? 0.15f : 0.23f) : (leftSide ? 0.08f : 0.12f);
        islandPolyPath.reset();
        if (leftSide) {
            islandPolyPath.moveTo(centerX - halfWidth + cutX, centerY + cutY);
            islandPolyPath.lineTo(centerX - cutX, centerY + halfHeight - cutY);
            islandPolyPath.quadTo(centerX - cutX * 0.5f,
                    centerY + halfHeight - cutY * 0.5f, centerX, splitY);
            islandPolyPath.lineTo(centerX, splitY + depth);
            islandPolyPath.quadTo(centerX - cutX * 0.5f,
                    centerY + halfHeight - cutY * 0.5f + depth,
                    centerX - cutX, centerY + halfHeight - cutY + depth);
            islandPolyPath.lineTo(centerX - halfWidth + cutX, centerY + cutY + depth);
            islandPolyPath.close();
            drawSmoothIslandPath(canvas, islandPolyPath,
                    centerX - halfWidth, centerY + cutY,
                    centerX, centerY + halfHeight + depth,
                    blend(fill, Color.BLACK, sideShade),
                    blend(fill, Color.BLACK, sideShade), 0, 0.0f);
        } else {
            islandPolyPath.moveTo(centerX, splitY);
            islandPolyPath.quadTo(centerX + cutX * 0.5f,
                    centerY + halfHeight - cutY * 0.5f,
                    centerX + cutX, centerY + halfHeight - cutY);
            islandPolyPath.lineTo(centerX + halfWidth - cutX, centerY + cutY);
            islandPolyPath.lineTo(centerX + halfWidth - cutX, centerY + cutY + depth);
            islandPolyPath.lineTo(centerX + cutX, centerY + halfHeight - cutY + depth);
            islandPolyPath.quadTo(centerX + cutX * 0.5f,
                    centerY + halfHeight - cutY * 0.5f + depth,
                    centerX, splitY + depth);
            islandPolyPath.close();
            drawSmoothIslandPath(canvas, islandPolyPath,
                    centerX, centerY + cutY,
                    centerX + halfWidth, centerY + halfHeight + depth,
                    blend(fill, Color.BLACK, sideShade),
                    blend(fill, Color.BLACK, sideShade), 0, 0.0f);
        }
    }

    private void drawIslandSoilBody(Canvas canvas, float soilLeftX, float centerX,
                                    float soilRightX, float middleY, float soilFrontY,
                                    float grassBandDepth, IslandPalette palette) {
        islandBodyVertices[0] = soilLeftX;
        islandBodyVertices[1] = middleY + grassBandDepth;
        islandBodyVertices[2] = centerX;
        islandBodyVertices[3] = soilFrontY + grassBandDepth;
        islandBodyVertices[4] = soilRightX;
        islandBodyVertices[5] = middleY + grassBandDepth;
        islandBodyVertices[6] = soilRightX;
        islandBodyVertices[7] = middleY + ISLAND_DEPTH;
        islandBodyVertices[8] = centerX;
        islandBodyVertices[9] = soilFrontY + ISLAND_DEPTH;
        islandBodyVertices[10] = soilLeftX;
        islandBodyVertices[11] = middleY + ISLAND_DEPTH;
        traceRoundedIslandPolygon(islandBodyPath, islandBodyVertices, 6, 2.2f);
        drawSmoothIslandPath(canvas, islandBodyPath,
                soilLeftX, middleY + grassBandDepth,
                soilRightX, soilFrontY + ISLAND_DEPTH,
                palette.soilLight, palette.soilDark, 0, 0.0f);
    }

    private void traceRoundedIslandPolygon(Path path, float[] vertices,
                                           int vertexCount, float cornerRadius) {
        path.reset();
        if (vertexCount < 3) return;
        float firstX = vertices[0];
        float firstY = vertices[1];
        float previousX = vertices[(vertexCount - 1) * 2];
        float previousY = vertices[(vertexCount - 1) * 2 + 1];
        float firstLength = (float) Math.hypot(previousX - firstX, previousY - firstY);
        float firstInset = firstLength <= 0.0f ? 0.0f
                : Math.min(cornerRadius, firstLength * 0.45f) / firstLength;
        path.moveTo(firstX + (previousX - firstX) * firstInset,
                firstY + (previousY - firstY) * firstInset);
        for (int index = 0; index < vertexCount; index++) {
            int current = index * 2;
            int next = ((index + 1) % vertexCount) * 2;
            float x = vertices[current];
            float y = vertices[current + 1];
            float nextX = vertices[next];
            float nextY = vertices[next + 1];
            float edgeLength = (float) Math.hypot(nextX - x, nextY - y);
            float inset = edgeLength <= 0.0f ? 0.0f
                    : Math.min(cornerRadius, edgeLength * 0.45f) / edgeLength;
            float outgoingX = x + (nextX - x) * inset;
            float outgoingY = y + (nextY - y) * inset;
            path.quadTo(x, y, outgoingX, outgoingY);
            path.lineTo(nextX + (x - nextX) * inset,
                    nextY + (y - nextY) * inset);
        }
        path.close();
    }

    private void drawSmoothIslandQuad(Canvas canvas,
                                      float x1, float y1, float x2, float y2,
                                      float x3, float y3, float x4, float y4,
                                      int lightColor, int shadeColor,
                                      int outlineColor, float outlineWidth) {
        islandPolyPath.reset();
        islandPolyPath.moveTo(x1, y1);
        islandPolyPath.lineTo(x2, y2);
        islandPolyPath.lineTo(x3, y3);
        islandPolyPath.lineTo(x4, y4);
        islandPolyPath.close();
        float minX = Math.min(Math.min(x1, x2), Math.min(x3, x4));
        float maxX = Math.max(Math.max(x1, x2), Math.max(x3, x4));
        float minY = Math.min(Math.min(y1, y2), Math.min(y3, y4));
        float maxY = Math.max(Math.max(y1, y2), Math.max(y3, y4));
        drawSmoothIslandPath(canvas, islandPolyPath, minX, minY, maxX, maxY,
                lightColor, shadeColor, outlineColor, outlineWidth);
    }

    private void drawSmoothIslandDiamond(Canvas canvas, float centerX, float centerY,
                                         float halfWidth, float halfHeight, float cornerRadius,
                                         int lightColor, int shadeColor,
                                         int outlineColor, float outlineWidth) {
        traceSmoothIslandDiamond(islandPolyPath, centerX, centerY,
                halfWidth, halfHeight, cornerRadius);
        drawSmoothIslandPath(canvas, islandPolyPath,
                centerX - halfWidth, centerY - halfHeight,
                centerX + halfWidth, centerY + halfHeight,
                lightColor, shadeColor, outlineColor, outlineWidth);
    }

    private void traceSmoothIslandDiamond(Path path, float centerX, float centerY,
                                          float halfWidth, float halfHeight,
                                          float cornerRadius) {
        float cutX = Math.min(cornerRadius, halfWidth * 0.34f);
        float cutY = cutX * halfHeight / halfWidth;
        path.reset();
        path.moveTo(centerX - cutX, centerY - halfHeight + cutY);
        path.quadTo(centerX, centerY - halfHeight,
                centerX + cutX, centerY - halfHeight + cutY);
        path.lineTo(centerX + halfWidth - cutX, centerY - cutY);
        path.quadTo(centerX + halfWidth, centerY,
                centerX + halfWidth - cutX, centerY + cutY);
        path.lineTo(centerX + cutX, centerY + halfHeight - cutY);
        path.quadTo(centerX, centerY + halfHeight,
                centerX - cutX, centerY + halfHeight - cutY);
        path.lineTo(centerX - halfWidth + cutX, centerY + cutY);
        path.quadTo(centerX - halfWidth, centerY,
                centerX - halfWidth + cutX, centerY - cutY);
        path.close();
    }

    private void drawSunlitIslandDiamond(Canvas canvas, float centerX, float centerY,
                                         float halfWidth, float halfHeight, float cornerRadius,
                                         int baseColor, IslandSunlight sunlight) {
        int tileColor = sunlight.strength > 0.015
                ? blend(baseColor, Color.rgb(182, 208, 161), 0.10 * sunlight.strength)
                : baseColor;
        drawSmoothIslandDiamond(canvas, centerX, centerY, halfWidth, halfHeight,
                cornerRadius, tileColor, tileColor, 0, 0.0f);
    }

    private void drawSmoothIslandPath(Canvas canvas, Path path,
                                      float minX, float minY, float maxX, float maxY,
                                      int lightColor, int shadeColor,
                                      int outlineColor, float outlineWidth) {
        islandPolyPaint.setStyle(Paint.Style.FILL);
        if (lightColor == shadeColor) {
            islandPolyPaint.setColor(lightColor);
            islandPolyPaint.setShader(null);
        } else {
            islandPolyPaint.setColor(Color.WHITE);
            islandPolyPaint.setShader(new LinearGradient(minX, minY, maxX, maxY,
                    lightColor, shadeColor, Shader.TileMode.CLAMP));
        }
        canvas.drawPath(path, islandPolyPaint);
        islandPolyPaint.setShader(null);
        if (outlineWidth > 0.0f && outlineColor != 0) {
            islandPolyPaint.setStyle(Paint.Style.STROKE);
            islandPolyPaint.setStrokeWidth(outlineWidth);
            islandPolyPaint.setColor(outlineColor);
            canvas.drawPath(path, islandPolyPaint);
        }
        islandPolyPaint.setStyle(Paint.Style.FILL);
    }

    private void drawSmoothIslandLine(Canvas canvas, float x1, float y1,
                                      float x2, float y2, int color, float width) {
        islandPolyPaint.setShader(null);
        islandPolyPaint.setStyle(Paint.Style.STROKE);
        islandPolyPaint.setStrokeWidth(width);
        islandPolyPaint.setColor(color);
        canvas.drawLine(x1, y1, x2, y2, islandPolyPaint);
        islandPolyPaint.setStyle(Paint.Style.FILL);
    }

        private void drawIslandPolygon(Canvas canvas,
                                   float x1, float y1, float x2, float y2,
                                   float x3, float y3, float x4, float y4,
                                   int fill, int outline) {
        shapePath.reset();
        shapePath.moveTo(x1, y1);
        shapePath.lineTo(x2, y2);
        shapePath.lineTo(x3, y3);
        shapePath.lineTo(x4, y4);
        shapePath.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(0.5f);
        paint.setAlpha(255);
        paint.setColor(fill);
        canvas.drawPath(shapePath, paint);
        if (outline != 0) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(outline);
            canvas.drawPath(shapePath, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void strokeIslandPolygon(Canvas canvas,
                                     float x1, float y1, float x2, float y2,
                                     float x3, float y3, float x4, float y4,
                                     int color) {
        shapePath.reset();
        shapePath.moveTo(x1, y1);
        shapePath.lineTo(x2, y2);
        shapePath.lineTo(x3, y3);
        shapePath.lineTo(x4, y4);
        shapePath.close();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.5f);
        paint.setAlpha(255);
        paint.setColor(color);
        canvas.drawPath(shapePath, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawIslandLine(Canvas canvas, float x1, float y1, float x2, float y2, int color) {
        shapePath.reset();
        shapePath.moveTo(x1, y1);
        shapePath.lineTo(x2, y2);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.5f);
        paint.setAlpha(255);
        paint.setColor(color);
        canvas.drawPath(shapePath, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawPrecipitation(Canvas canvas, double elapsedSeconds) {
        if (raindrops.length > 0) {
            double windSpeed = weather == null ? 4.0 : weather.windSpeedKmh;
            double windDirection = weather == null ? 90.0 : weather.windDirectionDegrees;
            float windDrift = (float) Math.sin(Math.toRadians(windDirection + 180.0 - headingDegrees))
                    * (float) clamp(windSpeed * 0.045, 0.2, 3.2);
            for (Raindrop drop : raindrops) {
                float sway = (float) Math.sin(elapsedSeconds * drop.swayFrequency + drop.phase)
                        * drop.swayAmplitude;
                float x = wrap(drop.x + elapsedSeconds * (windDrift + drop.windDrift) + sway, SCENE_WIDTH);
                float y = wrap(drop.y + elapsedSeconds * drop.speed, SCENE_HEIGHT + drop.length) - drop.length;
                float pixelX = (float) Math.floor(x * RENDER_SCALE) / RENDER_SCALE;
                float pixelY = (float) Math.floor(y * RENDER_SCALE) / RENDER_SCALE;
                paint.setColor(drop.color);
                paint.setAlpha(drop.alpha);
                canvas.drawRect(pixelX, pixelY,
                        pixelX + drop.width, pixelY + drop.length, paint);
            }
            paint.setAlpha(255);
        }

    }

    private void drawFog(Canvas canvas, int code) {
        if (code != 45 && code != 48) return;
        paint.setColor(Color.argb(48, 205, 211, 207));
        canvas.drawRect(0, 12, SCENE_WIDTH, 23, paint);
        paint.setColor(Color.argb(32, 226, 222, 209));
        canvas.drawRect(0, 31, SCENE_WIDTH, 43, paint);
    }

    private void drawStormFlash(Canvas canvas, double elapsedSeconds, int code) {
        if (code < 95 || code > 99) return;
        double phase = elapsedSeconds % 9.0;
        double intensity = Math.max(0.0, 1.0 - Math.abs(phase - 4.2) / 0.22);
        if (intensity <= 0.0) return;
        paint.setColor(Color.argb((int) (15 * intensity), 201, 216, 241));
        canvas.drawRect(0, 0, SCENE_WIDTH, SCENE_HEIGHT * 0.25f, paint);
        paint.setColor(Color.argb((int) (190 * intensity), 235, 230, 207));
        canvas.drawRect(221, 4, 225, 12, paint);
        canvas.drawRect(216, 12, 223, 17, paint);
        canvas.drawRect(220, 17, 224, 27, paint);
        canvas.drawRect(214, 26, 221, 31, paint);
        canvas.drawRect(212, 31, 216, 43, paint);
        paint.setAlpha(255);
    }

    private Bitmap cloudSprite(boolean bank) {
        int key = bank ? 1 : 0;
        Bitmap cached = cloudSprites.get(key);
        if (cached != null) return cached;
        String packageName = getContext().getPackageName();
        int resource = getResources().getIdentifier(
                bank ? CLOUD_BANK_SPRITE : CLOUD_MASS_SPRITE, "drawable", packageName);
        if (resource == 0) return null;
        Bitmap sprite = BitmapFactory.decodeResource(getResources(), resource);
        if (sprite == null) return null;
        cloudSprites.put(key, sprite);
        return sprite;
    }

    private Bitmap tintedCloudSprite(boolean bank, int tintColor, float tintAmount) {
        Bitmap sprite = cloudSprite(bank);
        if (sprite == null) return null;
        String key = (bank ? "bank" : "mass") + "|" + tintColor + "|"
                + Math.round(tintAmount * 32.0f);
        Bitmap cached = cloudTintCache.get(key);
        if (cached != null) return cached;
        Bitmap buffer = Bitmap.createBitmap(sprite.getWidth(), sprite.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas bufferCanvas = new Canvas(buffer);
        bufferCanvas.drawBitmap(sprite, 0, 0, null);
        if (tintAmount > 0.01f) {
            int alpha = Math.round(255.0f * Math.min(1.0f, tintAmount));
            int tint = Color.argb(alpha, Color.red(tintColor), Color.green(tintColor),
                    Color.blue(tintColor));
            // SOURCE_ATOP keeps the sprite's soft edges while shifting its colour.
            Paint tintPaint = new Paint();
            tintPaint.setColor(tint);
            tintPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
            bufferCanvas.drawRect(0, 0, sprite.getWidth(), sprite.getHeight(), tintPaint);
        }
        if (cloudTintCache.size() >= CLOUD_TINT_CACHE_LIMIT) cloudTintCache.clear();
        cloudTintCache.put(key, buffer);
        return buffer;
    }

    private void drawCloudSpriteAt(Canvas canvas, Bitmap sprite, float x, float y,
                                   float height, boolean mirror) {
        float scale = height / sprite.getHeight();
        float width = sprite.getWidth() * scale;
        if (mirror) {
            cloudMatrix.setScale(-1.0f, 1.0f);
            cloudMatrix.postTranslate(x + width, y);
            canvas.drawBitmap(sprite, cloudMatrix, cloudPaint);
        } else {
            canvas.drawBitmap(sprite, x, y, cloudPaint);
        }
    }

    private CloudTint cloudWeatherTint(double sunAltitude, double cloudCover, int code,
                                       float baseTint, int baseTintColor) {
        boolean storm = code >= 95;
        double cover = clamp(cloudCover / 100.0, 0.0, 1.0);
        double night = quantise(clamp(1.0 - sunAltitude / 8.0, 0.0, 1.0) * 0.85);
        double gloom = quantise(clamp(cover * 0.20 + (storm ? 0.22 : 0.0), 0.0, 0.55));
        double warm = quantise(clamp(sunAltitude / 40.0, 0.0, 1.0) * (1.0 - cover) * 0.35);
        int tintColor = blend(blend(blend(CLOUD_WARM_TINT, CLOUD_NIGHT_TINT, night),
                CLOUD_STORM_TINT, gloom), baseTintColor, baseTint * 0.6);
        float tintAmount = (float) clamp(baseTint + night * 0.55 + gloom * 0.45
                + warm * 0.25, 0.0, 0.92);
        return new CloudTint(tintColor, tintAmount);
    }

    // A distant bank of cloud behind the sea fills the windows between the island's
    // underside and the cloud tops, so the island sits against cloud, never against sky.
    private void drawDistantCloudHaze(Canvas canvas, double sunAltitude, double cloudCover,
                                      int code) {
        boolean storm = code >= 95;
        double cover = clamp(cloudCover / 100.0, 0.0, 1.0);
        double night = clamp(1.0 - sunAltitude / 8.0, 0.0, 1.0) * 0.85;
        double gloom = clamp(cover * 0.30 + (storm ? 0.26 : 0.0), 0.0, 0.55);
        paint.setShader(new LinearGradient(0, CLOUD_HAZE_TOP, 0, CLOUD_SEA_BASE + 10.0f,
                new int[] {
                        0x00d6e3ef,
                        blend(blend(Color.rgb(211, 225, 238), CLOUD_NIGHT_TINT, night),
                                CLOUD_STORM_TINT, gloom * 0.7),
                        blend(blend(Color.rgb(198, 216, 232), CLOUD_NIGHT_TINT, night),
                                CLOUD_STORM_TINT, gloom * 0.8)
                },
                new float[] {0.0f, 0.55f, 1.0f}, Shader.TileMode.CLAMP));
        paint.setAlpha(217);
        canvas.drawRect(0, CLOUD_HAZE_TOP, SCENE_WIDTH, CLOUD_SEA_BASE + 10.0f, paint);
        paint.setAlpha(255);
        paint.setShader(null);
    }

    private void drawCloudSea(Canvas canvas, double sunAltitude, double cloudCover, int code) {
        paint.setStyle(Paint.Style.FILL);
        boolean storm = code >= 95;
        double cover = clamp(cloudCover / 100.0, 0.0, 1.0);
        double night = quantise(clamp(1.0 - sunAltitude / 8.0, 0.0, 1.0) * 0.85);
        double gloom = quantise(clamp(cover * 0.30 + (storm ? 0.26 : 0.0), 0.0, 0.55));

        // A soft cloud-toned body under the sprites hides sky gaps between puffs. It fades in
        // from transparent at its top edge so the sea has no hard horizon line.
        paint.setShader(new LinearGradient(0, CLOUD_SEA_TOP, 0, SCENE_HEIGHT,
                new int[] {
                        0x00000000,
                        blend(blend(Color.rgb(196, 214, 229), CLOUD_NIGHT_TINT, night),
                                CLOUD_STORM_TINT, gloom * 0.6),
                        blend(blend(Color.rgb(159, 182, 204), CLOUD_NIGHT_TINT, night),
                                CLOUD_STORM_TINT, gloom * 0.7)
                },
                new float[] {0.0f, 0.35f, 1.0f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, CLOUD_SEA_TOP, SCENE_WIDTH, SCENE_HEIGHT, paint);
        paint.setShader(null);

        for (CloudLayer layer : CLOUD_LAYERS) {
            Random random = new Random(layer.salt);
            CloudTint tint = cloudWeatherTint(sunAltitude, cloudCover, code,
                    layer.tint, layer.tintColor);
            Bitmap bank = tintedCloudSprite(true, tint.color, tint.amount);
            Bitmap mass = tintedCloudSprite(false, tint.color, tint.amount);
            if (bank == null || mass == null) continue;
            cloudPaint.setAlpha(Math.round(255.0f * layer.alpha));
            float step = layer.height * 0.62f;
            for (float x = -step; x < SCENE_WIDTH + step; x += step) {
                float height = layer.height * layer.rowScale * (0.9f + random.nextFloat() * 0.22f);
                boolean useMass = random.nextFloat() < 0.5f;
                boolean mirror = random.nextFloat() < 0.5f;
                float puffX = x - step * 0.25f + random.nextFloat() * step * 0.5f;
                float puffY = layer.baseline - height * (0.9f + random.nextFloat() * 0.12f);
                drawCloudSpriteAt(canvas, useMass ? mass : bank, puffX, puffY, height, mirror);
            }
        }
        cloudPaint.setAlpha(255);
    }

    // Side and front banks overlap the island itself, so they draw after it.
    // Far-side clouds behind the island; the island and its foreground banks then cover them.
    private void drawBackCloudBanks(Canvas canvas, double sunAltitude, double cloudCover,
                                    int code) {
        // The far layer drifts too, at roughly a tenth of the front group's speed.
        double elapsedSeconds = animationEnabled
                ? (System.currentTimeMillis() - animationStartMillis) / 1000.0 : 0.0;
        for (CloudBank bank : CLOUD_BACK_BANKS) {
            CloudTint tint = cloudWeatherTint(sunAltitude, cloudCover, code,
                    bank.tint, CLOUD_NEAR_TINT);
            Bitmap sprite = tintedCloudSprite(bank.bank, tint.color, tint.amount);
            if (sprite == null) continue;
            float scale = bank.height / sprite.getHeight();
            float width = sprite.getWidth() * scale;
            double phase = elapsedSeconds * bank.speed + bank.phase;
            float offsetX = bank.sway > 0.0f ? (float) Math.sin(phase) * bank.sway : 0.0f;
            float offsetY = bank.bob > 0.0f ? (float) Math.cos(phase * 0.7) * bank.bob : 0.0f;
            cloudPaint.setAlpha(Math.round(bank.alpha * 255.0f));
            drawCloudSpriteAt(canvas, sprite,
                    bank.cx * SCENE_WIDTH - width / 2.0f + offsetX,
                    bank.top + offsetY, bank.height, bank.cx < 0.5f);
        }
        cloudPaint.setAlpha(255);
    }

    // Contact shade drawn after the banks: the crowns that touch the slab are darkened by the
    // island too, which is what stops them reading as pasted onto the soil.
    private void drawCloudContactShade(Canvas canvas, double sunAltitude, double cloudCover,
                                       int code) {
        boolean storm = code >= 95;
        double directLight = clamp((sunAltitude + 8.0) / 28.0, 0.0, 1.0);
        double filter = 1.0 - clamp(cloudCover / 100.0 * 0.45, 0.0, 0.45);
        if (storm) filter *= 0.82;
        double opacity = (0.12 + directLight * 0.22) * filter;
        float centerX = SCENE_WIDTH / 2.0f;
        float tipY = 152.0f;
        float radiusX = ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH * 0.46f;
        int core = Color.argb((int) Math.round(opacity * 255), 44, 62, 90);
        int mid = Color.argb((int) Math.round(opacity * 0.6 * 255), 48, 68, 96);
        paint.setShader(new RadialGradient(centerX, tipY, radiusX,
                new int[] {core, mid, 0x00344a68},
                new float[] {0.0f, 0.5f, 1.0f}, Shader.TileMode.CLAMP));
        canvas.save();
        canvas.translate(centerX, tipY);
        canvas.scale(1.0f, 0.42f);
        canvas.translate(-centerX, -tipY);
        canvas.drawCircle(centerX, tipY, radiusX, paint);
        canvas.restore();
        paint.setShader(null);
    }

    private void drawCloudBanks(Canvas canvas, double sunAltitude, double cloudCover, int code) {
        // Only the frontmost group animates; everything else stays perfectly still.
        double elapsedSeconds = animationEnabled
                ? (System.currentTimeMillis() - animationStartMillis) / 1000.0 : 0.0;
        for (CloudBank[] banks : new CloudBank[][] {CLOUD_GAP_FILLERS, CLOUD_SIDE_BANKS,
                CLOUD_BLANKET_BANKS, CLOUD_CRADLE_BANKS, CLOUD_FRONT_BANKS}) {
            for (CloudBank bank : banks) {
                CloudTint tint = cloudWeatherTint(sunAltitude, cloudCover, code,
                        bank.tint, CLOUD_NEAR_TINT);
                Bitmap sprite = tintedCloudSprite(bank.bank, tint.color, tint.amount);
                if (sprite == null) continue;
                float scale = bank.height / sprite.getHeight();
                float width = sprite.getWidth() * scale;
                double phase = elapsedSeconds * bank.speed + bank.phase;
                float offsetX = bank.sway > 0.0f ? (float) Math.sin(phase) * bank.sway : 0.0f;
                float offsetY = bank.bob > 0.0f ? (float) Math.cos(phase * 0.7) * bank.bob : 0.0f;
                cloudPaint.setAlpha(255);
                drawCloudSpriteAt(canvas, sprite,
                        bank.cx * SCENE_WIDTH - width / 2.0f + offsetX,
                        bank.top + offsetY, bank.height, bank.cx < 0.5f);
            }
        }
    }

    // A soft contact shadow where the island's mass darkens the cloud tops beneath it.
    private void drawIslandShadowOnClouds(Canvas canvas, float centerX, double sunAltitude,
                                          double cloudCover, int code) {
        double directLight = clamp((sunAltitude + 8.0) / 28.0, 0.0, 1.0);
        double cloudFilter = 1.0 - clamp(cloudCover / 100.0 * 0.45, 0.0, 0.45);
        if (code >= 95) cloudFilter *= 0.82;
        double opacity = (0.10 + directLight * 0.26) * cloudFilter;
        paint.setAlpha(255);
        float shadowY = CLOUD_SEA_BASE + 2.0f;
        float radiusX = ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH * 0.52f;
        float radiusY = 11.0f;
        paint.setShader(new RadialGradient(centerX, shadowY, radiusX,
                new int[] {
                        Color.argb((int) (255.0 * opacity), 48, 68, 96),
                        Color.argb((int) (255.0 * opacity * 0.55), 52, 74, 104),
                        Color.argb(0, 52, 74, 104)
                },
                new float[] {0.0f, 0.55f, 1.0f}, Shader.TileMode.CLAMP));
        canvas.save();
        canvas.translate(centerX, shadowY);
        canvas.scale(1.0f, radiusY / radiusX);
        canvas.translate(-centerX, -shadowY);
        canvas.drawRect(centerX - radiusX, shadowY - radiusX,
                centerX + radiusX, shadowY + radiusX, paint);
        canvas.restore();
        paint.setShader(null);
    }

    private static double quantise(double value) {
        return Math.round(value * 16.0) / 16.0;
    }

    private static final class CloudLayer {
        final float baseline;
        final float height;
        float alpha;
        final float tint;
        final int tintColor;
        final int salt;
        final float rowScale;

        CloudLayer(float baseline, float height, float alpha, float tint, int tintColor,
                   int salt, float rowScale) {
            this.baseline = baseline;
            this.height = height;
            this.alpha = alpha;
            this.tint = tint;
            this.tintColor = tintColor;
            this.salt = salt;
            this.rowScale = rowScale;
        }
    }

    private static final class CloudTint {
        final int color;
        final float amount;

        CloudTint(int color, float amount) {
            this.color = color;
            this.amount = amount;
        }
    }

    private static int paletteColor(int[] palette, double amount) {
        double position = clamp(amount, 0.0, 1.0) * (palette.length - 1);
        int index = Math.min(palette.length - 2, (int) Math.floor(position));
        return blend(palette[index], palette[index + 1], position - index);
    }

    private static int blend(int first, int second, double amount) {
        double t = clamp(amount, 0.0, 1.0);
        return Color.rgb(
                (int) Math.round(Color.red(first) + (Color.red(second) - Color.red(first)) * t),
                (int) Math.round(Color.green(first) + (Color.green(second) - Color.green(first)) * t),
                (int) Math.round(Color.blue(first) + (Color.blue(second) - Color.blue(first)) * t));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float wrap(double value, double range) {
        return (float) (((value % range) + range) % range);
    }

    private static double wrapCentered(double value, double range) {
        return (((value + range * 0.5) % range) + range) % range - range * 0.5;
    }

    private static double reflectCentered(double value, double halfRange) {
        double phase = (((value + halfRange) % (4.0 * halfRange))
                + 4.0 * halfRange) % (4.0 * halfRange);
        return phase <= 2.0 * halfRange
                ? phase - halfRange
                : 3.0 * halfRange - phase;
    }

    private static double cloudRandom(int seed, int index, int salt) {
        int value = seed ^ ((index + 1) * 0x9e3779b9) ^ ((salt + 1) * 0x85ebca6b);
        value = (value ^ (value >>> 16)) * 0x7feb352d;
        value = (value ^ (value >>> 15)) * 0x846ca68b;
        value ^= value >>> 16;
        return Integer.toUnsignedLong(value) / 4294967296.0;
    }

    private static double normalizeDegrees(double value) {
        return ((value % 360.0) + 360.0) % 360.0;
    }

    private static int interpolate(int first, int second, double amount) {
        return (int) Math.round(first + (second - first) * clamp(amount, 0.0, 1.0));
    }

    private static int[] colors(String... values) {
        int[] result = new int[values.length];
        for (int index = 0; index < values.length; index++) result[index] = Color.parseColor(values[index]);
        return result;
    }

    private int colorForSkyHeight(double y, double sunAltitude, double cloudCover) {
        int[] palette = sunAltitude < -12.0 ? NIGHT_SKY
                : sunAltitude > 2.0 ? DAY_SKY : DAWN_SKY;
        int base = paletteColor(palette, clamp(y, 0.0, 1.0));
        int overcast = Color.rgb(88, 101, 118);
        return blend(base, overcast, clamp(cloudCover / 100.0 * 0.58, 0.0, 0.58));
    }

    private void drawPixelDisc(Canvas canvas, int centerX, int centerY, int radius,
                               float cell, int color) {
        paint.setColor(color);
        for (float row = -radius; row <= radius; row += cell) {
            double sampleY = row + cell / 2.0;
            int half = (int) Math.floor(Math.sqrt(Math.max(0.0, radius * radius - sampleY * sampleY)));
            float left = (float) (Math.floor((centerX - half) / cell) * cell);
            float right = (float) (Math.ceil((centerX + half) / cell) * cell);
            canvas.drawRect(left, centerY + row, right, centerY + row + cell, paint);
        }
    }

    private float mapAltitudeToCelestialBand(double altitude) {
        double visibleAltitude = clamp(altitude, 0.0, 90.0);
        return (float) (CELESTIAL_BAND_TOP
                + (1.0 - visibleAltitude / 90.0) * CELESTIAL_BAND_HEIGHT);
    }

    private float mapAzimuthToX(double azimuth) {
        double relative = normalizeDegrees(azimuth - headingDegrees + 180.0);
        return (float) (relative / 360.0 * SCENE_WIDTH);
    }

    private float mapCloudAltitudeToY(double altitude) {
        double visibleAltitude = clamp(altitude, 0.0, 90.0);
        double verticalMix = Math.pow(visibleAltitude / 90.0, CLOUD_VISUAL_ALTITUDE_EXPONENT);
        return (float) ((1.0 - verticalMix) * CLOUD_VISUAL_HORIZON_Y);
    }

    private float mapSunToUpperEdgeX(double hourAngleDegrees) {
        double distance = sunEdgeDistance(hourAngleDegrees);
        double sideLength = SCENE_HEIGHT / 3.0 - SUN_GLOW_SAFE_INSET;
        double topHalfWidth = SCENE_WIDTH / 2.0 - SUN_GLOW_SAFE_INSET;
        double centerX = SCENE_WIDTH / 2.0;
        double rightX = SCENE_WIDTH - SUN_GLOW_SAFE_INSET;
        double leftX = SUN_GLOW_SAFE_INSET;
        if (hourAngleDegrees <= 0.0) {
            return (float) (distance <= sideLength
                    ? rightX : rightX - (distance - sideLength));
        }
        return (float) (distance <= topHalfWidth ? centerX - distance : leftX);
    }

    private float mapSunToUpperEdgeY(double hourAngleDegrees) {
        double distance = sunEdgeDistance(hourAngleDegrees);
        double sideLength = SCENE_HEIGHT / 3.0 - SUN_GLOW_SAFE_INSET;
        double topHalfWidth = SCENE_WIDTH / 2.0 - SUN_GLOW_SAFE_INSET;
        double topY = SUN_GLOW_SAFE_INSET;
        double sideY = SCENE_HEIGHT / 3.0;
        if (hourAngleDegrees <= 0.0) {
            return (float) (distance <= sideLength ? sideY - distance : topY);
        }
        return (float) (distance <= topHalfWidth ? topY : topY + distance - topHalfWidth);
    }

    private double sunEdgeDistance(double hourAngleDegrees) {
        double sideLength = SCENE_HEIGHT / 3.0 - SUN_GLOW_SAFE_INSET;
        double topHalfWidth = SCENE_WIDTH / 2.0 - SUN_GLOW_SAFE_INSET;
        double halfPathLength = sideLength + topHalfWidth;
        double dayFraction = hourAngleDegrees <= 0.0
                ? clamp((hourAngleDegrees + 90.0) / 90.0, 0.0, 1.0)
                : clamp(hourAngleDegrees / 90.0, 0.0, 1.0);
        return dayFraction * halfPathLength;
    }

    private void rebuildWeatherParticles(WeatherSnapshot snapshot) {
        boolean drizzle = snapshot.hasDrizzle();
        boolean raining = snapshot.hasRain() || snapshot.hasStorm();
        double intensity = Math.max(snapshot.precipitationMm, snapshot.rainMm);
        int rainCount = !raining && !drizzle ? 0
                : (snapshot.hasStorm() ? 145 : drizzle ? 48 : 76)
                + (int) Math.min(55.0, intensity * 12.0);
        Random random = new Random();

        raindrops = new Raindrop[rainCount];
        for (int index = 0; index < rainCount; index++) {
            boolean storm = snapshot.hasStorm();
            float speed = drizzle ? 14.0f + random.nextFloat() * 27.0f
                    : storm ? 55.0f + random.nextFloat() * 48.0f
                    : 34.0f + random.nextFloat() * 42.0f;
            float length = drizzle ? 0.5f + random.nextInt(3) * 0.5f
                    : 2.5f + random.nextInt(storm ? 10 : 6) * 0.5f;
            float width = 0.5f + random.nextInt(2) * 0.5f;
            raindrops[index] = new Raindrop(
                    random.nextFloat() * SCENE_WIDTH,
                    random.nextFloat() * SCENE_HEIGHT,
                    speed,
                    length,
                    width,
                    (random.nextFloat() - 0.5f) * (drizzle ? 0.85f : 0.45f),
                    0.15f + random.nextFloat() * (drizzle ? 1.5f : 0.8f),
                    0.5f + random.nextFloat() * 1.5f,
                    random.nextFloat() * 6.28f,
                    drizzle ? Color.rgb(188, 211, 215) : Color.rgb(158, 188, 207),
                    drizzle ? 55 + random.nextInt(95) : 130 + random.nextInt(90));
        }
    }

    private static final class CloudBank {
        final float cx;
        final float top;
        final float height;
        final float tint;
        /** true = cloud bank sprite, false = cloud mass sprite. */
        final boolean bank;
        /** Frontmost banks only: sway in scene units, vertical bob, angular speed, phase. */
        final float sway;
        final float bob;
        final float speed;
        final float phase;
        /** 0..1 opacity; near passes use full strength, the far layer is hazier. */
        final float alpha;

        CloudBank(float cx, float top, float height, float tint, boolean bank) {
            this(cx, top, height, tint, bank, 0.0f, 0.0f, 0.0f, 0.0f, 0.92f);
        }

        CloudBank(float cx, float top, float height, float tint, boolean bank,
                  float sway, float bob, float speed, float phase) {
            this(cx, top, height, tint, bank, sway, bob, speed, phase, 0.92f);
        }

        CloudBank(float cx, float top, float height, float tint, boolean bank,
                  float sway, float bob, float speed, float phase, float alpha) {
            this.cx = cx;
            this.top = top;
            this.height = height;
            this.tint = tint;
            this.bank = bank;
            this.sway = sway;
            this.bob = bob;
            this.speed = speed;
            this.phase = phase;
            this.alpha = alpha;
        }
    }

    private static final class Raindrop {
        final float x;
        final float y;
        final float speed;
        final float length;
        final float width;
        final float windDrift;
        final float swayAmplitude;
        final float swayFrequency;
        final float phase;
        final int color;
        final int alpha;

        Raindrop(float x, float y, float speed, float length, float width,
                 float windDrift, float swayAmplitude, float swayFrequency, float phase,
                 int color, int alpha) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.length = length;
            this.width = width;
            this.windDrift = windDrift;
            this.swayAmplitude = swayAmplitude;
            this.swayFrequency = swayFrequency;
            this.phase = phase;
            this.color = color;
            this.alpha = alpha;
        }
    }
}
