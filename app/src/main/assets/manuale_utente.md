# Documentazione Applicazione GestBraccianti

## Introduzione
**GestBraccianti** è un'applicazione Android progettata per semplificare il conteggio delle ore lavorate dai propri dipendenti. Permette di tracciare le ore di lavoro dei braccianti, gestire le tariffe orarie, organizzare i lavoratori in gruppi e generare riepiloghi dettagliati.

---

## 🔴 Guida all'Installazione (APK)

Poiché l'applicazione viene distribuita direttamente (non tramite il Play Store), la procedura cambia leggermente a seconda di come hai ricevuto il file. Scegli il tuo caso:

### Caso A: Ricezione tramite WhatsApp (Consigliato)
1. Apri la chat di WhatsApp dove hai ricevuto il file `GestBraccianti.apk`.
2. Clicca sull'icona del file (o sul nome).
3. **Importante:** Se appare il messaggio *"Per tua sicurezza, il telefono non è autorizzato a installare app sconosciute da questa origine"*, clicca su **Impostazioni** e attiva la levetta su **"Consenti da questa fonte"**.
4. Torna indietro e clicca su **Installa**.

### Caso B: Ricezione tramite Email
1. Apri l'email e clicca sull'allegato `GestBraccianti.apk` per scaricarlo.
2. Una volta scaricato, clicca sulla notifica di **"Download completato"** che appare in alto sullo schermo.
3. Se non trovi la notifica, apri l'app **"Download"** o **"File"** (o "Archivio") che trovi tra le tue applicazioni e cerca il file nella cartella "Download".
4. Procedi con l'installazione autorizzando le "origini sconosciute" se richiesto (come al punto 3 del Caso A).

---

## 🔴 Avvisi di Sicurezza Comuni

Durante l'installazione potresti vedere questi messaggi:

- **"App bloccata da Play Protect":** Google segnala che l'app è sconosciuta. Clicca su **"Altre informazioni"** (o una freccetta verso il basso) e poi sul tasto **"Installa comunque"**.
- **"Vuoi installare questa applicazione?":** Conferma cliccando su **Installa**.
- **Permessi Notifiche:** Al primo avvio, clicca su **"Consenti"** quando l'app chiede di inviare notifiche.

---

## Struttura dell'Applicazione (Nell'ordine di accesso per un corretto utilizzo dell'app)

1. **Selezione Annata**
2. **Varie** (Impostazioni e Backup)
3. **Braccianti** (Anagrafica e Gruppi)
4. **Ore** (Calendario e log giornalieri)
5. **Riepilogo** (Statistiche e report PDF)
6. **Manuale in linea**
7. **Sostieni il Progetto**
8. **Limitazione di Responsabilità**
9. **Se l'app ti è piaciuta**

---

## 1. Selezione Annata
Ci si accede tramite la freccia verso dx posta in testata. Al primo avvio bisogna inserire un'annata. Successivamente viene presentata l'ultima.
*   **Funzionalità:** Aggiunta di nuove annate, selezione di uno degli anni già impostati.

---

## 2. Impostazioni e Backup (Varie)
A T T E N Z I O N E ! Trattiamo questa sezione per prima perchè richiede alcuni parametri che influenzano il comportamento dell'intera app.
Una volta impostati tali parametri la sezione diventa "inutile" al processo.

Sezione dedicata alla configurazione e alla sicurezza dei dati.

