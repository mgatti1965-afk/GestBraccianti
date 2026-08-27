package com.example.gestbraccianti.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gestbraccianti.ui.navigation.Screen
import com.example.gestbraccianti.ui.utils.formatHours

@Composable
fun SmallStatChip(label: String, hours: Double, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = "$label: ${formatHours(hours)}",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GlobalHelpDialog(route: String?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Manuale Rapido")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isModificaOrari = route == "modifica_orari"

                if (!isModificaOrari) {
                    Text("MANUALE RAPIDO", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    HelpRow(Icons.AutoMirrored.Filled.Logout, "Annata", "Usa la freccia a destra nella barra in alto per tornare alla selezione dell'anno o crearne uno nuovo.")
                    HelpRow(Icons.Default.History, "ORE", "Registra presenze e orari nel calendario.")
                    HelpRow(Icons.Default.Calculate, "RIEPILOGO", "Controlla i totali e genera PDF/WhatsApp.")
                    HelpRow(Icons.Default.Group, "BRACCIANTI", "Gestisci l'anagrafica, le tariffe e i gruppi.")
                    HelpRow(Icons.Default.MoreHoriz, "VARIE", "Imposta i tuoi dati e gestisci i Backup CSV.")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }

                when {
                    route == Screen.Home.route || route == Screen.DailyLogging.route -> {
                        Text("1. ORE", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.CalendarToday, "Calendario", "I giorni colorati indicano ore già registrate. Tocca un giorno per inserire o modificare i dati.")
                        HelpRow(Icons.Default.SyncAlt, "Navigazione", "Usa le frecce in alto per scorrere i mesi dell'annata selezionata.")
                        HelpRow(Icons.Default.List, "Elenco", "Sotto il calendario vedi il riepilogo rapido delle giornate lavorate nel mese. Tocca per vedere il dettaglio della giornata.")
                    }
                    route?.startsWith("work_day_detail") == true -> {
                        Text("2. DETTAGLIO GIORNATA", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.PersonAdd, "Bracciante / Gruppi", "Usa i tasti in basso per aggiungere lavoratori/gruppi alla giornata.")
                        HelpRow(Icons.Default.Edit, "Modifica", "Tocca un bracciante nell'elenco per variare i suoi orari.")
                        HelpRow(Icons.Default.Delete, "Elimina", "Usa l'icona del cestino per rimuovere un lavoratore inserito per errore.")
                    }
                    route == Screen.FinancialSummary.route -> {
                        Text("3. RIEPILOGO", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.FilterList, "Filtri", "Filtra per periodo, singolo bracciante o gruppo per isolare i dati che ti servono.")
                        HelpRow(Icons.Default.Group, "Bracc./Gruppi", "Visualizza i costi e le ore per ogni lavoratore o gruppo.")
                        HelpRow(Icons.Default.ListAlt, "Vista (📝/📊)", "Passa dal dettaglio ai totali per lavoratore/gruppo.")
                        HelpRow(Icons.Default.Share, "Esporta", "Genera il PDF professionale o invia il riepilogo testuale su WhatsApp.")
                    }
                    route == Screen.WorkerRegistry.route -> {
                        Text("4. BRACCIANTI", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.Badge, "Anagrafica", "Gestisci nomi e paga oraria.")
                        HelpRow(Icons.Default.Groups, "Gruppi", "Crea gruppi di lavoratori (es. 'Squadra A') per aggiungere gli orari di tutti i componenti nelle giornate di lavoro.")
                        HelpRow(Icons.Default.Search, "Ricerca", "Usa la lente d'ingrandimento per trovare velocemente un bracciante nell'elenco.")
                    }
                    route == Screen.Others.route -> {
                        Text("5. VARIE", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.Business, "Dati Azienda", "Configura l'intestazione (Nome, P.IVA, etc.) che apparirà nei tuoi documenti PDF.")
                        HelpRow(Icons.Default.Backup, "Backup", "Esporta i dati in formato CSV per sicurezza o per aprirli in Excel/Google Sheets.")
                    }
                    isModificaOrari -> {
                        Text("6. MODIFICA ORARI", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.Add, "Tasti +/-", "Modifica l'orario a scatti di 15 minuti. Tieni premuto per uno scorrimento veloce.")
                        HelpRow(Icons.Default.Edit, "Manuale", "Tocca l'orario (testo) per inserire le ore precise con la tastiera.")
                        HelpRow(Icons.Default.DateRange, "Espandi", "Check che consente di accedere ad un selettore dove è possibile impostare una data futura. Alla conferma gli orari verranno replicati fino alla data scelta.")
                    }
                    else -> {
                        Text("Benvenuto", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        Text("Usa il menù in basso per navigare tra le sezioni. In ogni schermata troverai questa guida specifica.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Ho capito") }
        }
    )
}

@Composable
fun HelpRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
    }
}
