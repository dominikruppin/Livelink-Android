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
        var isUsernameOk = false
        var isEmailOk = false
        var isPasswordOk = false
        var isPasswordAgainOk = false

        binding.RegisterToLoginLink.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
            Log.d("Fragment", "Wir navigieren zum LoginFragment")
        }


        binding.RegisterUsernameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                if (input.isBlank()) {
                    binding.RegisterUsernameInputLayout.error = "Username darf nicht leer sein."
                    isUsernameOk = false
                } else if (!input.matches(Regex("^(?! )[a-zA-Z0-9]+(?: [a-zA-Z0-9]+)*(?! )\$"))) {
                    binding.RegisterUsernameInputLayout.error =
                        "Nur Buchstaben, Zahlen und einzelne Leerzeichen INNERHALB des Usernamens."
                    isUsernameOk = false
                } else {
                    viewModel.isUsernameTaken(input) { isTaken ->
                        if (isTaken) {
                            binding.RegisterUsernameInputLayout.error = "Username bereits vergeben."
                            isUsernameOk = false
                        } else {
                            binding.RegisterUsernameInputLayout.error = null
                            isUsernameOk = true
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.RegisterEmailEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().trim()
                if (input.isBlank()) {
                    binding.RegisterEmailInputLayout.error =
                        "Die E-Mailadresse darf nicht leer sein."
                    isEmailOk = false
                } else if (!input.matches(Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"))) {
                    binding.RegisterEmailInputLayout.error = "Keine gültige E-Mailadresse"
                    isEmailOk = false
                } else {
                    binding.RegisterEmailInputLayout.error = null
                    isEmailOk = true
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.RegisterPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().trim()
                if (input.length < 6) {
                    binding.RegisterPasswordInputLayout.error =
                        "Das Passwort muss mindestens 6 Zeichen lang sein."
                    isPasswordOk = false
                } else {
                    binding.RegisterPasswordInputLayout.error = null
                    isPasswordOk = true
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.RegisterPasswordWDHEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().trim()
                if (input != binding.RegisterPasswordEditText.text.toString()) {
                    binding.RegisterPasswordWDHInputLayout.error =
                        "Die Passwörter stimmen nicht überein."
                    isPasswordAgainOk = false
                } else {
                    binding.RegisterPasswordWDHInputLayout.error = null
                    isPasswordAgainOk = true
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.RegisterButton.setOnClickListener {
            if (!isUsernameOk || !isEmailOk || !isPasswordOk || !isPasswordAgainOk) {
                Toast.makeText(requireContext(), "Prüfe deine Eingaben.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
                val username = binding.RegisterUsernameEditText.text.toString().trim()
                val email = binding.RegisterEmailEditText.text.toString().trim()
                val password = binding.RegisterPasswordEditText.text.toString().trim()

                viewModel.register(username, email, password) { success ->
                    if (success) {
                        Toast.makeText(
                            requireContext(),
                            "Registrierung erfolgreich.",
                            Toast.LENGTH_LONG
                        ).show()
                        findNavController().navigate(R.id.loginFragment)
                    } else {
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
}