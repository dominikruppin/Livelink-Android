package com.livelink

import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
import com.livelink.data.model.ProfileVisitor
import com.livelink.data.remote.ZipCodeApi
import kotlinx.coroutines.launch

class SharedViewModel(application: Application) : AndroidViewModel(application) {
    // Instanz für das Authentifizierungssystem (Registrierung, Login, Passwort vergessen)
    val auth = FirebaseAuth.getInstance()
    // Instanz für die Datenbank wo userData, channels und Messages gespeichert sind
    private val database = FirebaseFirestore.getInstance()
    // Pfad der userData
    private val usersCollectionReference = database.collection("users")
    // Pfad der Channels
    private val channelsReference = database.collection("channels")
    // Storage für Profilbilder
    private val storage = FirebaseStorage.getInstance()
    // Repository für API Call
    private val repository = Repository(ZipCodeApi)

    // Speichert den aktuell eingeloggten User
    private val _currentUser = MutableLiveData<FirebaseUser?>(auth.currentUser)
    // Getter für den eingeloggten User
    val currentUser : LiveData<FirebaseUser?>
        get() = _currentUser

    // Speichert die UserData des eingeloggten Uses
    private val _userData = MutableLiveData<UserData>()
    // Getter für die UserData des eingeloggten Users
    val userData: LiveData<UserData>
        get() = _userData
    // Speichert die Liste aller Channel
    private val _channels = MutableLiveData<List<Channel>>()
    // Getter für die Channelliste
    val channels: LiveData<List<Channel>>
        get() = _channels
    // Getter für die Postleitzahlinfos der API // Referenz auf die LiveData im Repository
    val zipCodeInfos = repository.zipInfos
    // Speichert den aktuellen Channel
    private val _currentChannel = MutableLiveData<ChannelJoin>()
    // Getter für den aktuellen Channel
    val currentChannel: LiveData<ChannelJoin>
        get() = _currentChannel
    // Speichert die Nachrichten des aktuellen Channels
    private val _messages = MutableLiveData<List<Message>>()
    // Getter für die Nachrichten des aktuellen Channels
    val messages: LiveData<List<Message>>
        get() = _messages
    // Speichert die Daten des Users, dessen Profil man aufruft
    private val _profileUserData = MutableLiveData<UserData?>()
    // Getter für die Daten des Users, dessen Profil man aufruft
    val profileUserData: LiveData<UserData?>
        get() = _profileUserData
    // Speichert die Daten der gesuchten Usernamen
    private val _searchResults = MutableLiveData<List<UserData>>()
    // Getter für die Suchergebnisse
    val searchResults: LiveData<List<UserData>>
        get() = _searchResults

    private var userDataDocumentReference: DocumentReference? = null
    private var userDataListener: ListenerRegistration? = null
    private var messageListener: ListenerRegistration? = null


    // Wird beim erstellen des ViewModels ausgeführt (also appstart)
    init {
        // Wir richten die Umgebung des eingeloggten Users ein, zb holen wir uns
        // seine aktuellen UserDaten
        setupUserEnv()
        // Wir holen uns die aktuelle Channelliste
        fetchChannels()
    }

    // Wir richten die Userumgebung ein
    fun setupUserEnv() {
        Log.d("User", "setupUserEnv aufgerufen")
        Log.d("User", "aktueller user: ${currentUser.value}")
        // Wir speichern den aktuell eingeloggten FirebaseUser in der MutableLiveData
        // Falls niemand eingeloggt ist, wird null gespeichert
        _currentUser.value = auth.currentUser
        Log.d("User", "Neuer User: ${currentUser.value}")
        // Wir prüfen ob die LiveData NICHT null ist, also ein User eingeloggt ist
        if (currentUser.value != null) {
            Log.d("User", "User is not null, laden der Daten")
            // usersCollectionReference ist die Collection ("users"). Dort laden wir
            // das Document (DocumentID ist die Firebase UID) des jeweiligen Users,
            // welches die UserData enthält
            // Ergo: Wir greifen auf den Pfad "users/uid/" zu und kriegen damit die UserData
            userDataDocumentReference = usersCollectionReference.document(currentUser.value!!.uid)
            // Wir starten das beobachten der UserData
            startUserDataListener()
        } else {
            // Wenn currentUser NULL ist, also niemand eingeloggt, dann entfernen wir den
            // userDataListener
            userDataListener?.remove()
            userDataListener = null
        }
    }

