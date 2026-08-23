package com.vibelauncher.app.model

import kotlinx.serialization.Serializable

@Serializable
data class TodoItem(
    val id: Long,
    val text: String,
    val createdAt: Long
)
