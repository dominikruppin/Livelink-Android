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
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.livelink.R
import com.livelink.SharedViewModel
import com.livelink.databinding.FragmentLoginBinding

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

        var isUsernameOk = false

        binding.LoginToRegisterLink.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
            Log.d("Fragment", "Wir navigieren zum RegisterFragment")
        }

        binding.LoginUsernameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                if (input.isBlank()) {
                    binding.LoginUsernameInputLayout.error = "Username darf nicht leer sein."
                    isUsernameOk = false
                } else if (!input.matches(Regex("^(?! )[a-zA-Z0-9]+(?: [a-zA-Z0-9]+)*(?! )\$"))) {
                    binding.LoginUsernameInputLayout.error =
                        "Nur Buchstaben, Zahlen und einzelne Leerzeichen INNERHALB des Usernamens."
                    isUsernameOk = false
                } else {
                    binding.LoginUsernameInputLayout.error = null
                    isUsernameOk = true
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.LoginButton.setOnClickListener {
            Log.d("Login", "Loginbutton geklickt")
            if (isUsernameOk) {
                Log.d("Login", "Username OK, Starte Loginvorgang..")
                val username = binding.LoginUsernameEditText.text.toString().trim()
                Log.d("Login", "Username: $username")
                val password = binding.LoginPasswordEditText.text.toString()
                Log.d("Login", "Passwort: $password")

                viewModel.getMailFromUsername(username) { email ->
                    Log.d("Login", "Gefundene Email: $email")
                    if (email != null) {
                        viewModel.login(email, password) { success ->
                            if (success) {
                                Log.d("Login", "Login successLogin")
                                findNavController().navigate(R.id.nav_home)
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Login fehlgeschlagen.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
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