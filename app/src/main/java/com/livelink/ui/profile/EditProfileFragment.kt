package com.livelink.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import android.app.DatePickerDialog
import android.util.Log
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.view.isVisible
import coil.load
import com.livelink.R
import com.livelink.SharedViewModel
import com.livelink.databinding.FragmentEditprofileBinding
import java.text.SimpleDateFormat
import java.util.*

// Fragment um sein eigenes Profil zu bearbeiten (und auch ein Profilbild hochzuladen)
class EditProfileFragment : Fragment() {
    private lateinit var binding: FragmentEditprofileBinding
    private val viewModel: SharedViewModel by activityViewModels()

    // Funktion um die URI für eine Datei zu erhalten, in dem Fall dem ausgewählten Profilbild welches man hochladen möchte
    private val getContent =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            // Anhand der URI (wenn sie nicht null ist) dann den upload starten (in dem Fall Profilbild)
            uri?.let {
                viewModel.uploadProfilePicture(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditprofileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Wir holen uns die Daten des eingeloggten Users
        viewModel.userData.observe(viewLifecycleOwner) { user ->
            Log.d("UserData", "UserData Observer getriggert. $user")
            // und laden anhand der in den userdata gespeicherten url das Profilbild des Nutzers in die ImageView
            binding.ProfileImageView.load(user.profilePicURL) {
                // Platzhalterbild
                placeholder(R.drawable.placeholder_profilepic)
                // Falls das laden schief geht ebenfalls das Platzhalterbild
                error(R.drawable.placeholder_profilepic)
            }

            // Prüfen ob der User ein Land angegeben hat oder nicht
            // Falls kein Land angegeben wurde ..
            if (user.country.isEmpty() || user.country == "Keine Angabe") {
                // .. blenden wir das Feld für die Postleitzahl aus (da das Feld die Angabe des Landes braucht)
                binding.EditZipCodeInputLayout.isVisible = false
                // .. falls ein Land angegeben wurde ..
            } else {
                // .. blenden wir das Feld für die Postleitzahl ein
                binding.EditZipCodeInputLayout.isVisible = true
            }

            // Wir laden nun die aktuellen Profilangaben des Nutzers in die jeweiligen Texteingabefelder
            // Hat der Nutzer irgendwas davon nicht angegeben, wird Standardmäßig ein leerer String übergeben
            binding.EditNameEditText.setText(user.name)
            binding.EditAgeEditText.setText(user.age)

            if (user.birthday.isEmpty()) {
                binding.EditAgeEditText.isClickable = false
                binding.EditBirthdayEditText.isClickable = false
            } else {
                binding.EditAgeEditText.isClickable = true
                binding.EditBirthdayEditText.isClickable = true
            }

            binding.EditBirthdayEditText.setText(user.birthday)
            binding.EditZipCodeEditText.setText(user.zipCode)

            // Lädt die Geschlechts Auswahlmöglichkeiten
            val genderOptions = resources.getStringArray(R.array.gender_options)
            // Wenn der Nutzer bereits ein Geschlecht angegeben hat ..
            if (user.gender.isNotEmpty() && user.gender != "Keine Angabe") {
                // .. holen wir uns den Index des ausgewählten Geschlechts
                val defaultIndex = genderOptions.indexOf(user.gender)
                // Und setzen anhand des Indexes das aktuelle Geschlecht innerhalb des Spinners
                binding.EditGenderSpinner.setSelection(defaultIndex)
                // Außerdem deaktiviere wir, dass das Geschlecht geändert werden kann
                binding.EditGenderSpinner.isClickable = false
                // Wenn kein Geschlecht angegeben wurde ..
            } else {
                // Aktivieren wir, dass das Geschlecht ausgewählt werden kann
                binding.EditGenderSpinner.isClickable = true
            }

            // Lädt die Beziehungsstatus Auswahloptionen
            val relationShipOptions = resources.getStringArray(R.array.relationship_status_options)
            // Standardwert für ausgewählten Beziehungsstatus festlegen
            var selectedRelationShopOption = 0

            // Wir prüfen ob der Beziehungsstatus aktuell nicht leer ist. Falls er nicht leer ist..
            if (user.relationshipStatus.isNotEmpty()) {
                // Holen wir uns den Index der aktuell gespeicherten Auswahl und aktualisieren die ausgewählte Option
                selectedRelationShopOption = relationShipOptions.indexOf(user.relationshipStatus)
            }
            // Und setzen den Wert im Spinner
            binding.EditRelationshipStatusSpinner.setSelection(selectedRelationShopOption)

            // Lädt die Länder Auswahlmöglichkeiten
            val countryOptions = resources.getStringArray(R.array.country_options)
            // Wenn der Nutzer bereits ein Land hinterlegt hat..
            var selectedCountry = 0
            if (user.country.isNotEmpty()) {
                // Holen wir uns den Index seines aktuell hinterlegten Landes
                selectedCountry = countryOptions.indexOf(user.country)
            }
            // Und setzen den Wert im Spinner
            binding.EditCountrySpinner.setSelection(selectedCountry)

            // Wenn man auf das Profilbild klickt, öffnet sich der Dialog zum hochladen eines neuen Bildes
            binding.ProfileImageView.setOnClickListener {
                getContent.launch("image/*")
            }

            // Beim Klicken aus das Geburtstagsfeld öffnen wir den DatePicker
            binding.EditBirthdayEditText.setOnClickListener {
                if (user.birthday.isEmpty()) {
                    showDatePickerDialog()
                }
            }

            // Wir überwachen ob die Auswahl des Landes geändert wird
            binding.EditCountrySpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        // Beinhaltet das aktuell gewählte Land
                        val selectedCountry = parent.getItemAtPosition(position).toString()
                        // Prüfen ob ein Land angegeben ist und ob es nicht "Keine Angabe" ist
                        if (selectedCountry.isNotEmpty() && selectedCountry != "Keine Angabe") {
                            // falls ja, blenden wir das Feld zur Eingabe der Postleitzahl ein
                            binding.EditZipCodeInputLayout.isVisible = true
                        } else {
                            // falls nicht, blenden wir das Feld zur Eingabe der Postleitzahl aus
                            binding.EditZipCodeInputLayout.isVisible = false
                        }
                    }

                    // Wenn gar keine Option bei Land ausgewählt ist ..
                    override fun onNothingSelected(parent: AdapterView<*>) {
                        // .. blenden wir ebenfalls das Postleitzahlfeld aus
                        binding.EditZipCodeInputLayout.isVisible = false
                    }
                }
        }

