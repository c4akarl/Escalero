/*
    Escalero - An Android dice program.
    Copyright (C) 2016-2020  Karl Schreiner, c4akarl@gmail.com

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