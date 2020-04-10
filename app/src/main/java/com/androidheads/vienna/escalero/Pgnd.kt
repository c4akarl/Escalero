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

import java.io.*

class Pgnd(gameData: String) {
    private var tags = LinkedHashMap<String, String>()          // tag key, tag value
    private var games = LinkedHashMap<Int, Long>()     // game number, line pointer raf(random access file)
    private var gameCounter = 0

    init {
        if (gameData == "")
            initTags()
        else
            setTagsFromGameData(gameData)
    }

    fun dataToFile(path: String, file: String, data: String): Boolean {
        var filePath = path
        var fileData = data
        var isOk = true
        var append = true
        if (!filePath.endsWith("/"))
            filePath = "$filePath/"
        if (!pathExists(filePath))
            isOk = createPath(filePath)
        if (isOk and fileData.startsWith("")) {
            if (!file.endsWith(EXTENSION_PGND))
                isOk = false
            if (!fileExists(filePath, file))
                append = false
        }
        if (isOk) {
            try {
                val f = File(filePath + file)
                val fOut: FileOutputStream
                if (append) {
                    fileData = "\n" + fileData
                    fOut = FileOutputStream(f, true)
                } else
                    fOut = FileOutputStream(f)

                val osw = OutputStreamWriter(fOut)
                osw.write(fileData)
                osw.flush()
                osw.close()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                isOk = false
            } catch (e: IOException) {
                e.printStackTrace()
                isOk = false
            }

        }

        return isOk
    }

    fun pathExists(path: String): Boolean {
        var isPath = false
        val f = File(path)
        if (f.isDirectory)
            isPath = true
        return isPath
    }

    private fun createPath(path: String): Boolean {
        val f = File(path)
        return f.mkdirs()
    }

    private fun fileExists(path: String, file: String): Boolean {
        var isFile = true
        val fileName = path + file
        try {
            val f = File(fileName)                            // ... no action
            val fileIS = FileInputStream(f)        // ... no action
            fileIS.available()
        } catch (e: FileNotFoundException) {
            isFile = false
        }        // file not exists!
        catch (e: IOException) {
            isFile = false
        }
        // file not exists!
        return isFile
    }

    fun getGamesFromFile(path: String, file: String) {
        val f = File(path + file)
        if (!f.exists())
            return

        games.clear()
        gameCounter = 0
        val startSkip: Long = 0
        var line: String?
        val raf: Braf
        var rafLinePointer: Long
        try {
            raf = Braf(path + file, "r", BUF_SIZE)
        } catch (e1: FileNotFoundException) {
            e1.printStackTrace()
            return
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }

        // games
        try {
            raf.seek(startSkip)
            rafLinePointer = raf.filePointer

            do {
                line = raf.nextLine
                if (line == null)
                    break
                if (line.startsWith(GAME_START)) {
                    gameCounter++
                    games[gameCounter] = rafLinePointer
                }
                rafLinePointer = raf.filePointer
            }
            while (true)
            raf.close()

        } catch (e: IOException) {
            e.printStackTrace()
            return
        }

    }

    fun getGameListFromGames(path: String, file: String): Array<String> {
        val entries = games.entries.iterator()
        val gameList = Array(games.entries.size) { "" }
        while (entries.hasNext()) {
            val thisEntry = entries.next() as Map.Entry<*, *>
            val key = thisEntry.key as Int
            val listId = key - 1
            setTagsFromGameData(getGameDataFromGames(path, file, key))
            var game = getTag("Game")
            game = game?.replace("DiceEngine", "DE")
            var sg = "s"
            if (getTag("SingleGame") == "false")
                sg = "d"
            gameList[listId] = "" + key + " " + game + " " + getTag("Date") + " " + sg + getTag("Player")
        }

        return gameList

    }

    fun getGameDataFromGames(path: String, file: String, key: Int): String {

//        Log.i(TAG, "getGameDataFromGames(), file: $path$file, $key")

        var gameData = ""

        if (games[key] != null) {
            val raf: Braf

            val gamesLength = games[key]!!

            try {
                raf = Braf(path + file, "r", BUF_SIZE)
            } catch (e1: FileNotFoundException) {
                e1.printStackTrace()
                return ""
            } catch (e: IOException) {
                e.printStackTrace()
                return ""
            }

            // get game from key
            try {
                raf.seek(gamesLength)
                var isGameStart = true
                var line: String?
                do {
                    line = raf.nextLine
                    if (line == null)
                        break
                    if (line.startsWith(GAME_START) and !isGameStart)
                        break
                    if (line.startsWith(GAME_START)) {
                        if (isGameStart) {
                            gameData = gameData + line + "\n"
                            isGameStart = false
                        }
                    } else
                        gameData = gameData + line + "\n"
                }
                while (true)
                raf.close()

            } catch (e: IOException) {
                e.printStackTrace()
                return ""
            }

        } else
            return ""

        return gameData
    }

