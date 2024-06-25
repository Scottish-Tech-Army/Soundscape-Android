package com.kersnazzle.soundscapealpha

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kersnazzle.soundscapealpha.network.ITiles
import com.kersnazzle.soundscapealpha.network.Tiles
import kotlinx.coroutines.launch

class MainViewModel(var tileService: ITiles = Tiles()): ViewModel() {
    var tile: MutableLiveData<String?> = MutableLiveData<String?>()

    fun getTile(xtile: Int, ytile: Int) {
        viewModelScope.launch {
            val innerTile = tileService.getTile(xtile, ytile).toString()
            tile.postValue(innerTile)
        }
    }

}