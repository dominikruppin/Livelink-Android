package com.livelink.ui.channels

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.livelink.R
import com.livelink.SharedViewModel
import com.livelink.adapter.ChannelsAdapter
import com.livelink.data.model.Channel
import com.livelink.databinding.FragmentChannelsBinding

// Fragment zum Anzeigen der Channelliste (Sortiert nach Kategorien)
class ChannelsFragment : Fragment() {

    private lateinit var binding: FragmentChannelsBinding
    private val viewModel: SharedViewModel by activityViewModels()
    private lateinit var adapter: ChannelsAdapter
    private var originalChannelList: List<Channel> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChannelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Den Adapter mit der Funktion zum betreten eines Channels noch mal
        // initialisieren
        adapter = ChannelsAdapter(emptyList()) {
            viewModel.joinChannel(it)
            findNavController().navigate(R.id.channelFragment)
        }
        // Adapter an die RV anbinden
        binding.recyclerViewChannel.adapter = adapter

        // Channelliste hier verarbeiten
        viewModel.channels.observe(viewLifecycleOwner) { channelList ->
            // Weisen die empfangene Liste der originalChannelList zu und übergeben
            // sie an die Updatefunktion. Außerdem behalten wir sie, damit wir sie
            // noch mal nutzen können
            originalChannelList = channelList
            updateRecyclerView(originalChannelList)
        }

        // Wir beobachten die Eingabe im Suchfeld
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Stellt die aktuelle Eingabe da
                val query = s.toString()
                // Wenn nichts in der Suche steht, laden wir die Originale
                // Liste, welche ALLE Channel enthält
                if (query.isEmpty()) {
                    updateRecyclerView(originalChannelList)
                // Wenn was im Suchfeld steht, filtern wir die Originale Liste
                } else {
                    val filteredList = originalChannelList.filter {
                        it.name.contains(query, ignoreCase = true)
                    }
                    // Und geben die gefilterte Liste in die Updatefunktion
                    updateRecyclerView(filteredList)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // Aus der Liste der Channel wieder die Channel nach Kategorien gruppieren
    private fun updateRecyclerView(channelList: List<Channel>) {
        // Erstellt eine Map<String, List<Channel>> wobei der String die Kategorie ist
        val groupedChannels = channelList.groupBy { it.category }
        // Aus der Map machen wir eine Liste
        val groupedChannelList = groupedChannels.toList()
        // Und updaten damit den Adapter
        adapter.updateDataset(groupedChannelList)
    }
}