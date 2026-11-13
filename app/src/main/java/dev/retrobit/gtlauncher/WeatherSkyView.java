package dev.retrobit.gtlauncher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.graphics.Shader;
import android.util.AttributeSet;
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
    private static final float GLOW_REACH = 9.5f;
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
    private static final float ISLAND_CENTER_Y = ISLAND_TOP + ISLAND_TILE_HEIGHT / 2.0f
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
    // this gain is about four times faster, which puts the implied cloud layer at 283 m. The trade
    // is deliberate and the honest figure is the 0.0118 above.
    private static final double CLOUD_DRIFT_WIND_GAIN = 0.05;
    private static final double CLOUD_DRIFT_MAX = 4.2;
    private static final float CLOUD_EDGE_INSET = 4.0f;
    // The shadow is a shrunk copy of the cloud, not a same-size one: at full size it covered
    // most of the island and read as a hole rather than as shade.
    private static final float CLOUD_SHADOW_SCALE = 0.78f;
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
    private Bitmap mainCharacter, mainCharacterBack, houseImage, houseBackImage, portalImage, portalMarkerImage;
    private Bitmap bushSpiky,bushTiered,wildGrass,treeMango,treeBanana,treeCoconut,sunflowerFront,sunflowerBack,sellingBoxClosed,sellingBoxOpen,mailboxBirdhouse,storageClosed,storageOpen,storageBackClosed,storageBackOpen;
    private final java.util.Set<String> openStorage = new java.util.HashSet<>();
    private final java.util.Map<String,Long> sellingOpenedAt = new java.util.HashMap<>();
    private String surfaceWorldId="";
    private java.util.List<PortalTravel.Portal> worldPortals = java.util.Collections.emptyList();
    private boolean portalWalkActive;
    private java.util.List<WorldStore.SurfaceObject> worldSurfaces = java.util.Collections.emptyList();
    private boolean showHouse;
    private int houseColumn = 0, houseRow = 0;
    private float houseScale = 1.0f;
    private String houseFacing = "S";
    private String characterFacing = "S";
    private boolean showCharacter;
    private int characterColumn = 4, characterRow = 4;
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
        mainCharacter = BitmapFactory.decodeResource(getResources(), R.drawable.main_character);
        mainCharacterBack = BitmapFactory.decodeResource(getResources(), R.drawable.main_character_back);
        houseImage = BitmapFactory.decodeResource(getResources(), R.drawable.house);
        houseBackImage = BitmapFactory.decodeResource(getResources(), R.drawable.house_back);
        portalImage = BitmapFactory.decodeResource(getResources(), R.drawable.portal_gate);
        portalMarkerImage = BitmapFactory.decodeResource(getResources(), R.drawable.portal_exclamation_red);
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
        showCharacter = visible;
        characterFacing = facing;
        characterColumn = Math.max(0, Math.min(8, column));
        characterRow = Math.max(0, Math.min(8, row));
        characterScale = Math.max(0.5f, Math.min(1.5f, scale));
        invalidate();
    }

    public void setWorldHouse(boolean visible, int column, int row, float scale, String facing) {
        showHouse = visible; houseColumn = Math.max(0, Math.min(6, column));
        houseRow = Math.max(0, Math.min(6, row)); houseScale = Math.max(0.5f, Math.min(1.5f, scale));
        houseFacing = facing; invalidate();
    }

    void setWorldSurfaces(String worldId,java.util.List<WorldStore.SurfaceObject> objects) {
        String id=worldId==null?"":worldId;if(!surfaceWorldId.equals(id)){openStorage.clear();sellingOpenedAt.clear();surfaceWorldId=id;}
        worldSurfaces = objects == null ? java.util.Collections.emptyList() : new java.util.ArrayList<>(objects); invalidate();
    }
    private RectF surfaceSpriteBounds(WorldStore.SurfaceObject o) {
        float x=SCENE_WIDTH/2f+(o.column-o.row)*ISLAND_TILE_WIDTH/2f;
        float y=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(o.column+o.row)*ISLAND_TILE_HEIGHT/2f;
        float width,height,bottom;
        switch(o.type) {
            case "tree": boolean small=!o.material.equals("mango");width=small?24:48;height=small?30:60;bottom=y+ISLAND_TILE_HEIGHT*(small?.275f:1.05f);break;
            case "sunflower": width=15;height=23;bottom=y+ISLAND_TILE_HEIGHT*.325f;break;
            case "bush": width=o.material.equals("wild-grass")?10f:18.5f;height=o.material.equals("wild-grass")?8f:14.5f;bottom=y+ISLAND_TILE_HEIGHT*.325f;break;
            case "mailbox": width=13;height=30;bottom=y+ISLAND_TILE_HEIGHT*.15f;break;
            default: width=18;height=18;bottom=y+ISLAND_TILE_HEIGHT*.15f;
        }
        return new RectF(x-width/2,bottom-height,x+width/2,bottom);
    }
    private void drawSurfaceSprites(Canvas canvas,boolean front) {
        for(WorldStore.SurfaceObject o:worldSurfaces) {
            if(o.type.equals("road") || o.type.equals("fence")) continue;
            float depth=o.column+o.row+(o.type.equals("tree") && o.material.equals("mango")?1f:0f);
            if(showCharacter && (depth>=characterColumn+characterRow)!=front) continue;
            if(!showCharacter && front) continue;
            Bitmap image; boolean mirror=false;
            if(o.type.equals("tree")) image=o.material.equals("banana")?treeBanana:o.material.equals("palm")?treeCoconut:treeMango;
            else if(o.type.equals("sunflower")) { image=(o.material.equals("N")||o.material.equals("W"))?sunflowerBack:sunflowerFront;mirror=o.material.equals("N")||o.material.equals("E"); }
            else if(o.type.equals("bush")) image=o.material.equals("wild-grass")?wildGrass:o.material.equals("spiky")?bushSpiky:bushTiered;
            else if(o.type.equals("mailbox")) image=mailboxBirdhouse;
            else {boolean back=o.material.equals("N")||o.material.equals("W"),opened=openStorage.contains(o.column+","+o.row);
                mirror=o.material.equals("N")||o.material.equals("E");
                image=o.type.equals("selling-box")?(opened?sellingBoxOpen:sellingBoxClosed):back?(opened?storageBackOpen:storageBackClosed):(opened?storageOpen:storageClosed);
            }
            if(image==null)continue;RectF g=surfaceSpriteBounds(o);
            paint.setColor(Color.argb(35,12,23,40));paint.setStyle(Paint.Style.FILL);canvas.drawOval(new RectF(g.centerX()-g.width()*.3f,g.bottom-1,g.centerX()+g.width()*.3f,g.bottom+1),paint);
            paint.setColor(Color.WHITE);paint.setAlpha(255);paint.setFilterBitmap(false);int saved=canvas.save();
            if(animationEnabled && weather!=null && weather.windSpeedKmh>=1 && (o.type.equals("sunflower") || o.type.equals("tree") && !o.material.equals("mango"))) {
                double bearing=Math.toRadians(weather.windDirectionDegrees+180-headingDegrees);
                double direction=Math.sin(bearing)+.25*Math.cos(bearing),strength=Math.min(1,.25+weather.windSpeedKmh/35);
                double phase=o.column*1.73+o.row*2.31,frequency=o.type.equals("tree")?.6:.9,amplitude=o.type.equals("tree")?1.1:.65;
                double sway=amplitude*strength*direction*(.18+.82*Math.sin(plantMotionSeconds*frequency+phase));
                canvas.rotate((float)Math.toDegrees(Math.atan2(sway,g.height())),g.centerX(),g.bottom);
            }
            if(mirror)canvas.scale(-1,1,g.centerX(),0);
            Long started=sellingOpenedAt.get(o.column+","+o.row);
            if(o.type.equals("selling-box") && started!=null && animationEnabled && sellingBoxClosed!=null && sellingBoxOpen!=null) {
                float t=Math.max(0,Math.min(1,(android.os.SystemClock.uptimeMillis()-started)/260f));
                paint.setAlpha((int)(255*(1-t)));canvas.drawBitmap(sellingBoxClosed,null,g,paint);
                paint.setAlpha((int)(255*t));canvas.drawBitmap(sellingBoxOpen,null,g,paint);paint.setAlpha(255);
                if(t>=1)sellingOpenedAt.remove(o.column+","+o.row);
            } else canvas.drawBitmap(image,null,g,paint);
            canvas.restoreToCount(saved);
        }
    }
    private RectF sellingMarkerBounds(WorldStore.SurfaceObject o) {
        RectF box=surfaceSpriteBounds(o);float x=Math.max(0,Math.min(SCENE_WIDTH-11,box.right-5)),y=Math.max(0,Math.min(SCENE_HEIGHT-13,box.top-9));return new RectF(x,y,x+11,y+13);
    }
    private boolean sellingMarkerVisible(WorldStore.SurfaceObject o) {
        return o.type.equals("selling-box") && !portalWalkActive && showCharacter && Math.abs(characterColumn-o.column)+Math.abs(characterRow-o.row)==1 && !openStorage.contains(o.column+","+o.row);
    }
    private void drawSellingMarkers(Canvas canvas) {
        if(portalMarkerImage==null)return;paint.setAlpha(255);paint.setFilterBitmap(false);
        for(WorldStore.SurfaceObject o:worldSurfaces)if(sellingMarkerVisible(o)) {
            RectF g=sellingMarkerBounds(o);paint.setColor(Color.rgb(110,67,43));canvas.drawRect(g.left,g.top,g.right,g.bottom-2,paint);canvas.drawRect(g.left+2,g.bottom-2,g.left+6,g.bottom,paint);
            paint.setColor(Color.rgb(255,241,206));canvas.drawRect(g.left+1,g.top+1,g.right-1,g.bottom-3,paint);canvas.drawRect(g.left+3,g.bottom-3,g.left+5,g.bottom-1,paint);
            paint.setColor(Color.WHITE);canvas.drawBitmap(portalMarkerImage,null,new RectF(g.left+2.5f,g.top+1,g.right-2.5f,g.bottom-3),paint);
        }
    }
    WorldStore.SurfaceObject sellingMarkerAt(float sx,float sy) {
        if(getWidth()<=0 || getHeight()<=0)return null;float x=sx*SCENE_WIDTH/getWidth(),y=sy*SCENE_HEIGHT/getHeight();
        for(int i=worldSurfaces.size()-1;i>=0;i--){WorldStore.SurfaceObject o=worldSurfaces.get(i);RectF hit=sellingMarkerBounds(o);hit.inset(-3,-3);if(sellingMarkerVisible(o) && hit.contains(x,y))return o;}return null;
    }
    void setSellingOpen(WorldStore.SurfaceObject o,boolean opened) {if(o==null)return;String key=o.column+","+o.row;if(opened){openStorage.add(key);sellingOpenedAt.put(key,android.os.SystemClock.uptimeMillis());}else{openStorage.remove(key);sellingOpenedAt.remove(key);}invalidate();}
    boolean toggleStorageAt(float sx,float sy) {
        if(getWidth()<=0 || getHeight()<=0)return false;float x=sx*SCENE_WIDTH/getWidth(),y=sy*SCENE_HEIGHT/getHeight();
        for(int i=worldSurfaces.size()-1;i>=0;i--){WorldStore.SurfaceObject o=worldSurfaces.get(i);if(!o.type.equals("storage-box"))continue;
            if(surfaceSpriteBounds(o).contains(x,y)){String key=o.column+","+o.row;if(!openStorage.add(key))openStorage.remove(key);invalidate();return true;}}
        return false;
    }
    private void drawRoads(Canvas canvas) {
        for(WorldStore.SurfaceObject o:worldSurfaces) if(o.type.equals("road")) {
            float x=SCENE_WIDTH/2f+(o.column-o.row)*ISLAND_TILE_WIDTH/2f;
            float y=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(o.column+o.row)*ISLAND_TILE_HEIGHT/2f;
            float hw=ISLAND_TILE_WIDTH*.47f,hh=ISLAND_TILE_HEIGHT*.47f;
            Path path=new Path();path.moveTo(x,y-hh);path.lineTo(x+hw,y);path.lineTo(x,y+hh);path.lineTo(x-hw,y);path.close();
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.parseColor("water".equals(o.material)?"#719fa1":"#dba34b"));paint.setAlpha(255);canvas.drawPath(path,paint);
            paint.setColor(Color.parseColor("water".equals(o.material)?"#c0d8cc":"#efbe62"));canvas.drawRect(x-3,y-1,x+1,y-.5f,paint);
        }
    }
    private void drawFences(Canvas canvas,boolean front) {
        for(WorldStore.SurfaceObject o:worldSurfaces) if(o.type.equals("fence")) {
            float depth=o.column+o.row+((o.edge.equals("S")||o.edge.equals("E"))?.5f:-.5f);
            if(showCharacter && (depth>=characterColumn+characterRow)!=front) continue;
            if(!showCharacter && front) continue;
            float x=SCENE_WIDTH/2f+(o.column-o.row)*ISLAND_TILE_WIDTH/2f;
            float y=ISLAND_TOP+ISLAND_TILE_HEIGHT/2f+(o.column+o.row)*ISLAND_TILE_HEIGHT/2f;
            float hw=ISLAND_TILE_WIDTH*.4f,hh=ISLAND_TILE_HEIGHT*.4f,x1,y1,x2,y2;
            switch(o.edge){case "N":x1=x;y1=y-hh;x2=x+hw;y2=y;break;case "E":x1=x;y1=y+hh;x2=x+hw;y2=y;break;case "S":x1=x-hw;y1=y;x2=x;y2=y+hh;break;default:x1=x-hw;y1=y;x2=x;y2=y-hh;}
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2f);paint.setColor(Color.rgb(92,60,35));paint.setAlpha(255);
            canvas.drawLine(x1,y1-8,x2,y2-8,paint);canvas.drawLine(x1,y1-4,x2,y2-4,paint);
            paint.setStrokeWidth(2.5f);paint.setColor(Color.rgb(128,83,42));canvas.drawLine(x1,y1,x1,y1-11,paint);canvas.drawLine(x2,y2,x2,y2-11,paint);
            paint.setStyle(Paint.Style.FILL);
        }
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
    private void drawPortals(Canvas canvas, boolean front) {
        if(portalImage==null) return;
        for(PortalTravel.Portal portal:worldPortals) {
            float depth=portal.tile.column+portal.tile.row + ((portal.edge.equals("S") || portal.edge.equals("E")) ? .5f : -.5f);
            float playerDepth=characterColumn+characterRow;
            if(showCharacter && (depth>=playerDepth)!=front) continue;
            if(!showCharacter && front) continue;
            int save=canvas.save(); canvas.concat(portalMatrix(portal));
            if(portal.edge.equals("E") || portal.edge.equals("W")) { canvas.translate(64,0);canvas.scale(-1,1); }
            paint.setColor(Color.WHITE);paint.setAlpha(255);paint.setFilterBitmap(false);canvas.drawBitmap(portalImage,0,0,paint);canvas.restoreToCount(save);
        }
    }
    private RectF portalMarkerBounds(PortalTravel.Portal portal) {
        boolean rising=portal.edge.equals("E") || portal.edge.equals("W");
        float[] post={rising?46:44,rising?44:56};portalMatrix(portal).mapPoints(post);
        float width=11f,height=13f;
        float x=Math.max(0,Math.min(SCENE_WIDTH-width,post[0]-width*.45f));
        float y=Math.max(0,Math.min(SCENE_HEIGHT-height,post[1]-22.5f-height+3f));
        return new RectF(x,y,x+width,y+height);
    }
    void setPortalWalkActive(boolean walking) { portalWalkActive=walking;invalidate(); }
    boolean portalMarkerVisible(PortalTravel.Portal portal) {
        return !portalWalkActive && showCharacter && characterColumn==portal.tile.column && characterRow==portal.tile.row && portal.edge.equals(characterFacing);
    }
    private void drawPortalMarkers(Canvas canvas) {
        if(portalMarkerImage==null)return;
        paint.setColor(Color.WHITE);paint.setAlpha(255);paint.setFilterBitmap(false);
        for(PortalTravel.Portal portal:worldPortals) if(portalMarkerVisible(portal)) {
            RectF g=portalMarkerBounds(portal);
            paint.setColor(Color.rgb(110,67,43));canvas.drawRect(g.left,g.top,g.right,g.bottom-2,paint);canvas.drawRect(g.left+2,g.bottom-2,g.left+6,g.bottom,paint);
            paint.setColor(Color.rgb(255,241,206));canvas.drawRect(g.left+1,g.top+1,g.right-1,g.bottom-3,paint);canvas.drawRect(g.left+3,g.bottom-3,g.left+5,g.bottom-1,paint);
            paint.setColor(Color.WHITE);canvas.drawBitmap(portalMarkerImage,null,new RectF(g.left+2.5f,g.top+1,g.right-2.5f,g.bottom-3),paint);
        }
    }
    String portalMarkerAt(float screenX,float screenY) {
        if(getWidth()<=0 || getHeight()<=0) return null;
        float x=screenX*SCENE_WIDTH/getWidth(),y=screenY*SCENE_HEIGHT/getHeight();
        for(int i=worldPortals.size()-1;i>=0;i--){PortalTravel.Portal portal=worldPortals.get(i);
            RectF hit=portalMarkerBounds(portal);hit.inset(-3,-3);
            if(portalMarkerVisible(portal) && hit.contains(x,y)) return portal.id;
        }
        return null;
    }
    String portalAt(float screenX,float screenY) {
        if(getWidth()<=0 || getHeight()<=0) return null;
        float[] xy={screenX*SCENE_WIDTH/getWidth(),screenY*SCENE_HEIGHT/getHeight()};
        for(int i=worldPortals.size()-1;i>=0;i--){PortalTravel.Portal portal=worldPortals.get(i);Matrix inverse=new Matrix();
            if(!portalMatrix(portal).invert(inverse)) continue;float[] p=xy.clone();inverse.mapPoints(p);
            if(p[0]>=0 && p[0]<=64 && p[1]>=0 && p[1]<=64) return portal.id;
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
        paint.setFilterBitmap(false);
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
        Bitmap houseSprite = ("N".equals(houseFacing) || "W".equals(houseFacing)) ? houseBackImage : houseImage;
        if (showHouse && houseSprite != null) {
            float x = SCENE_WIDTH / 2.0f + (houseColumn - houseRow) * ISLAND_TILE_WIDTH / 2.0f;
            float y = ISLAND_TOP + ISLAND_TILE_HEIGHT / 2.0f + (houseColumn + houseRow + 2) * ISLAND_TILE_HEIGHT / 2.0f;
            drawObjectSilhouette(canvas, houseSprite, x, y, 70 * houseScale, 67 * houseScale, lightPoint,
                    "N".equals(houseFacing) || "E".equals(houseFacing));
        }
        Bitmap sprite = ("N".equals(characterFacing) || "W".equals(characterFacing)) ? mainCharacterBack : mainCharacter;
        if (showCharacter && sprite != null) {
            float x = SCENE_WIDTH / 2.0f + (characterColumn - characterRow) * ISLAND_TILE_WIDTH / 2.0f;
            float y = ISLAND_TOP + ISLAND_TILE_HEIGHT / 2.0f + (characterColumn + characterRow) * ISLAND_TILE_HEIGHT / 2.0f;
            drawObjectSilhouette(canvas, sprite, x, y, 13.5f * characterScale, 25 * characterScale, lightPoint,
                    "N".equals(characterFacing) || "E".equals(characterFacing));
        }
        // Faint contact diamonds stay attached to the ground anchors as objects move or scale.
        paint.setColor(Color.argb((int) (255 * 0.09), 12, 23, 40));
        if (showHouse && houseSprite != null) {
            float x = SCENE_WIDTH / 2.0f + (houseColumn - houseRow) * ISLAND_TILE_WIDTH / 2.0f;
            float y = ISLAND_TOP + ISLAND_TILE_HEIGHT / 2.0f + (houseColumn + houseRow + 2) * ISLAND_TILE_HEIGHT / 2.0f;
            drawObjectContact(canvas, x, y, 70 * houseScale, ISLAND_TILE_HEIGHT * 2.5f * houseScale);
        }
        if (showCharacter && sprite != null) {
            float x = SCENE_WIDTH / 2.0f + (characterColumn - characterRow) * ISLAND_TILE_WIDTH / 2.0f;
            float y = ISLAND_TOP + ISLAND_TILE_HEIGHT / 2.0f + (characterColumn + characterRow) * ISLAND_TILE_HEIGHT / 2.0f;
            drawObjectContact(canvas, x, y, 13.5f * characterScale, 5 * characterScale);
        }
        canvas.restoreToCount(save);
    }

    private void drawObjectContact(Canvas canvas, float x, float y, float width, float depth) {
        Path path = new Path();
        path.moveTo(x, y - depth / 2); path.lineTo(x + width / 2, y);
        path.lineTo(x, y + depth / 2); path.lineTo(x - width / 2, y);
        path.close(); canvas.drawPath(path, paint);
    }

    private void drawHouse(Canvas canvas) {
        boolean back = "N".equals(houseFacing) || "W".equals(houseFacing);
        boolean mirror = "N".equals(houseFacing) || "E".equals(houseFacing);
        Bitmap sprite = back ? houseBackImage : houseImage;
        if (!showHouse || sprite == null) return;
        float x = SCENE_WIDTH / 2.0f + (houseColumn - houseRow) * ISLAND_TILE_WIDTH / 2.0f;
        float bottom = ISLAND_TOP + ISLAND_TILE_HEIGHT / 2.0f + (houseColumn + houseRow + 2) * ISLAND_TILE_HEIGHT / 2.0f
                + 1.25f * ISLAND_TILE_HEIGHT * houseScale;
        paint.setColor(Color.WHITE); paint.setAlpha(255); paint.setFilterBitmap(false);
        int save = canvas.save();
        if (mirror) canvas.scale(-1, 1, x, 0);
        canvas.drawBitmap(sprite, null, new RectF(x - 35 * houseScale, bottom - 67 * houseScale, x + 35 * houseScale, bottom), paint);
        canvas.restoreToCount(save);
    }

    private void drawMainCharacter(Canvas canvas, double motionTime) {
        boolean back = "N".equals(characterFacing) || "W".equals(characterFacing);
        boolean mirror = "N".equals(characterFacing) || "E".equals(characterFacing);
        Bitmap sprite = back ? mainCharacterBack : mainCharacter;
        if (!showCharacter || sprite == null) return;
        float x = SCENE_WIDTH / 2.0f + (characterColumn - characterRow) * ISLAND_TILE_WIDTH / 2.0f;
        float y = ISLAND_TOP + ISLAND_TILE_HEIGHT / 2.0f
                + (characterColumn + characterRow) * ISLAND_TILE_HEIGHT / 2.0f;
        float width = 13.5f * characterScale;
        float idle = Math.sin(motionTime * Math.PI / 1.2) > 0.5 ? 0.5f : 0.0f;
        float height = 25.0f * characterScale + idle;
        paint.setColor(Color.WHITE);
        paint.setAlpha(255);
        paint.setFilterBitmap(false);
        int save = canvas.save();
        if (mirror) canvas.scale(-1, 1, x, 0);
        canvas.drawBitmap(sprite, null, new RectF(x - width / 2, y - height,
                x + width / 2, y), paint);
        canvas.restoreToCount(save);
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
        plantMotionSeconds=motionSeconds;
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
        // A virtual Moon continues to cast shadows while it is below the horizon.
        SkyMath.Position shadowLight = sun.altitudeDegrees <= MOON_VISIBLE_BELOW_SUN ? moon : sun;
        float sunY = sunPlanePosition(shadowLight.hourAngleDegrees)[1];
        drawClouds(scene, cloudStates, sun.altitudeDegrees, code, sunY, true);
        drawSun(scene, sun, cloudCover, motionSeconds);
        drawMoon(scene, moon, motionSeconds, sun.altitudeDegrees);
        drawFog(scene, code);
        drawStormFlash(scene, elapsedSeconds, code);
        scene.restoreToCount(skySave);
        // Ground shadows go down before the island so the plate covers them, and before the clouds
        // so no sprite is ever painted over by a shadow it did not come from.
        drawProjectedCloudShadows(scene, cloudStates, shadowLight.altitudeDegrees,
                shadowLight.hourAngleDegrees, cloudCover, code, false);
        drawFloatingIsland(scene, sun.altitudeDegrees, sun.azimuthDegrees,
                sun.hourAngleDegrees, cloudCover, code, moon, cloudStates);
        drawRoads(scene);
        drawSurfaceObjectShadows(scene, shadowLight, cloudCover, code);
        drawHouse(scene);
        drawFences(scene, false);
        drawSurfaceSprites(scene, false);
        drawPortals(scene, false);
        drawMainCharacter(scene, motionSeconds);
        drawPortals(scene, true);
        drawSurfaceSprites(scene, true);
        drawFences(scene, true);
        // Clouds sit in front of the island. They are not clipped to the sky band: a foreground
        // cloud can hang below the horizon line, and clipping it there hid the cloud while its
        // shadow still showed on the grass. Edge clamping already keeps every sprite in frame.
        drawClouds(scene, cloudStates, sun.altitudeDegrees, code, sunY, false);
        drawPrecipitation(scene, elapsedSeconds);
        drawPortalMarkers(scene);
        drawSellingMarkers(scene);
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
    private void drawGroundSilhouette(Canvas canvas, String[] shape, float centreX, float centreY,
            float width, float height) {
        float cellX = width / shape[0].length();
        float cellY = height / shape.length;
        paint.setStyle(Paint.Style.FILL);
        for (int row = 0; row < shape.length; row += 1) {
            String line = shape[row];
            float top = centreY - height * 0.5f + row * cellY;
            for (int column = 0; column < line.length(); column += 1) {
                if (line.charAt(column) == '0') continue;
                float left = centreX - width * 0.5f + column * cellX;
                canvas.drawRect(left, top, left + cellX, top + cellY, paint);
            }
        }
    }

    /**
     * The shadow is thrown AWAY from the light. Progress runs 0 at the eastern (right) end to 1 at
     * the western (left) end, so a morning sun on the right pushes the shadow left, which is the
     * negative direction. This is the opposite sign to the island body's own shadow, whose drift
     * describes which side the light is on rather than which way the shadow is thrown.
     */
    private static double cloudShadowLean(double lightHourAngle) {
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
        return (0.10 + directLight * 0.30) * cloudFilter * ISLAND_BODY_SHADOW_OPACITY_SCALE * 0.9;
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
            double sunHourAngle, double cloudCover, int code, boolean clipToIsland) {
        if (clouds.length == 0) return;
        double opacity = cloudShadowOpacity(Math.max(sunAltitude, 0.0), cloudCover, code);
        float[] sun = sunPlanePosition(sunHourAngle);
        float ratio = LANGIT_MATAHARI_LIFT / (LANGIT_MATAHARI_LIFT - LANGIT_AWAN_LIFT);
        int save = canvas.save();
        if (clipToIsland) setClipIslandSurface(canvas);
        paint.setColor(Color.argb((int) (255.0 * opacity), 12, 23, 40));
        for (CloudState cloud : clouds) {
            float cloudY = cloud.screenBottomY - cloud.shape.length * cloud.cell / 2;
            float x = sun[0] + ratio * (cloud.screenCenterX - sun[0]);
            float y = sun[1] + ratio * (cloudY - sun[1]);
            drawGroundSilhouette(canvas, cloud.shape, x, y,
                    cloud.shape[0].length() * cloud.cell * CLOUD_SHADOW_SCALE,
                    cloud.shape.length * cloud.cell * CLOUD_SHADOW_SCALE);
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
        // Keep the island's original, independent shadow model; cloud ray projection does not
        // position or resize the island shadow. The island remains close to its own shadow.
        drawIslandShadow(canvas, centerX, lowerY, lightAltitude, lightHourAngle, cloudCover, code);

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
        SkyMath.Position shadowLight = sunAltitude <= MOON_VISIBLE_BELOW_SUN ? moon : null;
        drawProjectedCloudShadows(canvas, cloudStates,
                shadowLight != null ? shadowLight.altitudeDegrees : sunAltitude,
                shadowLight != null ? shadowLight.hourAngleDegrees : sunHourAngle,
                cloudCover, code, true);
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

    /**
     * The island's cast shadow is pushed away from whichever body is lighting the scene. It
     * starts from that body's progress along the celestial arc, so the shadow drifts to the
     * opposite side as the sun or moon travels. Offset and size are kept small so the shadow
     * stays tucked close under the island instead of floating away from it.
     */
    private void drawIslandShadow(Canvas canvas, float centerX, float lowerY,
                                  double lightAltitude, double lightHourAngle,
                                  double cloudCover, int code) {
        // A fixed alpha is not enough on its own, and that is why the shadow vanished after dark:
        // alpha blends toward the shade colour, so it removes a fixed FRACTION of the backdrop. The
        // same 0.14 takes 25 levels off bright daytime ground but only 9 at night, where there is
        // barely any light left to take away. The silhouette keeps one colour and one size, and the
        // alpha is solved for a target amount of contrast instead of being fixed, so the shade lands
        // with the same visible weight in every weather and at every hour.
        double floorLuminance = floorLuminance(cloudCover, code);
        double opacity = clamp(SHADOW_CONTRAST_TARGET
                / Math.max(floorLuminance - SHADOW_LUMINANCE, 1.0), 0.06, 0.45);
        // The island shadow stays centred beneath the island, independent of celestial motion.
        float shadowCenterX = centerX;
        float shadowCenterY = lowerY - ISLAND_DEPTH - ISLAND_BODY_SHADOW_RISE;
        // One size, never a function of the light or the weather.
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

        int grass = blend(Color.rgb(63, 79, 40), Color.rgb(124, 157, 44), daylight);
        grass = blend(grass, Color.rgb(195, 157, 96), twilight * 0.25);
        grass = blend(grass, Color.rgb(120, 131, 111), weatherShade);
        boolean wet = (code >= 51 && code <= 67) || (code >= 80 && code <= 82);
        if (wet) grass = blend(grass, Color.rgb(98, 120, 109), 0.12);
        grass = blend(grass, atmosphere.tint, atmosphere.islandMix);

        int grassDark = blend(grass, Color.rgb(45, 73, 56), 0.24);
        double grassTileMix = (0.18 + daylight * 0.77) * (1.0 - weatherShade * 0.30);
        int grassTileSoftFern = blend(grass, Color.rgb(164, 207, 54), grassTileMix);
        int grassTileFernSage = blend(grass, Color.rgb(148, 189, 43), grassTileMix);
        int grassSeam = blend(grass, Color.rgb(49, 77, 55), 0.08);
        int grassWall = blend(grass, Color.rgb(35, 72, 44), 0.28);

        int soil = blend(Color.rgb(50, 37, 28), Color.rgb(154, 74, 15), daylight);
        soil = blend(soil, Color.rgb(190, 116, 45), twilight * 0.24);
        soil = blend(soil, Color.rgb(91, 72, 55), weatherShade * 0.45);
        soil = blend(soil, atmosphere.tint, atmosphere.islandMix * 0.22);
        int soilLight = blend(soil, Color.rgb(190, 106, 26), 0.20);
        int soilDark = blend(soil, Color.rgb(58, 27, 7), 0.50);
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
        int seed = column * 17 + row * 29;
        if (seed % 5 == 0) {
            float x = centerX + (seed % 7 - 3), y = centerY + (seed % 3 - 1) * 0.5f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(blend(fill, Color.rgb(60, 89, 17), 0.55));
            canvas.drawRect(x, y, x + 0.5f, y + 1.5f, paint);
            canvas.drawRect(x - 1.0f, y - 0.5f, x - 0.5f, y + 0.5f, paint);
            canvas.drawRect(x + 1.0f, y - 0.5f, x + 1.5f, y + 0.5f, paint);
            canvas.drawRect(x - 0.5f, y + 1.0f, x + 1.0f, y + 1.5f, paint);
            paint.setColor(blend(fill, Color.rgb(209, 228, 108), 0.35));
            canvas.drawRect(x - 0.5f, y, x, y + 0.5f, paint);
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
                ? blend(baseColor, Color.rgb(209, 228, 108), 0.10 * sunlight.strength)
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
                                   float strength) {
        if (visibility <= 0.0f || strength <= 0.0f) return;
        double high = clamp(clamp(altitude, 0.0, 90.0) / 45.0, 0.0, 1.0);
        float squash = (float) (0.52 + high * 0.48);
        double breathe = 0.5 + 0.5 * Math.sin(elapsedSeconds * GLOW_PULSE_SPEED);
        float reach = (float) (GLOW_REACH * (1.15 - high * 0.45) * (0.82 + breathe * 0.36));
        double alpha = GLOW_ALPHA * (0.72 + breathe * 0.42) * visibility * strength;
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
        float[] position = sunPlanePosition(sun.hourAngleDegrees);
        float centerX = Math.round(position[0] / RENDER_CELL) * RENDER_CELL;
        float centerY = Math.round(position[1] / RENDER_CELL) * RENDER_CELL;
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
        float[] position = sunPlanePosition(moon.hourAngleDegrees);
        float centerX = Math.round(position[0] / RENDER_CELL) * RENDER_CELL;
        float centerY = Math.round(position[1] / RENDER_CELL) * RENDER_CELL;
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

    private CloudState[] createCloudStates(double elapsedSeconds, double cloudCover,
            double windSpeed, double windDirection, double sunAltitude, double sunHourAngle) {
        double bearing = normalizeDegrees(windDirection + 180.0);
        if (windSpeed >= 3.0) {
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
        int count = cloudCountForCover(cloudCover);
        int seed = (int) (latitude * 37 + longitude * 17) ^ (count * 0x2545f491);
        CloudState[] clouds = new CloudState[count];
        double len = Math.hypot(ISLAND_TILE_WIDTH, ISLAND_TILE_HEIGHT);
        for (int index = 0; index < count; index++) {
            int variant = (int) (cloudHash(seed, index, 1) * CLOUD_SHAPE_VARIANTS.length);
            String[] shape = CLOUD_SHAPE_VARIANTS[variant];
            double east = ((cloudHash(seed, index, 2) * 9 + cloudEastTravel) % 9 + 9) % 9 - 4.5;
            double north = ((cloudHash(seed, index, 3) * 9 + cloudNorthTravel) % 9 + 9) % 9 - 4.5;
            float x = (float) (SCENE_WIDTH / 2.0 - LANGIT_AWAN_WEST_SHIFT * ISLAND_TILE_WIDTH / len
                    + (east + north) * ISLAND_TILE_WIDTH / 2.0);
            float y = (float) (ISLAND_CENTER_Y - LANGIT_AWAN_LIFT
                    - LANGIT_AWAN_WEST_SHIFT * ISLAND_TILE_HEIGHT / len
                    + (east - north) * ISLAND_TILE_HEIGHT / 2.0);
            clouds[index] = new CloudState(shape, CLOUD_CELL, x, y + shape.length * CLOUD_CELL / 2);
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
                paint.setAlpha(layerAlpha);
                float runLeft = snapToArtGrid(left + column * cell);
                float runRight = snapToArtGrid(left + (column + 1) * cell);
                canvas.drawRect(runLeft, rowTop, runRight, rowBottom, paint);
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

    private void drawClouds(Canvas canvas, CloudState[] clouds, double sunAltitude, int code,
            float sunY, boolean behind) {
        if (clouds.length == 0) return;
        int[] palette = cloudPalette(sunAltitude, code);
        for (CloudState cloud : clouds) {
            if ((cloud.screenBottomY - cloud.shape.length * cloud.cell / 2 < sunY) != behind) continue;
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
