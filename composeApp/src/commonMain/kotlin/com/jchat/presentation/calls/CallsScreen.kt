package com.jchat.presentation.calls

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun CallsScreen() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            CreateCallLink()
        }
        item {
            Text(
                text = "Recent",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold
            )
        }
        items(10) { index ->
            CallItem(index)
        }
    }
}

@Composable
private fun CreateCallLink() {
    ListItem(
        headlineContent = { Text("Create call link", fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text("Share a link for your JChat call") },
        leadingContent = {
            Surface(
                modifier = Modifier.size(48.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.padding(12.dp), tint = Color.White)
            }
        }
    )
}

@Composable
private fun CallItem(index: Int) {
    ListItem(
        headlineContent = { Text("Contact $index", fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            Row {
                Icon(
                    if (index % 2 == 0) Icons.Default.CallMade else Icons.Default.CallReceived,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (index % 3 == 0) Color.Red else Color.Green
                )
                Spacer(Modifier.width(4.dp))
                Text("Today, 10:${10 + index} AM")
            }
        },
        leadingContent = {
            AsyncImage(
                model = "https://i.pravatar.cc/150?u=call_$index",
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape)
            )
        },
        trailingContent = {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
            }
        }
    )
}
