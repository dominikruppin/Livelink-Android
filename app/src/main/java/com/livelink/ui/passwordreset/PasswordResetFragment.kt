package com.livelink.ui.passwordreset

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.livelink.R
import com.livelink.SharedViewModel
import com.livelink.databinding.FragmentPasswordResetBinding

class PasswordResetFragment : Fragment() {

    private lateinit var binding: FragmentPasswordResetBinding
    private val viewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPasswordResetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.PasswordResetButton.setOnClickListener {
            val username = binding.PasswordUsernameEditText.text.toString()
            viewModel.getMailFromUsername(username) { email ->
                if (email != null) {
                    val parts = email.split("@")
                    val localPart = parts[0]
                    val domain = parts[1]

                    val censoredLocalPart = localPart.mapIndexed { index, c ->
                        if (index != 0 && index != localPart.lastIndex) '*' else c
                    }.joinToString("")

                    val formattedMail = "$censoredLocalPart@$domain"
                    viewModel.auth.sendPasswordResetEmail(email)
                    Toast.makeText(requireContext(), "Passwort E-Mail gesendet an $formattedMail", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.loginFragment)
                } else {
                    Toast.makeText(requireContext(), "Username nicht gefunden.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}