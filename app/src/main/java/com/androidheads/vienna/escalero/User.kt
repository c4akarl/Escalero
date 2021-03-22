/*
    Escalero - An Android dice program.
    Copyright (C) 2016-2021  Karl Schreiner, c4akarl@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

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