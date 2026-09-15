package com.example.binminder.data.model

/**
 * Result model representing an identified UK council from a postcode lookup.
 * 
 * Provides the council name and a web search URL so users can find their
 * actual collection schedule on their council's website.
 */
data class CouncilScheduleResult(
    val postcode: String,
    val councilName: String,
    val adminDistrict: String,
    val councilWebSearchUrl: String
)
