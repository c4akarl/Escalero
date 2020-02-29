package com.androidheads.vienna.engine;

import android.util.Log;

import java.util.Random;

class DM
{
    // DataManager (DM)
    void newGame()
    {
        setColWorstBest();
        player = new Player(this);
    }
    boolean computeRound(char playerId, int diceTry, String roll, String hold, String dbl, boolean isDoubleServed)
    {
//Log.i(tag, "computeRound(), playerId: " + playerId + ", diceTry: " + diceTry
//+ ", roll: " + roll + ", hold: " + hold + ", dbl: " + dbl + ", isDoubleServed: " + isDoubleServed);

        if (roll.length() != 5 | hold.length() != 5)
            return false;
        round = new Round(this, playerId, diceTry);
        for (int i = 0; i < round.diceRoll.length; i++)
        {
            int r = -1;
            if (Character.isDigit(roll.charAt(i)))
                r = Character.getNumericValue(roll.charAt(i));
            round.diceRoll[i] = r;
            int h = -1;
            if (Character.isDigit(hold.charAt(i)))
                h = Character.getNumericValue(hold.charAt(i));
            round.diceHold[i] = h;
        }

        // double: round values from team player 1
        if (dbl.equals(""))
            roundDouble1 = null;
        if (!dbl.equals("") & roundDouble1 == null)
        {
            if (dbl.length() != 5)
                return false;
            roundDouble1 = new Round(this, playerId, 3);
            for (int i = 0; i < roundDouble1.diceRoll.length; i++)
            {
                int d = -1;
                if (Character.isDigit(dbl.charAt(i)))
                    d = Character.getNumericValue(dbl.charAt(i));
                if (isDoubleServed)
                {
                    roundDouble1.diceRoll[i] = d;
                    roundDouble1.diceHold[i] = -1;
                }
                else
                {
                    roundDouble1.diceHold[i] = d;
                    roundDouble1.diceRoll[i] = -1;
                }
            }
            roundDouble1.computeRoundValues();
            roundDouble1.setResult();
//Log.i(tag, "computeRound(), roundDouble1.bestCol: " + roundDouble1.bestCol + ", roundDouble1.bestCol: " + roundDouble1.bestCol);
            doubleServedId = 0;
            if (roundDouble1.isServed & roundDouble1.bestCombiId > 0)
                doubleServedId = roundDouble1.bestCombiId;
        }

        round.computeRoundValues();
        round.setResult();
//Log.i(tag, "computeRound(), round.bestCol: " + round.bestCol + ", round.bestCol: " + round.bestCol);
        return true;

    }

    private void setColWorstBest()
    {
        colRanking = new int[col];
        for (int i = 0; i < col; ++i)
        {
            colRanking[i] = i +1;
        }
    }

    void setColValues(String colPoints)
    {

//        Log.i(TAG, "setColValues(), colPoints: " + colPoints);

        String[] strSp = colPoints.split(",");
        colValues = new int[col];
        for (int i = 0; i < col; i++)
        {
            int val;
            val = Integer.parseInt(strSp[i]);
            colValues[i] = val;
        }
    }

    void setColRanking(String colPoints)
    {
        String[] strSp = colPoints.split(",");
        colRanking = new int[col];
        for (int i = 0; i < col; i++)
        {
            int val;
            val = Integer.parseInt(strSp[i]);
            colRanking[i] = val;
        }
        int min = 99;   int minCol = -1;
        int max = -1;   int maxCol = -1;
        for (int i = 0; i < col; i++)
        {
            if (colRanking[i] < min)
                {min = colRanking[i];   minCol = i;}
            if (colRanking[i] > max)
                {max = colRanking[i];   maxCol = i;}
        }
        if (max > min)
        {
            colRanking[minCol] = 1;
            colRanking[maxCol] = col;
            int next = 1;
            for (int i = 0; i < col; i++)
            {
                if (i != minCol & i != maxCol)
                {
                    next++;
                    colRanking[i] = next;
                }
            }
        }
        else
        {
            for (int i = 0; i < col; i++)
            {
                colRanking[i] = -1;
            }
            boolean isSetRandom = true;
            while (isSetRandom)
            {
                Random rd = new Random();
                int n = rd.nextInt(col) + 1;
                int cntEmpty = 0;
                boolean isEntered = false;
                for (int i = 0; i < col; i++)
                {
                    if (colRanking[i] == -1)
                        cntEmpty++;
                    if (colRanking[i] == n)
                        isEntered = true;
                }
                if (cntEmpty == 0)
                    isSetRandom = false;
                else
                {
                    if (!isEntered)
                    {
                        for (int i = 0; i < col; i++)
                        {
                            if (colRanking[i] == -1)
                            {
                                colRanking[i] = n;
                                break;
                            }
                        }
                    }
                }
            }
        }

        String logTxt = "col ranking: ";
        for (int i = 0; i < col; i++)
        {
            logTxt = logTxt + colRanking[i] + " ";
        }
        if (isLogging)
            Log.i(TAG, logTxt);
    }

