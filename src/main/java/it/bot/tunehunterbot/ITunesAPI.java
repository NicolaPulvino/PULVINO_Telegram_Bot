package it.bot.tunehunterbot;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

public class ITunesAPI {

    public static String searchSong(String query) {
        try {
            String url = "https://itunes.apple.com/search?term="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&entity=song&limit=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject obj = new JSONObject(response.body());
            JSONArray results = obj.getJSONArray("results");

            if (results.isEmpty()) {
                return null;
            }

            JSONObject song = results.getJSONObject(0);
            String title = song.getString("trackName");
            String artist = song.getString("artistName");
            String album = song.getString("collectionName");
            String preview = song.optString("previewUrl", "Nessun preview disponibile");

            return String.format(
                    "🎵 Titolo: %s\n👤 Artista: %s\n💿 Album: %s\n▶️ Preview: %s",
                    title, artist, album, preview
            );

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}