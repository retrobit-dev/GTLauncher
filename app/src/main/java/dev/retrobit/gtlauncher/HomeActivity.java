package dev.retrobit.gtlauncher;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.content.SharedPreferences;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** A non-scrollable, weather-driven home screen with a separate app drawer. */
public class HomeActivity extends Activity {
    private static final String INVENTORY_PREFS = "shared_inventory";
    private static final int COLOR_TEXT = Color.rgb(244, 246, 244);
    private static final int LOCATION_PERMISSION_REQUEST = 721;
    private static final int IMPORT_WORLD_REQUEST = 722;
    private static final int MAX_WORLD_BYTES = 256 * 1024;
    private static final String WEATHER_SETTINGS_PREFS = "weather_settings";
    private static final long WEATHER_REFRESH_INTERVAL_MS = 30L * 60L * 1000L;
    private static final long WEATHER_CACHE_MAX_AGE_MS = 6L * 60L * 60L * 1000L;
    private static final long GPS_TIMEOUT_MS = 12000L;
    private static final double FALLBACK_LATITUDE = -6.7097;
    private static final double FALLBACK_LONGITUDE = 111.3476;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService appLoader = Executors.newSingleThreadExecutor();
    private final ExecutorService weatherLoader = Executors.newSingleThreadExecutor();
    private final ExecutorService worldLoader = Executors.newSingleThreadExecutor();
    private final ExecutorService treeLoader = Executors.newSingleThreadExecutor();
    private final List<AppEntry> apps = new ArrayList<>();
    private final float[] rotationMatrix = new float[9];
    private final float[] remappedRotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private int loadGeneration;
    private int weatherRequestGeneration;
    private int locationRequestGeneration;
    private int activeLocationRequestId = -1;
    private LocationManager activeLocationManager;
    private LocationListener activeLocationListener;
    private CancellationSignal activeGpsCancellationSignal;
    private Runnable activeGpsTimeout;
    private long lastWeatherAttemptMillis;
    private boolean permissionRequestPending;
    private boolean weatherFlowStarted;
    private boolean precisePermissionKnown;
    private boolean lastPrecisePermission;

