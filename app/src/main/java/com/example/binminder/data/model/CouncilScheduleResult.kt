package com.example.binminder.data.model

import java.time.DayOfWeek

/**
 * Result model representing an auto-detected UK council waste collection timetable.
 * 
 * Includes the local council name, admin district, primary collection day, and
 * pre-filled bin setups based on postcode lookup.
 */
data class CouncilScheduleResult(
    val postcode: String,
    val councilName: String,
    val adminDistrict: String,
    val primaryCollectionDay: DayOfWeek,
    val binSetups: List<OnboardingBinSetup>
)
