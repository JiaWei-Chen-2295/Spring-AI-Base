package com.example.aitemplate.client.i18n

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

// ── Language enum ────────────────────────────────────────────────────────────
enum class Language(val label: String, val nativeLabel: String) {
    ZH("Chinese", "中文"),
    EN("English", "English");
}

// ── CompositionLocals ────────────────────────────────────────────────────────
val LocalStrings = staticCompositionLocalOf<Strings> { ZhStrings }
val LocalLanguage = compositionLocalOf { Language.ZH }
val LocalSetLanguage = compositionLocalOf<(Language) -> Unit> { {} }

// ── Strings interface ────────────────────────────────────────────────────────
interface Strings {

    // ── Common ───────────────────────────────────────────────────────────────
    val appName: String
    val cancel: String
    val save: String
    val close: String
    val delete: String
    val back: String
    val confirm: String
    val unknown: String
    val notSet: String
    val light: String
    val system: String
    val dark: String
    val logout: String
    val settings: String
    val profile: String
    val language: String

    // ── Login ────────────────────────────────────────────────────────────────
    val loginTitle: String
    val loginSubtitle: String
    val loginServerUrl: String
    val loginServerUrlPlaceholder: String
    val loginUsername: String
    val loginUsernamePlaceholder: String
    val loginPassword: String
    val loginPasswordPlaceholder: String
    val loginShowPassword: String
    val loginHidePassword: String
    val loginButton: String
    val loginDefaultAccount: String
    val loginChangeServer: String
    fun loginServerDisplay(url: String): String

    // ── Chat ─────────────────────────────────────────────────────────────────
    val chatDefaultTitle: String
    val chatMenu: String
    val chatConfiguration: String
    val chatAccount: String
    val chatDismiss: String
    val chatInputPlaceholder: String
    val chatInputDisclaimer: String
    val chatStop: String
    val chatSend: String
    val chatClearQuote: String
    val chatEmptyTitle: String
    val chatYou: String
    val chatAssistant: String
    val chatCopy: String
    val chatQuote: String
    val chatTotal: String
    val chatNewChat: String
    val chatExecutionSnapshot: String
    val chatModel: String
    val chatConv: String
    val chatMode: String
    val chatTools: String
    val chatSkills: String
    val chatSseStream: String
    val chatSingle: String
    val chatNone: String

    // ── Chat Quick Prompts ───────────────────────────────────────────────────
    val promptSummarize: String
    val promptSummarizeText: String
    val promptExplainCode: String
    val promptExplainCodeText: String
    val promptDebugJson: String
    val promptDebugJsonText: String
    val promptGenerateSql: String
    val promptGenerateSqlText: String

    // ── Config Panel ─────────────────────────────────────────────────────────
    val configTabSettings: String
    val configTabHistory: String
    val configModel: String
    val configModelHint: String
    val configBehavior: String
    val configStreamResponses: String
    val configStreamHint: String
    val configActiveTools: String
    val configSkills: String
    val configTheme: String
    val configServerConnected: String
    val configServerOnline: String
    val configNoConversations: String
    val configStartNewChat: String
    val configConversations: String
    fun configEnabled(count: Int): String
    fun configConversationLabel(id: String): String
    fun configCompleted(count: Int): String

    // ── Settings Screen ──────────────────────────────────────────────────────
    val settingsTitle: String
    val settingsSubtitle: String
    val settingsTestingConnection: String
    val settingsConnect: String
    val settingsTestConnection: String
    fun settingsConnected(modelCount: Int): String

    // ── Profile Screen ───────────────────────────────────────────────────────
    val profileTitle: String
    val profilePersonalInfo: String
    val profileEmail: String
    val profilePhone: String
    val profileGender: String
    val profileMale: String
    val profileFemale: String
    val profileEditProfile: String
    val profileChangePassword: String
    val profileConfirmLogout: String
    val profileLogoutMessage: String
    val profileCurrentPassword: String
    val profileNewPassword: String
    val profileConfirmNewPassword: String
    val profilePasswordMismatch: String
    val profileChange: String
    val profileName: String

    // ── Stream Status ────────────────────────────────────────────────────────
    val streamConnecting: String
    val streamStreaming: String
    val streamError: String

    // ── Tool Calls ───────────────────────────────────────────────────────────
    val toolCalls: String
    val toolInput: String
    val toolOutput: String
    fun toolCompleted(count: Int): String
}

// ══════════════════════════════════════════════════════════════════════════════
// 中文 (Default)
// ══════════════════════════════════════════════════════════════════════════════
object ZhStrings : Strings {

    // Common
    override val appName = "AI Template"
    override val cancel = "取消"
    override val save = "保存"
    override val close = "关闭"
    override val delete = "删除"
    override val back = "返回"
    override val confirm = "确认"
    override val unknown = "未知"
    override val notSet = "未设置"
    override val light = "浅色"
    override val system = "系统"
    override val dark = "深色"
    override val logout = "退出登录"
    override val settings = "设置"
    override val profile = "个人资料"
    override val language = "语言"

