package com.leet.pepperapp.network.remote

sealed class ResultApi<T>(val data:T?=null, val error:String?=null){
    class Success<T>(quotes: T):ResultApi<T>(data = quotes)
    class Error<T>(error: String):ResultApi<T>(error = error)
    class Loading<T>:ResultApi<T>()
}