package com.leet.pepperapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leet.pepperapp.model.dto.reponseDto
import com.leet.pepperapp.network.remote.ResultApi
import com.leet.pepperapp.network.repository.ChatApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


@HiltViewModel
class AppViewModel @Inject constructor(

    private val ChatService : ChatApi,
    private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _chatresponse = MutableStateFlow<ResultApi<reponseDto>>(ResultApi.InitState())


    val responseResult = _chatresponse.asStateFlow()


    fun fetchChatResponse(message : String?) {


        viewModelScope.launch {

            Log.i("Hello ", "From ViewModel : $message")

            ChatService.getResponse(message)
                .flowOn(defaultDispatcher)
                .catch {
                    _chatresponse.value = ResultApi.Error(it.message ?: "Error")
                }
                .collect {
                    _chatresponse.value = it
                }

        }

    }


    fun pepperState(state : String) {
        when(state) {
            "think" -> {
                _chatresponse.value = ResultApi.Thinking()
                Log.i("Hello Think", "From ViewModel : $state")
            }
            "init" -> {
                Log.i("Hello init", "From ViewModel : $state")
            }
            "listen" -> {
                Log.i("Hello listen", "From ViewModel : $state")
                _chatresponse.value = ResultApi.Listening()

            }
            "done" -> {
                _chatresponse.value = ResultApi.Done()
            }
        }
    }



}