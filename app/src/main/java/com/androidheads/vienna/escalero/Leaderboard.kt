package com.androidheads.vienna.escalero

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Leaderboard(

    var name: String? = "",
    var matchCnt: Long? = 0L,
    var score: Long? = 0L,  // Escalero Points (EP)
    var timestamp: Long? = 0L,
    var uid: String? = ""

)