    private fun initTags() {
        tags["Dice"] = "pgnd"
        tags["Variant"] = "escalero"
        tags["Game"] = ""
        tags["Date"] = ""
        tags["DiceState"] = "1"
        tags["SingleGame"] = "true"
        tags["Player"] = "2"
        tags["Starter"] = "A"
        tags["PlayerToMove"] = "A"
        tags["Col"] = "3"
        tags["ColPoints"] = "1 2 4 3"
        tags["ColMultiplier"] = "1"
        tags["Unit"] = "Points"
        tags["BonusServed"] = "5"
        tags["BonusServedGrande"] = "30"
        tags["PlayerColumn"] = "false"
        tags["Summation"] = "false"
        tags["FlipScreen"] = "false"
        tags["Icons"] = "2"
        tags["Sounds"] = "true"
        tags["Logging"] = "false"
        tags["Advertising"] = "true"

        tags["GridItem"] = "0"
        tags["Diced"] = "false"
        tags["Served"] = "false"
        tags["DiceModus"] = "0"
        tags["DiceModusPrev"] = "0"
        tags["DiceRoll"] = "-----"
        tags["DiceHold"] = "-----"
        tags["DiceRollPrev"] = "-----"
        tags["DiceHoldPrev"] = "-----"
        tags["DiceDouble1"] = "-----"
        tags["Double1"] = "false"
        tags["ServedDouble1"] = "false"

        tags["EnginePlayerA"] = "false"
        tags["EnginePlayerB"] = "false"
        tags["EnginePlayerC"] = "false"
        tags["NameA"] = ""
        tags["NameB"] = ""
        tags["NameC"] = ""
        tags["\$E1"] = ""
        tags["\$E2"] = ""
        tags["\$E3"] = ""

        tags["OnlinePlayerName"] = ""
        tags["OnlineEpA"] = "0"
        tags["OnlineEpB"] = "0"
        tags["OnlineStat"] = ""
        tags["OnlineCheckId"] = ""
        tags["AppVersionCode"] = ""
        tags["AppVersionName"] = ""
        tags["Position"] = ""
    }

    fun setTagsFromGameData(gameData: String) {

        //        Log.i(TAG, "gameData: \n" + gameData + "\n");

        tags.clear()
        val textStr = gameData.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var keyPosition = ""
        var valuePosition = ""
        for (i in textStr.indices) {
            var key = ""
            var value = ""
            if (textStr[i].startsWith("[") and textStr[i].endsWith("]")) {
                textStr[i] = textStr[i].replace("[", "")
                textStr[i] = textStr[i].replace("]", "")
                val textStr2 = textStr[i].split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (textStr2.size > 1) {
                    key = textStr2[0]
                    val repl = "$key "
                    textStr[i] = textStr[i].replace(repl, "")
                    value = textStr[i].replace("\"", "")
                }
                tags[key] = value
            } else {
                if (textStr[i].startsWith(POSITION_START)) {
                    textStr[i] = textStr[i].replace("[", "")
                    val textStr2 = textStr[i].split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (textStr2.size > 1)
                        keyPosition = textStr2[0]
                }
                if ((keyPosition != "") and textStr[i].startsWith(SETCOL_START))
                    valuePosition = valuePosition + "\n" + textStr[i]
            }
        }
        if (keyPosition != "") {
            //            Log.i(TAG, "key: " + keyPosition + ", value: " + valuePosition);
            tags[keyPosition] = valuePosition
        }
    }

    fun setTag(key: String, value: String) {
        if (tags[key] != null)
            tags[key] = value
    }

    fun getTag(key: String): String? {
        return if (tags[key] != null)
            tags[key]
        else
            ""
    }

    fun getGameData(): String {
        var data = ""

        val entries = tags.entries.iterator()
        while (entries.hasNext()) {
            val thisEntry = entries.next() as Map.Entry<*, *>
            data = data + "[" + thisEntry.key + " \"" + thisEntry.value + "\"]\n"
        }

        data = "$data\n*\n"

        //karl
        if (data.contains("\"]\"]"))
            data = data.replace("\"]\"]", "\"]")
        return data

    }

    companion object {
        const val TAG = "Pgnd"
        const val BUF_SIZE = 100000
        const val EXTENSION_PGND = ".pgnd"
        const val GAME_START = "[Dice "
        const val POSITION_START = "[Position "
        const val SETCOL_START = "setcol "
    }

}
