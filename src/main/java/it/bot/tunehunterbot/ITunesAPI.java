package it.bot.tunehunterbot;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

public class ITunesAPI {

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Logger LOGGER = Logger.getLogger(ITunesAPI.class.getName());

    public static String searchSong(String query) {
        try {
            String url = "https://itunes.apple.com/search?term="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&entity=song&limit=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

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
            LOGGER.log(Level.SEVERE, "An error occurred while searching for a song", e);
            return null;
        }
    }

    public static String searchArtistTopSongs(String query) {
        try {
            // 1. Search for artist to get artistId
            String searchUrl = "https://itunes.apple.com/search?term="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&entity=musicArtist&limit=1";

            HttpRequest searchRequest = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .GET()
                    .build();

            HttpResponse<String> searchResponse = httpClient.send(searchRequest, HttpResponse.BodyHandlers.ofString());

            JSONObject searchObj = new JSONObject(searchResponse.body());
            JSONArray searchResults = searchObj.getJSONArray("results");

            if (searchResults.isEmpty()) {
                return "Nessun artista trovato per: " + query;
            }

            JSONObject artist = searchResults.getJSONObject(0);
            long artistId = artist.getLong("artistId");
            String artistName = artist.getString("artistName");

            // 2. Lookup top 5 songs for the artist
            String lookupUrl = "https://itunes.apple.com/lookup?id=" + artistId + "&entity=song&limit=5";

            HttpRequest lookupRequest = HttpRequest.newBuilder()
                    .uri(URI.create(lookupUrl))
                    .GET()
                    .build();

            HttpResponse<String> lookupResponse = httpClient.send(lookupRequest, HttpResponse.BodyHandlers.ofString());

            JSONObject lookupObj = new JSONObject(lookupResponse.body());
            JSONArray songResults = lookupObj.getJSONArray("results");

            if (songResults.length() <= 1) { // First result is the artist info
                return "Nessuna canzone trovata per l'artista: " + artistName;
            }

            StringBuilder result = new StringBuilder("🎤 Top 5 canzoni di " + artistName + ":\n\n");
            // Start from 1 to skip the first result which is artist info
            for (int i = 1; i < songResults.length(); i++) {
                JSONObject song = songResults.getJSONObject(i);
                String trackName = song.getString("trackName");
                result.append(i).append(". ").append(trackName).append("\n");
            }

            return result.toString();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An error occurred while searching for an artist's top songs", e);
            return "Si è verificato un errore durante la ricerca dell'artista.";
        }
    }

    public static String searchAlbum(String albumQuery, String artistQuery) {
        try {
            // 1. Search for album to get collectionId
            String searchUrl = "https://itunes.apple.com/search?term="
                    + URLEncoder.encode(albumQuery + " " + artistQuery, StandardCharsets.UTF_8)
                    + "&entity=album&limit=1";

            HttpRequest searchRequest = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .GET()
                    .build();

            HttpResponse<String> searchResponse = httpClient.send(searchRequest, HttpResponse.BodyHandlers.ofString());

            JSONObject searchObj = new JSONObject(searchResponse.body());
            JSONArray searchResults = searchObj.getJSONArray("results");

            if (searchResults.isEmpty()) {
                return "Nessun album trovato per: " + albumQuery;
            }

            JSONObject album = searchResults.getJSONObject(0);
            long collectionId = album.getLong("collectionId");
            String albumName = album.getString("collectionName");
            String artistName = album.getString("artistName");

            // 2. Lookup tracks for the album
            String lookupUrl = "https://itunes.apple.com/lookup?id=" + collectionId + "&entity=song";

            HttpRequest lookupRequest = HttpRequest.newBuilder()
                    .uri(URI.create(lookupUrl))
                    .GET()
                    .build();

            HttpResponse<String> lookupResponse = httpClient.send(lookupRequest, HttpResponse.BodyHandlers.ofString());

            JSONObject lookupObj = new JSONObject(lookupResponse.body());
            JSONArray songResults = lookupObj.getJSONArray("results");

            if (songResults.length() <= 1) { // First result is the album info
                return "Nessuna traccia trovata per l'album: " + albumName;
            }

            StringBuilder result = new StringBuilder("💿 Tracce dell'album " + albumName + " di " + artistName + ":\n\n");
            // Start from 1 to skip the first result which is album info
            for (int i = 1; i < songResults.length(); i++) {
                JSONObject song = songResults.getJSONObject(i);
                String trackName = song.getString("trackName");
                int trackNumber = song.getInt("trackNumber");
                result.append(trackNumber).append(". ").append(trackName).append("\n");
            }

            return result.toString();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An error occurred while searching for an album", e);
            return "Si è verificato un errore durante la ricerca dell'album.";
        }
    }

    public static String getTopSongs() {
        try {
            String url = "https://itunes.apple.com/us/rss/topsongs/limit=10/json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject obj = new JSONObject(response.body());
            JSONArray results = obj.getJSONObject("feed").getJSONArray("entry");

            if (results.isEmpty()) {
                return "Nessuna canzone trovata nella top 10.";
            }

            StringBuilder result = new StringBuilder("📈 Top 10 Canzoni del Momento:\n\n");
            for (int i = 0; i < results.length(); i++) {
                JSONObject song = results.getJSONObject(i);
                String title = song.getJSONObject("im:name").getString("label");
                String artist = song.getJSONObject("im:artist").getString("label");
                result.append(i + 1).append(". ").append(title).append(" - ").append(artist).append("\n");
            }

            return result.toString();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An error occurred while getting top songs", e);
            return "Si è verificato un errore during il recupero della top 10.";
        }
    }
}