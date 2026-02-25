package com.example.aitemplate.client.util

import kotlinx.datetime.Clock

fun generateConversationId(): String =
    "c-${Clock.System.now().toEpochMilliseconds()}"
