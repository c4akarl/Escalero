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

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.webkit.WebView
import android.widget.*
import kotlinx.android.synthetic.main.file_manager.*
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

class FileManager : Activity(), AdapterView.OnItemClickListener {
    private lateinit var filePrefs: SharedPreferences
    private lateinit var prefs: SharedPreferences
    private lateinit var runPrefs: SharedPreferences
    private var pg: Pgnd? = null
    internal var gameCounter = 0
    internal var cntDelayed = 0
    internal var nextSavePosition = 0
    private lateinit var externalStorageDirectory: String
    internal var fileActionCode = 1     // 1 load, 2 save
    internal var gameData: String? = ""
    internal var isInitLayout = true
    internal var isFinish = false
    internal var showFiles = true   // true: files, false: games
    lateinit var files: ArrayAdapter<String>
    lateinit var games: ArrayAdapter<String>
    var empty: ArrayAdapter<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.file_manager)
        fileActionCode = intent.extras!!.getInt("fileActionCode")
        gameData = intent.extras!!.getString("gameData")
        if (gameData != "")
            pg = Pgnd(gameData!!)
        initLayout()
        filePrefs = getSharedPreferences("filePrefs", 0)
        runPrefs = getSharedPreferences("run", 0)
        prefs = getSharedPreferences("prefs", 0)
    }

    override fun onResume() {
        super.onResume()
        getFilePrefs()
    }

    override fun onItemClick(l: AdapterView<*>, v: View, position: Int, id: Long) {
        if (l === lvFiles) {
            val itemName = files.getItem(position)
            if (itemName!!.endsWith(EXTENSION_PGND)) {
                etFile.setText(itemName)
                FileTask(this).execute(etPath.text.toString(), itemName)
            } else {
                val f = File(etPath.text.toString() + itemName)
                if (f.isDirectory) {
                    val str = etPath.text.toString() + String.format("%s", itemName) + "/"
                    etPath.setText(str)
                    FileTask(this).execute(etPath.text.toString(), "")
                }
            }
        }
        if (l === lvGames) {
            btnList.visibility = ImageView.VISIBLE
            val ed = filePrefs.edit()
            ed.putInt("gamePosition", position)
            ed.apply()
            val gamesId = position + 1
            gameData = pg!!.getGameDataFromGames(etPath.text.toString(), etFile.text.toString(), gamesId)
            pg!!.setTagsFromGameData(gameData!!)
            if (fileActionCode == FILE_MANAGER_LOAD_REQUEST_CODE)
                this.title = getString(R.string.fileLoad) + getGameValues(false)
            if (fileActionCode == FILE_MANAGER_SAVE_REQUEST_CODE)
                this.title = getString(R.string.fileSave)
        }
    }

    fun myClickHandler(view: View) {
        when (view.id) {
            R.id.btnOk -> {
                val returnIntent = Intent()
                when (fileActionCode) {
                    FILE_MANAGER_LOAD_REQUEST_CODE -> {
                        setFilePrefs()
                        returnIntent.putExtra("gameData", gameData)
                        setResult(RESULT_OK, returnIntent)
                        finish()
                    }
                    FILE_MANAGER_SAVE_REQUEST_CODE -> {
                        returnIntent.putExtra("gameData", "")
                        if (gameCounter < MAX_GAMES) {
                            tvSaveGame.visibility = TextView.INVISIBLE
                            pg!!.dataToFile(etPath.text.toString(), etFile.text.toString(), gameData!!)
                            setFilePrefs()
                            isFinish = true

//                            Log.i(TAG, "myClickHandler(), R.id.btnOk, etFile.text: ${etFile.text}")

                            if (etFile.text.toString().endsWith(EXTENSION_PGND))
                                FileTask(this).execute(etPath.text.toString(), etFile.text.toString())
                            else
                                showTextDialog(resources.getString(R.string.extensionMissing))
                            return
                        } else {
                            if ((fileActionCode == FILE_MANAGER_SAVE_REQUEST_CODE) and (gameCounter >= MAX_GAMES))
                                showTextDialog(resources.getString(R.string.maxGames) + MAX_GAMES)
                        }
                    }
                }
            }
            R.id.btnDirBack -> {
                tvEmpty.visibility = TextView.INVISIBLE
                tvSaveGame.visibility = TextView.INVISIBLE
                if (fileActionCode == FILE_MANAGER_LOAD_REQUEST_CODE)
                    this.title = getString(R.string.fileLoad)
                if (fileActionCode == FILE_MANAGER_SAVE_REQUEST_CODE)
                    this.title = getString(R.string.fileSave)
                etFile.setText("")
                if (showFiles) {
                    val newPath = getNewPath(etPath.text.toString())
                    etPath.setText(newPath)
                    FileTask(this).execute(newPath, "")
                } else
                    FileTask(this).execute(etPath.text.toString(), "")
            }

            R.id.btnList -> showGameDataDialog(gameData)
        }
    }

    private fun initLayout() {
        etPath.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })
        etFile.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if ((lvGames != null) and !isInitLayout) {
                    lvGames!!.adapter = empty
                    val str = String.format("%d", 1) + getGameValues(true)
                    tvSaveGame.text = str
                    gameCounter = 0
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })
        btnList.visibility = ImageView.INVISIBLE
        lvGames.visibility = ListView.INVISIBLE
    }

    private fun getFilePrefs() {
        pg = Pgnd("")
        @Suppress("DEPRECATION")
        externalStorageDirectory = Environment.getExternalStorageDirectory().absolutePath + "/"

//        Log.i(TAG, "getFilePrefs(), externalStorageDirectory: $externalStorageDirectory")

        var path: String? = externalStorageDirectory
        var file: String? = ""
        when (fileActionCode) {
            FILE_MANAGER_LOAD_REQUEST_CODE -> {
                path = filePrefs.getString("pathLoad", externalStorageDirectory)
                file = filePrefs.getString("fileLoad", "")
                etPath.isEnabled = false
                etFile.isEnabled = false
            }
            FILE_MANAGER_SAVE_REQUEST_CODE -> {
                path = filePrefs.getString("pathSave", externalStorageDirectory + START_FOLDER)
                file = filePrefs.getString("fileSave", START_FILE)
                if (!pg!!.pathExists(path!!))
                    path = externalStorageDirectory + START_FOLDER
                etPath.isEnabled = true
                etFile.isEnabled = true
            }
        }
        if (!path!!.endsWith("/"))
            path = "$path/"
        etPath.setText(path)
        etPath.setSelection(etPath.length())
        etFile.setText(file)
        if (file!!.endsWith(EXTENSION_PGND))
            FileTask(this).execute(path, file)
        else
            FileTask(this).execute(path, "")

        return

    }

    private fun setFilePrefs() {
        val ed = filePrefs.edit()
        ed.putString("pathLoad", etPath.text.toString())
        when (fileActionCode) {
            FILE_MANAGER_LOAD_REQUEST_CODE -> {
                ed.putString("pathLoad", etPath.text.toString())
                ed.putString("fileLoad", etFile.text.toString())
            }
            FILE_MANAGER_SAVE_REQUEST_CODE -> {
                ed.putString("pathSave", etPath.text.toString())
                ed.putString("fileSave", etFile.text.toString())
                ed.putInt("gamePosition", nextSavePosition)
            }
        }
        ed.apply()
    }

    private fun showGameDataDialog(gameData: String?) {
        if (gameData == "")
            return
        val builder = AlertDialog.Builder(this)
        val title = getString(R.string.game) + getGameValues(false)
        val wv = WebView(this)
        builder.setView(wv)
        wv.loadDataWithBaseURL(null, gameData, "text", "utf-8", null)
        builder.setTitle(title)
        val alert = builder.create()
        alert.show()
    }

    fun getGameValues(isAll: Boolean): String {
        if (pg == null)
            return ""
        var game = pg!!.getTag("Game")
        game = game!!.replace("DiceEngine", "DE")
        var sg = "s"
        if (pg!!.getTag("SingleGame") == "false")
            sg = "d"
        return if (isAll)
            " " + game + " " + pg!!.getTag("Date") + " " + sg + pg!!.getTag("Player")
        else
            ": $game"
    }

    private fun getNewPath(oldPath: String): String {
        if (oldPath == externalStorageDirectory)
            return oldPath
        var newPath = ""
        var lastDirPos = 0
        oldPath.forEachIndexed { i, _ ->
            if ((oldPath[i] == '/') and (i != oldPath.length - 1))
                lastDirPos = i + 1
        }
        if (lastDirPos > 0)
            newPath = oldPath.substring(0, lastDirPos)
        return newPath
    }

    private fun showTextDialog(text: String) {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle(resources.getString(R.string.menuInfo, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
        alertDialog.setMessage(text)
        alertDialog.show()
    }

    private class FileTask internal constructor(context: FileManager) : AsyncTask<String, Void, Array<String>>() {

        private val activityReference: WeakReference<FileManager> = WeakReference(context)
        internal val extensionPgnd = ".pgnd"
        private var fileControl: Int = 0

        override fun doInBackground(vararg args: String): Array<String>? {
            val activity = activityReference.get()
            val path = args[0]
            val file = args[1]
            if (file.endsWith(extensionPgnd)) {
                fileControl = 1    // game list
                activity?.pg!!.getGamesFromFile(path, file)
                return activity.pg!!.getGameListFromGames(path, file)
            } else {
                fileControl = 2    // file list
                val fileA: Array<String>?
                var fileB: Array<String>
                var f = File(path)
                if (f.isDirectory) {
                    try {
                        fileA = f.list()
                        var cntFileOK = 0
                        for (i in fileA.indices) {
                            f = File(path + fileA[i])
                            if (!fileA[i].startsWith(".") and (f.isDirectory or fileA[i].endsWith(extensionPgnd)))
                                cntFileOK++
                        }
                        fileB = Array(cntFileOK) {""}
                        var x = 0
                        for (i in fileA.indices) {
                            f = File(path + fileA[i])
                            if (!fileA[i].startsWith(".") and (f.isDirectory or fileA[i].endsWith(extensionPgnd))) {
                                fileB[x] = fileA[i]
                                x++
                            }
                        }
                        val tempList = mutableListOf(*fileB)
                        tempList.sort()
                        Collections.sort(tempList, SortIgnoreCase())
                        fileB = tempList.toTypedArray()
                        return fileB
                    }
                    catch (e: IllegalStateException) { }
                    catch (e: SecurityException) { }
                    catch (e: NullPointerException) { }
                }
                return null
            }

        }

        inner class SortIgnoreCase : Comparator<Any> {
            override fun compare(o1: Any, o2: Any): Int {
                val s1 = o1 as String
                val s2 = o2 as String
                return s1.toLowerCase().compareTo(s2.toLowerCase())
            }
        }

        override fun onPostExecute(list: Array<String>?) {

            val activity = activityReference.get()
            if (activity == null || activity.isFinishing) return

            activity.tvEmpty.visibility = TextView.INVISIBLE
            activity.tvEmpty.text = activity.getString(R.string.noResults)
            if (list != null) {
                if (activity.fileActionCode == FILE_MANAGER_LOAD_REQUEST_CODE)
                    activity.title = activity.getString(R.string.fileLoad) + activity.getGameValues(false)
                if (activity.fileActionCode == FILE_MANAGER_SAVE_REQUEST_CODE)
                    activity.title = activity.getString(R.string.fileSave)
                activity.isInitLayout = false
                when (fileControl) {
                    1 -> {
                        activity.gameCounter = list.size
                        activity.nextSavePosition = list.size
                        var position = activity.filePrefs.getInt("gamePosition", MAX_GAMES)
                        if ((activity.filePrefs.getInt("gamePosition", MAX_GAMES) == MAX_GAMES)
                                or (position >= list.size) or (activity.fileActionCode == FILE_MANAGER_SAVE_REQUEST_CODE))
                            position = list.size - 1
                        activity.games = ArrayAdapter(activity, R.layout.listitemgames, list)
                        activity.lvGames!!.adapter = activity.games
                        if (activity.fileActionCode == FILE_MANAGER_LOAD_REQUEST_CODE) {
                            activity.lvGames!!.onItemClickListener = activity
                            activity.lvGames!!.performItemClick(activity.lvGames, position, activity.lvGames!!.getItemIdAtPosition(position))
                            activity.btnList.visibility = ImageView.VISIBLE
                            activity.tvSaveGame.visibility = TextView.INVISIBLE
                        } else {
                            activity.lvGames!!.isEnabled = false
                            activity.btnList.visibility = ImageView.INVISIBLE
                            if (!activity.isFinish)
                                activity.tvSaveGame.visibility = TextView.VISIBLE
                            if (activity.gameData != "")
                                activity.pg = Pgnd(activity.gameData!!)
                            val str = String.format("%d", (activity.nextSavePosition + 1)) + activity.getGameValues(true)
                            activity.tvSaveGame.text = str
                        }
                        activity.lvGames!!.setSelection(position)
                        activity.lvGames!!.isTextFilterEnabled = true
                        activity.lvGames!!.visibility = ListView.VISIBLE
                        activity.lvFiles.visibility = ListView.INVISIBLE
                        activity.showFiles = false
                    }
                    2 -> {
                        activity.lvFiles.onItemClickListener = activity
                        activity.files = ArrayAdapter(activity, R.layout.listitem, list)
                        activity.lvFiles.adapter = activity.files
                        activity.lvFiles.isTextFilterEnabled = true
                        activity.lvFiles.visibility = ListView.VISIBLE
                        activity.lvGames!!.visibility = ListView.INVISIBLE
                        activity.btnList.visibility = ImageView.INVISIBLE
                        val ed = activity.filePrefs.edit()
                        ed.putInt("gamePosition", MAX_GAMES)
                        ed.apply()
                        activity.showFiles = true
                    }
                }
                if (list.isEmpty())
                    activity.tvEmpty.visibility = TextView.VISIBLE
            } else {
                activity.tvEmpty.visibility = TextView.VISIBLE
            }
            if ((activity.fileActionCode == FILE_MANAGER_SAVE_REQUEST_CODE) and activity.isFinish) {
                val handler = Handler()
                val task = object : Runnable {
                    override fun run() {
                        if (activity.cntDelayed >= 1)
                            stop()
                        activity.cntDelayed++
                        handler.postDelayed(this, HANDLER_DELAY.toLong())
                    }
                }
                handler.postDelayed(task, HANDLER_DELAY.toLong())
            }
        }

        fun stop() {
            return
        }
    }

    companion object {
        const val TAG = "FileManager"
        const val EXTENSION_PGND = ".pgnd"
        const val START_FOLDER = "escalero"
        const val START_FILE = "myGames.pgnd"
        const val FILE_MANAGER_LOAD_REQUEST_CODE = 1
        const val FILE_MANAGER_SAVE_REQUEST_CODE = 2
        const val MAX_GAMES = 50
        const val HANDLER_DELAY = 500
    }

}