    // Funktion um einen Channel zu betreten
    fun joinChannel(channel: Channel) {
        // Setzen den übergebenen Channel als aktuellen Channel in die MutableLiveData
        _currentChannel.value = ChannelJoin(channel.name, channel.backgroundUrl)

        // Hier fügen wir nun den betretenen Channel in die UserData des
        // eingeloggten Users bei den lastChannels hinzu
        // Aktuelle Liste der letzten Channels abrufen und aktualisieren
        val updatedLastChannels = _userData.value?.lastChannels.orEmpty().toMutableList()

        // Prüfen, ob der Channel bereits in der Liste vorhanden ist
        val existingChannel = updatedLastChannels.find { it.name == channel.name }
        // Falls vorhanden
        if (existingChannel != null) {
            // entfernen des Channels (damit wir ihn anschließend wieder an die
            // letzte Position hinzufügen)
            updatedLastChannels.remove(existingChannel)
        }

        // Channel am Ende hinzufügen
        updatedLastChannels.add(channel)

        // Falls die Liste mehr als 10 Channels enthält..
        if (updatedLastChannels.size > 10) {
            // entfernen wir den ältesten
            updatedLastChannels.removeAt(0)
        }

        // Aktualisierte Liste in Firebase speichern
        val updates = mapOf("lastChannels" to updatedLastChannels)
        updateUserData(updates) { success ->
            if (success) {
                Log.d("JoinChannel", "Channel erfolgreich zu lastChannels hinzugefügt.")
            } else {
                Log.e("JoinChannel", "Fehler beim Hinzufügen des Channels zu lastChannels.")
            }
        }
    }

    // Ruft ein Nutzer das Profil eines anderen Nutzers auf, wird er in den Profil-
    // Besuchers dieses Nutzers gespeichert. So kann jeder seine letzten Profil-
    // Besucher sehen