    // Login
    override val loginTitle = "AI Template"
    override val loginSubtitle = "用户登录"
    override val loginServerUrl = "服务器地址"
    override val loginServerUrlPlaceholder = "http://localhost:8080"
    override val loginUsername = "用户名"
    override val loginUsernamePlaceholder = "请输入用户名"
    override val loginPassword = "密码"
    override val loginPasswordPlaceholder = "请输入密码"
    override val loginShowPassword = "显示密码"
    override val loginHidePassword = "隐藏密码"
    override val loginButton = "登录"
    override val loginDefaultAccount = "默认账号: admin / admin123"
    override val loginChangeServer = "修改"
    override fun loginServerDisplay(url: String) = "服务器: $url"

    // Chat
    override val chatDefaultTitle = "Clean Slate AI"
    override val chatMenu = "菜单"
    override val chatConfiguration = "配置"
    override val chatAccount = "账户"
    override val chatDismiss = "关闭"
    override val chatInputPlaceholder = "输入消息..."
    override val chatInputDisclaimer = "AI 回答可能存在错误，请核实重要信息。"
    override val chatStop = "停止"
    override val chatSend = "发送"
    override val chatClearQuote = "清除引用"
    override val chatEmptyTitle = "CLEAN SLATE AI"
    override val chatYou = "你"
    override val chatAssistant = "助手"
    override val chatCopy = "复制"
    override val chatQuote = "引用"
    override val chatTotal = "总计"
    override val chatNewChat = "新对话"
    override val chatExecutionSnapshot = "执行快照"
    override val chatModel = "模型"
    override val chatConv = "会话"
    override val chatMode = "模式"
    override val chatTools = "工具"
    override val chatSkills = "技能"
    override val chatSseStream = "SSE 流式"
    override val chatSingle = "单次"
    override val chatNone = "无"

    // Quick Prompts
    override val promptSummarize = "总结文本"
    override val promptSummarizeText = "请总结要点。"
    override val promptExplainCode = "解释代码"
    override val promptExplainCodeText = "请逐步解释。"
    override val promptDebugJson = "调试 JSON"
    override val promptDebugJsonText = "请调试以下 JSON。"
    override val promptGenerateSql = "生成 SQL"
    override val promptGenerateSqlText = "请生成 SQL..."

    // Config Panel
    override val configTabSettings = "设置"
    override val configTabHistory = "历史"
    override val configModel = "模型"
    override val configModelHint = "选择推理模型。切换模型将开始新会话。"
    override val configBehavior = "行为"
    override val configStreamResponses = "流式响应"
    override val configStreamHint = "逐字输出效果"
    override val configActiveTools = "启用工具"
    override val configSkills = "技能"
    override val configTheme = "主题"
    override val configServerConnected = "已连接到本地服务"
    override val configServerOnline = "在线"
    override val configNoConversations = "暂无对话"
    override val configStartNewChat = "开始新对话"
    override val configConversations = "对话列表"
    override fun configEnabled(count: Int) = "${count} 已启用"
    override fun configConversationLabel(id: String) = "对话 $id"
    override fun configCompleted(count: Int) = "${count} 已完成"

    // Settings Screen
    override val settingsTitle = "AI Template Client"
    override val settingsSubtitle = "请输入 Spring AI 服务器地址"
    override val settingsTestingConnection = "正在测试连接..."
    override val settingsConnect = "连接"
    override val settingsTestConnection = "测试连接"
    override fun settingsConnected(modelCount: Int) = "已连接 — $modelCount 个模型可用"

    // Profile
    override val profileTitle = "用户资料"
    override val profilePersonalInfo = "个人信息"
    override val profileEmail = "邮箱"
    override val profilePhone = "手机"
    override val profileGender = "性别"
    override val profileMale = "男"
    override val profileFemale = "女"
    override val profileEditProfile = "编辑资料"
    override val profileChangePassword = "修改密码"
    override val profileConfirmLogout = "确认退出"
    override val profileLogoutMessage = "确定要退出登录吗？"
    override val profileCurrentPassword = "当前密码"
    override val profileNewPassword = "新密码"
    override val profileConfirmNewPassword = "确认新密码"
    override val profilePasswordMismatch = "两次密码不一致"
    override val profileChange = "修改"
    override val profileName = "姓名"

    // Stream Status
    override val streamConnecting = "连接中"
    override val streamStreaming = "传输中"
    override val streamError = "错误"

    // Tool Calls
    override val toolCalls = "工具调用"
    override val toolInput = "输入"
    override val toolOutput = "输出"
    override fun toolCompleted(count: Int) = "${count} 已完成"
}

// ══════════════════════════════════════════════════════════════════════════════
// English
// ══════════════════════════════════════════════════════════════════════════════
object EnStrings : Strings {

    // Common
    override val appName = "AI Template"
    override val cancel = "Cancel"
    override val save = "Save"
    override val close = "Close"
    override val delete = "Delete"
    override val back = "Back"
    override val confirm = "Confirm"
    override val unknown = "Unknown"
    override val notSet = "Not set"
    override val light = "Light"
    override val system = "System"
    override val dark = "Dark"
    override val logout = "Logout"
    override val settings = "Settings"
    override val profile = "Profile"
    override val language = "Language"

