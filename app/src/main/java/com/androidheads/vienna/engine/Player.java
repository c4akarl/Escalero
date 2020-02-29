package com.androidheads.vienna.engine;

import android.util.Log;

import java.util.ArrayList;

class Player
{

    Player(DM dm)
    {
        this.dm = dm;
        initPlayerList(dm.col);
    }

    private void initPlayerList(int col)
    {
        for (int i = 0; i < col; i++)
        {
            playerA.add(new Column(i, dm.colRanking[i]));
            playerB.add(new Column(i, dm.colRanking[i]));
            playerC.add(new Column(i, dm.colRanking[i]));
        }
    }

    Column getPlayerColumn(char playerId, int col)
    {
        switch (playerId)
        {
            case 'A':  return playerA.get(col);
            case 'B':  return playerB.get(col);
            case 'C':  return playerC.get(col);
            default:  return null;
        }
    }

    void setPlayerColumn(char playerId, int col, Column column)
    {
        switch (playerId)
        {
            case 'A':  playerA.set(col, column); break;
            case 'B':  playerB.set(col, column); break;
            case 'C':  playerC.set(col, column); break;
        }
    }

    Column getOponentColumn(char playerId, int col)
    {
        Column op = null;
        switch (playerId)
        {
            case 'A':
                op = playerB.get(col);
                if (dm.playerNumber == 3 & playerC.get(col).sum > op.sum)
                    op = playerC.get(col);
                break;
            case 'B':
                op = playerA.get(col);
                if (dm.playerNumber == 3 & playerC.get(col).sum > op.sum)
                    op = playerC.get(col);
                break;
            case 'C':
                op = playerA.get(col);
                if (dm.playerNumber == 3 & playerB.get(col).sum > op.sum)
                    op = playerB.get(col);
                break;
        }
        return op;
    }

    int getEntry(char playerId, int col, int rowId)
    {
        Column column;
        switch (playerId)
        {
            case 'A':   column = playerA.get(col); break;
            case 'B':   column = playerB.get(col); break;
            case 'C':   column = playerC.get(col); break;
            default:    column = playerA.get(col); break;
        }
        return column.row[rowId];
    }

    void setEntry(char playerId, int col, int row, int value)
    {
        Column pl = getPlayerColumn(playerId, col);
        pl.row[row] = value;
        if (value == -1 & pl.cntEmpty >= 4)
            pl.isP1 = false;
        pl.sum = 0;
        pl.sumMax = 0;
        pl.cntEntry = 0;
        pl.cntEmpty = 0;
        for (int i = 0; i < pl.rowCnt; i++)
        {
            if (pl.row[i] >= 0)
            {
                pl.sum = pl.sum + pl.row[i];
                pl.sumMax = pl.sumMax + pl.row[i];
                pl.cntEntry++;
            }
            else
            {
                if (i <= 5)
                    pl.sumMax = pl.sumMax + (DM.ROW_MULTIPLIER[i] * 5);
                else
                {
                    if (i == 9)
                    {
                        if (pl.cntEmpty > 0)
                            pl.sumMax = pl.sumMax + DM.ROW_MULTIPLIER[i];
                        else
                            pl.sumMax = pl.sumMax + DM.ROW_MULTIPLIER[i] + dm.bonusServedGrande;
                    }
                    else
                        pl.sumMax = pl.sumMax + DM.ROW_MULTIPLIER[i] + dm.bonusServed;
                }
                pl.cntEmpty++;
            }
        }
        setPlayerColumn(playerId, col, pl);

        coumputePlayerEntry(col);
    }

    private void coumputePlayerEntry(int col)
    {
        Column columnA = playerA.get(col);
        Column columnB = playerB.get(col);
        Column columnC = playerC.get(col);
        columnA.isWon = false;
        columnB.isWon = false;
        columnC.isWon = false;
        columnA.isLost = false;
        columnB.isLost = false;
        columnC.isLost = false;
        if (dm.playerNumber == 2)
        {
            if (columnA.cntEntry == 0 | columnB.cntEntry == 0)
                return;
            else
            {
                if (columnA.sum > columnB.sumMax)
                {
                    columnA.isWon = true;
                    columnB.isLost = true;
                }
                if (columnB.sum > columnA.sumMax)
                {
                    columnB.isWon = true;
                    columnA.isLost = true;
                }
            }
        }
        else
        {
            if (columnA.cntEntry == 0 | columnB.cntEntry == 0 | columnC.cntEntry == 0)
                return;
            else
            {
                if (columnA.sum > columnB.sumMax & columnA.sum > columnC.sumMax)
                {
                    columnA.isWon = true;
                    columnB.isLost = true;
                    columnC.isLost = true;
                }
                if (columnB.sum > columnA.sumMax & columnB.sum > columnC.sumMax)
                {
                    columnB.isWon = true;
                    columnA.isLost = true;
                    columnC.isLost = true;
                }
                if (columnC.sum > columnA.sumMax & columnC.sum > columnB.sumMax)
                {
                    columnC.isWon = true;
                    columnA.isLost = true;
                    columnB.isLost = true;
                }
            }
        }

        playerA.set(col, columnA);
        playerB.set(col, columnB);
        playerC.set(col, columnC);
    }

    void computePlayerPlan(char playerId)
    {
        dm.playerRoundesToGo = getMovesToGo(playerId);
        if (!dm.isDouble)
        {
            dm.roundCheck4 = DM.ROUND_CHECK_4;
            dm.roundCheck6 = DM.ROUND_CHECK_6;
            dm.roundCheck8 = DM.ROUND_CHECK_8;
            dm.roundCheck12 = DM.ROUND_CHECK_12;
            dm.roundCheck16 = DM.ROUND_CHECK_16;
        }
        else
        {
            dm.roundCheck4 = DM.ROUND_CHECK_4 / 2;
            dm.roundCheck6 = DM.ROUND_CHECK_6 / 2;
            dm.roundCheck8 = DM.ROUND_CHECK_8 / 2;
            dm.roundCheck12 = DM.ROUND_CHECK_12 / 2;
            dm.roundCheck16 = DM.ROUND_CHECK_16 / 2;
            dm.roundCheck20 = DM.ROUND_CHECK_20 / 2;
        }

        setColRanking(playerId);
        dm.playerIsP1 = isP1(playerId);

        if (dm.playerIsP1)
            computeP1(playerId);
        else
            computeP3(playerId);
    }

    int getMovesToGo(char playerId)
    {
        int cnt = 0;
        for (int i = 0; i < dm.col; ++i)
        {
            for (int j = 0; j < DM.row; ++j)
            {
                if (dm.player.getEntry(playerId, i , j) < 0)
                    cnt++;
            }
        }
        return cnt;
    }

    private boolean isP1(char playerId)
    {
        boolean p1 = false;
        for (int i = 0; i < dm.col; i++)
        {
            Column pl = getPlayerColumn(playerId, i);
            Column op = getOponentColumn(playerId, i);
            if (pl.isP1)
                p1 = true;
            if (pl.cntEmpty <= 4 | pl.isWon | pl.isLost)
                p1 = true;
            if ((pl.row[DM.ROW_ID_GRANDE] > 0 | op.row[DM.ROW_ID_GRANDE] > 0) & pl.row[DM.ROW_ID_GRANDE] != op.row[DM.ROW_ID_GRANDE])
                p1 = true;
            if (dm.playerRoundesToGo < dm.roundCheck20)
                p1 = true;
        }

        for (int i = 0; i < dm.col; i++)
        {
            Column pl = getPlayerColumn(playerId, i);
            if (p1)
                pl.isP1 = true;
            else
                pl.isP1 = false;
            dm.player.setPlayerColumn(playerId, i, pl);
        }

        if (p1)
            dm.playerIsP1 = true;
        return p1;
    }

    private void setColRanking(char playerId)
    {

        boolean isSetRanking = false;
        int col1 = -1;
        int col2 = -1;
        int ranking1 = 0;
        int ranking2 = 0;
        int lowestAction = 999;
        for (int i = 0; i < dm.col; i++)
        {
            Column pl = getPlayerColumn(playerId, i);
//            if (pl.p1Action <= DM.P1_PLAY_WON & pl.colRanking != 1)
            if ((pl.isWon | pl.isLost) & pl.colRanking != 1)
            {
                pl.colRanking = 1;
//Log.i(tag, "1 setColRanking(), setPlayerColumn(), col: " + i + ", pl.isWon: " + pl.isWon + ", pl.isLost: " + pl.isLost);
                setPlayerColumn(playerId, i, pl);
                col1 = i;
                isSetRanking = true;
            }
            if (pl.p1Action < lowestAction)
                lowestAction = pl.p1Action;
            if (pl.colRanking == 1)
                ranking1++;
        }
        if (isSetRanking & ranking1 > 1)
        {
            for (int i = 0; i < dm.col; i++)
            {
                Column pl = getPlayerColumn(playerId, i);
                if (pl.colRanking == 1 & i != col1)
                {
                    pl.colRanking = 2;
//Log.i(tag, "2 setColRanking(), setPlayerColumn(), col: " + i + ", col1: " + col1);
                    setPlayerColumn(playerId, i, pl);
                    col2 = i;
                }
                if (pl.colRanking == 2)
                    ranking2++;
            }
            if (ranking2 > 1)
            {
                for (int i = 0; i < dm.col; i++)
                {
                    Column pl = getPlayerColumn(playerId, i);
                    if (pl.colRanking == 2 & i != col2)
                    {
                        pl.colRanking = 3;
                        setPlayerColumn(playerId, i, pl);
                    }
                }
            }
            loggingRank(playerId);
            return;
        }

        int counter;
        int counterWorst = 999;
        int counterBest = -999;
        int cor = 0;

        for (int i = 0; i < dm.col; i++)
        {
            Column pl = getPlayerColumn(playerId, i);
            Column op = getOponentColumn(playerId, i);
            if (dm.colValues[i] > 1)
            {
                cor = cor + i;
                if (i == dm.col -1)
                    cor = cor++;
            }
            counter = dm.colValues[i] + cor;
//Log.i(tag, "1 setColRanking(), col: " + i + ", dm.colValues[i]: " + dm.colValues[i] + ", counter: " + counter + ", pl.p1Action: " + pl.p1Action);
            int diff9_10 = 0;
            int diffImage = 0;
            for (int j = 0; j < DM.row; ++j)
            {
                int grandeCounter = 3;
                int addCounter = 0;
                if (j == DM.ROW_ID_GRANDE & dm.bonusServedGrande > 0)
                    addCounter = dm.bonusServedGrande / 5;
                int plValue = pl.row[j];
                int opValue = op.row[j];
                if (j < DM.ROW_ID_STRAIGHT)
                {
                    if (plValue < 0)
                        plValue = DM.ROW_MULTIPLIER[j] * 3;
                    if (opValue < 0)
                        opValue = DM.ROW_MULTIPLIER[j] * 3;
                    int rowValue;
                    diffImage = diffImage + plValue - opValue;
                    int cnt = 0;
                    if (j == DM.ROW_ID_9 | j == DM.ROW_ID_10)
                    {
                        diff9_10 = diff9_10 + plValue - opValue;
                        if (j == DM.ROW_ID_10)
                        {
                            rowValue = DM.ROW_MULTIPLIER[DM.ROW_ID_J];
                            if (diff9_10 != 0)
                                cnt = diff9_10 / rowValue;
                        }
                    }
                    else
                    {
                        int diff = plValue - opValue;
                        rowValue = DM.ROW_MULTIPLIER[j];
                        if (diff != 0)
                            cnt = diff / rowValue;
                    }
                    if (cnt == 0 & plValue > opValue)
                        cnt = 1;
                    if (cnt == 0 & opValue > plValue)
                        cnt = -1;
                    if (plValue == opValue)
                        cnt = 0;
                    counter = counter + cnt;
//Log.i(tag, "2a ??? setColRanking(), col: " + i + ", j: " + j + ", counter: " + counter + ", cnt: " + cnt + ", plValue: " + plValue + ", opValue: " + opValue);
                }
                else
                {
                    if (j == DM.ROW_ID_GRANDE)
                    {
                        if (plValue == DM.ROW_MULTIPLIER[j])
                            counter = counter + grandeCounter;
                        if (opValue == DM.ROW_MULTIPLIER[j])
                            counter = counter - grandeCounter;
                        if (plValue == DM.ROW_MULTIPLIER[j] + dm.bonusServedGrande)
                            counter = counter + grandeCounter + addCounter;
                        if (opValue == DM.ROW_MULTIPLIER[j] + dm.bonusServedGrande)
                            counter = counter - grandeCounter - addCounter;
                    }
                    else
                    {
                        if (plValue == DM.ROW_MULTIPLIER[j] + dm.bonusServed)
                            counter++;
                        if (opValue == DM.ROW_MULTIPLIER[j] + dm.bonusServed)
                            counter--;
                        if (plValue == 0 & op.row[j] > 0)
                            counter = counter - 2;
                        if (opValue == 0 & pl.row[j] > 0)
                            counter = counter + 2;
                        if (getMovesToGo(playerId) <= dm.roundCheck4)
                        {
                            if (plValue == DM.ROW_MULTIPLIER[j] & op.row[j] < 0)
                                counter = counter - 1;
                            if (opValue == DM.ROW_MULTIPLIER[j] & pl.row[j] < 0)
                                counter = counter + 1;
                        }
                    }
//Log.i(tag, "2b ??? setColRanking(), col: " + i + ", j: " + j + ", counter: " + counter);
                }
            }
            pl.playCounter = counter - dm.colValues[i] - cor;
            pl.colValue = dm.colValues[i];
            pl.rankingCounter = counter;
            if  (       isBestColValue(playerId, pl)  & dm.player.getMovesToGo(playerId) >=  dm.roundCheck8
                    &   pl.row[DM.ROW_ID_GRANDE] < 0 & op.row[DM.ROW_ID_GRANDE] > 0
                    )
                pl.rankingCounter = pl.rankingCounter + 3;
//Log.i(tag, "3 ??? setColRanking(), col: " + i + ", pl.playCounter: " + pl.playCounter  + ", pl.rankingCounter: " + pl.rankingCounter);
            if (counter < counterWorst)
            {
                counterWorst = counter;
            }
            if (counter > counterBest)
                counterBest = counter;
            pl.isPlayLess = false;
            pl.isPlayMore = false;
            pl.isOr = false;
            setPlayerColumn(playerId, i, pl);
        }
//Log.i(tag, "4 ??? setColRanking(), counterWorst: " + counterWorst + ", counterBest: " + counterBest);

        int lowestPlayCounter = 999;
        int highestPlayCounter = -999;
        int lCol = -1;
        int hCol = -1;
        for (int i = 0; i < dm.col; i++)
        {
            Column pl = getPlayerColumn(playerId, i);
            if (pl.rankingCounter < lowestPlayCounter)
            {
                lowestPlayCounter = pl.rankingCounter;
                lCol = i;
            }
            if (pl.rankingCounter > highestPlayCounter)
            {
                highestPlayCounter = pl.rankingCounter;
                hCol = i;
            }
        }
//Log.i(tag, "5 ??? setColRanking(), lowestPlayCounter: " + lowestPlayCounter + ", highestPlayCounter: " + highestPlayCounter);
        if (isEvenColValue() & lCol >= 0 & hCol >= 0 & lCol != hCol)
        {
            Column pl = getPlayerColumn(playerId, lCol);
            pl.colRanking = 1;
            setPlayerColumn(playerId, lCol, pl);
            pl = getPlayerColumn(playerId, hCol);
            pl.colRanking = dm.col;
            setPlayerColumn(playerId, hCol, pl);
            int ranking = 1;
            for (int i = 0; i < dm.col; i++)
            {
                pl = getPlayerColumn(playerId, i);
                if (i != lCol & i != hCol)
                {
                    ranking++;
                    pl.colRanking = ranking;
                    setPlayerColumn(playerId, i, pl);
                }
            }
        }
        else
        {
            int colR1 = -1;
            int colR2 = -1;
            for (int i = 0; i < dm.col; i++)
            {
                Column pl = getPlayerColumn(playerId, i);
                if (pl.colRanking == 1)
                    colR1 = i;
                if (pl.colRanking > 1 & pl.colRanking < dm.col - 2)
                    colR2 = i;
            }
            if (colR1 >= 0 & colR2 >= 0)
            {
                Column plR1 = getPlayerColumn(playerId, colR1);
                Column plR2 = getPlayerColumn(playerId, colR2);
                if (plR1.playCounter > 1 & plR2.playCounter > 1 & plR2.colValue < plR1.colValue)
                {
                    plR1.colRanking = 2;
                    plR2.colRanking = 1;
                    setPlayerColumn(playerId, colR1, plR1);
                    setPlayerColumn(playerId, colR2, plR2);
                }
            }
        }

//        if (dm.isLogging)
//        {
//            String txtRanking = "col ranking: ";
//            for (int i = 0; i < dm.col; i++)
//            {
//                Column pl = getPlayerColumn(playerId, i);
//                txtRanking = txtRanking + pl.colRanking + " ";
//            }
//            Log.i(tag, txtRanking);
//        }

        loggingRank(playerId);
    }

    private void loggingRank(char playerId)
    {
        if (dm.isLogging)
        {
            String txtRanking = "col ranking: ";
            for (int i = 0; i < dm.col; i++)
            {
                Column pl = getPlayerColumn(playerId, i);
                txtRanking = txtRanking + pl.colRanking + " ";
            }
            Log.i(TAG, txtRanking);
        }
    }

    private void computeP3(char playerId)
    {
        for (int i = 0; i < dm.col; ++i)
        {
            Column pl = dm.player.getPlayerColumn(playerId, i);
            Column op = dm.player.getOponentColumn(playerId, i);
            if (!dm.isDouble)
            {
                for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
                {
                    if (pl.row[j] == -1)
                    {
                        if (pl.colRanking == 1) pl.rowPlay[j] = DM.ROW_P3_COL_WORST[j];
                        if (pl.colRanking == 2) pl.rowPlay[j] = DM.ROW_P3_COL_CENTER[j];
                        if (pl.colRanking == 3) pl.rowPlay[j] = DM.ROW_P3_COL_BEST[j];
                    }
                    else
                        pl.rowPlay[j] = -1;
                    if (op.row[j] == -1)
                    {
                        if (pl.colRanking == 1) pl.rowOpo[j] = DM.ROW_P3_COL_WORST[j];
                        if (pl.colRanking == 2) pl.rowOpo[j] = DM.ROW_P3_COL_CENTER[j];
                        if (pl.colRanking == 3) pl.rowOpo[j] = DM.ROW_P3_COL_BEST[j];
                    }
                    else
                        pl.rowOpo[j] = -1;
                }
            }
            else
            {
                for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
                {
                    if (pl.row[j] == -1)
                    {
                        if (pl.colRanking == 1) pl.rowPlay[j] = DM.ROW_P3_DOUBLE_COL_WORST[j];
                        if (pl.colRanking == 2) pl.rowPlay[j] = DM.ROW_P3_DOUBLE_COL_CENTER[j];
                        if (pl.colRanking == 3) pl.rowPlay[j] = DM.ROW_P3_DOUBLE_COL_BEST[j];
                    }
                    else
                        pl.rowPlay[j] = -1;
                    if (op.row[j] == -1)
                    {
                        if (pl.colRanking == 1) pl.rowOpo[j] = DM.ROW_P3_DOUBLE_COL_WORST[j];
                        if (pl.colRanking == 2) pl.rowOpo[j] = DM.ROW_P3_DOUBLE_COL_CENTER[j];
                        if (pl.colRanking == 3) pl.rowOpo[j] = DM.ROW_P3_DOUBLE_COL_BEST[j];
                    }
                    else
                        pl.rowOpo[j] = -1;
                }
            }

            int diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
            if (diff < 0)
            {
                setRowPlay(pl, op, pl.colRanking, true);

                for (int j = 0; j < DM.ROW_ID_GRANDE; j++)
                {
                    if (j < DM.ROW_ID_STRAIGHT)
                    {
                        if (pl.row[j] > 0 & pl.rowOpo[j] > pl.row[j])
                            pl.rowOpo[j] = pl.rowOpo[j] - DM.ROW_MULTIPLIER[j];
                        if (op.row[j] > 0 & pl.rowPlay[j] > op.row[j])
                            pl.rowPlay[j] = pl.rowPlay[j] - DM.ROW_MULTIPLIER[j];
                    }
                    else
                    {
                        if (pl.row[j] > 0 & pl.rowOpo[j] > pl.row[j])
                            pl.rowOpo[j] = pl.row[j];
                        if (op.row[j] > 0 & pl.rowPlay[j] > op.row[j])
                            pl.rowPlay[j] = op.row[j];
                    }
                }

                diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
            }
            boolean isPlayP3 = false;
//Log.i(tag, "1 ??? col: " + pl.colId + ", computeP3(), diff: " + diff + ", pl.playCounter: " + pl.playCounter);
            if (diff >= 0)
                isPlayP3 = true;
            else
            {
                pl.p1Action = DM.P1_NON;
                playSetValues(pl, op);
                if (pl.rowPlay[DM.ROW_ID_STRAIGHT] >= 0 & pl.rowOpo[DM.ROW_ID_STRAIGHT] >= 0)
                    playStraight(playerId, pl, op, false);
                if (pl.p1Action == DM.P1_NON)
                    playGrande(pl, op);
                if (pl.p1Action == DM.P1_NON)
                    playHold(playerId, pl, op);
                if (pl.p1Action == DM.P1_NON)
                    playWeak(pl, op);
                if (pl.p1Action > DM.P1_NON)
                {
                    pl.isP1 = false;
                    pl.p1Action = DM.PLAY_P3 + pl.colRanking;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", computeP3(1), " + pl.p1Action);
                }
            }
            if (isPlayP3)
            {
                pl.p1Action = DM.PLAY_P3 + pl.colRanking;
                pl.isP1 = false;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", computeP3(2), " + pl.p1Action);
                int checkCounter = 2;
                if (dm.isDouble)
                    checkCounter = 4;
                if (isBestColValue(playerId, pl))
                    checkCounter = checkCounter + 3;
                if  (pl.playCounter >= checkCounter)
                    pl.isPlayLess = true;
                if (!pl.isPlayLess & pl.playCounter < checkCounter & pl.colRanking < dm.col & pl.colRanking > 1)
                    pl.isPlayMore = true;
                for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; j++)
                {
                    if (pl.row[j] > 0 & pl.rowOpo[j] > pl.row[j])
                        pl.rowOpo[j] = pl.row[j];
                    if (op.row[j] > 0 & pl.rowPlay[j] > op.row[j])
                        pl.rowPlay[j] = op.row[j];
                }
                diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
//Log.i(tag, "2 ??? col: " + pl.colId + ", computeP3(), isPlayLess: " + pl.isPlayLess + ", diff: " + diff);
                if (diff < DM.ROW_ID_K)
                    pl.isPlayLess = false;

