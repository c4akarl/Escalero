package com.androidheads.vienna.escalero

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(

        var ads: Boolean? = false,
        var appVersionCode: Long? = 0L,
        var chat: Boolean? = false,
        var currentMatchId: String? = "",
        var iconImageUri: String? = "",
        var language: String? = "",
        var name: String? = "",
        var notifications: Boolean? = true,
        var online: Boolean? = false,
        var onlineCheckId: String? = "",
        var playing: Boolean? = false,
        var singleGame: Boolean? = true,
        var timestamp: Long? = 0L,
        var token: String? = "",
        var uid: String? = ""

)