    private View homeScreen;
    private View homeDateHud;
    private ImageView seasonIcon;
    private ImageView dayNightIcon;
    private final Runnable dayNightRefresh = new Runnable() {
        @Override public void run() {
            updateDayNightIcon();
            updateCalendarHud();
            mainHandler.postDelayed(this, 30_000L);
        }
    };
    private void updateDayNightIcon() {
        if (dayNightIcon == null) return;
        boolean night = DayNight.isNight(System.currentTimeMillis(), currentLatitude, currentLongitude);
        String kind = HudWeather.kind(currentWeather == null ? -1 : currentWeather.weatherCode);
        int image;
        String weatherLabel;
        switch (kind) {
            case "cloud": image = night ? R.drawable.hud_weather_night_cloud : R.drawable.hud_weather_day_cloud; weatherLabel = "berawan"; break;
            case "rain": image = night ? R.drawable.hud_weather_night_rain : R.drawable.hud_weather_day_rain; weatherLabel = "hujan"; break;
            case "snow": image = night ? R.drawable.hud_weather_night_snow : R.drawable.hud_weather_day_snow; weatherLabel = "salju"; break;
            case "storm": image = night ? R.drawable.hud_weather_night_storm : R.drawable.hud_weather_day_storm; weatherLabel = "badai"; break;
            default: image = night ? R.drawable.hud_night : R.drawable.hud_day; weatherLabel = "clear".equals(kind) ? "cerah" : "cuaca belum diketahui";
        }
        dayNightIcon.setImageResource(image);
        dayNightIcon.setContentDescription(getString(night ? R.string.hud_night : R.string.hud_day) + " · " + weatherLabel);
    }
    private int lastCalendarBackground = 0;
    private void updateCalendarHud() {
        View calendar = findViewById(R.id.hud_calendar);
        if (calendar == null) return;
        int day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK);
        int background = CalendarHud.isWeekend(day)
                ? R.drawable.bg_date_hud_calendar_weekend : R.drawable.bg_date_hud_calendar;
        if (background == lastCalendarBackground) return;
        int left = calendar.getPaddingLeft(), top = calendar.getPaddingTop();
        int right = calendar.getPaddingRight(), bottom = calendar.getPaddingBottom();
        calendar.setBackgroundResource(background);
        calendar.setPadding(left, top, right, bottom);
        lastCalendarBackground = background;
    }
    private TextView batteryHud;
    private ImageView batteryIcon;
    private boolean batteryReceiverRegistered;
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent != null && Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) updateBatteryHud(intent);
        }
    };
    private int seasonRequestGeneration;
    private long seasonLastAttempt;
    private double seasonLatitude = Double.NaN, seasonLongitude = Double.NaN;
    private boolean seasonFallback;
    private int treeRequestGeneration;
    private long treeLastAttempt;
    private double treeLatitude=Double.NaN,treeLongitude=Double.NaN;
    private boolean treeFallback;
    private TreePhenology.Result treePhenology;
    private WeatherSkyView skyView;
    private int selectedTool = R.id.dock_tool_hand;
    private String movingObjectWorld;
    private WorldStore.House movingHouse;
    private WorldStore.SurfaceObject movingSurface;
    private WorldStore worldStore;
    private int portalWalkGeneration;
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private WeatherSnapshot currentWeather;
    private double currentLatitude = FALLBACK_LATITUDE;
    private double currentLongitude = FALLBACK_LONGITUDE;
    private TextView weatherSourceView;
    private TextView weatherLicenseView;
    private View drawerScreen;
    private final Runnable periodicWeatherRefresh = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed()) return;
            discardPreciseWeatherIfPermissionRevoked();
            expireWeatherCacheIfNeeded();
            refreshWeatherIfNeeded(false);
            mainHandler.postDelayed(this, WEATHER_REFRESH_INTERVAL_MS);
        }
    };
    private float swipeStartX;
    private float swipeStartY;
    private TextView appCountView;
    private TextView emptyView;
    private TextView defaultHomeButton;
    private EditText searchView;
    private GridLayout appGrid;

    private final SensorEventListener orientationListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (skyView == null || event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) return;
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

            int axisX = SensorManager.AXIS_X;
            int axisY = SensorManager.AXIS_Y;
            switch (getWindowManager().getDefaultDisplay().getRotation()) {
                case Surface.ROTATION_90:
                    axisX = SensorManager.AXIS_Y;
                    axisY = SensorManager.AXIS_MINUS_X;
                    break;
                case Surface.ROTATION_180:
                    axisX = SensorManager.AXIS_MINUS_X;
                    axisY = SensorManager.AXIS_MINUS_Y;
                    break;
                case Surface.ROTATION_270:
                    axisX = SensorManager.AXIS_MINUS_Y;
                    axisY = SensorManager.AXIS_X;
                    break;
                default:
                    break;
            }
            if (!SensorManager.remapCoordinateSystem(
                    rotationMatrix, axisX, axisY, remappedRotationMatrix)) {
                System.arraycopy(rotationMatrix, 0, remappedRotationMatrix, 0, rotationMatrix.length);
            }
            SensorManager.getOrientation(remappedRotationMatrix, orientationAngles);
            float heading = (float) Math.toDegrees(orientationAngles[0]);
            GeomagneticField magneticField = new GeomagneticField(
                    (float) currentLatitude, (float) currentLongitude, 0.0f, System.currentTimeMillis());
            skyView.setHeadingDegrees(heading + magneticField.getDeclination());
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // The renderer falls back to a north-facing sky panorama if the sensor is unavailable.
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        configureSystemBars();

        homeScreen = findViewById(R.id.home_screen);
        homeDateHud = findViewById(R.id.home_date_hud);
        // The right edge remains fixed when the entire HUD is scaled.
        homeDateHud.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            view.setPivotX(right - left);
            view.setPivotY(0f);
        });
        seasonIcon = findViewById(R.id.hud_season_icon);
        dayNightIcon = findViewById(R.id.hud_day_night_icon);
        batteryHud = findViewById(R.id.hud_battery);
        batteryIcon = findViewById(R.id.hud_battery_icon);
        resizeHudReadouts();
        skyView = (WeatherSkyView) homeScreen;
        skyView.setLocation(FALLBACK_LATITUDE, FALLBACK_LONGITUDE);
        worldStore = new WorldStore(getSharedPreferences("worlds", MODE_PRIVATE),new InventoryStore(getSharedPreferences(INVENTORY_PREFS,MODE_PRIVATE)));
        applyActiveWorld();
        drawerScreen = findViewById(R.id.app_drawer_screen);
        appCountView = findViewById(R.id.app_count);
        emptyView = findViewById(R.id.empty_message);
        defaultHomeButton = findViewById(R.id.default_home_button);
        searchView = findViewById(R.id.search_apps);
        appGrid = findViewById(R.id.app_grid);
        weatherSourceView = findViewById(R.id.weather_attribution_source);
        weatherLicenseView = findViewById(R.id.weather_attribution_license);
        configureWeatherAttribution();

        // Preview mode switches live outside the simulated tablet. The launcher opens in gameplay.
        setMapMode(true);
        homeScreen.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                portalWalkGeneration++;
                skyView.setPortalWalkActive(false);
                applyActiveWorld(); // Restore the last saved position if a walk was interrupted.
                swipeStartX = event.getX();
                swipeStartY = event.getY();
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                float deltaX = event.getX() - swipeStartX;
                float deltaY = event.getY() - swipeStartY;
                if (deltaY < -dp(72) && Math.abs(deltaY) > Math.abs(deltaX) * 1.15f) {
                    openDrawer();
                } else if (gameplayMode && Math.hypot(deltaX,deltaY) < dp(8)) {
                    gameplayTap(event.getX(),event.getY());
                }
                return true;
            }
            return true;
        });
        findViewById(R.id.drawer_back).setOnClickListener(view -> closeDrawer());
        findViewById(R.id.dock_tool_hand).setSelected(true);
        skyView.setHoeSelected(true);
        for(int tool: gameplayTools()) findViewById(tool).setOnClickListener(view -> selectTool(tool));
        findViewById(R.id.dock_add_tab).setOnClickListener(view ->
                Toast.makeText(this, "Penambahan tab dock belum tersedia.", Toast.LENGTH_SHORT).show());
        findViewById(R.id.home_phone_button).setOnClickListener(view -> openDrawer());
        findViewById(R.id.home_settings_button).setOnClickListener(view -> openDeviceSettings());
        findViewById(R.id.home_bag_button).setOnClickListener(view -> showBag());
        defaultHomeButton.setOnClickListener(view -> requestDefaultLauncher());
        findViewById(R.id.location_permission_button).setOnClickListener(view -> showLocationPermissionExplanation());
        findViewById(R.id.world_button).setOnClickListener(view -> showWorldDialog());

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Nothing to do.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderApps(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Nothing to do.
            }
        });
        searchView.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                searchView.clearFocus();
                return true;
            }
            return false;
        });

        appGrid.setUseDefaultMargins(false);
        appGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        renderApps("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBatteryUpdates();
        mainHandler.removeCallbacks(dayNightRefresh);
        dayNightRefresh.run();
        updateDefaultLauncherButton();
        refreshApps();
        if (skyView != null) skyView.setAnimationEnabled(true);
        registerOrientationSensor();
        if (precisePermissionKnown) {
            boolean preciseNow = hasPreciseLocationPermission();
            if (preciseNow != lastPrecisePermission) lastWeatherAttemptMillis = 0L;
            lastPrecisePermission = preciseNow;
        }
        discardPreciseWeatherIfPermissionRevoked();
        expireWeatherCacheIfNeeded();
        if (weatherFlowStarted) refreshWeatherIfNeeded(false);
        else startWeatherFlow();
        mainHandler.removeCallbacks(periodicWeatherRefresh);
        mainHandler.postDelayed(periodicWeatherRefresh, WEATHER_REFRESH_INTERVAL_MS);
    }

    @Override
    protected void onPause() {
        stopBatteryUpdates();
        portalWalkGeneration++;
        if (skyView != null) { skyView.setPortalWalkActive(false); applyActiveWorld(); }
        mainHandler.removeCallbacks(dayNightRefresh);
        mainHandler.removeCallbacks(periodicWeatherRefresh);
        if (sensorManager != null) sensorManager.unregisterListener(orientationListener);
        if (skyView != null) skyView.setAnimationEnabled(false);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        cancelPendingGpsRequest();
        loadGeneration++;
        weatherRequestGeneration++;
        locationRequestGeneration++;
        seasonRequestGeneration++;
        treeRequestGeneration++;
        treeLoader.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        appLoader.shutdownNow();
        weatherLoader.shutdownNow();
        worldLoader.shutdownNow();
        super.onDestroy();
    }

    private void updateBatteryHud(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        boolean present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true);
        if (!present || level < 0 || scale <= 0 || level > scale) {
            batteryIcon.setImageResource(R.drawable.hud_battery_unknown);
            batteryHud.setText("--.--");
            batteryHud.setTextSize(TypedValue.COMPLEX_UNIT_PX, ((TextView) findViewById(R.id.hud_time)).getTextSize());
            batteryHud.setContentDescription(getString(R.string.battery_unknown));
            resizeHudReadouts();
            return;
        }
        String value = String.format(Locale.US, "%.2f", 100.0 * level / scale);
        batteryHud.setText(value);
        TextView time = findViewById(R.id.hud_time);
        batteryHud.setTextSize(TypedValue.COMPLEX_UNIT_PX, time.getTextSize() * (value.length() > 5 ? 5f / 6f : 1f));
        boolean charging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING;
        boolean low = level * 5L <= scale;
        int icon = charging ? R.drawable.hud_battery_charging : low ? R.drawable.hud_battery_low : R.drawable.hud_battery_icon;
        int description = charging ? R.string.battery_accessibility_charging : low ? R.string.battery_accessibility_low : R.string.battery_accessibility;
        batteryIcon.setImageResource(icon);
        batteryHud.setContentDescription(getString(description, value));
        resizeHudReadouts();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void resizeHudReadouts() {
        TextView time = findViewById(R.id.hud_time);
        View timePanel = findViewById(R.id.hud_time_panel);
        View batteryPanel = findViewById(R.id.hud_battery_panel);
        // Keep the clock panel width fixed as battery readings change.
        float textWidth = time.getPaint().measureText("88:88");
        int width = dp(8) + dp(14) + dp(3) + dp(3) + (int) Math.ceil(textWidth);
        for (View panel : new View[]{timePanel, batteryPanel}) {
            if (panel.getLayoutParams().width != width) {
                android.view.ViewGroup.LayoutParams params = panel.getLayoutParams();
                params.width = width;
                panel.setLayoutParams(params);
            }
        }
    }

    private void startBatteryUpdates() {
        if (batteryReceiverRegistered) return;
        try {
            // ACTION_BATTERY_CHANGED is a protected sticky system broadcast.
            Intent current = registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            batteryReceiverRegistered = true;
            if (current != null) updateBatteryHud(current);
        } catch (RuntimeException ignored) {
            batteryIcon.setImageResource(R.drawable.hud_battery_unknown);
            batteryHud.setText("--.--");
            batteryHud.setTextSize(TypedValue.COMPLEX_UNIT_PX, ((TextView) findViewById(R.id.hud_time)).getTextSize());
            batteryHud.setContentDescription(getString(R.string.battery_unknown));
            resizeHudReadouts();
        }
    }

    private void stopBatteryUpdates() {
        if (!batteryReceiverRegistered) return;
        unregisterReceiver(batteryReceiver);
        batteryReceiverRegistered = false;
    }

    private boolean gameplayMode;
    private void setMapMode(boolean gameplay) {
        gameplayMode=gameplay;
        movingObjectWorld=null;movingHouse=null;movingSurface=null;
        portalWalkGeneration++;
        skyView.setPortalWalkActive(false);
        applyActiveWorld();
        skyView.setGameplayMode(gameplay);
    }
    private void openDeviceSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (ActivityNotFoundException | SecurityException error) {
            Toast.makeText(this, "Pengaturan perangkat tidak tersedia.", Toast.LENGTH_SHORT).show();
        }
    }
    private int[] gameplayTools() {return new int[]{R.id.dock_tool_hand,R.id.dock_tool_hoe,R.id.dock_tool_shovel,R.id.dock_tool_water,R.id.dock_tool_sickle,R.id.dock_tool_axe,R.id.dock_tool_pickaxe,R.id.dock_tool_fishing};}
    private void selectTool(int tool) {
        movingObjectWorld=null;movingHouse=null;movingSurface=null;
        selectedTool=selectedTool==tool ? -1 : tool;
        for(int id:gameplayTools())findViewById(id).setSelected(id==selectedTool);
        portalWalkGeneration++;skyView.setPortalWalkActive(false);applyActiveWorld();
        skyView.setHoeSelected(selectedTool==R.id.dock_tool_hoe || selectedTool==R.id.dock_tool_hand);
        Toast.makeText(this,selectedTool==-1?"Alat dilepas.":selectedTool==R.id.dock_tool_hand?"Tangan aktif: pilih objek di depan karakter.":selectedTool==R.id.dock_tool_hoe?"Cangkul aktif. Blok tanah pertanian belum tersedia.":"Alat dipilih; fungsinya belum tersedia.",Toast.LENGTH_SHORT).show();
    }
    private WorldStore.SurfaceObject frontSurface(WorldStore.World world,PortalTravel.Tile front) {
        for(WorldStore.SurfaceObject object:world.surfaces) {
            int size=object.type.equals("tree") && !object.material.equals("banana") && !object.material.equals("palm")?2:1;
            if(front.column>=object.column && front.column<object.column+size && front.row>=object.row && front.row<object.row+size)return object;
        }
        return null;
    }
    private WorldStore.House frontHouse(WorldStore.World world,PortalTravel.Tile front) {
        for(WorldStore.House house:world.houses)if(house.covers(front.column,front.row))return house;
        return null;
    }
    private void gameplayTap(float x,float y) {
        try {
            WorldStore.World world=worldStore.activeWorld();if(world==null || !world.showCharacter)return;
            PortalTravel.Tile target=skyView.groundTileAt(x,y),front=GameplayTarget.front(world.characterColumn,world.characterRow,world.characterFacing);
            if(movingObjectWorld!=null) {
                if(!movingObjectWorld.equals(world.id)){movingObjectWorld=null;movingHouse=null;movingSurface=null;Toast.makeText(this,"World berubah; pemindahan dibatalkan.",Toast.LENGTH_SHORT).show();return;}
                if(target==null){Toast.makeText(this,"Pilih tile di dalam world.",Toast.LENGTH_SHORT).show();return;}
                if(worldStore.moveSelectedObject(movingObjectWorld,movingHouse,movingSurface,target.column,target.row)) {
                    movingObjectWorld=null;movingHouse=null;movingSurface=null;applyActiveWorld();Toast.makeText(this,"Objek berhasil dipindahkan.",Toast.LENGTH_SHORT).show();
                } else Toast.makeText(this,"Objek tidak dapat ditempatkan di sini; periksa tapak dan penghalang.",Toast.LENGTH_LONG).show();
                return;
            }
            if(front==null)return;
            WorldStore.SurfaceObject object=frontSurface(world,front);WorldStore.House house=frontHouse(world,front);
            PortalTravel.Portal portal=null;for(PortalTravel.Portal p:world.portals)if(p.tile.equals(front)){portal=p;break;}
            boolean frontHit=front.equals(target);
            boolean characterHit=!frontHit && skyView.characterAt(x,y);
            if(selectedTool!=-1 && (characterHit || (frontHit && selectedTool==R.id.dock_tool_hand && (house!=null || object!=null || portal!=null)))) {
                if(selectedTool==R.id.dock_tool_hand) {
                    if(house!=null || object!=null || portal!=null)showHandObjectMenu(world.id,house,object,portal);
                    else Toast.makeText(this,"Tidak ada objek di depan karakter.",Toast.LENGTH_SHORT).show();
                } else if(selectedTool==R.id.dock_tool_hoe)Toast.makeText(this,"Blok tanah pertanian belum tersedia; tidak ada tile yang diubah.",Toast.LENGTH_SHORT).show();
                else Toast.makeText(this,"Fungsi alat ini belum tersedia.",Toast.LENGTH_SHORT).show();
                return;
            }
            // Existing marker UI remains usable with the hand; all other taps target movement.
            if(selectedTool==R.id.dock_tool_hand) {
                WorldStore.SurfaceObject markerBox=skyView.sellingMarkerAt(x,y);String marker=skyView.portalMarkerAt(x,y);
                if(markerBox!=null){showSellingPlaceholder(markerBox);return;}if(marker!=null){confirmPortalMarker(marker);return;}
            }
            walkToGround(x,y);
        } catch(JSONException error){Toast.makeText(this,"World tidak dapat dibaca.",Toast.LENGTH_SHORT).show();}
    }
    private void showHandObjectMenu(String worldId,WorldStore.House house,WorldStore.SurfaceObject object,PortalTravel.Portal portal) {
        boolean small=WorldStore.portableSmall(object),movable=house!=null || small;
        java.util.List<String> actions=new java.util.ArrayList<>();
        actions.add(small?"Ambil ke tas (belum bisa ditaruh kembali)":"Ambil — tidak tersedia");
        actions.add(movable?"Pindahkan":"Pindahkan — tidak tersedia");
        if(object!=null && object.type.equals("tree"))actions.add("Panen pohon");
        else if(object!=null && (object.type.equals("storage-box") || object.type.equals("selling-box")))actions.add("Buka box");
        else if(portal!=null)actions.add("Gunakan portal");
        String label=house!=null?"Rumah":object!=null?object.type:"Portal";
        new AlertDialog.Builder(this).setTitle(label+" dipilih")
            .setItems(actions.toArray(new String[0]),(dialog,which)->{
                    if(which==0) {
                        if(!small){Toast.makeText(this,"Objek ini tidak dapat diambil.",Toast.LENGTH_SHORT).show();return;}
                        new AlertDialog.Builder(this).setTitle("Ambil objek?")
                            .setMessage("Objek akan disimpan di tas perangkat ini. Menaruh kembali objek dari tas belum tersedia.")
                            .setPositiveButton("Ambil",(confirm,button)->{
                                try {
                                    if(!worldStore.takeSelectedObject(worldId,object))throw new JSONException("Object changed");
                                    applyActiveWorld();Toast.makeText(this,"Objek masuk ke tas.",Toast.LENGTH_SHORT).show();
                                } catch(JSONException | ClassCastException error){Toast.makeText(this,"Objek tidak berubah; periksa data world dan tas.",Toast.LENGTH_LONG).show();}
                            }).setNegativeButton("Batal",null).show();
                    } else if(which==1) {
                        if(!movable){Toast.makeText(this,"Objek ini belum dapat dipindahkan.",Toast.LENGTH_SHORT).show();return;}
                        movingObjectWorld=worldId;movingHouse=house;movingSurface=object;
                        Toast.makeText(this,house!=null?"Ketuk tile sudut kiri atas tapak rumah. Ketuk ikon tangan untuk batal.":"Ketuk tile tujuan. Ketuk ikon tangan untuk batal.",Toast.LENGTH_LONG).show();
                    } else if(object!=null && object.type.equals("tree"))interactTree(object);
                    else if(object!=null)showSellingPlaceholder(object);
                    else if(portal!=null)walkToPortal(portal.id);
            }).setNegativeButton("Batal",null).show();
    }
    private void walkToGround(float x,float y) {
        PortalTravel.Tile target=skyView.groundTileAt(x,y);
        if(target==null)return;
        try {
            WorldStore.World world=worldStore.activeWorld();
            if(world==null || !world.showCharacter)return;
            for(WorldStore.House house:world.houses)if(house.covers(target.column,target.row)
                    && (target.column!=world.characterColumn || target.row!=world.characterRow)) {
                if(selectedTool!=-1 && Math.abs(target.column-world.characterColumn)+Math.abs(target.row-world.characterRow)==1) {
                    String facing=GameplayTarget.direction(new PortalTravel.Tile(world.characterColumn,world.characterRow),target);
                    if(!facing.equals(world.characterFacing)) {
                        if(!worldStore.moveCharacter(world.id,world.characterColumn,world.characterRow,facing))throw new JSONException("Facing failed");
                        applyActiveWorld();Toast.makeText(this,"Arah hadap berubah.",Toast.LENGTH_SHORT).show();return;
                    }
                }
                Toast.makeText(this,"Tile ditempati rumah.",Toast.LENGTH_SHORT).show();return;
            }
            java.util.List<PortalTravel.Tile> route=PortalTravel.path(
                new PortalTravel.Tile(world.characterColumn,world.characterRow),target,portalTerrain(world),world.gridSize());
            if(route==null) {
                if(selectedTool!=-1 && world.tileExists(target.column,target.row) && Math.abs(target.column-world.characterColumn)+Math.abs(target.row-world.characterRow)==1) {
                    String facing=GameplayTarget.direction(new PortalTravel.Tile(world.characterColumn,world.characterRow),target);
                    if(!facing.equals(world.characterFacing)) {
                        if(!worldStore.moveCharacter(world.id,world.characterColumn,world.characterRow,facing))throw new JSONException("Facing failed");
                        applyActiveWorld();Toast.makeText(this,"Arah hadap berubah.",Toast.LENGTH_SHORT).show();return;
                    }
                }
                Toast.makeText(this,"Jalan terhalang.",Toast.LENGTH_SHORT).show();return;
            }
            if(selectedTool!=-1 && route.size()>1) {
                String facing=GameplayTarget.direction(route.get(0),route.get(1));
                if(!facing.equals(world.characterFacing)) {
                    if(!worldStore.moveCharacter(world.id,world.characterColumn,world.characterRow,facing))throw new JSONException("Facing failed");
                    applyActiveWorld();skyView.setGameplayRoute(route,1);Toast.makeText(this,"Arah hadap berubah. Ketuk tujuan lagi untuk bergerak.",Toast.LENGTH_SHORT).show();return;
                }
            }
            int generation=++portalWalkGeneration; skyView.setPortalWalkActive(true);skyView.setGameplayRoute(route,1);
            walkGroundStep(world,route,1,generation,world.characterFacing);
        } catch(JSONException error){Toast.makeText(this,"World tidak dapat dibaca.",Toast.LENGTH_SHORT).show();}
    }
    private void walkGroundStep(WorldStore.World world,java.util.List<PortalTravel.Tile> route,int index,int generation,String facing) {
        if(generation!=portalWalkGeneration)return;
        if(index>=route.size()) {
            try {PortalTravel.Tile end=route.get(route.size()-1);
                if(!worldStore.moveCharacter(world.id,end.column,end.row,facing)) throw new JSONException("Move failed");
                skyView.setPortalWalkActive(false);applyActiveWorld();
            } catch(JSONException e){skyView.setPortalWalkActive(false);applyActiveWorld();Toast.makeText(this,"Posisi tidak dapat disimpan.",Toast.LENGTH_SHORT).show();}
            return;
        }
        skyView.setGameplayRoute(route,index);
        PortalTravel.Tile previous=route.get(index-1),next=route.get(index);
        String direction=next.column>previous.column?"E":next.column<previous.column?"W":next.row>previous.row?"S":"N";
        int duration=android.animation.ValueAnimator.areAnimatorsEnabled()
                ? CharacterGait.durationMillis(index,route.size()-1) : 1;
        skyView.animateCharacterStep(previous.column,previous.row,next.column,next.row,(float)world.characterScale,direction,index,route.size()-1,duration);
        mainHandler.postDelayed(()->walkGroundStep(world,route,index+1,generation,direction),duration);
    }

    private void applyActiveWorld() {
        portalWalkGeneration++;
        skyView.setPortalWalkActive(false);
        try {
            WorldStore.World world = worldStore.activeWorld();
            skyView.setWorldIslandTint(world == null ? null : Color.parseColor(world.color));
            skyView.setWorldPortals(world == null ? null : world.portals);
            skyView.setWorldSurfaces(world == null ? null : world.id,world == null ? null : world.surfaces);
            skyView.setWorldTerrain(world == null ? null : world.previewData);
            refreshHarvestDisplay(world);
            skyView.setWorldHouses(world == null ? null : world.houses);
            skyView.setWorldCharacter(world != null && world.showCharacter,
                    world == null ? 4 : world.characterColumn, world == null ? 4 : world.characterRow,
                    world == null ? 1.0f : (float) world.characterScale,
                    world == null ? "S" : world.characterFacing);
        } catch (JSONException | IllegalArgumentException error) {
            skyView.setWorldIslandTint(null);
            skyView.setWorldPortals(null);
            skyView.setWorldSurfaces(null,null);
            skyView.setWorldTerrain(null);
            skyView.setWorldHouses(null);
            skyView.setWorldCharacter(false, 4, 4, 1.0f, "S");
            Toast.makeText(this, "Data world tidak dapat dibaca; data lama tidak ditimpa.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // The bag belongs to the player, not a particular world. Invalid data is
    // never replaced with an empty inventory.
    private InventoryStore inventoryStore() {return new InventoryStore(getSharedPreferences(INVENTORY_PREFS,MODE_PRIVATE));}
    private JSONArray readBag() throws JSONException {return inventoryStore().bag();}
    private boolean changeBag(String name,int delta) {
        try {if(!inventoryStore().changeBag(name,delta))throw new JSONException("Tidak dapat menyimpan");return true;}
        catch (JSONException | ClassCastException error) {Toast.makeText(this,"Tas tidak dapat dibaca atau disimpan. Periksa data dan ruang penyimpanan.",Toast.LENGTH_LONG).show();return false;}
    }
    private void renderBag(LinearLayout items,TextView notice) {
        items.removeAllViews();
        try {
            JSONArray data=readBag(),objects=worldStore.objectBag();notice.setText(data.length()==0 && objects.length()==0?"Tas masih kosong.":"");
            for(int i=0;i<data.length();i++) {
                JSONObject item=data.getJSONObject(i);String name=item.getString("name");int count=item.getInt("quantity");
                LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
                TextView text=new TextView(this);text.setText(name+" × "+count);text.setTextColor(Color.rgb(52,43,30));
                row.addView(text,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
                Button minus=new Button(this),plus=new Button(this);minus.setText("−");plus.setText("+");
                minus.setContentDescription("Kurangi satu "+name);plus.setContentDescription("Tambah satu "+name);
                minus.setOnClickListener(v->{if(changeBag(name,-1))renderBag(items,notice);});plus.setOnClickListener(v->{if(changeBag(name,1))renderBag(items,notice);});
                row.addView(minus);row.addView(plus);items.addView(row);
            }
            if(objects.length()>0){TextView heading=new TextView(this);heading.setText("Objek di tas · penempatan belum tersedia");heading.setTextColor(Color.rgb(52,43,30));items.addView(heading);}
            for(int i=0;i<objects.length();i++){
                JSONObject entry=objects.getJSONObject(i),object=entry.getJSONObject("object");
                TextView label=new TextView(this);label.setText(object.getString("type")+" · "+object.optString("species",object.optString("variant",""))+" ("+entry.getString("worldId")+")");label.setTextColor(Color.rgb(52,43,30));items.addView(label);
            }
        } catch (JSONException | ClassCastException error) {notice.setText("Data tas tidak dapat dibaca. Data lama tidak ditimpa.");}
    }
    private void showBag() {
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(16),dp(12),dp(16),dp(12));panel.setBackgroundResource(R.drawable.bg_portal_dialog);
        TextView hint=new TextView(this);hint.setText("Satu tas untuk semua world. Tambah atau kurangi barang secara manual.");hint.setTextColor(Color.rgb(52,43,30));panel.addView(hint);
        TextView notice=new TextView(this);notice.setTextColor(Color.rgb(52,43,30));panel.addView(notice);
        ScrollView scroll=new ScrollView(this);LinearLayout items=new LinearLayout(this);items.setOrientation(LinearLayout.VERTICAL);scroll.addView(items);panel.addView(scroll,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(180)));
        EditText name=new EditText(this);name.setSingleLine(true);name.setHint("Nama barang (maks. 40 karakter)");panel.addView(name);
        EditText quantity=new EditText(this);quantity.setSingleLine(true);quantity.setInputType(InputType.TYPE_CLASS_NUMBER);quantity.setText("1");quantity.setHint("Jumlah");panel.addView(quantity);
        Button add=new Button(this);add.setText("Tambah");panel.addView(add);
        add.setOnClickListener(v->{String n=name.getText().toString().trim(), q=quantity.getText().toString();
            try {int amount=Integer.parseInt(q);if(n.isEmpty() || n.length()>40 || amount<1 || amount>999999)throw new NumberFormatException();
                if(changeBag(n,amount)){name.setText("");quantity.setText("1");renderBag(items,notice);}}
            catch(NumberFormatException error){notice.setText("Isi nama dan jumlah bulat antara 1 dan 999999.");}
        });
        renderBag(items,notice);new AlertDialog.Builder(this).setTitle("Tas · Inventaris").setView(panel).setPositiveButton("Tutup",(dialog,which)->dialog.dismiss()).show();
    }

    private void showWorldDialog() {
        final List<WorldStore.World> worlds;
        try {
            worlds = worldStore.list();
        } catch (JSONException error) {
            new AlertDialog.Builder(this).setTitle("World")
                    .setMessage("Data world tidak dapat dibaca. Data lama tidak akan ditimpa.")
                    .setPositiveButton("Tutup", null).show();
            return;
        }
        String[] labels = new String[worlds.size() + 1];
        labels[0] = "Base World";
        String active = worldStore.activeId();
        int selected = 0;
        for (int i = 0; i < worlds.size(); i++) {
            WorldStore.World world = worlds.get(i);
            labels[i + 1] = world.name + (world.description.isEmpty() ? "" : " — " + world.description);
            if (world.id.equals(active)) selected = i + 1;
        }
        new AlertDialog.Builder(this).setTitle("Pilih world")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    String id = which == 0 ? WorldStore.BASE_ID : worlds.get(which - 1).id;
                    try {
                        if (!worldStore.select(id)) throw new IllegalStateException("save failed");
                        applyActiveWorld();
                        dialog.dismiss();
                        closeDrawer();
                    } catch (JSONException | IllegalStateException error) {
                        Toast.makeText(this, "Pilihan world tidak dapat disimpan.", Toast.LENGTH_LONG).show();
                    }
                }).setNeutralButton("Impor world", (dialog, which) -> chooseWorldFile())
                .setNegativeButton("Tutup", null).show();
    }

    private void chooseWorldFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // Some document providers label JSON as application/octet-stream. Show all files,
        // then validate the selected content before making any changes.
        try {
            startActivityForResult(intent, IMPORT_WORLD_REQUEST);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "Tidak ada pemilih berkas di perangkat ini.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != IMPORT_WORLD_REQUEST || resultCode != RESULT_OK || data == null
                || data.getData() == null) return;
        final Uri uri = data.getData();
        worldLoader.execute(() -> {
            try (InputStream input = getContentResolver().openInputStream(uri);
                 ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                if (input == null) throw new IllegalArgumentException("Empty stream");
                byte[] buffer = new byte[4096];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (bytes.size() + read > MAX_WORLD_BYTES) throw new IllegalArgumentException("Too large");
                    bytes.write(buffer, 0, read);
                }
                String json = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
                if (json.startsWith("\uFEFF")) json = json.substring(1);
                WorldStore.World world = WorldStore.parseExport(json);
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    confirmWorldImport(world);
                });
            } catch (Exception error) {
                // Do not imply that a newer, intact world is corrupt or silently strip its objects.
                final boolean unsupportedVersion = error instanceof JSONException
                        && "Unsupported world version".equals(error.getMessage());
                final boolean unsupportedObjects = error instanceof JSONException
                        && ("Unsupported object".equals(error.getMessage()) || "Unsupported objects".equals(error.getMessage())
                        || (error.getMessage()!=null && error.getMessage().startsWith("Unsupported field:")));
                mainHandler.post(() -> {
                    if (!isFinishing() && !isDestroyed())
                        Toast.makeText(this, unsupportedVersion
                                ? "Versi world ini belum didukung Android. Data tidak diubah."
                                : unsupportedObjects ? "World berisi objek yang belum didukung Android. Data tidak diubah."
                                : "Berkas world tidak valid atau tidak dapat dibaca.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // Portal worlds are admitted only if every object is currently supported by Android.
    private PortalTravel.Terrain portalTerrain(WorldStore.World world) {
        return new PortalTravel.Terrain() {
            public boolean canStand(int c,int r) {
                if(!world.tileExists(c,r)) return false;
                // Recover safely if an older editor allowed a house over the character.
                for(WorldStore.House house:world.houses)
                    if(house.covers(c,r) && !house.covers(world.characterColumn,world.characterRow)) return false;
                for(WorldStore.SurfaceObject o:world.surfaces) {
                    if(o.type.equals("tree") && c>=o.column && c<o.column+((o.material.equals("banana") || o.material.equals("palm"))?1:2) && r>=o.row && r<o.row+((o.material.equals("banana") || o.material.equals("palm"))?1:2)) return false;
                    if((o.type.equals("bush")||o.type.equals("sunflower")||o.type.equals("mailbox")||o.type.equals("storage-box")||o.type.equals("selling-box")||(o.type.equals("road") && "water".equals(o.material))) && o.column==c && o.row==r)return false;
                }
                return true;
            }
            public boolean canStep(int a,int b,int c,int d) {
                // Legacy saves may start inside a house. They can escape, but cannot re-enter.
                for(WorldStore.House house:world.houses)if(house.covers(c,d) && !house.covers(a,b))return false;
                for(WorldStore.SurfaceObject o:world.surfaces) if(o.type.equals("fence")) {
                    int x=o.column+(o.edge.equals("E")?1:o.edge.equals("W")?-1:0),y=o.row+(o.edge.equals("S")?1:o.edge.equals("N")?-1:0);
                    if((o.column==a && o.row==b && x==c && y==d) || (o.column==c && o.row==d && x==a && y==b)) return false;
                }
                return true;
            }
        };
    }
    private void refreshHarvestDisplay() {
        try {refreshHarvestDisplay(worldStore.activeWorld());}
        catch(JSONException | ClassCastException e){skyView.setHarvestedTrees(java.util.Collections.emptySet());}
    }
    private void refreshHarvestDisplay(WorldStore.World world) {
        java.util.Set<String> harvested=new java.util.HashSet<>();
        if(world!=null && treePhenology!=null)for(WorldStore.SurfaceObject tree:world.surfaces)if(tree.type.equals("tree")) {
            String cycle=treePhenology.fruitCycles.get(tree.material),key=InventoryStore.treeKey(world.id,tree.column,tree.row,tree.material);
            if(cycle!=null)try {if(inventoryStore().wasHarvested(key,cycle))harvested.add(key);}catch(JSONException | ClassCastException e){harvested.add(key);}
        }
        skyView.setHarvestedTrees(harvested);
    }
    private String fruitName(String species) {
        switch(species){case "apple":return "Apel";case "pear":return "Pir";case "orange":return "Jeruk";case "mango":return "Mangga";case "guava":return "Jambu biji";case "starfruit":return "Belimbing";case "banana":return "Pisang";case "palm":return "Kelapa";default:return null;}
    }
    private void interactTree(WorldStore.SurfaceObject tree) {
        try {
            WorldStore.World world=worldStore.activeWorld();if(world==null || !world.showCharacter){Toast.makeText(this,"Karakter tidak tersedia.",Toast.LENGTH_SHORT).show();return;}
            String stage=skyView.treeStage(tree.material),cycle=treePhenology==null?null:treePhenology.fruitCycles.get(tree.material),name=fruitName(tree.material);
            if(!stage.equals("ripe") || cycle==null || name==null){String label=stage.equals("flower")?"berbunga":stage.equals("fruit")?"buah muda":stage.equals("vegetative")?"belum berbuah":"belum diketahui (riwayat cuaca tidak tersedia)";Toast.makeText(this,"Tahap pohon: "+label+".",Toast.LENGTH_LONG).show();return;}
            String key=InventoryStore.treeKey(world.id,tree.column,tree.row,tree.material);
            if(inventoryStore().wasHarvested(key,cycle)){Toast.makeText(this,"Buah siklus ini sudah dipanen.",Toast.LENGTH_SHORT).show();return;}
            int size=(tree.material.equals("banana") || tree.material.equals("palm"))?1:2;
            int dx=Math.max(Math.max(tree.column-world.characterColumn,0),world.characterColumn-(tree.column+size-1));
            int dy=Math.max(Math.max(tree.row-world.characterRow,0),world.characterRow-(tree.row+size-1));
            if(dx+dy!=1){walkNearObject(world,tree,size,size,"Ketuk pohon lagi untuk panen.");return;}
            if(!inventoryStore().harvest(key,cycle,name))throw new JSONException("Save failed");
            refreshHarvestDisplay(world);Toast.makeText(this,"Panen berhasil: 1 "+name+" masuk ke tas.",Toast.LENGTH_LONG).show();
        } catch(JSONException | ClassCastException e) {Toast.makeText(this,"Panen gagal. Periksa tas dan penyimpanan.",Toast.LENGTH_LONG).show();}
    }
    private void approachBox(WorldStore.SurfaceObject box) {
        try {
            WorldStore.World world=worldStore.activeWorld();
            if(world==null || !world.showCharacter){Toast.makeText(this,"Karakter tidak tersedia.",Toast.LENGTH_SHORT).show();return;}
            if(Math.abs(world.characterColumn-box.column)+Math.abs(world.characterRow-box.row)==1){Toast.makeText(this,"Ketuk tanda seru untuk membuka box.",Toast.LENGTH_SHORT).show();return;}
            walkNearObject(world,box,1,1,"Ketuk tanda seru untuk membuka box.");
        } catch(JSONException e){Toast.makeText(this,"World tidak dapat dibaca.",Toast.LENGTH_SHORT).show();}
    }
    private void approachHouse(WorldStore.House house) {
        try {
            WorldStore.World world=worldStore.activeWorld();
            if(world==null || !world.showCharacter){Toast.makeText(this,"Karakter tidak tersedia.",Toast.LENGTH_SHORT).show();return;}
            HouseFootprint footprint=HouseFootprint.of(house.design);
            java.util.List<PortalTravel.Tile> route=PortalTravel.pathBeside(new PortalTravel.Tile(world.characterColumn,world.characterRow),house.column,house.row,footprint.columns,footprint.rows,portalTerrain(world),world.gridSize());
            if(route==null){Toast.makeText(this,"Jalan menuju rumah terhalang.",Toast.LENGTH_SHORT).show();return;}
            String message="Di dekat rumah. Interaksi rumah belum tersedia.";
            if(route.size()==1){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();return;}
            int generation=++portalWalkGeneration;skyView.setPortalWalkActive(true);skyView.setGameplayRoute(route,1);walkNearObjectStep(world,route,1,generation,world.characterFacing,message);
        } catch(JSONException error){Toast.makeText(this,"World tidak dapat dibaca.",Toast.LENGTH_SHORT).show();}
    }
    private void walkNearObject(WorldStore.World world,WorldStore.SurfaceObject object,int width,int height,String prompt) {
        PortalTravel.Terrain terrain=portalTerrain(world);PortalTravel.Tile start=new PortalTravel.Tile(world.characterColumn,world.characterRow);
        java.util.List<PortalTravel.Tile> route=PortalTravel.pathBeside(start,object.column,object.row,width,height,terrain,world.gridSize());
        if(route==null){Toast.makeText(this,"Jalan menuju objek terhalang.",Toast.LENGTH_SHORT).show();return;}
        int generation=++portalWalkGeneration;skyView.setPortalWalkActive(true);skyView.setGameplayRoute(route,1);walkNearObjectStep(world,route,1,generation,world.characterFacing,prompt);
    }
    private void walkNearObjectStep(WorldStore.World world,java.util.List<PortalTravel.Tile> route,int index,int generation,String facing,String prompt) {
        if(generation!=portalWalkGeneration || isFinishing() || isDestroyed() || !world.id.equals(worldStore.activeId()))return;
        if(index>=route.size()) {
            try{PortalTravel.Tile end=route.get(route.size()-1);if(!worldStore.moveCharacter(world.id,end.column,end.row,facing))throw new JSONException("Move failed");skyView.setPortalWalkActive(false);applyActiveWorld();Toast.makeText(this,prompt,Toast.LENGTH_SHORT).show();}
            catch(JSONException e){skyView.setPortalWalkActive(false);applyActiveWorld();Toast.makeText(this,"Karakter tidak dapat dipindahkan.",Toast.LENGTH_SHORT).show();}return;
        }
        skyView.setGameplayRoute(route,index);
        PortalTravel.Tile previous=route.get(index-1),next=route.get(index);String direction=next.column>previous.column?"E":next.column<previous.column?"W":next.row>previous.row?"S":"N";
        int duration=android.animation.ValueAnimator.areAnimatorsEnabled()
                ? CharacterGait.durationMillis(index,route.size()-1) : 1;
        skyView.animateCharacterStep(previous.column,previous.row,next.column,next.row,(float)world.characterScale,direction,index,route.size()-1,duration);
        mainHandler.postDelayed(()->walkNearObjectStep(world,route,index+1,generation,direction,prompt),duration);
    }

    private void walkToPortal(String id) {
        try {
            WorldStore.World source=worldStore.activeWorld(), target=null; PortalTravel.Portal from=null,to=null;
            if(source==null || !source.showCharacter) {Toast.makeText(this,"Karakter tidak tersedia di world ini.",Toast.LENGTH_SHORT).show();return;}
            for(PortalTravel.Portal p:source.portals) if(p.id.equals(id)) from=p;
            if(from!=null) for(WorldStore.World w:worldStore.list()) if(w.id.equals(from.destinationWorldId)) target=w;
            if(target!=null) for(PortalTravel.Portal p:target.portals) if(p.id.equals(from.destinationPortalId)) to=p;
            if(!PortalTravel.arePaired(from,to)) {Toast.makeText(this,"Portal belum memiliki pasangan yang valid.",Toast.LENGTH_SHORT).show();return;}
            final PortalTravel.Portal chosen=from; final WorldStore.World destination=target;
            java.util.List<PortalTravel.Tile> route=PortalTravel.pathToPortal(new PortalTravel.Tile(source.characterColumn,source.characterRow),from,portalTerrain(source));
            if(route==null) {Toast.makeText(this,"Jalan ke depan portal terhalang.",Toast.LENGTH_SHORT).show();return;}
            final int generation=++portalWalkGeneration;
            skyView.setPortalWalkActive(true);skyView.setGameplayRoute(route,1);
            walkPortalStep(source,chosen,destination,route,1,generation,source.characterFacing);
        } catch(JSONException e) {Toast.makeText(this,"Data portal tidak dapat dibaca.",Toast.LENGTH_SHORT).show();}
    }
    private void walkPortalStep(WorldStore.World source, PortalTravel.Portal portal, WorldStore.World destination,
            java.util.List<PortalTravel.Tile> route, int index, int generation, String facing) {
        if(generation!=portalWalkGeneration || isFinishing() || isDestroyed() || !source.id.equals(worldStore.activeId())) return;
        if(index>=route.size()) {
            try {
                PortalTravel.Tile end=route.get(route.size()-1);
                if(!worldStore.moveCharacter(source.id,end.column,end.row,portal.edge)) throw new JSONException("Move failed");
                skyView.setPortalWalkActive(false);applyActiveWorld();
                Toast.makeText(this,"Ketuk tanda seru untuk memilih tujuan.",Toast.LENGTH_SHORT).show();
            } catch(JSONException e) {skyView.setPortalWalkActive(false);applyActiveWorld();Toast.makeText(this,"Karakter tidak dapat dipindahkan.",Toast.LENGTH_SHORT).show();}
            return;
        }
        skyView.setGameplayRoute(route,index);
        PortalTravel.Tile previous=route.get(index-1), next=route.get(index);
        String direction=next.column>previous.column ? "E" : next.column<previous.column ? "W" : next.row>previous.row ? "S" : "N";
        int duration=android.animation.ValueAnimator.areAnimatorsEnabled()
                ? CharacterGait.durationMillis(index,route.size()-1) : 1;
        skyView.animateCharacterStep(previous.column,previous.row,next.column,next.row,(float)source.characterScale,direction,index,route.size()-1,duration);
        mainHandler.postDelayed(()->walkPortalStep(source,portal,destination,route,index+1,generation,direction),duration);
    }

    private void showSellingPlaceholder(WorldStore.SurfaceObject box) {
        String worldId=worldStore.activeId();int generation=portalWalkGeneration;skyView.setSellingOpen(box,true);
        mainHandler.postDelayed(()->{
            if(isFinishing() || isDestroyed() || !worldId.equals(worldStore.activeId())) return;
            if(generation!=portalWalkGeneration) {skyView.setSellingOpen(box,false);return;}
            LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(18),dp(18),dp(18),dp(18));panel.setBackgroundResource(R.drawable.bg_portal_dialog);
            TextView title=new TextView(this);title.setText(box.type.equals("selling-box")?"Selling box":"Storage box");title.setTextColor(Color.rgb(52,43,30));title.setTextSize(12);title.setTypeface(getResources().getFont(R.font.press_start_2p));panel.addView(title);
            if(box.type.equals("selling-box")) {
                TextView content=new TextView(this);content.setText("Penjualan dan saldo belum tersedia. Tidak ada barang yang dipindahkan.");content.setTextColor(Color.rgb(52,43,30));content.setPadding(0,dp(12),0,dp(12));panel.addView(content);
            } else {
                TextView notice=new TextView(this);notice.setTextColor(Color.rgb(52,43,30));panel.addView(notice);
                TextView hint=new TextView(this);hint.setText("Pindahkan satu barang. Kosongkan box sebelum mengganti world. Isi tersimpan hanya di perangkat ini.");panel.addView(hint);
                ScrollView scroll=new ScrollView(this);LinearLayout lists=new LinearLayout(this);lists.setOrientation(LinearLayout.VERTICAL);scroll.addView(lists);panel.addView(scroll,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(280)));
                renderStorageLists(lists,notice,InventoryStore.boxKey(worldId,box.column,box.row));
            }
            AlertDialog dialog=new AlertDialog.Builder(this).setView(panel).setPositiveButton("Tutup",(d,which)->d.dismiss()).create();dialog.setOnDismissListener(d->{if(worldId.equals(worldStore.activeId()))skyView.setSellingOpen(box,false);});dialog.show();
        },260);
    }

    private void renderStorageLists(LinearLayout lists,TextView notice,String key) {
        lists.removeAllViews();
        try {
            InventoryStore inventory=inventoryStore();JSONArray bag=inventory.bag(),box=inventory.box(key);
            for(int side=0;side<2;side++) {
                final boolean toBox=side==0;JSONArray items=toBox?bag:box;
                TextView title=new TextView(this);title.setText(toBox?"Tas":"Penyimpanan");title.setTextColor(Color.rgb(52,43,30));title.setTextSize(18);lists.addView(title);
                if(items.length()==0){TextView empty=new TextView(this);empty.setText("Kosong");lists.addView(empty);}
                for(int i=0;i<items.length();i++) {
                    JSONObject item=items.getJSONObject(i);String name=item.getString("name");int count=item.getInt("quantity");
                    LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView label=new TextView(this);label.setText(name+" × "+count);label.setTextColor(Color.rgb(52,43,30));row.addView(label,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
                    Button action=new Button(this);action.setText(toBox?"Simpan 1":"Ambil 1");action.setContentDescription((toBox?"Simpan satu ":"Ambil satu ")+name);
                    action.setOnClickListener(v->{try {if(!inventory.transfer(key,name,1,toBox))throw new JSONException("Gagal menyimpan");renderStorageLists(lists,notice,key);notice.setText("Barang dipindahkan.");}catch(JSONException | ClassCastException e){notice.setText("Transfer gagal. Periksa jumlah, batas, dan penyimpanan.");}});
                    row.addView(action);lists.addView(row);
                }
            }
        } catch(JSONException | ClassCastException e) {notice.setText("Data tas atau box tidak dapat dibaca. Data lama tidak ditimpa.");}
    }

    private void confirmPortalMarker(String id) {
        try {
            WorldStore.World source=worldStore.activeWorld(),target=null;PortalTravel.Portal from=null,to=null;
            if(source==null || !source.showCharacter)return;
            for(PortalTravel.Portal p:source.portals) if(p.id.equals(id))from=p;
            if(from==null || source.characterColumn!=from.tile.column || source.characterRow!=from.tile.row || !source.characterFacing.equals(from.edge)) return;
            for(WorldStore.World w:worldStore.list()) if(w.id.equals(from.destinationWorldId))target=w;
            if(target!=null)for(PortalTravel.Portal p:target.portals)if(p.id.equals(from.destinationPortalId))to=p;
            if(!PortalTravel.arePaired(from,to)){new AlertDialog.Builder(this).setMessage("Portal belum memiliki pasangan yang valid. Impor kedua world dan pasangkan portal.").setPositiveButton("Tutup",(dialog,which)->dialog.dismiss()).show();return;}
            showPortalConfirmation(source.id,id,target.name);
        } catch(JSONException e){Toast.makeText(this,"Data portal tidak dapat dibaca.",Toast.LENGTH_SHORT).show();}
    }
    private TextView portalButton(String label) {
        TextView button=new TextView(this);button.setText(label);button.setTextColor(Color.rgb(66,42,28));
        button.setTextSize(10);button.setTypeface(getResources().getFont(R.font.press_start_2p));
        button.setGravity(Gravity.CENTER);button.setPadding(dp(14),dp(10),dp(14),dp(10));
        button.setBackgroundResource(R.drawable.bg_portal_button);button.setClickable(true);button.setFocusable(true);
        return button;
    }
    private void showPortalConfirmation(String sourceId,String portalId,String destinationName) {
        android.widget.FrameLayout container=new android.widget.FrameLayout(this);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setGravity(Gravity.TOP);
        panel.setPadding(dp(18),dp(32),dp(18),dp(12));panel.setBackgroundResource(R.drawable.bg_portal_dialog);
        android.widget.FrameLayout.LayoutParams panelParams=new android.widget.FrameLayout.LayoutParams(-1,-2);panelParams.topMargin=dp(38);container.addView(panel,panelParams);
        ImageView portrait=new ImageView(this);portrait.setImageResource(R.drawable.character_original_uhd_front);portrait.setBackgroundResource(R.drawable.bg_character_hud);portrait.setPadding(dp(3),dp(3),dp(3),dp(3));portrait.setScaleType(ImageView.ScaleType.FIT_CENTER);portrait.setScaleX(-1f);
        android.widget.FrameLayout.LayoutParams portraitParams=new android.widget.FrameLayout.LayoutParams(dp(62),dp(62),Gravity.TOP|Gravity.LEFT);portraitParams.leftMargin=dp(12);container.addView(portrait,portraitParams);
        TextView name=new TextView(this);name.setText("Portal");name.setTextColor(Color.rgb(52,43,30));name.setBackgroundResource(R.drawable.bg_portal_button);name.setTypeface(getResources().getFont(R.font.press_start_2p));name.setTextSize(9);name.setPadding(dp(6),dp(5),dp(6),dp(5));
        android.widget.FrameLayout.LayoutParams nameParams=new android.widget.FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.LEFT);nameParams.leftMargin=dp(79);nameParams.topMargin=dp(18);container.addView(name,nameParams);
        TextView prompt=new TextView(this);prompt.setText("Pergi ke world " + destinationName + "?");prompt.setTextColor(Color.rgb(66,42,28));
        prompt.setTextSize(11);prompt.setTypeface(getResources().getFont(R.font.press_start_2p));prompt.setGravity(Gravity.START);prompt.setLineSpacing(dp(4),1f);
        panel.addView(prompt,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.HORIZONTAL);buttons.setGravity(Gravity.END);
        LinearLayout.LayoutParams row=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);row.topMargin=dp(16);panel.addView(buttons,row);
        TextView yes=portalButton("Ya"),no=portalButton("Tidak");
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);left.setMargins(dp(5),0,dp(5),0);
        LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);right.setMargins(dp(5),0,dp(5),0);
        buttons.addView(yes,left);buttons.addView(no,right);
        AlertDialog dialog=new AlertDialog.Builder(this).setView(container).create();
        yes.setOnClickListener(v->{dialog.dismiss();try {
            WorldStore.World current=worldStore.activeWorld();PortalTravel.Portal currentPortal=null;
            if(current!=null)for(PortalTravel.Portal p:current.portals)if(p.id.equals(portalId))currentPortal=p;
            if(current==null || !current.id.equals(sourceId) || currentPortal==null || current.characterColumn!=currentPortal.tile.column || current.characterRow!=currentPortal.tile.row || !current.characterFacing.equals(currentPortal.edge)) {Toast.makeText(this,"Karakter tidak lagi menghadap portal.",Toast.LENGTH_SHORT).show();return;}
            if(!worldStore.travel(sourceId,portalId)) Toast.makeText(this,"Portal tidak tersedia atau tujuan terhalang.",Toast.LENGTH_LONG).show(); else applyActiveWorld();
        } catch(JSONException e){Toast.makeText(this,"Perpindahan gagal; data tidak diubah.",Toast.LENGTH_LONG).show();}});
        no.setOnClickListener(v->dialog.dismiss());
        dialog.setOnShowListener(ignored->{if(dialog.getWindow()!=null){
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(.12f);
            dialog.getWindow().setLayout(Math.min(dp(520),getResources().getDisplayMetrics().widthPixels-dp(32)),android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }});
        dialog.show();
    }

    private void confirmWorldImport(WorldStore.World world) {
        try {
            WorldStore.World existing = null;
            for (WorldStore.World item : worldStore.list()) {
                if (item.id.equals(world.id)) { existing = item; break; }
            }
            if (existing == null) { saveImportedWorld(world); return; }
            new AlertDialog.Builder(this).setTitle("Ganti world?")
                    .setMessage("World dengan ID yang sama sudah ada: " + existing.name
                            + ". Ganti isinya dengan data dari berkas?")
                    .setPositiveButton("Ganti", (dialog, which) -> saveImportedWorld(world))
                    .setNegativeButton("Batal", null).show();
        } catch (JSONException error) {
            Toast.makeText(this, "Data world tidak dapat dibaca; data lama tidak ditimpa.", Toast.LENGTH_LONG).show();
        }
    }

    private void saveImportedWorld(WorldStore.World world) {
        try {
            if (!worldStore.upsertAndSelect(world)) throw new IllegalStateException("save failed");
            applyActiveWorld();
            closeDrawer();
            Toast.makeText(this, "World " + world.name + " disimpan dan dibuka.", Toast.LENGTH_LONG).show();
        } catch (JSONException | IllegalStateException error) {
            Toast.makeText(this, "World tidak dapat disimpan; data lama tidak ditimpa.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION_REQUEST) return;
        permissionRequestPending = false;
        boolean preciseLocationAllowed = hasPreciseLocationPermission();
        precisePermissionKnown = true;
        lastPrecisePermission = preciseLocationAllowed;
        if (preciseLocationAllowed) {
            lastWeatherAttemptMillis = 0;
            resolveGpsLocation();
        } else {
            useFallbackLocationForDisplay();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerScreen != null && drawerScreen.getVisibility() == View.VISIBLE) {
            closeDrawer();
        }
    }

    /** Keeps the homescreen immersive so no system clock or navigation pill is shown. */
    private void configureSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            android.view.WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(android.view.WindowInsets.Type.statusBars()
                        | android.view.WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            int flags = View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            configureSystemBars();
        }
    }

    private void configureWeatherAttribution() {
        if (weatherSourceView != null) {
            weatherSourceView.setText(R.string.weather_source_credit);
            weatherSourceView.setPaintFlags(
                    weatherSourceView.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            weatherSourceView.setOnClickListener(view ->
                    openExternalPage("https://open-meteo.com/en/docs"));
        }
        if (weatherLicenseView != null) {
            weatherLicenseView.setText(R.string.weather_license_credit);
            weatherLicenseView.setPaintFlags(
                    weatherLicenseView.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            weatherLicenseView.setOnClickListener(view ->
                    openExternalPage("https://creativecommons.org/licenses/by/4.0/"));
        }
    }

    private void openExternalPage(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException | SecurityException ignored) {
            // The launcher remains usable even if no browser is available.
        }
    }

    private void startWeatherFlow() {
        if (weatherFlowStarted) return;
        weatherFlowStarted = true;
        boolean preciseLocationAllowed = hasPreciseLocationPermission();
        precisePermissionKnown = true;
        lastPrecisePermission = preciseLocationAllowed;
        restoreWeatherCache(preciseLocationAllowed);

        if (preciseLocationAllowed) {
            refreshWeatherIfNeeded(true);
            return;
        }

        fetchWeather(FALLBACK_LATITUDE, FALLBACK_LONGITUDE, true);
        SharedPreferences settings = getSharedPreferences(WEATHER_SETTINGS_PREFS, MODE_PRIVATE);
        if (!settings.getBoolean("location_permission_explanation_shown", false)) {
            settings.edit().putBoolean("location_permission_explanation_shown", true).apply();
            showLocationPermissionExplanation();
        }
    }

    private void showLocationPermissionExplanation() {
        if (isFinishing() || isDestroyed() || permissionRequestPending) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.location_permission_title)
                .setMessage(R.string.location_permission_explanation)
                .setPositiveButton(R.string.location_permission_continue, (dialog, which) -> requestLocationPermissionOrSettings())
                .setNegativeButton(R.string.location_permission_later, null)
                .show();
    }

    private void requestLocationPermissionOrSettings() {
        if (permissionRequestPending) return;
        SharedPreferences settings = getSharedPreferences(WEATHER_SETTINGS_PREFS, MODE_PRIVATE);
        boolean requested = settings.getBoolean("location_permission_requested", false);
        // After a permanent denial or an approximate-only grant, Android may no longer
        // show a permission dialog. Offer app settings instead of silently failing.
        if (hasPreciseLocationPermission() || (requested && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION))) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            try { startActivity(intent); }
            catch (ActivityNotFoundException | SecurityException error) {
                Toast.makeText(this, R.string.location_permission_settings_unavailable, Toast.LENGTH_LONG).show();
            }
            return;
        }
        settings.edit().putBoolean("location_permission_requested", true).apply();
        permissionRequestPending = true;
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST);
    }

    private boolean hasPreciseLocationPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void refreshWeatherIfNeeded(boolean force) {
        if (permissionRequestPending) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastWeatherAttemptMillis < WEATHER_REFRESH_INTERVAL_MS) return;
        lastWeatherAttemptMillis = now;
        if (hasPreciseLocationPermission()) resolveGpsLocation();
        else fetchWeather(FALLBACK_LATITUDE, FALLBACK_LONGITUDE, true);
    }

    private void resolveGpsLocation() {
        cancelPendingGpsRequest();
        lastWeatherAttemptMillis = System.currentTimeMillis();
        if (!hasPreciseLocationPermission()) {
            fetchWeather(FALLBACK_LATITUDE, FALLBACK_LONGITUDE, true);
            return;
        }

        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) {
            fetchWeather(FALLBACK_LATITUDE, FALLBACK_LONGITUDE, true);
            return;
        }

        final int requestId = ++locationRequestGeneration;
        final AtomicBoolean completed = new AtomicBoolean(false);
        final LocationListener[] listenerRef = new LocationListener[1];
        final CancellationSignal cancellationSignal = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? new CancellationSignal() : null;
        final Runnable timeout = () -> finishGpsRequest(
                requestId, completed, null, null, manager, listenerRef[0], cancellationSignal);
        activeLocationRequestId = requestId;
        activeLocationManager = manager;
        activeLocationListener = null;
        activeGpsCancellationSignal = cancellationSignal;
        activeGpsTimeout = timeout;
        mainHandler.postDelayed(timeout, GPS_TIMEOUT_MS);

        try {
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                finishGpsRequest(requestId, completed, timeout, null, manager, null, cancellationSignal);
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manager.getCurrentLocation(LocationManager.GPS_PROVIDER, cancellationSignal, getMainExecutor(), location ->
                        finishGpsRequest(requestId, completed, timeout, location, manager, null, cancellationSignal));
            } else {
                listenerRef[0] = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        finishGpsRequest(requestId, completed, timeout, location, manager, listenerRef[0], cancellationSignal);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        // The timeout below handles unavailable GPS fixes.
                    }

                    @Override
                    public void onProviderEnabled(String provider) { }

                    @Override
                    public void onProviderDisabled(String provider) {
                        finishGpsRequest(requestId, completed, timeout, null, manager, listenerRef[0], cancellationSignal);
                    }
                };
                activeLocationListener = listenerRef[0];
                manager.requestSingleUpdate(
                        LocationManager.GPS_PROVIDER, listenerRef[0], Looper.getMainLooper());
            }
        } catch (SecurityException | IllegalArgumentException exception) {
            finishGpsRequest(requestId, completed, timeout, null, manager, listenerRef[0], cancellationSignal);
        }
    }

    private void cancelPendingGpsRequest() {
        if (activeLocationRequestId < 0) return;
        locationRequestGeneration++;
        if (activeGpsTimeout != null) mainHandler.removeCallbacks(activeGpsTimeout);
        if (activeGpsCancellationSignal != null) activeGpsCancellationSignal.cancel();
        if (activeLocationManager != null && activeLocationListener != null) {
            try {
                activeLocationManager.removeUpdates(activeLocationListener);
            } catch (SecurityException ignored) {
                // Permission may have been revoked while a GPS fix was pending.
            }
        }
        activeLocationRequestId = -1;
        activeLocationManager = null;
        activeLocationListener = null;
        activeGpsCancellationSignal = null;
        activeGpsTimeout = null;
    }

    private void finishGpsRequest(
            int requestId,
            AtomicBoolean completed,
            Runnable timeout,
            Location location,
            LocationManager manager,
            LocationListener listener,
            CancellationSignal cancellationSignal) {
        if (!completed.compareAndSet(false, true)) return;
        if (cancellationSignal != null) cancellationSignal.cancel();
        if (timeout != null) mainHandler.removeCallbacks(timeout);
        if (manager != null && listener != null) {
            try {
                manager.removeUpdates(listener);
            } catch (SecurityException ignored) {
                // Permission may have been revoked while a GPS fix was pending.
            }
        }
        if (activeLocationRequestId == requestId) {
            activeLocationRequestId = -1;
            activeLocationManager = null;
            activeLocationListener = null;
            activeGpsCancellationSignal = null;
            activeGpsTimeout = null;
        }
        if (requestId != locationRequestGeneration || isFinishing() || isDestroyed()) return;
        if (location != null && hasPreciseLocationPermission()) {
            currentWeather = null;
            currentLatitude = location.getLatitude();
            currentLongitude = location.getLongitude();
            updateDayNightIcon();
            updateSeason(currentLatitude, currentLongitude, false);
            if (skyView != null) skyView.setLocation(currentLatitude, currentLongitude);
            fetchWeather(currentLatitude, currentLongitude, false);
        } else {
            fetchWeather(FALLBACK_LATITUDE, FALLBACK_LONGITUDE, true);
        }
    }

    private void clearSeason() {
        seasonRequestGeneration++;
        seasonLastAttempt = 0;
        seasonLatitude = seasonLongitude = Double.NaN;
        if (seasonIcon != null) {
            seasonIcon.setImageResource(R.drawable.season_unknown);
            seasonIcon.setContentDescription(getString(R.string.season_unknown));
        }
    }

    private void updateTreeSeasons(double latitude,double longitude,boolean fallback) {
        boolean changed=!Double.isFinite(treeLatitude) || treeFallback!=fallback || Math.abs(latitude-treeLatitude)>.0005 || Math.abs(longitude-treeLongitude)>.0005;
        if(!changed && System.currentTimeMillis()-treeLastAttempt<6L*60*60*1000)return;
        int request=++treeRequestGeneration;treeLatitude=latitude;treeLongitude=longitude;treeFallback=fallback;treeLastAttempt=System.currentTimeMillis();
        treePhenology=null;skyView.setTreePhenology(null);refreshHarvestDisplay();
        treeLoader.execute(()->{
            TreePhenology.Result result;try{result=TreePhenology.fetch(latitude,longitude);}catch(Exception e){result=null;}
            final TreePhenology.Result value=result;
            runOnUiThread(()->{
                if(request!=treeRequestGeneration || isFinishing() || isDestroyed() || (!fallback && !hasPreciseLocationPermission()))return;
                // On failure retry at the next regular weather refresh, not in a tight loop.
                if(value==null)treeLastAttempt=System.currentTimeMillis()-6L*60*60*1000+WEATHER_REFRESH_INTERVAL_MS;
                treePhenology=value;skyView.setTreePhenology(value);refreshHarvestDisplay();
            });
        });
    }

    private void updateSeason(double latitude, double longitude, boolean fallback) {
        updateTreeSeasons(latitude,longitude,fallback);
        boolean changed = !Double.isFinite(seasonLatitude) || seasonFallback != fallback || Math.abs(latitude-seasonLatitude)>0.02 || Math.abs(longitude-seasonLongitude)>0.02;
        if (!changed && System.currentTimeMillis()-seasonLastAttempt < 6L*60*60*1000) return;
        clearSeason(); seasonLatitude=latitude; seasonLongitude=longitude; seasonFallback=fallback; seasonLastAttempt=System.currentTimeMillis();
        final int request=++seasonRequestGeneration;
        weatherLoader.execute(() -> {
            String result;
            try { result=SeasonEstimate.fetch(latitude, longitude); } catch (Exception error) { result="unknown"; }
            final String value=result;
            runOnUiThread(() -> {
                if (request!=seasonRequestGeneration || isFinishing() || isDestroyed() || (!fallback && !hasPreciseLocationPermission())) return;
                if ("unknown".equals(value)) seasonLastAttempt=0;
                int image="wet".equals(value)?R.drawable.season_wet:"dry".equals(value)?R.drawable.season_dry:R.drawable.season_unknown;
                int label="wet".equals(value)?R.string.season_wet:"dry".equals(value)?R.string.season_dry:R.string.season_unknown;
                seasonIcon.setImageResource(image); seasonIcon.setContentDescription(getString(label) + (fallback ? getString(R.string.season_fallback_suffix) : getString(R.string.season_device_suffix)));
            });
        });
    }

    private void fetchWeather(double latitude, double longitude, boolean fallbackLocation) {
        if (fallbackLocation) {
            updateSeason(FALLBACK_LATITUDE, FALLBACK_LONGITUDE, true);
            useFallbackLocationForDisplay();
        }
        final int generation = ++weatherRequestGeneration;
        lastWeatherAttemptMillis = System.currentTimeMillis();
        weatherLoader.execute(() -> {
            WeatherSnapshot result = null;
            try {
                result = OpenMeteoClient.fetch(latitude, longitude, fallbackLocation);
            } catch (Exception ignored) {
                // Keep the last cached snapshot; the renderer has a time-based sky fallback.
            }
            final WeatherSnapshot received = result;
            mainHandler.post(() -> {
                if (generation != weatherRequestGeneration || isFinishing() || isDestroyed()) return;
                if (received == null) {
                    expireWeatherCacheIfNeeded();
                    return;
                }
                currentWeather = received;
                currentLatitude = received.latitude;
                currentLongitude = received.longitude;
                updateDayNightIcon();
                if (skyView != null) skyView.setWeatherSnapshot(received);
                cacheWeatherSnapshot(received);
            });
        });
    }

    private void useFallbackLocationForDisplay() {
        if (currentWeather != null && currentWeather.fallbackLocation) {
            currentLatitude = currentWeather.latitude;
            currentLongitude = currentWeather.longitude;
            updateDayNightIcon();
            if (skyView != null) skyView.setWeatherSnapshot(currentWeather);
            return;
        }
        clearWeatherSnapshotAt(FALLBACK_LATITUDE, FALLBACK_LONGITUDE);
    }

    private void discardPreciseWeatherIfPermissionRevoked() {
        if (hasPreciseLocationPermission()) return;
        updateSeason(FALLBACK_LATITUDE, FALLBACK_LONGITUDE, true);
        boolean hasPreciseSnapshot = currentWeather != null && !currentWeather.fallbackLocation;
        boolean awayFromFallback = Math.abs(currentLatitude - FALLBACK_LATITUDE) > 0.001
                || Math.abs(currentLongitude - FALLBACK_LONGITUDE) > 0.001;
        if (hasPreciseSnapshot || awayFromFallback) {
            lastWeatherAttemptMillis = 0L;
            clearWeatherSnapshotAt(FALLBACK_LATITUDE, FALLBACK_LONGITUDE);
        }
    }

    private void expireWeatherCacheIfNeeded() {
        if (currentWeather == null
                || System.currentTimeMillis() - currentWeather.fetchedAtMillis <= WEATHER_CACHE_MAX_AGE_MS) {
            return;
        }
        if (currentWeather.fallbackLocation || !hasPreciseLocationPermission()) {
            clearWeatherSnapshotAt(FALLBACK_LATITUDE, FALLBACK_LONGITUDE);
        } else {
            clearWeatherSnapshotAt(currentWeather.latitude, currentWeather.longitude);
        }
    }

    private void clearWeatherSnapshotAt(double latitude, double longitude) {
        currentWeather = null;
        currentLatitude = latitude;
        currentLongitude = longitude;
        updateDayNightIcon();
        getSharedPreferences("weather_cache", MODE_PRIVATE).edit().clear().apply();
        if (skyView != null) skyView.setLocation(latitude, longitude);
    }

    private void restoreWeatherCache(boolean preciseLocationAllowed) {
        SharedPreferences preferences = getSharedPreferences("weather_cache", MODE_PRIVATE);
        if (!preferences.contains("fetched_at")) return;
        boolean fallback = preferences.getBoolean("fallback", true);
        if (!fallback && !preciseLocationAllowed) {
            preferences.edit().clear().apply();
            return;
        }

        long fetchedAt = preferences.getLong("fetched_at", 0L);
        if (fetchedAt <= 0L || System.currentTimeMillis() - fetchedAt > WEATHER_CACHE_MAX_AGE_MS) {
            preferences.edit().clear().apply();
            return;
        }
        WeatherSnapshot cached = new WeatherSnapshot(
                preferences.getInt("weather_code", 3),
                preferences.getFloat("cloud_cover", 45.0f),
                preferences.getFloat("precipitation", 0.0f),
                preferences.getFloat("rain", 0.0f),
                preferences.getFloat("wind_speed", 4.0f),
                preferences.getFloat("wind_direction", 90.0f),
                preferences.getFloat("latitude", (float) FALLBACK_LATITUDE),
                preferences.getFloat("longitude", (float) FALLBACK_LONGITUDE),
                fetchedAt,
                fallback);
        currentWeather = cached;
        currentLatitude = cached.latitude;
        currentLongitude = cached.longitude;
        updateDayNightIcon();
        if (skyView != null) skyView.setWeatherSnapshot(cached);
    }

    private void cacheWeatherSnapshot(WeatherSnapshot snapshot) {
        getSharedPreferences("weather_cache", MODE_PRIVATE).edit()
                .putInt("weather_code", snapshot.weatherCode)
                .putFloat("cloud_cover", (float) snapshot.cloudCoverPercent)
                .putFloat("precipitation", (float) snapshot.precipitationMm)
                .putFloat("rain", (float) snapshot.rainMm)
                .putFloat("wind_speed", (float) snapshot.windSpeedKmh)
                .putFloat("wind_direction", (float) snapshot.windDirectionDegrees)
                .putFloat("latitude", (float) snapshot.latitude)
                .putFloat("longitude", (float) snapshot.longitude)
                .putLong("fetched_at", snapshot.fetchedAtMillis)
                .putBoolean("fallback", snapshot.fallbackLocation)
                .apply();
    }

    private void registerOrientationSensor() {
        if (sensorManager == null) {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager == null) return;
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }
        if (rotationSensor != null) {
            sensorManager.registerListener(orientationListener, rotationSensor, SensorManager.SENSOR_DELAY_UI);
        } else if (skyView != null) {
            skyView.setHeadingDegrees(0.0f);
        }
    }

    private void updateDefaultLauncherButton() {
        if (defaultHomeButton == null) {
            return;
        }
        boolean isDefault = isDefaultLauncher();
        defaultHomeButton.setText(isDefault
                ? R.string.home_badge_default
                : R.string.home_badge_set_default);
        defaultHomeButton.setContentDescription(getString(isDefault
                ? R.string.home_default_accessibility
                : R.string.home_set_default_accessibility));
        defaultHomeButton.setEnabled(!isDefault);
        defaultHomeButton.setAlpha(isDefault ? 0.75f : 1.0f);
    }

    private boolean isDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME);
            }
        }

        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo currentHome = getPackageManager().resolveActivity(
                homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
        return currentHome != null
                && currentHome.activityInfo != null
                && getPackageName().equals(currentHome.activityInfo.packageName);
    }

    private void requestDefaultLauncher() {
        if (isDefaultLauncher()) {
            updateDefaultLauncherButton();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                try {
                    startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME));
                    return;
                } catch (ActivityNotFoundException | SecurityException ignored) {
                    // Fall back to the device's Home-app settings.
                }
            }
        }
        openHomeSettings();
    }

    private void openHomeSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (ActivityNotFoundException | SecurityException noHomeSettings) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
            } catch (ActivityNotFoundException | SecurityException noDefaultSettings) {
                Toast.makeText(this, R.string.home_settings_unavailable, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void refreshApps() {
        final int generation = ++loadGeneration;
        appLoader.execute(() -> {
            List<AppEntry> loadedApps = queryLaunchableApps();
            mainHandler.post(() -> {
                if (generation != loadGeneration || isFinishing() || isDestroyed()) {
                    return;
                }
                apps.clear();
                apps.addAll(loadedApps);
                CharSequence query = searchView.getText();
                renderApps(query == null ? "" : query.toString());
            });
        });
    }

    private List<AppEntry> queryLaunchableApps() {
        PackageManager packageManager = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> activities;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activities = packageManager.queryIntentActivities(
                    query, PackageManager.ResolveInfoFlags.of(0));
        } else {
            activities = packageManager.queryIntentActivities(query, 0);
        }

        List<AppEntry> result = new ArrayList<>();
        Set<String> seenComponents = new HashSet<>();
        for (ResolveInfo resolveInfo : activities) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo == null || getPackageName().equals(activityInfo.packageName)) {
                continue;
            }

            String componentKey = activityInfo.packageName + "/" + activityInfo.name;
            if (!seenComponents.add(componentKey)) {
                continue;
            }

            try {
                CharSequence label = resolveInfo.loadLabel(packageManager);
                String appLabel = label == null ? activityInfo.packageName : label.toString();
                Drawable icon = pixelArtIcon(resolveInfo.loadIcon(packageManager));
                result.add(new AppEntry(appLabel, activityInfo.packageName, activityInfo.name, icon));
            } catch (RuntimeException ignored) {
                // An app can be removed while the launcher is enumerating its activities.
            }
        }

        Collator collator = Collator.getInstance(Locale.getDefault());
        result.sort((first, second) -> collator.compare(first.label, second.label));
        return result;
    }

    private void renderApps(String rawQuery) {
        if (appGrid == null) {
            return;
        }
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.getDefault());
        List<AppEntry> visibleApps = new ArrayList<>();
        for (AppEntry app : apps) {
            if (query.isEmpty() || app.label.toLowerCase(Locale.getDefault()).contains(query)) {
                visibleApps.add(app);
            }
        }

        appCountView.setText(getString(R.string.app_count, visibleApps.size()));
        emptyView.setText(query.isEmpty() ? R.string.empty_apps : R.string.empty_search);
        emptyView.setVisibility(visibleApps.isEmpty() ? View.VISIBLE : View.GONE);
        appGrid.removeAllViews();

        int availableWidth = availableGridWidth(appGrid);
        int columns = Math.max(3, Math.min(8, widthDp(availableWidth) / 110));
        appGrid.setColumnCount(columns);
        int cellWidth = Math.max(1, availableWidth / columns);
        for (AppEntry app : visibleApps) {
            appGrid.addView(createDrawerItem(app, cellWidth));
        }
    }

    private View createDrawerItem(AppEntry app, int cellWidth) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(5), dp(8), dp(5), dp(8));
        item.setBackgroundResource(android.R.drawable.list_selector_background);
        item.setContentDescription(getString(R.string.open_app_accessibility, app.label));

        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
        layoutParams.width = cellWidth;
        layoutParams.height = dp(88);
        item.setLayoutParams(layoutParams);

        ImageView iconView = new ImageView(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        iconView.setLayoutParams(iconParams);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconView.setImageDrawable(app.icon);
        iconView.setBackgroundResource(R.drawable.bg_pixel_app_icon);
        int iconInset = dp(5);
        iconView.setPadding(iconInset, iconInset, iconInset, iconInset);
        iconView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        item.addView(iconView);

        TextView label = new TextView(this);
        label.setText(app.label);
        label.setTextColor(Color.rgb(52, 43, 30));
        label.setTypeface(getResources().getFont(R.font.press_start_2p));
        label.setTextSize(8);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setIncludeFontPadding(false);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(8);
        label.setLayoutParams(labelParams);
        item.addView(label);

        item.setOnClickListener(view -> launchApp(app));
        item.setOnLongClickListener(view -> {
            openAppInfo(app.packageName);
            return true;
        });
        return item;
    }

    private void openDrawer() {
        drawerScreen.setVisibility(View.VISIBLE);
        searchView.setText("");
        searchView.clearFocus();
        renderApps("");
        appGrid.post(() -> { if (drawerScreen.getVisibility() == View.VISIBLE) renderApps(searchView.getText().toString()); });
    }

    private void closeDrawer() {
        hideKeyboard();
        searchView.setText("");
        searchView.clearFocus();
        drawerScreen.setVisibility(View.GONE);
    }

    private void launchApp(AppEntry app) {
        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setComponent(new ComponentName(app.packageName, app.activityName));
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(launchIntent);
        } catch (ActivityNotFoundException | SecurityException exception) {
            Toast.makeText(this, R.string.app_launch_failed, Toast.LENGTH_SHORT).show();
            refreshApps();
        }
    }

    private void openAppInfo(String packageName) {
        Intent detailsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        detailsIntent.setData(Uri.parse("package:" + packageName));
        try {
            startActivity(detailsIntent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.app_info_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && searchView != null) {
            inputMethodManager.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }
    }

    private int availableGridWidth(GridLayout grid) {
        int availableWidth = grid.getWidth();
        if (availableWidth <= 0) {
            availableWidth = getResources().getDisplayMetrics().widthPixels - dp(48);
        }
        return Math.max(1, availableWidth);
    }

    private int widthDp(int widthPx) {
        return (int) (widthPx / getResources().getDisplayMetrics().density);
    }

    /** Rasterize once while loading apps; nearest-neighbour enlargement keeps the pixels crisp. */
    private Drawable pixelArtIcon(Drawable original) {
        if (original == null) return getDrawable(android.R.drawable.sym_def_app_icon);
        try {
            final int pixels = 24;
            Bitmap bitmap = Bitmap.createBitmap(pixels, pixels, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Rect oldBounds = new Rect(original.getBounds());
            try {
                original.setBounds(0, 0, pixels, pixels);
                original.draw(canvas);
            } finally {
                original.setBounds(oldBounds);
            }
            // A modest color reduction unifies the look without replacing brand colors.
            int[] colors = new int[pixels * pixels];
            bitmap.getPixels(colors, 0, pixels, 0, 0, pixels, pixels);
            for (int i = 0; i < colors.length; i++) {
                int color = colors[i];
                int r = Math.min(255, ((Color.red(color) + 8) / 16) * 16);
                int g = Math.min(255, ((Color.green(color) + 8) / 16) * 16);
                int b = Math.min(255, ((Color.blue(color) + 8) / 16) * 16);
                colors[i] = Color.argb(Color.alpha(color), r, g, b);
            }
            bitmap.setPixels(colors, 0, pixels, 0, 0, pixels, pixels);
            bitmap.setDensity(Bitmap.DENSITY_NONE);
            BitmapDrawable pixelIcon = new BitmapDrawable(getResources(), bitmap);
            pixelIcon.setFilterBitmap(false);
            pixelIcon.setDither(false);
            return pixelIcon;
        } catch (RuntimeException exception) {
            // Unusual or broken third-party drawables must not remove the app from the drawer.
            return original;
        }
    }

    private static final class AppEntry {
        final String label;
        final String packageName;
        final String activityName;
        final Drawable icon;

        AppEntry(String label, String packageName, String activityName, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.activityName = activityName;
            this.icon = icon;
        }
    }
}
