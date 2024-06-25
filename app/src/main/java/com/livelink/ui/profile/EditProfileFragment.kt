package com.livelink.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import coil.load
import com.livelink.R
import com.livelink.SharedViewModel
import com.livelink.databinding.FragmentEditprofileBinding

class EditProfileFragment : Fragment() {

    private lateinit var binding: FragmentEditprofileBinding
    private val viewModel: SharedViewModel by activityViewModels()

    private val getContent =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                viewModel.uploadProfilePicture(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditprofileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.userData.observe(viewLifecycleOwner) { user ->
            binding.ProfileImageView.load(user.profilePicURL) {
                placeholder(R.drawable.placeholder_profilepic)
                error(R.drawable.placeholder_profilepic)
            }

            binding.EditNameEditText.setText(user.name)
            binding.EditAgeEditText.setText(user.age.toString())
            binding.EditBirthdayEditText.setText(user.birthday)
            binding.EditGenderEditText.setText(user.gender)
            binding.EditRelationshipStatusEditText.setText(user.relationshipStatus)

            binding.ProfileImageView.setOnClickListener {
                getContent.launch("image/*")
            }


        }
    }
}