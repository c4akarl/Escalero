package com.androidheads.vienna.escalero

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserMatch(

        var matchId: String? = "",
        var players: String? = "",
        var timestamp: String? = "",
        var turnId: String? = "",
        var variant: String? = ""

)