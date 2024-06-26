package com.livelink

import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.livelink.data.Repository
import com.livelink.data.UserData
import com.livelink.data.remote.ZipCodeApi
import kotlinx.coroutines.launch

class SharedViewModel: ViewModel() {
    val auth = FirebaseAuth.getInstance()
    // val chatDatabase = FirebaseDatabase.getInstance()
    private val database = FirebaseFirestore.getInstance()
    private val usersCollectionReference = database.collection("users")
    private val storage = FirebaseStorage.getInstance()
    private val repository = Repository(ZipCodeApi)

    private val _currentUser = MutableLiveData<FirebaseUser?>(auth.currentUser)
    val currentUser : LiveData<FirebaseUser?>
        get() = _currentUser

    private val _userData = MutableLiveData<UserData>()
    val userData: LiveData<UserData>
        get() = _userData

    val zipCodeInfos = repository.zipInfos

    private var userDataDocumentReference: DocumentReference? = null
    private var userDataListener: ListenerRegistration? = null

    init {
        setupUserEnv()
    }

    fun setupUserEnv() {
        Log.d("User", "setupUserEnv aufgerufen")
        Log.d("User", "aktueller user: ${currentUser.value}")
        _currentUser.value = auth.currentUser
        Log.d("User", "Neuer User: ${currentUser.value}")
        if (currentUser.value != null) {
            Log.d("User", "User is not null, laden der Daten")
            userDataDocumentReference = usersCollectionReference.document(currentUser.value!!.uid)
            startUserDataListener()
        } else {
            userDataListener?.remove()
            userDataListener = null
        }
    }

    private fun startUserDataListener() {
        userDataListener?.remove()

        userDataListener = userDataDocumentReference?.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.e("UserDataListener", "Listen failed", exception)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val userData = snapshot.toObject(UserData::class.java)
                _userData.postValue(userData ?: UserData())
            } else {
                Log.d("UserDataListener", "Current data: null")
                _userData.postValue(UserData())
            }
        }
    }

    fun updateUserData(updates: Map<String, Any>, callback: (Boolean) -> Unit) {
        userDataDocumentReference?.update(updates)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(true)
            } else {
                callback(false)
            }
        }
    }


    fun register(username: String, email: String, password: String, callback: (Boolean) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                val userData = UserData(username = username, email = email)
                user?.let {
                    usersCollectionReference.document(it.uid).set(userData)
                        .addOnCompleteListener { setTask ->
                            if (setTask.isSuccessful) {
                                callback(true) // Erfolgreiche der Registrierung
                            } else {
                                callback(false) // Fehler beim Speichern der Benutzerdaten
                            }
                        }
                }
            } else {
                callback(false) // Fehler bei der Registrierung
            }
        }
    }

    fun isUsernameTaken(username: String, callback: (Boolean) -> Unit) {
        usersCollectionReference.whereEqualTo("username", username).get()
            .addOnSuccessListener { documents ->
                callback(!documents.isEmpty)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun getMailFromUsername(username: String, callback: (String?) -> Unit) {
        usersCollectionReference.whereEqualTo("username", username).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val email = documents.documents[0].getString("email")
                    callback(email)
                } else {
                    callback(null)
                }
            }
    }

    fun login(email: String, password: String, callback: (Boolean) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                callback(true)
                setupUserEnv()
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun logout() {
        Log.d("Logout", "Logout wird durchgeführt für User: ${currentUser.value}")
        auth.signOut()
        setupUserEnv()
    }

    fun uploadProfilePicture(uri: Uri) {
        val currentTimestampMillis = System.currentTimeMillis()
        val imageRef = storage.reference.child(
            "images/"
                    + currentUser.value!!.uid +
                    "/profilepics/" + currentTimestampMillis)

        val uploadTask = imageRef.putFile(uri)

        uploadTask.addOnCompleteListener {
            imageRef.downloadUrl.addOnSuccessListener {
                val imageUrl = it.toString()
                Log.d("ProfilePicUrl", imageUrl)
                userDataDocumentReference?.update("profilePicURL" , imageUrl)
            }
        }
    }

    fun loadZipInfos(country: String, zipcode: String) {
        viewModelScope.launch {
            repository.loadZipInfos(country, zipcode)
        }
    }
}