package com.livelink.ui.overview

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.livelink.R
import com.livelink.SharedViewModel
import com.livelink.databinding.FragmentOverviewBinding

// Fragment für die Hauptseite der App (nur bei aktivem Login)
// Zeigt unter anderem eine Begrüßung an, eine Suche um Nutzer Profile zu suchen
// außerdem werden die letzten Channel und Profilbesucher angezeigt
class OverviewFragment : Fragment() {
    private lateinit var binding: FragmentOverviewBinding
    private val viewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Prüft ob ein aktueller User vorhanden ist, also ein Nutzer in der App aktuell eingeloggt ist
        // Falls kein Nutzer eingeloggt ist, navigieren wir zum Login
        if (viewModel.currentUser.value == null) {
            findNavController().navigate(R.id.loginFragment)
        }

        // Wir holen uns hiermit die UserDaten des eingeloggten Nutzers
        viewModel.userData.observe(viewLifecycleOwner) {
            // Und setzen eine Begrüßung, zusammen mit dem Usernamen des eingeloggten Nutzers
            binding.greetingTextView.text = getString(R.string.greeting, it.username)
        }



    }

}