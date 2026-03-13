package com.jchat.presentation.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jchat.domain.model.ContentType
import com.jchat.domain.model.Message
import com.jchat.domain.model.MessageStatus
import kotlinx.datetime.Clock
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    chatId: String,
    currentUserId: String,
    onNavigateBack: () -> Unit,
    viewModel: ConversationViewModel = koinViewModel(parameters = { parametersOf(chatId) }),
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Scroll to the bottom on new messages
    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is ConversationEvent.ScrollToBottom -> {
                    if (state.messages.isNotEmpty()) {
                        listState.animateScrollToItem(state.messages.lastIndex)
                    }
                }
            }
        }
    }

    // Show errors
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(ConversationIntent.DismissError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = state.participantAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = state.participantName.ifBlank { "Chat" },
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            Text(
                                text = if (state.isTyping) "typing..." else state.participantStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.isTyping) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                state.uploadProgress?.let { progress ->
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                MessageInputBar(
                    text = state.inputText,
                    isSending = state.isSending,
                    onTextChange = { viewModel.onIntent(ConversationIntent.UpdateInput(it)) },
                    onSendClick = { viewModel.onIntent(ConversationIntent.SendTextMessage) },
                    onAttachClick = { /* TODO: Open file picker */ },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.surface,
                        )
                    )
                )
                .padding(paddingValues),
        ) {
            if (state.isLoading && state.messages.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.messages.isEmpty()) {
                Text(
                    text = "No messages yet. Say hello!",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                MessageList(
                    messages = state.messages,
                    currentUserId = currentUserId,
                    listState = listState,
                    onRetryFailedMessage = { messageId ->
                        viewModel.onIntent(ConversationIntent.RetryFailedMessage(messageId))
                    },
                )
            }
        }
    }
}

// ─── Message List ─────────────────────────────────────────────────────────────

@Composable
private fun MessageList(
    messages: List<Message>,
    currentUserId: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onRetryFailedMessage: (String) -> Unit,
) {
    val conversationItems = remember(messages, currentUserId) {
        buildConversationItems(messages = messages, currentUserId = currentUserId)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        reverseLayout = false,
    ) {
        items(
            items = conversationItems,
            key = {
                when (it) {
                    is ConversationListItem.DayHeader -> "day-${it.epochDay}"
                    is ConversationListItem.Bubble -> it.message.id
                }
            },
            contentType = {
                when (it) {
                    is ConversationListItem.DayHeader -> "day_header"
                    is ConversationListItem.Bubble -> it.message.contentType.name
                }
            },
        ) { item ->
            when (item) {
                is ConversationListItem.DayHeader -> DaySeparator(label = item.label)
                is ConversationListItem.Bubble -> MessageBubble(
                    message = item.message,
                    isFromCurrentUser = item.isFromCurrentUser,
                    groupedWithPrevious = item.groupedWithPrevious,
                    groupedWithNext = item.groupedWithNext,
                    onRetryFailedMessage = onRetryFailedMessage,
                )
            }
        }
    }
}

