/*
    Escalero - An Android dice program.
    Copyright (C) 2016-2021 Karl Schreiner, c4akarl@gmail.com

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

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.media.AudioManager
import android.media.SoundPool
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.androidheads.vienna.engine.Engine
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : Activity(), View.OnTouchListener {

    private fun loadRewardedVideoAd() {

        var adsId = resources.getString(R.string.adMobVideoId)
        if (BuildConfig.DEBUG)
            adsId = "ca-app-pub-3940256099942544/5224354917"
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(this,adsId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {

//                Log.d(TAG, "loadRewardedVideoAd(), onAdFailedToLoad(), error: $adError?.message")

                mRewardedAd = null
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {

//                Log.d(TAG, "loadRewardedVideoAd(), onAdLoaded()")

                mRewardedAd = rewardedAd
            }
        })

    }


    private val position: String
        get() {
            if (!ed.isNewGame) {
                var colValues = "\n"
                colValues = colValues + ed.getPlayerEngineValues('A', 0) + "\n"
                colValues = colValues + ed.getPlayerEngineValues('A', 1) + "\n"
                colValues = colValues + ed.getPlayerEngineValues('A', 2) + "\n"
                colValues = colValues + ed.getPlayerEngineValues('B', 0) + "\n"
                colValues = colValues + ed.getPlayerEngineValues('B', 1) + "\n"
                colValues = colValues + ed.getPlayerEngineValues('B', 2) + "\n"
                colValues = colValues + ed.getPlayerEngineValues('C', 0) + "\n"
                colValues = colValues + ed.getPlayerEngineValues('C', 1) + "\n"
                colValues = colValues + ed.getPlayerEngineValues('C', 2) + "\n"

                return colValues

            }

            return ""

        }

    var mUpdateAnimationDices: Runnable = Runnable {

//        Log.i(TAG, "mUpdateAnimationDices, isInitRound: $isInitRound")

        if (isDiceBoard) {
            animationDiceBoard(isInitRound)     // board
        }
        else
            animation(isInitRound)              // 2D
    }

    private var mUpdateEngine: Runnable = object : Runnable {
        override fun run() {

//            Log.i(TAG, "mUpdateEngine, engineCommand: " + engineCommand + ", engine: " + engine)

            if (engine == null)
                return
            if (ed.isGameOver)
                return
            if (prefs.getBoolean("playOnline", false))
                return

            val strSp = engineCommand.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (i in strSp.indices) {
                var result = engine!!.getResultFromEngine(strSp[i])
                result = checkEngineResult(strSp[i], result)
                val strResult = result.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (result.startsWith("dice ")) {
                    if (result.contains("r:")) {
                        val player = strResult[1][0]
                        if (player == ed.playerToMove) {
                            setRollHoldFromEngineCommands(result)
                            diceAction(diceRoll, diceHold, true)
                        }
                    } else
                    // with double values
                    {
                        if (result.contains("h1:") and (strResult.size == 4)) {
                            val dbl = strResult[2].replace("h1:", "")
                            if (isDouble1) {
                                for (j in diceRoll.indices) {
                                    var d = -1
                                    if (Character.isDigit(dbl[j]))
                                        d = Character.getNumericValue(dbl[j])
                                    diceDouble1[j] = d
                                }
                                diceMode = 1
                                isDouble1 = false
                                setBtnPlayer(ed.playerToMove)
                                diceView(diceRoll, diceHold)
                            }
                            diceAction(diceRoll, diceHold, true)
                        }
                    }
                }
                if (result.startsWith("entry ") and (strResult.size == 5)) {
                    // check entry values from engine!!!
                    val player = strResult[1][0]
                    val col = Integer.parseInt(strResult[2])
                    val row = Integer.parseInt(strResult[3])
                    val value = Integer.parseInt(strResult[4])
                    val isUpdated = ed.setPlayerResult(player, col, row, value)
                    if (isUpdated) {
                        ed.selectedGridItem = ed.getGridItemPosition(player, col, row)
                        updateTable(true)

                        if (!isOnlineActive)
                            ed.setPlayerToMove(getEngineName(isEnginePlayer(ed.nextPlayerToMove)))

                        diceMode =
                            if (isEnginePlayer(ed.playerToMove))
                                1
                            else
                                5
                        setBtnPlayer(ed.playerToMove)
                        performEngineEntryCommand("entry $player $col $row $value")
                        if (ed.isGameOver)
                            showAccountingDialog(resources.getString(R.string.gameOver) + "\n" + ed.accounting)
                        else {
                            if (isDiceBoard)
                                setToPrev()
                            if (isEnginePlayer(ed.playerToMove)) {
                                diceMode = 1
                                isDouble1 = true
                                diceView(diceRoll, diceHold)
                                handlerEngine.removeCallbacks(this)

                                if (!stopEngine)
                                    diceAction(diceRoll, diceHold, true)
                                else {
                                    stopEngine = false
                                    handlerAnimationDices.removeCallbacks(mUpdateAnimationDices)
                                    handlerEngine.removeCallbacks(this)
                                    engineIsRunning = false
                                    isDicing = false
                                }
                            } else {
                                diceMode = 5
                                if (isDiceBoard) {
                                    diceView(diceRoll, diceHold)
                                    animationDiceBoard(false)
                                } else
                                    diceView(diceRoll, diceHold)
                            }
                            stopEngine = false
                        }

                        if (!prefs.getBoolean("playOnline", false))
                            setRunPrefs()

                    }
                }
            }
        }
    }

    private lateinit var tableView: TableView

    private lateinit var rl1: RelativeLayout
    private lateinit var radio0: ImageView
    private lateinit var radio1: ImageView
    private lateinit var radio2: ImageView
    private lateinit var radio3: ImageView
    private lateinit var radio4: ImageView
    private lateinit var radio5: ImageView

    private lateinit var rl2: RelativeLayout
    private lateinit var diceD1: ImageView
    private lateinit var diceD2: ImageView
    private lateinit var diceD3: ImageView
    private lateinit var diceD4: ImageView
    private lateinit var diceD5: ImageView
    private lateinit var doubleA: ImageView
    private lateinit var diceA1: ImageView
    private lateinit var diceA2: ImageView
    private lateinit var diceA3: ImageView
    private lateinit var diceA4: ImageView
    private lateinit var diceA5: ImageView

    private lateinit var rl3: RelativeLayout
    private lateinit var diceBoard: BoardView

    private lateinit var btnDice: ImageView
    private lateinit var btnPlayerIcon: ImageView
    private lateinit var btnPlayerRound: TextView
    private lateinit var btnPlayerName: TextView
    private lateinit var btnPlayerInfo: TextView
    private lateinit var btnPlayerInfo2: TextView
    private lateinit var btnPlayerResult: TextView
    private lateinit var txtPlayers: TextView
    private lateinit var txtPlayerEps: TextView
    private lateinit var btnMenu: ImageView

    internal var orientation: Int = 0
    private var isConfigurationChanged = false
    private var isOrientationReverse = false
    private var isInitBoard = false
    private var isInitRound = true
    private var isInitDelay = false
    private lateinit var preferencesIntent: Intent
    private lateinit var newGameIntent: Intent
    private lateinit var infoIntent: Intent
    private var notificationIntent: Intent? = null
    private var notificationNoDialog = false

    internal lateinit var prefs: SharedPreferences
    private lateinit var runPrefs: SharedPreferences

    internal lateinit var ed: EscaleroData
    internal var engine: Engine? = null
    internal var engineCommand = ""
    private var engineEntryCommand = ""
    private var gridPosition = 0

    private var diceState = 0  // 0=enter result, 1=dice
    internal var isDiceBoard = true    // true: dice board, false: 2D
    private var initRollValues = true
    private var selectedCol = -1
    private var selectedRow = -1
    private var radioSelected = 3
    private var displayWidth: Int = 0
    private var displayHeight: Int = 0
    private var displayDensity = 1.0f
    private val rand = Random()

    private var flipMode = 0  // 0=no flip action, 1=ready for flip(touch button: btnDice), 2=is flipped(touch button: btnCancel)

    internal var diceMode = 0  // 0=new game, 1=first try, 2=second try, 3=third try, 4=enter result, 5=cancel result, 8=confirm result(online)
    internal var diceRoll = IntArray(5)
    internal var diceHold = IntArray(5)
    private var diceDouble1 = IntArray(5)
    private var diceModePrev = 0
    private var diceModeCheckRoll = -1
    private var diceRollPrev = IntArray(5)
    private var diceHoldPrev = IntArray(5)
    private var diceValues = IntArray(6)
    private var diceValuesDouble1 = IntArray(6)

    private var dbHold = IntArray(5)              // [index from diceHold]
    private var dbRollValues = Array(5) { IntArray(3) }   // [index from diceRoll][x,y,rotation]

    var handlerAnimationDices = Handler(Looper.getMainLooper())
    var handlerEngine = Handler(Looper.getMainLooper())
    internal var engineIsRunning = false
    internal var stopEngine = false
    private var startAnimationTime: Long = 0
    private var endAnimationTime: Long = 0
    private var diceTime = LongArray(5)
    private var diceAnimate = IntArray(5)

    private var startDelayTime: Long = 0

    private var isDicing = false
    private var isDiced = false
    private var isServed = false
    private var isServedDouble1 = false
    private var isDouble1 = false
    private var isDouble1Selected = false

    private var mSoundPool: SoundPool? = null
    private lateinit var soundsMap: HashMap<Int, Int>

    private var mRewardedAd: RewardedAd? = null
    private var startAds = 0L

    //ONLINE - variables
    private var mGoogleSignInClient: GoogleSignInClient? = null
    private lateinit var mAuthFirebase: FirebaseAuth
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var refQuickMatch: DatabaseReference
    private var refQuickMatchListener: ValueEventListener? = null
    private var quickMatchUpdateTime = 0L

    private var refMatch: DatabaseReference? = null
    private var refMatchListener: ValueEventListener? = null
    private var showOnlineTime = 1             // 0 = non, 1 = show time

    private lateinit var myReceiver: BroadcastReceiver

    private var isOnlineActive = false
    private var isDoingTurn = false
    private var isUpdating = false
    private var isOnlineEntry = false
    private var isRematchAccepted = false
    private var isUpdateError = false
    private var mPlayerId: String? = null
    private var mPlayerABC: String? = null
    private var mPlayerName: String? = null
    private var mPlayerEp = -1L      // Escalero Points
    private var mPlayerIconImageUri: String? = null

    private var mOpponentId: String? = null
    private var mOpponentABC: String? = null
    private var mOpponentName: String? = null
    private var mOpponentEp = -1L      // Escalero Points
    private var mOpponentAppVersionCode: Long? = null

    private var mMatchId: String? = null
    private var mMatchBaseData: MatchBaseData? = null
    private var mMatchUpdateData: MatchUpdateData? = null
    private var mTimestampPre = 0L
    private val mTimerQuickMatch = Timer("schedule", true)

    private var mIsSignIn: Boolean = false
    private var mIsVersionChecked: Boolean = false
    private var mIsContinueMatch: Boolean = false
    private var mCurrentLang: String? = null

    private var mAccountingDialog: AlertDialog? = null
    private lateinit var dialogMain: Dialog

    private lateinit var dialogMatch: Dialog
    private lateinit var matchList: ArrayList<MatchList>
    private lateinit var matchDeletedList: ArrayList<MatchList>
    private var matchCheckAll = false

    private lateinit var dialogPlayerQuery: Dialog
    private lateinit var userListArray: ArrayList<UserList>

    private lateinit var dialogPlayOnline: Dialog
    private lateinit var dialogPlayerName: Dialog
    private lateinit var dialogCurrentMatch: Dialog
    private lateinit var dialogInfo: Dialog
    private lateinit var dialogTwoBtn: Dialog
    private lateinit var dialogQuickMatchInvitation: Dialog
    private lateinit var dialogRematch: Dialog

    private fun appInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcesses = activityManager.runningAppProcesses ?: return false
        return runningAppProcesses.any { it.processName == context.packageName && it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }
    }

    private suspend fun isLatestVersionCode(): Boolean = suspendCoroutine { continuation ->
        if (BuildConfig.DEBUG) {
            continuation.resume(true)
        } else {
            val ref = FirebaseDatabase.getInstance().getReference("/versionCode")
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val manager = packageManager
                    val info = manager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                    val currentVersionCode = snapshot.value.toString()
                    @Suppress("DEPRECATION")
                    if (info.versionCode.toString() < currentVersionCode) {

//                        Log.d(TAG, "isLatestVersionCode(): info.versionCode: ${info.versionCode}, currentVersionCode: $currentVersionCode")

                       try { continuation.resume(false) }
                       catch (e: IllegalStateException) { }

                    } else {

//                        Log.d(TAG, "isLatestVersionCode() >= $currentVersionCode")

                        try {
                            continuation.resume(true)
                        } catch (e: IllegalStateException) {
                        }
                    }
                    ref.removeEventListener(this)
                }

                override fun onCancelled(error: DatabaseError) {

//                    Log.d(TAG, "isLatestVersionCode(): error: $error")

                    continuation.resume(true)
                    ref.removeEventListener(this)
                }
            })
        }
    }

    private suspend fun signIn(): GoogleSignInAccount? = suspendCoroutine { continuation ->
        mGoogleSignInClient!!.silentSignIn().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {

//                Log.d(TAG, "signIn(): success")

                continuation.resume(task.result)

            } else {

//                Log.d(TAG, "signIn(): failure", task.exception)

                continuation.resume(null)

            }
        }
    }

    private suspend fun updateMatchData(matchId: String?, matchBaseData: MatchBaseData?, matchUpdateData: MatchUpdateData?, serverTimestamp: Long): Boolean = suspendCoroutine { continuation ->
        mMatchId = matchId
        val ref = FirebaseDatabase.getInstance().getReference("matches")
        if (matchId == null)
            mMatchId = ref.push().key
        if (mMatchId != null) {
            if (matchBaseData != null)
                ref.child(mMatchId!!).child("baseData").setValue(matchBaseData)
            if (matchUpdateData != null)
                ref.child(mMatchId!!).child("updateData").setValue(matchUpdateData)
            ref.child(mMatchId!!).child("timestamp").setValue(serverTimestamp)
                .addOnSuccessListener {
                    continuation.resume(true)
                }
                .addOnFailureListener {
                    continuation.resume(false)
                }
        }
        else {
            continuation.resume(false)
        }
    }

    private suspend fun getMatchTimestamp(matchId: String): String = suspendCoroutine { continuation ->
        val ref = FirebaseDatabase.getInstance().getReference("matches")
        ref.child(matchId).child("timestamp")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    try {
                        continuation.resume(dataSnapshot.value.toString())
                    } catch (e: IllegalStateException) {
                    }
                    ref.removeEventListener(this)
                }

                override fun onCancelled(p0: DatabaseError) {
                    continuation.resume("0")
                    ref.removeEventListener(this)
                }
            })
    }

    private suspend fun getMatchBaseData(matchId: String): MatchBaseData? = suspendCoroutine { continuation ->
        val ref = FirebaseDatabase.getInstance().getReference("matches")
        ref.child(matchId).child("baseData")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    try {
                        continuation.resume(dataSnapshot.getValue(MatchBaseData::class.java))
                    } catch (e: IllegalStateException) {
                    }
                    ref.removeEventListener(this)
                }

                override fun onCancelled(p0: DatabaseError) {
                    continuation.resume(null)
                    ref.removeEventListener(this)
                }
            })
    }

    private suspend fun getMatchUpdateData(matchId: String): MatchUpdateData? = suspendCoroutine { continuation ->
        val ref = FirebaseDatabase.getInstance().getReference("matches")
        ref.child(matchId).child("updateData")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    try {
                        continuation.resume(dataSnapshot.getValue(MatchUpdateData::class.java))
                    } catch (e: IllegalStateException) {
                    }
                    ref.removeEventListener(this)
                }

                override fun onCancelled(p0: DatabaseError) {
                    continuation.resume(null)
                    ref.removeEventListener(this)
                }
            })
    }

    private suspend fun cancelMatch(deleteMatch: Boolean, matchId: String, playerIdA: String?, playerIdB: String?): Boolean = suspendCoroutine { continuation ->

//        Log.i(TAG, "cancelMatch(), matchId: $matchId, playerIdA: $playerIdA, playerIdB: $playerIdB")

        if (matchId.isEmpty())
            continuation.resume(true)

        if (playerIdA != null) {
            val refPlayer = FirebaseDatabase.getInstance().getReference("userMatches/$playerIdA/$matchId")
            refPlayer.removeValue()
        }
        if (playerIdB != null) {
            val refOpponent = FirebaseDatabase.getInstance().getReference("userMatches/$playerIdB/$matchId")
            refOpponent.removeValue()
        }

        if (deleteMatch && matchId != "") {
            val ref = FirebaseDatabase.getInstance().getReference("matches/$matchId")
            ref.removeValue()
                    .addOnSuccessListener {
                        continuation.resume(true)
                    }
                    .addOnFailureListener {
                        continuation.resume(false)
                    }
        }
        else
            continuation.resume(true)

    }

    private suspend fun matchUpdateListener(matchId: String?): String = suspendCoroutine { continuation ->

//        Log.i(TAG, "matchUpdateListener(), matchId: $matchId")

        if (!matchId.isNullOrEmpty()) {
            refMatch = FirebaseDatabase.getInstance().getReference("matches/$matchId")
            refMatch!!.child("timestamp")
            refMatchListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {

                        if (!prefs.getBoolean("playOnline", false)) {
                            refMatch!!.removeEventListener(refMatchListener!!)
                            return
                        }

                        if (snapshot.exists()) {

                            GlobalScope.launch(Dispatchers.Main) {

//                            Log.i(TAG, "matchUpdateListener(), onDataChange(), matchId: $matchId, mIsContinueMatch: $mIsContinueMatch")

                                if (mIsContinueMatch) {
                                    mIsContinueMatch = false
                                    return@launch
                                }

                                val deferredGetMatchTimestamp = async { getMatchTimestamp(matchId) }
                                var timestamp = deferredGetMatchTimestamp.await().toLong()
                                deferredGetMatchTimestamp.cancel()

                                if (timestamp == 0L && mTimestampPre > 0L)
                                    timestamp = mTimestampPre + 100L

                                if (mTimestampPre == 0L && isDoingTurn)
                                    mTimestampPre = timestamp

                                if (timestamp > mTimestampPre) {
                                    mTimestampPre = timestamp

                                    val deferredGetMatchBaseData = async { getMatchBaseData(matchId) }
                                    val checkBaseData = deferredGetMatchBaseData.await()
                                    deferredGetMatchBaseData.cancel()

                                    val deferredGetMatchUpdateData = async { getMatchUpdateData(matchId) }
                                    val checkUpdateData = deferredGetMatchUpdateData.await()
                                    deferredGetMatchUpdateData.cancel()

                                    if (checkUpdateData != null && checkBaseData != null) {

//                                    Log.i(TAG, "matchUpdateListener(), onlineAction: ${checkUpdateData.onlineAction}, myId: $mPlayerId, turnId: ${checkUpdateData.turnPlayerId}")

                                        if (checkUpdateData.onlineAction!!.startsWith(ONLINE_START)) {
                                            updateUserStatus(playerId = mPlayerId, playing = true, singleGame = ed.isSingleGame)
                                        }
                                        if (checkUpdateData.onlineAction!!.endsWith(PAUSED)) {
                                            updateUserStatus(playerId = mPlayerId, playing = false, singleGame = ed.isSingleGame)
                                        }

                                        if (checkUpdateData.onlineAction == ONLINE_GAME_OVER) {
                                            handlerAnimationDices.removeCallbacks(mUpdateAnimationDices)
                                        }

                                        // MY_TURN
                                        if (checkUpdateData.turnPlayerId == mPlayerId) {

                                            isDoingTurn = true

//                                        Log.i(TAG, "matchUpdateListener(), MY_TURN, onlineAction: ${checkUpdateData.onlineAction}")


                                            if (mPlayerId != null) {

//                                            Log.i(TAG, "matchUpdateListener(), userMatches")

                                                val players = "${checkBaseData.nameA!!} - ${checkBaseData.nameB!!}"
                                                var variant = getString(R.string.typeSingle)
                                                if (!checkBaseData.singleGame!!)
                                                    variant = getString(R.string.typeDouble)

                                                val deferredUpdateMatchPlayer = async { addMatchToUserMatches(mPlayerId!!, matchId, players, variant, checkUpdateData.turnPlayerId!!, timestamp.toString()) }
                                                deferredUpdateMatchPlayer.await()
                                                deferredUpdateMatchPlayer.cancel()

                                                var opponent = checkBaseData.playerIdB
                                                if (mPlayerId == checkBaseData.playerIdB)
                                                    opponent = checkBaseData.playerIdA

                                                val deferredUserLanguage = async { getUserLanguage(opponent!!) }
                                                val userLanguage = deferredUserLanguage.await()
                                                deferredUserLanguage.cancel()
                                                variant = getStringByLocal(this@MainActivity, R.string.typeSingle, null, null, userLanguage)
                                                if (!checkBaseData.singleGame!!)
                                                    variant = getStringByLocal(this@MainActivity, R.string.typeDouble, null, null, userLanguage)

//                                            Log.i(TAG, "matchUpdateListener(), userMatches, playerIdA: ${checkMatchBaseData!!.playerIdA}, playerIdB: ${checkMatchBaseData!!.playerIdB}")
//                                            Log.i(TAG, "matchUpdateListener(), userMatches, mPlayerId: $mPlayerId, opponent: $opponent")

                                                val deferredUpdateMatchOpponent = async { addMatchToUserMatches(opponent!!, matchId, players, variant, checkUpdateData.turnPlayerId!!, timestamp.toString()) }
                                                deferredUpdateMatchOpponent.await()
                                                deferredUpdateMatchOpponent.cancel()

//                                            Log.i(TAG, "matchUpdateListener(), addMatchToUserMatches(), mPlayerId: $mPlayerId, opponent: $opponent")

                                            }

                                            matchTurn(checkUpdateData)

                                        }
                                        // THEIR_TURN
                                        else {

                                            isDoingTurn = false
                                            matchUpdate(checkUpdateData)

                                        }
                                    }
                                } else {
                                    if (timestamp == 0L) {
                                        matchDataToDb(false, getPausedStatus())
                                        showInfoDialog(getString(R.string.matchBreak), getString(R.string.dataUpdateProblem), getString(R.string.ok))
                                        diceBoard.mOnlinePlayers = ""
                                        diceBoard.updateBoard(diceRoll, diceHold, diceDouble1, false, 1, initRollValues)
                                    }
                                }
                            }
                        }

                    } catch (e: IllegalStateException) {
                    }
                }

                override fun onCancelled(error: DatabaseError) {

//                Log.i(TAG, "matchUpdateListener(), onCancelled")

                    continuation.resume("")
                    refMatch!!.removeEventListener(this)
                }
            }

            refMatch!!.addValueEventListener(refMatchListener!!)
        }

    }

    private suspend fun quickMatchUpdateListener(): String = suspendCoroutine { continuation ->
        refQuickMatch = FirebaseDatabase.getInstance().getReference("quickMatch")
        refQuickMatchListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val userId = snapshot.value.toString()

//                    Log.i(TAG, "quickMatchUpdateListener(), userId: $userId")

                    if (dialogQuickMatchInvitation.isShowing)
                        dialogQuickMatchInvitation.dismiss()

                    if (!prefs.getBoolean("playOnline", false)) {
                        refQuickMatch.removeEventListener(refQuickMatchListener!!)
                        return
                    }

                    val time = System.currentTimeMillis()
                    if (userId != "0" && userId != mPlayerId && (time - quickMatchUpdateTime) > MIN_UPDATE_TIME) {
                        val title = "${getString(R.string.invitationTitle)}: ${getString(R.string.justNow)}"
                        if (!isOnlineActive) {
                            showInvitationFromQuickMatchDialog(userId, title, getString(R.string.reject), getString(R.string.accept))
                            quickMatchUpdateTime = time
                        }
                    }
                }
                catch (e: IllegalStateException) { continuation.resume("") }
            }
            override fun onCancelled(error: DatabaseError) {

            }
        }

        refQuickMatch.addValueEventListener(refQuickMatchListener!!)

    }

    private suspend fun getQuickMatchUserId(): String = suspendCoroutine { continuation ->
        val ref = FirebaseDatabase.getInstance().getReference("quickMatch")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    continuation.resume(snapshot.value.toString())
                } catch (e: IllegalStateException) {
                }
                ref.removeEventListener(this)
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resume("")
                ref.removeEventListener(this)
            }
        })
    }

    private suspend fun setQuickMatchUserId(userId: String): Boolean = suspendCoroutine { continuation ->
        val ref = FirebaseDatabase.getInstance().getReference("quickMatch")
        ref.setValue(userId)
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener {
                continuation.resume(false)
            }
    }

    private suspend fun updateLeaderboardData(userId: String, leaderboardData: Leaderboard): Boolean = suspendCoroutine { continuation ->
        val ref = FirebaseDatabase.getInstance().getReference("leaderboard")
        ref.child(userId).setValue(leaderboardData)
        ref.child(userId).child("timestamp").setValue(ServerValue.TIMESTAMP)
                .addOnSuccessListener {
                    continuation.resume(true)
                }
                .addOnFailureListener {
                    continuation.resume(false)
                }
    }

    private suspend fun getLeaderboardCount(): Long = suspendCoroutine { continuation ->
        val ref = FirebaseDatabase.getInstance().getReference("leaderboard")
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                continuation.resume(dataSnapshot.childrenCount)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                continuation.resume(0L)
            }
        }
        ref.addListenerForSingleValueEvent(valueEventListener)
    }

    private suspend fun getLeaderboardScore(userId: String): Long = suspendCoroutine { continuation ->
        val ref = FirebaseDatabase.getInstance().getReference("/leaderboard/$userId")
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val lb = dataSnapshot.getValue(Leaderboard::class.java)
                if (dataSnapshot.exists()) {
                    try { continuation.resume(lb!!.score ?: -4) }
                    catch (e: IllegalStateException) { }
                }
                else {
                    try { continuation.resume(-5) }
                    catch (e: IllegalStateException) { }
                }
                ref.removeEventListener(this)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                continuation.resume(-9)
                ref.removeEventListener(this)
            }
        }
        ref.addListenerForSingleValueEvent(valueEventListener)
    }

    private suspend fun getLeaderboardScores(): ArrayList<UserList> = suspendCoroutine { continuation ->

        //karl!!! lbTime = true --> list: Player > 200 days not active
        val lbTime = false

        val timeCheck       =       17280000000     // 200 days
        val timeCheckDay    =       86400000        // 1   day
        var serverTimestamp = 0L

        GlobalScope.launch(Dispatchers.Main) {
            val deferredTimestamp = async { getTimestampFromServer() }
            serverTimestamp = deferredTimestamp.await().toLong()
            deferredTimestamp.cancel()
        }
        var cntTime = 0

        userListArray = ArrayList()
        var place = 0
        val ref = FirebaseDatabase.getInstance().getReference("leaderboard")
        val query = ref
                .orderByChild("score")
                .limitToLast(ONLINE_LEADERBORD_MAX_PLAYERS.toInt())
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                if (lbTime) {

                    Log.i(TAG, "getPlayerQueryArray(), dataSnapshot.count: ${dataSnapshot.children.count()}")
                    Log.i(TAG, "Player > 200 days not active")
                    Log.i(TAG, "----------------------------")

                }

                dataSnapshot.children.reversed().forEach {
                    val leaderboard = it.getValue(Leaderboard::class.java)!!
                    if (leaderboard.name!!.isNotEmpty()) {
                        val lb = UserList()
                        place++
                        lb.info = "$place."
                        lb.playerName = leaderboard.name!!
                        lb.score = leaderboard.score!!
                        lb.playerId = ""
                        lb.scoreTag = ""
                        lb.timestamp = leaderboard.timestamp!!
                        lb.iconImageUri = ""
                        userListArray.add(lb)

                        if (lbTime && leaderboard.score!! > 15L && serverTimestamp != 0L && serverTimestamp - lb.timestamp > timeCheck) {
                            cntTime++

                            val d = (serverTimestamp - lb.timestamp) / timeCheckDay

                            Log.i(TAG, "${leaderboard.uid}, ${leaderboard.name}: ${leaderboard.score}, $d")

                        }

                    }

                }

                if (lbTime) {

                    Log.i(TAG, "----------------------------")
                    Log.i(TAG, "Player > 200 days: $cntTime")

                }

                try {
                    continuation.resume(userListArray)
                } catch (e: IllegalStateException) {
                }
                query.removeEventListener(this)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                continuation.resume(userListArray)
                query.removeEventListener(this)
            }
        })

    }

    private suspend fun firebaseAuthWithGoogleAccount(acct: GoogleSignInAccount?): Boolean = suspendCoroutine { continuation ->

//        Log.d(TAG, "0 firebaseAuthWithGoogleAccount, acct: $acct")

        if (acct != null) {
            mAuthFirebase = FirebaseAuth.getInstance()
            val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
            mAuthFirebase.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {

                        mPlayerId = mAuthFirebase.currentUser!!.uid
                        continuation.resume(true)

                    } else {

//                        Log.d(TAG, "8 !task.isSuccessful")

                        mPlayerId = null
                        continuation.resume(false)

                    }
                }
        }
        else {

//            Log.d(TAG, "9 acct (GoogleSignInAccount) == null")

            mPlayerId = null
            continuation.resume(false)

        }
    }

    private suspend fun updateUserData(userId: String, userData: User): Boolean = suspendCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        userData.token = task.result
                        val ref = FirebaseDatabase.getInstance().getReference("users")
                        ref.child(userId).setValue(userData)
                        ref.child(userId).child("timestamp").setValue(ServerValue.TIMESTAMP)

//                    Log.i(TAG, "updateUserData(), token: ${userData.token}")

                        continuation.resume(true)

                    } else {

//                    Log.i(TAG, "updateUserData(), !task.isSuccessful, ERROR")

                        continuation.resume(false)

                    }
                }
    }

    private suspend fun getUserData(userId: String): User? = suspendCoroutine { continuation ->
        val ref = FirebaseDatabase.getInstance().getReference("users")
        ref.child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        try {
                            continuation.resume(dataSnapshot.getValue(User::class.java))
                        } catch (e: IllegalStateException) {
                            continuation.resume(null)
                        }
                        ref.removeEventListener(this)
                    }

                    override fun onCancelled(p0: DatabaseError) {
                        continuation.resume(null)
                        ref.removeEventListener(this)
                    }
                })
    }

    private suspend fun addMatchToUserMatches(userId: String, matchId: String, players: String, variant: String, turnId: String, matchTimestamp: String): Boolean = suspendCoroutine { continuation ->
        val ref = FirebaseDatabase.getInstance().getReference("userMatches")
        val userMatch = UserMatch()
        userMatch.matchId = matchId
        userMatch.players = players
        userMatch.turnId = turnId
        userMatch.timestamp = matchTimestamp
        userMatch.variant = variant
        ref.child(userId).child(matchId).setValue(userMatch)
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener {
                continuation.resume(false)
            }
    }

    private suspend fun getUserMatches(userId: String): ArrayList<MatchList> = suspendCoroutine { continuation ->
        matchList = ArrayList()
        var serverTimestamp = 0L
        GlobalScope.launch(Dispatchers.Main) {
            val deferredTimestamp = async { getTimestampFromServer() }
            serverTimestamp = deferredTimestamp.await().toLong()
            deferredTimestamp.cancel()
        }
        val ref = FirebaseDatabase.getInstance().getReference("userMatches/$userId")
        val query = ref.orderByChild("timestamp")
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var cnt = 0
                if (dataSnapshot.children.count() > 0) {
                    dataSnapshot.children.reversed().forEach { item ->
                        val um = item.getValue(UserMatch::class.java)
                        if (um != null) {
                            cnt++
                            val ml = MatchList()
                            ml.selected = cnt > ONLINE_MAX_USER_MATCHES
                            ml.matchId = um.matchId!!
                            ml.players = um.players!!
                            if (um.turnId!! == mPlayerId)
                                ml.statusId = 0
                            else
                                ml.statusId = 1
                            ml.status = resources.getString(R.string.yourTurn)
                            if (ml.statusId == 1) {
                                val strResult = ml.players.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                                if (strResult[0] == mPlayerName)
                                    ml.status = resources.getString(R.string.theirTurn, strResult[2])
                                else
                                    ml.status = resources.getString(R.string.theirTurn, strResult[0])
                            }
                            ml.variant =
                                    if (um.variant.isNullOrEmpty())
                                        getString(R.string.typeSingle)
                                    else
                                        um.variant!!

                            if (um.timestamp != null)
                                ml.time = getUpdateTime(um.timestamp!!.toLong(), serverTimestamp)
                            else
                                ml.time = um.timestamp!!

//                            Log.d(TAG, "!!! matchId: ${um.matchId}, players: ${um.players}, time: ${ml.time}, ml.variant: ${ml.variant}")

                            matchList.add(ml)
                        }

                    }
                }
                try {
                    continuation.resume(matchList)
                } catch (e: IllegalStateException) {
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {
                continuation.resume(matchList)
                ref.removeEventListener(this)
            }
        })

    }

    private suspend fun getTimestampFromServer(): String = suspendCoroutine { continuation ->
        val ref = FirebaseDatabase.getInstance().getReference("/timestamp")
        ref.setValue(ServerValue.TIMESTAMP)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    continuation.resume(snapshot.value.toString())
                } catch (e: IllegalStateException) {
                }
                ref.removeEventListener(this)
            }

            override fun onCancelled(error: DatabaseError) {

//                Log.i(TAG, "getTimestampFromServer(), error.code: ${error.code}, mTimestampPre: $mTimestampPre")

                try {
                    if (error.code == DatabaseError.PERMISSION_DENIED) {
                        if (mTimestampPre > 0)
                            continuation.resume((mTimestampPre + 100L).toString())
                        else
                            continuation.resume("0")
                    }
                } catch (e: IllegalStateException) {
                }

            }
        })
    }

    private suspend fun getUserIdFromName(userName: String): String = suspendCoroutine { continuation ->
        val ref = FirebaseDatabase.getInstance().getReference("users")
        val query = ref
                .orderByChild("name")
                .equalTo(userName)
        query.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.children.count() > 0) {
                    dataSnapshot.children.forEach {
                        try {
                            continuation.resume(it.key.toString())
                        } catch (e: IllegalStateException) {
                            continuation.resume("")
                        }
                        query.removeEventListener(this)
                    }
                } else
                    continuation.resume("")
            }

            override fun onCancelled(p0: DatabaseError) {
                continuation.resume("")
                query.removeEventListener(this)
            }

        })
    }

    private suspend fun getUserLanguage(userId: String): String = suspendCoroutine { continuation ->
        val ref = FirebaseDatabase.getInstance().getReference("/users/$userId")
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                try { continuation.resume(user!!.language ?: "") }
                catch (e: IllegalStateException) { }
                ref.removeEventListener(this)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                continuation.resume("")
                ref.removeEventListener(this)
            }
        }
        ref.addListenerForSingleValueEvent(valueEventListener)
    }

    private suspend fun getPlayerQueryArray(isOnline: Boolean): ArrayList<UserList> = suspendCoroutine { continuation ->
        var timestamp = 0L
        var cnt = 0
        GlobalScope.launch(Dispatchers.Main) {
            val deferredTimestamp = async { getTimestampFromServer() }
            timestamp = deferredTimestamp.await().toLong()
            deferredTimestamp.cancel()
        }
        userListArray = ArrayList()
        val ref = FirebaseDatabase.getInstance().reference

        // timestamp desc, max 100, sortMax 50
        val query = ref.child("users")
                .orderByChild("timestamp")
                .limitToLast(100)
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

//                Log.i(TAG, "getPlayerQueryArray(), isOnline: $isOnline, dataSnapshot.count: ${dataSnapshot.children.count()}")

                    dataSnapshot.children.reversed().forEach {
                        val userData = it.getValue(User::class.java)!!
                        val timestampUser = userData.timestamp ?: 0L
                        val timeInMinutes = (timestamp - timestampUser) / 60000
                        val timeInHours = (timestamp - timestampUser) / 3600000
                        val timeInDays = (timestamp - timestampUser) / 86400000

//                    Log.i(TAG, "getPlayerQueryArray(), timestampUser: $timestampUser, ${userData.online}, ${userData.info}, ${timeInHours}h")

                        if (cnt < 50) {
                            val lb = UserList()
                            lb.playerName = userData.name ?: ""
                            lb.playerId = userData.uid ?: ""
                            lb.info = ""    // showOnlineTime == 0

                            lb.score = -99      // set to "" in UserListAdapter

                            lb.iconImageUri = userData.iconImageUri ?: ""
                            if (!isOnline) {
                                if (BuildConfig.DEBUG && showOnlineTime == 1) {
                                    if (timeInHours == 0L)
                                        lb.info = "${timeInMinutes}m"
                                    else {
                                        if (timeInHours < 96)
                                            lb.info = "${timeInHours}h"
                                        else
                                            lb.info = "${timeInDays}d"
                                    }
                                }
                                userListArray.add(lb)
                            } else {
                                if (userData.online == true && timeInHours == 0L) {
                                    if (BuildConfig.DEBUG && showOnlineTime == 1)
                                        lb.info = "${timeInMinutes}m"
                                    userListArray.add(lb)
                                }
                            }
                            cnt++
                        }

                    }
                    try {
                        continuation.resume(userListArray)
                    } catch (e: IllegalStateException) {
                    }
                    query.removeEventListener(this)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    continuation.resume(userListArray)
                    query.removeEventListener(this)
                }
            })

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        Log.i(TAG, "start onCreate()")

        val configuration = resources.configuration

        if (configuration.fontScale > 1f) {

//            Log.i(TAG, "onCreate(), ${configuration.fontScale}")

            configuration.fontScale = 1f
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(metrics)
            metrics.scaledDensity = configuration.fontScale * metrics.density
            @Suppress("DEPRECATION")
            baseContext.resources.updateConfiguration(configuration, metrics)
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            val channelId = getString(R.string.default_notification_channel_id)
            val channelName = getString(R.string.invitations)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_HIGH))
        }

        notificationIntent = null
        intent.extras?.let {

//            Log.d(TAG, "onCreate(), notification, intent.extras")
//            Log.d(TAG, "onCreate(), notification, intent.action: ${intent.action}")
//            Log.d(TAG, "onCreate(), notification, intent.flags: ${intent.flags}")

            if (intent.flags == Intent.FLAG_ACTIVITY_CLEAR_TOP + Intent.FLAG_ACTIVITY_NEW_TASK)
            {

//                Log.d(TAG, "onCreate(), intent.flags: ${(Intent.FLAG_ACTIVITY_CLEAR_TOP + Intent.FLAG_ACTIVITY_NEW_TASK)}")

                notificationIntent = intent
                prefs = getSharedPreferences("prefs", 0)
                val edi = prefs.edit()
                edi.putBoolean("playOnline", true)
                edi.apply()

            }

        }

        dialogMain = Dialog(this)
        dialogMain.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogMain.setContentView(R.layout.dialogmain)

        dialogMatch = Dialog(this, android.R.style.Theme_Light)
        dialogMatch.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogMatch.setContentView(R.layout.dialogmatch)

        dialogPlayerQuery = Dialog(this, android.R.style.Theme_Light)
        dialogPlayerQuery.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogPlayerQuery.setContentView(R.layout.dialogplayerquery)

        dialogPlayOnline = Dialog(this)
        dialogPlayOnline.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogPlayOnline.setContentView(R.layout.dialogplayonline)

        dialogPlayerName = Dialog(this)
        dialogPlayerName.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogPlayerName.setContentView(R.layout.dialogplayername)

        dialogCurrentMatch = Dialog(this)
        dialogCurrentMatch.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogCurrentMatch.setContentView(R.layout.dialogcurrentmatch)

        dialogInfo = Dialog(this)
        dialogInfo.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogInfo.setContentView(R.layout.dialoginfo)

        dialogTwoBtn = Dialog(this)
        dialogTwoBtn.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogTwoBtn.setContentView(R.layout.dialogtwobtn)

        dialogQuickMatchInvitation = Dialog(this)
        dialogQuickMatchInvitation.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogQuickMatchInvitation.setContentView(R.layout.dialogtwobtn)

        dialogRematch = Dialog(this)
        dialogRematch.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogRematch.setContentView(R.layout.dialogrematch)

        setUI()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        mAuthFirebase = FirebaseAuth.getInstance()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        loadRewardedVideoAd()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mSoundPool = SoundPool.Builder()
                    .setMaxStreams(2)
                    .build()
        } else {
            @Suppress("DEPRECATION")
            mSoundPool = SoundPool(2, AudioManager.STREAM_MUSIC, 100)
        }

        soundsMap = HashMap()
        soundsMap[1] = mSoundPool!!.load(this, R.raw.move_wrong, 1)
        soundsMap[2] = mSoundPool!!.load(this, R.raw.dice_shake, 1)
        soundsMap[3] = mSoundPool!!.load(this, R.raw.dice_roll, 1)
        soundsMap[4] = mSoundPool!!.load(this, R.raw.new_game, 1)
        soundsMap[5] = mSoundPool!!.load(this, R.raw.no_connection, 1)

        if (runPrefs.getInt("firstAppEntry", 0) == 0) {
            val edi = runPrefs.edit()
            edi.putInt("firstAppEntry", 1)
            edi.apply()
            showWelcomeDialog()
        }

//        Log.i(TAG, "end onCreate()")

    }

    @RequiresApi(Build.VERSION_CODES.N)
    public override fun onResume() {
        super.onResume()
        engineIsRunning = false
        initRollValues = true
        if (isEnginePlayer(ed.playerToMove) and (diceMode >= 4)) {
            if (!ed.isSingleGame and isDouble1) {
                isDouble1 = false
                setDiceDouble1()
                setDiceValues(true)
            }
            if (ed.selectedGridItem >= 0)
                diceMode = 5
        }

//        Log.i(TAG, "onResume(), ed.playerToMove: " + ed.playerToMove + ", isDiceBoard: " + isDiceBoard)

        if (diceA1.height > 0) {
            val h = diceA1.height / 2 // 50%
            btnPlayerInfo.layoutParams.height = h
            btnPlayerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, h * 0.8f)
            btnPlayerInfo.gravity = Gravity.CENTER or Gravity.BOTTOM
            btnPlayerInfo2.layoutParams.height = h
            btnPlayerInfo2.setTextSize(TypedValue.COMPLEX_UNIT_PX, h * 0.8f)
            btnPlayerInfo2.gravity = Gravity.CENTER or Gravity.BOTTOM
        }

        if (isDiceBoard && mMatchId != null) {
            isInitBoard = true
            animationDiceBoard(false)
        }

//        Log.i(TAG, "onResume(), isAppStart: " + runPrefs.getBoolean("isAppStart", true))

        // latest app version code?
        GlobalScope.launch(Dispatchers.Main) {
            if (!mIsVersionChecked) {
                val deferredIsLastVersionCode = async { isLatestVersionCode() }
                val isLastVersionCode = deferredIsLastVersionCode.await()
                deferredIsLastVersionCode.cancel()
                if (!isLastVersionCode) {
                    showUpdateAppDialog(getString(R.string.notUpToDate), getString(R.string.matchUpdate) + "?",
                            getString(R.string.dialogLater), getString(R.string.dialogYes))
                }
                mIsVersionChecked = true
            }
        }

        if (runPrefs.getBoolean("isAppStart", true)) {
            if (prefs.getBoolean("playOnline", false))
                playOnline(runPrefs.getBoolean("isAppStart", true))
            else
                playOffline(false)
            setAppStart(false)
        }
        else {
            if (prefs.getBoolean("playOnline", false)) {
                signInSilently()
            }
        }

        mCurrentLang = Locale.getDefault().language

//        Log.i(TAG, "onResume(), mCurrentLang: $mCurrentLang")

        myReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                val actionId = intent.extras?.getString("actionId")?: "null"
                val matchId = intent.extras?.getString("matchId")?: ""
                val fromUsername = intent.extras?.getString("fromUsername")?: ""
                val fromUserId = intent.extras?.getString("fromUserId")?: ""
                val tsIntent = intent.extras?.getString("timestamp")?.toLong() ?: 0L

//                Log.i(TAG, "onResume(), myReceiver(), actionId: $actionId, matchId: $matchId, fromUsername: $fromUsername")

                if (prefs.getBoolean("playOnline", false) && actionId != "null") {
                    if (isOnlineActive && !(actionId == "8" || actionId == "9")) {

//                        Log.i(TAG, "onResume(), myReceiver(), !!! isOnlineActive !!!, actionId: $actionId, matchId: $matchId, fromUserId: $fromUserId")

                        GlobalScope.launch(Dispatchers.Main) {
                            val deferredTimestamp = async { getTimestampFromServer() }
                            val tsServer = deferredTimestamp.await().toLong()
                            deferredTimestamp.cancel()
                            when (actionId) {
                                "7" -> {
                                    initOnline()
                                    setNoActiveMatch()
                                    if (tsServer - tsIntent <= NOTIFICATION_DELAY)
                                        showInfoDialog(getString(R.string.info), getString(R.string.playerNotAvailable, fromUsername), getString(R.string.ok))
                                    startRemoveMatch(true, matchId, null, null)
                                }
                                else -> {
                                    if (tsServer - tsIntent <= NOTIFICATION_DELAY)
                                        invitationNotAvailable(fromUserId, matchId)
                                }
                            }
                        }

                    }
                    else {
                        when (actionId) {
                            "", "1" -> showInvitationFromNotificationDialog(intent)
                            "2" -> startOnlineMatch(matchId)
                            "3", "4" -> showContinueMatchDialog(matchId, actionId)
                            "8", "9" -> {
                                if (mMatchId != null) {
                                    if (mMatchId == matchId) {
                                        updateUserStatus(playerId = mPlayerId, playing = false, singleGame = ed.isSingleGame)
                                        initOnline()
                                        setNoActiveMatch()
                                        when (actionId) {
                                            "8" -> showInfoDialog(getString(R.string.info), getString(R.string.gameOver), getString(R.string.ok))
                                            "9" -> showInfoDialog(getString(R.string.info), getString(R.string.matchDeletedByPlayer, fromUsername), getString(R.string.ok))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else {

                    showOfflineInvitationFromNotificationDialog(intent)

                }
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver, IntentFilter(BROADCAST_PUSH_NOTIFICATION))

//        Log.d(TAG, "onResume(), notification, intent.extras: + ${intent.extras}")

        if (notificationIntent == null) {
            intent.extras?.let {

//                Log.d(TAG, "onResume(), notification, intent.extras")
//                Log.d(TAG, "onResume(), notification, intent.action: ${intent.action}")
//                Log.d(TAG, "onResume(), notification, intent.flags: ${intent.flags}")
//                Log.d(TAG, "onResume(), Intent.FLAG_ACTIVITY_CLEAR_TOP: ${Intent.FLAG_ACTIVITY_CLEAR_TOP}")
//                Log.d(TAG, "onResume(), Intent.FLAG_ACTIVITY_NEW_TASK: ${Intent.FLAG_ACTIVITY_NEW_TASK}")

                //karl Intent.FLAG ???
//                if (intent.flags == Intent.FLAG_ACTIVITY_CLEAR_TOP + Intent.FLAG_ACTIVITY_NEW_TASK) {
                if (intent.flags == 339738624) {

//                    Log.d(TAG, "onResume(), intent.flags == 339738624")

                    notificationIntent = intent

                    prefs = getSharedPreferences("prefs", 0)
                    val edi = prefs.edit()
                    edi.putBoolean("playOnline", true)
                    edi.apply()

                }
            }
        }

    }

    private fun setAppStart(isAppStart: Boolean) {
        val edi = runPrefs.edit()
        edi.putBoolean("isAppStart", isAppStart)
        edi.apply()
    }

    public override fun onPause() {
        super.onPause()

//        Log.i(TAG, "onPause(), isGameOver: " + ed.isGameOver)

        if (!prefs.getBoolean("playOnline", false))
            setRunPrefs()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver)

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

//        Log.i(TAG, "onConfigurationChanged(), orientation: " + orientation + ", newConfig.orientation: " + newConfig.orientation)

        orientation = newConfig.orientation
        isConfigurationChanged = true

        if (dialogMain.isShowing) {
            setAppStart(true)
        }

        setRunPrefs()

        setContentView(R.layout.activity_main)
        setUI()

        if (prefs.getBoolean("playOnline", false) && isOnlineEntry) {
            btnPlayerRound.visibility = ImageView.INVISIBLE
            btnDice.visibility = ImageView.VISIBLE
            btnDice.setImageResource(R.drawable.button_ok)
            btnPlayerResult.text = resources.getString(R.string.confirmEntry)
        }

        if (dialogPlayerQuery.isShowing) {
            dialogPlayerQuery.dismiss()
            dialogPlayerQuery = Dialog(this, android.R.style.Theme_Light)
            dialogPlayerQuery.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialogPlayerQuery.setContentView(R.layout.dialogplayerquery)
            showPlayerQuery()
        }

    }

    override fun onBackPressed() {
        showMainDialog()
    }

    override fun onDestroy() {
        super.onDestroy()

//        Log.i(TAG, "onDestroy()")

        handlerEngine.removeCallbacks(mUpdateEngine)
        handlerAnimationDices.removeCallbacks(mUpdateAnimationDices)

        if (!prefs.getBoolean("playOnline", false))
            setRunPrefs()

    }

    private fun finishApp(updateUser: Boolean) {

//        Log.i(TAG, "start finishApp()")

        logOutUpdateUser(true, updateUser)

    }

    @SuppressLint("ClickableViewAccessibility", "SourceLockedOrientationActivity")
    private fun setUI(): Boolean {

//        Log.i(TAG, "start setUI()")

        runPrefs = getSharedPreferences("run", 0)
        prefs = getSharedPreferences("prefs", 0)
        diceState = prefs.getInt("dice", 1)
        isDiceBoard = prefs.getBoolean("isDiceBoard", isDiceBoard)
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        displayDensity = displayMetrics.density
        displayWidth = displayMetrics.widthPixels
        displayHeight = displayMetrics.heightPixels
        var displayMax = displayHeight
        if (displayWidth > displayHeight)
            displayMax = displayWidth

//        Log.i(TAG, "1 setUI(), orientation: $orientation, displayWidth: $displayWidth, displayHeight: $displayHeight")

        if (displayMax <= 400) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else
            orientation = this.resources.configuration.orientation

//        Log.i(TAG, "2 setUI(), orientation: $orientation, displayWidth: $displayWidth, displayHeight: $displayHeight")

        ed = EscaleroData(this, runPrefs, prefs, displayWidth, displayHeight, displayDensity, orientation)
        getPermissions()
        getRunPrefs()
        initDiceBoardRollValues()
        initEngines(false)
        if (runPrefs.getString("diceRoll", "") != "") {
            setDiceValues(false)
            ed.getDiceText(isDouble1, isServed, diceValues, diceMode)
        } else
            ed.diceText = ""
        if (ed.isFlipScreen) {
            isOrientationReverse = runPrefs.getBoolean("isOrientationReverse", false)
            isOrientationReverse = !isOrientationReverse
            computeFlipScreen(false)
        }

        tableView = findViewById<View>(R.id.tableView) as TableView
        tableView.setOnTouchListener(this)
        tableView.initTable(ed, orientation, displayWidth, displayHeight)

        btnDice = findViewById<View>(R.id.btnDice) as ImageView
        btnDice.setOnTouchListener(this)
        btnPlayerIcon = findViewById<View>(R.id.btnPlayerIcon) as ImageView
        btnPlayerRound = findViewById<View>(R.id.btnPlayerRound) as TextView
        btnPlayerName = findViewById<View>(R.id.btnPlayerName) as TextView
        btnPlayerInfo = findViewById<View>(R.id.btnPlayerInfo) as TextView
        btnPlayerInfo2 = findViewById<View>(R.id.btnPlayerInfo2) as TextView
        btnPlayerResult = findViewById<View>(R.id.btnPlayerResult) as TextView
        txtPlayers = findViewById<View>(R.id.txtPlayers) as TextView
        txtPlayerEps = findViewById<View>(R.id.txtPlayerEps) as TextView
        btnMenu = findViewById<View>(R.id.btnMenu) as ImageView

        rl1 = findViewById<View>(R.id.rl1) as RelativeLayout
        radio0 = findViewById<View>(R.id.radio0) as ImageView
        radio1 = findViewById<View>(R.id.radio1) as ImageView
        radio2 = findViewById<View>(R.id.radio2) as ImageView
        radio3 = findViewById<View>(R.id.radio3) as ImageView
        radio4 = findViewById<View>(R.id.radio4) as ImageView
        radio5 = findViewById<View>(R.id.radio5) as ImageView

        radio0.visibility = ImageView.INVISIBLE
        radio1.visibility = ImageView.INVISIBLE
        radio2.visibility = ImageView.INVISIBLE
        radio3.visibility = ImageView.INVISIBLE
        radio4.visibility = ImageView.INVISIBLE
        radio5.visibility = ImageView.INVISIBLE
        btnDice.minimumWidth = ed.diceImageSize
        btnDice.minimumHeight = ed.diceImageSize

        if (diceState == 0) {
            btnDice.visibility = ImageView.INVISIBLE
            if (ed.selectedGridItem >= 0 && selectedCol >= 0 && selectedRow >= 0)
                setEntryButtons()
        }
        if (flipMode == 1) {
            btnDice.setImageResource(R.drawable.button_flip)
            btnDice.visibility = ImageView.VISIBLE
        }
        btnMenu.minimumWidth = ed.diceImageSize
        btnMenu.minimumHeight = ed.diceImageSize

        rl2 = findViewById<View>(R.id.rl2) as RelativeLayout
        diceD1 = findViewById<View>(R.id.diceD_1) as ImageView
        diceD2 = findViewById<View>(R.id.diceD_2) as ImageView
        diceD3 = findViewById<View>(R.id.diceD_3) as ImageView
        diceD4 = findViewById<View>(R.id.diceD_4) as ImageView
        diceD5 = findViewById<View>(R.id.diceD_5) as ImageView
        doubleA = findViewById<View>(R.id.doubleA) as ImageView
        diceA1 = findViewById<View>(R.id.diceA_1) as ImageView
        diceA2 = findViewById<View>(R.id.diceA_2) as ImageView
        diceA3 = findViewById<View>(R.id.diceA_3) as ImageView
        diceA4 = findViewById<View>(R.id.diceA_4) as ImageView
        diceA5 = findViewById<View>(R.id.diceA_5) as ImageView

        diceD1.minimumWidth = ed.diceImageSize
        diceD1.minimumHeight = ed.diceImageSize
        diceD2.minimumWidth = ed.diceImageSize
        diceD2.minimumHeight = ed.diceImageSize
        diceD3.minimumWidth = ed.diceImageSize
        diceD3.minimumHeight = ed.diceImageSize
        diceD4.minimumWidth = ed.diceImageSize
        diceD4.minimumHeight = ed.diceImageSize
        diceD5.minimumWidth = ed.diceImageSize
        diceD5.minimumHeight = ed.diceImageSize
        diceA1.minimumWidth = ed.diceImageSize
        diceA1.minimumHeight = ed.diceImageSize
        diceA1.setOnTouchListener(this)
        diceA2.minimumWidth = ed.diceImageSize
        diceA2.minimumHeight = ed.diceImageSize
        diceA2.setOnTouchListener(this)
        diceA3.minimumWidth = ed.diceImageSize
        diceA3.minimumHeight = ed.diceImageSize
        diceA3.setOnTouchListener(this)
        diceA4.minimumWidth = ed.diceImageSize
        diceA4.minimumHeight = ed.diceImageSize
        diceA4.setOnTouchListener(this)
        diceA5.minimumWidth = ed.diceImageSize
        diceA5.minimumHeight = ed.diceImageSize
        diceA5.setOnTouchListener(this)
        doubleA.minimumWidth = ed.diceImageSize
        doubleA.minimumHeight = ed.diceImageSize

        rl3 = findViewById<View>(R.id.rl3) as RelativeLayout
        diceBoard = findViewById<View>(R.id.diceBoard) as BoardView
        diceBoard.setOnTouchListener(this)
        diceBoard.setDiceIcons(ed.diceIcons)

        setRelativeLayout(diceState)

        if (diceState == 1) {
            if (!ed.isSingleGame and !isDouble1) {
                for (i in diceDouble1.indices) {
                    when (i) {
                        0 -> diceD1.setImageResource(getImageId(diceDouble1[i]))
                        1 -> diceD2.setImageResource(getImageId(diceDouble1[i]))
                        2 -> diceD3.setImageResource(getImageId(diceDouble1[i]))
                        3 -> diceD4.setImageResource(getImageId(diceDouble1[i]))
                        4 -> diceD5.setImageResource(getImageId(diceDouble1[i]))
                    }
                }
            }
            if (!isDiceBoard)
                diceView(diceRoll, diceHold)
        }

//        Log.i(TAG, "3 setUI(), ed.isGameOver: " + ed.isGameOver + ", ed.selectedGridItem: " + ed.selectedGridItem + ", ed.playerToMove: " + ed.playerToMove)

        if (ed.isGameOver)
            isDicing = false
        else {
            if (ed.selectedGridItem > -1 && !isOnlineActive)
                ed.setPlayerToMove(getEngineName(isEnginePlayer(ed.nextPlayerToMove)))
            setBtnPlayer(ed.playerToMove)
        }

        updateTable(true)

        if (isEnginePlayer(ed.playerToMove) and (diceMode == 4))
            performEngineCommands(getEngineDiceCommand(ed.playerToMove, 3, diceRoll, diceHold, diceDouble1, isServedDouble1))
        else {
            if (diceState == 1) {
                isInitRound = false
                isInitBoard = true
                handlerAnimationDices.postDelayed(mUpdateAnimationDices, 0)
            }
        }

//        Log.i(TAG, "end setUI()")

        return true
    }

    private fun setRelativeLayout(diceState: Int) {
        when (diceState) {
            0 -> {
                rl1.visibility = RelativeLayout.VISIBLE
                rl2.visibility = RelativeLayout.INVISIBLE
                rl3.visibility = RelativeLayout.INVISIBLE
            }
            1 -> {
                rl1.visibility = RelativeLayout.INVISIBLE
                if (isDiceBoard) {
                    rl3.visibility = RelativeLayout.VISIBLE
                    rl2.visibility = RelativeLayout.INVISIBLE
                } else {
                    rl3.visibility = RelativeLayout.INVISIBLE
                    rl2.visibility = RelativeLayout.VISIBLE
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    if (prefs.getBoolean("logging", false)) {
                        for (i in grantResults.indices) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
                                Log.i(TAG, permissions[i] + " was granted")
                            else
                                Log.i(TAG, permissions[i] + " denied")
                        }
                    }
                }
                return
            }
        }
    }

    fun myClickHandler(view: View) {
        radioSelected = -1
        when (view.id) {

            R.id.btnMenu -> if (isDicing)
                playSound(1, 0)
            else {
                if (prefs.getBoolean("playOnline", false))
                    showMenuOnline()
                else
                    showMenuOffline()
            }

            R.id.doubleA -> {
                if (prefs.getBoolean("playOnline", false) && isOnlineActive && !isDoingTurn) {
                    Toast.makeText(applicationContext, getString(R.string.notYourTurn), Toast.LENGTH_SHORT).show()
                    return
                }

//                Log.i(TAG, "myClickHandler(), R.id.doubleA, isDouble1: $isDouble1, diceMode: $diceMode, diceModePrev: $diceModePrev")

                if (isDouble1 and (diceMode != 5)) {
                    isDouble1 = false
                    setDiceDouble1()
                    diceMode = 1
                    setDiceValues(true)
                    setBtnPlayer(ed.playerToMove)
                    diceView(diceRoll, diceHold)
                } else {
                    if (!isDouble1 and (diceModePrev <= 3)) {
                        for (i in diceDouble1.indices) {
                            diceDouble1[i] = -1
                        }
                        isDouble1 = true
                        getFromPrev()
                        setDiceValues(false)
                        setBtnPlayer(ed.playerToMove)
                        diceView(diceRoll, diceHold)
                    }
                }

                if (prefs.getBoolean("playOnline", false) && isOnlineActive && isDoingTurn)
                    matchDataToDb(false, ONLINE_ACTIVE)

            }

            // entry buttons
            R.id.radio0, R.id.radio1, R.id.radio2, R.id.radio3, R.id.radio4, R.id.radio5 -> {
                if (view.id == R.id.radio0) radioSelected = 0
                if (view.id == R.id.radio1) radioSelected = 1
                if (view.id == R.id.radio2) radioSelected = 2
                if (view.id == R.id.radio3) radioSelected = 3
                if (view.id == R.id.radio4) radioSelected = 4
                if (view.id == R.id.radio5) radioSelected = 5
                if (selectedRow >= 0) {
                    val result = ed.getResultFromSelectedGridItem(selectedRow, radioSelected)
                    if (ed.setPlayerResult(ed.playerToMove, selectedCol, selectedRow, result)) {
                        diceMode = 5
                        updateValues(gridPosition)
                        updateTable(true)
                    }
                }
                radio0.visibility = ImageView.INVISIBLE
                radio1.visibility = ImageView.INVISIBLE
                radio2.visibility = ImageView.INVISIBLE
                radio3.visibility = ImageView.INVISIBLE
                radio4.visibility = ImageView.INVISIBLE
                radio5.visibility = ImageView.INVISIBLE
            }
        }
        if (!prefs.getBoolean("playOnline", false))
            setRunPrefs()
    }

    private fun isTouchDelayOk(): Boolean {
        var isOk = false
        if (System.currentTimeMillis() - startDelayTime >= MIN_BTN_DELAY)
            isOk = true
        startDelayTime = System.currentTimeMillis()
        return isOk
    }

    private fun btnDiceClicked() {

//        Log.i(TAG, "1 btnDiceClicked(), isOnlineEntry: $isOnlineEntry, ed.isGameOver: ${ed.isGameOver}")

        if (prefs.getBoolean("playOnline", false) && isOnlineActive && !isDoingTurn) {
            return
        }

        if (prefs.getBoolean("playOnline", false) && !isOnlineActive) {
            showPlayOnlineDialog()
            return
        }

        if (isUpdating) {
            isUpdatingWarning()
            return
        }

        if (isOnlineEntry) {

//            Log.i(TAG, "2 btnDiceClicked(), isOnlineEntry: $isOnlineEntry, ed.isGameOver: ${ed.isGameOver}")

            isDouble1 = true
            if (ed.isGameOver)
                matchDataToDb(false, ONLINE_GAME_OVER)
            else
                matchDataToDb(true, ONLINE_ACTIVE_TURN)

            return
        }

        isDouble1Selected = false
        var isSelectedGridItem = false
        if (ed.selectedGridItem >= 0)
            isSelectedGridItem = true

        updateTable(false)

        doubleA.visibility = ImageView.INVISIBLE
        btnPlayerInfo.visibility = TextView.INVISIBLE
        engineEntryCommand = ""
        if ((diceMode == 1) or (diceMode == 5))
            btnPlayerInfo2.visibility = TextView.INVISIBLE
        if (engineIsRunning and allEnginePlayer()) {
            Toast.makeText(applicationContext, getString(R.string.isStopped), Toast.LENGTH_SHORT).show()
            stopEngine = true

            if (!prefs.getBoolean("playOnline", false))
                setRunPrefs()
            return
        }
        if (ed.isGameOver)
            isDicing = false
        else {

            if (flipMode == 1) {
                flipMode = 0
                setDiceActions()
                setSelectedItem(-1)
                computeFlipScreen(false)

                if (!prefs.getBoolean("playOnline", false))
                    setRunPrefs()
                return
            }

            if (flipMode == 2)
                flipMode = 0

//            Log.i(TAG, "btnDiceClicked(), isDiceBoard: $isDiceBoard, diceMode: $diceMode, diceModePrev: $diceModePrev, isDouble1: $isDouble1")

            if (!isDiceBoard && !ed.isSingleGame && diceModePrev == 4 && ((diceMode == 1 && isDouble1) || (diceMode == 5 && !isDouble1))) {

                diceD1.visibility = ImageView.INVISIBLE
                diceD2.visibility = ImageView.INVISIBLE
                diceD3.visibility = ImageView.INVISIBLE
                diceD4.visibility = ImageView.INVISIBLE
                diceD5.visibility = ImageView.INVISIBLE
                doubleA.visibility = ImageView.INVISIBLE

            }

            if (diceMode == 5) {
                diceMode = 1
                if (!ed.isSingleGame) {
                    isDouble1 = true
                    ed.diceTextDouble1 = ""
                    diceView(diceRoll, diceHold)
                }
            }

            setSelectedItem(-1)
            if (isSelectedGridItem)
                updateTable(true)

            if ((diceMode > 1) and (diceMode <= 4) and isEnginePlayer(ed.playerToMove)) {
                if ((diceRoll[0] >= 0) or (diceHold[0] >= 0))
                    performEngineCommands(getEngineDiceCommand(ed.playerToMove, diceMode - 1, diceRoll, diceHold, diceDouble1, isServedDouble1))
            } else {
                if (diceMode <= 3) {
                    if (diceMode == 0)
                        diceMode = 1
                    btnDice.visibility = ImageView.INVISIBLE
                    diceAction(diceRoll, diceHold, true)
                } else {
                    updateTable(false)
                }
            }
        }
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        onTouchEvent(event)
        if (prefs.getBoolean("playOnline", false) && isOnlineActive && !isDoingTurn) {
            if (        view.id == R.id.btnDice
                    ||  view.id == R.id.diceBoard
                    ||  view.id == R.id.diceA_1
                    ||  view.id == R.id.diceA_2
                    ||  view.id == R.id.diceA_3
                    ||  view.id == R.id.diceA_4
                    ||  view.id == R.id.diceA_5) {
                Toast.makeText(applicationContext, getString(R.string.notYourTurn), Toast.LENGTH_SHORT).show()
                return true
            }
        }

        if (isEnginePlayer(ed.playerToMove) and isDicing) {
            playSound(1, 0)
            return true
        }
        if (event.action == MotionEvent.ACTION_UP) {

            if ((diceMode == 2) or (diceMode == 3)) {
                // 2D
                when (view.id) {
                    R.id.diceA_1 -> setDiceValues(0)
                    R.id.diceA_2 -> setDiceValues(1)
                    R.id.diceA_3 -> setDiceValues(2)
                    R.id.diceA_4 -> setDiceValues(3)
                    R.id.diceA_5 -> setDiceValues(4)
                }

                if (prefs.getBoolean("playOnline", false) && isOnlineActive && isDoingTurn) {
                    matchDataToDb(false, ONLINE_ACTIVE)
                }

            }

            if (prefs.getBoolean("playOnline", false) && diceMode == 1 && isOnlineActive && isDoingTurn) {

                if (mPlayerABC != null) {
                    val nPlayer = ed.getNextPlayerAB(ed.playerStart)

//                    Log.i(TAG, "onTouch(), mPlayerName: $mPlayerName, mPlayerABC: $mPlayerABC, nPlayer: $nPlayer")

                    if (mPlayerABC != nPlayer.toString()) {
                        Toast.makeText(applicationContext, getString(R.string.correctionNextPlayer, mOpponentName), Toast.LENGTH_LONG).show()
                        matchDataToDb(true, ONLINE_ACTIVE + CORRECTION)
                        return true
                    }

                }

            }

            if (view.id == R.id.btnDice) {
                if (isTouchDelayOk())
                    btnDiceClicked()
                return true
            }

            // tableView
            if (view.id == R.id.tableView) {
                val touchPoint = Point(event.x.toInt(), event.y.toInt())
                val position = tableView.getPositionFromTouchPoint(touchPoint)

                //Log.i(TAG, "Position clicked: " + position + ", GridId: " + ed.gridCurrent[position])

                gridPosition = position

                if (isDicing) {
                    playSound(1, 0)
                    return true
                }

                val gridId = ed.gridCurrent[position]
                var playerId = ' '
                selectedCol = -1
                selectedRow = -1
                var col = 0
                var row = 0

                if (gridId.startsWith("A") or gridId.startsWith("B") or (gridId.startsWith("C") and (gridId.length == 3))) {
                    if (ed.isGameOver) {
                        isDicing = false
                        return true
                    }

                    if (prefs.getBoolean("playOnline", false) && isOnlineActive && !isDoingTurn) {
                        Toast.makeText(applicationContext, getString(R.string.notYourTurn), Toast.LENGTH_SHORT).show()
                        return true
                    }

                    if ((ed.selectedGridItem == position) and ((diceMode == 5) or ((diceMode == 1) and (flipMode == 1)))
                            and !isEnginePlayer(ed.getPrevPlayer(ed.playerToMove))) {

                        if (isUpdating) {
                            isUpdatingWarning()
                            return true
                        }

                        cancelButton()

//                        Log.i(TAG, "onTouch(), isOnlineActive: $isOnlineActive, isDoingTurn: $isDoingTurn, isOnlineEntry: $isOnlineEntry")

                        if (prefs.getBoolean("playOnline", false) && isOnlineActive && isDoingTurn && isOnlineEntry) {
                            isOnlineEntry = false
                            matchDataToDb(false, ONLINE_ACTIVE_ENTRY_STORNO)
                        }

                        return true
                    }

                    if ((diceState == 1) and (diceMode >= 5)) {
                        btnDice.visibility = ImageView.VISIBLE
                        Toast.makeText(applicationContext, getString(R.string.pressCenterButton), Toast.LENGTH_SHORT).show()
                        playSound(1, 0)
                        return true
                    }
                    if ((diceState == 1) and isEnginePlayer(ed.playerToMove)) {
                        Toast.makeText(applicationContext, getString(R.string.prefsEngines), Toast.LENGTH_SHORT).show()
                        playSound(1, 0)
                        return true
                    }
                    playerId = gridId[0]
                    col = Character.getNumericValue(gridId[1])
                    row = Character.getNumericValue(gridId[2])
                    if ((diceState == 0) and (playerId == ed.playerToMove) and (ed.getPlayerResult(playerId, col, row) == -1)) {
                        selectedCol = col
                        selectedRow = row
                        ed.selectedGridItem = position
                        setEntryButtons()
                        diceMode = 4
                        updateTable(true)
                        return true
                    }
                } else {
                    if (gridId.startsWith("L") and (gridId.length == 3)) {
                        ed.setStartPlayer(gridId)
                        updateTable(true)
                    }
                    // switch: column / playerName
                    if (gridId == "X00") {
                        if (ed.isPlayerColumn) {
                            ed.isPlayerColumn = false
                            Toast.makeText(this@MainActivity, getString(R.string.displayColumns), Toast.LENGTH_SHORT).show()
                        } else {
                            ed.isPlayerColumn = true
                            Toast.makeText(applicationContext, getString(R.string.displayPlayers), Toast.LENGTH_SHORT).show()
                        }
                        var selectedId = ""
                        if (ed.selectedGridItem >= 0)
                            selectedId = ed.gridCurrent[ed.selectedGridItem]
                        ed.setGridControl()
                        if (selectedId != "") {
                            ed.setSelectedGridItem(selectedId)
                            setSelectedItem(ed.selectedGridItem)
                        }
                        val edi = prefs.edit()
                        edi.putBoolean("isPlayerColumn", ed.isPlayerColumn)
                        edi.apply()
                        updateTable(true)
                    }
                    // flip screen
                    if (gridId == "X01") {
                        if (prefs.getBoolean("playOnline", false)) {
                            ed.isFlipScreen = false
                            isOrientationReverse = false
                            Toast.makeText(applicationContext, getString(R.string.flipDisabled), Toast.LENGTH_SHORT).show()
                        }
                        else {
                            if (ed.playerNumber == 2) {
                                if (!prefs.getBoolean("enginePlayer", true) or (diceState == 0)) {
                                    if (ed.isFlipScreen) {
                                        ed.isFlipScreen = false
                                        isOrientationReverse = true
                                        Toast.makeText(applicationContext, getString(R.string.flipDisabled), Toast.LENGTH_SHORT).show()
                                        if (diceState == 0)
                                            btnDice.visibility = ImageView.INVISIBLE
                                        else {

                                            if (!ed.isSingleGame && diceMode == 1 && diceModePrev >= 4 && !isDouble1)
                                                isDouble1 = true

                                            if (flipMode == 1)
                                                btnDice.setImageResource(R.drawable.button_dice)
                                        }

                                        computeFlipScreen(true)

                                    } else {
                                        ed.isFlipScreen = true
                                        isOrientationReverse = false
                                        Toast.makeText(applicationContext, getString(R.string.flipEnabled), Toast.LENGTH_SHORT).show()
                                    }
                                    updateTable(true)
                                    flipMode = 0
                                } else {
                                    ed.isFlipScreen = false
                                    Toast.makeText(applicationContext, getString(R.string.flipEngines), Toast.LENGTH_SHORT).show()
                                    playSound(1, 0)
                                }
                            } else {
                                ed.isFlipScreen = false
                                isOrientationReverse = false
                                computeFlipScreen(false)
                                Toast.makeText(applicationContext, getString(R.string.flip3Players), Toast.LENGTH_SHORT).show()
                                playSound(1, 0)
                                updateTable(true)
                            }
                        }
                        val edi = prefs.edit()
                        edi.putBoolean("computeFlipScreen", ed.isFlipScreen)
                        edi.apply()
                    }
                    // isSummation / difference
                    if (gridId == "X02") {
                        if (ed.playerNumber == 2) {
                            if (ed.isSummation) {
                                ed.isSummation = false
                                Toast.makeText(applicationContext, getString(R.string.accountingDifference), Toast.LENGTH_SHORT).show()
                            } else {
                                ed.isSummation = true
                                Toast.makeText(applicationContext, getString(R.string.accountingSummation), Toast.LENGTH_SHORT).show()
                            }
                            val edi = prefs.edit()
                            edi.putBoolean("isSummation", ed.isSummation)
                            edi.apply()
                        } else {
                            Toast.makeText(applicationContext, getString(R.string.accounting3Player), Toast.LENGTH_SHORT).show()
                            playSound(1, 0)
                            ed.isSummation = true
                        }
                        updateTable(true)
                    }
                    // accounting
                    if (gridId == "X03") {

                        if (!prefs.getBoolean("playOnline", false))
                            showAccountingDialog(ed.accounting)
                        else {
                            if (isOnlineActive) {
                                if (mMatchId != null)
                                    startDialogCurrentMatch(mMatchId!!)
                                else {
                                    initOnline()
                                    updateTable(true)
                                    showPlayOnlineDialog()
                                }
                            }
                            else {
                                showPlayOnlineDialog()
                            }
                        }

                    }
                }

                if (playerId != ' ') {

//                    Log.i(TAG, "1 onTouch(), R.id.tableView, diceMode: $diceMode, isOnlineActive: $isOnlineActive, isDoingTurn: $isDoingTurn")

                    if (isUpdating) {
                        isUpdatingWarning()
                        return true
                    }

                    if (flipMode == 1) {
                        btnDice.visibility = ImageView.VISIBLE
                        Toast.makeText(applicationContext, getString(R.string.pressCenterButton), Toast.LENGTH_SHORT).show()
                        playSound(1, 0)
                        return true
                    } else
                        flipMode = 0
                    var result = 0
                    if ((diceState == 1) and isDiced) {
                        if (!ed.isSingleGame and (diceDouble1[0] >= 0)) {
                            setDiceValues(false)
                            result = ed.getResultFromDiceValues(row, isServed, diceValues)
                            setDiceValues(true)
                            val result1 = ed.getResultFromDiceValues(row, isServedDouble1, diceValuesDouble1)
                            if (result1 > result) {
                                result = result1
                            }
                        } else {
                            setDiceValues(false)
                            result = ed.getResultFromDiceValues(row, isServed, diceValues)
                        }
                    }

                    var isUpdated = false
                    if ((diceState == 0) or ((diceState == 1) and isDiced))
                        isUpdated = ed.setPlayerResult(playerId, col, row, result)

//                    Log.i(TAG, "2 onTouch(), R.id.tableView, diceMode: $diceMode, playerId: $playerId, playerToMove: " + ed.playerToMove)
//                    Log.i(TAG, "2 onTouch(), R.id.tableView, isUpdated: $isUpdated, isDiced: $isDiced")

                    if (isUpdated) {

                        if (!prefs.getBoolean("playOnline", false))
                            performEngineEntryCommand("entry $playerId $col $row $result")
                        updateValues(position)
                        if (isDiceBoard) {
                            isInitBoard = true
                            animationDiceBoard(false)
                        }
                    } else {
                        if (playerId == ed.playerToMove) {
                            if (ed.isEntry)
                                Toast.makeText(applicationContext, getString(R.string.noEntry), Toast.LENGTH_SHORT).show()
                            else {
                                btnDice.visibility = ImageView.VISIBLE
                                Toast.makeText(applicationContext, getString(R.string.pressCenterButton), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(applicationContext, getString(R.string.nextPlayer) + " " + ed.playerToMove, Toast.LENGTH_SHORT).show()
                            radio0.visibility = ImageView.INVISIBLE
                            radio1.visibility = ImageView.INVISIBLE
                            radio2.visibility = ImageView.INVISIBLE
                            radio3.visibility = ImageView.INVISIBLE
                            radio4.visibility = ImageView.INVISIBLE
                            radio5.visibility = ImageView.INVISIBLE
                        }
                        playSound(1, 0)
                    }

//                    Log.i(TAG, "3 onTouch(), R.id.tableView, diceMode: " + diceMode)

                    val isUpdateTable = updateTable(true)

//                    Log.i(TAG, "4 onTouch(), isUpdateTable: $isUpdateTable, diceMode: $diceMode")

                    if (isUpdateTable && prefs.getBoolean("playOnline", false) && isOnlineActive && isDoingTurn && diceMode == 5) {

//                        Log.i(TAG, "5 onTouch(), isOnlineEntry: $isOnlineEntry")

                        isOnlineEntry = true
                        matchDataToDb(false, ONLINE_ACTIVE_ENTRY)
                        return true
                    }

                }
            }

            // diceBoard
            var isDraw = false
            if (view.id == R.id.diceBoard) {

                if (isUpdating) {
                    isUpdatingWarning()
                    return true
                }

                val touchPoint = Point(event.x.toInt(), event.y.toInt())
                var id = diceBoard.getIdFromTouchPoint(touchPoint)

//                Log.i(TAG, "touchPoint x: " + touchPoint.x + ", y: " + touchPoint.y + ", id: " + id)

                if (diceMode > 3) {
                    if ((id == R.id.dbRoll_1) or (id == R.id.dbRoll_2) or (id == R.id.dbRoll_3) or (id == R.id.dbRoll_4) or (id == R.id.dbRoll_5))
                        id = 0
                    if ((id == R.id.dbHold_1) or (id == R.id.dbHold_2) or (id == R.id.dbHold_3) or (id == R.id.dbHold_4) or (id == R.id.dbHold_5))
                        id = 0
                }
                when (id) {
                    R.id.dbRoll_1 -> {
                        setDiceValues(0)
                        isDraw = true
                    }
                    R.id.dbRoll_2 -> {
                        setDiceValues(1)
                        isDraw = true
                    }
                    R.id.dbRoll_3 -> {
                        setDiceValues(2)
                        isDraw = true
                    }
                    R.id.dbRoll_4 -> {
                        setDiceValues(3)
                        isDraw = true
                    }
                    R.id.dbRoll_5 -> {
                        setDiceValues(4)
                        isDraw = true
                    }
                    R.id.dbHold_1 -> {
                        setDiceValues(diceBoard.getHoldIdFromBoard(0))
                        isDraw = true
                    }
                    R.id.dbHold_2 -> {
                        setDiceValues(diceBoard.getHoldIdFromBoard(1))
                        isDraw = true
                    }
                    R.id.dbHold_3 -> {
                        setDiceValues(diceBoard.getHoldIdFromBoard(2))
                        isDraw = true
                    }
                    R.id.dbHold_4 -> {
                        setDiceValues(diceBoard.getHoldIdFromBoard(3))
                        isDraw = true
                    }
                    R.id.dbHold_5 -> {
                        setDiceValues(diceBoard.getHoldIdFromBoard(4))
                        isDraw = true
                    }
                    R.id.dbHoldFast -> {
                        setHoldFast()
                        isDraw = true
                    }
                    R.id.dbD1 -> {

//                        Log.i(TAG, "R.id.diceBoard, R.id.dbD1")

                        if (isDouble1 and (diceMode != 5)) {
                            isDouble1 = false
                            if (diceMode <= 3)
                                isDouble1Selected = true
                            setDiceDouble1()
                            diceMode = 1

                            setDiceValues(true)
                            setBtnPlayer(ed.playerToMove)
                            diceView(diceRoll, diceHold)
                            isDraw = true
                        } else {
                            if (!isDouble1 and (diceModePrev <= 3)) {
                                isDouble1 = true
                                getFromPrev()
                                setDiceValues(false)
                                for (i in diceDouble1.indices) {
                                    diceDouble1[i] = -1
                                }
                                setBtnPlayer(ed.playerToMove)
                                diceView(diceRoll, diceHold)
                                isDraw = true
                            }
                        }
                    }
                }
            }
            if (isDraw) {

//                Log.i(TAG, "onTouch(), diceBoard, view.id: " + view.id )

                if (prefs.getBoolean("playOnline", false) && isOnlineActive && isDoingTurn) {
                    matchDataToDb(false, ONLINE_ACTIVE)
                }

                initRollValues = false
                animationDiceBoard(false)
            }
            view.performClick()
        }
        return true
    }

    private fun setEntryButtons() {
        if (selectedRow < 6) {
            radio0.visibility = RadioButton.VISIBLE
            setEntryView(radio0, "0", 0)
            radio1.visibility = RadioButton.VISIBLE
            setEntryView(radio1, "1", EscaleroData.PICTURE_MULTIPLIER[selectedRow])
            radio2.visibility = RadioButton.VISIBLE
            setEntryView(radio2, "2", EscaleroData.PICTURE_MULTIPLIER[selectedRow] * 2)
            radio3.visibility = RadioButton.VISIBLE
            setEntryView(radio3, "3", EscaleroData.PICTURE_MULTIPLIER[selectedRow] * 3)
            radio4.visibility = RadioButton.VISIBLE
            setEntryView(radio4, "4", EscaleroData.PICTURE_MULTIPLIER[selectedRow] * 4)
            radio5.visibility = RadioButton.VISIBLE
            setEntryView(radio5, "5", EscaleroData.PICTURE_MULTIPLIER[selectedRow] * 5)
        } else {
            radio0.visibility = RadioButton.VISIBLE
            setEntryView(radio0, "0", 0)
            radio1.visibility = RadioButton.VISIBLE
            setEntryView(radio1, "N", EscaleroData.PICTURE_MULTIPLIER[selectedRow])
            radio2.visibility = RadioButton.VISIBLE
            if (selectedRow == 9)
                setEntryView(radio2, "S", EscaleroData.PICTURE_MULTIPLIER[selectedRow] + ed.bonusServedGrande)
            else
                setEntryView(radio2, "S", EscaleroData.PICTURE_MULTIPLIER[selectedRow] + ed.bonusServed)
            radio3.visibility = RadioButton.INVISIBLE
            radio4.visibility = RadioButton.INVISIBLE
            radio5.visibility = RadioButton.INVISIBLE
        }
    }

    private fun setEntryView(imageView: ImageView, value: String, entry: Int) {
        val strEntry = entry.toString()
        val bmBackground = BitmapFactory.decodeResource(this.resources, R.drawable.entry).copy(Bitmap.Config.ARGB_8888, true)
        val canvasBg = Canvas()
        canvasBg.setBitmap(bmBackground)

        val paint = Paint()
        paint.color = Color.BLACK
        paint.textSize = canvasBg.height * 0.5f
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeWidth = 2f
        var bounds = Rect()
        paint.textAlign = Paint.Align.CENTER
        paint.getTextBounds(value, 0, value.length, bounds)
        var x = canvasBg.width / 2
        var y = (canvasBg.height / 2 - (paint.descent() + paint.ascent()) / 2).toInt()
        canvasBg.drawText(value, x.toFloat(), y.toFloat(), paint)

        paint.color = Color.RED
        paint.textSize = canvasBg.height * 0.3f
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeWidth = 2f
        bounds = Rect()
        paint.getTextBounds(strEntry, 0, strEntry.length, bounds)
        x = canvasBg.width - bounds.width() - 2
        y = canvasBg.height - canvasBg.height / 12
        if (entry > 9) x += 5
        canvasBg.drawText(strEntry, x.toFloat(), y.toFloat(), paint)
        imageView.setImageBitmap(bmBackground)

    }

    private fun cancelButton() {
        radio0.visibility = ImageView.INVISIBLE
        radio1.visibility = ImageView.INVISIBLE
        radio2.visibility = ImageView.INVISIBLE
        radio3.visibility = ImageView.INVISIBLE
        radio4.visibility = ImageView.INVISIBLE
        radio5.visibility = ImageView.INVISIBLE
        if (isDicing) {
            playSound(1, 0)
            return
        }
        if (ed.selectedGridItem >= 0) {
            flipMode = 0
            cancelEntry()
        } else {
            if (!ed.isSingleGame and !isDouble1 and (diceMode == 1)) {
                isDouble1 = true
                isDiced = true
                getFromPrev()
                setDiceValues(false)
                ed.getDiceText(isDouble1, isServed, diceValues, diceMode - 1)
                if (diceMode <= 3)
                    btnDice.visibility = ImageView.VISIBLE
                else
                    btnDice.visibility = ImageView.INVISIBLE
                diceView(diceRoll, diceHold)
                setDiceValues(false)
                ed.getDiceText(isDouble1, isServed, diceValues, diceMode)
            }
        }
        setSelectedItem(-1)

        if (!prefs.getBoolean("playOnline", false))
            setRunPrefs()
    }

    private fun cancelEntry() {
        ed.cancelPlayerResult(ed.selectedGridItem)
        val gridId = ed.gridCurrent[ed.selectedGridItem]
        if (gridId.startsWith("A") or gridId.startsWith("B") or (gridId.startsWith("C") and (gridId.length == 3))) {
            val playerId = gridId[0]
            val col = Character.getNumericValue(gridId[1])
            val row = Character.getNumericValue(gridId[2])

            if (!prefs.getBoolean("playOnline", false))
                performEngineEntryCommand("entry " + playerId + " " + col + " " + row + " " + -1)

        }
        if (diceState == 1) {
            isDiced = true
            getFromPrev()
            if (diceMode <= 3)
                btnDice.visibility = ImageView.VISIBLE
            else
                btnDice.visibility = ImageView.INVISIBLE
            diceView(diceRoll, diceHold)
            initRollValues = false
            animationDiceBoard(false)
        } else
            btnDice.visibility = ImageView.INVISIBLE
        updateTable(true)
    }

    private fun updateValues(position: Int) {
        if (ed.isFlipScreen) {
            flipMode = 1
            setToPrev()
            btnDice.setImageResource(R.drawable.button_flip)
            btnDice.visibility = ImageView.VISIBLE
        }

        setSelectedItem(position)

        if (ed.isGameOver) {
            if (prefs.getBoolean("playOnline", false) && isOnlineActive) {
                if (!isOnlineEntry)
                    setBtnPlayer(ed.playerToMove)
                if (!ed.isFlipScreen)
                    setDiceActions()
            }
            else
                showAccountingDialog(resources.getString(R.string.gameOver) + "\n" + ed.accounting)
        }
        else {

            if (!isOnlineActive)
                ed.setPlayerToMove(getEngineName(isEnginePlayer(ed.nextPlayerToMove)))

            if (!isOnlineEntry)
                setBtnPlayer(ed.playerToMove)

            if (!ed.isFlipScreen)
                setDiceActions()
        }

        if (!prefs.getBoolean("playOnline", false))
            setRunPrefs()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            PREFERENCES_REQUEST_CODE -> if (resultCode == RESULT_OK) {
                engineIsRunning = false
                if (isDiceBoard) {
                    diceBoard.setDiceSize()
                    diceBoard.invalidate()
                }
                if (prefs.getBoolean("cbNewGame", false)) {
                    newGameIntent = Intent(this, NewGame::class.java)
                    startActivityForResult(newGameIntent, NEW_GAME_REQUEST_CODE)
                } else
                    setDataUpdate()

                if (diceState == 1)
                    diceView(diceRoll, diceHold)
                if (engine != null)
                    performEngineCommands("logging " + prefs.getBoolean("logging", false))
            }
            NEW_GAME_REQUEST_CODE -> if (resultCode == RESULT_OK) {
                engineIsRunning = false
                if (prefs.getBoolean("gameFromFile", false) and !prefs.getBoolean("cbNewGame", false)) {
                    setUI()
                    return
                }

                setDataUpdate()
                initRunPrefs()
                ed = EscaleroData(this, runPrefs, prefs, displayWidth, displayHeight, displayDensity, orientation)
                initDiceArrays()
                diceMode = 0
                diceModePrev = 0
                if (prefs.getBoolean("enginePlayer", true))
                    initEngines(true)

                ed.playerStart = runPrefs.getString("playerStart", "B")!![0]

                ed.playerToMove = ed.playerStart

                ed.computePrevPlayerToMove(ed.playerStart)
                ed.computeNextPlayerToMove(ed.playerStart)

                if (!prefs.getBoolean("playOnline", false))
                    setRunPrefs()

                flipMode = 0
                if (diceState == 1) {
                    isDouble1 = true
                    diceInit(diceRoll, diceHold)
                    diceMode = 0
                    diceView(diceRoll, diceHold)

                }
                if (ed.playerNumber == 3) {
                    ed.isFlipScreen = false
                    ed.isSummation = true
                }
                updateTable(true)
                setSelectedItem(-1)
                if (diceState == 0)
                    btnDice.visibility = ImageView.INVISIBLE

                if (isDiceBoard and (requestCode == NEW_GAME_REQUEST_CODE)) {
                    if (!prefs.getBoolean("playOnline", false))
                        setRunPrefs()
                    setUI()
                    return
                }

            }

            RC_SIGN_IN -> {
                if (data == null) {
                    onDisconnected()
                    showErrorMessage(R.string.notLoggedIn)
                    return
                }
                val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
                if (result != null) {
                    if (result.isSuccess) {
                        // The signed in account is stored in the result.
                        val signedInAccount = result.signInAccount
                        onConnected(signedInAccount, true)
                    } else {
                        onDisconnected()
                        showErrorMessage(R.string.notLoggedIn)
                    }
                } else {
                    onDisconnected()
                    showErrorMessage(R.string.notLoggedIn)
                }
            }

        }
    }

    private fun invitePlayer(user: User) {

//        Log.i(TAG, "invitePlayer(), playerName: $playerName")

        GlobalScope.launch(Dispatchers.Main) {

            if (user.uid!!.isNotEmpty()) {
                val deferredTimestamp = async { getTimestampFromServer() }
                val ts = deferredTimestamp.await()
                deferredTimestamp.cancel()

//                Log.i(TAG, "invitePlayer(), fbUserId: $fbUserId, playerName: $playerName, ts: $ts")

                var gameType = ""
                if (!ed.isSingleGame)
                    gameType = getStringByLocal(this@MainActivity, R.string.typeDouble, null, null, user.language!!)

                val ref = FirebaseDatabase.getInstance().getReference("/notifications/${user.uid}")
                val notifications = ref.child("notificationRequests")
                val notification: HashMap<String, String> = HashMap()
                notification["title"] = getStringByLocal(this@MainActivity, R.string.invitationTitle, null, null, user.language!!)
                notification["message"] = getStringByLocal(this@MainActivity, R.string.playerInvitation, mPlayerName
                        ?: "", gameType, user.language!!)
                notification["actionId"] = "1"
                notification["matchId"] = ""
                notification["fromUsername"] = mPlayerName ?: ""
                notification["fromUserId"] = mPlayerId ?: ""
                notification["fromGpsId"] = ""
                notification["toUsername"] = user.name ?: ""
                notification["toUserId"] = user.uid ?: ""
                notification["toGpsId"] = ""
                notification["timestamp"] = ts
                notifications.push().setValue(notification)

                Toast.makeText(applicationContext, getString(R.string.playerInvited, user.name
                        ?: ""), Toast.LENGTH_SHORT).show()

            }

        }

    }

    private fun continueMatchNotification(actionId: String, matchId: String, userId: String, playerName: String, playerA: String, playerB: String) {

//        Log.i(TAG, "continueMatchNotification(), actionId: $actionId, matchId: $matchId, playerName: $playerName")

        GlobalScope.launch(Dispatchers.Main) {

            val deferredUserLanguage = async { getUserLanguage(userId) }
            val userLanguage = deferredUserLanguage.await()
            deferredUserLanguage.cancel()
            val deferredTimestamp = async { getTimestampFromServer() }
            val ts = deferredTimestamp.await()
            deferredTimestamp.cancel()

//                Log.i(TAG, "invitePlayer(), fbUserId: $fbUserId, playerName: $playerName, ts: $ts")

            val ref = FirebaseDatabase.getInstance().getReference("/notifications/$userId")
            val notifications = ref.child("notificationRequests")
            val notification: HashMap<String, String> = HashMap()
            notification["title"] = getStringByLocal(this@MainActivity, R.string.continueMatchTitle, null, null, userLanguage)
            notification["message"] = getString(R.string.continueMatchMessage, playerName, playerA, playerB)
            notification["actionId"] = actionId
            notification["matchId"] = matchId
            notification["fromUsername"] = mPlayerName ?: ""
            notification["fromUserId"] = mPlayerId ?: ""
            notification["fromGpsId"] = ""
            notification["toUsername"] = playerName
            notification["toUserId"] = userId
            notification["toGpsId"] = ""
            notification["timestamp"] = ts
            notifications.push().setValue(notification)

            Toast.makeText(applicationContext, getString(R.string.playerInvitedToContinue, playerName, playerA, playerB), Toast.LENGTH_LONG).show()

        }

    }

    private fun invitationAccepted(toUserId: String, matchId: String) {

//        Log.i(TAG, "invitationAccepted(), toUserId: $toUserId, matchId: $matchId")

        GlobalScope.launch(Dispatchers.Main) {

            val deferredUserLanguage = async { getUserLanguage(toUserId) }
            val userLanguage = deferredUserLanguage.await()
            deferredUserLanguage.cancel()

            val deferredTimestamp = async { getTimestampFromServer() }
            val ts = deferredTimestamp.await()
            deferredTimestamp.cancel()

            val ref = FirebaseDatabase.getInstance().getReference("/notifications/$toUserId")
            val notifications = ref.child("notificationRequests")
            val notification: HashMap<String, String> = HashMap()
            notification["title"] = getStringByLocal(this@MainActivity, R.string.invitationTitle, null, null, userLanguage)
            notification["message"] = getStringByLocal(this@MainActivity, R.string.invitationAccepted, mPlayerName
                    ?: "", null, userLanguage)
            notification["actionId"] = "2"
            notification["matchId"] = matchId
            notification["fromUsername"] = ""
            notification["fromUserId"] = mPlayerId!!
            notification["fromGpsId"] = ""
            notification["toUsername"] = ""
            notification["toUserId"] = ""
            notification["toGpsId"] = ""
            notification["timestamp"] = ts
            notifications.push().setValue(notification)

        }

    }

    private fun invitationNotAvailable(toUserId: String, matchId: String) {

//        Log.i(TAG, "invitationNotAvailable(), toUserId: $toUserId, matchId: $matchId")

        GlobalScope.launch(Dispatchers.Main) {

            val deferredUserLanguage = async { getUserLanguage(toUserId) }
            val userLanguage = deferredUserLanguage.await()
            deferredUserLanguage.cancel()

            val deferredTimestamp = async { getTimestampFromServer() }
            val ts = deferredTimestamp.await()
            deferredTimestamp.cancel()

            val ref = FirebaseDatabase.getInstance().getReference("/notifications/$toUserId")
            val notifications = ref.child("notificationRequests")
            val notification: HashMap<String, String> = HashMap()
            notification["title"] = getStringByLocal(this@MainActivity, R.string.invitationTitle, null, null, userLanguage)
            notification["message"] = getStringByLocal(this@MainActivity, R.string.playerNotAvailable, mPlayerName
                    ?: "", null, userLanguage)
            notification["actionId"] = "7"
            notification["matchId"] = matchId
            notification["fromUsername"] = mPlayerName ?: "???"
            notification["fromUserId"] = mPlayerId ?: ""
            notification["fromGpsId"] = ""
            notification["toUsername"] = ""
            notification["toUserId"] = toUserId
            notification["toGpsId"] = ""
            notification["timestamp"] = ts
            notifications.push().setValue(notification)

        }

    }

    private fun matchCanceled(toUserId: String, matchId: String, actionId: String) {

//        Log.i(TAG, "matchCanceled(), toUserId: $toUserId, matchId: $matchId, actionId: $actionId")

        GlobalScope.launch(Dispatchers.Main) {

            updateUserStatus(playerId = mPlayerId, playing = false, singleGame = ed.isSingleGame)

            val deferredUserLanguage = async { getUserLanguage(toUserId) }
            val userLanguage = deferredUserLanguage.await()
            deferredUserLanguage.cancel()

            val deferredTimestamp = async { getTimestampFromServer() }
            val ts = deferredTimestamp.await()
            deferredTimestamp.cancel()

            val ref = FirebaseDatabase.getInstance().getReference("/notifications/$toUserId")
            val notifications = ref.child("notificationRequests")
            val notification: HashMap<String, String> = HashMap()
            notification["title"] = getStringByLocal(this@MainActivity, R.string.info, null, null, userLanguage)
            notification["message"] = getStringByLocal(this@MainActivity, R.string.matchDeletedByPlayer, mPlayerName
                    ?: "", null, userLanguage)
            if (actionId == "8")
                notification["message"] = getStringByLocal(this@MainActivity, R.string.gameOver, null, null, userLanguage)
            notification["actionId"] = actionId
            notification["matchId"] = matchId
            notification["fromUsername"] = mPlayerName ?: ""
            notification["fromUserId"] = ""
            notification["fromGpsId"] = ""
            notification["toUsername"] = ""
            notification["toUserId"] = ""
            notification["toGpsId"] = ""
            notification["timestamp"] = ts
            notifications.push().setValue(notification)

        }

    }

    private fun setDiceValues(diceId: Int) {
        if (diceRoll[diceId] >= 0) {
            diceHold[diceId] = diceRoll[diceId]
            diceRoll[diceId] = -1
        } else {
            diceRoll[diceId] = diceHold[diceId]
            diceHold[diceId] = -1
        }

        diceView(diceRoll, diceHold)
    }

    private fun setHoldFast() {
        var cntDice = 0
        var cntDiceMax = 0
        var maxId = -1
        setDiceValues(false)
        for (i in diceValues.indices) {
            cntDice += diceValues[i]
            if (diceValues[i] >= cntDiceMax) {
                maxId = i
                cntDiceMax = diceValues[i]
            }
        }
        var cntHoldMax = 0
        for (i in diceHold.indices) {
            if (diceHold[i] == maxId)
                cntHoldMax++
        }
        if ((cntDice == 5) and (cntDiceMax > 1)) {
            for (i in diceHold.indices)
            // all hold ---> roll
            {
                if (diceHold[i] >= 0) {
                    diceRoll[i] = diceHold[i]
                    diceHold[i] = -1
                }
            }
            if (cntHoldMax < cntDiceMax)
            // maxId: roll ---> hold
            {
                for (i in diceRoll.indices) {
                    if (diceRoll[i] == maxId) {
                        diceHold[i] = diceRoll[i]
                        diceRoll[i] = -1
                    }
                }
            }
        }
    }

    private fun showMenuOnline() {
        val menuStartMatch = 0
        val menuCheckMatches = 1
        val menuCurrentMatch = 2
        val menuSettings = 3
        val menuInformation = 4
        val menuAdvertising = 5
        val menuOffline = 6
        val builderMenu = AlertDialog.Builder(this)
        builderMenu.setIcon(R.drawable.icon_dialog)
        builderMenu.setTitle(resources.getString(R.string.escaleroOnline))
        builderMenu.setCancelable(true)
        val arrayAdapter = ArrayAdapter<String>(this, R.layout.dialog_item)
        val actions = ArrayList<Int>()
        arrayAdapter.add(resources.getString(R.string.inviteFriends))
        actions.add(menuStartMatch)
        arrayAdapter.add(resources.getString(R.string.checkGames))
        actions.add(menuCheckMatches)
        if (isOnlineActive) {
            arrayAdapter.add(resources.getString(R.string.currentMatch))
            actions.add(menuCurrentMatch)
        }
        arrayAdapter.add(resources.getString(R.string.prefTitle))
        actions.add(menuSettings)
        arrayAdapter.add(resources.getString(R.string.info))
        actions.add(menuInformation)
        arrayAdapter.add(resources.getString(R.string.getEp))
        actions.add(menuAdvertising)
        arrayAdapter.add(">> ${resources.getString(R.string.offline)}")
        actions.add(menuOffline)
        builderMenu.setAdapter(
                arrayAdapter
        ) { _, which ->
            when (actions[which]) {
                menuStartMatch -> {
                    showPlayOnlineDialog()
                }
                menuCheckMatches -> {
                    checkMatches()
                }
                menuCurrentMatch -> {
                    if (mMatchId != null) {
                        startDialogCurrentMatch(mMatchId!!)
                    } else {
                        initOnline()
                        updateTable(true)
                        showPlayOnlineDialog()
                    }
                }
                menuSettings -> {
                    preferencesIntent = Intent(this, Preferences::class.java)
                    startActivityForResult(preferencesIntent, PREFERENCES_REQUEST_CODE)
                }
                menuInformation -> {
                    showInfoMenu()
                }
                menuAdvertising -> {
                    showAds()
                }
                menuOffline -> {
                    playOffline(true)
                }
            }
        }
        builderMenu.show()
    }

    private fun showMenuOffline() {
        val menuNewGame = 0
        val menuSettings = 3
        val menuInformation = 4
        val menuAdvertising = 5
        val menuOnline = 6
        val builderMenu = AlertDialog.Builder(this)
        builderMenu.setIcon(R.drawable.icon_dialog)
        builderMenu.setTitle(resources.getString(R.string.escaleroOffline))
        builderMenu.setCancelable(true)
        val arrayAdapter = ArrayAdapter<String>(
                this, R.layout.dialog_item)
        val actions = ArrayList<Int>()
        arrayAdapter.add(resources.getString(R.string.newGame))
        actions.add(menuNewGame)
        arrayAdapter.add(resources.getString(R.string.prefTitle))
        actions.add(menuSettings)
        arrayAdapter.add(resources.getString(R.string.info))
        actions.add(menuInformation)
        arrayAdapter.add(resources.getString(R.string.getEp))
        actions.add(menuAdvertising)
        arrayAdapter.add(">> ${resources.getString(R.string.online)}")
        actions.add(menuOnline)
        builderMenu.setAdapter(
                arrayAdapter
        ) { _, which ->
            when (actions[which]) {
                menuNewGame -> {
                    newGameIntent = Intent(this, NewGame::class.java)
                    startActivityForResult(newGameIntent, NEW_GAME_REQUEST_CODE)
                }
                menuSettings -> {
                    preferencesIntent = Intent(this, Preferences::class.java)
                    startActivityForResult(preferencesIntent, PREFERENCES_REQUEST_CODE)
                }
                menuInformation -> { showInfoMenu() }
                menuAdvertising -> { showAds() }
                menuOnline -> { playOnline(false) }
            }
        }
        builderMenu.show()
    }

    fun showAccountingDialog(text: String) {

//        Log.i(TAG, "showAccountingDialog(), text: $text")

        //Log.i(TAG, "showAccountingDialog(), ads: " + prefs.getBoolean("advertising", true) + ", showAds: " + showAds
        //        + ", adsCounter: " + adsCounter + ", adsCounterMaximum: " + adsCounterMaximum + ", mInterstitialAd: " + mInterstitialAd);

        if (mAccountingDialog != null) {
            if (mAccountingDialog!!.isShowing)
                return
        }

        val accountingDialog = AlertDialog.Builder(this)
        accountingDialog.setMessage(text)
        accountingDialog.setCancelable(true)
        accountingDialog.setOnCancelListener {
        }
        mAccountingDialog = accountingDialog.create()
        mAccountingDialog!!.show()

    }

    private fun showInfoMenu() {
        val builderMenu = AlertDialog.Builder(this)
        builderMenu.setIcon(R.drawable.icon_dialog)
        builderMenu.setTitle(resources.getString(R.string.menuInfo, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
        builderMenu.setCancelable(true)

        val arrayAdapter = ArrayAdapter<String>(
                this, R.layout.dialog_item)
        arrayAdapter.add(resources.getString(R.string.infoWhatsNew))
        arrayAdapter.add(resources.getString(R.string.infoGame))
        arrayAdapter.add(resources.getString(R.string.privacyPolicy))
        arrayAdapter.add(resources.getString(R.string.infoHomepage))
        arrayAdapter.add(resources.getString(R.string.infoContact))
        builderMenu.setAdapter(
                arrayAdapter
        ) { _, which ->
            infoIntent = Intent(Intent.ACTION_VIEW)
            GlobalScope.launch(Dispatchers.Main) {
                val userLanguage: String
                if (mPlayerId != null) {
                    val deferredUserLanguage = async { getUserLanguage(mPlayerId!!) }
                    userLanguage = deferredUserLanguage.await()
                    deferredUserLanguage.cancel()
                }
                else
                    userLanguage = Locale.getDefault().language

                when (which) {
                    0 -> {
                        when (userLanguage) {
                            "de" -> infoIntent.data = Uri.parse(URI_WHATS_NEW_DE)
                            else -> infoIntent.data = Uri.parse(URI_WHATS_NEW_EN)
                        }
                        startActivityForResult(infoIntent, INFO_REQUEST_CODE)
                    }
                    1 -> {
                        when (userLanguage) {
                            "de" -> infoIntent.data = Uri.parse(URI_MANUAL_DE)
                            else -> infoIntent.data = Uri.parse(URI_MANUAL_EN)
                        }
                        startActivityForResult(infoIntent, INFO_REQUEST_CODE)
                    }
                    2 -> {
                        when (userLanguage) {
                            "de" -> infoIntent.data = Uri.parse(URI_PRIVACY_POLICY_DE)
                            else -> infoIntent.data = Uri.parse(URI_PRIVACY_POLICY_EN)
                        }
                        startActivityForResult(infoIntent, INFO_REQUEST_CODE)
                    }
                    3 -> {
                        when (userLanguage) {
                            "de" -> infoIntent.data = Uri.parse(URI_HOMEPAGE_DE)
                            else -> infoIntent.data = Uri.parse(URI_HOMEPAGE_EN)
                        }
                        startActivityForResult(infoIntent, INFO_REQUEST_CODE)
                    }
                    4 -> {
                        val send = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", APP_EMAIL.toString(), null))
                        send.putExtra(Intent.EXTRA_SUBJECT, "")
                        send.putExtra(Intent.EXTRA_TEXT, "")
                        try {
                            startActivityForResult(send, R.string.sendEmail)
                        } catch (ignored: Exception) {
                        }

                    }
                }
            }
        }
        builderMenu.show()
    }

    private fun setDataUpdate() {
        ed.isFlipScreen = prefs.getBoolean("computeFlipScreen", false)
        if (diceState != prefs.getInt("dice", 1)) {
            diceState = prefs.getInt("dice", 1)
            isDiceBoard = prefs.getBoolean("isDiceBoard", isDiceBoard)
            isDouble1 = true
            ed.selectedGridItem = -1
            ed.diceText = ""
            ed.diceTextDouble1 = ""
            diceMode = 0
            flipMode = 0
            ed.isFlipScreen = false
            if (isOrientationReverse)
                computeFlipScreen(false)
            setRelativeLayout(diceState)
            if (diceState == 0)
                btnDice.visibility = ImageView.INVISIBLE
            if (diceState == 1) {
                diceMode = 1
                diceInit(diceRoll, diceHold)
                btnDice.setImageResource(R.drawable.button_dice)

                btnDice.visibility = ImageView.VISIBLE
                diceView(diceRoll, diceHold)
            }
        } else {
            if (isDiceBoard != prefs.getBoolean("isDiceBoard", isDiceBoard)) {
                isDiceBoard = prefs.getBoolean("isDiceBoard", isDiceBoard)
                setRelativeLayout(diceState)
            }
        }
        ed.isPlayerColumn = prefs.getBoolean("isPlayerColumn", false)
        ed.isSummation = prefs.getBoolean("isSummation", false)
        if (ed.playerNumber == 3) {
            ed.isFlipScreen = false
            ed.isSummation = true
        }
        ed.pointsColumn1 = prefs.getInt("pointsCol1", 1)
        ed.pointsColumn2 = prefs.getInt("pointsCol2", 2)
        ed.pointsColumn3 = prefs.getInt("pointsCol3", 4)
        ed.pointsBonus = prefs.getInt("pointsBon", 2)
        ed.payoutMultiplier = prefs.getInt("multiplier", 1)
        ed.monetaryUnit = prefs.getString("unit", resources.getString(R.string.points))
        ed.setGridControl()
        ed.setDiceIconValues(prefs)
        ed.isSingleGame = prefs.getBoolean("isSingleGame", true)
        ed.playerNumber = prefs.getInt("players", 2)

        tableView.initTable(ed, orientation, displayWidth, displayHeight)

        updateTable(true)
    }

    private fun setDiceActions() {
        if (diceState == 1) {
            isDiced = false
            if (flipMode != 2)
                setToPrev()
            btnDice.visibility = ImageView.VISIBLE
            diceMode = 5
            diceView(diceRoll, diceHold)
        } else
            btnDice.visibility = ImageView.INVISIBLE
        setSelectedItem(ed.selectedGridItem)
    }

    private fun computeFlipScreen(setOrientationReverse: Boolean) {
        orientation = this.resources.configuration.orientation
        if (ed.isFlipScreen) {

            requestedOrientation =
            if (isOrientationReverse) {
                if (orientation == Configuration.ORIENTATION_PORTRAIT)
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

            } else {
                if (orientation == Configuration.ORIENTATION_PORTRAIT)
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                else
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            }

            if (!setOrientationReverse) {
                isOrientationReverse = !isOrientationReverse
            }
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        }
    }

    private fun initDiceArrays() {
        for (i in diceRoll.indices) {
            diceRoll[i] = -1
            diceHold[i] = -1
            diceDouble1[i] = -1
            diceRollPrev[i] = -1
            diceHoldPrev[i] = -1
            dbHold[i] = -1
        }
    }

    private fun initDiceBoardRollValues() {
        for (i in diceRoll.indices) {
            for (j in 0..2) {
                if (j == 2)
                    dbRollValues[i][j] = rand.nextInt(360)
                else
                    dbRollValues[i][j] = 0
            }
        }
    }

    fun setToPrev() {
        diceModePrev = diceMode
        diceMode = 1
        for (i in diceRoll.indices) {
            diceRollPrev[i] = diceRoll[i]
            diceHoldPrev[i] = diceHold[i]
        }
        for (i in diceRoll.indices) {
            if (diceRoll[i] >= 0) {
                diceHold[i] = diceRoll[i]
                diceRoll[i] = -1
            }
        }
    }

    private fun getFromPrev() {
        diceMode = diceModePrev
        diceModePrev = 0
        for (i in diceRollPrev.indices) {
            diceRoll[i] = diceRollPrev[i]
            diceHold[i] = diceHoldPrev[i]
        }
    }

    private fun diceInit(diceRoll: IntArray, diceHold: IntArray) {
        for (i in diceRoll.indices) {
            diceRoll[i] = -2
            diceHold[i] = -1
        }
        diceView(diceRoll, diceHold)
    }

    fun diceAction(diceRoll: IntArray, diceHold: IntArray, fromButton: Boolean) {

//        Log.i(TAG, "1 diceAction(), diceMode: " + diceMode + ", isDouble1: " + isDouble1 + ", fromButton: " + fromButton);

        if (fromButton)
            playSound(3, 0)

        isDicing = true
        isServed = false
        if (!ed.isSingleGame and isDouble1)
            isServedDouble1 = false
        var cntDiceRoll = 0
        if (diceMode <= 1) {
            for (i in diceRoll.indices) {
                diceRoll[i] = -2
                diceHold[i] = -1
                if (isDouble1)
                    diceDouble1[i] = -1
            }
        }

        for (i in diceTime.indices) {
            diceTime[i] = -1
            diceAnimate[i] = -1
        }
        startAnimationTime = System.currentTimeMillis()
        endAnimationTime = startAnimationTime + 5000
        var stopAnimationTime = startAnimationTime
        var isFirst = true
        for (i in diceRoll.indices) {
            if ((diceRoll[i] >= 0) or (diceRoll[i] == -2)) {
                diceRoll[i] = rand.nextInt(6)
                isDiced = true
                if (diceRoll[i] >= 0)
                    cntDiceRoll++
                var t = ANIMATE_TIME
                if (isFirst) {
                    t = FIRST_ANIMATE_TIME
                    isFirst = false
                }
                stopAnimationTime += t
                diceTime[i] = stopAnimationTime
                diceAnimate[i] = diceRoll[i]
            }
        }

        if (!fromButton) {
            for (i in diceRoll.indices) {
                diceRoll[i] = -1
                diceHold[i] = -1
                cntDiceRoll = 0
            }
        }

        //Log.i(TAG, "1 diceAction(), diceRoll[]: " + diceRoll[0] + ", "  + diceRoll[1] + ", " + diceRoll[2] + ", " + diceRoll[3] + ", " + diceRoll[4])
        //Log.i(TAG, "2 diceAction(), diceTime[]: " + diceTime[0] + ", "  + diceTime[1] + ", " + diceTime[2] + ", " + diceTime[3] + ", " + diceTime[4])

        if (cntDiceRoll == 5) {
            if (!ed.isSingleGame and isDouble1)
                isServedDouble1 = true
            isServed = true
        }
        setDiceValues(false)
        if (isDiceBoard) {
            startAnimationTime = System.currentTimeMillis()
            val animate = ANIMATE_TIME
            endAnimationTime = startAnimationTime + animate
        }

        handlerAnimationDices.removeCallbacks(mUpdateAnimationDices)
        isInitRound = true
        handlerAnimationDices.postDelayed(mUpdateAnimationDices, 0)

    }

    private fun setDiceValues(fromDouble: Boolean) {
        for (i in diceValues.indices) {
            diceValues[i] = 0
            diceValuesDouble1[i] = 0
        }
        if (fromDouble and !ed.isSingleGame and (diceDouble1[0] >= 0)) {
            for (aDiceDouble1 in diceDouble1) {
                if (aDiceDouble1 >= 0)
                    diceValuesDouble1[aDiceDouble1] = diceValuesDouble1[aDiceDouble1] + 1
            }
        } else {
            for (i in diceRoll.indices) {
                if (diceRoll[i] >= 0)
                    diceValues[diceRoll[i]] = diceValues[diceRoll[i]] + 1
                else {
                    if (diceHold[i] >= 0)
                        diceValues[diceHold[i]] = diceValues[diceHold[i]] + 1
                }
            }
        }
    }

    private fun setDiceDouble1() {
        diceModePrev = diceMode
        for (i in diceRoll.indices) {
            if (diceRoll[i] >= 0)
                diceDouble1[i] = diceRoll[i]
            else {
                if (diceHold[i] >= 0)
                    diceDouble1[i] = diceHold[i]
            }
            diceRollPrev[i] = diceRoll[i]
            diceHoldPrev[i] = diceHold[i]
            when (i) {
                0 -> diceD1.setImageResource(getImageId(diceDouble1[i]))
                1 -> diceD2.setImageResource(getImageId(diceDouble1[i]))
                2 -> diceD3.setImageResource(getImageId(diceDouble1[i]))
                3 -> diceD4.setImageResource(getImageId(diceDouble1[i]))
                4 -> diceD5.setImageResource(getImageId(diceDouble1[i]))
            }

        }
        for (i in diceRoll.indices) {
            diceRollPrev[i] = diceRoll[i]
            diceHoldPrev[i] = diceHold[i]
            diceRoll[i] = -1
            diceHold[i] = -1
        }
    }

    fun diceView(diceRoll: IntArray, diceHold: IntArray) {

//        Log.i(TAG, "diceView(), diceMode: " + diceMode + ", diceModePrev: "  + diceModePrev)

        for (i in diceRoll.indices) {
            if (diceRoll[i] >= 0) {
                when (i) {
                    0 -> diceA1.setImageResource(getImageId(diceRoll[i]))
                    1 -> diceA2.setImageResource(getImageId(diceRoll[i]))
                    2 -> diceA3.setImageResource(getImageId(diceRoll[i]))
                    3 -> diceA4.setImageResource(getImageId(diceRoll[i]))
                    4 -> diceA5.setImageResource(getImageId(diceRoll[i]))
                }
            } else {
                if (diceRoll[i] < 0) {
                    when (i) {
                        0 -> diceA1.setImageResource(R.drawable.qm)
                        1 -> diceA2.setImageResource(R.drawable.qm)
                        2 -> diceA3.setImageResource(R.drawable.qm)
                        3 -> diceA4.setImageResource(R.drawable.qm)
                        4 -> diceA5.setImageResource(R.drawable.qm)
                    }
                }
            }
        }

        if ((diceMode == 1 && diceModePrev == 4)) {
            // online, take turn!
            for (i in diceRoll.indices) {
                if (diceRoll[i] >= 0) {
                    when (i) {
                        0 -> diceA1.setImageResource(getImageId(diceRoll[i]))
                        1 -> diceA2.setImageResource(getImageId(diceRoll[i]))
                        2 -> diceA3.setImageResource(getImageId(diceRoll[i]))
                        3 -> diceA4.setImageResource(getImageId(diceRoll[i]))
                        4 -> diceA5.setImageResource(getImageId(diceRoll[i]))
                    }
                } else {
                    if (diceHold[i] >= 0) {
                        when (i) {
                            0 -> diceA1.setImageResource(getImageId(diceHold[i]))
                            1 -> diceA2.setImageResource(getImageId(diceHold[i]))
                            2 -> diceA3.setImageResource(getImageId(diceHold[i]))
                            3 -> diceA4.setImageResource(getImageId(diceHold[i]))
                            4 -> diceA5.setImageResource(getImageId(diceHold[i]))
                        }
                    }
                }
            }
        } else  {
            for (i in diceHold.indices) {
                if (diceHold[i] >= 0) {
                    when (i) {
                        0 -> diceA1.setImageBitmap(getHoldBitmap(getImageId(diceHold[i])))
                        1 -> diceA2.setImageBitmap(getHoldBitmap(getImageId(diceHold[i])))
                        2 -> diceA3.setImageBitmap(getHoldBitmap(getImageId(diceHold[i])))
                        3 -> diceA4.setImageBitmap(getHoldBitmap(getImageId(diceHold[i])))
                        4 -> diceA5.setImageBitmap(getHoldBitmap(getImageId(diceHold[i])))
                    }
                }
            }
        }

        when (diceMode) {
            0 -> btnDice.setImageResource(R.drawable.button_dice)
            1 -> btnDice.setImageResource(R.drawable.button_dice)
            2 -> btnDice.setImageResource(R.drawable.button_dice)
            3 -> btnDice.setImageResource(R.drawable.button_dice)
            5 -> btnDice.setImageResource(R.drawable.button_dice)
            else -> btnDice.setImageResource(R.drawable.button_dice)

        }

        btnDice.visibility = ImageView.VISIBLE
        if ((diceMode == 4) and !isEnginePlayer(ed.playerToMove))
            btnDice.visibility = ImageView.INVISIBLE

        if (prefs.getBoolean("playOnline", false) && isOnlineEntry)
            btnDice.visibility = ImageView.INVISIBLE

//        Log.i(TAG, "diceView(), diceMode: " + diceMode + ", isDoingTurn: "  + isDoingTurn + ", isOnlineEntry: "  + isOnlineEntry)
//        Log.i(TAG, "diceView(), diceMode: $diceMode, diceModePrev: $diceModePrev, ed.isSingleGame: ${ed.isSingleGame}, isDouble1: $isDouble1")
//        Log.i(TAG, "A diceView(), setOnlineMessage()")

        setOnlineMessage()

        if (ed.isSingleGame) {
            diceD1.visibility = ImageView.INVISIBLE
            diceD2.visibility = ImageView.INVISIBLE
            diceD3.visibility = ImageView.INVISIBLE
            diceD4.visibility = ImageView.INVISIBLE
            diceD5.visibility = ImageView.INVISIBLE
            doubleA.visibility = ImageView.INVISIBLE
        } else {
//            if (isDouble1 && !(diceMode == 1 && diceModePrev == 4)) {
            if (!prefs.getBoolean("playOnline", false) && isDouble1 && !(diceMode == 1 && diceModePrev == 4)) {
                diceD1.visibility = ImageView.INVISIBLE
                diceD2.visibility = ImageView.INVISIBLE
                diceD3.visibility = ImageView.INVISIBLE
                diceD4.visibility = ImageView.INVISIBLE
                diceD5.visibility = ImageView.INVISIBLE
                doubleA.visibility = ImageView.INVISIBLE
                if (diceMode >= 2)
                    doubleA.visibility = ImageView.VISIBLE
            } else {
                for (i in diceDouble1.indices) {
                    when (i) {
                        0 -> diceD1.setImageResource(getImageId(diceDouble1[i]))
                        1 -> diceD2.setImageResource(getImageId(diceDouble1[i]))
                        2 -> diceD3.setImageResource(getImageId(diceDouble1[i]))
                        3 -> diceD4.setImageResource(getImageId(diceDouble1[i]))
                        4 -> diceD5.setImageResource(getImageId(diceDouble1[i]))
                    }
                }
                diceD1.visibility = ImageView.VISIBLE
                diceD2.visibility = ImageView.VISIBLE
                diceD3.visibility = ImageView.VISIBLE
                diceD4.visibility = ImageView.VISIBLE
                diceD5.visibility = ImageView.VISIBLE
//                if (isDouble1 && diceMode == 1) {
                if (!prefs.getBoolean("playOnline", false) && isDouble1 && diceMode == 1) {
                    diceD1.visibility = ImageView.INVISIBLE
                    diceD2.visibility = ImageView.INVISIBLE
                    diceD3.visibility = ImageView.INVISIBLE
                    diceD4.visibility = ImageView.INVISIBLE
                    diceD5.visibility = ImageView.INVISIBLE
                }

                if ((diceMode == 1) and ((diceModePrev == 2) or (diceModePrev == 3)))
                    doubleA.visibility = ImageView.VISIBLE
                else
                    doubleA.visibility = ImageView.INVISIBLE

            }
        }

        if (isEnginePlayer(ed.playerToMove))
            doubleA.visibility = ImageView.INVISIBLE
        if (!ed.isSingleGame && prefs.getBoolean("playOnline", false) && isDouble1 && diceMode >= 2)
            doubleA.visibility = ImageView.VISIBLE

        if (flipMode == 1) {
            btnDice.setImageResource(R.drawable.button_flip)
            btnDice.visibility = ImageView.VISIBLE
        }
        setSelectedItem(ed.selectedGridItem)
    }

    private fun setOnlineMessage() {

//        Log.i(TAG, "setOnlineMessage(), online: ${prefs.getBoolean("playOnline", false)}, mIsSignIn: $mIsSignIn, isOnlineActive: $isOnlineActive")
//        Log.i(TAG, "setOnlineMessage(), isOnlineActive: $isOnlineActive, isDoingTurn: $isDoingTurn, diceMode: $diceMode, diceModePrev: $diceModePrev")

        if (prefs.getBoolean("playOnline", false) && isOnlineActive) {
            btnPlayerResult.visibility = TextView.VISIBLE
            btnPlayerRound.visibility = TextView.VISIBLE
            if (isDoingTurn) {
                when (diceMode) {
                    0, 1 -> {
                        btnDice.visibility = ImageView.VISIBLE
                        btnDice.setImageResource(R.drawable.button_dice)

                        btnPlayerResult.text = resources.getString(R.string.yourTurn)
                    }
                    2 -> {
                        btnDice.visibility = ImageView.VISIBLE
                        btnDice.setImageResource(R.drawable.button_dice)

                        btnPlayerResult.text = resources.getString(R.string.yourTurn)
                    }
                    3 -> {
                        btnDice.visibility = ImageView.VISIBLE
                        btnDice.setImageResource(R.drawable.button_dice)

                        btnPlayerResult.text = resources.getString(R.string.yourTurn)
                    }
                    4 -> {
                        btnPlayerRound.visibility = TextView.INVISIBLE
                    }
                    5 -> {
                        btnPlayerRound.visibility = TextView.INVISIBLE
                        if (isOnlineEntry) {
                            if (mPlayerABC == ed.getNextPlayerAB(ed.playerStart).toString()) {

//                                Log.i(TAG, "setOnlineMessage(), entry correction, diceMode: $diceMode --> $diceModePrev")

                                diceMode = diceModePrev
                                diceView(diceRoll, diceHold)
                            } else {
                                if (btnPlayerResult.text != resources.getString(R.string.confirmEntry))
                                    btnPlayerResult.text = resources.getString(R.string.dataUpdate)
                            }
                        } else
                            btnPlayerResult.text = resources.getString(R.string.waitingFor)
                    }
                }
            }
            else {
                btnDice.setImageResource(R.drawable.button_stop)
                btnPlayerRound.visibility = TextView.VISIBLE
                when (diceMode) {
                    0, 1, 2, 3 -> btnPlayerResult.text = resources.getString(R.string.waitingFor)
                    4, 5 -> {
                        btnPlayerResult.text = resources.getString(R.string.waitForOk)
                        btnPlayerRound.visibility = TextView.INVISIBLE
                    }
                }
            }

        }

        if (prefs.getBoolean("playOnline", false)) {
            if (!isOnlineActive)
                setNoActiveMatch()
            if (!mIsSignIn)
                btnPlayerResult.text = resources.getString(R.string.connectToServer)
        }

    }

    private fun getImageId(diceValue: Int): Int {
        var imageId = 0
        when (diceValue) {
            0 -> imageId = ed.icon1
            1 -> imageId = ed.icon2
            2 -> imageId = ed.icon3
            3 -> imageId = ed.icon4
            4 -> imageId = ed.icon5
            5 -> imageId = ed.icon6
        }
        return imageId
    }

    private fun getHoldBitmap(imageId: Int): Bitmap {
        // hold BORDER
        val bm = BitmapFactory.decodeResource(this.resources, imageId).copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas()
        canvas.setBitmap(bm)
        val paint = Paint()
        val stroke = canvas.width / 5
        paint.strokeWidth = stroke.toFloat()
        paint.color = ContextCompat.getColor(this, R.color.colorHoldRed)
        paint.style = Paint.Style.STROKE
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        if (diceMode < 4)
            canvas.drawRect(0f, 0f, width, height, paint)
        return bm
    }

    private fun playSound(idx: Int, loop: Int) {
        if (prefs.getBoolean("sounds", true))
            mSoundPool!!.play(soundsMap[idx]!!, 0.2f, 0.2f, 1, loop, 1.0f)
    }

    private fun setSelectedItem(id: Int) {
        ed.selectedGridItem = id
    }

    fun setBtnPlayer(playerId: Char) {

//        Log.i(TAG, "setBtnPlayer(), playerId: $playerId, diceMode: $diceMode, diceModePrev: $diceModePrev")

        if (ed.isGameOver && !prefs.getBoolean("playOnline", false)) {
            gameOver()
            return
        }
        var name: String? = ""
        var info = ""
        setDiceValues(true)
        val diceTextDouble = ed.getDiceText(true, isServedDouble1, diceValuesDouble1, 3)
        setDiceValues(false)
        val diceText = ed.getDiceText(false, isServed, diceValues, diceMode)
        btnPlayerName.text = name
        btnPlayerIcon.visibility = TextView.VISIBLE
        when (playerId) {
            'A' -> {
                name =
                        if (!prefs.getBoolean("enginePlayerA", true) or (diceState == 0) or prefs.getBoolean("playOnline", false)) {
                            btnPlayerIcon.setImageResource(R.drawable.button_human_a)
                            if (prefs.getBoolean("playOnline", false))
                                mMatchBaseData?.nameA ?: "???"
                            else
                                prefs.getString("nameA", resources.getString(R.string.yourName))
                        } else {
                            btnPlayerIcon.setImageResource(R.drawable.button_mobile_a)
                            runPrefs.getString("engineNameA", "")
                        }
                btnPlayerRound.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerA))
                btnPlayerName.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerA))
                btnPlayerInfo.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerA))
                btnPlayerInfo2.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerA))
                if (diceMode == 5) {
                    btnPlayerInfo.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerB))
                    btnPlayerInfo2.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerB))
                    if (ed.playerNumber == 3) {
                        btnPlayerInfo.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerC))
                        btnPlayerInfo2.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerC))
                    }
                }
                btnPlayerResult.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerA))
            }
            'B' -> {
                name =
                        if (!prefs.getBoolean("enginePlayerB", false) or (diceState == 0) or prefs.getBoolean("playOnline", true)) {
                            btnPlayerIcon.setImageResource(R.drawable.button_human_b)
                            if (prefs.getBoolean("playOnline", false))
                                mMatchBaseData?.nameB ?: "???"
                            else
                                prefs.getString("nameB", resources.getString(R.string.yourName))
                        } else {
                            btnPlayerIcon.setImageResource(R.drawable.button_mobile_b)
                            runPrefs.getString("engineNameB", "")
                        }

                btnPlayerRound.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerB))
                btnPlayerName.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerB))
                btnPlayerInfo.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerB))
                btnPlayerInfo2.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerB))
                if (diceMode == 5) {
                    btnPlayerInfo.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerA))
                    btnPlayerInfo2.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerA))
                }
                btnPlayerResult.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerB))
            }
            'C' -> {
                name =
                        if (!prefs.getBoolean("enginePlayerC", false) or (diceState == 0) or prefs.getBoolean("playOnline", false)) {
                            btnPlayerIcon.setImageResource(R.drawable.button_human_c)
                            prefs.getString("nameC", resources.getString(R.string.yourName))
                        } else {
                            btnPlayerIcon.setImageResource(R.drawable.button_mobile_c)
                            runPrefs.getString("engineNameC", "")
                        }
                btnPlayerRound.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerC))
                btnPlayerName.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerC))
                btnPlayerInfo.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerC))
                btnPlayerInfo2.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerC))
                if (diceMode == 5) {
                    btnPlayerInfo.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerB))
                    btnPlayerInfo2.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerB))
                }
                btnPlayerResult.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerC))
            }
        }
        if ((diceState == 1) and !ed.isSingleGame) {
            var doublePlayer = 1

//            Log.i(TAG, "setBtnPlayer(), playerId: $playerId, diceMode: $diceMode, diceModePrev: $diceModePrev, flipMode: $flipMode, isDouble1: $isDouble1")

            if (!isDouble1 or (isDouble1 and (diceMode >= 4))) {
                doublePlayer = 2
                if (diceMode == 5 || (!isDouble1 && diceMode == 1 && flipMode == 1))
                    doublePlayer = 1
            }
            name = "$name ($doublePlayer)"
        }
        if (name != "") {
            btnPlayerName.text = name
            btnPlayerName.visibility = TextView.VISIBLE
        } else
            btnPlayerName.visibility = TextView.INVISIBLE

        var diceTry = diceMode
        if (diceTry == 5)
            diceTry = 1

        if ((diceState == 1) and (diceMode >= 1))
            info = diceText

        var round = ""
        btnPlayerInfo.visibility = TextView.VISIBLE
        btnPlayerResult.visibility = TextView.INVISIBLE
        when (diceTry) {
            0, 1, 2, 3 -> if (diceState == 1) {
                if (diceTry == 0)
                    diceTry = 1
                round = "$diceTry.>"
            } else {
                btnPlayerResult.visibility = TextView.VISIBLE
                btnPlayerResult.text = resources.getString(R.string.resultTable)
            }
            4 -> if ((diceState == 1) and !isEnginePlayer(playerId)) {
                btnPlayerResult.visibility = TextView.VISIBLE
                btnPlayerResult.text = resources.getString(R.string.diceEnterResult)
            } else {
                if (diceState == 0) {
                    btnPlayerResult.visibility = TextView.VISIBLE
                    btnPlayerResult.text = resources.getString(R.string.resultValue)
                    diceMode = 0
                }
            }
        }

        if (prefs.getBoolean("playOnline", false) && isOnlineActive) {
            btnPlayerResult.visibility = TextView.VISIBLE
        }

        if (!ed.isSingleGame and !isDouble1 and (diceTextDouble != "")) {
            btnPlayerInfo2.text = diceTextDouble
            btnPlayerInfo2.visibility = TextView.VISIBLE
        } else
            btnPlayerInfo2.visibility = TextView.INVISIBLE
        if (info != "") {
            if (diceMode == 1 && diceModePrev == 4) {
                if (ed.playerToMove == 'A')
                    btnPlayerInfo.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerB))
                if (ed.playerToMove == 'B')
                    btnPlayerInfo.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerA))
            }
            btnPlayerInfo.text = info
            btnPlayerInfo.visibility = TextView.VISIBLE
        } else
            btnPlayerInfo.visibility = TextView.INVISIBLE

        if (round != "") {
            btnPlayerRound.text = round
            btnPlayerRound.visibility = TextView.VISIBLE
        } else
            btnPlayerRound.visibility = TextView.INVISIBLE

//        Log.i(TAG, "B setBtnPlayer(), setOnlineMessage()")

        setOnlineMessage()

    }

    private fun setRunPrefs() {
        val edi = runPrefs.edit()
        edi.putString("A0", ed.getPlayerPrefs('A', 0))
        edi.putString("A1", ed.getPlayerPrefs('A', 1))
        edi.putString("A2", ed.getPlayerPrefs('A', 2))
        edi.putString("B0", ed.getPlayerPrefs('B', 0))
        edi.putString("B1", ed.getPlayerPrefs('B', 1))
        edi.putString("B2", ed.getPlayerPrefs('B', 2))
        edi.putString("C0", ed.getPlayerPrefs('C', 0))
        edi.putString("C1", ed.getPlayerPrefs('C', 1))
        edi.putString("C2", ed.getPlayerPrefs('C', 2))

//        Log.i(TAG, "setRunPrefs(), diceMode: " + diceMode + ", diceModePrev: " + diceModePrev + ", ed.selectedGridItem: " + ed.selectedGridItem)

        edi.putInt("selectedGridItem", ed.selectedGridItem)
        edi.putInt("selectedCol", selectedCol)
        edi.putInt("selectedRow", selectedRow)
        edi.putString("playerStart", ed.playerStart.toString())
        edi.putString("playerToMove", ed.playerToMove.toString())

        edi.putBoolean("isDiced", isDiced)
        edi.putBoolean("isServed", isServed)
        edi.putInt("diceModus", diceMode)
        edi.putInt("flipModus", 0)
        edi.putBoolean("isOrientationReverse", false)
        var str = ""
        for (aDiceRoll in diceRoll) {
            str = "$str$aDiceRoll "
        }
        edi.putString("diceRoll", str)
        edi.putInt("diceModusPrev", diceModePrev)

        str = ""
        for (aDiceHold in diceHold) {
            str = "$str$aDiceHold "
        }
        edi.putString("diceHold", str)
        str = ""
        for (aDiceRollPrev in diceRollPrev) {
            str = "$str$aDiceRollPrev "
        }
        edi.putString("diceRollPrev", str)
        str = ""
        for (aDiceHoldPrev in diceHoldPrev) {
            str = "$str$aDiceHoldPrev "
        }
        edi.putString("diceHoldPrev", str)

//        Log.i(TAG, "setRunPrefs(), isDouble1: $isDouble1")

        // double values
        if (!prefs.getBoolean("isSingleGame", true)) {
            edi.putBoolean("isServedDouble1", isServedDouble1)
            edi.putBoolean("isDouble1", isDouble1)
        } else {
            edi.putBoolean("isServedDouble1", false)
            edi.putBoolean("isDouble1", false)
        }
        str = ""
        for (aDiceDouble1 in diceDouble1) {
            str = "$str$aDiceDouble1 "
        }
        edi.putString("diceDouble1", str)
        edi.putString("diceText", ed.diceText)                     // canceled
        edi.putString("diceTextDouble1", ed.diceTextDouble1)       // canceled

        edi.apply()

    }

    private fun getRunPrefs() {
        isDiced = runPrefs.getBoolean("isDiced", false)
        isServed = runPrefs.getBoolean("isServed", false)
        flipMode = runPrefs.getInt("flipModus", 0)
        diceMode = runPrefs.getInt("diceModus", 0)
        diceModePrev = runPrefs.getInt("diceModusPrev", 0)

//        Log.i(TAG, "getRunPrefs(), diceMode: " + diceMode + ", diceModePrev: " + diceModePrev)

        if (runPrefs.getString("diceRoll", "") == "")
            initDiceArrays()
        var strSp: Array<String>
        if (runPrefs.getString("diceRoll", "") != "") {
            strSp = runPrefs.getString("diceRoll", "")!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (strSp.size == diceRoll.size) {
                for (i in diceRoll.indices) {
                    diceRoll[i] = Integer.parseInt(strSp[i])
                }
            }
        }
        if (runPrefs.getString("diceHold", "") != "") {
            strSp = runPrefs.getString("diceHold", "")!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (strSp.size == diceHold.size) {
                for (i in diceHold.indices) {
                    diceHold[i] = Integer.parseInt(strSp[i])
                }
            }
        }
        if (runPrefs.getString("diceRollPrev", "") != "") {
            strSp = runPrefs.getString("diceRollPrev", "")!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (strSp.size == diceRollPrev.size) {
                for (i in diceRollPrev.indices) {
                    diceRollPrev[i] = Integer.parseInt(strSp[i])
                }
            }
        }
        if (runPrefs.getString("diceHoldPrev", "") != "") {
            strSp = runPrefs.getString("diceHoldPrev", "")!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (strSp.size == diceHoldPrev.size) {
                for (i in diceHoldPrev.indices) {
                    diceHoldPrev[i] = Integer.parseInt(strSp[i])
                }
            }
        }

        // double values
        isServedDouble1 = runPrefs.getBoolean("isServedDouble1", false)
        isDouble1 = runPrefs.getBoolean("isDouble1", false)

//        Log.i(TAG, "getRunPrefs(), isDouble1: $isDouble1")
//        Log.i(TAG, "getRunPrefs(), diceDouble1: ${runPrefs.getString("diceDouble1", "")}")

        if (runPrefs.getString("diceDouble1", "") != "") {
            strSp = runPrefs.getString("diceDouble1", "")!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (strSp.size == diceHold.size) {
                for (i in diceHold.indices) {
                    diceDouble1[i] = Integer.parseInt(strSp[i])
                }
            }
        }
        ed.diceText = runPrefs.getString("diceText", ed.diceText)!!
        ed.diceTextDouble1 = runPrefs.getString("diceTextDouble1", "")!!

        selectedCol = runPrefs.getInt("selectedCol", selectedCol)
        selectedRow = runPrefs.getInt("selectedRow", selectedRow)

    }

    private fun initRunPrefs() {
        val edi = runPrefs.edit()
        edi.putString("A0", EscaleroData.PREFS_DEFAULT)
        edi.putString("A1", EscaleroData.PREFS_DEFAULT)
        edi.putString("A2", EscaleroData.PREFS_DEFAULT)
        edi.putString("B0", EscaleroData.PREFS_DEFAULT)
        edi.putString("B1", EscaleroData.PREFS_DEFAULT)
        edi.putString("B2", EscaleroData.PREFS_DEFAULT)
        edi.putString("C0", EscaleroData.PREFS_DEFAULT)
        edi.putString("C1", EscaleroData.PREFS_DEFAULT)
        edi.putString("C2", EscaleroData.PREFS_DEFAULT)
        ed.selectedGridItem = -1
        setSelectedItem(ed.selectedGridItem)
        edi.putInt("selectedGridItem", ed.selectedGridItem)
        edi.apply()
        isDiced = false
        isServed = false
        isServedDouble1 = false
    }

    // 2D
    private fun animation(initRound: Boolean) {

//        Log.i(TAG, "animation(), initRound: $initRound, diceMode: $diceMode, diceModePrev: $diceModePrev")

        if (diceMode == 1) {
            diceA1.visibility = ImageView.VISIBLE
            diceA2.visibility = ImageView.VISIBLE
            diceA3.visibility = ImageView.VISIBLE
            diceA4.visibility = ImageView.VISIBLE
            diceA5.visibility = ImageView.VISIBLE
        }
        val currentTime = System.currentTimeMillis()
        var cnt = 0
        for (i in diceTime.indices) {
            if ((diceTime[i] != -1L) and (currentTime > diceTime[i])) {
                diceTime[i] = -1
                diceAnimate[i] = -1
                when (i) {
                    0 -> diceA1.setImageResource(getImageId(diceRoll[i]))
                    1 -> diceA2.setImageResource(getImageId(diceRoll[i]))
                    2 -> diceA3.setImageResource(getImageId(diceRoll[i]))
                    3 -> diceA4.setImageResource(getImageId(diceRoll[i]))
                    4 -> diceA5.setImageResource(getImageId(diceRoll[i]))
                }
            }
            if (diceTime[i] == -1L)
                cnt++
        }

        if ((cnt == 5) or (currentTime > endAnimationTime)) {
            ed.getDiceText(isDouble1, isServed, diceValues, diceMode)

            val oldDiceMode = diceMode


            if (prefs.getBoolean("playOnline", false) && diceMode == 5 && !initRound) {

//                Log.i(TAG, "animation(), return")

                return
            }

            if (!initRound and (diceMode < 4))
                isInitRound = true
            else {
                if (diceMode < 2)
                    diceMode = 2
                else {
                    if (diceMode < 4)
                        diceMode++
                }
            }

            if ((diceMode == 4) and !isEnginePlayer(ed.playerToMove)) {
                if (!ed.isSingleGame and isDouble1) {
                    isDouble1 = false
                    diceMode = 1
                    setDiceDouble1()
                    setDiceValues(true)
                    ed.getDiceText(isDouble1, isServedDouble1, diceValuesDouble1, 0)
                    val txt = ed.diceText
                    ed.getDiceText(true, isServedDouble1, diceValuesDouble1, 0)
                    ed.diceTextDouble1 = ed.diceText
                    ed.diceText = txt
                    isDicing = false
                    handlerAnimationDices.removeCallbacks(mUpdateAnimationDices)

                    diceAction(diceRoll, diceHold, false)
                } else {
                    ed.getDiceText(isDouble1, isServed, diceValues, 4)
                    btnDice.visibility = ImageView.INVISIBLE
                }
            } else {
                btnDice.visibility = ImageView.VISIBLE
            }
            diceView(diceRoll, diceHold)

            if (isOnlineActive && diceMode == 5)
                updateTable(true)
            else
                updateTable(false)

            isDicing = false

            if (isEnginePlayer(ed.playerToMove) and (oldDiceMode <= 3))
                performEngineCommands(getEngineDiceCommand(ed.playerToMove, oldDiceMode, diceRoll, diceHold, diceDouble1, isServedDouble1))

//            Log.i(TAG, "animation(), matchDataToDb(), isOnlineActive: $isOnlineActive, isDoingTurn: $isDoingTurn")

            if (prefs.getBoolean("playOnline", false) && isOnlineActive && isDoingTurn) {
                matchDataToDb(false, ONLINE_ACTIVE)
            }

            if (prefs.getBoolean("playOnline", false)) {
                txtPlayers.visibility = TextView.VISIBLE
                txtPlayerEps.visibility = TextView.VISIBLE
                var players = ""
                if (mMatchId != null)
                    players = getPlayersFromMatch(mMatchId!!)
                if (players == "" && !isOnlineActive) {
                    players = if (mPlayerName == null)
                        ""
                    else
                        "$mPlayerName: $mPlayerEp EP"
                }
                txtPlayers.text = players
                if (players.isNotEmpty() && players.contains("-") && !players.contains("???"))
                    txtPlayerEps.text = getPlayersEp()

//                Log.i(TAG, "9 animation(), txtPlayers: ${txtPlayers.text}")

            }
            else {
                txtPlayers.visibility = TextView.INVISIBLE
                txtPlayerEps.visibility = TextView.INVISIBLE
            }

            handlerAnimationDices.removeCallbacks(mUpdateAnimationDices)

        } else {
            for (i in diceAnimate.indices) {
                if (diceAnimate[i] != -1) {
                    when (i) {
                        0 -> diceA1.setImageResource(R.drawable.qm)
                        1 -> diceA2.setImageResource(R.drawable.qm)
                        2 -> diceA3.setImageResource(R.drawable.qm)
                        3 -> diceA4.setImageResource(R.drawable.qm)
                        4 -> diceA5.setImageResource(R.drawable.qm)
                    }
                } else {
                    if (diceRoll[i] >= 0) {
                        when (i) {
                            0 -> diceA1.setImageResource(getImageId(diceRoll[i]))
                            1 -> diceA2.setImageResource(getImageId(diceRoll[i]))
                            2 -> diceA3.setImageResource(getImageId(diceRoll[i]))
                            3 -> diceA4.setImageResource(getImageId(diceRoll[i]))
                            4 -> diceA5.setImageResource(getImageId(diceRoll[i]))
                        }
                    } else {
                        when (i) {
                            0 -> diceA1.setImageResource(R.drawable.qm)
                            1 -> diceA2.setImageResource(R.drawable.qm)
                            2 -> diceA3.setImageResource(R.drawable.qm)
                            3 -> diceA4.setImageResource(R.drawable.qm)
                            4 -> diceA5.setImageResource(R.drawable.qm)
                        }
                    }
                }
                if (diceHold[i] >= 0) {
                    when (i) {
                        0 -> diceA1.setImageBitmap(getHoldBitmap(getImageId(diceHold[i])))
                        1 -> diceA2.setImageBitmap(getHoldBitmap(getImageId(diceHold[i])))
                        2 -> diceA3.setImageBitmap(getHoldBitmap(getImageId(diceHold[i])))
                        3 -> diceA4.setImageBitmap(getHoldBitmap(getImageId(diceHold[i])))
                        4 -> diceA5.setImageBitmap(getHoldBitmap(getImageId(diceHold[i])))
                    }
                }
            }
            if (!isEnginePlayer(ed.playerToMove))
                btnPlayerInfo.visibility = TextView.INVISIBLE
            handlerAnimationDices.removeCallbacks(mUpdateAnimationDices)
            isInitRound = true
            handlerAnimationDices.postDelayed(mUpdateAnimationDices, DELAY_TIME.toLong())
        }
    }

    // diceBoard
    fun animationDiceBoard(initRound: Boolean) {

//        Log.i(TAG, "1a animationDiceBoard(), isDoingTurn: $isDoingTurn, isInitBoard: $isInitBoard, initRound: $initRound")

        if (prefs.getBoolean("playOnline", false) && !isDoingTurn && !isInitBoard)
            return

        if (isInitBoard) {
            isInitBoard = false
            isInitRound = false
            diceBoard.initBoard()

//            Log.i(TAG, "1b animationDiceBoard(), isDoingTurn: $isDoingTurn, playOnline: ${prefs.getBoolean("playOnline", false)}")

            if (!(prefs.getBoolean("playOnline", false) && !isDoingTurn)) {

//                Log.i(TAG, "1c animationDiceBoard(), handlerAnimationDices()")

                handlerAnimationDices.removeCallbacks(mUpdateAnimationDices)
                handlerAnimationDices.postDelayed(mUpdateAnimationDices, 50)
                return
            }

        }

//        Log.i(TAG, "2 animationDiceBoard(), isInitDelay: $isInitDelay")

        if (!isInitDelay) {
            var isSelectable = true
            if (isEnginePlayer(ed.playerToMove) or (diceMode >= 4))
                isSelectable = false
            setDiceValues(false)
            val playerInfo = ed.getDiceResult(isServed, diceValues)
            var doubleState = 0
            var playerInfoDouble = ""

//            Log.i(TAG, "A animationDiceBoard(), isDouble1: $isDouble1, diceMode: $diceMode, diceModePrev: $diceModePrev")

            if (!ed.isSingleGame) {
                if (isDouble1 and (diceMode >= 1) and (diceMode <= 3))
                    doubleState = 1
                if (!isDouble1) {
                    doubleState =
                        if ((diceMode == 1) and ((diceModePrev == 2) or (diceModePrev == 3)))
                            1
                        else
                            2
                }
                if (isEnginePlayer(ed.playerToMove) and (doubleState == 1))
                    doubleState = 2
                setDiceValues(true)
                playerInfoDouble = ed.getDiceResult(isServedDouble1, diceValuesDouble1)

//                Log.i(TAG, "B animationDiceBoard(), doubleState: $doubleState, playerInfoDouble: $playerInfoDouble")

            }
            var playerToMove = ed.playerToMove
            if (prefs.getBoolean("playOnline", false)) {
                if (diceMode == 1 && diceModePrev == 4)  // turn !
                    playerToMove = ed.prevPlayerToMove
            } else {
                if (diceMode == 5)
                    playerToMove = ed.prevPlayerToMove
            }

            var players = ""

            if (mMatchId != null)
                players = getPlayersFromMatch(mMatchId!!)

//            Log.i(TAG, "3 animationDiceBoard(), mMatchId: $mMatchId, mMatchBaseData: $mMatchBaseData, players: $players")

            if (prefs.getBoolean("playOnline", false)
                    && players == ""
                    && !isOnlineActive
                ) {

                players = if (mPlayerName == null)
                    ""
                else
                    "$mPlayerName: $mPlayerEp EP"
            }
            if (ed.isSingleGame && players.isNotEmpty() && players.contains("-") && !players.contains("???")) {
                val plEp = getPlayersEp()
                if (plEp.isNotEmpty())
                    players += "\n$plEp"
            }

            diceBoard.setRoundValues(ed.colValues, ed.isSingleGame, playerToMove, playerInfo, playerInfoDouble, players)

//            Log.d(TAG, "3 animationDiceBoard(), diceMode: $diceMode, diceModeCheckRoll: $diceModeCheckRoll")

            if (prefs.getBoolean("playOnline", false) && !isDoingTurn && diceMode == diceModeCheckRoll)
                initRollValues = false

//            Log.i(TAG, "4 animationDiceBoard(), initRollValues: $initRollValues, isConfigurationChanged: $isConfigurationChanged")

            if (prefs.getBoolean("playOnline", false) && !isDoingTurn && isConfigurationChanged) {
                initRollValues = true
                isConfigurationChanged = false
            }

            diceModeCheckRoll = diceMode

            diceBoard.updateBoard(diceRoll, diceHold, diceDouble1, isSelectable, doubleState, initRollValues)

            if (diceBoard.mBoardWidth <= 0 || diceBoard.mBoardHeight <= 0) {
                diceModeCheckRoll = -1
            }

            initRollValues = true
        }

        if (!initRound) {
            isInitRound = true

//            Log.i(TAG, "C animationDiceBoard(), setOnlineMessage()")

            if (prefs.getBoolean("playOnline", false))
                setOnlineMessage()
            else {
                if ((diceMode == 4) and !isEnginePlayer(ed.playerToMove)) {
                    if (!ed.isSingleGame and isDouble1)
                        btnDice.visibility = ImageView.VISIBLE
                    else
                        btnDice.visibility = ImageView.INVISIBLE
                }
            }
            return
        }
        if (isEnginePlayer(ed.playerToMove)) {
            val currentTime = System.currentTimeMillis()
            if (currentTime < endAnimationTime) {
                handlerEngine.removeCallbacks(mUpdateEngine)
                handlerAnimationDices.removeCallbacks(mUpdateAnimationDices)
                isInitDelay = true
                handlerAnimationDices.postDelayed(mUpdateAnimationDices, DELAY_TIME_DICE_BOARD.toLong())
                return
            }
        }
        isInitDelay = false
        ed.getDiceText(isDouble1, isServed, diceValues, diceMode)
        val oldDiceMode = diceMode
        if (diceMode < 2)
            diceMode = 2
        else {
            if (diceMode < 4)
                diceMode++
        }
        if ((diceMode == 4) and !isEnginePlayer(ed.playerToMove)) {
            if (!ed.isSingleGame and isDouble1) {
                isDouble1 = false
                diceMode = 1
                setDiceDouble1()
                setDiceValues(true)
                ed.getDiceText(isDouble1, isServedDouble1, diceValuesDouble1, 0)
                val txt = ed.diceText
                ed.getDiceText(true, isServedDouble1, diceValuesDouble1, 0)
                ed.diceTextDouble1 = ed.diceText
                ed.diceText = txt
                isDicing = false
                btnDice.visibility = ImageView.VISIBLE
                animationDiceBoard(false)
            } else {
                ed.getDiceText(isDouble1, isServed, diceValues, 4)
                btnDice.visibility = ImageView.INVISIBLE
            }
        } else {
            btnDice.visibility = ImageView.VISIBLE
        }

        diceView(diceRoll, diceHold)
        setSelectedItem(-1)
        updateTable(false)
        isDicing = false

        if (isEnginePlayer(ed.playerToMove) and (oldDiceMode <= 3) and ((diceRoll[0] >= 0) or (diceHold[0] >= 0)))
            performEngineCommands(getEngineDiceCommand(ed.playerToMove, oldDiceMode, diceRoll, diceHold, diceDouble1, isServedDouble1))

        if (prefs.getBoolean("playOnline", false) && isOnlineActive && isDoingTurn) {

//            Log.i(TAG, "animationDiceBoard(), matchDataToDb(false, ONLINE_ACTIVE)")

            matchDataToDb(false, ONLINE_ACTIVE)
        }

    }

    private fun initBoard() {

//        Log.i(TAG, "initBoard(), isDoingTurn: $isDoingTurn, isInitBoard: $isInitBoard, isInitDelay: $isInitDelay")

        if (prefs.getBoolean("playOnline", false) && !isDoingTurn && !isInitBoard)
            return

        if (isInitBoard) {
            isInitBoard = false
            isInitRound = false
            diceBoard.initBoard()
            handlerAnimationDices.removeCallbacks(mUpdateAnimationDices)
            handlerAnimationDices.postDelayed(mUpdateAnimationDices, 50)

            if (!(prefs.getBoolean("playOnline", false) && !isDoingTurn))
                return

        }

//        Log.i(TAG, "2 initBoard(), isInitDelay: $isInitDelay")

        if (!isInitDelay) {
            var isSelectable = true
            if (isEnginePlayer(ed.playerToMove) or (diceMode >= 4))
                isSelectable = false
            setDiceValues(false)
            val playerInfo = ed.getDiceResult(isServed, diceValues)
            var doubleState = 0
            var playerInfoDouble = ""
            if (!ed.isSingleGame) {
                if (isDouble1 and (diceMode >= 1) and (diceMode <= 3))
                    doubleState = 1
                if (!isDouble1) {
                    doubleState =
                            if ((diceMode == 1) and ((diceModePrev == 2) or (diceModePrev == 3)))
                                1
                            else
                                2
                }
                if (isEnginePlayer(ed.playerToMove) and (doubleState == 1))
                    doubleState = 2
                setDiceValues(true)
                playerInfoDouble = ed.getDiceResult(isServedDouble1, diceValuesDouble1)
            }
            var playerToMove = ed.playerToMove
            if (prefs.getBoolean("playOnline", false)) {
                if (diceMode == 1 && diceModePrev == 4)  // turn !
                    playerToMove = ed.prevPlayerToMove
            } else {
                if (diceMode == 5)
                    playerToMove = ed.prevPlayerToMove
            }

            var players = ""
            if (mMatchId != null)
                players = getPlayersFromMatch(mMatchId!!)

            if (prefs.getBoolean("playOnline", false)
                    && players == ""
                    && !isOnlineActive
            ) {
                players = if (mPlayerName == null)
                    ""
                else
                    "$mPlayerName: $mPlayerEp EP"
            }
            if (ed.isSingleGame && players.isNotEmpty() && players.contains("-") && !players.contains("???")) {
                val plEp = getPlayersEp()
                if (plEp.isNotEmpty())
                    players += "\n$plEp"
            }

            diceBoard.setRoundValues(ed.colValues, ed.isSingleGame, playerToMove, playerInfo, playerInfoDouble, players)

//            Log.d(TAG, "3 initBoard(), diceMode: $diceMode, diceModeCheckRoll: $diceModeCheckRoll")

            if (prefs.getBoolean("playOnline", false) && !isDoingTurn && diceMode == diceModeCheckRoll)
                initRollValues = false
            if (prefs.getBoolean("playOnline", false) && !isDoingTurn && diceModeCheckRoll == -1)
                initRollValues = true
            diceModeCheckRoll = diceMode

            diceBoard.updateBoard(diceRoll, diceHold, diceDouble1, isSelectable, doubleState, initRollValues)
            initRollValues = true
        }

        if (prefs.getBoolean("playOnline", false))
            isInitRound = true

//        Log.i(TAG, "D initBoard(), setOnlineMessage()")

        if (prefs.getBoolean("playOnline", false))
            setOnlineMessage()
        else {
            if ((diceMode == 4) and !isEnginePlayer(ed.playerToMove)) {
                if (!ed.isSingleGame and isDouble1)
                    btnDice.visibility = ImageView.VISIBLE
                else
                    btnDice.visibility = ImageView.INVISIBLE
            }
        }

    }

    fun updateTable(isUpdate: Boolean): Boolean {
        if (isUpdate) {
            if (prefs.getBoolean("playOnline", false)) {
                tableView.updateTable(ed, true, isOnlineActive)

                if (isOnlineActive) {
                    if (ed.playerToMove == 'A')
                        btnPlayerInfo.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerA))
                    if (ed.playerToMove == 'B')
                        btnPlayerInfo.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerB))
                    if (ed.playerToMove == 'C')
                        btnPlayerInfo.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlayerC))
                }

//                Log.d(TAG, "updateTable(), ed.playerToMove: " + ed.playerToMove + ", isOnlineEntry: " + isOnlineEntry)

                if (!isOnlineEntry)
                    setBtnPlayer(ed.playerToMove)

                return true
            }

            tableView.updateTable(ed, false, isOnlineActive)

//            Log.d(TAG, "A updateTable(), diceMode: $diceMode")

            ed.setPlayerToMove(getEngineName(isEnginePlayer(ed.nextPlayerToMove)))

//            Log.d(TAG, "B updateTable(), ed.playerToMove: " + ed.playerToMove + ", diceMode: " + diceMode)

            if (!isOnlineEntry)
                setBtnPlayer(ed.playerToMove)

        } else {

            if (!prefs.getBoolean("playOnline", false)) {
                ed.setPlayerToMove(getEngineName(isEnginePlayer(ed.nextPlayerToMove)))
            }
            setBtnPlayer(ed.playerToMove)

        }

        return isUpdate
    }

    private fun getPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissions = arrayOf(
                    Manifest.permission.INTERNET,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if  (       ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED
                    ||  ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                )
                    ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun gameOver() {
        isDicing = false
        btnPlayerResult.visibility = TextView.VISIBLE
        btnPlayerResult.text = resources.getString(R.string.gameOver)
        btnPlayerInfo.visibility = TextView.INVISIBLE
        btnPlayerInfo2.visibility = TextView.INVISIBLE
        btnPlayerName.visibility = TextView.INVISIBLE
        btnPlayerIcon.visibility = TextView.INVISIBLE
        btnPlayerRound.visibility = TextView.INVISIBLE
        btnDice.visibility = TextView.INVISIBLE

    }

    fun checkEngineResult(command: String, result: String): String {
        var res = result
        var isEngineError = false
        var errorMessage = ""
        val oldResult = res
        val strResult = res.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        var player = 'A'
        var col = 0
        var row = 0
        var value = 0
        var resultsPlayer: Array<IntArray>? = null

        if (res.startsWith("dice ")) {
            player = strResult[1][0]
            if (player != ed.playerToMove) {
                errorMessage = ", playerName id error: $player"
                isEngineError = true

                Log.i(TAG, "$ENGINE_DICE_ERROR$command  >>>  $res$errorMessage")

            }
            if (!checkRollHoldFromEngineCommands(res)) {
                res = command
                isEngineError = true

                Log.i(TAG, "$ENGINE_DICE_ERROR$oldResult  >>>  $res$errorMessage")

            }
        }
        if (res.startsWith("entry ")) {
            if (strResult.size == 5) {
                player = strResult[1][0]
                col = Integer.parseInt(strResult[2])
                row = Integer.parseInt(strResult[3])
                value = Integer.parseInt(strResult[4])
                if (player != ed.playerToMove) {
                    player = ed.playerToMove
                    isEngineError = true
                }
                when (player) {
                    'A' -> resultsPlayer = ed.resultsPlayerA
                    'B' -> resultsPlayer = ed.resultsPlayerB
                    'C' -> resultsPlayer = ed.resultsPlayerC
                }
                if (resultsPlayer!![col][row] >= 0)
                    isEngineError = true
            } else
                isEngineError = true
            if (isEngineError) {
                var isValue = false
                for (i in 0 until EscaleroData.COLS) {
                    for (j in 0 until EscaleroData.ROWS) {
                        if (resultsPlayer!![i][j] < 0) {
                            col = i
                            row = j
                            value = 0
                            isValue = true
                            break
                        }
                    }
                    if (isValue) break
                }
                res = "entry $player $col $row $value"

                Log.i(TAG, "$ENGINE_ENTRY_ERROR$oldResult  >>>  $res")

            }
        }
        return res
    }

    private fun showMainDialog() {

        val onlineIcon = dialogMain.findViewById<ImageView>(R.id.onlineIcon)
        val onlineText = dialogMain.findViewById<TextView>(R.id.onlineText)
        val offlineIcon = dialogMain.findViewById<ImageView>(R.id.offlineIcon)
        val offlineText = dialogMain.findViewById<TextView>(R.id.offlineText)
        val adsIcon = dialogMain.findViewById<ImageView>(R.id.adsIcon)
        val adsText = dialogMain.findViewById<TextView>(R.id.adsText)
        val exitIcon = dialogMain.findViewById<ImageView>(R.id.exitIcon)
        val exitText = dialogMain.findViewById<TextView>(R.id.exitText)

        dialogMain.setCancelable(true)
        onlineIcon.setOnClickListener {
            playOnline(false)
            dialogMain.dismiss()
        }
        onlineText.setOnClickListener {
            playOnline(false)
            dialogMain.dismiss()
        }
        offlineIcon.setOnClickListener {
            playOffline(true)
            dialogMain.dismiss()
        }
        offlineText.setOnClickListener {
            playOffline(true)
            dialogMain.dismiss()
        }
        adsIcon.setOnClickListener {
            showAds()
        }
        adsText.setOnClickListener {
            showAds()
        }
        exitIcon.setOnClickListener {
            dialogMain.dismiss()
            finishApp(true)
        }
        exitText.setOnClickListener {
            dialogMain.dismiss()
            finishApp(true)
        }

        if (!dialogMain.isShowing && !isFinishing)
            dialogMain.show()
    }

    private fun playOnline(isAppStart: Boolean) {
        if (engineIsRunning) {
            Toast.makeText(applicationContext, getString(R.string.isStopped), Toast.LENGTH_SHORT).show()
            stopEngine = true
            engineIsRunning = false
        }
        diceState = 1
        val edi = prefs.edit()
        edi.putBoolean("playOnline", true)
        edi.putBoolean("isSingleGame", ed.isSingleGame)
        edi.putInt("players", 2)
        edi.putInt("dice", 1)
        edi.putBoolean("computeFlipScreen", false)
        edi.putInt("pointsCol1", 1)
        edi.putInt("pointsCol2", 2)
        edi.putInt("pointsCol3", 4)
        edi.putInt("pointsBon", 3)
        edi.putInt("multiplier", 1)
        edi.putString("unit", getString(R.string.points))
        edi.putInt("bonusServed", 5)
        edi.putInt("bonusServedGrande", 30)
        edi.apply()

        if (!isAppStart)
            setUI()

        ed.isFlipScreen = false
        isOrientationReverse = false
        computeFlipScreen(true)

        initOnline()
        initDiceArrays()
        initBoard()

        diceBoard.mOnlinePlayers = ""

        signInSilently()

    }


    private fun playOffline(initData: Boolean) {
        if (!isOnlineActive) {

            logOutUpdateUser(isFinishApp = false, updateUser = true)

            ed.isFlipScreen = false
            isOrientationReverse = false
            val ed = prefs.edit()
            ed.putBoolean("playOnline", false)
            ed.putBoolean("computeFlipScreen", false)
            ed.putInt("flipModus", 0)
            ed.apply()
            initOnline()
            if (initData) {
                setDataUpdate()
                initRunPrefs()
                initDiceArrays()
                diceMode = 0
                diceModePrev = 0
            }

            diceView(diceRoll, diceHold)
            initBoard()
        } else {
            showPauseMatchDialog()
        }
    }

    private fun showAds() {

        if (prefs.getBoolean("advertising", true)) {
            val timeStamp = System.currentTimeMillis()
            if (timeStamp - prefs.getLong("adsDelay", 0L) >= ADS_DELAY && mRewardedAd != null) {

//                Log.d(TAG, "showAds(), timeStamp: " + timeStamp + ", adsDelay" + prefs.getLong("adsDelay", 0L) + ", ADS_DELAY: " + ADS_DELAY)

                mRewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {

//                        Log.d(TAG, "showAds(), onAdShowedFullScreenContent()")

                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError?) {

//                        Log.d(TAG, "showAds(), onAdFailedToShowFullScreenContent()")

                    }

                    override fun onAdDismissedFullScreenContent() {

                        val ed = prefs.edit()
                        ed.putLong("adsDelay", timeStamp)
                        ed.apply()

//                        Log.d(TAG, "showAds(), onAdDismissedFullScreenContent()")

                        loadRewardedVideoAd()

                    }
                }

                mRewardedAd?.show(this) { rewardItem ->

                    val rewardAmount = rewardItem.amount

//                    Log.d(TAG, "showAds(), rewardAmount: $rewardAmount, mPlayerId: $mPlayerId")

                    if (mPlayerId != null && rewardAmount > 0) {
                        var getEp =
                            ((System.currentTimeMillis() - startAds) / DELAY_TIME_ADS_EP) + ADS_EP_MIN
                        if (getEp > ADS_EP_MAX)
                            getEp = ADS_EP_MAX
                        if (mPlayerEp >= 100)
                            getEp = ADS_EP_1
                        val sum = mPlayerEp + getEp

//                        Log.d(TAG, "showAds(), mPlayerEp: $mPlayerEp, getEp: $getEp")

                        val newEp =
                            "${getString(R.string.escaleroPoints)}:\n\n$mPlayerEp + $getEp = $sum EP"
                        showInfoDialog(
                            getString(R.string.info),
                            newEp,
                            getString(R.string.ok)
                        )
                        updatePlayerEp(mPlayerId!!, getEp)
                    }
                }

            }
            else {
                Toast.makeText(applicationContext, getString(R.string.currentlyNoAds), Toast.LENGTH_LONG).show()
            }

        }
    }

    private fun showMatchDialog(matchList: ArrayList<MatchList>, setAllChecked: Boolean) {
        if (!checkConnectivity(false))
            return

        val matchPlayer = dialogMatch.findViewById<TextView>(R.id.matchPlayer)
        val matchStatus = dialogMatch.findViewById<TextView>(R.id.matchStatus)
        val matchListView = dialogMatch.findViewById<ListView>(R.id.matchListView)
        val matchEmpty = dialogMatch.findViewById<TextView>(R.id.matchEmpty)
        val matchCheckBoxAll = dialogMatch.findViewById<CheckBox>(R.id.matchCheckBoxAll)
        val matchBack = dialogMatch.findViewById<TextView>(R.id.matchBack)
        val matchDelete = dialogMatch.findViewById<TextView>(R.id.matchDelete)
        val matchView = dialogMatch.findViewById<TextView>(R.id.matchView)

        matchPlayer.text = getString(R.string.player)
        matchStatus.text = mPlayerName
        matchListView.visibility = ListView.VISIBLE
        matchEmpty.visibility = ImageView.INVISIBLE
        matchCheckAll = setAllChecked
        dialogMatch.setCancelable(true)
        matchCheckBoxAll.isChecked = setAllChecked
        matchCheckBoxAll.setOnCheckedChangeListener { _, isChecked ->
            matchCheckAll = isChecked
            if (isChecked) {
                for (i in matchList.indices) {
                    matchList[i].selected = true
                }
            } else {
                for (i in matchList.indices) {
                    matchList[i].selected = false
                }
            }
            showMatchDialog(matchList, matchCheckAll)
        }

//        Log.d(TAG, "showMatchDialog(), matchList.size: ${matchList.size}")

        val adapter = MatchListAdapter(this, matchList)
        matchListView.adapter = adapter
        val lastViewItemIndex = matchListView.lastVisiblePosition
        matchListView.setSelection(lastViewItemIndex)
        matchListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, x, _ ->
            matchList[x].selected = !matchList[x].selected
            val firstViewItemIndex = matchListView.firstVisiblePosition
            showMatchDialog(matchList, matchCheckBoxAll.isChecked)
            matchListView.setSelection(firstViewItemIndex)
        }

        if (matchList.isEmpty()) {
            matchListView.adapter = MatchListAdapter(this, matchList)
            matchListView.visibility = ListView.INVISIBLE
            matchEmpty.visibility = ImageView.VISIBLE
            matchCheckBoxAll.isChecked = false
            matchCheckAll = false
        }

        matchBack.setOnClickListener {
            dialogMatch.dismiss()
        }
        matchDelete.setOnClickListener {

//            Log.d(TAG, "showMatchDialog(), allMatchList.size: ${matchList.size}")

            matchDeletedList = ArrayList()
            var cnt = 0
            for (i in matchList.indices) {
                if (matchList[i].selected)
                    cnt++
            }

            var del = 0

            for (i in matchList.indices) {
                if (matchList[i].selected) {
                    del++
                    Toast.makeText(applicationContext, "$del($cnt)", Toast.LENGTH_SHORT).show()

                    GlobalScope.launch(Dispatchers.Main) {
                        val matchId = matchList[i].matchId
                        val deferredGetMatchBaseData = async { getMatchBaseData(matchId) }
                        val mbd = deferredGetMatchBaseData.await()
                        deferredGetMatchBaseData.cancel()

//                        Log.d(TAG, "showMatchDialog(), matchId: $matchId, mbd: $mbd")

                        if (mbd != null)
                            startRemoveMatch(true, matchId, mbd.playerIdA!!, mbd.playerIdB!!)
                        else
                            startRemoveMatch(true, matchId, mPlayerId, null)

                    }
                }
            }

            if (cnt ==0) {
                showInfoDialog(getString(R.string.info), getString(R.string.nothingSelected), getString(R.string.ok))
            }
            else {
                dialogMatch.dismiss()
                if (!isOnlineActive && del > 0) {
                    setNoActiveMatch()
                }
            }

        }

        // continue match
        matchView.setOnClickListener {

            var isSelected = false
            var matchId = ""
            for (i in matchList.indices) {
                if (matchList[i].selected) {
                    isSelected = true
                    matchId = matchList[i].matchId
                    break
                }
            }

//            Log.d(TAG, "1 showMatchDialog()")

            GlobalScope.launch(Dispatchers.Main) {

//                Log.d(TAG, "2 showMatchDialog()")

                if (isSelected && !isOnlineActive) {
                    if (matchId != "") {

                        if (refMatch != null && refMatchListener != null)
                            refMatch!!.removeEventListener(refMatchListener!!)

                        val deferredGetMatchBaseData = async { getMatchBaseData(matchId) }
                        mMatchBaseData = deferredGetMatchBaseData.await()
                        deferredGetMatchBaseData.cancel()
                        val deferredGetMatchUpdateData = async { getMatchUpdateData(matchId) }
                        mMatchUpdateData = deferredGetMatchUpdateData.await()
                        deferredGetMatchUpdateData.cancel()
                        if (mMatchBaseData != null && mMatchUpdateData != null) {
                            mMatchId = matchId

                            val deferredGetMatchTimestamp = async { getMatchTimestamp(matchId) }
                            val timestamp = deferredGetMatchTimestamp.await().toLong()
                            deferredGetMatchTimestamp.cancel()

                            mTimestampPre = timestamp
                            mMatchUpdateData!!.onlineAction = ONLINE_GAME_CONTINUE
                            if (mPlayerId == mMatchBaseData!!.playerIdA!!) {
                                mOpponentId = mMatchBaseData!!.playerIdB!!
                                mOpponentName = mMatchBaseData!!.nameB
                                mOpponentABC = "B"
                                mPlayerABC = "A"
                                setOpponentData(mMatchBaseData!!.playerIdB!!, "B")
                            }
                            else {
                                mOpponentId = mMatchBaseData!!.playerIdA!!
                                mOpponentName = mMatchBaseData!!.nameA
                                mOpponentABC = "A"
                                mPlayerABC = "B"
                                setOpponentData(mMatchBaseData!!.playerIdA!!, "A")
                            }
                            var actionId = "3"
                            if (mMatchUpdateData!!.turnPlayerId!! != mPlayerId)
                                actionId = "4"

                            ed.isSingleGame = mMatchBaseData!!.singleGame ?: true
                            val edi = prefs.edit()
                            edi.putBoolean("isSingleGame", ed.isSingleGame)
                            edi.apply()

//                            Log.d(TAG, "4 showMatchDialog(), actionId: $actionId")

                            val deferredGetUserData = async { getUserData(mOpponentId!!) }
                            val user = deferredGetUserData.await()
                            deferredGetUserData.cancel()

                            if (user != null) {

//                                Log.d(TAG, "4 showMatchDialog(), user.currentMatchId: ${user.currentMatchId}, user.playing: ${user.playing}")

                                if (user.notifications!! && (!user.playing!! || (user.playing!! && user.currentMatchId == matchId))) {
                                    continueMatchNotification(actionId, mMatchId!!, mOpponentId!!, mOpponentName!!, mMatchBaseData!!.nameA!!, mMatchBaseData!!.nameB!!)
                                    mIsContinueMatch = true
                                    matchUpdate(mMatchUpdateData!!)
                                    startMatchUpdateListener(mMatchId!!)
                                }
                                else
                                    showInfoDialog(getString(R.string.info), getString(R.string.playerNotAvailable, user.name), getString(R.string.ok))
                            }

                        }
                        else {
                            val mes = "${getString(R.string.noActiveMatch)}\n${getString(R.string.matchDeleted)}"
                            showInfoDialog(getString(R.string.info), mes, getString(R.string.ok))
                            val refPlayerA = FirebaseDatabase.getInstance().getReference("userMatches")
                            refPlayerA.child(mPlayerId!!).child(matchId).removeValue()
                        }
                        dialogMatch.dismiss()
                    }
                } else {
                    if (!isSelected)
                        showInfoDialog(getString(R.string.info), getString(R.string.nothingSelected), getString(R.string.ok))
                    else {
                        if (isOnlineActive)
                            showInfoDialog(getString(R.string.info), getString(R.string.firstFinishActiveMatch), getString(R.string.ok))
                        dialogMatch.dismiss()
                    }
                }
            }

        }

        if (!dialogMatch.isShowing && !isFinishing)
            dialogMatch.show()

    }

    private fun showInfoDialog(mesTitle: String, mes: String, action: String) {
        val title = dialogInfo.findViewById<TextView>(R.id.mesTitle)
        val message = dialogInfo.findViewById<TextView>(R.id.mes)
        val messageAction = dialogInfo.findViewById<TextView>(R.id.messageAction)

        title.text = mesTitle
        message.text = mes
        messageAction.text = action
        dialogInfo.setCancelable(true)
        dialogInfo.setOnCancelListener {
            if (dialogInfo.isShowing)
                dialogInfo.dismiss()
            if (dialogCurrentMatch.isShowing)
                dialogCurrentMatch.dismiss()
        }
        messageAction.setOnClickListener {
            if (dialogInfo.isShowing)
                dialogInfo.dismiss()
            if (dialogCurrentMatch.isShowing)
                dialogCurrentMatch.dismiss()
        }

        if (!isFinishing)
            dialogInfo.show()
    }

    private fun showContinueMatchDialog(matchId: String, actionId: String) {

//        Log.d(TAG, "showContinueMatchDialog(), matchId: $matchId, actionId: $actionId")
        val mes2Title = dialogTwoBtn.findViewById<TextView>(R.id.mes2Title)
        val mes2 = dialogTwoBtn.findViewById<TextView>(R.id.mes2)
        val action1 = dialogTwoBtn.findViewById<TextView>(R.id.action1)
        val action2 = dialogTwoBtn.findViewById<TextView>(R.id.action2)

        GlobalScope.launch(Dispatchers.Main) {

            mMatchId = matchId
            val deferredGetMatchBaseData = async { getMatchBaseData(matchId) }
            mMatchBaseData = deferredGetMatchBaseData.await()
            deferredGetMatchBaseData.cancel()
            val deferredGetMatchUpdateData = async { getMatchUpdateData(matchId) }
            mMatchUpdateData = deferredGetMatchUpdateData.await()
            deferredGetMatchUpdateData.cancel()

            if (mMatchBaseData != null && mMatchUpdateData != null) {

                if (mAccountingDialog != null) {
                    if (mAccountingDialog!!.isShowing)
                        mAccountingDialog!!.dismiss()
                }
                var gameType = ""
                if (!mMatchBaseData!!.singleGame!!)
                    gameType = getString(R.string.typeDouble).uppercase((Locale.ROOT))
                val deferredTimestamp = async { getTimestampFromServer() }
                val serverTimestamp = deferredTimestamp.await().toLong()
                deferredTimestamp.cancel()
                val deferredGetMatchTimestamp = async { getMatchTimestamp(matchId) }
                val matchTimestamp = deferredGetMatchTimestamp.await().toLong()
                deferredGetMatchTimestamp.cancel()
                val time = getUpdateTime(matchTimestamp, serverTimestamp)
                var type = ""
                if (!mMatchBaseData!!.singleGame!!)
                    type = "($gameType)"
                val title = "${getString(R.string.continueMatchTitle)} $type \n$time"
                mes2Title.text = title
                val players = "${mMatchBaseData!!.nameA} - ${mMatchBaseData!!.nameB}"
                mes2.text = players
                action1.text = getString(R.string.reject)
                action2.text = getString(R.string.continuation)
                dialogTwoBtn.setCancelable(true)
                dialogTwoBtn.setOnCancelListener {
                    dialogTwoBtn.dismiss()
                    if (mAccountingDialog != null) {
                        if (mAccountingDialog!!.isShowing)
                            mAccountingDialog!!.dismiss()
                    }
                    initOnline()
                    setNoActiveMatch()
                }
                action1.setOnClickListener {
                    GlobalScope.launch(Dispatchers.Main) {
                        dialogTwoBtn.dismiss()
                        initOnline()
                        setNoActiveMatch()
                    }

                }
                action2.setBackgroundResource(R.drawable.rectangleyellow)
                action2.setOnClickListener {

                    continueOnlineMatch(actionId)

                    dialogTwoBtn.dismiss()
                    dialogPlayerQuery.dismiss()
                    if (mAccountingDialog != null) {
                        if (mAccountingDialog!!.isShowing)
                            mAccountingDialog!!.dismiss()
                        if (mAccountingDialog!!.isShowing)
                            mAccountingDialog!!.dismiss()
                    }

                }
                action1.visibility = ImageView.VISIBLE
                action2.visibility = ImageView.VISIBLE

                dialogTwoBtn.show()

            }
        }
    }

    private fun showPlayerQueryDialog(user: User, mesTitle: String, mes: String, action1a: String, action2a: String) {

        val mes2Title = dialogTwoBtn.findViewById<TextView>(R.id.mes2Title)
        val mes2 = dialogTwoBtn.findViewById<TextView>(R.id.mes2)
        val action1 = dialogTwoBtn.findViewById<TextView>(R.id.action1)
        val action2 = dialogTwoBtn.findViewById<TextView>(R.id.action2)

        mes2Title.text = mesTitle
        mes2.text = mes
        action1.text = action1a
        action2.text = action2a
        dialogTwoBtn.setCancelable(true)
        dialogTwoBtn.setOnCancelListener {
            dialogTwoBtn.dismiss()
        }
        action1.setOnClickListener {
            dialogTwoBtn.dismiss()
        }
        action2.setOnClickListener {
            dialogTwoBtn.dismiss()
            invitePlayer(user)
        }
        action1.visibility = ImageView.VISIBLE
        action2.visibility = ImageView.VISIBLE
        if (!isFinishing)
            dialogTwoBtn.show()
    }

    private fun showRemoveQuickMatchDialog(mesTitle: String, mes: String, action1a: String, action2a: String) {

        val mes2Title = dialogTwoBtn.findViewById<TextView>(R.id.mes2Title)
        val mes2 = dialogTwoBtn.findViewById<TextView>(R.id.mes2)
        val action1 = dialogTwoBtn.findViewById<TextView>(R.id.action1)
        val action2 = dialogTwoBtn.findViewById<TextView>(R.id.action2)

        mes2Title.text = mesTitle
        mes2.text = mes
        action1.text = action1a
        action2.text = action2a
        dialogTwoBtn.setCancelable(true)
        dialogTwoBtn.setOnCancelListener {
            dialogTwoBtn.dismiss()
        }
        action1.setOnClickListener {
            dialogTwoBtn.dismiss()
        }
        action2.setOnClickListener {
            dialogTwoBtn.dismiss()
            GlobalScope.launch(Dispatchers.Main) {
                val deferredSetQuickMatchUserId = async { setQuickMatchUserId("0") }
                deferredSetQuickMatchUserId.await()
                deferredSetQuickMatchUserId.cancel()
                Toast.makeText(applicationContext, getString(R.string.removedQuickMatch), Toast.LENGTH_LONG).show()
            }
        }
        action1.visibility = ImageView.VISIBLE
        action2.visibility = ImageView.VISIBLE
        if (!isFinishing)
            dialogTwoBtn.show()
    }

    private fun showMultipleRegisteredDialog() {

        val mes2Title = dialogTwoBtn.findViewById<TextView>(R.id.mes2Title)
        val mes2 = dialogTwoBtn.findViewById<TextView>(R.id.mes2)
        val action1 = dialogTwoBtn.findViewById<TextView>(R.id.action1)
        val action2 = dialogTwoBtn.findViewById<TextView>(R.id.action2)

        mes2Title.text = getString(R.string.warningTitle)
        mes2.text = getString(R.string.multipleRegistered)
        action1.text = getString(R.string.appContinue)
        action2.text = getString(R.string.appExit)
        dialogTwoBtn.setCancelable(false)
        action1.setOnClickListener {
            dialogTwoBtn.dismiss()
        }
        action2.setOnClickListener {
            dialogTwoBtn.dismiss()
            finishApp(false)
        }
        action1.visibility = ImageView.VISIBLE
        action2.visibility = ImageView.VISIBLE
        if (!isFinishing)
            dialogTwoBtn.show()
    }

    private fun showNoEpDialog() {

        val mes2Title = dialogTwoBtn.findViewById<TextView>(R.id.mes2Title)
        val mes2 = dialogTwoBtn.findViewById<TextView>(R.id.mes2)
        val action1 = dialogTwoBtn.findViewById<TextView>(R.id.action1)
        val action2 = dialogTwoBtn.findViewById<TextView>(R.id.action2)

        mes2Title.text = getString(R.string.warningTitle)
        mes2.text = getString(R.string.noEp)
        action1.text = getString(R.string.noThanks)
        action2.text = getString(R.string.getEp)
        dialogTwoBtn.setCancelable(true)
        action1.setOnClickListener {
            dialogTwoBtn.dismiss()
        }
        action2.setOnClickListener {
            dialogTwoBtn.dismiss()
            showAds()
        }
        action1.visibility = ImageView.VISIBLE
        action2.visibility = ImageView.VISIBLE
        if (!isFinishing)
            dialogTwoBtn.show()
    }

    private fun showInvitationFromNotificationDialog(intent: Intent) {

        val mes2Title = dialogTwoBtn.findViewById<TextView>(R.id.mes2Title)
        val mes2 = dialogTwoBtn.findViewById<TextView>(R.id.mes2)
        val action1 = dialogTwoBtn.findViewById<TextView>(R.id.action1)
        val action2 = dialogTwoBtn.findViewById<TextView>(R.id.action2)

        var showDialog = true
        val timestamp = intent.extras?.getString("timestamp") ?: ""

//        Log.i(TAG, "showInvitationFromNotificationDialog(), timestamp: $timestamp, notificationNoDialog: $notificationNoDialog")

        GlobalScope.launch(Dispatchers.Main) {
            if (timestamp.isEmpty())
                mes2Title.text = getString(R.string.invitationTitle)
            else {

                val tsLong = timestamp.toLong()
                val deferredTimestamp = async { getTimestampFromServer() }
                val ts = deferredTimestamp.await().toLong()
                deferredTimestamp.cancel()
                val diffMin = (ts - tsLong) / 60000
                @SuppressLint("SetTextI18n")
                when (diffMin) {
                    0L -> mes2Title.text = "${getString(R.string.invitationTitle)}: ${getString(R.string.justNow)}"
                    1L -> mes2Title.text = "${getString(R.string.invitationTitle)}: ${getString(R.string.time)} $diffMin ${getString(R.string.minute)}"
                    in 2L..30L -> mes2Title.text = "${getString(R.string.invitationTitle)}: ${getString(R.string.time)} $diffMin ${getString(R.string.minutes)}"
                    else -> showDialog = false
                }
            }

            if (!showDialog)
                return@launch

            val fbUserId = intent.extras?.getString("fromUserId") ?: ""
            val deferredGetUserData = async { getUserData(fbUserId) }
            val user = deferredGetUserData.await()
            deferredGetUserData.cancel()
            var gameType = ""
            if (user != null && !user.singleGame!!)
                gameType = getString(R.string.typeDouble).uppercase((Locale.ROOT))

            mes2.text = getString(R.string.playerInvitation, intent.extras?.getString("fromUsername")
                    ?: "", gameType)
            action1.text = getString(R.string.reject)
            action2.text = getString(R.string.accept)
            dialogTwoBtn.setCancelable(true)
            dialogTwoBtn.setOnCancelListener {
                dialogTwoBtn.dismiss()
                notificationIntent = null
            }
            action1.setOnClickListener {
                dialogTwoBtn.dismiss()
                notificationIntent = null
            }
            action2.setOnClickListener {
                dialogTwoBtn.dismiss()
                dialogInfo.dismiss()
                dismissAllDialogs()
                val inviterUserId = intent.extras?.getString("fromUserId") ?: ""
                if (inviterUserId.isNotEmpty())
                    initMatch(inviterUserId)
                notificationIntent = null
            }
            action1.visibility = ImageView.VISIBLE
            action2.visibility = ImageView.VISIBLE
            if (!isFinishing)
                dialogTwoBtn.show()

        }

    }

    private fun showOfflineInvitationFromNotificationDialog(intent: Intent) {

        val mes2Title = dialogTwoBtn.findViewById<TextView>(R.id.mes2Title)
        val mes2 = dialogTwoBtn.findViewById<TextView>(R.id.mes2)
        val action1 = dialogTwoBtn.findViewById<TextView>(R.id.action1)
        val action2 = dialogTwoBtn.findViewById<TextView>(R.id.action2)

        GlobalScope.launch(Dispatchers.Main) {
            var title = getString(R.string.invitationTitle)

            val fbUserId = intent.extras?.getString("fromUserId") ?: ""
            val deferredGetUserData = async { getUserData(fbUserId) }
            val user = deferredGetUserData.await()
            deferredGetUserData.cancel()
            var gameType = ""
            if (user != null && !user.singleGame!!)
                gameType = getString(R.string.typeDouble).uppercase((Locale.ROOT))

            var mes = getString(R.string.playerInvitation, intent.extras?.getString("fromUsername")
                    ?: "", gameType)
            val actionId = intent.extras?.getString("actionId") ?: ""
            val matchId = intent.extras?.getString("matchId") ?: ""

//            Log.i(TAG, "showOfflineInvitationFromNotificationDialog(), matchId: $matchId, actionId: $actionId")

            var showDialog = false

            if (matchId.isNotEmpty() && (actionId == "3" || actionId == "4")) {
                val fromUsername = intent.extras?.getString("fromUsername") ?: ""
                val deferredGetMatchBaseData = async { getMatchBaseData(matchId) }
                val baseData = deferredGetMatchBaseData.await()
                deferredGetMatchBaseData.cancel()

                val deferredTimestamp = async { getTimestampFromServer() }
                val serverTimestamp = deferredTimestamp.await().toLong()
                deferredTimestamp.cancel()
                val deferredGetMatchTimestamp = async { getMatchTimestamp(matchId) }
                val matchTimestamp = deferredGetMatchTimestamp.await().toLong()
                deferredGetMatchTimestamp.cancel()
                val time = getUpdateTime(matchTimestamp, serverTimestamp)
                var type = ""
                if (!mMatchBaseData!!.singleGame!!)
                    type = "($gameType)"
                title = "${getString(R.string.continueMatchTitle)} $type \n$time"

                if (baseData != null) {
                    showDialog = true
                    mes = getString(R.string.continueMatchMessage, fromUsername, baseData.nameA, baseData.nameB)
                }

            }
            if (actionId == "1")
                showDialog = true

            if (showDialog) {
                notificationIntent = null
                mes2Title.text = title
                mes2.text = mes
                action1.text = getString(R.string.reject)
                action2.text = getString(R.string.accept)
                dialogTwoBtn.setCancelable(true)
                dialogTwoBtn.setOnCancelListener {
                    dialogTwoBtn.dismiss()
                }
                action1.setOnClickListener {
                    dialogTwoBtn.dismiss()
                }
                action2.setOnClickListener {
                    dialogTwoBtn.dismiss()
                    dialogInfo.dismiss()
                    dismissAllDialogs()
                    if (prefs.getBoolean("playOnline", false)) {
                        notificationIntent = intent
                        if (actionId == "1" || actionId == "3" || actionId == "4")
                            notificationNoDialog = true
                        playOnline(false)
                    }
                }
                action1.visibility = ImageView.VISIBLE
                action2.visibility = ImageView.VISIBLE
                if (!isFinishing)
                    dialogTwoBtn.show()
            }
        }
    }

    private fun showInvitationFromQuickMatchDialog(userId: String, mesTitle: String, action1a: String, action2a: String) {

        val mes2Title = dialogQuickMatchInvitation.findViewById<TextView>(R.id.mes2Title)
        val mes2 = dialogQuickMatchInvitation.findViewById<TextView>(R.id.mes2)
        val action1 = dialogQuickMatchInvitation.findViewById<TextView>(R.id.action1)
        val action2 = dialogQuickMatchInvitation.findViewById<TextView>(R.id.action2)

//        Log.i(TAG, "showInvitationFromQuickMatchDialog(), userId: $userId, quickMatchRejected: ${prefs.getString("quickMatchRejected", "")}")

        GlobalScope.launch(Dispatchers.Main) {
            val deferredGetUserData = async { getUserData(userId) }
            val user = deferredGetUserData.await()
            deferredGetUserData.cancel()
            if (user != null) {
                val userName = user.name ?: ""

//                Log.i(TAG, "showInvitationFromQuickMatchDialog(), userName: $userName, userGpsId: $userGpsId")

                mes2Title.text = mesTitle
                var gameType = ""
                if (!user.singleGame!!)
                    gameType = getString(R.string.typeDouble).uppercase((Locale.ROOT))
                mes2.text = getString(R.string.playerInvitation, userName, gameType)
                action1.text = action1a
                action2.text = action2a
                dialogQuickMatchInvitation.setCancelable(true)
                dialogQuickMatchInvitation.setOnCancelListener {
                    dialogQuickMatchInvitation.dismiss()
                }
                action1.setOnClickListener {
                    dialogQuickMatchInvitation.dismiss()
                }
                action2.setOnClickListener {
                    dialogQuickMatchInvitation.dismiss()
                    dialogInfo.dismiss()
                    dismissAllDialogs()
                    GlobalScope.launch(Dispatchers.Main) {
                        quickMatchUpdateTime = System.currentTimeMillis()
                        val deferredSetQuickMatchUserId = async { setQuickMatchUserId("0") }
                        deferredSetQuickMatchUserId.await()
                        deferredSetQuickMatchUserId.cancel()
                    }
                    btnPlayerResult.text = resources.getString(R.string.waitingForPlayer, userName)
                    initMatch(userId)

                }
                action1.visibility = ImageView.VISIBLE
                action2.visibility = ImageView.VISIBLE

                if (!isFinishing)
                    dialogQuickMatchInvitation.show()

            }

        }
    }

    private fun showUpdateAppDialog(mesTitle: String, mes: String, action1a: String, action2a: String) {

        val title = dialogTwoBtn.findViewById<TextView>(R.id.mes2Title)
        val mes2 = dialogTwoBtn.findViewById<TextView>(R.id.mes2)
        val action1 = dialogTwoBtn.findViewById<TextView>(R.id.action1)
        val action2 = dialogTwoBtn.findViewById<TextView>(R.id.action2)

        title.text = mesTitle
        mes2.text = mes
        action1.text = action1a
        action2.text = action2a
        dialogTwoBtn.setCancelable(true)
        dialogTwoBtn.setOnCancelListener {
            dialogTwoBtn.dismiss()
            notificationIntent = null
        }
        action1.setOnClickListener {
            dialogTwoBtn.dismiss()
            notificationIntent = null
        }
        action2.setOnClickListener {
            dialogTwoBtn.dismiss()
            infoIntent = Intent(Intent.ACTION_VIEW)
            infoIntent.data = Uri.parse(URI_ESCALERO)
            infoIntent.setPackage("com.android.vending")
            startActivityForResult(infoIntent, INFO_REQUEST_CODE)
        }
        action1.visibility = ImageView.VISIBLE
        action2.visibility = ImageView.VISIBLE
        if (!isFinishing)
            dialogTwoBtn.show()
    }

    @SuppressLint("SetTextI18n")
    private fun showRematchDialog(isRematch: Boolean, rmPlayerA: String, rmPlayerB: String, playerEpA: Long, playerEpB: Long, playerResultA: Long, playerResultB: Long, result: Long) {

//        Log.d(TAG, "showRematchDialog(), isRematch: $isRematch, A: $rmPlayerA, B: $rmPlayerB, " +
//                "playerEpA: $playerEpA, playerEpB: $playerEpB, playerResultA: $playerResultA, playerResultB: $playerResultB")

        val playerNames = dialogRematch.findViewById<TextView>(R.id.playerNames)
        val playerResult = dialogRematch.findViewById<TextView>(R.id.playerResult)
        val playerNameA = dialogRematch.findViewById<TextView>(R.id.playerNameA)
        val playerEpOldA = dialogRematch.findViewById<TextView>(R.id.playerEpOldA)
        val playerResultAx = dialogRematch.findViewById<TextView>(R.id.playerResultA)
        val playerEpNewA = dialogRematch.findViewById<TextView>(R.id.playerEpNewA)
        val playerNameB = dialogRematch.findViewById<TextView>(R.id.playerNameB)
        val playerEpOldB = dialogRematch.findViewById<TextView>(R.id.playerEpOldB)
        val playerResultBx = dialogRematch.findViewById<TextView>(R.id.playerResultB)
        val playerEpNewB = dialogRematch.findViewById<TextView>(R.id.playerEpNewB)
        val action1 = dialogRematch.findViewById<TextView>(R.id.action1)
        val action2 = dialogRematch.findViewById<TextView>(R.id.action2)

        updateUserStatus(playerId = mPlayerId, playing = false, singleGame = ed.isSingleGame)

        if (mPlayerId != null)
            updatePlayerEp(mPlayerId!!, result)

        playerNames.text = "$rmPlayerA - $rmPlayerB"
        playerResult.text = ed.playerResult

        var newA = playerEpA + playerResultA
        if (newA < 0L)
            newA = 0L
        var newB = playerEpB + playerResultB
        if (newB < 0L)
            newB = 0L

//        Log.d(TAG, "showRematchDialog(), A: $rmPlayerA, playerEpA: $playerEpA, playerResultA: $playerResultA, newA: $newA")
//        Log.d(TAG, "showRematchDialog(), B: $rmPlayerB, playerEpB: $playerEpB, playerResultB: $playerResultB, newB: $newB")

        mOpponentEp =  if (mPlayerABC == "A")
            newB
        else
            newA

        diceBoard.mOnlinePlayers = "$rmPlayerA - $rmPlayerB"
        diceBoard.updateBoard(diceRoll, diceHold, diceDouble1, false, 0, initRollValues)

        playerNameA.text = "$rmPlayerA:"
        playerEpOldA.text = "$playerEpA"
        if (playerResultA > 0L)
            playerResultAx.text = "+$playerResultA"
        else
            playerResultAx.text = "$playerResultA"
        playerEpNewA.text = "$newA EP"

        playerNameB.text = "$rmPlayerB:"
        playerEpOldB.text = "$playerEpB"
        if (playerResultB > 0L)
            playerResultBx.text = "+$playerResultB"
        else
            playerResultBx.text = "$playerResultB"
        playerEpNewB.text = "$newB EP"

        dialogRematch.setOnCancelListener {
            if (mMatchId != null) {
                GlobalScope.launch(Dispatchers.Main) {
                    matchCanceled(mOpponentId!!, mMatchId!!, "8")
                    startRemoveMatch(false, mMatchId!!, mPlayerId!!, mOpponentId!!)
                }
            }
            dialogRematch.dismiss()
        }
        action1.setOnClickListener {
            if (mMatchId != null) {
                GlobalScope.launch(Dispatchers.Main) {
                    matchCanceled(mOpponentId!!, mMatchId!!, "8")
                    startRemoveMatch(false, mMatchId!!, mPlayerId!!, mOpponentId!!)
                }
            }
            dialogRematch.dismiss()
        }
        action2.setBackgroundResource(R.drawable.rectangleyellow)
        action2.setOnClickListener {

//            Log.d(TAG, "showRematchDialog(), isRematch: $isRematch, mMatchId: $mMatchId")

            if (mMatchId != null) {
                if (isRematch)
                    matchDataToDb(false, ONLINE_REMATCH)
                else
                    matchDataToDb(true, ONLINE_REMATCH_TURN)
            }
            else
                showErrorMessage(R.string.matchPauseAndFinished)

            dialogRematch.dismiss()
        }
        dialogRematch.setCancelable(true)

        if (!isFinishing)
            dialogRematch.show()

    }

    private fun showAcceptRematchDialog(isNextPlayer: Boolean, rmPlayerA: String, rmPlayerB: String) {

        val mes2Title = dialogTwoBtn.findViewById<TextView>(R.id.mes2Title)
        val mes2 = dialogTwoBtn.findViewById<TextView>(R.id.mes2)
        val action1 = dialogTwoBtn.findViewById<TextView>(R.id.action1)
        val action2 = dialogTwoBtn.findViewById<TextView>(R.id.action2)

//        Log.d(TAG, "showRematchDialog(), isRematch: $isRematch, A: $rmPlayerA, B: $rmPlayerB, starter: $starter")

        mes2Title.text = getString(R.string.rematchTitle)
        val rmPlayers = "$rmPlayerA - $rmPlayerB"
        val rmRematch = getString(R.string.playAgain, mOpponentName ?: "???")

        diceBoard.mOnlinePlayers = "$rmPlayerA - $rmPlayerB"
        diceBoard.updateBoard(diceRoll, diceHold, diceDouble1, false, 0, initRollValues)

        val mes = "$rmPlayers\n\n$rmRematch"
        mes2.text = mes
        action1.text = getString(R.string.dialogNo)
        action2.text = getString(R.string.dialogYes)
        dialogTwoBtn.setOnCancelListener {
            if (mMatchId != null) {
                GlobalScope.launch(Dispatchers.Main) {
                    matchCanceled(mOpponentId!!, mMatchId!!, "8")
                    startRemoveMatch(false, mMatchId!!, mPlayerId!!, mOpponentId!!)
                }
            }
            dialogTwoBtn.dismiss()
        }
        action1.setOnClickListener {
            if (mMatchId != null) {
                GlobalScope.launch(Dispatchers.Main) {
                    matchCanceled(mOpponentId!!, mMatchId!!, "8")
                    startRemoveMatch(false, mMatchId!!, mPlayerId!!, mOpponentId!!)
                }
            }
            dialogTwoBtn.dismiss()
        }
        action2.setBackgroundResource(R.drawable.rectangleyellow)
        action2.setOnClickListener {
            if (isNextPlayer) {
                matchDataToDb(true, ONLINE_START_REMATCH)
                btnDice.visibility = ImageView.VISIBLE
                btnDice.setImageResource(R.drawable.button_stop)
            }
            else {
                if (mMatchId != null && mMatchUpdateData != null)
                    startRematch()
            }
            dialogTwoBtn.dismiss()
            dialogRematch.dismiss()
        }
        dialogTwoBtn.setCancelable(true)
        action1.visibility = ImageView.VISIBLE
        action2.visibility = ImageView.VISIBLE

        if (!isFinishing)
            dialogTwoBtn.show()

    }

    private fun updatePlayerEp(playerId: String, result: Long) {

//        Log.d(TAG, "updatePlayerEp(), playerId: $playerId, result: $result")

        GlobalScope.launch(Dispatchers.Main) {

            var isMaxScore = false
            var newScore = 0L

            val deferredLeaderboardScore = async { getLeaderboardScore(playerId) }
            val leaderboardScore = deferredLeaderboardScore.await()
            deferredLeaderboardScore.cancel()

//            Log.d(TAG, "updatePlayerEp(), leaderboardScore: $leaderboardScore")

            if (leaderboardScore >= 0) {
                val ld = Leaderboard()
                ld.name = mPlayerName
                ld.matchCnt = 0L    //karl
                newScore = leaderboardScore + result
                if (newScore < 0L)
                    newScore = 0L
                if (newScore >= ONLINE_LEADERBORD_MAX) {
                    isMaxScore = true
                    newScore = ONLINE_LEADERBORD_BONUS_1
                }
                ld.score = newScore

                ld.timestamp = 0L
                ld.uid = playerId

//                Log.d(TAG, "updatePlayerEp(), leaderboardScore: $leaderboardScore, newScore: $newScore")

                val deferredUpdateLeaderboardData = async { updateLeaderboardData(playerId, ld) }
                deferredUpdateLeaderboardData.await()
                deferredUpdateLeaderboardData.cancel()

            }

            mPlayerEp = newScore

            setEP()

            if (isMaxScore) {
                showInfoDialog(getString(R.string.info), getString(R.string.max300), getString(R.string.ok))
            }

        }
    }

    private fun showPauseMatchDialog() {

        val mes2Title = dialogTwoBtn.findViewById<TextView>(R.id.mes2Title)
        val mes2 = dialogTwoBtn.findViewById<TextView>(R.id.mes2)
        val action1 = dialogTwoBtn.findViewById<TextView>(R.id.action1)
        val action2 = dialogTwoBtn.findViewById<TextView>(R.id.action2)

        action1.visibility = ImageView.VISIBLE
        action2.visibility = ImageView.VISIBLE
        mes2Title.text = getString(R.string.info)
        if (isDoingTurn)
            mes2.text = getString(R.string.finishActiveMatch)
        else
            mes2.text = getString(R.string.theirTurnPauseMatch)
        action1.text = getString(R.string.appContinue)
        action2.text = getString(R.string.matchPause)
        dialogTwoBtn.setCancelable(true)
        dialogTwoBtn.setOnCancelListener {
            dialogTwoBtn.dismiss()
        }
        action1.setOnClickListener {
            dialogTwoBtn.dismiss()
        }
        action2.setBackgroundResource(R.drawable.rectangleyellow)
        action2.setOnClickListener {
            if (isDoingTurn) {
                    matchDataToDb(false, getPausedStatus())
                    Toast.makeText(applicationContext, getString(R.string.matchBreak), Toast.LENGTH_SHORT).show()
                    diceBoard.mOnlinePlayers = ""
                    diceBoard.updateBoard(diceRoll, diceHold, diceDouble1, false, 1, initRollValues)
            } else {
                Toast.makeText(applicationContext, getString(R.string.matchPauseAndFinished), Toast.LENGTH_SHORT).show()
                var status = getPausedStatus()
                status = status.replace(PAUSED, "_$THEIR_TURN_PAUSED")
                matchDataToDb(true, status)
                diceBoard.mOnlinePlayers = ""
                diceBoard.updateBoard(diceRoll, diceHold, diceDouble1, false, 0, initRollValues)
            }
            dialogTwoBtn.dismiss()
        }
        if (!isFinishing)
            dialogTwoBtn.show()
    }

    private fun showDeleteMatchDialog() {

        val mes2Title = dialogTwoBtn.findViewById<TextView>(R.id.mes2Title)
        val mes2 = dialogTwoBtn.findViewById<TextView>(R.id.mes2)
        val action1 = dialogTwoBtn.findViewById<TextView>(R.id.action1)
        val action2 = dialogTwoBtn.findViewById<TextView>(R.id.action2)

        action1.visibility = ImageView.VISIBLE
        action2.visibility = ImageView.VISIBLE
        mes2Title.text = getString(R.string.info)
        mes2.text = getString(R.string.dismissMatch)
        action1.text = getString(R.string.dialogNo)
        action2.text = getString(R.string.dialogYes)
        dialogTwoBtn.setCancelable(true)
        dialogTwoBtn.setOnCancelListener {
            dialogTwoBtn.dismiss()
        }
        action1.setOnClickListener {
            dialogTwoBtn.dismiss()
        }
        action2.setBackgroundResource(R.drawable.rectangleyellow)
        action2.setOnClickListener {
            if (checkConnectivity(false) && mMatchId != null && mOpponentId != null) {
                GlobalScope.launch(Dispatchers.Main) {
                    matchCanceled(mOpponentId!!, mMatchId!!, "9")
                    startRemoveMatch(true, mMatchId!!, mPlayerId!!, mOpponentId!!)
                }
            }
            dialogTwoBtn.dismiss()
        }
        if (!isFinishing)
            dialogTwoBtn.show()
    }

    private fun showWelcomeDialog() {

        val mes2Title = dialogTwoBtn.findViewById<TextView>(R.id.mes2Title)
        val mes2 = dialogTwoBtn.findViewById<TextView>(R.id.mes2)
        val action1 = dialogTwoBtn.findViewById<TextView>(R.id.action1)
        val action2 = dialogTwoBtn.findViewById<TextView>(R.id.action2)

        action1.visibility = ImageView.VISIBLE
        action2.visibility = ImageView.VISIBLE
        mes2Title.text = getString(R.string.welcome)
        mes2.text = getString(R.string.welcomeText)
        action1.text = getString(R.string.infoGame)
        action2.text = getString(R.string.appContinue)
        dialogTwoBtn.setCancelable(true)
        dialogTwoBtn.setOnCancelListener {
            dialogTwoBtn.dismiss()
        }
        action1.setOnClickListener {
            infoIntent = Intent(Intent.ACTION_VIEW)
            val currentLang = Locale.getDefault().language
            infoIntent.data =
                when (currentLang) {
                    "de" -> Uri.parse(URI_MANUAL_DE)
                    else -> Uri.parse(URI_MANUAL_EN)
                }
            startActivityForResult(infoIntent, INFO_REQUEST_CODE)
            dialogTwoBtn.dismiss()
        }
        action2.setBackgroundResource(R.drawable.rectangleyellow)
        action2.setOnClickListener {
            playOffline(false)
            dialogTwoBtn.dismiss()
        }
        if (!isFinishing)
            dialogTwoBtn.show()
    }

    //ONLINE - methods, general
    private fun showPlayOnlineDialog() {

//        Log.d(TAG, "1 showPlayOnlineDialog(), mIsSignIn: $mIsSignIn, mPlayerEp: $mPlayerEp")

        if (!checkConnectivity(false))
            return
        if (mIsSignIn && mPlayerEp == 0L) {
            showNoEpDialog()
            return
        }

        GlobalScope.launch(Dispatchers.Main) {

            val logOutBtn = dialogPlayOnline.findViewById<TextView>(R.id.logOutBtn)
            val logInName = dialogPlayOnline.findViewById<TextView>(R.id.logInName)
            val escaleroPoints = dialogPlayOnline.findViewById<TextView>(R.id.escaleroPoints)
            val rbSingle = dialogPlayOnline.findViewById<RadioButton>(R.id.rb_single)
            val rbDouble = dialogPlayOnline.findViewById<RadioButton>(R.id.rb_double)
            val playerSearch = dialogPlayOnline.findViewById<TextView>(R.id.playerSearch)
            val quickMatch = dialogPlayOnline.findViewById<TextView>(R.id.quickMatch)
            val checkMatches = dialogPlayOnline.findViewById<TextView>(R.id.checkMatches)

            if (mIsSignIn) {
                logOutBtn.visibility = TextView.VISIBLE
                logInName.text = mPlayerName
            }
            else {
                logOutBtn.visibility = TextView.INVISIBLE
                logInName.text = getString(R.string.logIn)
                val epPlayer = "$mPlayerEp EP"
                val epPrefs = "(? EP)"
                if (mIsSignIn && mPlayerEp >= 0)
                    escaleroPoints.text = epPlayer
                else
                    escaleroPoints.text = epPrefs
            }

            logInName.setOnClickListener {

                if (checkConnectivity(false)) {
                    if (mIsSignIn) {
                        if (isOnlineActive)
                            showInfoDialog(getString(R.string.info), getString(R.string.disabledOnline), getString(R.string.ok))
                        else
                            showUpdatePlayerNameDialog(false)
                    } else {
                        startSignInIntent()
                    }
                }

            }

            logOutBtn.setOnClickListener {
                signOut()
                mIsSignIn = false
                if (checkConnectivity(true))
                    showPlayOnlineDialog()
            }

            if (ed.isSingleGame) {
                rbSingle.isChecked = true
                rbDouble.isChecked = false
            }
            else {
                rbDouble.isChecked = true
                rbSingle.isChecked = false
            }
            rbSingle.setOnClickListener {
                if (isOnlineActive) {
                    showInfoDialog(getString(R.string.info), getString(R.string.firstFinishActiveMatch), getString(R.string.ok))
                    dialogPlayOnline.dismiss()
                }
                else {
                    if (!ed.isSingleGame) {
                        ed.isSingleGame = true
                        val edi = prefs.edit()
                        edi.putBoolean("isSingleGame", ed.isSingleGame)
                        edi.apply()
                        rbSingle.isChecked = true
                        rbDouble.isChecked = false
                        updateUserStatus(playerId = mPlayerId, singleGame = ed.isSingleGame)
                        isInitBoard = true
                        handlerAnimationDices.removeCallbacks(mUpdateAnimationDices)
                        handlerAnimationDices.postDelayed(mUpdateAnimationDices, 50)

                        //                Log.d(TAG, "showPlayOnlineDialog(), rb_single, ed.isSingleGame: ${ed.isSingleGame}")

                    }
                }
            }
            rbDouble.setOnClickListener {
                if (isOnlineActive) {
                    showInfoDialog(getString(R.string.info), getString(R.string.firstFinishActiveMatch), getString(R.string.ok))
                    dialogPlayOnline.dismiss()
                }
                else {
                    if (ed.isSingleGame) {
                        ed.isSingleGame = false
                        val edi = prefs.edit()
                        edi.putBoolean("isSingleGame", ed.isSingleGame)
                        edi.apply()
                        rbDouble.isChecked = true
                        rbSingle.isChecked = false
                        updateUserStatus(playerId = mPlayerId, singleGame = ed.isSingleGame)
                        isInitBoard = true
                        handlerAnimationDices.removeCallbacks(mUpdateAnimationDices)
                        handlerAnimationDices.postDelayed(mUpdateAnimationDices, 50)

                        //                Log.d(TAG, "showPlayOnlineDialog(), rb_double, ed.isSingleGame: ${ed.isSingleGame}")

                    }
                }

    //            Log.d(TAG, "showPlayOnlineDialog(), dialogPlayOnline.rb_double, clicked")

            }

            playerSearch.setOnClickListener {
                if (!mIsSignIn) {
                    showErrorMessage(R.string.notLoggedIn)
                } else {
                    showPlayerQuery()
                }
            }

            quickMatch.setOnClickListener {
                if (!mIsSignIn) {
                    showErrorMessage(R.string.notLoggedIn)
                } else {
                    if (isOnlineActive)
                        showInfoDialog(getString(R.string.info), getString(R.string.firstFinishActiveMatch), getString(R.string.ok))
                    else {
                        startQuickMatch()
                        dialogPlayOnline.dismiss()
                    }
                }
            }

            checkMatches.setOnClickListener {
                if (!mIsSignIn) {
                    showErrorMessage(R.string.notLoggedIn)
                } else {
                    checkMatches()
                }
            }

                val epPlayer = "$mPlayerEp EP"
                val epPrefs = "(? EP)"

    //        Log.d(TAG, "A  showPlayOnlineDialog(), mIsSignIn: $mIsSignIn, mPlayerEp: $mPlayerEp")
    //        Log.d(TAG, "B showPlayOnlineDialog(), dialogPlayOnline.isShowing: ${dialogPlayOnline.isShowing}, isFinishing: $isFinishing")

            if (mIsSignIn && mPlayerEp >= 0)
                escaleroPoints.text = epPlayer

            if (!mPlayerId.isNullOrBlank()) {
                val deferredLeaderboardScore = async { getLeaderboardScore(mPlayerId!!) }
                mPlayerEp = deferredLeaderboardScore.await()
                deferredLeaderboardScore.cancel()

//                Log.d(TAG, "showPlayOnlineDialog(), leaderboardScore: $mPlayerEp")

                setEP()

            }

//                Log.d(TAG, "Z2 showPlayOnlineDialog(), prefs, leaderboardTimestamp: ${runPrefs.getLong("leaderboardTimestamp", 1L)}")

            if (mIsSignIn && mPlayerEp >= 0)
                escaleroPoints.text = epPlayer
            else
                escaleroPoints.text = epPrefs

            if (!dialogPlayOnline.isShowing && !isFinishing)
                dialogPlayOnline.show()

        }

    }

    private fun showUpdatePlayerNameDialog(isNewPlayer: Boolean) {

        dialogPlayOnline.dismiss()

        val playerName = dialogPlayerName.findViewById<TextView>(R.id.playerName)
        val playerNameAction = dialogPlayerName.findViewById<TextView>(R.id.playerNameAction)
        val playerNameInfo = dialogPlayerName.findViewById<TextView>(R.id.playerNameInfo)
        val btnAction = dialogPlayerName.findViewById<TextView>(R.id.btnAction)
        val btnCancel = dialogPlayerName.findViewById<TextView>(R.id.btnCancel)

        if (isNewPlayer) {
            dialogPlayerName.setCancelable(false)
            playerNameAction.text = getString(R.string.playerNew)
            playerNameInfo.text = getString(R.string.infoPlayerNew)
            btnAction.text = getString(R.string.googleSignIn)
        }
        else {
            dialogPlayerName.setCancelable(true)
            playerNameAction.text = getString(R.string.playerUpdate, mPlayerName)
            playerNameInfo.text = getString(R.string.infoPlayerUpdate)
            btnAction.text = getString(R.string.matchUpdate)
        }

        btnCancel.setOnClickListener {
            if (!isNewPlayer)
                dialogPlayerName.dismiss()
        }

        btnAction.setOnClickListener {

//            Log.d(TAG, "1 showUpdatePlayerNameDialog(), isNewPlayer: $isNewPlayer, mPlayerName: $mPlayerName")
//            Log.d(TAG, "2 showUpdatePlayerNameDialog(), isNewPlayer: $isNewPlayer, dialogPlayerName: ${dialogPlayerName.playerName.text}")
//            Log.d(TAG, "3 showUpdatePlayerNameDialog(), isNewPlayer: $isNewPlayer, text.length: ${dialogPlayerName.playerName.text.length}")

            if (playerName.text.length >= 4 && mPlayerId != null) {

//                Log.d(TAG, "6 showUpdatePlayerNameDialog(), isNewPlayer: $isNewPlayer, mPlayerId: $mPlayerId")

                GlobalScope.launch(Dispatchers.Main) {

                    val deferredUserIdFromName = async { getUserIdFromName(playerName.text.toString()) }
                    val fbUserId = deferredUserIdFromName.await()
                    deferredUserIdFromName.cancel()

                    if (fbUserId == "")
                        mPlayerName = playerName.text.toString()
                    else {
                        showInfoDialog(getString(R.string.info), getString(R.string.playerNameExists, playerName.text.toString()), getString(R.string.ok))
                        playerName.text = ""
                        return@launch
                    }

                    if (mPlayerName.isNullOrEmpty())
                        return@launch

                    val userData = User()
                    userData.ads = true
                    userData.appVersionCode = BuildConfig.VERSION_CODE.toLong()
                    userData.chat = false
                    userData.currentMatchId = ""
                    userData.iconImageUri = mPlayerIconImageUri
                    userData.language = mCurrentLang
                    userData.name = mPlayerName
                    userData.notifications = true
                    userData.online = true
                    @SuppressLint("HardwareIds")
                    userData.onlineCheckId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).subSequence(0, 7).toString()
                    userData.playing = false
                    userData.singleGame = ed.isSingleGame
                    userData.timestamp = 0L
                    userData.token = ""
                    userData.uid = mPlayerId

                    val deferredUpdateUserData = async { updateUserData(mPlayerId!!, userData) }
                    deferredUpdateUserData.await()
                    deferredUpdateUserData.cancel()

                    mPlayerEp = if (isNewPlayer)
                        ONLINE_LEADERBORD_BONUS_1
                    else
                        0L
                    val ld = Leaderboard()
                    ld.name = mPlayerName
                    ld.score = mPlayerEp
                    ld.uid = mPlayerId

                    val deferredUpdateLeaderboardData = async { updateLeaderboardData(mPlayerId!!, ld) }
                    deferredUpdateLeaderboardData.await()
                    deferredUpdateLeaderboardData.cancel()

                    val ref = FirebaseDatabase.getInstance().getReference("userMatches/${mPlayerId!!}")
                    ref.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.children.count() > 0) {
                                dataSnapshot.children.forEach {
                                    val um = it.getValue(UserMatch::class.java)

                                    if (um != null) {
                                        val matchId = um.matchId ?: ""
                                        if (matchId.isNotEmpty()) {
                                            GlobalScope.launch(Dispatchers.Main) {
                                                val deferredGetMatchBaseData = async { getMatchBaseData(matchId) }
                                                val baseData = deferredGetMatchBaseData.await()
                                                deferredGetMatchBaseData.cancel()
                                                if (baseData != null) {

//                                                Log.d(TAG, "B showUpdatePlayerNameDialog(), baseData.playerIdA: ${baseData.playerIdA}, baseData.playerIdB: ${baseData.playerIdB}")

                                                    val refPlayerA = FirebaseDatabase.getInstance().getReference("userMatches")
                                                    refPlayerA.child(baseData.playerIdA!!).child(matchId).removeValue()
                                                    val refPlayerB = FirebaseDatabase.getInstance().getReference("userMatches")
                                                    refPlayerB.child(baseData.playerIdB!!).child(matchId).removeValue()
                                                    if (matchId != "") {
                                                        val refMatches = FirebaseDatabase.getInstance().getReference("matches/$matchId")
                                                        refMatches.removeValue()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            ref.removeEventListener(this)
                        }
                    })

                    showPlayOnlineDialog()

                    dialogPlayerName.dismiss()

                }
            }
            else {
                showInfoDialog(getString(R.string.info), getString(R.string.playerNameInfo), getString(R.string.ok))
            }

        }

        dialogPlayerName.show()
    }

    // custom UI for managing matches
    private fun checkMatches() {

        if (mPlayerId != null) {
            GlobalScope.launch(Dispatchers.Main) {
                val deferredGetUserMatches = async { getUserMatches(mPlayerId!!) }
                val matchList = deferredGetUserMatches.await()
                deferredGetUserMatches.cancel()
                showMatchDialog(matchList, false)
            }
        }

        dismissAllDialogs()

    }

    private fun showPlayerQuery() {

//        Log.i(TAG, "showPlayerQuery(), start")

        dialogPlayerQuery.setContentView(R.layout.dialogplayerquery)

        val playerOnline = dialogPlayerQuery.findViewById<TextView>(R.id.playerOnline)
        val playerOftenActive = dialogPlayerQuery.findViewById<TextView>(R.id.playerOftenActive)
        val playerLeaderboard = dialogPlayerQuery.findViewById<TextView>(R.id.playerLeaderboard)
        val leaderboardListView = dialogPlayerQuery.findViewById<ListView>(R.id.leaderboardListView)
        val leaderboardEmpty = dialogPlayerQuery.findViewById<TextView>(R.id.leaderboardEmpty)
        val lbMes = dialogPlayerQuery.findViewById<TextView>(R.id.lbMes)
        val btnQuickMatch = dialogPlayerQuery.findViewById<TextView>(R.id.btnQuickMatch)
        val btnOk = dialogPlayerQuery.findViewById<TextView>(R.id.btnOk)

        dialogPlayOnline.dismiss()

        when (prefs.getInt("playerQueryId", 2)) {
            1 -> {
                playerOnline.setBackgroundColor(ContextCompat.getColor(this, R.color.colorDiceB))
                playerOftenActive.setBackgroundColor(ContextCompat.getColor(this, R.color.text_yellow))
                playerLeaderboard.setBackgroundColor(ContextCompat.getColor(this, R.color.text_yellow))
            }
            2 -> {
                playerOnline.setBackgroundColor(ContextCompat.getColor(this, R.color.text_yellow))
                playerOftenActive.setBackgroundColor(ContextCompat.getColor(this, R.color.colorDiceB))
                playerLeaderboard.setBackgroundColor(ContextCompat.getColor(this, R.color.text_yellow))
            }
            3 -> {
                playerOnline.setBackgroundColor(ContextCompat.getColor(this, R.color.text_yellow))
                playerOftenActive.setBackgroundColor(ContextCompat.getColor(this, R.color.text_yellow))
                playerLeaderboard.setBackgroundColor(ContextCompat.getColor(this, R.color.colorDiceB))
            }
        }
        playerOnline.setOnClickListener {
            val edi = prefs.edit()
            edi.putInt("playerQueryId", 1)
            edi.apply()
            dialogPlayerQuery.dismiss()
            showPlayerQuery()
        }
        playerOftenActive.setOnClickListener {
            val edi = prefs.edit()
            edi.putInt("playerQueryId", 2)
            edi.apply()
            dialogPlayerQuery.dismiss()
            showPlayerQuery()
        }
        playerLeaderboard.setOnClickListener {
            val edi = prefs.edit()
            edi.putInt("playerQueryId", 3)
            edi.apply()
            dialogPlayerQuery.dismiss()
            showPlayerQuery()
        }

        leaderboardListView.visibility = ListView.INVISIBLE
        leaderboardEmpty.visibility = ListView.VISIBLE
        dialogPlayerQuery.setCancelable(true)
        if (!isOnlineActive)
            lbMes.text = getString(R.string.invitationSelectPlayer)
        else
            lbMes.text = ""
        btnQuickMatch.setOnClickListener {
            if (isOnlineActive)
                showInfoDialog(getString(R.string.info), getString(R.string.firstFinishActiveMatch), getString(R.string.ok))
            else
                startQuickMatch()
        }
        btnOk.setOnClickListener {
            dialogPlayerQuery.dismiss()
        }
        dialogPlayerQuery.setOnCancelListener {
            dialogPlayerQuery.dismiss()
        }

        if (!dialogPlayerQuery.isShowing && !isFinishing)
            dialogPlayerQuery.show()

        GlobalScope.launch(Dispatchers.Main) {

//            Log.i(TAG, "showPlayerQuery(), bestScores, worstScores")

            val deferredLeaderboardCount = async { getLeaderboardCount() }
            val lbCount = deferredLeaderboardCount.await()
            deferredLeaderboardCount.cancel()
            var lbView = lbCount
            if (lbCount > ONLINE_LEADERBORD_MAX_PLAYERS)
                lbView = ONLINE_LEADERBORD_MAX_PLAYERS
            val lbText = getString(R.string.leaderboard)
            val lbInfo = "$lbText $lbView($lbCount)"
            playerLeaderboard.text = lbInfo

            val lbList: ArrayList<UserList>
            when (prefs.getInt("playerQueryId", 2)) {
                1 -> {
                    val deferredOnline = async { getPlayerQueryArray(true) }
                    lbList = deferredOnline.await()
                    deferredOnline.cancel()
                }
                2 -> {
                    val deferredOftenActive = async { getPlayerQueryArray(false) }
                    lbList = deferredOftenActive.await()
                    deferredOftenActive.cancel()
                }
                else -> {
                    val deferredGetLeaderboardScores = async { getLeaderboardScores() }
                    lbList = deferredGetLeaderboardScores.await()
                    deferredGetLeaderboardScores.cancel()
                }
            }

            leaderboardListView.visibility = ListView.VISIBLE
            leaderboardEmpty.visibility = ListView.INVISIBLE

//            Log.i(TAG, "showPlayerQuery(), lbList.size: ${lbList.size}")

            val adapter = UserListAdapter(applicationContext, lbList)
            leaderboardListView.adapter = adapter
            leaderboardListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, x, _ ->

//                Log.i(TAG, "showPlayerQuery(), playerName: ${userListArray[x].playerName}, id: ${userListArray[x].playerId}")
//                Log.i(TAG, "showPlayerQuery(), mPlayerId: $mPlayerId")

                if (isOnlineActive) {
                    showInfoDialog(getString(R.string.info), getString(R.string.firstFinishActiveMatch), getString(R.string.ok))
                }
                else {
                    if (lbList.size > 0) {

//                        Log.i(TAG, "showPlayerQuery(), playerName: ${lbList[x].playerName}, mPlayerName: $mPlayerName")

                        if (lbList[x].playerName == mPlayerName) {
                            showInfoDialog(getString(R.string.info), getString(R.string.inviteYourself), getString(R.string.ok))
                        } else {
                            val playerIdList = arrayListOf<String>()
                            playerIdList.addAll(listOf(lbList[x].playerId))

                            GlobalScope.launch(Dispatchers.Main) {
                                val deferredUserIdFromName = async { getUserIdFromName(lbList[x].playerName) }
                                val fbUserId = deferredUserIdFromName.await()
                                deferredUserIdFromName.cancel()

                                val deferredGetUserData = async { getUserData(fbUserId) }
                                val user = deferredGetUserData.await()
                                deferredGetUserData.cancel()

                                if (user != null) {

//                                    Log.i(TAG, "showPlayerQuery(), queryUserId: ${user.uid!!}, queryUsername: ${user.name!!}")

                                    val deferredLeaderboardScore = async { getLeaderboardScore(fbUserId) }
                                    val leaderboardScore = deferredLeaderboardScore.await()
                                    deferredLeaderboardScore.cancel()
                                    var gameType = ""
                                    var isInvitation = true
                                    if (!ed.isSingleGame) {
                                        if (user.appVersionCode!! < MIN_VERSION_CODE) {
                                            val info = "$mPlayerName - ${user.name}\n\n${getString(R.string.differentAppVersions)}"
                                            showInfoDialog(getString(R.string.info), info, getString(R.string.ok))
                                            isInvitation = false
                                        }
                                        gameType = getString(R.string.typeDouble).uppercase((Locale.ROOT))
                                    }
                                    if (isInvitation) {
                                        val mes = "${user.name}, $leaderboardScore EP"
                                        if (user.notifications!! && (!user.playing!! || (user.playing!! && user.currentMatchId == mMatchId ?: "?")))
                                            showPlayerQueryDialog(user, getString(R.string.invitationNewGame, gameType),
                                                    mes, getString(R.string.appCancel), getString(R.string.invitationTitle))
                                        else {
                                            showInfoDialog(getString(R.string.info), getString(R.string.playerNotAvailable, user.name), getString(R.string.ok))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }

            try { dialogPlayerQuery.show() }
            catch (ignored: WindowManager.BadTokenException) { }

//                Log.i(TAG, "showPlayerQuery(), dialogPlayerQuery.show()")

        }

//        Log.i(TAG, "showPlayerQuery(), end")

    }

    private fun startQuickMatch() {
        if (!mPlayerId.isNullOrBlank()) {
            GlobalScope.launch(Dispatchers.Main) {
                val deferredGetQuickMatchUserId = async { getQuickMatchUserId() }
                val userId = deferredGetQuickMatchUserId.await()
                deferredGetQuickMatchUserId.cancel()
                if (userId == "0") {

//                    Log.i(TAG, "startQuickMatch(), userId: $userId")

                    val deferredSetQuickMatchUserId = async { setQuickMatchUserId(mPlayerId!!) }
                    deferredSetQuickMatchUserId.await()
                    deferredSetQuickMatchUserId.cancel()
                    Toast.makeText(applicationContext, getString(R.string.activatedQuickMatch), Toast.LENGTH_LONG).show()

                    mTimerQuickMatch.schedule(QUICK_MATCH_DELAY) {

//                        Log.i(TAG, "startQuickMatch(), mTimerQuickMatch.schedule")

                        GlobalScope.launch(Dispatchers.Main) {
                            val deferredGetQuickMatchTimerUserId = async { getQuickMatchUserId() }
                            val userIdTimer = deferredGetQuickMatchTimerUserId.await()
                            deferredGetQuickMatchTimerUserId.cancel()
                            if (userIdTimer == mPlayerId!!) {
                                setQuickMatchUserId("0")
                                if (appInForeground(applicationContext))
                                    Toast.makeText(applicationContext, getString(R.string.removedQuickMatch), Toast.LENGTH_LONG).show()
                            }
                        }

                    }

                } else {
                    if (userId == mPlayerId!!) {
                        showRemoveQuickMatchDialog(getString(R.string.info), getString(R.string.removeQuickMatch),
                                getString(R.string.appCancel), getString(R.string.dialogYes))
                    }
                    else {

//                        Log.i(TAG, "startQuickMatch(), userId: $userId, mPlayerId: ${mPlayerId!!}")

                        val deferredSetQuickMatchUserId = async { setQuickMatchUserId("0") }
                        deferredSetQuickMatchUserId.await()
                        deferredSetQuickMatchUserId.cancel()
                        val deferredGetUserData = async { getUserData(userId) }
                        val user = deferredGetUserData.await()
                        deferredGetUserData.cancel()
                        if (user != null) {
                            btnPlayerResult.text = resources.getString(R.string.waitingForPlayer, user.name)
                            if (user.uid != null)
                                initMatch(user.uid!!)
                            dialogPlayerQuery.dismiss()
                        }
                    }
                }
            }
        }
    }

    private fun getUpdateTime(timestamp: Long, serverTimestamp: Long): String {
        val timeMilSec = serverTimestamp - timestamp
        val minutes = (timeMilSec / (1000 * 60) % 60)
        val hours = (timeMilSec / (1000 * 60 * 60) % 24)
        val days = (timeMilSec / (1000 * 60 * 60) / 24)

//        Log.d(TAG, "getUpdateTime(), timeMs: $timeMilSec, days: $days, hours: $hours, minutes: $minutes")

        var d = getString(R.string.days)
        if (days == 1L)
            d = getString(R.string.day)
        var h = getString(R.string.hours)
        if (hours == 1L)
            h = getString(R.string.hour)
        var m = getString(R.string.minutes)
        if (minutes == 1L)
            m = getString(R.string.minute)
        val timeTxt = "${getString(R.string.time)} "

        if (days != 0L)
            return "$timeTxt$days $d"
        if (hours != 0L)
            return "$timeTxt$hours $h"
        if (minutes != 0L)
            return "$timeTxt$minutes $m"
        return getString(R.string.justNow)

    }

    private fun dismissAllDialogs() {

//        Log.d(TAG, "dismissAllDialogs()")

        if (dialogMain.isShowing)
            dialogMain.dismiss()
        if (dialogMatch.isShowing)
            dialogMatch.dismiss()
        if (dialogCurrentMatch.isShowing)
            dialogCurrentMatch.dismiss()
        if (dialogPlayOnline.isShowing)
            dialogPlayOnline.dismiss()
        if (dialogPlayerQuery.isShowing)
            dialogPlayerQuery.dismiss()
        if (mAccountingDialog != null) {
            if (mAccountingDialog!!.isShowing)
                mAccountingDialog!!.dismiss()
        }
    }

    private fun startOnlineMatch(matchId: String) {

        GlobalScope.launch(Dispatchers.Main) {

//            Log.d(TAG, "startOnlineMatch(), matchId: $matchId")

            mMatchId = matchId
            val deferredGetMatchBaseData = async { getMatchBaseData(matchId) }
            mMatchBaseData = deferredGetMatchBaseData.await()
            deferredGetMatchBaseData.cancel()
            val deferredGetMatchUpdateData = async { getMatchUpdateData(matchId) }
            mMatchUpdateData = deferredGetMatchUpdateData.await()
            deferredGetMatchUpdateData.cancel()

            if (mMatchId != null && mMatchBaseData != null && mMatchUpdateData != null) {
                dismissAllDialogs()

//                Log.d(TAG, "startOnlineMatch(), startMatchUpdateListener(), mMatchId: $mMatchId")
//                Log.d(TAG, "startOnlineMatch(), startMatchUpdateListener(), mOpponentId: $mOpponentId")
//                Log.d(TAG, "startOnlineMatch(), startMatchUpdateListener(), playerIdB: ${mMatchBaseData!!.playerIdB}, nameB: ${mMatchBaseData!!.nameB}")

                startMatchUpdateListener(mMatchId!!)

                if (mMatchUpdateData!!.starter == "A" && mMatchUpdateData!!.playerToMove == "A" && mMatchBaseData!!.nameA == mPlayerName) {
                    playSound(4, 0)
                    isOnlineActive = true
                    isDoingTurn = true
                    mOpponentId = mMatchBaseData!!.playerIdB!!
                    setOpponentData(mMatchBaseData!!.playerIdB!!, "B")
                    setPrefs(mMatchUpdateData!!)
                    setDataUpdate()
                    initRunPrefs()
                    setUI()
                    diceView(diceRoll, diceHold)
                    setNoActiveMatch()
                    matchDataToDb(false, ONLINE_START)
                } else {
                    initRunPrefs()
                    setPrefs(mMatchUpdateData!!)
                    setUI()
                    matchDataToDb(true, ONLINE_START)
                    setNoActiveMatch()
                }

            }
        }
    }

    private fun continueOnlineMatch(actionId: String) {

        if (mMatchId != null && mMatchBaseData != null && mMatchUpdateData != null) {
            dismissAllDialogs()

            if (mPlayerId == mMatchBaseData!!.playerIdA!!) {
                setOpponentData(mMatchBaseData!!.playerIdB!!, "B")
                mOpponentId = mMatchBaseData!!.playerIdB!!
                mOpponentName = mMatchBaseData!!.nameB
                mOpponentABC = "B"
                mPlayerABC = "A"
            }
            else {
                setOpponentData(mMatchBaseData!!.playerIdA!!, "A")
                mOpponentId = mMatchBaseData!!.playerIdA!!
                mOpponentName = mMatchBaseData!!.nameA
                mOpponentABC = "A"
                mPlayerABC = "B"
            }

            startMatchUpdateListener(mMatchId!!)

            mMatchUpdateData!!.onlineAction = ONLINE_START_CONTINUE

//            Log.d(TAG, "continueOnlineMatch(), mMatchId: $mMatchId, actionId: $actionId")

            ed.isSingleGame = mMatchBaseData!!.singleGame ?: true
            val edi = prefs.edit()
            edi.putBoolean("isSingleGame", ed.isSingleGame)
            edi.apply()

            when (actionId) {
                "3" -> {
                    initRunPrefs()
                    setPrefs(mMatchUpdateData!!)
                    setUI()
                    matchDataToDb(true, ONLINE_START_CONTINUE)
                    setNoActiveMatch()
                }
                "4" -> {
                    isOnlineActive = true
                    isDoingTurn = true
                    setPrefs(mMatchUpdateData!!)
                    setDataUpdate()
                    setUI()
                    diceView(diceRoll, diceHold)

//                    Log.d(TAG, "continueOnlineMatch(), setBtnPlayer(), ed.playerToMove: ${ed.playerToMove}")

                    setBtnPlayer(ed.playerToMove)
                    matchDataToDb(false, ONLINE_START_CONTINUE)
                }
            }
        }
    }

    private fun startMatchUpdateListener(matchId: String) {

//        Log.d(TAG, "startMatchUpdateListener(), matchId: $matchId")

        GlobalScope.launch(Dispatchers.Default) {
            matchUpdateListener(matchId)
        }

    }

    //ONLINE - methods, signing
    private fun checkConnectivity(isSignOut: Boolean): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val isConnected: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw      = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        }
        else
        @Suppress("DEPRECATION")
        {
            val nwInfo = connectivityManager.activeNetworkInfo ?: return false
            nwInfo.isConnected
        }

        if (!isConnected && !isSignOut)
            showInfoDialog(getString(R.string.warningTitle), getString(R.string.noInternet), getString(R.string.ok))
        return isConnected
    }

    private fun startSignInIntent() {
        startActivityForResult(mGoogleSignInClient!!.signInIntent, RC_SIGN_IN)
    }

    private fun signInSilently() {

//        Log.d(TAG, "signInSilently(), mPlayerId: $mPlayerId")

        GlobalScope.launch(Dispatchers.Main) {
            val deferredGoogleSignInAccount = async { signIn() }
            val googleSignInAccount = deferredGoogleSignInAccount.await()
            deferredGoogleSignInAccount.cancel()

//            Log.d(TAG, "signInSilently(), googleSignInAccount: $googleSignInAccount")

            if (googleSignInAccount != null) {
                onConnected(googleSignInAccount, mPlayerId.isNullOrBlank())
            }
            else {
                if (!dialogTwoBtn.isShowing)
                    showPlayOnlineDialog()
            }
        }

    }

    private fun onConnected(googleSignInAccount: GoogleSignInAccount?, showOnlineDialog: Boolean) {

//        Log.i(TAG, "onConnected(), showOnlineDialog: $showOnlineDialog")

        if (!checkConnectivity(false))
            return

//        Log.i(TAG, "2 onConnected(), checkConnectivity")

        //karl!?
        if (mIsSignIn && !mPlayerId.isNullOrBlank())
            return

        mIsSignIn = false

        GlobalScope.launch(Dispatchers.Main) {

            val deferredFirebaseAuthWithGoogleAccount = async { firebaseAuthWithGoogleAccount(googleSignInAccount) }
            mIsSignIn = deferredFirebaseAuthWithGoogleAccount.await()
            deferredFirebaseAuthWithGoogleAccount.cancel()

//            Log.d(TAG, "onConnected(), isFirebaseAuthOk: $mIsSignIn, mPlayerId: $mPlayerId")

            if (mIsSignIn && !mPlayerId.isNullOrBlank()) {

                val deferredGetUserData = async { getUserData(mPlayerId!!) }
                val user = deferredGetUserData.await()
                deferredGetUserData.cancel()
                if (user != null) {
                    mPlayerName = user.name ?: ""

//                    Log.d(TAG, "onConnected(), mMatchId: $mMatchId, user.matchId: ${user.currentMatchId}, user.online: ${user.online}, user.playing: ${user.playing}")

                    //karl??? problems with Settings.Secure.ANDROID_ID
                    @SuppressLint("HardwareIds")
                    if (user.online!! && user.currentMatchId != "" && user.onlineCheckId != Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).subSequence(0, 7)) {
                        playSound(1, 1)
                        showMultipleRegisteredDialog()
                        return@launch
                    }

                    if (mMatchId == null && user.online!! && user.playing!! && user.currentMatchId != "" ) {

//                        Log.d(TAG, "onConnected(), mMatchId: $mMatchId, user.matchId: ${user.currentMatchId}, user.online: ${user.online}, user.playing: ${user.playing}")

                        val deferredGetMatchBaseData = async { getMatchBaseData(user.currentMatchId!!) }
                        mMatchBaseData = deferredGetMatchBaseData.await()
                        deferredGetMatchBaseData.cancel()
                        val deferredGetMatchUpdateData = async { getMatchUpdateData(user.currentMatchId!!) }
                        mMatchUpdateData = deferredGetMatchUpdateData.await()
                        deferredGetMatchUpdateData.cancel()
                        if (mMatchBaseData != null && mMatchUpdateData != null) {
                            mMatchId = user.currentMatchId!!

                            val deferredGetMatchTimestamp = async { getMatchTimestamp(user.currentMatchId!!) }
                            val timestamp = deferredGetMatchTimestamp.await().toLong()
                            deferredGetMatchTimestamp.cancel()
                            val deferredTimestamp = async { getTimestampFromServer() }
                            val serverTimestamp = deferredTimestamp.await().toLong()
                            deferredTimestamp.cancel()

                            if (serverTimestamp - timestamp <= RECONNECT_TIME) {
                                mTimestampPre = timestamp
                                if (mPlayerId == mMatchBaseData!!.playerIdA!!) {
                                    mOpponentId = mMatchBaseData!!.playerIdB!!
                                    mOpponentName = mMatchBaseData!!.nameB
                                    mOpponentABC = "B"
                                    mPlayerABC = "A"
                                    setOpponentData(mMatchBaseData!!.playerIdB!!, "B")
                                } else {
                                    mOpponentId = mMatchBaseData!!.playerIdA!!
                                    mOpponentName = mMatchBaseData!!.nameA
                                    mOpponentABC = "A"
                                    mPlayerABC = "B"
                                    setOpponentData(mMatchBaseData!!.playerIdA!!, "A")
                                }

                                ed.isSingleGame = mMatchBaseData!!.singleGame ?: true
                                val edi = prefs.edit()
                                edi.putBoolean("isSingleGame", ed.isSingleGame)
                                edi.apply()

//                            Log.d(TAG, "4 showMatchDialog(), actionId: $actionId")

                                val deferredGetUserDataOpo = async { getUserData(mOpponentId!!) }
                                val userOpo = deferredGetUserDataOpo.await()
                                deferredGetUserDataOpo.cancel()

                                if (userOpo != null) {
                                    dialogTwoBtn.dismiss()
                                    dismissAllDialogs()
                                    if (userOpo.online!! && userOpo.playing!! && userOpo.currentMatchId == user.currentMatchId) {
                                        isOnlineActive = true
                                        startMatchUpdateListener(mMatchId!!)

                                        if (mMatchUpdateData!!.turnPlayerId!! == mPlayerId) {
                                            mMatchUpdateData!!.onlineAction += CORRECTION
                                            matchTurn(mMatchUpdateData!!)
                                        } else {
                                            matchUpdate(mMatchUpdateData!!)
                                        }

                                        return@launch

                                    }
                                } else {
                                    initOnline()
                                }
                            }
                        }
                    }

                }
                else {
                    showUpdatePlayerNameDialog(true)

                    return@launch

                }

//                Log.d(TAG, "2 onConnected(), mPlayerId: $mPlayerId, mPlayerName: $mPlayerName")

                if (!mPlayerName.isNullOrBlank()) {

                    val userData = User()
                    userData.ads = true
                    userData.appVersionCode = BuildConfig.VERSION_CODE.toLong()
                    userData.chat = false
                    userData.currentMatchId = if (isOnlineActive)
                        mMatchId
                    else
                        ""
                    userData.iconImageUri = mPlayerIconImageUri
                    userData.language = mCurrentLang
                    userData.name = mPlayerName
                    userData.notifications = true
                    userData.online = true
                    @SuppressLint("HardwareIds")
                    userData.onlineCheckId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).subSequence(0, 7).toString()
                    userData.playing = isOnlineActive
                    userData.singleGame = ed.isSingleGame
                    userData.timestamp = 0L
                    userData.token = ""
                    userData.uid = mPlayerId

                    val deferredUpdateUserData = async { updateUserData(mPlayerId!!, userData) }
                    deferredUpdateUserData.await()
                    deferredUpdateUserData.cancel()

                    val deferredLeaderboardScore = async { getLeaderboardScore(mPlayerId!!) }
                    mPlayerEp = deferredLeaderboardScore.await()
                    deferredLeaderboardScore.cancel()

                    if (mPlayerEp == -5L) {  // no data
                        val ld = Leaderboard()
                        ld.name = mPlayerName
                        ld.matchCnt = 0L    //karl
                        ld.score = ONLINE_LEADERBORD_BONUS_1

                        mPlayerEp = ld.score!!

                        ld.timestamp = 0L
                        ld.uid = mPlayerId

                        val deferredUpdateLeaderboardData = async { updateLeaderboardData(mPlayerId!!, ld) }
                        deferredUpdateLeaderboardData.await()
                        deferredUpdateLeaderboardData.cancel()

//                        Log.d(TAG, "3 onConnected(), mPlayerName: $mPlayerName, mPlayerEp: $mPlayerEp")

                    }

                    isInitBoard = true
                    setEP()

//                    Log.d(TAG, "4 onConnected(), notificationIntent: $notificationIntent, showOnlineDialog: $showOnlineDialog")

                    if (notificationIntent != null) {
                        val actionId = notificationIntent!!.extras?.getString("actionId")?: ""
                        val matchId = notificationIntent!!.extras?.getString("matchId")?: ""
                        val inviterUserId = notificationIntent!!.extras?.getString("fromUserId")?: ""
                        val fromUsername = notificationIntent!!.extras?.getString("fromUsername")?: ""
                        val tsIntent = notificationIntent!!.extras?.getString("timestamp")?.toLong() ?: 0L

//                        Log.i(TAG, "5 onConnected(), notificationIntent, actionId: $actionId, matchId; $matchId, tsIntent: $tsIntent")

                        if (tsIntent == 0L || prefs.getLong("notificationCheck", 0L) != tsIntent) {
                            val edi = prefs.edit()
                            edi.putLong("notificationCheck", tsIntent)
                            edi.apply()
                            if (isOnlineActive && !(actionId == "8" || actionId == "9")) {

//                            Log.i(TAG, "onConnected(), notificationIntent, !!! isOnlineActive !!!, actionId: $actionId, matchId: $matchId, fromUsername: $fromUsername")

                                GlobalScope.launch(Dispatchers.Main) {
                                    val deferredTimestamp = async { getTimestampFromServer() }
                                    val tsServer = deferredTimestamp.await().toLong()
                                    deferredTimestamp.cancel()
                                    when (actionId) {
                                        "7" -> {
                                            initOnline()
                                            setNoActiveMatch()
                                            if (tsServer - tsIntent <= NOTIFICATION_DELAY)
                                                showInfoDialog(getString(R.string.info), getString(R.string.playerNotAvailable, fromUsername), getString(R.string.ok))
                                            startRemoveMatch(true, matchId, null, null)
                                        }
                                        else -> {
                                            if (tsServer - tsIntent <= NOTIFICATION_DELAY)
                                                invitationNotAvailable(inviterUserId, matchId)
                                        }
                                    }
                                }

                            } else {
                                if (notificationNoDialog) {
                                    dialogTwoBtn.dismiss()
                                    dismissAllDialogs()
                                    notificationNoDialog = false
                                    if (actionId == "3" || actionId == "4") {
                                        mMatchId = matchId
                                        val deferredGetMatchBaseData = async { getMatchBaseData(matchId) }
                                        mMatchBaseData = deferredGetMatchBaseData.await()
                                        deferredGetMatchBaseData.cancel()
                                        val deferredGetMatchUpdateData = async { getMatchUpdateData(matchId) }
                                        mMatchUpdateData = deferredGetMatchUpdateData.await()
                                        deferredGetMatchUpdateData.cancel()

                                        continueOnlineMatch(actionId)

                                    } else {
                                        if (inviterUserId.isNotEmpty())
                                            initMatch(inviterUserId)
                                    }
                                } else {
                                    when (actionId) {
                                        "", "1" -> {
                                            if (notificationIntent != null)
                                                showInvitationFromNotificationDialog(notificationIntent!!)
                                        }
                                        "2" -> startOnlineMatch(matchId)
                                        "3", "4" -> showContinueMatchDialog(matchId, actionId)
                                        "8", "9" -> {
                                            if (mMatchId != null) {
                                                if (mMatchId == matchId) {
                                                    updateUserStatus(playerId = mPlayerId, playing = false, singleGame = ed.isSingleGame)
                                                    initOnline()
                                                    setNoActiveMatch()
                                                    when (actionId) {
                                                        "8" -> showInfoDialog(getString(R.string.info), getString(R.string.gameOver), getString(R.string.ok))
                                                        "9" -> showInfoDialog(getString(R.string.info), getString(R.string.matchDeletedByPlayer, fromUsername), getString(R.string.ok))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        notificationIntent = null

                    }
                    else {
                        if (showOnlineDialog && !dialogTwoBtn.isShowing)
                            showPlayOnlineDialog()
                        if (!isOnlineActive)
                            quickMatchListener()
                    }

                }

            }
            else
                showPlayOnlineDialog()

        }

    }

    private fun initOnline() {

//        Log.d(TAG, "initOnline()")

        handlerAnimationDices.removeCallbacks(mUpdateAnimationDices)

        isOnlineActive = false
        isDoingTurn = false
        isUpdating = false
        isOnlineEntry = false
        isUpdateError = false

        mMatchId = null

        mPlayerABC = null
        mOpponentId = null
        mOpponentABC = null
        mOpponentName = null
        mTimestampPre = 0L

        updateTable(true)

    }

    private fun quickMatchListener() {

//        Log.d(TAG, "quickMatchListener(), start")

        GlobalScope.launch(Dispatchers.Default) {

            quickMatchUpdateListener()

        }

    }

    private fun updateUserStatus(playerId: String? = null, ads: Boolean = true, chat: Boolean = false, notifications: Boolean = true, online: Boolean = true, playing: Boolean = false, singleGame: Boolean = true) {

//        Log.d(TAG, "updateUserStatus(), playerId: $playerId")

        if (playerId != null) {
            GlobalScope.launch(Dispatchers.Main) {
                val deferredGetUserData = async { getUserData(playerId) }
                val user = deferredGetUserData.await()
                deferredGetUserData.cancel()
                if (user != null) {
                    user.ads = ads
                    user.chat = chat
                    user.currentMatchId = if (playing)
                        mMatchId
                    else
                        ""
                    user.notifications = notifications
                    user.online = online
                    user.playing = playing
                    user.singleGame = singleGame
                    val deferredUpdateUserData = async { updateUserData(playerId, user) }
                    deferredUpdateUserData.await()
                    deferredUpdateUserData.cancel()
                }
            }
        }
    }

    private fun logOutUpdateUser(isFinishApp: Boolean, updateUser: Boolean) {

//        Log.d(TAG, "logOutUpdateUser(), isFinishApp: $isFinishApp, updateUser: $updateUser, mPlayerName: $mPlayerName, mPlayerId: $mPlayerId")

        GlobalScope.launch(Dispatchers.Main) {

            if (updateUser && !mPlayerId.isNullOrBlank() && !mPlayerName.isNullOrBlank()) {

                val userData = User()
                userData.ads = true
                userData.appVersionCode = BuildConfig.VERSION_CODE.toLong()
                userData.chat = false
                userData.currentMatchId = ""
                userData.iconImageUri = mPlayerIconImageUri
                userData.language = mCurrentLang
                userData.name = mPlayerName
                userData.notifications = true
                userData.online = false
                @SuppressLint("HardwareIds")
                userData.onlineCheckId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).subSequence(0, 7).toString()
                userData.playing = false
                userData.singleGame = ed.isSingleGame
                userData.timestamp = 0L
                userData.token = ""
                userData.uid = mPlayerId

                val deferredUpdateUserData = async { updateUserData(mPlayerId!!, userData) }
                deferredUpdateUserData.await()
                deferredUpdateUserData.cancel()

                val deferredGetQuickMatchUserId = async { getQuickMatchUserId() }
                val userId = deferredGetQuickMatchUserId.await()
                deferredGetQuickMatchUserId.cancel()
                if (userId == mPlayerId) {
                    val deferredSetQuickMatchUserId = async { setQuickMatchUserId("0") }
                    deferredSetQuickMatchUserId.await()
                    deferredSetQuickMatchUserId.cancel()
                }

                if (mMatchId != null) {
                    val deferredGetMatchUpdateData = async { getMatchUpdateData(mMatchId!!) }
                    val checkUpdateData = deferredGetMatchUpdateData.await()
                    deferredGetMatchUpdateData.cancel()

                    if (checkUpdateData != null && mOpponentId != null) {
                        if (checkUpdateData.position == "") {
                            startRemoveMatch(true, mMatchId!!, mPlayerId!!, mOpponentId!!)
                        }
                        else {
                            if (isDoingTurn)
                                matchDataToDb(false, getPausedStatus())
                            else {
                                var status = getPausedStatus()
                                status = status.replace(PAUSED, "_$THEIR_TURN_PAUSED")
                                matchDataToDb(true, status)
                            }
                        }
                    }
                }

                if (isFinishApp) {
                    setAppStart(true)
                    setRunPrefs()
                    finish()

                }

            }
            else {
                if (isFinishApp) {
                    setAppStart(true)
                    setRunPrefs()
                    finish()

                }
            }
        }
    }

    private fun setNoActiveMatch() {

        if (prefs.getBoolean("playOnline", false) && !isOnlineActive)
            quickMatchListener()

        btnDice.visibility = TextView.VISIBLE
        btnDice.setImageResource(R.drawable.button_play)
        btnPlayerResult.visibility = TextView.VISIBLE
        var playerResult = ""
        val playerResultCheck = getString(R.string.waitingForPlayer).subSequence(0, 5)
        if (btnPlayerResult.text.length >= 5)
            playerResult = btnPlayerResult.text.subSequence(0, 5).toString()
        if (playerResult != playerResultCheck) {
            btnPlayerResult.text = getString(R.string.noActiveMatch)
        }
        btnPlayerName.visibility = TextView.INVISIBLE
        btnPlayerIcon.visibility = TextView.INVISIBLE
        btnPlayerRound.visibility = TextView.INVISIBLE
    }

    private fun onDisconnected() {

//        Log.d(TAG, "onDisconnected()")

        initOnline()

    }

    private fun signOut() {

//        Log.d(TAG, "signOut()")

        initOnline()
        mGoogleSignInClient!!.signOut()
        FirebaseAuth.getInstance().signOut()

    }

    private fun setEP() {

        initBoard()
        if (mPlayerEp >= 0)
            diceBoard.mOnlinePlayers = "$mPlayerName: $mPlayerEp EP"
        else
            diceBoard.mOnlinePlayers = "$mPlayerName: (?) EP"

//        Log.d(TAG, "setEP(), diceBoard.mOnlinePlayers: ${diceBoard.mOnlinePlayers}")

        diceBoard.updateBoard(diceRoll, diceHold, diceDouble1, false, 0, initRollValues)

    }

    //ONLINE - methods, match
    private fun matchDataToDb(isNextPlayer: Boolean, onlineAction: String) {

//        Log.d(TAG, "matchDataToDb(), isNextPlayer: $isNextPlayer, onlineAction: $onlineAction, mOpponentId: $mOpponentId")

        if (onlineAction != ONLINE_START && mOpponentId.isNullOrEmpty()) {
            initOnline()
            setNoActiveMatch()
            showErrorMessage(R.string.matchPauseAndFinished)

            return

        }

        isUpdating = false

        diceView(diceRoll, diceHold)

        @SuppressLint("HardwareIds")
        if (!mPlayerName.isNullOrEmpty())
            ed.onlineCheckId = mPlayerName!! + " " + Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).subSequence(0, 7)

        when (onlineAction) {
            ONLINE_ACTIVE_ENTRY -> {
                btnDice.visibility = ImageView.INVISIBLE
            }
            ONLINE_ACTIVE_TURN -> {
                isOnlineEntry = false

                ed.computeNextPlayerToMove(ed.playerToMove)
                if (ed.playerToMove == ed.nextPlayerToMove) {
                    Toast.makeText(applicationContext, "ONLINE_ACTIVE_TURN, " + getString(R.string.dataUpdateProblem), Toast.LENGTH_SHORT).show()
                    playSound(1, 0)
                    return
                }
                ed.playerToMove = ed.nextPlayerToMove
                updateTable(true)
                diceMode = 1
            }
            ONLINE_GAME_OVER -> {
                gameOver()
            }

        }

        GlobalScope.launch(Dispatchers.Main) {

            isUpdating = true

            val deferredTimestamp = async { getTimestampFromServer() }
            val serverTimestamp = deferredTimestamp.await().toLong()
            deferredTimestamp.cancel()

            if (setMatchData(isNextPlayer, onlineAction)) {
                val deferredUpdateDb = async { updateMatchData(mMatchId, mMatchBaseData, mMatchUpdateData, serverTimestamp) }
                val isUpdated = deferredUpdateDb.await()
                deferredUpdateDb.cancel()

//            Log.d(TAG, "matchDataToDb(), isUpdated: $isUpdated")

                if (isUpdated) {
                    isUpdating = false
                    isUpdateError = false
                    if (onlineAction != ONLINE_ACTIVE_ENTRY)
                        isOnlineEntry = false
                    if (onlineAction == ONLINE_INIT && mMatchId != null) {
                        invitationAccepted(mOpponentId!!, mMatchId!!)
                        startMatchUpdateListener(mMatchId!!)
                    } else
                        onUpdateMatch(onlineAction)
                } else {
                    if (isUpdateError) {
                        initOnline()
                        setNoActiveMatch()
                        showErrorMessage(R.string.match_error_locally_modified)
                    } else {
                        isUpdateError = true

//                    Log.d(TAG, "matchDataToDb(), isUpdateError: $isUpdateError, isNextPlayer: $isNextPlayer, onlineAction: $onlineAction")

                        matchDataToDb(isNextPlayer, onlineAction)

                    }
                }
            }

        }

    }

    private fun onUpdateMatch(gameStatus: String) {

//        Log.d(TAG, "onUpdateMatch(), gameStatus: $gameStatus")

        setRunPrefs()

        when (gameStatus) {
            ONLINE_START -> {
                diceView(diceRoll, diceHold)
            }
            ONLINE_ACTIVE_ENTRY_STORNO -> {
                setBtnPlayer(ed.playerToMove)
            }
        }

        return

    }

    private fun setMatchData(isNextPlayer: Boolean, onlineAction: String) : Boolean {

        var isNext = isNextPlayer

//        Log.d(TAG, "setMatchData(), isNextPlayer: $isNextPlayer, onlineAction: $onlineAction, mMatchId: $mMatchId, mOpponentId: $mOpponentId")

        // --> mMatchBaseData (once only)
        if (mMatchId == null) {
            mMatchBaseData = MatchBaseData()
            mMatchBaseData!!.appVersionCodeA = mOpponentAppVersionCode
            mMatchBaseData!!.appVersionCodeB = BuildConfig.VERSION_CODE.toLong()
            mMatchBaseData!!.bonusServed = ed.bonusServed.toLong()
            mMatchBaseData!!.bonusServedGrande = ed.bonusServedGrande.toLong()
            mMatchBaseData!!.col = EscaleroData.COLS.toLong()
            mMatchBaseData!!.colMultiplier = ed.payoutMultiplier.toLong()
            mMatchBaseData!!.colPoints = "1 2 4 3"
            val cDate = Date()
            val date = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(cDate)
            mMatchBaseData!!.date = date
            mMatchBaseData!!.diceState = diceState.toLong()
            mMatchBaseData!!.nameA = mOpponentName
            mMatchBaseData!!.nameB = mPlayerName
            mMatchBaseData!!.player = ed.playerNumber.toLong()               // player number
            mMatchBaseData!!.playerIdA = mOpponentId
            mMatchBaseData!!.playerIdB = mPlayerId
            mMatchBaseData!!.silent = false

//            Log.d(TAG, "setMatchData(), ed.isSingleGame: ${ed.isSingleGame}, prefs.isSingleGame: ${prefs.getBoolean("isSingleGame", true)}")

            mMatchBaseData!!.singleGame = ed.isSingleGame
            mMatchBaseData!!.unit = "Points"

        }

        // --> mMatchUpdateData
        mMatchUpdateData = MatchUpdateData()
        mMatchUpdateData!!.diced = isDiced
        var roll = ""
        for (i in diceDouble1.indices) {
            if (diceDouble1[i] < 0)
                roll = "$roll-"
            else
                roll += diceDouble1[i]
        }
        mMatchUpdateData!!.diceDouble1 = roll
        var hold = ""
        for (i in diceHold.indices) {
            if (diceHold[i] < 0)
                hold = "$hold-"
            else
                hold += diceHold[i]
        }
        mMatchUpdateData!!.diceHold = hold
        hold = ""
        for (i in diceHoldPrev.indices) {
            if (diceHoldPrev[i] < 0)
                hold = "$hold-"
            else
                hold += diceHoldPrev[i]
        }
        mMatchUpdateData!!.diceHoldPrev = hold
        mMatchUpdateData!!.diceModus = diceMode.toLong()
        mMatchUpdateData!!.diceModusPrev = diceModePrev.toLong()
        roll = ""
        for (i in diceRoll.indices) {
            roll =
                    if (diceRoll[i] < 0)
                        "$roll-"
                    else
                        roll + diceRoll[i]
        }
        mMatchUpdateData!!.diceRoll = roll
        roll = ""
        for (i in diceRollPrev.indices) {
            if (diceRollPrev[i] < 0)
                roll = "$roll-"
            else
                roll += diceRollPrev[i]
        }
        mMatchUpdateData!!.diceRollPrev = roll
        mMatchUpdateData!!.double1 = isDouble1

//        Log.d(TAG, "setMatchData(), ed.selectedGridItem: ${ed.selectedGridItem}, gridPosition: $gridPosition")

        var selectedGrid = ed.selectedGridItem
        if (ed.selectedGridItem > 0 && gridPosition > 0) {
            selectedGrid = gridPosition
            if (!ed.isPlayerColumn) {
                val gridId = ed.gridCurrent[selectedGrid]
                for (i in EscaleroData.GRID_PLAY_PLAYER2.indices) {
                    if (EscaleroData.GRID_PLAY_PLAYER2[i] == gridId) {
                        selectedGrid = i
                        break
                    }
                }
            }
        }
        mMatchUpdateData!!.gridItem = selectedGrid.toLong()

        mMatchUpdateData!!.onlineAction = onlineAction
        mMatchUpdateData!!.starter = ed.playerStart.toString()
        mMatchUpdateData!!.playerToMove = ed.playerToMove.toString()
        mMatchUpdateData!!.position = position
        mMatchUpdateData!!.served = isServed
        mMatchUpdateData!!.servedDouble1 = isServedDouble1

        if (mMatchUpdateData!!.diceModus == 1L && mMatchUpdateData!!.diceModusPrev == 4L && mMatchUpdateData!!.diceRoll == "-----") {

            val nPlayer = ed.getNextPlayerAB(ed.playerStart)

//            Log.d(TAG, "setMatchData(), ed.playerStart: ${ed.playerStart}, nPlayer: $nPlayer, ed.playerToMove: ${ed.playerToMove}, isNext: $isNext")
//            Log.d(TAG, "setMatchData(), onlineAction: $onlineAction, mMatchId: $mMatchId, mPlayerId: $mPlayerId, mOpponentId: $mOpponentId")
//            Log.d(TAG, "setMatchData(), onlineAction: $onlineAction, playerIdA: ${mMatchBaseData!!.playerIdA}, playerIdB: ${mMatchBaseData!!.playerIdB}")

            if (nPlayer != ed.playerToMove) {
                mMatchUpdateData!!.playerToMove = nPlayer.toString()
                ed.playerToMove = nPlayer
                isNext = if (nPlayer == 'A')
                    mPlayerId != mMatchBaseData!!.playerIdA
                else
                    mPlayerId != mMatchBaseData!!.playerIdB

//                Log.d(TAG, "!!! setMatchData(), ERROR, isNext: $isNext")

            }
        }


        if (isNext) {
            mMatchUpdateData!!.turnPlayerId = mOpponentId
            isDoingTurn = false
        }
        else {
            mMatchUpdateData!!.turnPlayerId = mPlayerId
            isDoingTurn = true
        }

        var checkPlayerToMove = 'A'
        if (mMatchUpdateData!!.turnPlayerId == mMatchBaseData!!.playerIdB)
            checkPlayerToMove = 'B'
        if (ed.playerToMove != checkPlayerToMove) {

//            Log.d(TAG, "setMatchData(), playerToMove-ERROR, ed.playerToMove: ${ed.playerToMove}, checkPlayerToMove: $checkPlayerToMove")

            mMatchUpdateData!!.playerToMove = checkPlayerToMove.toString()
            ed.playerToMove = checkPlayerToMove
        }

        isUpdating = false
        isOnlineActive = true

        return true

    }

    private fun matchTurn(matchUpdateData: MatchUpdateData) {

//        Log.d(TAG, "!! matchTurn(), MY_TURN, mMatchId: $mMatchId, onlineAction: ${matchUpdateData.onlineAction}")

        if (matchUpdateData.onlineAction == ONLINE_ACTIVE)
            return

        if (matchUpdateData.onlineAction != ONLINE_GAME_OVER) {
            isOnlineActive = true
            isDoingTurn = true
            setPrefs(matchUpdateData)
            setDataUpdate()
            getRunPrefs()
            diceView(diceRoll, diceHold)
            setUI()

//            Log.d(TAG, "matchTurn(), != ONLINE_GAME_OVER --> : ${matchUpdateData.onlineAction} turnPlayerId: ${matchUpdateData.turnPlayerId}")

        }

        when (matchUpdateData.onlineAction ) {

            ONLINE_START -> {
                if (mMatchBaseData != null) {

                    val playerIdA = mMatchBaseData!!.playerIdA
                    val playerIdB = mMatchBaseData!!.playerIdB

//                    Log.d(TAG, "matchTurn(), appVersionCodeA: ${mMatchBaseData!!.appVersionCodeA}, appVersionCodeB: ${mMatchBaseData!!.appVersionCodeB}")
//                    Log.d(TAG, "matchTurn(), mMatchBaseData!!.singleGame: ${mMatchBaseData!!.singleGame}, ed.isSingleGame: ${ed.isSingleGame}")

                    if (!ed.isSingleGame) {
                        if (mMatchBaseData!!.appVersionCodeA!! < MIN_VERSION_CODE || mMatchBaseData!!.appVersionCodeB!! < MIN_VERSION_CODE) {
                            val info = "${mMatchBaseData!!.nameA} - ${mMatchBaseData!!.nameB}\n\n${getString(R.string.differentAppVersions)}"
                            showInfoDialog(getString(R.string.info), info, getString(R.string.ok))
                            matchCanceled(mOpponentId!!, mMatchId!!, "9")
                            startRemoveMatch(true, mMatchId!!, mMatchBaseData!!.playerIdA!!, mMatchBaseData!!.playerIdB!!)
                            return
                        }
                    }

                    GlobalScope.launch(Dispatchers.Main) {
                        if (playerIdA != null) {
                            val deferredGetUserMatches = async { getUserMatches(playerIdA) }
                            val matchListA = deferredGetUserMatches.await()
                            deferredGetUserMatches.cancel()

//                            Log.d(TAG, "matchTurn(), ONLINE_START, playerIdA: $playerIdA, countA: ${matchListA.size}")

                            for (i in matchListA.indices) {
                                val m = matchListA[i]
                                if (m.selected && m.matchId.isNotEmpty()) {
                                    val refPlayer = FirebaseDatabase.getInstance().getReference("userMatches")
                                    refPlayer.child(playerIdA).child(m.matchId).removeValue()
                                    if (m.matchId != "") {
                                        val refMatches = FirebaseDatabase.getInstance().getReference("matches/${m.matchId}")
                                        refMatches.removeValue()
                                    }
                                }
                            }

                        }
                        if (playerIdB != null) {
                            val deferredGetUserMatches = async { getUserMatches(playerIdB) }
                            val matchListB = deferredGetUserMatches.await()
                            deferredGetUserMatches.cancel()

//                            Log.d(TAG, "matchTurn(), ONLINE_START, playerIdB: $playerIdB, countB: ${matchListB.size}")

                            for (i in matchListB.indices) {
                                val m = matchListB[i]
                                if (m.selected && m.matchId.isNotEmpty()) {
                                    val refPlayer = FirebaseDatabase.getInstance().getReference("userMatches")
                                    refPlayer.child(playerIdB).child(m.matchId).removeValue()
                                    if (m.matchId != "") {
                                        val refMatches = FirebaseDatabase.getInstance().getReference("matches/${m.matchId}")
                                        refMatches.removeValue()
                                    }
                                }
                            }

                        }
                    }

                }
            }

            ONLINE_ACTIVE_TURN -> {
                if (setMatchData(false, ONLINE_ACTIVE_TURN))
                    return
            }

            ONLINE_REMATCH_TURN -> {
                //kotlin.KotlinNullPointerException
                if (mMatchBaseData != null) {
                    // kotlin.KotlinNullPointerException
                    if (mMatchBaseData!!.nameA != null && mMatchBaseData!!.nameB != null)
                        showAcceptRematchDialog(false, mMatchBaseData!!.nameA!!, mMatchBaseData!!.nameB!!)
                }
            }

            ONLINE_START_REMATCH -> {
                if (mMatchId != null && mMatchUpdateData != null)
                    startRematch()
            }

            ONLINE_GAME_OVER -> {
                gameOver()
                ed.accounting
                var result = ed.resultA.toLong()
                if (mPlayerABC == "B")
                    result = ed.resultB.toLong()

                var playerA = mPlayerName!!
                var playerB = mOpponentName ?: "???"
                var epA = mPlayerEp
                var epB = mOpponentEp
                if (mPlayerABC == "B") {
                    playerA = mOpponentName ?: "???"
                    playerB = mPlayerName!!
                    epA = mOpponentEp
                    epB = mPlayerEp
                }

//            Log.d(TAG, "matchTurn(), mPlayerName: $mPlayerName, ed.resultA: ${ed.resultA}, ed.resultB: ${ed.resultB}, result: $result")

                if (!dialogRematch.isShowing)
                    showRematchDialog(true, playerA, playerB, epA, epB, ed.resultA.toLong(), ed.resultB.toLong(), result)
            }

            ONLINE_ACTIVE_ENTRY, ONLINE_START_CONTINUE -> {
                if (matchUpdateData.diceModus!!.toInt() == 5) {
                    runOnUiThread {

//                        Log.d(TAG, "matchTurn(), MY_TURN, ONLINE_ACTIVE_ENTRY, set confirmEntry")

                        isOnlineEntry = true
                        btnPlayerRound.visibility = ImageView.INVISIBLE
                        btnDice.visibility = ImageView.VISIBLE
                        btnDice.setImageResource(R.drawable.button_ok)
                        btnPlayerResult.text = resources.getString(R.string.confirmEntry)

                    }
                    return
                }
                if (matchUpdateData.onlineAction == ONLINE_START_CONTINUE && mMatchBaseData != null) {
                    playSound(4, 0)
                    if (mPlayerId == mMatchBaseData!!.playerIdA!!)
                        setOpponentData(mMatchBaseData!!.playerIdB!!, "B")
                    else
                        setOpponentData(mMatchBaseData!!.playerIdA!!, "A")
                }
            }

            else -> {
                if (matchUpdateData.onlineAction!!.endsWith(PAUSED)) {
                    initOnline()
                    setNoActiveMatch()
                    if (matchUpdateData.onlineAction!!.endsWith(THEIR_TURN_PAUSED)) {
                        val msg = "${getString(R.string.matchPauseAndFinished)}\n${getString(R.string.continueMatch)}"
                        showInfoDialog(getString(R.string.matchBreak), msg, getString(R.string.ok))
                    }
                }
            }

        }

    }

    private fun matchUpdate(matchUpdateData: MatchUpdateData) {

//        Log.d(TAG, "!! matchUpdate(), THEIR_TURN, mMatchId: $mMatchId, onlineAction: ${matchUpdateData.onlineAction}")

        isDoingTurn = false
        initRunPrefs()
        setPrefs(matchUpdateData)

        if (matchUpdateData.onlineAction != ONLINE_GAME_OVER) {

            setDataUpdate()
            getRunPrefs()
            setUI()
            diceView(diceRoll, diceHold)

        }

        when (matchUpdateData.onlineAction ) {

            ONLINE_START_CONTINUE -> {
                isOnlineActive = true
            }

            ONLINE_REMATCH -> {
                if (isRematchAccepted) {
                    isRematchAccepted = false
                    matchDataToDb(true, ONLINE_START_REMATCH)
                } else {
                    //kotlin.KotlinNullPointerException
                    if (mPlayerName != null && mOpponentName != null) {
                        var playerA = mPlayerName!!
                        var playerB = mOpponentName!!
                        if (mPlayerABC == "B") {
                            playerA = mOpponentName!!
                            playerB = mPlayerName!!
                        }
                        showAcceptRematchDialog(true, playerA, playerB)
                    }
                }
            }

            ONLINE_START_REMATCH -> {
                ed.playerStart = runPrefs.getString("playerStart", "B")!![0]
                if (mOpponentABC!! == "A")
                    setOpponentData(mMatchBaseData!!.playerIdA!!, mOpponentABC!!)
                else
                    setOpponentData(mMatchBaseData!!.playerIdB!!, mOpponentABC!!)
            }

            ONLINE_GAME_OVER -> {
                gameOver()
                ed.accounting
                var result = ed.resultA.toLong()
                if (mPlayerABC == "B")
                    result = ed.resultB.toLong()
                var playerA = mPlayerName!!
                var playerB = mOpponentName ?: ""
                var epA = mPlayerEp
                var epB = mOpponentEp
                if (mPlayerABC == "B") {
                    playerA = mOpponentName ?: ""
                    playerB = mPlayerName!!
                    epA = mOpponentEp
                    epB = mPlayerEp
                }

//            Log.d(TAG, "matchUpdate(), mPlayerName: $mPlayerName, ed.resultA: ${ed.resultA}, ed.resultB: ${ed.resultB}, result: $result")

                if (!dialogRematch.isShowing)
                    showRematchDialog(false, playerA, playerB, epA, epB, ed.resultA.toLong(), ed.resultB.toLong(), result)
            }

            else -> {
                if (matchUpdateData.onlineAction!!.endsWith(PAUSED)) {
                    initOnline()
                    val msg = "${getString(R.string.matchPauseAndFinished)}\n${getString(R.string.continueMatch)}"
                    showInfoDialog(getString(R.string.matchBreak), msg, getString(R.string.ok))
                }
            }

        }
    }

    private fun setPrefs(matchUpdateData: MatchUpdateData) {

        var isRollOrHold = false

        val eDiced = matchUpdateData.diced!!
        var eDiceMode = matchUpdateData.diceModus!!.toInt()
        if (!eDiced && eDiceMode == 4)
            eDiceMode = 1

        val edi = runPrefs.edit()

        edi.putBoolean("isDiced", matchUpdateData.diced!!)
        edi.putInt("diceModus", eDiceMode)
        edi.putInt("diceModusPrev", matchUpdateData.diceModusPrev!!.toInt())
        edi.putBoolean("isDouble1", matchUpdateData.double1!!)
        edi.putBoolean("isServed", matchUpdateData.served!!)
        edi.putBoolean("isServedDouble1", matchUpdateData.servedDouble1!!)
        edi.putString("playerStart", matchUpdateData.starter)
        if (matchUpdateData.playerToMove != "")
            edi.putString("playerToMove", "" + matchUpdateData.playerToMove)

        var str = ""
        var tmp = matchUpdateData.diceRoll
        var y = tmp!!.length -1
        for (i in 0..y) {
            str =
                    if (tmp[i] == '-')
                        "$str-1 "
                    else {
                        isRollOrHold = true
                        str + tmp[i] + " "
                    }
        }
        edi.putString("diceRoll", str)

        str = ""
        tmp = matchUpdateData.diceHold
        y = tmp!!.length -1
        for (i in 0..y) {
            str =
                    if (tmp[i] == '-')
                        "$str-1 "
                    else {
                        isRollOrHold = true
                        str + tmp[i] + " "
                    }
        }
        edi.putString("diceHold", str)

        str = ""
        tmp = matchUpdateData.diceRollPrev
        y = tmp!!.length -1
        for (i in 0..y) {
            str =
                    if (tmp[i] == '-')
                        "$str-1 "
                    else
                        str + tmp[i] + " "
        }
        edi.putString("diceRollPrev", str)

        str = ""
        tmp = matchUpdateData.diceHoldPrev
        y = tmp!!.length -1
        for (i in 0..y) {
            str =
                    if (tmp[i] == '-')
                        "$str-1 "
                    else
                        str + tmp[i] + " "
        }
        edi.putString("diceHoldPrev", str)

        str = ""
        tmp = matchUpdateData.diceDouble1
        y = tmp!!.length -1
        for (i in 0..y) {
            str =
                    if (tmp[i] == '-')
                        "$str-1 "
                    else
                        str + tmp[i] + " "
        }

//        Log.d(TAG, "setPrefs(matchUpdateData: MatchUpdateData) , diceDouble1: $str")

        edi.putString("diceDouble1", str)

        edi.putInt("selectedGridItem", matchUpdateData.gridItem!!.toInt())
        if (!ed.isPlayerColumn && matchUpdateData.gridItem!! > 0) {
            val gridId = EscaleroData.GRID_PLAY_PLAYER2[matchUpdateData.gridItem!!.toInt()]
            for (i in EscaleroData.GRID_COL_PLAYER2.indices) {
                if (EscaleroData.GRID_COL_PLAYER2[i] == gridId) {
                    edi.putInt("selectedGridItem", i)
                    break
                }
            }
        }

        val position = matchUpdateData.position
        if (position == "") {
            if (!isRollOrHold) {
                edi.putInt("diceModus", 0)
                edi.putInt("diceModusPrev", 0)
                val ini = "-1 -1 -1 -1 -1 "
                edi.putString("diceRoll", ini)
                edi.putString("diceHold", ini)
                edi.putString("diceRollPrev", ini)
                edi.putString("diceHoldPrev", ini)
            }
        } else {
            val textStr = position!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (i in textStr.indices) {
                if (textStr[i].startsWith("setcol ")) {
                    val tmpStr = textStr[i].split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val id = tmpStr[1] + tmpStr[2]
                    val value = tmpStr[3].replace("-", "-1")
                    edi.putString(id, value)
                }
            }
        }

        edi.apply()

    }

    private fun initMatch(inviterUserId: String) {

//        Log.d(TAG, "initMatch(), inviterUserId: $inviterUserId")

        initOnline()

        GlobalScope.launch(Dispatchers.Main) {
            val deferredGetUserData = async { getUserData(inviterUserId) }
            val user = deferredGetUserData.await()
            deferredGetUserData.cancel()
            if (user != null) {
                mOpponentId = user.uid ?: ""

                val deferredLeaderboardScore = async { getLeaderboardScore(mOpponentId!!) }
                mOpponentEp = deferredLeaderboardScore.await()
                deferredLeaderboardScore.cancel()

                mOpponentName = user.name ?: ""
                mOpponentABC = "A"
                mOpponentAppVersionCode = user.appVersionCode
                mPlayerABC = "B"

                ed.playerStart = 'A'
                ed.playerToMove = 'A'
                ed.selectedGridItem = -1

                // update opponent single/double
                ed.isSingleGame = user.singleGame ?: true
                val edi = prefs.edit()
                edi.putBoolean("isSingleGame", ed.isSingleGame)
                edi.apply()
                // update player single/double
                updateUserStatus(playerId = mPlayerId, playing = true, singleGame = ed.isSingleGame)

                initDiceArrays()
                diceMode = 0// update opponent single/double
                diceModePrev = 0
                isDiced = false
                isServed = false
                isServedDouble1 = false
                isDouble1 = true
                isDouble1Selected = false

//                Log.i(TAG, "initMatch(), ed.isSingleGame: ${ed.isSingleGame}, mOpponentName: $mOpponentName, mOpponentId $mOpponentId")

                matchDataToDb(true, ONLINE_INIT)

            }
        }

    }

    private fun startRemoveMatch(deleteMatch: Boolean, matchId: String?, playerIdA: String?, playerIdB: String?) {

        if (matchId.isNullOrEmpty())
            return

        GlobalScope.launch(Dispatchers.Main) {
            val deferredCancelMatch = async { cancelMatch(deleteMatch, matchId, playerIdA, playerIdB) }
            val isRemoved = deferredCancelMatch.await()
            deferredCancelMatch.cancel()
            if (isRemoved || !isRemoved) {
                initOnline()
                setNoActiveMatch()

//                Log.i(TAG, "E initBoard(), setOnlineMessage()")

                setOnlineMessage()
            }
        }
    }

    private fun startRematch() {

//        Log.d(TAG, "startRematch(), mOpponentEp: $mOpponentEp")

        GlobalScope.launch(Dispatchers.Main) {
            if (mOpponentId != null) {

                playSound(4, 0)
                isOnlineActive = true
                isDoingTurn = true
                if (mPlayerABC == "A") {
                    mMatchUpdateData!!.starter = "A"
                    mMatchUpdateData!!.playerToMove = "A"
                    ed.playerStart = 'A'
                    ed.playerToMove = 'A'
                }
                else {
                    mMatchUpdateData!!.starter = "B"
                    mMatchUpdateData!!.playerToMove = "B"
                    ed.playerStart = 'B'
                    ed.playerToMove = 'B'
                }

                setPrefs(mMatchUpdateData!!)
                setDataUpdate()
                initRunPrefs()

                ed.selectedGridItem = -1

                initDiceArrays()
                diceMode = 0
                diceModePrev = 0
                isDiced = false
                isServed = false
                isServedDouble1 = false
                isDouble1 = true
                isDouble1Selected = false

                isOnlineActive = true
                isDoingTurn = true
                isUpdating = false
                isOnlineEntry = false

                setUI()
                diceView(diceRoll, diceHold)

                isInitBoard = false
                isInitDelay = false
                initBoard()
                setNoActiveMatch()

                matchDataToDb(false, ONLINE_START_REMATCH)

            }
        }
    }

    private fun setOpponentData(userId: String, userABC: String) {

//        Log.i(TAG, "setOpponentData(), userId: $userId, userABC: $userABC")

        GlobalScope.launch(Dispatchers.Main) {
            val deferredGetUserData = async { getUserData(userId) }
            val user = deferredGetUserData.await()
            deferredGetUserData.cancel()
            if (user != null) {
                mOpponentId = user.uid ?: ""

                val deferredLeaderboardScore = async { getLeaderboardScore(mOpponentId!!) }
                mOpponentEp = deferredLeaderboardScore.await()
                deferredLeaderboardScore.cancel()

//                Log.i(TAG, "setOpponentData(), mOpponentId: $mOpponentId, mOpponentEp: $mOpponentEp")

                mOpponentName = user.name ?: ""
                mOpponentAppVersionCode = user.appVersionCode
                when (userABC) {
                    "A" -> {
                        mOpponentABC = "A"
                        mPlayerABC = "B"
                    }
                    "B" -> {
                        mOpponentABC = "B"
                        mPlayerABC = "A"
                    }
                }

//                Log.i(TAG, "setOpponentData(), mOpponentName: $mOpponentName, mOpponentId: $mOpponentId")

            }
        }
    }

    private fun getPausedStatus(): String {
        var status = ONLINE_ACTIVE
        if (diceMode == 5 && isOnlineEntry)
            status = ONLINE_ACTIVE_ENTRY
        if (diceMode == 1 && diceModePrev == 4)
            status = ONLINE_ACTIVE_TURN
        return status + PAUSED
    }

    //ONLINE - methods, dialogs

    private fun startDialogCurrentMatch(matchID: String) {

        val matchPlayers = dialogCurrentMatch.findViewById<TextView>(R.id.matchPlayers)
        val matchPlayerEps = dialogCurrentMatch.findViewById<TextView>(R.id.matchPlayerEps)
        val matchDate = dialogCurrentMatch.findViewById<TextView>(R.id.matchDate)
        val matchTurn = dialogCurrentMatch.findViewById<TextView>(R.id.matchTurn)
        val matchAccounting = dialogCurrentMatch.findViewById<TextView>(R.id.matchAccounting)
        val currentMatchDelete = dialogCurrentMatch.findViewById<TextView>(R.id.currentMatchDelete)
        val matchPause = dialogCurrentMatch.findViewById<TextView>(R.id.matchPause)
        val matchUpdate = dialogCurrentMatch.findViewById<TextView>(R.id.matchUpdate)

        if (!checkConnectivity(false) || mMatchBaseData == null || mMatchUpdateData == null)
            return

        if (mMatchId != null) {
            GlobalScope.launch(Dispatchers.Main) {
                val deferredGetMatchTimestamp = async { getMatchTimestamp(mMatchId!!) }
                val timestampStr = deferredGetMatchTimestamp.await()
                deferredGetMatchTimestamp.cancel()
                val deferredTimestamp = async { getTimestampFromServer() }
                val serverTimestampStr = deferredTimestamp.await()
                deferredTimestamp.cancel()

                if (timestampStr.isEmpty() || serverTimestampStr.isEmpty())
                    return@launch

                //java.lang.NumberFormatException
                val timestamp = timestampStr.toLong()
                val serverTimestamp = serverTimestampStr.toLong()
                val diffTime = timestamp - mTimestampPre

//                Log.d(TAG, "startDialogCurrentMatch(), timestamp-A: $timestamp, timestamp-P: $mTimestampPre, diffTime: $diffTime")
//                Log.d(TAG, "startDialogCurrentMatch(), mMatchUpdateData!!.turnPlayerId : ${mMatchUpdateData!!.turnPlayerId }")

                if (serverTimestamp - timestamp >= MATCH_TIMEOUT) {
                    if (isOnlineActive) {
                        if (isDoingTurn) {
                            matchDataToDb(false, getPausedStatus())
                            diceBoard.mOnlinePlayers = ""
                            diceBoard.updateBoard(diceRoll, diceHold, diceDouble1, false, 0, initRollValues)
                        } else {
                            var status = getPausedStatus()
                            status = status.replace(PAUSED, "_$THEIR_TURN_PAUSED")
                            matchDataToDb(true, status)
                            diceBoard.mOnlinePlayers = ""
                            diceBoard.updateBoard(diceRoll, diceHold, diceDouble1, false, 0, initRollValues)
                        }
                    }
                    val msg = "${getString(R.string.matchEndedAuto, MATCH_TIMEOUT / 60000)}\n${getString(R.string.continueMatch)}"
                    showInfoDialog(getString(R.string.matchBreak), msg, getString(R.string.ok))
                }
                else {
                    if (diffTime >= MIN_UPDATE_TIME) {
                        // data synchronization
                        if (mMatchId != null) {
                            if (mMatchBaseData == null) {
                                val deferredGetMatchBaseData = async { getMatchBaseData(mMatchId!!) }
                                mMatchBaseData = deferredGetMatchBaseData.await()
                                deferredGetMatchBaseData.cancel()
                            }
                            val deferredGetMatchUpdateData = async { getMatchUpdateData(mMatchId!!) }
                            val checkUpdateData = deferredGetMatchUpdateData.await()
                            deferredGetMatchUpdateData.cancel()
                            if (checkUpdateData != null) {
                                mTimestampPre = timestamp
                                Toast.makeText(applicationContext, getString(R.string.dataUpdate), Toast.LENGTH_LONG).show()
                                playSound(1, 0)
                                if (checkUpdateData.turnPlayerId == mPlayerId) {
                                    matchTurn(checkUpdateData)
                                } else {
                                    matchUpdate(checkUpdateData)
                                }
                            }
                        }
                    }
                }
            }
        }

        matchPlayers.text = getPlayersFromMatch(matchID)
        matchPlayerEps.text = getPlayersEp()
        matchDate.text = mMatchBaseData!!.date
        if (mMatchUpdateData!!.turnPlayerId == mPlayerId)
            matchTurn.text = getString(R.string.yourTurn)
        else
            matchTurn.text = getString(R.string.theirTurn, mOpponentName)
        matchAccounting.setOnClickListener {
            showAccountingDialog(ed.accounting)
            dialogCurrentMatch.dismiss()
        }
        currentMatchDelete.setOnClickListener {
            dialogCurrentMatch.dismiss()
            showDeleteMatchDialog()
        }
        matchPause.setOnClickListener {
            if (checkConnectivity(false)) {
                if (isDoingTurn) {
                        matchDataToDb(false, getPausedStatus())
                        Toast.makeText(applicationContext, getString(R.string.matchBreak), Toast.LENGTH_SHORT).show()
                        diceBoard.mOnlinePlayers = ""
                        diceBoard.updateBoard(diceRoll, diceHold, diceDouble1, false, 1, initRollValues)
                } else
                    showPauseMatchDialog()
            }
            dialogCurrentMatch.dismiss()
        }
        matchUpdate.setOnClickListener {
            if (checkConnectivity(false)) {
                checkMatches()
            }
            else {
                if (mMatchId != null)
                    startMatchUpdateListener(mMatchId!!)
            }
            dialogCurrentMatch.dismiss()
        }

        dialogCurrentMatch.show()

    }

    private fun getPlayersFromMatch(matchId: String?): String {
        return  if (matchId != null && mMatchBaseData != null)
            "${mMatchBaseData!!.nameA} - ${mMatchBaseData!!.nameB}"
        else
            ""
    }

    private fun getPlayersEp(): String {

//        Log.d(TAG, "getPlayersEp(), mPlayerEp: $mPlayerEp, mOpponentEp: $mOpponentEp")

        return if (mOpponentId != null && mPlayerEp >= 0 && mOpponentEp >= 0) {
            return  if (mPlayerABC == "A")
                "$mPlayerEp    EP    $mOpponentEp"
            else
                "$mOpponentEp    EP    $mPlayerEp"
        }
        else
            ""

    }

    private fun isUpdatingWarning() {
        Toast.makeText(applicationContext, getString(R.string.dataUpdate), Toast.LENGTH_SHORT).show()
        playSound(1, 0)
    }

    @NonNull
    private fun getStringByLocal(context: Activity, id: Int, arg1: String?, arg2: String?, locale: String): String {

//        Log.d(TAG, "getStringByLocal(), resource.id: $id, locale: $locale")

        return  if (locale == "" || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    if (arg1 != "" && arg2 == null)
                        context.getString(id, arg1)
                    else {
                        if (arg1 != "" && arg2 != null)
                            context.getString(id, arg1, arg2)
                        else
                            context.getString(id)
                    }
                }
                    else {
                        val config = context.resources.configuration
                        config.setLocale(Locale(locale))
                        applicationContext.resources.configuration.setTo(config)
                        if (arg1 != "" && arg2 == null)
                            context.createConfigurationContext(config).resources.getString(id, arg1)
                        else {
                            if (arg1 != "" && arg2 != null)
                                context.createConfigurationContext(config).resources.getString(id, arg1, arg2)
                            else
                                context.createConfigurationContext(config).resources.getString(id)
                        }
                }
    }

    private fun showErrorMessage(stringId: Int) {
        showInfoDialog(getString(R.string.warningTitle), resources.getString(stringId), getString(R.string.ok))
    }

    // ENGINE
    private fun initEngines(isNewGame: Boolean) {

//        Log.i(TAG, "initEngines(), isNewGame: $isNewGame")

        engineEntryCommand = ""
        engine = Engine()
        val edi = runPrefs.edit()
        edi.putString("engineNameA", getEngineName(true))
        edi.putString("engineNameB", getEngineName(true))
        edi.putString("engineNameC", getEngineName(true))
        edi.apply()

        val udi = "udi"
        val logging = "logging " + prefs.getBoolean("logging", false) + ""
        val isReady = "isready"
        val newGame = "newgame"
        val optionPlayer = "setoption name players value " + ed.playerNumber
        val optionCol = "setoption name col value " + EscaleroData.COLS
        val optionColPoints = "setoption name colpoints value " + ed.pointsColumn1 + "," + ed.pointsColumn2 + "," + ed.pointsColumn3
        val optionColBonus = "setoption name colbonus value " + ed.pointsBonus
        val bonusServed = "setoption name bonusserved value " + ed.bonusServed
        val bonusServedGrande = "setoption name bonusservedgrande value " + ed.bonusServedGrande
        var isDouble = true
        if (ed.isSingleGame)
            isDouble = false
        val optionDouble = "setoption name double value $isDouble"

        if (engine != null) {
            if (engine!!.getResultFromEngine(udi) == "") return
            if (engine!!.getResultFromEngine(logging) == "") return
            if (engine!!.getResultFromEngine(optionPlayer) == "") return
            if (engine!!.getResultFromEngine(optionCol) == "") return
            if (engine!!.getResultFromEngine(optionColPoints) == "") return
            if (engine!!.getResultFromEngine(optionColBonus) == "") return
            if (engine!!.getResultFromEngine(bonusServed) == "") return
            if (engine!!.getResultFromEngine(bonusServedGrande) == "") return
            if (engine!!.getResultFromEngine(optionDouble) == "") return
            if (engine!!.getResultFromEngine(newGame) == "") return
            if (!isNewGame) {
                if (engine!!.getResultFromEngine(ed.getPlayerEngineValues('A', 0)) != OK) return
                if (engine!!.getResultFromEngine(ed.getPlayerEngineValues('A', 1)) != OK) return
                if (engine!!.getResultFromEngine(ed.getPlayerEngineValues('A', 2)) != OK) return
                if (engine!!.getResultFromEngine(ed.getPlayerEngineValues('B', 0)) != OK) return
                if (engine!!.getResultFromEngine(ed.getPlayerEngineValues('B', 1)) != OK) return
                if (engine!!.getResultFromEngine(ed.getPlayerEngineValues('B', 2)) != OK) return
                if (engine!!.getResultFromEngine(ed.getPlayerEngineValues('C', 0)) != OK) return
                if (engine!!.getResultFromEngine(ed.getPlayerEngineValues('C', 1)) != OK) return
                if (engine!!.getResultFromEngine(ed.getPlayerEngineValues('C', 2)) != OK) return
            }
            if (engine!!.getResultFromEngine(isReady) != Engine.READYOK) return
        }

    }

    private fun getEngineDiceCommand(player: Char, lastTry: Int, diceRoll: IntArray, diceHold: IntArray, diceDouble1: IntArray?, doubleServed: Boolean): String {
        var command = "dice $player $lastTry r:"
        var roll = ""
        for (i in diceRoll.indices) {
            roll =
                if (diceRoll[i] >= 0)
                    roll + diceRoll[i]
                else
                    "$roll-"
        }
        command += roll
        command = "$command h:"
        var hold = ""
        for (i in diceHold.indices) {
            hold =
                if (diceHold[i] >= 0)
                    hold + diceHold[i]
                else
                    "$hold-"
        }
        command += hold
        var isDouble = false
        if (diceDouble1 != null) {
            if (diceDouble1[0] >= 0)
                isDouble = true
        }
        if (isDouble) {
            command = "$command h1:"
            var double1 = ""
            for (i in diceDouble1!!.indices) {
                double1 =
                    if (diceDouble1[i] >= 0)
                        double1 + diceDouble1[i]
                    else
                        "$double1-"
            }
            command += double1
            command = "$command $doubleServed"
        }
        return command
    }

    private fun getEngineName(isEnginePlayer: Boolean): String {
        return if (isEnginePlayer)
            engine!!.NAME
        else
            ""
    }

    fun isEnginePlayer(playerToMove: Char): Boolean {
        var enginePlayer = false
        if ((diceState == 1) && prefs.getBoolean("enginePlayer", true) && !prefs.getBoolean("playOnline", false)) {
            if (prefs.getBoolean("enginePlayerA", true) and (playerToMove == 'A'))
                enginePlayer = true
            if (prefs.getBoolean("enginePlayerB", false) and (playerToMove == 'B'))
                enginePlayer = true
            if (ed.playerNumber == 3) {
                if (prefs.getBoolean("enginePlayerC", false) and (playerToMove == 'C'))
                    enginePlayer = true
            }
        }
        return enginePlayer
    }

    private fun allEnginePlayer(): Boolean {
        var enginePlayer = false
        if ((diceState == 1) and prefs.getBoolean("enginePlayer", true)) {
            enginePlayer = true
            if (!prefs.getBoolean("enginePlayerA", true))
                enginePlayer = false
            if (!prefs.getBoolean("enginePlayerB", false))
                enginePlayer = false
            if (ed.playerNumber == 3) {
                if (!prefs.getBoolean("enginePlayerC", false))
                    enginePlayer = false
            }
        }
        return enginePlayer
    }

    fun setRollHoldFromEngineCommands(command: String) {
        val strResult = command.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val diceTry = Integer.parseInt(strResult[2])
        diceMode = diceTry + 1
        val roll = strResult[3].replace("r:", "")
        val hold = strResult[4].replace("h:", "")
        for (j in diceRoll.indices) {
            var r = -1
            if (Character.isDigit(roll[j]))
                r = Character.getNumericValue(roll[j])
            diceRoll[j] = r
            var h = -1
            if (Character.isDigit(hold[j]))
                h = Character.getNumericValue(hold[j])
            diceHold[j] = h
        }
    }

    private fun checkRollHoldFromEngineCommands(command: String): Boolean {
        var checkOK = true
        val strResult = command.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var oldRH = ""
        var newRH = ""
        if (command.contains("r:") and command.contains("h:")) {
            val roll = strResult[3].replace("r:", "")
            val hold = strResult[4].replace("h:", "")
            for (j in diceRoll.indices) {
                var r = -1
                if (Character.isDigit(roll[j]))
                    r = Character.getNumericValue(roll[j])
                var h = -1
                if (Character.isDigit(hold[j]))
                    h = Character.getNumericValue(hold[j])
                if (r >= 0) newRH += r
                if (h >= 0) newRH += h
            }
        }
        if (!command.contains("r:") and command.contains("h1:"))
            newRH = strResult[2].replace("h1:", "")
        for (j in diceRoll.indices) {
            if (diceRoll[j] >= 0) oldRH += diceRoll[j]
            if (diceHold[j] >= 0) oldRH += diceHold[j]
        }
        if (oldRH != newRH)
            checkOK = false

        //        Log.i(TAG, "oldRH: " + oldRH + ", newRH: " + newRH + ", checkOK: " + checkOK)

        return checkOK
    }

    private fun performEngineCommands(command: String) {

        if (prefs.getBoolean("playOnline", false))
            return

        engineCommand =
            if (engineEntryCommand != "")
                engineEntryCommand + command
            else
                command

        //Log.i(TAG, "performEngineCommands(), command: " + command)

        engineEntryCommand = ""
        handlerEngine.removeCallbacks(mUpdateEngine)
        if (isDiceBoard and command.startsWith("dice "))
            handlerEngine.postDelayed(mUpdateEngine, 1000)
        else
            handlerEngine.postDelayed(mUpdateEngine, 500)
        if (command.startsWith("dice "))
            engineIsRunning = true
    }

    fun performEngineEntryCommand(command: String) {
        if (engine!!.getResultFromEngine(command) != OK) {

            Log.i(TAG, "$ENGINE_ENTRY_ERROR$command performEngineEntryCommand()")

        }
    }

    companion object {

        const val TAG = "MainActivity"
        const val PREFERENCES_REQUEST_CODE = 10
        const val NEW_GAME_REQUEST_CODE = 11
        const val INFO_REQUEST_CODE = 30
        const val PERMISSIONS_REQUEST_CODE = 50

        const val BROADCAST_PUSH_NOTIFICATION = "push-notification"

        const val URI_ESCALERO = "https://play.google.com/store/apps/details?id=com.androidheads.vienna.escalero"
        const val URI_HOMEPAGE_EN = "https://sites.google.com/view/escalero/en"
        const val URI_HOMEPAGE_DE = "https://sites.google.com/view/escalero/de"
        const val URI_MANUAL_EN = "https://docs.google.com/document/d/1IjHDcmL4sLJ67TkGWukL4algus3Ne556rlGeoGu8QQg/edit?usp=sharing"
        const val URI_MANUAL_DE = "https://docs.google.com/document/d/1B1dXbe4sMyiO6_Casum7wCx7ilDlif-0Oj-JVMOZD6U/edit?usp=sharing"
        const val URI_WHATS_NEW_EN = "https://docs.google.com/document/d/1IxVPCN_qaMlWj9-KOPV3R79iDUAcvnE9o-mLfXf_8B0/edit?usp=sharing"
        const val URI_WHATS_NEW_DE = "https://docs.google.com/document/d/1bawYWbVZnmJ-ssitL5INGIlUkxZGPcAdM1UWTdbOLqE/edit?usp=sharing"
        const val URI_PRIVACY_POLICY_EN = "https://docs.google.com/document/d/1wzVYEMVVOsjYflgAnxO6mjijQam5LxI5zdraV48VpD0/edit?usp=sharing"
        const val URI_PRIVACY_POLICY_DE = "https://docs.google.com/document/d/1qoOXCUHZHJmnQEh09HHyPwD6z0kLFuoB6NrAmleDHxg/edit?usp=sharing"
        val APP_EMAIL: CharSequence = "c4akarl@gmail.com"

        const val ENGINE_DICE_ERROR = "ENGINE DICE ERROR: "
        const val ENGINE_ENTRY_ERROR = "ENGINE ENTRY ERROR: "

        const val DELAY_TIME = 100
        const val DELAY_TIME_DICE_BOARD = 400
        const val DELAY_TIME_ADS_EP = 3000     // 10 sec
        const val ANIMATE_TIME = 400
        const val FIRST_ANIMATE_TIME = 1000

        const val OK = "ok"

        //ONLINE - constants
        const val ONLINE_INIT = "ONLINE_INIT"
        const val ONLINE_START = "ONLINE_START"
        const val ONLINE_ACTIVE = "ONLINE_ACTIVE"
        const val ONLINE_ACTIVE_ENTRY = "ONLINE_ACTIVE_ENTRY"
        const val ONLINE_ACTIVE_ENTRY_STORNO = "ONLINE_ACTIVE_ENTRY_STORNO"
        const val ONLINE_ACTIVE_TURN = "ONLINE_ACTIVE_TURN"
        const val ONLINE_GAME_OVER = "ONLINE_GAME_OVER"
        const val ONLINE_GAME_CONTINUE = "ONLINE_GAME_CONTINUE"
        const val ONLINE_START_CONTINUE = "ONLINE_START_CONTINUE"
        const val ONLINE_REMATCH = "ONLINE_REMATCH"
        const val ONLINE_REMATCH_TURN = "ONLINE_REMATCH_TURN"
        const val ONLINE_START_REMATCH = "ONLINE_START_REMATCH"
        const val PAUSED = "_PAUSED"
        const val THEIR_TURN_PAUSED = "THEIR_TURN_PAUSED"
        const val CORRECTION = "_CORRECTION"

        const val ONLINE_LEADERBORD_MAX = 500L
        const val ONLINE_LEADERBORD_MAX_PLAYERS = 100L
        const val ONLINE_LEADERBORD_BONUS_1 = 15L
        const val ONLINE_MAX_USER_MATCHES = 50

        const val NOTIFICATION_DELAY = 15000L
        const val ADS_DELAY = 600000L           // 10   min
        const val QUICK_MATCH_DELAY = 180000L   // 3    min
        const val RECONNECT_TIME = 180000L      // 3    min
        const val MIN_UPDATE_TIME = 400L        // mil sec
        const val MATCH_TIMEOUT = 300000L       // 5    min
        const val MIN_BTN_DELAY = 300L

        const val ADS_EP_MIN = 3L
        const val ADS_EP_MAX = 5L
        const val ADS_EP_1 = 1L

        const val RC_SIGN_IN = 10001
        const val MIN_VERSION_CODE = 55

    }

}