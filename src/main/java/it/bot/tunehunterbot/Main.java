package it.bot.tunehunterbot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            DatabaseManager.initialize();
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TuneHunterBot());
            System.out.println("TuneHunterBot avviato!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

