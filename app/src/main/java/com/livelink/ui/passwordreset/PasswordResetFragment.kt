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

// Fragment zum zurücksetzen des Passworts
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

        // Wenn der Reset Button angeklickt wird...
        binding.PasswordResetButton.setOnClickListener {
            // Holen wir uns den Usernamen aus dem Eingabefeld
            val username = binding.PasswordUsernameEditText.text.toString()
            // Anhand des Usernamens versuchen wir uns die Emailadresse zu holen, da diese für das Passwort
            // zurücksetzen mit Firebase Auth benötigt wird
            viewModel.getMailFromUsername(username) { email ->
                // Wenn eine Email zum eingegebenen Nutzernamen gefunden wurde..
                if (email != null) {
                    // Splitten wir die Emailadresse in die Teile vor dem @ (email alias) und nach dem @ (die Domain)
                    val parts = email.split("@")
                    // Das ist der Emailalias (der Teil vor dem @)
                    val localPart = parts[0]
                    // Das ist die Domain (der Teil nach dem @)
                    val domain = parts[1]

                    // Wir erstellen eine Map, bestehend aus dem Index und dem jeweiligen Buchstaben des Email Alias
                    // Bedeutet aus dem alias domi wird eine map mit 0 = d, 1 = o, 2 = m, 3 = i
                    val censoredLocalPart = localPart.mapIndexed { index, c ->
                        // und wir ersetzen alle Buchstaben des Emailaliases durch Sternchen, außer den ersten und letzten Buchstaben
                        if (index != 0 && index != localPart.lastIndex) '*' else c
                        // und fügen das ganze dann wieder zu einem String zusammen, ergo dem zensierten Emailalias (d**i)
                    }.joinToString("")

                    // Wir fügen nun die Email zusammen, bestehend aus dem zensierten Alias, dem @ und der Originalen Domain
                    val formattedMail = "$censoredLocalPart@$domain"
                    // Nun senden wir eine Email raus um das Passwort neuzusetzen, dafür übergeben wir die GEFUNDENE
                    // Emailadresse - nicht die zensierte
                    viewModel.auth.sendPasswordResetEmail(email)
                    // Der User bekommt angezeigt, dass die Passwort Email gesendet wurde. Außerdem kriegt er die
                    // ZENSIERTE Emailadresse angezeigt (Datenschutzgründe, sonst könnten Nutzer darüber fremde Emailadressen
                    // auslesen)
                    Toast.makeText(requireContext(), "Passwort E-Mail gesendet an $formattedMail", Toast.LENGTH_LONG).show()
                    // Anschließend navigieren wir zum login
                    findNavController().navigate(R.id.loginFragment)
                    // Wenn keine Email zum eingegeben Nutzer gefunden wurde..
                } else {
                    // .. zeigen wir eine Fehlermeldung an
                    Toast.makeText(requireContext(), "Username nicht gefunden.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}