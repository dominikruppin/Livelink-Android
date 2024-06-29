package com.livelink

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.livelink.databinding.ActivityMainBinding
import androidx.activity.viewModels
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import coil.load
import com.livelink.data.UserData
import com.livelink.data.model.ProfileVisitor
import com.livelink.databinding.PopupProfileBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val viewModel: SharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Layout Binden
        binding = ActivityMainBinding.inflate(layoutInflater)
        // Layout anzeigen
        setContentView(binding.root)
        // Toolbar aus Layout binden und anzeigen
        setSupportActionBar(binding.appBarMain.toolbar)
        // Binden des Navigationslayouts im Seitenmenü
        val drawerLayout: DrawerLayout = binding.drawerLayout
        // Navigation View für Seitenmenü
        val navView: NavigationView = binding.navView
        // NavController zum navigieren der Fragmente
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Binding für den Header der Navigation View (Seitenmenü)
        val headerBinding = binding.navView.getHeaderView(0)
        // TextView für den Benutzernamen im Seitenmenü
        val username = headerBinding.findViewById<TextView>(R.id.MenuUsername)
        // ImageView für das Profilbild im Seitenmenü
        val profilePic = headerBinding.findViewById<ImageView>(R.id.MenuProfilePicture)
        // Konfiguration der AppBar mit den Fragmenten die angesteuert werden
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.overviewFragment, R.id.channelsFragment, R.id.editProfileFragment
            ), drawerLayout
        )
        // Toolbar mit Navcontroller verbinden
        setupActionBarWithNavController(navController, appBarConfiguration)
        // Navigation View(Seitenmenü) mit NavController verbinden
        navView.setupWithNavController(navController)

        // Überprüfen in welches Fragment der NavController gerade navigiert
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Je nach Fragment die Toolbar ein oder ausblenden
            // Außerdem den Titel in der Toolbar anpassen
            when (destination.id) {
                R.id.overviewFragment -> {
                    supportActionBar?.show()
                    supportActionBar?.title = "Übersicht"
                }
                R.id.channelsFragment -> {
                    supportActionBar?.show()
                    supportActionBar?.title = "Channels"
                }
                R.id.editProfileFragment -> {
                    supportActionBar?.show()
                    supportActionBar?.title = "Profil bearbeiten"
                }
                R.id.channelFragment -> {
                    supportActionBar?.show()
                    supportActionBar?.title = viewModel.currentChannel.value?.channelID
                }
                else -> supportActionBar?.hide()
            }
        }

        // Abrufen der UserData des eingeloggten Nutzers
        viewModel.userData.observe(this) { user ->
            Log.d("User", user.toString())
            // Wenn der eingeloggte User KEIN Profilbild hat..
            if (user.profilePicURL.isEmpty()) {
                // Setzen wir einen Platzhalter in die Imageview
                profilePic.setImageResource(R.drawable.placeholder_profilepic)
            // Wenn er ein Bild hat..
            } else {
                // .. laden wir das Bild in die ImageView
                // Das Bild kriegen wir aus den UserData (profilePicURL)
                profilePic.load(user.profilePicURL)
            }

            // Wir setzen den Usernamen in die dafür vorgesehene TextView
            // Die TextView ist im Header des Seitenmenüs
            username.text = user.username

            // Beim Klick auf den Header des Seitenmenüs..
            headerBinding.setOnClickListener {
                // Öffnen wir das Profil des eingeloggten Users
                viewModel.openProfile(user.username)
            }
        }

        // Wenn ein Nutzerprofil aufgerufen wird, werden die Daten
        // in die LiveData (profileUserData) geladen
        // Hier beobachten wir die LiveData. Neue LiveData bedeutet
        // das ein neues Profil geöffnet werden soll
        viewModel.profileUserData.observe(this) { userData ->
            // Wir erstellen aus dem aktuell eingeloggten User und seinem
            // Usernamen und der URL zu seinem Profilbild ein neues Objekt
            // vom Typ ProfileVisitor
            val visitor = viewModel.userData.value?.let {
                ProfileVisitor(
                    it.username,
                    it.profilePicURL
                )
            }
            // Wenn die Daten des Users dessen Profil wir aufrufen wollen
            // nicht null sind..
            if (userData != null) {
                // Dann rufen wir die Funktion zum öffnen des Profils mit
                // diesen Daten auf
                showProfilePopup(userData)
                // Wir prüfen ob wir NICHT unser eigenes Profil aufrufen
                if (viewModel.userData.value!!.username != userData.username) {
                    // Wir prüfen ob unsere eigenen Daten nicht NULL sind
                    if (visitor != null) {
                        // Wir speichern unsere Daten (ProfileUser Objekt)
                        // in den UserData des Nutzers, dessen Profil wir
                        // aufgerufen haben in den letzten Profilbesuchern
                        // Dafür übergeben wir unsere eigenen Daten als UserData
                        // und die des Nutzers, dessen Profil wir aufgerufen haben
                        // als ProfileVisitor
                        viewModel.addProfileVisitor(userData, visitor)
                    }
                }
            }
        }
    }

    // Funktion zum anzeigen des Profils als Popup
    private fun showProfilePopup(userData: UserData) {
        // Wir inflaten und binden das Layout (popup_profile.xml)
        val popupBinding = PopupProfileBinding.inflate(LayoutInflater.from(this))
        val popupView = popupBinding.root

        // Legen die Attribute des Popups fest
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        // Insbesondere die Position auf dem Bidlschirm. In dem Fall mittig
        popupWindow.showAtLocation(binding.root, Gravity.CENTER, 0, 0)

        // Wenn man den Button zum schließen des Popups anklickt..
        popupBinding.btnClose.setOnClickListener {
            // .. schließen wir das Popup hiermit. Cool oder?
            popupWindow.dismiss()
        }

        // Wir füllen die verschiedenen Views mit den Profildaten
        popupBinding.textViewUsername.text = userData.username
        if (userData.profilePicURL.isNotEmpty()) {
            popupBinding.imageViewProfilePic.load(userData.profilePicURL)
        }
    }

    // Erstellen des Optionsmenüs in der Action Bar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }
    /*
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }*/

    // Anpassen der zurück Navigation
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Überprüfen, ob das aktuelle Ziel das LoginFragment,
        // Passwort vergessen oder RegisterFragment ist
        val currentDestinationId = navController.currentDestination?.id
        if (currentDestinationId == R.id.loginFragment ||
            currentDestinationId == R.id.registerFragment || currentDestinationId == R.id.passwordResetFragment
        ) {
            // Rücknavigation in diesen Fragmenten unterbinden
            return false
        }
        // Ansonsten normale Navigation ermöglichen
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    // Reagieren auf Klicks im Optionsmenü der Action Bar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Logout wurde angeklickt
            R.id.action_logout -> {
                // Funktion zum ausloggen des Users aufrufen
                viewModel.logout()
                // Und zum Login Fragment navigieren
                findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.loginFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

