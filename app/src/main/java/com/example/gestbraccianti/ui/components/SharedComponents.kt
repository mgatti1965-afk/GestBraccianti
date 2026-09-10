package com.example.gestbraccianti.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gestbraccianti.R
import com.example.gestbraccianti.ui.navigation.Screen
import com.example.gestbraccianti.ui.utils.formatHours
import kotlinx.coroutines.launch

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

/**
 * Funzione per aprire la pagina di donazione PayPal.
 */
fun launchDonationIntent(context: android.content.Context, appName: String, isTestMode: Boolean = false) {
    val baseUrl = if (isTestMode) 
        "https://www.sandbox.paypal.com/cgi-bin/webscr" 
    else 
        "https://www.paypal.com/cgi-bin/webscr"
        
    val url = "$baseUrl?cmd=_xclick&business=marco.gatti65@alice.it&amount=5.00&currency_code=EUR&item_name=Offerta%20Caffe%20$appName&solution_type=Sole&landing_page=Billing"
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
    context.startActivity(intent)
}

/**
 * Finestra di dialogo per la donazione.
 */
@Composable
fun DonationDialog(
    donationCount: Int,
    appName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isBlocked = donationCount >= 2
    val hasWarning = donationCount == 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isBlocked) "Grazie di cuore! ☕" else "Offri un caffè ☕",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column {
                if (hasWarning) {
                    Text(
                        "ATTENZIONE: Hai già sostenuto il progetto in precedenza.\n",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (isBlocked)
                        "Hai già sostenuto il progetto il numero massimo di volte. Ti ringraziamo immensamente per il tuo supporto!"
                    else "Sostieni lo sviluppo di $appName con un contributo di 5€ per un buon caffè.\n\nVerrai reindirizzato su una pagina sicura gestita da PayPal dove potrai usare il tuo account o una normale carta di credito/prepagata.",
                    fontSize = 16.sp
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isBlocked) "CHIUDI" else "ANNULLA")
                }
                if (!isBlocked) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50) // GreenPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SOSTIENI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = null,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun GlobalHelpDialog(route: String?, onDismiss: () -> Unit) {
    var showFullManual by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showFullManual) {
        val manualText = remember {
            try {
                context.assets.open("manuale_utente.md").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                "Errore caricamento manuale."
            }
        }
        AlertDialog(
            onDismissRequest = { showFullManual = false },
            title = { Text(stringResource(R.string.manual_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.height(500.dp)) {
                    MarkdownText(manualText)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFullManual = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ho capito", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

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
                    HelpRow(Icons.Default.MoreHoriz, "VARIE", "Imposta i tuoi dati (Nome/Cognome) e gestisci i Backup CSV.")
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
                        HelpRow(Icons.Default.Business, "Dati Azienda", "Configura l'intestazione (Nome e Cognome) che apparirà nei tuoi documenti PDF.")
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showFullManual = true },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Manuale", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ho capito", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = null,
        shape = RoundedCornerShape(20.dp)
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

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val lines = remember(text) { text.split("\n") }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val linkColor = Color.Blue // Blu standard per massima visibilità
    val uriHandler = LocalUriHandler.current

    val anchors = remember(lines) {
        val map = mutableMapOf<String, Int>()
        lines.forEachIndexed { index, line ->
            if (line.startsWith("#")) {
                val headerText = line.trimStart('#').trim()
                // More permissive slugging to match the manual's anchors
                val slug = headerText.lowercase()
                    .replace(Regex("[^a-z0-9\\s\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF-]"), "")
                    .trim()
                    .replace(Regex("\\s+"), "-")
                
                map[slug] = index
                // Handle the manual's specific dash prefix for emoji headers
                if (line.contains("🔴")) {
                    map["-$slug"] = index
                }
            }
        }
        map
    }

    val showScrollToTop by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 2 }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            itemsIndexed(lines) { index, line ->
                MarkdownLine(
                    line = line,
                    linkColor = linkColor,
                    onAnchorClick = { anchor ->
                        anchors[anchor]?.let { targetIndex ->
                            coroutineScope.launch {
                                lazyListState.animateScrollToItem(targetIndex)
                            }
                        }
                    },
                    onUrlClick = { url ->
                        try {
                            uriHandler.openUri(url)
                        } catch (e: Exception) {
                            // Ignora errori di apertura
                        }
                    }
                )
            }
        }

        if (showScrollToTop) {
            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .pointerHoverIcon(PointerIcon.Hand),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Torna su"
                )
            }
        }
    }
}

