package com.leet.pepperapp.network.repository

import com.leet.pepperapp.model.dto.reponseDto
import com.leet.pepperapp.network.remote.ResultApi
import kotlinx.coroutines.flow.Flow

interface ChatApi {
    fun getResponse(audioPath : String?) : Flow<ResultApi<reponseDto>>

}