// ─── Message Bubble ───────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(
    message: Message,
    isFromCurrentUser: Boolean,
    groupedWithPrevious: Boolean,
    groupedWithNext: Boolean,
    onRetryFailedMessage: (String) -> Unit,
) {
    val alignment = if (isFromCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isFromCurrentUser) {
        SentBubbleColor
    } else {
        ReceivedBubbleColor
    }
    val contentColor = if (isFromCurrentUser) {
        SentBubbleContentColor
    } else {
        ReceivedBubbleContentColor
    }
    val shape = bubbleShape(
        isFromCurrentUser = isFromCurrentUser,
        groupedWithPrevious = groupedWithPrevious,
        groupedWithNext = groupedWithNext,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (groupedWithPrevious) 1.dp else 6.dp,
                bottom = if (groupedWithNext) 1.dp else 4.dp,
            ),
        contentAlignment = alignment,
    ) {
        Surface(
            shape = shape,
            color = bubbleColor,
            contentColor = contentColor,
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                when (message.contentType) {
                    ContentType.TEXT -> {
                        Text(
                            text = message.content ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }

                    ContentType.IMAGE -> {
                        val imageModel = message.mediaUrl ?: message.mediaLocalPath
                        AsyncImage(
                            model = imageModel,
                            contentDescription = "Image message",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    }

                    ContentType.AUDIO -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Audio message", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    else -> {
                        Text(
                            text = "📎 ${message.contentType.name.lowercase()} attachment",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp),
                ) {
                    Text(
                        text = formatTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f),
                    )
                    if (isFromCurrentUser) {
                        Spacer(modifier = Modifier.size(4.dp))
                        MessageStatusIcon(
                            status = message.status,
                            defaultTint = contentColor.copy(alpha = 0.8f),
                        )

                        if (message.status == MessageStatus.FAILED) {
                            TextButton(
                                onClick = { onRetryFailedMessage(message.id) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            ) {
                                Text(
                                    text = "Retry",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(
    status: MessageStatus,
    defaultTint: Color,
) {
    when (status) {
        MessageStatus.SENDING -> {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Sending",
                modifier = Modifier.size(14.dp),
                tint = defaultTint.copy(alpha = 0.7f),
            )
        }

        MessageStatus.SENT, MessageStatus.DELIVERED -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered",
                modifier = Modifier.size(14.dp),
                tint = defaultTint,
            )
        }

        MessageStatus.READ -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Read",
                modifier = Modifier.size(14.dp),
                tint = ReadReceiptColor,
            )
        }

        MessageStatus.FAILED -> Text(
            text = "!",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun DaySeparator(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Input Bar ────────────────────────────────────────────────────────────────

@Composable
private fun MessageInputBar(
    text: String,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            IconButton(onClick = onAttachClick) {
                Icon(Icons.Default.AttachFile, contentDescription = "Attach file")
            }

            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                placeholder = { Text("Message…") },
                maxLines = 6,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )

            val canSend = text.isNotBlank() && !isSending
            IconButton(
                onClick = onSendClick,
                enabled = canSend,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (canSend) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) MaterialTheme.colorScheme.onTertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Formats an [Instant] to a short "HH:MM" string in the device's local timezone. */
private fun formatTime(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hours = localDateTime.hour.toString().padStart(2, '0')
    val minutes = localDateTime.minute.toString().padStart(2, '0')
    return "$hours:$minutes"
}

private sealed interface ConversationListItem {
    data class DayHeader(
        val epochDay: Int,
        val label: String,
    ) : ConversationListItem

    data class Bubble(
        val message: Message,
        val isFromCurrentUser: Boolean,
        val groupedWithPrevious: Boolean,
        val groupedWithNext: Boolean,
    ) : ConversationListItem
}

private const val MessageGroupWindowMs = 5 * 60 * 1000L
private val ReadReceiptColor = Color(0xFF1EA7FD)
private val SentBubbleColor = Color(0xFFDCF8C6)
private val SentBubbleContentColor = Color(0xFF102A1F)
private val ReceivedBubbleColor = Color(0xFFFFFFFF)
private val ReceivedBubbleContentColor = Color(0xFF1A262B)

private fun buildConversationItems(
    messages: List<Message>,
    currentUserId: String,
): List<ConversationListItem> {
    if (messages.isEmpty()) return emptyList()

    val tz = TimeZone.currentSystemDefault()
    val result = mutableListOf<ConversationListItem>()

    for (index in messages.indices) {
        val current = messages[index]
        val previous = messages.getOrNull(index - 1)
        val next = messages.getOrNull(index + 1)

        val currentEpochDay = current.createdAt.toLocalDateTime(tz).date.toEpochDays()
        val previousEpochDay = previous?.createdAt?.toLocalDateTime(tz)?.date?.toEpochDays()

        if (previousEpochDay == null || previousEpochDay != currentEpochDay) {
            result += ConversationListItem.DayHeader(
                epochDay = currentEpochDay,
                label = formatDayLabel(current.createdAt),
            )
        }

        result += ConversationListItem.Bubble(
            message = current,
            isFromCurrentUser = current.senderId == currentUserId,
            groupedWithPrevious = shouldGroupMessages(previous, current),
            groupedWithNext = shouldGroupMessages(current, next),
        )
    }

    return result
}

private fun shouldGroupMessages(
    first: Message?,
    second: Message?,
): Boolean {
    if (first == null || second == null) return false
    if (first.senderId != second.senderId) return false

    val tz = TimeZone.currentSystemDefault()
    val firstDate = first.createdAt.toLocalDateTime(tz).date
    val secondDate = second.createdAt.toLocalDateTime(tz).date
    if (firstDate != secondDate) return false

    val diffMs = second.createdAt.toEpochMilliseconds() - first.createdAt.toEpochMilliseconds()
    return diffMs in 0..MessageGroupWindowMs
}

private fun bubbleShape(
    isFromCurrentUser: Boolean,
    groupedWithPrevious: Boolean,
    groupedWithNext: Boolean,
): RoundedCornerShape {
    val large = 18.dp
    val medium = 8.dp
    val tail = 4.dp
    return if (isFromCurrentUser) {
        RoundedCornerShape(
            topStart = large,
            topEnd = if (groupedWithPrevious) medium else large,
            bottomEnd = if (groupedWithNext) medium else tail,
            bottomStart = large,
        )
    } else {
        RoundedCornerShape(
            topStart = if (groupedWithPrevious) medium else large,
            topEnd = large,
            bottomEnd = large,
            bottomStart = if (groupedWithNext) medium else tail,
        )
    }
}

private fun formatDayLabel(instant: Instant): String {
    val tz = TimeZone.currentSystemDefault()
    val date = instant.toLocalDateTime(tz).date
    val today = Clock.System.now().toLocalDateTime(tz).date
    val todayEpochDay = today.toEpochDays()
    val targetEpochDay = date.toEpochDays()

    if (targetEpochDay == todayEpochDay) return "Today"
    if (targetEpochDay == todayEpochDay - 1) return "Yesterday"

    val month = when (date.monthNumber) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        else -> "Dec"
    }
    return "${date.dayOfMonth} $month"
}
