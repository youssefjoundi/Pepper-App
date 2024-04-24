package com.leet.pepperapp.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class reponseDto(
    @SerialName("question")
    val question: String?,
    @SerialName("text")
    val text: String?,
)
