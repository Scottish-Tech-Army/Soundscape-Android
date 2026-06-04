package org.scottishtecharmy.soundscape.viewmodels

import org.junit.Test
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ExtractTest {

    private val geojson =
        """{"type": "Feature", "geometry": {"type": "Polygon", 
            |"coordinates": [[[139.81691185318002, 37.56831909920214], 
            |[139.81452073905476, 37.84856243009939], [139.86499116717772, 37.84884359203526], 
            |[139.86341400048522, 38.03211295423315], [141.00240215693833, 38.03278598895171], 
            |[140.99885371261206, 37.500096384164024], [141.45101968072916, 37.49735188393407], 
            |[141.4398807979843, 36.59634687676648], [141.20879156854664, 36.59795080805193], 
            |[141.20471272846316, 36.147418099935344], [141.08364793550157, 36.14807554122666], 
            |[141.08328622945226, 36.099285004209975], [139.97284530020468, 36.09951739243942], 
            |[139.96894767976792, 36.66802927279343], [139.7042382614323, 36.66653760928801], 
            |[139.69471400090455, 37.56755097639593], [139.81691185318002, 37.56831909920214]]]}, 
            |"properties": {"name": "Iwaki", "iso_a2": "JP", "feature_type": "city_cluster", 
            |"name_local": "いわき市", "city_names": ["Hitachi", "Nihommatsu", "Kōriyama", 
            |"Hitachi-ota", "Sukagawa", "Shirakawa", "Iwaki"], 
            |"city_local_names": ["日立", "二本松", "郡山市", "常陸太田", "須賀川市", "白河", "いわき市"], 
            |"extract-size": 87491126, "extract-size-string":"0.4GB", 
            |"filename": "iwaki-jp.pmtiles"}}""".trimMargin()
    private val adapter = GeoJsonObjectMoshiAdapter()
    private val extract = Extract((adapter.fromJson(geojson) as Feature))

    @Test
    fun extractSizeIsAsDefined() {
        assertEquals(87491126.0, extract.size)
    }

    @Test
    fun extractSizeReadableAsDefined() {
        assertEquals("0.4GB", extract.sizeReadable)
    }

    @Test
    fun extractLocalNameAsDefined() {
        assertEquals("いわき市", extract.localName)
    }

    @Test
    fun extractLocalCitiesAsDefined() {
        assertContentEquals(
            expected = listOf(
                "日立", "二本松", "郡山市", "常陸太田", "須賀川市", "白河", "いわき市"
            ), extract.localCities
        )
    }

    @Test
    fun extractAlternateCitiesAsDefined() {
        assertContentEquals(
            expected = listOf(
                "Hitachi", "Nihommatsu", "Kōriyama", "Hitachi-ota", "Sukagawa", "Shirakawa", "Iwaki"
            ), extract.alternateCities
        )
    }

    @Test
    fun extractHasCityClusterCitiesAsDefined() {
        assert(extract.hasCityCluster)
    }

    @Test
    fun extractHasFilenameAsDefined() {
        assertEquals("iwaki-jp.pmtiles", extract.filename)
    }

    @Test
    fun featureCollectionAsExtractsReturnsListOfExtracts() {
        val fc = FeatureCollection()
        assertEquals(fc.count(), fc.asExtracts().size)
    }

}