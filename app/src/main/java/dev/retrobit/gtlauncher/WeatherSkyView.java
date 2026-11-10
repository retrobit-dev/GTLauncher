package dev.retrobit.gtlauncher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/** Full-screen, non-scrolling pixel-art sky driven by local astronomy and weather data. */
public final class WeatherSkyView extends View {
    private static final int SCENE_WIDTH = 320;
    private static final int SCENE_HEIGHT = 200;
    // A 2x backing bitmap makes half-scene-unit clusters land on crisp source pixels.
    private static final int RENDER_SCALE = 2;
    /** One pixel-art cell in scene units, matching the preview renderer. */
    private static final float RENDER_CELL = 1.0f / RENDER_SCALE;
    private static final int RENDER_WIDTH = SCENE_WIDTH * RENDER_SCALE;
    private static final int RENDER_HEIGHT = SCENE_HEIGHT * RENDER_SCALE;
    private static final int ISLAND_GRID_SIZE = 9;
    private static final int ISLAND_TILE_WIDTH = 28;
    private static final String ISLAND_COLUMN_LABELS = "ABCDEFGHI";
    // Logical grid IDs are A1–I9: columns A–I left to right, rows 1–9 top to bottom.
    // Diamond compass frame is rotated 45° clockwise from the screen diagonal so the grid
    // axes are cardinal: moving along a column runs east–west and moving along a row runs
    // north–south. Top corner=NW, upper-right edge=N, right corner=NE, lower-right edge=E,
    // bottom=SE, lower-left edge=S, left=SW, and upper-left edge=W.
    // A row of tiles is a parallel of latitude; a column of tiles is a meridian of longitude.
    // Because the frame is shifted 45°, the camera now faces northwest instead of north.
    private static final float COMPASS_FRAME_OFFSET_DEGREES = 45.0f;
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
    private static final int ISLAND_VERTICAL_LIFT = 10;
    private static final int ISLAND_BOTTOM_MARGIN = ISLAND_BASE_BOTTOM_MARGIN + ISLAND_VERTICAL_LIFT;
    private static final float ISLAND_TOP = SCENE_HEIGHT - ISLAND_BOTTOM_MARGIN - ISLAND_DEPTH
            - ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT;
    // Keep celestial centers in the upper third with enough margin for the Sun's glow.
    // The sun is a plain pixel disc shaded with a soft two-colour vertical gradient: the warm
    // light tone at the top fading into the deeper tone at the bottom. No rays, no facets.
    private static final float SUN_BODY_RADIUS = 10.5f;
    // East and west ends of the celestial arc, inset so the disc never clips the frame.
    // East sits on the right of the screen and west on the left, so the sun rises on the right
    // and sets on the left, which is the reading most people expect from a sky panel.
    private static final float SUN_RISE_X = 284.0f;
    private static final float SUN_SET_X = 36.0f;
    private static final float ARC_TILT = 9.0f;
    // The moon only appears once the sun has dropped below this altitude.
    private static final double MOON_VISIBLE_BELOW_SUN = -2.0;
    private static final int GLOW_STEPS = 14;
    private static final float GLOW_REACH = 15.0f;
    private static final double GLOW_ALPHA = 0.040;
    private static final double GLOW_PULSE_SPEED = 1.15;

