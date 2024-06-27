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
import android.widget.Toast
import coil.load
import com.livelink.R
import com.livelink.SharedViewModel
import com.livelink.data.model.ZipCodeInfos
import com.livelink.databinding.FragmentEditprofileBinding
import java.text.SimpleDateFormat
import java.util.*

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
        binding.apply {
            ProfileImageView.load(user.profilePicURL) {
                placeholder(R.drawable.placeholder_profilepic)
                error(R.drawable.placeholder_profilepic)
            }

            EditNameEditText.setText(user.name)
            EditAgeEditText.setText(user.age.toString())
            EditBirthdayEditText.setText(user.birthday)
            EditZipCodeEditText.setText(user.zipCode)

            val genderOptions = resources.getStringArray(R.array.gender_options)
            if (user.gender.isNotEmpty()) {
                val defaultIndex = genderOptions.indexOf(user.gender)
                EditGenderSpinner.setSelection(defaultIndex)
                EditGenderSpinner.isClickable = false
            } else {
                EditGenderSpinner.isClickable = true
            }

            val relationShipOptions =
                resources.getStringArray(R.array.relationship_status_options)
            val selectedRelationShopOption =
                relationShipOptions.indexOf(user.relationshipStatus)
            binding.EditRelationshipStatusSpinner.setSelection(selectedRelationShopOption)

            val countryOptions = resources.getStringArray(R.array.country_options)
            val selectedCountry = countryOptions.indexOf(user.country)
            binding.EditCountrySpinner.setSelection(selectedCountry)

            ProfileImageView.setOnClickListener {
                getContent.launch("image/*")
            }

            EditBirthdayEditText.setOnClickListener {
                showDatePickerDialog()
            }
        }
    }

    binding.SaveEditProfileButton.setOnClickListener {
        val name = binding.EditNameEditText.text.toString()
        val age = binding.EditAgeEditText.text.toString()
        val birthday = binding.EditBirthdayEditText.text.toString()
        val zipCode = binding.EditZipCodeEditText.text.toString()
        val relationshipStatus = binding.EditRelationshipStatusSpinner.selectedItem.toString()
        val country = binding.EditCountrySpinner.selectedItem.toString()

        val isValidName = name.matches(Regex("^[a-zA-ZäöüÄÖÜß ]{1,24}$")) || name.isEmpty()
        val isValidAge = age.isEmpty() || age.toInt() in 16..100
        val isValidBirthday = true // TODO: Implement birthday validation
        val isValidZipCodeFormat = when (country) {
            "Deutschland" -> zipCode.matches(Regex("^[0-9]{5}$"))
            "Österreich" -> zipCode.matches(Regex("^[0-9]{4}$"))
            "Schweiz" -> zipCode.matches(Regex("^[0-9]{4}$"))
            else -> false
        } || zipCode.isEmpty()

        binding.EditNameEditText.error = if (isValidName) null else "Ungültiger Name"
        binding.EditAgeEditText.error = if (isValidAge) null else "Ungültiges Alter"
        binding.EditZipCodeEditText.error =
            if (isValidZipCodeFormat) null else "Ungültige Postleitzahl"

        if (!isValidName || !isValidAge || !isValidBirthday || !isValidZipCodeFormat) {
            return@setOnClickListener
        }

        // Clear previous errors
        binding.EditZipCodeEditText.error = null

        // Only load zip code information if necessary
        if (zipCode.isNotEmpty() && country.isNotEmpty() && country != "Keine Angabe") {
            val countryCode = when (country) {
                "Deutschland" -> "de"
                "Österreich" -> "at"
                "Schweiz" -> "ch"
                else -> ""
            }
            viewModel.loadZipInfos(countryCode, zipCode)
        } else {
            // No zip code or country specified, update other user data only
            val updates = mutableMapOf<String, Any>()
            if (name != viewModel.userData.value?.name) {
                updates["name"] = name
            }
            if (age != viewModel.userData.value?.age) {
                updates["age"] = age
            }
            if (relationshipStatus != viewModel.userData.value?.relationshipStatus) {
                updates["relationshipStatus"] = relationshipStatus
            }
            if (birthday != viewModel.userData.value?.birthday) {
                updates["birthday"] = birthday
            }
            if (country != viewModel.userData.value?.country) {
                updates["country"] = country
            }
            val selectedGender = binding.EditGenderSpinner.selectedItem.toString()
            if (selectedGender != viewModel.userData.value?.gender) {
                updates["gender"] = selectedGender
            }

            // Save the user data
            saveUserData(updates)
        }
    }

    // Observer for zip code information
    viewModel.zipCodeInfos.observe(viewLifecycleOwner) { zipInfo ->
        if (zipInfo == null) {
            binding.EditZipCodeEditText.error = "Ungültige Postleitzahl"
            Toast.makeText(requireContext(), "Nicht gespeichert.", Toast.LENGTH_LONG).show()
        } else {
            val updates = mutableMapOf<String, Any>()
            // Process valid zip code info
            if (zipInfo.postalCode != viewModel.userData.value?.zipCode) {
                updates["zipCode"] = zipInfo.postalCode
                updates["city"] = zipInfo.name
                if (zipInfo.federalState != null) {
                    updates["state"] = zipInfo.federalState.name!!
                }
            }

            // Update other user data if necessary
            val name = binding.EditNameEditText.text.toString()
            val age = binding.EditAgeEditText.text.toString()
            val birthday = binding.EditBirthdayEditText.text.toString()
            val relationshipStatus = binding.EditRelationshipStatusSpinner.selectedItem.toString()
            val country = binding.EditCountrySpinner.selectedItem.toString()

            if (name != viewModel.userData.value?.name) {
                updates["name"] = name
            }
            if (age != viewModel.userData.value?.age) {
                updates["age"] = age
            }
            if (relationshipStatus != viewModel.userData.value?.relationshipStatus) {
                updates["relationshipStatus"] = relationshipStatus
            }
            if (birthday != viewModel.userData.value?.birthday) {
                updates["birthday"] = birthday
            }
            if (country != viewModel.userData.value?.country) {
                updates["country"] = country
            }
            val selectedGender = binding.EditGenderSpinner.selectedItem.toString()
            if (selectedGender != viewModel.userData.value?.gender) {
                updates["gender"] = selectedGender
            }

            // Save the user data
            saveUserData(updates)
        }
    }
}


    private fun saveUserData(updates: Map<String, Any>) {
        Log.d("UserData", "updates: $updates")
        if (updates.isNotEmpty()) {
            viewModel.updateUserData(updates) { success ->
                val message = if (success) "Erfolgreich gespeichert." else "Fehler beim Speichern."
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "Du hast nichts geändert.", Toast.LENGTH_LONG).show()
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

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, monthOfYear, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(Calendar.YEAR, year)
                selectedCalendar.set(Calendar.MONTH, monthOfYear)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
                val selectedDate = dateFormat.format(selectedCalendar.time)

                binding.EditBirthdayEditText.setText(selectedDate)
            },
            existingYear ?: year,
            existingMonth,
            existingDay ?: day
        )

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




