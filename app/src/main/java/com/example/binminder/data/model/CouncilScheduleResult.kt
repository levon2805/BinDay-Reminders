package com.example.binminder.data.model

import java.time.DayOfWeek

/**
 * Result model representing an auto-detected UK council waste collection timetable.
 */
data class CouncilScheduleResult(
    val postcode: String,
    val councilName: String,
    val adminDistrict: String,
    val primaryCollectionDay: DayOfWeek,
    val binSetups: List<OnboardingBinSetup>
)
