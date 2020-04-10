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