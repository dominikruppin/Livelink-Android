package com.livelink

import android.app.Application
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.livelink.data.UserData

class SharedViewModel: ViewModel() {
    val auth = FirebaseAuth.getInstance()
    // val chatDatabase = FirebaseDatabase.getInstance()
    val database = FirebaseFirestore.getInstance()
    val usersCollectionReference = database.collection("users")

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser : LiveData<FirebaseUser?>
        get() = _currentUser

    private var userDataDocumentReference: DocumentReference? = null

    init {
        setupUserEnv()
    }

    fun setupUserEnv() {
        _currentUser.postValue(currentUser.value)
        if(currentUser.value != null){
            userDataDocumentReference = usersCollectionReference.document(currentUser.value!!.uid)
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
                                _currentUser.postValue(user)
                                callback(true) // Erfolg der Registrierung
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

}