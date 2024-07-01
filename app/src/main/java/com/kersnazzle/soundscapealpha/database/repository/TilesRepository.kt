package com.kersnazzle.soundscapealpha.database.repository

import com.kersnazzle.soundscapealpha.database.local.dao.TilesDao
import com.kersnazzle.soundscapealpha.database.local.model.TileData
import io.realm.kotlin.notifications.ResultsChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TilesRepository(val tilesDao: TilesDao) {

    suspend fun insertTile(tile: TileData) = withContext(Dispatchers.IO) {
        tilesDao.insertTile(tile)
    }

    fun getAllTiles(): Flow<ResultsChange<TileData>> = tilesDao.getAllTiles()

    suspend fun deleteTile(quadkey: String) = withContext(Dispatchers.IO) {
        tilesDao.deleteTile(quadkey)
    }

    suspend fun updateTile(tile: TileData) = withContext(Dispatchers.IO) {
        tilesDao.updateTile(tile)
    }
}