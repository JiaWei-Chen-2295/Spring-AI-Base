package com.example.aitemplate.client.ui.screen.chat

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.example.aitemplate.client.data.model.*
import com.example.aitemplate.client.data.remote.AuthApi
import com.example.aitemplate.client.data.repository.ChatRepository
import com.example.aitemplate.client.data.repository.MetadataRepository
import com.example.aitemplate.client.ui.screen.auth.LoginScreen
import com.example.aitemplate.client.ui.screen.settings.SettingsScreenModel
import com.example.aitemplate.client.util.generateConversationId
import com.russhwolf.settings.Settings
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class ChatMessage(
    val role: String,
    val content: String,
    val toolCalls: List<ToolCallInfo> = emptyList(),
    val appliedSkills: List<SkillApplyInfo> = emptyList(),
    val timestamp: Instant = Clock.System.now()
)

enum class StreamState { IDLE, CONNECTING, STREAMING, ERROR }

class ChatScreenModel(
    private val chatRepo: ChatRepository,
    private val metadataRepo: MetadataRepository,
    private val authApi: AuthApi,
    private val settings: Settings
) : ScreenModel {

    // Username for display
    private val _username = MutableStateFlow<String?>(null)
    val username = _username.asStateFlow()

    init {
        _username.value = settings.getStringOrNull("username")
    }

    private val baseUrl: String
        get() = settings.getString(SettingsScreenModel.KEY_SERVER_URL, SettingsScreenModel.DEFAULT_SERVER_URL)

    // Metadata
    var models by mutableStateOf<List<ModelInfo>>(emptyList())
        private set
    var tools by mutableStateOf<List<ToolInfo>>(emptyList())
        private set
    var skills by mutableStateOf<List<SkillInfo>>(emptyList())
        private set

    // Selection state
    var selectedModelId by mutableStateOf("")
    var selectedTools by mutableStateOf<Set<String>>(emptySet())
    var selectedSkills by mutableStateOf<Set<String>>(emptySet())
    var streamMode by mutableStateOf(true)

    // Conversation
    var conversationId by mutableStateOf(generateConversationId())
        private set
    var conversations by mutableStateOf<List<ConversationInfo>>(emptyList())
        private set
    var messages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set

    // Stream state
    private val _streamState = MutableStateFlow(StreamState.IDLE)
    val streamState = _streamState.asStateFlow()
    var sending by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    // Quote state
    var quotedMessage by mutableStateOf<ChatMessage?>(null)
        private set

    fun setQuote(message: ChatMessage) { quotedMessage = message }
    fun clearQuote() { quotedMessage = null }

    private var streamJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun loadMetadata() {
        scope.launch {
            try {
                val fetchedModels = metadataRepo.getModels(baseUrl, refresh = true)
                val fetchedTools = metadataRepo.getTools(baseUrl, refresh = true)
                val fetchedSkills = metadataRepo.getSkills(baseUrl, refresh = true)

                models = fetchedModels
                tools = fetchedTools
                skills = fetchedSkills

                // Auto-select first model if none selected
                if (selectedModelId.isBlank() && fetchedModels.isNotEmpty()) {
                    selectedModelId = fetchedModels.first().modelId
                }
            } catch (e: ClientRequestException) {
                if (e.response.status == HttpStatusCode.Unauthorized) {
                    logout() // token expired — clear credentials and redirect to LoginScreen
                } else {
                    error = "Failed to load metadata: ${e.message}"
                }
            } catch (e: Exception) {
                error = "Failed to load metadata: ${e.message}"
            }
        }
    }

    fun loadConversations() {
        scope.launch {
            try {
                conversations = chatRepo.listConversations(baseUrl)
            } catch (_: Exception) { }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || sending) return

        error = null

        // Add user message
        messages = messages + ChatMessage(role = "user", content = text)

        if (streamMode) {
            sendStream(text)
        } else {
            sendSync(text)
        }
    }

    private fun sendStream(text: String) {
        sending = true
        _streamState.value = StreamState.CONNECTING

        // Add empty assistant placeholder
        messages = messages + ChatMessage(role = "assistant", content = "")

        streamJob = scope.launch {
            try {
                val flow = chatRepo.streamChat(
                    baseUrl = baseUrl,
                    conversationId = conversationId,
                    model = selectedModelId,
                    message = text,
                    tools = selectedTools.toList(),
                    skills = selectedSkills.toList()
                )

                flow.collect { event ->
                    _streamState.value = StreamState.STREAMING
                    val lastIdx = messages.lastIndex
                    val last = messages[lastIdx]

                    when (event) {
                        is SseEvent.Token -> {
                            messages = messages.toMutableList().apply {
                                this[lastIdx] = last.copy(content = last.content + event.text)
                            }
                        }
                        is SseEvent.ToolCall -> {
                            messages = messages.toMutableList().apply {
                                this[lastIdx] = last.copy(
                                    toolCalls = last.toolCalls + event.info
                                )
                            }
                        }
                        is SseEvent.ToolCallProgress -> {
                            // Upsert: if a call with same toolName already exists update it, else append
                            val existing = last.toolCalls.indexOfFirst { it.toolName == event.info.toolName }
                            val updatedCalls = if (existing >= 0) {
                                last.toolCalls.toMutableList().apply { this[existing] = event.info }
                            } else {
                                last.toolCalls + event.info
                            }
                            messages = messages.toMutableList().apply {
                                this[lastIdx] = last.copy(toolCalls = updatedCalls)
                            }
                        }
                        is SseEvent.SkillApply -> {
                            messages = messages.toMutableList().apply {
                                this[lastIdx] = last.copy(appliedSkills = event.skills)
                            }
                        }
                        is SseEvent.Error -> {
                            error = event.message
                            _streamState.value = StreamState.ERROR
                        }
                        is SseEvent.Done -> {
                            // Stream completed
                        }
                    }
                }

                _streamState.value = StreamState.IDLE
            } catch (e: CancellationException) {
                _streamState.value = StreamState.IDLE
            } catch (e: Exception) {
                error = e.message ?: "Stream failed"
                _streamState.value = StreamState.ERROR
            } finally {
                sending = false
                loadConversations()
            }
        }
    }

    private fun sendSync(text: String) {
        sending = true
        scope.launch {
            try {
                val response = chatRepo.chatOnce(
                    baseUrl = baseUrl,
                    request = ChatRequest(
                        conversationId = conversationId,
                        modelId = selectedModelId,
                        message = text,
                        tools = selectedTools.toList(),
                        skills = selectedSkills.toList()
                    )
                )

                messages = messages + ChatMessage(
                    role = "assistant",
                    content = response.content,
                    toolCalls = response.toolCalls
                )
            } catch (e: Exception) {
                error = e.message ?: "Request failed"
            } finally {
                sending = false
                loadConversations()
            }
        }
    }

    fun stopStream() {
        streamJob?.cancel()
        streamJob = null
        sending = false
        _streamState.value = StreamState.IDLE
    }

    fun newConversation() {
        conversationId = generateConversationId()
        messages = emptyList()
        error = null
        quotedMessage = null
    }

    fun switchConversation(id: String) {
        conversationId = id
        messages = emptyList()
        error = null
        quotedMessage = null
        scope.launch {
            try {
                val history = chatRepo.getMessages(baseUrl, id)
                messages = history.map { ChatMessage(role = it.role, content = it.content) }
            } catch (e: Exception) {
                error = "Failed to load history: ${e.message}"
            }
        }
    }

    fun deleteConversation(id: String) {
        scope.launch {
            try {
                chatRepo.deleteConversation(baseUrl, id)
                if (id == conversationId) {
                    newConversation()
                }
                loadConversations()
            } catch (e: Exception) {
                error = "Failed to delete: ${e.message}"
            }
        }
    }

    fun dismissError() {
        error = null
    }

    fun logout() {
        scope.launch {
            try {
                authApi.logout(baseUrl)
            } catch (e: Exception) {
                // Ignore error
            } finally {
                // Clear all auth data
                settings.remove("access_token")
                settings.remove("refresh_token")
                settings.remove("user_id")
                settings.remove("username")
                _username.value = null
                // Navigation will be handled by UI observing state change
            }
        }
    }

    override fun onDispose() {
        streamJob?.cancel()
        scope.cancel()
    }
}
