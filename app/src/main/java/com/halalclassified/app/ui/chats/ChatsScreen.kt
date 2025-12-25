package com.halalclassified.app.ui.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.halalclassified.app.data.ads.AdWithImage
import com.halalclassified.app.data.ads.AdsRepository
import com.halalclassified.app.data.chat.ChatRecord
import com.halalclassified.app.data.chat.ChatRepository
import com.halalclassified.app.data.chat.MessageRecord
import com.halalclassified.app.data.supabase.SupabaseClientProvider
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ChatsScreen(
    startChatId: String?,
    onChatConsumed: () -> Unit
) {
    val supabase = SupabaseClientProvider.client
    val chatRepository = remember { ChatRepository(supabase) }
    val adsRepository = remember { AdsRepository(supabase) }
    val sessionStatus by supabase.auth.sessionStatus.collectAsState()
    val userId = (sessionStatus as? SessionStatus.Authenticated)?.session?.user?.id
    val scope = rememberCoroutineScope()

    var listState by remember { mutableStateOf(ChatListState()) }
    var selectedChatId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        if (userId.isNullOrBlank()) {
            listState = listState.copy(
                isLoading = false,
                errorMessage = "Log in to view your chats."
            )
            return@LaunchedEffect
        }
        listState = listState.copy(isLoading = true, errorMessage = null)
        val chats = runCatching { chatRepository.fetchChats(userId) }
            .getOrElse {
                listState = listState.copy(
                    isLoading = false,
                    errorMessage = it.message ?: "Unable to load chats."
                )
                emptyList()
            }
        val adItems = adsRepository.fetchAdsByIds(
            chats.map { it.adId }.distinct(),
            activeOnly = false
        )
        val adById = adItems.associateBy { it.ad.id }
        listState = listState.copy(
            isLoading = false,
            chats = chats.map { chat ->
                ChatThreadItem(
                    chat = chat,
                    ad = adById[chat.adId]
                )
            }
        )
    }

    LaunchedEffect(startChatId) {
        if (!startChatId.isNullOrBlank()) {
            selectedChatId = startChatId
            onChatConsumed()
        }
    }

    if (selectedChatId != null) {
        ChatThreadScreen(
            chatId = selectedChatId ?: "",
            onBack = { selectedChatId = null }
        )
        return
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Chats",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Your conversations about listings live here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (listState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (listState.errorMessage != null) {
                StatusBanner(message = listState.errorMessage ?: "")
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(listState.chats, key = { it.chat.id }) { item ->
                    ChatRow(
                        item = item,
                        isSeller = item.chat.sellerId == userId,
                        onClick = { selectedChatId = item.chat.id }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatThreadScreen(
    chatId: String,
    onBack: () -> Unit
) {
    val supabase = SupabaseClientProvider.client
    val chatRepository = remember { ChatRepository(supabase) }
    val sessionStatus by supabase.auth.sessionStatus.collectAsState()
    val userId = (sessionStatus as? SessionStatus.Authenticated)?.session?.user?.id
    val scope = rememberCoroutineScope()

    var messages by remember { mutableStateOf<List<MessageRecord>>(emptyList()) }
    var input by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(chatId) {
        loading = true
        messages = runCatching { chatRepository.fetchMessages(chatId) }
            .getOrElse { emptyList() }
        loading = false
    }

    LaunchedEffect(chatId) {
        chatRepository.observeMessages(chatId).collect { message ->
            if (messages.none { it.id == message.id }) {
                messages = messages + message
            }
        }
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(text = "Chat", style = MaterialTheme.typography.titleLarge)
            }

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isMine = message.senderId == userId
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Type a message") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.Button(
                    onClick = {
                        val senderId = userId ?: return@Button
                        val body = input.trim()
                        if (body.isBlank()) return@Button
                        scope.launch {
                            val message = chatRepository.sendMessage(chatId, senderId, body)
                            if (messages.none { it.id == message.id }) {
                                messages = messages + message
                            }
                            input = ""
                        }
                    }
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun ChatRow(
    item: ChatThreadItem,
    isSeller: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageUrl = item.ad?.imageUrl
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    )
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = item.ad?.ad?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Chat",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.ad?.ad?.title?.takeIf { it.isNotBlank() } ?: "Listing",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isSeller) "Buyer inquiry" else "Seller",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatChatTime(item.chat.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MessageBubble(message: MessageRecord, isMine: Boolean) {
    val background = if (isMine) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (isMine) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = background,
            contentColor = content,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(text = message.body, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = formatChatTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = content.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class ChatListState(
    val isLoading: Boolean = false,
    val chats: List<ChatThreadItem> = emptyList(),
    val errorMessage: String? = null
)

private data class ChatThreadItem(
    val chat: ChatRecord,
    val ad: AdWithImage?
)

private fun formatChatTime(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val instant = runCatching { Instant.parse(raw) }.getOrNull() ?: return ""
    return DateTimeFormatter.ofPattern("dd MMM", Locale.US)
        .withZone(ZoneId.systemDefault())
        .format(instant)
}