    // Login
    override val loginTitle = "AI Template"
    override val loginSubtitle = "User Login"
    override val loginServerUrl = "Server URL"
    override val loginServerUrlPlaceholder = "http://localhost:8080"
    override val loginUsername = "Username"
    override val loginUsernamePlaceholder = "Enter username"
    override val loginPassword = "Password"
    override val loginPasswordPlaceholder = "Enter password"
    override val loginShowPassword = "Show password"
    override val loginHidePassword = "Hide password"
    override val loginButton = "Login"
    override val loginDefaultAccount = "Default account: admin / admin123"
    override val loginChangeServer = "Edit"
    override fun loginServerDisplay(url: String) = "Server: $url"

    // Chat
    override val chatDefaultTitle = "Clean Slate AI"
    override val chatMenu = "Menu"
    override val chatConfiguration = "Configuration"
    override val chatAccount = "Account"
    override val chatDismiss = "Dismiss"
    override val chatInputPlaceholder = "Message Clean Slate AI..."
    override val chatInputDisclaimer = "Clean Slate AI can make mistakes. Verify important information."
    override val chatStop = "Stop"
    override val chatSend = "Send"
    override val chatClearQuote = "Clear quote"
    override val chatEmptyTitle = "CLEAN SLATE AI"
    override val chatYou = "You"
    override val chatAssistant = "Assistant"
    override val chatCopy = "Copy"
    override val chatQuote = "Quote"
    override val chatTotal = "Total"
    override val chatNewChat = "New Chat"
    override val chatExecutionSnapshot = "Execution Snapshot"
    override val chatModel = "Model"
    override val chatConv = "Conv"
    override val chatMode = "Mode"
    override val chatTools = "Tools"
    override val chatSkills = "Skills"
    override val chatSseStream = "SSE Stream"
    override val chatSingle = "Single"
    override val chatNone = "none"

    // Quick Prompts
    override val promptSummarize = "Summarize Text"
    override val promptSummarizeText = "Please summarize the key points."
    override val promptExplainCode = "Explain Code"
    override val promptExplainCodeText = "Please explain step by step."
    override val promptDebugJson = "Debug JSON"
    override val promptDebugJsonText = "Debug the following JSON."
    override val promptGenerateSql = "Generate SQL"
    override val promptGenerateSqlText = "Generate SQL for..."

    // Config Panel
    override val configTabSettings = "Settings"
    override val configTabHistory = "History"
    override val configModel = "MODEL"
    override val configModelHint = "Select the underlying inference model. Changing models will start a new session."
    override val configBehavior = "BEHAVIOR"
    override val configStreamResponses = "Stream Responses"
    override val configStreamHint = "Typewriter effect for tokens"
    override val configActiveTools = "ACTIVE TOOLS"
    override val configSkills = "SKILLS"
    override val configTheme = "THEME"
    override val configServerConnected = "Connected to Localhost"
    override val configServerOnline = "Online"
    override val configNoConversations = "No conversations yet"
    override val configStartNewChat = "Start a new chat to begin"
    override val configConversations = "CONVERSATIONS"
    override fun configEnabled(count: Int) = "$count Enabled"
    override fun configConversationLabel(id: String) = "Conversation $id"
    override fun configCompleted(count: Int) = "$count completed"

    // Settings Screen
    override val settingsTitle = "AI Template Client"
    override val settingsSubtitle = "Enter Spring AI server URL"
    override val settingsTestingConnection = "Testing connection..."
    override val settingsConnect = "Connect"
    override val settingsTestConnection = "Test Connection"
    override fun settingsConnected(modelCount: Int) = "Connected — $modelCount model(s) available"

    // Profile
    override val profileTitle = "User Profile"
    override val profilePersonalInfo = "Personal Information"
    override val profileEmail = "Email"
    override val profilePhone = "Phone"
    override val profileGender = "Gender"
    override val profileMale = "Male"
    override val profileFemale = "Female"
    override val profileEditProfile = "Edit Profile"
    override val profileChangePassword = "Change Password"
    override val profileConfirmLogout = "Confirm Logout"
    override val profileLogoutMessage = "Are you sure you want to logout?"
    override val profileCurrentPassword = "Current Password"
    override val profileNewPassword = "New Password"
    override val profileConfirmNewPassword = "Confirm New Password"
    override val profilePasswordMismatch = "Passwords do not match"
    override val profileChange = "Change"
    override val profileName = "Name"

    // Stream Status
    override val streamConnecting = "connecting"
    override val streamStreaming = "streaming"
    override val streamError = "error"

    // Tool Calls
    override val toolCalls = "Tool Calls"
    override val toolInput = "INPUT"
    override val toolOutput = "OUTPUT"
    override fun toolCompleted(count: Int) = "$count completed"
}

// ── Factory ──────────────────────────────────────────────────────────────────
fun getStrings(language: Language): Strings = when (language) {
    Language.ZH -> ZhStrings
    Language.EN -> EnStrings
}