    private static final float CELESTIAL_BAND_TOP = SCENE_HEIGHT * 0.10f;
    private static final float CELESTIAL_BAND_HEIGHT = SCENE_HEIGHT / 3.0f
            - CELESTIAL_BAND_TOP;
    // The Sun's outer halo reaches about 18 scene units from its center.
    private static final float SUN_GLOW_SAFE_INSET = 20.0f;
    // Temporarily hide sky objects while retaining their renderer for later design iterations.
    // The previous sky objects (pixel sun, moon and star field) were removed. Weather still
    // drives the sky gradient, fog and lightning, and the sun's position still drives the
    // island lighting; only the drawn celestial bodies are gone while the replacement is designed.
    // This stylized 9 x 9 tile surface represents a 2.25 km square ground patch.
    private static final double ISLAND_TILE_METERS = 250.0;
    private static final float ISLAND_CENTER_Y = ISLAND_TOP + ISLAND_TILE_HEIGHT / 2.0f
            + (ISLAND_GRID_SIZE - 1) * ISLAND_TILE_HEIGHT / 2.0f;
    // Clouds are raster sprites traced from reference art, downsampled to a 30-column grid and
    // quantised into three flat bands. One cell value drives both axes, so each silhouette keeps
    // the ratio it was traced at instead of stretching into a flat smudge.
    // One cloud per step cleared; the first step is 0 so even a clear sky keeps one cloud.
    private static final int[] CLOUD_COVER_STEPS = {0, 32, 58, 82};
    // Half the previous sprite size. The previous cell ran 1.45-2.10 scene units, so half is
    // 0.73-1.05; 1 is the only whole-pixel step in that band at RENDER_SCALE 2, and it keeps every
    // cell edge on the art grid. A fractional cell left antialiased seams across the cloud face.
    private static final float CLOUD_CELL = 1.0f;
    // Slots are absolute scene units so the widest sprite still clears both frame edges. The last
    // two sit above the island so their shadows fall on its top surface.
    private static final float[] CLOUD_SLOT_X = {56.0f, 264.0f, 118.0f, 206.0f};
    // Vertical ties per slot, used as a tiebreak when several clouds share the band.
    private static final float[] CLOUD_SLOT_ORDER = {0.12f, 0.05f, 0.68f, 0.9f};
    // Clouds must hang below the sun so the light can reach past them and drop their shadow on the
    // island. They spread from just under the sun down over the island's back half.
    private static final float CLOUD_SUN_CLEARANCE = 4.0f;
    private static final float CLOUD_BAND_REACH = 34.0f;
    private static final float CLOUD_BAND_MIN_SPAN = 26.0f;
    private static final float CLOUD_SCREEN_TOP_MARGIN = 4.0f;
    // Clouds travel across the frame like real wind-driven cloud, then re-enter from the other
    // side. Screen speed is derived from the reported wind so a breeze crawls and a storm hurries.
    private static final double CLOUD_DRIFT_BASE = 0.55;
    private static final double CLOUD_DRIFT_WIND_GAIN = 0.16;
    private static final double CLOUD_DRIFT_MIN = 0.18;
    private static final double CLOUD_DRIFT_MAX = 5.5;
    private static final float CLOUD_EDGE_INSET = 4.0f;
    // A soft, dark tint so the shadow reads as shade on grass rather than a grey sticker.
    private static final int CLOUD_SHADOW_ON_ISLAND_COLOR = Color.argb(36, 17, 26, 39);
    // The island's own cast shadow: close to the base, drifting with the light direction.
    private static final float ISLAND_BODY_SHADOW_SCALE = 1.55f;
    private static final float ISLAND_BODY_SHADOW_OPACITY_SCALE = 0.46f;
    private static final float ISLAND_BODY_SHADOW_RISE = 1.0f;
    private static final float ISLAND_BODY_SHADOW_DRIFT = 10.0f;
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
    // Traced from reference art, then downsampled and colour-quantised. '0' is empty and
    // '1'..'3' are the three light bands, darkest first. Row counts differ by design.
    private static final String[][] CLOUD_SHAPES = {
            {"000000000000001110011110000000",
                    "000000022211321111111110000000",
                    "000000322223321111111111000000",
                    "000000333333331111111111100000",
                    "000003333333331111111111110000",
                    "000033332333331111111111111000",
                    "033333322233331111111111111100",
                    "333333322233331111111111111100",
                    "333333311133331111111111111110",
                    "333333311113331111111111111111",
                    "333333311112221111111111111111",
                    "032333111112221111111111111111",
                    "000233111111211111111111111110",
                    "000000000000011111000000000000"},
            {"000000000002221110000000000000",
                    "000000000033221111111100000000",
                    "000000223333322111111110000000",
                    "000002233333322111111111000000",
                    "000003333333322211111111111000",
                    "003333333333322211111111111100",
                    "033333333333322211111111111110",
                    "033333333333322211111111111111",
                    "333333333333322211111111111111",
                    "333333333333322211111111111111",
                    "333333333333322211111111111111",
                    "333333333333322211111111111111",
                    "333332233333322111111111111110",
                    "033322223333322111111111111110",
                    "003322113333322111111111111100",
                    "000331110333221111100111110000",
                    "000000000003221110000000000000"},
            {"000000000002222200000000000000",
                    "000000000022222220000000000000",
                    "000000000332222222000000000000",
                    "000001133332222222200000000000",
                    "000033333332211122222222000000",
                    "000333333322111111222222200000",
                    "000333333322111111112222220000",
                    "003333333322111111111122220000",
                    "033333333322111111111111220000",
                    "033323333322111111111111111100",
                    "333111333322111111111111111110",
                    "333111333322111111111111111111",
                    "333111133322111111111111111111",
                    "333111133322111111111111111111",
                    "031111113322111111111111111111",
                    "031111110321111111111111111110",
                    "000111000001111100000000000000"},
            {"000000000000000000222220000000",
                    "000000000111111003222222000000",
                    "000000033111111133311111100000",
                    "000000333311111111111111100000",
                    "002223333311111111111111110000",
                    "032223333311111111111111110000",
                    "333233333311111111111111111000",
                    "333333333311111111111111111110",
                    "333333333331111111111111111110",
                    "333333333331111111111111111111",
                    "333333333331111112111111111111",
                    "033333333331111122211111111111",
                    "033333333311111122211111111111",
                    "000333333311111122211111111110",
                    "000003333311111122211111111110",
                    "000000333311111112111111111000",
                    "000000333111111111111111100000",
                    "000000333111111111111111000000",
                    "000000031111110001111110000000",
                    "000000001111100000000000000000"},
    };

    // Day, dusk, night and storm share one light model: three flat bands, darkest at the base.
    private static final int[] CLOUD_DAY_BANDS = {
            Color.rgb(169, 192, 217), Color.rgb(228, 239, 250), Color.rgb(255, 255, 255)};
    private static final int[] CLOUD_DUSK_BANDS = {
            Color.rgb(140, 127, 156), Color.rgb(187, 174, 198), Color.rgb(239, 215, 210)};
    private static final int[] CLOUD_NIGHT_BANDS = {
            Color.rgb(64, 78, 106), Color.rgb(92, 109, 138), Color.rgb(143, 160, 189)};
    private static final int[] CLOUD_STORM_BANDS = {
            Color.rgb(90, 106, 128), Color.rgb(147, 161, 179), Color.rgb(198, 208, 220)};

