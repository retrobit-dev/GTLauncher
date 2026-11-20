package dev.retrobit.gtlauncher;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Fetches only the weather fields needed to paint the launcher sky. */
public final class OpenMeteoClient {
    private static final String ENDPOINT = "https://api.open-meteo.com/v1/forecast";

    private OpenMeteoClient() { }

    public static WeatherSnapshot fetch(
            double latitude,
            double longitude,
            boolean fallbackLocation) throws IOException, JSONException {
        String urlText = String.format(
                Locale.US,
                ENDPOINT + "?latitude=%.5f&longitude=%.5f"
                        + "&current=weather_code,cloud_cover,precipitation,rain,wind_speed_10m,wind_direction_10m"
                        + "&timezone=auto",
                latitude,
                longitude);

        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(9000);
        connection.setReadTimeout(9000);
        connection.setRequestProperty("Accept", "application/json");

        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("Open-Meteo returned HTTP " + status);
            }

            String response = readFully(connection.getInputStream());
            JSONObject current = new JSONObject(response).getJSONObject("current");
            return new WeatherSnapshot(
                    current.optInt("weather_code", 3),
                    current.optDouble("cloud_cover", 45.0),
                    current.optDouble("precipitation", 0.0),
                    current.optDouble("rain", 0.0),
                    current.optDouble("wind_speed_10m", 4.0),
                    current.optDouble("wind_direction_10m", 90.0),
                    latitude,
                    longitude,
                    System.currentTimeMillis(),
                    fallbackLocation);
        } finally {
            connection.disconnect();
        }
    }

    private static String readFully(InputStream inputStream) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
}