        // Beim klicken auf den Speicher Button..
        binding.SaveEditProfileButton.setOnClickListener {
            // .. holen wir uns aus den Eingabefeldern die aktuellen Werte. Zuerst für den Namen..
            val name = binding.EditNameEditText.text.toString()
            // .. dann für das Alter ..
            val age = binding.EditAgeEditText.text.toString()
            // .. dann für den Geburtstag ..
            val birthday = binding.EditBirthdayEditText.text.toString()
            // .. dann für die Postleitzahl ..
            val zipCode = binding.EditZipCodeEditText.text.toString()
            // .. dann für den Beziehungsstatus ..
            val relationshipStatus = binding.EditRelationshipStatusSpinner.selectedItem.toString()
            // .. dann für das Land ..
            val country = binding.EditCountrySpinner.selectedItem.toString()
            // .. dann für das Geschlecht ..
            val selectedGender = binding.EditGenderSpinner.selectedItem.toString()

            // Wir prüfen ob der Name 1 bis 24 Zeichen hat, nicht leer ist und nur Buchstaben, Zahlen und Leerzeichen enthält
            // Ergebnis ist ein Boolean
            val isValidName = name.matches(Regex("^[a-zA-ZäöüÄÖÜß ]{1,24}$")) || name.isEmpty()
            // Wir prüfen ob das Alter leer ist oder innerhalb der Range von 16 bis 100 Jahren liegt
            // Ergebnis ist ein Boolean
            val isValidAge = age.isEmpty() || age.toInt() in 16..100
            // Ebenfalls ein Boolean, der angibt ob das angebene Geburtsdatum okay ist. Noch nicht implementiert.
            var isValidBirthday = false

            if (binding.EditBirthdayEditText.text.toString().isNotEmpty()) {
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                dateFormat.isLenient = false
                val date = dateFormat.parse(binding.EditBirthdayEditText.text.toString())
                // Heutiges Datum
                val today = Calendar.getInstance()
                // Minimum Alter: 16 Jahre
                val minAge = Calendar.getInstance()
                minAge.add(Calendar.YEAR, -16)
                // Maximum Alter: 100 Jahre
                val maxAge = Calendar.getInstance()
                maxAge.add(Calendar.YEAR, -100)
                // Prüfen, ob das Datum zwischen den gültigen Altersgrenzen liegt
                if (date.before(minAge.time) && date.after(maxAge.time)) {
                    isValidBirthday = true
                    binding.EditAgeEditText.isClickable = false
                    // Alter berechnen
                    val birthDate = Calendar.getInstance().apply { time = date }
                    var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
                    if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
                        age--
                    }

                    // Alter in EditAgeText setzen
                    binding.EditAgeEditText.setText(age.toString())
                } else {
                    isValidBirthday = false
                    binding.EditAgeEditText.isClickable = true
                }
            }