    private final Path islandClipPath = new Path();
    private final Paint paint = new Paint();
    private final Paint islandPolyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint renderBitmapPaint = new Paint();
    private final Path shapePath = new Path();
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
        // Rotating the diamond frame by 45° turns the camera to face northwest, so the
        // device heading is shifted to keep the sky aligned with real compass bearings.
        this.headingDegrees = (float) normalizeDegrees(headingDegrees - COMPASS_FRAME_OFFSET_DEGREES);
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
        SkyMath.Position moon = SkyMath.moon(now, latitude, longitude);
        double cloudCover = weather == null ? 42.0 : weather.cloudCoverPercent;
        int code = weather == null ? 3 : weather.weatherCode;
        double windSpeed = weather == null ? 4.0 : weather.windSpeedKmh;
        double windDirection = weather == null ? 90.0 : weather.windDirectionDegrees;
        CloudState[] cloudStates = createCloudStates(motionSeconds, cloudCover,
                windSpeed, windDirection, sun.altitudeDegrees, sun.hourAngleDegrees);

        Canvas scene = sceneCanvas;
        int save = scene.save();
        scene.scale(RENDER_SCALE, RENDER_SCALE);
        drawSky(scene, sun, cloudCover, code);
        int skySave = scene.save();
        // The sky band runs from above the frame down to the horizon; clipping at half height
        // used to behead any cloud tall enough to reach past it.
        scene.clipRect(0, -SCENE_HEIGHT, SCENE_WIDTH, SCENE_HEIGHT * 2);
        drawSun(scene, sun, cloudCover, motionSeconds);
        drawMoon(scene, moon, motionSeconds, sun.altitudeDegrees);
        drawFog(scene, code);
        drawStormFlash(scene, elapsedSeconds, code);
        scene.restoreToCount(skySave);
        drawFloatingIsland(scene, sun.altitudeDegrees, sun.azimuthDegrees,
                sun.hourAngleDegrees, cloudCover, code, moon, cloudStates);
        // Clouds sit in front of the island. They are not clipped to the sky band: a foreground
        // cloud can hang below the horizon line, and clipping it there hid the cloud while its
        // shadow still showed on the grass. Edge clamping already keeps every sprite in frame.
        drawClouds(scene, cloudStates, sun.altitudeDegrees, code);
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
    }

    private float snapCelestialCoordinate(float coordinate) {
        return Math.round(coordinate * RENDER_SCALE) / (float) RENDER_SCALE;
    }

    private void setClipIslandSurface(Canvas canvas) {
        float centerX = SCENE_WIDTH / 2.0f;
        float halfWidth = ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH / 2.0f;
        float halfHeight = ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT / 2.0f;
        islandClipPath.reset();
        islandClipPath.moveTo(centerX, ISLAND_CENTER_Y - halfHeight);
        islandClipPath.lineTo(centerX + halfWidth, ISLAND_CENTER_Y);
        islandClipPath.lineTo(centerX, ISLAND_CENTER_Y + halfHeight);
        islandClipPath.lineTo(centerX - halfWidth, ISLAND_CENTER_Y);
        islandClipPath.close();
        canvas.clipPath(islandClipPath);
    }

    private void drawCloudShadowsOnIsland(Canvas canvas, CloudState[] clouds, double sunAltitude,
            double sunHourAngle, double cloudCover, int code) {
        if (clouds.length == 0) return;
        double altitude = Math.max(sunAltitude, 0.0);
        double elevationSine = Math.max(Math.sin(altitude * Math.PI / 180.0), 0.35);
        double directLight = clamp((altitude + 8.0) / 28.0, 0.0, 1.0);
        double cloudFilter = 1.0 - clamp(cloudCover / 100.0 * 0.45, 0.0, 0.45);
        if (code >= 95) cloudFilter *= 0.82;
        double opacity = (0.10 + directLight * 0.30) * cloudFilter
                * ISLAND_BODY_SHADOW_OPACITY_SCALE * 0.9;
        if (opacity <= 0.002) return;
        // Progress along the arc decides which way the light leans, so the shadow swings with it.
        double lean = (0.5 - celestialProgress(sunHourAngle))
                * ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH * 0.55;
        double throwFactor = Math.min(1.0 / elevationSine, 1.6);
        float halfIsland = ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH * 0.5f;
        int save = canvas.save();
        setClipIslandSurface(canvas);
        for (CloudState cloud : clouds) {
            float sceneWidth = cloud.shape[0].length() * cloud.cell;
            float sceneHeight = cloud.shape.length * cloud.cell;
            // The shadow lands where the cloud appears in its own band: clouds hang at a range
            // where their whole band is only a few degrees above the island, so the ground point
            // is close to the sprite, and this keeps a shadow on the island for every cloud.
            float bandTopCloud = cloudBandTop(sunAltitude, sunHourAngle, sceneHeight);
            float bandSpanCloud = Math.max(cloudBandBottom() - bandTopCloud,
                    CLOUD_BAND_MIN_SPAN);
            float bandMix = (float) clamp(
                    1.0 - (cloud.screenBottomY - bandTopCloud) / bandSpanCloud, 0.0, 1.0);
            float centreX = cloud.screenCenterX
                    + (float) clamp(lean * throwFactor, -sceneWidth, sceneWidth);
            // Kept inside the island: a physical drop would push the shadow off the front edge at
            // this panel size, so the plane stays close enough to read as belonging to its cloud.
            float straightAboveY = ISLAND_CENTER_Y + ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT * 0.12f;
            float centreY = straightAboveY
                    - bandMix * ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT * 0.45f;
            if (Math.abs(centreX - SCENE_WIDTH / 2.0f) > halfIsland * 0.9f) continue;
            float halfWidth = sceneWidth * 0.85f;
            float halfHeight = halfWidth * (ISLAND_TILE_HEIGHT / ISLAND_TILE_WIDTH);
            drawIslandPolygon(canvas,
                    centreX, centreY - halfHeight,
                    centreX + halfWidth, centreY,
                    centreX, centreY + halfHeight,
                    centreX - halfWidth, centreY,
                    Color.argb((int) (255.0 * opacity), 12, 23, 40), 0);
        }
        canvas.restoreToCount(save);
    }

    private void drawFloatingIsland(Canvas canvas, double sunAltitude, double sunAzimuth,
            double sunHourAngle, double cloudCover, int code,
            SkyMath.Position moon, CloudState[] cloudStates) {
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

        // The sun lights the island by day; once it is gone the moon takes over, so the cast
        // shadow follows whichever body is actually in the sky.
        double lightAltitude = sunAltitude;
        double lightHourAngle = sunHourAngle;
        if (sunAltitude <= 2.0 && moon != null && moon.altitudeDegrees > 2.0) {
            lightAltitude = clamp(moon.altitudeDegrees
                    * (0.35 + moon.illumination * 0.65), 0.0, 90.0);
            lightHourAngle = moon.hourAngleDegrees;
        }
        drawIslandShadow(canvas, centerX, lowerY, lightAltitude, lightHourAngle,
                cloudCover, code);

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
        drawCloudShadowsOnIsland(canvas, cloudStates, sunAltitude, sunHourAngle,
                cloudCover, code);
        int topRim = blend(palette.grassSeam, palette.grassDark, 0.32);
        drawSmoothIslandLine(canvas, leftX, middleY, centerX, bottomY, topRim, 0.60f);
        drawSmoothIslandLine(canvas, centerX, bottomY, rightX, middleY, topRim, 0.60f);
    }

    /**
     * The island's cast shadow is pushed away from whichever body is lighting the scene. It
     * starts from that body's progress along the celestial arc, so the shadow drifts to the
     * opposite side as the sun or moon travels. Offset and size are kept small so the shadow
     * stays tucked close under the island instead of floating away from it.
     */
    private void drawIslandShadow(Canvas canvas, float centerX, float lowerY,
                                  double lightAltitude, double lightHourAngle,
                                  double cloudCover, int code) {
        double directLight = clamp((lightAltitude + 8.0) / 28.0, 0.0, 1.0);
        double cloudFilter = 1.0 - clamp(cloudCover / 100.0 * 0.45, 0.0, 0.45);
        if (code >= 95) cloudFilter *= 0.82;
        double opacity = (0.10 + directLight * 0.30) * cloudFilter
                * ISLAND_BODY_SHADOW_OPACITY_SCALE;
        if (opacity <= 0.002) return;
        double progress = celestialProgress(lightHourAngle);
        float drift = (float) ((0.5 - progress) * 2.0 * ISLAND_BODY_SHADOW_DRIFT);
        float shadowCenterX = centerX - drift;
        float shadowCenterY = lowerY - ISLAND_DEPTH - ISLAND_BODY_SHADOW_RISE;
        float halfWidth = ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH / 4.0f
                * (float) ISLAND_BODY_SHADOW_SCALE;
        float halfHeight = ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT / 4.0f
                * (float) ISLAND_BODY_SHADOW_SCALE;
        int shadowColor = Color.argb((int) (255.0 * opacity), 12, 23, 40);
        drawIslandPolygon(canvas,
                shadowCenterX, shadowCenterY - halfHeight,
                shadowCenterX + halfWidth, shadowCenterY,
                shadowCenterX, shadowCenterY + halfHeight,
                shadowCenterX - halfWidth, shadowCenterY,
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


    /** Light tone on top, deep tone at the bottom, warmed near the horizon and dulled. */
    private static int[] sunPalette(double altitude, double cloudCover) {
        double daylight = clamp((altitude + 6.0) / 18.0, 0.0, 1.0);
        double low = clamp(1.0 - daylight, 0.0, 1.0);
        double dull = clamp(cloudCover / 100.0, 0.0, 1.0) * 0.4;
        int ash = Color.rgb(125, 132, 148);
        int top = blend(blend(Color.rgb(255, 253, 240), Color.rgb(255, 240, 194), low * 0.8),
                ash, dull);
        int bottom = blend(blend(Color.rgb(255, 212, 74), Color.rgb(255, 157, 51), low * 0.8),
                ash, dull);
        return new int[] { top, bottom };
    }

    /** One disc of the halo, flattened vertically by squash. */
    private void glowDisc(Canvas canvas, float centerX, float centerY, float radius,
                          float squash, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        for (float row = -radius; row < radius; row += RENDER_CELL) {
            float sampleY = row + RENDER_CELL / 2.0f;
            float scaledY = sampleY / squash;
            float halfWidth = (float) Math.sqrt(Math.max(0.0,
                    radius * radius - scaledY * scaledY));
            float left = Math.round((centerX - halfWidth) / RENDER_CELL) * RENDER_CELL;
            float right = Math.round((centerX + halfWidth) / RENDER_CELL) * RENDER_CELL;
            if (right > left) {
                canvas.drawRect(left, centerY + row, right, centerY + row + RENDER_CELL, paint);
            }
        }
    }

    /**
     * Many faint stacked discs instead of a few hard rings. The halo breathes: the discs slide
     * outward and back on a slow cycle, which is what makes it visibly alive. Its shape comes
     * from the sun's real position - a low sun spreads it wider and flatter while a high sun
     * keeps it tight and round - so it reflects the current latitude and longitude. Shared by
     * the sun and the moon.
     */
    private void drawCelestialGlow(Canvas canvas, float centerX, float centerY, double altitude,
                                   double elapsedSeconds, int tint, float visibility,
                                   float strength) {
        if (visibility <= 0.0f || strength <= 0.0f) return;
        double high = clamp(clamp(altitude, 0.0, 90.0) / 45.0, 0.0, 1.0);
        float squash = (float) (0.52 + high * 0.48);
        double breathe = 0.5 + 0.5 * Math.sin(elapsedSeconds * GLOW_PULSE_SPEED);
        float reach = (float) (GLOW_REACH * (1.25 - high * 0.55) * (0.68 + breathe * 0.72));
        double alpha = GLOW_ALPHA * (0.55 + breathe * 0.75) * visibility * strength;
        int color = (tint & 0x00FFFFFF) | ((int) Math.round(255.0 * alpha) << 24);
        for (int step = 0; step < GLOW_STEPS; step++) {
            float progress = step / (float) (GLOW_STEPS - 1);
            glowDisc(canvas, centerX, centerY, SUN_BODY_RADIUS + 1.0f + progress * reach,
                    squash, color);
        }
    }

    private void drawSunBody(Canvas canvas, float centerX, float centerY, int[] palette) {
        float radius = SUN_BODY_RADIUS;
        paint.setStyle(Paint.Style.FILL);
        for (float row = -radius; row < radius; row += RENDER_CELL) {
            float sampleY = row + RENDER_CELL / 2.0f;
            float halfWidth = (float) Math.sqrt(Math.max(0.0,
                    radius * radius - sampleY * sampleY));
            float left = Math.round((centerX - halfWidth) / RENDER_CELL) * RENDER_CELL;
            float right = Math.round((centerX + halfWidth) / RENDER_CELL) * RENDER_CELL;
            if (right <= left) continue;
            // 0 at the top of the disc, 1 at the bottom.
            float mix = (float) clamp((sampleY + radius) / (radius * 2.0), 0.0, 1.0);
            paint.setColor(blend(palette[0], palette[1], mix));
            canvas.drawRect(left, centerY + row, right, centerY + row + RENDER_CELL, paint);
        }
    }

    /**
     * Position comes straight from the existing astronomy: azimuth picks the screen column and
     * altitude picks the height inside the upper celestial band. The sun fades in just above
     * the horizon and is gone below it.
     */
    private void drawSun(Canvas canvas, SkyMath.Position sun, double cloudCover,
                         double elapsedSeconds) {
        if (sun.altitudeDegrees <= -1.0) return;
        int[] palette = sunPalette(sun.altitudeDegrees, cloudCover);
        float centerX = Math.round(mapCelestialX(sun.hourAngleDegrees) / RENDER_CELL)
                * RENDER_CELL;
        float centerY = Math.round(mapCelestialY(sun.altitudeDegrees, sun.hourAngleDegrees)
                / RENDER_CELL)
                * RENDER_CELL;
        int save = canvas.save();
        paint.setAlpha((int) Math.round(255.0
                * clamp((sun.altitudeDegrees + 1.0) / 3.0, 0.0, 1.0)));
        drawCelestialGlow(canvas, centerX, centerY, sun.altitudeDegrees, elapsedSeconds,
                blend(palette[0], Color.rgb(255, 217, 138), 0.55),
                (float) clamp((sun.altitudeDegrees + 1.0) / 3.0, 0.0, 1.0), 1.0f);
        drawSunBody(canvas, centerX, centerY, palette);
        canvas.restoreToCount(save);
        paint.setAlpha(255);
    }


    /** The moon reuses the sun's construction: one disc, the same halo, a cooler palette. */
    private static int[] moonPalette() {
        return new int[] { Color.rgb(244, 247, 251), Color.rgb(185, 198, 218) };
    }

    private void drawMoon(Canvas canvas, SkyMath.Position moon, double elapsedSeconds,
                          double sunAltitude) {
        if (moon.altitudeDegrees <= -1.0) return;
        // The moon is only shown once the sun is gone, so it reads as a night object rather than
        // a pale sliver that competes with the sun in the daytime sky. It also fades in over the
        // last few degrees of sunset instead of popping in.
        double nightFall = clamp((MOON_VISIBLE_BELOW_SUN - sunAltitude) / 4.0, 0.0, 1.0);
        if (nightFall <= 0.0) return;
        int[] palette = moonPalette();
        float centerX = Math.round(mapCelestialX(moon.hourAngleDegrees) / RENDER_CELL)
                * RENDER_CELL;
        float centerY = Math.round(mapCelestialY(moon.altitudeDegrees, moon.hourAngleDegrees)
                / RENDER_CELL)
                * RENDER_CELL;
        float visibility = (float) (clamp((moon.altitudeDegrees + 1.0) / 3.0, 0.0, 1.0)
                * nightFall);
        int save = canvas.save();
        paint.setAlpha((int) Math.round(255.0 * visibility));
        drawCelestialGlow(canvas, centerX, centerY, moon.altitudeDegrees, elapsedSeconds,
                blend(palette[0], Color.rgb(207, 224, 255), 0.45), visibility,
                (float) (0.35 + clamp(moon.illumination, 0.0, 1.0) * 0.55));
        drawMoonBody(canvas, centerX, centerY, palette, moon);
        canvas.restoreToCount(save);
        paint.setAlpha(255);
    }

    /**
     * The terminator is projected as an ellipse rather than an offset circle, which is the
     * difference between a correct gibbous moon and a permanent crescent. At half illumination
     * the terminator is a straight line through the centre; as the moon fills out it bows toward
     * the dark limb, and as it thins it bows back toward the lit limb.
     */
    private void drawMoonBody(Canvas canvas, float centerX, float centerY, int[] palette,
                              SkyMath.Position moon) {
        float radius = SUN_BODY_RADIUS;
        double illumination = clamp(moon.illumination, 0.0, 1.0);
        float terminator = (float) (1.0 - illumination * 2.0);
        // In the southern hemisphere the lit limb faces the opposite side from the north.
        boolean litOnRight = latitude >= 0.0 ? moon.waxing : !moon.waxing;
        paint.setStyle(Paint.Style.FILL);
        for (float row = -radius; row < radius; row += RENDER_CELL) {
            float sampleY = row + RENDER_CELL / 2.0f;
            float halfWidth = (float) Math.sqrt(Math.max(0.0,
                    radius * radius - sampleY * sampleY));
            if (halfWidth <= 0.0f) continue;
            float mix = (float) clamp((sampleY + radius) / (radius * 2.0), 0.0, 1.0);
            int bandColor = blend(palette[0], palette[1], mix);
            float ellipse = (float) Math.sqrt(Math.max(0.0,
                    1.0 - (sampleY / radius) * (sampleY / radius)));
            float edge = terminator * radius * ellipse;
            // Test each cell centre against the disc instead of rounding the row extent, which
            // would leave single-cell steps poking out of the limb.
            float left = (float) Math.floor((centerX - halfWidth) / RENDER_CELL) * RENDER_CELL;
            float right = (float) Math.ceil((centerX + halfWidth) / RENDER_CELL) * RENDER_CELL;
            paint.setColor(bandColor);
            for (float x = left; x < right; x += RENDER_CELL) {
                float offset = x + RENDER_CELL / 2.0f - centerX;
                if (offset * offset + sampleY * sampleY > radius * radius) continue;
                boolean lit = litOnRight ? offset >= edge : offset <= -edge;
                if (lit) {
                    canvas.drawRect(x, centerY + row, x + RENDER_CELL, centerY + row + RENDER_CELL,
                            paint);
                }
            }
        }
    }

    private float mapAzimuthToX(double azimuth) {
        double relative = normalizeDegrees(azimuth - headingDegrees + 180.0);
        return (float) (relative / 360.0 * SCENE_WIDTH);
    }

    /**
     * Celestial bodies walk across the sky the way they do overhead: the body rises on the east
     * horizon, arcs through the top centre of the band at its highest and sets on the west
     * horizon. The horizontal position therefore comes from the body's own progress along that
     * arc rather than from the raw compass bearing, which would wrap to the opposite screen side
     * once the sun crosses due south shortly before solar noon at this latitude.
     */
    /** Progress along the arc: 0 at the eastern end, 1 at the western end, 0.5 at noon. */
    private static double celestialProgress(double hourAngleDegrees) {
        return clamp(0.5 + hourAngleDegrees / 180.0, 0.0, 1.0);
    }

    private float mapCelestialX(double hourAngleDegrees) {
        // Negative hour angle is before solar noon (east), positive is after it (west).
        double progress = celestialProgress(hourAngleDegrees);
        return (float) (SUN_RISE_X + (SUN_SET_X - SUN_RISE_X) * progress);
    }

    /**
     * The arc is tilted a few units: the body starts slightly lower on the eastern side and
     * finishes slightly higher on the western side, matching the diamond frame where east sits
     * at the lower-right corner and west at the upper-left one. The tilt stays gentle so the sun
     * still sinks toward the horizon instead of setting high in the sky.
     */
    private float mapCelestialY(double altitude, double hourAngleDegrees) {
        double progress = celestialProgress(hourAngleDegrees);
        return (float) (mapAltitudeToCelestialBand(altitude) + ARC_TILT * (0.5 - progress));
    }

    private float mapAltitudeToCelestialBand(double altitude) {
        double visibleAltitude = clamp(altitude, 0.0, 90.0);
        return (float) (CELESTIAL_BAND_TOP
                + (1.0 - visibleAltitude / 90.0) * CELESTIAL_BAND_HEIGHT);
    }

    private static final String[][] CLOUD_SHAPE_VARIANTS = buildCloudShapeVariants();

    private static String[][] buildCloudShapeVariants() {
        String[][] variants = new String[CLOUD_SHAPES.length * 2][];
        for (int shape = 0; shape < CLOUD_SHAPES.length; shape += 1) {
            variants[shape * 2] = CLOUD_SHAPES[shape];
            String[] mirrored = new String[CLOUD_SHAPES[shape].length];
            for (int row = 0; row < CLOUD_SHAPES[shape].length; row += 1) {
                mirrored[row] = new StringBuilder(CLOUD_SHAPES[shape][row]).reverse().toString();
            }
            variants[shape * 2 + 1] = mirrored;
        }
        return variants;
    }

    private static int cloudCountForCover(double cloudCover) {
        double cover = clamp(cloudCover, 0.0, 100.0);
        int count = 0;
        for (int step : CLOUD_COVER_STEPS) {
            if (cover >= step) count += 1;
        }
        return count;
    }

    private static double cloudHash(int seed, int index, int salt) {
        int value = seed ^ ((index + 1) * 0x9e3779b9) ^ ((salt + 1) * 0x85ebca6b);
        value = (value ^ (value >>> 16)) * 0x7feb352d;
        value = (value ^ (value >>> 15)) * 0x846ca68b;
        value ^= value >>> 16;
        return Integer.toUnsignedLong(value) / 4294967296.0;
    }

    /**
     * Clouds are laid out by slot rather than advected across a physical sky field: the panel is a
     * stylised diorama, so composition matters more than meteorology.
     */
    private static double cloudDriftSpeed(double windSpeed) {
        return clamp(CLOUD_DRIFT_BASE + Math.max(0.0, windSpeed) * CLOUD_DRIFT_WIND_GAIN,
                CLOUD_DRIFT_MIN, CLOUD_DRIFT_MAX);
    }

    /**
     * Right to left is what a wind from the east does in this compass frame, where east sits at the
     * right edge and west at the left. A wind from the west reverses the travel.
     */
    private static int cloudTravelDirection(double windDirection) {
        return Math.sin(windDirection * Math.PI / 180.0) >= 0 ? -1 : 1;
    }

    /**
     * Where clouds may hang: only under the sun, because a cloud above the light cannot shade
     * anything. Exposed separately so the shadow pass maps a cloud to its ground point with the
     * same band it was placed in.
     */
    private float cloudBandTop(double sunAltitude, double sunHourAngle, float cloudHeight) {
        float sunY = (float) mapCelestialY(sunAltitude, sunHourAngle);
        return Math.max(CLOUD_SCREEN_TOP_MARGIN,
                Math.min(sunY + CLOUD_SUN_CLEARANCE,
                        ISLAND_TOP + CLOUD_BAND_REACH - cloudHeight - 8.0f));
    }

    private float cloudBandBottom() {
        return ISLAND_TOP + CLOUD_BAND_REACH;
    }

    private CloudState[] createCloudStates(double elapsedSeconds, double cloudCover,
            double windSpeed, double windDirection, double sunAltitude, double sunHourAngle) {
        int count = cloudCountForCover(cloudCover);
        if (count == 0) return new CloudState[0];
        int seed = (int) Math.round(latitude * 37 + longitude * 17) ^ (count * 0x2545f491);
        int direction = cloudTravelDirection(windDirection);
        double speed = cloudDriftSpeed(windSpeed);
        CloudState[] clouds = new CloudState[count];
        for (int index = 0; index < count; index += 1) {
            int variant = (int) (cloudHash(seed, index, 1) * CLOUD_SHAPE_VARIANTS.length);
            String[] shape = CLOUD_SHAPE_VARIANTS[variant];
            float cell = CLOUD_CELL;
            float width = shape[0].length() * cell;
            float height = shape.length * cell;
            // Every cloud sits under the sun, which is what lets it shade the island at all. A low
            // sun leaves a sliver, so a minimum span keeps the clouds from stacking on one line.
            float bandTop = cloudBandTop(sunAltitude, sunHourAngle, height);
            float bandSpan = Math.max(cloudBandBottom() - bandTop - height, CLOUD_BAND_MIN_SPAN);
            float order = count == 1
                    ? 0.45f
                    : CLOUD_SLOT_ORDER[index % CLOUD_SLOT_ORDER.length];
            // Each cloud starts at its own offset across the frame so they do not travel in a block.
            float startX = CLOUD_SLOT_X[index % CLOUD_SLOT_X.length]
                    + (float) ((cloudHash(seed, index, 4) - 0.5) * 40.0);
            double travel = direction * speed * elapsedSeconds * (0.85 + index * 0.06);
            // Wrapping over frame width plus sprite width keeps re-entry seamless: the sprite is
            // fully clear of one edge before it appears at the other.
            double span = SCENE_WIDTH + width + CLOUD_EDGE_INSET * 2.0;
            double travelled = ((startX + travel + width + CLOUD_EDGE_INSET) % span + span) % span;
            float centreX = (float) (travelled - width * 0.5 - CLOUD_EDGE_INSET);
            float bottomY = bandTop + height + order * bandSpan
                    + (float) ((cloudHash(seed, index, 3) - 0.5) * 4.0);
            clouds[index] = new CloudState(shape, cell, centreX, bottomY);
        }
        return clouds;
    }

    /** Three flat bands keyed to the sun's height, matching the Sun and Moon's own light model. */
    private int[] cloudPalette(double sunAltitude, int code) {
        if (weatherCodeIsStorm(code)) return CLOUD_STORM_BANDS;
        if (sunAltitude < -6.0) return CLOUD_NIGHT_BANDS;
        if (sunAltitude < 10.0) return CLOUD_DUSK_BANDS;
        return CLOUD_DAY_BANDS;
    }

    /**
     * One rectangle per sprite cell keeps every edge on the art grid, which is what holds the
     * cloud to the same pixel language as the rest of the scene at this size.
     */
    private void drawCloudRows(Canvas canvas, CloudState cloud, int[] palette) {
        String[] shape = cloud.shape;
        float cell = cloud.cell;
        float left = cloud.screenCenterX - shape[0].length() * cell * 0.5f;
        float top = cloud.screenBottomY - shape.length * cell;
        for (int row = 0; row < shape.length; row += 1) {
            String line = shape[row];
            float rowTop = snapToArtGrid(top + row * cell);
            float rowBottom = snapToArtGrid(top + (row + 1) * cell);
            for (int column = 0; column < line.length(); column += 1) {
                char band = line.charAt(column);
                if (band == '0') continue;
                paint.setColor(palette[band - '1']);
                paint.setStyle(Paint.Style.FILL);
                float runLeft = snapToArtGrid(left + column * cell);
                float runRight = snapToArtGrid(left + (column + 1) * cell);
                canvas.drawRect(runLeft, rowTop, runRight, rowBottom, paint);
            }
        }
    }

    private void drawClouds(Canvas canvas, CloudState[] clouds, double sunAltitude, int code) {
        if (clouds.length == 0) return;
        int[] palette = cloudPalette(sunAltitude, code);
        for (CloudState cloud : clouds) {
            drawCloudRows(canvas, cloud, palette);
            // Draw the wrapped copy too, so a cloud straddling an edge stays continuous instead of
            // being sliced in half while it leaves one side and arrives at the other.
            float width = cloud.shape[0].length() * cloud.cell;
            float span = SCENE_WIDTH + width + CLOUD_EDGE_INSET * 2.0f;
            if (cloud.screenCenterX - width * 0.5f < 0.0f) {
                drawCloudRows(canvas, cloud.withCentreX(cloud.screenCenterX + span), palette);
            } else if (cloud.screenCenterX + width * 0.5f > SCENE_WIDTH) {
                drawCloudRows(canvas, cloud.withCentreX(cloud.screenCenterX - span), palette);
            }
        }
    }

    private float snapToArtGrid(float value) {
        return Math.round(value * RENDER_SCALE) / (float) RENDER_SCALE;
    }

    private int colorForSkyHeight(double y, double sunAltitude, double cloudCover) {
        int[] palette = sunAltitude < -12.0 ? NIGHT_SKY
                : sunAltitude > 2.0 ? DAY_SKY : DAWN_SKY;
        int base = paletteColor(palette, clamp(y, 0.0, 1.0));
        int overcast = Color.rgb(88, 101, 118);
        return blend(base, overcast, clamp(cloudCover / 100.0 * 0.58, 0.0, 0.58));
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

    private static int interpolate(int first, int second, double amount) {
        return (int) Math.round(first + (second - first) * clamp(amount, 0.0, 1.0));
    }

    private static int[] colors(String... values) {
        int[] result = new int[values.length];
        for (int index = 0; index < values.length; index++) result[index] = Color.parseColor(values[index]);
        return result;
    }

    private static float wrap(double value, double range) {
        return (float) (((value % range) + range) % range);
    }

    private static double normalizeDegrees(double value) {
        return ((value % 360.0) + 360.0) % 360.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class CloudState {
        final String[] shape;
        final float cell;
        final float screenCenterX;
        final float screenBottomY;

        CloudState(String[] shape, float cell, float screenCenterX, float screenBottomY) {
            this.shape = shape;
            this.cell = cell;
            this.screenCenterX = screenCenterX;
            this.screenBottomY = screenBottomY;
        }

        CloudState withCentreX(float newCentreX) {
            return new CloudState(shape, cell, newCentreX, screenBottomY);
        }
    }

    private static final class SkyAtmosphere {
        final int tint;
        final double islandMix;

        SkyAtmosphere(int tint, double islandMix) {
            this.tint = tint;
            this.islandMix = islandMix;
        }
    }

    private static final class IslandSunlight {
        final double strength;

        IslandSunlight(double strength) {
            this.strength = strength;
        }
    }

    private static final class IslandPalette {
        final int grass;
        final int grassDark;
        final int grassTileSoftFern;
        final int grassTileFernSage;
        final int grassSeam;
        final int grassWall;
        final int soilLight;
        final int soilDark;

        IslandPalette(int grass, int grassDark,
                      int grassTileSoftFern, int grassTileFernSage, int grassSeam,
                      int grassWall, int soilLight, int soilDark) {
            this.grass = grass;
            this.grassDark = grassDark;
            this.grassTileSoftFern = grassTileSoftFern;
            this.grassTileFernSage = grassTileFernSage;
            this.grassSeam = grassSeam;
            this.grassWall = grassWall;
            this.soilLight = soilLight;
            this.soilDark = soilDark;
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
