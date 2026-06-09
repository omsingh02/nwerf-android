package com.omsingh.nwerf

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class IdentifyTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val tile = qsTile
        if (tile.state == Tile.STATE_INACTIVE) {
            tile.state = Tile.STATE_ACTIVE
            tile.updateTile()

            // Launch MainActivity with identify deep link since we need permission and UI context
            // Background recording from a TileService is heavily restricted in modern Android
            val intent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = android.net.Uri.parse("nwerf://identify")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivityAndCollapse(intent)
            
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile.state = Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
}
