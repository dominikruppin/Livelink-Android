package com.livelink.ui.overview

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        // Prüft ob ein aktueller User vorhanden ist, also ein Nutzer in der App aktuell eingeloggt ist
        // Falls kein Nutzer eingeloggt ist, navigieren wir zum Login
        if (viewModel.currentUser.value == null) {
            findNavController().navigate(R.id.loginFragment)
        }

        binding.searchEditText.addTextChangedListener {
            val query = it.toString().trim()
            viewModel.searchUsers(query)
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { searchResults ->
            Log.d("Search", "Searchresults: $searchResults")
            // Aktualisiere die RecyclerView-Adapter mit den Suchergebnissen
            val searchResultsAdapter = SearchResultsAdapter(searchResults) { user ->
                // Implementiere die Logik für den Klick auf ein Suchergebnis, z.B. Profil öffnen
                viewModel.openProfile(user.username)
            }
            // Setze den Adapter für die RecyclerView, die die Suchergebnisse anzeigt
            //binding.searchResultsRecyclerView.adapter = searchResultsAdapter
        }

        // Wir holen uns hiermit die UserDaten des eingeloggten Nutzers
        viewModel.userData.observe(viewLifecycleOwner) {
            // Und setzen eine Begrüßung, zusammen mit dem Usernamen des eingeloggten Nutzers
            binding.greetingTextView.text = getString(R.string.greeting, it.username)
            val lastChannelsAdapter = LastChannelsAdapter(it.lastChannels.reversed()) { channel ->
                // Wir joinen dem angeklickten Channel..
                viewModel.joinChannel(channel)
                // .. und wechseln dafür das Fragment
                findNavController().navigate(R.id.channelFragment)
            }
            val lastVisitorsAdapter = LastVisitorsAdapter(it.recentProfileVisitors.reversed()) { profileVistor ->
                viewModel.openProfile(profileVistor.username)
            }
            // Das übliche binden des Adapters an die recyclerview um die letzten Channel anzuzeigen
            binding.recentChannelsRecyclerView.adapter = lastChannelsAdapter
            binding.recentVisitorsRecyclerView.adapter = lastVisitorsAdapter
        }



    }

}