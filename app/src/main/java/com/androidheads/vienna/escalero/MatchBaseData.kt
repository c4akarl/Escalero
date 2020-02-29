package com.androidheads.vienna.escalero

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class MatchBaseData(

        var appVersionCodeA: Long? = 1L,
        var appVersionCodeB: Long? = 1L,
        var bonusServed: Long? = 5L,
        var bonusServedGrande: Long? = 30L,
        var col: Long? = 3L,
        var colMultiplier: Long? = 1L,
        var colPoints: String? = "1 2 4 3",
        var date: String? = "",
        var diceState: Long? = 1L,
        var nameA: String? = "",
        var nameB: String? = "",
        var player: Long? = 2L,                 // player number: 2
        var playerIdA: String? = "",
        var playerIdB: String? = "",
        var silent: Boolean? = false,
        var singleGame: Boolean? = true,
        var unit: String? = "Points"

)