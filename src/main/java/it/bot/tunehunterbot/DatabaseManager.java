package it.bot.tunehunterbot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:tunehunterbot.db";

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

                System.out.println("✅ Database inizializzato correttamente.");
            }
        } catch (Exception e) {
            System.err.println("❌ Errore nell'inizializzazione del database:");
            e.printStackTrace();
        }
    }

    public static String getDbUrl() {
        return DB_URL;
    }
}