    // TODO:
    fun addProfileVisitor(visitedUser: UserData, visitor: ProfileVisitor) {
        // Aktuelle Liste der Profilbesucher aus dem userData des aufgerufenen Nutzers laden
        val updatedProfileVisitors = visitedUser.recentProfileVisitors.toMutableList()

        // Prüfen, ob der aufrufende Nutzer (visitor) bereits in der Liste vorhanden ist
        val existingVisitor = updatedProfileVisitors.find { it.username == visitor.username }

        // Falls vorhanden, entfernen, um später wieder hinzugefügt zu werden
        if (existingVisitor != null) {
            updatedProfileVisitors.remove(existingVisitor)
        }

        // Hinzufügen des aufrufenden Nutzers (visitor) an die vorderste Position der Liste
        updatedProfileVisitors.add(visitor)

        // Falls die Liste mehr als 30 Besucher enthält, den ältesten entfernen
        if (updatedProfileVisitors.size > 30) {
            updatedProfileVisitors.removeAt(30)
        }

        // map stellen, mit UserData Feld als Key und Liste der Profilbesucher als value
        val updates = mapOf("recentProfileVisitors" to updatedProfileVisitors)

        // Wir holen uns die usersCollection über den Usernamen des Nutzers, dessen
        // Profil aufgerufen wurde
        usersCollectionReference.whereEqualTo("usernameLowercase", visitedUser.usernameLowercase)
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Überprüfen, ob der Nutzer gefunden wurde
                if (!querySnapshot.isEmpty) {
                    val documentSnapshot = querySnapshot.documents[0]
                    // Dokumentreferenz des Nutzers, dessen Profilbesucher aktualisiert werden sollen
                    val userDocumentRef = documentSnapshot.reference

                    // Prpfilbesucher in den UserData updaten
                    userDocumentRef.update(updates)
                        // Erfolgreich
                        .addOnSuccessListener {
                            Log.d("AddProfileVisitor", "Profilbesucher erfolgreich hinzugefügt.")
                        }
                        // Nicht erfolgreich
                        .addOnFailureListener { e ->
                            Log.e("AddProfileVisitor", "Fehler beim Hinzufügen des Profilbesuchers.", e)
                        }
                // Nutzer wurde nicht gefunden
                } else {
                    Log.e("AddProfileVisitor", "Nutzer mit usernameLowercase '${visitedUser.usernameLowercase}' nicht gefunden.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("AddProfileVisitor", "Fehler beim Suchen des Nutzers mit usernameLowercase '${visitedUser.usernameLowercase}'.", e)
            }
    }


    // Funktion zum senden einer Nachricht an einen Channel
    fun sendMessage(message: Message) {
        // Wir holen uns die ID des aktuellen Channels (id ist gleichzeitig
        // der Channelname
        val channelId = currentChannel.value?.channelID

        // Wenn die channelid nicht null ist..
        channelId?.let { id ->
            // Konvertiere Message-Objekt in eine Map
            val messageData = hashMapOf(
                "senderId" to message.senderId,
                "content" to message.content,
                "timestamp" to message.timestamp
            )

            // Speichere die Nachricht in der entsprechenden Channel-Nachrichtencollection
            channelsReference.document(id)
                .collection("messages")
                .add(messageData)
                .addOnSuccessListener { documentReference ->
                    Log.d("Channels", "Nachricht gesendet: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.e("Channels", "Fehler beim senden der Nachricht: $e")
                }
        }
    }

    // Funktion zum abrufen der Nachrichten eines Channels
    fun fetchMessages(channelJoin: ChannelJoin) {
        // Zuerst den bestehenden Listener entfernen, falls vorhanden
        messageListener?.remove()

        // Wir nehmen die channelID aus dem übergebenen ChannelJoin-Objekt und holen uns
        // das Dokument mit den Nachrichten
        Log.d("Chat", "Lade Nachrichten aus ${channelJoin.channelID} von ${channelJoin.timestamp}")
        messageListener = channelsReference.document(channelJoin.channelID)
            // dort greifen wir auf die Collection mit den Nachrichten zu
            .collection("messages")
            // Wir filtern nach dem timestamp, sodass nur Nachrichten geladen werden,
            // die seit dem Betreten des Channels gesendet wurden
            .whereGreaterThan("timestamp", channelJoin.timestamp)
            // wir sortieren die Nachrichten nach dem Timestamp aufsteigend
            .orderBy("timestamp", Query.Direction.ASCENDING)
            // Fügen einen addSnapshotListener hinzu, bedeutet wenn neue Nachrichten
            // vorhanden sind, kriegen wir diese automatisch
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Channels", "Error fetching messages", error)
                    return@addSnapshotListener
                }
                // legen eine Liste an, in der wir alle Nachrichten speichern
                val messageList = mutableListOf<Message>()
                // Erstellen aus den übergebenen Daten die Message-Objekte
                snapshot?.documents?.forEach { doc ->
                    val message = doc.toObject(Message::class.java)
                    // Fügen nach dem Umwandeln die Message Objekte der Liste hinzu
                    message?.let {
                        messageList.add(it)
                    }
                }
                // Speichern die Liste in der LiveData
                Log.d("Chat", "Folgende Nachrichten geladen: $messageList")
                _messages.postValue(messageList)
            }
    }

