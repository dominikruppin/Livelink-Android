package com.livelink

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
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
import androidx.core.text.HtmlCompat
import com.livelink.databinding.ActivityMainBinding
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import coil.load
import com.livelink.data.model.UserData
import com.livelink.data.model.ProfileVisitor
import com.livelink.databinding.PopupProfileBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val viewModel: SharedViewModel by viewModels()
    private lateinit var popupBinding: PopupProfileBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Layout Binden
        binding = ActivityMainBinding.inflate(layoutInflater)
        popupBinding = PopupProfileBinding.inflate(LayoutInflater.from(this))
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

        // Wir legen fest, wann die Actionbar zu sehen ist und den jeweiligen angezeigten Titel
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
                Log.d("Lock", "Nutzer ist noch gesperrt")
                isLocked(user)
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

    // Funktion falls ein User gesperrt wird
    fun isLocked(userData: UserData) {
        // Den User ausloggen
        viewModel.auth.signOut()
        // UserData reseten
        viewModel.setupUserEnv()
        // navController holen
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        val currentDestination = navController.currentDestination
        // Wenn der Nutzer nicht im LoginFragment ist, werfen wir ihn dahin
        if (currentDestination?.id != R.id.loginFragment) {
            findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.loginFragment)
        }
        // Holen uns den Timestamp seiner Sperre
        val expirationTimestamp = userData.lockInfo!!.expirationTimestamp
        // Wandeln den in ein lesbares Format um
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        // Anzeige der Sperrdauer
        val expirationDate =
            if (userData.lockInfo.expirationTimestamp == -1L) "permanent gesperrt." else "bis zum " + dateFormat.format(
                Date(expirationTimestamp)
            ) + " gesperrt und wird im Laufe des darauffolgenden Tages wieder entsperrt."
        // Holen uns den Grund der Sperre
        val userLockReason = userData.lockInfo.reason
        // Und bauen nun einen AlertDialog zur Anzeige der Sperre
        val builder = AlertDialog.Builder(this)
        val lockText =
            "Dein Account wurde von ${userData.lockInfo.lockedBy} <b>$expirationDate</b>\n<b>Begründung:</b><br>${userLockReason}"
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
        val popupView = popupBinding.root

        // Margins in dp
        val marginInDp = 32
        val scale = resources.displayMetrics.density
        val marginInPx = (marginInDp * scale + 0.5f).toInt()

        // Berechne die Breite des Popup-Fensters mit Berücksichtigung der Margins
        val width = resources.displayMetrics.widthPixels - 2 * marginInPx
        val height = resources.displayMetrics.heightPixels - 2 * marginInPx

        // Legen die Attribute des Popups fest
        val popupWindow = PopupWindow(
            popupView,
            width,
            height,
            true
        )

        // Insbesondere die Position auf dem Bildschirm. In dem Fall mittig
        popupWindow.showAtLocation(binding.root, Gravity.CENTER, 0, 0)

        // Wenn man den Button zum schließen des Popups anklickt..
        popupBinding.btnClose.setOnClickListener {
            // .. schließen wir das Popup hiermit. Cool oder?
            popupWindow.dismiss()
        }

        // Anzeigen der Nutzersperre für User ab Status 4
        if (userData.lockInfo != null && viewModel.userData.value?.status!! > 4) {
            popupBinding.textViewUserlock.isVisible = true
            val expirationTimestamp = userData.lockInfo!!.expirationTimestamp
            // Wandeln den in ein lesbares Format um
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            // Anzeige der Sperrdauer
            val expirationDate =
                if (userData.lockInfo.expirationTimestamp == -1L) "permanent gesperrt." else "bis zum " + dateFormat.format(
                    Date(expirationTimestamp)
                ) + " gesperrt und wird im Laufe des darauffolgenden Tages wieder entsperrt."
            popupBinding.textViewUserlock.text = this.getString(
                R.string.profile_lock,
                userData.username,
                userData.lockInfo.lockedBy,
                expirationDate,
                userData.lockInfo.reason
            )
        } else {
            popupBinding.textViewUserlock.isVisible = false
        }

        // Wir füllen die verschiedenen Views mit den Profildaten
        popupBinding.textViewUsername.text = userData.username
        popupBinding.textViewStatus.text = getUserStatus(userData.status)
        popupBinding.textViewOnlineStatus.text = ""

        if (userData.profilePicURL.isNotEmpty()) {
            popupBinding.imageViewProfilePic.load(userData.profilePicURL)
        } else {
            popupBinding.imageViewProfilePic.setImageResource(R.drawable.placeholder_profilepic)
        }

        val appContext = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val (isOnline, channelID) = viewModel.checkUserOnlineInAnyChannel(userData.username)
            // Auf dem UI-Thread das Ergebnis anzeigen
            withContext(Dispatchers.Main) {
                // Den Online-Status anzeigen
                if (isOnline) {
                    // Online + Channel anzeigen
                    popupBinding.textViewOnlineStatus.text = appContext.getString(R.string.profile_online, channelID)
                    popupBinding.textViewOnlineStatus.setTextColor(Color.GREEN)
                } else {
                    // Offline anzeigen
                    popupBinding.textViewOnlineStatus.text = appContext.getString(R.string.profile_offline)
                    popupBinding.textViewOnlineStatus.setTextColor(Color.RED)
                }
            }
        }

        popupBinding.textViewReg.text = this.getString(
            R.string.profile_reg,
            userData.username,
            convertTimestampToDate(userData.regDate),
            convertTimestampToTime(userData.regDate)
        )

        if (userData.name.isNotEmpty()) {
            popupBinding.nameLL.isVisible = true
            popupBinding.textViewName.text = userData.name
        } else {
            popupBinding.nameLL.isVisible = false
        }

        val gender = userData.gender
        if (gender.isNotEmpty() && gender != "Keine Angabe") {
            popupBinding.genderLL.isVisible = true
            popupBinding.textViewGender.text = gender
        } else {
            popupBinding.genderLL.isVisible = false
        }

        val age = userData.age
        if (age.isNotEmpty() && age != "0") {
            popupBinding.ageLL.isVisible = true
            popupBinding.textViewAge.text = age
        } else {
            popupBinding.ageLL.isVisible = false
        }

        if (userData.birthday.isNotEmpty()) {
            popupBinding.birthdayLL.isVisible = true
            popupBinding.textViewBirthday.text = userData.birthday
        } else {
            popupBinding.birthdayLL.isVisible = false
        }

        val relationshipStatus = userData.relationshipStatus
        if (relationshipStatus.isNotEmpty() && relationshipStatus != "Keine Angabe") {
            popupBinding.relationshipLL.isVisible = true
            popupBinding.textViewRelationshipStatus.text = relationshipStatus
        } else {
            popupBinding.relationshipLL.isVisible = false
        }

        if (userData.zipCode.isNotEmpty()) {
            popupBinding.zipcodeLL.isVisible = true
            popupBinding.textViewZip.text = userData.zipCode
        } else {
            popupBinding.zipcodeLL.isVisible = false
        }

        val country = userData.country
        if (country.isNotEmpty() && country != "Keine Angabe") {
            popupBinding.countryLL.isVisible = true
            popupBinding.textViewCountry.text = country
        } else {
            popupBinding.countryLL.isVisible = false
        }

        if (userData.state.isNotEmpty()) {
            popupBinding.stateLL.isVisible = true
            popupBinding.textViewState.text = userData.state
        } else {
            popupBinding.stateLL.isVisible = false
        }

        if (userData.city.isNotEmpty()) {
            popupBinding.cityLL.isVisible = true
            popupBinding.textViewCity.text = userData.city
        } else {
            popupBinding.cityLL.isVisible = false
        }

        // Prüfen ob die Wildspace vorhanden ist
        if (userData.wildspace.isNotEmpty()) {
            // Mit dem Regex filtern wir die Bildurl. Die hat das Format [URL]
            val pattern = "\\[(.*?)]".toRegex()

            // Wir suchen uns die erste BildURL
            val matchResult = pattern.find(userData.wildspace)
            val imageUrl = matchResult?.groupValues?.getOrNull(1) ?: ""

            // Alle Bildurls außer der ersten durch leeren String ersetzen
            // So stellen wir sicher, dass in einer Wildspace nur ein Bild sein kann
            val replacedText = userData.wildspace.replace(pattern) { result ->
                if (result.range.first != matchResult?.range?.first) {
                    ""
                    // Hier setzen wir für die erste BildURL den Imagetag um
                    // das Bild dann im Profil anzuzeigen. Maximale Größe ist
                    // 100x100 Pixel
                } else {
                    Log.d("Profil", "URL: ${result.groupValues[1]}")
                    "<img src='${imageUrl}' style='max-width: 100px; max-height: 100px;' />"
                }
            }

            CoroutineScope(Dispatchers.Main).launch {
                val spanned = withContext(Dispatchers.IO) {
                    HtmlCompat.fromHtml(
                        replacedText,
                        HtmlCompat.FROM_HTML_MODE_COMPACT,
                        object : Html.ImageGetter {
                            override fun getDrawable(source: String): Drawable? {
                                return try {
                                    Log.d("Profil", "Trying to load image... $source")
                                    val url = URL(source)
                                    val connection = url.openConnection() as HttpURLConnection
                                    connection.doInput = true
                                    connection.connect()
                                    val input = connection.inputStream
                                    val drawable = Drawable.createFromStream(input, "srcName")
                                    if (drawable != null) {
                                        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                                    }
                                    input.close()
                                    Log.d("Profil", "Image loaded successfully")
                                    drawable
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Log.e("Profil", "Error loading image: $source", e)
                                    ColorDrawable(Color.TRANSPARENT)
                                }
                            }
                        },
                        null
                    )
                }

                popupBinding.wildspaceContent.text = SpannableStringBuilder.valueOf(spanned)

                popupBinding.wildspaceTitelLL.isVisible = true
                popupBinding.wildspaceContentLL.isVisible = true
            }
        } else {
            popupBinding.wildspaceTitelLL.isVisible = false
            popupBinding.wildspaceContentLL.isVisible = false
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

