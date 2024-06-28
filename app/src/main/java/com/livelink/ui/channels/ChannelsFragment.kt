package com.livelink.ui.channels

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
import com.livelink.adapter.ChannelsAdapter
import com.livelink.databinding.FragmentChannelsBinding

class ChannelsFragment : Fragment() {

    private lateinit var binding: FragmentChannelsBinding
    private val viewModel: SharedViewModel by activityViewModels()

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

        viewModel.channels.observe(viewLifecycleOwner) { channelList ->
            // Map erstellen, um Channels nach Kategorien zu gruppieren
            val groupedChannels = channelList.groupBy { it.category }
            // Liste erstellen aus Pair<String(Kategorie), List<Channel>>(Channels)
            val groupedChannelList = groupedChannels.toList()
            // Adapter mit der Liste erstellen
            val adapter = ChannelsAdapter(groupedChannelList) {
                viewModel.joinChannel(it)
                findNavController().navigate(R.id.channelFragment)
            }
            binding.recyclerViewChannel.adapter = adapter
        }
    }

}