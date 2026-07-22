package com.ofa.vpn

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.gson.Gson
import com.ofa.vpn.core.ConfigParser
import com.ofa.vpn.core.UpdateManager
import com.ofa.vpn.data.local.AppDatabase
import com.ofa.vpn.data.local.ServerEntity
import com.ofa.vpn.data.remote.SubFetcher
import com.ofa.vpn.service.VpnConnectionService
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
    val updateManager = remember { UpdateManager(context) }
    
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var errorLogs by remember { mutableStateOf(listOf<String>()) }
    var showErrors by remember { mutableStateOf(false) }
    var selectedServer by remember { mutableStateOf<ServerEntity?>(null) }
    var updateStatus by remember { mutableStateOf("") }

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            selectedServer?.let { startVpnService(context, it) }
        }
    }

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
                    TextButton(onClick = { clipboardManager.setText(AnnotatedString(errorLogs.joinToString("\n"))) }) { Text("Copy Log") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { showErrors = false }) { Text("Close") }
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("OFA VPN", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { 
                updateStatus = "Downloading core..."
                updateManager.downloadAndInstallCore { success, msg ->
                    updateStatus = if (success) "Core updated!" else "Failed: $msg"
                }
            }) {
                Text("Update Core")
            }
        }
        if (updateStatus.isNotEmpty()) {
            Text(updateStatus, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = inputText, 
            onValueChange = { inputText = it }, 
            label = { Text("Paste Sub URL OR Config (JSON/vless)") }, 
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = { 
            if (inputText.isNotBlank()) { 
                isLoading = true; statusMessage = "Processing..."; errorLogs = emptyList()
                scope.launch { 
                    val trimmedInput = inputText.trim()
                    if (!trimmedInput.startsWith("http://") && !trimmedInput.startsWith("https://")) {
                        try {
                            val parsedServers = ConfigParser.parseSubscription(trimmedInput)
                            withContext(Dispatchers.IO) { db.serverDao().deleteAll() }
                            withContext(Dispatchers.IO) { db.serverDao().insertAll(parsedServers) }
                            statusMessage = "Added ${parsedServers.size} config(s)."
                        } catch (e: Exception) {
                            statusMessage = "Parse Error: ${e.message}"
                        }
                    } else {
                        val urls = trimmedInput.split("\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
                        val allServers = mutableListOf<ServerEntity>()
                        val errors = mutableListOf<String>()
                        for (url in urls) {
                            try { allServers.addAll(withContext(Dispatchers.IO) { SubFetcher.fetchAndParse(url) }) } 
                            catch (e: Exception) { errors.add("$url -> ${e.message ?: "Unknown"}") }
                        }
                        if (allServers.isEmpty() && errors.isNotEmpty()) { statusMessage = "All links failed!" }
                        else {
                            withContext(Dispatchers.IO) { db.serverDao().deleteAll() }
                            withContext(Dispatchers.IO) { db.serverDao().insertAll(allServers) }
                            statusMessage = "Added ${allServers.size} servers."
                        }
                        errorLogs = errors
                    }
                    isLoading = false 
                } 
            } 
        }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) { Text(if (isLoading) "Loading..." else "Fetch / Add Config") }
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top=8.dp)) {
            Text(text = statusMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
            if (errorLogs.isNotEmpty()) { OutlinedButton(onClick = { showErrors = true }) { Text("View Log") } }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Servers List:", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        LazyColumn(modifier = Modifier.fillMaxSize()) { 
            items(servers) { server -> 
                ServerCard(server, onConnect = {
                    selectedServer = server
                    val intent = VpnService.prepare(context)
                    if (intent != null) {
                        vpnLauncher.launch(intent)
                    } else {
                        startVpnService(context, server)
                    }
                }) 
            } 
        }
    }
}

fun startVpnService(context: android.content.Context, server: ServerEntity) {
    val intent = Intent(context, VpnConnectionService::class.java)
    intent.action = "START"
    intent.putExtra("server_config", Gson().toJson(server))
    context.startService(intent)
}

@Composable
fun ServerCard(server: ServerEntity, onConnect: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) { Text(server.name); Text("${server.protocol} - ${server.address}:${server.port}", style = MaterialTheme.typography.bodySmall) }
            Button(onClick = onConnect) { Text("Connect") }
        }
    }
}