    String computeDiceAction(char player, int diceTry)
    {
        if (isDouble)
        {
            if ((diceTry == 3 | round.entryOK))
            {
                if (roundDouble1 == null)
                    return "dice " + player + " h1:" + round.diceAll + " " + round.isServed;
                else
                    return getReturnEntry(player);
            }
        }
        else
        {
            if (diceTry == 3 | round.entryOK)
                return getReturnEntry(player);
        }

        return getReturnRollAndHold(player, diceTry);

    }

    private String getReturnEntry(char player)
    {
        if (isDouble & roundDouble1 != null & checkDoubleReturnEntry(player))
            return "entry " + player + " " + roundDouble1.entryCol + " " + roundDouble1.entryRow + " " + roundDouble1.entryValue;
        else
            return "entry " + player + " " + round.entryCol + " " + round.entryRow + " " + round.entryValue;
    }

    private boolean checkDoubleReturnEntry(char playerId)
    {
        // true : roundDouble1, false : round
        if (roundDouble1 == null)
            return false;
        if (isLogging)
        {
            Log.i(TAG, "D1 col: " + roundDouble1.bestCol + ", row: " + roundDouble1.bestRow + ", value: " + roundDouble1.bestValue + ", rating: " + roundDouble1.bestRating);
            Log.i(TAG, "D2 col: " + round.bestCol + ", row: " + round.bestRow + ", value: " + round.bestValue + ", rating: " + round.bestRating);
        }

        if (roundDouble1.bestValue > 0 & round.bestValue == 0)
            return true;
        if (round.bestValue > 0 & roundDouble1.bestValue == 0)
            return false;
        if (roundDouble1.imageCount[roundDouble1.bestImageId] == 5 & round.bestRow < ROW_ID_GRANDE)
            return true;
        if (round.imageCount[round.bestImageId] == 5 & roundDouble1.bestRow < ROW_ID_GRANDE)
            return false;
//Log.i(tag, "10 ??? checkDoubleReturnEntry(), served");
        if (roundDouble1.isServed & !round.isServed & roundDouble1.bestRow >= ROW_ID_STRAIGHT & round.bestRow != ROW_ID_GRANDE)
            return true;
        if (round.isServed & !roundDouble1.isServed & round.bestRow >= ROW_ID_STRAIGHT & roundDouble1.bestRow != ROW_ID_GRANDE)
            return false;
//Log.i(tag, "20 ??? checkDoubleReturnEntry(), round.bestRow: " + round.bestRow + ", roundDouble1.bestRow: " + roundDouble1.bestRow);
        if  (       roundDouble1.bestRow == DM.ROW_ID_FULL & round.bestRow < ROW_ID_STRAIGHT
                &   round.bestValue / DM.ROW_MULTIPLIER[round.bestRow] <= 3
            )
            return true;
        if  (       round.bestRow == DM.ROW_ID_FULL & roundDouble1.bestRow < ROW_ID_STRAIGHT
                &   roundDouble1.bestValue / DM.ROW_MULTIPLIER[roundDouble1.bestRow] <= 3
            )
            return false;
//Log.i(tag, "30 ??? checkDoubleReturnEntry(), < ROW_ID_STRAIGHT 1");
        if  (       roundDouble1.bestRow < ROW_ID_STRAIGHT & round.bestRow < ROW_ID_STRAIGHT
                &   roundDouble1.bestValue / DM.ROW_MULTIPLIER[roundDouble1.bestRow] <= 2
                &   roundDouble1.bestValue / DM.ROW_MULTIPLIER[roundDouble1.bestRow] == round.bestValue / DM.ROW_MULTIPLIER[round.bestRow]
                )
        {
            if (roundDouble1.bestRow <= round.bestRow)
                return true;
            else
                return false;
        }
//Log.i(tag, "40 ??? checkDoubleReturnEntry(), < ROW_ID_STRAIGHT 2");
        if  (       roundDouble1.bestRow < ROW_ID_STRAIGHT & round.bestRow < ROW_ID_STRAIGHT
                &   roundDouble1.bestValue / DM.ROW_MULTIPLIER[roundDouble1.bestRow] == round.bestValue / DM.ROW_MULTIPLIER[round.bestRow]
            )
            {
                if (roundDouble1.bestValue / DM.ROW_MULTIPLIER[roundDouble1.bestRow] <= 3)
                {
                    if (roundDouble1.bestRow < round.bestRow)
                        return true;
                    else
                        return false;
                }
                else
                {
                    if (roundDouble1.bestCol == round.bestCol)
                    {
                        if (roundDouble1.bestRow > round.bestRow)
                            return true;
                        else
                            return false;
                    }
                    if (roundDouble1.getRowCnt(roundDouble1.bestRow, true) >= round.getRowCnt(round.bestRow, true))
                        return true;
                    else
                        return false;
                }
            }
//Log.i(tag, "50 ??? checkDoubleReturnEntry(), >= ROW_ID_STRAIGHT");
        if (roundDouble1.bestRow >= DM.ROW_ID_STRAIGHT & round.bestRow < ROW_ID_STRAIGHT & round.bestValue / DM.ROW_MULTIPLIER[round.bestRow] <= 3)
            return true;
        if (round.bestRow >= DM.ROW_ID_STRAIGHT & roundDouble1.bestRow < ROW_ID_STRAIGHT & roundDouble1.bestValue / DM.ROW_MULTIPLIER[roundDouble1.bestRow] <= 3)
            return false;
        if (roundDouble1.bestRow >= DM.ROW_ID_STRAIGHT & round.bestRow < ROW_ID_STRAIGHT & round.bestValue / DM.ROW_MULTIPLIER[round.bestRow] >= 4)
            return false;
        if (round.bestRow >= DM.ROW_ID_STRAIGHT & roundDouble1.bestRow < ROW_ID_STRAIGHT & roundDouble1.bestValue / DM.ROW_MULTIPLIER[roundDouble1.bestRow] >= 4)
            return true;
//Log.i(tag, "60 ??? checkDoubleReturnEntry(), served|!served");
        if ((roundDouble1.isServed & round.isServed) | (!roundDouble1.isServed & !round.isServed))
        {
            if  (roundDouble1.bestRow == DM.ROW_ID_STRAIGHT & (round.bestRow == ROW_ID_FULL | round.bestRow == ROW_ID_POKER))
                return true;
            if  (round.bestRow == DM.ROW_ID_STRAIGHT & (roundDouble1.bestRow == ROW_ID_FULL | roundDouble1.bestRow == ROW_ID_POKER))
                return false;
        }

        if  (       roundDouble1.bestRow < DM.ROW_ID_STRAIGHT & round.bestRow < ROW_ID_STRAIGHT
                &   roundDouble1.bestValue / DM.ROW_MULTIPLIER[roundDouble1.bestRow] > round.bestValue / DM.ROW_MULTIPLIER[round.bestRow]
            )
            return true;
        if  (       roundDouble1.bestRow < DM.ROW_ID_STRAIGHT & round.bestRow < ROW_ID_STRAIGHT
                &   round.bestValue / DM.ROW_MULTIPLIER[round.bestRow] > roundDouble1.bestValue / DM.ROW_MULTIPLIER[roundDouble1.bestRow]
                )
            return false;
//Log.i(tag, "99 ??? checkDoubleReturnEntry()");
        if (roundDouble1.bestRow >= DM.ROW_ID_FULL & round.bestRow >= ROW_ID_FULL)
        {
            if (roundDouble1.bestValue >= round.bestValue)
                return true;
            else
                return false;
        }

        if (isLogging)
            Log.i(TAG, "checkDoubleReturnEntry(), return false");
        return false;

    }

