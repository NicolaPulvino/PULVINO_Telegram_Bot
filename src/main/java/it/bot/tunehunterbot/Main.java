package it.bot.tunehunterbot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            DatabaseManager.initialize();
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TuneHunterBot());
            System.out.println("TuneHunterBot avviato!");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An error occurred", e);
        }
    }
}
