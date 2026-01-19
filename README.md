# PULVINO_Telegram_Bot

## Descrizione del Progetto

TuneHunterBot è un bot Telegram progettato per gli amanti della musica. Utilizzando le API di iTunes e Lyrics.ovh, il bot permette agli utenti di cercare informazioni su canzoni, artisti, album, scoprire le classifiche musicali e trovare i testi delle loro canzoni preferite. Tutte le ricerche vengono salvate per fornire statistiche interessanti e una cronologia personale.

## API Utilizzate

*   **iTunes Search API**: Utilizzata per cercare canzoni, artisti, album e top chart.
    *   [Documentazione Ufficiale](https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI/index.html)
*   **Lyrics.ovh API**: Utilizzata per trovare i testi delle canzoni.
    *   [Documentazione Ufficiale](https://lyricsovh.docs.apiary.io/)

## Setup del Progetto

### Prerequisiti

*   Java JDK 21 o superiore
*   Maven 3.6.3 o superiore
*   Un token per un bot Telegram (ottenibile tramite [BotFather](https://t.me/botfather))

### Installazione e Configurazione

1.  **Clonare il repository:**
    ```
    git clone https://github.com/tuo-username/PULVINO_Telegram_Bot.git
    cd PULVINO_Telegram_Bot
    ```

2.  **Configurare le credenziali:**
    *   Naviga in `src/main/resources`.
    *   Crea una copia del file `config.properties.example` e rinominala in `config.properties`.
    *   Apri `config.properties` e inserisci il token del tuo bot e il suo username:
        ```
        BOT_TOKEN=IL_TUO_TOKEN_QUI
        BOT_USERNAME=IL_TUO_USERNAME_QUI
        ```

3.  **Compilare ed eseguire il progetto con Maven:**
    ```
    mvn clean install
    mvn exec:java -Dexec.mainClass="it.bot.tunehunterbot.Main"
    ```

### Setup del Database

Il database SQLite (`tunehunterbot.db`) viene creato e inizializzato automaticamente al primo avvio del bot. Non è richiesta alcuna configurazione manuale.

## Guida all'Utilizzo

Ecco la lista dei comandi disponibili:

*   `/start`: Mostra il messaggio di benvenuto e la lista dei comandi.
*   `/help`: Fornisce una guida dettagliata su come usare i comandi.
*   `/song <nome canzone>`: Cerca una canzone e ne mostra i dettagli (artista, album, preview).
    *   *Esempio*: `/song Bohemian Rhapsody`
*   `/artist <nome artista>`: Mostra le 5 canzoni più popolari di un artista.
    *   *Esempio*: `/artist Queen`
*   `/album <nome album> - <nome artista>`: Mostra la lista delle tracce di un album.
    *   *Esempio*: `/album A Night at the Opera - Queen`
*   `/top`: Mostra la classifica delle 10 canzoni più popolari al momento.
*   `/lyrics <nome artista> - <nome canzone>`: Cerca il testo di una canzone.
    *   *Esempio*: `/lyrics Queen - Bohemian Rhapsody`
*   `/history`: Mostra le tue ultime 10 ricerche.
*   `/stats`: Mostra statistiche generali di utilizzo del bot.

## Esempi di Conversazione

**Ricerca di una canzone:**
> **Utente:** /song Believer
> **Bot:** Cerco: Believer...
> Titolo: Believer
> Artista: Imagine Dragons
> Album: Evolve
> Preview: [link]

**Statistiche:**
> **Utente:** /stats
> **Bot:** Statistiche TuneHunterBot
>
> Ricerche totali: 150
> Utenti registrati: 25
>
> Artista più cercato: Queen (15 volte)
> Canzone più cercata: Bohemian Rhapsody (10 volte)

## Schema del Database

Il database è composto da due tabelle principali: `utenti` e `ricerche`.

*   **`utenti`**: Memorizza le informazioni base degli utenti che interagiscono con il bot.
    *   `id` (INTEGER, PRIMARY KEY, AUTOINCREMENT)
    *   `chat_id` (TEXT, UNIQUE)
    *   `username` (TEXT)

*   **`ricerche`**: Salva ogni ricerca effettuata da un utente.
    *   `id` (INTEGER, PRIMARY KEY, AUTOINCREMENT)
    *   `user_id` (INTEGER, FOREIGN KEY -> utenti.id)
    *   `query` (TEXT) - La stringa di ricerca originale.
    *   `result_title` (TEXT) - Il titolo del brano trovato.
    *   `result_artist` (TEXT) - L'artista del brano trovato.
    *   `timestamp` (DATETIME) - La data e ora della ricerca.

## Esempi di Query Implementate

Le statistiche vengono generate tramite le seguenti query SQL:

*   **Conteggio ricerche totali**:
    ```
    SELECT COUNT(*) FROM ricerche;
    ```
*   **Conteggio utenti totali**:
    ```
    SELECT COUNT(*) FROM utenti;
    ```
*   **Artista più cercato**:
    ```
    SELECT result_artist, COUNT(*) as count FROM ricerche WHERE result_artist != '' GROUP BY result_artist ORDER BY count DESC LIMIT 1;
    ```
*   **Canzone più cercata**:
    ```
    SELECT result_title, COUNT(*) as count FROM ricerche WHERE result_title != '' GROUP BY result_title ORDER BY count DESC LIMIT 1;
    ```
*   **Cronologia ricerche utente**:
    ```
    SELECT query, timestamp FROM ricerche WHERE user_id = ? ORDER BY timestamp DESC LIMIT 10;
    ```