    private String getReturnRollAndHold(char player, int diceTry)
    {
        String doubleValue = "";
        if (roundDouble1 != null)
            doubleValue = " h1:" + roundDouble1.diceAll + " " + roundDouble1.isServed;
        return "dice " + player + " " + diceTry + " r:" + round.getRollValues() + " h:" + round.getHoldValues() + doubleValue;
    }

    // HELPERS
    int getRowSum(int[] row1, int[] row2)
    {
        int sum = 0;
        if (row1 != null)
        {
            for (int i = 0; i < row; i++)
            {
                if (row1[i] > 0)
                    sum = sum + row1[i];
            }
        }
        if (row2 != null)
        {
            for (int i = 0; i < row; i++)
            {
                if (row2[i] > 0)
                    sum = sum + row2[i];
            }
        }
        return sum;
    }

    private final String TAG = "DM";
    boolean isLogging = false;
    boolean isTesting = false;

    int playerNumber = 2;            // 2 or 3
    boolean isDouble = false;
    final static int row = 10; // escalero: 6 images, 4 combinations
    int col = 3;                // 1...7
    int colRanking[];
    public int colValues[];
    String colPoints = "1,2,4";
    int colBonus = 3;
    int bonusServed = 5;
    int bonusServedGrande = 30;

    Player player;  // player data(Columns)
    private Round round;
    Round roundDouble1;
    int doubleServedId = 0;
    boolean playerIsP1 = false;
    int maxRoundes = 0;
    int playerRoundesToGo = 0;
    int roundCheck4 = ROUND_CHECK_4;
    int roundCheck6 = ROUND_CHECK_6;
    int roundCheck8 = ROUND_CHECK_8;
    int roundCheck12 = ROUND_CHECK_12;
    int roundCheck16 = ROUND_CHECK_16;
    int roundCheck20 = ROUND_CHECK_20;

