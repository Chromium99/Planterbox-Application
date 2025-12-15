package com.example.planterbox.net

data class ProcessResponse(
    val status: String?,
    val message: String?,
    val data: ProcessData?
)

data class ProcessData(
    val species_name: String?,
    val diagnostic: Diagnostic?
)

data class Diagnostic(
    val validated_species: String?,
    val visible_symptoms: List<String>?,
    val likely_causes: List<String>?,
    val care_recommendations: String?,
    val urgency_level: String?,
    val error: String?,
    val raw_output: String?
)
