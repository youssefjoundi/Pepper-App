package com.leet.pepperapp.network.repository

import android.util.Log
import com.leet.pepperapp.model.dto.reponseDto
import com.leet.pepperapp.network.remote.ResultApi
import com.leet.pepperapp.utils.resource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.lang.Exception
import javax.inject.Inject

class ChatApiImpl  @Inject constructor(private val client: HttpClient) : ChatApi {
    override fun getResponse(audioPath : String?): Flow<ResultApi<reponseDto>> = flow {

        emit(ResultApi.Loading())

        val file = audioPath?.let { File(it) }


        Log.d("Hello", "STart : ")

        try {

            emit(
                ResultApi.Success(
                    client.post {
                        url(resource.pepperUrl)
                        setBody(
                            MultiPartFormDataContent(
                                formData {
                                    file?.readBytes()?.let {
                                        append("audio", it, Headers.build {
                                            append(HttpHeaders.ContentDisposition, "filename=$audioPath")
                                        })
                                    }
                                }
                            )
                        )
                    }.body<reponseDto>()


//                    client.post {
//                        url(resource.flowiseUrl)
//                        setBody("{\"question\" : \"what is the best solution to learn football\"}")
//                        contentType(ContentType.Application.Json)
//                    }.body<reponseDto>()






                )
            )



        } catch ( e : Exception) {
            e.printStackTrace()

            emit(ResultApi.Error(e.message ?: "Something went wrong !"))
        }

    }

}