package com.livelink.ui.register

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.livelink.R
import com.livelink.SharedViewModel
import com.livelink.databinding.FragmentRegisterBinding

// Fragment für das Registrierungsformular
class RegisterFragment : Fragment() {
    private lateinit var binding: FragmentRegisterBinding
    private val viewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Booleans die repräsentieren ob die jeweiligen Werte okay (true) sind oder nicht (false)
        var isUsernameOk = false
        var isEmailOk = false
        var isPasswordOk = false
        var isPasswordAgainOk = false

        // Beim Klick auf den Loginlink ..
        binding.RegisterToLoginLink.setOnClickListener {
            // .. navigieren wir zum... TROMMELWIRBEL... LOGIN!
            findNavController().navigate(R.id.loginFragment)
            Log.d("Fragment", "Wir navigieren zum LoginFragment")
        }

        // Wir beobachten live was als Username eingegeben wird
        binding.RegisterUsernameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Input ist der aktuell eingegebene Username
                val input = s.toString()
                // Wenn keiner eingeben ist ..
                if (input.isBlank()) {
                    // .. geben wir eine Fehlermeldung aus ..
                    binding.RegisterUsernameInputLayout.error = "Username darf nicht leer sein."
                    // .. und setzen den Wert auf false, also dass der Username nicht okay ist..
                    isUsernameOk = false
                    // Wenn der Username nicht den Vorgaben entspricht (Nur Buchstaben, Zahlen und Leerzeichen)
                } else if (!input.matches(Regex("^(?! )[a-zA-Z0-9]+(?: [a-zA-Z0-9]+)*(?! )\$"))) {
                    // geben wir eine entsprechende Fehlermeldung aus
                    binding.RegisterUsernameInputLayout.error =
                        "Nur Buchstaben, Zahlen und einzelne Leerzeichen INNERHALB des Usernamens."
                    // und setzen den Wert wieder auf false
                    isUsernameOk = false
                // Ansonsten ist der Username formell okay
                } else {
                    // wir senden den eingegebenen Usernamen dann an eine Funktion, die prüft, ob der Username bereits
                    // vergeben ist. Wir erhalten über einen Callback die Info, ob er vergeben ist oder nicht (Boolean)
                    viewModel.isUsernameTaken(input) { isTaken ->
                        // wenn er bereits vergeben ist..
                        if (isTaken) {
                            // .. geben wir wieder eine Fehlermeldung aus ..
                            binding.RegisterUsernameInputLayout.error = "Username bereits vergeben."
                            // .. und setzen den Wert auf false
                            isUsernameOk = false
                        // Ansonsten (wenn er NICHT vergeben ist)..
                        } else {
                            // Setzen wir die Fehlermeldungen zurück
                            binding.RegisterUsernameInputLayout.error = null
                            // und den Wert auf true, also dass alles roger in kambodscha ist
                            isUsernameOk = true
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Wir beobachten live die Eingabe der Emailadresse..
        binding.RegisterEmailEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // input ist die aktuell eingegebene Emailadresse
                val input = s.toString().trim()
                // Wenn die Eingabe leer ist..
                if (input.isBlank()) {
                    // .. setzen wir eine entsprechende Fehlermeldung ..
                    binding.RegisterEmailInputLayout.error =
                        "Die E-Mailadresse darf nicht leer sein."
                    // .. und den Wert auf false ..
                    isEmailOk = false
                // wenn die Eingabe nicht dem Muster entspricht (also alias@domain.endung)..
                } else if (!input.matches(Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"))) {
                    // setzen wir wieder eine Fehlermeldung ..
                    binding.RegisterEmailInputLayout.error = "Keine gültige E-Mailadresse"
                    // .. und den Wert auf false ..
                    isEmailOk = false
                // ansonsten ist die Email formell okay
                } else {
                    // wir löschen die Fehlermeldungen
                    binding.RegisterEmailInputLayout.error = null
                    // und setzen den Wert auf true
                    isEmailOk = true
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Wir beobachten live die Eingabe des Passwortes..
        binding.RegisterPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // input = aktuelle Eingabe
                val input = s.toString().trim()
                // Wenn das eingebene Passwort weniger als 6 Zeichen hat..
                if (input.length < 6) {
                    // geben wir eine Fehlermeldung aus
                    binding.RegisterPasswordInputLayout.error =
                        "Das Passwort muss mindestens 6 Zeichen lang sein."
                    // und setzen den Wert auf false
                    isPasswordOk = false
                // ansonsten ist das Passwort formell okay
                } else {
                    // und wir löschen die Fehlermeldung
                    binding.RegisterPasswordInputLayout.error = null
                    // und setzen den wert auf true
                    isPasswordOk = true
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Wir beobachten live die Eingabe des Passwort wiederholen Feldes..
        binding.RegisterPasswordWDHEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // input = aktuelle Eingabe
                val input = s.toString().trim()
                // wenn das eingebene (wiederholende) Passwort nicht dem passwort feld entspricht, ergo
                // nicht beide Passworteingaben gleich sind..
                if (input != binding.RegisterPasswordEditText.text.toString()) {
                    // setzen wir eine Fehlermeldung
                    binding.RegisterPasswordWDHInputLayout.error =
                        "Die Passwörter stimmen nicht überein."
                    // und den Wert auf false
                    isPasswordAgainOk = false
                // Ansonsten ist das Passwort okay (beide eingebenden passwörter stimmen überein)
                } else {
                    // löschen die Fehlermeldung
                    binding.RegisterPasswordWDHInputLayout.error = null
                    // sagen dass das Passwort okay ist
                    isPasswordAgainOk = true
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Beim Klick auf den Registrieren Button..
        binding.RegisterButton.setOnClickListener {
            // Überprüfen wir, ob alle formellen Voraussetzungen erfüllt sind. Wenn nicht..
            if (!isUsernameOk || !isEmailOk || !isPasswordOk || !isPasswordAgainOk) {
                // geben wir eine Fehlermeldung aus und brechen die weitere Codeausführung ab
                Toast.makeText(requireContext(), "Prüfe deine Eingaben.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

                // Wir holen uns die ganzen Werte aus den Eingabefeldern
                val username = binding.RegisterUsernameEditText.text.toString().trim()
                val email = binding.RegisterEmailEditText.text.toString().trim()
                val password = binding.RegisterPasswordEditText.text.toString().trim()

                // Und rufen mit den Werten die Registrierungsfunktion auf. Diese liefert über einen Callback
                // die Antwort ob die Registrierung erfolgreich war (Boolean)
                viewModel.register(username, email, password) { success ->
                    // Wenn die Registrierung erfolgreich war..
                    if (success) {
                        // laden wir die Benutzerumgebung (zb UserData)
                        viewModel.setupUserEnv()
                        // Und geben eine Erfolgsmeldung aus.
                        Toast.makeText(
                            requireContext(),
                            "Registrierung erfolgreich.",
                            Toast.LENGTH_LONG
                        ).show()
                        // Außerdem navigieren wir zur Hauptseite der App
                        findNavController().navigate(R.id.overviewFragment)
                    // Wenn die Registrierung NICHT erfolgreich war..
                    } else {
                        // Geben wir eine Fehlermeldung aus
                        Toast.makeText(
                            requireContext(),
                            "Fehler bei der Registrierung.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }