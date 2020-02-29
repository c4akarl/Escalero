package com.androidheads.vienna.engine;

import android.util.Log;

public class Engine
{
    public Engine()
    {
        dm = new DM();
    }
    public String getResultFromEngine(String action)
    {

//        Log.i(TAG, "getResultFromEngine(), action: " + action);

        String result = ERROR;
        if (action.startsWith(LOGGING))
        {
            isLogging = action.endsWith("true");
            dm.isLogging = isLogging;
            result =  OK;
        }
        if (isLogging)
            Log.i(TAG, ">" + action);
        if (action.equals(PROTOCOL))
        {
            result = getUdi();
        }
        if (action.equals(ISREADY) & isReady)
            result = READYOK;
        if (action.equals(NEWGAME))
        {
            dm.newGame();
            result =  READYOK;
        }
        if (action.startsWith(SETOPTION))
            result =  setOption(action);

        if (action.startsWith(SETCOL))
        {
            if (dm.player == null)
                dm.newGame();
            result = setCol(action);
        }
        if (action.startsWith(ENTRY))
            result =  setEntry(action);
        if (action.startsWith(DICE))
            result =  getDiceAction(action);

        if (isLogging)
            Log.i(TAG, result);

//Log.i(tag, "getResultFromEngine(), action: " + action + ", result: " + result);
        return result;

    }
    private String getDiceAction(String action)
    {
        char playerId;
        int diceTry;
        String roll;
        String hold;
        String holdDouble = "";
        dm.isTesting = false;
        if (action.endsWith("TEST"))
        {
            dm.isTesting = true;
            action = action.replace("TEST", "");
        }
        String[] strSp = action.split(" ");
        if (!(strSp.length == 5 | strSp.length == 7))
            return ERROR + "length: " + strSp.length;
        playerId = strSp[1].charAt(0);
        if (!(playerId == 'A' | playerId == 'B' | playerId == 'C'))
            return ERROR + "playerId: " + playerId;
        diceTry = Integer.parseInt(strSp[2]);
        if (!strSp[3].startsWith("r:"))
            return ERROR + "roll: " + strSp[3];
        else
            roll = strSp[3].replace("r:", "");
        if (!strSp[4].startsWith("h:"))
            return ERROR + "hold: strSp[4]";
        else
            hold = strSp[4].replace("h:", "");
        boolean isDoubleServed = false;
        if (dm.isDouble & strSp.length == 7)
        {
            if (!strSp[5].startsWith("h1:"))
                return ERROR + "hold1: " + strSp[5];
            else
                holdDouble = strSp[5].replace("h1:", "");
            if (strSp[6].startsWith("t"))
                isDoubleServed = true;
        }
// 20180818, java.lang.NullPointerException: com.androidheads.vienna.engine.Engine.getDiceAction (Engine.java:93)
        if (dm.player == null)
            return ERROR + " dm.player";
        if (diceTry == 1 | diceTry == 2 | diceTry == 3)
            dm.player.computePlayerPlan(playerId);
        if (!dm.computeRound(playerId, diceTry, roll, hold, holdDouble, isDoubleServed))
            return ERROR + " round values";

        return dm.computeDiceAction(playerId, diceTry);

    }
    private String setEntry(String action)
    {
        char player;
        int colP;
        int rowP;
        int entryP;
        String[] strSp = action.split(" ");
        if (strSp.length != 5)
            return ERROR + " length";
        player = strSp[1].charAt(0);
        if (!(player == 'A' | player == 'B' | player == 'C'))
            return ERROR + " playerId: " + player;
        colP = Integer.parseInt(strSp[2]);
        if (colP < 0 | colP >= dm.col)
            return ERROR + " col";
        rowP = Integer.parseInt(strSp[3]);
        if (rowP < 0 | rowP >= DM.row)
            return ERROR + " row";
        entryP = Integer.parseInt(strSp[4]);
        if (entryP < -1 | entryP >= 100)
            return ERROR + " result";
//Log.i(tag, "setEntry(), dm: " + dm + ", dm.player: " + dm.player);
// 20180813, java.lang.NullPointerException: at com.androidheads.vienna.engine.Engine.setEntry (Engine.java:122)
        if (dm.player == null)
            return ERROR + " dm.player";
        dm.player.setEntry(player, colP, rowP, entryP);

        return OK;
    }
    private String getUdi()
    {
        String rv = "id name " + NAME + "\n";
        rv = rv + "id author " + AUTHOR + "\n";
        rv = rv + "option name playerNumber type spin default " + PLAYERS_DEFAULT + " min " + PLAYERS_MIN + " max " + PLAYERS_MAX + "\n";
        rv = rv + "option name col type spin default " + COL_DEFAULT + " min " + COL_MIN + " max " + COL_MAX + "\n";
        rv = rv + "option name colpoints type string default " + COL_POINTS_DEFAULT + "\n";
        rv = rv + "option name colbonus spin default " + COL_BONUS_DEFAULT + " min " + COL_BONUS_MIN + " max " + COL_BONUS_MAX + "\n";
        rv = rv + "option name bonusserved spin default " + BONUS_SERVED_DEFAULT + " min " + BONUS_SERVED_MIN + " max " + BONUS_SERVED_MAX + "\n";
        rv = rv + "option name bonusservedgrande spin default " + BONUS_SERVED_GRANDE_DEFAULT + " min " + BONUS_SERVED_GRANDE_MIN + " max " + BONUS_SERVED_GRANDE_MAX + "\n";
        rv = rv + "option name double type check default " + DOUBLE_DEFAULT + "\n";
        rv = rv + "udiok";
        isReady = true;

        return rv;
    }
    private String setOption(String action)
    {

//        Log.i(TAG, "setOption(), action: " + action);

        String returnValue = ERROR + action;
        String[] strSp = action.split(" ");
        if (strSp.length != 5)
            return ERROR + " length: " + strSp.length;
        if (strSp[2].equals("players"))
        {
            dm.playerNumber = Integer.parseInt(strSp[4]);
            if (dm.playerNumber < PLAYERS_MIN | dm.playerNumber > PLAYERS_MAX) dm.playerNumber = PLAYERS_DEFAULT;
            returnValue = "option name players value " + dm.playerNumber;
        }
        if (strSp[2].equals("col"))
        {
            dm.col = Integer.parseInt(strSp[4]);
            dm.maxRoundes = dm.col * DM.row;
            if (dm.col < COL_MIN | dm.col > COL_MAX) dm.col = COL_DEFAULT;
            returnValue = "option name col value " + dm.col;
        }
        if (strSp[2].equals("colpoints"))
        {
            dm.colPoints = getColPoints(strSp[4]);
            returnValue = "option name colpoints value " + dm.colPoints;
            dm.setColValues(dm.colPoints);
            dm.setColRanking(dm.colPoints);
        }
        if (strSp[2].equals("colbonus"))
        {
            dm.colBonus = Integer.parseInt(strSp[4]);
            if (dm.colBonus < COL_BONUS_MIN | dm.colBonus > COL_BONUS_MAX) dm.colBonus = COL_BONUS_DEFAULT;
            returnValue = "option name colbonus value " + dm.colBonus;
        }
        if (strSp[2].equals("bonusserved"))
        {
            dm.bonusServed = Integer.parseInt(strSp[4]);
            if (dm.bonusServed < BONUS_SERVED_MIN | dm.bonusServed > BONUS_SERVED_MAX) dm.bonusServed = BONUS_SERVED_DEFAULT;
            returnValue = "option name bonusServed value " + dm.bonusServed;
        }
        if (strSp[2].equals("bonusservedgrande"))
        {
            dm.bonusServedGrande = Integer.parseInt(strSp[4]);
            if (dm.bonusServedGrande < BONUS_SERVED_GRANDE_MIN | dm.bonusServedGrande > BONUS_SERVED_GRANDE_MAX) dm.bonusServedGrande = BONUS_SERVED_DEFAULT;
            returnValue = "option name bonusServedGrande value " + dm.bonusServedGrande;
        }
        if (strSp[2].equals("double"))
        {
            dm.isDouble = Boolean.valueOf(strSp[4]);
            returnValue = "option name double value " + dm.isDouble;
        }

        return returnValue;
    }
    private String setCol(String action)
    {
//Log.i(tag, "1 setCol(), action: " + action + "\ndm.player: " + dm.player);
        String returnValue = OK;
        char player;
        int colP;
        String[] strSp = action.split(" ");
        if (strSp.length != 4)
            return ERROR + " length";
        player = strSp[1].charAt(0);
        if (!(player == 'A' | player == 'B' | player == 'C'))
            return ERROR + " playerId: " + player;
        colP = Integer.parseInt(strSp[2]);
        if (colP < 0 | colP >= dm.col)
            return ERROR + " col";

        String[] entrySp = strSp[3].split(",");
        if (entrySp.length != 10)
            return ERROR + " result entries not 10";
        for (int j = 0; j < DM.row; ++j)
        {
            int val = -1;
            if (!entrySp[j].equals("-"))
                val = Integer.parseInt(entrySp[j]);
            dm.player.setEntry(player, colP, j, val);
        }
//Log.i(tag, "2 setCol(), action: " + action + "\ndm.player: " + dm.player);
        return returnValue;
    }
    private String getColPoints(String colValues)
    {
        String[] strSp = colValues.split(",");
        if (strSp.length != dm.col)
        {
            dm.col = COL_DEFAULT;
            colValues = COL_POINTS_DEFAULT;
        }
        return colValues;
    }

    private static final String TAG = "Engine";
    private boolean isReady = false;
    private boolean isLogging = false;
    public DM dm;

    // identification
    private static final String PROTOCOL = "udi";
    private static final String LOGGING = "logging";
    public final String NAME = "DiceEngine 1.6";
    private static final String AUTHOR = "c4akarl";
    // commands
    public static final String ERROR = "command error: ";
    public static final String OK = "ok";
    public static final String SETOPTION = "setoption name ";
    public static final String SETCOL = "setcol ";
    public static final String ISREADY = "isready";
    public static final String READYOK = "readyok";
    public static final String NEWGAME = "newgame";
    public static final String ENTRY = "entry ";
    public static final String DICE = "dice";
    // options (command values)
    private static final int PLAYERS_MIN = 2;
    private static final int PLAYERS_MAX = 3;
    private static final int PLAYERS_DEFAULT = 2;
    private static final int COL_MIN = 1;
    private static final int COL_MAX = 7;
    private static final int COL_DEFAULT = 3;
    private static final String COL_POINTS_DEFAULT = "1,2,4";
    private static final int COL_BONUS_DEFAULT = 3;
    private static final int COL_BONUS_MIN = 0;
    private static final int COL_BONUS_MAX = 9;
    private static final int BONUS_SERVED_DEFAULT = 5;
    private static final int BONUS_SERVED_MIN = 0;
    private static final int BONUS_SERVED_MAX = 50;
    private static final int BONUS_SERVED_GRANDE_DEFAULT = 30;
    private static final int BONUS_SERVED_GRANDE_MIN = 0;
    private static final int BONUS_SERVED_GRANDE_MAX = 90;
    private static final boolean DOUBLE_DEFAULT = false;
}