*   **Dati Titolare:** Inserimento di Nome, Cognome e Telefono del proprietario (utilizzati nell'intestazione dei report PDF).
*   **Soglia Straordinari:** Impostazione del numero di ore giornaliere oltre le quali scatta la tariffa straordinaria.
*   **Giorni Festivi Settimanali:** Configurazione dei giorni che l'app deve considerare automaticamente come festivi (Nessuno, Sabato, Domenica o entrambi).
*   **Backup CSV:**
    *   **Esporta:** Salva tutti i dati in un file CSV leggibile da Excel.
    *   **Importa:** Ripristina i dati da un file precedentemente salvato.
*   **Cronologia Backup:** L'app mantiene una lista dei backup interni creati, che possono essere condivisi o eliminati.

---

## 3. Registro Braccianti (Anagrafica)
Gestione centralizzata dei collaboratori.

### Anagrafica
*   **Scheda Lavoratore:** Nome, Cognome, Numero di telefono e gestione tariffe per l'anno selezionato:
    *   **Tariffa Base:** Applicata alle ore ordinarie.
    *   **Tariffa Straordinari:** Applicata alle ore che eccedono la soglia giornaliera (se impostata).
    *   **Tariffa Festivi:** Applicata nei giorni festivi.
*   **Copia Annata:** Al cambio anno funzione per copiare l'elenco dei braccianti dall'anno precedente per non doverli reinserire.

### Gruppi
Permette di raggruppare i braccianti (es. "Squadra Raccolta", "Squadra Potatura") per aggiungerli massivamente alle giornate di lavoro con un solo click.

---

## 4. Gestione Ore 
Questa è la schermata principale dove si registrano le presenze. Si presenta con due sezioni: Il calendario ed un elenco dei lavoratori di giornata.

### Calendario
Principalmente mostra la riga della settimana in corso. Click su "v" consente di espandere il calendario al mese. Sono evidenziate con colore diverso le gg con ore lavorate e le gg festive.
*   **Icona Calendario Dinamica:** Ogni giorno mostra il numero del giorno e, se presenti, le ore totali registrate.
*   **Navigazione:** È possibile scorrere i mesi utilizzando le frecce in alto.
*   **Festività:** Long-click su una giornata consente di attivare/disattivare una festività non prevista dall'automatismo.

### Dettaglio Giornata
Cliccando su un giorno si accede alla gestione dei lavoratori per quella data:
*   **Aggiunta rapida:** È possibile aggiungere singoli braccianti o interi gruppi predefiniti.
*   **Gestione Orari e Tariffe Differenziate:**
    *   **Selettori Rapidi:** I controlli per l'inserimento dell'orario supportano l'auto-ripetizione (pressione prolungata).
    *   **Calcolo Automatico Avanzato:** L'app calcola in tempo reale il totale delle ore e l'importo dovuto, distinguendo tra ore ordinarie, straordinarie e festive.
*   **Espandi Periodo (Inserimento Massivo):**
    *   Permette di duplicare gli orari inseriti su un intervallo di giorni consecutivi.
*   **Allineamento Tariffe:** Se la tariffa di un bracciante viene modificata nel registro, se non ri-confermati i vecchi importi non verranno modificati.

---

## 5. Riepilogo
In questa sezione è possibile visualizzare l'andamento economico e lavorativo con diversi livelli di dettaglio.

*   **Filtri Temporali:** Visualizzazione dati per Anno, Mese, Settimana o Giorno.
*   **Calcoli e Etichette Dinamiche:** Le diciture dei totali si adattano al contesto (es. "Totale Mensile", "Totale Settimanale") per una lettura immediata. Mostra le ore totali e l'importo totale dovuto (basato sulle tariffe orarie impostate).
*   **Modalità di Visualizzazione:**
    *   **Vista Braccianti (👤):** Raggruppa i dati per ogni singolo lavoratore.
    *   **Vista Gruppi (👥):** Raggruppa i dati per le squadre definite, permettendo di vedere il costo totale di un gruppo specifico.
    *   **Vista Dettagliata (📝):** Mostra ogni singola registrazione giornaliera (data, nome, ore, compenso).
    *   **Vista Totali (📊):** Mostra un riepilogo sintetico con i totali di ore e compensi per lavoratore o gruppo nel periodo scelto.
*   **Generazione Report:**
    *   **Esportazione PDF:** Genera un documento PDF con i dettagli dei pagamenti. Il report include ora il dettaglio per **Ore Ordinarie**, **Straordinari** e **Festivi**, marcando i giorni festivi con l'etichetta `[F]`.

---

## 6 - Manuale in linea
Icona "?" consente di accedere alle informazioni essenziali riguardanti la pagina. La scelta Manuale consente di accedere a questo manuale.

---

## 7 - Sostieni il Progetto
Icona della tazzina di caffè (PayPal). Transazione singola da 5€ per sostenere lo sviluppo (massimo 2 volte).

---

## 8 - 🔴 Limitazione di Responsabilità (Disclaimer)

L’applicazione ha scopo puramente gestionale. Lo sviluppatore non risponde di errate totalizzazioni. L'utente è tenuto a verificare sempre gli effettivi importi.

---

## 9 - Se l'app ti è piaciuta
Grazie per aver usato l'app! Se ti è stata utile, consigliala a parenti ed amici.
