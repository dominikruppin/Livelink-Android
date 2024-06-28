package com.livelink.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.livelink.data.model.ZipCodeInfos
import com.livelink.data.remote.ZipCodeApi
import com.livelink.data.remote.ZipCodeApiService

class Repository(val api: ZipCodeApi) {

    private val _zipInfos = MutableLiveData<ZipCodeInfos?>()

    val zipInfos: LiveData<ZipCodeInfos?>
        get() = _zipInfos

    suspend fun loadZipInfos(country: String, zipcode: String) {
        try {
            _zipInfos.postValue(api.apiService.getZipInfos(country, zipcode).firstOrNull())
        } catch (e: Exception) {
            Log.d("UserData", "Scheiße, API Call lief schief: $e")
            _zipInfos.postValue(null)
        }
    }

}