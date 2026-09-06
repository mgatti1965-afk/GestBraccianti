# Documentazione Applicazione GestBraccianti

## Introduzione
**GestBraccianti** è un'applicazione Android progettata per semplificare la gestione amministrativa e operativa del lavoro agricolo. Permette di tracciare le ore di lavoro dei braccianti, gestire le tariffe orarie, organizzare i lavoratori in gruppi e generare riepiloghi finanziari dettagliati.

---

## Struttura dell'Applicazione

L'app si divide in 5 aree principali, accessibili tramite la barra di navigazione inferiore:

1. **Selezione Annata** (Schermata iniziale)
2. **Ore** (Calendario e log giornalieri)
3. **Riepilogo** (Statistiche e report PDF)
4. **Braccianti** (Anagrafica e Gruppi)
5. **Varie** (Impostazioni e Backup)

---

## 1. Selezione Annata
All'avvio, l'utente sceglie l'annata agraria su cui lavorare (es. 2024). Questo permette di tenere i dati separati per ogni stagione.
*   **Funzionalità:** Aggiunta di nuove annate, selezione dell'anno corrente.

---

## 2. Gestione Ore (Daily Logging)
Questa è la schermata principale dove si registrano le presenze.

### Calendario Mensile
Mostra una griglia dei giorni del mese. I giorni con ore registrate sono evidenziati in colore.
*   **Icona Calendario Dinamica:** Ogni giorno mostra il numero del giorno e, se presenti, le ore totali registrate.
*   **Navigazione:** È possibile scorrere i mesi utilizzando le frecce in alto.

### Dettaglio Giornata
Cliccando su un giorno si accede alla gestione dei lavoratori per quella data:
*   **Aggiunta rapida:** È possibile aggiungere singoli braccianti o interi gruppi predefiniti.
*   **Gestione Orari e Tariffe Differenziate:**
    *   **Selettori Rapidi:** I controlli per l'inserimento dell'orario supportano l'auto-ripetizione (pressione prolungata).
    *   **Calcolo Automatico Avanzato:** L'app calcola in tempo reale il totale delle ore e l'importo dovuto, distinguendo tra ore ordinarie, straordinarie e festive.
    *   **Gestione Festività:** È possibile contrassegnare manualmente una giornata come festiva tramite un interruttore dedicato (toggle), che applicherà la tariffa festiva a tutte le ore della giornata.
*   **Espandi Periodo (Inserimento Massivo):**
    *   Permette di duplicare gli orari inseriti su un intervallo di giorni consecutivi.
    *   **Selettore Data Abbreviato:** Visualizzazione chiara (es. "Lun 25/12") per una selezione rapida del termine del periodo.
    *   **Sicurezza:** Un dialogo di conferma indica il numero totale di giorni interessati e previene sovrascritture accidentali.
    *   **Flusso di Lavoro:** Al salvataggio di un periodo, l'app torna automaticamente alla visualizzazione principale per aggiornare i riepiloghi.
*   **Allineamento Tariffe:** Se la tariffa di un bracciante viene modificata nel registro, l'app aggiornerà automaticamente il totale di una giornata passata non appena questa viene riconfermata (salvata nuovamente), mostrando un avviso del ricalcolo effettuato.
*   **Lettura SMS (Opzionale):** Funzione avanzata per importare orari ricevuti tramite messaggi.

---

## 3. Riepilogo Finanziario
In questa sezione è possibile visualizzare l'andamento economico e lavorativo con diversi livelli di dettaglio.

*   **Filtri Temporali:** Visualizzazione dati per Anno, Mese, Settimana o Giorno.
*   **Calcoli e Etichette Dinamiche:** Le diciture dei totali si adattano al contesto (es. "Totale Mensile", "Totale Settimanale") per una lettura immediata. Mostra le ore totali e l'importo totale dovuto (basato sulle tariffe orarie impostate).
*   **Modalità di Visualizzazione:**
    *   **Vista Braccianti (👤):** Raggruppa i dati per ogni singolo lavoratore.
    *   **Vista Gruppi (👥):** Raggruppa i dati per le squadre definite, permettendo di vedere il costo totale di un gruppo specifico.
    *   **Vista Dettagliata (📝):** Mostra ogni singola registrazione giornaliera (data, nome, ore, compenso).
    *   **Vista Totali (📊):** Mostra un riepilogo sintetico con i totali di ore e compensi per lavoratore o gruppo nel periodo scelto.
*   **Generazione Report:**
    *   **Condivisione WhatsApp/Testo:** Genera un riepilogo testuale pronto da inviare.
    *   **Esportazione PDF:** Genera un documento PDF professionale con il logo e i dettagli dei pagamenti. Il report include ora il dettaglio per **Ore Ordinarie**, **Straordinari** e **Festivi**, marcando i giorni festivi con l'etichetta `[F]`.

---

## 4. Registro Braccianti (Anagrafica)
Gestione centralizzata dei collaboratori.

### Anagrafica
*   **Scheda Lavoratore:** Nome, Cognome, Numero di telefono e gestione tariffe per l'anno selezionato:
    *   **Tariffa Base:** Applicata alle ore ordinarie.
    *   **Tariffa Straordinari:** Applicata alle ore che eccedono la soglia giornaliera (se impostata).
    *   **Tariffa Festivi:** Applicata nei giorni festivi o domenicali.
*   **Importazione Contatti:** Possibilità di importare i dati dei lavoratori direttamente dalla rubrica del telefono.
*   **Copia Annata:** Funzione per copiare l'elenco dei braccianti dall'anno precedente per non doverli reinserire.

### Gruppi
Permette di raggruppare i braccianti (es. "Squadra Raccolta", "Squadra Potatura") per aggiungerli massivamente alle giornate di lavoro con un solo click.

---

## 5. Impostazioni e Backup (Varie)
Sezione dedicata alla configurazione e alla sicurezza dei dati.

*   **Dati Titolare:** Inserimento di Nome, Cognome e Telefono del proprietario (utilizzati nell'intestazione dei report PDF).
*   **Giorni Festivi Settimanali:** Configurazione dei giorni che l'app deve considerare automaticamente come festivi (Nessuno, Sabato, Domenica o entrambi).
*   **Soglia Straordinari:** Impostazione del numero di ore giornaliere oltre le quali scatta la tariffa straordinaria.
*   **Backup CSV:**
    *   **Esporta:** Salva tutti i dati in un file CSV leggibile da Excel.
    *   **Importa:** Ripristina i dati da un file precedentemente salvato.
*   **Cronologia Backup:** L'app mantiene una lista dei backup interni creati, che possono essere condivisi o eliminati.

---

## Note Tecniche
*   **Database:** Utilizza Room Persistence Library. Versione corrente: **v7** (introdotte tariffe multiple e flag festività).
*   **Interfaccia:** Sviluppata interamente in Jetpack Compose con Material Design 3.
*   **Reportistica:** Utilizza iText/Canvas per la generazione dei documenti PDF, con supporto al riepilogo analitico delle tariffe.
*   **Portabilità:** Supporto importazione CSV da versioni precedenti (legacy) con ricalcolo automatico dei compensi.

---
*Documentazione generata automaticamente per il progetto GestBraccianti.*
