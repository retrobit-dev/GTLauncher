(() => {
  const canvas = document.getElementById('sky-canvas');
  const displayCtx = canvas?.getContext('2d', { alpha: false });
  const renderCanvas = document.createElement('canvas');
  const ctx = renderCanvas.getContext('2d', { alpha: false });
  const weatherModeButton = document.getElementById('weather-cycle-button');
  const weatherModeLabel = document.getElementById('weather-mode-label');
  const islandElevationInput = document.getElementById('island-camera-elevation');
  const islandElevationOutput = document.getElementById('island-camera-elevation-value');
  const islandZoomInput = document.getElementById('island-camera-zoom');
  const islandZoomOutput = document.getElementById('island-camera-zoom-value');
  const islandCameraReset = document.getElementById('island-camera-reset');
  if (!canvas || !ctx || !displayCtx) return;

  const WIDTH = 320;
  const HEIGHT = 200;
  // Preserve the existing pixel-art grid while rendering to the MatePad Mini display resolution.
  const RENDER_SCALE = 2;
  const BACKING_SCALE = 8;
  // Draw the expensive scene at half the device resolution, then upscale once.
  const SCENE_RENDER_SCALE = 4;
  const RENDER_CELL = 1 / RENDER_SCALE;
  canvas.width = WIDTH * BACKING_SCALE;
  canvas.height = HEIGHT * BACKING_SCALE;
  renderCanvas.width = WIDTH * SCENE_RENDER_SCALE;
  renderCanvas.height = HEIGHT * SCENE_RENDER_SCALE;
  ctx.imageSmoothingEnabled = false;
  ctx.scale(SCENE_RENDER_SCALE, SCENE_RENDER_SCALE);
  displayCtx.imageSmoothingEnabled = false;
  // Reference-derived stationary front view; a single logical character shared by worlds.
  // The per-world visibility flag is currently a preview-only setting.
  const grassDetailImage = new Image();
  grassDetailImage.onload = () => { if (REDUCED_MOTION) drawFrame(); };
  grassDetailImage.src = './assets/island-grass-detail.png';
  const playerImage = new Image();
  playerImage.onload = () => { if (REDUCED_MOTION) drawFrame(); };
  playerImage.src = './assets/main-character.png?v=short-stocky-1';
  const playerBackImage = new Image();
  playerBackImage.onload = () => { if (REDUCED_MOTION) drawFrame(); };
  playerBackImage.src = './assets/main-character-back.png?v=short-stocky-1';
  // Full-image gait frames keep hands and feet connected to the body.
  const gaitFrames = Object.fromEntries(['walk','run'].map(mode => [mode,Object.fromEntries(['front','back'].map(side => [side,
    Array.from({length:12},(_,i)=>{const image=new Image();image.src=`./assets/character-${mode}/${side}-${String(i).padStart(2,'0')}.png?v=short-stocky-1`;return image;})]))]));
  const bridgeSprites=Object.fromEntries(['wood','stone'].map(kind=>{const img=new Image();img.onload=()=>{if(REDUCED_MOTION)drawFrame();};img.src=`./assets/bridge-${kind}-reference.png?v=1`;return [kind,img];}));
  const wideBridgeSprites=Object.fromEntries(['wood','stone'].map(kind=>{const img=new Image();img.onload=()=>{if(REDUCED_MOTION)drawFrame();};img.src=`./assets/bridge-${kind}-wide.png?v=1`;return [kind,img];}));
  const houseImage = new Image();
  houseImage.onload = () => { if (REDUCED_MOTION) drawFrame(); };
  houseImage.src = './assets/simple-house-aligned.png?v=1';
  const houseBackImage = new Image();
  houseBackImage.onload = () => { if (REDUCED_MOTION) drawFrame(); };
  houseBackImage.src = './assets/simple-house-back-aligned.png?v=1';
  const orangeTreeImage = new Image();
  orangeTreeImage.onload = () => { if (REDUCED_MOTION) drawFrame(); };
  orangeTreeImage.src = './assets/orange-tree-pixel.png?v=1';
  const coniferTreeImage = new Image();
  coniferTreeImage.onload=()=>{if(REDUCED_MOTION)drawFrame();};
  coniferTreeImage.src='./assets/cemara-laut-redesign.png?v=1';
  const wildGrassImage=new Image();wildGrassImage.onload=()=>{if(REDUCED_MOTION)drawFrame();};wildGrassImage.src='./assets/wild-grass-selected.png?v=3';
  const bushImage=new Image();bushImage.onload=()=>{if(REDUCED_MOTION)drawFrame();};bushImage.src='./assets/bush-selected.png?v=1';
  const bushFlowerImage=new Image(),bushBerryImage=new Image();for(const [image,path] of [[bushFlowerImage,'jasmine'],[bushBerryImage,'mulberry']]){image.onload=()=>{if(REDUCED_MOTION)drawFrame();};image.src=`./assets/bush-${path}.png?v=1`;}
  const treeSprite=tree=>tree.kind==='bush'?(window.BushHarvest?.ready(window.worldManager?.getActive(),tree)?(tree.variant==='flower'?bushFlowerImage:bushBerryImage):bushImage):tree.kind==='wild-grass'?wildGrassImage:tree.kind==='conifer'?coniferTreeImage:orangeTreeImage;
  const treeSize=tree=>tree.size===1?1:2;
  const treeVisualScale=tree=>tree.kind==='bush'?14.5/60*treeSize(tree):tree.kind==='wild-grass'?14/60:treeSize(tree)/2;
  const treeWidth=tree=>tree.kind==='bush'?18.5*treeSize(tree):tree.kind==='wild-grass'?28:ORANGE_TREE_WIDTH*treeVisualScale(tree);
  const treeHeight=tree=>ORANGE_TREE_HEIGHT*treeVisualScale(tree);
  // Preserve the approved artwork's aspect ratio at a 60-unit height.
  const ORANGE_TREE_HEIGHT=60, ORANGE_TREE_WIDTH=ORANGE_TREE_HEIGHT*512/490;
  const PLAYER_WIDTH = 13.5;
  const PLAYER_HEIGHT = 25;
  // Base and new worlds are 50×50; older worlds retain their original geometry.
  let ISLAND_GRID_SIZE = window.worldManager?.getActive()?.id === 'base' ? 50 : (window.worldManager?.getActive()?.gridSize || 9);
  const ISLAND_TILE_WIDTH = 28;
  function columnLabelFor(column) { let n=column+1, label=''; while(n>0) { n--; label=String.fromCharCode(65+n%26)+label; n=Math.floor(n/26); } return label; }
  // Logical grid IDs are A1–I9: columns A–I left to right, rows 1–9 top to bottom.
  // The diamond compass frame is rotated 45° clockwise from the screen diagonal so the grid
  // axes are cardinal: moving along a column runs east–west, moving along a row runs
  // north–south. Top corner=NW, upper-right edge=N, right corner=NE, lower-right edge=E,
  // bottom=SE, lower-left edge=S, left=SW, and upper-left edge=W.
  // A row of tiles is therefore a parallel of latitude and a column is a meridian of longitude.
  // The frame shift makes the camera face northwest instead of north, so device headings are
  // compensated in setHeadingDegrees to keep the sky on real compass bearings.
  const COMPASS_FRAME_OFFSET_DEGREES = 45;
  // Fixed 45° azimuth and 25° elevation keep a consistent dimetric projection.
  const DEFAULT_ISLAND_CAMERA_ELEVATION = 25;
  const MIN_ISLAND_CAMERA_ELEVATION = 20;
  const MAX_ISLAND_CAMERA_ELEVATION = 38;
  let islandCameraElevation = DEFAULT_ISLAND_CAMERA_ELEVATION;
  let islandTileHeight = ISLAND_TILE_WIDTH
    * Math.sin(islandCameraElevation * Math.PI / 180);
  // Fit the larger locked Base in the existing preview viewport; legacy worlds retain their scale.
  let islandZoom = 9 / ISLAND_GRID_SIZE;
  let editorZoom = 7.5, cameraPanX = 0, cameraPanY = 0;
  let cameraMode = 'gameplay', savedEditorCamera = {zoom: 7.5, panX: 0, panY: 0};
  let gameplayFollowCentre = null, gameplayFollowTime = null, gameplayCameraMoving = false;
  let cameraWorldId = window.worldManager?.getActive()?.id;
  function baseFitZoom() { return 9 / ISLAND_GRID_SIZE; }
  // Interior tile faces stay shallow; exposed perimeter faces extend through the grass band.
  const ISLAND_TILE_SIDE_DEPTH = 2.5;
  // Half-height grass block band above the unchanged 16-unit soil underside.
  const ISLAND_GRASS_BAND_DEPTH = 4;
  const ISLAND_SOIL_BAND_DEPTH = 16;
  // The green cap overhangs the narrower soil body along the front perimeter.
  const ISLAND_GRASS_OVERHANG = 4;
  const ISLAND_DEPTH = ISLAND_GRASS_BAND_DEPTH + ISLAND_SOIL_BAND_DEPTH;
  // World levels and the grass block band share the same half-height.
  const BLOCK_HEIGHT = ISLAND_GRASS_BAND_DEPTH;
  const WATER_SURFACE_DROP = BLOCK_HEIGHT * .25;
  const ISLAND_BASE_BOTTOM_MARGIN = 8;
  const ISLAND_VERTICAL_LIFT = 10;
  const ISLAND_BOTTOM_MARGIN = ISLAND_BASE_BOTTOM_MARGIN + ISLAND_VERTICAL_LIFT;
  let islandTop = HEIGHT - ISLAND_BOTTOM_MARGIN - ISLAND_DEPTH
    - ISLAND_GRID_SIZE * islandTileHeight;
  let islandCenterY = islandTop + ISLAND_GRID_SIZE * islandTileHeight / 2;
  // Keep celestial centers in the upper third with enough margin for the Sun's glow.
  const CELESTIAL_BAND_TOP = HEIGHT * 0.10;
  const CELESTIAL_BAND_HEIGHT = HEIGHT / 3 - CELESTIAL_BAND_TOP;
  // Keep the Sun's roughly 18-unit halo inside the canvas along the upper edge.
  const SUN_GLOW_SAFE_INSET = 20;
  // Sun and moon sprites remain hidden while the replacement is designed. Weather and the
  // virtual bodies still drive sky lighting; decorative stars are rendered at night.
  // A stylized 9 x 9 tile surface represents a 2.25 km square ground patch.
  const ISLAND_TILE_METERS = 250;
  // Clouds are raster sprites traced from reference art, downsampled to a 30-column grid and
  // quantised into three flat bands. One `cell` value drives both axes, so each silhouette
  // keeps the ratio it was traced at instead of stretching into a flat smudge.
  // Horizontal slots are absolute scene units, not fractions, so the widest sprite still clears
  // both frame edges.
  const CLOUD_SLOT_X = [56, 264, 118, 206];
  // Clouds must hang below the sun so the light can reach past them and drop their shadow on the
  // island.
  const CLOUD_SUN_CLEARANCE = 4;
  // Clouds ride the same tilted arc as the Sun and Moon instead of a flat line: one dome across
  // the frame, leaning the same way. Each cloud sits on its own parallel arc so they stack in
  // depth instead of sharing one path.
  const CLOUD_ARC_TOP = 46;
  const CLOUD_ARC_RISE = 42;
  const CLOUD_ARC_OFFSETS = [2, 0, 12, 20];
  // Two identical island-shaped 9 x 9 frames: Langit Matahari above and Langit Awan
  // 50 scene units below it. The latter is intended as the future cloud-motion reference;
  // the existing Sun and cloud motion are unchanged for now.
  const LANGIT_MATAHARI_ENABLED = false;
  const LANGIT_AWAN_ENABLED = false;
  const LANGIT_LINE = 0.5;
  // Distance from a frame's own edge to the outermost ink, so the boundary stroke's outer half stays
  // inside the shape.
  const LANGIT_INSET = 1.5;
  const LANGIT_ALPHA = 0.38;
  const LANGIT_EDGE_ALPHA = 0.62;
  // Screen units, so the frames clear the plate by a fixed distance regardless of the plate's own
  // tilt or zoom. 53 is the highest lift that keeps a whole frame on screen.
  const LANGIT_MATAHARI_LIFT = 100;
  const LANGIT_AWAN_LIFT = 50;
  // Distance along the projected westward grid axis, in scene units.
  const LANGIT_MATAHARI_WEST_SHIFT = 20;
  const LANGIT_AWAN_WEST_SHIFT = 10;
  // A cloud crossing the island turns partly transparent so the tiles behind it stay readable.
  const CLOUD_ISLAND_ALPHA = 0.55;
  // Clouds travel right to left like real wind-driven cloud, cross the whole frame, then re-enter
  // from the other side. Screen speed is derived from the reported wind so a breeze crawls and a
  // storm hurries; the old oscillating sway was fast and never actually went anywhere.
  // The frame maps the whole compass — 360 degrees of sky — onto 320 units, so one unit is 1.125
  // degrees. That makes drift speed a function of cloud altitude: a layer at 1200 m needs 0.0118
  // units per second per km/h. Taking that literally would look frozen on a panel this small, so
  // the gain is intentionally faster for readability; it is not an exact physical speed.
  // A cloud is fully off-screen on the left only once its trail clears the frame with margin.
  const CLOUD_EDGE_INSET = 4;
  // The island's own cast shadow: close to the base, drifting with the light direction.
  const ISLAND_BODY_SHADOW_SCALE = 1.55;
  const ISLAND_BODY_SHADOW_OPACITY_SCALE = 0.46;
  // The plate's own cast shadow keeps one size and one colour in every weather; only its position
  // and the alpha it lands with change. The alpha is scaled against the brightness of the floor it
  // lands on, so the shade reads at the same weight at noon and at midnight.
  const SHADOW_CONTRAST_TARGET = 24;
  // Relative luminance of the shade itself (#0c1728), used to solve the alpha for that target.
  const SHADOW_LUMINANCE = 15;
  const ISLAND_BODY_SHADOW_RISE = 1;
  const ISLAND_BODY_SHADOW_DRIFT = 10;
  const FALLBACK = { latitude: -6.7097, longitude: 111.3476, fallback: true };
  const REFRESH_INTERVAL = 10 * 60 * 1000;
  const REDUCED_MOTION = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const CLOUD_SHAPES = [
    ['000000000000001110011110000000',
      '000000022211321111111110000000',
      '000000322223321111111111000000',
      '000000333333331111111111100000',
      '000003333333331111111111110000',
      '000033332333331111111111111000',
      '033333322233331111111111111100',
      '333333322233331111111111111100',
      '333333311133331111111111111110',
      '333333311113331111111111111111',
      '333333311112221111111111111111',
      '032333111112221111111111111111',
      '000233111111211111111111111110',
      '000000000000011111000000000000'],
    ['000000000002221110000000000000',
      '000000000033221111111100000000',
      '000000223333322111111110000000',
      '000002233333322111111111000000',
      '000003333333322211111111111000',
      '003333333333322211111111111100',
      '033333333333322211111111111110',
      '033333333333322211111111111111',
      '333333333333322211111111111111',
      '333333333333322211111111111111',
      '333333333333322211111111111111',
      '333333333333322211111111111111',
      '333332233333322111111111111110',
      '033322223333322111111111111110',
      '003322113333322111111111111100',
      '000331110333221111100111110000',
      '000000000003221110000000000000'],
    ['000000000002222200000000000000',
      '000000000022222220000000000000',
      '000000000332222222000000000000',
      '000001133332222222200000000000',
      '000033333332211122222222000000',
      '000333333322111111222222200000',
      '000333333322111111112222220000',
      '003333333322111111111122220000',
      '033333333322111111111111220000',
      '033323333322111111111111111100',
      '333111333322111111111111111110',
      '333111333322111111111111111111',
      '333111133322111111111111111111',
      '333111133322111111111111111111',
      '031111113322111111111111111111',
      '031111110321111111111111111110',
      '000111000001111100000000000000'],
    ['000000000000000000222220000000',
      '000000000111111003222222000000',
      '000000033111111133311111100000',
      '000000333311111111111111100000',
      '002223333311111111111111110000',
      '032223333311111111111111110000',
      '333233333311111111111111111000',
      '333333333311111111111111111110',
      '333333333331111111111111111110',
      '333333333331111111111111111111',
      '333333333331111112111111111111',
      '033333333331111122211111111111',
      '033333333311111122211111111111',
      '000333333311111122211111111110',
      '000003333311111122211111111110',
      '000000333311111112111111111000',
      '000000333111111111111111100000',
      '000000333111111111111111000000',
      '000000031111110001111110000000',
      '000000001111100000000000000000'],
  ];
  let viewQuarterTurns=0;
  const viewPoint=(column,row)=>window.WorldViewRotation?.point(column,row,ISLAND_GRID_SIZE,viewQuarterTurns) || {column,row};
  const worldPoint=(column,row)=>window.WorldViewRotation?.point(column,row,ISLAND_GRID_SIZE,-viewQuarterTurns) || {column,row};
  const viewFacing=facing=>window.WorldViewRotation?.direction(facing,viewQuarterTurns) || facing;
  function worldNeighbor(column,row,direction) {
    const d=window.WorldViewRotation?.direction(direction,-viewQuarterTurns) || direction;
    const [x,y]={N:[0,-1],E:[1,0],S:[0,1],W:[-1,0]}[d];return {column:column+x,row:row+y};
  }
  const viewDepth=(column,row)=>{const p=viewPoint(column,row);return p.column+p.row;};
  const treeViewDepth=tree=>viewDepth(tree.column+(treeSize(tree)-1)/2,tree.row+(treeSize(tree)-1)/2)+treeSize(tree)-1;
  const fenceViewDepth=f=>viewDepth(f.column,f.row)+((viewFacing(f.edge)==='S'||viewFacing(f.edge)==='E')?.5:-.5);
  const ISLAND_TILES = [];
  function rebuildIslandProjection() {
    islandTileHeight = ISLAND_TILE_WIDTH * Math.sin(islandCameraElevation * Math.PI / 180);
    islandTop = HEIGHT - ISLAND_BOTTOM_MARGIN - ISLAND_DEPTH
      - ISLAND_GRID_SIZE * islandTileHeight;
    islandCenterY = islandTop + ISLAND_GRID_SIZE * islandTileHeight / 2;
    ISLAND_TILES.length = 0;
    for (let diagonal = 0; diagonal < ISLAND_GRID_SIZE * 2 - 1; diagonal += 1) {
      for (let row = 0; row < ISLAND_GRID_SIZE; row += 1) {
        const column = diagonal - row;
        if (column < 0 || column >= ISLAND_GRID_SIZE) continue;
        const p=worldPoint(column,row),worldColumn=p.column,worldRow=p.row;
        const columnLabel = columnLabelFor(worldColumn);
        const rowLabel = worldRow + 1;
        ISLAND_TILES.push({
          column:worldColumn,
          row:worldRow,
          viewColumn:column, viewRow:row,
          columnLabel,
          rowLabel,
          coordinate: `${columnLabel}${rowLabel}`,
          variant: (column + row) % 2,
          x: WIDTH / 2 + (column - row) * ISLAND_TILE_WIDTH / 2,
          y: islandTop + islandTileHeight / 2 + (column + row) * islandTileHeight / 2
        });
      }
    }
  }
  rebuildIslandProjection();
  function defaultEditorCamera() {
    const [anchorX, anchorY] = islandAnchor(), zoom = 7.5;
    return {zoom, panX: WIDTH / 2 - anchorX - (WIDTH / 2 - anchorX) * baseFitZoom() * zoom,
      panY: HEIGHT * .56 - anchorY - (islandCenterY - anchorY) * baseFitZoom() * zoom};
  }
  savedEditorCamera = defaultEditorCamera();
  const NIGHT = ['#111a35', '#1d2948', '#35415d', '#514c65'];
  const DAWN = ['#425783', '#7183a7', '#c79aa5', '#f0bd88'];
  const SUNSET = ['#39486e', '#6e6f91', '#d38d84', '#f5b573'];
  const DAY = ['#4c7fa5', '#73a5b8', '#abc7c1', '#e5d4b6'];
  const SKY_ATMOSPHERES = {
    night: { tint: '#465b84', islandMix: 0.18 },
    twilight: { tint: '#cd9488', islandMix: 0.11 },
    overcast: { tint: '#778f9a', islandMix: 0.12 },
    day: { tint: '#dae2cf', islandMix: 0.07 }
  };
  const STARS = [
    [101.287, -16.716, 1.00], [95.988, -52.696, 0.78], [213.915, 19.182, 0.72],
    [79.172, 45.998, 0.65], [78.634, -8.202, 0.61], [88.793, 7.407, 0.57],
    [279.234, 38.783, 0.55], [201.298, -11.161, 0.48], [247.351, -26.432, 0.45],
    [297.695, 8.868, 0.42], [68.980, 16.509, 0.40], [310.358, 45.280, 0.38],
    [114.825, 5.225, 0.35], [186.650, -63.099, 0.34], [24.429, -57.237, 0.30],
    [154.993, 19.842, 0.29]
  ];

  let location = FALLBACK;
  let weather = {
    weatherCode: 3,
    cloudCover: 42,
    precipitation: 0,
    rain: 0,
    windSpeed: 4,
    windDirection: 90
  };
  const PREVIEW_MODES = [
    { label: 'Live · Open-Meteo', live: true },
    {
      label: 'Cerah · Siang', solarHour: 12, sunAtSkyCenter: true,
      weather: { weatherCode: 0, cloudCover: 5, precipitation: 0, rain: 0, windSpeed: 3, windDirection: 90 }
    },
    {
      label: 'Mendung', solarHour: 12.5,
      weather: { weatherCode: 3, cloudCover: 96, precipitation: 0, rain: 0, windSpeed: 5, windDirection: 100 }
    },
    {
      label: 'Hujan', solarHour: 14,
      weather: { weatherCode: 63, cloudCover: 95, precipitation: 1.5, rain: 1.2, windSpeed: 12, windDirection: 120 }
    },
    {
      label: 'Badai', solarHour: 14,
      weather: { weatherCode: 95, cloudCover: 100, precipitation: 6, rain: 4.5, windSpeed: 25, windDirection: 150 }
    },
    {
      label: 'Senja', solarHour: 18.15,
      weather: { weatherCode: 0, cloudCover: 10, precipitation: 0, rain: 0, windSpeed: 2, windDirection: 90 }
    },
    {
      label: 'Subuh', solarHour: 5.5,
      weather: { weatherCode: 0, cloudCover: 10, precipitation: 0, rain: 0, windSpeed: 2, windDirection: 90 }
    },
    {
      label: 'Malam', solarHour: 20.5,
      weather: { weatherCode: 0, cloudCover: 10, precipitation: 0, rain: 0, windSpeed: 2, windDirection: 90 }
    }
  ];
  let previewModeIndex = 0;
  let headingDegrees = 0;
  let particles = [];
  let particleEpoch = performance.now();
  let lastFetch = 0;
  let weatherError = false;
  let weatherRefreshing = false;
  let weatherModelTime = '';
  const weatherRefreshButton = document.getElementById('weather-refresh-button');
  const animationStart = performance.now();

  function clamp(value, min, max) {
    return Math.max(min, Math.min(max, value));
  }

  function wrap(value, range) {
    return ((value % range) + range) % range;
  }

  function getSceneWeather() {
    const mode = PREVIEW_MODES[previewModeIndex];
    return mode.live ? weather : mode.weather;
  }

  function getSceneTimestamp(now) {
    const mode = PREVIEW_MODES[previewModeIndex];
    if (mode.live || !Number.isFinite(mode.solarHour)) return now;
    const date = new Date(now);
    const utcDayStart = Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate());
    let timestamp = utcDayStart + (mode.solarHour - location.longitude / 15) * 60 * 60 * 1000;
    // Mean solar noon differs from apparent solar noon. For the clear-day demo, converge on
    // the Sun's meridian transit so the weather and lighting also reflect its daily peak.
    if (mode.sunAtSkyCenter) {
      for (let i = 0; i < 2; i += 1) {
        const hourAngle = sunPosition(timestamp, location.latitude, location.longitude).hourAngleDegrees;
        timestamp -= hourAngle / 15 * 60 * 60 * 1000;
      }
    }
    return timestamp;
  }

  function updateWeatherModeControls() {
    const mode = PREVIEW_MODES[previewModeIndex];
    if (weatherModeLabel) {
      const source = `${location.fallback ? 'Rembang, Jawa Tengah (fallback)' : 'lokasi perangkat'} (${location.latitude.toFixed(3)}, ${location.longitude.toFixed(3)})`;
      const observation = lastFetch > 0
        ? ` · diterima ${new Date(lastFetch).toLocaleTimeString('id-ID', {hour:'2-digit',minute:'2-digit'})}${weatherModelTime ? ` · waktu model ${weatherModelTime}` : ''} · kode ${weather.weatherCode} · hujan ${weather.rain} mm · presipitasi ${weather.precipitation} mm · awan ${Math.round(weather.cloudCover)}% · angin ${weather.windSpeed} km/jam${weatherError ? ' · pembaruan gagal, memakai data terakhir' : ''}${weatherRefreshing ? ' · memperbarui…' : ''}`
        : weatherError ? ' · data cuaca tidak tersedia' : weatherRefreshing ? ' · memperbarui…' : ' · data cuaca belum diterima';
      weatherModeLabel.textContent = mode.live
        ? `Suasana: ${mode.label} · ${source}${observation}` : `Demo: ${mode.label}`;
      weatherModeLabel.title = weatherModeLabel.textContent;
    }
    if (weatherModeButton) {
      weatherModeButton.setAttribute('aria-label', `Ganti suasana preview. Aktif: ${mode.label}`);
    }
  }

  function hexRgb(color) {
    if (color.startsWith('#')) {
      const text = color.slice(1);
      return [parseInt(text.slice(0, 2), 16), parseInt(text.slice(2, 4), 16), parseInt(text.slice(4, 6), 16)];
    }
    const match = color.match(/^rgb\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)$/i);
    return match ? [Number(match[1]), Number(match[2]), Number(match[3])] : [0, 0, 0];
  }

  function blendColor(first, second, amount) {
    const a = hexRgb(first);
    const b = hexRgb(second);
    const t = clamp(amount, 0, 1);
    return `rgb(${Math.round(a[0] + (b[0] - a[0]) * t)}, ${Math.round(a[1] + (b[1] - a[1]) * t)}, ${Math.round(a[2] + (b[2] - a[2]) * t)})`;
  }

  function colorFromPalette(palette, amount) {
    const position = clamp(amount, 0, 1) * (palette.length - 1);
    const index = Math.min(palette.length - 2, Math.floor(position));
    return blendColor(palette[index], palette[index + 1], position - index);
  }

  function weatherCodeIsDrizzle(code) {
    return code >= 51 && code <= 57;
  }

  function weatherCodeIsRain(code) {
    return (code >= 61 && code <= 67) || (code >= 80 && code <= 82);
  }

  function weatherCodeIsSolidPrecipitation(code) {
    return (code >= 71 && code <= 77) || (code >= 85 && code <= 86);
  }

  function weatherCodeIsStorm(code) {
    return code >= 95 && code <= 99;
  }


  function julianDay(timeMillis) {
    return timeMillis / 86400000 + 2440587.5;
  }

  function normalizeDegrees(value) {
    return ((value % 360) + 360) % 360;
  }

  function normalizeSigned(value) {
    const normalized = normalizeDegrees(value);
    return normalized > 180 ? normalized - 360 : normalized;
  }

  function horizonPosition(rightAscension, declination, jd, latitude, longitude) {
    const centuries = (jd - 2451545) / 36525;
    const gmst = normalizeDegrees(280.46061837
      + 360.98564736629 * (jd - 2451545)
      + 0.000387933 * centuries * centuries
      - centuries * centuries * centuries / 38710000);
    const hourAngleDegrees = normalizeSigned(gmst + longitude - rightAscension);
    const hourAngle = hourAngleDegrees * Math.PI / 180;
    const lat = latitude * Math.PI / 180;
    const dec = declination * Math.PI / 180;
    const altitude = Math.asin(Math.sin(lat) * Math.sin(dec)
      + Math.cos(lat) * Math.cos(dec) * Math.cos(hourAngle)) * 180 / Math.PI;
    const azimuth = Math.atan2(-Math.sin(hourAngle),
      Math.tan(dec) * Math.cos(lat) - Math.sin(lat) * Math.cos(hourAngle)) * 180 / Math.PI;
    return { azimuth: normalizeDegrees(azimuth), altitude, hourAngleDegrees, declination };
  }

  function solarEclipticLongitude(jd) {
    const centuries = (jd - 2451545) / 36525;
    const meanLongitude = normalizeDegrees(280.46646 + centuries * (36000.76983 + centuries * 0.0003032));
    const meanAnomaly = normalizeDegrees(357.52911 + centuries * (35999.05029 - 0.0001537 * centuries));
    const m = meanAnomaly * Math.PI / 180;
    const center = Math.sin(m) * (1.914602 - centuries * (0.004817 + 0.000014 * centuries))
      + Math.sin(2 * m) * (0.019993 - 0.000101 * centuries)
      + Math.sin(3 * m) * 0.000289;
    return normalizeDegrees(meanLongitude + center);
  }


  // Simplified Keplerian lunar elements, matching the Android SkyMath.moon implementation.
  // Illumination follows the real Sun-Moon elongation, so the crescent is astronomical
  // rather than decorative.
  function moonPosition(timeMillis, latitude, longitude, sunLongitude) {
    const jd = julianDay(timeMillis);
    const days = jd - 2451543.5;
    const radians = Math.PI / 180;
    const node = (125.1228 - 0.0529538083 * days) * radians;
    const inclination = 5.1454 * radians;
    const periapsis = (318.0634 + 0.1643573223 * days) * radians;
    const eccentricity = 0.0549;
    const meanAnomaly = normalizeDegrees(115.3654 + 13.0649929509 * days) * radians;
    let eccentricAnomaly = meanAnomaly + eccentricity * Math.sin(meanAnomaly)
      * (1 + eccentricity * Math.cos(meanAnomaly));
    for (let iteration = 0; iteration < 3; iteration += 1) {
      eccentricAnomaly -= (eccentricAnomaly - eccentricity * Math.sin(eccentricAnomaly)
        - meanAnomaly) / (1 - eccentricity * Math.cos(eccentricAnomaly));
    }
    const x = Math.cos(eccentricAnomaly) - eccentricity;
    const y = Math.sqrt(1 - eccentricity * eccentricity) * Math.sin(eccentricAnomaly);
    const trueAnomaly = Math.atan2(y, x);
    const distance = Math.sqrt(x * x + y * y);
    const argument = trueAnomaly + periapsis;
    const eclipticX = distance * (Math.cos(node) * Math.cos(argument)
      - Math.sin(node) * Math.sin(argument) * Math.cos(inclination));
    const eclipticY = distance * (Math.sin(node) * Math.cos(argument)
      + Math.cos(node) * Math.sin(argument) * Math.cos(inclination));
    const eclipticZ = distance * Math.sin(argument) * Math.sin(inclination);
    const longitudeEcliptic = Math.atan2(eclipticY, eclipticX);
    const obliquity = 23.4393 * radians;
    const equatorialY = eclipticY * Math.cos(obliquity) - eclipticZ * Math.sin(obliquity);
    const equatorialZ = eclipticY * Math.sin(obliquity) + eclipticZ * Math.cos(obliquity);
    const rightAscension = normalizeDegrees(Math.atan2(equatorialY, eclipticX) / radians);
    const declination = Math.atan2(equatorialZ,
      Math.sqrt(eclipticX * eclipticX + equatorialY * equatorialY)) / radians;
    const position = horizonPosition(rightAscension, declination, jd, latitude, longitude);
    const phase = normalizeDegrees(longitudeEcliptic / radians - sunLongitude) / 360;
    position.illumination = (1 - Math.cos(phase * 2 * Math.PI)) / 2;
    // Waxing means the moon is growing toward full; the lit limb then faces the sun.
    position.waxing = phase < 0.5;
    return position;
  }

  function sunPosition(timeMillis, latitude, longitude) {
    const jd = julianDay(timeMillis);
    const centuries = (jd - 2451545) / 36525;
    const meanLongitude = normalizeDegrees(280.46646 + centuries * (36000.76983 + centuries * 0.0003032));
    const meanAnomaly = normalizeDegrees(357.52911 + centuries * (35999.05029 - 0.0001537 * centuries));
    const m = meanAnomaly * Math.PI / 180;
    const center = Math.sin(m) * (1.914602 - centuries * (0.004817 + 0.000014 * centuries))
      + Math.sin(2 * m) * (0.019993 - 0.000101 * centuries)
      + Math.sin(3 * m) * 0.000289;
    const omega = (125.04 - 1934.136 * centuries) * Math.PI / 180;
    const lambda = meanLongitude + center - 0.00569 - 0.00478 * Math.sin(omega);
    const meanObliquity = 23 + (26 + (21.448 - centuries * (46.815
      + centuries * (0.00059 - centuries * 0.001813))) / 60) / 60;
    const obliquity = (meanObliquity + 0.00256 * Math.cos(omega)) * Math.PI / 180;
    const lambdaRadians = lambda * Math.PI / 180;
    const rightAscension = normalizeDegrees(Math.atan2(
      Math.cos(obliquity) * Math.sin(lambdaRadians), Math.cos(lambdaRadians)) * 180 / Math.PI);
    const declination = Math.asin(Math.sin(obliquity) * Math.sin(lambdaRadians)) * 180 / Math.PI;
    const position = horizonPosition(rightAscension, declination, jd, latitude, longitude);
    position.eclipticLongitude = normalizeDegrees(lambda);
    return position;
  }

  // Bright-star catalog coordinates are J2000; apply Meeus' IAU 1976 precession.
  function starPosition(raDegrees, decDegrees, timeMillis, latitude, longitude) {
    const jd = julianDay(timeMillis);
    const centuries = (jd - 2451545) / 36525;
    const t2 = centuries * centuries;
    const t3 = t2 * centuries;
    const radians = Math.PI / 180;
    const zeta = (2306.2181 * centuries + 0.30188 * t2 + 0.017998 * t3)
      / 3600 * radians;
    const z = (2306.2181 * centuries + 1.09468 * t2 + 0.018203 * t3)
      / 3600 * radians;
    const theta = (2004.3109 * centuries - 0.42665 * t2 - 0.041833 * t3)
      / 3600 * radians;
    const alpha = raDegrees * radians;
    const delta = decDegrees * radians;
    const a = Math.cos(delta) * Math.sin(alpha + zeta);
    const b = Math.cos(theta) * Math.cos(delta) * Math.cos(alpha + zeta)
      - Math.sin(theta) * Math.sin(delta);
    const c = Math.sin(theta) * Math.cos(delta) * Math.cos(alpha + zeta)
      + Math.cos(theta) * Math.sin(delta);
    const precessedRa = normalizeDegrees(Math.atan2(a, b) / radians + z / radians);
    const precessedDec = Math.asin(clamp(c, -1, 1)) / radians;
    return horizonPosition(precessedRa, precessedDec, jd, latitude, longitude);
  }

  function mapAzimuth(azimuth) {
    return wrap(azimuth - headingDegrees + 180, 360) / 360 * WIDTH;
  }

  // Dawn enters at the inset east/right edge, solar noon reaches top center, dusk exits west/left.
  // Celestial bodies walk across the sky the way they do overhead: the body rises on the east
  // horizon, arcs through the top centre of the band at its highest and sets on the west
  // horizon. The horizontal position therefore comes from the body's own progress along that
  // arc rather than from the raw compass bearing, which would wrap to the opposite screen side
  // once the sun crosses due south shortly before solar noon at this latitude.
  function celestialProgress(hourAngleDegrees) {
    // Negative hour angle is before solar noon (east), positive is after it (west), so the
    // midpoint of a half circle lands the body dead centre at solar noon.
    return clamp(0.5 + hourAngleDegrees / 180, 0, 1);
  }

  function mapCelestialX(hourAngleDegrees) {
    const progress = celestialProgress(hourAngleDegrees);
    return SUN_RISE_X + (SUN_SET_X - SUN_RISE_X) * progress;
  }

  // The arc is tilted a few units: the body starts slightly lower on the eastern side and
  // finishes slightly higher on the western side, which matches the diamond frame where east
  // sits at the lower-right corner and west at the upper-left one. The tilt is deliberately
  // gentle so the sun still sinks toward the horizon instead of setting high in the sky.
  function mapCelestialY(altitude, hourAngleDegrees) {
    const progress = celestialProgress(hourAngleDegrees);
    return mapAltitudeToCelestialBand(altitude) + ARC_TILT * (0.5 - progress);
  }

  function mapAltitudeToCelestialBand(altitude) {
    return CELESTIAL_BAND_TOP
      + (1 - clamp(altitude, 0, 90) / 90) * CELESTIAL_BAND_HEIGHT;
  }

  // The sun is a plain pixel disc shaded with a soft two-colour vertical gradient: the warm
  // light tone at the top fading into the deeper tone at the bottom. No rays, no facets.
  const SUN_BODY_RADIUS = ISLAND_TILE_WIDTH / 2;
  // East and west ends of the celestial arc, inset so the disc never clips the frame.
  // East sits on the right of the screen and west on the left, so the sun rises on the right
  // and sets on the left, which is the reading most people expect from a sky panel.
  const SUN_RISE_X = 284;
  const SUN_SET_X = 36;
  const ARC_TILT = 9;
  // The moon only appears once the sun has dropped below this altitude.
  const MOON_VISIBLE_BELOW_SUN = -2;
  const GLOW_STEPS = 14;
  // The halo used to reach 15 units past a 10.5-unit disc at 0.040 alpha per ring, which washed out
  // the sky around it and read brighter than the disc itself. Pulled in and faded down.
  const GLOW_REACH = 9.5;
  const GLOW_ALPHA = 0.024;
  const GLOW_PULSE_SPEED = 1.15;

  // Light tone on top, deep tone at the bottom. The pair warms near the horizon and dulls
  // under a heavy cloud deck; the disc itself never changes shape.
  function sunPalette(altitude, cloudCover) {
    const daylight = clamp((altitude + 6) / 18, 0, 1);
    const low = clamp(1 - daylight, 0, 1);
    const warm = (highSun, lowSun) => blendColor(highSun, lowSun, low * 0.8);
    const dull = clamp(cloudCover / 100, 0, 1) * 0.4;
    const shaded = color => blendColor(color, '#7d8494', dull);
    return {
      top: shaded(warm('#fffdf0', '#fff0c2')),
      bottom: shaded(warm('#ffd44a', '#ff9d33'))
    };
  }


  // The glow is a stack of faint discs instead of a few hard rings, so it fades out smoothly.
  // It also breathes: the discs slide outward and back on a slow cycle, which is what makes
  // the halo visibly alive rather than a static ring. Its shape follows the sun's real
  // position: a low sun spreads it wider and flatter (longer path through the air), a high
  // sun keeps it tight and round, so the glow reflects the current latitude and longitude.
  function glowDisc(centerX, centerY, radius, squash) {
    for (let row = -radius; row < radius; row += RENDER_CELL) {
      const sampleY = row + RENDER_CELL / 2;
      const scaledY = sampleY / squash;
      const halfWidth = Math.sqrt(Math.max(0, radius * radius - scaledY * scaledY));
      const left = Math.round((centerX - halfWidth) / RENDER_CELL) * RENDER_CELL;
      const right = Math.round((centerX + halfWidth) / RENDER_CELL) * RENDER_CELL;
      if (right > left) ctx.fillRect(left, centerY + row, right - left, RENDER_CELL);
    }
  }

  // Shared by the sun and the moon so both halos breathe with the same rhythm.
  function drawCelestialGlow(centerX, centerY, altitude, motionTime, tint, visibility,
    strength, size = 1) {
    if (visibility <= 0 || strength <= 0) return;
    const high = clamp(clamp(altitude, 0, 90) / 45, 0, 1);
    const squash = 0.52 + high * 0.48;
    const breathe = 0.5 + 0.5 * Math.sin(motionTime * GLOW_PULSE_SPEED);
    const reach = GLOW_REACH * size * (1.15 - high * 0.45) * (0.82 + breathe * 0.36);
    const alpha = GLOW_ALPHA * (0.72 + breathe * 0.42) * visibility * strength;
    ctx.fillStyle = tint.replace('rgb', 'rgba').replace(')', `, ${alpha})`);
    for (let step = 0; step < GLOW_STEPS; step += 1) {
      const progress = step / (GLOW_STEPS - 1);
      glowDisc(centerX, centerY, SUN_BODY_RADIUS * size + size + progress * reach, squash);
    }
  }

  function drawSunBody(centerX, centerY, palette) {
    const radius = SUN_BODY_RADIUS;
    for (let row = -radius; row < radius; row += RENDER_CELL) {
      const sampleY = row + RENDER_CELL / 2;
      const halfWidth = Math.sqrt(Math.max(0, radius * radius - sampleY * sampleY));
      const left = Math.round((centerX - halfWidth) / RENDER_CELL) * RENDER_CELL;
      const right = Math.round((centerX + halfWidth) / RENDER_CELL) * RENDER_CELL;
      if (right <= left) continue;
      // 0 at the top of the disc, 1 at the bottom.
      const mix = clamp((sampleY + radius) / (radius * 2), 0, 1);
      ctx.fillStyle = blendColor(palette.top, palette.bottom, mix);
      ctx.fillRect(left, centerY + row, right - left, RENDER_CELL);
    }
  }


  // The moon reuses the sun's construction exactly: one flat disc with a soft two-colour
  // vertical gradient, the same stack-of-discs halo, and the same astronomy-driven shape.
  // Only the palette is cooler, and a crescent mask is carved out from the real Sun-Moon
  // elongation so the shape matches the phase actually visible tonight.
  function moonPalette() {
    return { top: '#f4f7fb', bottom: '#b9c6da' };
  }

  function drawMoon(moon, motionTime, sunAltitude) {
    if (sunAltitude >= 0 || moon.altitude <= 0) return;
    const palette = moonPalette();
    const position = celestialPosition(moon);
    const centerX = position.x, centerY = position.y;
    const visibility = clamp(moon.altitude / 2, 0, 1);
    ctx.save(); applyIslandTransform();
    const previousAlpha = ctx.globalAlpha;
    ctx.globalAlpha = previousAlpha * visibility;
    drawCelestialGlow(centerX, centerY, moon.altitude, motionTime,
      blendColor(palette.top, '#cfe0ff', 0.45), visibility,
      0.35 + clamp(moon.illumination, 0, 1) * 0.55, .5);
    drawMoonBody(centerX, centerY, palette, moon);
    ctx.globalAlpha = previousAlpha; ctx.restore();
  }

  // The terminator is projected as an ellipse rather than an offset circle, which is the
  // difference between a correct gibbous moon and a permanent crescent. At half illumination
  // the terminator is a straight line through the centre; as the moon fills out it bows toward
  // the dark limb, and as it thins it bows back toward the lit limb.
  function drawMoonBody(centerX, centerY, palette, moon) {
    const radius = SUN_BODY_RADIUS / 2;
    const illumination = clamp(moon.illumination, 0, 1);
    const terminator = 1 - illumination * 2;
    // In the southern hemisphere the lit limb faces the opposite side from the north.
    const litOnRight = location.latitude >= 0 ? moon.waxing : !moon.waxing;
    for (let row = -radius; row < radius; row += RENDER_CELL) {
      const sampleY = row + RENDER_CELL / 2;
      const halfWidth = Math.sqrt(Math.max(0, radius * radius - sampleY * sampleY));
      if (halfWidth <= 0) continue;
      const mix = clamp((sampleY + radius) / (radius * 2), 0, 1);
      const bandColor = blendColor(palette.top, palette.bottom, mix);
      const ellipse = Math.sqrt(Math.max(0, 1 - (sampleY / radius) * (sampleY / radius)));
      const edge = terminator * radius * ellipse;
      // Test each cell centre against the disc instead of rounding the row extent, which
      // would leave single-cell steps poking out of the limb.
      const litLeft = Math.floor((centerX - halfWidth) / RENDER_CELL) * RENDER_CELL;
      const litRight = Math.ceil((centerX + halfWidth) / RENDER_CELL) * RENDER_CELL;
      ctx.fillStyle = bandColor;
      for (let x = litLeft; x < litRight; x += RENDER_CELL) {
        const offset = x + RENDER_CELL / 2 - centerX;
        if (offset * offset + sampleY * sampleY > radius * radius) continue;
        const lit = litOnRight ? offset >= edge : offset <= -edge;
        if (lit) ctx.fillRect(x, centerY + row, RENDER_CELL, RENDER_CELL);
      }
    }
  }

  // Position comes straight from the existing astronomy: azimuth picks the screen column and
  // altitude picks the height inside the upper celestial band. The sun fades in just above
  // the horizon and is gone below it.
  // Coordinates in the island projection, before island zoom. At night this same continuous
  // orbit provides a virtual light for cloud shadows, while the visible Sun remains hidden.
  function sunPlanePosition(hourAngle) {
    const a = hourAngle * Math.PI / 180;
    const axisLength = Math.hypot(ISLAND_TILE_WIDTH, islandTileHeight);
    const dx = -40 * Math.sin(a);
    const dy = 15 * (1 - Math.cos(a));
    return {
      x: WIDTH / 2 - LANGIT_MATAHARI_WEST_SHIFT * ISLAND_TILE_WIDTH / axisLength
        + dx,
      y: islandCenterY - LANGIT_MATAHARI_LIFT
        - LANGIT_MATAHARI_WEST_SHIFT * islandTileHeight / axisLength
        + dy
    };
  }

  function planeToScreen(x, y) {
    const [anchorX, anchorY] = islandAnchor();
    return { x: anchorX + (x - anchorX) * islandZoom + cameraPanX,
      y: anchorY + (y - anchorY) * islandZoom + cameraPanY };
  }

  function celestialPosition(body) {
    const p=window.CelestialGeometry.offset(body.hourAngleDegrees,body.declination,location.latitude);
    return {x:WIDTH/2+p.x,y:islandCenterY+p.y};
  }
  function drawSun(sun, cloudCover, motionTime) {
    if (sun.altitude <= 0) return;
    const palette = sunPalette(sun.altitude, cloudCover);
    const position = celestialPosition(sun);
    const centerX = position.x, centerY = position.y;
    ctx.save(); applyIslandTransform();
    const previousAlpha = ctx.globalAlpha;
    const visibility = clamp(sun.altitude / 2, 0, 1);
    ctx.globalAlpha = previousAlpha * visibility;
    drawCelestialGlow(centerX, centerY, sun.altitude, motionTime,
      blendColor(palette.top, '#ffd98a', 0.55), visibility, 1);
    drawSunBody(centerX, centerY, palette);
    ctx.globalAlpha = previousAlpha; ctx.restore();
  }

  // Decorative stars on the same island-relative arc as Android; not an actual star map.
  function drawStars(sunAltitude, cloudCover) {
    const opacity=clamp((-sunAltitude-4)/8,0,1)*(1-clamp(cloudCover/100,0,1)*.85);
    if(opacity<=0)return;
    ctx.save();applyIslandTransform();ctx.fillStyle=`rgba(244,245,220,${opacity})`;
    for(let i=0;i<16;i++) {
      const angle=Math.PI*(i+.5)/16, side=RENDER_CELL*.5*(i%4===0?2:1);
      const x=WIDTH/2+300*Math.cos(angle),y=islandCenterY-300*Math.sin(angle);
      ctx.fillRect(x-side/2,y-side/2,side,side);
    }
    ctx.restore();
  }

  function blendedSkyPalette(sun) {
    const w=window.SkyBlend.weights(sun.altitude),twilight=sun.azimuth>180?SUNSET:DAWN;
    return NIGHT.map((n,i)=>blendColor(blendColor(n,twilight[i],w.night+w.twilight ? w.twilight/(w.night+w.twilight) : 0),DAY[i],w.day));
  }
  function drawSky(now, sun, code, cloudCover) {
    const palette=blendedSkyPalette(sun);

    const band = 2;
    for (let y = 0; y < HEIGHT; y += band) {
      const base = colorFromPalette(palette, y / HEIGHT);
      const overcast = `rgb(${Math.round(65 + y / HEIGHT * 70)}, ${Math.round(79 + y / HEIGHT * 70)}, ${Math.round(98 + y / HEIGHT * 66)})`;
      const cover = clamp(cloudCover / 100 * 0.58, 0, 0.58);
      const storm = weatherCodeIsStorm(code) ? 0.18 : 0;
      const result = blendColor(base, overcast, Math.max(cover, storm));
      ctx.fillStyle = result;
      ctx.fillRect(0, y, WIDTH, band);
    }
  }

  function skyColorAt(y, sun, cloudCover) {
    const palette=blendedSkyPalette(sun);
    const base = colorFromPalette(palette, clamp(y / HEIGHT, 0, 1));
    return blendColor(base, '#586576', clamp(cloudCover / 100 * 0.58, 0, 0.58));
  }

  // Clouds need two variants of every silhouette: the traced shape and its mirror, so a row of
  // them never looks stamped from a single die.
  const CLOUD_SHAPE_VARIANTS = CLOUD_SHAPES.reduce((variants, shape) => {
    variants.push(shape);
    variants.push(shape.map(row => row.split('').reverse().join('')));
    return variants;
  }, []);


  function cloudHash(seed, index, salt) {
    let value = (seed ^ Math.imul(index + 1, 0x9e3779b9)
      ^ Math.imul(salt + 1, 0x85ebca6b)) | 0;
    value = Math.imul(value ^ (value >>> 16), 0x7feb352d);
    value = Math.imul(value ^ (value >>> 15), 0x846ca68b);
    value ^= value >>> 16;
    return (value >>> 0) / 4294967296;
  }

  // Integrate distance instead of multiplying the latest wind by total elapsed time: changing
  // weather or axis must not teleport clouds. Hysteresis holds the current cardinal direction
  // until the new direction is more than 50 degrees away.
  let cloudEastTravel = 0;
  let cloudNorthTravel = 0;
  let cloudLastElapsed = null;
  let cloudWindCardinal = null;
  function createCloudStates(elapsedSeconds, sceneWeather) {
    const speedKmh = Math.max(0, sceneWeather.windSpeed ?? 0);
    const bearing = normalizeDegrees((sceneWeather.windDirection ?? 0) + 180);
    if (speedKmh > 0) {
      if (cloudWindCardinal === null ||
          Math.abs(normalizeSigned(bearing - cloudWindCardinal)) > 50) {
        cloudWindCardinal = wrap(Math.round(bearing / 90) * 90, 360);
      }
      const delta = cloudLastElapsed === null ? 0 : Math.max(0, elapsedSeconds - cloudLastElapsed);
      const axisLength = Math.hypot(ISLAND_TILE_WIDTH, islandTileHeight) / 2;
      const distance = window.CloudLayout.driftSpeed(speedKmh)
        * delta / axisLength;
      const angle = cloudWindCardinal * Math.PI / 180;
      cloudEastTravel += Math.sin(angle) * distance;
      cloudNorthTravel += Math.cos(angle) * distance;
    }
    cloudLastElapsed = elapsedSeconds;
    const count = window.CloudLayout.count(sceneWeather.cloudCover ?? 0, sceneWeather.weatherCode);
    const seed = Math.trunc(location.latitude * 37 + location.longitude * 17)
      ^ Math.imul(count, 0x2545f491);
    const axisLength = Math.hypot(ISLAND_TILE_WIDTH, islandTileHeight);
    const clouds = [];
    for (let index = 0; index < count; index += 1) {
      const shape = CLOUD_SHAPE_VARIANTS[Math.floor(cloudHash(seed, index, 1) * CLOUD_SHAPE_VARIANTS.length)];
      const cell=window.CloudLayout.cellSize(ISLAND_TILE_WIDTH,islandTileHeight,shape[0].length,shape.length,ISLAND_GRID_SIZE,count);
      const {east:eastSeed,north:northSeed}=window.CloudLayout.seed(index,count,ISLAND_GRID_SIZE,cloudHash(seed,index,2),cloudHash(seed,index,3));
      const east = wrap(eastSeed + cloudEastTravel, ISLAND_GRID_SIZE) - ISLAND_GRID_SIZE/2;
      const north = wrap(northSeed + cloudNorthTravel, ISLAND_GRID_SIZE) - ISLAND_GRID_SIZE/2;
      const x = WIDTH / 2 - LANGIT_AWAN_WEST_SHIFT * ISLAND_TILE_WIDTH / axisLength
        + (east + north) * ISLAND_TILE_WIDTH / 2;
      const y = islandCenterY - LANGIT_AWAN_LIFT
        - LANGIT_AWAN_WEST_SHIFT * islandTileHeight / axisLength
        + (east - north) * islandTileHeight / 2;
      const displayY=y-(1000/ISLAND_TILE_METERS*ISLAND_TILE_WIDTH-LANGIT_AWAN_LIFT)-shape.length*cell/2;
      const screen = planeToScreen(x, displayY);
      clouds.push({ shape, cell, planeX: x, planeY: y,
        screenCenterX: screen.x, screenBottomY: screen.y + shape.length * cell / 2 });
    }
    return clouds;
  }

  // Three flat bands keyed to the sun's height, matching the Sun and Moon's own light model.
  function cloudPalette(sunAltitude, code) {
    if (weatherCodeIsStorm(code)) {
      return ['#5a6a80', '#93a1b3', '#c6d0dc'];
    }
    if (sunAltitude < -6) return ['#404e6a', '#5c6d8a', '#8fa0bd'];
    if (sunAltitude < 10) return ['#8c7f9c', '#bbaec6', '#efd7d2'];
    return ['#a9c0d9', '#e4eefb', '#ffffff'];
  }

  // One rectangle per sprite cell keeps every edge on the art grid, which is what holds the
  // cloud to the same pixel language as the rest of the scene at this size.
  function drawCloudRows(cloud, palette) {
    const shape = cloud.shape;
    const cell = cloud.cell;
    const snap = value => Math.round(value * RENDER_SCALE) / RENDER_SCALE;
    // Snap shared cell boundaries to device pixels; fractional cell widths then meet without seams.
    const left = cloud.screenCenterX - shape[0].length * cell * 0.5;
    const top = cloud.screenBottomY - shape.length * cell;
    for (let row = 0; row < shape.length; row += 1) {
      const line = shape[row];
      const rowTop = snap(top + row * cell);
      const rowBottom = snap(top + (row + 1) * cell);
      for (let column = 0; column < line.length; column += 1) {
        const band = line[column];
        if (band === '0') continue;
        ctx.fillStyle = palette[Number(band) - 1];
        const x1 = snap(left + column * cell);
        const x2 = snap(left + (column + 1) * cell);
        ctx.fillRect(x1, rowTop, x2 - x1, rowBottom - rowTop);
      }
    }
  }

  // The island's top surface as a screen-space diamond. Clouds pass in front of it, so this is the
  // shape a cloud has to cross to be shading or hiding anything.
  function cloudOverlapsIsland(cloud) {
    const width = cloud.shape[0].length * cloud.cell;
    const left = cloud.screenCenterX - width * 0.5;
    const right = cloud.screenCenterX + width * 0.5;
    const top = cloud.screenBottomY - cloud.shape.length * cloud.cell;
    const bottom = cloud.screenBottomY;
    for (const point of islandSurfacePoints()) {
      if (point[0] >= left && point[0] <= right && point[1] >= top && point[1] <= bottom) {
        return true;
      }
    }
    const halfWidth = ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH * 0.5;
    const halfHeight = ISLAND_GRID_SIZE * islandTileHeight * 0.5;
    const dx = (left + right) * 0.5 - WIDTH / 2;
    const dy = (top + bottom) * 0.5 - islandCenterY;
    return Math.abs(dx) / (halfWidth + width * 0.5)
      + Math.abs(dy) / (halfHeight + (bottom - top) * 0.5) <= 1;
  }

  // A cloud crossing the island drops to partial opacity, so whatever sits on the tiles is not
  // hidden behind it. The island's own surface shadows are unaffected: this is only the sprite.
  // Every cloud carries the same opacity, whether or not it is crossing the island. Dimming only
  // the ones over the plate made a cloud visibly change weight as it drifted on, which read as the
  // cloud flickering rather than as depth.
  function cloudOpacity() {
    return CLOUD_ISLAND_ALPHA;
  }

  function drawClouds(clouds, sunAltitude, code) {
    if (clouds.length === 0) return;
    const palette = cloudPalette(sunAltitude, code);
    clouds.forEach(cloud => {
      const alpha = cloudOpacity();
      const previousAlpha = ctx.globalAlpha;
      ctx.globalAlpha = previousAlpha * alpha;
      drawCloudRows(cloud, palette);
      ctx.globalAlpha = previousAlpha;
    });
  }

  // The island is drawn scaled about this anchor, so any overlay on its top surface has to be
  // placed with the same transform or the shadow drifts as soon as the zoom changes.
  function islandAnchor() {
    return [WIDTH / 2, HEIGHT - ISLAND_BOTTOM_MARGIN];
  }

  // The floor the island floats over. The island's own cast shadow already establishes where that
  // floor reads on screen, so cloud shadows use the same line rather than a guess: they sit on the
  // ground beside the island's shadow and share its language instead of floating on their own.
  function groundShadowY() {
    const bottomY = islandTop + ISLAND_GRID_SIZE * islandTileHeight;
    return bottomY + ISLAND_DEPTH - ISLAND_DEPTH - ISLAND_BODY_SHADOW_RISE;
  }

  function applyIslandTransform() {
    const [anchorX, anchorY] = islandAnchor();
    ctx.translate(anchorX + cameraPanX, anchorY + cameraPanY);
    ctx.scale(islandZoom, islandZoom);
    ctx.translate(-anchorX, -anchorY);
  }

  function islandSurfacePoints() {
    const centerX = WIDTH / 2;
    const halfWidth = ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH / 2;
    const halfHeight = ISLAND_GRID_SIZE * islandTileHeight / 2;
    return [
      [centerX, islandCenterY - halfHeight],
      [centerX + halfWidth, islandCenterY],
      [centerX, islandCenterY + halfHeight],
      [centerX - halfWidth, islandCenterY]
    ];
  }

  function tileVisible(tile, extra = 0) {
    const [ax, ay] = islandAnchor();
    const x=ax+cameraPanX+(tile.x-ax)*islandZoom;
    const y=ay+cameraPanY+(tile.y-ay)*islandZoom;
    const hw=ISLAND_TILE_WIDTH*islandZoom/2, hh=islandTileHeight*islandZoom/2;
    return x+hw>=0 && x-hw<=WIDTH && y+hh+extra*islandZoom>=0 && y-hh<=HEIGHT;
  }

  let surfaceClipCache={tiles:null,stacks:null,path:null};
  function clipIslandSurface() {
    const world=window.worldManager?.getActive(),tiles=world?.tiles,stacks=world?.blockStacks;
    if(surfaceClipCache.tiles!==tiles || surfaceClipCache.stacks!==stacks || surfaceClipCache.tileHeight!==islandTileHeight || surfaceClipCache.gridSize!==ISLAND_GRID_SIZE || surfaceClipCache.turns!==viewQuarterTurns || !surfaceClipCache.path) {
      const path=new Path2D();
      if(!tiles) {
        const points=islandSurfacePoints();path.moveTo(points[0][0],points[0][1]);
        for(const [x,y] of points.slice(1))path.lineTo(x,y);path.closePath();
      }
      for(const tile of tiles ? ISLAND_TILES : []) {
        if(!tiles[tile.row*ISLAND_GRID_SIZE+tile.column])continue;
        const height=surfaceHeight(tile.column,tile.row), y=tile.y-Math.max(0,height-1)*BLOCK_HEIGHT;
        const hw=ISLAND_TILE_WIDTH/2,hh=islandTileHeight/2;
        path.moveTo(tile.x,y-hh);path.lineTo(tile.x+hw,y);path.lineTo(tile.x,y+hh);path.lineTo(tile.x-hw,y);path.closePath();
      }
      surfaceClipCache={tiles,stacks,path,tileHeight:islandTileHeight,gridSize:ISLAND_GRID_SIZE,turns:viewQuarterTurns};
    }
    ctx.clip(surfaceClipCache.path);
  }

  // Chooses which body is currently lighting the scene: the sun whenever it is up, otherwise the
  // moon when it is up and bright enough to cast anything. Returns a neutral light for the in
  // between cases so the shadow simply fades rather than snapping to a new direction.
  function pickLightSource(sunAltitude, sunHourAngle, moon, sun) {
    if (sunAltitude > 2) return { altitude: sunAltitude, hourAngle: sunHourAngle, declination: sun.declination };
    if (moon && moon.altitude > 2) {
      // Moonlight is far weaker, so scale its apparent elevation down.
      const moonlight = clamp(moon.altitude * (0.35 + moon.illumination * 0.65), 0, 90);
      return { altitude: moonlight, hourAngle: moon.hourAngleDegrees, declination: moon.declination };
    }
    return { altitude: Math.max(sunAltitude, moon ? moon.altitude : -90), hourAngle: sunHourAngle, declination: sun.declination };
  }

  // Relative luminance of what actually sits behind the shadow: the sky gradient at the floor line,
  // after the same weather blend drawSky uses. Measuring beats guessing here, because the whole
  // problem is that a fixed alpha removes a fixed fraction of a backdrop whose own brightness swings
  // by nearly an order of magnitude between noon and midnight.
  function islandFloorLuminance(cloudCover, code) {
    const now = getSceneTimestamp(Date.now());
    const sun = sunPosition(new Date(now), location.latitude, location.longitude);
    const palette=blendedSkyPalette(sun);
    const y = HEIGHT - 6;
    const base = hexRgb(colorFromPalette(palette, clamp(y / HEIGHT, 0, 1)));
    const overcast = [65 + y / HEIGHT * 70, 79 + y / HEIGHT * 70, 98 + y / HEIGHT * 66];
    const cover = clamp(cloudCover / 100 * 0.58, 0, 0.58);
    const blend = Math.max(cover, weatherCodeIsStorm(code) ? 0.18 : 0);
    const rgb = base.map((value, index) => value + (overcast[index] - value) * blend);
    return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
  }

  function drawIslandShadow(centerX, lowerY, lightAltitude, lightHourAngle, cloudCover, code, lightDeclination) {
    // Constant, like the size: weather decides how the sky looks, not how dark the plate's shadow
    // is. Opacity used to follow the light level, so the same shadow faded from 0.14 by day to 0.02
    // at night -- it looked as if the shadow shrank and then disappeared, when all that changed was
    // its weight. The lower edge remains 50 units below the bottom tile center.
    //
    // A fixed alpha is not enough on its own, and that is why the shadow vanished after dark: alpha
    // blends toward the shade colour, so it removes a fixed FRACTION of the backdrop. The same 0.14
    // takes 25 levels off bright daytime ground but only 9 at night, where there is barely any light
    // left to take away. The silhouette keeps one colour and one size, and the alpha is solved for a
    // target amount of contrast instead of being fixed, so the shade lands with the same visible
    // weight in every weather and at every hour.
    const floorLuminance = islandFloorLuminance(cloudCover, code);
    const opacity = clamp(SHADOW_CONTRAST_TARGET / Math.max(floorLuminance - SHADOW_LUMINANCE, 1),
      0.06, 0.45);
    // The island shadow is fixed directly beneath the island, independent of celestial motion.
    const shadowCenterX = centerX;
    const scale=ISLAND_BODY_SHADOW_SCALE*window.CelestialGeometry.shadowScale(lightHourAngle,lightDeclination,location.latitude,ISLAND_DEPTH);
    const halfWidth=ISLAND_GRID_SIZE*ISLAND_TILE_WIDTH/4*scale;
    const halfHeight=ISLAND_GRID_SIZE*islandTileHeight/4*scale;
    const shadowCenterY=window.CelestialGeometry.shadow(lowerY-ISLAND_DEPTH,islandTileHeight,halfHeight);
    drawIslandPolygon([
      [shadowCenterX, shadowCenterY - halfHeight],
      [shadowCenterX + halfWidth, shadowCenterY],
      [shadowCenterX, shadowCenterY + halfHeight],
      [shadowCenterX - halfWidth, shadowCenterY]
    ], `rgba(12,23,40,${opacity})`, null);
  }

  function skyAtmosphere(sunAltitude, cloudCover, code) {
    const overcastOrWet = cloudCover >= 65 || code === 45 || code === 48 || (code >= 51 && code <= 99);
    const w=window.SkyBlend.weights(sunAltitude),night=SKY_ATMOSPHERES.night,twilight=SKY_ATMOSPHERES.twilight,day=overcastOrWet?SKY_ATMOSPHERES.overcast:SKY_ATMOSPHERES.day;
    return {tint:blendColor(blendColor(night.tint,twilight.tint,w.night+w.twilight ? w.twilight/(w.night+w.twilight) : 0),day.tint,w.day),islandMix:night.islandMix*w.night+twilight.islandMix*w.twilight+day.islandMix*w.day};
  }

  function islandPalette(sunAltitude, cloudCover, code) {
    const daylight = clamp((sunAltitude + 12) / 24, 0, 1);
    const twilight = clamp(1 - Math.abs(sunAltitude + 1) / 9, 0, 1);
    let weatherShade = clamp((cloudCover - 25) / 100 * 0.34, 0, 0.26);
    if (weatherCodeIsStorm(code)) weatherShade = Math.max(weatherShade, 0.34);
    const atmosphere = skyAtmosphere(sunAltitude, cloudCover, code);
    let grass = blendColor('#384c43', '#91ad91', daylight);
    grass = blendColor(grass, '#c39d60', twilight * 0.25);
    grass = blendColor(grass, '#78836f', weatherShade);
    const wet = (code >= 51 && code <= 67) || (code >= 80 && code <= 82);
    if (wet) grass = blendColor(grass, '#62786d', 0.12);
    grass = blendColor(grass, atmosphere.tint, atmosphere.islandMix);
    const grassDark = blendColor(grass, '#2d4938', 0.24);
    const grassTileMix = (0.18 + daylight * 0.77) * (1 - weatherShade * 0.30);
    const grassTileSoftFern = blendColor(grass, '#96b397', grassTileMix);
    const grassTileFernSage = blendColor(grass, '#8ba78c', grassTileMix);
    const grassSeam = blendColor(grass, '#314d37', 0.08);
    const grassWall = blendColor(grass, '#587761', 0.28);
    let soil = blendColor('#4a4036', '#b6a183', daylight);
    soil = blendColor(soil, '#be742d', twilight * 0.24);
    soil = blendColor(soil, '#5b4837', weatherShade * 0.45);
    soil = blendColor(soil, atmosphere.tint, atmosphere.islandMix * 0.22);
    const soilLight = blendColor(soil, '#cdbb99', 0.20);
    const soilDark = blendColor(soil, '#604e40', 0.50);
    const palette = { grass, grassDark, grassTileSoftFern, grassTileFernSage,
      grassSeam, grassWall, soilLight, soilDark };
    // Base World stays byte-for-byte unchanged. A custom world applies a tint to the island
    // only; weather, paths, shadows and the rest of the shared world remain unchanged.
    const worldColor = window.worldManager?.getActive()?.color;
    if (worldColor && !['#a8c795', '#a4cf36'].includes(worldColor.toLowerCase())) {
      for (const key of ['grass', 'grassDark', 'grassTileSoftFern', 'grassTileFernSage',
        'grassSeam', 'grassWall']) palette[key] = blendColor(palette[key], worldColor, 0.55);
    }
    return palette;
  }

  function islandSunlight(sunAltitude, cloudCover, code) {
    if (sunAltitude <= 0) return { strength: 0 };
    const altitudeRadians = clamp(sunAltitude, 0, 90) * Math.PI / 180;
    let strength = clamp(Math.sin(altitudeRadians) / Math.sin(Math.PI / 3), 0, 1)
      * (1 - clamp(cloudCover / 100, 0, 1));
    if (weatherCodeIsStorm(code)) strength *= 0.55;
    return { strength };
  }

  // The same 32-bit hash as Android GrassDetail.variant.
  function grassDetailVariant(column, row) {
    let hash = Math.imul(column, 73856093) ^ Math.imul(row, 19349663);
    hash ^= hash >>> 16; hash = Math.imul(hash, 0x7feb352d); hash ^= hash >>> 15;
    const slot = hash & 7;
    return slot < 4 ? 0 : 1 + (slot - 4) % 3;
  }

  // Only the two camera-facing faces are visible. A water neighbour at the
  // same level replaces that face; other heights/materials retain the bank.
  function waterBorders(column, row, level, world = window.worldManager?.getActive()) {
    const size=world?.gridSize;
    function isWater(c,r) {
      if (!Number.isInteger(size) || c<0 || r<0 || c>=size || r>=size) return false;
      const index=r*size+c;
      return world.blockStacks ? world.blockStacks[index]?.[level] === 3
        : level===0 && world.tiles?.[index] === 3;
    }
    const left=worldNeighbor(column,row,'S'),right=worldNeighbor(column,row,'E');
    return {left:!isWater(left.column,left.row),right:!isWater(right.column,right.row)};
  }
  // The upper rim follows exposed water surfaces, including rear edges. A
  // neighbouring water surface at any height leaves a passage for a waterfall.
  function waterTopBorders(column,row,world=window.worldManager?.getActive()) {
    const size=world?.gridSize,tiles=world?.tiles;
    const isWater=(c,r)=>Number.isInteger(size) && c>=0 && r>=0 && c<size && r<size && tiles?.[r*size+c]===3;
    const isViewWater=d=>{const p=worldNeighbor(column,row,d);return isWater(p.column,p.row);};
    return {left:!isViewWater('S'),right:!isViewWater('E'),backLeft:!isViewWater('W'),backRight:!isViewWater('N')};
  }
  function drawWaterTopBanks(tile,halfWidth,halfHeight,palette,world) {
    const x=tile.x,y=tile.y,hw=halfWidth,hh=halfHeight;
    const vertices=[[x,y-hh],[x+hw,y],[x,y+hh],[x-hw,y]];
    const names=['backRight','right','left','backLeft'];
    const borders=waterTopBorders(tile.column,tile.row,world),inset=.13;
    // Constant perpendicular-in-grid offsets make neighbouring segments line
    // up at shared vertices instead of tapering independently to each tile.
    const offsets=[[-hw*inset,hh*inset],[-hw*inset,-hh*inset],
      [hw*inset,-hh*inset],[hw*inset,hh*inset]];
    const color=blendColor(palette.soilLight,palette.soilDark,.22);
    const add=(v,d)=>[v[0]+d[0],v[1]+d[1]];
    for(let i=0;i<4;i++) {
      if(!borders[names[i]]) continue;
      const a=vertices[i],b=vertices[(i+1)%4],d=offsets[i];
      drawSmoothIslandPolygon([a,b,add(b,d),add(a,d)],color,color);
      // Fill the corner only when both adjoining edges are closed; openings to
      // water and waterfalls remain open all the way to the corner.
      const next=(i+1)%4;
      if(borders[names[next]]) {
        const e=offsets[next];
        drawSmoothIslandPolygon([b,add(b,d),add(add(b,d),e),add(b,e)],color,color);
      }
    }
  }
  // A fall exists only between neighbouring exposed water surfaces, and is
  // visual: it never creates, moves or consumes terrain blocks.
  function waterFalls(column,row,world=window.worldManager?.getActive()) {
    const size=world?.gridSize, tiles=world?.tiles;
    if(!tiles || column<0 || row<0 || column>=size || row>=size || tiles[row*size+column]!==3) return {};
    const height=(c,r)=>world.blockStacks ? world.blockStacks[r*size+c].length : 1;
    const source=height(column,row), result={};
    for(const [name,edge] of [['left','S'],['right','E'],['backLeft','W'],['backRight','N']]) {
      const p=worldNeighbor(column,row,edge),c=p.column,r=p.row;
      if(c<0||r<0||c>=size||r>=size || tiles[r*size+c]!==3) continue;
      const drop=source-height(c,r);
      if(drop>0) result[name]=drop;
    }
    return result;
  }
  function drawWaterfalls(tile, level, falls, motionTime) {
    const hw=ISLAND_TILE_WIDTH/2,hh=islandTileHeight/2, x=tile.x,y=tile.y-level*BLOCK_HEIGHT+WATER_SURFACE_DROP;
    const edges={left:[[x-hw,y],[x,y+hh]],right:[[x,y+hh],[x+hw,y]],
      backLeft:[[x,y-hh],[x-hw,y]],backRight:[[x+hw,y],[x,y-hh]]};
    for(const [side,drop] of Object.entries(falls)) {
      const [a,b]=edges[side],depth=drop*BLOCK_HEIGHT;
      const [dx,dy]=side==='left'?[-hw,hh]:side==='right'?[hw,hh]:side==='backLeft'?[-hw,-hh]:[hw,-hh];
      const endA=[a[0]+dx*.5,a[1]+dy*.5+depth],endB=[b[0]+dx*.5,b[1]+dy*.5+depth];
      ctx.save();ctx.beginPath();ctx.moveTo(...a);ctx.lineTo(...b);ctx.lineTo(...endB);ctx.lineTo(...endA);ctx.closePath();
      ctx.fillStyle='rgba(105,204,215,0.68)';ctx.fill();ctx.clip();
      ctx.strokeStyle='rgba(218,250,244,0.60)';ctx.lineWidth=.65;ctx.lineCap='round';
      const phase=REDUCED_MOTION ? 0 : (motionTime*.38)%1;
      for(let i=1;i<=5;i++) {const f=i/6,px=a[0]+(b[0]-a[0])*f,py=a[1]+(b[1]-a[1])*f;
        for(let k=-1;k<=2;k++){const start=Math.max(0,k*.5+phase),end=Math.min(1,k*.5+phase+.22);if(end>start){ctx.beginPath();ctx.moveTo(px+dx*.5*start,py+(dy*.5+depth)*start);ctx.lineTo(px+dx*.5*end,py+(dy*.5+depth)*end);ctx.stroke();}}}
      ctx.restore();
      // The two outer banks frame the slope while both water ends stay open.
      ctx.save();ctx.strokeStyle='rgba(112,91,66,.92)';ctx.lineWidth=1.15;ctx.lineCap='round';
      ctx.beginPath();ctx.moveTo(...a);ctx.lineTo(...endA);ctx.moveTo(...b);ctx.lineTo(...endB);ctx.stroke();ctx.restore();
    }
  }
  function drawWaterFlow(tile,time,world) {
    const direction=window.WaterFlow?.direction(world,tile.column,tile.row);if(!direction)return;
    const view=viewFacing(direction),hw=ISLAND_TILE_WIDTH/2,hh=islandTileHeight/2;
    const vector=view==='E'?[hw,hh]:view==='S'?[-hw,hh]:view==='W'?[-hw,-hh]:[hw,-hh];
    const [dx,dy]=vector,phase=REDUCED_MOTION?0:(time*.3)%1;
    ctx.save();ctx.beginPath();ctx.moveTo(tile.x,tile.y-hh);ctx.lineTo(tile.x+hw,tile.y);ctx.lineTo(tile.x,tile.y+hh);ctx.lineTo(tile.x-hw,tile.y);ctx.closePath();ctx.clip();
    ctx.strokeStyle='rgba(220,250,245,.48)';ctx.lineWidth=.45;ctx.lineCap='round';
    for(let j=-1;j<=1;j++){const ox=-dy*1.5*j,oy=dx*.375*j;for(let k=-1;k<=1;k++){const p=k*.5+phase-.5;ctx.beginPath();ctx.moveTo(tile.x+ox+dx*p,tile.y+oy+dy*p);ctx.lineTo(tile.x+ox+dx*(p+.18),tile.y+oy+dy*(p+.18));ctx.stroke();}}
    ctx.restore();
  }

  // Ramps inherit block proportions and use their own selected material.
  // Faces use the same material palette as blocks and only front-facing sides.
  function drawRamp(tile, direction, material, palette, time=0) {
    const shape=window.RampGeometry?.create(direction,ISLAND_TILE_WIDTH,islandTileHeight,BLOCK_HEIGHT);
    if(!shape)return;
    const grassTop=material>=7 && material<=10, base=grassTop?({7:2,8:4,9:5,10:6})[material]:material;
    const terrain={3:'#75cbd0',4:'#b8b2a7',5:'#dac695',6:'#bd7549'};
    let side=base===1?(tile.variant===0?palette.grassTileSoftFern:palette.grassTileFernSage):base===2?blendColor(palette.soilLight,'#d49d65',.4):base===4?blendColor(terrain[4],palette.soilDark,.08):blendColor(terrain[base]||'#ae8b68',palette.grassDark,.12);
    if(base!==1 && tile.variant!==0)side=blendColor(side,'#000000',.06);
    const top=grassTop?(tile.variant===0?palette.grassTileSoftFern:palette.grassTileFernSage):side;
    const polygon=(face,color)=>drawSmoothIslandPolygon(face.map(p=>[tile.x+p.x,tile.y+p.y]),color,color,blendColor(color,'#000000',.12),.35);
    ctx.save();if(material===3)ctx.globalAlpha*=.64;
    for(const face of shape.sideFaces) {
      const u=face.every(p=>p.u===1),v=face.every(p=>p.v===1);
      if(u || v)polygon(face,blendColor(side,'#000000',v?.15:.23));
    }
    if(direction==='S' || direction==='E')polygon(shape.highFace,blendColor(side,'#000000',direction==='S'?.15:.23));
    polygon(shape.topFace,top);
    if(material===3){
      const point=(u,v)=>({x:tile.x+(u-v)*ISLAND_TILE_WIDTH/2,y:tile.y+(u+v-1)*islandTileHeight/2-window.RampGeometry.fraction(direction,u,v)*BLOCK_HEIGHT});
      const uv=(t,q)=>direction==='E'?[1-t,q]:direction==='W'?[t,q]:direction==='S'?[q,1-t]:[q,t];
      const line=(t1,t2,q)=>{const [u,v]=uv(t1,q),[a,b]=uv(t2,q),p=point(u,v),z=point(a,b);ctx.moveTo(p.x,p.y);ctx.lineTo(z.x,z.y);};
      ctx.strokeStyle='rgba(112,91,66,.92)';ctx.lineWidth=1.15;ctx.beginPath();line(0,1,0);line(0,1,1);ctx.stroke();
      ctx.strokeStyle='rgba(220,250,245,.7)';ctx.lineWidth=.5;const phase=REDUCED_MOTION?0:(time*.35)%1;
      ctx.beginPath();for(const q of [.2,.5,.8])for(let k=-1;k<=2;k++){const a=Math.max(0,k*.5+phase),b=Math.min(1,k*.5+phase+.18);if(b>a)line(a,b,q);}ctx.stroke();
    }
    ctx.restore();
  }

  // A single darker color for the structural earth on both sides.
  function baseSoilColor(palette) { return blendColor(palette.soilLight, "#000000", .16); }
  function drawGrassTile(tile, palette, sunlight, material = 1, level = 0, baseLayer = level === 0, coveredBaseGrass = false) {
    const raised = level > 0;
    const borders = material === 3 ? waterBorders(tile.column,tile.row,level) : null;
    const world=material===3 ? window.worldManager?.getActive() : null;
    const stack=world?.blockStacks?.[tile.row*ISLAND_GRID_SIZE+tile.column],top=stack?.length-1;
    const waterAbove=material===3 && stack?.[level+1]===3;
    const falls=world?.blockStacks && material===3 && level===top ? waterFalls(tile.column,tile.row,world) : {};
    if(borders && falls.left) borders.left=false;
    if(borders && falls.right) borders.right=false;
    const grassTop=material>=7 && material<=10;
    const baseMaterial=grassTop ? ({7:2,8:4,9:5,10:6})[material] : material;
    const lightTile = (tile.column+tile.row+level)%2 === 0;
    const terrain = {2:'#ae8b68', 3:'#75cbd0', 4:'#b8b2a7', 5:'#dac695', 6:'#bd7549'};
    let fill = baseMaterial === 1 ? (lightTile ? palette.grassTileSoftFern : palette.grassTileFernSage)
      : baseMaterial === 2 ? blendColor(palette.soilLight, '#d49d65', 0.4)
      : baseMaterial === 4 ? blendColor(terrain[4],palette.soilDark,0.08)
      : blendColor(terrain[baseMaterial] || terrain[2], palette.grassDark, 0.12);
    if(baseMaterial===2 && baseLayer)fill=baseSoilColor(palette);
    else if(baseMaterial!==1 && !lightTile)fill=blendColor(fill,'#000000',.06);
    const halfWidth = ISLAND_TILE_WIDTH / 2 - (material===3 || baseLayer ? 0 : 0.05);
    const halfHeight = islandTileHeight / 2 - (material===3 || baseLayer ? 0 : 0.025);
    const cornerRadius = material===3 || baseLayer ? 0 : 1.4;
    const viewTile=viewPoint(tile.column,tile.row);
    const leftPerimeterFace = viewTile.row === ISLAND_GRID_SIZE - 1;
    const rightPerimeterFace = viewTile.column === ISLAND_GRID_SIZE - 1;
    // A flat water tile sits over a riverbed; raised water blends over the block
    // beneath (and the surrounding scene) instead of painting an opaque cap.
    if (material === 3) {
      if (!raised) drawSunlitIslandDiamond(tile.x,tile.y,halfWidth,halfHeight,cornerRadius,
        blendColor(palette.soilLight,'#dac695',0.45),sunlight);
      ctx.save();ctx.globalAlpha *= 0.64;
    }
    const leftSideDepth = raised ? BLOCK_HEIGHT : leftPerimeterFace ? ISLAND_GRASS_BAND_DEPTH : ISLAND_TILE_SIDE_DEPTH;
    const rightSideDepth = raised ? BLOCK_HEIGHT : rightPerimeterFace ? ISLAND_GRASS_BAND_DEPTH : ISLAND_TILE_SIDE_DEPTH;
    const baseSoil=baseLayer && (baseMaterial===2 || coveredBaseGrass);
    const sideFill=coveredBaseGrass ? baseSoilColor(palette) : fill;
    if (!borders || borders.left) drawGrassTileSide(tile.x, tile.y,
      leftPerimeterFace ? ISLAND_TILE_WIDTH / 2 : halfWidth,
      leftPerimeterFace ? islandTileHeight / 2 : halfHeight,
      leftPerimeterFace ? 0 : cornerRadius, leftSideDepth, sideFill, true, !baseLayer, baseSoil);
    if (!borders || borders.right) drawGrassTileSide(tile.x, tile.y,
      rightPerimeterFace ? ISLAND_TILE_WIDTH / 2 : halfWidth,
      rightPerimeterFace ? islandTileHeight / 2 : halfHeight,
      rightPerimeterFace ? 0 : cornerRadius, rightSideDepth, sideFill, false, !baseLayer, baseSoil);

    if(baseMaterial===6) {
      // Small earthy clods on the side faces, including the grass-topped variant.
      ctx.save();ctx.fillStyle='rgba(88,45,29,.45)';
      for(const [x,y,d] of [[-.68,.32,leftSideDepth],[-.26,.74,leftSideDepth],[.35,.65,rightSideDepth],[.72,.28,rightSideDepth]])
        ctx.fillRect(tile.x+halfWidth*x,tile.y+halfHeight*y+d*.48,.65,.45);
      ctx.restore();
    }
    // Each top stays solid; direct sunlight adds only a uniform, subtle color lift.
    if(!waterAbove) {
      const topY=tile.y+(material===3?WATER_SURFACE_DROP:0);
      if(coveredBaseGrass) drawSmoothIslandDiamond(tile.x,topY,halfWidth,halfHeight,cornerRadius,sideFill,sideFill);
      else drawSunlitIslandDiamond(tile.x,topY,halfWidth,halfHeight,
        cornerRadius, grassTop ? (lightTile ? palette.grassTileSoftFern : palette.grassTileFernSage) : fill, sunlight);
    }
    if (material === 3) {
      ctx.restore();
      // Exposed faces are solid earthen banks, not merely an outline. Adjacent
      // water and waterfalls leave the shared side open.
      if(borders.left) drawGrassTileSide(tile.x,tile.y,halfWidth,halfHeight,0,leftSideDepth,palette.soilLight,true);
      if(borders.right) drawGrassTileSide(tile.x,tile.y,halfWidth,halfHeight,0,rightSideDepth,palette.soilLight,false);
      if(!world?.blockStacks || level===top) drawWaterTopBanks(tile,halfWidth,halfHeight,palette,world);
      // Quiet, fixed ripples remain legible without flickering at high zoom.
      if(!waterAbove){ctx.save();ctx.globalAlpha *= 0.50;ctx.strokeStyle='#d8faf1';ctx.lineWidth=0.55;
      ctx.lineCap='round';
      for (const [dx,dy] of [[-0.35,-0.2],[0.20,0.13]]) {
        const x=tile.x+halfWidth*dx,y=tile.y+WATER_SURFACE_DROP+halfHeight*dy;
        ctx.beginPath();ctx.moveTo(x-halfWidth*0.17,y);ctx.lineTo(x-halfWidth*0.05,y-halfHeight*0.07);
        ctx.lineTo(x+halfWidth*0.06,y);ctx.lineTo(x+halfWidth*0.17,y-halfHeight*0.07);ctx.stroke();
      }
      ctx.restore();}
    }
    if (material === 4) {
      // Subtle fissures and pale mineral flecks inspired by the selected stone concept.
      ctx.save();ctx.strokeStyle='rgba(66,65,62,.50)';ctx.lineWidth=.45;ctx.lineCap='round';
      const direction=(tile.column+tile.row)%2 ? -1 : 1;
      ctx.beginPath();ctx.moveTo(tile.x-halfWidth*.48*direction,tile.y-halfHeight*.02);
      ctx.lineTo(tile.x-halfWidth*.30*direction,tile.y-halfHeight*.08);
      ctx.lineTo(tile.x-halfWidth*.20*direction,tile.y+halfHeight*.09);
      ctx.lineTo(tile.x-halfWidth*.04*direction,tile.y+halfHeight*.05);ctx.stroke();
      ctx.fillStyle='rgba(236,229,211,.70)';ctx.fillRect(tile.x+halfWidth*.30*direction,tile.y-halfHeight*.17,.85,.65);
      ctx.fillStyle='rgba(87,84,79,.40)';ctx.fillRect(tile.x+halfWidth*.30*direction,tile.y-halfHeight*.17+.65,.85,.35);
      ctx.restore();
    }
    if (material === 6) {
      // Four short parallel furrows, warm soil and small clods from the approved concept.
      ctx.save();ctx.lineCap='round';ctx.lineJoin='round';
      for (const v of [-.60,-.20,.20,.60]) {
        const a={x:tile.x+halfWidth*(v+.78)/2,y:tile.y+halfHeight*(v-.78)/2};
        const b={x:tile.x+halfWidth*(v-.78)/2,y:tile.y+halfHeight*(v+.78)/2};
        ctx.beginPath();ctx.moveTo(a.x,a.y);ctx.lineTo(b.x,b.y);ctx.strokeStyle='rgba(87,44,27,.67)';ctx.lineWidth=1.05;ctx.stroke();
        ctx.beginPath();ctx.moveTo(a.x,a.y-.45);ctx.lineTo(b.x,b.y-.45);ctx.strokeStyle='rgba(239,175,110,.48)';ctx.lineWidth=.32;ctx.stroke();
      }
      for(const [x,y] of [[-.52,-.02],[.29,-.30],[.53,.04],[-.22,.29]]) {
        ctx.fillStyle='rgba(91,50,32,.56)';ctx.fillRect(tile.x+x*halfWidth,tile.y+y*halfHeight,.55,.45);
      }
      ctx.restore();
    }
    // A subtle dark edge keeps individual grass tiles legible without a green rim.
    if(!coveredBaseGrass && (material===1 || grassTop)) {
      ctx.save();ctx.beginPath();traceSmoothIslandDiamond(tile.x,tile.y,halfWidth,halfHeight,cornerRadius);
      ctx.strokeStyle=blendColor(grassTop ? (lightTile ? palette.grassTileSoftFern : palette.grassTileFernSage) : fill,"#000000",.18);ctx.lineWidth=.28;ctx.lineJoin="round";ctx.stroke();ctx.restore();
    }
    // Stable sparse variants match Android's sheet and hash; no world data is changed.
    const variant = grassDetailVariant(tile.column, tile.row);
    if (!coveredBaseGrass && (material === 1 || grassTop) && variant && grassDetailImage.complete && grassDetailImage.naturalWidth === 1280) {
      ctx.save();
      ctx.globalAlpha *= 0.45 + 0.55 * clamp(sunlight.strength * 2, 0, 1);
      ctx.imageSmoothingEnabled = true;
      ctx.drawImage(grassDetailImage, variant * 320, 0, 320, 160,
        tile.x - ISLAND_TILE_WIDTH / 2, tile.y - islandTileHeight / 2,
        ISLAND_TILE_WIDTH, islandTileHeight);
      ctx.restore();
    }
  }

  function drawGrassTileSide(centerX, centerY, halfWidth, halfHeight, cornerRadius,
    depth, fill, leftSide, outline = true, flatSoil = false) {
    const cutX = Math.min(cornerRadius, halfWidth * 0.34);
    const cutY = cutX * halfHeight / halfWidth;
    const splitY = centerY + halfHeight - cutY * 0.5;
    const sideShade = flatSoil ? 0 : depth > ISLAND_TILE_SIDE_DEPTH
      ? (leftSide ? 0.15 : 0.23) : (leftSide ? 0.08 : 0.12);
    if (leftSide) {
      drawSmoothIslandPath(() => {
        ctx.moveTo(centerX - halfWidth + cutX, centerY + cutY);
        ctx.lineTo(centerX - cutX, centerY + halfHeight - cutY);
        ctx.quadraticCurveTo(centerX - cutX * 0.5,
          centerY + halfHeight - cutY * 0.5, centerX, splitY);
        ctx.lineTo(centerX, splitY + depth);
        ctx.quadraticCurveTo(centerX - cutX * 0.5,
          centerY + halfHeight - cutY * 0.5 + depth,
          centerX - cutX, centerY + halfHeight - cutY + depth);
        ctx.lineTo(centerX - halfWidth + cutX, centerY + cutY + depth);
        ctx.closePath();
      }, centerX - halfWidth, centerY + cutY,
      centerX, centerY + halfHeight + depth,
      blendColor(fill, '#000000', sideShade), blendColor(fill, '#000000', sideShade), blendColor(fill, '#000000', sideShade+.12), outline ? .35 : 0);
    } else {
      drawSmoothIslandPath(() => {
        ctx.moveTo(centerX, splitY);
        ctx.quadraticCurveTo(centerX + cutX * 0.5,
          centerY + halfHeight - cutY * 0.5,
          centerX + cutX, centerY + halfHeight - cutY);
        ctx.lineTo(centerX + halfWidth - cutX, centerY + cutY);
        ctx.lineTo(centerX + halfWidth - cutX, centerY + cutY + depth);
        ctx.lineTo(centerX + cutX, centerY + halfHeight - cutY + depth);
        ctx.quadraticCurveTo(centerX + cutX * 0.5,
          centerY + halfHeight - cutY * 0.5 + depth,
          centerX, splitY + depth);
        ctx.closePath();
      }, centerX, centerY + cutY,
      centerX + halfWidth, centerY + halfHeight + depth,
      blendColor(fill, '#000000', sideShade), blendColor(fill, '#000000', sideShade), blendColor(fill, '#000000', sideShade+.12), outline ? .35 : 0);
    }
  }

  function drawIslandSoilBody(soilLeftX, centerX, soilRightX,
    middleY, soilFrontY, grassBandDepth, palette) {
    const vertices = [
      [soilLeftX, middleY + grassBandDepth],
      [centerX, soilFrontY + grassBandDepth],
      [soilRightX, middleY + grassBandDepth],
      [soilRightX, middleY + ISLAND_DEPTH],
      [centerX, soilFrontY + ISLAND_DEPTH],
      [soilLeftX, middleY + ISLAND_DEPTH]
    ];
    const minX = Math.min(...vertices.map(point => point[0]));
    const maxX = Math.max(...vertices.map(point => point[0]));
    const minY = middleY + grassBandDepth;
    const maxY = soilFrontY + ISLAND_DEPTH;
    drawSmoothIslandPath(() => traceRoundedIslandPolygon(vertices, 2.2),
      minX, minY, maxX, maxY, baseSoilColor(palette), baseSoilColor(palette));
  }

  function traceRoundedIslandPolygon(vertices, cornerRadius) {
    const first = vertices[0];
    const previous = vertices[vertices.length - 1];
    const firstLength = Math.hypot(previous[0] - first[0], previous[1] - first[1]);
    const firstInset = firstLength <= 0 ? 0 : Math.min(cornerRadius, firstLength * 0.45) / firstLength;
    ctx.moveTo(first[0] + (previous[0] - first[0]) * firstInset,
      first[1] + (previous[1] - first[1]) * firstInset);
    for (let index = 0; index < vertices.length; index += 1) {
      const [x, y] = vertices[index];
      const [nextX, nextY] = vertices[(index + 1) % vertices.length];
      const edgeLength = Math.hypot(nextX - x, nextY - y);
      const inset = edgeLength <= 0 ? 0 : Math.min(cornerRadius, edgeLength * 0.45) / edgeLength;
      const outgoingX = x + (nextX - x) * inset;
      const outgoingY = y + (nextY - y) * inset;
      ctx.quadraticCurveTo(x, y, outgoingX, outgoingY);
      ctx.lineTo(nextX + (x - nextX) * inset, nextY + (y - nextY) * inset);
    }
    ctx.closePath();
  }

  function drawSmoothIslandPath(tracePath, minX, minY, maxX, maxY,
    lightColor, shadeColor, outlineColor = null, outlineWidth = 0) {
    ctx.save();
    ctx.beginPath();
    tracePath();
    if (lightColor === shadeColor) {
      ctx.fillStyle = lightColor;
    } else {
      const gradient = ctx.createLinearGradient(minX, minY, maxX, maxY);
      gradient.addColorStop(0, lightColor);
      gradient.addColorStop(1, shadeColor);
      ctx.fillStyle = gradient;
    }
    ctx.fill();
    if (outlineColor && outlineWidth > 0) {
      ctx.strokeStyle = outlineColor;
      ctx.lineWidth = outlineWidth;
      ctx.lineJoin = 'round';
      ctx.lineCap = 'round';
      ctx.stroke();
    }
    ctx.restore();
  }

  function drawSmoothIslandPolygon(points, lightColor, shadeColor,
    outlineColor = null, outlineWidth = 0) {
    const xs = points.map(point => point[0]);
    const ys = points.map(point => point[1]);
    drawSmoothIslandPath(() => {
      ctx.moveTo(points[0][0], points[0][1]);
      for (let index = 1; index < points.length; index += 1) {
        ctx.lineTo(points[index][0], points[index][1]);
      }
      ctx.closePath();
    }, Math.min(...xs), Math.min(...ys), Math.max(...xs), Math.max(...ys),
    lightColor, shadeColor, outlineColor, outlineWidth);
  }

  function traceSmoothIslandDiamond(centerX, centerY, halfWidth, halfHeight, cornerRadius) {
    const cutX = Math.min(cornerRadius, halfWidth * 0.34);
    const cutY = cutX * halfHeight / halfWidth;
    ctx.moveTo(centerX - cutX, centerY - halfHeight + cutY);
    ctx.quadraticCurveTo(centerX, centerY - halfHeight,
      centerX + cutX, centerY - halfHeight + cutY);
    ctx.lineTo(centerX + halfWidth - cutX, centerY - cutY);
    ctx.quadraticCurveTo(centerX + halfWidth, centerY,
      centerX + halfWidth - cutX, centerY + cutY);
    ctx.lineTo(centerX + cutX, centerY + halfHeight - cutY);
    ctx.quadraticCurveTo(centerX, centerY + halfHeight,
      centerX - cutX, centerY + halfHeight - cutY);
    ctx.lineTo(centerX - halfWidth + cutX, centerY + cutY);
    ctx.quadraticCurveTo(centerX - halfWidth, centerY,
      centerX - halfWidth + cutX, centerY - cutY);
    ctx.closePath();
  }

  function drawSmoothIslandDiamond(centerX, centerY, halfWidth, halfHeight,
    cornerRadius, lightColor, shadeColor, outlineColor = null, outlineWidth = 0) {
    drawSmoothIslandPath(() => traceSmoothIslandDiamond(
      centerX, centerY, halfWidth, halfHeight, cornerRadius),
    centerX - halfWidth, centerY - halfHeight,
    centerX + halfWidth, centerY + halfHeight,
    lightColor, shadeColor, outlineColor, outlineWidth);
  }

  function drawSunlitIslandDiamond(centerX, centerY, halfWidth, halfHeight,
    cornerRadius, baseColor, sunlight) {
    // Keep the whole tile solid; direct sunlight adds only a subtle uniform tint.
    const tileColor = sunlight && sunlight.strength > 0.015
      ? blendColor(baseColor, '#ccdec1', 0.10 * sunlight.strength)
      : baseColor;
    drawSmoothIslandDiamond(centerX, centerY, halfWidth, halfHeight,
      cornerRadius, tileColor, tileColor);
  }

  function drawSmoothIslandLine(x1, y1, x2, y2, color, width) {
    ctx.save();
    ctx.beginPath();
    ctx.moveTo(x1, y1);
    ctx.lineTo(x2, y2);
    ctx.strokeStyle = color;
    ctx.lineWidth = width;
    ctx.lineJoin = 'round';
    ctx.lineCap = 'round';
    ctx.stroke();
    ctx.restore();
  }

  // One sky frame: a 9 x 9 lattice hung above the plate, traced on the plate's own tile seams
  // so the two share a pitch. Both Langit Matahari and Langit Awan are this function with different
  // lifts and colors; the geometry and opacity are shared.
  //
  // Every line is a tile seam, so the lattice is addressed in the projection's own (column, row)
  // coordinates: integer values land on tile centres, which means the seam between two tiles sits
  // at a half value. The plate is the diamond |a| + |b| = 9 with a = column - row and
  // b = column + row - 8, which is why each seam runs from -0.5 to 8.5 along the other axis.
  function drawLangitFrame(lift, color, offsetX = 0, offsetY = 0) {
    const centreX = WIDTH / 2;
    const centreY = islandCenterY;
    const halfWidth = ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH / 2;
    const halfHeight = ISLAND_GRID_SIZE * islandTileHeight / 2;
    const snap = value => Math.round(value * RENDER_SCALE) / RENDER_SCALE;
    const tilePoint = (column, row) => [
      centreX + (column - row) * ISLAND_TILE_WIDTH / 2,
      islandTop + islandTileHeight / 2 + (column + row) * islandTileHeight / 2
    ];
    const first = -0.5;
    const last = ISLAND_GRID_SIZE - 0.5;
    const line = LANGIT_LINE;
    ctx.save();
    ctx.translate(offsetX, offsetY - lift);
    // Own silhouette, own clip. The lattice is meant to stay whole, so it no longer relies on the
    // plate's surface clip; it clips to itself instead. The inset is the hair of margin that keeps
    // the boundary stroke's outer half inside the shape.
    const inset = LANGIT_INSET;
    ctx.beginPath();
    traceSmoothIslandDiamond(centreX, centreY, halfWidth - inset, halfHeight - inset, 2.2);
    ctx.clip();
    ctx.strokeStyle = color;
    ctx.lineWidth = line;
    ctx.lineCap = 'butt';
    // Round joins, not miter: the rim's four vertices turn by about 53 degrees, and a miter join
    // there shoots a spike well outside the plate. The spike was the only ink that escaped the
    // silhouette.
    ctx.lineJoin = 'round';
    ctx.globalAlpha = LANGIT_ALPHA;
    ctx.beginPath();
    for (let edge = 1; edge < ISLAND_GRID_SIZE; edge += 1) {
      // The seam between columns edge-1 and edge, and the seam between rows edge-1 and edge.
      const seam = edge - 0.5;
      const fromColumn = tilePoint(seam, first);
      const toColumn = tilePoint(seam, last);
      ctx.moveTo(snap(fromColumn[0]), snap(fromColumn[1]));
      ctx.lineTo(snap(toColumn[0]), snap(toColumn[1]));
      const fromRow = tilePoint(first, seam);
      const toRow = tilePoint(last, seam);
      ctx.moveTo(snap(fromRow[0]), snap(fromRow[1]));
      ctx.lineTo(snap(toRow[0]), snap(toRow[1]));
    }
    ctx.stroke();
    // The rim is the plate's own silhouette, inset by a line so its outer half stays inside the
    // clip. Drawn more opaque than the seams because it is the boundary of the frame, not one of its
    // cells.
    ctx.globalAlpha = LANGIT_EDGE_ALPHA;
    ctx.beginPath();
    traceSmoothIslandDiamond(centreX, centreY,
      halfWidth - inset - line / 2, halfHeight - inset - line / 2, 2.2);
    ctx.stroke();
    ctx.restore();
  }

  // The upper sheet: Langit Matahari.
  function drawLangitMatahari() {
    const axisLength = Math.hypot(ISLAND_TILE_WIDTH, islandTileHeight);
    drawLangitFrame(LANGIT_MATAHARI_LIFT, '#0b1420',
      -LANGIT_MATAHARI_WEST_SHIFT * ISLAND_TILE_WIDTH / axisLength,
      -LANGIT_MATAHARI_WEST_SHIFT * islandTileHeight / axisLength);
  }

  // The lower sheet: intended as the future reference for cloud movement.
  function drawLangitAwan() {
    const axisLength = Math.hypot(ISLAND_TILE_WIDTH, islandTileHeight);
    drawLangitFrame(LANGIT_AWAN_LIFT, '#ffffff',
      -LANGIT_AWAN_WEST_SHIFT * ISLAND_TILE_WIDTH / axisLength,
      -LANGIT_AWAN_WEST_SHIFT * islandTileHeight / axisLength);
  }

  function drawPixelPolygon(points, color, alpha) {
    if (points.length < 3) return;
    const minY = Math.min(...points.map(point => point[1]));
    const maxY = Math.max(...points.map(point => point[1]));
    const firstRow = Math.floor(minY / RENDER_CELL);
    const lastRow = Math.ceil(maxY / RENDER_CELL);
    ctx.beginPath();
    for (let row = firstRow; row < lastRow; row += 1) {
      const top = row * RENDER_CELL;
      const sampleY = top + RENDER_CELL / 2;
      const intersections = [];
      for (let index = 0; index < points.length; index += 1) {
        const first = points[index];
        const second = points[(index + 1) % points.length];
        if ((first[1] <= sampleY && second[1] > sampleY)
            || (second[1] <= sampleY && first[1] > sampleY)) {
          const fraction = (sampleY - first[1]) / (second[1] - first[1]);
          intersections.push(first[0] + fraction * (second[0] - first[0]));
        }
      }
      intersections.sort((first, second) => first - second);
      for (let index = 0; index + 1 < intersections.length; index += 2) {
        const firstCell = Math.ceil(intersections[index] / RENDER_CELL - 0.5);
        const endCell = Math.ceil(intersections[index + 1] / RENDER_CELL - 0.5);
        if (endCell > firstCell) {
          ctx.rect(firstCell * RENDER_CELL, top,
            (endCell - firstCell) * RENDER_CELL, RENDER_CELL);
        }
      }
    }
    // Optional alpha keeps the caller's colour and only varies how much of it lands, which is what
    // lets a shadow hold one visual weight across wildly different backdrops.
    ctx.globalAlpha = alpha === undefined ? 1 : clamp(alpha, 0, 1);
    ctx.fillStyle = color;
    ctx.fill();
    ctx.globalAlpha = 1;
  }

  function appendPixelLine(first, second, widthCells) {
    const cell = RENDER_CELL;
    let x = Math.round(first[0] / cell);
    let y = Math.round(first[1] / cell);
    const endX = Math.round(second[0] / cell);
    const endY = Math.round(second[1] / cell);
    const deltaX = Math.abs(endX - x);
    const stepX = x < endX ? 1 : -1;
    const deltaY = -Math.abs(endY - y);
    const stepY = y < endY ? 1 : -1;
    let error = deltaX + deltaY;
    const brushOffset = -Math.floor(widthCells / 2);
    while (true) {
      for (let offsetY = 0; offsetY < widthCells; offsetY += 1) {
        for (let offsetX = 0; offsetX < widthCells; offsetX += 1) {
          ctx.rect((x + brushOffset + offsetX) * cell,
            (y + brushOffset + offsetY) * cell, cell, cell);
        }
      }
      if (x === endX && y === endY) break;
      const doubledError = error * 2;
      if (doubledError >= deltaY) {
        error += deltaY;
        x += stepX;
      }
      if (doubledError <= deltaX) {
        error += deltaX;
        y += stepY;
      }
    }
  }

  function drawPixelPolyline(points, color, closed = false, width = 0.5) {
    if (points.length < 2) return;
    const widthCells = Math.max(1, Math.round(width / RENDER_CELL));
    ctx.beginPath();
    const segmentCount = closed ? points.length : points.length - 1;
    for (let index = 0; index < segmentCount; index += 1) {
      appendPixelLine(points[index], points[(index + 1) % points.length], widthCells);
    }
    ctx.fillStyle = color;
    ctx.fill();
  }

  function drawPixelLine(first, second, color, width = 0.5) {
    drawPixelPolyline([first, second], color, false, width);
  }

  function drawIslandPolygon(points, fill, outline, alpha) {
    drawPixelPolygon(points, fill, alpha);
    if (outline) drawPixelPolyline(points, outline, true, 0.5);
  }

  // Progress across the frame, measured the same way the celestial arc measures it: 0 at the
  // eastern (right) end, 1 at the western (left) end.
  function cloudProgress(centreX) {
    return clamp((SUN_RISE_X - centreX) / (SUN_RISE_X - SUN_SET_X), 0, 1);
  }

  // The same dome and the same lean as the Sun's arc, dropped into the cloud band. Keeping the
  // tilt term identical is what makes the two paths read as parallel rather than merely curved.
  function cloudArcY(centreX, offset) {
    const progress = cloudProgress(centreX);
    return CLOUD_ARC_TOP + CLOUD_ARC_RISE * (1 - Math.sin(Math.PI * progress))
      + ARC_TILT * (0.5 - progress) + offset;
  }

  // One shared light model for every cloud shadow, on the island or on the ground, so the two never
  // drift apart in weight the way the island body's and the cloud's used to.
  function cloudShadowOpacity(light, cloudCover, code) {
    const altitude = Math.max(light.altitude, 0);
    const directLight = clamp((altitude + 8) / 28, 0, 1);
    let cloudFilter = 1 - clamp(cloudCover / 100 * 0.45, 0, 0.45);
    if (weatherCodeIsStorm(code)) cloudFilter *= 0.82;
    return (0.10 + directLight * 0.30) * cloudFilter * ISLAND_BODY_SHADOW_OPACITY_SCALE * 0.9;
  }

  // Hidden clouds cast diffuse, unioned shadows on the island surface.
  function drawCloudShadowsOnIsland(clouds, lightAltitude, lightHourAngle, cloudCover, code, motionTime) {
    if (!clouds.length) return;
    const wet=window.CloudLayout.wet(cloudCover,code);
    const opacity=Math.max(cloudShadowOpacity({altitude:lightAltitude},cloudCover,code),wet?.16:0)
      *window.CloudLayout.fade(motionTime,!REDUCED_MOTION);
    // drawFloatingIsland already applied the island camera transform.
    ctx.save();clipIslandSurface();
    ctx.fillStyle=`rgba(12,23,40,${opacity})`;
    ctx.filter=`blur(${wet?3:1.5}px)`;
    ctx.beginPath();
    const transform=ctx.getTransform(),visible={left:-transform.e/transform.a,right:(renderCanvas.width-transform.e)/transform.a,top:-transform.f/transform.d,bottom:(renderCanvas.height-transform.f)/transform.d};
    for(const cloud of clouds) {
      // Wrap on the world plane, independent of camera or player movement.
      const dx=(cloud.planeX-WIDTH/2)/(ISLAND_TILE_WIDTH/2);
      const dy=(cloud.planeY+LANGIT_AWAN_LIFT-islandCenterY)/(islandTileHeight/2);
      const a=wrap((dx+dy)/2+ISLAND_GRID_SIZE/2,ISLAND_GRID_SIZE)-ISLAND_GRID_SIZE/2;
      const b=wrap((dx-dy)/2+ISLAND_GRID_SIZE/2,ISLAND_GRID_SIZE)-ISLAND_GRID_SIZE/2;
      const x=WIDTH/2+(a+b)*ISLAND_TILE_WIDTH/2, y=islandCenterY+(a-b)*islandTileHeight/2;
      const w=cloud.shape[0].length*cloud.cell,h=cloud.shape.length*cloud.cell;
      // Draw periodic copies at the island edges, so a moving shadow crosses a
      // boundary smoothly instead of disappearing or being confined centrally.
      for(let ia=-1;ia<=1;ia++)for(let ib=-1;ib<=1;ib++) {
        const px=x+(ia+ib)*ISLAND_GRID_SIZE*ISLAND_TILE_WIDTH/2,py=y+(ia-ib)*ISLAND_GRID_SIZE*islandTileHeight/2;
        const margin=(wet?3:1.5)*4;
        if(px+w/2+margin<visible.left || px-w/2-margin>visible.right || py+h/2+margin<visible.top || py-h/2-margin>visible.bottom)continue;
        for(let row=0;row<cloud.shape.length;row++)for(let col=0;col<cloud.shape[row].length;col++)
          if(cloud.shape[row][col]!=='0')ctx.rect(px-w/2+col*cloud.cell,py-h/2+row*cloud.cell,cloud.cell+.02,cloud.cell+.02);
      }
    }
    // One union fill avoids darker overlaps; blur makes the shadow diffuse.
    ctx.fill();ctx.restore();
  }

  let editingDraft = null, editingHouseDraft = null, gameplayDraft = null, gameplayRoute = null;
  let editingSelected = false, editingHouseSelected = false;
  function surfaceHeight(column,row) {
    const world=window.worldManager?.getActive(), size=world?.gridSize;
    const deck=window.BridgeGeometry?.deckHeight(world,{column,row});if(deck!=null)return deck;
    if(!world?.blockStacks || !Number.isInteger(column) || !Number.isInteger(row) || column<0 || row<0 || column>=size || row>=size) return 1;
    return world.blockStacks[row*size+column].length;
  }
  function waterDepth(column,row) {
    const world=window.worldManager?.getActive(), size=world?.gridSize;
    if (!world?.tiles || !Number.isInteger(column) || !Number.isInteger(row) || column<0 || row<0 || column>=size || row>=size) return 0;
    const ramp=world.ramps?.find(r=>r.column===column && r.row===row);
    return (ramp ? ramp.material===3 : world.tiles[row*size+column]===3) && !(world.bridges||[]).some(b=>window.BridgeGeometry?.indexAt(b,{column,row})>=0) ? BLOCK_HEIGHT : 0;
  }
  function characterWaterDepth(character) {
    return Number.isFinite(character.waterDepth) ? Math.max(0,Math.min(BLOCK_HEIGHT,character.waterDepth)) : waterDepth(character.column,character.row);
  }
  function characterCentre(character) {
    const ramp=character.visible && window.worldManager?.getActive()?.ramps?.find(r=>r.column===character.column && r.row===character.row);
    const elevation=Number.isFinite(character.blockHeight) ? character.blockHeight : surfaceHeight(character.column,character.row)+(ramp?.direction ? .5 : 0);
    const p=viewPoint(character.column,character.row);
    return {x: WIDTH / 2 + (p.column - p.row) * ISLAND_TILE_WIDTH / 2,
      y: islandTop + islandTileHeight / 2 + (p.column + p.row) * islandTileHeight / 2 - Math.max(0,elevation-1)*BLOCK_HEIGHT};
  }

  // Shared ground-shadow primitive for surface objects. Each renderer supplies a ground
  // anchor, inset footprint and visual height. Future object types must use this same helper.
  function objectShadowPolygon(anchor, width, depth, height, light) {
    // The light's displayed point is lifted above the ground plane by the sky-frame lift.
    const h = clamp(height, 0, LANGIT_MATAHARI_LIFT * 0.8);
    const ratio = h / (LANGIT_MATAHARI_LIFT - h);
    const dx = (anchor.x - light.x) * ratio;
    const dy = (anchor.y - light.y - LANGIT_MATAHARI_LIFT) * ratio;
    const base = [[anchor.x, anchor.y - depth / 2], [anchor.x + width / 2, anchor.y],
      [anchor.x, anchor.y + depth / 2], [anchor.x - width / 2, anchor.y]];
    const points = [...base, ...base.map(([x, y]) => [x + dx, y + dy])]
      .sort((a, b) => a[0] - b[0] || a[1] - b[1]);
    const cross = (a, b, c) => (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);
    const lower = [], upper = [];
    for (const p of points) { while (lower.length > 1 && cross(lower.at(-2), lower.at(-1), p) <= 0) lower.pop(); lower.push(p); }
    for (const p of points.slice().reverse()) { while (upper.length > 1 && cross(upper.at(-2), upper.at(-1), p) <= 0) upper.pop(); upper.push(p); }
    lower.pop(); upper.pop();
    // Uniformly shrink the full projected shape around the anchor. It must fit within
    // half the sprite width and height, even when a low-angle light stretches it.
    const hull = lower.concat(upper);
    if (!hull.length) return hull;
    const xs = hull.map(p => p[0]), ys = hull.map(p => p[1]);
    const spanX = Math.max(...xs) - Math.min(...xs), spanY = Math.max(...ys) - Math.min(...ys);
    const scale = Math.min(0.5, width * 0.5 / Math.max(spanX, 1e-6), height * 0.5 / Math.max(spanY, 1e-6));
    return hull.map(([x, y]) => [anchor.x + (x - anchor.x) * scale,
      anchor.y + (y - anchor.y) * scale]);
  }
  // A limited, stylized ground projection of the currently visible character silhouette.
  function characterShadowOffset(azimuth, altitude, height) {
    const a=azimuth*Math.PI/180, e=clamp(altitude,8,90)*Math.PI/180;
    const length=Math.min(height*3,height/Math.tan(e));
    return {x:-length*(Math.sin(a)+Math.cos(a))/Math.SQRT2,
      y:-length*(Math.sin(a)-Math.cos(a))*.30};
  }
  const characterShadowMasks=new WeakMap();
  const idleBowController=window.PlayerIdleBow.create();
  const idleBowFrames=Object.fromEntries(['front','back'].map(side=>[side,
    ['transition','concept','sit-lowering','sit-nearly','sitting'].map(pose=>{const image=new Image();image.dataset.playerIdleBow='true';image.src=`./assets/player-idle-${side}-${pose}.png?v=13`;return image;})]));
  let idleBowAmount=0, idleSitAmount=0, idleBowSprite=null, waterEscapeKey=null;
  const idleBlend=document.createElement('canvas');idleBlend.width=512;idleBlend.height=800;
  idleBlend.dataset.playerIdleBow='true';
  let idleBlendKey='';
  function updateIdleBow(now) {
    const world=window.worldManager?.getActive(), character=window.worldManager?.getCharacter();
    const enabled=!REDUCED_MOTION && cameraMode==='gameplay' && !document.hidden && !gameplayDraft && !editingDraft && world?.id!=='base' && character?.visible;
    const key=enabled?`${world.id}:${character.column}:${character.row}`:null;
    idleBowAmount=idleBowController.update(now,key,enabled,character && characterWaterDepth(character)===0); idleBowSprite=null;
    if (!key || !character || characterWaterDepth(character)===0) waterEscapeKey=null;
    if (key && characterWaterDepth(character)>0 && idleBowController.elapsed()>=5000 && waterEscapeKey!==key) {
      waterEscapeKey=key;
      const elapsed=idleBowController.elapsed();
      queueMicrotask(()=>window.dispatchEvent(new CustomEvent('playerwateridle',{detail:{key,elapsed}})));
    }
    const sit=character && characterWaterDepth(character)===0 ? idleBowController.sitting() : 0;
    idleSitAmount=sit;
    if((idleBowAmount>0 || sit>0) && character) {
      const back=viewFacing(character.facing)==='N'||viewFacing(character.facing)==='W', side=back?'back':'front';
      const standing=back?playerBackImage:playerImage, frames=idleBowFrames[side];
      const progress=Math.max(0,Math.min(2,idleBowAmount*2)), index=Math.min(1,Math.floor(progress)), mix=progress-index;
      const poses=[standing,frames[0],frames[1]], sitPoses=[standing,frames[2],frames[3],frames[4]];
      const sitProgress=Math.min(3,sit*3), sitIndex=Math.min(2,Math.floor(sitProgress));
      const from=sit>0?sitPoses[sitIndex]:poses[index], to=sit>0?sitPoses[sitIndex+1]:poses[index+1];
      const weight=sit>0?sitProgress-sitIndex:mix, canvasWidth=sit>0?1280:512;
      if(from.complete && from.naturalWidth && to.complete && to.naturalWidth) {
        const blendKey=`${side}:${sit>0?'sit'+sitIndex:index}:${weight}`;
        if(blendKey!==idleBlendKey) {
          if(idleBlend.width!==canvasWidth)idleBlend.width=canvasWidth;
          const c=idleBlend.getContext('2d');c.clearRect(0,0,canvasWidth,800);c.globalCompositeOperation='source-over';
          c.globalAlpha=1-weight;c.drawImage(from,(canvasWidth-from.naturalWidth)/2,0);
          c.globalCompositeOperation='lighter';c.globalAlpha=weight;c.drawImage(to,(canvasWidth-to.naturalWidth)/2,0);
          c.globalAlpha=1;c.globalCompositeOperation='source-over';idleBlendKey=blendKey;
          characterShadowMasks.delete(idleBlend);
        }
        idleBowSprite=idleBlend;
      }
    }
  }
  document.addEventListener('visibilitychange',()=>idleBowController.reset());
  function activeCharacterSprite(character) {
    const back=viewFacing(character.facing)==='N'||viewFacing(character.facing)==='W';
    let sprite=back?playerBackImage:playerImage;
    if(gameplayDraft && Number.isFinite(gameplayDraft.walkPhase) && !REDUCED_MOTION) {
      const index=((Math.floor(gameplayDraft.walkPhase/(Math.PI*2)*12)%12)+12)%12;
      const frame=gaitFrames[gameplayDraft.running?'run':'walk'][back?'back':'front'][index];
      if(frame.complete && frame.naturalWidth) sprite=frame;
    }
    return sprite;
  }
  function drawCharacterProjectedShadow(light, cloudCover) {
    const world=window.worldManager?.getActive(), character=editingDraft || gameplayDraft || window.worldManager?.getCharacter();
    if(!world || world.id==='base' || !character?.visible || characterWaterDepth(character)>0 || !Number.isFinite(light.azimuth) || light.altitude<=0)return;
    const source=activeCharacterSprite(character), sprite=idleBowSprite || source;
    if(!source.complete || !source.naturalWidth)return;
    let mask=characterShadowMasks.get(sprite);
    if(!mask) {
      mask=document.createElement('canvas');mask.width=sprite.naturalWidth||sprite.width;mask.height=sprite.naturalHeight||sprite.height;
      const m=mask.getContext('2d');m.drawImage(sprite,0,0);m.globalCompositeOperation='source-in';m.fillStyle='#0c1728';m.fillRect(0,0,mask.width,mask.height);
      characterShadowMasks.set(sprite,mask);
    }
    const anchor=characterCentre(character), baseWidth=PLAYER_WIDTH*character.scale,
      width=baseWidth*(idleBowSprite ? (idleBowSprite.naturalWidth||idleBowSprite.width)/432 : 1),height=PLAYER_HEIGHT*character.scale;
    const ray=characterShadowOffset(light.azimuth,light.altitude,height);
    if(Math.abs(ray.y)<height*.12)ray.y=(ray.y<0?-1:1)*height*.12; // visible thickness for side-lit sprites
    const strength=clamp((light.altitude/10),0,1)*(1-.65*clamp(cloudCover/100,0,1));
    ctx.save();applyIslandTransform();clipIslandSurface();ctx.globalAlpha=.34*strength;
    // Bound the complete projected silhouette to the character dimensions. Scale around
    // its ground anchor so the feet stay attached and the light direction is preserved.
    const shadowScale=Math.min(1,baseWidth/(width+2*Math.abs(ray.x)),height/(2*Math.abs(ray.y)));
    ctx.translate(anchor.x,anchor.y);ctx.scale(shadowScale,shadowScale);
    ctx.transform(1,0,-ray.x/height,-ray.y/height,-width/2+ray.x,ray.y);
    if(viewFacing(character.facing)==='W'||viewFacing(character.facing)==='E'){ctx.translate(width,0);ctx.scale(-1,1);}
    ctx.drawImage(mask,0,0,width,height);ctx.restore();
  }
  function drawSurfaceObjectShadows(light, cloudCover, code) {
    const world = worldForObjects();
    if (!world || world.id === 'base') return;
    const shadows = [];
    const house = editingHouseDraft || window.worldManager.getHouse?.();
    const houseSprite = house && (viewFacing(house.facing) === 'N' || viewFacing(house.facing) === 'W') ? houseBackImage : houseImage;
    for(const bridge of world.bridges||[]){const p=bridgeBounds(bridge);shadows.push({anchor:{x:p.x,y:p.y},width:p.w*.8*(bridge.length||3)/3,depth:islandTileHeight*(bridge.length||3)/2,height:4});}
    const trees=world.orangeTrees||[], fences=world.fences||[];
    if (!shadows.length && !trees.length && !fences.length && !house?.visible) return;
    const lightPoint = sunPlanePosition(light.hourAngleDegrees);
    ctx.save(); applyIslandTransform(); clipIslandSurface();
    // Object shadows remain legible even under overcast conditions or virtual moonlight.
    const opacity = Math.max(0.28, cloudShadowOpacity(light, cloudCover, code) * 1.8);
    ctx.fillStyle = `rgba(12,23,40,${opacity})`;
    for (const spec of shadows) {
      const points = objectShadowPolygon(spec.anchor, spec.width, spec.depth, spec.height, lightPoint);
      if (points.length < 3) continue;
      ctx.beginPath(); ctx.moveTo(points[0][0], points[0][1]);
      for (const [x, y] of points.slice(1)) ctx.lineTo(x, y);
      ctx.closePath(); ctx.fill();
    }
    // A soft contact footprint stays attached to the house base even at noon,
    // when the directional projection is short and mostly hidden by the sprite.
    if(house?.visible && window.HouseShadow) {
      const {centre}=houseProjection(house),points=window.HouseShadow.contact(84*house.scale,islandTileHeight*3*house.scale,house.scale,light.azimuth);
      ctx.save();ctx.fillStyle='rgba(12,23,40,.24)';ctx.filter='blur(.4px)';ctx.beginPath();ctx.moveTo(centre.x+points[0].x,centre.y+points[0].y);
      for(const p of points.slice(1))ctx.lineTo(centre.x+p.x,centre.y+p.y);ctx.closePath();ctx.fill();ctx.restore();
    }
    // Fence shadows share the exact endpoints of the visible segment and stay on top faces.
    for(const fence of fences) {
      const {a,b}=fenceGeometry(fence),d=window.FenceShadow.offset(a,b,lightPoint,LANGIT_MATAHARI_LIFT,fence.material==='stone-tall'?127.5:fence.material==='stone'?9:15);
      ctx.fillStyle=`rgba(12,23,40,${opacity*.55})`;ctx.beginPath();ctx.moveTo(a.x,a.y);ctx.lineTo(b.x,b.y);ctx.lineTo(b.x+d.x,b.y+d.y);ctx.lineTo(a.x+d.x,a.y+d.y);ctx.closePath();ctx.fill();
    }
    // The projected alpha preserves branches and canopy, including mirroring and wind sway.
    if(trees.length && window.TreeShadow) {
      ctx.globalAlpha=opacity;ctx.imageSmoothingEnabled=true;
      for(const tree of trees) {
        // A projected blade silhouette separates visually from this short tuft.
        if(tree.kind==='wild-grass'||tree.kind==='bush')continue;
        const sprite=treeSprite(tree);
        if(!sprite.complete || !sprite.naturalWidth)continue;
        let mask=characterShadowMasks.get(sprite);
        if(!mask) {
          mask=document.createElement('canvas');mask.width=sprite.naturalWidth;mask.height=sprite.naturalHeight;
          const m=mask.getContext('2d');m.drawImage(sprite,0,0);m.globalCompositeOperation='source-in';m.fillStyle='#0c1728';m.fillRect(0,0,mask.width,mask.height);
          characterShadowMasks.set(sprite,mask);
        }
        const scale=treeVisualScale(tree),width=treeWidth(tree),height=treeHeight(tree);
        const anchor=orangeTreeAnchor(tree), projection=window.TreeShadow.projection(anchor,lightPoint,width,height,LANGIT_MATAHARI_LIFT,orangeTreeSway(tree)*scale);
        ctx.save();ctx.translate(anchor.x,anchor.y);ctx.scale(projection.scale,projection.scale);ctx.transform(1,0,projection.shear,projection.vertical,0,0);
        if(viewFacing(tree.facing)==='W'||viewFacing(tree.facing)==='E')ctx.scale(-1,1);
        ctx.drawImage(mask,-width/2,-height,width,height);ctx.restore();
      }
    }
    ctx.restore();
  }

  function houseProjection(house) {
    const centre=characterCentre({column:house.column+1,row:house.row+1});
    const geometry=window.HouseGeometry.create(ISLAND_TILE_WIDTH,islandTileHeight,house.scale);
    return {centre,geometry,bottom:centre.y+geometry.halfDepth,mirror:viewFacing(house.facing)==='N'||viewFacing(house.facing)==='E'};
  }
  let editingBridgeSelection=null;
  function bridgeBounds(bridge) {
    const half=((bridge.length||3)-1)/2;
    const centre=characterCentre({column:bridge.column+(bridge.axis==='E'?half:((bridge.width||1)-1)/2),row:bridge.row+(bridge.axis==='S'?half:((bridge.width||1)-1)/2),blockHeight:surfaceHeight(bridge.column,bridge.row)});
    return {x:centre.x,y:centre.y,w:ISLAND_TILE_WIDTH*2.35,h:ISLAND_TILE_WIDTH*1.56};
  }
  function drawWideBridge(bridge) {
    const corners=window.BridgeGeometry.wideDeck(bridge.length||3,bridge.width||1,ISLAND_TILE_WIDTH,islandTileHeight,.1),[a,b,c,d]=corners,wood=bridge.material==='wood';
    const path=(points,fill)=>{ctx.beginPath();ctx.moveTo(points[0].x,points[0].y);for(const p of points.slice(1))ctx.lineTo(p.x,p.y);ctx.closePath();ctx.fillStyle=fill;ctx.fill();ctx.strokeStyle=wood?'#57362c':'#514942';ctx.lineWidth=.35;ctx.stroke();};
    const down=p=>({x:p.x,y:p.y+2.5});
    path([d,c,down(c),down(d)],wood?'#895033':'#827465');path([b,c,down(c),down(b)],wood?'#6c3e2d':'#6d6258');
    path(corners,wood?'#bd8054':'#b3a391');
    ctx.strokeStyle=wood?'#855033':'#897c70';ctx.lineWidth=.35;
    const n=(bridge.length||3)*(wood?4:2);
    for(let i=1;i<n;i++){const t=i/n;ctx.beginPath();ctx.moveTo(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t);ctx.lineTo(d.x+(c.x-d.x)*t,d.y+(c.y-d.y)*t);ctx.stroke();}
    if(!wood)for(let i=1;i<(bridge.width||1)*2;i++){const t=i/((bridge.width||1)*2);ctx.beginPath();ctx.moveTo(a.x+(d.x-a.x)*t,a.y+(d.y-a.y)*t);ctx.lineTo(b.x+(c.x-b.x)*t,b.y+(c.y-b.y)*t);ctx.stroke();}
    for(const [p,q] of [[a,b],[d,c]]) {ctx.strokeStyle=wood?'#71442f':'#6f645b';ctx.lineWidth=2;ctx.beginPath();ctx.moveTo(p.x,p.y-BLOCK_HEIGHT);ctx.lineTo(q.x,q.y-BLOCK_HEIGHT);ctx.stroke();ctx.strokeStyle=wood?'#cb936a':'#c6b9a7';ctx.lineWidth=.55;ctx.stroke();}
    for(const post of corners) {const x=post.x,y=post.y+2;
      path([{x:x-2,y:y-9.5},{x,y:y-8.5},{x,y},{x:x-2,y:y-1}],wood?'#a45f3d':'#988879');
      path([{x,y:y-8.5},{x:x+2,y:y-9.5},{x:x+2,y:y-1},{x,y}],wood?'#78472f':'#756b61');
      path([{x:x-2,y:y-9.5},{x,y:y-10.5},{x:x+2,y:y-9.5},{x,y:y-8.5}],wood?'#c9895e':'#b7a896');
    }
    if(editingBridgeSelection && editingBridgeSelection.column===bridge.column && editingBridgeSelection.row===bridge.row && editingBridgeSelection.axis===bridge.axis){
      ctx.strokeStyle='#fff4a8';ctx.lineWidth=.65;ctx.beginPath();ctx.moveTo(a.x,a.y);for(const p of [b,c,d])ctx.lineTo(p.x,p.y);ctx.closePath();ctx.stroke();
    }
  }
  function drawBridge(bridge) {
    const p=bridgeBounds(bridge);ctx.save();ctx.globalAlpha*=(100-(bridge.opacity ?? 0))/100;applyIslandTransform();ctx.translate(p.x,p.y);if((viewQuarterTurns%2===0?bridge.axis:bridge.axis==='E'?'S':'E')==='E')ctx.scale(-1,1);
    drawWideBridge(bridge);ctx.restore();
  }
  function drawHouse() {
    const house = editingHouseDraft || window.worldManager?.getHouse?.();
    if (!house?.visible) return;
    const back=viewFacing(house.facing)==='N'||viewFacing(house.facing)==='W';
    const sprite=back?houseBackImage:houseImage;
    if (!sprite.complete || !sprite.naturalWidth || !window.HouseGeometry) return;
    const {centre,geometry:g,bottom,mirror}=houseProjection(house);
    ctx.save();applyIslandTransform();ctx.imageSmoothingEnabled=true;ctx.imageSmoothingQuality='high';
    ctx.translate(centre.x,bottom);if(mirror)ctx.scale(-1,1);
    // Independently align the two visible foundation faces with the tile axes.
    for(const [from,to,side] of [[0,g.pivotX,g.left],[g.pivotX,512,g.right]]) {
      ctx.save();ctx.transform(side.sx,side.shear,0,g.sy,0,0);
      ctx.drawImage(sprite,from,0,to-from,490,from-g.pivotX,-g.pivotY,to-from,490);ctx.restore();
    }
    ctx.restore();
    if(editingHouseSelected) {
      const points=[[0,0],[g.pivotX,0],[512,0],[0,490],[g.pivotX,490],[512,490]].map(([x,y])=>window.HouseGeometry.project(g,x,y));
      const xs=points.map(p=>p.x*(mirror?-1:1)), ys=points.map(p=>p.y);
      ctx.save();applyIslandTransform();ctx.strokeStyle='#fff4a8';ctx.lineWidth=1;
      ctx.strokeRect(centre.x+Math.min(...xs)-2,bottom+Math.min(...ys)-2,Math.max(...xs)-Math.min(...xs)+4,Math.max(...ys)-Math.min(...ys)+4);ctx.restore();
    }
  }

  // Footprint centre, shared by rendering, hit testing and shadows.
  function orangeTreeAnchor(tree) {
    const offset=(treeSize(tree)-1)/2;
    const centre=characterCentre({column:tree.column+offset,row:tree.row+offset,blockHeight:surfaceHeight(tree.column,tree.row)});
    // The foliage base sits slightly forward on its tile, with selection and shadow sharing the same anchor.
    if(tree.kind==='wild-grass'||tree.kind==='bush') centre.y+=0.75;
    return centre;
  }
  const treeAlphaBoundsCache=new WeakMap();
  function treeAlphaBounds(sprite) {
    if(treeAlphaBoundsCache.has(sprite))return treeAlphaBoundsCache.get(sprite);
    const canvas=document.createElement('canvas');canvas.width=128;canvas.height=128;
    const c=canvas.getContext('2d',{willReadFrequently:true});c.drawImage(sprite,0,0,128,128);
    const pixels=c.getImageData(0,0,128,128).data;let left=128,top=128,right=0,bottom=0;
    for(let y=0;y<128;y++)for(let x=0;x<128;x++)if(pixels[(y*128+x)*4+3]>32) {
      left=Math.min(left,x);right=Math.max(right,x+1);top=Math.min(top,y);bottom=Math.max(bottom,y+1);
    }
    const bounds=left<right?{left:left/128,top:top/128,right:right/128,bottom:bottom/128}:null;
    treeAlphaBoundsCache.set(sprite,bounds);return bounds;
  }
  let editingTreeSelection=null, editingTreeGroup=[], editingFenceGroup=[];
  let editingBlockSelection=null;
  function drawSelectedBlock() {
    if(!editingBlockSelection)return;
    const b=editingBlockSelection,w=worldForObjects(),i=b.row*ISLAND_GRID_SIZE+b.column,stack=w?.blockStacks?.[i] || (w?.tiles?.[i]?[w.tiles[i]]:[]);
    if(b.level<0 || b.level>=stack.length)return;
    const c=characterCentre({column:b.column,row:b.row,blockHeight:b.level+1}),hw=ISLAND_TILE_WIDTH/2,hh=islandTileHeight/2;
    ctx.save();applyIslandTransform();ctx.strokeStyle='#fff4a8';ctx.lineWidth=.8;ctx.beginPath();ctx.moveTo(c.x,c.y-hh);ctx.lineTo(c.x+hw,c.y);ctx.lineTo(c.x,c.y+hh);ctx.lineTo(c.x-hw,c.y);ctx.closePath();ctx.stroke();ctx.restore();
  }
  let orangeTreeMotionState={time:0,speed:0,direction:90,heading:0};
  function orangeTreeSway(tree) {
    const m=orangeTreeMotionState;
    return window.OrangeTreeMotion?.sway(!REDUCED_MOTION,m.speed,m.direction,m.heading,m.time,tree.column,tree.row) || 0;
  }
  function objectSway(tree) {
    if(!['wild-grass','bush'].includes(tree.kind))return orangeTreeSway(tree)*treeVisualScale(tree);
    const m=orangeTreeMotionState;
    if(REDUCED_MOTION || !Number.isFinite(m.speed) || m.speed<=0)return 0;
    const bearing=(m.direction+180-m.heading)*Math.PI/180;
    const sign=Math.sin(bearing)+.25*Math.cos(bearing)<0?-1:1;
    const amplitude=Math.min(2.4,.65+m.speed*.12)*(tree.size===2?2:1);
    return sign*amplitude*Math.sin(m.time*1.25+tree.column*1.73+tree.row*2.31);
  }
  function drawOrangeTree(tree,motionTime,sceneWeather) {
    const sprite=treeSprite(tree);
    if(!sprite.complete || !sprite.naturalWidth)return;
    const centre=orangeTreeAnchor(tree);
    const scale=treeVisualScale(tree),bottom=centre.y, width=treeWidth(tree),height=treeHeight(tree);
    const sway=objectSway(tree);
    ctx.save();applyIslandTransform();
    // Contact is anchored to the same bottom as the artwork, and clipped to top faces.
    ctx.save();clipIslandSurface();ctx.fillStyle='rgba(28,49,35,0.18)';ctx.beginPath();
    ctx.ellipse(centre.x,bottom,tree.kind==='bush'?5*treeSize(tree):tree.kind==='wild-grass'?5:Math.min(width*.16,8*treeSize(tree)),0.65*treeSize(tree),0,0,Math.PI*2);ctx.fill();ctx.restore();
    ctx.translate(centre.x,bottom);ctx.transform(1,0,-sway/height,1,0,0);
    ctx.imageSmoothingEnabled=true;ctx.imageSmoothingQuality='high';
    if(viewFacing(tree.facing)==='W'||viewFacing(tree.facing)==='E')ctx.scale(-1,1);
    ctx.drawImage(sprite,-width/2,-height,width,height);ctx.restore();
    if((editingTreeSelection && tree.column===editingTreeSelection.column && tree.row===editingTreeSelection.row) || editingTreeGroup.some(t=>tree.column===t.column && tree.row===t.row)) {
      const b=treeAlphaBounds(sprite);
      if(b) {
        ctx.save();applyIslandTransform();ctx.translate(centre.x,bottom);ctx.transform(1,0,-sway/height,1,0,0);
        if(viewFacing(tree.facing)==='W'||viewFacing(tree.facing)==='E')ctx.scale(-1,1);
        ctx.strokeStyle='#fff4a8';ctx.lineWidth=.6;
        ctx.strokeRect(-width/2+b.left*width,-height+b.top*height,(b.right-b.left)*width,(b.bottom-b.top)*height);ctx.restore();
      }
    }
  }

  let grassPairsCache={trees:null,stacks:null,pairs:[]};
  function grassPairs(world) {
    const trees=world?.orangeTrees,stacks=world?.blockStacks;
    if(grassPairsCache.trees!==trees || grassPairsCache.stacks!==stacks)grassPairsCache={trees,stacks,pairs:window.GrassClusters?.pairs(trees,surfaceHeight)||[]};
    return grassPairsCache.pairs;
  }
  function drawGrassBridge(a,b) {
    if(!wildGrassImage.complete || !wildGrassImage.naturalWidth)return;
    const p=orangeTreeAnchor(a),q=orangeTreeAnchor(b),x=(p.x+q.x)/2,y=(p.y+q.y)/2;
    const sway=(objectSway(a)+objectSway(b))/2;
    ctx.save();ctx.globalAlpha*=(100-Math.max(a.opacity ?? 0,b.opacity ?? 0))/100;applyIslandTransform();ctx.save();clipIslandSurface();ctx.fillStyle='rgba(28,49,35,.14)';ctx.beginPath();ctx.ellipse(x,y,4.5,.55,0,0,Math.PI*2);ctx.fill();ctx.restore();
    ctx.translate(x,y);ctx.transform(1,0,-sway/11,1,0,0);ctx.imageSmoothingEnabled=true;ctx.imageSmoothingQuality='high';
    ctx.drawImage(wildGrassImage,-11,-11,22,11);ctx.restore();
  }
  // Fade foreground artwork when opaque pixels overlap sampled points in the player bounds.
  // This is an approximation, not a pixel-perfect character mask.
  function coversCharacter(player, hit, bounds) {
    if(!player?.visible || !window.sceneEditing)return false;
    const anchor=characterCentre(player), scale=player.scale || 1;
    const bottom=anchor.y+.5*scale+characterWaterDepth(player)-(player.jumpHeight||0), height=PLAYER_HEIGHT*scale, width=PLAYER_WIDTH*scale;
    if(bounds && (anchor.x+width/2<=bounds.left || anchor.x-width/2>=bounds.right || bottom<=bounds.top || bottom-height>=bounds.bottom))return false;
    for(let yi=1;yi<10;yi++)for(let xi=1;xi<6;xi++) {
      const p={x:anchor.x-width/2+width*xi/6,y:bottom-height+height*yi/10};
      if(hit(p))return true;
    }
    return false;
  }
  function treeContains(tree,point) {
    const sprite=treeSprite(tree);if(!sprite.complete || !sprite.naturalWidth)return false;
    const centre=orangeTreeAnchor(tree),width=treeWidth(tree),height=treeHeight(tree),bottom=centre.y;
    const x=point.x-centre.x+(point.y-bottom)*objectSway(tree)/height;
    let u=(x+width/2)/width,v=(point.y-bottom+height)/height;
    if(u<0||u>=1||v<0||v>=1)return false;
    if(viewFacing(tree.facing)==='W'||viewFacing(tree.facing)==='E')u=1-u;
    try {
      let data=houseHitCache.get(sprite);
      if(!data){const c=document.createElement('canvas');c.width=128;c.height=128;const h=c.getContext('2d',{willReadFrequently:true});h.drawImage(sprite,0,0,128,128);data=h.getImageData(0,0,128,128).data;houseHitCache.set(sprite,data);}
      return data[(Math.floor(v*128)*128+Math.floor(u*128))*4+3]>32;
    }catch(_){return false;}
  }
  function orangeDropPosition(tree) {
    const p=orangeTreeAnchor(tree);return {x:p.x+6,y:p.y+1};
  }
  let objectDragWorld=null;
  function worldForObjects() {const world=window.worldManager?.getActive();return objectDragWorld?.id===world?.id?objectDragWorld:world;}
  let joinCache={fences:null,stacks:null,turns:null,plan:[]};
  function fenceJoins(world) {
    const fences=world?.fences,stacks=world?.blockStacks;
    if(joinCache.fences!==fences || joinCache.stacks!==stacks || joinCache.turns!==viewQuarterTurns){
      const projected=(fences||[]).map(f=>({...f,...window.WorldViewRotation.fence(f,ISLAND_GRID_SIZE,viewQuarterTurns)}));
      joinCache={fences,stacks,turns:viewQuarterTurns,plan:window.FenceJoins.plan(projected,(c,r)=>{const p=worldPoint(c,r);return surfaceHeight(p.column,p.row);})};
    }
    return joinCache.plan;
  }
  let editingFenceSelection=null;
  function fenceGeometry(f) {
    const c=characterCentre(f),hw=ISLAND_TILE_WIDTH/2,hh=islandTileHeight/2;
    let a,b;
    switch(viewFacing(f.edge)) {
      case 'N':a={x:c.x,y:c.y-hh};b={x:c.x+hw,y:c.y};break;
      case 'E':a={x:c.x,y:c.y+hh};b={x:c.x+hw,y:c.y};break;
      case 'S':a={x:c.x-hw,y:c.y};b={x:c.x,y:c.y+hh};break;
      default:a={x:c.x-hw,y:c.y};b={x:c.x,y:c.y-hh};
    }
    return {a,b};
  }
  function drawFence(f,join) {
    const g=fenceGeometry(f);ctx.save();ctx.globalAlpha*=(100-(f.opacity ?? 0))/100;applyIslandTransform();
    ctx.save();clipIslandSurface();ctx.strokeStyle='rgba(12,23,40,.16)';ctx.lineWidth=1.2;ctx.beginPath();ctx.moveTo(g.a.x,g.a.y);ctx.lineTo(g.b.x,g.b.y);ctx.stroke();ctx.restore();
    const player=editingDraft || gameplayDraft || window.worldManager?.getCharacter?.();
    if(player?.visible && f.column===Math.round(player.column) && f.row===Math.round(player.row) && (viewFacing(f.edge)==='S'||viewFacing(f.edge)==='E'))ctx.globalAlpha*=.5;
    const selected=(editingFenceSelection && editingFenceSelection.column===f.column && editingFenceSelection.row===f.row && editingFenceSelection.edge===f.edge) || editingFenceGroup.some(t=>t.column===f.column && t.row===f.row && t.edge===f.edge);
    ctx.lineWidth=.30;ctx.lineJoin='round';
    for(const face of window.FenceMesh.faces(g.a,g.b,f.material,join)) {
      ctx.beginPath();ctx.moveTo(face.points[0].x,face.points[0].y);for(const p of face.points.slice(1))ctx.lineTo(p.x,p.y);ctx.closePath();
      ctx.fillStyle=face.color;ctx.fill();if(!face.detail){ctx.strokeStyle=selected?'#fff4a8':(f.material.startsWith('stone')?'#687666':'#664936');ctx.stroke();}
    }
    ctx.restore();
  }
  function fenceAt(point) {
    const world=worldForObjects(),fences=world?.fences||[],joins=fenceJoins(world);
    // Use the same painter order as drawing, including stable array order for ties.
    const order=fences.map((f,i)=>({i,depth:fenceViewDepth(f)})).sort((a,b)=>b.depth-a.depth || b.i-a.i);
    for(const {i} of order) {
      const f=fences[i],g=fenceGeometry(f);
      if(window.FenceMesh.faces(g.a,g.b,f.material,joins[i]).some(face=>window.FenceMesh.contains(face.points,point.x,point.y)))return i;
    }
    return null;
  }
  function drawOrangeDrop(tree) {
    const world=window.worldManager?.getActive(),now=Date.now();
    if(tree.kind!==undefined || !window.OrangeDrops?.available(world,tree,now))return;
    const p=orangeDropPosition(tree), progress=window.OrangeDrops.falling(now,REDUCED_MOTION);
    const y=p.y-(1-progress*progress)*ORANGE_TREE_HEIGHT*.58;
    ctx.save();applyIslandTransform();ctx.fillStyle='#b76a25';ctx.fillRect(p.x-1.5,y-1,3,3);
    ctx.fillStyle='#eb9c35';ctx.fillRect(p.x-1,y-1.3,2,2);ctx.fillStyle='#668043';ctx.fillRect(p.x-.3,y-2.1,1.4,.7);ctx.restore();
  }

  function drawGameplayRoute() {
    if(cameraMode!=='gameplay' || !gameplayRoute || gameplayRoute.tiles.length<2)return;
    const {tiles,next}=gameplayRoute, character=gameplayDraft || window.worldManager?.getCharacter();
    const remaining=tiles.slice(Math.max(1,next));if(!remaining.length)return;
    const start=characterCentre(character || tiles[0]);
    ctx.save();applyIslandTransform();ctx.lineJoin='round';ctx.lineCap='butt';ctx.setLineDash([2.5,2]);
    ctx.strokeStyle='#000';ctx.lineWidth=.5;ctx.beginPath();ctx.moveTo(start.x,start.y);
    for(const tile of remaining){const p=characterCentre(tile);ctx.lineTo(p.x,p.y);}ctx.stroke();
    const p=characterCentre(remaining[remaining.length-1]),hw=ISLAND_TILE_WIDTH/2-1,hh=islandTileHeight/2-.5;
    ctx.beginPath();ctx.moveTo(p.x,p.y-hh);ctx.lineTo(p.x+hw,p.y);ctx.lineTo(p.x,p.y+hh);ctx.lineTo(p.x-hw,p.y);ctx.closePath();ctx.stroke();ctx.restore();
  }
  function drawStandingBlock() {
    const world=window.worldManager?.getActive(),character=editingDraft || gameplayDraft || window.worldManager?.getCharacter?.();
    if(!character?.visible || !world || world.id==='base')return;
    const column=Math.round(character.column),row=Math.round(character.row);
    const size=world.gridSize||ISLAND_GRID_SIZE;
    if(column<0 || row<0 || column>=size || row>=size || (world.tiles && !world.tiles[row*size+column] && window.BridgeGeometry?.deckHeight(world,{column,row})==null))return;
    const {x,y}=characterCentre({column,row}),hw=ISLAND_TILE_WIDTH/2-.45,hh=islandTileHeight/2-.25;
    const ramp=world.ramps?.find(r=>r.column===column && r.row===row),shape=ramp && window.RampGeometry?.create(viewFacing(ramp.direction),ISLAND_TILE_WIDTH,islandTileHeight,BLOCK_HEIGHT);
    ctx.save();applyIslandTransform();ctx.strokeStyle='rgba(30,44,31,.86)';ctx.lineWidth=1.1;ctx.lineJoin='round';ctx.beginPath();
    if(shape){ctx.moveTo(x+shape.topFace[0].x,y+shape.topFace[0].y);for(const p of shape.topFace.slice(1))ctx.lineTo(x+p.x,y+p.y);}
    else {ctx.moveTo(x,y-hh);ctx.lineTo(x+hw,y);ctx.lineTo(x,y+hh);ctx.lineTo(x-hw,y);}
    ctx.closePath();ctx.stroke();ctx.restore();
  }
  function drawHoeTarget() {
    if(cameraMode !== 'gameplay' || !document.querySelector('[data-dock="hoe"][aria-pressed="true"], [data-dock="hand"][aria-pressed="true"]'))return;
    const world=window.worldManager?.getActive(), character=gameplayDraft || window.worldManager?.getCharacter();
    if(!world || world.id==='base' || !character?.visible)return;
    const offset={N:[0,-1],E:[1,0],S:[0,1],W:[-1,0]}[character.facing];if(!offset)return;
    const column=Math.round(character.column)+offset[0],row=Math.round(character.row)+offset[1];
    const size=world.gridSize||ISLAND_GRID_SIZE;
    if(column<0 || row<0 || column>=size || row>=size || (world.tiles && !world.tiles[row*size+column] && window.BridgeGeometry?.deckHeight(world,{column,row})==null))return;
    const {x,y}=characterCentre({column,row}),hw=ISLAND_TILE_WIDTH/2-.5,hh=islandTileHeight/2-.3;
    ctx.save();applyIslandTransform();ctx.strokeStyle='#fff';ctx.lineWidth=1.2;ctx.lineJoin='round';ctx.beginPath();
    ctx.moveTo(x,y-hh);ctx.lineTo(x+hw,y);ctx.lineTo(x,y+hh);ctx.lineTo(x-hw,y);ctx.closePath();ctx.stroke();ctx.restore();
  }
  const sparrowImage=new Image();sparrowImage.src='./assets/pet-sparrow.png?v=1';
  const sparrowFlap=new Image();sparrowFlap.src='./assets/pet-sparrow-flap.png?v=3';
  const sparrowBack=new Image();sparrowBack.src='./assets/pet-sparrow-back.png?v=2';
  const sparrowBackFlap=new Image();sparrowBackFlap.src='./assets/pet-sparrow-back-flap.png?v=4';
  for(const image of [sparrowImage,sparrowFlap,sparrowBack,sparrowBackFlap]) image.onload=()=>{if(REDUCED_MOTION)drawFrame();};
  const sparrow=window.SparrowPet.create();
  let foregroundSparrow = null;
  function drawMainCharacter(motionTime) {
    const world = window.worldManager?.getActive();
    if (!world || world.id === 'base') return;
    const character = editingDraft || gameplayDraft || window.worldManager.getCharacter();
    if (!character.visible) return;
    const back = viewFacing(character.facing) === 'N' || viewFacing(character.facing) === 'W';
    const mirror = viewFacing(character.facing) === 'W' || viewFacing(character.facing) === 'E';
    const sprite = back ? playerBackImage : playerImage;
    if (!sprite.complete || !sprite.naturalWidth) return;
    const anchor = characterCentre(character), depth=characterWaterDepth(character), x=anchor.x;
    const seatWeight=idleBowSprite ? window.PlayerIdleBow.seatGroundWeight(idleSitAmount) : 0;
    // Put the hips, rather than the frontmost boot, on the tile's surface.
    const seatY=back?778:725;
    const groundY=anchor.y + .5 * character.scale + depth + (800-seatY)/800 * PLAYER_HEIGHT * character.scale * seatWeight;
    const y=groundY-(character.jumpHeight||0);
    const width = PLAYER_WIDTH * character.scale * (idleBowSprite ? (idleBowSprite.naturalWidth||idleBowSprite.width)/432 : 1);
    // Breathing changes height only, never the ground anchor. With reduced motion time is zero.
    const idle = Math.sin(motionTime * Math.PI / 1.2) > 0.5 ? 0.5 : 0;
    const height = PLAYER_HEIGHT * character.scale + idle;
    ctx.save();
    applyIslandTransform();
    // The intact sprite avoids exposed seams until independently drawn gait frames exist.
    if(depth===0) {
      ctx.fillStyle='rgba(12,23,30,.38)';ctx.globalAlpha=1-seatWeight;
      ctx.beginPath();ctx.ellipse(x,groundY-.45*character.scale,4.6*character.scale,.8*character.scale,0,0,Math.PI*2);ctx.fill();
      if(seatWeight>0) {
        // Contact patches follow the illustrated hip and supporting palms, including mirrors.
        const contacts=back?[[600,778,2.5],[395,774,1.3],[835,795,1.3]]:[[680,725,2.5],[850,751,1.3]];
        const dir=mirror?-1:1;ctx.globalAlpha=seatWeight;
        for(const [px,py,r] of contacts) {
          const cx=x+dir*(px-640)*width/1280,cy=y+(py-800)*height/800;
          ctx.beginPath();ctx.ellipse(cx,cy,r*character.scale,.45*character.scale,0,0,Math.PI*2);ctx.fill();
        }
      }
      ctx.globalAlpha=1;
    }
    ctx.imageSmoothingEnabled = false;
    const drawnSprite=idleBowSprite || activeCharacterSprite(character);
    function drawSprite() {ctx.save();ctx.translate(x,y);if (mirror) ctx.scale(-1,1);ctx.drawImage(drawnSprite,-width/2,-height,width,height);ctx.restore();}
    if(depth>0) {
      // The portion below the surface is translucent against the already drawn water.
      const waterline=groundY-depth;
      ctx.save();ctx.beginPath();ctx.rect(x-width/2-2,y-height-2,width+4,Math.max(0,waterline-(y-height-2)));ctx.clip();drawSprite();ctx.restore();
      ctx.save();ctx.beginPath();
      ctx.moveTo(x-ISLAND_TILE_WIDTH/2,waterline);ctx.lineTo(x,waterline-islandTileHeight/2);
      ctx.lineTo(x+ISLAND_TILE_WIDTH/2,waterline);ctx.lineTo(x,waterline+islandTileHeight/2);ctx.closePath();ctx.clip();
      ctx.beginPath();ctx.rect(x-width/2-2,waterline,width+4,Math.max(0,y-waterline+2));ctx.clip();ctx.globalAlpha*=0.35;drawSprite();ctx.restore();
      ctx.save();ctx.strokeStyle='rgba(218,250,244,.6)';ctx.lineWidth=.45;ctx.beginPath();ctx.ellipse(x,waterline,Math.min(width*.48,5),1.1,0,0,Math.PI*2);ctx.stroke();ctx.restore();
    } else drawSprite();
    if(sparrowImage.complete && sparrowImage.naturalWidth) {
      const headY=y-height+seatWeight*(back?2.9:2.1)*character.scale;
      const p=sparrow.update(performance.now(),world.id,anchor,character.scale,seatWeight,mirror,back,!!gameplayDraft,REDUCED_MOTION,headY);
      const size=6*character.scale;
      if(p.seat<.5 && depth===0){ctx.fillStyle='rgba(12,23,30,.22)';ctx.beginPath();ctx.ellipse(p.x,anchor.y+.5*character.scale,.9*character.scale,.3*character.scale,0,0,Math.PI*2);ctx.fill();}
      const rest=back?sparrowBack:sparrowImage, flap=back?sparrowBackFlap:sparrowFlap;
      const bird=p.flap && flap.complete && flap.naturalWidth ? flap : rest;
      if(bird.complete && bird.naturalWidth) {
        const drawBird=()=>{ctx.save();applyIslandTransform();ctx.imageSmoothingEnabled=false;ctx.translate(p.x,p.y);if(mirror)ctx.scale(-1,1);
          ctx.drawImage(bird,-size/2,-size,size,size);ctx.restore();};
        if(p.seat < 1) foregroundSparrow=drawBird;
        else {ctx.save();ctx.imageSmoothingEnabled=false;ctx.translate(p.x,p.y);if(mirror)ctx.scale(-1,1);ctx.drawImage(bird,-size/2,-size,size,size);ctx.restore();}
      }
    }
    if (editingSelected) {
      ctx.strokeStyle = '#fff4a8';
      ctx.lineWidth = 1;
      ctx.strokeRect(x - width / 2 - 2, y - height - 2, width + 4, height + 4);
    }
    ctx.restore();
  }

  let pondCache={tiles:null,stacks:null,id:null,layout:null};
  let pondWind={x:0,y:0,last:null};
  function pondLayout(world) {
    if(!window.PondLife || !world?.tiles) return null;
    if(pondCache.tiles!==world.tiles || pondCache.stacks!==world.blockStacks || pondCache.id!==world.id) {
      pondCache={tiles:world.tiles,stacks:world.blockStacks,id:world.id,layout:window.PondLife.build(world)};
    }
    return pondCache.layout;
  }
  function updatePondWind(time,sceneWeather) {
    if(REDUCED_MOTION) return;
    const dt=pondWind.last===null ? 0 : clamp(time-pondWind.last,0,.25);pondWind.last=time;
    const speed=Number.isFinite(sceneWeather.windSpeed) ? Math.max(0,sceneWeather.windSpeed) : 0;
    const direction=Number.isFinite(sceneWeather.windDirection) ? sceneWeather.windDirection : 0;
    const angle=(direction+180)*Math.PI/180;
    pondWind.x+=Math.sin(angle)*speed*dt;pondWind.y-=Math.cos(angle)*speed*dt;
  }
  function drawPondLife(tile,items,motionTime) {
    if(!items?.length || !window.PondLife) return;
    const hw=ISLAND_TILE_WIDTH/2,hh=islandTileHeight/2;
    ctx.save();ctx.beginPath();ctx.moveTo(tile.x,tile.y-hh);ctx.lineTo(tile.x+hw,tile.y);
    ctx.lineTo(tile.x,tile.y+hh);ctx.lineTo(tile.x-hw,tile.y);ctx.closePath();ctx.clip();
    // Fish are painted before pads, underneath the floating leaves.
    for(const type of ['fish','lilypad']) for(const item of items) {
      if(item.type!==type)continue;
      const pos=window.PondLife.position(item,motionTime,pondWind.x,pondWind.y,REDUCED_MOTION);
      const x=tile.x+(pos.u-pos.v)*hw, y=tile.y+(pos.u+pos.v)*hh;
      if(type==='fish') {
        ctx.save();ctx.translate(x,y);ctx.scale(pos.direction,1);ctx.fillStyle='rgba(33,77,79,.47)';
        ctx.beginPath();ctx.ellipse(0,0,2.2,.85,0,0,Math.PI*2);ctx.fill();
        ctx.beginPath();ctx.moveTo(-1.6,0);ctx.lineTo(-3,-1.1);ctx.lineTo(-3,1.1);ctx.closePath();ctx.fill();ctx.restore();
      } else {
        ctx.fillStyle='#557f4b';ctx.beginPath();ctx.ellipse(x,y,2.5,1.25,0,0,Math.PI*2);ctx.fill();
        ctx.strokeStyle='#a9ca89';ctx.lineWidth=.4;ctx.beginPath();ctx.moveTo(x,y);ctx.lineTo(x+1.9,y-1);ctx.stroke();
      }
    }
    ctx.restore();
  }

  function drawFloatingIsland(sunAltitude, sunAzimuth, sunHourAngle, cloudCover, code,
    moon, cloudStates, motionTime, sceneWeather, sun) {
    ctx.save();
    applyIslandTransform();
    const palette = islandPalette(sunAltitude, cloudCover, code);
    const sunlight = islandSunlight(sunAltitude, cloudCover, code);
    const centerX = WIDTH / 2;
    const leftX = centerX - ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH / 2;
    const rightX = centerX + ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH / 2;
    const topY = islandTop;
    const middleY = topY + ISLAND_GRID_SIZE * islandTileHeight / 2;
    const bottomY = topY + ISLAND_GRID_SIZE * islandTileHeight;
    const lowerY = bottomY + ISLAND_DEPTH;
    const grassBandDepth = ISLAND_GRASS_BAND_DEPTH;
    const soilFrontInsetY = ISLAND_GRASS_OVERHANG / 2;
    const soilFrontY = bottomY - soilFrontInsetY;
    const soilLeftX = leftX + ISLAND_GRASS_OVERHANG;
    const soilRightX = rightX - ISLAND_GRASS_OVERHANG;

    // The sun lights the island by day; once it is gone the moon takes over, so the cast
    // shadow follows whichever body is actually in the sky. With the water gone the plate's shadow
    // lands straight on the ground again, so it is drawn here, in the same pass as the plate.
    // The island keeps its original independent shadow, not the cloud ray projection.
    const light = pickLightSource(sunAltitude, sunHourAngle, moon, sun);
    drawIslandShadow(centerX, lowerY, light.altitude, light.hourAngle, cloudCover, code, light.declination);
    const activeWorld = window.worldManager?.getActive();
    const tileData = activeWorld?.tiles, stacks=activeWorld?.blockStacks;
    const blockOpacity=new Map((activeWorld?.blockOpacity||[]).map(e=>[`${e.row*ISLAND_GRID_SIZE+e.column}:${e.level}`,e.opacity]));
    const ramps=new Map((activeWorld?.ramps||[]).map(r=>[r.row*ISLAND_GRID_SIZE+r.column,r]));
    const pond=pondLayout(activeWorld);updatePondWind(motionTime,sceneWeather);
    if (tileData && (stacks || blockOpacity.size || ramps.size || tileData.some(value => value !== 1))) {
      for (const tile of ISLAND_TILES) {
        const index=tile.row * ISLAND_GRID_SIZE + tile.column, stack=stacks?.[index];
        if (!tileData[index]) continue;
        // An untouched Base-template column surrounded on all sides by identical,
        // opaque columns has no visible lower faces. Avoid painting its four
        // buried courses; preserve the full path near edits, holes and opacity.
        const baseLayers=window.BaseTerrain?.LAYERS;
        const matchesBase=s=>s && baseLayers && s.length===baseLayers.length && s.every((v,i)=>v===baseLayers[i]);
        const buried=stacks && !blockOpacity.size && matchesBase(stack) && [[0,-1],[1,0],[0,1],[-1,0]].every(([dc,dr])=>{
          const c=tile.column+dc,r=tile.row+dr;
          return c>=0 && r>=0 && c<ISLAND_GRID_SIZE && r<ISLAND_GRID_SIZE && matchesBase(stacks[r*ISLAND_GRID_SIZE+c]);
        });
        if (!buried && tileVisible(tile, ISLAND_DEPTH)) {
          ctx.save();ctx.globalAlpha*=(100-(blockOpacity.get(`${index}:0`)||0))/100;
          const hw = ISLAND_TILE_WIDTH / 2, hh = islandTileHeight / 2;
          // Full-depth faces expose the silhouette and the soil beneath each remaining tile.
          const left=worldNeighbor(tile.column,tile.row,'S'),right=worldNeighbor(tile.column,tile.row,'E');
          const empty=p=>p.column<0 || p.row<0 || p.column>=ISLAND_GRID_SIZE || p.row>=ISLAND_GRID_SIZE || !tileData[p.row*ISLAND_GRID_SIZE+p.column];
          const leftOpen=empty(left),rightOpen=empty(right);
          if (leftOpen) drawSmoothIslandPolygon([[tile.x-hw,tile.y],[tile.x,tile.y+hh],[tile.x,tile.y+hh+ISLAND_DEPTH],[tile.x-hw,tile.y+ISLAND_DEPTH]], baseSoilColor(palette), baseSoilColor(palette));
          if (rightOpen) drawSmoothIslandPolygon([[tile.x,tile.y+hh],[tile.x+hw,tile.y],[tile.x+hw,tile.y+ISLAND_DEPTH],[tile.x,tile.y+hh+ISLAND_DEPTH]], baseSoilColor(palette), baseSoilColor(palette));
          drawGrassTile(tile, palette, sunlight, stack ? stack[0] : tileData[index],0,stack ? !!window.BaseTerrain?.isBaseLayer(stack,0) : true,!!stack && stack.length>1 && stack[0]===1);ctx.restore();
        }
        if (stack) for(let level=1;level<stack.length;level++) {
          if(buried && level<stack.length-1)continue;
          const raised={...tile,y:tile.y-level*BLOCK_HEIGHT};
          if(tileVisible(raised,BLOCK_HEIGHT)){ctx.save();ctx.globalAlpha*=(100-(blockOpacity.get(`${index}:${level}`)||0))/100;drawGrassTile(raised,palette,sunlight,stack[level],level,!!window.BaseTerrain?.isBaseLayer(stack,level),level<stack.length-1 && stack[level]===1 && !!window.BaseTerrain?.isBaseLayer(stack,level));ctx.restore();}
        }
        if(tileData[index]===3 && !ramps.has(index)) {
          const level=stack ? stack.length-1 : 0,topTile={...tile,y:tile.y-level*BLOCK_HEIGHT+WATER_SURFACE_DROP};
          if(tileVisible(topTile,BLOCK_HEIGHT)){ctx.save();ctx.globalAlpha*=(100-(blockOpacity.get(`${index}:${level}`)||0))/100;drawWaterFlow(topTile,motionTime,activeWorld);ctx.restore();}
        }
        if(tileData[index]===3 && pond?.byTile.has(index)) {
          const level=stack ? stack.length-1 : 0, topTile={...tile,y:tile.y-level*BLOCK_HEIGHT+WATER_SURFACE_DROP};
          if(tileVisible(topTile,BLOCK_HEIGHT)){ctx.save();ctx.globalAlpha*=(100-(blockOpacity.get(`${index}:${level}`)||0))/100;drawPondLife(topTile,pond.byTile.get(index),motionTime);ctx.restore();}
        }
        if(stack && tileData[index]===3) {
          const level=stack ? stack.length-1 : 0;
          const falls=waterFalls(tile.column,tile.row,activeWorld);
          if(Object.keys(falls).length){ctx.save();ctx.globalAlpha*=(100-(blockOpacity.get(`${index}:${level}`)||0))/100;drawWaterfalls(tile,level,falls,motionTime);ctx.restore();}
        }
        const ramp=ramps.get(index),rampLevel=stack ? stack.length-1 : 0;
        if(ramp && tileVisible({...tile,y:tile.y-(rampLevel+1)*BLOCK_HEIGHT},BLOCK_HEIGHT)) {
          ctx.save();ctx.globalAlpha*=(100-(blockOpacity.get(`${index}:${rampLevel}`)||0))/100;
          drawRamp({...tile,variant:(tile.column+tile.row+rampLevel+1)%2,y:tile.y-rampLevel*BLOCK_HEIGHT+((ramp.material ?? tileData[index])===3?WATER_SURFACE_DROP:0)},viewFacing(ramp.direction),ramp.material ?? tileData[index],palette,motionTime);ctx.restore();
        }
      }
    } else {
    // One rounded, uninterrupted soil slab sits inside the wider grass cap.
    drawIslandSoilBody(soilLeftX, centerX, soilRightX,
      middleY, soilFrontY, grassBandDepth, palette);
    // Match the base grass surface under its edges.
    const grassTileJoin = blendColor(palette.grassTileSoftFern, palette.grassTileFernSage, .5);
    const leftBandLight = blendColor(grassTileJoin, '#000000', 0.08);
    const leftBandShade = blendColor(grassTileJoin, '#000000', 0.22);
    const rightBandLight = blendColor(grassTileJoin, '#000000', 0.16);
    const rightBandShade = blendColor(grassTileJoin, '#000000', 0.30);
    drawSmoothIslandPolygon([
      [leftX, middleY], [centerX, bottomY],
      [centerX, bottomY + grassBandDepth], [leftX, middleY + grassBandDepth]
    ], leftBandLight, leftBandShade);
    drawSmoothIslandPolygon([
      [centerX, bottomY], [rightX, middleY],
      [rightX, middleY + grassBandDepth], [centerX, bottomY + grassBandDepth]
    ], rightBandLight, rightBandShade);

    // The narrow underside uses soil colors, not a green ledge.
    const grassLedgeLight = baseSoilColor(palette);
    const grassLedgeDark = baseSoilColor(palette);
    drawSmoothIslandPolygon([
      [leftX, middleY + grassBandDepth], [centerX, bottomY + grassBandDepth],
      [centerX, soilFrontY + grassBandDepth], [soilLeftX, middleY + grassBandDepth]
    ], grassLedgeLight, grassLedgeDark);
    drawSmoothIslandPolygon([
      [centerX, bottomY + grassBandDepth], [rightX, middleY + grassBandDepth],
      [soilRightX, middleY + grassBandDepth], [centerX, soilFrontY + grassBandDepth]
    ], grassLedgeLight, grassLedgeDark);

    // A soft mid-green underlay closes hairline gaps without drawing dark grid seams.
    const tileSeam = blendColor(grassTileJoin, palette.grass, 0.10);
    drawSmoothIslandPolygon([
      [centerX, topY], [rightX, middleY], [centerX, bottomY], [leftX, middleY]
    ], tileSeam, tileSeam);
    ISLAND_TILES.forEach(tile => { if (tileVisible(tile, ISLAND_DEPTH)) drawGrassTile(tile, palette, sunlight); });
    }
    const shadowLight = sunAltitude <= MOON_VISIBLE_BELOW_SUN && moon
      ? moon : {altitude: sunAltitude, hourAngleDegrees: sunHourAngle};
    drawCloudShadowsOnIsland(cloudStates, shadowLight.altitude, shadowLight.hourAngleDegrees,
      cloudCover, code, motionTime);
    // No surface clip here any more: the overlay carries its own, and clipping to the plate as well
    // would slice the top off a lattice that has been lifted clear of it.
    // Draw the upper frame last so it remains on top where the projections overlap.
    if (LANGIT_AWAN_ENABLED) {
      drawLangitAwan();
    }
    if (LANGIT_MATAHARI_ENABLED) {
      drawLangitMatahari();
    }
    ctx.restore();
  }

  // The farmer stands on one named tile, so his feet sit on that tile's centre and his body rises
  // over the tiles behind it. He is drawn after the island surface but before the clouds, so a
  // cloud crossing the plate passes in front of him exactly as it does over the grass.
  function rebuildParticles(sceneWeather) {
    const code = sceneWeather.weatherCode;
    const drizzle = weatherCodeIsDrizzle(code);
    const solidPrecipitation = weatherCodeIsSolidPrecipitation(code);
    const raining = !solidPrecipitation && (drizzle || weatherCodeIsRain(code)
      || weatherCodeIsStorm(code) || sceneWeather.rain > 0.05
      || sceneWeather.precipitation > 0.1);
    const intensity = Math.max(sceneWeather.rain, sceneWeather.precipitation);
    const storm = weatherCodeIsStorm(code);
    const rainCount = !raining ? 0
      : (storm ? 145 : drizzle ? 48 : 76) + Math.min(55, Math.round(intensity * 12));
    const random = Math.random;
    particles = [];
    for (let index = 0; index < rainCount; index += 1) {
      const speed = drizzle ? 14 + random() * 27
        : storm ? 55 + random() * 48 : 34 + random() * 42;
      particles.push({
        kind: 'rain',
        x: random() * WIDTH,
        y: random() * HEIGHT,
        speed,
        length: drizzle ? 0.5 + Math.floor(random() * 3) * 0.5
          : 2.5 + Math.floor(random() * (storm ? 10 : 6)) * 0.5,
        width: 0.5 + Math.floor(random() * 2) * 0.5,
        windJitter: (random() - 0.5) * (drizzle ? 0.85 : 0.45),
        swayAmplitude: 0.15 + random() * (drizzle ? 1.5 : 0.8),
        swayFrequency: 0.5 + random() * 1.5,
        phase: random() * Math.PI * 2,
        color: drizzle ? '#c5d9db' : '#a4bed0',
        alpha: drizzle ? 0.22 + random() * 0.36 : 0.5 + random() * 0.34
      });
    }
    particleEpoch = performance.now();
  }

  function drawPrecipitation(sceneWeather) {
    const elapsed = (performance.now() - particleEpoch) / 1000;
    const baseWindDrift = Math.sin((sceneWeather.windDirection + 180 - headingDegrees) * Math.PI / 180)
      * clamp(sceneWeather.windSpeed * 0.045, 0.2, 3.2);
    particles.forEach(particle => {
      const sway = Math.sin(elapsed * particle.swayFrequency + particle.phase) * particle.swayAmplitude;
      const x = wrap(particle.x + elapsed * (baseWindDrift + particle.windJitter) + sway, WIDTH);
      const y = wrap(particle.y + elapsed * particle.speed, HEIGHT + particle.length) - particle.length;
      const pixelX = Math.floor(x * RENDER_SCALE) / RENDER_SCALE;
      const pixelY = Math.floor(y * RENDER_SCALE) / RENDER_SCALE;
      ctx.globalAlpha = particle.alpha;
      ctx.fillStyle = particle.color;
      ctx.fillRect(pixelX, pixelY, particle.width, particle.length);
    });
    ctx.globalAlpha = 1;
  }

  function drawFog(code) {
    if (code !== 45 && code !== 48) return;
    ctx.fillStyle = 'rgba(205,211,207,.19)';
    ctx.fillRect(0, 12, WIDTH, 11);
    ctx.fillStyle = 'rgba(226,222,209,.13)';
    ctx.fillRect(0, 31, WIDTH, 12);
  }

  function drawLightning(time, code) {
    if (!weatherCodeIsStorm(code)) return;
    const phase = time % 9;
    const intensity = Math.max(0, 1 - Math.abs(phase - 4.2) / 0.22);
    if (intensity <= 0) return;
    ctx.fillStyle = `rgba(201,216,241,${0.06 * intensity})`;
    ctx.fillRect(0, 0, WIDTH, HEIGHT * 0.25);
    ctx.fillStyle = `rgba(235,230,207,${0.75 * intensity})`;
    ctx.fillRect(221, 4, 4, 8);
    ctx.fillRect(216, 12, 7, 5);
    ctx.fillRect(220, 17, 4, 10);
    ctx.fillRect(214, 26, 7, 5);
    ctx.fillRect(212, 31, 4, 12);
  }

  function applyIslandCameraSettings() {
    islandCameraElevation = clamp(Number(islandElevationInput?.value ?? DEFAULT_ISLAND_CAMERA_ELEVATION),
      MIN_ISLAND_CAMERA_ELEVATION, MAX_ISLAND_CAMERA_ELEVATION);
    editorZoom = clamp(Number(islandZoomInput?.value ?? 750) / 100, 0.5, 20);
    islandZoom = baseFitZoom() * editorZoom;
    rebuildIslandProjection();
    if (islandElevationOutput) islandElevationOutput.textContent = `${Math.round(islandCameraElevation)}°`;
    if (islandZoomOutput) islandZoomOutput.textContent = String(Math.round(editorZoom * 10) / 10);
    // The running animation loop paints the next frame; reduced-motion mode is event-driven.
    if (REDUCED_MOTION) drawFrame();
  }

  // Limit expensive canvas repaints, and keep only one animation loop even after manual redraws.
  let framePending = false, lastAnimatedFrame = -Infinity;
  function scheduleFrame() {
    if (REDUCED_MOTION || framePending) return;
    framePending = true;
    window.requestAnimationFrame(timestamp => {
      framePending = false;
      if (timestamp - lastAnimatedFrame < 1000 / (gameplayDraft || gameplayCameraMoving || window.worldManager?.getActive()?.orangeTrees?.length ? 30 : cameraMode==='gameplay' && window.worldManager?.getCharacter()?.visible && idleSitAmount<1 ? 30 : 8)) { scheduleFrame(); return; }
      lastAnimatedFrame = timestamp;
      drawFrame();
    });
  }
  function drawFrame() {
    if (cameraMode === 'gameplay') applyGameplayCamera();
    const now = Date.now();
    const skyNow = getSceneTimestamp(now);
    const sceneWeather = getSceneWeather();
    const time = (performance.now() - animationStart) / 1000;
    const motionTime = REDUCED_MOTION ? 0 : time;
    updateIdleBow(performance.now());
    orangeTreeMotionState={time:motionTime,speed:sceneWeather.windSpeed,direction:sceneWeather.windDirection,heading:headingDegrees};
    const sun = sunPosition(skyNow, location.latitude, location.longitude);
    const moonNow = moonPosition(skyNow, location.latitude, location.longitude,
      sun.eclipticLongitude);
    const cloudStates = createCloudStates(motionTime, sceneWeather,
      sun.altitude, sun.hourAngleDegrees);
    ctx.clearRect(0, 0, WIDTH, HEIGHT);
    drawSky(skyNow, sun, sceneWeather.weatherCode, sceneWeather.cloudCover);
    ctx.save();
    ctx.beginPath();
    // The sky band runs from above the frame down to the horizon; clipping at half height used
    // to behead any cloud tall enough to reach past it.
    ctx.rect(0, -HEIGHT, WIDTH, HEIGHT * 1.5);
    ctx.clip();
    // At night the Moon is the light source, even below its horizon (virtual Moon), so
    // cloud shadows remain visible as requested.
    const shadowLight = sun.altitude <= MOON_VISIBLE_BELOW_SUN ? moonNow : sun;
    // Cloud sprites are hidden; their moving island shadows remain.
    // Sun and moon sprites are temporarily hidden; lighting and stars remain active.
    ctx.restore();
    ctx.save();
    ctx.beginPath();
    ctx.rect(0, 0, WIDTH, HEIGHT * 0.5);
    ctx.clip();
    drawFog(sceneWeather.weatherCode);
    drawLightning(time, sceneWeather.weatherCode);
    ctx.restore();
    // Ground shadows go down before the island so the plate covers them, and before the clouds so
    // no sprite is ever painted over by a shadow it did not come from.
    drawFloatingIsland(sun.altitude, sun.azimuth, sun.hourAngleDegrees,
      sceneWeather.cloudCover, sceneWeather.weatherCode, moonNow, cloudStates, motionTime, sceneWeather, sun);
    drawSurfaceObjectShadows(shadowLight, sceneWeather.cloudCover, sceneWeather.weatherCode);
    drawCharacterProjectedShadow(shadowLight, sceneWeather.cloudCover);
    drawGameplayRoute();
    drawHoeTarget();
    drawSelectedBlock();
    const visibleHouse=editingHouseDraft || window.worldManager?.getHouse?.();
    const visiblePlayer=editingDraft || gameplayDraft || window.worldManager?.getCharacter?.();
    const objectWorld=worldForObjects(),joins=fenceJoins(objectWorld);
    for(const bridge of objectWorld?.bridges||[])drawBridge(bridge);
    drawStandingBlock();
    const sceneObjects=(objectWorld?.orangeTrees||[]).map(tree=>({depth:treeViewDepth(tree),draw:()=>{
      const foreground=visiblePlayer?.visible && treeViewDepth(tree)>=viewDepth(visiblePlayer.column,visiblePlayer.row);
      ctx.save();ctx.globalAlpha*=(100-(tree.opacity ?? 0))/100;const t=foreground?orangeTreeAnchor(tree):null,w=foreground?treeWidth(tree):0,h=foreground?treeHeight(tree):0;
      if(foreground && coversCharacter(visiblePlayer,p=>treeContains(tree,p),{left:t.x-w/2-3,right:t.x+w/2+3,top:t.y-h,bottom:t.y}))ctx.globalAlpha*=.5;
      drawOrangeTree(tree,motionTime,sceneWeather);drawOrangeDrop(tree);ctx.restore();
    }}));
    for(const [a,b] of grassPairs(objectWorld))sceneObjects.push({depth:(viewDepth(a.column,a.row)+viewDepth(b.column,b.row))/2,draw:()=>drawGrassBridge(a,b)});
    (objectWorld?.fences||[]).forEach((f,i)=>sceneObjects.push({depth:fenceViewDepth(f),draw:()=>drawFence(f,joins[i])}));
    if(visibleHouse?.visible)sceneObjects.push({depth:viewDepth(visibleHouse.column+1,visibleHouse.row+1)+2,draw:()=>{
      const foreground=visiblePlayer?.visible && viewDepth(visibleHouse.column+1,visibleHouse.row+1)+2>=viewDepth(visiblePlayer.column,visiblePlayer.row);
      const projection=foreground && window.HouseGeometry?houseProjection(visibleHouse):null;
      const points=projection?[[0,0],[256,0],[512,0],[0,490],[256,490],[512,490]].map(([x,y])=>window.HouseGeometry.project(projection.geometry,x,y)):[];
      const xs=points.map(p=>projection.centre.x+p.x*(projection.mirror?-1:1)),ys=points.map(p=>projection.bottom+p.y);
      const bounds=projection?{left:Math.min(...xs),right:Math.max(...xs),top:Math.min(...ys),bottom:Math.max(...ys)}:null;
      ctx.save();ctx.globalAlpha*=(100-(visibleHouse.opacity ?? 0))/100;
      if(projection && coversCharacter(visiblePlayer,p=>houseAt(p,visibleHouse),bounds))ctx.globalAlpha*=.5;drawHouse();ctx.restore();
    }});
    foregroundSparrow=null;
    if(visiblePlayer?.visible)sceneObjects.push({depth:viewDepth(visiblePlayer.column,visiblePlayer.row),draw:()=>drawMainCharacter(motionTime)});
    sceneObjects.sort((a,b)=>a.depth-b.depth);for(const object of sceneObjects)object.draw();
    // Keep the flying companion visible as a player marker even behind foreground vegetation.
    foregroundSparrow?.();
    // Clouds sit in front of the island. They are not clipped to the sky band: a foreground
    // cloud can hang below the horizon line, and clipping it there hid the cloud while its
    // shadow still showed on the grass. Edge clamping already keeps every sprite in frame.
    // Foreground cloud sprites are hidden as well; decorative stars remain visible at night.
    drawStars(sun.altitude,sceneWeather.cloudCover);
    drawPrecipitation(sceneWeather);
    displayCtx.drawImage(renderCanvas, 0, 0, canvas.width, canvas.height);
    scheduleFrame();
  }

  function getDeviceLocation() {
    return new Promise(resolve => {
      if (!navigator.geolocation) {
        resolve(FALLBACK);
        return;
      }
      navigator.geolocation.getCurrentPosition(
        position => resolve({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          fallback: false
        }),
        () => resolve(FALLBACK),
        { enableHighAccuracy: true, timeout: 12000, maximumAge: 15 * 60 * 1000 }
      );
    });
  }

  async function fetchWeather(target) {
    const params = new URLSearchParams({
      latitude: target.latitude.toFixed(5),
      longitude: target.longitude.toFixed(5),
      current: 'weather_code,cloud_cover,precipitation,rain,wind_speed_10m,wind_direction_10m',
      timezone: 'auto'
    });
    const response = await fetch(`https://api.open-meteo.com/v1/forecast?${params.toString()}`, {
      cache: 'no-store', signal: AbortSignal.timeout(12000)
    });
    if (!response.ok) throw new Error(`Open-Meteo HTTP ${response.status}`);
    const payload = await response.json();
    const current = payload.current;
    if (!current || ['weather_code','cloud_cover','precipitation','rain','wind_speed_10m','wind_direction_10m'].some(key => typeof current[key] !== 'number' || !Number.isFinite(current[key]))) throw new Error('Incomplete Open-Meteo current weather');
    weather = {
      weatherCode: Number(current.weather_code ?? 3),
      cloudCover: Number(current.cloud_cover ?? 45),
      precipitation: Number(current.precipitation ?? 0),
      rain: Number(current.rain ?? 0),
      windSpeed: Number(current.wind_speed_10m ?? 4),
      windDirection: Number(current.wind_direction_10m ?? 90)
    };
    location = target;
    weatherModelTime = typeof current.time === 'string' ? `${current.time.replace('T',' ')} ${typeof payload.timezone === 'string' ? payload.timezone : ''}`.trim() : '';
    lastFetch = Date.now();
    weatherError = false;
    updateWeatherModeControls();
    window.dispatchEvent(new Event('hudweatherchange'));
    if (PREVIEW_MODES[previewModeIndex].live) rebuildParticles(weather);
    if (REDUCED_MOTION) drawFrame();
  }

  async function refreshWeather(target) {
    if (weatherRefreshing) return;
    weatherRefreshing=true;
    if(weatherRefreshButton) weatherRefreshButton.disabled=true;
    updateWeatherModeControls();
    try { await fetchWeather(target); }
    catch (error) { weatherError=true; }
    finally {
      weatherRefreshing=false;
      if(weatherRefreshButton) weatherRefreshButton.disabled=false;
      updateWeatherModeControls();
    }
  }
  async function initializeWeather() {
    const gps = await getDeviceLocation();
    location = gps;
    await refreshWeather(gps);
    if (lastFetch === 0 && PREVIEW_MODES[previewModeIndex].live) rebuildParticles(weather);
    window.setInterval(() => {
      if (Date.now() - lastFetch >= REFRESH_INTERVAL) refreshWeather(location);
    }, REFRESH_INTERVAL);
  }
  weatherRefreshButton?.addEventListener('click', () => refreshWeather(location));

  if (islandElevationInput) {
    islandElevationInput.value = String(DEFAULT_ISLAND_CAMERA_ELEVATION);
    islandElevationInput.addEventListener('input', applyIslandCameraSettings);
  }
  if (islandElevationOutput) {
    islandElevationOutput.textContent = `${DEFAULT_ISLAND_CAMERA_ELEVATION}°`;
  }
  if (islandZoomInput) islandZoomInput.value = '750';
  if (islandZoomOutput) islandZoomOutput.textContent = '7.5';
  if (islandZoomInput) islandZoomInput.addEventListener('input', applyIslandCameraSettings);
  if (islandCameraReset) {
    islandCameraReset.addEventListener('click', () => {
      if (islandElevationInput) islandElevationInput.value = String(DEFAULT_ISLAND_CAMERA_ELEVATION);
      if (islandZoomInput) islandZoomInput.value = '750';
      applyIslandCameraSettings();
    });
  }

  // Clock uses the real device time and active location, never the weather demo time.
  window.previewHudLocation = () => ({latitude: location.latitude, longitude: location.longitude});
  window.previewHudSunAltitude = time => sunPosition(time, location.latitude, location.longitude).altitude;
  window.previewHudWeatherCode = () => {
    if (!lastFetch) return null;
    const code=weather.weatherCode;
    return !weatherCodeIsSolidPrecipitation(code) && !weatherCodeIsStorm(code) && (weather.rain>0.05 || weather.precipitation>0.1) ? 61 : code;
  };
  window.addEventListener('gameplaytoolchange',()=>{if(REDUCED_MOTION)drawFrame();});
  window.addEventListener('orangefruitupdate',()=>{if(REDUCED_MOTION)drawFrame();});
  window.addEventListener('bushharvestchange',()=>{if(REDUCED_MOTION)drawFrame();});
  window.addEventListener('worldchange', () => {
    const nextWorldId = window.worldManager?.getActive()?.id, previousWorldId = cameraWorldId;
    if (nextWorldId !== cameraWorldId) {
      cameraWorldId = nextWorldId;
      gameplayFollowCentre = null; gameplayFollowTime = null; gameplayCameraMoving = false;
    }
    const nextSize = nextWorldId === 'base' ? 50 : (window.worldManager?.getActive()?.gridSize || 9);
    if (nextSize !== ISLAND_GRID_SIZE) {
      ISLAND_GRID_SIZE = nextSize;
      gameplayFollowCentre = null; gameplayFollowTime = null; gameplayCameraMoving = false;
      editorZoom = 7.5; cameraPanX = 0; cameraPanY = 0;
      islandZoom = baseFitZoom() * editorZoom;
      rebuildIslandProjection();
    }
    if (nextWorldId !== previousWorldId) {
      savedEditorCamera = defaultEditorCamera();
      if (cameraMode === 'editor') {
        ({zoom: editorZoom, panX: cameraPanX, panY: cameraPanY} = savedEditorCamera);
        islandZoom = baseFitZoom() * editorZoom;
      }
    }
    if (cameraMode === 'gameplay') applyGameplayCamera();
    if (REDUCED_MOTION) drawFrame();
  });
  updateWeatherModeControls();
  if (weatherModeButton) {
    weatherModeButton.addEventListener('click', () => {
      previewModeIndex = (previewModeIndex + 1) % PREVIEW_MODES.length;
      rebuildParticles(getSceneWeather());
      updateWeatherModeControls();
      drawFrame();
    });
  }

  if ('DeviceOrientationEvent' in window) {
    window.addEventListener('deviceorientation', event => {
      const compassHeading = Number.isFinite(event.webkitCompassHeading)
        ? event.webkitCompassHeading
        : event.absolute && Number.isFinite(event.alpha) ? event.alpha : null;
      if (compassHeading !== null) {
        // The rotated diamond frame turns the camera 45° to face northwest, so the device
        // heading is shifted to keep sky bearings aligned with the compass.
        headingDegrees = normalizeDegrees(compassHeading - COMPASS_FRAME_OFFSET_DEGREES);
        if (REDUCED_MOTION) drawFrame();
      }
    }, true);
  }

  // Gameplay uses 7.5 times the editor's default fit scale, independent of its camera controls.
  function applyGameplayCamera() {
    editorZoom = 1;
    islandZoom = baseFitZoom() * 7.5;
    const character = gameplayDraft || window.worldManager?.getCharacter();
    const target = character?.visible ? characterCentre(character) : {x: WIDTH / 2, y: islandCenterY};
    // Ease toward the closest point of the dead zone, independently of frame rate.
    const now=performance.now(), dt=gameplayFollowTime===null?0:clamp((now-gameplayFollowTime)/1000,0,.1);
    gameplayFollowTime=now;
    if (!gameplayFollowCentre || !character?.visible || Math.hypot(target.x-gameplayFollowCentre.x,target.y-gameplayFollowCentre.y)>ISLAND_TILE_WIDTH*6) {
      gameplayFollowCentre={...target};gameplayCameraMoving=false;
    } else {
      const marginX=28/islandZoom, marginY=16/islandZoom;
      const desiredX=clamp(gameplayFollowCentre.x,target.x-marginX,target.x+marginX);
      const desiredY=clamp(gameplayFollowCentre.y,target.y-marginY,target.y+marginY);
      const factor=REDUCED_MOTION?1:1-Math.exp(-dt/.16);
      gameplayFollowCentre.x+=(desiredX-gameplayFollowCentre.x)*factor;
      gameplayFollowCentre.y+=(desiredY-gameplayFollowCentre.y)*factor;
      gameplayCameraMoving=Math.hypot(desiredX-gameplayFollowCentre.x,desiredY-gameplayFollowCentre.y)>.01;
    }
    const [anchorX, anchorY] = islandAnchor();
    cameraPanX = WIDTH / 2 - anchorX - (gameplayFollowCentre.x - anchorX) * islandZoom;
    cameraPanY = HEIGHT * 0.56 - anchorY - (gameplayFollowCentre.y - anchorY) * islandZoom;
  }
  function setPreviewCameraMode(mode) {
    idleBowController.reset(); idleBowAmount=0; idleSitAmount=0; idleBowSprite=null;
    if (mode !== 'gameplay' && mode !== 'editor') return;
    if (cameraMode === mode) return;
    if (mode === 'gameplay') {
      savedEditorCamera = {zoom: editorZoom, panX: cameraPanX, panY: cameraPanY};
      cameraMode = mode;
      gameplayFollowCentre = null; gameplayFollowTime = null; gameplayCameraMoving = false;
      applyGameplayCamera();
    } else {
      cameraMode = mode;
      ({zoom: editorZoom, panX: cameraPanX, panY: cameraPanY} = savedEditorCamera);
      islandZoom = baseFitZoom() * editorZoom;
    }
    if (REDUCED_MOTION) drawFrame();
  }
  applyGameplayCamera();

  const houseHitCache = new WeakMap();
  function houseAt(point, house) {
    if (!house?.visible) return false;
    const back=viewFacing(house.facing)==='N'||viewFacing(house.facing)==='W', mirror=viewFacing(house.facing)==='N'||viewFacing(house.facing)==='E';
    const image=back?houseBackImage:houseImage;
    if(!image.complete || !image.naturalWidth || !image.naturalHeight) return false;
    if(!window.HouseGeometry)return false;
    const {centre,geometry,bottom}=houseProjection(house);
    const p=window.HouseGeometry.unproject(geometry,(point.x-centre.x)*(mirror?-1:1),point.y-bottom);
    let u=p.x/512,v=p.y/490;
    if(u<0||u>=1||v<0||v>=1) return false;
    try {
      let data=houseHitCache.get(image);
      if(!data){const c=document.createElement('canvas');c.width=128;c.height=128;const h=c.getContext('2d',{willReadFrequently:true});h.drawImage(image,0,0,128,128);data=h.getImageData(0,0,128,128).data;houseHitCache.set(image,data);}
      return data[(Math.min(127,Math.floor(v*128))*128+Math.min(127,Math.floor(u*128)))*4+3]>32;
    } catch (_) { return false; }
  }

  // Geometry bridge for the preview editor. Coordinates are transformed through the actual
  // canvas bounds and island zoom, so dragging remains correct on different device sizes.
  window.sceneEditing = Object.freeze({
    camera() { return {zoom: editorZoom, renderZoom: islandZoom, panX: cameraPanX, panY: cameraPanY,quarterTurns:viewQuarterTurns}; },
    turnIsland(delta) {
      if(!Number.isInteger(delta) || !window.WorldViewRotation)return false;
      viewQuarterTurns=((viewQuarterTurns+delta)%4+4)%4;rebuildIslandProjection();
      gameplayFollowCentre=null;gameplayFollowTime=null;gameplayCameraMoving=false;
      if(cameraMode==='gameplay')applyGameplayCamera();drawFrame();
      const status=document.getElementById('island-turn-status');if(status)status.textContent=`Rotasi pulau ${viewQuarterTurns*90} derajat.`;
      return true;
    },
    // Capture the same pose and facing that drawMainCharacter currently uses.
    characterCatalogIcon() {
      const character=editingDraft || gameplayDraft || window.worldManager?.getCharacter();
      if(!character?.visible)return null;
      const sprite=idleBowSprite || activeCharacterSprite(character);
      const w=sprite?.naturalWidth || sprite?.width, h=sprite?.naturalHeight || sprite?.height;
      if(!w || !h || (sprite instanceof HTMLImageElement && !sprite.complete))return null;
      const icon=document.createElement('canvas');icon.width=w;icon.height=h;const c=icon.getContext('2d');
      c.imageSmoothingEnabled=false;if(viewFacing(character.facing)==='W'||viewFacing(character.facing)==='E'){c.translate(w,0);c.scale(-1,1);}c.drawImage(sprite,0,0,w,h);
      return icon.toDataURL();
    },
    setMode: setPreviewCameraMode,
    resetEditorCamera() {
      const c=defaultEditorCamera();
      editorZoom=c.zoom;cameraPanX=c.panX;cameraPanY=c.panY;
      islandZoom=baseFitZoom()*editorZoom;
      if(REDUCED_MOTION) drawFrame();
    },
    resumeIdle(elapsed) { idleBowController.resume(elapsed); },
    setGameplayRoute(route, next=1) {
      gameplayRoute = route ? {tiles:route.map(tile=>({column:tile.column,row:tile.row})),next} : null;
      if (REDUCED_MOTION) drawFrame();
    },
    setGameplayDraft(draft) {
      if(draft) { idleBowController.reset(); idleBowSprite=null; idleBowAmount=0; idleSitAmount=0; }
      gameplayDraft = draft;
      if (cameraMode === 'gameplay') applyGameplayCamera();
      if (REDUCED_MOTION) drawFrame();
    },
    setCamera(zoom, panX, panY) {
      if (![zoom, panX, panY].every(Number.isFinite)) return false;
      editorZoom = clamp(zoom, 0.5, 20);
      cameraPanX = panX; cameraPanY = panY;
      islandZoom = baseFitZoom() * editorZoom;
      if (REDUCED_MOTION) drawFrame();
      return true;
    },
    toLocal(clientX, clientY) {
      const rect = canvas.getBoundingClientRect();
      const x = (clientX - rect.left) * WIDTH / rect.width;
      const y = (clientY - rect.top) * HEIGHT / rect.height;
      const [anchorX, anchorY] = islandAnchor();
      return {x: anchorX + (x - anchorX - cameraPanX) / islandZoom,
        y: anchorY + (y - anchorY - cameraPanY) / islandZoom};
    },
    centre: characterCentre,
    fenceAt, fenceGeometry,
    bridgeAt(point) {
      const all=worldForObjects()?.bridges||[];
      for(let i=all.length-1;i>=0;i--) {
        const bridge=all[i];
        const p=bridgeBounds(bridge),mirror=(viewQuarterTurns%2===0?bridge.axis:bridge.axis==='E'?'S':'E')==='E';
        const local={x:(point.x-p.x)*(mirror?-1:1),y:point.y-p.y};
        if(window.BridgeGeometry.wideContains(local,window.BridgeGeometry.wideDeck(bridge.length||3,bridge.width||1,ISLAND_TILE_WIDTH,islandTileHeight,.1),BLOCK_HEIGHT))return i;
      }
      return null;
    },
    setSelectedBridge(b) {editingBridgeSelection=b;if(REDUCED_MOTION)drawFrame();},
    setObjectDrag(kind,index,value) {
      const world=window.worldManager?.getActive();
      if(!world || !value){objectDragWorld=null;} else {
        const key=kind==='bridge'?'bridges':kind==='fence'?'fences':'orangeTrees',items=(world[key]||[]).map((o,i)=>i===index?{...value}:o);
        objectDragWorld={...world,[key]:items};
      }
      if(REDUCED_MOTION)drawFrame();
    },
    setSelectedBlock(block) {editingBlockSelection=block;if(REDUCED_MOTION)drawFrame();},
    blockAt(point) {
      const world=window.worldManager?.getActive(),tile=this.tileAt(point);if(!tile || !world?.tiles)return null;
      const index=tile.row*ISLAND_GRID_SIZE+tile.column,stack=world.blockStacks?.[index] || (world.tiles[index]?[world.tiles[index]]:[]);if(!stack.length)return null;
      const centre=characterCentre({column:tile.column,row:tile.row,blockHeight:stack.length}),hw=ISLAND_TILE_WIDTH/2,hh=islandTileHeight/2;
      if(stack[stack.length-1]===3)centre.y+=WATER_SURFACE_DROP;
      const dx=Math.abs(point.x-centre.x)/hw,edge=centre.y+hh*(1-dx);
      if(dx>1)return null;
      if(dx+Math.abs(point.y-centre.y)/hh<=1)return {column:tile.column,row:tile.row,level:stack.length-1};
      const sideDepth=(stack.length-1)*BLOCK_HEIGHT;
      if(point.y<edge || point.y>edge+sideDepth || sideDepth<=0)return null;
      const levelsDown=Math.min(stack.length-1,Math.floor((point.y-edge)/BLOCK_HEIGHT));
      return {column:tile.column,row:tile.row,level:stack.length-1-levelsDown};
    },
    setObjectGroup(trees,fences) {editingTreeGroup=trees||[];editingFenceGroup=fences||[];if(REDUCED_MOTION)drawFrame();},
    setSelectedFence(f) {editingFenceSelection=f;if(REDUCED_MOTION)drawFrame();},
    surfaceHeight,
    waterDepth,
    waterBorders,
    waterTopBorders,
    waterFalls,
    houseAt,
    coversCharacter,
    treeContains,
    setSelectedTree(tree) {editingTreeSelection=tree ? {...tree} : null;if(REDUCED_MOTION)drawFrame();},
    orangeTreeAnchor,
    plantSway(tree) { return objectSway(tree); },
    grassPairs() {return grassPairs(window.worldManager?.getActive()).map(([a,b])=>[{column:a.column,row:a.row},{column:b.column,row:b.row}]);},
    orangeDropPosition,
    orangeDropAt(point) {
      const world=window.worldManager?.getActive(),now=Date.now();
      if(!window.OrangeDrops || window.OrangeDrops.falling(now,REDUCED_MOTION)<1)return null;
      const trees=world?.orangeTrees||[];
      for(let i=trees.length-1;i>=0;i--) {const tree=trees[i],p=orangeDropPosition(tree);
        if(window.OrangeDrops.available(world,tree,now) && Math.abs(point.x-p.x)<=4 && Math.abs(point.y-p.y)<=4)return {...tree};
      }
      return null;
    },
    orangeTreeAt(point) {
      try {
        const trees=window.worldManager?.getActive()?.orangeTrees || [];
        const order=trees.map((tree,i)=>({i,depth:treeViewDepth(tree)})).sort((a,b)=>b.depth-a.depth || b.i-a.i);
        for(const {i} of order) {
          const tree=trees[i], sprite=treeSprite(tree);
          if(!sprite.complete || !sprite.naturalWidth)continue;
          let data=houseHitCache.get(sprite);
          if(!data){const c=document.createElement('canvas');c.width=128;c.height=128;const h=c.getContext('2d',{willReadFrequently:true});h.drawImage(sprite,0,0,128,128);data=h.getImageData(0,0,128,128).data;houseHitCache.set(sprite,data);}
          const centre=orangeTreeAnchor(tree),bottom=centre.y,scale=treeVisualScale(tree);
          const width=treeWidth(tree),height=treeHeight(tree);
          const x=point.x-centre.x+(point.y-bottom)*objectSway(tree)/height;
          let u=(x+width/2)/width,v=(point.y-(bottom-height))/height;
          if(u<0||u>=1||v<0||v>=1)continue;
          if(viewFacing(tree.facing)==='W'||viewFacing(tree.facing)==='E')u=1-u;
          if(data[(Math.min(127,Math.floor(v*128))*128+Math.min(127,Math.floor(u*128)))*4+3]>32)return i;
        }
      } catch (_) { return null; }
      return null;
    },
    centreHouse(house) { return characterCentre({column: house.column + 1, row: house.row + 1}); },
    hitHouse(point, house) {
      if (!house?.visible) return false;
      return houseAt(point,house);
    },
    hit(point, character) {
      if (!character.visible) return false;
      const {x, y: surfaceY} = characterCentre(character);
      const y=surfaceY+.5*character.scale+characterWaterDepth(character)-(character.jumpHeight||0)+(idleBowSprite?(800-(viewFacing(character.facing)==='N'||viewFacing(character.facing)==='W'?778:725))/800*PLAYER_HEIGHT*character.scale*window.PlayerIdleBow.seatGroundWeight(idleSitAmount):0);
      const sprite=idleBowSprite || activeCharacterSprite(character);
      const width=PLAYER_WIDTH*character.scale*(idleBowSprite?(idleBowSprite.naturalWidth||idleBowSprite.width)/432:1),height=PLAYER_HEIGHT*character.scale;
      let u=(point.x-x)/width+.5,v=(point.y-y+height)/height;
      if(u<0||u>=1||v<0||v>=1)return false;
      if(viewFacing(character.facing)==='W'||viewFacing(character.facing)==='E')u=1-u;
      try {
        let data=houseHitCache.get(sprite);
        if(!data || !(sprite instanceof HTMLImageElement)) {
          const c=document.createElement('canvas');c.width=128;c.height=128;const h=c.getContext('2d',{willReadFrequently:true});
          h.drawImage(sprite,0,0,128,128);data=h.getImageData(0,0,128,128).data;
          if(sprite instanceof HTMLImageElement)houseHitCache.set(sprite,data);
        }
        return data[(Math.min(127,Math.floor(v*128))*128+Math.min(127,Math.floor(u*128)))*4+3]>32;
      } catch (_) { return false; }
    },
    tileAt(point) {
      const world=window.worldManager?.getActive();
      for(const bridge of world?.bridges||[])if(bridge.gap)for(const tile of window.BridgeGeometry.cells(bridge)||[]){const p=characterCentre(tile),hw=ISLAND_TILE_WIDTH/2,hh=islandTileHeight/2;if(Math.abs(point.x-p.x)/hw+Math.abs(point.y-p.y)/hh<=1)return tile;}
      if(viewQuarterTurns && world?.tiles && window.WorldViewRotation) {
        const hit=window.WorldViewRotation.pickWorld(world,viewQuarterTurns,point.x,point.y,WIDTH/2,islandTop+islandTileHeight/2,ISLAND_TILE_WIDTH,islandTileHeight,BLOCK_HEIGHT,ISLAND_DEPTH);
        if(hit)return hit;
      }
      if(!viewQuarterTurns && (world?.blockStacks || world?.ramps?.length || world?.tiles?.includes(3))) {
        const hw=ISLAND_TILE_WIDTH/2,hh=islandTileHeight/2;
        const ramps=new Map((world.ramps||[]).map(r=>[r.row*ISLAND_GRID_SIZE+r.column,r]));
        // Reverse painter order picks the visible top face nearest the viewer.
        for(let i=ISLAND_TILES.length-1;i>=0;i--) {
          const tile=ISLAND_TILES[i],index=tile.row*ISLAND_GRID_SIZE+tile.column;
          const height=world.blockStacks ? world.blockStacks[index].length : (world.tiles?.[index]?1:0);
          if(!height) continue;
          const y=tile.y-(height-1)*BLOCK_HEIGHT,ramp=ramps.get(index),topY=y+((ramp ? ramp.material : world.tiles?.[index])===3?WATER_SURFACE_DROP:0);
          if(ramp && window.RampGeometry?.hit(ramp.direction,ISLAND_TILE_WIDTH,islandTileHeight,BLOCK_HEIGHT,point.x-tile.x,point.y-topY))return {column:tile.column,row:tile.row};
          const dx=Math.abs(point.x-tile.x)/hw;
          if(dx<=1 && (dx+Math.abs(point.y-topY)/hh<=1 ||
             (height>1 && point.y>=y+hh*(1-dx) && point.y<=y+hh*(1-dx)+(height-1)*BLOCK_HEIGHT)))
            return {column:tile.column,row:tile.row};
        }
      }
      const dx = (point.x - WIDTH / 2) / (ISLAND_TILE_WIDTH / 2);
      const dy = (point.y - islandTop - islandTileHeight / 2) / (islandTileHeight / 2);
      const column = Math.round((dx + dy) / 2), row = Math.round((dy - dx) / 2);
      return column >= 0 && column < ISLAND_GRID_SIZE && row >= 0 && row < ISLAND_GRID_SIZE ? worldPoint(column,row) : null;
    },
    tile(point) {
      const visible = this.tileAt(point);
      if (visible) return visible;
      const dx = (point.x - WIDTH / 2) / (ISLAND_TILE_WIDTH / 2);
      const dy = (point.y - islandTop - islandTileHeight / 2) / (islandTileHeight / 2);
      return worldPoint(clamp(Math.round((dx + dy) / 2), 0, ISLAND_GRID_SIZE - 1),clamp(Math.round((dy - dx) / 2), 0, ISLAND_GRID_SIZE - 1));
    },
    update(draft, selected, type = 'character') {
      editingDraft = type === 'character' ? draft : null;
      editingSelected = type === 'character' && selected;
      editingHouseDraft = type === 'house' ? draft : null;
      editingHouseSelected = type === 'house' && selected;
      if (REDUCED_MOTION) drawFrame();
    }
  });
  document.getElementById('island-turn-left')?.addEventListener('click',()=>window.sceneEditing.turnIsland(-1));
  document.getElementById('island-turn-right')?.addEventListener('click',()=>window.sceneEditing.turnIsland(1));
  drawFrame();
  initializeWeather();
})();
