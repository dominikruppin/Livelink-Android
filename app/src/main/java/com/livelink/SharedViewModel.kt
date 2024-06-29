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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.livelink.data.Repository
import com.livelink.data.UserData
import com.livelink.data.model.Channel
import com.livelink.data.model.ChannelJoin
import com.livelink.data.model.Message
import com.livelink.data.model.ZipCodeInfos
import com.livelink.data.remote.ZipCodeApi
import kotlinx.coroutines.launch

class SharedViewModel: ViewModel() {
    val auth = FirebaseAuth.getInstance()
    private val database = FirebaseFirestore.getInstance()
    private val usersCollectionReference = database.collection("users")
    private val channelsReference = database.collection("channels")
    private val storage = FirebaseStorage.getInstance()
    private val repository = Repository(ZipCodeApi)

    private val _currentUser = MutableLiveData<FirebaseUser?>(auth.currentUser)
    val currentUser : LiveData<FirebaseUser?>
        get() = _currentUser

    private val _userData = MutableLiveData<UserData>()
    val userData: LiveData<UserData>
        get() = _userData

    private val _channels = MutableLiveData<List<Channel>>()
    val channels: LiveData<List<Channel>>
        get() = _channels

    val zipCodeInfos = repository.zipInfos

    private val _currentChannel = MutableLiveData<ChannelJoin>()
    val currentChannel: LiveData<ChannelJoin>
        get() = _currentChannel

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>>
        get() = _messages

    private val _profileUserData = MutableLiveData<UserData?>()
    val profileUserData: LiveData<UserData?>
        get() = _profileUserData

    private var userDataDocumentReference: DocumentReference? = null
    private var userDataListener: ListenerRegistration? = null

    init {
        setupUserEnv()
        fetchChannels()
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

    fun joinChannel(channel: String) {
        _currentChannel.value = ChannelJoin(channel)
    }

    fun sendMessage(message: Message) {
        val channelId = currentChannel.value?.channelID // Hole die aktuelle Channel ID aus LiveData

        channelId?.let { id ->
            // Konvertiere Message-Objekt in eine Map
            val messageData = hashMapOf(
                "senderId" to message.senderId,
                "content" to message.content,
                "timestamp" to message.timestamp
            )

            // Speichere die Nachricht in der entsprechenden Channel-Nachrichtenkollektion
            channelsReference.document(id)
                .collection("messages")
                .add(messageData)
                .addOnSuccessListener { documentReference ->
                    Log.d("Channels", "Message sent with ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.e("Channels", "Error sending message", e)
                }
        }
    }

    fun fetchMessages(channelJoin: ChannelJoin) {
        channelsReference.document(channelJoin.channelID)
            .collection("messages")
            .whereGreaterThan("timestamp", channelJoin.timestamp) // Nachrichten filtern nach Zeitpunkt
            .orderBy("timestamp", Query.Direction.ASCENDING) // Sortieren nach Timestamp, absteigend
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Channels", "Error fetching messages", error)
                    return@addSnapshotListener
                }

                val messageList = mutableListOf<Message>()
                snapshot?.documents?.forEach { doc ->
                    val message = doc.toObject(Message::class.java)
                    message?.let {
                        messageList.add(it)
                    }
                }

                _messages.postValue(messageList)
            }
    }


    fun fetchChannels() {
        channelsReference
            .get()
            .addOnSuccessListener { querySnapshot ->
                val channelList = mutableListOf<Channel>()
                for (document in querySnapshot.documents) {
                    val channel = document.toObject(Channel::class.java)
                    channel?.let {
                        channelList.add(it)
                    }
                }
                _channels.value = channelList
                Log.d("Channels", "Fetched channels: $channelList")
            }
            .addOnFailureListener { e ->
                Log.e("Channels", "Error fetching channels", e)
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
                val userData = UserData(username = username, usernameLowercase = username.lowercase(), email = email)
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
        val lowercaseUsername = username.lowercase()
        usersCollectionReference.whereEqualTo("usernameLowercase", lowercaseUsername).get()
            .addOnSuccessListener { documents ->
                callback(!documents.isEmpty)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun getMailFromUsername(username: String, callback: (String?) -> Unit) {
        val lowercaseUsername = username.lowercase()
        usersCollectionReference.whereEqualTo("usernameLowercase", lowercaseUsername).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val email = documents.documents[0].getString("email")
                    callback(email)
                } else {
                    callback(null)
                }
            }
    }

    fun openProfile(username: String) {
        val lowercaseUsername = username.lowercase()
        usersCollectionReference.whereEqualTo("usernameLowercase", lowercaseUsername)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userData = documents.documents[0].toObject(UserData::class.java)
                    userData?.let {
                        Log.d("Profile", "Daten von $username geladen: $it")
                        _profileUserData.postValue(it)
                    }
                } else {
                    Log.d("Profile", "Benutzerdaten für $username nicht gefunden.")
                    _profileUserData.postValue(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Profile", "Fehler beim Abrufen der Benutzerdaten für $username", exception)
                _profileUserData.postValue(null)
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

    fun loadZipInfos(countryCode: String, zipCode: String) {
        viewModelScope.launch {
            repository.loadZipInfos(countryCode, zipCode)
        }
    }
}