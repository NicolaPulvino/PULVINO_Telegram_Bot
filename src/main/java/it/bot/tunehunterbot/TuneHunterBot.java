package it.bot.tunehunterbot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TuneHunterBot extends TelegramLongPollingBot {

    private static final Logger LOGGER = Logger.getLogger(TuneHunterBot.class.getName());

    @Override
    public String getBotUsername() {
        return Config.get("BOT_USERNAME");
    }

    @Override
    public String getBotToken() {
        return Config.get("BOT_TOKEN");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message msg = update.getMessage();
            String text = msg.getText();
            long chatId = msg.getChatId();
            String username = msg.getFrom().getUserName();

            try {
                // Registra l'utente se non esiste
                registerUser(chatId, username);

                if (text.startsWith("/start")) {
                    sendMessage(chatId, """
                            🎵 Ciao! Sono TuneHunterBot!

                            Comandi disponibili:
                            /song <nome> - Cerca una canzone
                            /artist <nome> - Cerca le 5 canzoni più famose di un artista
                            /album <nome album> - <nome artista> - Cerca le tracce di un album
                            /top - Mostra le 10 canzoni del momento
                            /lyrics <artista> - <canzone> - Cerca il testo di una canzone
                            /history - Mostra la tua cronologia di ricerca
                            /stats - Vedi le statistiche
                            /help - Mostra questo messaggio""");

                } else if (text.startsWith("/help")) {
                    sendMessage(chatId, """
                            🎵 TuneHunterBot - Guida

                            Usa /song seguito dal nome della canzone per cercarla.
                            Esempio: /song Believer

                            Usa /artist seguito dal nome dell'artista per le sue 5 canzoni più famose.
                            Esempio: /artist Queen

                            Usa /album seguito dal nome dell'album e dell'artista per le sue tracce.
                            Esempio: /album A Night at the Opera - Queen

                            Usa /top per vedere le 10 canzoni più famose del momento.

                            Usa /lyrics <artista> - <canzone> per cercare il testo di una canzone.
                            Esempio: /lyrics Queen - Bohemian Rhapsody

                            Usa /history per vedere la tua cronologia di ricerca.

                            Usa /stats per vedere le statistiche delle ricerche.""");

                } else if (text.startsWith("/song")) {
                    String query = text.replace("/song", "").trim();

                    if (query.isEmpty()) {
                        sendMessage(chatId, "❌ Devi scrivere il nome della canzone dopo /song\n" +
                                "Esempio: /song Imagine Dragons");
                        return;
                    }

                    sendMessage(chatId, "🔍 Cerco: " + query + "...");

                    // Chiamata API
                    String result = ITunesAPI.searchSong(query);

                    if (result != null) {
                        sendMessage(chatId, result);
                        // Salva la ricerca nel database
                        saveSearch(chatId, query, result);
                    } else {
                        sendMessage(chatId, "❌ Nessun risultato trovato per: " + query);
                    }

                } else if (text.startsWith("/artist")) {
                    String query = text.replace("/artist", "").trim();

                    if (query.isEmpty()) {
                        sendMessage(chatId, "❌ Devi scrivere il nome dell'artista dopo /artist\n" +
                                "Esempio: /artist Queen");
                        return;
                    }

                    sendMessage(chatId, "🎤 Cerco le canzoni più famose di: " + query + "...");
                    String result = ITunesAPI.searchArtistTopSongs(query);
                    sendMessage(chatId, result);

                } else if (text.startsWith("/album")) {
                    String query = text.replace("/album", "").trim();
                    String[] parts = query.split("-");

                    if (parts.length < 2) {
                        sendMessage(chatId, "❌ Formato non corretto. Usa /album <nome album> - <nome artista>");
                        return;
                    }

                    String album = parts[0].trim();
                    String artist = parts[1].trim();

                    sendMessage(chatId, "💿 Cerco le tracce di: " + album + " di " + artist + "...");
                    String result = ITunesAPI.searchAlbum(album, artist);
                    sendMessage(chatId, result);

                } else if (text.startsWith("/top")) {
                    sendMessage(chatId, "📈 Cerco le 10 canzoni più famose del momento...");
                    String result = ITunesAPI.getTopSongs();
                    sendMessage(chatId, result);

                } else if (text.startsWith("/lyrics")) {
                    String query = text.replace("/lyrics", "").trim();
                    String[] parts = query.split("-");

                    if (parts.length < 2) {
                        sendMessage(chatId, "❌ Formato non corretto. Usa /lyrics <artista> - <canzone>");
                        return;
                    }

                    String artist = parts[0].trim();
                    String title = parts[1].trim();

                    sendMessage(chatId, "📜 Cerco il testo di: " + title + " di " + artist + "...");
                    String result = LyricsAPI.searchLyrics(artist, title);
                    sendMessage(chatId, result);

                } else if (text.startsWith("/history")) {
                    String history = getHistory(chatId);
                    sendMessage(chatId, history);

                } else if (text.startsWith("/stats")) {
                    String stats = getStats();
                    sendMessage(chatId, stats);

                } else {
                    sendMessage(chatId, "❓ Comando non riconosciuto.\nUsa /help per vedere i comandi disponibili.");
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "An error occurred", e);
                sendMessage(chatId, "❌ Si è verificato un errore. Riprova.");
            }
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An error occurred while sending a message", e);
        }
    }

    private void registerUser(long chatId, String username) {
        String sql = "INSERT OR IGNORE INTO utenti (chat_id, username) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DatabaseManager.getDbUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, String.valueOf(chatId));
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "An error occurred while registering a user", e);
        }
    }

    private void saveSearch(long chatId, String query, String result) {
        String getUserIdSql = "SELECT id FROM utenti WHERE chat_id = ?";
        try (Connection conn = DriverManager.getConnection(DatabaseManager.getDbUrl());
             PreparedStatement getUserStmt = conn.prepareStatement(getUserIdSql)) {
            getUserStmt.setString(1, String.valueOf(chatId));
            try (ResultSet rs = getUserStmt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("id");

                    String title = "";
                    String artist = "";

                    if (result.contains("Titolo:")) {
                        title = result.split("Titolo: ")[1].split("\n")[0];
                    }
                    if (result.contains("Artista:")) {
                        artist = result.split("Artista: ")[1].split("\n")[0];
                    }

                    String insertSql = "INSERT INTO ricerche (user_id, query, result_title, result_artist) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                        pstmt.setInt(1, userId);
                        pstmt.setString(2, query);
                        pstmt.setString(3, title);
                        pstmt.setString(4, artist);
                        pstmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "An error occurred while saving a search", e);
        }
    }

    private String getStats() {
        StringBuilder stats = new StringBuilder("📊 Statistiche TuneHunterBot\n\n");
        try (Connection conn = DriverManager.getConnection(DatabaseManager.getDbUrl());
             Statement stmt = conn.createStatement()) {

            // Totale ricerche
            try (ResultSet rs1 = stmt.executeQuery("SELECT COUNT(*) as total FROM ricerche")) {
                if (rs1.next()) {
                    stats.append("🔍 Ricerche totali: ").append(rs1.getInt("total")).append("\n");
                }
            }

            // Totale utenti
            try (ResultSet rs2 = stmt.executeQuery("SELECT COUNT(*) as total FROM utenti")) {
                if (rs2.next()) {
                    stats.append("👥 Utenti registrati: ").append(rs2.getInt("total")).append("\n\n");
                }
            }

            // Artista più cercato
            try (ResultSet rs3 = stmt.executeQuery(
                    "SELECT result_artist, COUNT(*) as count FROM ricerche " +
                            "WHERE result_artist != '' " +
                            "GROUP BY result_artist ORDER BY count DESC LIMIT 1"
            )) {
                if (rs3.next()) {
                    stats.append("🎤 Artista più cercato: ")
                            .append(rs3.getString("result_artist"))
                            .append(" (").append(rs3.getInt("count")).append(" volte)\n");
                }
            }

            // Canzone più cercata
            try (ResultSet rs4 = stmt.executeQuery(
                    "SELECT result_title, COUNT(*) as count FROM ricerche " +
                            "WHERE result_title != '' " +
                            "GROUP BY result_title ORDER BY count DESC LIMIT 1"
            )) {
                if (rs4.next()) {
                    stats.append("🎵 Canzone più cercata: ")
                            .append(rs4.getString("result_title"))
                            .append(" (").append(rs4.getInt("count")).append(" volte)\n");
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "An error occurred while getting stats", e);
            return "Errore nel recupero delle statistiche.";
        }
        return stats.toString();
    }

    private String getHistory(long chatId) {
        String getUserIdSql = "SELECT id FROM utenti WHERE chat_id = ?";
        try (Connection conn = DriverManager.getConnection(DatabaseManager.getDbUrl());
             PreparedStatement getUserStmt = conn.prepareStatement(getUserIdSql)) {
            getUserStmt.setString(1, String.valueOf(chatId));
            try (ResultSet rs = getUserStmt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    String sql = "SELECT query, timestamp FROM ricerche WHERE user_id = ? ORDER BY timestamp DESC LIMIT 10";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setInt(1, userId);
                        try (ResultSet historyRs = pstmt.executeQuery()) {
                            StringBuilder history = new StringBuilder("📖 La tua cronologia di ricerca (ultime 10):\n\n");
                            int count = 1;
                            while (historyRs.next()) {
                                history.append(count++).append(". ").append(historyRs.getString("query")).append("\n");
                            }

                            if (count == 1) {
                                return "Nessuna cronologia di ricerca trovata.";
                            }
                            return history.toString();
                        }
                    }
                } else {
                    return "Nessun utente trovato.";
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "An error occurred while getting history", e);
            return "Errore nel recupero della cronologia.";
        }
    }
}