(() => {
  const canvas = document.getElementById('sky-canvas');
  const ctx = canvas?.getContext('2d', { alpha: false });
  const weatherModeButton = document.getElementById('weather-cycle-button');
  const weatherModeLabel = document.getElementById('weather-mode-label');
  const islandElevationInput = document.getElementById('island-camera-elevation');
  const islandElevationOutput = document.getElementById('island-camera-elevation-value');
  const islandZoomInput = document.getElementById('island-camera-zoom');
  const islandZoomOutput = document.getElementById('island-camera-zoom-value');
  const islandCameraReset = document.getElementById('island-camera-reset');
  if (!canvas || !ctx) return;

  const WIDTH = 320;
  const HEIGHT = 200;
  // The 2x backing store keeps half-scene-unit pixel clusters crisp.
  const RENDER_SCALE = 2;
  const RENDER_CELL = 1 / RENDER_SCALE;
  canvas.width = WIDTH * RENDER_SCALE;
  canvas.height = HEIGHT * RENDER_SCALE;
  ctx.imageSmoothingEnabled = false;
  ctx.scale(RENDER_SCALE, RENDER_SCALE);
  const ISLAND_GRID_SIZE = 9;
  const ISLAND_TILE_WIDTH = 28;
  const ISLAND_COLUMN_LABELS = 'ABCDEFGHI';
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
  let islandZoom = 1;
  // Interior tile faces stay shallow; exposed perimeter faces extend through the grass band.
  const ISLAND_TILE_SIDE_DEPTH = 2.5;
  // Explicit side-wall bands: 8 units of grass above 16 units of exposed soil.
  const ISLAND_GRASS_BAND_DEPTH = 8;
  const ISLAND_SOIL_BAND_DEPTH = 16;
  // The green cap overhangs the narrower soil body along the front perimeter.
  const ISLAND_GRASS_OVERHANG = 4;
  const ISLAND_DEPTH = ISLAND_GRASS_BAND_DEPTH + ISLAND_SOIL_BAND_DEPTH;
  const ISLAND_BASE_BOTTOM_MARGIN = 8;
  const ISLAND_VERTICAL_LIFT = 16;
  const ISLAND_BOTTOM_MARGIN = ISLAND_BASE_BOTTOM_MARGIN + ISLAND_VERTICAL_LIFT;
  let islandTop = HEIGHT - ISLAND_BOTTOM_MARGIN - ISLAND_DEPTH
    - ISLAND_GRID_SIZE * islandTileHeight;
  let islandCenterY = islandTop + ISLAND_GRID_SIZE * islandTileHeight / 2;
  // Cloud bases meet the island's top plane at the upper-half visual horizon.
  const CLOUD_VISUAL_HORIZON_Y = HEIGHT * 0.5;
  // A square-root curve gives low-altitude clouds more visible vertical separation.
  const CLOUD_VISUAL_ALTITUDE_EXPONENT = 0.50;
  // Keep celestial centers in the upper third with enough margin for the Sun's glow.
  const CELESTIAL_BAND_TOP = HEIGHT * 0.10;
  const CELESTIAL_BAND_HEIGHT = HEIGHT / 3 - CELESTIAL_BAND_TOP;
  // Keep the Sun's roughly 18-unit halo inside the canvas along the upper edge.
  const SUN_GLOW_SAFE_INSET = 20;
  // Temporarily hide sky objects while retaining their renderer for later design iterations.
  // The previous sky objects (pixel sun, moon and star field) were removed. Weather still
  // drives the sky gradient, fog and lightning, and the sun's position still drives the
  // island lighting; only the drawn celestial bodies are gone while the replacement is designed.
  // A stylized 9 x 9 tile surface represents a 2.25 km square ground patch.
  const ISLAND_TILE_METERS = 250;
  const CLOUD_BASE_MIN_METERS = 800;
  const CLOUD_BASE_RANGE_METERS = 9201;
  const CLOUD_MIN_BASE_ALTITUDE_GAP_METERS = 200;
  const CLOUD_MIN_SCREEN_ALTITUDE_GAP = 2.55;
  const CLOUD_ALTITUDE_SAMPLE_ATTEMPTS = 256;
  const CLOUD_WIDTH_MIN_METERS = 1200;
  const CLOUD_WIDTH_RANGE_METERS = 4401;
  const CLOUD_FIELD_MIN_RADIUS_METERS = 500;
  const CLOUD_FIELD_RADIUS_RANGE_METERS = 44500;
  const CLOUD_FIELD_HALF_LENGTH_METERS = 60000;
  const CLOUD_RADIUS_RANDOM_SALT = 34;
  const CLOUD_ALTITUDE_RANDOM_SALT = 2;
  const CLOUD_WIDTH_RANDOM_SALT = 3;
  const CLOUD_DEPTH_RANDOM_SALT = 4;
  const CLOUD_BEARING_RANDOM_SALT = 1;
  const CLOUD_SHAPE_RANDOM_SALT = 0;
  const CLOUD_ART_SIZE_RANDOM_SALT = 5;
  // Rendering advances cloud advection at 24 simulated seconds per animation second.
  const CLOUD_SIMULATION_RATE = 24;
  const CLOUD_VERTICAL_ART_EXAGGERATION = 1.5;
  const CLOUD_HORIZONTAL_ART_EXAGGERATION = 3;
  const CLOUD_MIN_SCREEN_WIDTH = 52;
  const CLOUD_MAX_SCREEN_WIDTH = 64;
  const CLOUD_LAYOUT_GAP = 5;
  const CLOUD_MIN_SCREEN_HEIGHT = 18;
  const CLOUD_MAX_SCREEN_HEIGHT = 23;
  const CLOUD_SCREEN_TOP_MARGIN = 8;
  const CLOUD_SHADOW_FOOTPRINT_SCALE = 0.52;
  const CLOUD_SHADOW_MAX_SPRITE_RATIO = 0.90;
  const ISLAND_BODY_SHADOW_SCALE = 2.0;
  const ISLAND_BODY_SHADOW_OPACITY_SCALE = 0.40;
  const ISLAND_BODY_SHADOW_RISE = 3;
  const CLOUD_SHADOW_MIN_SUN_ALTITUDE = 3;
  const CLOUD_SHADOW_CLEAR_OPACITY = 0.10;
  const CLOUD_SHADOW_COVER_OPACITY = 0.08;
  const CLOUD_SHADOW_STORM_BONUS = 0.02;
  const CLOUD_SHADOW_MAX_OPACITY = 0.20;
  const CLOUD_SHADOW_FALLBACK_MIN_OPACITY = 0.16;
  const FALLBACK = { latitude: -6.7097, longitude: 111.3476, fallback: true };
  const REFRESH_INTERVAL = 30 * 60 * 1000;
  const REDUCED_MOTION = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  // Tall cumulus silhouettes: a broad base, with one or two higher rounded lobes.
  const CLOUD_SHAPES = [
    ['000000111000000', '000001111100000', '000011111110000', '000111111111000',
      '001111111111100', '011111111111110', '111111111111111', '111111111111111',
      '111111111111111', '111111111111111'],
    ['000111000000000', '001111100000000', '011111110000000', '011111111100000',
      '111111111111000', '111111111111110', '111111111111111', '111111111111111',
      '111111111111111', '111111111111111'],
    ['000000000111000', '000000001111100', '000000011111110', '000001111111110',
      '000111111111111', '011111111111111', '111111111111111', '111111111111111',
      '111111111111111', '111111111111111'],
    ['000111000111000', '001111101111100', '011111111111110', '111111111111111',
      '111111111111111', '111111111111111', '111111111111111', '111111111111111',
      '111111111111111', '111111111111111'],
    ['000001110000000', '000011111000000', '000111111100000', '001111111110000',
      '011111111111000', '111111111111100', '111111111111111', '111111111111111',
      '111111111111111', '111111111111111']
  ];
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
        const columnLabel = ISLAND_COLUMN_LABELS[column];
        const rowLabel = row + 1;
        ISLAND_TILES.push({
          column,
          row,
          columnLabel,
          rowLabel,
          coordinate: `${columnLabel}${rowLabel}`,
          variant: ((columnLabel.charCodeAt(0) - 65) + rowLabel - 1) % 2,
          x: WIDTH / 2 + (column - row) * ISLAND_TILE_WIDTH / 2,
          y: islandTop + islandTileHeight / 2 + (column + row) * islandTileHeight / 2
        });
      }
    }
  }
  rebuildIslandProjection();
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
      label: 'Cerah · Siang', solarHour: 12,
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
      label: 'Senja · Malam', solarHour: 18.7,
      weather: { weatherCode: 0, cloudCover: 10, precipitation: 0, rain: 0, windSpeed: 2, windDirection: 90 }
    }
  ];
  let previewModeIndex = 0;
  let headingDegrees = 0;
  let particles = [];
  let particleEpoch = performance.now();
  let lastFetch = 0;
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
    return utcDayStart + (mode.solarHour - location.longitude / 15) * 60 * 60 * 1000;
  }

  function updateWeatherModeControls() {
    const mode = PREVIEW_MODES[previewModeIndex];
    if (weatherModeLabel) {
      const source = location.fallback ? 'Rembang, Jawa Tengah (fallback)' : 'lokasi perangkat';
      const observation = lastFetch > 0
        ? ` · awan ${Math.round(weather.cloudCover)}%`
        : ' · data cuaca belum diterima';
      weatherModeLabel.textContent = mode.live
        ? `Suasana: ${mode.label} · ${source}${observation}` : `Demo: ${mode.label}`;
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
    return { azimuth: normalizeDegrees(azimuth), altitude, hourAngleDegrees };
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
  function mapCelestialX(hourAngleDegrees) {
    // Negative hour angle is before solar noon (east), positive is after it (west), so the
    // midpoint of a half circle lands the body dead centre at solar noon.
    const progress = clamp(0.5 + hourAngleDegrees / 180, 0, 1);
    return SUN_RISE_X + (SUN_SET_X - SUN_RISE_X) * progress;
  }

  function mapAltitudeToCelestialBand(altitude) {
    return CELESTIAL_BAND_TOP
      + (1 - clamp(altitude, 0, 90) / 90) * CELESTIAL_BAND_HEIGHT;
  }

  function mapCloudAltitude(altitude) {
    const verticalMix = Math.pow(clamp(altitude, 0, 90) / 90,
      CLOUD_VISUAL_ALTITUDE_EXPONENT);
    return (1 - verticalMix) * CLOUD_VISUAL_HORIZON_Y;
  }

  // The sun is a plain pixel disc shaded with a soft two-colour vertical gradient: the warm
  // light tone at the top fading into the deeper tone at the bottom. No rays, no facets.
  const SUN_BODY_RADIUS = 10.5;
  // East and west ends of the celestial arc, inset so the disc never clips the frame.
  // East sits on the right of the screen and west on the left, so the sun rises on the right
  // and sets on the left, which is the reading most people expect from a sky panel.
  const SUN_RISE_X = 284;
  const SUN_SET_X = 36;
  const GLOW_STEPS = 14;
  const GLOW_REACH = 15.0;
  const GLOW_ALPHA = 0.040;
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
    strength) {
    if (visibility <= 0 || strength <= 0) return;
    const high = clamp(clamp(altitude, 0, 90) / 45, 0, 1);
    const squash = 0.52 + high * 0.48;
    const breathe = 0.5 + 0.5 * Math.sin(motionTime * GLOW_PULSE_SPEED);
    const reach = GLOW_REACH * (1.25 - high * 0.55) * (0.68 + breathe * 0.72);
    const alpha = GLOW_ALPHA * (0.55 + breathe * 0.75) * visibility * strength;
    ctx.fillStyle = tint.replace('rgb', 'rgba').replace(')', `, ${alpha})`);
    for (let step = 0; step < GLOW_STEPS; step += 1) {
      const progress = step / (GLOW_STEPS - 1);
      glowDisc(centerX, centerY, SUN_BODY_RADIUS + 1.0 + progress * reach, squash);
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

  function drawMoon(moon, motionTime) {
    if (moon.altitude <= -1) return;
    const palette = moonPalette();
    const centerX = Math.round(mapCelestialX(moon.hourAngleDegrees) / RENDER_CELL) * RENDER_CELL;
    const centerY = Math.round(mapAltitudeToCelestialBand(moon.altitude) / RENDER_CELL)
      * RENDER_CELL;
    const visibility = clamp((moon.altitude + 1) / 3, 0, 1);
    const previousAlpha = ctx.globalAlpha;
    ctx.globalAlpha = previousAlpha * visibility;
    drawCelestialGlow(centerX, centerY, moon.altitude, motionTime,
      blendColor(palette.top, '#cfe0ff', 0.45), visibility,
      0.35 + clamp(moon.illumination, 0, 1) * 0.55);
    drawMoonBody(centerX, centerY, palette, moon);
    ctx.globalAlpha = previousAlpha;
  }

  // The terminator is projected as an ellipse rather than an offset circle, which is the
  // difference between a correct gibbous moon and a permanent crescent. At half illumination
  // the terminator is a straight line through the centre; as the moon fills out it bows toward
  // the dark limb, and as it thins it bows back toward the lit limb.
  function drawMoonBody(centerX, centerY, palette, moon) {
    const radius = SUN_BODY_RADIUS;
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
  function drawSun(sun, cloudCover, motionTime) {
    if (sun.altitude <= -1) return;
    const palette = sunPalette(sun.altitude, cloudCover);
    const centerX = Math.round(mapCelestialX(sun.hourAngleDegrees) / RENDER_CELL) * RENDER_CELL;
    const centerY = Math.round(mapAltitudeToCelestialBand(sun.altitude) / RENDER_CELL)
      * RENDER_CELL;
    const previousAlpha = ctx.globalAlpha;
    const visibility = clamp((sun.altitude + 1) / 3, 0, 1);
    ctx.globalAlpha = previousAlpha * visibility;
    drawCelestialGlow(centerX, centerY, sun.altitude, motionTime,
      blendColor(palette.top, '#ffd98a', 0.55), visibility, 1);
    drawSunBody(centerX, centerY, palette);
    ctx.globalAlpha = previousAlpha;
  }

  function drawSky(now, sun, code, cloudCover) {
    let palette;
    if (sun.altitude < -12) palette = NIGHT;
    else if (sun.altitude < 2) palette = sun.azimuth > 180 ? SUNSET : DAWN;
    else palette = DAY;

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
    const palette = sun.altitude < -12 ? NIGHT : sun.altitude > 2 ? DAY : DAWN;
    const base = colorFromPalette(palette, clamp(y / HEIGHT, 0, 1));
    return blendColor(base, '#586576', clamp(cloudCover / 100 * 0.58, 0, 0.58));
  }

  function appendCloudRows(x, y, unitX, unitY, shape,
    firstRow, lastRow, offsetY = 0) {
    for (let rowIndex = firstRow; rowIndex < lastRow; rowIndex += 1) {
      const row = shape[rowIndex];
      let column = 0;
      while (column < row.length) {
        while (column < row.length && row[column] !== '1') column += 1;
        const firstColumn = column;
        while (column < row.length && row[column] === '1') column += 1;
        if (column === firstColumn) continue;
        const left = Math.round((x + firstColumn * unitX) * RENDER_SCALE) / RENDER_SCALE;
        const right = Math.round((x + column * unitX) * RENDER_SCALE) / RENDER_SCALE;
        const top = Math.round((y + rowIndex * unitY + offsetY) * RENDER_SCALE) / RENDER_SCALE;
        const bottom = Math.round((y + (rowIndex + 1) * unitY + offsetY) * RENDER_SCALE) / RENDER_SCALE;
        ctx.rect(left, top, right - left, bottom - top);
      }
    }
  }

  function drawCloud(x, y, unitX, unitY, shape, base, shade, light) {
    const middleRow = Math.floor(shape.length / 2);
    const bottomRow = shape.length - 1;

    // Offset lower blocks form a compact shadow bevel beneath the broad base.
    ctx.beginPath();
    appendCloudRows(x, y, unitX, unitY, shape, middleRow, shape.length, unitY * 0.34);
    ctx.fillStyle = shade;
    ctx.fill();

    // Joined row-runs retain the rounded cumulus silhouette as chunky pixel steps.
    ctx.beginPath();
    appendCloudRows(x, y, unitX, unitY, shape, 0, shape.length);
    ctx.fillStyle = base;
    ctx.fill();

    // A narrow lower facet separates the cloud's face from its underside.
    ctx.beginPath();
    for (let rowIndex = Math.max(middleRow, bottomRow - 1);
      rowIndex <= bottomRow; rowIndex += 1) {
      const row = shape[rowIndex];
      let column = 0;
      while (column < row.length) {
        while (column < row.length && row[column] !== '1') column += 1;
        const firstColumn = column;
        while (column < row.length && row[column] === '1') column += 1;
        if (column === firstColumn) continue;
        const left = Math.round((x + firstColumn * unitX + unitX * 0.06) * RENDER_SCALE) / RENDER_SCALE;
        const right = Math.round((x + column * unitX - unitX * 0.06) * RENDER_SCALE) / RENDER_SCALE;
        const top = Math.round((y + (rowIndex + 0.66) * unitY) * RENDER_SCALE) / RENDER_SCALE;
        const bottom = Math.round((y + (rowIndex + 1) * unitY) * RENDER_SCALE) / RENDER_SCALE;
        ctx.rect(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
      }
    }
    ctx.fillStyle = blendColor(base, shade, 0.48);
    ctx.fill();

    // Small flat facets catch light along the stepped crowns and left shoulders.
    const snapCloudPoint = value => Math.round(value * RENDER_SCALE) / RENDER_SCALE;
    ctx.beginPath();
    for (let rowIndex = 0; rowIndex < middleRow; rowIndex += 1) {
      const row = shape[rowIndex];
      for (let column = 0; column < row.length; column += 1) {
        if (row[column] !== '1') continue;
        const exposedTop = rowIndex === 0 || shape[rowIndex - 1][column] !== '1';
        const exposedLeft = column === 0 || row[column - 1] !== '1';
        const left = x + column * unitX;
        const right = left + unitX;
        const top = y + rowIndex * unitY;
        const bottom = top + unitY;
        if (exposedTop) {
          ctx.moveTo(snapCloudPoint(left + unitX * 0.08), snapCloudPoint(top + unitY * 0.08));
          ctx.lineTo(snapCloudPoint(right - unitX * 0.08), snapCloudPoint(top + unitY * 0.08));
          ctx.lineTo(snapCloudPoint(left + unitX * 0.56), snapCloudPoint(top + unitY * 0.48));
          ctx.lineTo(snapCloudPoint(left + unitX * 0.08), snapCloudPoint(top + unitY * 0.48));
          ctx.closePath();
        } else if (exposedLeft) {
          ctx.moveTo(snapCloudPoint(left + unitX * 0.08), snapCloudPoint(top + unitY * 0.08));
          ctx.lineTo(snapCloudPoint(left + unitX * 0.42), snapCloudPoint(top + unitY * 0.25));
          ctx.lineTo(snapCloudPoint(left + unitX * 0.42), snapCloudPoint(bottom - unitY * 0.08));
          ctx.lineTo(snapCloudPoint(left + unitX * 0.08), snapCloudPoint(bottom - unitY * 0.08));
          ctx.closePath();
        }
      }
    }
    ctx.fillStyle = light;
    ctx.fill();
  }

  function wrapCentered(value, range) {
    return ((value + range * 0.5) % range + range) % range - range * 0.5;
  }

  function reflectCentered(value, halfRange) {
    const phase = ((value + halfRange) % (4 * halfRange) + 4 * halfRange) % (4 * halfRange);
    return phase <= 2 * halfRange ? phase - halfRange : 3 * halfRange - phase;
  }

  function cloudRandom(seed, index, salt) {
    let value = (seed ^ Math.imul(index + 1, 0x9e3779b9)
      ^ Math.imul(salt + 1, 0x85ebca6b)) | 0;
    value = Math.imul(value ^ (value >>> 16), 0x7feb352d);
    value = Math.imul(value ^ (value >>> 15), 0x846ca68b);
    value ^= value >>> 16;
    return (value >>> 0) / 4294967296;
  }

  function createCloudStates(elapsedSeconds, sceneWeather) {
    const code = sceneWeather.weatherCode;
    const count = code === 0 || code === 1 ? 0 : 1;
    if (count === 0) return [];

    const windSpeedKmh = Math.max(0, sceneWeather.windSpeed ?? 4);
    const windFromDegrees = sceneWeather.windDirection ?? 90;
    const windBearing = normalizeDegrees(windFromDegrees + 180) * Math.PI / 180;
    const alongEast = Math.sin(windBearing);
    const alongNorth = Math.cos(windBearing);
    const crossEast = -alongNorth;
    const crossNorth = alongEast;
    const simulatedSeconds = elapsedSeconds * CLOUD_SIMULATION_RATE;
    const windSpeedMetersPerSecond = windSpeedKmh / 3.6;
    const cloudSeed = Math.trunc(location.latitude * 37 + location.longitude * 17) | 0;
    const shapeIndex = Math.floor(cloudRandom(cloudSeed, 0, CLOUD_SHAPE_RANDOM_SALT)
      * CLOUD_SHAPES.length);
    const shape = CLOUD_SHAPES[shapeIndex];
    const initialBearing = cloudRandom(cloudSeed, 0, CLOUD_BEARING_RANDOM_SALT) * Math.PI * 2;
    const initialRadius = CLOUD_FIELD_MIN_RADIUS_METERS
      + cloudRandom(cloudSeed, 0, CLOUD_RADIUS_RANDOM_SALT) * CLOUD_FIELD_RADIUS_RANGE_METERS;
    const initialEast = Math.sin(initialBearing) * initialRadius;
    const initialNorth = Math.cos(initialBearing) * initialRadius;
    let along = initialEast * alongEast + initialNorth * alongNorth;
    const across = initialEast * crossEast + initialNorth * crossNorth;
    along = reflectCentered(along + windSpeedMetersPerSecond * simulatedSeconds,
      CLOUD_FIELD_HALF_LENGTH_METERS);
    const east = along * alongEast + across * crossEast;
    const north = along * alongNorth + across * crossNorth;
    const rangeMeters = Math.max(1, Math.hypot(east, north));
    const widthMeters = CLOUD_WIDTH_MIN_METERS
      + cloudRandom(cloudSeed, 0, CLOUD_WIDTH_RANDOM_SALT) * CLOUD_WIDTH_RANGE_METERS;
    const depthRatio = 0.60 + cloudRandom(cloudSeed, 0, CLOUD_DEPTH_RANDOM_SALT) * 0.35;
    const depthMeters = widthMeters * depthRatio;
    const baseAltitude = sampleCloudBaseAltitude(cloudSeed, 0, initialRadius,
      depthMeters, shape.length, [], []);
    const azimuth = normalizeDegrees(Math.atan2(east, north) * 180 / Math.PI);
    const baseElevation = Math.atan2(baseAltitude, rangeMeters) * 180 / Math.PI;
    const topElevation = Math.atan2(baseAltitude + depthMeters, rangeMeters) * 180 / Math.PI;
    const angularWidth = 2 * Math.atan2(widthMeters * 0.5, rangeMeters);
    const artSizeMix = cloudRandom(cloudSeed, 0, CLOUD_ART_SIZE_RANDOM_SALT);
    const maxSpriteWidth = Math.min(
      CLOUD_MAX_SCREEN_WIDTH * (0.82 + artSizeMix * 0.18), WIDTH / count - CLOUD_LAYOUT_GAP);
    const spriteWidth = clamp(angularWidth * 180 / Math.PI * CLOUD_HORIZONTAL_ART_EXAGGERATION
      * WIDTH / 360, CLOUD_MIN_SCREEN_WIDTH, maxSpriteWidth);
    const unitX = spriteWidth / shape[0].length;
    const baseY = mapCloudAltitude(baseElevation);
    const topY = mapCloudAltitude(topElevation);
    const spriteHeight = clamp(Math.max(shape.length * 0.7,
      (baseY - topY) * CLOUD_VERTICAL_ART_EXAGGERATION),
    CLOUD_MIN_SCREEN_HEIGHT, CLOUD_MAX_SCREEN_HEIGHT);
    const unitY = spriteHeight / shape.length;
    const spriteTopY = Math.max(CLOUD_SCREEN_TOP_MARGIN, baseY - spriteHeight);
    const cloud = {
      shape,
      eastMeters: east,
      northMeters: north,
      baseAltitudeMeters: baseAltitude,
      widthMeters,
      depthMeters,
      alongEast,
      alongNorth,
      crossEast,
      crossNorth,
      screenCenterX: mapAzimuth(azimuth),
      screenY: spriteTopY,
      unitX,
      unitY
    };
    separateOverlappingClouds([cloud]);
    return [cloud];
  }

  function sampleCloudBaseAltitude(seed, index, rangeMeters, depthMeters, shapeHeight,
    assignedAltitudes, assignedCenterY) {
    let bestCandidate = CLOUD_BASE_MIN_METERS
      + cloudRandom(seed, index, CLOUD_ALTITUDE_RANDOM_SALT + 8192) * CLOUD_BASE_RANGE_METERS;
    let bestScore = -1;
    for (let attempt = 0; attempt < CLOUD_ALTITUDE_SAMPLE_ATTEMPTS; attempt += 1) {
      const salt = CLOUD_ALTITUDE_RANDOM_SALT + attempt * 37;
      const candidate = CLOUD_BASE_MIN_METERS
        + cloudRandom(seed, index, salt) * CLOUD_BASE_RANGE_METERS;
      const centerY = cloudSpriteCenterY(candidate, rangeMeters, depthMeters, shapeHeight);
      const score = altitudeSpacingScore(candidate, centerY, assignedAltitudes, assignedCenterY);
      if (score > bestScore) {
        bestScore = score;
        bestCandidate = candidate;
      }
      if (score >= 1) return candidate;
    }
    const latticeSize = 4096;
    const start = Math.floor(cloudRandom(seed, index, CLOUD_ALTITUDE_RANDOM_SALT + 16384)
      * latticeSize);
    for (let offset = 0; offset < latticeSize; offset += 1) {
      const slot = (start + offset) % latticeSize;
      const candidate = CLOUD_BASE_MIN_METERS + (slot + 0.5) / latticeSize * CLOUD_BASE_RANGE_METERS;
      const centerY = cloudSpriteCenterY(candidate, rangeMeters, depthMeters, shapeHeight);
      const score = altitudeSpacingScore(candidate, centerY, assignedAltitudes, assignedCenterY);
      if (score > bestScore) {
        bestScore = score;
        bestCandidate = candidate;
      }
      if (score >= 1) return candidate;
    }
    return bestCandidate;
  }

  function cloudSpriteCenterY(baseAltitudeMeters, rangeMeters, depthMeters, shapeHeight) {
    const baseElevation = Math.atan2(baseAltitudeMeters, rangeMeters) * 180 / Math.PI;
    const topElevation = Math.atan2(baseAltitudeMeters + depthMeters, rangeMeters) * 180 / Math.PI;
    const baseY = mapCloudAltitude(baseElevation);
    const topY = mapCloudAltitude(topElevation);
    const spriteHeight = clamp(Math.max(shapeHeight * 0.7,
      (baseY - topY) * CLOUD_VERTICAL_ART_EXAGGERATION),
    CLOUD_MIN_SCREEN_HEIGHT, CLOUD_MAX_SCREEN_HEIGHT);
    const spriteTopY = Math.max(CLOUD_SCREEN_TOP_MARGIN, baseY - spriteHeight);
    return spriteTopY + spriteHeight * 0.5;
  }

  function altitudeSpacingScore(altitudeMeters, screenCenterY, assignedAltitudes, assignedScreenCenterY) {
    if (assignedAltitudes.length === 0) return Infinity;
    let score = Infinity;
    for (let index = 0; index < assignedAltitudes.length; index += 1) {
      const altitudeGap = Math.abs(altitudeMeters - assignedAltitudes[index]);
      const screenGap = Math.abs(screenCenterY - assignedScreenCenterY[index]);
      score = Math.min(score, altitudeGap / CLOUD_MIN_BASE_ALTITUDE_GAP_METERS,
        screenGap / CLOUD_MIN_SCREEN_ALTITUDE_GAP);
    }
    return score;
  }

  function separateOverlappingClouds(clouds) {
    for (let iteration = 0; iteration < 16; iteration += 1) {
      let moved = false;
      for (let first = 0; first < clouds.length; first += 1) {
        const a = clouds[first];
        const widthA = a.unitX * a.shape[0].length;
        const heightA = a.unitY * a.shape.length;
        const centerYA = a.screenY + heightA * 0.5;
        for (let second = first + 1; second < clouds.length; second += 1) {
          const b = clouds[second];
          const widthB = b.unitX * b.shape[0].length;
          const heightB = b.unitY * b.shape.length;
          const centerYB = b.screenY + heightB * 0.5;
          if (Math.abs(centerYA - centerYB) >= (heightA + heightB) * 0.5) continue;
          const deltaX = wrapCentered(b.screenCenterX - a.screenCenterX, WIDTH);
          const overlap = (widthA + widthB) * 0.5 + CLOUD_LAYOUT_GAP - Math.abs(deltaX);
          if (overlap <= 0) continue;
          const direction = deltaX < 0 ? -1 : 1;
          const push = overlap * 0.5;
          a.screenCenterX = wrap(a.screenCenterX - direction * push, WIDTH);
          b.screenCenterX = wrap(b.screenCenterX + direction * push, WIDTH);
          moved = true;
        }
      }
      if (!moved) break;
    }
    clouds.forEach(cloud => {
      const range = Math.hypot(cloud.eastMeters, cloud.northMeters);
      const azimuth = normalizeDegrees(cloud.screenCenterX / WIDTH * 360 + headingDegrees - 180);
      cloud.eastMeters = Math.sin(azimuth * Math.PI / 180) * range;
      cloud.northMeters = Math.cos(azimuth * Math.PI / 180) * range;
    });
  }

  function drawClouds(clouds, sunAltitude, code) {
    let base;
    let shade;
    let light;
    if (sunAltitude < -5) {
      base = '#647796'; shade = '#3d4c69'; light = '#aebeda';
    } else if (sunAltitude < 8) {
      base = '#d5b9c7'; shade = '#8e7e9d'; light = '#f9e0c5';
    } else {
      base = '#d7e3f3'; shade = '#8096b4'; light = '#fffaef';
    }
    if (weatherCodeIsStorm(code)) {
      base = '#8e9bae'; shade = '#4d5e76'; light = '#cbd4e0';
    }
    clouds.forEach(cloud => {
      const width = cloud.unitX * cloud.shape[0].length;
      const x = cloud.screenCenterX - width / 2;
      drawCloud(x, cloud.screenY, cloud.unitX, cloud.unitY, cloud.shape, base, shade, light);
      if (x < 0) {
        drawCloud(x + WIDTH, cloud.screenY, cloud.unitX, cloud.unitY,
          cloud.shape, base, shade, light);
      } else if (x + width > WIDTH) {
        drawCloud(x - WIDTH, cloud.screenY, cloud.unitX, cloud.unitY,
          cloud.shape, base, shade, light);
      }
    });
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

  function clipIslandSurface() {
    const points = islandSurfacePoints();
    ctx.beginPath();
    ctx.moveTo(points[0][0], points[0][1]);
    for (let index = 1; index < points.length; index += 1) {
      ctx.lineTo(points[index][0], points[index][1]);
    }
    ctx.closePath();
    ctx.clip();
  }

  function drawCloudsOverIsland(clouds, sunAltitude, code) {
    if (clouds.length === 0) return;
    ctx.save();
    clipIslandSurface();
    ctx.beginPath();
    ctx.rect(0, 0, WIDTH, HEIGHT * 0.5);
    ctx.clip();
    drawClouds(clouds, sunAltitude, code);
    ctx.restore();
  }

  function drawIslandCloudShadows(clouds, sunAltitude, sunAzimuth, cloudCover, code) {
    if (clouds.length === 0 || sunAltitude <= CLOUD_SHADOW_MIN_SUN_ALTITUDE) return;
    const physicalShadowMayHit = drawCloudShadowsOnSurface(
      clouds, sunAltitude, sunAzimuth, cloudCover, 0, code);
    if (!physicalShadowMayHit) {
      drawIslandCloudShadowCue(clouds, sunAltitude, sunAzimuth, cloudCover, code);
    }
  }

  function drawCloudShadowsOnSurface(clouds, sunAltitude, sunAzimuth, cloudCover,
    receiverElevationMeters, code) {
    if (sunAltitude <= CLOUD_SHADOW_MIN_SUN_ALTITUDE) return false;
    const altitudeRadians = sunAltitude * Math.PI / 180;
    const azimuthRadians = sunAzimuth * Math.PI / 180;
    const sineAltitude = Math.sin(altitudeRadians);
    const horizontalSun = Math.cos(altitudeRadians);
    const sunEast = horizontalSun * Math.sin(azimuthRadians);
    const sunNorth = horizontalSun * Math.cos(azimuthRadians);
    const directLight = clamp((sineAltitude - Math.sin(3 * Math.PI / 180))
      / (Math.sin(18 * Math.PI / 180) - Math.sin(3 * Math.PI / 180)), 0, 1);
    const coverFactor = clamp(cloudCover / 100, 0, 1);
    let opacity = (CLOUD_SHADOW_CLEAR_OPACITY + CLOUD_SHADOW_COVER_OPACITY * coverFactor)
      * directLight;
    if (weatherCodeIsStorm(code)) opacity = Math.min(CLOUD_SHADOW_MAX_OPACITY,
      opacity + CLOUD_SHADOW_STORM_BONUS);
    const shadowColor = `rgba(17,26,39,${opacity})`;
    let physicalShadowMayHit = false;
    ctx.save();
    clipIslandSurface();
    clouds.forEach(cloud => {
      const relativeCloudHeight = cloud.baseAltitudeMeters - receiverElevationMeters;
      if (relativeCloudHeight <= 0) return;
      const shiftEast = -relativeCloudHeight * sunEast / sineAltitude;
      const shiftNorth = -relativeCloudHeight * sunNorth / sineAltitude;
      const scale = cloudShadowFootprintScale(cloud);
      if (cloudShadowIntersectsIsland(cloud, shiftEast, shiftNorth, scale)) {
        physicalShadowMayHit = true;
      }
      buildCloudShadowPath(cloud, shiftEast, shiftNorth, scale);
      ctx.fillStyle = shadowColor;
      ctx.fill();
    });
    ctx.restore();
    return physicalShadowMayHit;
  }

  function drawIslandCloudShadowCue(clouds, sunAltitude, sunAzimuth, cloudCover, code) {
    const altitudeRadians = sunAltitude * Math.PI / 180;
    const azimuthRadians = sunAzimuth * Math.PI / 180;
    const sineAltitude = Math.sin(altitudeRadians);
    if (sineAltitude <= 0) return;
    const horizontalSun = Math.cos(altitudeRadians);
    const sunEast = horizontalSun * Math.sin(azimuthRadians);
    const sunNorth = horizontalSun * Math.cos(azimuthRadians);
    const directLight = clamp((sineAltitude - Math.sin(3 * Math.PI / 180))
      / (Math.sin(18 * Math.PI / 180) - Math.sin(3 * Math.PI / 180)), 0, 1);
    const coverFactor = clamp(cloudCover / 100, 0, 1);
    let opacity = (CLOUD_SHADOW_CLEAR_OPACITY + CLOUD_SHADOW_COVER_OPACITY * coverFactor)
      * directLight;
    if (weatherCodeIsStorm(code)) opacity = Math.min(CLOUD_SHADOW_MAX_OPACITY,
      opacity + CLOUD_SHADOW_STORM_BONUS);
    const cueOpacity = Math.max(CLOUD_SHADOW_FALLBACK_MIN_OPACITY * directLight, opacity * 0.72);
    let nearest = null;
    let nearestEast = 0;
    let nearestNorth = 0;
    let nearestDistance = Infinity;
    clouds.forEach(cloud => {
      const height = cloud.baseAltitudeMeters;
      if (height <= 0) return;
      const shiftEast = -height * sunEast / sineAltitude;
      const shiftNorth = -height * sunNorth / sineAltitude;
      const centerEast = cloud.eastMeters + shiftEast;
      const centerNorth = cloud.northMeters + shiftNorth;
      const distance = Math.hypot(centerEast, centerNorth);
      if (distance < nearestDistance) {
        nearestDistance = distance;
        nearest = cloud;
        nearestEast = centerEast;
        nearestNorth = centerNorth;
      }
    });
    if (!nearest) return;
    const cueRadiusMeters = ISLAND_GRID_SIZE * ISLAND_TILE_METERS * 0.20;
    const maxAxisDistance = Math.max(Math.abs(nearestEast), Math.abs(nearestNorth));
    const pull = maxAxisDistance > cueRadiusMeters ? cueRadiusMeters / maxAxisDistance : 1;
    const cueShiftEast = nearestEast * pull - nearest.eastMeters;
    const cueShiftNorth = nearestNorth * pull - nearest.northMeters;
    ctx.save();
    clipIslandSurface();
    buildCloudShadowPath(nearest, cueShiftEast, cueShiftNorth, cloudShadowFootprintScale(nearest));
    ctx.fillStyle = `rgba(17,26,39,${cueOpacity})`;
    ctx.fill();
    ctx.restore();
  }

  function cloudShadowIntersectsIsland(cloud, shadowEast, shadowNorth, footprintScale) {
    const cellAlong = cloud.widthMeters * footprintScale / cloud.shape[0].length;
    const cellAcross = cloud.depthMeters * footprintScale / cloud.shape.length;
    const halfExtent = ISLAND_GRID_SIZE * ISLAND_TILE_METERS / 2;
    for (let row = 0; row < cloud.shape.length; row += 1) {
      const pixels = cloud.shape[row];
      let column = 0;
      while (column < pixels.length) {
        while (column < pixels.length && pixels[column] !== '1') column += 1;
        const runStart = column;
        while (column < pixels.length && pixels[column] === '1') column += 1;
        if (runStart === column) continue;
        const alongStart = (runStart - pixels.length * 0.5) * cellAlong;
        const alongEnd = (column - pixels.length * 0.5) * cellAlong;
        const acrossStart = (row - cloud.shape.length * 0.5) * cellAcross;
        const acrossEnd = (row + 1 - cloud.shape.length * 0.5) * cellAcross;
        const points = [
          shadowVertex(cloud, alongStart, acrossStart, shadowEast, shadowNorth),
          shadowVertex(cloud, alongEnd, acrossStart, shadowEast, shadowNorth),
          shadowVertex(cloud, alongEnd, acrossEnd, shadowEast, shadowNorth),
          shadowVertex(cloud, alongStart, acrossEnd, shadowEast, shadowNorth)
        ];
        if (shadowQuadIntersectsIsland(points, halfExtent)) return true;
      }
    }
    return false;
  }

  function shadowVertex(cloud, along, across, shadowEast, shadowNorth) {
    return [
      cloud.eastMeters + along * cloud.alongEast + across * cloud.crossEast + shadowEast,
      cloud.northMeters + along * cloud.alongNorth + across * cloud.crossNorth + shadowNorth
    ];
  }

  function shadowQuadIntersectsIsland(points, halfExtent) {
    const east = points.map(point => point[0]);
    const north = points.map(point => point[1]);
    if (Math.min(...east) > halfExtent || Math.max(...east) < -halfExtent
      || Math.min(...north) > halfExtent || Math.max(...north) < -halfExtent) return false;
    for (let edge = 0; edge < 4; edge += 1) {
      const next = (edge + 1) % 4;
      const axisX = -(north[next] - north[edge]);
      const axisY = east[next] - east[edge];
      const projections = points.map(point => point[0] * axisX + point[1] * axisY);
      const squareRadius = halfExtent * (Math.abs(axisX) + Math.abs(axisY));
      if (Math.min(...projections) > squareRadius || Math.max(...projections) < -squareRadius) return false;
    }
    return true;
  }

  function buildCloudShadowPath(cloud, shadowEast, shadowNorth, footprintScale) {
    const cellAlong = cloud.widthMeters * footprintScale / cloud.shape[0].length;
    const cellAcross = cloud.depthMeters * footprintScale / cloud.shape.length;
    ctx.beginPath();
    for (let row = 0; row < cloud.shape.length; row += 1) {
      const pixels = cloud.shape[row];
      let column = 0;
      while (column < pixels.length) {
        while (column < pixels.length && pixels[column] !== '1') column += 1;
        const runStart = column;
        while (column < pixels.length && pixels[column] === '1') column += 1;
        if (runStart === column) continue;
        const alongStart = (runStart - pixels.length * 0.5) * cellAlong;
        const alongEnd = (column - pixels.length * 0.5) * cellAlong;
        const acrossStart = (row - cloud.shape.length * 0.5) * cellAcross;
        const acrossEnd = (row + 1 - cloud.shape.length * 0.5) * cellAcross;
        const corners = [
          shadowVertex(cloud, alongStart, acrossStart, shadowEast, shadowNorth),
          shadowVertex(cloud, alongEnd, acrossStart, shadowEast, shadowNorth),
          shadowVertex(cloud, alongEnd, acrossEnd, shadowEast, shadowNorth),
          shadowVertex(cloud, alongStart, acrossEnd, shadowEast, shadowNorth)
        ].map(([east, north]) => [
          Math.round((WIDTH / 2 + (east - north) * ISLAND_TILE_WIDTH
            / (2 * ISLAND_TILE_METERS)) * RENDER_SCALE) / RENDER_SCALE,
          Math.round((islandCenterY + (east + north) * islandTileHeight
            / (2 * ISLAND_TILE_METERS)) * RENDER_SCALE) / RENDER_SCALE
        ]);
        ctx.moveTo(corners[0][0], corners[0][1]);
        for (let index = 1; index < corners.length; index += 1) {
          ctx.lineTo(corners[index][0], corners[index][1]);
        }
        ctx.closePath();
      }
    }
  }

  function cloudShadowFootprintScale(cloud) {
    const modelWidth = cloud.widthMeters;
    const modelDepth = cloud.depthMeters;
    const projectedWidth = (modelWidth * Math.abs(cloud.alongEast - cloud.alongNorth)
      + modelDepth * Math.abs(cloud.crossEast - cloud.crossNorth))
      * ISLAND_TILE_WIDTH / (2 * ISLAND_TILE_METERS);
    const projectedHeight = (modelWidth * Math.abs(cloud.alongEast + cloud.alongNorth)
      + modelDepth * Math.abs(cloud.crossEast + cloud.crossNorth))
      * islandTileHeight / (2 * ISLAND_TILE_METERS);
    const spriteWidth = cloud.unitX * cloud.shape[0].length;
    const spriteHeight = cloud.unitY * cloud.shape.length;
    const widthLimit = spriteWidth / Math.max(1e-6, projectedWidth);
    const heightLimit = spriteHeight / Math.max(1e-6, projectedHeight);
    return Math.min(CLOUD_SHADOW_FOOTPRINT_SCALE,
      CLOUD_SHADOW_MAX_SPRITE_RATIO * Math.min(widthLimit, heightLimit));
  }

  function drawIslandShadow(centerX, lowerY, sunAltitude, cloudCover, code) {
    const directLight = clamp((sunAltitude + 8) / 28, 0, 1);
    let cloudFilter = 1 - clamp(cloudCover / 100 * 0.45, 0, 0.45);
    if (weatherCodeIsStorm(code)) cloudFilter *= 0.82;
    const opacity = (0.08 + directLight * 0.25) * cloudFilter * ISLAND_BODY_SHADOW_OPACITY_SCALE;
    const shadowCenterY = lowerY - ISLAND_DEPTH - ISLAND_BODY_SHADOW_RISE;
    const halfWidth = ISLAND_GRID_SIZE * ISLAND_TILE_WIDTH / 4 * ISLAND_BODY_SHADOW_SCALE;
    const halfHeight = ISLAND_GRID_SIZE * islandTileHeight / 4 * ISLAND_BODY_SHADOW_SCALE;
    drawIslandPolygon([
      [centerX, shadowCenterY - halfHeight], [centerX + halfWidth, shadowCenterY],
      [centerX, shadowCenterY + halfHeight], [centerX - halfWidth, shadowCenterY]
    ], `rgba(12,23,40,${opacity})`, null);
  }

  function skyAtmosphere(sunAltitude, cloudCover, code) {
    if (sunAltitude < -8) return SKY_ATMOSPHERES.night;
    if (sunAltitude < 5) return SKY_ATMOSPHERES.twilight;
    const overcastOrWet = cloudCover >= 65 || code === 45 || code === 48
      || (code >= 51 && code <= 99);
    return overcastOrWet ? SKY_ATMOSPHERES.overcast : SKY_ATMOSPHERES.day;
  }

  function islandPalette(sunAltitude, cloudCover, code) {
    const daylight = clamp((sunAltitude + 12) / 24, 0, 1);
    const twilight = clamp(1 - Math.abs(sunAltitude + 1) / 9, 0, 1);
    let weatherShade = clamp((cloudCover - 25) / 100 * 0.34, 0, 0.26);
    if (weatherCodeIsStorm(code)) weatherShade = Math.max(weatherShade, 0.34);
    const atmosphere = skyAtmosphere(sunAltitude, cloudCover, code);
    let grass = blendColor('#385344', '#5a7365', daylight);
    grass = blendColor(grass, '#c39d60', twilight * 0.25);
    grass = blendColor(grass, '#78836f', weatherShade);
    const wet = (code >= 51 && code <= 67) || (code >= 80 && code <= 82);
    if (wet) grass = blendColor(grass, '#62786d', 0.12);
    grass = blendColor(grass, atmosphere.tint, atmosphere.islandMix);
    const grassDark = blendColor(grass, '#2d4938', 0.24);
    const grassTileMix = (0.18 + daylight * 0.77) * (1 - weatherShade * 0.30);
    const grassTileSoftFern = blendColor(grass, '#a8c795', grassTileMix);
    const grassTileFernSage = blendColor(grass, '#98ba87', grassTileMix);
    const grassSeam = blendColor(grass, '#314d37', 0.08);
    const grassWall = blendColor(grass, '#23482c', 0.28);
    let soil = blendColor('#32251c', '#9a4a0f', daylight);
    soil = blendColor(soil, '#be742d', twilight * 0.24);
    soil = blendColor(soil, '#5b4837', weatherShade * 0.45);
    soil = blendColor(soil, atmosphere.tint, atmosphere.islandMix * 0.22);
    const soilLight = blendColor(soil, '#be6a1a', 0.20);
    const soilDark = blendColor(soil, '#3a1b07', 0.50);
    return { grass, grassDark, grassTileSoftFern, grassTileFernSage,
      grassSeam, grassWall, soilLight, soilDark };
  }

  function islandSunlight(sunAltitude, cloudCover, code) {
    if (sunAltitude <= 0) return { strength: 0 };
    const altitudeRadians = clamp(sunAltitude, 0, 90) * Math.PI / 180;
    let strength = clamp(Math.sin(altitudeRadians) / Math.sin(Math.PI / 3), 0, 1)
      * (1 - clamp(cloudCover / 100, 0, 1));
    if (weatherCodeIsStorm(code)) strength *= 0.55;
    return { strength };
  }

  function drawGrassTile(tile, palette, sunlight) {
    const lightTile = tile.variant === 0;
    const fill = lightTile ? palette.grassTileSoftFern : palette.grassTileFernSage;
    const halfWidth = ISLAND_TILE_WIDTH / 2 - 0.05;
    const halfHeight = islandTileHeight / 2 - 0.025;
    const cornerRadius = 1.4;
    const leftPerimeterFace = tile.row === ISLAND_GRID_SIZE - 1;
    const rightPerimeterFace = tile.column === ISLAND_GRID_SIZE - 1;
    const leftSideDepth = leftPerimeterFace ? ISLAND_GRASS_BAND_DEPTH : ISLAND_TILE_SIDE_DEPTH;
    const rightSideDepth = rightPerimeterFace ? ISLAND_GRASS_BAND_DEPTH : ISLAND_TILE_SIDE_DEPTH;
    drawGrassTileSide(tile.x, tile.y,
      leftPerimeterFace ? ISLAND_TILE_WIDTH / 2 : halfWidth,
      leftPerimeterFace ? islandTileHeight / 2 : halfHeight,
      leftPerimeterFace ? 0 : cornerRadius, leftSideDepth, fill, true);
    drawGrassTileSide(tile.x, tile.y,
      rightPerimeterFace ? ISLAND_TILE_WIDTH / 2 : halfWidth,
      rightPerimeterFace ? islandTileHeight / 2 : halfHeight,
      rightPerimeterFace ? 0 : cornerRadius, rightSideDepth, fill, false);

    // Each top stays solid; direct sunlight adds only a uniform, subtle color lift.
    drawSunlitIslandDiamond(tile.x, tile.y, halfWidth, halfHeight,
      cornerRadius, fill, sunlight);
  }

  function drawGrassTileSide(centerX, centerY, halfWidth, halfHeight, cornerRadius,
    depth, fill, leftSide) {
    const cutX = Math.min(cornerRadius, halfWidth * 0.34);
    const cutY = cutX * halfHeight / halfWidth;
    const splitY = centerY + halfHeight - cutY * 0.5;
    const sideShade = depth > ISLAND_TILE_SIDE_DEPTH
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
      blendColor(fill, '#000000', sideShade), blendColor(fill, '#000000', sideShade));
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
      blendColor(fill, '#000000', sideShade), blendColor(fill, '#000000', sideShade));
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
      minX, minY, maxX, maxY, palette.soilLight, palette.soilDark);
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
      ? blendColor(baseColor, '#b6d0a1', 0.10 * sunlight.strength)
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

  function drawPixelPolygon(points, color) {
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
    ctx.fillStyle = color;
    ctx.fill();
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

  function drawIslandPolygon(points, fill, outline) {
    drawPixelPolygon(points, fill);
    if (outline) drawPixelPolyline(points, outline, true, 0.5);
  }

  function drawFloatingIsland(sunAltitude, sunAzimuth, cloudCover, code, clouds) {
    ctx.save();
    const islandAnchorX = WIDTH / 2;
    const islandAnchorY = HEIGHT - ISLAND_BOTTOM_MARGIN;
    ctx.translate(islandAnchorX, islandAnchorY);
    ctx.scale(islandZoom, islandZoom);
    ctx.translate(-islandAnchorX, -islandAnchorY);
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

    drawIslandShadow(centerX, lowerY, sunAltitude, cloudCover, code);
    // One rounded, uninterrupted soil slab sits inside the wider grass cap.
    drawIslandSoilBody(soilLeftX, centerX, soilRightX,
      middleY, soilFrontY, grassBandDepth, palette);
    // A side-shaded average of the two tile hues fills rounded joints without a third green.
    const grassTileJoin = blendColor(palette.grassTileSoftFern, palette.grassTileFernSage, 0.5);
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

    // A darker underside makes the overhang read as a distinct grass platform above the soil.
    const grassLedgeLight = blendColor(palette.grassWall, palette.grassDark, 0.16);
    const grassLedgeDark = blendColor(palette.grassWall, palette.grassDark, 0.32);
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
    ISLAND_TILES.forEach(tile => drawGrassTile(tile, palette, sunlight));
    drawIslandCloudShadows(clouds, sunAltitude, sunAzimuth, cloudCover, code);
    const topRim = blendColor(palette.grassSeam, palette.grassDark, 0.32);
    drawSmoothIslandLine(leftX, middleY, centerX, bottomY, topRim, 0.60);
    drawSmoothIslandLine(centerX, bottomY, rightX, middleY, topRim, 0.60);
    ctx.restore();
  }

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
    islandZoom = clamp(Number(islandZoomInput?.value ?? 100) / 100, 0.80, 1.05);
    rebuildIslandProjection();
    if (islandElevationOutput) islandElevationOutput.textContent = `${Math.round(islandCameraElevation)}°`;
    if (islandZoomOutput) islandZoomOutput.textContent = `${Math.round(islandZoom * 100)}%`;
    // The running animation loop paints the next frame; reduced-motion mode is event-driven.
    if (REDUCED_MOTION) drawFrame();
  }

  function drawFrame() {
    const now = Date.now();
    const skyNow = getSceneTimestamp(now);
    const sceneWeather = getSceneWeather();
    const time = (performance.now() - animationStart) / 1000;
    const motionTime = REDUCED_MOTION ? 0 : time;
    const sun = sunPosition(skyNow, location.latitude, location.longitude);
    const cloudStates = [];
    ctx.clearRect(0, 0, WIDTH, HEIGHT);
    drawSky(skyNow, sun, sceneWeather.weatherCode, sceneWeather.cloudCover);
    ctx.save();
    ctx.beginPath();
    ctx.rect(0, 0, WIDTH, HEIGHT * 0.5);
    ctx.clip();
    drawSun(sun, sceneWeather.cloudCover, motionTime);
    drawMoon(moonPosition(skyNow, location.latitude, location.longitude,
      sunPosition(skyNow, location.latitude, location.longitude).eclipticLongitude), motionTime);
    ctx.restore();
    ctx.save();
    ctx.beginPath();
    ctx.rect(0, 0, WIDTH, HEIGHT * 0.5);
    ctx.clip();
    drawFog(sceneWeather.weatherCode);
    drawLightning(time, sceneWeather.weatherCode);
    ctx.restore();
    drawFloatingIsland(sun.altitude, sun.azimuth,
      sceneWeather.cloudCover, sceneWeather.weatherCode, cloudStates);
    drawPrecipitation(sceneWeather);
    if (!REDUCED_MOTION) window.requestAnimationFrame(drawFrame);
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
      cache: 'no-store'
    });
    if (!response.ok) throw new Error(`Open-Meteo HTTP ${response.status}`);
    const payload = await response.json();
    const current = payload.current;
    if (!current) throw new Error('Open-Meteo response has no current weather');
    weather = {
      weatherCode: Number(current.weather_code ?? 3),
      cloudCover: Number(current.cloud_cover ?? 45),
      precipitation: Number(current.precipitation ?? 0),
      rain: Number(current.rain ?? 0),
      windSpeed: Number(current.wind_speed_10m ?? 4),
      windDirection: Number(current.wind_direction_10m ?? 90)
    };
    location = target;
    lastFetch = Date.now();
    updateWeatherModeControls();
    if (PREVIEW_MODES[previewModeIndex].live) rebuildParticles(weather);
    if (REDUCED_MOTION) drawFrame();
  }

  async function initializeWeather() {
    const gps = await getDeviceLocation();
    location = gps;
    updateWeatherModeControls();
    try {
      await fetchWeather(gps);
    } catch (error) {
      if (!gps.fallback) {
        location = FALLBACK;
        updateWeatherModeControls();
        try {
          await fetchWeather(FALLBACK);
        } catch (fallbackError) {
          // Keep the quiet time-based sky if weather data cannot be reached.
        }
      }
    }
    if (lastFetch === 0 && PREVIEW_MODES[previewModeIndex].live) rebuildParticles(weather);
    window.setInterval(async () => {
      if (Date.now() - lastFetch < REFRESH_INTERVAL) return;
      try {
        await fetchWeather(location);
      } catch (error) {
        // Continue animating from the last successful weather snapshot.
      }
    }, REFRESH_INTERVAL);
  }

  if (islandElevationInput) {
    islandElevationInput.value = String(DEFAULT_ISLAND_CAMERA_ELEVATION);
    islandElevationInput.addEventListener('input', applyIslandCameraSettings);
  }
  if (islandElevationOutput) {
    islandElevationOutput.textContent = `${DEFAULT_ISLAND_CAMERA_ELEVATION}°`;
  }
  if (islandZoomInput) islandZoomInput.value = '100';
  if (islandZoomOutput) islandZoomOutput.textContent = '100%';
  if (islandZoomInput) islandZoomInput.addEventListener('input', applyIslandCameraSettings);
  if (islandCameraReset) {
    islandCameraReset.addEventListener('click', () => {
      if (islandElevationInput) islandElevationInput.value = String(DEFAULT_ISLAND_CAMERA_ELEVATION);
      if (islandZoomInput) islandZoomInput.value = '100';
      applyIslandCameraSettings();
    });
  }

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

  drawFrame();
  initializeWeather();
})();
