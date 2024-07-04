package com.livelink

import android.app.AlertDialog
import android.content.DialogInterface
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
import androidx.activity.viewModels
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.livelink.databinding.ActivityMainBinding
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import coil.load
import com.livelink.data.model.UserData
import com.livelink.data.model.ProfileVisitor
import com.livelink.databinding.PopupProfileBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.overviewFragment -> {
                    navController.navigate(R.id.overviewFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.channelsFragment -> {
                    navController.navigate(R.id.channelsFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.editProfileFragment -> {
                    navController.navigate(R.id.editProfileFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                else -> false
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.overviewFragment -> {
                    supportActionBar?.title = "Übersicht"
                    supportActionBar?.show()
                }

                R.id.channelsFragment -> {
                    supportActionBar?.title = "Channels"
                    supportActionBar?.show()
                }

                R.id.editProfileFragment -> {
                    supportActionBar?.title = "Profil bearbeiten"
                    supportActionBar?.show()
                }

                R.id.channelFragment -> {
                    supportActionBar?.title = viewModel.currentChannel.value?.channelID
                    supportActionBar?.show()
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

            val lockInfo = user.lockInfo
            if (lockInfo != null) {
                val expirationTimestamp = lockInfo.expirationTimestamp
                // Aktuelle Zeit
                val currentTimeMillis = System.currentTimeMillis()

                if (expirationTimestamp == -1L || currentTimeMillis < expirationTimestamp) {
                    Log.d("Lock", "Nutzer ist noch gesperrt")
                    isLocked(user)
                } else {
                    Log.d("Lock", "Nutzer ist nicht mehr gesperrt.")
                    viewModel.unlockUser(
                        user.username,
                        true
                    ) // Funktion zum Entfernen der Sperre aufrufen
                }
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

    fun isLocked(userData: UserData) {
        viewModel.auth.signOut()
        findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.loginFragment)
        val expirationTimestamp = userData.lockInfo!!.expirationTimestamp
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val expirationDate = if (userData.lockInfo.expirationTimestamp == -1L) "permanent" else "bis zum " + dateFormat.format(Date(expirationTimestamp))
        val userLockReason = userData.lockInfo.reason
        val builder = AlertDialog.Builder(this)
        val lockText = "Dein Account wurde von ${userData.lockInfo.lockedBy} <b>$expirationDate</b> gesperrt und wird im Laufe des darauffolgenden Tages wieder entsperrt.\n<b>Begründung:</b><br>${userLockReason}"
        builder.setTitle("Account gesperrt")
        builder.setMessage(fromHtml(lockText))
        builder.setPositiveButton("OK") { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss()
        }

        // AlertDialog anzeigen
        val dialog = builder.create()
        dialog.show()
    }

    // Funktion zum anzeigen des Profils als Popup
    private fun showProfilePopup(userData: UserData) {
        // Wir inflaten und binden das Layout (popup_profile.xml)
        val popupBinding = PopupProfileBinding.inflate(LayoutInflater.from(this))
        val popupView = popupBinding.root

        // Margins in dp
        val marginInDp = 32
        val scale = resources.displayMetrics.density
        val marginInPx = (marginInDp * scale + 0.5f).toInt()

        // Berechne die Breite des Popup-Fensters mit Berücksichtigung der Margins
        val width = resources.displayMetrics.widthPixels - 2 * marginInPx

        // Legen die Attribute des Popups fest
        val popupWindow = PopupWindow(
            popupView,
            width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Insbesondere die Position auf dem Bildschirm. In dem Fall mittig
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

        popupBinding.textViewStatus.text = getUserStatus(userData.status)

        if (userData.name.isNotEmpty()) {
            popupBinding.textViewName.isVisible = true
            popupBinding.textViewName.text = userData.name
        } else {
            popupBinding.textViewName.isVisible = false
        }

        val gender = userData.gender
        if (gender.isNotEmpty() && gender != "Keine Angabe") {
            popupBinding.textViewGender.isVisible = true
            popupBinding.textViewGender.text = gender
        } else {
            popupBinding.textViewGender.isVisible = false
        }

        val age = userData.age
        if (age.isNotEmpty() && age != "0") {
            popupBinding.textViewAge.isVisible = true
            popupBinding.textViewAge.text = age
        } else {
            popupBinding.textViewAge.isVisible = false
        }

        if (userData.birthday.isNotEmpty()) {
            popupBinding.textViewBirthday.isVisible = true
            popupBinding.textViewBirthday.text = userData.birthday
        } else {
            popupBinding.textViewBirthday.isVisible = false
        }

        val relationshipStatus = userData.relationshipStatus
        if (relationshipStatus.isNotEmpty() && relationshipStatus != "Keine Angabe") {
            popupBinding.textViewRelationshipStatus.isVisible = true
            popupBinding.textViewRelationshipStatus.text = relationshipStatus
        } else {
            popupBinding.textViewRelationshipStatus.isVisible = true
        }

        if (userData.zipCode.isNotEmpty()) {
            popupBinding.textViewZip.isVisible = true
            popupBinding.textViewZip.text = userData.zipCode
        } else {
            popupBinding.textViewZip.isVisible = false
        }

        val country = userData.country
        if (country.isNotEmpty() && country != "Keine Angabe") {
            popupBinding.textViewCountry.isVisible = true
            popupBinding.textViewCountry.text = country
        } else {
            popupBinding.textViewCountry.isVisible = false
        }

        if (userData.state.isNotEmpty()) {
            popupBinding.textViewState.isVisible = true
            popupBinding.textViewState.text = userData.state
        } else {
            popupBinding.textViewState.isVisible = false
        }

        if (userData.city.isNotEmpty()) {
            popupBinding.textViewCity.isVisible = true
            popupBinding.textViewCity.text = userData.city
        } else {
            popupBinding.textViewCity.isVisible = false
        }
    }

    // Erstellen des Optionsmenüs in der Action Bar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    // Anpassen der zurück Navigation
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
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

