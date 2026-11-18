package dev.retrobit.gtlauncher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;
import java.util.Arrays;

/** Full-screen, non-scrolling pixel-art sky driven by local astronomy and weather data. */
public final class WeatherSkyView extends View {
    private static final int SCENE_WIDTH = 320;
    private static final int SCENE_HEIGHT = 200;
    // A 2x backing bitmap makes half-scene-unit clusters land on crisp source pixels.
    private static final int RENDER_SCALE = 2;
    /** One pixel-art cell in scene units, matching the preview renderer. */
    private static final float RENDER_CELL = 1.0f / RENDER_SCALE;
    private int ISLAND_GRID_SIZE = 50;
    private static final int ISLAND_TILE_WIDTH = 28;
    private static final String ISLAND_COLUMN_LABELS = "ABCDEFGHIJKLMNOPQRSTUVWXY";
    // Logical grid IDs are A1–I9: columns A–I left to right, rows 1–9 top to bottom.
    // Diamond compass frame is rotated 45° clockwise from the screen diagonal so the grid
    // axes are cardinal: moving along a column runs east–west and moving along a row runs
    // north–south. Top corner=NW, upper-right edge=N, right corner=NE, lower-right edge=E,
    // bottom=SE, lower-left edge=S, left=SW, and upper-left edge=W.
    // A row of tiles is a parallel of latitude; a column of tiles is a meridian of longitude.
    // Because the frame is shifted 45°, the camera now faces northwest instead of north.
    private static final float COMPASS_FRAME_OFFSET_DEGREES = 45.0f;
    private static final float ISLAND_CAMERA_ELEVATION_DEGREES = 30.0f;
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
    private static final float ISLAND_VERTICAL_LIFT = 7.5f;
    private static final float ISLAND_BOTTOM_MARGIN = ISLAND_BASE_BOTTOM_MARGIN + ISLAND_VERTICAL_LIFT;
    private float ISLAND_TOP = SCENE_HEIGHT - ISLAND_BOTTOM_MARGIN - ISLAND_DEPTH
            - ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT;
    // Keep celestial centers in the upper third with enough margin for the Sun's glow.
    // The sun is a plain pixel disc shaded with a soft two-colour vertical gradient: the warm
    // light tone at the top fading into the deeper tone at the bottom. No rays, no facets.
    private float celestialReferenceRadius() { return ISLAND_TILE_WIDTH * islandScaleX(); }
    private float celestialScale() { return mapZoom() / 2f; }
    private float celestialRadius() { return celestialReferenceRadius() * celestialScale(); }
    // East and west ends of the celestial arc, inset so the disc never clips the frame.
    // East sits on the right of the screen and west on the left, so the sun rises on the right
    // and sets on the left, which is the reading most people expect from a sky panel.
    private static final float SUN_RISE_X = 284.0f;
    private static final float SUN_SET_X = 36.0f;
    private static final float ARC_TILT = 9.0f;
    // Island-shaped sky frames. Langit Awan is 50 scene units below Langit Matahari;
    // cloud and Sun movement remain unchanged until their new paths are implemented.
    private static final boolean LANGIT_MATAHARI_ENABLED = false;
    private static final boolean LANGIT_AWAN_ENABLED = false;
    private static final float LANGIT_LINE = 0.5f;
    private static final float LANGIT_INSET = 1.5f;
    private static final float LANGIT_ALPHA = 0.38f;
    private static final float LANGIT_EDGE_ALPHA = 0.62f;
    private static final float LANGIT_MATAHARI_LIFT = 100.0f;
    private static final float LANGIT_AWAN_LIFT = 50.0f;
    private static final float LANGIT_MATAHARI_WEST_SHIFT = 20.0f;
    private static final float LANGIT_AWAN_WEST_SHIFT = 10.0f;
    // The moon only appears once the sun has dropped below this altitude.
    private static final double MOON_VISIBLE_BELOW_SUN = -2.0;
    private static final int GLOW_STEPS = 14;
    // The halo used to reach 15 units past a 10.5-unit disc, which washed out the sky around it and
    // read brighter than the disc itself. Pulled in and faded down.
    private static final float GLOW_REACH = 3.5f;
    private static final double GLOW_ALPHA = 0.024;
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
    private float ISLAND_CENTER_Y = ISLAND_TOP + ISLAND_TILE_HEIGHT / 2.0f
            + (ISLAND_GRID_SIZE - 1) * ISLAND_TILE_HEIGHT / 2.0f;
    // Clouds are raster sprites traced from reference art, downsampled to a 30-column grid and
    // quantised into three flat bands. One cell value drives both axes, so each silhouette keeps
    // the ratio it was traced at instead of stretching into a flat smudge.
    // One cloud per step cleared; the first step is 0 so even a clear sky keeps one cloud.
    private static final int[] CLOUD_COVER_STEPS = {0, 32, 58, 82};
    // Both axes are two thirds of the previous cloud size; shadows use the same cell size.
    private static final float CLOUD_CELL = 2.0f / 3.0f;
    // Slots are absolute scene units so the widest sprite still clears both frame edges. The last
    // two sit above the island so their shadows fall on its top surface.
    private static final float[] CLOUD_SLOT_X = {56.0f, 264.0f, 118.0f, 206.0f};
    // Clouds ride the same tilted arc as the Sun and Moon instead of a flat line: one dome across
    // the frame, leaning the same way. Each cloud sits on its own parallel arc so they stack in
    // depth instead of sharing one path.
    private static final float CLOUD_ARC_TOP = 46.0f;
    private static final float CLOUD_ARC_RISE = 42.0f;
    private static final float[] CLOUD_ARC_OFFSETS = {2.0f, 0.0f, 12.0f, 20.0f};
    // Must hang below the sun: a cloud above the light cannot shade anything.
    private static final float CLOUD_SUN_CLEARANCE = 4.0f;
    // A cloud crossing the island turns partly transparent so the tiles behind it stay readable.
    private static final float CLOUD_ISLAND_ALPHA = 0.55f;
    // Clouds travel across the frame like real wind-driven cloud, then re-enter from the other
    // side. Screen speed is derived from the reported wind so a breeze crawls and a storm hurries.
    // The frame maps the whole compass - 360 degrees of sky - onto 320 units, so one unit is 1.125
    // degrees. That makes drift speed a function of cloud altitude: a layer at 1200 m needs 0.0118
    // units per second per km/h. Taking that literally would look frozen on a panel this small, so
    // the gain is deliberately faster so gentle wind remains perceptible.
    private static final double CLOUD_DRIFT_WIND_GAIN = 0.12;
    private static final double CLOUD_DRIFT_MAX = 4.2;
    private static final float CLOUD_EDGE_INSET = 4.0f;
    // The shadow is a shrunk copy of the cloud, not a same-size one: at full size it covered
    // most of the island and read as a hole rather than as shade.
    // A soft, dark tint so the shadow reads as shade on grass rather than a grey sticker.
    private static final int CLOUD_SHADOW_ON_ISLAND_COLOR = Color.argb(36, 17, 26, 39);
    // The island's own cast shadow: close to the base, drifting with the light direction.
    private static final float ISLAND_BODY_SHADOW_SCALE = 1.55f;
    private static final float ISLAND_BODY_SHADOW_OPACITY_SCALE = 0.46f;
    /** Target luminance drop for the plate's shadow, matched in every weather and at every hour. */
    private static final double SHADOW_CONTRAST_TARGET = 24.0;
    /** Relative luminance of the shade itself (#0c1728), used to solve the alpha for that target. */
    private static final double SHADOW_LUMINANCE = 15.0;
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
    // Reused by the grid overlay so the draw path allocates nothing per frame.
    private final Path gridPath = new Path();
    private final Paint paint = new Paint();
    private final Paint islandPolyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint renderBitmapPaint = new Paint();
    private final Paint grassDetailPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private Bitmap grassDetail;
    private final Rect grassDetailSource = new Rect();
    private final RectF grassDetailDestination = new RectF();
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
    private double plantMotionSeconds;
    private Integer worldIslandTint;
    private Bitmap mainCharacter, mainCharacterBack, idleFrontTransition, idleFrontPose, idleBackTransition, idleBackPose, houseImage, houseBackImage, houseWideImage, houseWideBackImage, houseWideMirrorImage, houseWideBackMirrorImage, portalMarkerImage;
    private final Bitmap[] walkFront=new Bitmap[12], walkBack=new Bitmap[12], runFront=new Bitmap[12], runBack=new Bitmap[12];
    private Bitmap fenceFarm,fenceSlat,fenceStone,fenceIron;
    private final java.util.Map<String,Bitmap> flowerSprites=new java.util.HashMap<>();
    private Bitmap treeDormant,treeBananaFruitless,treeCoconutFruitless;
    private TreePhenology.Result treePhenology;
    private java.util.Set<String> harvestedTrees=java.util.Collections.emptySet();
    private Bitmap bushSpiky,bushTiered,bushRound,bushFlower,bushBerry,wildGrass,treeApple,treeOrange,treePear,treeGuava,treeStarfruit,treeMango,treeBanana,treeCoconut,sunflowerFront,sunflowerBack,sellingBoxClosed,sellingBoxOpen,mailboxBirdhouse,storageClosed,storageOpen,storageBackClosed,storageBackOpen;
    private final java.util.Set<String> openStorage = new java.util.HashSet<>();
    private final java.util.Map<String,Long> sellingOpenedAt = new java.util.HashMap<>();
    private String surfaceWorldId="";
    private java.util.List<PortalTravel.Portal> worldPortals = java.util.Collections.emptyList();
    private boolean portalWalkActive;
    private java.util.List<WorldStore.SurfaceObject> worldSurfaces = java.util.Collections.emptyList();
    private java.util.List<WorldStore.House> worldHouses=java.util.Collections.emptyList();
    private String characterFacing = "S";
    private boolean showCharacter;
    private int characterColumn = 4, characterRow = 4;
    private int stepFromColumn, stepFromRow, stepIndex, stepCount, stepDuration;
    private long stepStarted;
    private boolean characterStepping, characterRunning;
    void setCharacterRunning(boolean running) { characterRunning=running; invalidate(); }
    int accelerateCharacterStep(int duration) {
        if(!characterStepping)return 0;
        float progress=stepProgress();stepDuration=duration;
        stepStarted=SystemClock.uptimeMillis()-Math.round(progress*duration);
        invalidate();return Math.max(0,Math.round((1-progress)*duration));
    }
    private float stepProgress() { return characterStepping ? CharacterGait.progress(SystemClock.uptimeMillis()-stepStarted,stepDuration) : 1f; }
    float groundStepProgress() { return stepProgress(); }
    int redirectCharacterStep(boolean reverse,int duration,int total) {
        float progress=stepProgress();
        if(reverse) {
            int oldColumn=characterColumn,oldRow=characterRow;
            characterColumn=stepFromColumn;characterRow=stepFromRow;
            stepFromColumn=oldColumn;stepFromRow=oldRow;
            characterFacing=characterColumn>stepFromColumn?"E":characterColumn<stepFromColumn?"W":characterRow>stepFromRow?"S":"N";
            progress=1-progress;
        }
        stepIndex=1;stepCount=total;stepDuration=duration;
        stepStarted=SystemClock.uptimeMillis()-Math.round(progress*duration);
        invalidate();return Math.max(0,Math.round((1-progress)*duration));
    }
    private float waterDepthAt(int column, int row) {
        if (worldTerrain == null || column < 0 || row < 0 || column >= ISLAND_GRID_SIZE || row >= ISLAND_GRID_SIZE) return 0f;
        return worldTerrain.tile(column,row)==3 ? ISLAND_GRASS_BAND_DEPTH : 0f;
    }
    private float visualWaterDepth() {
        float target=waterDepthAt(characterColumn,characterRow);
        return characterStepping ? waterDepthAt(stepFromColumn,stepFromRow)*(1f-stepProgress()) + target*stepProgress() : target;
    }
    private float visualColumn() { return characterStepping ? stepFromColumn+(characterColumn-stepFromColumn)*stepProgress() : characterColumn; }
    private float visualRow() { return characterStepping ? stepFromRow+(characterRow-stepFromRow)*stepProgress() : characterRow; }
    void animateCharacterStep(int fromColumn,int fromRow,int toColumn,int toRow,float scale,String facing,int index,int total,int duration) {
        playerIdleBow.reset();idleBowAmount=0;
        setWorldCharacter(true,toColumn,toRow,scale,facing);
        stepFromColumn=fromColumn; stepFromRow=fromRow; stepIndex=index; stepCount=total;
        stepDuration=duration; stepStarted=SystemClock.uptimeMillis();
        characterStepping=android.animation.ValueAnimator.areAnimatorsEnabled();
        postInvalidateOnAnimation();
    }
    private float characterScale = 1.0f;
    private double cloudEastTravel, cloudNorthTravel;
    private double cloudLastElapsed = Double.NaN;
    private double cloudWindCardinal = Double.NaN;
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
        paint.setFilterBitmap(true);
        paint.setStyle(Paint.Style.FILL);
        islandPolyPaint.setAntiAlias(true);
        islandPolyPaint.setDither(true);
        islandPolyPaint.setStrokeJoin(Paint.Join.ROUND);
        islandPolyPaint.setStrokeCap(Paint.Cap.ROUND);
        renderBitmapPaint.setAntiAlias(false);
        renderBitmapPaint.setFilterBitmap(true);
        renderBitmapPaint.setDither(false);
        grassDetail = BitmapFactory.decodeResource(getResources(), R.drawable.island_grass_detail);
        mainCharacter = BitmapFactory.decodeResource(getResources(), R.drawable.character_original_uhd_front);
        mainCharacterBack = BitmapFactory.decodeResource(getResources(), R.drawable.character_original_uhd_back);
        idleFrontTransition=BitmapFactory.decodeResource(getResources(),R.drawable.player_idle_front_transition);
        idleFrontPose=BitmapFactory.decodeResource(getResources(),R.drawable.player_idle_front_concept);
        idleBackTransition=BitmapFactory.decodeResource(getResources(),R.drawable.player_idle_back_transition);
        idleBackPose=BitmapFactory.decodeResource(getResources(),R.drawable.player_idle_back_concept);
        int[] frontFrames={R.drawable.character_walk_front_00, R.drawable.character_walk_front_01, R.drawable.character_walk_front_02, R.drawable.character_walk_front_03, R.drawable.character_walk_front_04, R.drawable.character_walk_front_05, R.drawable.character_walk_front_06, R.drawable.character_walk_front_07, R.drawable.character_walk_front_08, R.drawable.character_walk_front_09, R.drawable.character_walk_front_10, R.drawable.character_walk_front_11};
        int[] backFrames={R.drawable.character_walk_back_00, R.drawable.character_walk_back_01, R.drawable.character_walk_back_02, R.drawable.character_walk_back_03, R.drawable.character_walk_back_04, R.drawable.character_walk_back_05, R.drawable.character_walk_back_06, R.drawable.character_walk_back_07, R.drawable.character_walk_back_08, R.drawable.character_walk_back_09, R.drawable.character_walk_back_10, R.drawable.character_walk_back_11};
        for(int i=0;i<12;i++) {walkFront[i]=BitmapFactory.decodeResource(getResources(),frontFrames[i]);walkBack[i]=BitmapFactory.decodeResource(getResources(),backFrames[i]);}
        int[] runFrontFrames={R.drawable.character_run_front_00, R.drawable.character_run_front_01, R.drawable.character_run_front_02, R.drawable.character_run_front_03, R.drawable.character_run_front_04, R.drawable.character_run_front_05, R.drawable.character_run_front_06, R.drawable.character_run_front_07, R.drawable.character_run_front_08, R.drawable.character_run_front_09, R.drawable.character_run_front_10, R.drawable.character_run_front_11},runBackFrames={R.drawable.character_run_back_00, R.drawable.character_run_back_01, R.drawable.character_run_back_02, R.drawable.character_run_back_03, R.drawable.character_run_back_04, R.drawable.character_run_back_05, R.drawable.character_run_back_06, R.drawable.character_run_back_07, R.drawable.character_run_back_08, R.drawable.character_run_back_09, R.drawable.character_run_back_10, R.drawable.character_run_back_11};
        for(int i=0;i<12;i++) {runFront[i]=BitmapFactory.decodeResource(getResources(),runFrontFrames[i]);runBack[i]=BitmapFactory.decodeResource(getResources(),runBackFrames[i]);}
        houseImage = BitmapFactory.decodeResource(getResources(), R.drawable.simple_house_aligned);
        houseBackImage = BitmapFactory.decodeResource(getResources(), R.drawable.simple_house_back_aligned);
        houseWideImage=BitmapFactory.decodeResource(getResources(),R.drawable.house_redesign_wide);houseWideBackImage=BitmapFactory.decodeResource(getResources(),R.drawable.house_redesign_wide_back);
        houseWideMirrorImage=BitmapFactory.decodeResource(getResources(),R.drawable.house_redesign_wide_mirror);houseWideBackMirrorImage=BitmapFactory.decodeResource(getResources(),R.drawable.house_redesign_wide_back_mirror);
        portalMarkerImage = BitmapFactory.decodeResource(getResources(), R.drawable.portal_exclamation_red);
        fenceFarm=BitmapFactory.decodeResource(getResources(),R.drawable.fence_farm);fenceSlat=BitmapFactory.decodeResource(getResources(),R.drawable.fence_slat);fenceStone=BitmapFactory.decodeResource(getResources(),R.drawable.fence_stone);fenceIron=BitmapFactory.decodeResource(getResources(),R.drawable.fence_iron);
        flowerSprites.put("rose",BitmapFactory.decodeResource(getResources(),R.drawable.petals_rose));
        flowerSprites.put("tulip",BitmapFactory.decodeResource(getResources(),R.drawable.petals_tulip));
        flowerSprites.put("lavender",BitmapFactory.decodeResource(getResources(),R.drawable.petals_lavender));
        flowerSprites.put("daisy",BitmapFactory.decodeResource(getResources(),R.drawable.petals_daisy));
        flowerSprites.put("marigold",BitmapFactory.decodeResource(getResources(),R.drawable.petals_marigold));
        flowerSprites.put("bluebell",BitmapFactory.decodeResource(getResources(),R.drawable.petals_bluebell));
        flowerSprites.put("jasmine",BitmapFactory.decodeResource(getResources(),R.drawable.petals_jasmine));
        flowerSprites.put("ylang",BitmapFactory.decodeResource(getResources(),R.drawable.petals_ylang));
        flowerSprites.put("frangipani",BitmapFactory.decodeResource(getResources(),R.drawable.petals_frangipani));
        flowerSprites.put("hibiscus",BitmapFactory.decodeResource(getResources(),R.drawable.petals_hibiscus));
        flowerSprites.put("periwinkle",BitmapFactory.decodeResource(getResources(),R.drawable.petals_periwinkle));
        bushRound=BitmapFactory.decodeResource(getResources(),R.drawable.bush_round);bushFlower=BitmapFactory.decodeResource(getResources(),R.drawable.bush_flower);bushBerry=BitmapFactory.decodeResource(getResources(),R.drawable.bush_berry);
        treeDormant=BitmapFactory.decodeResource(getResources(),R.drawable.tree_dormant);treeBananaFruitless=BitmapFactory.decodeResource(getResources(),R.drawable.tree_banana_fruitless);treeCoconutFruitless=BitmapFactory.decodeResource(getResources(),R.drawable.tree_coconut_fruitless);
        treeApple=BitmapFactory.decodeResource(getResources(),R.drawable.tree_apple);treeOrange=BitmapFactory.decodeResource(getResources(),R.drawable.orange_tree_pixel);treePear=BitmapFactory.decodeResource(getResources(),R.drawable.tree_pear);treeGuava=BitmapFactory.decodeResource(getResources(),R.drawable.tree_guava);treeStarfruit=BitmapFactory.decodeResource(getResources(),R.drawable.tree_starfruit);
        wildGrass=BitmapFactory.decodeResource(getResources(),R.drawable.wild_grass);bushSpiky=BitmapFactory.decodeResource(getResources(),R.drawable.bush_spiky);bushTiered=BitmapFactory.decodeResource(getResources(),R.drawable.bush_tiered);
        treeBanana=BitmapFactory.decodeResource(getResources(),R.drawable.tree_banana_simple);treeCoconut=BitmapFactory.decodeResource(getResources(),R.drawable.tree_coconut_small);sunflowerFront=BitmapFactory.decodeResource(getResources(),R.drawable.sunflower_front);sunflowerBack=BitmapFactory.decodeResource(getResources(),R.drawable.sunflower_back);treeMango=BitmapFactory.decodeResource(getResources(),R.drawable.tree_mango);mailboxBirdhouse=BitmapFactory.decodeResource(getResources(),R.drawable.mailbox_birdhouse);
        sellingBoxClosed=BitmapFactory.decodeResource(getResources(),R.drawable.selling_box_closed);sellingBoxOpen=BitmapFactory.decodeResource(getResources(),R.drawable.selling_box_open);storageClosed=BitmapFactory.decodeResource(getResources(),R.drawable.storage_closed);storageOpen=BitmapFactory.decodeResource(getResources(),R.drawable.storage_open);
        storageBackClosed=BitmapFactory.decodeResource(getResources(),R.drawable.storage_back_closed);storageBackOpen=BitmapFactory.decodeResource(getResources(),R.drawable.storage_back_open);
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

