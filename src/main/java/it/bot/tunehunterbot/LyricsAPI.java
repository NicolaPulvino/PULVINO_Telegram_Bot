package it.bot.tunehunterbot;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

public class LyricsAPI {

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Logger LOGGER = Logger.getLogger(LyricsAPI.class.getName());

    public static String searchLyrics(String artist, String title) {
        try {
            String url = "https://api.lyrics.ovh/v1/"
                    + URLEncoder.encode(artist, StandardCharsets.UTF_8)
                    + "/"
                    + URLEncoder.encode(title, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                return "Nessun testo trovato per " + title + " - " + artist;
            }

            JSONObject obj = new JSONObject(response.body());
            String lyrics = obj.getString("lyrics");

            return "📜 Testo di " + title + " - " + artist + ":\n\n" + lyrics;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An error occurred while searching for lyrics", e);
            return "Si è verificato un errore durante la ricerca del testo.";
        }
    }
}