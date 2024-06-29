package com.livelink.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.livelink.data.model.ZipCodeInfos
import com.livelink.data.remote.ZipCodeApi
import com.livelink.data.remote.ZipCodeApiService

// Repository zum abrufen der API Daten (openPLZ Datenbank)
class Repository(val api: ZipCodeApi) {

    // Hier speichern wir das Ergebnis des API Calls
    private val _zipInfos = MutableLiveData<ZipCodeInfos?>()

    // Getter für die empfangenen und gespeicherten API Daten
    val zipInfos: LiveData<ZipCodeInfos?>
        get() = _zipInfos

    // Funktion zum abrufen der der API Daten und Speicherung in der MutableLiveData (oder den Wert null,
    // falls API Call schief ging). sad
    suspend fun loadZipInfos(country: String, zipcode: String) {
        try {
            _zipInfos.postValue(api.apiService.getZipInfos(country, zipcode).firstOrNull())
        } catch (e: Exception) {
            Log.d("UserData", "Scheiße, API Call lief schief: $e")
            _zipInfos.postValue(null)
        }
    }

}