package com.example.binminder.data.repository

import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.CouncilScheduleResult
import com.example.binminder.data.model.OnboardingBinSetup
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.data.service.CouncilLookupService
import com.example.binminder.data.service.CouncilLookupServiceImpl
import com.example.binminder.data.service.PostcodeLookupDto
import java.time.DayOfWeek

interface CouncilLookupRepository {
    suspend fun lookupPostcode(postcodeOrQuery: String): Result<CouncilScheduleResult>
}

class CouncilLookupRepositoryImpl(
    private val service: CouncilLookupService = CouncilLookupServiceImpl()
) : CouncilLookupRepository {

    override suspend fun lookupPostcode(postcodeOrQuery: String): Result<CouncilScheduleResult> {
        val trimmed = postcodeOrQuery.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Postcode cannot be blank."))
        }

        val sanitizedPostcode = postcodeOrQuery.replace("\\s+".toRegex(), "").trim().uppercase()

        // Try API lookup via postcodes.io
        val dto = service.lookupPostcode(sanitizedPostcode)
        if (dto != null) {
            val schedule = resolveCouncilSchedule(dto)
            return Result.success(schedule)
        }

        // Fallback: Check if query matches known council names or keywords directly
        val directMatch = resolveDirectCouncilNameMatch(trimmed)
        if (directMatch != null) {
            return Result.success(directMatch)
        }

        return Result.failure(Exception("Could not auto-detect timetable for: $postcodeOrQuery"))
    }

    private fun resolveCouncilSchedule(dto: PostcodeLookupDto): CouncilScheduleResult {
        val collectionDay = resolveCollectionDay(dto.postcode)
        val admin = dto.adminDistrict.trim()
        val reg = dto.region?.trim() ?: ""

        // Normalize admin string for matching
        val lowerAdmin = admin.lowercase()

        return when {
            // -----------------------------------------------------------------
            // SPECIFIC HIGH-DENSITY COUNCILS & REGIONAL EXEMPLARS
            // -----------------------------------------------------------------
            lowerAdmin.contains("st albans") || lowerAdmin.contains("saint albans") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "St Albans City and District Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.BLACK, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Black wheelie bin for non-recyclable refuse."),
                    OnboardingBinSetup("Recycling", "Recycling", BinColor.BROWN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Brown wheelie bin for cans, plastics, and glass."),
                    OnboardingBinSetup("Paper & Cardboard", "Paper & Cardboard", BinColor.GREEN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Green wheelie bin or box for paper and cardboard."),
                    OnboardingBinSetup("Food Waste Caddy", "Food Waste Caddy", BinColor.GREEN, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Green food waste caddy."),
                    OnboardingBinSetup("Garden Waste", "Garden Waste", BinColor.GREEN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Green wheelie bin for garden waste.")
                )
            )

            lowerAdmin.contains("manchester") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Manchester City Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.GREY, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Grey bin for non-recyclable household waste."),
                    OnboardingBinSetup("Paper & Cardboard", "Paper & Cardboard", BinColor.BLUE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Blue bin for paper and card."),
                    OnboardingBinSetup("Glass, Cans & Plastics", "Glass, Cans & Plastics", BinColor.BROWN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Brown bin for glass, cans, and plastics."),
                    OnboardingBinSetup("Food & Garden Waste", "Food & Garden Waste", BinColor.GREEN, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Green bin for food leftovers and garden waste.")
                )
            )

            lowerAdmin.contains("stockport") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Stockport Metropolitan Borough Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.BLACK, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Black bin for general non-recyclable household waste."),
                    OnboardingBinSetup("Paper & Cardboard", "Paper & Cardboard", BinColor.BLUE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Blue bin for paper and cardboard."),
                    OnboardingBinSetup("Glass, Cans & Plastics", "Glass, Cans & Plastics", BinColor.BROWN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Brown bin for glass bottles, food tins, drink cans, and plastic bottles."),
                    OnboardingBinSetup("Food & Garden Waste", "Food & Garden Waste", BinColor.GREEN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Green bin for garden waste and food waste.")
                )
            )

            lowerAdmin.contains("westminster") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Westminster City Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "Household Rubbish", BinColor.BLACK, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Black bin for general waste."),
                    OnboardingBinSetup("Dry Recycling", "Mixed Recycling", BinColor.BLUE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Blue bin for mixed dry recycling."),
                    OnboardingBinSetup("Food Waste", "Food Waste Caddy", BinColor.BROWN, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Brown caddy for kitchen food waste.")
                )
            )

            lowerAdmin.contains("birmingham") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Birmingham City Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.GREY, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Grey bin for non-recyclable waste."),
                    OnboardingBinSetup("Recycling", "Dry Recycling", BinColor.BLUE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Blue bin for paper, cans, and plastics."),
                    OnboardingBinSetup("Garden Waste", "Garden Waste", BinColor.GREEN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Green bin for garden waste.")
                )
            )

            lowerAdmin.contains("leeds") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Leeds City Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.BLACK, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Black bin for household waste."),
                    OnboardingBinSetup("Recycling", "Dry Recycling", BinColor.GREEN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Green bin for paper, card, and plastics."),
                    OnboardingBinSetup("Garden Waste", "Garden Waste", BinColor.BROWN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Brown bin for garden waste.")
                )
            )

            lowerAdmin.contains("glasgow") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Glasgow City Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.GREEN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Green bin for general waste."),
                    OnboardingBinSetup("Recycling", "Dry Recycling", BinColor.BLUE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Blue bin for paper and plastic."),
                    OnboardingBinSetup("Glass Recycling", "Glass Recycling", BinColor.PURPLE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Purple bin for glass bottles and jars."),
                    OnboardingBinSetup("Food & Garden", "Food & Garden Waste", BinColor.BROWN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Brown bin for garden cuttings and food scraps.")
                )
            )

            lowerAdmin.contains("edinburgh") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "City of Edinburgh Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.GREY, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Grey bin for general refuse."),
                    OnboardingBinSetup("Recycling", "Packaging Recycling", BinColor.GREEN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Green bin for packaging and card."),
                    OnboardingBinSetup("Glass Recycling", "Glass Box", BinColor.BLUE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Blue box for glass containers."),
                    OnboardingBinSetup("Food Waste", "Food Waste Caddy", BinColor.BLACK, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Small caddy for kitchen food waste.")
                )
            )

            lowerAdmin.contains("bristol") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Bristol City Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.BLACK, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Black bin for household waste."),
                    OnboardingBinSetup("Recycling", "Recycling Box & Bags", BinColor.GREY, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Recycling box for glass, tins, paper."),
                    OnboardingBinSetup("Garden Waste", "Garden Waste", BinColor.BROWN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Brown bin for garden waste.")
                )
            )

            lowerAdmin.contains("sheffield") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Sheffield City Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.BLACK, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Black bin for general rubbish."),
                    OnboardingBinSetup("Paper Recycling", "Paper & Card Recycling", BinColor.BLUE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Blue bin for paper and card."),
                    OnboardingBinSetup("Glass & Plastics", "Glass & Cans Recycling", BinColor.BROWN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Brown bin for glass bottles, cans, and plastics.")
                )
            )

            lowerAdmin.contains("cornwall") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Cornwall Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.BLACK, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Black bin for non-recyclable rubbish."),
                    OnboardingBinSetup("Recycling", "Recycling Sacks", BinColor.RED, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Recycling bags for card, paper, and metal."),
                    OnboardingBinSetup("Food Waste", "Food Waste Caddy", BinColor.GREY, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Caddy for food scraps.")
                )
            )

            lowerAdmin.contains("liverpool") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Liverpool City Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.PURPLE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Purple bin for general household waste."),
                    OnboardingBinSetup("Recycling", "Dry Recycling", BinColor.BLUE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Blue bin for paper, card, and plastic bottles."),
                    OnboardingBinSetup("Garden Waste", "Garden Waste", BinColor.GREEN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Green bin for garden clippings.")
                )
            )

            lowerAdmin.contains("leicester") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Leicester City Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "Refuse Bin", BinColor.BLACK, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Black bin for household waste."),
                    OnboardingBinSetup("Recycling", "Recycling Bags", BinColor.YELLOW, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Orange/Yellow bags for household recycling.")
                )
            )

            lowerAdmin.contains("nottingham") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Nottingham City Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.GREEN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Green bin for non-recyclable waste."),
                    OnboardingBinSetup("Recycling", "Dry Recycling", BinColor.BROWN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Brown bin for paper, cans, and plastics.")
                )
            )

            lowerAdmin.contains("cardiff") || lowerAdmin.contains("caerdydd") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Cardiff Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.BLACK, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Black bin for general waste."),
                    OnboardingBinSetup("Recycling", "Dry Mixed Recycling", BinColor.GREEN, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Green bags for dry mixed recycling."),
                    OnboardingBinSetup("Food Waste", "Food Waste Caddy", BinColor.BROWN, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Brown caddy for food waste.")
                )
            )

            lowerAdmin.contains("belfast") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Belfast City Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.BLACK, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Black bin for general waste."),
                    OnboardingBinSetup("Recycling", "Dry Recycling", BinColor.BLUE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Blue bin for dry recycling."),
                    OnboardingBinSetup("Organic Waste", "Organic Waste", BinColor.BROWN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Brown bin for food scraps and garden waste.")
                )
            )

            lowerAdmin.contains("oxford") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Oxford City Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.GREEN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Green bin for general household waste."),
                    OnboardingBinSetup("Recycling", "Dry Recycling", BinColor.BLUE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Blue bin for mixed dry recycling."),
                    OnboardingBinSetup("Food Waste Caddy", "Food Waste Caddy", BinColor.BROWN, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Brown caddy for kitchen food waste.")
                )
            )

            lowerAdmin.contains("cambridge") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Cambridge City Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.BLACK, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Black bin for general waste."),
                    OnboardingBinSetup("Recycling", "Dry Recycling", BinColor.BLUE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Blue bin for dry recycling."),
                    OnboardingBinSetup("Organic Waste", "Organic Waste", BinColor.GREEN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Green bin for food and garden waste.")
                )
            )

            lowerAdmin.contains("brighton") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Brighton & Hove City Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.BLACK, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Black bin for general household waste."),
                    OnboardingBinSetup("Recycling", "Mixed Recycling", BinColor.GREEN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Green bin for dry mixed recycling.")
                )
            )

            lowerAdmin.contains("newcastle") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "Newcastle City Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.GREEN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Green bin for household waste."),
                    OnboardingBinSetup("Recycling", "Dry Recycling", BinColor.BLUE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Blue bin for paper, card, and plastics."),
                    OnboardingBinSetup("Garden Waste", "Garden Waste", BinColor.BROWN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Brown bin for garden waste.")
                )
            )

            lowerAdmin.contains("york") -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = "City of York Council",
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.GREY, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Grey bin for non-recyclable waste."),
                    OnboardingBinSetup("Recycling", "Dry Recycling", BinColor.GREEN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Green bin for recycling."),
                    OnboardingBinSetup("Garden Waste", "Garden Waste", BinColor.BROWN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Brown bin for garden waste.")
                )
            )

            // -----------------------------------------------------------------
            // REGIONAL CATEGORIES (330+ UK Local Authorities)
            // -----------------------------------------------------------------

            // London Boroughs (32 boroughs + City of London)
            isLondonBorough(lowerAdmin, reg) -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = formatCouncilName(admin, "Council"),
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "Household Rubbish", BinColor.BLACK, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Non-recyclable household waste."),
                    OnboardingBinSetup("Dry Recycling", "Mixed Recycling", BinColor.BLUE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Paper, card, plastic bottles, tins, and cans."),
                    OnboardingBinSetup("Food Waste", "Food Waste Caddy", BinColor.BROWN, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Kitchen food waste caddy."),
                    OnboardingBinSetup("Garden Waste", "Garden Waste", BinColor.GREEN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Green waste and garden cuttings.")
                )
            )

            // Scottish Local Authorities (32 local authorities)
            isScottishCouncil(lowerAdmin, reg) -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = formatCouncilName(admin, "Council"),
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "Non-Recyclable Waste", BinColor.GREY, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Grey or green bin for general non-recyclable waste."),
                    OnboardingBinSetup("Paper & Cardboard", "Paper & Cardboard", BinColor.BLUE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Blue bin for clean paper, card, and magazines."),
                    OnboardingBinSetup("Metals, Plastics & Glass", "Packaging & Glass", BinColor.PURPLE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Plastics, metals, and glass containers."),
                    OnboardingBinSetup("Food & Garden Waste", "Food & Garden Waste", BinColor.BROWN, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Brown bin for kitchen food scraps and garden waste.")
                )
            )

            // Welsh Local Authorities (22 local authorities)
            isWelshCouncil(lowerAdmin, reg) -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = formatCouncilName(admin, "Council"),
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.BLACK, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Non-recyclable household waste bin."),
                    OnboardingBinSetup("Dry Recycling", "Dry Recycling", BinColor.GREEN, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Welsh Collections Blueprint dry mixed recycling."),
                    OnboardingBinSetup("Food Waste", "Food Waste Caddy", BinColor.BROWN, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Weekly kitchen food waste caddy."),
                    OnboardingBinSetup("Garden Waste", "Garden Waste", BinColor.GREEN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Garden plant clippings and grass.")
                )
            )

            // Northern Ireland District Councils (11 district councils)
            isNorthernIrelandCouncil(lowerAdmin, reg) -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = formatCouncilName(admin, "Council"),
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "Landfill Bin", BinColor.BLACK, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Black bin for non-recyclable household rubbish."),
                    OnboardingBinSetup("Recycling", "Dry Recycling", BinColor.BLUE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Blue bin for paper, cardboard, tins, and plastics."),
                    OnboardingBinSetup("Organic Waste", "Organic Bin", BinColor.BROWN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Brown bin for food scraps and garden waste.")
                )
            )

            // Default UK Council Standard Engine (English Shire Districts, Unitary & Metropolitan Authorities)
            else -> CouncilScheduleResult(
                postcode = dto.postcode,
                councilName = formatCouncilName(admin, "Council"),
                adminDistrict = admin,
                primaryCollectionDay = collectionDay,
                binSetups = listOf(
                    OnboardingBinSetup("General Waste", "General Waste", BinColor.GREY, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Household refuse and non-recyclable waste."),
                    OnboardingBinSetup("Recycling", "Dry Mixed Recycling", BinColor.BLUE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true, customNote = "Paper, cardboard, plastic bottles, and tins."),
                    OnboardingBinSetup("Garden Waste", "Garden Waste", BinColor.GREEN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false, customNote = "Grass clippings, leaves, and hedge trimmings."),
                    OnboardingBinSetup("Food Waste", "Food Waste Caddy", BinColor.BROWN, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false, customNote = "Kitchen food leftovers and vegetable peelings.")
                )
            )
        }
    }

    private fun resolveDirectCouncilNameMatch(query: String): CouncilScheduleResult? {
        val q = query.trim()
        val lowerQ = q.lowercase()

        val knowns = listOf(
            "St Albans", "Saint Albans", "Manchester", "Stockport", "Westminster", "Birmingham",
            "Leeds", "Glasgow", "Edinburgh", "Bristol", "Sheffield", "Cornwall", "Liverpool",
            "Leicester", "Nottingham", "Cardiff", "Belfast", "Oxford", "Cambridge", "Brighton",
            "Newcastle", "York", "Camden", "Islington", "Hackney", "Southwark", "Lambeth",
            "Wandsworth", "Ealing", "Barnet", "Croydon", "Bromley", "Highland", "Fife",
            "Aberdeenshire", "Aberdeen", "Dundee", "Renfrewshire", "Ayrshire", "Powys",
            "Swansea", "Newport", "Wrexham", "Gwynedd", "Derry", "Armagh", "Fermanagh",
            "Wiltshire", "Dorset", "Somerset", "Buckinghamshire", "Surrey", "Hampshire",
            "Kent", "Essex", "Norfolk", "Suffolk", "Hertfordshire", "Lincolnshire",
            "Leicestershire", "Nottinghamshire", "Derbyshire", "Staffordshire", "Warwickshire",
            "Worcestershire", "Lancashire", "Cheshire", "Northumberland", "Durham"
        )

        val match = knowns.firstOrNull { lowerQ.contains(it.lowercase()) }
        return if (match != null) {
            resolveCouncilSchedule(PostcodeLookupDto(postcode = q, adminDistrict = match))
        } else if (lowerQ.contains("council") || lowerQ.contains("borough") || lowerQ.contains("district") || lowerQ.contains("county")) {
            val dummyDto = PostcodeLookupDto(postcode = q, adminDistrict = q)
            resolveCouncilSchedule(dummyDto)
        } else null
    }

    private fun resolveCollectionDay(postcode: String): DayOfWeek {
        val clean = postcode.replace(" ", "").uppercase()
        val hash = Math.abs(clean.hashCode())
        val weekdays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
        return weekdays[hash % weekdays.size]
    }

    private fun formatCouncilName(admin: String, defaultSuffix: String): String {
        val trimmed = admin.trim()
        val lower = trimmed.lowercase()

        return when {
            lower.contains("council") -> trimmed
            lower.contains("borough") -> if (lower.contains("council")) trimmed else "$trimmed Council"
            lower.contains("district") -> if (lower.contains("council")) trimmed else "$trimmed Council"
            lower.contains("city") -> if (lower.contains("council")) trimmed else "$trimmed Council"
            else -> "$trimmed $defaultSuffix"
        }
    }

    private fun isLondonBorough(admin: String, region: String): Boolean {
        if (region.lowercase() == "london") return true
        val londonDistricts = listOf(
            "barking and dagenham", "barnet", "bexley", "brent", "bromley", "camden",
            "city of london", "croydon", "ealing", "enfield", "greenwich", "hackney",
            "hammersmith and fulham", "haringey", "harrow", "havering", "hillingdon",
            "hounslow", "islington", "kensington and chelsea", "kingston upon thames",
            "lambeth", "lewisham", "merton", "newham", "redbridge", "richmond upon thames",
            "southwark", "sutton", "tower hamlets", "waltham forest", "wandsworth"
        )
        return londonDistricts.any { admin.contains(it) }
    }

    private fun isScottishCouncil(admin: String, region: String): Boolean {
        if (region.lowercase() == "scotland") return true
        val scottishDistricts = listOf(
            "aberdeen", "aberdeenshire", "angus", "argyll and bute", "clackmannanshire",
            "dumfries and galloway", "dundee", "east ayrshire", "east dunbartonshire",
            "east lothian", "east renfrewshire", "edinburgh", "falkirk", "fife",
            "glasgow", "highland", "inverclyde", "midlothian", "moray", "na h-eileanan siar",
            "north ayrshire", "north lanarkshire", "orkney", "perth and kinross",
            "renfrewshire", "scottish borders", "shetland", "south ayrshire",
            "south lanarkshire", "stirling", "west dunbartonshire", "west lothian", "western isles"
        )
        return scottishDistricts.any { admin.contains(it) }
    }

    private fun isWelshCouncil(admin: String, region: String): Boolean {
        if (region.lowercase() == "wales") return true
        val welshDistricts = listOf(
            "blaenau gwent", "bridgend", "caerphilly", "cardiff", "caerdydd", "carmarthenshire",
            "ceredigion", "conwy", "denbighshire", "flintshire", "gwynedd", "isle of anglesey",
            "ynys môn", "merthyr tydfil", "monmouthshire", "neath port talbot", "newport",
            "pembrokeshire", "powys", "rhondda cynon taf", "swansea", "torfaen", "vale of glamorgan",
            "wrexham", "wrecsam"
        )
        return welshDistricts.any { admin.contains(it) }
    }

    private fun isNorthernIrelandCouncil(admin: String, region: String): Boolean {
        if (region.lowercase().contains("northern ireland")) return true
        val niDistricts = listOf(
            "antrim and newtownabbey", "ards and north down", "armagh city", "banbridge",
            "belfast", "causeway coast and glens", "derry city and strabane", "fermanagh and omagh",
            "lisburn and castlereagh", "mid and east antrim", "mid ulster", "newry, mourne and down"
        )
        return niDistricts.any { admin.contains(it) }
    }
}
