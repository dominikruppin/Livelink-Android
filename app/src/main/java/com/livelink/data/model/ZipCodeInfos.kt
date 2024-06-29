package com.livelink.data.model

// Datenklasse für die openPLZ API, zum abrufen der Postleitzahl Infos
// Name ist dabei der Städtename
// postalCode natürlich die Postleitzahl (welche man sowieso übergeben hat)
// Sowie das Bundesland als eigenes Objekt
data class ZipCodeInfos(
    val name: String,
    val postalCode: String,
    val federalState: FederalState?
)

