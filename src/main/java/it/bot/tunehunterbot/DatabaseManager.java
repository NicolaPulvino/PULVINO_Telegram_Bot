package it.bot.tunehunterbot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:tunehunterbot.db";
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    public static void initialize() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                Statement stmt = conn.createStatement();

                // Tabella utenti
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS utenti (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        chat_id TEXT UNIQUE,
                        username TEXT
                    )
                """);

                // Tabella ricerche
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS ricerche (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER,
                        query TEXT,
                        result_title TEXT,
                        result_artist TEXT,
                        timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY(user_id) REFERENCES utenti(id)
                    )
                """);

                LOGGER.info("Database inizializzato correttamente.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Errore nell'inizializzazione del database:", e);
        }
    }

    public static String getDbUrl() {
        return DB_URL;
    }
}