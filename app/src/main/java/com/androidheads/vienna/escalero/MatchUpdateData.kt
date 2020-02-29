package com.androidheads.vienna.escalero

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class MatchUpdateData(

        var diced: Boolean? = false,
        var diceDouble1: String? = "-----",
        var diceHold: String? = "-----",
        var diceHoldPrev: String? = "-----",
        var diceModus: Long? = 0L,
        var diceModusPrev: Long? = 0L,
        var diceRoll: String? = "-----",
        var diceRollPrev: String? = "-----",
        var double1: Boolean? = false,
        var gridItem: Long? = 0L,
        var onlineAction: String? = "",
        var playerToMove: String? = "A",
        var position: String? = "",
        var served: Boolean? = false,
        var servedDouble1: Boolean? = false,
        var starter: String? = "A",
        var turnPlayerId: String? = ""          // firebase uid

)