    // Funktion zum abrufen der Channels
    fun fetchChannels() {
        // Anhand der channelsReference ("channels)" holen wir uns die Channelliste
        channelsReference
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Legen eine Liste an, in der wir die Channel-Objekte speichern können
                val channelList = mutableListOf<Channel>()
                for (document in querySnapshot.documents) {
                    // Erstellen die Channel-Objekte für die übergebenen Daten
                    val channel = document.toObject(Channel::class.java)
                    // Und fügen diese der channelliste hinzu
                    channel?.let {
                        channelList.add(it)
                    }
                }
                // Channelliste wird in der LiveData gespeichert
                _channels.value = channelList
                Log.d("Channels", "Fetched channels: $channelList")
            }
            .addOnFailureListener { e ->
                Log.e("Channels", "Error fetching channels", e)
            }
    }

    // Funktion um die UserData des eingeloggten Users zu beobachten
    private fun startUserDataListener() {
        // Falls schon ein Listener vorhanden, entfernen wir ihn
        userDataListener?.remove()

        // Neuen Listener anlegen der die UserData des eingeloggten Nutzers überwacht
        userDataListener = userDataDocumentReference?.addSnapshotListener { snapshot, exception ->
            // Bei einem Fehler wird die weitere Ausführung des Codes abgebrochen
            if (exception != null) {
                Log.e("UserDataListener", "Listen failed", exception)
                return@addSnapshotListener
            }
            // Wenn es neue Daten gibt..
            if (snapshot != null && snapshot.exists()) {
                // Konvertieren wir diese in ein UserData Objekt
                val userData = snapshot.toObject(UserData::class.java)
                // Und speichern es in der LiveData
                _userData.postValue(userData ?: UserData())
            } else {
                Log.d("UserDataListener", "Current data: null")
                _userData.postValue(UserData())
            }
        }
    }

    // Funktion zum ändern der UserData des eingeloggten Nutzers
    // Die geänderten Daten werden als Map übergeben. Key ist dabei das jeweilige
    // Profilfeld, value der neue Wert. Außerdem ein Callback ob das ganze
    // erfolgreich war oder nicht
    fun updateUserData(updates: Map<String, Any>, callback: (Boolean) -> Unit) {
        userDataDocumentReference?.update(updates)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(true)
            } else {
                callback(false)
            }
        }
    }

    // Funktion zum registrieren eines neuen Nutzers
    // Benötigt einen usernamen, email und password. Außerdem wird ein Callback
    // zurückgegeben, ob die Registrierung erfolgreich war
    fun register(username: String, email: String, password: String, callback: (Boolean) -> Unit) {
        // Erstellen den neuen Nutzer per Firebase Auth
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            // Wenn erfolgreich erstellt..
            if (task.isSuccessful) {
                // holen wir uns den eingeloggten User
                val user = auth.currentUser
                // Und erstellen ein neues UserData Objekt
                // Dabei speichern wir den Usernamen extra noch mal im Feld
                // usernameLowercase als Kleinbuchstaben ab, für spätere
                // Datenbankabfragen
                val userData = UserData(username = username, usernameLowercase = username.lowercase(), email = email)
                // Anhand des Firebaseuser..
                user?.let {
                    // holen wir uns die uid, welche dann als DokumentID dient und erstellen ein Dokument in dem wir die
                    // UserData speichern.
                    usersCollectionReference.document(it.uid).set(userData)
                        .addOnCompleteListener { setTask ->
                            if (setTask.isSuccessful) {
                                // Erfolgreich UserData gespeichert
                                callback(true)
                            } else {
                                // Fehler beim speichern der userData
                                callback(false)
                            }
                        }
                }
            } else {
                // Fehler bei der Registrierung
                callback(false)
            }
        }
    }

    // Funktion prüft ob ein Username vergeben ist, liefert per Callback das
    // Ergebnis als Boolean
    fun isUsernameTaken(username: String, callback: (Boolean) -> Unit) {
        // Wandeln den übergebenen Usernamen in Kleinbuchstaben um
        val lowercaseUsername = username.lowercase()
        // Durchsuchen die usersCollection (wo alle User gespeichert sind)
        // nach dem user. Dabei suchen wir explit nach dem Feld usernameLowercase
        usersCollectionReference.whereEqualTo("usernameLowercase", lowercaseUsername).get()
            // Wenn wir einen Treffer haben, existiert der Username und wir senden
            // den Callback mit false
            .addOnSuccessListener { documents ->
                callback(!documents.isEmpty)
            }
            // Wenn wir keinen Treffer haben, existiert der Username nicht und wir
            // senden den Callback mit true
            .addOnFailureListener {
                callback(false)
            }
    }

    // Funktion zum abrufen der Emailadresse zu einem Usernamen
    // Callback welcher die Emailadresse zurückliefert
    fun getMailFromUsername(username: String, callback: (String?) -> Unit) {
        // Wandeln den Usernamen in Kleinbuchstaben um
        val lowercaseUsername = username.lowercase()
        // Suchen in der usersCollection nach dem Usernamen
        usersCollectionReference.whereEqualTo("usernameLowercase", lowercaseUsername).get()
            // Haben wir einen Treffer..
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Laden wir uns die Email aus den Feld email
                    val email = documents.documents[0].getString("email")
                    // und senden die mail als callback
                    callback(email)
                // Haben wir keinen Treffer..
                } else {
                    // senden wir einen Callback mit null
                    callback(null)
                }
            }
    }

    // Funktion zum öffnen eines Profils
    fun openProfile(username: String) {
        // Wandeln den übergebenen Usernamen in Kleinbuchstaben um
        val lowercaseUsername = username.lowercase()
        // Suchen in der usersCollection im Feld usernameLowercase nach dem Usernamen
        usersCollectionReference.whereEqualTo("usernameLowercase", lowercaseUsername)
            .get()
            // Haben wir einen Treffer..
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Dann laden wir die UserDaten und konvertieren sie in ein
                    // UserData Objekt
                    val userData = documents.documents[0].toObject(UserData::class.java)
                    // Das Objekt laden wir dann in die LiveData
                    userData?.let {
                        Log.d("Profile", "Daten von $username geladen: $it")
                        _profileUserData.postValue(it)
                    }
                // Haben wir KEINEN Treffer..
                } else {
                    // Geben wir eine Fehlermeldung aus
                    Log.d("Profile", "Benutzerdaten für $username nicht gefunden.")
                    Toast.makeText(getApplication(), "Der Nutzer $username existiert nicht.", Toast.LENGTH_LONG).show()
                    // Und setzen die LiveData auf null
                    _profileUserData.postValue(null)
                }
            }
            // Ebenfalls wenn wir einen Fehler erhalten..
            .addOnFailureListener { exception ->
                Log.e("Profile", "Fehler beim Abrufen der Benutzerdaten für $username", exception)
                // Senden wir eine Fehlermeldung
                Toast.makeText(getApplication(), "Fehler beim Abrufen der Benutzerdaten für $username.", Toast.LENGTH_LONG).show()
                // Und setzen die LiveData auf null
                _profileUserData.postValue(null)
            }
    }

    // Funktion um nach Nutzern zu suchen
    fun searchUsers(query: String) {
        val queryLowercase = query.lowercase()
        val queryEnd = queryLowercase + "\uf8ff"

        usersCollectionReference.whereGreaterThanOrEqualTo("usernameLowercase", queryLowercase)
            .whereLessThanOrEqualTo("usernameLowercase", queryEnd)
            .get()
            .addOnSuccessListener { documents ->
                val users = documents.mapNotNull { it.toObject(UserData::class.java) }
                val filteredUsers = users.filter { it.usernameLowercase.contains(queryLowercase) }
                _searchResults.postValue(filteredUsers)
            }
            .addOnFailureListener {
                _searchResults.postValue(emptyList())
            }
    }


    // Funktion zum einloggen eines Nutzers
    // Callback liefert das Ergebnis ob der Login erfolgreich war oder nicht
    fun login(email: String, password: String, callback: (Boolean) -> Unit) {
        // Rufen die Firebase Auth Funktion für den Login mit Emailadresse und
        // Passwort auf
        auth.signInWithEmailAndPassword(email, password)
            // Login erfolgreich
            .addOnSuccessListener {
                // Senden den Callback mit true, also Login erfolgreich
                callback(true)
                // Und richten die Userumgebung (UserData etc.) ein
                setupUserEnv()
            }
            // Login nicht erfolgreich
            .addOnFailureListener {
                // Senden den Callback mit false, also Login fehlgeschlagen
                callback(false)
            }
    }

    // Funktion um den aktuellen User auszuloggen
    fun logout() {
        Log.d("Logout", "Logout wird durchgeführt für User: ${currentUser.value}")
        // Firebase Auth Funktion um den Nutzer auszuloggen
        auth.signOut()
        // Setzen die Userumgebung neu (bei ausgeloggtem User wird alles auf
        // null gesetzt)
        setupUserEnv()
    }

    // Funktion zum hochladen eines Profilbildes
    fun uploadProfilePicture(uri: Uri) {
        // Den aktuellen Zeitpunkt als Timestamp
        val currentTimestampMillis = System.currentTimeMillis()
        // Wir legen den Bildpfad fest auf images/userUID/profilepics/timestamp
        val imageRef = storage.reference.child(
            "images/"
                    + currentUser.value!!.uid +
                    "/profilepics/" + currentTimestampMillis)

        // Hochladen des Bildes
        val uploadTask = imageRef.putFile(uri)
        // Listener ob Upload vollständig
        uploadTask.addOnCompleteListener {
            // Listener der uns die downloadUrl des Bildes liefert
            imageRef.downloadUrl.addOnSuccessListener {
                // Speichern der URL als String
                val imageUrl = it.toString()
                Log.d("ProfilePicUrl", imageUrl)
                // Updaten der UserData des eingeloggten Nutzers
                // Im Feld profilePicURL wird die URL zum Bild gespeichert
                userDataDocumentReference?.update("profilePicURL" , imageUrl)
            }
        }
    }

    // Funktion zum laden der Infos zu einer Postleitzahl per openPLZ API
    // Dafür wird das Länderkürzel und die Postleitzahl benötigt
    fun loadZipInfos(countryCode: String, zipCode: String) {
        viewModelScope.launch {
            repository.loadZipInfos(countryCode, zipCode)
        }
    }
}