                if  (pl.p1Action == 403 & dm.playerRoundesToGo <= dm.roundCheck16)
                    pl.isPlayLess = true;

                if (diff > 0)
                {
                    computeImagePlayValues(pl, op);
                    computePlayValues(pl, op);
                }
                else
                {
                    for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
                    {
                        int opPlay = 0;
                        if (pl.rowOpo[j] >= 0)
                            opPlay = pl.rowOpo[j];
                        else
                            opPlay = op.row[j];
                        if (pl.rowPlay[j] >= 0 & pl.rowPlay[j] < opPlay)
                            pl.rowPlay[j] = opPlay;
                    }
                }
            }
            else
            {
                diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                if (diff < 0)
                {
                    playSetValues(pl, op);
                    pl.p1Action = DM.P1_PLAY_GIVE_IT_UP;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", computeP3(), DM.P1_PLAY_GIVE_IT_UP 8");
                    computePlayValues(pl, op);
                }

            }
            if (pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & pl.rowOpo[DM.ROW_ID_GRANDE] == 0)
            {
                pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                pl.rowOpo[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
            }
            setPlayerColumn(playerId, i, pl);
        }
        setServeValues(playerId);
        setPlayerOponentGrande(playerId);

        if (dm.isLogging) loggingPlayValues(playerId);

        setPlay0(playerId);
        setEntryTable(playerId);

    }

    private void computeP1(char playerId)
    {
        for (int i = 0; i < dm.col; i++)
        {
            Column pl = getPlayerColumn(playerId, i);
            Column op = getOponentColumn(playerId, i);
            pl.p1Action = DM.P1_NON;
            pl.isPlayLess = false;

            playSetValues(pl, op);
            if (pl.p1Action == DM.P1_NON)
                playFullWonLost(pl, op);

//            if (pl.p1Action == DM.P1_NON)
//                playOpoEmpty1(playerId, pl, op);

            if (pl.p1Action == DM.P1_NON)
                playEmpty1(playerId, pl, op);

            if (pl.p1Action == DM.P1_NON)
                playP3(playerId, pl, op);

            if (pl.p1Action == DM.P1_NON)
                playP1_Best(playerId, pl, op);

            if (pl.p1Action == DM.P1_NON)
                playGrandeHold(playerId, pl, op);

            if (pl.p1Action == DM.P1_NON)
                playGrandeAttack(playerId, pl, op);

            if (pl.p1Action == DM.P1_NON)
                playImage(pl, op);

            if (pl.p1Action == DM.P1_NON)
                playStraight(playerId, pl, op, false);

            if (pl.p1Action == DM.P1_NON)
                playHold(playerId, pl, op);

            if (pl.p1Action == DM.P1_NON)
                playStraight(playerId, pl, op, true);

            if (pl.p1Action == DM.P1_NON)
                playAttackCounter(pl, op);

            if (pl.p1Action == DM.P1_NON)
                playGrande(pl, op);

            if (pl.p1Action == DM.P1_NON)
                playWeak(pl, op);

            setPlayerColumn(playerId, i, pl);

        }

        setPlayerOponentGrande(playerId);
        if (dm.isLogging) loggingPlayValues(playerId);

        setPlay0(playerId);
        setEntryTable(playerId);

    }

    private void playSetValues(Column pl, Column op)
    {
        pl.diffPlayImage = 0;
        pl.diffPlayCombo = 0;
        for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
        {
            if (pl.row[j] == -1)
                pl.rowPlay[j] = 0;
            else
                pl.rowPlay[j] = -1;

            if (op.row[j] == -1)
                pl.rowOpo[j] = 0;
            else
                pl.rowOpo[j] = -1;

            if (j < DM.ROW_ID_STRAIGHT)
            {
                if (op.row[j] == -1)
                    pl.rowOpo[j] = getEstimatedRowValue(j, dm.playerRoundesToGo, dm.isDouble, false);
                if (pl.row[j] == -1)
                {
                    pl.rowPlay[j] = getEstimatedRowValue(j, dm.playerRoundesToGo, dm.isDouble, false);
                    pl.diffPlayImage = pl.diffPlayImage + DM.ROW_MULTIPLIER[j] * 3;
                }
                else
                    pl.diffPlayImage = pl.diffPlayImage + pl.row[j];
                if (op.row[j] == -1)
                    pl.diffPlayImage = pl.diffPlayImage - DM.ROW_MULTIPLIER[j] * 3;
                else
                    pl.diffPlayImage = pl.diffPlayImage - op.row[j];
            }
            else
            {
                if (j == DM.ROW_ID_GRANDE)
                {
                    if (op.row[j] == -1 & pl.row[j] > 0)
                    {
                        if (pl.isWon)
                            pl.rowOpo[j] = getEstimatedRowValue(j, dm.playerRoundesToGo, dm.isDouble, false);
                    }
                }
                else
                {
                    if (pl.row[j] == -1)
                    {
                        if (j != DM.ROW_ID_STRAIGHT)
                            pl.rowPlay[j] = getEstimatedRowValue(j, dm.playerRoundesToGo, dm.isDouble, false);
                        pl.diffPlayCombo = pl.diffPlayCombo + DM.ROW_MULTIPLIER[j];
                    }
                    else
                        pl.diffPlayCombo = pl.diffPlayCombo + pl.row[j];
                    if (op.row[j] == -1)
                    {
                        if (j != DM.ROW_ID_STRAIGHT)
                            pl.rowOpo[j] = getEstimatedRowValue(j, dm.playerRoundesToGo, dm.isDouble, false);
                        pl.diffPlayCombo = pl.diffPlayCombo - DM.ROW_MULTIPLIER[j];
                    } else
                        pl.diffPlayCombo = pl.diffPlayCombo - op.row[j];
                }
            }
        }
        pl.diffPlay = pl.diffPlayImage + pl.diffPlayCombo;
    }

    private void playFullWonLost(Column pl, Column op)
    {
        if (pl.p1Action == DM.P1_NON & pl.cntEmpty == 0)
        {
            pl.p1Action = DM.P1_COLUMN_FULL;
            if (dm.isLogging)
                Log.i(TAG, "col: " + pl.colId + ", playFullWonLost(), DM.P1_COLUMN_FULL");
        }
        if (pl.p1Action == DM.P1_NON & pl.isWon)
        {
            pl.p1Action = DM.P1_PLAY_WON;
            if (dm.isLogging)
                Log.i(TAG, "col: " + pl.colId + ", playFullWonLost(), DM.P1_PLAY_WON");
        }
        if (pl.p1Action == DM.P1_NON & pl.isLost)
        {
            int diffRow = dm.getRowSum(pl.row, null) - dm.getRowSum(op.row, null);
            if (pl.rowPlay[DM.ROW_ID_GRANDE] >= 0 & diffRow >= (DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] + dm.bonusServedGrande) * -1)
            {
                pl.p1Action = DM.P1_PLAY_WEAK;
                pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] + dm.bonusServedGrande;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playFullWonLost(), DM.P1_PLAY_WEAK 8");
                computePlayValues(pl, op);
                return;
            }
            else
            {
                pl.p1Action = DM.P1_PLAY_LOST;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playFullWonLost(), DM.P1_PLAY_LOST");
            }
        }
        if (pl.p1Action == DM.P1_NON)
        {
            int diffRow = dm.getRowSum(pl.row, null) - dm.getRowSum(op.row, null);
//Log.i(tag, "1 playFullWonLost(), col: " + pl.colId + ", playFullWonLost(), diffRow: " + diffRow);
            if (diffRow > 0)
            {
                for (int j = 0; j <= DM.ROW_ID_GRANDE; ++j)
                {
                    int value = 0;
                    if (op.row[j] < 0)
                    {
                        if (j < DM.ROW_ID_STRAIGHT)
                            value = DM.ROW_MULTIPLIER[j] * 5;
                        else
                        {
                            if (j >= DM.ROW_ID_STRAIGHT & j < DM.ROW_ID_GRANDE)
                                value = DM.ROW_MULTIPLIER[j] + dm.bonusServed;
                            if (j == DM.ROW_ID_GRANDE & pl.row[DM.ROW_ID_GRANDE] > 0)
                                value = DM.ROW_MULTIPLIER[j];
                        }
                        diffRow = diffRow - value;
                    }
                }
//Log.i(tag, "2 playFullWonLost(), col: " + pl.colId + ", playFullWonLost(), diffRow: " + diffRow);
                if (diffRow > 0)
                {
                    pl.p1Action = DM.P1_PLAY_WON_OR_LOST;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playFullWonLost(), DM.P1_PLAY_WON_OR_LOST");
                }
            }
        }
        for (int j = 0; j < DM.ROW_ID_GRANDE; ++j)
        {
            if (pl.p1Action != DM.P1_NON & pl.rowPlay[j] > 0)
                pl.rowPlay[j] = 0;
        }
    }

    private void playP1_Best(char playerId, Column pl, Column op)
    {
        boolean isPlay0 = false;
        for (int i = 0; i < dm.col; i++)
        {
            Column plX = getPlayerColumn(playerId, i);
            if (plX.colId != pl.colId)
            {
                for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
                {
                    if (plX.rowPlay[j] == 0)
                    {
                        isPlay0 = true;
                        break;
                    }
                }
            }
        }
        if (pl.colRanking != dm.col | (!isPlay0 & dm.playerRoundesToGo <= dm.roundCheck8))
            return;
        if (pl.row[DM.ROW_ID_GRANDE] != op.row[DM.ROW_ID_GRANDE])
            return;

        if (pl.p1Action != DM.P1_NON)
            return;
        int checkCounter = 5;
        if (dm.isDouble)
            checkCounter = 7;
        int playCounter = pl.playCounter;
        if (pl.playCounter < 0)
            playCounter = pl.playCounter * -1;
        if (dm.playerRoundesToGo >= dm.roundCheck12)
            checkCounter++;
        if (pl.row[DM.ROW_ID_GRANDE] >= 0 & op.row[DM.ROW_ID_GRANDE] >= 0)
            checkCounter = checkCounter -3;
        if (playCounter >= checkCounter)
            return;
        if (op.row[DM.ROW_ID_GRANDE] >= 0 & pl.row[DM.ROW_ID_GRANDE] < 0)
            return;
        if (pl.cntEmpty > dm.playerRoundesToGo / 2)
            return;
        setRowPlay(pl, op, pl.colRanking, false);
        if  (       pl.cntEmpty >= 3
                &   pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & pl.rowOpo[DM.ROW_ID_GRANDE] == 0
            )
        {
            pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
            pl.rowOpo[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
        }
        int diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
        if (diff < 0 | (dm.playerRoundesToGo <= dm.roundCheck8 & pl.playCounter > 0))
            playSetValues(pl, op);
        else
        {
            pl.p1Action = DM.P1_PLAY_BEST;
            if (dm.isLogging)
                Log.i(TAG, "col: " + pl.colId + ", playP1_Best(), DM.P1_PLAY_BEST 1");
            int checkPlayLess = 2;
//            int roundPlayLess = dm.roundCheck12;
            int roundPlayLess = dm.roundCheck8;
            if (dm.isDouble)
            {
                checkPlayLess = 3;
                roundPlayLess = dm.roundCheck6;
            }
            if (pl.playCounter >= checkPlayLess)
                pl.isPlayLess = true;

            if (pl.cntEmpty >= 3 & dm.playerRoundesToGo <= roundPlayLess)
                pl.isPlayLess = true;
            if (!isBestColValue(playerId, pl) & pl.rowPlay[DM.ROW_ID_GRANDE] > 0 & pl.rowOpo[DM.ROW_ID_GRANDE] > 0)
                pl.isPlayLess = true;
            if (pl.cntEmpty >= 3 & pl.row[DM.ROW_ID_GRANDE] < 0 & dm.playerRoundesToGo <= dm.roundCheck12)
                pl.isPlayLess = true;

            if (dm.playerRoundesToGo <= dm.roundCheck8)
            {
                for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; j++)
                {
                    if (pl.rowOpo[j] > DM.ROW_MULTIPLIER[j])
                        pl.isPlayLess = true;
                }
            }
            computePlayValues(pl, op);
        }
    }

    private void playImage(Column pl, Column op)
    {
        if (pl.cntEmpty > 4)
            return;
        playSetValues(pl, op);
        int imageRow = -1;
        int imageCnt = 0;
        int diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
        if (diff < 0)
        {
            for (int j = DM.ROW_ID_A; j >= 0; j--)
            {
                if (pl.rowPlay[j] > 0 & diff + DM.ROW_MULTIPLIER[j] >= 0)
                {
                    imageRow = j;
                    imageCnt++;
                }
            }
            if (imageCnt == 1)
            {
                if (pl.cntEmpty >= 2)
                {
                    for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; j++)
                    {
                        if (pl.rowPlay[j] == 0 & diff + DM.ROW_MULTIPLIER[j] >= 0)
                        {
                            pl.rowPlay[j] = DM.ROW_MULTIPLIER[j];
                            pl.p1Action = DM.P1_PLAY_HOLD_BEST;
                            if (dm.isLogging)
                                Log.i(TAG, "col: " + pl.colId + ", playImage(), P1_PLAY_HOLD_BEST 1");
                            if (Math.abs(diff) >= DM.ROW_MULTIPLIER[imageRow])
                                pl.isPlayMore = true;
                            else
                                computePlayValues(pl, op);
                            return;
                        }
                    }
                }
                if (pl.rowPlay[imageRow] < DM.ROW_MULTIPLIER[imageRow] * 4)
                    pl.rowPlay[imageRow] = pl.rowPlay[imageRow] + DM.ROW_MULTIPLIER[imageRow];
                diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                if (diff < 0)
                {
                    for (int j = DM.ROW_ID_POKER; j >= DM.ROW_ID_STRAIGHT; j--)
                    {
                        if (pl.rowPlay[j] == DM.ROW_MULTIPLIER[j])
                        {
                            pl.rowPlay[j] = pl.rowPlay[j] + dm.bonusServed;
                            if (!dm.isDouble)
                                pl.isPlayLess = true;
                        }
                    }
                }
                diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                if (diff >= 0)
                {
                    if (pl.colRanking == dm.col)
                    {
                        pl.p1Action = DM.P1_PLAY_POSIBLE_WIN;
                        pl.isPlayMore = true;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playImage(), P1_PLAY_POSIBLE_WIN 2");
                    } else
                    {
                        pl.p1Action = DM.P1_PLAY_IMAGE;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playImage(), DM.P1_PLAY_IMAGE 1");
                    }
                    computePlayValues(pl, op);
                    if (dm.playerRoundesToGo <= dm.roundCheck4)
                        pl.isPlayLess = true;
                }
                else
                {
                    pl.p1Action = DM.P1_NON;
                    playSetValues(pl, op);
                    return;
                }
            }

            if (imageCnt > 1)
            {
                for (int j = DM.ROW_ID_A; j >= 0; j--)
                {
                    if (pl.rowPlay[j] > 0 & diff + DM.ROW_MULTIPLIER[j] >= 0)
                    {
                        if (pl.rowPlay[j] < DM.ROW_MULTIPLIER[j] * 4)
                            pl.rowPlay[j] = pl.rowPlay[j] + DM.ROW_MULTIPLIER[j];
                    }
                }
                if (!dm.isDouble)
                    pl.isPlayLess = true;
                if (pl.rowPlay[DM.ROW_ID_STRAIGHT] == 0)
                {
                    pl.rowPlay[DM.ROW_ID_STRAIGHT] = DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT];
                    pl.p1Action = DM.P1_PLAY_IMAGE_STRAIGHT;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playImage(), DM.P1_PLAY_IMAGE_STRAIGHT 1");
                }
                else
                {
                    pl.p1Action = DM.P1_PLAY_IMAGE;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playImage(), DM.P1_PLAY_IMAGE 2");
                }
            }
        }
    }

    private void playStraight(char playerId, Column pl, Column op, boolean isDiff)
    {

        if (pl.rowPlay[DM.ROW_ID_STRAIGHT] < 0)
            return;

        int ranking = pl.colRanking;
        if (dm.playerRoundesToGo <= dm.roundCheck8 & ranking > 1)
                ranking--;
        setRowPlay(pl, op, ranking, true);
        int diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);

        if  (       diff < 0 & pl.rowPlay[DM.ROW_ID_STRAIGHT] > 0 & pl.rowOpo[DM.ROW_ID_STRAIGHT] <= 0
                &   pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & pl.playCounter < 0
            )
            return;

        if (diff < 0 | isDiff)
        {
            if (dm.playerRoundesToGo >= dm.roundCheck8 & isBestColValue(playerId, pl) & pl.playCounter >= -1 & diff >= -5)
            {
                for (int j = DM.ROW_ID_POKER; j >= 0; j--)
                {
                    if (diff < 0)
                    {
                        if (j >= DM.ROW_ID_STRAIGHT)
                        {
                            if (pl.rowOpo[j] == DM.ROW_MULTIPLIER[j] + dm.bonusServed)
                            {
                                pl.rowOpo[j] = DM.ROW_MULTIPLIER[j];
                                diff = diff + dm.bonusServed;
                            }
                        }
                        else
                        {
                            if (pl.rowOpo[j] == DM.ROW_MULTIPLIER[j] * 4)
                            {
                                pl.rowOpo[j] = DM.ROW_MULTIPLIER[j] * 3;
                                diff = diff + DM.ROW_MULTIPLIER[j];
                            }
                        }
                    }
                }
                if (pl.colRanking == 3 & pl.cntEmpty >= 3 & dm.playerRoundesToGo <= dm.roundCheck20)
                    pl.isPlayLess = true;
                pl.p1Action = DM.P1_PLAY_HOLD;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playStraight(), DM.P1_PLAY_HOLD 8");
                computePlayValues(pl, op);
                return;
            }
            playSetValues(pl, op);
            diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
            if (pl.rowPlay[DM.ROW_ID_STRAIGHT] == 0 & pl.rowOpo[DM.ROW_ID_STRAIGHT] <= 0 & diff < 0)
            {
                int value = 0;
                int checkStraightServed = diff + DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT] + dm.bonusServed;
                int checkStraight = diff + DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT];
                if (checkStraightServed >= 0)
                    value = DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT] + dm.bonusServed;
                if (checkStraight >= 0)
                    value = DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT];
                if (value > 0)
                {
                    pl.rowPlay[DM.ROW_ID_STRAIGHT] = value;
                    pl.p1Action = DM.P1_PLAY_STRAIGHT;
                    if (!pl.isPlayLess & !pl.isPlayMore)
                        computePlayValues(pl, op);
                    diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
//Log.i(tag, "1 ??? col: " + pl.colId + ", playStraight(), diff: " + diff);
                    if (isBestColValue(playerId, pl) & pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & diff < 0)
                    {
                        pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                        if (pl.rowOpo[DM.ROW_ID_GRANDE] == 0)
                            pl.rowOpo[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                        pl.p1Action = DM.P1_PLAY_STRAIGHT_GRANDE;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playStraight(), DM.P1_PLAY_STRAIGHT_GRANDE 2");
                    }
                    else
                    {
                        if (pl.playCounter < 0)
                        {
                            if (pl.rowOpo[DM.ROW_ID_STRAIGHT] < 0 & pl.rowPlay[DM.ROW_ID_GRANDE] <= 0)
                            {
                                playSetValues(pl, op);
                                return;
                            }
                            pl.p1Action = DM.P1_PLAY_STRAIGHT;
                            if (dm.isLogging)
                                Log.i(TAG, "col: " + pl.colId + ", playStraight(), DM.P1_PLAY_STRAIGHT 1");
                        }
                        else
                        {
                            pl.p1Action = DM.P1_PLAY_STRAIGHT;
                            if (dm.isLogging)
                                Log.i(TAG, "col: " + pl.colId + ", playStraight(), DM.P1_PLAY_STRAIGHT 2");
                        }
                        if (pl.colRanking > 1 & pl.playCounter >= -1 & dm.playerRoundesToGo >= dm.roundCheck8)
                        {
                            pl.isPlayMore = true;
                            return;
                        }
                    }

                    if (!isBestColValue(playerId, pl))
                        computePlayValues(pl, op);

                }
            }
        }
    }

    private void playAttackCounter(Column pl, Column op)
    {
        playSetValues(pl, op);
        int cntAttack = 0;
        if (pl.rowPlay[DM.ROW_ID_STRAIGHT] == 0)
            pl.rowPlay[DM.ROW_ID_STRAIGHT] = DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT];
        for (int j = DM.ROW_ID_POKER; j >= DM.ROW_ID_J; j--)
        {
            if (pl.rowPlay[j] >= 0)
            {
                if (j >= DM.ROW_ID_STRAIGHT)
                {
                    if (pl.rowPlay[j] < DM.ROW_MULTIPLIER[j] + dm.bonusServed)
                    {
                        pl.rowPlay[j] = DM.ROW_MULTIPLIER[j] + dm.bonusServed;
                        cntAttack++;
                        break;
                    }
                }
                else
                {
                    if (pl.rowPlay[j] < DM.ROW_MULTIPLIER[j] * 4)
                    {
                        pl.rowPlay[j] = DM.ROW_MULTIPLIER[j] * 4;
                        cntAttack++;
                        break;
                    }
                }
            }
        }
        int diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
        boolean isPlayAttack = false;
        if (pl.colRanking > 1 | (pl.colRanking == 1 & pl.cntEmpty <= 4))
            isPlayAttack = true;
        if (pl.cntEmpty * 2.5 > dm.playerRoundesToGo)
            isPlayAttack = false;
        if (isPlayAttack & (pl.playCounter == -1 | pl.playCounter == -2) & diff >= -5)
        {
            pl.isPlayLess = false;
            for (int j = DM.ROW_ID_POKER; j >= DM.ROW_ID_J; j--)
            {
                if (pl.rowPlay[j] >= 0)
                {
                    if (j >= DM.ROW_ID_STRAIGHT)
                    {
                        if (pl.rowPlay[j] < DM.ROW_MULTIPLIER[j] + dm.bonusServed)
                        {
                            pl.rowPlay[j] = DM.ROW_MULTIPLIER[j] + dm.bonusServed;
                            pl.isPlayLess = true;
                        }
                    }
                    else
                    {
                        if (pl.rowPlay[j] < DM.ROW_MULTIPLIER[j] * 4)
                        {
                            pl.rowPlay[j] = DM.ROW_MULTIPLIER[j] * 4;
                            pl.isPlayLess = true;
                        }
                    }
                }
            }
            pl.p1Action = DM.P1_PLAY_MAX_VALUE;
            if (dm.isLogging)
                Log.i(TAG, "col: " + pl.colId + ", playAttackCounter(), DM.P1_PLAY_MAX_VALUE 2");
        }
        else
            playSetValues(pl, op);
    }

    private void playGrande(Column pl, Column op)
    {
        playSetValues(pl, op);

        if (pl.rowPlay[DM.ROW_ID_GRANDE] < 0)
            return;
        int diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
        if (pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & diff < 0)
        {
            int value = 0;
            int checkGrandeServed = diff + DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] + dm.bonusServedGrande;
            int checkGrande = diff + DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
            if (checkGrandeServed >= 0)
                value = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] + dm.bonusServed;
            if (checkGrande >= 0)
                value = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
            diff = diff + value;
            if (diff >= 0)
            {
                pl.rowPlay[DM.ROW_ID_GRANDE] = value;
                diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                if (pl.rowPlay[DM.ROW_ID_STRAIGHT] > 0 & diff > pl.rowPlay[DM.ROW_ID_STRAIGHT])
                    pl.rowPlay[DM.ROW_ID_STRAIGHT] = 0;
                if (value == DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE])
                {
                    if (pl.cntEmpty > 2 & !dm.isDouble)
                        pl.isPlayLess = true;
                    if (pl.rowOpo[DM.ROW_ID_GRANDE] == 0)
                    {
                        pl.p1Action = DM.P1_PLAY_GRANDE_WEAK;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrande(), DM.P1_PLAY_GRANDE_WEAK 1");
                    }
                    else
                    {
                        pl.p1Action = DM.P1_PLAY_GRANDE;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrande(), DM.P1_PLAY_GRANDE 1");
                    }
                    computePlayValues(pl, op);

                    int diffRow = dm.getRowSum(pl.row, null) - dm.getRowSum(op.row, null);
                    if (diffRow > 0)
                    {
                        int iRowPl = -1;
                        int iCntPl = 0;
                        int iCntOp = 0;
                        int iSumOp = 0;
                        for (int j = 0; j < DM.ROW_ID_STRAIGHT; j++)
                        {
                            if (pl.rowPlay[j] >= 0)
                            {
                                iCntPl++;
                                iRowPl = j;
                            }
                            if (pl.rowOpo[j] > 0)
                            {
                                iCntOp++;
                                iSumOp = iSumOp + pl.rowOpo[j];
                            }
                        }
                        if (iRowPl >= 0 & iCntPl == 1 & iCntOp > 0)
                        {
                            int tmpRowPlay = pl.rowPlay[iRowPl];
                            if (pl.rowPlay[iRowPl] + diffRow + DM.ROW_MULTIPLIER[iRowPl] >= iSumOp)
                                pl.rowPlay[iRowPl] = pl.rowPlay[iRowPl] + DM.ROW_MULTIPLIER[iRowPl];
                            else
                            {
                                if (pl.rowPlay[iRowPl] + diffRow + (DM.ROW_MULTIPLIER[iRowPl] * 2) >= iSumOp)
                                    pl.rowPlay[iRowPl] = pl.rowPlay[iRowPl] + DM.ROW_MULTIPLIER[iRowPl] * 2;
                                else
                                {
                                    if (pl.rowPlay[iRowPl] + diffRow + (DM.ROW_MULTIPLIER[iRowPl] * 3) >= iSumOp)
                                        pl.rowPlay[iRowPl] = pl.rowPlay[iRowPl] + DM.ROW_MULTIPLIER[iRowPl] * 3;
                                }
                            }
                            if (pl.rowPlay[iRowPl] >= DM.ROW_MULTIPLIER[iRowPl] * 4)
                                pl.rowPlay[iRowPl] = tmpRowPlay;
                        }
                    }
                }
                else
                {
                    pl.p1Action = DM.P1_PLAY_WORST;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playGrande(), DM.P1_PLAY_WORST 2");
                    computePlayValues(pl, op);
                }
            }
            else
            {
                pl.p1Action = DM.P1_PLAY_GIVE_IT_UP;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playGrande(), DM.P1_PLAY_GIVE_IT_UP 3");
                computePlayValues(pl, op);
            }
        }
    }

    private void playOpoEmpty1(char playerId, Column pl, Column op)
    {
        if (op.cntEmpty == 1)
        {
            int diffRow = dm.getRowSum(pl.row, null) - dm.getRowSum(op.row, null);
            int rowOpo = -1;
//Log.i(tag, "playOpoEmpty1(), diffRow: " + diffRow );
            if (diffRow < 0 & dm.playerRoundesToGo <= dm.roundCheck8)
            {
                for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; j++)
                {
                    if (pl.rowOpo[j] > 0 & pl.rowOpo[j] >= diffRow)
                    {
                        rowOpo = j;
                        break;
                    }
                }
                if (rowOpo >= 0)
                {
                    for (int j = 0; j < DM.ROW_ID_STRAIGHT; j++)
                    {
                        if (pl.rowPlay[j] > 0 & Math.abs(diffRow) <= DM.ROW_MULTIPLIER[j] * 3)
                        {
                            pl.rowOpo[rowOpo] = 0;
                            pl.rowPlay[j] = 0;
                            for (int k = 0; k < 3; k++)
                            {
                                if (diffRow < 0)
                                {
                                    pl.rowPlay[j] = pl.rowPlay[j] + DM.ROW_MULTIPLIER[j];
                                    diffRow = diffRow + DM.ROW_MULTIPLIER[j];
                                }
                                else
                                {
                                    int diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                                    if  (       pl.rowPlay[DM.ROW_ID_GRANDE] == 0
                                            &   Math.abs(diff) <= DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE]
                                            & dm.playerRoundesToGo >= dm.roundCheck4
                                        )
                                        pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                                    pl.p1Action = DM.P1_PLAY_ATTACK;
                                    if (dm.isLogging)
                                        Log.i(TAG, "col: " + pl.colId + ", playOpoEmpty1(), DM._P1_PLAY_ATTACK 2");
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void playEmpty1(char playerId, Column pl, Column op)
    {
        if (pl.cntEmpty > 2 | (pl.cntEmpty ==  2 & pl.row[DM.ROW_ID_GRANDE] >= 0))
            return;
        if  (pl.row[DM.ROW_ID_GRANDE] > 0 & pl.rowOpo[DM.ROW_ID_GRANDE] >= 0)
            return;
        if  (op.row[DM.ROW_ID_GRANDE] > 0 & pl.rowPlay[DM.ROW_ID_GRANDE] >= 0)
            return;

        int diffRow = dm.getRowSum(pl.row, null) - dm.getRowSum(op.row, null);
        int diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
//Log.i(tag, "1 ??? col: " + pl.colId + ", playEmpty1(), diffRow: " + diffRow + ", diff: " +diff);
        if (isBestColValue(playerId, pl) & pl.cntEmpty == 1 & pl.playCounter >= -1 & diff > 0)
        {
            int opBest = 0;
            for (int j = 0; j < DM.ROW_ID_GRANDE; j++)
            {
                if (pl.rowPlay[j] >= 0)
                {
                    if (pl.playCounter >= 1)
                    {
                        if (j < DM.ROW_ID_STRAIGHT)
                            pl.rowPlay[j] = DM.ROW_MULTIPLIER[j] * 3;
                        else
                            pl.rowPlay[j] = DM.ROW_MULTIPLIER[j];
                    }
                    else
                    {
                        if (j < DM.ROW_ID_STRAIGHT)
                            pl.rowPlay[j] = DM.ROW_MULTIPLIER[j] * 4;
                        else
                            pl.rowPlay[j] = DM.ROW_MULTIPLIER[j] + dm.bonusServed;
                    }
                }
                if (pl.rowOpo[j] >= 0)
                {
                    if (j < DM.ROW_ID_STRAIGHT)
                        opBest = opBest + (DM.ROW_MULTIPLIER[j] * 5) - pl.rowOpo[j];
                    else
                    {
                        opBest = opBest + DM.ROW_MULTIPLIER[j] + dm.bonusServed - pl.rowOpo[j];
                    }
                }
            }
            diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
            if (diff - opBest >= 0)
            {
                computePlayValues(pl, op);
                pl.p1Action = DM.P1_PLAY_WIN;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), DM.P1_PLAY_WIN 7");
                return;
            }
            else
            {
                pl.p1Action = DM.P1_PLAY_HOLD_BEST;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), DM.P1_PLAY_HOLD_BEST 2");
                return;
            }
        }
        if (diffRow > 0 & pl.cntEmpty == 1 & op.cntEmpty == 1)
        {
            int diffPlay = diffRow;
            boolean isImage = false;
            boolean isGrande = false;
            for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
            {
                if (pl.rowPlay[j] >= 0)
                {
                    if (j == DM.ROW_ID_GRANDE)
                        isGrande = true;
                    pl.rowPlay[j] = DM.ROW_MULTIPLIER[j];
                    if (j < DM.ROW_ID_STRAIGHT)
                    {
                        pl.rowPlay[j] = pl.rowPlay[j] * 4;
                        isImage = true;
                    }
                    diffPlay = diffPlay + pl.rowPlay[j];
                }
                if (pl.rowOpo[j] >= 0)
                {
                    pl.rowOpo[j] = DM.ROW_MULTIPLIER[j];
                    if (j < DM.ROW_ID_STRAIGHT)
                        pl.rowOpo[j] = pl.rowOpo[j] * 3;
                    diffPlay = diffPlay - pl.rowOpo[j];
                    if (diffRow - pl.rowOpo[j] >= 0)
                        diffPlay = -1;
                }
            }
            if (diffPlay >= 0)
            {
                if (isGrande)
                {
                    pl.rowPlay[DM.ROW_ID_GRANDE] = 0;
                    pl.p1Action = DM.P1_PLAY_WEAK;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), DM.P1_PLAY_WEAK 2");
                    return;
                }
                else
                {
                    if (dm.playerRoundesToGo <= dm.roundCheck8 & pl.rowOpo[DM.ROW_ID_GRANDE] > 0)
                    {
                        pl.rowOpo[DM.ROW_ID_GRANDE] = 0;
                        pl.p1Action = DM.P1_PLAY_WON_OR_LOST;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), DM.P1_PLAY_WON_OR_LOST 2");
                        computePlayValues(pl, op);
                    }
                    else
                    {
                        pl.p1Action = DM.P1_PLAY_WIN;
                        if (isImage)
                            computePlayValues(pl, op);
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), P1_PLAY_WIN 5");
                    }
                    return;
                }
            }
            else
                playSetValues(pl, op);
        }
        if (op.cntEmpty == 0)
        {
            if (diff >= 0 & pl.rowPlay[DM.ROW_ID_GRANDE] < 0)
            {
                pl.p1Action = DM.P1_PLAY_WIN;
                computePlayValues(pl, op);
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), P1_PLAY_WIN 4");
                return;
            }
        }
        if (pl.cntEmpty ==  1 & pl.rowPlay[DM.ROW_ID_GRANDE] >= 0)
        {
            if (op.cntEmpty == 0 & diffRow < 0 & diffRow + DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] < 0)
            {
                if (diffRow + DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] + dm.bonusServedGrande >= 0)
                {
                    pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] + dm.bonusServedGrande;
                    pl.p1Action = DM.P1_PLAY_WORST;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), DM.P1_PLAY_WORST 8");
                }
            }
            return;
        }
        if (diffRow >= 0)
        {
            if  (       pl.rowOpo[DM.ROW_ID_GRANDE] == 0 & diff - DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] >= 0
                    &   dm.playerRoundesToGo >= dm.roundCheck8
                )
            {
                pl.rowOpo[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                diff = diff - DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
            }

            if (diff == 0 & pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & pl.rowOpo[DM.ROW_ID_GRANDE] == 0)
            {
                if  (dm.playerRoundesToGo >= dm.roundCheck6)
                    pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                pl.p1Action = DM.P1_PLAY_GRANDE_BEST;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), DM.P1_PLAY_GRANDE_BEST 1");
                return;
            }
//Log.i(tag, "col: " + pl.colId + ", diff: " + diff);
            if (diff >= 0)
            {
                for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; j++)
                {
                    if (diff >= DM.ROW_MULTIPLIER[j] & pl.rowOpo[j] == 0)
                    {
                        pl.rowOpo[j] = DM.ROW_MULTIPLIER[j];
                        diff = diff - DM.ROW_MULTIPLIER[j];
                    }
                }
                pl.p1Action = DM.P1_PLAY_HOLD;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), DM.P1_PLAY_HOLD 1");
                computePlayValues(pl, op);
                return;
            }
            else
            {
                if (pl.rowPlay[DM.ROW_ID_STRAIGHT] == 0 & diff + DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT] >= 0)
                    pl.rowPlay[DM.ROW_ID_STRAIGHT] = DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT];
                if (pl.rowPlay[DM.ROW_ID_STRAIGHT] == 0 & diff + DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT] + dm.bonusServed >= 0)
                    pl.rowPlay[DM.ROW_ID_STRAIGHT] = DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT]  + dm.bonusServed;
                diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                if (diff >= 0)
                {
                    pl.p1Action = DM.P1_PLAY_ATTACK;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), DM.P1_PLAY_ATTACK 3");
                }
                else
                {
                    if (pl.cntEmpty == 1 & pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & diff + DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] >= 0)
                        pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                    if (pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & diff + DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] >= 0)
                        pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                    pl.p1Action = DM.P1_PLAY_HOLD_WEAK;
                    computePlayValues(pl, op);
                    for (int j = 0; j < DM.ROW_ID_GRANDE; j++)
                    {
                        if (pl.rowPlay[j] == 0)
                        {
                            pl.p1Action = DM.P1_PLAY_VALUE_0;
                            if (dm.isLogging)
                                Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), DM.P1_PLAY_VALUE_0 1");
                            return;
                        }
                    }
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), DM.P1_PLAY_HOLD_WEAK 1");
                }

            }
        }
        else
        {
            int imageOpo = 0;
            for (int j = 0; j < DM.ROW_ID_GRANDE; j++)
            {
                if (pl.rowOpo[j] > 0)
                    imageOpo = imageOpo + pl.rowOpo[j];
            }
            for (int j = 0; j < DM.ROW_ID_STRAIGHT; j++)
            {
                if (pl.rowPlay[j] >= 0 & diffRow + DM.ROW_MULTIPLIER[j] * 5 >= 0)
                {
                    int imageAttack = DM.ROW_MULTIPLIER[j] * 2;
                    if (diffRow + imageAttack < 0)
                        imageAttack = DM.ROW_MULTIPLIER[j] * 3;
                    if (diffRow + imageAttack - imageOpo >= 0)
                    {
                        pl.rowPlay[j] = imageAttack;
                        pl.p1Action = DM.P1_PLAY_WIN_ATTACK;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), DM.P1_PLAY_WIN_ATTACK 4");
                        computePlayValues(pl, op);
                        return;
                    }
                    if (diffRow + (DM.ROW_MULTIPLIER[j] * 4) - imageOpo >= 0)
                    {
                        pl.rowPlay[j] = DM.ROW_MULTIPLIER[j] * 4;
                        pl.p1Action = DM.P1_PLAY_WIN;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), P1_PLAY_WIN 7");
                        computePlayValues(pl, op);
                        return;
                    }
                    if (pl.rowPlay[DM.ROW_ID_GRANDE] == 0)
                    {
                        pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                        pl.p1Action = DM.P1_PLAY_GRANDE;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), DM.P1_PLAY_GRANDE 2");
                        computePlayValues(pl, op);
                        return;
                    }
                    else
                    {
                        pl.rowPlay[j] = DM.ROW_MULTIPLIER[j] * 5;
                        pl.p1Action = DM.P1_PLAY_WORST;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), DM.P1_PLAY_WORST 7");
                        computePlayValues(pl, op);
                        return;
                    }
                }
            }
            for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; j++)
            {
                int value = DM.ROW_MULTIPLIER[j];
                if (diffRow + value < 0)
                    value = DM.ROW_MULTIPLIER[j] + dm.bonusServed;
                if (pl.rowPlay[j] >= 0 & diffRow + value >= 0)
                {
                    pl.rowPlay[j] = value;
                    pl.p1Action = DM.P1_PLAY_WIN;
                    computePlayValues(pl, op);
                    diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
//Log.i(tag, "1 ??? col: " + pl.colId + ", playEmpty1(), diff: " + diff);
                    if (diff < 0)
                    {
                        for (int k = DM.ROW_ID_STRAIGHT; k < DM.ROW_ID_GRANDE; k++)
                        {
                            if (diff < 0 & pl.rowOpo[k] > 0 & diff + pl.rowOpo[k] >= 0)
                            {
                                diff = diff + pl.rowOpo[k];
                                pl.rowOpo[k] = 0;
                            }
                        }
                    }
                    if (diff >= 0)
                    {
                        pl.isPlayMore = true;
                        if (diffRow < 0)
                        {
                            pl.p1Action = DM.P1_PLAY_WIN_ATTACK;
                            if (dm.isLogging)
                                Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), P1_PLAY_WIN_ATTACK 1");
                        }
                        else
                        {
                            if (dm.isLogging)
                                Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), P1_PLAY_WIN 1");
                        }
                        return;
                    }
                    else
                    {
                        pl.p1Action = DM.P1_NON;
                    }

                }
            }

            playSetValues(pl, op);

            if (pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & diffRow + DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] >= 0)
            {
                diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
//Log.i(tag, "1 ??? col: " + pl.colId + ", playEmpty1(), diff: " + diff);
                pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                if (diff < 0 & Math.abs(diff) <= dm.bonusServed)
                {
                    for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; j++)
                    {
                        if (diff < 0 & pl.rowPlay[j] == DM.ROW_MULTIPLIER[j])
                        {
                            pl.rowPlay[j] = DM.ROW_MULTIPLIER[j] + dm.bonusServed;
                            diff = diff + dm.bonusServed;
                        }
                    }
                    if (diff >= 0)
                    {
                        pl.p1Action = DM.P1_PLAY_WIN_ATTACK;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), P1_PLAY_WIN_ATTACK 3");
                        return;
                    }
                }
                pl.p1Action = DM.P1_PLAY_HOLD_WEAK;
                computePlayValues(pl, op);
                diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                if (diff >= 0)
                {
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), DM.P1_PLAY_HOLD_WEAK 3");
                }
                else
                {
                    pl.p1Action = DM.P1_PLAY_GIVE_IT_UP;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playEmpty1(), DM.P1_PLAY_GIVE_IT_UP 4");
                }
            }
            else
            {

            }
        }
    }

    private void playP3(char playerId, Column pl, Column op)
    {
        boolean isPlayP3 = true;
        if (dm.player.isP1(playerId))
            isPlayP3 = false;
        if (dm.playerRoundesToGo <= dm.roundCheck12)
            isPlayP3 = false;
        if (pl.row[DM.ROW_ID_GRANDE] > 0 | op.row[DM.ROW_ID_GRANDE] > 0)
            isPlayP3 = false;
        if (pl.row[DM.ROW_ID_GRANDE] !=op.row[DM.ROW_ID_GRANDE])
            isPlayP3 = false;
        if ((pl.row[DM.ROW_ID_STRAIGHT] > 0 | op.row[DM.ROW_ID_STRAIGHT] > 0) & pl.row[DM.ROW_ID_STRAIGHT] != op.row[DM.ROW_ID_STRAIGHT])
            isPlayP3 = false;
        if (isBestColValue(playerId, pl))
            isPlayP3 = false;
        if (!isPlayP3)
            return;
        int ranking = pl.colRanking;
        int counterCheck = 4;
        if (dm.isDouble)
            counterCheck = 6;
        if (pl.playCounter >= counterCheck & pl.colRanking > 1)
            ranking = pl.colRanking -1;
        setRowPlay(pl, op, ranking, true);

        pl.isP1 = true;
        int diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
//Log.i(tag, "1 999 col: " + pl.colId + ", playP3(), diff: " + diff);
        if (diff < 0 & pl.playCounter == -1)
        {
            boolean isPlayImage = false;
            for (int j = 0; j < DM.ROW_ID_STRAIGHT; j++)
            {
                if (pl.rowPlay[j] >= 0 & pl.rowPlay[j] <= DM.ROW_MULTIPLIER[j] * 3 & Math.abs(diff) <= DM.ROW_MULTIPLIER[j])
                    isPlayImage = true;
            }
            if (isPlayImage)
            {
                pl.isPlayMore = true;
                if (pl.rowPlay[DM.ROW_ID_STRAIGHT] > 0)
                {
                    pl.p1Action = DM.P1_PLAY_IMAGE_STRAIGHT;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playP3(), DM.P1_PLAY_IMAGE_STRAIGHT 2");
                    if (pl.rowOpo[DM.ROW_ID_STRAIGHT] > 0)
                    {
                        pl.rowOpo[DM.ROW_ID_STRAIGHT] = 0;
                        computePlayValues(pl, op);
                    }
                }
                else
                {
                    pl.p1Action = DM.P1_PLAY_IMAGE;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playP3(), DM.P1_PLAY_IMAGE 3");
                }
                return;
            }
        }
        if (pl.rowPlay[DM.ROW_ID_STRAIGHT] > 0 & pl.rowOpo[DM.ROW_ID_STRAIGHT] > 0 & diff < 0 & diff + pl.rowOpo[DM.ROW_ID_STRAIGHT] >= 0)
        {
            pl.rowOpo[DM.ROW_ID_STRAIGHT] = 0;
            if  (       (
                                pl.rowPlay[DM.ROW_ID_GRANDE] < 0 & pl.rowOpo[DM.ROW_ID_GRANDE] < 0
                            |   pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & pl.rowOpo[DM.ROW_ID_GRANDE] == 0
                        )
                    & dm.playerRoundesToGo >= dm.roundCheck12
                )
            {
                pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                pl.rowOpo[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                pl.p1Action = DM.P1_PLAY_STRAIGHT_GRANDE;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playP3(), DM.P1_PLAY_STRAIGHT_GRANDE 5");
            }
            else
            {
                pl.p1Action = DM.P1_PLAY_STRAIGHT;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playP3(), DM.P1_PLAY_STRAIGHT 4");
            }
            computePlayValues(pl, op);
            return;
        }
        else
        {
            if (diff < 0 & (dm.playerRoundesToGo <= dm.roundCheck12 | pl.playCounter < -1))
            {
                pl.p1Action = DM.P1_NON;
                pl.isP1 = true;
                playSetValues(pl, op);
                return;
            }
            if (pl.rowPlay[DM.ROW_ID_GRANDE] >= 0 & pl.colRanking == 1 & diff > 0 & diff - pl.rowPlay[DM.ROW_ID_GRANDE] > 0)
                pl.rowPlay[DM.ROW_ID_GRANDE] = 0;
            pl.p1Action = DM.P1_PLAY_P3 + pl.colRanking;
            pl.isP1 = true;
            if (pl.colId == 0)
                pl.isP1 = false;
            if (dm.playerRoundesToGo <= dm.roundCheck12)
                pl.isPlayLess = true;
            else
            {
                if (pl.playCounter == 0 | pl.playCounter == -1)
                    pl.isPlayMore = true;
            }
            computePlayValues(pl, op);
            diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
            if (diff < 0)
            {
                if (diff < DM.ROW_ID_A * -1 & pl.cntEmpty <= 4)
                {
                    pl.p1Action = DM.P1_NON;
                    pl.isP1 = true;
                    playSetValues(pl, op);
                    return;
                }
                else
                {
                    for (int j = DM.ROW_ID_A; j >= DM.ROW_ID_9; j--)
                    {
                        if (diff < 0 & pl.rowPlay[j] == DM.ROW_MULTIPLIER[j] * 3)
                        {
                            pl.rowPlay[j] = pl.rowPlay[j] + DM.ROW_MULTIPLIER[j];
                            diff = diff + DM.ROW_MULTIPLIER[j];
                        }
                    }
                    for (int j = DM.ROW_ID_POKER; j >= DM.ROW_ID_STRAIGHT; j--)
                    {
                        if (diff < 0 & pl.rowOpo[j] > DM.ROW_MULTIPLIER[j])
                        {
                            diff = diff + pl.rowOpo[j] - DM.ROW_MULTIPLIER[j];
                            pl.rowOpo[j] = DM.ROW_MULTIPLIER[j];
                            break;
                        }
                    }
                    for (int j = 0; j < DM.ROW_ID_STRAIGHT; j++)
                    {
                        if (diff < 0 & pl.rowOpo[j] > DM.ROW_MULTIPLIER[j])
                        {
                            pl.rowOpo[j] = pl.rowOpo[j] - DM.ROW_MULTIPLIER[j];
                            diff = diff + DM.ROW_MULTIPLIER[j];
                        }
                    }
                    if (diff >= 0)
                    {
                        pl.p1Action = DM.P1_PLAY_ATTACK;
                        pl.isPlayMore = false;
                        pl.isPlayLess = true;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playP3(), DM.P1_PLAY_ATTACK 4");
                    }
                    else
                    {
                        pl.p1Action = DM.P1_NON;
                        pl.isP1 = true;
                        playSetValues(pl, op);
                        return;
                    }
                }
            }
            else
            {
                diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                pl.isPlayMore = false;
                pl.isPlayLess = false;
                pl.p1Action = DM.P1_PLAY_P3 + pl.colRanking;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playP3(), pl.p1Action: " + pl.p1Action);
                if (diff > 0)
                    computePlayValues(pl, op);
            }
        }
        if (dm.playerRoundesToGo > dm.roundCheck12 & pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & pl.rowOpo[DM.ROW_ID_GRANDE] == 0)
        {
            pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
            pl.rowOpo[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
        }
    }

    private void playGrandeHold(char playerId, Column pl, Column op)
    {
        // player: grande, oponent: no grande
        if (pl.row[DM.ROW_ID_GRANDE] <= 0 | op.row[DM.ROW_ID_GRANDE] >= 0)
            return;
        int plRow[] = pl.row.clone();
        int opRow[] = op.row.clone();
        for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
        {
            if (j < DM.ROW_ID_STRAIGHT)
            {
                if (plRow[j] < 0)
                    plRow[j] = DM.ROW_MULTIPLIER[j] * 4;
                if (opRow[j] < 0)
                    opRow[j] = DM.ROW_MULTIPLIER[j] * 4;
            }
            else
            {
                if (plRow[j] < 0)
                {
                    plRow[j] = DM.ROW_MULTIPLIER[j];
                    if (j < DM.ROW_ID_GRANDE & dm.playerRoundesToGo >= dm.roundCheck8)
                        plRow[j] = plRow[j] + dm.bonusServed;
                }
                if (opRow[j] < 0)
                {
                    opRow[j] = DM.ROW_MULTIPLIER[j];
                    if (j < DM.ROW_ID_GRANDE & dm.playerRoundesToGo >= dm.roundCheck8)
                        opRow[j] = opRow[j] + dm.bonusServed;
                }
            }
        }
        int sumDiff = dm.getRowSum(plRow, null) - dm.getRowSum(opRow, null);
        int diffPlOp = dm.getRowSum(pl.row, null) - dm.getRowSum(opRow, null);
        if (diffPlOp > 0)
        {
            pl.p1Action = DM.P1_PLAY_GIVE_IT_UP;
            if (dm.isLogging)
                Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), DM.P1_PLAY_GIVE_IT_UP 12");
            computePlayValues(pl, op);
            return;
        }

        int diffRow = dm.getRowSum(pl.row, null) - dm.getRowSum(op.row, null);
        if (diffRow <= 0 & pl.cntEmpty == 1 & op.cntEmpty == 1 & dm.playerRoundesToGo <= dm.roundCheck8)
        {
            boolean isWin = false;
            for (int j = 0; j < DM.ROW_ID_GRANDE; j++)
            {
                if (j < DM.ROW_ID_STRAIGHT)
                {
                    if (pl.row[j] == -1 & diffRow + (DM.ROW_MULTIPLIER[j] * 3) >= 0)
                    {
                        pl.rowPlay[j] = DM.ROW_MULTIPLIER[j] * 3;
                        isWin = true;
                    }
                }
                if (j >= DM.ROW_ID_STRAIGHT & pl.row[j] == -1)
                {
                    if (diffRow + DM.ROW_MULTIPLIER[j] >= 0)
                    {
                        pl.rowPlay[j] = DM.ROW_MULTIPLIER[j];
                        isWin = true;
                    }
                    if (j == DM.ROW_ID_STRAIGHT & pl.rowOpo[j] == -1)
                    {
                        pl.rowPlay[j] = 0;
                        pl.rowOpo[j] = 0;
                    }
                }
            }
            if (isWin)
            {
                pl.p1Action = DM.P1_PLAY_WIN_ATTACK;
                if (diffRow < 0)
                    computePlayValues(pl, op);
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), P1_PLAY_WIN_ATTACK 2");
                return;
            }
        }
        int counterCheck = 5;
        if (dm.isDouble)
            counterCheck = 7;
        if (pl.cntEmpty <= 5)
            counterCheck--;
        int counter = pl.playCounter;
        int check = dm.roundCheck8;
        if (pl.cntEmpty <= 3 & counter <= counterCheck)
        {
            if (sumDiff >= 0)
            {
                boolean isHoldGrandePlay = false;
                boolean isHoldGrande = true;
                for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; j++)
                {
                    if (pl.row[j] < 0 & diffRow < DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE]
                            & diffRow + DM.ROW_MULTIPLIER[j] >= DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE])
                    {
                        isHoldGrandePlay = true;
                        plRow[j] = DM.ROW_MULTIPLIER[j];
                        opRow[j] = DM.ROW_MULTIPLIER[j];
                        if (dm.playerRoundesToGo <= dm.roundCheck8)
                        {
                            plRow[j] = 0;
                            opRow[j] = 0;
                            isHoldGrande = false;
                        }
                    }
                    if (pl.row[j] < 0)
                        pl.rowPlay[j] = plRow[j];
                    if (op.row[j] < 0)
                        pl.rowOpo[j] = opRow[j];
                    if (isHoldGrandePlay)
                        break;
                }
                if (isHoldGrandePlay)
                {
                    if (isHoldGrande)
                    {
                        pl.p1Action = DM.P1_PLAY_GRANDE_HOLD;
                        computePlayValues(pl, op);
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), DM.P1_PLAY_GRANDE_HOLD 6");
                    }
                    else
                    {
                        pl.p1Action = DM.P1_PLAY_GRANDE_HOLD_WEAK;
                        computePlayValues(pl, op);
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), DM.P1_PLAY_GRANDE_HOLD_WEAK 3");
                    }
                    return;
                }

                for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
                {
                    if (j >= DM.ROW_ID_STRAIGHT & sumDiff >= plRow[j])
                    {
                        sumDiff = sumDiff - plRow[j];
                        plRow[j] = 0;
                    }
                    if (j == DM.ROW_ID_STRAIGHT & pl.row[j] < 0 & op.row[j] < 0)
                    {
                        if (dm.playerRoundesToGo <= dm.roundCheck8)
                        {
                            plRow[j] = DM.ROW_MULTIPLIER[j];
                            opRow[j] = DM.ROW_MULTIPLIER[j];
                        }
                        if (dm.playerRoundesToGo <= dm.roundCheck4)
                        {
                            plRow[j] = 0;
                            opRow[j] = 0;
                        }
                    }
                    if (pl.row[j] < 0)
                        pl.rowPlay[j] = plRow[j];
                    if (op.row[j] < 0)
                        pl.rowOpo[j] = opRow[j];
                }
                pl.p1Action = DM.P1_PLAY_GRANDE_HOLD;
                computePlayValues(pl, op);
                if (pl.cntEmpty >= 2)
                    pl.isPlayLess = true;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), DM.P1_PLAY_GRANDE_HOLD 7");
                return;
            }
            else
            {
                opRow[DM.ROW_ID_GRANDE] = 0;
                sumDiff = sumDiff + DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                for (int j = 0; j < DM.ROW_ID_GRANDE; j++)
                {
                    if (j >= DM.ROW_ID_STRAIGHT & sumDiff >= plRow[j])
                    {
                        sumDiff = sumDiff - plRow[j];
                        plRow[j] = 0;
                    }
                    if (pl.row[j] < 0)
                        pl.rowPlay[j] = plRow[j];
                    if (op.row[j] < 0)
                        pl.rowOpo[j] = opRow[j];
                }
                pl.p1Action = DM.P1_PLAY_GRANDE_HOLD_WEAK;
                computePlayValues(pl, op);
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), P1_PLAY_GRANDE_HOLD_WEAK 2");
                return;
            }
        }

        if (dm.isDouble)
            check = dm.roundCheck4;
        if (dm.playerRoundesToGo > check)
            pl.rowOpo[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
        int diff;
        if (counter > counterCheck)
        {
            int rowP[];
            int ranking = pl.colRanking;
            if (ranking > 1)
                ranking--;
            if (!dm.isDouble)
            {
                if (ranking == 1)
                    rowP = DM.ROW_P3_COL_WORST;
                else
                    rowP = DM.ROW_P3_COL_CENTER;
            }
            else
            {
                if (ranking == 1)
                    rowP = DM.ROW_P3_DOUBLE_COL_WORST;
                else
                    rowP = DM.ROW_P3_DOUBLE_COL_CENTER;
            }
            for (int j = 0; j < DM.ROW_ID_GRANDE; j++)
            {
                if (pl.row[j] == -1)
                    pl.rowPlay[j] = rowP[j];
                else
                    pl.rowPlay[j] = -1;
                if (op.row[j] == -1)
                    pl.rowOpo[j] = rowP[j];
                else
                    pl.rowOpo[j] = -1;
            }
            diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
            if (diff >= 0)
            {
                pl.isP1 = true;
                if (pl.colId == 0)
                    pl.isP1 = false;
                if (pl.rowOpo[DM.ROW_ID_GRANDE] == 0 & diff - DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] >= 0)
                {
                    pl.rowOpo[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                    pl.p1Action = DM.P1_PLAY_GRANDE_HOLD;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), DM.P1_PLAY_GRANDE_HOLD 4");
                }
                else
                {
                    pl.p1Action = DM.P1_PLAY_HOLD;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), DM.P1_PLAY_HOLD 7");
                }
                if (pl.rowPlay[DM.ROW_ID_STRAIGHT] == pl.rowOpo[DM.ROW_ID_STRAIGHT]
                        & pl.rowPlay[DM.ROW_ID_STRAIGHT] > 0 & pl.rowOpo[DM.ROW_ID_STRAIGHT] > 0)
                {
                    pl.rowPlay[DM.ROW_ID_STRAIGHT] = 0;
                    pl.rowOpo[DM.ROW_ID_STRAIGHT] = 0;
                }
                if (diff > 0)
                    computePlayValues(pl, op);

//                pl.isPlayMore = true;
                if (pl.colRanking == 3 & pl.cntEmpty >= 3 & dm.playerRoundesToGo <= dm.roundCheck16)
                    pl.isPlayLess = true;

                return;
            }
        }
        else
        {
            int ranking = pl.colRanking;
            setRowPlay(pl, op, ranking, true);
            diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
            if (diff >= 0 | (isBestColValue(playerId, pl) & diff >= -6))
            {
                int roundCheck = dm.roundCheck8;
                if (dm.playerRoundesToGo >= roundCheck | (dm.playerRoundesToGo <= (roundCheck * 2) & pl.cntEmpty > (dm.playerRoundesToGo / 3)))
                {
                    setRowPlay(pl, op, ranking, true);
                    diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                    if (diff < 0)
                    {
                        if  (       pl.rowPlay[DM.ROW_ID_STRAIGHT] > 0 & pl.rowOpo[DM.ROW_ID_STRAIGHT] > 0
                                &   diff + DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT] >= 0
                            )
                            pl.rowOpo[DM.ROW_ID_STRAIGHT] = 0;
                        else
                        {
                            if (pl.rowOpo[DM.ROW_ID_GRANDE] > 0
                                    & diff + DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] >= 0
                                    )
                                pl.rowOpo[DM.ROW_ID_GRANDE] = 0;
                        }

                        if (isBestColValue(playerId, pl) & (pl.rowOpo[DM.ROW_ID_STRAIGHT] == 0 | pl.rowOpo[DM.ROW_ID_GRANDE] == 0))
                            pl.isPlayLess = true;

                    }
                    diffRow = dm.getRowSum(pl.row, null) - dm.getRowSum(op.row, null);
                    if (diffRow < 0)
                    {
                        pl.p1Action = DM.P1_PLAY_GRANDE_HOLD;
                        if (pl.rowOpo[DM.ROW_ID_GRANDE] == 0 & op.cntEmpty == 1)
                        {
                            boolean isImageBetter = false;
                            boolean isWin = false;
                            for (int j = 0; j < DM.ROW_ID_GRANDE; j++)
                            {
                                if (j < DM.ROW_ID_STRAIGHT)
                                {
                                    if (pl.row[j] == -1 & diffRow + (DM.ROW_MULTIPLIER[j] * 3) >= 0)
                                    {
                                        pl.rowPlay[j] = DM.ROW_MULTIPLIER[j] * 3;
                                        isImageBetter = true;
                                    }
                                }
                                if (j >= DM.ROW_ID_STRAIGHT & pl.row[j] == -1)
                                {
                                    if (isImageBetter)
                                        pl.rowPlay[j] = 0;
                                    else
                                    {
                                      if (diffRow + DM.ROW_MULTIPLIER[j] >= 0)
                                      {
                                          pl.rowPlay[j] = DM.ROW_MULTIPLIER[j];
                                          isWin = true;
                                      }
                                    }
                                }
                            }
                            if (isWin | isImageBetter)
                            {
                                pl.p1Action = DM.P1_PLAY_WIN;
                                computePlayValues(pl, op);
                                if (dm.isLogging)
                                    Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), P1_PLAY_WIN 2");
                                return;
                            }
                        }
//Log.i(tag, "1 ??? col: " + pl.colId + ", playGrandeHold(), diff: " + diff + ", diffRow: " + diffRow);
                        int checkDif = diff;
                        if (checkDif >= DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] + dm.bonusServed & dm.playerRoundesToGo <= dm.roundCheck16)
                        {
                            if (pl.rowOpo[DM.ROW_ID_GRANDE] == 0)
                            {
                                pl.rowOpo[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                                checkDif = checkDif - DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                            }
                            for (int j = DM.ROW_ID_POKER; j >= DM.ROW_ID_STRAIGHT; j--)
                            {
                                if (pl.rowPlay[j] > DM.ROW_MULTIPLIER[j] & checkDif >= dm.bonusServed)
                                {
                                    pl.rowPlay[j] = pl.rowPlay[j] - dm.bonusServed;
                                    checkDif = checkDif - dm.bonusServed;
                                }
                            }
                        }
                        computePlayValues(pl, op);

                        if (pl.colRanking == 3 & pl.cntEmpty >= 3 & dm.playerRoundesToGo <= dm.roundCheck16)
                            pl.isPlayLess = true;

                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), DM.P1_PLAY_GRANDE_HOLD 2");
                        return;
                    }
                }
                else
                {
                    int checkCounterSlow = 5;
                    if (dm.isDouble)
                        checkCounterSlow = 6;
                    if (isBestColValue(playerId, pl))
                        checkCounterSlow = checkCounterSlow + 2;
                    if (dm.playerRoundesToGo <= dm.roundCheck8 | pl.playCounter >= checkCounterSlow)
                    {
                        pl.isPlayLess = true;
                        pl.p1Action = DM.P1_PLAY_GRANDE_HOLD_WEAK;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), P1_PLAY_GRANDE_HOLD_WEAK 1");
                    }
                    else
                    {
                        pl.p1Action = DM.P1_PLAY_GRANDE_HOLD;
                        if (pl.colRanking == 3 & pl.cntEmpty >= 3 & dm.playerRoundesToGo <= dm.roundCheck16)
                            pl.isPlayLess = true;

                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), P1_PLAY_GRANDE_HOLD 1");
                    }
                    computePlayValues(pl, op);
                    return;
                }

            }
            else
            {
                setRowPlay(pl, op, ranking, true);
                diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                if (pl.rowPlay[DM.ROW_ID_STRAIGHT] > 0 & pl.rowOpo[DM.ROW_ID_STRAIGHT] > 0 & diff + DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT] >= 0)
                {
                    pl.rowOpo[DM.ROW_ID_STRAIGHT] = 0;
                    pl.p1Action = DM.P1_PLAY_GRANDE_BEST;
                    if (pl.cntEmpty > 2 & !dm.isDouble)
                        pl.isPlayLess = true;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), DM.P1_PLAY_GRANDE_BEST 2");
                    computePlayValues(pl, op);
                    return;
                }

                if (diff < 0 & pl.rowOpo[DM.ROW_ID_GRANDE] > 0)
                {
                    if  (       (pl.playCounter >= 2 & diff >= -5 & dm.playerRoundesToGo > dm.roundCheck6)
                            |   (pl.playCounter >= 1 & diff >= -10 & dm.playerRoundesToGo > dm.roundCheck16)

                        )
                    {
                        if (pl.colRanking == 3 & pl.cntEmpty >= 3 & dm.playerRoundesToGo <= dm.roundCheck16)
                            pl.isPlayLess = true;
                        pl.p1Action = DM.P1_PLAY_HOLD;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), DM.P1_PLAY_HOLD 2");
                        return;
                    }
                    if (diff + dm.bonusServed >= 0)
                    {
                        pl.rowPlay[DM.ROW_ID_STRAIGHT] = pl.rowPlay[DM.ROW_ID_STRAIGHT] + dm.bonusServed;
                        pl.p1Action = DM.P1_PLAY_HOLD_WEAK;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), DM.P1_PLAY_HOLD_WEAK 6");
                        computePlayValues(pl, op);
                        return;
                    }

                    if (pl.rowOpo[DM.ROW_ID_STRAIGHT] >= 0)
                    {
                        pl.rowOpo[DM.ROW_ID_STRAIGHT] = 0;
                        diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                        if (diff >= 0)
                        {
                            pl.p1Action = DM.P1_PLAY_HOLD_WEAK;
                            if (dm.isLogging)
                                Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), DM.P1_PLAY_HOLD_WEAK 2");
                            computePlayValues(pl, op);
                            return;
                        }
                    }

                    pl.rowOpo[DM.ROW_ID_GRANDE] = 0;
                    diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                    if (pl.rowPlay[DM.ROW_ID_STRAIGHT] > 0 & diff >= pl.rowPlay[DM.ROW_ID_STRAIGHT])
                    {
                        pl.rowPlay[DM.ROW_ID_STRAIGHT] = 0;
                        diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                    }
                    if (diff >= 0)
                    {
                        pl.p1Action = DM.P1_PLAY_GRANDE_HOLD;
                        computePlayValues(pl, op);
                        if (pl.colRanking == 3 & pl.cntEmpty >= 3 & dm.playerRoundesToGo <= dm.roundCheck16)
                            pl.isPlayLess = true;

                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), DM.P1_PLAY_GRANDE_HOLD 3");
                        return;
                    }
                    else
                    {
                        pl.p1Action = DM.P1_PLAY_GIVE_IT_UP;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), DM.P1_PLAY_GIVE_IT_UP 5");
                        computePlayValues(pl, op);
                        return;
                    }
                }
            }
        }
        diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
        pl.isPlayLess = false;
        if (diff >= 0 & pl.p1Action == DM.P1_NON)
        {
            if (dm.playerRoundesToGo <= dm.roundCheck12 & pl.rowPlay[DM.ROW_ID_STRAIGHT] < 0 & pl.rowOpo[DM.ROW_ID_STRAIGHT] > 0)
            {
                pl.rowOpo[DM.ROW_ID_STRAIGHT] = 0;
                for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; j++)
                {
                    if (pl.rowPlay[j] > DM.ROW_MULTIPLIER[j])
                        pl.rowPlay[j] = DM.ROW_MULTIPLIER[j];
                }
                pl.p1Action = DM.P1_PLAY_STRAIGHT_GRANDE_HOLD;
                if (pl.colRanking == 3 & pl.cntEmpty >= 3 & dm.playerRoundesToGo <= dm.roundCheck16)
                    pl.isPlayLess = true;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), DM.P1_PLAY_STRAIGHT_GRANDE_HOLD 1");
            }
            else
            {
                pl.p1Action = DM.P1_PLAY_GRANDE_HOLD;
                if (pl.colRanking == 3 & pl.cntEmpty >= 3 & dm.playerRoundesToGo <= dm.roundCheck16)
                    pl.isPlayLess = true;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playGrandeHold(), DM.P1_PLAY_GRANDE_HOLD 5");
            }
            computePlayValues(pl, op);
            return;
        }
        if (pl.p1Action != DM.P1_PLAY_HOLD_WEAK & diff > DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT])
            computePlayValues(pl, op);
    }

    private void playGrandeAttack(char playerId, Column pl, Column op)
    {
        // player: no grande, oponent: grande
        if (pl.row[DM.ROW_ID_GRANDE] >= 0 | op.row[DM.ROW_ID_GRANDE] <= 0)
            return;

        int counterCheck = -5;
        if (dm.isDouble)
            counterCheck = -7;
        if (isBestColValue(playerId, pl))
            counterCheck = counterCheck -1;
        int counter = pl.playCounter;
        int ranking = pl.colRanking;
        int diffRow = dm.getRowSum(pl.row, null) - dm.getRowSum(op.row, null);
        if (counter >= counterCheck)
        {
            if (dm.playerRoundesToGo <= dm.roundCheck12 & ranking > 1)
                ranking--;
            setRowPlay(pl, op, ranking, false);
            int diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
            if (diff < 0)
            {
                pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                diff = diff + DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                if (pl.cntEmpty == 1 & diff < 0 & diff + dm.bonusServedGrande >= 0)
                {
                    if (diffRow + DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] >= 0)
                    {
                        pl.p1Action = DM.P1_PLAY_GRANDE;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_GRANDE 1");
                        computePlayValues(pl, op);
                        diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                        if (diff < 0)
                        {
                            for (int j = 0; j < DM.ROW_ID_GRANDE; j++)
                            {
                                if (pl.rowOpo[j] > 0 & diff < 0)
                                {
                                    if (j < DM.ROW_ID_STRAIGHT)
                                    {
                                        for (int k = 0; k < 3; k++)
                                        {
                                            if (pl.rowOpo[j] > 0 & diff < 0)
                                            {
                                                diff = diff + DM.ROW_MULTIPLIER[j];
                                                pl.rowOpo[j] = pl.rowOpo[j] - DM.ROW_MULTIPLIER[j];
                                            }
                                        }
                                    }
                                    else
                                    {
                                        diff = diff + pl.rowOpo[j];
                                        pl.rowOpo[j] = 0;
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        pl.rowPlay[DM.ROW_ID_GRANDE] = pl.rowPlay[DM.ROW_ID_GRANDE] + dm.bonusServedGrande;
                        pl.p1Action = DM.P1_PLAY_WORST;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_WORST 7");
                        computePlayValues(pl, op);
                    }
                    return;
                }
            }
            if (diff >= 0)
            {
                if (op.cntEmpty == 1)
                {
                    for (int j = 0; j < DM.ROW_ID_GRANDE; j++)
                    {
                        if (pl.rowOpo[j] > 0)
                        {
                            if (j < DM.ROW_ID_STRAIGHT)
                            {
                                if (diff - ((DM.ROW_MULTIPLIER[j] * 4) - pl.rowOpo[j]) >= 0)
                                    pl.rowOpo[j] = DM.ROW_MULTIPLIER[j] * 4;
                            }
                            else
                            {
                                if (pl.rowOpo[j] == DM.ROW_MULTIPLIER[j] & diff - dm.bonusServed >= 0)
                                    pl.rowOpo[j] = pl.rowOpo[j] + dm.bonusServed;
                            }
                            break;
                        }
                    }
                }
                if (dm.isDouble)
                {
                    if (diffRow < 0)
                    {
                        pl.p1Action = DM.P1_PLAY_ATTACK_WIN;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_ATTACK_WIN 1");
                    }
                    else
                    {
                        pl.p1Action = DM.P1_PLAY_ATTACK;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_ATTACK 3");
                    }
                    computePlayValues(pl, op);
                }
                else
                {
                    int pCounter = pl.playCounter;
                    if (dm.isDouble)
                        pCounter = -2 + pl.playCounter;
                    if (!isBestColValue(playerId, pl) | dm.playerRoundesToGo <= dm.roundCheck16 | pCounter <= -4)
                        pl.isPlayLess = true;
                    if (pl.rowPlay[DM.ROW_ID_STRAIGHT] > 0 & pl.rowOpo[DM.ROW_ID_STRAIGHT] < 0)
                    {

                        pl.p1Action = DM.P1_PLAY_STRAIGHT_AND_GRANDE;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_STRAIGHT_AND_GRANDE 1");
                        computePlayValues(pl, op);
                    }
                    else
                    {
                        if  (       pl.rowPlay[DM.ROW_ID_STRAIGHT] > 0 & pl.rowOpo[DM.ROW_ID_STRAIGHT] > 0
                                &   dm.playerRoundesToGo <= dm.roundCheck6
                            )
                        {
                            pl.rowPlay[DM.ROW_ID_STRAIGHT] = 0;
                            pl.rowOpo[DM.ROW_ID_STRAIGHT] = 0;
                        }
                        if (isBestColValue(playerId, pl))
                        {
                            pl.p1Action = DM.P1_PLAY_GRANDE_HOLD;
                            if (dm.isLogging)
                                Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_GRANDE_HOLD 8");
                        }
                        else
                        {
                            pl.p1Action = DM.P1_PLAY_GRANDE;
                            if (dm.isLogging)
                                Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_GRANDE 3");
                        }
                        computePlayValues(pl, op);
                    }
                }
            }
            else
            {
                if (dm.playerRoundesToGo <= dm.roundCheck6 & diffRow < 0)
                {
                    boolean isPlay = false;
                    for (int j = 0; j < DM.ROW_ID_GRANDE; j++)
                    {
                        if (j < DM.ROW_ID_STRAIGHT)
                        {
                            if (pl.row[j] < 0 & diffRow + DM.ROW_MULTIPLIER[j] * 3 >= 0)
                            {
                                pl.rowPlay[j] = DM.ROW_MULTIPLIER[j] * 3;
                                isPlay = true;
                                break;
                            }
                        }
                        else
                        {
                            if (pl.row[j] < 0 & diffRow + DM.ROW_MULTIPLIER[j] >= 0)
                            {
                                pl.rowPlay[j] = DM.ROW_MULTIPLIER[j];
                                isPlay = true;
                                break;
                            }
                        }
                    }
                    if (isPlay)
                    {
                        pl.p1Action = DM.P1_PLAY_GRANDE_ATTACK;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_GRANDE_ATTACK 3");
                        computePlayValues(pl, op);
                        return;
                    }
                }

                if (pl.rowOpo[DM.ROW_ID_STRAIGHT] > 0 & diff + pl.rowOpo[DM.ROW_ID_STRAIGHT] >= 0)
                {
                    pl.rowOpo[DM.ROW_ID_STRAIGHT] = 0;
                    pl.p1Action = DM.P1_PLAY_GRANDE_ATTACK;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_GRANDE_ATTACK 4");
                    computePlayValues(pl, op);
                    return;
                }

                if (pl.playCounter >= 0 | (isBestColValue(playerId, pl) & pl.playCounter == -1 & dm.playerRoundesToGo >= dm.roundCheck8))
                {
                    pl.p1Action = DM.P1_PLAY_GRANDE;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_GRANDE 5");
                    return;
                }
                for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; j++)
                {
                    if (pl.rowPlay[j] >= 0 & pl.rowOpo[j] >= 0)
                        pl.rowOpo[j] = 0;
                }
                int cnt5 = 0;
                for (int j = DM.ROW_ID_K; j < DM.ROW_ID_GRANDE; j++)
                {
                    if (j < DM.ROW_ID_STRAIGHT)
                    {
                        if (pl.rowOpo[j] > DM.ROW_MULTIPLIER[j] * 3)
                            cnt5++;
                    }
                    else
                    {
                        if (pl.rowOpo[j] > DM.ROW_MULTIPLIER[j])
                            cnt5++;
                    }
                }
                pl.p1Action = DM.P1_PLAY_WORST;
                computePlayValues(pl, op);
                diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                int checkCounter = -4;
                if (dm.isDouble)
                    checkCounter = -5;
                if (diff >= -5 & cnt5 > 0 & pl.cntEmpty >= 3 & dm.playerRoundesToGo >= dm.roundCheck8 & pl.playCounter >= checkCounter)
                {
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_WORST 8");
                }
                else
                {
                    pl.p1Action = DM.P1_PLAY_GIVE_IT_UP;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_GIVE_IT_UP 6");
                    computePlayValues(pl, op);
                }
            }
        }
        else
        {
            if (ranking > 1)
                ranking--;
            setRowPlay(pl, op, ranking, false);
            pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
            int diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
            if (diff >= 0)
            {
                pl.p1Action = DM.P1_PLAY_GRANDE_ATTACK;
                pl.isPlayLess = true;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_GRANDE_ATTACK 3");
                computePlayValues(pl, op);
            }
            else
            {
                if (pl.rowPlay[DM.ROW_ID_STRAIGHT] > 0 & pl.rowOpo[DM.ROW_ID_STRAIGHT] > 0 & diff >= -20)
                {
                    pl.rowOpo[DM.ROW_ID_STRAIGHT] = 0;
                    pl.p1Action = DM.P1_PLAY_STRAIGHT_GRANDE;
                    computePlayValues(pl, op);
                    diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                    if (diff >= 0)
                    {
                        pl.p1Action = DM.P1_PLAY_STRAIGHT_AND_GRANDE;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_STRAIGHT_AND_GRANDE 2");
                        return;
                    }
                    if (pl.rowOpo[DM.ROW_ID_GRANDE] < 0)
                    {
                        if (pl.playCounter <= -4)
                        {
                            pl.p1Action = DM.P1_PLAY_GIVE_IT_UP;
                            if (dm.isLogging)
                                Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_GIVE_IT_UP 12");
                        }
                        else
                        {
                            pl.p1Action = DM.P1_PLAY_GRANDE_WEAK;
                            if (dm.isLogging)
                                Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_GRANDE_WEAK 3");
                        }
                    }
                    else
                    {
                        pl.p1Action = DM.P1_PLAY_STRAIGHT_GRANDE;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_STRAIGHT_GRANDE 5");
                    }
                    computePlayValues(pl, op);
                    return;
                }
                for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; j++)
                {
                    if (diff + pl.rowOpo[j] >= 0)
                    {
                        pl.rowOpo[j] = 0;
                        if (dm.playerRoundesToGo <= dm.roundCheck4)
                        {
                            if (diffRow >= -50)
                            {
                                pl.p1Action = DM.P1_PLAY_GRANDE_ATTACK;
                                if (dm.isLogging)
                                    Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_GRANDE_ATTACK 4");
                                computePlayValues(pl, op);
                                return;
                            }
                            else
                            {
                                pl.p1Action = DM.P1_PLAY_LAST_ROUNDES;
                                if (dm.isLogging)
                                    Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_LAST_ROUNDES 2");
                                computePlayValues(pl, op);
                                return;
                            }
                        }
                        break;
                    }
                }

                for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; j++)
                {
                    if (diff < 0 & pl.rowPlay[j] == 0)
                    {
                        pl.rowPlay[j] = DM.ROW_MULTIPLIER[j];
                        diff = diff + DM.ROW_MULTIPLIER[j];
                    }
                }
                diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                if (diff >= 0)
                {
                    pl.p1Action = DM.P1_PLAY_WEAK;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_WEAK 3");
                    computePlayValues(pl, op);
                }
                else
                {
                    pl.p1Action = DM.P1_PLAY_GIVE_IT_UP;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playGrandeAttack(), DM.P1_PLAY_GIVE_IT_UP 7");
                    computePlayValues(pl, op);
                }
            }
        }
    }

    private void setRowPlay(Column pl, Column op, int ranking, boolean isMin)
    {
        int rowP[];
        if (!dm.isDouble)
        {
            if (ranking == dm.col)
                rowP = DM.ROW_P3_COL_BEST;
            else
            {
                if (ranking == 1)
                    rowP = DM.ROW_P3_COL_WORST;
                else
                    rowP = DM.ROW_P3_COL_CENTER;
            }
        }
        else
        {
            if (ranking == dm.col)
                rowP = DM.ROW_P3_DOUBLE_COL_BEST;
            else
            {
                if (ranking == 1)
                    rowP = DM.ROW_P3_DOUBLE_COL_WORST;
                else
                    rowP = DM.ROW_P3_DOUBLE_COL_CENTER;
            }
        }
        for (int j = 0; j < DM.ROW_ID_GRANDE; j++)
        {
            if (pl.row[j] == -1)
            {
                pl.rowPlay[j] = rowP[j];
                if (isMin & j < DM.ROW_ID_STRAIGHT & pl.rowPlay[j] < DM.ROW_MULTIPLIER[j] * 3)
                    pl.rowPlay[j] = DM.ROW_MULTIPLIER[j] * 3;
            }
            else
                pl.rowPlay[j] = -1;
            if (op.row[j] == -1)
            {
                pl.rowOpo[j] = rowP[j];
                if (isMin & j < DM.ROW_ID_STRAIGHT & pl.rowOpo[j] < DM.ROW_MULTIPLIER[j] * 3)
                    pl.rowOpo[j] = DM.ROW_MULTIPLIER[j] * 3;
            }
            else
                pl.rowOpo[j] = -1;
        }
    }

    private void playHold(char playerId, Column pl, Column op)
    {
        if (pl.p1Action != DM.P1_NON)
            return;
        int diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
        int checkCounterBest = -2;
        if (dm.isDouble)
            checkCounterBest = -3;
        if (dm.playerRoundesToGo <= dm.roundCheck12)
            checkCounterBest = checkCounterBest + 1;

        if (isBestColValue(playerId, pl) & dm.playerRoundesToGo > dm.roundCheck8 & pl.playCounter >= checkCounterBest)
        {
            setRowPlay(pl, op, dm.col, true);
            diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
//Log.i(tag, "1 ??? col: " + pl.colId + ", playHold(), diff: " + diff);
            if (diff >= -10)
            {
                if  (pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & pl.rowOpo[DM.ROW_ID_GRANDE] == 0)
                {
                    pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                    pl.rowOpo[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                }
                pl.p1Action = DM.P1_PLAY_HOLD;
                if (diff > 0)
                    computePlayValues(pl, op);
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playHold(), DM.P1_PLAY_HOLD 3");
                return;
            }
            else
            {
                playSetValues(pl, op);
                diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
            }
        }
        if (diff >= 0)
        {
            boolean isLastEntry = false;
            if  (       pl.rowPlay[DM.ROW_ID_STRAIGHT] < 0 & pl.rowOpo[DM.ROW_ID_STRAIGHT] == 0
                    &   !(isBestColValue(playerId, pl) & pl.playCounter >= -1)
                )
            {
                pl.p1Action = DM.P1_PLAY_HOLD_STRAIGHT;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playHold(), DM.P1_PLAY_HOLD_STRAIGHT 1");
            }
            else
            {
                if (pl.cntEmpty == 1)
                {
                    for (int j = 0; j < DM.ROW_ID_GRANDE; j++)
                    {
                        if (pl.rowPlay[j] == diff & isBestColValue(playerId, pl))
                            isLastEntry = true;
                    }
                }
                if (isLastEntry)
                {
                    pl.p1Action = DM.P1_PLAY_WIN_BEST_COL;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playHold(), DM.P1_PLAY_WIN_BEST_COL");
                }
                else
                {
                    if  (pl.rowPlay[DM.ROW_ID_STRAIGHT] == 0 & pl.rowOpo[DM.ROW_ID_STRAIGHT] == 0 & dm.playerRoundesToGo > dm.roundCheck8)
                    {
                        pl.rowPlay[DM.ROW_ID_STRAIGHT] = DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT];
                        pl.rowOpo[DM.ROW_ID_STRAIGHT] = DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT];
                    }
                    if  (pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & pl.rowOpo[DM.ROW_ID_GRANDE] == 0 & dm.playerRoundesToGo > dm.roundCheck12)
                    {
                        pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                        pl.rowOpo[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                    }
                    int diffOpo = diff;
                    for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; j++)
                    {
                        if (diffOpo >= DM.ROW_MULTIPLIER[j] & pl.rowOpo[j] == 0)
                        {
                            pl.rowOpo[j] = DM.ROW_MULTIPLIER[j];
                            diffOpo = diffOpo + DM.ROW_MULTIPLIER[j];
                        }
                    }
                    pl.p1Action = DM.P1_PLAY_HOLD;
                    if (pl.colRanking == 3 & pl.cntEmpty >= 3 & dm.playerRoundesToGo <= dm.roundCheck16)
                        pl.isPlayLess = true;
                    else
                    {
                        if (pl.playCounter <= 0)
                            pl.isPlayMore = true;
                    }
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playHold(), DM.P1_PLAY_HOLD 4");
                }
            }
            if (!isLastEntry)
                computePlayValues(pl, op);
            return;
        }
        int ranking = pl.colRanking;
        pl.isPlayMore = false;
        int checkCounter = -4;
        if (dm.isDouble)
            checkCounter = -6;
        if (dm.playerRoundesToGo <= dm.roundCheck16 & pl.playCounter <= checkCounter)
        {
            if (pl.playCounter < (dm.playerRoundesToGo / 3 * -1))
                return;
        }
        if (dm.playerRoundesToGo <= dm.roundCheck8)
        {
            if (ranking > 1)
            {
                ranking--;
                pl.isPlayMore = true;
            }
        }
        else
        {
            if (dm.isDouble & pl.playCounter >= 1 & ranking < dm.col)
                ranking++;
        }
        setRowPlay(pl, op, ranking, true);
        diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
        if (diff >= 0)
        {
            if (pl.cntEmpty == 1 & pl.rowPlay[DM.ROW_ID_GRANDE] == 0)
            {
                pl.p1Action = DM.P1_PLAY_WON_OR_LOST;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playHold(), DM.P1_PLAY_WON_OR_LOST 3");
                computePlayValues(pl, op);
                return;
            }
            if (op.cntEmpty == 1 & pl.rowOpo[DM.ROW_ID_GRANDE] == 0)
            {
                pl.p1Action = DM.P1_PLAY_BEST;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playHold(), DM.P1_PLAY_BEST 3");
                computePlayValues(pl, op);
                return;
            }

            if (pl.cntEmpty <= 2 & dm.playerRoundesToGo <= dm.roundCheck8 & pl.rowPlay[DM.ROW_ID_GRANDE] < 0)
            {
                boolean isPlayWin = true;
                int cntWin = 0;
                for (int j = 0; j < DM.ROW_ID_GRANDE; j++)
                {
                    if (j < DM.ROW_ID_STRAIGHT)
                    {
                        if (pl.rowPlay[j] > DM.ROW_MULTIPLIER[j] * 3)
                            isPlayWin = false;
                    }
                    else
                    {
                        if (pl.rowPlay[j] > DM.ROW_MULTIPLIER[j])
                            isPlayWin = false;
                    }
                    if (pl.rowPlay[j] > 0 & diff - pl.rowPlay[j] > 0)
                        cntWin++;
                }
                if (isPlayWin)
                {
                    pl.p1Action = DM.P1_PLAY_WIN;
                    if (cntWin == 2)
                        pl.isOr = true;
                    else
                    {
                        if (pl.rowOpo[DM.ROW_ID_GRANDE] == 0 & diff - DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] >= 0)
                            pl.rowOpo[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                        else
                            pl.isPlayMore = false;
                        computePlayValues(pl, op);
                    }
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playHold(), P1_PLAY_WIN 6");
                    return;
                }
            }

            for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; j++)
            {
                if (pl.rowOpo[j] == DM.ROW_MULTIPLIER[j])
                {
                    pl.rowOpo[j] = pl.rowOpo[j] + dm.bonusServed;
                    diff = diff - dm.bonusServed;
                    if (diff >= 0)
                    {
                        for (int k = DM.ROW_ID_STRAIGHT; k < DM.ROW_ID_GRANDE; k++)
                        {
                            if (pl.rowPlay[k] > 0 & diff - pl.rowPlay[k] >= 0)
                            {
                                diff = diff - pl.rowPlay[k];
                                pl.rowPlay[k] = 0;
                            }
                        }
                    }
                    else
                    {
                        setRowPlay(pl, op, ranking, true);
                        if (pl.rowOpo[DM.ROW_ID_GRANDE] == 0)
                        {
                            pl.rowOpo[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                            pl.rowOpo[j] = 0;
                            computePlayValues(pl, op);
                            diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                            if (diff < 0)
                            {
                                pl.rowOpo[DM.ROW_ID_GRANDE] = 0;
                                pl.rowOpo[j] = DM.ROW_MULTIPLIER[j];
                            }
                        }
                        else
                        {
                            pl.rowOpo[j] = DM.ROW_MULTIPLIER[j];
                            computePlayValues(pl, op);
                        }
                    }
                    break;
                }
            }

            if (diff >= 0)
            {
                pl.p1Action = DM.P1_PLAY_HOLD;
                pl.isPlayLess = false;
                int checkPlayLess = 2;
                if (dm.isDouble)
                    checkPlayLess = 3;
                if (pl.playCounter >= checkPlayLess)
                    pl.isPlayLess = true;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playHold(), DM.P1_PLAY_HOLD 5");
                if (dm.playerRoundesToGo > dm.roundCheck12 & pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & pl.rowOpo[DM.ROW_ID_GRANDE] == 0)
                {
                    pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                    pl.rowOpo[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                }
                computePlayValues(pl, op);

            }
            else
                pl.p1Action = DM.P1_NON;
            return;
        }
        else
        {
            if (pl.rowPlay[DM.ROW_ID_GRANDE] == 0)
                return;

            if (pl.rowPlay[DM.ROW_ID_STRAIGHT] < 0 & pl.rowOpo[DM.ROW_ID_STRAIGHT] >= 0 & pl.playCounter < 0)
            {
                pl.rowOpo[DM.ROW_ID_STRAIGHT] = 0;
                pl.p1Action = DM.P1_PLAY_HOLD_STRAIGHT;

                if (!dm.isDouble)
                {
                    if (pl.playCounter >= 1)
                        pl.isPlayLess = true;
                    if (pl.playCounter < 0)
                        pl.isPlayMore = true;
                }

                diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                if (diff >= 0)
                {
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playHold(), DM.P1_PLAY_HOLD_STRAIGHT 2");
                    return;
                }
            }
            if (pl.rowPlay[DM.ROW_ID_STRAIGHT] > 0 & pl.rowOpo[DM.ROW_ID_STRAIGHT] >= 0)
            {
                pl.rowOpo[DM.ROW_ID_STRAIGHT] = 0;
                pl.p1Action = DM.P1_PLAY_STRAIGHT;
                if (!dm.isDouble)
                    pl.isPlayLess = true;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playHold(), DM.P1_PLAY_STRAIGHT 3");
                computePlayValues(pl, op);
                diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                if (diff >= 0)
                    return;
            }

            if (ranking == pl.colRanking & ranking > 1)
            {
                ranking--;
                setRowPlay(pl, op, ranking, false);
            }
            pl.isPlayLess = false;
            if (pl.colRanking == dm.col & (pl.playCounter == -1 | (pl.playCounter >= -2 & dm.playerRoundesToGo >= dm.roundCheck8)))
                pl.isPlayMore = true;
            int checkHoldCounter = -1;
            diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
            if (diff >= 0 & pl.playCounter >= checkHoldCounter)
            {
                pl.p1Action = DM.P1_PLAY_HOLD;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playHold(), DM.P1_PLAY_HOLD 6");
            }
            else
            {
                if (diff >= 0)
                {
                    pl.p1Action = DM.P1_PLAY_HOLD_WEAK;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playHold(), DM.P1_PLAY_HOLD_WEAK 4");
                }
                else
                {
                    for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; j++)
                    {
                        if (pl.rowOpo[j] > 0 & diff + pl.rowOpo[j] >= 0)
                        {
                            pl.rowOpo[j] = 0;
                            pl.p1Action = DM.P1_PLAY_HOLD_WEAK;
                            if (dm.isLogging)
                                Log.i(TAG, "col: " + pl.colId + ", playHold(), DM.P1_PLAY_HOLD_WEAK 8");
                            computePlayValues(pl, op);
                            return;
                        }
                    }
                    int cntMax = 0;
                    pl.p1Action = DM.P1_PLAY_MAX_VALUE;
                    for (int j = 0; j < DM.ROW_ID_GRANDE; j++)
                    {
                        if (pl.rowPlay[j] >= 0)
                        {
                            cntMax++;
                            if (j < DM.ROW_ID_STRAIGHT)
                                pl.rowPlay[j] = DM.ROW_MULTIPLIER[j] * 5;
                            else
                                pl.rowPlay[j] = DM.ROW_MULTIPLIER[j] + dm.bonusServed;
                        }
                    }
                    diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                    if (diff >= 0 & cntMax <= 2)
                    {
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playHold(), P1_PLAY_MAX_VALUE");
                        computePlayValues(pl, op);
                    }
                    else
                    {
                        pl.p1Action = DM.P1_PLAY_GIVE_IT_UP;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", playHold(), DM.P1_PLAY_GIVE_IT_UP 11");
                        computePlayValues(pl, op);
                    }
                }
            }
        }
    }

    private void setPlayerOponentGrande(char playerId)
    {
        for (int i = 0; i < dm.col; i++)
        {
            Column pl = getPlayerColumn(playerId, i);
            Column op = getOponentColumn(playerId, i);
            boolean setValue = false;
            if (dm.playerRoundesToGo >= dm.roundCheck8 & pl.colRanking == dm.col)
                setValue = true;
            if (dm.playerRoundesToGo >= dm.roundCheck12 & pl.colRanking > 1 & pl.colRanking < dm.col)
                setValue = true;
            if (dm.playerRoundesToGo >= dm.roundCheck16 & pl.colRanking == 1)
                setValue = true;
            if (setValue & pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & pl.rowOpo[DM.ROW_ID_GRANDE] == 0)
            {
                pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                pl.rowOpo[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
            }
            setPlayerColumn(playerId, i, pl);
        }
    }

    private boolean setPlay0(char playerId)
    {
        boolean isPlayUpdate = false;
        int actionLowest = 999;
        int actionHighest;
        int play0Cnt = 0;
        play0Col = -1;
        play0Row = -1;
        int lowestCol = -1;
        int lowestRow = 99;
        play0ColIsCancel = false;
        int cancelCol = -1;
        int cancelRow = -1;
        int grandeAttackCnt = 0;
        int grandeHoldCnt = 0;
        int colValue = 999;
        int grandeHoldCol = -1;
        int grandeHoldRow = -1;
        for (int i = 0; i < dm.col; i++)
        {
            Column pl = getPlayerColumn(playerId, i);
//Log.i(tag, "A ??? setPlay0(), col: " + i + ", pl.colValue: " + pl.colValue);
            int playSumOp = dm.getRowSum(null, pl.rowOpo);
            if(pl.cntEmpty > 0)
            {
                if (pl.p1Action == DM.P1_PLAY_GRANDE_ATTACK)
                    grandeAttackCnt++;
                if (pl.rowPlay[DM.ROW_ID_GRANDE] == DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE])
                    grandeHoldCnt++;
                for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
                {
                    if (pl.rowPlay[j] == 0 & pl.p1Action == DM.P1_PLAY_VALUE_0)
                    {
                        play0Col = i;
                        play0Row = j;
                        if (dm.isLogging)
                            Log.i(TAG, "setPlay0(), <= DM.P1_PLAY_VALUE_0, col: " + play0Col + ", row: " + play0Row);
                        return true;
                    }

                    if (pl.rowPlay[j] >= 0 & j == DM.ROW_ID_GRANDE & pl.colValue <= colValue)
                    {
                        if (pl.colValue <= colValue)
                        {
                            grandeHoldCol = i;
                            grandeHoldRow = j;
                            colValue = pl.colValue;
                        }
                    }
                    if (pl.rowPlay[j] == 0)
                    {
                        play0Cnt++;
                        if (j <= lowestRow & pl.p1Action < actionLowest)
                        {
                            lowestCol = i;
                            lowestRow = j;
                        }
                        if (pl.p1Action < actionLowest)
                        {
                            actionLowest = pl.p1Action;
                            play0Col = i;
                            play0Row = j;
                        }
                    }
                }
            }
        }

        if (actionLowest <= DM.P1_PLAY_WON & play0Col >= 0 & play0Row >= 0)
        {
            if (dm.isLogging)
                Log.i(TAG, "setPlay0(), <= DM.P1_PLAY_WON, col: " + play0Col + ", row: " + play0Row);
            return true;
        }

        if (dm.playerRoundesToGo == play0Cnt)
        {
//Log.i(tag, "1 ??? setPlay0(), dm.playerRoundesToGo: " + dm.playerRoundesToGo);
            isAllPlay0 = true;
            int wCol = -1;
            int wRow = -1;
            int wAction = 0;
            for (int i = 0; i < dm.col; i++)
            {
                Column pl = getPlayerColumn(playerId, i);
                for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
                {
                    if (pl.rowPlay[j] == 0 & pl.p1Action >= wAction)
                    {
                        wAction = pl.p1Action;
                        wCol = i;
                        wRow = j;
                        break;
                    }
                }
            }
            if (wCol >= 0 & wRow >= 0)
            {
                Column pl = getPlayerColumn(playerId, wCol);
                Column op = getOponentColumn(playerId, wCol);
                if (pl.rowPlay[wRow] == 0)
                    pl.rowPlay[wRow] = DM.ROW_MULTIPLIER[wRow];
                if (pl.rowOpo[wRow] == 0)
                    pl.rowOpo[wRow] = DM.ROW_MULTIPLIER[wRow];
                int diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
                if (diff < 0 & wRow >= DM.ROW_ID_STRAIGHT)
                {
                    int bonus = dm.bonusServed;
                    if (wRow == DM.ROW_ID_GRANDE)
                        bonus = dm.bonusServedGrande;
                    if (diff + bonus >= 0)
                    {
                        pl.rowPlay[wRow] = pl.rowPlay[wRow] + bonus;
                        pl.p1Action = DM.P1_PLAY_WEAK;
                        if (dm.isLogging)
                            Log.i(TAG, "col: " + pl.colId + ", setPlay0(), DM.P1_PLAY_WEAK 1");
                    }
                }
            }
            if (dm.isLogging)
            {
                Log.i(TAG, " ");
                Log.i(TAG, "setPlay0(), all play 0, set best pl.p1Action to value:");
                loggingPlayValues(playerId);
            }
        }
        if (play0Cnt == 0 & grandeAttackCnt <= 1 & grandeHoldCnt <= 1)
        {
//Log.i(tag, "2 ??? setPlay0(), play0Cnt: " + play0Cnt);
            if (dm.playerRoundesToGo <= dm.roundCheck6)
            {
                int maxEmptyCnt = 0;
                int maxEmptyCol = -1;
                actionLowest = 999;
                for (int i = 0; i < dm.col; i++)
                {
                    Column pl = getPlayerColumn(playerId, i);
                    if (pl.cntEmpty > 0)
                    {
                        if (pl.p1Action < actionLowest)
                            actionLowest = pl.p1Action;
                        if (pl.cntEmpty > maxEmptyCnt)
                        {
                            maxEmptyCol = i;
                            maxEmptyCnt = pl.cntEmpty;
                        }
                    }
                }
                if (maxEmptyCol > 1)
                {
                    int worstRow = -1;
                    Column pl = getPlayerColumn(playerId, maxEmptyCol);
                    if (pl.rowPlay[DM.ROW_ID_STRAIGHT] > 0 & pl.rowPlay[DM.ROW_ID_STRAIGHT] == pl.rowOpo[DM.ROW_ID_STRAIGHT])
                        worstRow = DM.ROW_ID_STRAIGHT;
                    if (pl.rowPlay[DM.ROW_ID_GRANDE] > 0 & pl.rowPlay[DM.ROW_ID_GRANDE] == pl.rowOpo[DM.ROW_ID_GRANDE])
                        worstRow = DM.ROW_ID_GRANDE;
                    if (worstRow >= 0)
                    {
                        play0Col = maxEmptyCol;
                        play0Row = worstRow;
                    }
                }
            }
            else
            {
                for (int i = 0; i < dm.col; i++)
                {
                    Column pl = getPlayerColumn(playerId, i);
                    boolean isPlay = true;
                    if (isPlay & pl.p1Action < actionLowest & pl.cntEmpty > 0)
                    {
                        actionLowest = pl.p1Action;
                        play0Col = i;
                    }
                }
                if (play0Col >= 0)
                {
                    Column pl = getPlayerColumn(playerId, play0Col);
                    for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
                    {
                        if (pl.rowPlay[j] > 0)
                        {
                            play0Row = j;
                            boolean cancelGrande = false;
                            if (dm.playerRoundesToGo <= dm.roundCheck16)
                                cancelGrande = true;
                            if (!isPlayUpdate & cancelGrande & pl.rowPlay[DM.ROW_ID_GRANDE] > 0 & pl.rowOpo[DM.ROW_ID_GRANDE] > 0 & pl.playCounter >= 0)
                            {
                                play0Row = DM.ROW_ID_GRANDE;
                                pl.rowPlay[DM.ROW_ID_GRANDE] = 0;
                                pl.rowOpo[DM.ROW_ID_GRANDE] = 0;
                                isPlayUpdate = true;
                                break;
                            }
                            boolean cancelStraight = false;
                            if (pl.colId == 0 & dm.playerRoundesToGo <= dm.roundCheck8)
                                cancelStraight = true;
                            if (pl.colId != 0 & pl.colId != dm.col -1 & dm.playerRoundesToGo <= dm.roundCheck6)
                                cancelStraight = true;
                            if (pl.colId == dm.col -1 & dm.playerRoundesToGo <= dm.roundCheck4)
                                cancelStraight = true;
                            if (!isPlayUpdate & cancelStraight & pl.rowPlay[DM.ROW_ID_STRAIGHT] > 0 & pl.rowOpo[DM.ROW_ID_STRAIGHT] > 0 & pl.playCounter >= 0)
                            {
                                play0Row = DM.ROW_ID_STRAIGHT;
                                pl.rowPlay[DM.ROW_ID_STRAIGHT] = 0;
                                pl.rowOpo[DM.ROW_ID_STRAIGHT] = 0;
                                isPlayUpdate = true;
                                break;
                            }
                            if (play0Row == DM.ROW_ID_STRAIGHT & pl.rowPlay[DM.ROW_ID_GRANDE] > 0 & pl.rowOpo[DM.ROW_ID_GRANDE] > 0)
                                play0Row = DM.ROW_ID_GRANDE;
                            isPlayUpdate = true;
                            break;
                        }
                    }
                }
            }

        }
        if (play0Cnt > 1 & play0Col >= 0)
        {
//Log.i(tag, "3 ??? setPlay0(), play0Cnt: " + play0Cnt);
            if ((play0Row == DM.ROW_ID_STRAIGHT | play0Row == DM.ROW_ID_GRANDE) & dm.playerRoundesToGo >= dm.roundCheck12)
            {
                int imageCol = -1;
                int imageRow = -1;
                int imageAction = 999;
                for (int i = 0; i < dm.col; i++)
                {
                    Column pl = getPlayerColumn(playerId, i);
                    for (int j = 0; j < DM.ROW_ID_STRAIGHT; j++)
                    {
                        if (pl.rowPlay[j] == DM.ROW_MULTIPLIER[j] & pl.p1Action < imageAction)
                        {
                            imageCol = i;
                            imageRow = j;
                            imageAction = pl.p1Action;
                        }
                    }
                }
                if (imageCol < 0)
                {
                    for (int i = 0; i < dm.col; i++)
                    {
                        Column pl = getPlayerColumn(playerId, i);
                        for (int j = 0; j < DM.ROW_ID_STRAIGHT; j++)
                        {
                            if (pl.rowPlay[j] == DM.ROW_MULTIPLIER[j] * 2 & pl.p1Action < imageAction)
                            {
                                imageCol = i;
                                imageRow = j;
                                imageAction = pl.p1Action;
                            }
                        }
                    }
                }
                if (imageCol >= 0)
                {
                    isPlayUpdate = true;
                    play0Col = imageCol;
                    play0Row = imageRow;
                }
            }
        }
        if (play0Cnt > 0)
        {
//Log.i(tag, "4 ??? setPlay0(), play0Cnt: " + play0Cnt);
            int action = 999;
            for (int i = 0; i < dm.col; i++)
            {
                Column pl = getPlayerColumn(playerId, i);
                boolean isFirst = true;
                if (pl.cntEmpty > 0 & pl.p1Action <= DM.P1_PLAY_WEAK & pl.p1Action < action)
                {
                    for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
                    {
                        if (pl.rowPlay[j] == 0)
                        {
                            if (isFirst)
                            {
                                action = pl.p1Action;
                                isFirst = false;
                                cancelCol = i;
                                cancelRow = j;
                            }
                        }
                    }
                }
            }
            if (cancelCol < 0)
            {
                action = 999;
                for (int i = 0; i < dm.col; i++)
                {
                    Column pl = getPlayerColumn(playerId, i);
                    if (pl.cntEmpty > 0 & pl.p1Action <= DM.P1_PLAY_WEAK & pl.p1Action < action)
                    {
                        for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
                        {
                            if (pl.rowPlay[j] > 0)
                            {
                                action = pl.p1Action;
                                cancelCol = i;
                                cancelRow = j;
                                break;
                            }
                        }
                    }
                }
            }
            if (cancelCol >= 0)
            {
                play0ColIsCancel = true;
                play0Col = cancelCol;
                play0Row = cancelRow;
            }
        }
        if (cancelCol < 0 & play0Cnt > 1 & lowestCol >= 0 & lowestRow >= 0)
        {
//Log.i(tag, "5 ??? setPlay0()");
            Column pl = getPlayerColumn(playerId, lowestCol);
            if (pl.rowPlay[lowestRow] == 0)
            {
                play0Col = lowestCol;
                play0Row = lowestRow;
                if  (       lowestRow == DM.ROW_ID_STRAIGHT & pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & pl.cntEmpty >= 2
                        &   pl.rowPlay[DM.ROW_ID_FULL] <= 0 & pl.rowPlay[DM.ROW_ID_POKER] <= 0
                    )
                    play0Row = DM.ROW_ID_GRANDE;
                if (lowestRow < DM.ROW_ID_STRAIGHT & lowestRow >= DM.ROW_ID_Q
                        & pl.rowPlay[DM.ROW_ID_STRAIGHT] == 0 & pl.rowPlay[DM.ROW_ID_GRANDE] > 0)
                    play0Row = DM.ROW_ID_STRAIGHT;
            }
        }
//Log.i(tag, "6a ??? setPlay0(), play0Col: " + play0Col + ", grandeHoldCnt: " + grandeHoldCnt + ", grandeHoldCol: " + grandeHoldCol);
        if (dm.playerRoundesToGo < dm.roundCheck20 & grandeHoldCnt > 1 & grandeHoldCol >= 0 & grandeHoldRow >= 0)
        {
//Log.i(tag, "6b ??? setPlay0()");
            play0Col = grandeHoldCol;
            Column pl = getPlayerColumn(playerId, grandeHoldCol);
            for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
            {
                if (pl.rowPlay[j] >= 0)
                {
                    if (pl.rowPlay[j] > 0 & pl.rowPlay[DM.ROW_ID_GRANDE] == 0)
                        play0Row = DM.ROW_ID_GRANDE;
                    else
                        play0Row = j;
                    break;
                }
            }
            pl.p1Action = DM.P1_PLAY;
            setPlayerColumn(playerId, grandeHoldCol, pl);
            if (dm.isLogging)
                Log.i(TAG, "multiple GRANDE, value: 0, col: " + play0Col + ", row: " + play0Row);
        }
        if (play0Col < 0)
        {
//Log.i(tag, "7 ??? setPlay0()");
            int lcol = -1;
            int minValue = 999;
            int maxValue = -1;
            int emtyCntMin = 99;
            int emtyCntMax = 0;
            for (int i = 0; i < dm.col; i++)
            {
                Column pl = getPlayerColumn(playerId, i);
                if (pl.cntEmpty > 0)
                {
                    if (pl.cntEmpty < emtyCntMin)
                        emtyCntMin = pl.cntEmpty;
                    if (pl.cntEmpty > emtyCntMax)
                    {
                        emtyCntMax = pl.cntEmpty;
                        lcol = i;
                    }
                }
            }
            if (lcol >= 0 & emtyCntMax > emtyCntMin & emtyCntMin == 1)
            {
                Column pl = getPlayerColumn(playerId, lcol);
                for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
                {
                    if (pl.rowPlay[j] >= 0)
                    {
                        play0Col = lcol;
                        play0Row = j;
                        break;
                    }
                }
            }
            if (play0Col < 0 & emtyCntMax == 1 & emtyCntMin == 1)
            {
                int sfpRowId = 99;
                int sfpCol = -1;
                int gRanking = 99;
                int gCol = -1;
                for (int i = 0; i < dm.col; i++)
                {
                    Column pl = getPlayerColumn(playerId, i);
                    for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
                    {
                        if (pl.rowPlay[j] >= 0)
                        {
                            if (j < DM.ROW_ID_GRANDE)
                            {
                                if (j < sfpRowId)
                                {
                                    sfpCol = i;
                                    sfpRowId = j;
                                }
                            }
                            else
                            {
                                if (pl.colRanking < gRanking)
                                {
                                    gCol = i;
                                    gRanking = pl.colRanking;
                                }
                            }
                        }
                    }
                }
                if (gCol >= 0)
                {
                    play0Col = gCol;
                    play0Row = DM.ROW_ID_GRANDE;
                }
                else
                {
                    if (sfpCol >= 0)
                    {
                        play0Col = sfpCol;
                        play0Row = sfpRowId;
                    }
                }


            }
            lcol = -1;
            if (play0Col < 0)
            {
                for (int i = 0; i < dm.col; i++)
                {
                    Column pl = getPlayerColumn(playerId, i);
                    if (pl.colValue < minValue)
                    {
                        minValue = pl.colValue;
                        lcol = i;
                    }
                    if (pl.colValue > maxValue)
                        maxValue = pl.colValue;
                }
                if (minValue != maxValue & lcol >= 0)
                {
                    Column pl = getPlayerColumn(playerId, lcol);
                    for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
                    {
                        if (pl.rowPlay[j] >= 0)
                        {
                            play0Col = lcol;
                            play0Row = j;
                            break;
                        }
                    }
                }
            }
        }
        if (play0Col < 0)
        {
//Log.i(tag, "8 ??? setPlay0()");
            actionLowest = 999;
            actionHighest = 0;
            int lCol = -1;
            int hCol = -1;
            int lCntEmpty = 0;
            int hCntEmpty = 0;
            for (int i = 0; i < dm.col; i++)
            {
                Column pl = getPlayerColumn(playerId, i);
                if (pl.p1Action < actionLowest & pl.p1Action > DM.P1_COLUMN_FULL)
                {
                    actionLowest = pl.p1Action;
                    lCol = i;
                    lCntEmpty = pl.cntEmpty;
                }
                if (pl.p1Action >= actionHighest)
                {
                    actionHighest = pl.p1Action;
                    hCol = i;
                    hCntEmpty = pl.cntEmpty;
                }
            }
            int bCol = -1;
            int bRow = -1;
            if (actionHighest == actionLowest)
            {
                if (lCntEmpty >= hCntEmpty)
                    bCol = lCol;
                else
                    bCol = hCol;
            }
            if (actionHighest > actionLowest)
                bCol = lCol;
            if (bCol >= 0)
            {
                Column pl = getPlayerColumn(playerId, bCol);
                for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
                {
                    if (pl.rowPlay[j] >= 0)
                    {
                        bRow = j;
                        break;
                    }
                }
                if (bCol >= 0 & bRow >= 0)
                {
                    isPlayUpdate = true;
                    play0Col = bCol;
                    play0Row = bRow;
                }
            }
        }

        if (dm.isLogging)
        {
            String movesGo = "moves to go: " + dm.player.getMovesToGo(playerId) + "; ";
            if (play0Col >= 0)
                Log.i(TAG, movesGo + "setPlay0(), col: " + play0Col + ", row: " + play0Row + ", count 0: " + play0Cnt + ", play0ColIsCancel: " + play0ColIsCancel);
            else
                Log.i(TAG, movesGo + "setPlay0(), count 0: " + play0Cnt);
        }
        return isPlayUpdate;
    }

    private void playWeak(Column pl, Column op)
    {
        int ranking = pl.colRanking;
        if (dm.playerRoundesToGo <= dm.roundCheck8)
        {
            if (ranking > 1)
                ranking--;
        }

        setRowPlay(pl, op, ranking, true);

        int diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
        if (diff >= 0)
        {
            boolean isPlayerImage = false;
            boolean isOpo0 = false;
            for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
            {
                if (pl.rowPlay[j] > 0 & j < DM.ROW_ID_STRAIGHT)
                    isPlayerImage = true;
                if (pl.rowOpo[j] == 0)
                    isOpo0 = true;
            }
            if (isPlayerImage & isOpo0)
            {
                pl.p1Action = DM.P1_PLAY_OPO_WEAK;
                if (dm.playerRoundesToGo <= dm.roundCheck12)
                    pl.isPlayLess = true;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playWeak(), P1_PLAY_OPO_WEAK 1");
            }
            else
            {
                pl.p1Action = DM.P1_PLAY_HOLD_WEAK;
                if (dm.playerRoundesToGo <= dm.roundCheck12)
                    pl.isPlayLess = true;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playWeak(), P1_PLAY_HOLD_WEAK 9");
            }
        }
        else
        {
            if (diff < 0 & pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & diff + DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] >= 0)
            {
                pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
                if (pl.colRanking == dm.col)
                {
                    pl.p1Action = DM.P1_PLAY_GRANDE;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playWeak(), DM.P1_PLAY_GRANDE 4");
                }
                else
                {
                    pl.p1Action = DM.P1_PLAY_GRANDE_WEAK;
                    if (dm.isLogging)
                        Log.i(TAG, "col: " + pl.colId + ", playWeak(), DM.P1_PLAY_GRANDE_WEAK 2");
                }
                computePlayValues(pl, op);
                return;
            }
            if (diff >= -5 & dm.playerRoundesToGo > dm.roundCheck8)
            {
                pl.p1Action = DM.P1_PLAY_HOLD_WEAK;
                pl.isPlayLess = true;
                computePlayValues(pl, op);
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playWeak(), DM.P1_PLAY_HOLD_WEAK 10");
                return;
            }
            for (int j = DM.ROW_ID_STRAIGHT; j <= DM.ROW_ID_POKER; j++)
            {
                if (diff < 0 & pl.rowOpo[j] > 0)
                {
                    pl.rowOpo[j] = 0;
                    diff = diff + DM.ROW_MULTIPLIER[j];
                }
            }
            if (diff >= 0)
            {
                pl.p1Action = DM.P1_PLAY_WORST;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playWeak(), P1_PLAY_WORST 4");
            }
            else
            {
                playSetValues(pl, op);
                pl.p1Action = DM.P1_PLAY_GIVE_IT_UP;
                if (dm.isLogging)
                    Log.i(TAG, "col: " + pl.colId + ", playWeak(), DM.P1_PLAY_GIVE_IT_UP 7");
            }
            computePlayValues(pl, op);
        }
    }

    private void setServeValues(char playerId)
    {
        if (dm.playerRoundesToGo <= dm.roundCheck16)
            return;

        for (int i = dm.col -1; i >= 0; i--)
        {
            Column pl = getPlayerColumn(playerId, i);
            if (i >= 2)
            {
                for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; j++)
                {
                    if (pl.rowPlay[j] < 0)
                    {
                        Column plC = getPlayerColumn(playerId, i -1);
                        Column plW = getPlayerColumn(playerId, i -2);
                        if (plC.rowPlay[j] == DM.ROW_MULTIPLIER[j] & plW.rowPlay[j] == DM.ROW_MULTIPLIER[j])
                        {
                            if (plC.p1Action >= plW.p1Action)
                            {
                                plC.rowPlay[j] = DM.ROW_MULTIPLIER[j] + dm.bonusServed;
                                setPlayerColumn(playerId, i -1, plC);
                            }
                            else
                            {
                                plW.rowPlay[j] = DM.ROW_MULTIPLIER[j] + dm.bonusServed;
                                setPlayerColumn(playerId, i -2, plW);
                            }
                        }
                    }
                }
            }
        }
    }

    private void computeImagePlayValues(Column pl, Column op)
    {
        int diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
        for (int j = 0; j < DM.ROW_ID_STRAIGHT; ++j)
        {
            if (diff > 0 & pl.rowPlay[j] > 0 & pl.rowOpo[j] < 0 & op.row[j] + DM.ROW_MULTIPLIER[j] < pl.rowPlay[j])
            {
                if (diff - pl.rowPlay[j] + DM.ROW_MULTIPLIER[j] > 0)
                {
                    pl.rowPlay[j] = DM.ROW_MULTIPLIER[j];
                    diff = diff - pl.rowPlay[j] + DM.ROW_MULTIPLIER[j];
                }
            }
        }

    }

    private void computePlayValues(Column pl, Column op)
    {
        int diff = 0;
        for (int h = 0; h < 4; h++)
        {
            diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
            for (int j = 0; j < DM.ROW_ID_STRAIGHT; ++j)
            {
                if (pl.p1Action > DM.P1_NON & pl.p1Action < DM.P1_PLAY_WEAK & pl.rowPlay[j] > 0)
                    pl.rowPlay[j] = 0;
                else
                {
                    if (pl.rowPlay[j] > 0 & diff >= DM.ROW_MULTIPLIER[j])
                    {

                        if  (       dm.playerRoundesToGo > dm.roundCheck8 & diff - DM.ROW_MULTIPLIER[j] < DM.ROW_MULTIPLIER[j]
                                &   pl.rowPlay[j] / DM.ROW_MULTIPLIER[j] <= 2
                                &   pl.p1Action >= DM.PLAY_P3
                            )
                            break;

                        pl.rowPlay[j] = pl.rowPlay[j] - DM.ROW_MULTIPLIER[j];
                        diff = diff - DM.ROW_MULTIPLIER[j];
                        if (diff == 0 & pl.p1Action == DM.P1_PLAY_HOLD_BEST)
                            pl.rowPlay[j] = pl.rowPlay[j] + DM.ROW_MULTIPLIER[j];
                    }
                    if (pl.p1Action == DM.P1_PLAY_WIN & pl.rowPlay[j] > 0 & pl.rowPlay[j] < DM.ROW_MULTIPLIER[j] * 5 & diff < 0)
                    {
                        pl.rowPlay[j] = pl.rowPlay[j] + DM.ROW_MULTIPLIER[j];
                        diff = diff + DM.ROW_MULTIPLIER[j];
                    }
                }
            }
        }
//Log.i(tag, "1a ??? col: " + pl.colId + ", computePlayValues(), diff: " + diff + ", pl.rowPlay[DM.ROW_ID_STRAIGHT]: " + pl.rowPlay[DM.ROW_ID_STRAIGHT]);
        for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; ++j)
        {
            if (pl.rowPlay[j] > 0)
            {
                if (diff >= pl.rowPlay[j])
                {
                    diff = diff - pl.rowPlay[j];
                    pl.rowPlay[j] = 0;
                }
                if (pl.rowPlay[j] > DM.ROW_MULTIPLIER[j] & diff >= dm.bonusServed)
                {
                    diff = diff - dm.bonusServed;
                    pl.rowPlay[j] = pl.rowPlay[j] - dm.bonusServed;
                }
            }
        }
        if  (       dm.playerRoundesToGo >= dm.roundCheck20 & pl.p1Action >= DM.P1_PLAY
                &   pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & pl.rowOpo[DM.ROW_ID_GRANDE] == 0
            )
        {
            pl.rowPlay[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
            pl.rowOpo[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE];
        }
        if (dm.playerRoundesToGo <= dm.roundCheck8 & pl.rowPlay[DM.ROW_ID_GRANDE] > 0 & pl.rowOpo[DM.ROW_ID_GRANDE] > 0)
        {
            pl.rowPlay[DM.ROW_ID_GRANDE] = 0;
            pl.rowOpo[DM.ROW_ID_GRANDE] = 0;
        }
        if (pl.p1Action >= DM.P1_PLAY_POSIBLE_WIN)
        {
            diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
            if (diff == 0)
            {
                for (int j = 0; j < DM.ROW_ID_STRAIGHT; ++j)
                {
                    if (pl.rowPlay[j] == 0)
                        pl.rowPlay[j] = DM.ROW_MULTIPLIER[j];
                }
            }
        }
//Log.i(tag, "1b ??? col: " + pl.colId + ", computePlayValues(), diff: " + diff + ", pl.rowPlay[DM.ROW_ID_STRAIGHT]: " + pl.rowPlay[DM.ROW_ID_STRAIGHT]);
        if (pl.p1Action <= DM.P1_PLAY_GIVE_IT_UP)
        {
            for (int j = 0; j <= DM.ROW_ID_GRANDE; ++j)
            {
                if (pl.rowPlay[j] > 0)
                    pl.rowPlay[j] = 0;
            }
        }


//Log.i(tag, "2 ??? col: " + pl.colId + ", computePlayValues(), diff: " + diff + ", pl.rowPlay[DM.ROW_ID_STRAIGHT]: " + pl.rowPlay[DM.ROW_ID_STRAIGHT]);
        // NEU: diffRow < 0 & isCombiOpo
        int diffRow = dm.getRowSum(pl.row, null) - dm.getRowSum(op.row, null);
//Log.i(tag, "1 col: " + pl.colId + ", computePlayValues(), diff: " + diff + ", diffRow: " + diffRow);
        boolean isCombiOpo = false;
        for (int j = DM.ROW_ID_STRAIGHT; j < DM.ROW_ID_GRANDE; ++j)
        {
            if (pl.rowOpo[j] > 0)
                isCombiOpo = true;
        }
        if  (       diffRow < 0 & isCombiOpo & dm.playerRoundesToGo <= dm.roundCheck6
                &   pl.rowPlay[DM.ROW_ID_GRANDE] > 0 & pl.p1Action >= DM.P1_PLAY_WEAK
            )
        {
            for (int j = DM.ROW_ID_A; j >= 0; --j)
            {
                if (diffRow < 0 & Math.abs(diffRow) <= (DM.ROW_MULTIPLIER[j] * 3) & pl.rowPlay[j] >= 0 & pl.rowPlay[j] < DM.ROW_MULTIPLIER[j] * 2)
                {
                    diffRow = diffRow + pl.rowPlay[j];
                    for (int k = 0; k < 3; k++)
                    {
                        if (diffRow < 0)
                        {
                            pl.rowPlay[j] = pl.rowPlay[j] + DM.ROW_MULTIPLIER[j];
                            diffRow = diffRow + DM.ROW_MULTIPLIER[j];
                        }
                    }
                }
                break;
            }
//Log.i(tag, "2 col: " + pl.colId + ", computePlayValues(), diff: " + diff + ", diffRow: " + diffRow);
        }

    }

    private int getEstimatedRowValue(int rowId, int movesToGo, boolean isDouble, boolean isMaxValue)
    {
        int bonus = 0;
        if (isMaxValue)
        {
            if (rowId == 6 | rowId == 7 | rowId == 8)    // straight, full, poker
                bonus = dm.bonusServed;
            if (rowId <= 5)
                return DM.ROW_MULTIPLIER[rowId] * 5;
            else
                return DM.ROW_MULTIPLIER[rowId] + bonus;
        }
        else
        {
            if (!isDouble | movesToGo <= 4)
            {
                if (rowId <= 5)
                    return DM.ROW_MULTIPLIER[rowId] * 3;
                else
                    return DM.ROW_MULTIPLIER[rowId];
            }
            else
            {
                bonus = 0;
                if (rowId == 6 | rowId == 7)    // straight, full
                    bonus = dm.bonusServed;
                if (rowId <= 5)
                    return DM.ROW_MULTIPLIER[rowId] * 4;
                else
                    return DM.ROW_MULTIPLIER[rowId] + bonus;
            }
        }
    }

    private void setEntryTable(char playerId)
    {
        for (int i = 0; i < entryCounter; i++)
        {
            for (int j = 0; j < entryRow; j++)
            {
                entryTable[i][j] = -1;
                entryRanking[i][j] = 0;
            }
        }
        for (int i = 0; i < entryCounter; i++)
        {
            setEntryTableCol(playerId, i);
        }

        computeEntryTable(playerId);

        if (dm.isLogging)
            loggingEntryTable(playerId);
    }

    private void setEntryTableCol(char playerId, int imageCnt)    // colId : image count 0 . . .  5
    {
//Log.i(tag, "1 ??? setEntryTableCol(), playerId: " + playerId + ", imageCnt: " + imageCnt);
        if (imageCnt == 0)
        {
            if (play0Col >= 0 & play0Row >= 0)
            {
                if (play0Row >= DM.ROW_ID_STRAIGHT)
                {
                    entryTable[imageCnt][6] = play0Col;
                    entryTable[imageCnt][7] = play0Row;
                }
                else
                    entryTable[imageCnt][play0Row] = play0Col;
            }

            for (int j = 0; j < DM.ROW_ID_STRAIGHT; j++)
            {
                for (int i = 0; i < dm.col; i++)
                {
                    Column pl = getPlayerColumn(playerId, i);
                    if (pl.rowPlay[j] == 0)
                    {
                        entryTable[0][j] = i;
                        if (j <= DM.ROW_ID_10 & (i == play0Col | pl.colRanking == 1))
                        {
                            entryTable[1][j] = i;
                            entryTable[2][j] = i;
                        }
                    }
                }
            }

        }
        else
        {
            for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
            {
                int iAction = 0;
                int iCol = -1;
                int iColPlayMore = -1;
                boolean isOr = false;
                boolean iPlayLess = false;
                int cAction = 0;
                int cCounter = 999;
                int cCol = -1;
                int lCol = -1;
                int cColSFP = -1;
                int pRanking = 99;
                int rRanking = 0;
                int cColGrande = -1;
                int cColGrandeBest = -1;
                int cColGrandeCounter = -1;
                boolean cPlayLess = false;
                boolean cPlayMore = false;
                int csAction = 0;
                int csCol = -1;
                for (int i = 0; i < dm.col; i++)
                {
                    Column pl = getPlayerColumn(playerId, i);
                    if (j < DM.ROW_ID_STRAIGHT)
                    {
                        if (pl.rowPlay[j] == DM.ROW_MULTIPLIER[j] * imageCnt & pl.p1Action >= iAction)
                        {
                            iCol = i;
                            iAction = pl.p1Action;
                            if  (!iPlayLess & (pl.rowPlay[j] == DM.ROW_MULTIPLIER[j] * 4 | pl.rowPlay[j] == DM.ROW_MULTIPLIER[j] * 3))
                            {
                                if (!(pl.colRanking == 3 & j <= DM.ROW_ID_10))
                                {
                                    iPlayLess = pl.isPlayLess;
                                    if (iPlayLess)
                                        lCol = i;
                                }
                            }
                            if (iColPlayMore < 0 & pl.rowPlay[j] == DM.ROW_MULTIPLIER[j] * 3 & pl.isPlayMore)
                                iColPlayMore = i;
                            if (pl.isOr)
                                isOr = pl.isOr;
                        }
                    }
                    else
                    {
                        int combiId = 0;
                        int pAction = pl.p1Action;
                        if (imageCnt == 5)
                            combiId = DM.ROW_ID_GRANDE;
                        if (imageCnt == 4)
                            combiId = DM.ROW_ID_POKER;
                        if (imageCnt == 3 | imageCnt == 2)
                            combiId = DM.ROW_ID_FULL;
                        if (imageCnt == 1)
                            combiId = DM.ROW_ID_STRAIGHT;
                        if (combiId > 0)
                        {
                            if (pl.rowPlay[combiId] == DM.ROW_MULTIPLIER[combiId] & pAction >= cAction)
                            {
                                cCol = i;
                                cAction = pAction;
                                cPlayMore = pl.isPlayMore;
                                if (pl.isOr)
                                    isOr = pl.isOr;
                            }
                            if  (       pl.rowPlay[combiId] == DM.ROW_MULTIPLIER[combiId]
                                    &   combiId >= DM.ROW_ID_STRAIGHT & combiId <= DM.ROW_ID_POKER
                                    &   dm.playerRoundesToGo >= dm.roundCheck16
                                    &   pl.colRanking > pRanking
                                )
                            {
                                cColSFP = i;
                                pRanking = pl.colRanking;
                            }
                            if (combiId == DM.ROW_ID_GRANDE & pl.rowPlay[combiId] == DM.ROW_MULTIPLIER[combiId] & pl.colRanking > rRanking)
                            {
                                cColGrande = i;
                                rRanking = pl.colRanking;
                            }
                            if  (       combiId == DM.ROW_ID_GRANDE & pl.rowPlay[combiId] == DM.ROW_MULTIPLIER[combiId]
                                    &   pl.playCounter <= -4 & pl.playCounter <= cCounter
                                )
                            {
                                cColGrandeCounter = i;
                                cCounter = pl.playCounter;
                            }
                            if  (       combiId == DM.ROW_ID_GRANDE & pl.rowPlay[combiId] == DM.ROW_MULTIPLIER[combiId]
                                    &   pAction == DM.P1_PLAY_GRANDE_BEST
                                )
                            {
                                cColGrandeBest = i;
                            }
                            if (pl.rowPlay[combiId] > DM.ROW_MULTIPLIER[combiId] & pAction >= csAction)
                            {
                                csCol = i;
                                csAction = pAction;
                                if (pl.isPlayLess)
                                    cPlayLess = pl.isPlayLess;
                                if (pl.isOr)
                                    isOr = pl.isOr;
                            }
                        }
                    }
                }
//Log.i(tag, "1a ??? setEntryTableCol(), cColGrande: " + cColGrande + ", cColGrandeCounter]: " + cColGrandeCounter + ", cColGrandeBest: " + cColGrandeBest);
                if (cColGrande >= 0)
                {
                    cCol = cColGrande;
                    Column pl = getPlayerColumn(playerId, cCol);
                    if (cColGrandeCounter >= 0 & pl.colValue <= 1)
                    {
                        Column plC = getPlayerColumn(playerId, cColGrandeCounter);
                        if (plC.p1Action >= DM.PLAY_P3)
                            cCol = cColGrandeCounter;
                    }
                }
                if (cColGrandeBest >= 0)
                    cCol = cColGrandeBest;
                if (cColSFP >= 0)
                    cCol = cColSFP;
//Log.i(tag, "1b ??? setEntryTableCol(), cCol: " + cCol);
//Log.i(tag, "2 ??? setEntryTableCol(), iCol: " + iCol + ", j: " + j + ", imageCnt: " + imageCnt);
                if (iCol >= 0)
                {
                    boolean isPlay = false;
                    if (entryTable[imageCnt][j] >= 0)
                    {
                        Column plI = getPlayerColumn(playerId, iCol);
                        Column pl = getPlayerColumn(playerId, entryTable[imageCnt][j]);
                        if (pl.isPlayMore & pl.colRanking > plI.colRanking)
                            isPlay = true;
                        if (j <= DM.ROW_ID_10 & (pl.colId == play0Col | pl.colRanking == 1))
                            isPlay = true;
                    }

                    if (!isPlay)
                        entryTable[imageCnt][j] = iCol;
                    if (iPlayLess & (imageCnt == 4 | imageCnt == 3))
                    {
                        if (lCol >= 0 & entryTable[imageCnt - 1][j] < 0)
                        {
                            entryTable[imageCnt - 1][j] = lCol;
                            entryRanking[imageCnt][j] = 1;
                        }
                    }
                    if (isOr)
                        entryRanking[imageCnt][j] = 2;
                    if (entryTable[imageCnt][j] == iCol)
                    {
//Log.i(tag, "3a ??? setEntryTableCol(), entryCol: " + entryTable[imageCnt][j] + ", j: " + j + ", imageCnt: " + imageCnt + ", iColPlayMore: " + iColPlayMore);
                        Column pl = getPlayerColumn(playerId, iCol);
                        if (pl.isPlayMore & imageCnt == 3)
                        {
                            if (entryTable[imageCnt + 1][j] < 0 & j >= DM.ROW_ID_J)
                                entryTable[imageCnt + 1][j] = iCol;
                        }
                        if (iColPlayMore >= 0 & imageCnt == 3)
                        {
                            if (entryTable[imageCnt + 1][j] < 0 & j >= DM.ROW_ID_J)
                                entryTable[imageCnt + 1][j] = iColPlayMore;
                        }
                        if (pl.isPlayMore & imageCnt == 4 & j == DM.ROW_ID_A)
                        {
                            if (entryTable[imageCnt + 1][j] < 0)
                                entryTable[imageCnt + 1][j] = iCol;
                        }
//Log.i(tag, "3b ??? setEntryTableCol(), entryCol: " + entryTable[imageCnt][j] + ", j: " + j + ", imageCnt: " + imageCnt + ", iColPlayMore: " + iColPlayMore);
//Log.i(tag, "3c ??? setEntryTableCol(), entryCol +1: " + entryTable[imageCnt + 1][j] + ", j: " + j);
                    }
                }

                if (cCol >= 0)
                {
                    entryTable[imageCnt][6] = cCol;
                    if (isOr)
                        entryRanking[imageCnt][6] = 2;
                    if (cPlayMore & cCol > 0 & entryTable[imageCnt][7] < 0)
                        entryTable[imageCnt][7] = cCol;
                }
                if (csCol < 0 & cCol >= 0 & imageCnt != 4)
                    csCol = cCol;
                if (csCol >= 0)
                {
                    entryTable[imageCnt][7] = csCol;
                    if (isOr)
                        entryRanking[imageCnt][7] = 2;
                    if (cPlayLess & entryTable[imageCnt][6] < 0)
                        entryTable[imageCnt][6] = csCol;
                }
            }
        }
        if (imageCnt == 5)
        {
            int ranking = 0;
            for (int i = 0; i < dm.col; i++)
            {
                Column pl = getPlayerColumn(playerId, i);
                if (pl.rowPlay[DM.ROW_ID_9] == DM.ROW_MULTIPLIER[DM.ROW_ID_9] & pl.colRanking > ranking)
                {
                    entryTable[1][DM.ROW_ID_9] = i;
                    ranking = pl.colRanking;
                }
            }
            ranking = 0;
            for (int i = 0; i < dm.col; i++)
            {
                Column pl = getPlayerColumn(playerId, i);
                if (pl.rowPlay[DM.ROW_ID_10] == DM.ROW_MULTIPLIER[DM.ROW_ID_10] & pl.colRanking > ranking)
                {
                    entryTable[1][DM.ROW_ID_10] = i;
                    ranking = pl.colRanking;
                }
            }
            ranking = 0;
            for (int i = 0; i < dm.col; i++)
            {
                Column pl = getPlayerColumn(playerId, i);
                if (pl.rowPlay[DM.ROW_ID_9] == DM.ROW_MULTIPLIER[DM.ROW_ID_9] * 2 & pl.colRanking > ranking)
                {
                    entryTable[2][DM.ROW_ID_9] = i;
                    ranking = pl.colRanking;
                }
            }
            ranking = 0;
            for (int i = 0; i < dm.col; i++)
            {
                Column pl = getPlayerColumn(playerId, i);
                if (pl.rowPlay[DM.ROW_ID_10] == DM.ROW_MULTIPLIER[DM.ROW_ID_10] * 2 & pl.colRanking > ranking)
                {
                    entryTable[2][DM.ROW_ID_10] = i;
                    ranking = pl.colRanking;
                }
            }
        }
    }

    private void computeEntryTable(char playerId)
    {
        for (int j = 0; j <= 5; j++)
        {
            if (entryTable[1][j] >= 0 & entryTable[2][j] < 0)
                entryTable[2][j] = entryTable[1][j];
            if (entryTable[2][j] >= 0 & entryTable[3][j] < 0)
                entryTable[3][j] = entryTable[2][j];

            if (entryRanking[4][j] == 1 & entryTable[4][j] >= 0 & entryTable[3][j] < 0)
            {
                if (entryTable[2][j] < 0)
                    entryTable[3][j] = entryTable[4][j];
                else
                    entryTable[3][j] = entryTable[2][j];
            }
        }
        if (entryTable[5][6] < 0)
        {
            int bCol = -1;
            int bAction = -1;
            for (int i = 0; i < dm.col; i++)
            {
                Column pl = getPlayerColumn(playerId, i);
                if  (pl.rowPlay[DM.ROW_ID_GRANDE] == 0 & pl.p1Action >= DM.PLAY_P3 & pl.p1Action >= bAction)
                {
                    bCol = i;
                    bAction = pl.p1Action;
                }
            }
            if (bCol >= 0)
            {
                entryTable[5][6] = bCol;
                entryTable[5][7] = bCol;
            }
        }

        boolean isEntry0 = false;
        int worstCol = 0;
        for (int i = 0; i < dm.col; i++)    // get worst Column
        {
            Column pl = getPlayerColumn(playerId, i);
            if (pl.colRanking == 1)
            {
                worstCol = i;
                break;
            }
        }
        int entry0Row = 0;
        for (int j = 0; j <= 5; j++)
        {
            if (isEntry0 & j > entry0Row)
            {
                entryTable[2][j] = -1;
                entryTable[1][j] = -1;
            }
            if (entryTable[0][j] == worstCol & (entryTable[1][j] == worstCol | entryTable[1][j] < 0))
            {
                entryTable[1][j] = worstCol;
                entryTable[2][j] = worstCol;
                if (j == DM.ROW_ID_9)
                {
                    if (entryTable[1][DM.ROW_ID_10] == worstCol)
                        entryTable[2][DM.ROW_ID_10] = worstCol;
                    entryTable[1][DM.ROW_ID_10] = -1;
                }
                if (j <= DM.ROW_ID_10)
                {
                    entry0Row = j + 1;
                    isEntry0 = true;
                }
            }
            else
            {
                if (entryTable[1][j] != worstCol & entryTable[2][j] < 0)
                    entryTable[2][j] = entryTable[1][j];
            }
        }
    }

    int getColFromEntryTable(int calcCol, int calcRow)
    {
        if (calcCol >= 1 & calcCol <= 3 & calcRow <= 5)
        {
            for (int i = calcCol; i > 0; i--)
            {
                if (entryTable[i][calcRow] >= 0)
                    return entryTable[i][calcRow];
            }
        }
        return entryTable[calcCol][calcRow];
    }

    int getBestColFromEntryTable(int calcCol, int calcRow)
    {
        int col = getColFromEntryTable(calcCol, calcRow);
        if (entryRanking[calcCol][calcRow] == 1 & col >= 0)
            return col;
        else
            return -1;
    }
    boolean isRowInEntryTable(int calcRow)
    {
        for (int i = 1; i < entryCounter; i++)
        {
            if (entryTable[i][calcRow] >= 0)
                return true;
        }
        return false;
    }

    boolean isBestColValue(char playerId, Column pl)
    {
        boolean isBest = false;
        boolean isBestRanking = false;
        if (pl.colRanking == dm.col)
            isBestRanking = true;
        int colValue = 1;
        int id = -1;
        for (int i = 0; i < dm.col; i++)
        {
            Column plC = getPlayerColumn(playerId, i);
            if (plC.colValue > colValue)
            {
                colValue = plC.colValue;
                id = i;
            }
        }
        if (colValue > 1 & pl.colId == id)
            isBest = true;
        if (colValue == 1 & isBestRanking)
            isBest = true;
        return isBest;
    }

    boolean isEvenColValue()
    {
        int minColValue = 999;
        int maxColValue = -999;
        for (int i = 0; i < dm.col; i++)
        {
            if (dm.colValues[i] < minColValue)
                minColValue = dm.colValues[i];
            if (dm.colValues[i] > maxColValue)
                maxColValue = dm.colValues[i];
        }
        if (minColValue == maxColValue)
            return true;
        else
            return false;
    }

    private void loggingPlayValues(char playerId)
    {
        for (int i = 0; i < dm.col; i++)
        {
            Column pl = getPlayerColumn(playerId, i);
            Column op = getOponentColumn(playerId, i);
            String plRow = "";
            String plPlay = "";
            String opRow = "";
            String opPlay = "";
            for (int j = 0; j <= DM.ROW_ID_GRANDE; j++)
            {
                if (pl.row[j] < 0) plRow = plRow + "-- ";
                if (pl.row[j] == 0) plRow = plRow + "00 ";
                if (pl.row[j] >= 10) plRow = plRow + pl.row[j] + " ";
                if (pl.row[j] > 0 & pl.row[j] < 10) plRow = plRow + "0" + pl.row[j] + " ";

                if (pl.rowPlay[j] < 0) plPlay = plPlay + "-- ";
                if (pl.rowPlay[j] == 0) plPlay = plPlay + "00 ";
                if (pl.rowPlay[j] >= 10) plPlay = plPlay + pl.rowPlay[j] + " ";
                if (pl.rowPlay[j] > 0 & pl.rowPlay[j] < 10) plPlay = plPlay + "0" + pl.rowPlay[j] + " ";

                if (op.row[j] < 0) opRow = opRow + "-- ";
                if (op.row[j] == 0) opRow = opRow + "00 ";
                if (op.row[j] >= 10) opRow = opRow + op.row[j] + " ";
                if (op.row[j] > 0 & op.row[j] < 10) opRow = opRow + "0" + op.row[j] + " ";

                if (pl.rowOpo[j] < 0) opPlay = opPlay + "-- ";
                if (pl.rowOpo[j] == 0) opPlay = opPlay + "00 ";
                if (pl.rowOpo[j] >= 10) opPlay = opPlay + pl.rowOpo[j] + " ";
                if (pl.rowOpo[j] > 0 & pl.rowOpo[j] < 10) opPlay = opPlay + "0" + pl.rowOpo[j] + " ";
            }
            String sumPlE = "" + dm.getRowSum(pl.row, null);
            if (sumPlE.length() == 2) sumPlE = "0" + sumPlE;
            if (sumPlE.length() == 1) sumPlE = "00" + sumPlE;
            String sumOpE = "" + dm.getRowSum(op.row, null);
            if (sumOpE.length() == 2) sumOpE = "0" + sumOpE;
            if (sumOpE.length() == 1) sumOpE = "00" + sumOpE;
            String sumPlP = "" + dm.getRowSum(pl.row, pl.rowPlay);
            if (sumPlP.length() == 2) sumPlP = "0" + sumPlP;
            if (sumPlP.length() == 1) sumPlP = "00" + sumPlP;
            String sumOpP = "" + dm.getRowSum(op.row, pl.rowOpo);
            if (sumOpP.length() == 2) sumOpP = "0" + sumOpP;
            if (sumOpP.length() == 1) sumOpP = "00" + sumOpP;
            plRow = plRow + sumPlE;
            opRow = opRow + sumOpE;
            plPlay = plPlay + sumPlP;
            opPlay = opPlay + sumOpP;
            int diffRow = dm.getRowSum(pl.row, null) - dm.getRowSum(op.row, null);
            int diff = dm.getRowSum(pl.row, pl.rowPlay) - dm.getRowSum(op.row, pl.rowOpo);
            String lessMore = "";
            if (pl.isPlayLess)
                lessMore = lessMore + "L";
            if (pl.isPlayMore)
                lessMore = lessMore + "M";
            Log.i(TAG, "   ");
            Log.i(TAG, playerId + ", col " + i + ", pl-E " + plRow + " op-E " + opRow + ", " + diffRow + " Pl " + pl.p1Action + "-" + pl.colRanking);
            Log.i(TAG, playerId + ", col " + i + ", pl-P " + plPlay + " op-P " + opPlay + ", " + diff + ", " + pl.playCounter + "(" + pl.rankingCounter + ") " + lessMore);
        }
    }

    private void loggingEntryTable(char playerId)
    {
        Log.i(TAG, "setEntryTable():");
        Log.i(TAG, playerId + "  5  4  3  2  1  0");
        Log.i(TAG, "-------------------");
        for (int j = 0; j < entryRow; j++)
        {
            String txt = "" + j + " ";
            if (j == 6) txt = "c ";
            if (j == 7) txt = "s ";
            for (int i = entryCounter -1; i >= 0; i--)
            {
                char rank = ' ';
                if (entryRanking[i][j] == 1)    rank = '*';
                if (entryRanking[i][j] == 2)    rank = '|';
                if (entryTable[i][j] == -1)
                    txt = txt + " -" + rank;
                else
                    txt = txt + " " + entryTable[i][j] + rank;
            }
            Log.i(TAG, txt);
        }
        Log.i(TAG, "   ");
    }

    private final String TAG = "Player";
    private DM dm;
    private ArrayList<Column> playerA = new ArrayList<>();
    private ArrayList<Column> playerB = new ArrayList<>();
    private ArrayList<Column> playerC = new ArrayList<>();

    private int entryCounter = 6;    // image counter
    private int entryRow = 8;    // 0...5 : imageId, 6 : combi, 7 : combi served
    public int entryTable[][] = new int[entryCounter][entryRow];
    public int entryRanking[][] = new int[entryCounter][entryRow];    // ranking: image/combi

    int play0Col = -1;
    int play0Row = -1;
    boolean play0ColIsCancel = false;
    boolean isAllPlay0 = false;
}