            // Boolean der angibt ob die Postleitzahl formell okay ist. Dazu prüfen wir, ob der Nutzer ein gültiges Land
            // ausgewählt hat. Falls ja, prüfen wir anhand des Landes ob die eingebene Postleitzahl nur aus Zahlen besteht
            val isValidZipCodeFormat = if (country.isNotEmpty() && country != "Keine Angabe") {
                when (country) {
                    // und der richtigen Länge des jeweiligen Landes. In dem Fall wird die Postleitzahl als formell okay angesehen.
                    "Deutschland" -> zipCode.matches(Regex("^[0-9]{5}$"))
                    "Österreich" -> zipCode.matches(Regex("^[0-9]{4}$"))
                    "Schweiz" -> zipCode.matches(Regex("^[0-9]{4}$"))
                    else -> false
                    // Außerdem auch, wenn gar keine Postleitzahl angegeben wurde.
                } || zipCode.isEmpty()
                // Wenn kein gültiges Land ausgewählt wurde..
            } else {
                // setzen wir den Wert trotzdem auf true, damit gespeichert werden kann (Dann ist die Postleitzahl sowieso
                // egal, da wir Land + PLZ brauchen zur API Abfrage
                true
            }

            // Wenn ein API Call (openPLZ API) gestartet wurde, wird das Ergbenis in einer MutableLiveData im ViewModel
            // gespeichert. Diese beobachten wir, so wissen wir, wann die Ergbenisse da sind und wann wir dann das klicken des
            // Speicherbuttons weiter verarbeiten können
            fun observeZipInfos() {
                viewModel.zipCodeInfos.observe(viewLifecycleOwner) { zipInfo ->
                    // Wir prüfen ob die erhaltene API Antwort null ist (also eine ungültige Postleitzahl)
                    if (zipInfo == null) {
                        // Falls die Antwort ungültig (null) ist, geben wir Fehlermeldungen aus. Einmal im Postleitzahl Feld
                        binding.EditZipCodeEditText.error = "Ungültige Postleitzahl"
                        // und einmal als Banner Message
                        Toast.makeText(requireContext(), "Nicht gespeichert.", Toast.LENGTH_LONG).show()
                        // Wenn das Ergebnis nicht null ist, wir also gültige Daten zur Postleitzahl erhalten haben..
                    } else {
                        // legen wir wieder eine Map an, mit den Profilangaben die gespeichert werden müssen. String ist wieder der Key
                        // der Datenbank bzw. des Profilfeldes und value der neue Wert
                        val updates = mutableMapOf<String, Any>()

                        // Wenn die Daten der API Antwort NICHT den bereits gespeicherten entsprechen
                        if (zipInfo.postalCode != viewModel.userData.value?.zipCode) {
                            // dann fügen wir die übergebene Postleitzahl
                            updates["zipCode"] = zipInfo.postalCode
                            // sowie die übergebene Stadt
                            updates["city"] = zipInfo.name
                            // und falls vorhanden
                            if (zipInfo.federalState != null) {
                                // das übergebene Bundesland mit in die Update Map ein
                                updates["state"] = zipInfo.federalState.name
                            }
                        }

                        // Ansonsten holen wir uns wieder die angegebenen Werte aus den Eingabefeldern
                        val name = binding.EditNameEditText.text.toString()
                        val age = binding.EditAgeEditText.text.toString()
                        val birthday = binding.EditBirthdayEditText.text.toString()
                        val relationshipStatus =
                            binding.EditRelationshipStatusSpinner.selectedItem.toString()
                        val country = binding.EditCountrySpinner.selectedItem.toString()

                        // Prüfen ob die angegeben Daten sich von den bereits gespeicherten Unterscheiden
                        // Falls ja, fügen wir sie ebenfalls mit in die Update Map ein
                        if (name != viewModel.userData.value?.name) {
                            updates["name"] = name
                        }
                        // Prüfen ob kein Geburtsdatum vorliegt
                        if (binding.EditBirthdayEditText.text.toString().isNotEmpty()) {
                            // Wenn kein Geburtsdatum vorliegt, prüfen ob das Alter geändert wurde
                            if (age != viewModel.userData.value?.age) {
                                updates["age"] = age
                            }
                        }
                        if (relationshipStatus != viewModel.userData.value?.relationshipStatus) {
                            updates["relationshipStatus"] = relationshipStatus
                        }
                        if (birthday != viewModel.userData.value?.birthday) {
                            updates["birthday"] = birthday
                        }
                        if (country != viewModel.userData.value?.country) {
                            updates["country"] = country
                        }
                        val selectedGender = binding.EditGenderSpinner.selectedItem.toString()
                        if (selectedGender != viewModel.userData.value?.gender) {
                            updates["gender"] = selectedGender
                        }
                        // Und übergeben die Update Map mit den geänderten Profilfeldern dann an die Profilspeichern Funktion
                        saveUserData(updates)
                        viewModel.zipCodeInfos.removeObservers(viewLifecycleOwner)
                    }
                }
            }


