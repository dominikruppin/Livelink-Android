package com.livelink.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.livelink.data.model.BotMessage
import com.livelink.data.model.BotRequest
import com.livelink.data.model.Message
import com.livelink.data.model.ZipCodeInfos
import com.livelink.data.remote.BotApi
import com.livelink.data.remote.ZipCodeApi
import com.livelink.data.remote.ZipCodeApiService

// Repository zum abrufen der API Daten (openPLZ Datenbank)
class Repository(val zipAPI: ZipCodeApi, val botAPI: BotApi) {

    // Hier speichern wir das Ergebnis des API Calls
    private val _zipInfos = MutableLiveData<ZipCodeInfos?>()

    // Getter für die empfangenen und gespeicherten API Daten
    val zipInfos: LiveData<ZipCodeInfos?>
        get() = _zipInfos

    private val _botMessage = MutableLiveData<Message?>()
    val botMessage: LiveData<Message?>
        get() = _botMessage

    // Funktion zum abrufen der der API Daten und Speicherung in der MutableLiveData (oder den Wert null,
    // falls API Call schief ging). sad
    suspend fun loadZipInfos(country: String, zipcode: String) {
        try {
            _zipInfos.postValue(zipAPI.apiService.getZipInfos(country, zipcode).firstOrNull())
        } catch (e: Exception) {
            Log.d("UserData", "Scheiße, API Call lief schief: $e")
            _zipInfos.postValue(null)
        }
    }

    fun resetBotMessage() {
        _botMessage.value = null
    }

    suspend fun sendMessageToBot(text: String, apiKey: String) {
            try {
                val botRequest = BotRequest(
                    model = "llama-3-sonar-large-32k-online",
                    messages = listOf(
                        BotMessage("system", "Du bist ein Chatbot in einer Livechat App die LiveLink heißt. Dein eigener Name ist Paul und du arbeitest beim Syntax Institut."),
                        BotMessage("user", text)
                    )
                )
                val response = BotApi.apiService.sendMessage("Bearer $apiKey", botRequest)
                val botReply = response.choices.firstOrNull()?.message?.content ?: "Ich schlafe gerade."
                _botMessage.postValue(Message("Paul", botReply))
            } catch (e: Exception) {
                e.printStackTrace()
                _botMessage.postValue(null)
            }
    }

}