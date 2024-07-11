package com.livelink.ui.overview

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.livelink.R
import com.livelink.SharedViewModel
import com.livelink.adapter.LastChannelsAdapter
import com.livelink.adapter.LastVisitorsAdapter
import com.livelink.adapter.SearchResultsAdapter
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
        // Fokus auf das Constraint Layout, damit die Suche nicht automatisch ausgewählt ist
        binding.overviewCL.requestFocus()

        // Prüft ob ein aktueller User vorhanden ist, also ein Nutzer in der App aktuell eingeloggt ist
        // Falls kein Nutzer eingeloggt ist, navigieren wir zum Login
        if (viewModel.currentUser.value == null) {
            findNavController().navigate(R.id.loginFragment)
        }

        // Wir beobachten die Eingabe der Usersuche
        binding.searchEditText.addTextChangedListener {
            // Die Eingabe
            val query = it.toString().trim()
            // Wenn Text eingegeben ist..
            if (query.isEmpty()) {
                binding.searchResultsRecyclerView.isVisible = false
            } else {
                viewModel.searchUsers(query)
                binding.searchResultsRecyclerView.isVisible = true
            }
        }

        // Laden der Suchergebnisse
        viewModel.searchResults.observe(viewLifecycleOwner) { searchResults ->
            Log.d("Search", "Searchresults: $searchResults")
            // Adapter erstellen mit den Suchergebnissen
            val searchResultsAdapter = SearchResultsAdapter(requireContext(), searchResults) { user ->
                // Implementiere die Logik für den Klick auf ein Suchergebnis, z.B. Profil öffnen
                viewModel.openProfile(user.username)
            }
            // Setze den Adapter für die RecyclerView, die die Suchergebnisse anzeigt
            binding.searchResultsRecyclerView.adapter = searchResultsAdapter
        }

        // Wir holen uns hiermit die UserDaten des eingeloggten Nutzers
        viewModel.userData.observe(viewLifecycleOwner) {
            // Und setzen eine Begrüßung, zusammen mit dem Usernamen des eingeloggten Nutzers
            binding.greetingTextView.text = getString(R.string.greeting, it.username)
            // Prüfen ob der User schon mal Channel besucht hat..
            if (it.lastChannels.isEmpty()) {
                // .. falls Nein, blenden wir den Hinweis ein..
                binding.noRecentChannelsTextView.visibility = View.VISIBLE
                // Falls ja, blenden wir den Hinweis aus..
            } else {
                binding.noRecentChannelsTextView.visibility = View.GONE
            }
            // SetOnClickListener für den Channelhinweis, leitet zur Channelauswahl
            binding.noRecentChannelsTextView.setOnClickListener {
                findNavController().navigate(R.id.channelsFragment)
            }
            // Prüfen ob der User bereits Profilbesucher hatte
            if (it.recentProfileVisitors.isEmpty()) {
                // Falls Nein, blenden wir den Hinweis ein...
                binding.noRecentVisitorsTextView.visibility = View.VISIBLE
            } else {
                binding.noRecentVisitorsTextView.visibility = View.GONE
            }
            // Liste der letzten Channel an den zugehörigen Adapter geben und Funktion zum anklicken eines
            // Channels definieren
            val lastChannelsAdapter = LastChannelsAdapter(it.lastChannels.reversed()) { channel ->
                // Wir joinen dem angeklickten Channel..
                viewModel.joinChannel(channel)
                // .. und wechseln dafür das Fragment
                findNavController().navigate(R.id.channelFragment)
            }
            // Liste der letzten Profilbesucher an den zugehörigen Adapter geben und Funktion zum anklicken
            // eines Users definieren
            val lastVisitorsAdapter = LastVisitorsAdapter(it.recentProfileVisitors.reversed()) { profileVistor ->
                viewModel.openProfile(profileVistor.username)
            }
            // Das übliche binden des Adapters an die recyclerview um die letzten Channel anzuzeigen
            binding.recentChannelsRecyclerView.adapter = lastChannelsAdapter
            binding.recentVisitorsRecyclerView.adapter = lastVisitorsAdapter
        }



    }

}