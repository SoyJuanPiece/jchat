package com.jchat.presentation.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun UpdatesScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        // Status Section
        Text(
            text = "Status",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().height(100.dp)
        ) {
            item {
                MyStatusItem()
            }
            items(5) { index ->
                OtherStatusItem(index)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Channels Section
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Channels",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {}) {
                Icon(Icons.Default.Add, contentDescription = "Find Channels")
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(3) { index ->
                ChannelItem(index)
            }
        }
    }
}

@Composable
private fun MyStatusItem() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(60.dp)) {
            AsyncImage(
                model = "https://via.placeholder.com/150",
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.LightGray)
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .align(Alignment.BottomEnd)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
        Text("My Status", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun OtherStatusItem(index: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
        ) {
            AsyncImage(
                model = "https://i.pravatar.cc/150?u=status_$index",
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text("Contact $index", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ChannelItem(index: Int) {
    ListItem(
        headlineContent = { Text("WhatsApp Channel $index", fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text("Latest news and updates from this channel...") },
        leadingContent = {
            AsyncImage(
                model = "https://i.pravatar.cc/150?u=channel_$index",
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape)
            )
        },
        trailingContent = {
            Text("10:45 AM", style = MaterialTheme.typography.labelSmall)
        }
    )
}
