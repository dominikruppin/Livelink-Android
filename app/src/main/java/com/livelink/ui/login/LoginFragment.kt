package com.livelink.ui.login

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
import com.livelink.databinding.FragmentLoginBinding

// Fragment um sich einzuloggen
class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private val viewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Boolean um zu schauen ob der eingebene Nutzername okay ist
        var isUsernameOk = false

        // Beim anklicken des Passwort vergessen Links..
        binding.ForgotPasswordLink.setOnClickListener {
            // .. navigieren wir zum Fragment um ein neues Passwort anzufordern
            findNavController().navigate(R.id.passwordResetFragment)
        }

        // Beim anklicken des Registrierungs Links..
        binding.LoginToRegisterLink.setOnClickListener {
            // .. navigieren wir zum Registrierungs Fragment
            findNavController().navigate(R.id.registerFragment)
        }

        // Wir prüfen live die eingegebenen Usernamen
        binding.LoginUsernameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Input = eingegebener Username
                val input = s.toString()
                // Prüfen ob es leer ist, falls ja..
                if (input.isBlank()) {
                    // .. geben wir eine Fehlermeldung aus
                    binding.LoginUsernameInputLayout.error = "Username darf nicht leer sein."
                    // Und sagen, NÖ, Username ist nicht ok!!!
                    isUsernameOk = false
                    // Wenn der eingegebene Nutzername nicht nur aus Buchstaben, Zahlen und Leerzeichen besteht...
                } else if (!input.matches(Regex("^(?! )[a-zA-Z0-9]+(?: [a-zA-Z0-9]+)*(?! )\$"))) {
                    // geben wir eine Fehlermeldung aus
                    binding.LoginUsernameInputLayout.error =
                        "Nur Buchstaben, Zahlen und einzelne Leerzeichen INNERHALB des Usernamens."
                    // Und sagen wieder, nö, Username ist nicht ok!
                    isUsernameOk = false
                    // Falls die obigen Bedingungen nicht greifen, ist der eingegebene Username formell okay..
                } else {
                    // .. und wir entfernen die Fehlermeldungen ..
                    binding.LoginUsernameInputLayout.error = null
                    // außerdem sagen wir, dass der Username okay ist
                    isUsernameOk = true
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Wenn auf den Loginbutton geklickt wird ..
        binding.LoginButton.setOnClickListener {
            Log.d("Login", "Loginbutton geklickt")
            // .. prüfen wir, ob der Username formell sein ok gekriegt hat..
            if (isUsernameOk) {
                // Dann holen wir uns den Usernamen aus dem Textfeld und entfernen vorne und hinten die Leerzeichen
                val username = binding.LoginUsernameEditText.text.toString().trim()
                // Wir holen uns das Passwort aus dem Textfeld
                val password = binding.LoginPasswordEditText.text.toString()

                // Anhand des Usernamens holen wir uns per Funktion die Email des Nutzers, da wir diese für die Firebase
                // Authentifizierung benötigen. (Firebase Auth unterstützt in dem Fall nur login mit email:pass
                viewModel.getMailFromUsername(username) { email ->
                    // Wir prüfen ob die übergebene Email nicht null ist (also ob eine emailadresse gefunden wurde)
                    if (email != null) {
                        // falls sie nicht null ist, versuchen wir den Login mit der Emailadresse und dem eingebenen Passwort
                        viewModel.login(email, password) { success ->
                            // Wir kriegen die Antwort und prüfen ob der Loginvorgang erfolgreich war. Falls ja..
                            if (success) {
                                // .. navigieren wir zum Overview Fragment falls keine Sperre besteht
                                viewModel.userData.observe(viewLifecycleOwner) { userData ->
                                    if (userData != null) {
                                        if (userData.lockInfo == null) {
                                            findNavController().navigate(R.id.overviewFragment)
                                        }
                                    }
                                }
                                // .. falls nein ..
                            } else {
                                // .. geben wir eine Fehlermeldung aus
                                Toast.makeText(
                                    requireContext(),
                                    "Login fehlgeschlagen.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        // Greift wenn keine Mail zum Nutzernamen gefunden wurde (die Antwort also null war)
                    } else {
                        // In dem Fall geben wir ebenfalls eine Fehlermeldung aus
                        Toast.makeText(
                            requireContext(),
                            "Benutzername nicht gefunden.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}