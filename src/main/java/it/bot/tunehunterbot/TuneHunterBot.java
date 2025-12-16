package it.bot.tunehunterbot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TuneHunterBot extends TelegramLongPollingBot {

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
                    sendMessage(chatId, "🎵 Ciao! Sono TuneHunterBot!\n\n" +
                            "Comandi disponibili:\n" +
                            "/song <nome> - Cerca una canzone\n" +
                            "/stats - Vedi le statistiche\n" +
                            "/help - Mostra questo messaggio");

                } else if (text.startsWith("/help")) {
                    sendMessage(chatId, "🎵 TuneHunterBot - Guida\n\n" +
                            "Usa /song seguito dal nome della canzone per cercarla.\n" +
                            "Esempio: /song Believer\n\n" +
                            "Usa /stats per vedere le statistiche delle ricerche.");

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

                } else if (text.startsWith("/stats")) {
                    String stats = getStats();
                    sendMessage(chatId, stats);

                } else {
                    sendMessage(chatId, "❓ Comando non riconosciuto.\nUsa /help per vedere i comandi disponibili.");
                }

            } catch (Exception e) {
                e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    private void registerUser(long chatId, String username) {
        try (Connection conn = DriverManager.getConnection(DatabaseManager.getDbUrl())) {
            String sql = "INSERT OR IGNORE INTO utenti (chat_id, username) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, String.valueOf(chatId));
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveSearch(long chatId, String query, String result) {
        try (Connection conn = DriverManager.getConnection(DatabaseManager.getDbUrl())) {
            // Ottieni user_id
            String getUserIdSql = "SELECT id FROM utenti WHERE chat_id = ?";
            PreparedStatement getUserStmt = conn.prepareStatement(getUserIdSql);
            getUserStmt.setString(1, String.valueOf(chatId));
            ResultSet rs = getUserStmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");

                // Estrai titolo e artista dal risultato
                String title = "";
                String artist = "";

                if (result.contains("Titolo:")) {
                    title = result.split("Titolo: ")[1].split("\n")[0];
                }
                if (result.contains("Artista:")) {
                    artist = result.split("Artista: ")[1].split("\n")[0];
                }

                // Salva la ricerca
                String sql = "INSERT INTO ricerche (user_id, query, result_title, result_artist) VALUES (?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, userId);
                pstmt.setString(2, query);
                pstmt.setString(3, title);
                pstmt.setString(4, artist);
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getStats() {
        try (Connection conn = DriverManager.getConnection(DatabaseManager.getDbUrl())) {
            StringBuilder stats = new StringBuilder("📊 Statistiche TuneHunterBot\n\n");

            // Totale ricerche
            ResultSet rs1 = conn.createStatement().executeQuery("SELECT COUNT(*) as total FROM ricerche");
            if (rs1.next()) {
                stats.append("🔍 Ricerche totali: ").append(rs1.getInt("total")).append("\n");
            }

            // Totale utenti
            ResultSet rs2 = conn.createStatement().executeQuery("SELECT COUNT(*) as total FROM utenti");
            if (rs2.next()) {
                stats.append("👥 Utenti registrati: ").append(rs2.getInt("total")).append("\n\n");
            }

            // Artista più cercato
            ResultSet rs3 = conn.createStatement().executeQuery(
                    "SELECT result_artist, COUNT(*) as count FROM ricerche " +
                            "WHERE result_artist != '' " +
                            "GROUP BY result_artist ORDER BY count DESC LIMIT 1"
            );
            if (rs3.next()) {
                stats.append("🎤 Artista più cercato: ")
                        .append(rs3.getString("result_artist"))
                        .append(" (").append(rs3.getInt("count")).append(" volte)\n");
            }

            // Canzone più cercata
            ResultSet rs4 = conn.createStatement().executeQuery(
                    "SELECT result_title, COUNT(*) as count FROM ricerche " +
                            "WHERE result_title != '' " +
                            "GROUP BY result_title ORDER BY count DESC LIMIT 1"
            );
            if (rs4.next()) {
                stats.append("🎵 Canzone più cercata: ")
                        .append(rs4.getString("result_title"))
                        .append(" (").append(rs4.getInt("count")).append(" volte)\n");
            }

            return stats.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Errore nel recupero delle statistiche.";
        }
    }
}