@Composable
fun MarkdownLine(
    line: String, 
    linkColor: Color, 
    onAnchorClick: (String) -> Unit,
    onUrlClick: (String) -> Unit
) {
    val annotatedString = remember(line) { parseInlineMarkdown(line, linkColor) }
    
    val firstAnchor = remember(annotatedString) {
        annotatedString.getStringAnnotations(tag = "ANCHOR", start = 0, end = annotatedString.length)
            .firstOrNull()?.item
    }
    
    val firstUrl = remember(annotatedString) {
        annotatedString.getStringAnnotations(tag = "URL", start = 0, end = annotatedString.length)
            .firstOrNull()?.item
    }

    when {
        line.startsWith("# ") -> {
            val content = line.substring(2)
            Text(
                text = content,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (content.contains("🔴")) Color.Red else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }
        line.startsWith("## ") -> {
            val content = line.substring(3)
            Text(
                text = content,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (content.contains("🔴")) Color.Red else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
        }
        line.startsWith("### ") -> {
            val content = line.substring(4)
            Text(
                text = content,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (content.contains("🔴")) Color.Red else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )
        }
        line.startsWith("---") -> {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 2.dp)
        }
        else -> {
            if (line.isNotBlank()) {
                val modifier = when {
                    firstAnchor != null -> {
                        Modifier
                            .fillMaxWidth()
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { onAnchorClick(firstAnchor) }
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    }
                    firstUrl != null -> {
                        Modifier
                            .fillMaxWidth()
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { onUrlClick(firstUrl) }
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    }
                    else -> Modifier.padding(vertical = 2.dp)
                }

                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = modifier
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

fun parseInlineMarkdown(text: String, linkColor: Color): AnnotatedString {
    // Sostituisce il trattino del markdown con un pallino più visibile
    val displayLine = if (text.trimStart().startsWith("- ")) {
        text.replaceFirst("-", "•")
    } else text

    return buildAnnotatedString {
        var currentIndex = 0
        // Supporta **grassetto**, [testo](#ancora), [testo](url) e link diretti http/https
        val combinedRegex = Regex("(\\*\\*(.*?)\\*\\*)|\\[(.*?)\\]\\((.*?)\\)|(https?://[^\\s\\)]+)")
        
        combinedRegex.findAll(displayLine).forEach { match ->
            append(displayLine.substring(currentIndex, match.range.first))
            
            val boldText = match.groupValues[2]
            val linkText = match.groupValues[3]
            val linkTarget = match.groupValues[4]
            val rawUrl = match.groupValues[5]
            
            when {
                boldText.isNotEmpty() -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(boldText)
                    }
                }
                linkText.isNotEmpty() -> {
                    val isAnchor = linkTarget.startsWith("#")
                    val tag = if (isAnchor) "ANCHOR" else "URL"
                    val annotation = if (isAnchor) linkTarget.removePrefix("#") else linkTarget
                    
                    pushStringAnnotation(tag = tag, annotation = annotation)
                    withStyle(style = SpanStyle(
                        color = linkColor, 
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )) {
                        append(linkText)
                    }
                    pop()
                }
                rawUrl.isNotEmpty() -> {
                    pushStringAnnotation(tag = "URL", annotation = rawUrl)
                    withStyle(style = SpanStyle(
                        color = linkColor,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )) {
                        append(rawUrl)
                    }
                    pop()
                }
            }
            
            currentIndex = match.range.last + 1
        }
        
        if (currentIndex < displayLine.length) {
            append(displayLine.substring(currentIndex))
        }
    }
}
