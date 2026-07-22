package com.ofa.vpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.ofa.vpn.data.local.AppDatabase
import com.ofa.vpn.data.local.ServerEntity
import com.ofa.vpn.data.remote.SubFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(modifier = Modifier.fillMaxSize()) { VpnAppScreen() } } }
    }
}

@Composable
fun VpnAppScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val servers by db.serverDao().getAllServers().collectAsState(initial = emptyList())
    var bulkUrls by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var errorLogs by remember { mutableStateOf(listOf<String>()) }
    var showErrors by remember { mutableStateOf(false) }

    if (showErrors) {
        AlertDialog(
            onDismissRequest = { showErrors = false },
            title = { Text("Log of Failed Links") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (errorLogs.isEmpty()) { Text("No errors!") } 
                    else { errorLogs.forEach { err -> Text("- $err\n", style = MaterialTheme.typography.bodySmall) } }
                }
            },
            confirmButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        clipboardManager.setText(AnnotatedString(errorLogs.joinToString("\n")))
                    }) {
                        Text("Copy Log")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { showErrors = false }) { Text("Close") }
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("OFA VPN - Bulk Import", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = bulkUrls, onValueChange = { bulkUrls = it }, label = { Text("Paste Sub URLs (One per line)") }, modifier = Modifier.fillMaxWidth().height(120.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { 
            if (bulkUrls.isNotBlank()) { 
                isLoading = true; statusMessage = "Fetching..."; errorLogs = emptyList()
                scope.launch { 
                    val urls = bulkUrls.split("\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
                    val allServers = mutableListOf<ServerEntity>()
                    val errors = mutableListOf<String>()
                    for (url in urls) {
                        try { allServers.addAll(withContext(Dispatchers.IO) { SubFetcher.fetchAndParse(url) }) } 
                        catch (e: Exception) { errors.add("$url -> ${e.message ?: "Unknown"}") }
                    }
                    if (allServers.isEmpty() && errors.isNotEmpty()) { statusMessage = "All links failed! Check log." }
                    else {
                        withContext(Dispatchers.IO) { db.serverDao().deleteAll() }
                        withContext(Dispatchers.IO) { db.serverDao().insertAll(allServers) }
                        statusMessage = "Added ${allServers.size} servers. (${errors.size} failed)"
                    }
                    errorLogs = errors
                    isLoading = false 
                } 
            } 
        }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) { Text(if (isLoading) "Loading..." else "Fetch All Subscriptions") }
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top=8.dp)) {
            Text(text = statusMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
            if (errorLogs.isNotEmpty()) { OutlinedButton(onClick = { showErrors = true }) { Text("View Error Log") } }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) { items(servers) { ServerCard(it) } }
    }
}

@Composable
fun ServerCard(server: ServerEntity) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) { Text(server.name); Text("${server.protocol} - ${server.address}:${server.port}", style = MaterialTheme.typography.bodySmall) }
            Button(onClick = {}) { Text("Connect") }
        }
    }
}