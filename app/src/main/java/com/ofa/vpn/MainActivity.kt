package com.ofa.vpn
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val servers by db.serverDao().getAllServers().collectAsState(initial = emptyList())
    var subUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("OFA VPN - Servers", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = subUrl, onValueChange = { subUrl = it }, label = { Text("Subscription URL") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { if (subUrl.isNotBlank()) { isLoading = true; statusMessage = "Fetching..."; scope.launch { try { val s = withContext(Dispatchers.IO) { SubFetcher.fetchAndParse(subUrl) }; withContext(Dispatchers.IO) { db.serverDao().deleteAll() }; withContext(Dispatchers.IO) { db.serverDao().insertAll(s) }; statusMessage = "Added ${s.size} servers." } catch (e: Exception) { statusMessage = "Error: ${e.message}" } finally { isLoading = false } } } }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) { Text(if (isLoading) "Loading..." else "Fetch Subscription") }
        Text(text = statusMessage, color = MaterialTheme.colorScheme.error)
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