    /** Set the imported world's island tint; null restores the unmodified Base World. */
    public void setWorldIslandTint(Integer color) {
        worldIslandTint = color;
        invalidate();
    }

    /** Set the character's world-specific tile and scale from the imported world. */
    public void setWorldCharacter(boolean visible, int column, int row, float scale, String facing) {
        characterStepping = false;
        showCharacter = visible;
        characterFacing = facing;
        characterColumn = Math.max(0, Math.min(ISLAND_GRID_SIZE-1, column));
        characterRow = Math.max(0, Math.min(ISLAND_GRID_SIZE-1, row));
        characterScale = Math.max(0.5f, Math.min(1.5f, scale));
        invalidate();
    }

    void setWorldHouses(java.util.List<WorldStore.House> houses) {
        worldHouses=houses==null?new java.util.ArrayList<>():new java.util.ArrayList<>(houses);
        worldHouses.sort((a,b)->Integer.compare(a.column+a.row,b.column+b.row));invalidate();
    }

    void setTreePhenology(TreePhenology.Result result) {
        treePhenology=result;
        setContentDescription("Langit pixel art dan pulau terapung. " + (result==null?"Tahap buah pohon belum diketahui.":"Tahap buah diperkirakan dari riwayat cuaca hingga "+result.lastDate+" UTC."));
        invalidate();
    }
    String treeStage(String species) {return treePhenology==null?"unknown":treePhenology.stages.getOrDefault(species,"unknown");}
    void setHarvestedTrees(java.util.Set<String> trees) {harvestedTrees=new java.util.HashSet<>(trees);invalidate();}
    private boolean isHarvested(WorldStore.SurfaceObject tree) {return harvestedTrees.contains(InventoryStore.treeKey(surfaceWorldId,tree.column,tree.row,tree.material));}
    private Bitmap treeImage(WorldStore.SurfaceObject o) {
        if(!"ripe".equals(treeStage(o.material)) || isHarvested(o))return o.material.equals("banana")?treeBananaFruitless:o.material.equals("palm")?treeCoconutFruitless:treeDormant;
        return o.material.equals("banana")?treeBanana:o.material.equals("palm")?treeCoconut:o.material.equals("apple")?treeApple:o.material.equals("orange")?treeOrange:o.material.equals("pear")?treePear:o.material.equals("guava")?treeGuava:o.material.equals("starfruit")?treeStarfruit:treeMango;
    }
    void setWorldSurfaces(String worldId,java.util.List<WorldStore.SurfaceObject> objects) {
        ISLAND_GRID_SIZE = worldId == null || "base".equals(worldId) ? 50 : 9;
        ISLAND_TOP = SCENE_HEIGHT - ISLAND_BOTTOM_MARGIN - ISLAND_DEPTH - ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT;
        ISLAND_CENTER_Y = ISLAND_TOP + ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT / 2f;
        String id=worldId==null?"":worldId;if(!surfaceWorldId.equals(id)){openStorage.clear();sellingOpenedAt.clear();surfaceWorldId=id;gameplayCamera.reset();cameraFrameTime=0;}
        worldSurfaces = objects == null ? java.util.Collections.emptyList() : new java.util.ArrayList<>(objects); invalidate();
    }
    private PreviewWorld20 worldTerrain;
    private PondLife pondLife;
    private double pondWindX=0,pondWindY=0,pondWindLast=-1;
    void setWorldTerrain(PreviewWorld20 terrain) {
        worldTerrain=terrain;
        pondLife=terrain==null ? null : PondLife.build(terrain.size,terrain::tile);
        if(terrain!=null) {
            ISLAND_GRID_SIZE=terrain.size;
            ISLAND_TOP=SCENE_HEIGHT-ISLAND_BOTTOM_MARGIN-ISLAND_DEPTH-ISLAND_GRID_SIZE*ISLAND_TILE_HEIGHT;
            ISLAND_CENTER_Y=ISLAND_TOP+ISLAND_GRID_SIZE*ISLAND_TILE_HEIGHT/2f;
        }
        invalidate();
    }
    private RectF surfaceSpriteBounds(WorldStore.SurfaceObject o) {
        float x=SCENE_WIDTH/2f+(o.column-o.row)*ISLAND_TILE_WIDTH/2f;
        float y=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(o.column+o.row)*ISLAND_TILE_HEIGHT/2f;
        float width,height,bottom;
        switch(o.type) {
            case "tree": boolean small=o.material.equals("banana") || o.material.equals("palm");width=small?24:(o.material.equals("orange") && "ripe".equals(treeStage(o.material)) && !isHarvested(o)?60f*512f/490f:48);height=small?30:60;bottom=y+ISLAND_TILE_HEIGHT*(small?.275f:o.material.equals("orange")?.5f:1.05f);break;
            case "sunflower": width=15;height=23;bottom=y+ISLAND_TILE_HEIGHT*.325f;break;
            case "bush": width=o.material.equals("wild-grass")?28f:18.5f;height=o.material.equals("wild-grass")?14f:14.5f;bottom=y+ISLAND_TILE_HEIGHT*.325f;break;
            case "mailbox": width=13;height=30;bottom=y+ISLAND_TILE_HEIGHT*.15f;break;
            default: width=18;height=18;bottom=y+ISLAND_TILE_HEIGHT*.15f;
        }
        return new RectF(x-width/2,bottom-height,x+width/2,bottom);
    }
    private double plantSway(WorldStore.SurfaceObject o,double factor) {
        if(weather==null)return 0;
        return factor*PlantMotion.sway(animationEnabled,weather.windSpeedKmh,weather.windDirectionDegrees,
                headingDegrees,plantMotionSeconds,o.column,o.row,o.type,o.material);
    }
    private double orangeTreeSway(WorldStore.SurfaceObject o) {
        double speed=weather==null?4:weather.windSpeedKmh,direction=weather==null?90:weather.windDirectionDegrees;
        return OrangeTreeMotion.sway(animationEnabled && android.animation.ValueAnimator.areAnimatorsEnabled(),speed,direction,headingDegrees,plantMotionSeconds,o.column,o.row);
    }
    private void drawSurfaceSprites(Canvas canvas,boolean front) {
        for(WorldStore.SurfaceObject o:worldSurfaces) {
            if(o.type.equals("road") || o.type.equals("fence") || o.type.equals("flower")) continue;
            float depth=o.column+o.row+(o.type.equals("tree") && !(o.material.equals("banana") || o.material.equals("palm"))?1f:0f);
            if(showCharacter && (depth>=visualColumn()+visualRow())!=front) continue;
            if(!showCharacter && front) continue;
            Bitmap image; boolean mirror=false;
            if(o.type.equals("tree")) image=treeImage(o);
            else if(o.type.equals("sunflower")) { image=(o.material.equals("N")||o.material.equals("W"))?sunflowerBack:sunflowerFront;mirror=o.material.equals("N")||o.material.equals("E"); }
            else if(o.type.equals("bush")) image=o.material.equals("wild-grass")?wildGrass:o.material.equals("spiky")?bushSpiky:o.material.equals("tiered")?bushTiered:o.material.equals("flower")?bushFlower:o.material.equals("berry")?bushBerry:bushRound;
            else if(o.type.equals("mailbox")) image=mailboxBirdhouse;
            else {boolean back=o.material.equals("N")||o.material.equals("W"),opened=openStorage.contains(o.column+","+o.row);
                mirror=o.material.equals("N")||o.material.equals("E");
                image=o.type.equals("selling-box")?(opened?sellingBoxOpen:sellingBoxClosed):back?(opened?storageBackOpen:storageBackClosed):(opened?storageOpen:storageClosed);
            }
            if(image==null)continue;RectF g=surfaceSpriteBounds(o);
            int alpha=Math.round(255*(100-Math.max(0,o.opacity))/100f);
            if(!(o.type.equals("tree") && o.material.equals("orange"))) {
                paint.setColor(Color.argb(Math.round(35*alpha/255f),12,23,40));paint.setStyle(Paint.Style.FILL);canvas.drawOval(new RectF(g.centerX()-g.width()*.3f,g.bottom-1,g.centerX()+g.width()*.3f,g.bottom+1),paint);
            }
            paint.setColor(Color.WHITE);paint.setAlpha(alpha);paint.setFilterBitmap(true);int saved=canvas.save();
            if(o.type.equals("tree") && o.material.equals("orange")) {
                double sway=orangeTreeSway(o);
                canvas.translate(g.centerX(),g.bottom);canvas.skew((float)(-sway/g.height()),0);canvas.translate(-g.centerX(),-g.bottom);
            } else if(o.type.equals("sunflower") || o.type.equals("tree") || o.type.equals("bush")) {
                double sway=plantSway(o,1.0);
                canvas.rotate((float)Math.toDegrees(Math.atan2(sway,g.height())),g.centerX(),g.bottom);
            }
            if(mirror)canvas.scale(-1,1,g.centerX(),0);
            Long started=sellingOpenedAt.get(o.column+","+o.row);
            if((o.type.equals("selling-box") || o.type.equals("storage-box")) && started!=null && animationEnabled && image!=null) {
                boolean back=o.material.equals("N") || o.material.equals("W"); Bitmap closed=o.type.equals("selling-box")?sellingBoxClosed:(back?storageBackClosed:storageClosed);
                if(closed==null)closed=image;
                float t=Math.max(0,Math.min(1,(android.os.SystemClock.uptimeMillis()-started)/260f));
                paint.setAlpha((int)(alpha*(1-t)));canvas.drawBitmap(closed,null,g,paint);
                paint.setAlpha((int)(alpha*t));canvas.drawBitmap(image,null,g,paint);paint.setAlpha(255);
                if(t>=1)sellingOpenedAt.remove(o.column+","+o.row);
            } else canvas.drawBitmap(image,null,g,paint);
            if(o.type.equals("tree")) {
                String stage=treeStage(o.material);
                if(stage.equals("flower") || stage.equals("fruit")) {
                    paint.setColor(stage.equals("flower")?Color.rgb(230,215,196):Color.rgb(160,183,125));paint.setAlpha(alpha);
                    float[][] positions=(o.material.equals("banana")?new float[][]{{.68f,.68f},{.74f,.72f},{.79f,.68f}}:o.material.equals("palm")?new float[][]{{.51f,.43f},{.57f,.45f},{.62f,.44f}}:new float[][]{{.29f,.28f},{.61f,.21f},{.78f,.38f},{.38f,.52f},{.67f,.57f}});
                    for(float[] p:positions){float x=g.left+g.width()*p[0],y=g.top+g.height()*p[1];canvas.drawRect(x-.7f,y-.7f,x+.7f,y+.7f,paint);}
                }
            }
            canvas.restoreToCount(saved);
        }
    }
    private RectF sellingMarkerBounds(WorldStore.SurfaceObject o) {
        RectF box=surfaceSpriteBounds(o);float x=Math.max(0,Math.min(SCENE_WIDTH-11,box.right-5)),y=Math.max(0,Math.min(SCENE_HEIGHT-13,box.top-9));return new RectF(x,y,x+11,y+13);
    }
    private boolean sellingMarkerVisible(WorldStore.SurfaceObject o) {
        return (o.type.equals("selling-box") || o.type.equals("storage-box")) && !portalWalkActive && showCharacter && Math.abs(characterColumn-o.column)+Math.abs(characterRow-o.row)==1 && !openStorage.contains(o.column+","+o.row);
    }
    private void drawSellingMarkers(Canvas canvas) {
        if(portalMarkerImage==null)return;paint.setAlpha(255);paint.setFilterBitmap(true);
        for(WorldStore.SurfaceObject o:worldSurfaces)if(sellingMarkerVisible(o)) {
            RectF g=sellingMarkerBounds(o);paint.setColor(Color.rgb(110,67,43));canvas.drawRect(g.left,g.top,g.right,g.bottom-2,paint);canvas.drawRect(g.left+2,g.bottom-2,g.left+6,g.bottom,paint);
            paint.setColor(Color.rgb(255,241,206));canvas.drawRect(g.left+1,g.top+1,g.right-1,g.bottom-3,paint);canvas.drawRect(g.left+3,g.bottom-3,g.left+5,g.bottom-1,paint);
            paint.setColor(Color.WHITE);canvas.drawBitmap(portalMarkerImage,null,new RectF(g.left+2.5f,g.top+1,g.right-2.5f,g.bottom-3),paint);
        }
    }
    private boolean hoeSelected = false;
    void setHoeSelected(boolean selected) { hoeSelected=selected;invalidate(); }
    private boolean gameplayMode = false;
    private final CameraFollow gameplayCamera=new CameraFollow();
    private long cameraFrameTime;
    private boolean cameraMoving;
    void setGameplayMode(boolean value) { if(gameplayMode!=value){playerIdleBow.reset();idleBowAmount=0;gameplayCamera.reset();cameraFrameTime=0;} gameplayMode=value; invalidate(); }
    private float mapSourceX() { return gameplayMode && showCharacter
            ? (gameplayCamera.ready()?gameplayCamera.x():SCENE_WIDTH/2f+(visualColumn()-visualRow())*ISLAND_TILE_WIDTH/2f) : SCENE_WIDTH/2f; }
    private float mapSourceY() { return gameplayMode && showCharacter
            ? (gameplayCamera.ready()?gameplayCamera.y():ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(visualColumn()+visualRow())*ISLAND_TILE_HEIGHT/2f)
            : ISLAND_GRID_SIZE >= 20 ? ISLAND_CENTER_Y : SCENE_HEIGHT-ISLAND_BOTTOM_MARGIN; }
    private float mapDestinationX() { return SCENE_WIDTH/2f; }
    private float mapDestinationY() { return (gameplayMode && showCharacter) || ISLAND_GRID_SIZE >= 20 ? SCENE_HEIGHT*.56f : SCENE_HEIGHT-ISLAND_BOTTOM_MARGIN; }
    private float mapZoom() { return IslandProjection.renderZoom(gameplayMode); }
    // Object and marker hit tests use the exact inverse of the current map camera.
    private float islandX(float screenX) {return IslandProjection.inverse(screenX,getWidth(),SCENE_WIDTH,mapSourceX(),mapDestinationX(),mapZoom());}
    private float islandY(float screenY) {return IslandProjection.inverse(screenY,getHeight(),SCENE_HEIGHT,mapSourceY(),mapDestinationY(),mapZoom());}
    private float islandScaleX() { return IslandProjection.scale(getWidth(), SCENE_WIDTH); }
    private float islandScaleY() { return IslandProjection.scale(getHeight(), SCENE_HEIGHT); }
    // Celestial positions remain in the sky, independent of the map camera.
    private float islandScreenX(float x) {return SCENE_WIDTH/2f+(x-SCENE_WIDTH/2f)*islandScaleX();}
    private float islandScreenY(float y) {float anchor=SCENE_HEIGHT-ISLAND_BOTTOM_MARGIN;return anchor+(y-anchor)*islandScaleY();}
    private void applyIslandScale(Canvas canvas) {
        canvas.translate(mapDestinationX(),mapDestinationY());
        canvas.scale(islandScaleX()*mapZoom(),islandScaleY()*mapZoom());
        canvas.translate(-mapSourceX(),-mapSourceY());
    }
    PortalTravel.Tile groundTileAt(float sx,float sy) {
        if(getWidth()<=0 || getHeight()<=0)return null;
        float x=islandX(sx), y=islandY(sy);
        float dx=(x-SCENE_WIDTH/2f)/(ISLAND_TILE_WIDTH/2f);
        float dy=(y-ISLAND_TOP-ISLAND_TILE_HEIGHT/2f)/(ISLAND_TILE_HEIGHT/2f);
        float c=(dx+dy)/2f, r=(dy-dx)/2f;
        int col=Math.round(c),row=Math.round(r);
        if(col<0 || col>=ISLAND_GRID_SIZE || row<0 || row>=ISLAND_GRID_SIZE
                || Math.abs(c-col)>.5f || Math.abs(r-row)>.5f) return null;
        return new PortalTravel.Tile(col,row);
    }
    boolean characterAt(float sx,float sy) {
        if(!showCharacter || getWidth()<=0 || getHeight()<=0)return false;
        Bitmap sprite=(characterFacing.equals("N")||characterFacing.equals("W"))?mainCharacterBack:mainCharacter;
        float x=SCENE_WIDTH/2f+(visualColumn()-visualRow())*ISLAND_TILE_WIDTH/2f;
        float y=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(visualColumn()+visualRow())*ISLAND_TILE_HEIGHT/2f+visualWaterDepth();
        return opaqueAt(sprite,new RectF(x-6.75f*characterScale,y-25*characterScale,x+6.75f*characterScale,y),islandX(sx),islandY(sy),characterFacing.equals("W")||characterFacing.equals("E"));
    }
    WorldStore.SurfaceObject sellingMarkerAt(float sx,float sy) {
        if(getWidth()<=0 || getHeight()<=0)return null;float x=islandX(sx),y=islandY(sy);
        for(int i=worldSurfaces.size()-1;i>=0;i--){WorldStore.SurfaceObject o=worldSurfaces.get(i);RectF hit=sellingMarkerBounds(o);hit.inset(-3,-3);if(sellingMarkerVisible(o) && hit.contains(x,y))return o;}return null;
    }
    void setSellingOpen(WorldStore.SurfaceObject o,boolean opened) {if(o==null)return;String key=o.column+","+o.row;if(opened){openStorage.add(key);sellingOpenedAt.put(key,android.os.SystemClock.uptimeMillis());}else{openStorage.remove(key);sellingOpenedAt.remove(key);}invalidate();}
    WorldStore.SurfaceObject storageBoxAt(float sx,float sy) {
        if(getWidth()<=0 || getHeight()<=0)return null;float x=islandX(sx),y=islandY(sy);
        for(int i=worldSurfaces.size()-1;i>=0;i--){WorldStore.SurfaceObject o=worldSurfaces.get(i);if((o.type.equals("storage-box")||o.type.equals("selling-box")) && opaqueAt(surfaceImage(o),surfaceSpriteBounds(o),x,y,o.material.equals("N")||o.material.equals("E")))return o;}return null;
    }
    WorldStore.SurfaceObject treeAt(float sx,float sy) {
        if(getWidth()<=0 || getHeight()<=0)return null;float x=islandX(sx),y=islandY(sy);
        for(int i=worldSurfaces.size()-1;i>=0;i--) {
            WorldStore.SurfaceObject o=worldSurfaces.get(i);if(!o.type.equals("tree"))continue;
            RectF bounds=surfaceSpriteBounds(o);float hitX=x;
            if(o.material.equals("orange"))hitX+=(y-bounds.bottom)*(float)(orangeTreeSway(o)/bounds.height());
            if(opaqueAt(surfaceImage(o),bounds,hitX,y,false))return o;
        }
        return null;
    }
    WorldStore.House houseAt(float sx,float sy) {
        if(getWidth()<=0 || getHeight()<=0)return null;
        float x=islandX(sx), y=islandY(sy);
        for(int i=worldHouses.size()-1;i>=0;i--) {
            WorldStore.House h=worldHouses.get(i); if(!h.visible)continue;
            boolean mirror=h.facing.equals("N")||h.facing.equals("E"),wide="wide".equals(h.design);
            float scale=(float)h.scale,cx=SCENE_WIDTH/2f+(h.column-h.row)*ISLAND_TILE_WIDTH/2f+(wide?35*scale/6:0);
            float bottom=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(h.column+h.row+2)*ISLAND_TILE_HEIGHT/2f+1.25f*ISLAND_TILE_HEIGHT*scale;
            float width=70*scale*(wide?7f/6:1),height=(wide?100:74)*scale;
            if(opaqueAt(houseArtwork(h),new RectF(cx-width/2,bottom-height,cx+width/2,bottom),x,y,mirror))return h;
        }
        return null;
    }
    private void drawDecorativeFlowers(Canvas canvas) {
        paint.setColor(Color.WHITE);paint.setAlpha(255);paint.setFilterBitmap(true);
        final float[] offsets={.2f,.5f,.8f};
        for(WorldStore.SurfaceObject flower:worldSurfaces) if(flower.type.equals("flower")) {
            Bitmap image=flowerSprites.get(flower.material);if(image==null)continue;
            float cx=SCENE_WIDTH/2f+(flower.column-flower.row)*ISLAND_TILE_WIDTH/2f;
            float cy=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(flower.column+flower.row)*ISLAND_TILE_HEIGHT/2f;
            for(float u:offsets)for(float v:offsets) {
                float x=cx+(u-v)*ISLAND_TILE_WIDTH/2f,y=cy+(u+v-1)*ISLAND_TILE_HEIGHT/2f;
                int save=canvas.save();canvas.rotate((float)Math.toDegrees(Math.atan2(plantSway(flower,.3),3.5)),x,y+.7f);
                canvas.drawBitmap(image,null,new RectF(x-2.1f,y-2.8f,x+2.1f,y+.7f),paint);canvas.restoreToCount(save);
            }
        }
    }
    private boolean hasRoad(int column,int row,boolean water) {
        for(WorldStore.SurfaceObject o:worldSurfaces)if(o.type.equals("road") && o.column==column && o.row==row && water=="water".equals(o.material))return true;
        return false;
    }
    private float[] roadPoint(float cx,float cy,float u,float v) {
        return new float[]{cx+(u-v)*ISLAND_TILE_WIDTH/2f,cy+(u+v-1)*ISLAND_TILE_HEIGHT/2f};
    }
    private Path roadPath(float cx,float cy,float[][] coords,String facing) {
        Path path=new Path();for(int i=0;i<coords.length;i++) {float[] uv=facing==null?coords[i]:RoadGeometry.rotate(coords[i][0],coords[i][1],facing);float[] xy=roadPoint(cx,cy,uv[0],uv[1]);if(i==0)path.moveTo(xy[0],xy[1]);else path.lineTo(xy[0],xy[1]);}path.close();return path;
    }
    private void drawRoads(Canvas canvas) {
        for(WorldStore.SurfaceObject o:worldSurfaces)if(o.type.equals("road")) {
            float cx=SCENE_WIDTH/2f+(o.column-o.row)*ISLAND_TILE_WIDTH/2f,cy=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(o.column+o.row)*ISLAND_TILE_HEIGHT/2f;
            boolean water="water".equals(o.material);float[][] outline=RoadGeometry.outline(o.column,o.row,water,this::hasRoad);
            int base=Color.parseColor(o.material.equals("dirt")?"#cdbb99":o.material.equals("stone")?"#727b74":o.material.equals("gravel")?"#b4a993":o.material.equals("paving")?"#746d5e":o.material.equals("asphalt")?"#59625e":"#719fa1");
            int light=Color.parseColor(o.material.equals("dirt")?"#ded0b2":o.material.equals("stone")?"#a6ada3":o.material.equals("gravel")?"#d2c8b3":o.material.equals("paving")?"#aaa18f":o.material.equals("asphalt")?"#747e75":"#a8c7bc");
            int dark=Color.parseColor(o.material.equals("dirt")?"#a99476":o.material.equals("stone")?"#899187":o.material.equals("gravel")?"#8e8575":o.material.equals("paving")?"#968d7d":o.material.equals("asphalt")?"#454d4a":"#588487");
            paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);
            // At the island front, continue the path down the visible grass rim.
            for(int side=0;side<2;side++)if((side==0 && o.row==8)||(side==1 && o.column==8)) {
                float lo=side==0?(hasRoad(o.column-1,o.row,water)||o.column==0?0:RoadGeometry.INSET):(hasRoad(o.column,o.row-1,water)||o.row==0?0:RoadGeometry.INSET);
                float hi=side==0?(hasRoad(o.column+1,o.row,water)||o.column==8?1:1-RoadGeometry.INSET):(hasRoad(o.column,o.row+1,water)||o.row==8?1:1-RoadGeometry.INSET);
                float[] a=side==0?roadPoint(cx,cy,lo,1):roadPoint(cx,cy,1,lo),b=side==0?roadPoint(cx,cy,hi,1):roadPoint(cx,cy,1,hi);
                Path band=new Path();band.moveTo(a[0],a[1]-1);band.lineTo(b[0],b[1]-1);band.lineTo(b[0],b[1]+ISLAND_GRASS_BAND_DEPTH);band.lineTo(a[0],a[1]+ISLAND_GRASS_BAND_DEPTH);band.close();paint.setColor(blend(base,Color.BLACK,side==0?.15:.23));canvas.drawPath(band,paint);
            }
            Path surface=roadPath(cx,cy,outline,null);paint.setColor(base);canvas.drawPath(surface,paint);
            int save=canvas.save();canvas.clipPath(surface);String facing=o.edge==null?"S":o.edge;
            if(o.material.equals("stone") || o.material.equals("paving")) {
                int count=o.material.equals("stone")?2:4;
                for(int v=0;v<count;v++)for(int u=-1;u<count;u++) {
                    float shift=o.material.equals("paving")&&(v%2==1)?.5f:0,gap=.025f, width=o.material.equals("paving")?3f:2f;
                    float u0=(u+shift)/width+gap,u1=(u+1+shift)/width-gap,v0=v/(float)count+gap,v1=(v+1)/(float)count-gap;
                    if(u1<=0 || u0>=1)continue;
                    float[][] rect={{u0,v0},{u1,v0},{u1,v1},{u0,v1}};paint.setColor((u+v)%2==0?dark:light);canvas.drawPath(roadPath(cx,cy,rect,facing),paint);
                }
            } else if(water) {
                paint.setColor(light);for(float[] uv:new float[][]{{.3f,.28f},{.67f,.62f},{.34f,.73f}}){float[] rotated=RoadGeometry.rotate(uv[0],uv[1],facing),xy=roadPoint(cx,cy,rotated[0],rotated[1]);canvas.drawRect(xy[0]-1.5f,xy[1],xy[0]+1.5f,xy[1]+.55f,paint);}
            } else {
                int count=o.material.equals("gravel")?13:7;
                for(int i=0;i<count;i++) {float u=.08f+((i*37+o.column*13+o.row*7)%83)/100f,v=.08f+((i*61+o.column*5+o.row*17)%83)/100f;float[] uv=RoadGeometry.rotate(u,v,facing),xy=roadPoint(cx,cy,uv[0],uv[1]);paint.setColor(i%2==0?light:dark);canvas.drawRect(xy[0],xy[1],xy[0]+(count==13?1.5f:1.8f),xy[1]+.65f,paint);}
            }
            canvas.restoreToCount(save);paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);
        }
    }
    private static FenceOcclusion.Bounds occlusionBounds(RectF r) {return new FenceOcclusion.Bounds(r.left,r.top,r.right,r.bottom);}
    private final java.util.IdentityHashMap<Bitmap,int[]> occlusionPixels=new java.util.IdentityHashMap<>();
    private boolean opaqueAt(Bitmap bitmap,RectF bounds,float x,float y,boolean mirror) {
        if(bitmap==null || bounds.width()<=0 || bounds.height()<=0)return false;
        float u=(x-bounds.left)/bounds.width(),v=(y-bounds.top)/bounds.height();if(u<0 || u>=1 || v<0 || v>=1)return false;
        if(mirror)u=1f-u-0.000001f;
        int px=Math.max(0,Math.min(bitmap.getWidth()-1,(int)(u*bitmap.getWidth()))),py=Math.max(0,Math.min(bitmap.getHeight()-1,(int)(v*bitmap.getHeight())));
        int[] pixels=occlusionPixels.get(bitmap);if(pixels==null){pixels=new int[bitmap.getWidth()*bitmap.getHeight()];bitmap.getPixels(pixels,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());occlusionPixels.put(bitmap,pixels);}
        return Color.alpha(pixels[py*bitmap.getWidth()+px])>=32;
    }
    private boolean fenceOverlaps(Bitmap fence,RectF fenceBounds,boolean mirror,Bitmap target,RectF targetBounds,boolean targetMirror) {
        return target!=null && FenceOcclusion.overlaps(occlusionBounds(fenceBounds),(x,y)->opaqueAt(fence,fenceBounds,x,y,mirror),occlusionBounds(targetBounds),(x,y)->opaqueAt(target,targetBounds,x,y,targetMirror));
    }
    private Bitmap surfaceImage(WorldStore.SurfaceObject o) {
        if(o.type.equals("tree"))return treeImage(o);
        if(o.type.equals("sunflower"))return o.material.equals("N")||o.material.equals("W")?sunflowerBack:sunflowerFront;
        if(o.type.equals("bush"))return o.material.equals("wild-grass")?wildGrass:o.material.equals("spiky")?bushSpiky:o.material.equals("tiered")?bushTiered:o.material.equals("flower")?bushFlower:o.material.equals("berry")?bushBerry:bushRound;
        if(o.type.equals("mailbox"))return mailboxBirdhouse;
        if(o.type.equals("storage-box") || o.type.equals("selling-box")) {
            boolean back=o.material.equals("N")||o.material.equals("W"),opened=openStorage.contains(o.column+","+o.row);
            return o.type.equals("selling-box")?(opened?sellingBoxOpen:sellingBoxClosed):back?(opened?storageBackOpen:storageBackClosed):(opened?storageOpen:storageClosed);
        }
        return null;
    }
    private Bitmap houseArtwork(WorldStore.House house) {
        boolean back=house.facing.equals("N")||house.facing.equals("W"),mirror=house.facing.equals("N")||house.facing.equals("E");
        if(!"wide".equals(house.design))return back?houseBackImage:houseImage;
        return back?(mirror?houseWideBackMirrorImage:houseWideBackImage):(mirror?houseWideMirrorImage:houseWideImage);
    }
    private boolean fenceCoversEarlierObject(Bitmap image,RectF f,boolean mirror,boolean front) {
        for(WorldStore.House house:worldHouses) if(house.visible && (front || !showCharacter || house.column+house.row+2<visualColumn()+visualRow())) {
            boolean back=house.facing.equals("N")||house.facing.equals("W"),wide="wide".equals(house.design);
            Bitmap sprite=houseArtwork(house);float scale=(float)house.scale;
            float x=SCENE_WIDTH/2f+(house.column-house.row)*ISLAND_TILE_WIDTH/2f+(wide?35*scale/6:0),bottom=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(house.column+house.row+2)*ISLAND_TILE_HEIGHT/2f+1.25f*ISLAND_TILE_HEIGHT*scale;
            float width=70*scale*(wide?7f/6:1),height=(wide?100:74)*scale;
            if(fenceOverlaps(image,f,mirror,sprite,new RectF(x-width/2,bottom-height,x+width/2,bottom),house.facing.equals("N")||house.facing.equals("E")))return true;
        }
        if(front && showCharacter) {
            float x=SCENE_WIDTH/2f+(visualColumn()-visualRow())*ISLAND_TILE_WIDTH/2f,y=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(visualColumn()+visualRow())*ISLAND_TILE_HEIGHT/2f+visualWaterDepth(),width=13.5f*characterScale,height=25*characterScale;
            boolean back=characterFacing.equals("N")||characterFacing.equals("W");
            if(fenceOverlaps(image,f,mirror,back?mainCharacterBack:mainCharacter,new RectF(x-width/2,y-height,x+width/2,y),characterFacing.equals("W")||characterFacing.equals("E")))return true;
        }
        for(WorldStore.SurfaceObject o:worldSurfaces) {
            if(o.type.equals("flower")) {
                Bitmap sprite=flowerSprites.get(o.material);if(sprite==null)continue;
                float cx=SCENE_WIDTH/2f+(o.column-o.row)*ISLAND_TILE_WIDTH/2f,cy=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(o.column+o.row)*ISLAND_TILE_HEIGHT/2f;
                for(float u:new float[]{.2f,.5f,.8f})for(float v:new float[]{.2f,.5f,.8f}){float x=cx+(u-v)*ISLAND_TILE_WIDTH/2,y=cy+(u+v-1)*ISLAND_TILE_HEIGHT/2;if(fenceOverlaps(image,f,mirror,sprite,new RectF(x-2.1f,y-2.8f,x+2.1f,y+.7f),false))return true;}
            } else if(front) {
                Bitmap sprite=surfaceImage(o);if(sprite==null)continue;
                boolean flipped=(o.type.equals("sunflower") || o.type.equals("storage-box") || o.type.equals("selling-box")) && (o.material.equals("N")||o.material.equals("E"));
                if(fenceOverlaps(image,f,mirror,sprite,surfaceSpriteBounds(o),flipped))return true;
            }
        }
        return false;
    }
    private void drawFences(Canvas canvas,boolean front) {
        java.util.List<FenceGeometry.Edge> edges=new java.util.ArrayList<>();
        for(WorldStore.SurfaceObject o:worldSurfaces)if(o.type.equals("fence"))edges.add(new FenceGeometry.Edge(o.column,o.row,o.edge));
        for(PortalTravel.Portal portal:worldPortals)edges.add(new FenceGeometry.Edge(portal.tile.column,portal.tile.row,portal.edge));
        for(WorldStore.SurfaceObject o:worldSurfaces) if(o.type.equals("fence")) {
            float depth=o.column+o.row+((o.edge.equals("S")||o.edge.equals("E"))?.5f:-.5f);
            if(showCharacter && (depth>=visualColumn()+visualRow())!=front) continue;
            if(!showCharacter && front) continue;
            float x=SCENE_WIDTH/2f+(o.column-o.row)*ISLAND_TILE_WIDTH/2f;
            float y=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(o.column+o.row)*ISLAND_TILE_HEIGHT/2f;
            FenceGeometry.Bounds g=FenceGeometry.bounds(new FenceGeometry.Edge(o.column,o.row,o.edge),edges,x,y,ISLAND_TILE_WIDTH,ISLAND_TILE_HEIGHT);
            Bitmap image="slat".equals(o.material)?fenceSlat:"stone".equals(o.material)?fenceStone:"iron".equals(o.material)?fenceIron:fenceFarm;
            if(image==null)continue;
            RectF fenceBounds=new RectF(g.x,g.y,g.x+g.width,g.y+g.height);boolean fades=fenceCoversEarlierObject(image,fenceBounds,!g.rising,front);
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setAlpha(Math.round(255*(100-Math.max(0,o.opacity))/100f*(fades?.5f:1f)));paint.setFilterBitmap(true);
            int save=canvas.save();if(!g.rising)canvas.scale(-1,1,g.x+g.width/2,0);
            canvas.drawBitmap(image,null,new RectF(g.x,g.y,g.x+g.width,g.y+g.height),paint);canvas.restoreToCount(save);
        }
        paint.setAlpha(255);
    }
    void setWorldPortals(java.util.List<PortalTravel.Portal> portals) {
        worldPortals = portals == null ? java.util.Collections.emptyList() : new java.util.ArrayList<>(portals); invalidate();
    }
    private Matrix portalMatrix(PortalTravel.Portal portal) {
        float cx=SCENE_WIDTH/2f+(portal.tile.column-portal.tile.row)*ISLAND_TILE_WIDTH/2f;
        float cy=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(portal.tile.column+portal.tile.row)*ISLAND_TILE_HEIGHT/2f;
        float hw=ISLAND_TILE_WIDTH*.4f, hh=ISLAND_TILE_HEIGHT*.4f;
        float x1,y1,x2,y2;
        switch(portal.edge) {
            case "N": x1=cx;y1=cy-hh;x2=cx+hw;y2=cy;break;
            case "E": x1=cx;y1=cy+hh;x2=cx+hw;y2=cy;break;
            case "S": x1=cx-hw;y1=cy;x2=cx;y2=cy+hh;break;
            default: x1=cx-hw;y1=cy;x2=cx;y2=cy-hh;
        }
        boolean rising=portal.edge.equals("E") || portal.edge.equals("W");
        float leftX=rising?20:18,rightX=rising?46:44,leftY=rising?56:44,rightY=rising?44:56;
        float a=(x2-x1)/(rightX-leftX),d=.9f,b=(y2-y1-d*(rightY-leftY))/(rightX-leftX);
        float e=x1-a*leftX,f=y1-b*leftX-d*leftY;
        Matrix result=new Matrix(); result.setValues(new float[]{a,0,e,b,d,f,0,0,1});return result;
    }
    private RectF groundPortalBounds(PortalTravel.Portal portal) {
        float cx=SCENE_WIDTH/2f+(portal.tile.column-portal.tile.row)*ISLAND_TILE_WIDTH/2f;
        float cy=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(portal.tile.column+portal.tile.row)*ISLAND_TILE_HEIGHT/2f;
        return new RectF(cx-ISLAND_TILE_WIDTH/3f,cy-ISLAND_TILE_HEIGHT/3f,cx+ISLAND_TILE_WIDTH/3f,cy+ISLAND_TILE_HEIGHT/3f);
    }
    private static void portalDiamond(Path path,RectF g,float scale) {
        float cx=g.centerX(),cy=g.centerY(),hw=g.width()*scale/2f,hh=g.height()*scale/2f;
        path.moveTo(cx,cy-hh);path.lineTo(cx+hw,cy);path.lineTo(cx,cy+hh);path.lineTo(cx-hw,cy);path.close();
    }
    private static PointF portalOutlinePoint(RectF g,float time) {
        float t=((time%4)+4)%4;int index=(int)t;float f=t-index,cx=g.centerX(),cy=g.centerY(),hw=g.width()*.42f,hh=g.height()*.42f;
        float[] xs={cx,cx+hw,cx,cx-hw,cx},ys={cy-hh,cy,cy+hh,cy,cy-hh};
        return new PointF(xs[index]*(1-f)+xs[index+1]*f,ys[index]*(1-f)+ys[index+1]*f);
    }
    private void drawPortals(Canvas canvas, boolean front) {
        if(front)return;
        for(PortalTravel.Portal portal:worldPortals) {
            int alpha=Math.round(255*(100-Math.max(0,portal.opacity))/100f);RectF g=groundPortalBounds(portal);
            Path outline=new Path();outline.setFillType(Path.FillType.EVEN_ODD);portalDiamond(outline,g,1f);portalDiamond(outline,g,.68f);
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(54,159,146));paint.setAlpha(alpha);canvas.drawPath(outline,paint);
            int save=canvas.save();canvas.clipPath(outline);
            float start=animationEnabled && android.animation.ValueAnimator.areAnimatorsEnabled()?(float)((plantMotionSeconds*.4)%4):0f,end=start+.55f;
            PointF point=portalOutlinePoint(g,start);Path highlight=new Path();highlight.moveTo(point.x,point.y);
            for(int t=(int)Math.floor(start)+1;t<end;t++){point=portalOutlinePoint(g,t);highlight.lineTo(point.x,point.y);}
            point=portalOutlinePoint(g,end);highlight.lineTo(point.x,point.y);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.5f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(Color.rgb(180,255,226));paint.setAlpha(alpha);canvas.drawPath(highlight,paint);canvas.restoreToCount(save);
        }
        paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);paint.setStrokeCap(Paint.Cap.BUTT);
    }
    private RectF portalMarkerBounds(PortalTravel.Portal portal) {
        RectF g=groundPortalBounds(portal);float width=11f,height=13f;
        float x=Math.max(0,Math.min(SCENE_WIDTH-width,g.right-5));
        float y=Math.max(0,Math.min(SCENE_HEIGHT-height,g.top-height+2));
        return new RectF(x,y,x+width,y+height);
    }
    private java.util.List<PortalTravel.Tile> gameplayRoute = java.util.Collections.emptyList();
    private int routeNext = 1;
    void setGameplayRoute(java.util.List<PortalTravel.Tile> route,int next) {
        gameplayRoute=route==null?java.util.Collections.emptyList():new java.util.ArrayList<>(route);
        routeNext=Math.max(1,next);invalidate();
    }
    void setPortalWalkActive(boolean walking) { portalWalkActive=walking; if (!walking) { characterStepping=false;characterRunning=false;setGameplayRoute(null,1); } invalidate(); }
    boolean portalMarkerVisible(PortalTravel.Portal portal) {
        return !portalWalkActive && showCharacter && characterColumn==portal.tile.column && characterRow==portal.tile.row && portal.edge.equals(characterFacing);
    }
    private void drawPortalMarkers(Canvas canvas) {
        if(portalMarkerImage==null)return;
        paint.setColor(Color.WHITE);paint.setAlpha(255);paint.setFilterBitmap(true);
        for(PortalTravel.Portal portal:worldPortals) if(portalMarkerVisible(portal)) {
            RectF g=portalMarkerBounds(portal);
            paint.setColor(Color.rgb(110,67,43));canvas.drawRect(g.left,g.top,g.right,g.bottom-2,paint);canvas.drawRect(g.left+2,g.bottom-2,g.left+6,g.bottom,paint);
            paint.setColor(Color.rgb(255,241,206));canvas.drawRect(g.left+1,g.top+1,g.right-1,g.bottom-3,paint);canvas.drawRect(g.left+3,g.bottom-3,g.left+5,g.bottom-1,paint);
            paint.setColor(Color.WHITE);canvas.drawBitmap(portalMarkerImage,null,new RectF(g.left+2.5f,g.top+1,g.right-2.5f,g.bottom-3),paint);
        }
    }
    String portalMarkerAt(float screenX,float screenY) {
        if(getWidth()<=0 || getHeight()<=0) return null;
        float x=islandX(screenX),y=islandY(screenY);
        for(int i=worldPortals.size()-1;i>=0;i--){PortalTravel.Portal portal=worldPortals.get(i);
            RectF hit=portalMarkerBounds(portal);hit.inset(-3,-3);
            if(portalMarkerVisible(portal) && hit.contains(x,y)) return portal.id;
        }
        return null;
    }
    String portalAt(float screenX,float screenY) {
        if(getWidth()<=0 || getHeight()<=0) return null;
        float x=islandX(screenX),y=islandY(screenY);
        for(int i=worldPortals.size()-1;i>=0;i--){PortalTravel.Portal portal=worldPortals.get(i);
            RectF g=groundPortalBounds(portal);float dx=(x-g.centerX())/(g.width()/2),dy=(y-g.centerY())/(g.height()/2);if(Math.abs(dx)+Math.abs(dy)<=1f)return portal.id;
        }
        return null;
    }

    // Shared projection for surface objects; future types provide their footprint and height.
    private static float shadowCross(PointF a, PointF b, PointF c) {
        return (b.x-a.x)*(c.y-a.y) - (b.y-a.y)*(c.x-a.x);
    }

    private void drawObjectShadow(Canvas canvas, float x, float y, float width, float depth,
            float height, float[] light) {
        float h = Math.max(0, Math.min(height, LANGIT_MATAHARI_LIFT * 0.8f));
        float ratio = h / (LANGIT_MATAHARI_LIFT - h);
        float dx = (x - light[0]) * ratio;
        float dy = (y - light[1] - LANGIT_MATAHARI_LIFT) * ratio;
        PointF[] points = new PointF[] {new PointF(x, y-depth/2), new PointF(x+width/2, y),
            new PointF(x, y+depth/2), new PointF(x-width/2, y),
            new PointF(x+dx, y-depth/2+dy), new PointF(x+width/2+dx, y+dy),
            new PointF(x+dx, y+depth/2+dy), new PointF(x-width/2+dx, y+dy)};
        Arrays.sort(points, (a, b) -> { int c = Float.compare(a.x, b.x); return c != 0 ? c : Float.compare(a.y, b.y); });
        PointF[] hull = new PointF[16]; int n = 0;
        for (PointF point : points) {
            while (n >= 2 && shadowCross(hull[n-2], hull[n-1], point) <= 0) n--;
            hull[n++] = point;
        }
        int lowerSize = n;
        for (int i = points.length - 2; i >= 0; i--) {
            PointF point = points[i];
            while (n > lowerSize && shadowCross(hull[n-2], hull[n-1], point) <= 0) n--;
            hull[n++] = point;
        }
        if (n < 4) return;
        // Draw the full light projection rather than shrinking it to a fixed sprite fraction.
        Path path = new Path();
        path.moveTo(hull[0].x, hull[0].y);
        for (int i = 1; i < n-1; i++) path.lineTo(hull[i].x, hull[i].y);
        path.close(); canvas.drawPath(path, paint);
    }

    // Project the sprite alpha onto the ground as a 2D silhouette approximation.
    // There is no 3D object model, so the ground-depth component is stylized.
    private static final PorterDuffColorFilter OBJECT_SHADOW_FILTER =
            new PorterDuffColorFilter(Color.rgb(12, 23, 40), PorterDuff.Mode.SRC_IN);
    private void drawObjectSilhouette(Canvas canvas, Bitmap sprite, float x, float y,
                                      float width, float height, float[] light, boolean mirror) {
        float h = Math.max(0, Math.min(height, LANGIT_MATAHARI_LIFT * 0.8f));
        float ratio = h / (LANGIT_MATAHARI_LIFT - h);
        float dx = (x - light[0]) * ratio;
        float dy = (y - light[1] - LANGIT_MATAHARI_LIFT) * ratio + height * 0.4f;
        Matrix transform = new Matrix();
        transform.setValues(new float[]{1, -dx / height, x - width / 2 + dx,
                0, -dy / height, y + dy, 0, 0, 1});
        int saved = canvas.save();
        canvas.concat(transform);
        if (mirror) { canvas.translate(width, 0); canvas.scale(-1, 1); }
        paint.setColorFilter(OBJECT_SHADOW_FILTER);
        paint.setFilterBitmap(true);
        canvas.drawBitmap(sprite, null, new RectF(0, 0, width, height), paint);
        paint.setColorFilter(null);
        canvas.restoreToCount(saved);
    }

    private void drawSurfaceObjectShadows(Canvas canvas, SkyMath.Position light, double cloudCover, int code) {
        int save = canvas.save();
        setClipIslandSurface(canvas);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb((int) (255 * Math.max(0.12,
                cloudShadowOpacity(Math.max(light.altitudeDegrees, 0), cloudCover, code))), 12, 23, 40));
        float[] lightPoint = sunPlanePosition(light.hourAngleDegrees);
        for(WorldStore.House house:worldHouses) if(house.visible) {
            boolean wide="wide".equals(house.design),back="N".equals(house.facing)||"W".equals(house.facing);
            Bitmap image=houseArtwork(house);if(image==null)continue;
            float scale=(float)house.scale,x=SCENE_WIDTH/2f+(house.column-house.row)*ISLAND_TILE_WIDTH/2f+(wide?35*scale/6:0);
            float y=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(house.column+house.row+2)*ISLAND_TILE_HEIGHT/2f;
            int originalAlpha=paint.getAlpha();paint.setAlpha(Math.round(originalAlpha*(100-Math.max(0,house.opacity))/100f));
            drawObjectSilhouette(canvas,image,x,y,70*scale*(wide?7f/6:1),(wide?100:74)*scale,lightPoint,"N".equals(house.facing)||"E".equals(house.facing));
            paint.setAlpha(originalAlpha);
        }
        // Project the current orange-tree bitmap's alpha, including its wind deformation.
        for(WorldStore.SurfaceObject tree:worldSurfaces) if(tree.type.equals("tree") && tree.material.equals("orange")) {
            Bitmap treeSprite=treeImage(tree);if(treeSprite==null)continue;
            RectF g=surfaceSpriteBounds(tree);float height=g.height(),width=g.width();
            float h=Math.min(height,LANGIT_MATAHARI_LIFT*.8f),ratio=h/(LANGIT_MATAHARI_LIFT-h);
            float dx=(float)clamp((g.centerX()-lightPoint[0])*ratio,-height*1.5,height*1.5);
            float dy=(float)clamp((g.bottom-lightPoint[1]-LANGIT_MATAHARI_LIFT)*ratio,-height*.9,height*.9);
            if(Math.abs(dy)<height*.2f)dy=dy<0?-height*.2f:height*.2f;
            float sway=(float)orangeTreeSway(tree);
            Matrix transform=new Matrix();transform.setValues(new float[]{1,-(dx+sway)/height,g.centerX()-width/2+dx+sway,0,-dy/height,g.bottom+dy,0,0,1});
            int saveTree=canvas.save(),originalAlpha=paint.getAlpha();canvas.concat(transform);
            paint.setAlpha(Math.round(originalAlpha*(100-Math.max(0,tree.opacity))/100f));
            paint.setColorFilter(OBJECT_SHADOW_FILTER);paint.setFilterBitmap(true);
            canvas.drawBitmap(treeSprite,null,new RectF(0,0,width,height),paint);
            paint.setColorFilter(null);paint.setAlpha(originalAlpha);canvas.restoreToCount(saveTree);
        }
        Bitmap sprite = currentCharacterSprite();
        if (showCharacter && sprite != null && light.altitudeDegrees > 0 && visualWaterDepth() <= 0) {
            float x = SCENE_WIDTH / 2f + (visualColumn() - visualRow()) * ISLAND_TILE_WIDTH / 2f;
            float y = ISLAND_TOP + ISLAND_TILE_HEIGHT / 2f + (visualColumn() + visualRow()) * ISLAND_TILE_HEIGHT / 2f;
            float height=25f*characterScale,width=13.5f*characterScale;
            float[] ray=CharacterShadow.offset(light.azimuthDegrees,light.altitudeDegrees,height);
            if(Math.abs(ray[1])<height*.12f)ray[1]=Math.copySign(height*.12f,ray[1]);
            float strength=(float)Math.min(1,light.altitudeDegrees/10)*(1-.65f*(float)Math.max(0,Math.min(1,cloudCover/100)));
            paint.setColor(Color.argb(Math.round(255*.34f*strength),12,23,40));
            float shadowScale=CharacterShadow.fitScale(width,height,ray[0],ray[1]);
            Matrix transform=new Matrix();transform.setValues(new float[]{shadowScale,-shadowScale*ray[0]/height,x+shadowScale*(-width/2+ray[0]),0,-shadowScale*ray[1]/height,y+shadowScale*ray[1],0,0,1});
            int shadowSave=canvas.save();canvas.concat(transform);
            if("W".equals(characterFacing)||"E".equals(characterFacing)){canvas.translate(width,0);canvas.scale(-1,1);}
            paint.setColorFilter(OBJECT_SHADOW_FILTER);paint.setFilterBitmap(true);
            canvas.drawBitmap(sprite,null,new RectF(0,0,width,height),paint);
            paint.setColorFilter(null);canvas.restoreToCount(shadowSave);
        }
        // Faint contact diamonds stay attached to the ground anchors as objects move or scale.
        paint.setColor(Color.argb((int) (255 * 0.09), 12, 23, 40));
        for(WorldStore.House house:worldHouses) if(house.visible) {
            boolean wide="wide".equals(house.design);float scale=(float)house.scale;
            float x=SCENE_WIDTH/2f+(house.column-house.row)*ISLAND_TILE_WIDTH/2f+(wide?35*scale/6:0);
            float y=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(house.column+house.row+2)*ISLAND_TILE_HEIGHT/2f;
            int originalAlpha=paint.getAlpha();paint.setAlpha(Math.round(originalAlpha*(100-Math.max(0,house.opacity))/100f));
            drawObjectContact(canvas,x,y,70*scale*(wide?7f/6:1),ISLAND_TILE_HEIGHT*2.5f*scale);paint.setAlpha(originalAlpha);
        }
        canvas.restoreToCount(save);
    }

    private void drawObjectContact(Canvas canvas, float x, float y, float width, float depth) {
        Path path = new Path();
        path.moveTo(x, y - depth / 2); path.lineTo(x + width / 2, y);
        path.lineTo(x, y + depth / 2); path.lineTo(x - width / 2, y);
        path.close(); canvas.drawPath(path, paint);
    }

    private void drawHouses(Canvas canvas,boolean front) {
        for(WorldStore.House house:worldHouses) {
            if(!house.visible)continue;
            float depth=house.column+house.row+2;
            if(showCharacter && (depth>=visualColumn()+visualRow())!=front)continue;
            if(!showCharacter && front)continue;
            boolean back="N".equals(house.facing)||"W".equals(house.facing),mirror="N".equals(house.facing)||"E".equals(house.facing),wide="wide".equals(house.design);
            Bitmap sprite=houseArtwork(house);if(sprite==null)continue;
            float scale=(float)house.scale,x=SCENE_WIDTH/2f+(house.column-house.row)*ISLAND_TILE_WIDTH/2f+(wide?35*scale/6:0);
            float bottom=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(house.column+house.row+2)*ISLAND_TILE_HEIGHT/2f+1.25f*ISLAND_TILE_HEIGHT*scale;
            float width=70*scale*(wide?7f/6:1),height=(wide?100:74)*scale;
            paint.setColor(Color.WHITE);paint.setAlpha(Math.round(255*(100-Math.max(0,house.opacity))/100f));paint.setFilterBitmap(true);
            int save=canvas.save();if(mirror)canvas.scale(-1,1,x,0);
            canvas.drawBitmap(sprite,null,new RectF(x-width/2,bottom-height,x+width/2,bottom),paint);canvas.restoreToCount(save);
        }
        paint.setAlpha(255);
    }

    private float routeX(float column,float row){return SCENE_WIDTH/2f+(column-row)*ISLAND_TILE_WIDTH/2f;}
    private float routeY(float column,float row){return ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(column+row)*ISLAND_TILE_HEIGHT/2f;}
    private void drawGameplayRoute(Canvas canvas) {
        if(!gameplayMode || !showCharacter || gameplayRoute.size()<2 || routeNext>=gameplayRoute.size())return;
        Path path=new Path();path.moveTo(routeX(visualColumn(),visualRow()),routeY(visualColumn(),visualRow()));
        for(int i=routeNext;i<gameplayRoute.size();i++){PortalTravel.Tile tile=gameplayRoute.get(i);path.lineTo(routeX(tile.column,tile.row),routeY(tile.column,tile.row));}
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.BUTT);paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{2.5f,2f},0));
        paint.setColor(Color.BLACK);paint.setStrokeWidth(.5f);canvas.drawPath(path,paint);
        paint.setStyle(Paint.Style.FILL);
        PortalTravel.Tile end=gameplayRoute.get(gameplayRoute.size()-1);float x=routeX(end.column,end.row),y=routeY(end.column,end.row),hw=ISLAND_TILE_WIDTH/2f-1,hh=ISLAND_TILE_HEIGHT/2f-.5f;
        Path diamond=new Path();diamond.moveTo(x,y-hh);diamond.lineTo(x+hw,y);diamond.lineTo(x,y+hh);diamond.lineTo(x-hw,y);diamond.close();
        paint.setStyle(Paint.Style.STROKE);canvas.drawPath(diamond,paint);
        paint.setPathEffect(null);paint.setStyle(Paint.Style.FILL);paint.setStrokeCap(Paint.Cap.BUTT);
    }
    private void drawHoeTarget(Canvas canvas) {
        if(!gameplayMode || !hoeSelected || !showCharacter)return;
        int column=Math.round(visualColumn()),row=Math.round(visualRow());
        switch(characterFacing) {case "N":row--;break;case "E":column++;break;case "S":row++;break;case "W":column--;break;default:return;}
        if(column<0 || row<0 || column>=ISLAND_GRID_SIZE || row>=ISLAND_GRID_SIZE || (worldTerrain!=null && !worldTerrain.canStand(column,row)))return;
        float x=SCENE_WIDTH/2f+(column-row)*ISLAND_TILE_WIDTH/2f;
        float y=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(column+row)*ISLAND_TILE_HEIGHT/2f;
        float hw=ISLAND_TILE_WIDTH/2f-.5f,hh=ISLAND_TILE_HEIGHT/2f-.3f;
        Path outline=new Path();outline.moveTo(x,y-hh);outline.lineTo(x+hw,y);outline.lineTo(x,y+hh);outline.lineTo(x-hw,y);outline.close();
        paint.setColor(Color.WHITE);paint.setStrokeWidth(1.2f);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(outline,paint);paint.setStyle(Paint.Style.FILL);
    }
    private final PlayerIdleBow playerIdleBow = new PlayerIdleBow();
    private float idleBowAmount;
    private void updateIdleBow(long now) {
        boolean enabled=gameplayMode && showCharacter && !characterStepping && !portalWalkActive
                && animationEnabled && android.animation.ValueAnimator.areAnimatorsEnabled();
        String key=enabled ? surfaceWorldId+":"+characterColumn+":"+characterRow : null;
        idleBowAmount=playerIdleBow.update(now,key,enabled);
    }
    private Bitmap currentCharacterSprite() {
        boolean back="N".equals(characterFacing)||"W".equals(characterFacing);
        Bitmap sprite=back?mainCharacterBack:mainCharacter;
        int idleFrame=PlayerIdleBow.frame(idleBowAmount);
        if(idleFrame>0 && !characterStepping) {
            Bitmap idle=back ? (idleFrame==1?idleBackTransition:idleBackPose)
                    : (idleFrame==1?idleFrontTransition:idleFrontPose);
            if(idle!=null)return idle;
        }
        if(characterStepping && animationEnabled && android.animation.ValueAnimator.areAnimatorsEnabled()) {
            int frame=(int)Math.floor((stepIndex-1+stepProgress())*12)%12;
            Bitmap current=characterRunning?(back?runBack:runFront)[frame]:(back?walkBack:walkFront)[frame];
            if(current!=null)sprite=current;
        }
        return sprite;
    }
    private void drawMainCharacter(Canvas canvas, double motionTime) {
        boolean back = "N".equals(characterFacing) || "W".equals(characterFacing);
        boolean mirror = "W".equals(characterFacing) || "E".equals(characterFacing);
        Bitmap sprite = currentCharacterSprite();
        if (!showCharacter || sprite == null) return;
        float x = SCENE_WIDTH / 2.0f + (visualColumn() - visualRow()) * ISLAND_TILE_WIDTH / 2.0f;
        float y = ISLAND_TOP + ISLAND_TILE_HEIGHT / 2.0f
                + (visualColumn() + visualRow()) * ISLAND_TILE_HEIGHT / 2.0f;
        float depth=visualWaterDepth();
        y+=.5f*characterScale+depth;
        float width = 13.5f * characterScale;
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.argb(97,12,23,30));
        if(depth==0f) canvas.drawOval(new RectF(x-4.6f*characterScale,y-1.25f*characterScale,x+4.6f*characterScale,y+.35f*characterScale),paint);
        float idle = characterStepping ? 0f : (Math.sin(motionTime * Math.PI / 1.2) > 0.5 ? 0.5f : 0.0f);
        // Keep the dimensions fixed while moving; bounce translates rather than stretches.
        float height = 25.0f * characterScale + (characterStepping ? 0 : idle);
        paint.setColor(Color.WHITE);
        paint.setAlpha(255);
        paint.setFilterBitmap(true);
        int save = canvas.save();
        if (mirror) canvas.scale(-1, 1, x, 0);
        // Use the same gait frame, compositing the submerged portion over water.
        RectF destination=new RectF(x-width/2,y-height,x+width/2,y);
        if (depth>0) {
            int upper=canvas.save();canvas.clipRect(x-width/2-2,y-height-2,x+width/2+2,y-depth);
            canvas.drawBitmap(sprite,null,destination,paint);canvas.restoreToCount(upper);
            int lower=canvas.save();
            Path waterFootprint=new Path();waterFootprint.moveTo(x-ISLAND_TILE_WIDTH/2f,y-depth);
            waterFootprint.lineTo(x,y-depth-ISLAND_TILE_HEIGHT/2f);waterFootprint.lineTo(x+ISLAND_TILE_WIDTH/2f,y-depth);
            waterFootprint.lineTo(x,y-depth+ISLAND_TILE_HEIGHT/2f);waterFootprint.close();canvas.clipPath(waterFootprint);
            canvas.clipRect(x-width/2-2,y-depth,x+width/2+2,y+2);
            paint.setAlpha(89);canvas.drawBitmap(sprite,null,destination,paint);paint.setAlpha(255);canvas.restoreToCount(lower);
        } else canvas.drawBitmap(sprite,null,destination,paint);
        canvas.restoreToCount(save);
        if(depth>0) {
            paint.setColor(0x99dafaf4);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.45f);
            float radius=Math.min(width*.48f,5f);
            canvas.drawOval(new RectF(x-radius,y-depth-1.1f,x+radius,y-depth+1.1f),paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawMovingHair(Canvas canvas, Bitmap sprite, int sw, int sh, int top,
            float width, float height, float gait, float runBlend) {
        int hairEnd=(int)(sh*CharacterHairMotion.HAIR_END), bands=8;
        Rect src=new Rect(); RectF dst=new RectF();
        boolean enabled=animationEnabled && android.animation.ValueAnimator.areAnimatorsEnabled();
        for(int i=0;i<bands;i++) {
            int y0=i*hairEnd/bands,y1=(i+1)*hairEnd/bands;
            float dx=CharacterHairMotion.offset((y0+y1)/(2f*sh),gait,runBlend,characterScale,enabled);
            src.set(0,y0,sw,y1);
            dst.set(-width/2+dx,height*y0/sh,width/2+dx,height*y1/sh);
            canvas.drawBitmap(sprite,src,dst,paint);
        }
        src.set(0,hairEnd,sw,top);
        dst.set(-width/2,height*hairEnd/sh,width/2,height*top/sh);
        canvas.drawBitmap(sprite,src,dst,paint);
    }

    private void drawPivotedSleeve(Canvas canvas,Bitmap sprite,int left,int top,int right,int bottom,
            float x,float y,float width,float height,float angle,float phase,float runBlend,boolean leftArm) {
        int elbow=top+(int)((bottom-top)*.55f);
        float upperHeight=height*(elbow-top)/(bottom-top);
        int save=canvas.save(); canvas.translate(x,y); canvas.rotate(angle);
        // One source pixel of overlap avoids a gap at the rotating elbow.
        canvas.drawBitmap(sprite,new Rect(left,top,right,elbow+1),
                new RectF(-width/2,0,width/2,upperHeight+height/(bottom-top)),paint);
        canvas.translate(0,upperHeight);
        canvas.rotate((float)Math.toDegrees(CharacterGait.elbowRadians(phase,runBlend,leftArm)));
        canvas.drawBitmap(sprite,new Rect(left,elbow,right,bottom),
                new RectF(-width/2,0,width/2,height-upperHeight),paint);
        canvas.restoreToCount(save);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if(playerIdleBow!=null)playerIdleBow.reset();
        idleBowAmount=0;
    }

    public void setAnimationEnabled(boolean enabled) {
        playerIdleBow.reset(); idleBowAmount=0;
        animationEnabled = enabled;
        if (enabled) postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() <= 0 || getHeight() <= 0) return;

        long frameTime=SystemClock.uptimeMillis();
        updateIdleBow(frameTime);
        if(gameplayMode && showCharacter) {
            float tx=SCENE_WIDTH/2f+(visualColumn()-visualRow())*ISLAND_TILE_WIDTH/2f;
            float ty=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(visualColumn()+visualRow())*ISLAND_TILE_HEIGHT/2f;
            float dt=cameraFrameTime==0?0:(frameTime-cameraFrameTime)/1000f;
            boolean snap=!animationEnabled || !android.animation.ValueAnimator.areAnimatorsEnabled();
            cameraMoving=gameplayCamera.advance(tx,ty,28f/(islandScaleX()*mapZoom()),16f/(islandScaleY()*mapZoom()),dt,snap);
        } else {gameplayCamera.reset();cameraMoving=false;}
        cameraFrameTime=frameTime;
        long now = System.currentTimeMillis();
        double elapsedSeconds = (now - animationStartMillis) / 1000.0;
        boolean pondMotion=animationEnabled && android.animation.ValueAnimator.areAnimatorsEnabled();
        double motionSeconds = animationEnabled ? elapsedSeconds : 0.0;
        plantMotionSeconds=motionSeconds;
        if(pondMotion) {
            double dt=pondWindLast<0?0:Math.max(0,Math.min(.25,elapsedSeconds-pondWindLast));
            double speed=weather==null?4:Math.max(0,weather.windSpeedKmh);
            double bearing=Math.toRadians((weather==null?90:weather.windDirectionDegrees)+180);
            pondWindX+=Math.sin(bearing)*speed*dt;pondWindY-=Math.cos(bearing)*speed*dt;
            pondWindLast=elapsedSeconds;
        } else pondWindLast=-1;
        SkyMath.Position sun = SkyMath.sun(now, latitude, longitude);
        SkyMath.Position moon = SkyMath.moon(now, latitude, longitude);
        double cloudCover = weather == null ? 42.0 : weather.cloudCoverPercent;
        int code = weather == null ? 3 : weather.weatherCode;
        double windSpeed = weather == null ? 4.0 : weather.windSpeedKmh;
        double windDirection = weather == null ? 90.0 : weather.windDirectionDegrees;
        CloudState[] cloudStates = createCloudStates(motionSeconds, cloudCover,
                windSpeed, windDirection, sun.altitudeDegrees, sun.hourAngleDegrees, code);

        if (sceneBitmap == null || sceneBitmap.getWidth() != getWidth()
                || sceneBitmap.getHeight() != getHeight()) {
            sceneBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            sceneCanvas = new Canvas(sceneBitmap);
        }
        Canvas scene = sceneCanvas;
        int save = scene.save();
        scene.scale((float)getWidth() / SCENE_WIDTH, (float)getHeight() / SCENE_HEIGHT);
        drawSky(scene, sun, cloudCover, code);
        int skySave = scene.save();
        // The sky band runs from above the frame down to the horizon; clipping at half height
        // used to behead any cloud tall enough to reach past it.
        scene.clipRect(0, -SCENE_HEIGHT, SCENE_WIDTH, SCENE_HEIGHT * 2);
        // A virtual Moon continues to cast shadows while it is below the horizon.
        SkyMath.Position shadowLight = sun.altitudeDegrees <= MOON_VISIBLE_BELOW_SUN ? moon : sun;
        drawFog(scene, code);
        drawStormFlash(scene, elapsedSeconds, code);
        scene.restoreToCount(skySave);
        int islandSave=scene.save();applyIslandScale(scene);
        drawFloatingIsland(scene, sun.altitudeDegrees, sun.azimuthDegrees,
                sun.hourAngleDegrees, cloudCover, code, moon, cloudStates, sun);
        drawDecorativeFlowers(scene);
        drawRoads(scene);
        drawGameplayRoute(scene);
        drawSurfaceObjectShadows(scene, shadowLight, cloudCover, code);
        drawHouses(scene,false);
        drawFences(scene, false);
        drawSurfaceSprites(scene, false);
        drawPortals(scene, false);
        drawMainCharacter(scene, motionSeconds);
        drawHouses(scene,true);
        drawPortals(scene, true);
        drawSurfaceSprites(scene, true);
        drawFences(scene, true);
        drawHoeTarget(scene);
        // Shadow union is above the island and its objects but clipped to the surface.
        drawProjectedCloudShadows(scene, cloudStates, shadowLight.altitudeDegrees,
                shadowLight.hourAngleDegrees, cloudCover, code, true,
                CloudLayout.shadowFade(motionSeconds, animationEnabled));
        scene.restoreToCount(islandSave);
        // Cloud sprites are intentionally hidden; only their surface shadows remain.
        // Sun and moon sprites are temporarily hidden; lighting and stars remain active.
        drawStars(scene, sun.altitudeDegrees, cloudCover);
        drawPrecipitation(scene, elapsedSeconds);
        int markerSave=scene.save();applyIslandScale(scene);
        drawPortalMarkers(scene);
        drawSellingMarkers(scene);
        scene.restoreToCount(markerSave);
        scene.restoreToCount(save);

        renderedSceneBounds.set(0, 0, getWidth(), getHeight());
        canvas.drawBitmap(sceneBitmap, null, renderedSceneBounds, renderBitmapPaint);

        if (getVisibility() == VISIBLE && (animationEnabled || cameraMoving)) {
            postInvalidateDelayed((characterStepping || cameraMoving) ? 33 : 40);
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

        // A single gradient avoids rasterization seams between adjacent scaled bands.
        int[] colors = new int[palette.length];
        double coverMix = clamp(cloudCover / 100.0 * 0.58, 0.0, 0.58);
        if (code >= 95) coverMix = Math.max(coverMix, 0.38);
        for (int i = 0; i < colors.length; i++) {
            double amount = i / (double) (colors.length - 1);
            int overcast = Color.rgb(interpolate(55, 130, amount),
                    interpolate(68, 140, amount), interpolate(84, 151, amount));
            colors[i] = blend(palette[i], overcast, coverMix);
            if (daylight < 0.35 && code >= 61) {
                colors[i] = blend(colors[i], Color.rgb(27, 34, 53), 0.16);
            }
        }
        paint.setShader(new LinearGradient(0, 0, 0, SCENE_HEIGHT, colors, null,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, SCENE_WIDTH, SCENE_HEIGHT, paint);
        paint.setShader(null);
    }

    // Decorative screen-space stars; not a representation of the real night sky.
    private void drawStars(Canvas canvas, double altitude, double cloudCover) {
        double opacity=clamp((-altitude-4)/8,0,1)*(1-clamp(cloudCover/100,0,1)*.85);
        if(opacity<=0)return;
        float[][] stars={{.08f,.12f},{.18f,.22f},{.30f,.08f},{.39f,.19f},{.51f,.06f},{.62f,.16f},{.73f,.08f},{.85f,.20f},{.94f,.09f},{.11f,.35f},{.25f,.30f},{.44f,.34f},{.57f,.28f},{.68f,.36f},{.80f,.31f},{.93f,.38f}};
        paint.setColor(Color.rgb(244,245,220)); paint.setAlpha((int)Math.round(255*opacity));
        for(int i=0;i<stars.length;i++) {
            float side=RENDER_CELL*celestialScale()*(i%4==0?2:1);
            double angle=Math.PI*(i+.5)/stars.length;
            float x=mapDestinationX()+(SCENE_WIDTH/2f+(float)(300*Math.cos(angle))-mapSourceX())*islandScaleX()*mapZoom();
            float y=mapDestinationY()+(ISLAND_CENTER_Y-(float)(300*Math.sin(angle))-mapSourceY())*islandScaleY()*mapZoom();
            canvas.drawRect(x-side/2,y-side/2,x+side/2,y+side/2,paint);
        }
        paint.setAlpha(255);
    }

    /**
     * Relative luminance of what actually sits behind the plate's shadow: the sky gradient at the
     * floor line, after the same weather blend drawSky applies. Measuring beats guessing here,
     * because the whole problem is that a fixed alpha removes a fixed fraction of a backdrop whose
     * own brightness swings by nearly an order of magnitude between noon and midnight.
     */
    private double floorLuminance(double cloudCover, int code) {
        SkyMath.Position sun = SkyMath.sun(System.currentTimeMillis(), latitude, longitude);
        int[] palette;
        if (sun.altitudeDegrees < -12.0) palette = NIGHT_SKY;
        else if (sun.altitudeDegrees < 2.0) {
            palette = sun.azimuthDegrees > 180.0 ? SUNSET_SKY : DAWN_SKY;
        } else palette = DAY_SKY;
        float amount = (SCENE_HEIGHT - 6) / (float) SCENE_HEIGHT;
        int base = paletteColor(palette, amount);
        int overcast = Color.rgb(
                interpolate(55, 130, amount),
                interpolate(68, 140, amount),
                interpolate(84, 151, amount));
        double coverMix = clamp(cloudCover / 100.0 * 0.58, 0.0, 0.58);
        if (code >= 95) coverMix = Math.max(coverMix, 0.38);
        int mixed = blend(base, overcast, coverMix);
        return 0.2126 * Color.red(mixed) + 0.7152 * Color.green(mixed) + 0.0722 * Color.blue(mixed);
    }

    private float snapCelestialCoordinate(float coordinate) {
        return Math.round(coordinate * RENDER_SCALE) / (float) RENDER_SCALE;
    }

    private void setClipIslandSurface(Canvas canvas) {
        float centerX = SCENE_WIDTH / 2.0f;
        float halfWidth = ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH / 2.0f;
        float halfHeight = ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT / 2.0f;
        islandClipPath.reset();
        if(worldTerrain!=null) {
            for(int row=0;row<ISLAND_GRID_SIZE;row++) for(int column=0;column<ISLAND_GRID_SIZE;column++) {
                if(!worldTerrain.canStand(column,row)) continue;
                float x=centerX+(column-row)*ISLAND_TILE_WIDTH/2f;
                float y=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(column+row)*ISLAND_TILE_HEIGHT/2f;
                islandClipPath.moveTo(x,y-ISLAND_TILE_HEIGHT/2f);islandClipPath.lineTo(x+ISLAND_TILE_WIDTH/2f,y);
                islandClipPath.lineTo(x,y+ISLAND_TILE_HEIGHT/2f);islandClipPath.lineTo(x-ISLAND_TILE_WIDTH/2f,y);islandClipPath.close();
            }
            canvas.clipPath(islandClipPath);return;
        }
        islandClipPath.moveTo(centerX, ISLAND_CENTER_Y - halfHeight);
        islandClipPath.lineTo(centerX + halfWidth, ISLAND_CENTER_Y);
        islandClipPath.lineTo(centerX, ISLAND_CENTER_Y + halfHeight);
        islandClipPath.lineTo(centerX - halfWidth, ISLAND_CENTER_Y);
        islandClipPath.close();
        canvas.clipPath(islandClipPath);
    }

    /**
     * Draws a sprite mask as a ground shadow: the same cells, scaled down, then squashed by the
     * island's own tile ratio so it lies flat on the dimetric surface instead of standing upright.
     */
    private void addGroundSilhouette(Path path, String[] shape, float centreX, float centreY,
            float width, float height) {
        float cellX = width / shape[0].length();
        float cellY = height / shape.length;
        paint.setStyle(Paint.Style.FILL);
        for (int row = 0; row < shape.length; row += 1) {
            String line = shape[row];
            float top = centreY - height * 0.5f + row * cellY;
            for (int column = 0; column < line.length();) {
                if (line.charAt(column) == '0') { column++; continue; }
                int end=column+1; while(end<line.length() && line.charAt(end)!='0') end++;
                float left = centreX - width * 0.5f + column * cellX;
                path.addRect(left, top, left + (end-column)*cellX, top + cellY, Path.Direction.CW);
                column=end;
            }
        }
    }

    /**
     * The shadow is thrown AWAY from the light. Progress runs 0 at the eastern (right) end to 1 at
     * the western (left) end, so a morning sun on the right pushes the shadow left, which is the
     * negative direction. This is the opposite sign to the island body's own shadow, whose drift
     * describes which side the light is on rather than which way the shadow is thrown.
     */
    private double cloudShadowLean(double lightHourAngle) {
        return -(0.5 - celestialProgress(lightHourAngle))
                * ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH * 0.55;
    }

    /**
     * One shared light model for every cloud shadow, on the island or on the ground, so the two
     * never drift apart in weight the way the island body's and the cloud's used to.
     */
    private double cloudShadowOpacity(double altitude, double cloudCover, int code) {
        double directLight = clamp((altitude + 8.0) / 28.0, 0.0, 1.0);
        double cloudFilter = 1.0 - clamp(cloudCover / 100.0 * 0.45, 0.0, 0.45);
        if (code >= 95) cloudFilter *= 0.82;
        return (0.10 + directLight * 0.30) * cloudFilter * 0.70 * 0.9 * 0.6;
    }

    /**
     * The floor the island floats over. The island's own cast shadow already establishes where that
     * floor reads on screen, so cloud shadows use the same line rather than a guess.
     */
    private float groundShadowY() {
        float bottomY = ISLAND_TOP + ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT;
        return bottomY - ISLAND_BODY_SHADOW_RISE;
    }

    /**
     * Ground shadows sit on the floor the island floats over, drawn before the island so the plate
     * covers them where they overlap. They borrow the island's own cast-shadow language: the same
     * colour, the same opacity model and the same squash, so a shadow on the ground reads as the
     * same material as the one under the island rather than as a second effect.
     */
    private void drawProjectedCloudShadows(Canvas canvas, CloudState[] clouds, double sunAltitude,
            double sunHourAngle, double cloudCover, int code, boolean clipToIsland, double fade) {
        if (clouds.length == 0) return;
        boolean wet = cloudCover >= 65 || code == 3 || code == 45 || code == 48 || code >= 51;
        double opacity = Math.max(cloudShadowOpacity(Math.max(sunAltitude, 0.0), cloudCover, code), wet ? 0.16 : 0.0) * fade;
        int save = canvas.save();
        if (clipToIsland) setClipIslandSurface(canvas);
        paint.setColor(Color.argb((int) (255.0 * opacity), 12, 23, 40));
        Path mask = new Path();
        float blur = CloudLayout.shadowBlur(cloudCover, code);
        float blurMargin = blur * 3f;
        for (CloudState cloud : clouds) {
            float x = cloud.planeX, y = cloud.planeY + LANGIT_AWAN_LIFT;
            float[] position=CloudShadowPlacement.onIsland(x,y,SCENE_WIDTH/2f,ISLAND_CENTER_Y,ISLAND_TILE_WIDTH,ISLAND_TILE_HEIGHT,ISLAND_GRID_SIZE);
            float width=cloud.shape[0].length()*cloud.cell, height=cloud.shape.length*cloud.cell;
            float scale=Math.min(1f, Math.min(4f*ISLAND_TILE_WIDTH/width, 4f*ISLAND_TILE_HEIGHT/height));
            if (canvas.quickReject(position[0]-width*scale/2-blurMargin, position[1]-height*scale/2-blurMargin,
                    position[0]+width*scale/2+blurMargin, position[1]+height*scale/2+blurMargin, Canvas.EdgeType.AA)) continue;
            addGroundSilhouette(mask, cloud.shape, position[0], position[1], width*scale, height*scale);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setMaskFilter(new BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL));
        canvas.drawPath(mask, paint); // Blur the union once, without darkening overlaps.
        paint.setMaskFilter(null);
        canvas.restoreToCount(save);
    }

    private void drawFloatingIsland(Canvas canvas, double sunAltitude, double sunAzimuth,
            double sunHourAngle, double cloudCover, int code,
            SkyMath.Position moon, CloudState[] cloudStates, SkyMath.Position sunBody) {
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
        double lightDeclination = sunBody.declinationDegrees;
        if (sunAltitude <= 2.0 && moon != null && moon.altitudeDegrees > 2.0) {
            lightAltitude = clamp(moon.altitudeDegrees
                    * (0.35 + moon.illumination * 0.65), 0.0, 90.0);
            lightHourAngle = moon.hourAngleDegrees;
            lightDeclination = moon.declinationDegrees;
        }
        // Cloud shadows are separate; the island shadow scales with its selected light's distance.
        drawIslandShadow(canvas, centerX, lowerY, lightAltitude, lightHourAngle, cloudCover, code, lightDeclination);

        if(worldTerrain!=null) {
            drawPortableTerrain(canvas,palette,sunlight);
            return;
        }
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
                if (!canvas.quickReject(tileX-ISLAND_TILE_WIDTH/2f, tileY-ISLAND_TILE_HEIGHT/2f,
                        tileX+ISLAND_TILE_WIDTH/2f, tileY+ISLAND_TILE_HEIGHT/2f+ISLAND_GRASS_BAND_DEPTH,
                        Canvas.EdgeType.AA))
                    drawGrassTile(canvas, tileX, tileY, lightTile, column, row, palette, sunlight);
            }
        }
        // Draw the upper frame last where the two projections overlap.
        if (LANGIT_AWAN_ENABLED) {
            float axisLength = (float) Math.hypot(ISLAND_TILE_WIDTH, ISLAND_TILE_HEIGHT);
            drawLangitFrame(canvas, LANGIT_AWAN_LIFT, 0xFFFFFFFF,
                    -LANGIT_AWAN_WEST_SHIFT * ISLAND_TILE_WIDTH / axisLength,
                    -LANGIT_AWAN_WEST_SHIFT * ISLAND_TILE_HEIGHT / axisLength);
        }
        if (LANGIT_MATAHARI_ENABLED) {
            float axisLength = (float) Math.hypot(ISLAND_TILE_WIDTH, ISLAND_TILE_HEIGHT);
            drawLangitFrame(canvas, LANGIT_MATAHARI_LIFT, 0xFF0B1420,
                    -LANGIT_MATAHARI_WEST_SHIFT * ISLAND_TILE_WIDTH / axisLength,
                    -LANGIT_MATAHARI_WEST_SHIFT * ISLAND_TILE_HEIGHT / axisLength);
        }
        int topRim = blend(palette.grassSeam, palette.grassDark, 0.32);
        drawSmoothIslandLine(canvas, leftX, middleY, centerX, bottomY, topRim, 0.60f);
        drawSmoothIslandLine(canvas, centerX, bottomY, rightX, middleY, topRim, 0.60f);
    }

    private void drawWaterTopBanks(Canvas canvas, float x, float y, float hw, float hh,
                                   boolean[] borders, IslandPalette palette) {
        float inset=.13f;
        float[][] v={{x,y-hh},{x+hw,y},{x,y+hh},{x-hw,y}};
        float[][] d={{-hw*inset,hh*inset},{-hw*inset,-hh*inset},
                {hw*inset,-hh*inset},{hw*inset,hh*inset}};
        int color=blend(palette.soilLight,palette.soilDark,.22);
        for(int i=0;i<4;i++) {
            if (!borders[i]) continue;
            int next=(i+1)%4;
            float ax=v[i][0],ay=v[i][1],bx=v[next][0],by=v[next][1],dx=d[i][0],dy=d[i][1];
            drawSmoothIslandQuad(canvas,ax,ay,bx,by,bx+dx,by+dy,ax+dx,ay+dy,color,color,0,0f);
            if (borders[next]) {
                float ex=d[next][0],ey=d[next][1];
                drawSmoothIslandQuad(canvas,bx,by,bx+dx,by+dy,bx+dx+ex,by+dy+ey,bx+ex,by+ey,color,color,0,0f);
            }
        }
    }
    private boolean waterAt(int column,int row) {
        return column>=0 && row>=0 && column<ISLAND_GRID_SIZE && row<ISLAND_GRID_SIZE
                && worldTerrain.tile(column,row)==3;
    }
    private void drawPondLife(Canvas canvas, float x, float y, int index) {
        if(pondLife==null || index<0 || index>=pondLife.cells.length || pondLife.cells[index]==null)return;
        boolean animate=animationEnabled && android.animation.ValueAnimator.areAnimatorsEnabled();
        float hw=ISLAND_TILE_WIDTH/2f,hh=ISLAND_TILE_HEIGHT/2f;
        Path diamond=new Path();diamond.moveTo(x,y-hh);diamond.lineTo(x+hw,y);diamond.lineTo(x,y+hh);diamond.lineTo(x-hw,y);diamond.close();
        int save=canvas.save();canvas.clipPath(diamond);
        // Draw fish first, beneath the floating pads.
        for(int pass=0;pass<2;pass++)for(PondLife.Item item:pondLife.cells[index]) {
            if(item.fish!=(pass==0))continue;
            double phase=(item.seed%997)/997.0*Math.PI*2, u,v;
            if(item.fish) {
                double a=phase+(animate?plantMotionSeconds*(.6+(item.seed%3)*.1):0);
                u=.17*Math.cos(a);v=.13*Math.sin(a);
                float px=x+(float)(u-v)*hw,py=y+(float)(u+v)*hh,dir=Math.sin(a)>=0?1:-1;
                paint.setStyle(Paint.Style.FILL);paint.setColor(0x78214d4f);
                canvas.drawOval(new RectF(px-2.2f,py-.85f,px+2.2f,py+.85f),paint);
                Path tail=new Path();tail.moveTo(px-1.6f*dir,py);tail.lineTo(px-3*dir,py-1.1f);tail.lineTo(px-3*dir,py+1.1f);tail.close();canvas.drawPath(tail,paint);
            } else {
                u=.16*Math.sin(pondWindX*.10+phase);v=.16*Math.sin(pondWindY*.10+phase*1.17);
                float px=x+(float)(u-v)*hw,py=y+(float)(u+v)*hh;
                paint.setStyle(Paint.Style.FILL);paint.setColor(0xff557f4b);canvas.drawOval(new RectF(px-2.5f,py-1.25f,px+2.5f,py+1.25f),paint);
                paint.setStyle(Paint.Style.STROKE);paint.setColor(0xffa9ca89);paint.setStrokeWidth(.4f);canvas.drawLine(px,py,px+1.9f,py-1f,paint);paint.setStyle(Paint.Style.FILL);
            }
        }
        canvas.restoreToCount(save);
    }
    private void drawPortableTerrain(Canvas canvas, IslandPalette palette, IslandSunlight sunlight) {
        for(int diagonal=0;diagonal<ISLAND_GRID_SIZE*2-1;diagonal++) for(int row=0;row<ISLAND_GRID_SIZE;row++) {
            int column=diagonal-row;
            if(column<0 || column>=ISLAND_GRID_SIZE) continue;
            int material=worldTerrain.tile(column,row); if(material==0)continue;
            float x=SCENE_WIDTH/2f+(column-row)*ISLAND_TILE_WIDTH/2f;
            float y=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(column+row)*ISLAND_TILE_HEIGHT/2f;
            float hw=ISLAND_TILE_WIDTH/2f,hh=ISLAND_TILE_HEIGHT/2f;
            if(canvas.quickReject(x-hw,y-hh,x+hw,y+hh+ISLAND_DEPTH,Canvas.EdgeType.AA))continue;
            if(!worldTerrain.canStand(column,row+1))
                drawSmoothIslandQuad(canvas,x-hw,y,x,y+hh,x,y+hh+ISLAND_DEPTH,x-hw,y+ISLAND_DEPTH,palette.soilLight,palette.soilDark,0,0f);
            if(!worldTerrain.canStand(column+1,row))
                drawSmoothIslandQuad(canvas,x,y+hh,x+hw,y,x+hw,y+ISLAND_DEPTH,x,y+hh+ISLAND_DEPTH,palette.soilDark,palette.soilDark,0,0f);
            if(material==1) drawGrassTile(canvas,x,y,(column+row)%2==0,column,row,palette,sunlight);
            else {
                int[] colors={0,0,0xffae8b68,0xff75cbd0,0xffb8b2a7,0xffdac695,0xff805e43};
                int fill=material==2 ? blend(palette.soilLight,0xffd49d65,0.4) : material==4 ? blend(colors[material],palette.soilDark,0.08) : blend(colors[material],palette.grassDark,0.12);
                boolean leftWater = material==3 && row+1<ISLAND_GRID_SIZE && worldTerrain.tile(column,row+1)==3;
                boolean rightWater = material==3 && column+1<ISLAND_GRID_SIZE && worldTerrain.tile(column+1,row)==3;
                if (material==3) {
                    // Flat portable worlds have a riverbed under translucent water.
                    drawSunlitIslandDiamond(canvas,x,y,hw,hh,0,
                            blend(palette.soilLight,0xffdac695,0.45),sunlight);
                    canvas.saveLayerAlpha(x-hw-2,y-hh-2,x+hw+2,y+hh+ISLAND_TILE_SIDE_DEPTH+2,163,Canvas.ALL_SAVE_FLAG);
                }
                if (!leftWater) drawGrassTileSide(canvas,x,y,hw,hh,material==3?0:1.4f,ISLAND_TILE_SIDE_DEPTH,fill,true);
                if (!rightWater) drawGrassTileSide(canvas,x,y,hw,hh,material==3?0:1.4f,ISLAND_TILE_SIDE_DEPTH,fill,false);
                drawSunlitIslandDiamond(canvas,x,y,material==3?hw:hw-0.05f,material==3?hh:hh-0.025f,material==3?0:1.4f,fill,sunlight);
                if (material==3) {
                    canvas.restore();
                    // Solid exposed soil walls frame the water; shared water edges stay open.
                    if (!leftWater) drawGrassTileSide(canvas,x,y,hw,hh,0,ISLAND_TILE_SIDE_DEPTH,palette.soilLight,true);
                    if (!rightWater) drawGrassTileSide(canvas,x,y,hw,hh,0,ISLAND_TILE_SIDE_DEPTH,palette.soilLight,false);
                    drawWaterTopBanks(canvas,x,y,hw,hh,new boolean[]{!waterAt(column,row-1),!rightWater,
                            !leftWater,!waterAt(column-1,row)},palette);
                    paint.setColor(0x80d8faf1); paint.setStrokeWidth(.55f); paint.setStyle(Paint.Style.STROKE);
                    for (float[] offset : new float[][]{{-.35f,-.2f},{.20f,.13f}}) {
                        float rx=x+hw*offset[0], ry=y+hh*offset[1];
                        canvas.drawLine(rx-hw*.17f,ry,rx-hw*.05f,ry-hh*.07f,paint);
                        canvas.drawLine(rx-hw*.05f,ry-hh*.07f,rx+hw*.06f,ry,paint);
                        canvas.drawLine(rx+hw*.06f,ry,rx+hw*.17f,ry-hh*.07f,paint);
                    }
                    paint.setStyle(Paint.Style.FILL);
                }
                if(material==4) {
                    float direction=(column+row)%2==0?1f:-1f;
                    float topHw=hw-.05f, topHh=hh-.025f;
                    paint.setColor(0x8042413e);paint.setStrokeWidth(.45f);paint.setStyle(Paint.Style.STROKE);
                    Path fissure=new Path();fissure.moveTo(x-topHw*.48f*direction,y-topHh*.02f);
                    fissure.lineTo(x-topHw*.30f*direction,y-topHh*.08f);
                    fissure.lineTo(x-topHw*.20f*direction,y+topHh*.09f);
                    fissure.lineTo(x-topHw*.04f*direction,y+topHh*.05f);canvas.drawPath(fissure,paint);
                    paint.setStyle(Paint.Style.FILL);paint.setColor(0xb3ece5d3);
                    float fx=x+topHw*.30f*direction,fy=y-topHh*.17f;
                    canvas.drawRect(fx,fy,fx+.85f,fy+.65f,paint);
                    paint.setColor(0x6657544f);canvas.drawRect(fx,fy+.65f,fx+.85f,fy+1f,paint);
                }
                if(material==6) {
                    paint.setColor(0xff523b2d);paint.setStrokeWidth(.75f);paint.setStyle(Paint.Style.STROKE);
                    for(float offset:new float[]{-.42f,0f,.42f}) canvas.drawLine(x-hw*(1-Math.abs(offset)),y+hh*offset,x+hw*(1-Math.abs(offset)),y+hh*offset,paint);
                    paint.setStyle(Paint.Style.FILL);
                }
                if(material==3) drawPondLife(canvas,x,y,row*ISLAND_GRID_SIZE+column);
            }
        }
    }

    /**
     * The island's cast shadow is pushed away from whichever body is lighting the scene. It
     * starts from that body's progress along the celestial arc, so the shadow drifts to the
     * opposite side as the sun or moon travels. Offset and size are kept small so the shadow
     * stays tucked close under the island instead of floating away from it.
     */
    private void drawIslandShadow(Canvas canvas, float centerX, float lowerY,
                                  double lightAltitude, double lightHourAngle,
                                  double cloudCover, int code, double lightDeclination) {
        // A fixed alpha is not enough on its own, and that is why the shadow vanished after dark:
        // alpha blends toward the shade colour, so it removes a fixed FRACTION of the backdrop. The
        // same 0.14 takes 25 levels off bright daytime ground but only 9 at night, where there is
        // barely any light left to take away. The silhouette keeps one colour, and the
        // alpha is solved for a target amount of contrast instead of being fixed, so the shade lands
        // with the same visible weight in every weather and at every hour.
        double floorLuminance = floorLuminance(cloudCover, code);
        double opacity = clamp(SHADOW_CONTRAST_TARGET
                / Math.max(floorLuminance - SHADOW_LUMINANCE, 1.0), 0.06, 0.45);
        // The island shadow stays centred beneath the island, independent of celestial motion.
        float shadowCenterX = centerX;
        float bottomTileY = lowerY - ISLAND_DEPTH - ISLAND_TILE_HEIGHT / 2f;
        float scale = ISLAND_BODY_SHADOW_SCALE * (float) CelestialArc.islandShadowScale(
                lightHourAngle, lightDeclination, latitude, ISLAND_DEPTH);
        float halfWidth = ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH / 4.0f * scale;
        float halfHeight = ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT / 4.0f * scale;
        float shadowCenterY = bottomTileY + 50f - halfHeight;
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

        int grass = blend(Color.rgb(56, 76, 67), Color.rgb(145, 173, 145), daylight);
        grass = blend(grass, Color.rgb(195, 157, 96), twilight * 0.25);
        grass = blend(grass, Color.rgb(120, 131, 111), weatherShade);
        boolean wet = (code >= 51 && code <= 67) || (code >= 80 && code <= 82);
        if (wet) grass = blend(grass, Color.rgb(98, 120, 109), 0.12);
        grass = blend(grass, atmosphere.tint, atmosphere.islandMix);

        int grassDark = blend(grass, Color.rgb(45, 73, 56), 0.24);
        double grassTileMix = (0.18 + daylight * 0.77) * (1.0 - weatherShade * 0.30);
        int grassTileSoftFern = blend(grass, Color.rgb(150, 179, 151), grassTileMix);
        int grassTileFernSage = blend(grass, Color.rgb(139, 167, 140), grassTileMix);
        int grassSeam = blend(grass, Color.rgb(49, 77, 55), 0.08);
        int grassWall = blend(grass, Color.rgb(88, 119, 97), 0.28);

        int soil = blend(Color.rgb(74, 64, 54), Color.rgb(182, 161, 131), daylight);
        soil = blend(soil, Color.rgb(190, 116, 45), twilight * 0.24);
        soil = blend(soil, Color.rgb(91, 72, 55), weatherShade * 0.45);
        soil = blend(soil, atmosphere.tint, atmosphere.islandMix * 0.22);
        int soilLight = blend(soil, Color.rgb(205, 187, 153), 0.20);
        int soilDark = blend(soil, Color.rgb(96, 78, 64), 0.50);
        if (worldIslandTint != null && worldIslandTint != Color.rgb(168, 199, 149)
                && worldIslandTint != Color.rgb(164, 207, 54)) {
            int tint = worldIslandTint;
            grass = blend(grass, tint, 0.55);
            grassDark = blend(grassDark, tint, 0.55);
            grassTileSoftFern = blend(grassTileSoftFern, tint, 0.55);
            grassTileFernSage = blend(grassTileFernSage, tint, 0.55);
            grassSeam = blend(grassSeam, tint, 0.55);
            grassWall = blend(grassWall, tint, 0.55);
        }
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
        // Match the preview's stable, transparent 320×160 detail variants.
        if (grassDetail != null) {
            int variant = GrassDetail.variant(column, row);
            if (variant == 0) return; // Empty tile variant keeps the meadow calm.
            grassDetailSource.set(variant * 320, 0, variant * 320 + 320, 160);
            grassDetailDestination.set(centerX - ISLAND_TILE_WIDTH / 2f, centerY - ISLAND_TILE_HEIGHT / 2f,
                    centerX + ISLAND_TILE_WIDTH / 2f, centerY + ISLAND_TILE_HEIGHT / 2f);
            grassDetailPaint.setAlpha((int)Math.round(255 * (0.45 + 0.55 * clamp(sunlight.strength * 2, 0, 1))));
            canvas.drawBitmap(grassDetail, grassDetailSource, grassDetailDestination, grassDetailPaint);
            grassDetailPaint.setAlpha(255);
        }
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
        // Same static, clipped soil flecks as the browser preview.
        canvas.save();
        canvas.clipPath(islandBodyPath);
        int previousColor = paint.getColor(), previousAlpha = paint.getAlpha();
        Paint.Style previousStyle = paint.getStyle();
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 32; i++) {
            float x = soilLeftX + (soilRightX - soilLeftX) * (i + 0.5f) / 32.0f;
            float t = x <= centerX ? (x - soilLeftX) / (centerX - soilLeftX)
                    : (soilRightX - x) / (soilRightX - centerX);
            float y = middleY + (soilFrontY - middleY) * t + grassBandDepth + 3 + (i * 7 % 9);
            paint.setColor(blend(palette.soilLight, palette.soilDark, i % 3 == 0 ? 0.25 : 0.8));
            paint.setAlpha(115);
            canvas.drawRect(x, y, x + 1.5f, y + 0.75f, paint);
            if (i % 4 == 0) canvas.drawRect(x + 0.5f, y + 0.75f, x + 1.25f, y + 1.25f, paint);
        }
        paint.setColor(previousColor);
        paint.setAlpha(previousAlpha);
        paint.setStyle(previousStyle);
        canvas.restore();
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
                ? blend(baseColor, Color.rgb(204, 222, 193), 0.10 * sunlight.strength)
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

    /**
     * Draws either sky frame: a 9 x 9 lattice hung above the plate. Every line is a tile seam, so it is
     * addressed in the projection's own (column, row) coordinates: integer values land on tile
     * centres, which means the seam between two tiles sits at a half value. The plate is the
     * diamond |a| + |b| = 9 with a = column - row and b = column + row - 8, which is why each seam
     * runs from -0.5 to 8.5 along the other axis.
     *
     * The rim reuses the plate silhouette, so the frame is the same shape as the island rather
     * than a separate diamond that merely lines up. Round joins matter: the rim's four vertices
     * turn by about 53 degrees and a miter join there shoots a spike outside the plate.
     */
    private void drawLangitFrame(Canvas canvas, float lift, int color, float offsetX, float offsetY) {
        float centerX = SCENE_WIDTH / 2.0f;
        float centerY = ISLAND_CENTER_Y;
        float halfWidth = ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH / 2.0f;
        float halfHeight = ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT / 2.0f;
        float line = LANGIT_LINE;
        float inset = LANGIT_INSET;
        int save = canvas.save();
        // Screen units, so the overlay clears the plate by a fixed distance.
        canvas.translate(offsetX, offsetY - lift);
        // Clip the lattice a whole unit inside the plate; see the inset constant.
        islandClipPath.reset();
        traceSmoothIslandDiamondPath(islandClipPath, centerX, centerY,
                halfWidth - inset, halfHeight - inset);
        canvas.clipPath(islandClipPath);

        Path grid = gridPath;
        grid.reset();
        float first = -0.5f;
        float last = ISLAND_GRID_SIZE - 0.5f;
        for (int edge = 1; edge < ISLAND_GRID_SIZE; edge++) {
            float seam = edge - 0.5f;
            grid.moveTo(tilePointX(seam, first), tilePointY(seam, first));
            grid.lineTo(tilePointX(seam, last), tilePointY(seam, last));
            grid.moveTo(tilePointX(first, seam), tilePointY(first, seam));
            grid.lineTo(tilePointX(last, seam), tilePointY(last, seam));
        }
        islandPolyPaint.setShader(null);
        islandPolyPaint.setStyle(Paint.Style.STROKE);
        islandPolyPaint.setStrokeWidth(line);
        islandPolyPaint.setStrokeCap(Paint.Cap.BUTT);
        islandPolyPaint.setStrokeJoin(Paint.Join.ROUND);
        islandPolyPaint.setColor(color);
        islandPolyPaint.setAlpha(Math.round(LANGIT_ALPHA * 255));
        canvas.drawPath(grid, islandPolyPaint);

        Path rim = gridPath;
        rim.reset();
        traceSmoothIslandDiamondPath(rim, centerX, centerY,
                halfWidth - inset - line / 2.0f, halfHeight - inset - line / 2.0f);
        islandPolyPaint.setAlpha(Math.round(LANGIT_EDGE_ALPHA * 255));
        canvas.drawPath(rim, islandPolyPaint);

        islandPolyPaint.setAlpha(255);
        islandPolyPaint.setStyle(Paint.Style.FILL);
        canvas.restoreToCount(save);
    }

    private float tilePointX(float column, float row) {
        return SCENE_WIDTH / 2.0f + (column - row) * ISLAND_TILE_WIDTH / 2.0f;
    }

    private float tilePointY(float column, float row) {
        return ISLAND_TOP + ISLAND_TILE_HEIGHT / 2.0f + (column + row) * ISLAND_TILE_HEIGHT / 2.0f;
    }

    /** The plate silhouette, as a path, so it can be stroked or clipped. */
    private void traceSmoothIslandDiamondPath(Path path, float centerX, float centerY,
            float halfWidth, float halfHeight) {
        float cornerRadius = 2.2f;
        float cutX = Math.min(cornerRadius, halfWidth * 0.34f);
        float cutY = cutX * halfHeight / halfWidth;
        path.moveTo(centerX - cutX, centerY - halfHeight + cutY);
        path.quadTo(centerX, centerY - halfHeight, centerX + cutX, centerY - halfHeight + cutY);
        path.lineTo(centerX + halfWidth - cutX, centerY - cutY);
        path.quadTo(centerX + halfWidth, centerY, centerX + halfWidth - cutX, centerY + cutY);
        path.lineTo(centerX + cutX, centerY + halfHeight - cutY);
        path.quadTo(centerX, centerY + halfHeight, centerX - cutX, centerY + halfHeight - cutY);
        path.lineTo(centerX - halfWidth + cutX, centerY + cutY);
        path.quadTo(centerX - halfWidth, centerY, centerX - halfWidth + cutX, centerY - cutY);
        path.close();
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
                                   float strength, float size) {
        if (visibility <= 0.0f || strength <= 0.0f) return;
        double high = clamp(clamp(altitude, 0.0, 90.0) / 45.0, 0.0, 1.0);
        float squash = (float) (0.52 + high * 0.48);
        double breathe = 0.5 + 0.5 * Math.sin(elapsedSeconds * GLOW_PULSE_SPEED);
        float reach = (float) (GLOW_REACH * celestialScale() * size * (1.15 - high * 0.45) * (0.82 + breathe * 0.36));
        double alpha = GLOW_ALPHA * (0.72 + breathe * 0.42) * visibility * strength;
        int color = (tint & 0x00FFFFFF) | ((int) Math.round(255.0 * alpha) << 24);
        for (int step = 0; step < GLOW_STEPS; step++) {
            float progress = step / (float) (GLOW_STEPS - 1);
            glowDisc(canvas, centerX, centerY, celestialRadius() * size + celestialScale() * size + progress * reach,
                    squash, color);
        }
    }

    private void drawSunBody(Canvas canvas, float centerX, float centerY, int[] palette) {
        float radius = celestialRadius();
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
        if (sun.altitudeDegrees <= 0.0) return;
        int[] palette = sunPalette(sun.altitudeDegrees, cloudCover);
        float[] position = celestialScreenPosition(sun);
        float centerX = Math.round(position[0] / RENDER_CELL) * RENDER_CELL;
        float centerY = Math.round(position[1] / RENDER_CELL) * RENDER_CELL;
        int save = canvas.save();
        paint.setAlpha((int) Math.round(255.0
                * clamp(sun.altitudeDegrees / 2.0, 0.0, 1.0)));
        drawCelestialGlow(canvas, centerX, centerY, sun.altitudeDegrees, elapsedSeconds,
                blend(palette[0], Color.rgb(255, 217, 138), 0.55),
                (float) clamp(sun.altitudeDegrees / 2.0, 0.0, 1.0), 1.0f, 1.0f);
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
        if (sunAltitude >= 0.0 || moon.altitudeDegrees <= 0.0) return;
        int[] palette = moonPalette();
        float[] position = celestialScreenPosition(moon);
        float centerX = Math.round(position[0] / RENDER_CELL) * RENDER_CELL;
        float centerY = Math.round(position[1] / RENDER_CELL) * RENDER_CELL;
        float visibility = (float) (clamp(moon.altitudeDegrees / 2.0, 0.0, 1.0));
        int save = canvas.save();
        paint.setAlpha((int) Math.round(255.0 * visibility));
        drawCelestialGlow(canvas, centerX, centerY, moon.altitudeDegrees, elapsedSeconds,
                blend(palette[0], Color.rgb(207, 224, 255), 0.45), visibility,
                (float) (0.35 + clamp(moon.illumination, 0.0, 1.0) * 0.55), 0.5f);
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
        float radius = CelestialArc.moonRadius(celestialRadius());
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

    private int cloudCountForCover(double cloudCover, int weatherCode) {
        return CloudLayout.count(cloudCover, weatherCode);
    }

    private static double cloudHash(int seed, int index, int salt) {
        int value = seed ^ ((index + 1) * 0x9e3779b9) ^ ((salt + 1) * 0x85ebca6b);
        value = (value ^ (value >>> 16)) * 0x7feb352d;
        value = (value ^ (value >>> 15)) * 0x846ca68b;
        value ^= value >>> 16;
        return Integer.toUnsignedLong(value) / 4294967296.0;
    }

    private float[] celestialViewPoint(float x,float y) {
        float anchor=SCENE_HEIGHT-ISLAND_BOTTOM_MARGIN;
        float referenceY=ISLAND_GRID_SIZE==25 ? SCENE_HEIGHT*.56f : anchor+(ISLAND_CENTER_Y-anchor)*islandScaleY()*2;
        float islandX=mapDestinationX()+(SCENE_WIDTH/2f-mapSourceX())*islandScaleX()*mapZoom();
        float islandY=mapDestinationY()+(ISLAND_CENTER_Y-mapSourceY())*islandScaleY()*mapZoom();
        return new float[]{islandX+(x-SCENE_WIDTH/2f)*celestialScale(),islandY+(y-referenceY)*celestialScale()};
    }

    private float[] celestialScreenPosition(SkyMath.Position body) {
        float[] offset=CelestialArc.islandOffset(body.hourAngleDegrees,body.declinationDegrees,latitude,300f,175f);
        float x=SCENE_WIDTH/2f+offset[0], y=ISLAND_CENTER_Y+offset[1];
        return new float[]{mapDestinationX()+(x-mapSourceX())*islandScaleX()*mapZoom(),
                mapDestinationY()+(y-mapSourceY())*islandScaleY()*mapZoom()};
    }

    // The Sun stays on Langit Matahari. The same continuous orbit is used as a virtual
    // light at night, although the visible Sun is suppressed below the horizon.
    private float[] sunPlanePosition(double hourAngle) {
        double a = Math.toRadians(hourAngle);
        double dx = -40.0 * Math.sin(a);
        double dy = 15.0 * (1.0 - Math.cos(a));
        double len = Math.hypot(ISLAND_TILE_WIDTH, ISLAND_TILE_HEIGHT);
        return new float[] {
            (float) (SCENE_WIDTH / 2.0 - LANGIT_MATAHARI_WEST_SHIFT * ISLAND_TILE_WIDTH / len
                    + dx),
            (float) (ISLAND_CENTER_Y - LANGIT_MATAHARI_LIFT
                    - LANGIT_MATAHARI_WEST_SHIFT * ISLAND_TILE_HEIGHT / len
                    + dy)
        };
    }

    // Illustrative 1 km cloud base with 250 metres per 28-unit tile.
    private float cloudDisplayLift() { return (float)(1000.0 / ISLAND_TILE_METERS * ISLAND_TILE_WIDTH); }

    private CloudState[] createCloudStates(double elapsedSeconds, double cloudCover,
            double windSpeed, double windDirection, double sunAltitude, double sunHourAngle, int weatherCode) {
        double bearing = normalizeDegrees(windDirection + 180.0);
        if (windSpeed > 0.0) {
            double difference = Double.isNaN(cloudWindCardinal) ? 180.0
                    : Math.abs((bearing - cloudWindCardinal + 540.0) % 360.0 - 180.0);
            if (difference > 50.0) {
                cloudWindCardinal = normalizeDegrees(Math.round(bearing / 90.0) * 90.0);
            }
            double delta = Double.isNaN(cloudLastElapsed) ? 0.0
                    : Math.max(0.0, elapsedSeconds - cloudLastElapsed);
            double axisLength = Math.hypot(ISLAND_TILE_WIDTH, ISLAND_TILE_HEIGHT) / 2.0;
            double distance = Math.min(windSpeed * CLOUD_DRIFT_WIND_GAIN, CLOUD_DRIFT_MAX)
                    * delta / axisLength;
            cloudEastTravel += Math.sin(Math.toRadians(cloudWindCardinal)) * distance;
            cloudNorthTravel += Math.cos(Math.toRadians(cloudWindCardinal)) * distance;
        }
        cloudLastElapsed = elapsedSeconds;
        int count = cloudCountForCover(cloudCover, weatherCode);
        int seed = (int) (latitude * 37 + longitude * 17) ^ (count * 0x2545f491);
        CloudState[] clouds = new CloudState[count];
        double len = Math.hypot(ISLAND_TILE_WIDTH, ISLAND_TILE_HEIGHT);
        float lift=cloudDisplayLift(), sx=islandScaleX()*mapZoom(), sy=islandScaleY()*mapZoom();
        int cols=(int)Math.ceil(Math.sqrt(count)),rows=(int)Math.ceil(count/(double)cols);
        for (int index = 0; index < count; index++) {
            int variant = (int) (cloudHash(seed, index, 1) * CLOUD_SHAPE_VARIANTS.length);
            String[] shape = CLOUD_SHAPE_VARIANTS[variant];
            float cell=CloudLayout.cellSize(ISLAND_TILE_WIDTH,ISLAND_TILE_HEIGHT,shape[0].length(),shape.length);
            double eastSeed=((index%cols)+.2+.6*cloudHash(seed,index,2))/cols*ISLAND_GRID_SIZE;
            double northSeed=(Math.floor(index/(double)cols)+.2+.6*cloudHash(seed,index,3))/rows*ISLAND_GRID_SIZE;
            double east = ((eastSeed + cloudEastTravel) % ISLAND_GRID_SIZE + ISLAND_GRID_SIZE) % ISLAND_GRID_SIZE - ISLAND_GRID_SIZE / 2.0;
            double north = ((northSeed + cloudNorthTravel) % ISLAND_GRID_SIZE + ISLAND_GRID_SIZE) % ISLAND_GRID_SIZE - ISLAND_GRID_SIZE / 2.0;
            float x = (float) (SCENE_WIDTH / 2.0 - LANGIT_AWAN_WEST_SHIFT * ISLAND_TILE_WIDTH / len
                    + (east + north) * ISLAND_TILE_WIDTH / 2.0);
            float y = (float) (ISLAND_CENTER_Y - LANGIT_AWAN_LIFT
                    - LANGIT_AWAN_WEST_SHIFT * ISLAND_TILE_HEIGHT / len
                    + (east - north) * ISLAND_TILE_HEIGHT / 2.0);
            float displayY=y-(lift-LANGIT_AWAN_LIFT)-shape.length*cell/2f;
            float screenX=mapDestinationX()+(x-mapSourceX())*sx;
            float screenY=mapDestinationY()+(displayY-mapSourceY())*sy;
            clouds[index] = new CloudState(shape, cell, x, y, displayY, screenX, screenY, sy);
        }
        return clouds;
    }

    /** Three flat bands keyed to the sun's height, matching the Sun and Moon's own light model. */
    private int[] cloudPalette(double sunAltitude, int code) {
        if (code >= 95 && code <= 99) return CLOUD_STORM_BANDS;
        if (sunAltitude < -6.0) return CLOUD_NIGHT_BANDS;
        if (sunAltitude < 10.0) return CLOUD_DUSK_BANDS;
        return CLOUD_DAY_BANDS;
    }

    /**
     * One rectangle per sprite cell keeps every edge on the art grid, which is what holds the
     * cloud to the same pixel language as the rest of the scene at this size.
     */
    private void drawCloudRows(Canvas canvas, CloudState cloud, int[] palette, int layerAlpha) {
        String[] shape = cloud.shape;
        float cell = cloud.cell;
        float left = cloud.planeX - shape[0].length() * cell * 0.5f;
        float top = cloud.displayY - shape.length * cell * .5f;
        for (int row = 0; row < shape.length; row += 1) {
            String line = shape[row];
            float rowTop = snapToArtGrid(top + row * cell);
            float rowBottom = snapToArtGrid(top + (row + 1) * cell);
            for (int column = 0; column < line.length();) {
                char band = line.charAt(column); int end=column+1;
                while(end<line.length() && line.charAt(end)==band) end++;
                if (band != '0') {
                    paint.setColor(palette[band - '1']);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setAlpha(layerAlpha);
                    float runLeft = snapToArtGrid(left + column * cell);
                    float runRight = snapToArtGrid(left + end * cell);
                    canvas.drawRect(runLeft, rowTop, runRight, rowBottom, paint);
                }
                column=end;
            }
        }
    }

    /** The shape a cloud has to cross to be covering part of the island's top surface. */
    private boolean cloudOverlapsIsland(CloudState cloud) {
        float width = cloud.shape[0].length() * cloud.cell;
        float height = cloud.shape.length * cloud.cell;
        float left = cloud.screenCenterX - width * 0.5f;
        float right = cloud.screenCenterX + width * 0.5f;
        float top = cloud.screenBottomY - height;
        float bottom = cloud.screenBottomY;
        float halfWidth = ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH * 0.5f;
        float halfHeight = ISLAND_GRID_SIZE * ISLAND_TILE_HEIGHT * 0.5f;
        float dx = (left + right) * 0.5f - SCENE_WIDTH / 2.0f;
        float dy = (top + bottom) * 0.5f - ISLAND_CENTER_Y;
        return Math.abs(dx) / (halfWidth + width * 0.5f)
                + Math.abs(dy) / (halfHeight + height * 0.5f) <= 1.0f;
    }

    private void drawClouds(Canvas canvas, CloudState[] clouds, double sunAltitude, int code) {
        if (clouds.length == 0) return;
        int[] palette = cloudPalette(sunAltitude, code);
        int save=canvas.save(); applyIslandScale(canvas);
        for (CloudState cloud : clouds) {
            float halfWidth=cloud.shape[0].length()*cloud.cell/2f;
            if (canvas.quickReject(cloud.planeX-halfWidth, cloud.displayY-cloud.shape.length*cloud.cell/2f,
                    cloud.planeX+halfWidth, cloud.displayY+cloud.shape.length*cloud.cell/2f, Canvas.EdgeType.AA)) continue;
            // A cloud crossing the island drops to partial opacity, so whatever sits on the tiles
            // is not hidden behind it.
            int previousAlpha = paint.getAlpha();
            // Every cloud carries the same opacity, whether or not it is crossing the island.
            // Dimming only the ones over the plate made a cloud visibly change weight as it drifted
            // on, which read as the cloud flickering rather than as depth.
            int layerAlpha = (int) (255 * CLOUD_ISLAND_ALPHA);
            drawCloudRows(canvas, cloud, palette, layerAlpha);
            paint.setAlpha(previousAlpha);
        }
        canvas.restoreToCount(save);
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
        final float planeX, planeY, displayY, screenCenterX, screenBottomY, screenHeight;
        CloudState(String[] shape, float cell, float planeX, float planeY, float displayY, float screenX, float screenY, float scaleY) {
            this.shape=shape;this.cell=cell;this.planeX=planeX;this.planeY=planeY;this.displayY=displayY;
            this.screenCenterX=screenX;this.screenHeight=shape.length*cell*scaleY;
            this.screenBottomY=screenY+screenHeight/2;
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
