package it.bot.tunehunterbot;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static Properties prop = new Properties();

    static {
        try (InputStream input = new FileInputStream("src/main/resources/config.properties")) {
            prop.load(input);
            System.out.println("✅ Configurazione caricata.");
        } catch (Exception e) {
            System.err.println("❌ Errore nel caricamento di config.properties:");
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        return prop.getProperty(key);
    }
}
