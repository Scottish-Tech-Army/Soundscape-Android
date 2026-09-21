package org.scottishtecharmy.soundscape.utils

import platform.MapKit.MKPointOfInterestCategory
import platform.MapKit.MKPointOfInterestCategoryAirport
import platform.MapKit.MKPointOfInterestCategoryAmusementPark
import platform.MapKit.MKPointOfInterestCategoryAquarium
import platform.MapKit.MKPointOfInterestCategoryATM
import platform.MapKit.MKPointOfInterestCategoryBakery
import platform.MapKit.MKPointOfInterestCategoryBank
import platform.MapKit.MKPointOfInterestCategoryBeach
import platform.MapKit.MKPointOfInterestCategoryBrewery
import platform.MapKit.MKPointOfInterestCategoryCafe
import platform.MapKit.MKPointOfInterestCategoryCampground
import platform.MapKit.MKPointOfInterestCategoryCarRental
import platform.MapKit.MKPointOfInterestCategoryEVCharger
import platform.MapKit.MKPointOfInterestCategoryFireStation
import platform.MapKit.MKPointOfInterestCategoryFitnessCenter
import platform.MapKit.MKPointOfInterestCategoryFoodMarket
import platform.MapKit.MKPointOfInterestCategoryGasStation
import platform.MapKit.MKPointOfInterestCategoryHospital
import platform.MapKit.MKPointOfInterestCategoryHotel
import platform.MapKit.MKPointOfInterestCategoryLaundry
import platform.MapKit.MKPointOfInterestCategoryLibrary
import platform.MapKit.MKPointOfInterestCategoryMarina
import platform.MapKit.MKPointOfInterestCategoryMovieTheater
import platform.MapKit.MKPointOfInterestCategoryMuseum
import platform.MapKit.MKPointOfInterestCategoryNationalPark
import platform.MapKit.MKPointOfInterestCategoryNightlife
import platform.MapKit.MKPointOfInterestCategoryPark
import platform.MapKit.MKPointOfInterestCategoryParking
import platform.MapKit.MKPointOfInterestCategoryPharmacy
import platform.MapKit.MKPointOfInterestCategoryPolice
import platform.MapKit.MKPointOfInterestCategoryPostOffice
import platform.MapKit.MKPointOfInterestCategoryPublicTransport
import platform.MapKit.MKPointOfInterestCategoryRestaurant
import platform.MapKit.MKPointOfInterestCategoryRestroom
import platform.MapKit.MKPointOfInterestCategorySchool
import platform.MapKit.MKPointOfInterestCategoryStadium
import platform.MapKit.MKPointOfInterestCategoryStore
import platform.MapKit.MKPointOfInterestCategoryTheater
import platform.MapKit.MKPointOfInterestCategoryUniversity
import platform.MapKit.MKPointOfInterestCategoryWinery
import platform.MapKit.MKPointOfInterestCategoryZoo

/**
 * Maps an Apple [MKPointOfInterestCategory] onto the OSM feature-class key that
 * `LocalizedStrings.resolveFeatureClass()` understands.
 *
 * Going via an OSM key rather than introducing a parallel set of Apple-specific strings means the
 * category text a search result shows is the same wording, already translated, that the offline
 * and Photon geocoders produce for the same kind of place - a result that says "Cafe" reads
 * identically whether it came from a tile, from Photon or from MapKit.
 *
 * Only the categories available at our iOS 16 deployment target are listed. Apple adds more with
 * most releases (and iOS 18 added a large batch); those arrive here as null, which simply means
 * the result renders with no category line rather than the wrong one.
 */
fun osmKeyForPoiCategory(category: MKPointOfInterestCategory?): String? = when (category) {
    null -> null
    MKPointOfInterestCategoryAirport -> "airport"
    MKPointOfInterestCategoryAmusementPark -> "theme_park"
    // No osm_aquarium string exists, and "attraction" is the closest honest generalisation.
    MKPointOfInterestCategoryAquarium -> "attraction"
    MKPointOfInterestCategoryATM -> "atm"
    MKPointOfInterestCategoryBakery -> "bakery"
    MKPointOfInterestCategoryBank -> "bank"
    MKPointOfInterestCategoryBeach -> "beach"
    MKPointOfInterestCategoryBrewery -> "brewery"
    MKPointOfInterestCategoryCafe -> "cafe"
    MKPointOfInterestCategoryCampground -> "camp_site"
    MKPointOfInterestCategoryCarRental -> "car_rental"
    MKPointOfInterestCategoryEVCharger -> "charging_station"
    MKPointOfInterestCategoryFireStation -> "fire_station"
    MKPointOfInterestCategoryFitnessCenter -> "fitness_centre"
    MKPointOfInterestCategoryFoodMarket -> "supermarket"
    MKPointOfInterestCategoryGasStation -> "fuel"
    MKPointOfInterestCategoryHospital -> "hospital"
    MKPointOfInterestCategoryHotel -> "hotel"
    MKPointOfInterestCategoryLaundry -> "laundry"
    MKPointOfInterestCategoryLibrary -> "library"
    MKPointOfInterestCategoryMarina -> "marina"
    MKPointOfInterestCategoryMovieTheater -> "cinema"
    MKPointOfInterestCategoryMuseum -> "museum"
    MKPointOfInterestCategoryNationalPark -> "national_park"
    MKPointOfInterestCategoryNightlife -> "nightclub"
    MKPointOfInterestCategoryPark -> "park"
    MKPointOfInterestCategoryParking -> "parking"
    MKPointOfInterestCategoryPharmacy -> "pharmacy"
    MKPointOfInterestCategoryPolice -> "police"
    MKPointOfInterestCategoryPostOffice -> "post_office"
    // Deliberately the generic "Station": Apple lumps every transit mode under this one category,
    // so anything more specific would be guessing at the mode.
    MKPointOfInterestCategoryPublicTransport -> "station"
    MKPointOfInterestCategoryRestaurant -> "restaurant"
    MKPointOfInterestCategoryRestroom -> "toilets"
    MKPointOfInterestCategorySchool -> "school"
    MKPointOfInterestCategoryStadium -> "stadium"
    MKPointOfInterestCategoryStore -> "shop"
    MKPointOfInterestCategoryTheater -> "theatre"
    MKPointOfInterestCategoryUniversity -> "university"
    MKPointOfInterestCategoryWinery -> "winery"
    MKPointOfInterestCategoryZoo -> "zoo"
    else -> null
}
