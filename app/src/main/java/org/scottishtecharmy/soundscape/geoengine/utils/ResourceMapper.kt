package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.R

class ResourceMapper {
    companion object {
        private val resourceMap: HashMap<String, Int> by
        lazy {
            HashMap<String, Int>().apply {
                put("crossing", R.string.osm_tag_crossing)
                put("construction", R.string.osm_tag_construction)
                put("dangerous_area", R.string.osm_tag_dangerous_area)
                put("townhall", R.string.osm_tag_townhall)
                put("steps", R.string.osm_tag_steps)
                put("elevator", R.string.osm_tag_elevator)
                put("walking_path", R.string.osm_tag_walking_path)
                put("pedestrian_street", R.string.osm_tag_pedestrian_street)
                put("bicycle_path", R.string.osm_tag_bicycle_path)
                put("residential_street", R.string.osm_tag_residential_street)
                put("service_road", R.string.osm_tag_service_road)
                put("road", R.string.osm_tag_road)
                put("highway", R.string.osm_tag_highway)
                put("highway_named", R.string.osm_tag_highway_named)
                put("highway_refed", R.string.osm_tag_highway_refed)
                put("intersection", R.string.osm_tag_intersection)
                put("roundabout", R.string.osm_tag_roundabout)
                put("highway_ramp", R.string.osm_tag_highway_ramp)
                put("merging_lane", R.string.osm_tag_merging_lane)
                put("office", R.string.osm_tag_office_building)
                put("school", R.string.osm_tag_school_building)
                put("roof", R.string.osm_tag_covered_pavilion)
                put("convenience", R.string.osm_tag_convenience_store)
                put("entrance", R.string.osm_tag_building_entrance)
                put("assembly_point", R.string.osm_tag_assembly_point)
                put("cycle_barrier", R.string.osm_tag_cycle_barrier)
                put("turnstile", R.string.osm_tag_turnstile)
                put("cattle_grid", R.string.osm_tag_cattle_grid)
                put("gate", R.string.osm_tag_gate)
                put("lift_gate", R.string.osm_tag_gate)
                put("toilets", R.string.osm_tag_restroom)
                put("parking", R.string.osm_tag_parking_lot)
                put("parking_entrance", R.string.osm_tag_parking_entrance)
                put("bench", R.string.osm_tag_bench)
                put("taxi", R.string.osm_tag_taxi_waiting_area)
                put("post_office", R.string.osm_tag_post_office)
                put("post_box", R.string.osm_tag_post_box)
                put("waste_basket", R.string.osm_tag_waste_basket)
                put("shower", R.string.osm_tag_shower)
                put("bicycle_parking", R.string.osm_tag_bike_parking)
                put("cafe", R.string.osm_tag_cafe)
                put("restaurant", R.string.osm_tag_restaurant)
                put("telephone", R.string.osm_tag_telephone)
                put("fuel", R.string.osm_tag_gas_station)
                put("bank", R.string.osm_tag_bank)
                put("atm", R.string.osm_tag_atm)
                put("atm_named", R.string.osm_tag_atm_named)
                put("atm_refed", R.string.osm_tag_atm_refed)
                put("bus_stop", R.string.osm_tag_bus_stop)
                put("recycling", R.string.osm_tag_recycling_bin)
                put("fountain", R.string.osm_tag_fountain)
                put("place_of_worship", R.string.osm_tag_place_of_worship)
                put("drinking_water", R.string.osm_tag_water_fountain)
                put("car_wash", R.string.osm_tag_car_wash)
                put("vending_machine", R.string.osm_tag_vending_machine)
                put("playground", R.string.osm_tag_playground)
                put("pitch", R.string.osm_tag_sports_field)
                put("swimming_pool", R.string.osm_tag_swimming_pool)
                put("garden", R.string.osm_tag_garden)
                put("park", R.string.osm_tag_park)
                put("picnic_table", R.string.osm_tag_picnic_table)
                put("picnic_site", R.string.osm_tag_picnic_area)
            }
        }
        fun getResourceId(key: String?): Int? {
            if(key == null) return null
            return resourceMap[key]
        }
    }
}