            // Wir prüfen nun, ob alle formellen Prüfungen okay sind, ansonsten geben wir Fehlermeldungen aus, je nachdem
            // welche Prüfung fehlschlägt
            // Hier wird geprüft, ob die formelle Namensprüfung okay ist
            binding.EditNameEditText.error = if (isValidName) null else "Ungültiger Name"
            // Hier wird geprüft, ob die formelle Altersprüfung okay ist
            binding.EditAgeEditText.error = if (isValidAge) null else "Ungültiges Alter"
            // Hier wird geprüft, ob die formelle Postleitzahlprüfung okay ist
            binding.EditZipCodeEditText.error =
                if (isValidZipCodeFormat) null else "Ungültige Postleitzahl"
            // Hier wird geprüft, ob die formelle Altersprüfung okay ist
            binding.EditBirthdayInputLayout.error = if (isValidBirthday) null else "Ungültiges Geburtsdatum. Mindestens 16 Jahre, maximal 100 Jahre."

            // Ist eine der formellen Prüfungen nicht okay, wird die weitere Codeausführung unterbrochen
            if (!isValidName || !isValidAge || !isValidBirthday || !isValidZipCodeFormat) {
                return@setOnClickListener
            }

            // Wenn eine Postleitzahl und ein gültiges Land ausgewählt wurde..
            // WICHTIG: HIER ERFOLGT EIN API CALL (openPLZ API)
            if (zipCode.isNotEmpty() && country.isNotEmpty() && country != "Keine Angabe") {
                // holen wir uns anhand des ausgewählten Landes das Landeskürzel
                val countryCode = when (country) {
                    "Deutschland" -> "de"
                    "Österreich" -> "at"
                    "Schweiz" -> "ch"
                    else -> ""
                }
                // Mit dem Landeskürzel und der angegebenen Postleitzahl, holen wir uns Infos zur Postleitzahl
                viewModel.loadZipInfos(countryCode, zipCode)
                observeZipInfos()
                // Wenn entweder keine Postleitzahl oder kein gültiges Land vorhanden ist..
                // WICHTIG: WENN ALSO KEIN API CALL ERFOLGT!!!
            } else {
                // erstellen wir eine Map mit den Profilangaben die geändert werden müssen. String ist dabei der Key des
                // Datenbank/Profilfeldes und der value der jeweilige Wert
                val updates = mutableMapOf<String, Any>()
                // Wenn die jeweilige Profilangabe nicht mit der bereits gespeicherten Profileingabe übereinstimmt ..
                //  // fügen wir den neuen Wert mit Key in die Update Map ein.

                // Wir prüfen wir ob sich der Name geändert hat
                if (name != viewModel.userData.value?.name) {
                    updates["name"] = name
                }
                // Wir prüfen ob sich das Alter geändert hat
                if (binding.EditBirthdayEditText.text.toString().isNotEmpty()) {
                    if (age != viewModel.userData.value?.age) {
                        updates["age"] = age
                    }
                }
                // Wir überprüfen ob sich der Beziehungsstatus geändert hat
                if (relationshipStatus != viewModel.userData.value?.relationshipStatus) {
                    updates["relationshipStatus"] = relationshipStatus
                }
                // Wir überprüfen ob sich der Geburtstag geändert hat
                if (birthday != viewModel.userData.value?.birthday) {
                    updates["birthday"] = birthday
                }
                // Wir überprüfen ob sich das Land geändert hat
                if (country != viewModel.userData.value?.country) {
                    updates["country"] = country
                }
                // Wir überprüfen ob sich das Geschlecht geändert hat
                if (selectedGender != viewModel.userData.value?.gender) {
                    updates["gender"] = selectedGender
                }
                if (zipCode != viewModel.userData.value?.zipCode) {
                    if (zipCode.isEmpty()) {
                        updates["zipCode"] = ""
                        updates["city"] = ""
                        updates["state"] = ""
                    } else {
                        updates["zipCode"] = zipCode
                    }
                }
                // Wir übergeben nun die Map mit den Updates an die Profilspeicherfunktion
                saveUserData(updates)
            }
        }
}


    // Funktion um die übergebenen Profilfelder (als Map) zu speichern
    private fun saveUserData(updates: Map<String, Any>) {
        Log.d("UserData", "updates: $updates")
        // Wir prüfen ob die übergebene NICHT Map leer ist
        if (updates.isNotEmpty()) {
            // Wir speichern die übergebenen Werte, dazu übergeben wir sie ans ViewModel und kriegen per Callback
            // Bescheid ob das speichern erfolgreich war
            // Wenn erfolgreich ..
            viewModel.updateUserData(updates) { success ->
                // geben wir eine Erfolgsmeldung aus
                val message = if (success) "Erfolgreich gespeichert." else "Fehler beim Speichern."
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
            // ist die Map leer, also wenn nichts geupdatet werden muss, da sich nichts geändert hat..
        } else {
            // Geben wir nur eine entsprechende Meldung aus
            Toast.makeText(requireContext(), "Du hast nichts geändert.", Toast.LENGTH_LONG).show()
        }
    }

    // Funktion zum aufrufen des Datepickers für das Geburtstagsfeld
    private fun showDatePickerDialog() {
        Log.d("Profil", "DatePicker aufgerufen")
        // Holen uns die Instanz des Kalenders und die jeweiligen Werte
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        // Wenn bereits ein Geburtstag im Eingabefeld ist, holen wir uns das
        val existingDate = binding.EditBirthdayEditText.text?.toString()

        // Das existierende Geburtsdatum splitten wir anhand der Punkte (Format: DD.mm.YYYY)
        val dateParts = existingDate?.split(".") ?: listOf(null, null, null)
        // in Jahr
        val existingYear = dateParts.getOrNull(2)?.toIntOrNull()
        // in Monat
        val existingMonth = (dateParts.getOrNull(1)?.toIntOrNull() ?: month) - 1
        // in Tag
        val existingDay = dateParts.getOrNull(0)?.toIntOrNull()

        // Dialog erstellen welcher den Picker anzeigt
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, monthOfYear, dayOfMonth ->
                // Wir setzen das ausgewählte Datum in den Kalender
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(Calendar.YEAR, year)
                selectedCalendar.set(Calendar.MONTH, monthOfYear)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
                val selectedDate = dateFormat.format(selectedCalendar.time)

                binding.EditBirthdayEditText.setText(selectedDate)
            },
            // Wenn bereits ein Geburtsdatum vorhanden ist, setzen wir den Kalender auf diese Werte
            existingYear ?: year,
            existingMonth,
            existingDay ?: day
        )

        // Wir setzen den ersten Tag der Woche auf Monat (wie in Deutschland üblich)
        datePickerDialog.datePicker.firstDayOfWeek = Calendar.MONDAY

        // Beim Klick auf okay wird das ausgewählte Datum ins Eingabefeld gesetzt. Dafür ..
        datePickerDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, "OK") { dialog, which ->
            val selectedYear = datePickerDialog.datePicker.year
            val selectedMonth = datePickerDialog.datePicker.month
            val selectedDayOfMonth = datePickerDialog.datePicker.dayOfMonth

            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(Calendar.YEAR, selectedYear)
            selectedCalendar.set(Calendar.MONTH, selectedMonth)
            selectedCalendar.set(Calendar.DAY_OF_MONTH, selectedDayOfMonth)

            // .. wandeln wir es vorher ins übliche dd.MM.yyyy Format (für Deutschland) um.
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
            val selectedDate = dateFormat.format(selectedCalendar.time)

            binding.EditBirthdayEditText.setText(selectedDate)
        }

        // Schließt den DatePicker ohne was weiteres zu unternehmen
        datePickerDialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, "Abbrechen") { dialog, which ->
            datePickerDialog.dismiss()
        }
        // Zeigt den DatePicker an, nachdem wir alle Attribute festgelegt haben
        datePickerDialog.show()
    }
}