    final static int ROW_MULTIPLIER[] = { 1, 2, 3, 4, 5, 6, 20, 30, 40, 50};

    final static int ROW_P3_COL_WORST[]    = {0, 2, 6, 8, 10, 12, 20, 30, 40, 50};
    final static int ROW_P3_COL_CENTER[]   = {3, 6, 9, 12, 15, 18, 20, 30, 40, 50};
    final static int ROW_P3_COL_BEST[]     = {3, 6, 12, 16, 20, 24, 25, 35, 45, 50};
    final static int ROW_P3_DOUBLE_COL_WORST[]    = {2, 4, 9, 12, 15, 18, 20, 30, 40, 50};
    final static int ROW_P3_DOUBLE_COL_CENTER[]   = {3, 6, 9, 12, 20, 24, 25, 35, 40, 50};
    final static int ROW_P3_DOUBLE_COL_BEST[]     = {4, 8, 12, 16, 20, 24, 25, 35, 45, 50};

    final static int ROW_ID_9 = 0;
    final static int ROW_ID_10 = 1;
    final static int ROW_ID_J = 2;
    final static int ROW_ID_Q = 3;
    final static int ROW_ID_K = 4;
    final static int ROW_ID_A = 5;
    final static int ROW_ID_STRAIGHT = 6;
    final static int ROW_ID_FULL = 7;
    final static int ROW_ID_POKER = 8;
    final static int ROW_ID_GRANDE = 9;
    final static int ROW_ID_PAIR = 2;
    final static int ROUND_CHECK_4 = 4;
    final static int ROUND_CHECK_6 = 6;
    final static int ROUND_CHECK_8 = 8;
    final static int ROUND_CHECK_12 = 12;
    final static int ROUND_CHECK_16 = 16;
    final static int ROUND_CHECK_20 = 20;

    final static int P1_NON                         = 0;

    final static int P1_COLUMN_FULL                 = 100;

    final static int P1_PLAY_LOST                   = 210;
    final static int P1_PLAY_WON                    = 220;
    final static int P1_PLAY_VALUE_0                = 222;
    final static int P1_PLAY_WON_OR_LOST            = 230;
    final static int P1_PLAY_GIVE_IT_UP             = 250;

    final static int P1_PLAY_WEAK                   = 290;
    final static int P1_PLAY_MAX_VALUE              = 295;

    final static int P1_PLAY                        = 300;
    final static int P1_PLAY_WORST                  = 310;

    final static int PLAY_P3                        = 400;

    final static int P1_PLAY_LAST_ROUNDES           = 505;
    final static int P1_PLAY_STRAIGHT_AND_GRANDE    = 510;
    final static int P1_PLAY_HOLD_WEAK              = 520;
    final static int P1_PLAY_GRANDE_ATTACK          = 520;
    final static int P1_PLAY_GRANDE_WEAK            = 523;
    final static int P1_PLAY_GRANDE                 = 525;
    final static int P1_PLAY_GRANDE_BEST            = 529;

    final static int P1_PLAY_P3                     = 540;
    final static int P1_PLAY_STRAIGHT               = 560;
    final static int P1_PLAY_STRAIGHT_GRANDE        = 562;
    final static int P1_PLAY_HOLD                   = 564;
    final static int P1_PLAY_HOLD_STRAIGHT          = 566;


    final static int P1_PLAY_IMAGE                  = 570;
    final static int P1_PLAY_IMAGE_STRAIGHT         = 575;
    final static int P1_PLAY_HOLD_BEST              = 580;
    final static int P1_PLAY_GRANDE_HOLD_WEAK       = 530;
    final static int P1_PLAY_OPO_WEAK               = 530;
    final static int P1_PLAY_GRANDE_HOLD            = 580;
    final static int P1_PLAY_STRAIGHT_GRANDE_HOLD   = 585;
    final static int P1_PLAY_ATTACK                 = 590;
    final static int P1_PLAY_ATTACK_WIN             = 595;

    final static int P1_PLAY_POSIBLE_WIN            = 610;

    final static int P1_PLAY_BEST                   = 720;
    final static int P1_PLAY_WIN                    = 750;
    final static int P1_PLAY_WIN_ATTACK             = 755;
    final static int P1_PLAY_WIN_BEST_COL           = 760;

}
