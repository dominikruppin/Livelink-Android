package com.livelink.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import android.app.DatePickerDialog
import android.util.Log
import android.widget.DatePicker
import java.util.*
import coil.load
import com.livelink.R
import com.livelink.SharedViewModel
import com.livelink.databinding.FragmentEditprofileBinding
import java.text.SimpleDateFormat

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

            val genderOptions = resources.getStringArray(R.array.gender_options)
            if (user.gender.isNotEmpty()) {
                val defaultIndex = genderOptions.indexOf(user.gender)
                binding.EditGenderSpinner.setSelection(defaultIndex)
                // binding.EditGenderSpinner.isEnabled = false
            } else {
                binding.EditGenderSpinner.isEnabled = true
            }

            binding.ProfileImageView.setOnClickListener {
                getContent.launch("image/*")
            }

            binding.EditBirthdayEditText.setOnClickListener {
                Log.d("Profil", "Birthday clicked")
                showDatePickerDialog()
            }




        }
    }

    private fun showDatePickerDialog() {
        Log.d("Profil", "DatePicker aufgerufen")
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val existingDate = binding.EditBirthdayEditText.text?.toString()
        val dateParts = existingDate?.split(".")
        val existingYear = dateParts?.get(2)?.toIntOrNull()
        val existingMonth = (dateParts?.get(1)?.toIntOrNull() ?: month) - 1
        val existingDay = dateParts?.get(0)?.toIntOrNull()

        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, monthOfYear, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(Calendar.YEAR, year)
            selectedCalendar.set(Calendar.MONTH, monthOfYear)
            selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
            val selectedDate = dateFormat.format(selectedCalendar.time)

            binding.EditBirthdayEditText.setText(selectedDate)
        }, existingYear ?: year, existingMonth, existingDay ?: day)

        datePickerDialog.datePicker.firstDayOfWeek = Calendar.MONDAY

        datePickerDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, "OK") { dialog, which ->
            val selectedYear = datePickerDialog.datePicker.year
            val selectedMonth = datePickerDialog.datePicker.month
            val selectedDayOfMonth = datePickerDialog.datePicker.dayOfMonth

            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(Calendar.YEAR, selectedYear)
            selectedCalendar.set(Calendar.MONTH, selectedMonth)
            selectedCalendar.set(Calendar.DAY_OF_MONTH, selectedDayOfMonth)

            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
            val selectedDate = dateFormat.format(selectedCalendar.time)

            binding.EditBirthdayEditText.setText(selectedDate)
        }

        datePickerDialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, "Abbrechen") { dialog, which ->
            datePickerDialog.dismiss()
        }
        datePickerDialog.show()
    }






}