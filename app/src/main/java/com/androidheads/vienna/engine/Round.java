package com.androidheads.vienna.engine;

import android.util.Log;

class Round
{

    Round(DM dm, char playerId, int diceTry)
    {
        this.dm = dm;
        this.playerId = playerId;
        this.diceTry = diceTry;
    }

    void computeRoundValues()
    {
//Log.i(tag, "computeRoundValues(), dm: " + dm + ", dm.player: " + dm.player);
        setPlan();
        if (dm.playerIsP1)
        {
            entry0Col = -1;
            entry0Row = -1;
        }
        for (int i = 0; i < imageCount.length; i++)
        {
            imageCount[i] = 0;
        }
        int cntRollEntry = 0;
        diceAll = "";
        for (int i = 0; i < diceRoll.length; i++)
        {
            if (diceRoll[i] >= 0)
            {
                imageCount[diceRoll[i]] = imageCount[diceRoll[i]] + 1;
                diceAll = diceAll + diceRoll[i];
                cntRollEntry++;
            }
            else
            {
                if (diceHold[i] >= 0)
                {
                    imageCount[diceHold[i]] = imageCount[diceHold[i]] + 1;
                    diceAll = diceAll +  diceHold[i];
                }
            }
        }
        if (imageCount[0] == 1 & imageCount[1] == 1  & imageCount[2] == 1 & imageCount[3] == 1 & imageCount[4] == 1)
            isStraight = true;
        if (imageCount[1] == 1  & imageCount[2] == 1 & imageCount[3] == 1 & imageCount[4] == 1 & imageCount[5] == 1)
            isStraight = true;
        int cntTwoPair = 0;
        for (int diceValue : imageCount)
        {
            if (diceValue == 5)
                isGrande = true;
            if (diceValue == 4)
                isPoker = true;
            if (diceValue == 3)
                is3 = true;
            if (diceValue == 2)
            {
                is2 = true;
                cntTwoPair++;
            }
        }
        if (cntTwoPair == 2)
            isTwoPair = true;
        if (is3 & is2)
            isFull = true;
        if (cntRollEntry == 5 & (isStraight | isFull | isPoker | isGrande))
            isServed = true;

        for (int i = 0; i < diceResult.length; i++)
        {
            diceResult[i] = 0;
        }
        bestImageId = 0;
        int highestDiceValue = 0;
        firstPairId = -1;
        for (int i = 0; i < imageCount.length; i++)
        {
            if (imageCount[i] >= highestDiceValue)
            {
                if ((isTwoPair | isFull) & imageCount[i] == 2 & firstPairId < 0)
                    firstPairId = i;
                bestImageId = i;
                highestDiceValue = imageCount[i];
            }
            if (imageCount[i] > 0)
                diceResult[i] = DM.ROW_MULTIPLIER[i] * imageCount[i];
        }
        bestCombiId = 0;
        int serv = 0;
        if (isServed)
            serv = dm.bonusServed;
        if (isStraight)
        {
            diceResult[DM.ROW_ID_STRAIGHT] = DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT] + serv;
            bestCombiId = DM.ROW_ID_STRAIGHT;
        }
        if (isFull)
        {
            diceResult[DM.ROW_ID_FULL] = DM.ROW_MULTIPLIER[DM.ROW_ID_FULL] + serv;
            bestCombiId = DM.ROW_ID_FULL;
            for (int i = 0; i < imageCount.length; i++)
            {
                if (imageCount[i] == 2)
                {
                    firstPairId = i;
                    break;
                }
            }
        }
        if (isPoker)
        {
            diceResult[DM.ROW_ID_POKER] = DM.ROW_MULTIPLIER[DM.ROW_ID_POKER] + serv;
            bestCombiId = DM.ROW_ID_POKER;
        }
        if (isServed)
            serv = dm.bonusServedGrande;
        if (isGrande)
        {
            diceResult[DM.ROW_ID_GRANDE] = DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE] + serv;
            bestCombiId = DM.ROW_ID_GRANDE;
        }
        for (int i = 0; i < dm.col; ++i)
        {
            Column pl = dm.player.getPlayerColumn(playerId, i);
            for (int j = 0; j < DM.row; ++j)
            {
                if (pl.row[j] == -1)
                    pl.rowResult[j] = diceResult[j];
                else
                    pl.rowResult[j] = -1;
                dm.player.setPlayerColumn(playerId, i, pl);
            }
        }
    }

    boolean setResult()
    {
        bestCol = -1;
        bestRow = -1;

        fastResult();

        if (bestCol >= 0 & bestRow >= 0)
        {
            Column pl = dm.player.getPlayerColumn(playerId, bestCol);
            bestValue = diceResult[bestRow];
            bestRating = pl.p1Action ;
            setEntryValue(bestCol, bestRow);
            return true;
        }
        else
        {
            if (diceTry == 3)
            {
                if (dm.isLogging)
                    Log.i(TAG, "diceTry == 3, set next entry");
                for (int i = 0; i < dm.col; ++i)
                {
                    Column pl = dm.player.getPlayerColumn(playerId, i);
                    for (int j = 0; j < DM.row; j++)
                    {
                        if (pl.row[j] < 0)
                        {
                            bestCol = i;
                            bestRow = j;
                            bestValue = diceResult[j];
                            bestRating = pl.p1Action ;
                            isBestEntry = false;
                            setEntryValue(bestCol, bestRow);
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    private void fastResult()
    {
        if (bestCombiId > 0)
            calcCombiEntry();
        if (bestCol < 0)
        {
            if (diceTry == 3)
                calcImageEntry();
            else
                setRollHold();
        }
    }

    private void calcCombiEntry()
    {
//Log.i(tag, "0 ??? calcCombiEntry()");
        int grandeCol = dm.player.getColFromEntryTable(5, 6);
        int grandeImageCol = dm.player.getColFromEntryTable(5, bestImageId);
        if (diceTry < 3)    // diceTry < 3 : roll/hold (full house, poker, grande )
        {
            if (!isServed & bestCombiId == DM.ROW_ID_POKER & (grandeImageCol >= 0 | grandeCol >= 0))
                return;
            if  (       isServed & bestCombiId == DM.ROW_ID_POKER & grandeCol >= 0
                    &   !isPlayValue(DM.ROW_ID_POKER, DM.ROW_MULTIPLIER[DM.ROW_ID_POKER] + dm.bonusServed, false)
                    &   dm.player.getMovesToGo(playerId) <= dm.roundCheck12
                )
                return;
            if (!isServed & bestCombiId != DM.ROW_ID_GRANDE & imageCount[bestImageId] >= 2 & getRowCnt(bestImageId, true) == dm.col)
                return;
            if (!isServed & bestCombiId == DM.ROW_ID_FULL & getRowCnt(bestImageId, true) > getRowCnt(DM.ROW_ID_FULL, true))
                return;
            if (!isServed & bestCombiId == DM.ROW_ID_POKER & getRowCnt(bestImageId, true) > getRowCnt(DM.ROW_ID_POKER, true))
                return;
        }
//Log.i(tag, "1 ??? calcCombiEntry(), check entryTable");
        int calcCol = 0;
        if (bestCombiId == DM.ROW_ID_GRANDE)    calcCol = 5;
        if (bestCombiId == DM.ROW_ID_POKER)     calcCol = 4;
        if (bestCombiId == DM.ROW_ID_FULL)      calcCol = 3;
        if (bestCombiId == DM.ROW_ID_STRAIGHT)  calcCol = 1;
        int calcRow = 6;
        int calcRowServed = 7;
        if (isServed)
        {
            boolean isServeOK = true;
            if (dm.isDouble & dm.roundDouble1 != null)
            {
                if (dm.roundDouble1.bestCombiId == DM.ROW_ID_GRANDE & bestCombiId != DM.ROW_ID_GRANDE & diceTry < 3)
                    return;
            }
            if (dm.isDouble & dm.doubleServedId > 0)
            {
                if (bestCombiId > DM.ROW_ID_STRAIGHT & dm.doubleServedId == DM.ROW_ID_STRAIGHT)
                    isServeOK = false;
                if (bestCombiId < DM.ROW_ID_POKER & dm.doubleServedId == DM.ROW_ID_POKER)
                    isServeOK = false;
                if (bestCombiId == dm.doubleServedId & diceTry < 3)
                    isServeOK = false;
                if (bestCombiId == DM.ROW_ID_POKER & dm.doubleServedId < DM.ROW_ID_POKER)
                    isServeOK = true;
                if (diceTry == 3)
                    isServeOK = true;
            }
            if (bestCombiId == DM.ROW_ID_POKER)
            {
                if (dm.player.getColFromEntryTable(4, calcRowServed) < 0 & dm.player.getColFromEntryTable(5, calcRow) >= 0)
                    isServeOK = false;
            }
            int bCol = dm.player.getColFromEntryTable(calcCol, calcRowServed);
            if  (       bCol < 0 & bestCombiId == DM.ROW_ID_POKER & getBestP1Action(bestCombiId, true) >= DM.PLAY_P3
                    &   getBestP1Action(bestImageId, true) < DM.P1_PLAY_POSIBLE_WIN
                )
                bCol = dm.player.getColFromEntryTable(calcCol, calcRow);
            if (bCol >= 0 & isServeOK)
            {
                bestCol = bCol;
                bestRow = bestCombiId;
                if (dm.isLogging)
                    Log.i(TAG, "1 calcCombiEntry(), served, col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
                return;
            }
        }

        if (bestCombiId == DM.ROW_ID_STRAIGHT)
        {
            int cCol = dm.player.getColFromEntryTable(calcCol, calcRowServed);
            if (isServed & cCol >= 0)
            {
                bestCol = cCol;
                bestRow = bestCombiId;
                if (dm.isLogging)
                    Log.i(TAG, "2a calcCombiEntry(), straight served, col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
                return;
            }
            cCol = dm.player.getColFromEntryTable(calcCol, calcRow);
            if (cCol >= 0)
            {
                bestCol = cCol;
                bestRow = bestCombiId;
                if (dm.isLogging)
                    Log.i(TAG, "2b calcCombiEntry(), straight, col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
                return;
            }
            int bCol = -1;
            int action = 0;
            for (int i = 0; i < dm.col; ++i)
            {
                Column pl = dm.player.getPlayerColumn(playerId, i);
                if  (pl.rowPlay[bestCombiId] >= 0 & pl.p1Action >= action & pl.p1Action >= DM.P1_PLAY)
                {
                    bCol = i;
                    action = pl.p1Action;
                }
            }
            if (bCol >= 0)
            {
                bestCol = bCol;
                bestRow = bestCombiId;
                if (dm.isLogging)
                    Log.i(TAG, "2c calcCombiEntry(), straight isPlayMore, col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
                return;
            }
        }

        if (bestCombiId == DM.ROW_ID_FULL)
        {
//Log.i(tag, "2 ??? calcCombiEntry(), check DM.ROW_ID_FULL");
            int iCol = dm.player.getBestColFromEntryTable(calcCol, bestImageId);
            int cCol = dm.player.getColFromEntryTable(calcCol, calcRow);
            int cntImage = getRowCnt(bestImageId, true);
            int cntCombi = getRowCnt(DM.ROW_ID_FULL, true);
            if (iCol >= 0 & (cntImage > cntCombi) | cCol < 0)
            {
                bestCol = iCol;
                bestRow = bestImageId;
                if (dm.isLogging)
                    Log.i(TAG, "2 calcCombiEntry(), full house(best image), col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
                return;
            }
        }
        if (bestCombiId == DM.ROW_ID_POKER)
        {
//Log.i(tag, "3 ??? calcCombiEntry(), check DM.ROW_ID_POKER");
            int bCol = dm.player.getColFromEntryTable(5, calcRow);
            int bColCombi = dm.player.getColFromEntryTable(calcCol, calcRow);
            if (bCol >= 0 & diceTry < 3)
                return;
            bCol = dm.player.getColFromEntryTable(calcCol, bestImageId);
            int colRanking = 1;
            boolean isLess = false;
            if (bCol >= 0)
            {
                Column pl = dm.player.getPlayerColumn(playerId, bCol);
                if (pl.colRanking == dm.col)
                    colRanking = dm.col;
                isLess = pl.isPlayLess;
            }
            boolean isImage = false;
            if (getRowCnt(bestImageId, true) > getRowCnt(DM.ROW_ID_POKER, true))
                isImage = true;
            if (getRowCnt(bestImageId, true) >= 2 & dm.playerRoundesToGo >= DM.ROUND_CHECK_8)
                isImage = true;
            if (getRowCnt(bestImageId, true) >= 1 & dm.playerRoundesToGo >= DM.ROUND_CHECK_8 & bestImageId >= DM.ROW_ID_Q)
                isImage = true;
            if (getRowCnt(bestImageId, true) >= 1 & getBestP1Action(bestImageId, true) >= DM.P1_PLAY_BEST)
                isImage = true;
            if (dm.isDouble)
                isImage = true;
            if (bCol >= 0 & bCol == bColCombi)
                isImage = true;
            if (colRanking != dm.col & getRowCnt(bestImageId, true) == 1 & getRowCnt(DM.ROW_ID_POKER, true) - getRowCnt(bestImageId, true) >= 2)
                isImage = false;
            if (isLess)
                isImage = false;
            if (bCol >= 0 & isImage)
            {
                if (diceTry < 3)
                    return;
                bestCol = bCol;
                bestRow = bestImageId;
                if (dm.isLogging)
                    Log.i(TAG, "3 calcCombiEntry(), four of kind, col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
                return;
            }


            int sCol = dm.player.getColFromEntryTable(calcCol, calcRowServed);
//Log.i(tag, "1 ??? 3a calcCombiEntry(), bCol: " + bCol + ", sCol: " + sCol);
            if  (bColCombi < 0 & sCol >= 0 & bCol < 0 & bestCombiId == DM.ROW_ID_POKER & dm.playerRoundesToGo <= DM.ROUND_CHECK_16)
            {
                Column pl = dm.player.getPlayerColumn(playerId, sCol);
                if (pl.rowPlay[bestCombiId] >= 0)
                {
                    if (pl.rowPlay[DM.ROW_ID_POKER] > DM.ROW_MULTIPLIER[DM.ROW_ID_POKER] & pl.rowOpo[DM.ROW_ID_POKER] > DM.ROW_MULTIPLIER[DM.ROW_ID_POKER])
                        bCol = sCol;
                    if (pl.rowPlay[DM.ROW_ID_STRAIGHT] < 0 & pl.rowOpo[DM.ROW_ID_STRAIGHT] >= DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT])
                        bCol = sCol;
                    if (pl.rowPlay[DM.ROW_ID_GRANDE] == DM.ROW_MULTIPLIER[DM.ROW_ID_GRANDE])
                        bCol = sCol;
                }
            }
            if (bColCombi >= 0 & sCol >= 0)
            {
                bestCol = bColCombi;
                bestRow = bestCombiId;
                if (dm.isLogging)
                    Log.i(TAG, "3a calcCombiEntry(), poker(weak): " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
                return;
            }
        }
        if (bestCombiId == DM.ROW_ID_GRANDE)
        {
//Log.i(tag, "4 ??? calcCombiEntry(), check DM.ROW_ID_GRANDE");
            int bCol = dm.player.getColFromEntryTable(5, bestImageId);
            if (bCol >= 0 & bCol == dm.col -1)
            {
                Column pl = dm.player.getPlayerColumn(playerId, bCol);
                if  (       pl.row[DM.ROW_ID_GRANDE] > 0 & getRowCnt(DM.ROW_ID_GRANDE, true) <= 1
                        |   (dm.playerRoundesToGo > dm.maxRoundes * 0.9 & bestImageId == DM.ROW_ID_A)
                    )
                {
                    bestCol = bCol;
                    bestRow = bestImageId;
                    if (dm.isLogging)
                        Log.i(TAG, "4a calcCombiEntry(), five of kind, col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
                    return;
                }
            }
            if (bCol < 0)
            {
                if (dm.player.isEvenColValue())
                {
                    int ranking = 999;
                    for (int i = 0; i < dm.col; ++i)
                    {
                        Column pl = dm.player.getPlayerColumn(playerId, i);
                        if  (       pl.rowPlay[bestCombiId] == DM.ROW_MULTIPLIER[bestCombiId]
                                &   pl.p1Action >= DM.P1_PLAY_GRANDE_ATTACK & pl.colRanking < ranking
                            )
                        {
                            bCol = i;
                            ranking = pl.colRanking;
                        }
                    }
                    if (bCol >= 0)
                    {
                        bestCol = bCol;
                        bestRow = bestCombiId;
                        if (dm.isLogging)
                            Log.i(TAG, "4b calcCombiEntry(), best combi(even col value), col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
                        return;
                    }
                }
                for (int i = 0; i < dm.col; ++i)
                {
                    Column pl = dm.player.getPlayerColumn(playerId, i);
                    if (dm.player.isBestColValue(playerId, pl) & pl.rowPlay[bestCombiId] == DM.ROW_MULTIPLIER[bestCombiId]
                            & pl.playCounter >= -6 & pl.playCounter <= 4)
                    {
                        bestCol = pl.colId;
                        bestRow = bestCombiId;
                        if (dm.isLogging)
                            Log.i(TAG, "4c calcCombiEntry(), best grande, col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
                        return;
                    }
                }
            }
        }
//Log.i(tag, "5 ??? calcCombiEntry(), no image entry");
        if (!isServed)
        {
            int bCol = dm.player.getColFromEntryTable(calcCol, calcRow);

            if (bCol >= 0)
            {
                bestCol = bCol;
                bestRow = bestCombiId;
                if (dm.isLogging)
                    Log.i(TAG, "5 calcCombiEntry(), best combi, col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
                return;
            }
        }
        if (bestCol < 0 & bestCombiId >= DM.ROW_ID_STRAIGHT & bestCombiId <= DM.ROW_ID_POKER)
        {
            for (int i = 0; i < dm.col; ++i)
            {
                Column pl = dm.player.getPlayerColumn(playerId, i);
                if (pl.rowPlay[bestCombiId] == 0 & pl.p1Action >= DM.P1_PLAY_WEAK)
                {
                    for (int j = 0; j < DM.ROW_ID_STRAIGHT; ++j)
                    {
                        if (pl.rowPlay[j] > 0)
                        {
                            bestCol = i;
                            bestRow = bestCombiId;
                            if (dm.isLogging)
                                Log.i(TAG, "6 calcCombiEntry(), bestCombiId: : " + bestCombiId + ", col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
                            return;
                        }
                    }
                }
            }
        }

        int cCol = -1;
        int action = DM.P1_PLAY_WEAK;
        int cntPlay0 = 0;
        boolean isPlayCombiGreater0 = false;
        for (int i = 0; i < dm.col; ++i)
        {
            Column pl = dm.player.getPlayerColumn(playerId, i);
            if (pl.rowPlay[bestCombiId] > 0)
                isPlayCombiGreater0 = true;
            if (pl.rowPlay[bestCombiId] == 0 & pl.p1Action >= action)
            {
                for (int j = 0; j <= bestCombiId; ++j)
                {
                    if (pl.rowPlay[j] >= 0)
                    {
                        cCol = i;
                        action = pl.p1Action;
                    }
                    if (pl.rowPlay[j] == 0)
                        cntPlay0++;
                }
            }
        }
        if (!isPlayCombiGreater0 & cCol >= 0 & cntPlay0 >= 2)
        {
            bestCol = cCol;
            bestRow = bestCombiId;
            if (dm.isLogging)
                Log.i(TAG, "7 calcCombiEntry(), best combi, rowPlay == 0, col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
        }

    }

    private void calcImageEntry()
    {
        int imageId = bestImageId;
        int f2Col = -1;
        if (firstPairId >= 0 & bestCombiId == 0)
        {
            f2Col = dm.player.getColFromEntryTable(2, firstPairId);
            if (f2Col >= 0)
                imageId = firstPairId;
        }
        int imageCnt = imageCount[imageId];
        int bImageId = imageId;
        if (imageCnt == 1)
        {
            int iCol = -1;
            int iId = 0;
            for (int i = 0; i < imageCount.length; i++)
            {
                if (iCol <= 0)
                {
                    iCol = dm.player.getColFromEntryTable(imageCnt, i);
                    iId = i;
                }
            }
            if (iCol >= 0)
                imageId = iId;
        }

        int bCol = dm.player.getColFromEntryTable(imageCnt, imageId);
        if (bCol < 0 & imageCnt == 2)
        {
            if (firstPairId >= 0)
            {
                int i1Col = dm.player.getColFromEntryTable(1, firstPairId);
                int i0Col = dm.player.getColFromEntryTable(0, firstPairId);
                if (i1Col >= 0)
                {
                    bCol = i1Col;
                    imageId = firstPairId;
                }
                else
                {
                    if (i0Col >= 0)
                    {
                        bCol = i0Col;
                        imageId = firstPairId;
                    }
                }
            }
            else
            {
                int i1Col = dm.player.getColFromEntryTable(1, imageId);
                int i0Col = dm.player.getColFromEntryTable(0, imageId);
                if (i1Col >= 0)
                    bCol = i1Col;
                else
                {
                    if (i0Col >= 0)
                        bCol = i0Col;
                }
            }
            if (bCol < 0 & imageId <= DM.ROW_ID_10 & imageCount[imageId] == 2)
            {
                int ranking = 99;
                for (int i = 0; i < dm.col; ++i)
                {
                    Column pl = dm.player.getPlayerColumn(playerId, i);
                    if (pl.colRanking < dm.col & pl.colRanking < ranking & pl.playCounter >= 0 & pl.cntEmpty >= 3
                            & pl.rowPlay[imageId] >= 0 & pl.rowPlay[imageId] <= DM.ROW_MULTIPLIER[imageId] * 3)
                    {
                        bCol = i;
                        ranking = pl.colRanking;
                    }
                }
            }
        }
//Log.i(tag, "1 ??? calcImageEntry(), bCol: " + bCol + ", bRow: " + imageId + ", value: " + diceResult[imageId] + ", imageCnt: " + imageCnt);
        if (bCol < 0 & imageCnt == 2)
        {
            int bColFirst = -1;
            if (firstPairId >= 0)
            {
                bColFirst = dm.player.getColFromEntryTable(imageCnt, firstPairId);
                if (bColFirst >= 0)
                {
                    bCol = bColFirst;
                    imageId = firstPairId;
                }
                if (bCol < 0 & firstPairId <= DM.ROW_ID_10 & imageCount[firstPairId] == 2)
                {
                    int ranking = 99;
                    for (int i = 0; i < dm.col; ++i)
                    {
                        Column pl = dm.player.getPlayerColumn(playerId, i);
                        if (pl.colRanking < dm.col & pl.colRanking < ranking & pl.playCounter >= 0 & pl.cntEmpty >= 3
                                & pl.rowPlay[firstPairId] >= 0 & pl.rowPlay[firstPairId] <= DM.ROW_MULTIPLIER[firstPairId] * 3)
                        {
                            bCol = i;
                            imageId = firstPairId;
                            ranking = pl.colRanking;
                        }
                    }
                }
            }
//Log.i(tag, "1b ??? calcImageEntry(), bCol: " + bCol + ", bRow: " + imageId + ", firstPairId: " + firstPairId + ", bColFirst: " + bColFirst);
            if (imageId >= DM.ROW_ID_J | bCol < 0)
            {
                int iCol = -1;
                iCol = dm.player.getColFromEntryTable(1, imageId);
                if (iCol < 0)
                    iCol = dm.player.getColFromEntryTable(0, imageId);
                if (iCol >= 0)
                    bCol = iCol;
                else
                {
                    for (int i = 0; i <= DM.ROW_ID_10; i++)
                    {
                        iCol = dm.player.getColFromEntryTable(1, i);
                        if (iCol >= 0)
                        {
                            bCol = iCol;
                            imageId = i;
                            break;
                        }
                    }
                    if (iCol < 0)
                    {
                        for (int i = 0; i <= DM.ROW_ID_10; i++)
                        {
                            iCol = dm.player.getColFromEntryTable(0, i);
                            if (iCol >= 0)
                            {
                                bCol = iCol;
                                imageId = i;
                                break;
                            }
                        }
                    }
                }
            }
        }
//Log.i(tag, "2a ??? calcImageEntry(), bCol: " + bCol + ", bRow: " + imageId + ", value: " + diceResult[imageId]);
        if (bCol < 0 & imageCnt == 3 & getRowCnt(bestImageId, true) == 1)
        {
            int iCol = dm.player.getColFromEntryTable(4, imageId);
            if (iCol >= 0)
            {
                Column plW = dm.player.getPlayerColumn(playerId, iCol);
                if (diceResult[imageId] >= plW.rowPlay[imageId])
                    bCol = iCol;
            }
        }
//Log.i(tag, "2b ??? calcImageEntry(), bCol: " + bCol + ", bRow: " + imageId + ", value: " + diceResult[imageId]);
        if (bCol < 0 & imageCnt == 3)
        {
            for (int i = imageCnt; i >= 0; i--)
            {
                int xCol = dm.player.getColFromEntryTable(i, imageId);
                if (xCol >= 0)
                {
                    bCol = xCol;
                    break;
                }
            }
        }

        if (bCol < 0 & imageCnt <= 2)
        {
            int iCol = -1;
            for (int i = imageId -1; i >= 0; i--)
            {
                if (iCol < 0 & imageCount[i] > 0)
                {
                    iCol = dm.player.getColFromEntryTable(imageCount[i], i);
                    if (iCol >= 0)
                    {
                        Column plW = dm.player.getPlayerColumn(playerId, iCol);
                        if (plW.colRanking == 1)
                        {
                            bCol = iCol;
                            imageId = i;
                            imageCnt = imageCount[i];
                            break;
                        }
                    }
                }
            }
        }
//Log.i(tag, "3 ??? calcImageEntry(), bCol: " + bCol + ", bRow: " + imageId + ", value: " + diceResult[imageId]);
        if (bCol < 0 & imageCnt == 4)
            bCol = dm.player.getColFromEntryTable(3, imageId);
        if (bCol < 0)
        {
            int b4Col = dm.player.getColFromEntryTable(4, imageId);
            if (b4Col >= 0 & imageCount[imageId] == 3 & getRowCnt(imageId, true) >= 1)
            {
                Column plW = dm.player.getPlayerColumn(playerId, b4Col);
                if (plW.rowPlay[DM.ROW_ID_FULL] >= 0 & bestCombiId > 0)
                {
                    bestCol = bCol;
                    bestRow = DM.ROW_ID_FULL;
                    if (dm.isLogging)
                        Log.i(TAG, "0 calcImageEntry(), imageCnt == 3 --> full, col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
                    return;
                }
            }
            if (bCol < 0)
            {
                for (int i = 0; i < imageCount.length; i++)
                {
                    int iCount = imageCount[i];
                    if (iCount > 0 & bCol < 0)
                    {
                        bCol = dm.player.getColFromEntryTable(iCount, i);
                        if (bCol >= 0)
                            imageId = i;
                    }
                    if (iCount - 1 >= 3 & bCol < 0)
                        bCol = dm.player.getColFromEntryTable(iCount - 1, i);
                    if (iCount - 2 >= 3 & bCol < 0)
                        bCol = dm.player.getColFromEntryTable(iCount - 2, i);
                }
            }
        }
//Log.i(tag, "4 ??? calcImageEntry(), bCol: " + bCol + ", bRow: " + imageId + ", value: " + diceResult[imageId]);
        if (bCol >= 0 & imageCnt == 2 & dm.player.getMovesToGo(playerId) >= dm.roundCheck16)
        {
            int lCol = -1;
            int lRow = -1;
            int xCol = dm.player.getColFromEntryTable(2, DM.ROW_ID_10);
            if (xCol >= 0)
            {
                lCol = xCol;
                lRow = DM.ROW_ID_10;
            }
            xCol = dm.player.getColFromEntryTable(2, DM.ROW_ID_9);
            if (xCol >= 0)
            {
                lCol = xCol;
                lRow = DM.ROW_ID_9;
            }
            xCol = dm.player.getColFromEntryTable(1, DM.ROW_ID_10);
            if (xCol >= 0)
            {
                lCol = xCol;
                lRow = DM.ROW_ID_10;
            }
            xCol = dm.player.getColFromEntryTable(1, DM.ROW_ID_9);
            if (xCol >= 0)
            {
                lCol = xCol;
                lRow = DM.ROW_ID_9;
            }

            if (lCol >= 0 & lRow >= 0)
            {
                Column plW = dm.player.getPlayerColumn(playerId, lCol);
                if (!dm.player.isBestColValue(playerId, plW) & imageCount[lRow] >= imageCnt)
                {
                    bCol = lCol;
                    if (!(plW.rowPlay[imageId] == 0 | plW.rowPlay[imageId] == DM.ROW_MULTIPLIER[imageId]))
                        imageId = lRow;
                }
            }
        }
//Log.i(tag, "5 ??? calcImageEntry(), bCol: " + bCol + ", bRow: " + imageId + ", value: " + diceResult[imageId]);
        if (bCol >= 0 & bestImageId < imageId & imageCount[bestImageId] > imageCount[imageId])
        {
            for (int i = imageCount[bestImageId]; i >= 0; i--)
            {
                int checkCol = dm.player.getColFromEntryTable(i, bestImageId);
                if (checkCol >= 0 & checkCol == bCol)
                {
                    imageId = bestImageId;
                    break;
                }
            }
        }
//Log.i(tag, "6a ??? calcImageEntry(), play0Col: " + dm.player.play0Col + ", play0Row: " + dm.player.play0Row + ", value: " + diceResult[imageId]);
        if (bCol >= 0 & bCol == dm.player.play0Col & imageId == dm.player.play0Row)
        {
//Log.i(tag, "6b ??? calcImageEntry(), col: " + bCol + ", bRow: " + imageId + ", value: " + diceResult[imageId]);
            Column pl = dm.player.getPlayerColumn(playerId, bCol);
            if (pl.colRanking > 1)
            {
                int bValue = diceResult[imageId];
                for (int j = 0; j <= DM.ROW_ID_GRANDE; ++j)
                {
                    boolean isCountBetter = true;
                    if (pl.rowPlay[j] >= 0 & diceResult[j] > bValue)
                    {
                        if (j < DM.ROW_ID_STRAIGHT)
                        {
                            if (imageCount[j] < imageCount[imageId] | imageCount[j] < 3)
                                isCountBetter = false;
                            if (isCountBetter)
                            {
                                bValue = diceResult[j];
                                imageId = j;
                            }
                        }
                        else
                        {
                            if (pl.rowPlay[j] >= 0 & diceResult[j] > bValue)
                            {
                                bValue = diceResult[j];
                                imageId = j;
                            }
                        }
                    }
                }
            }
        }
//Log.i(tag, "6 ??? calcImageEntry(), bCol: " + bCol + ", bRow: " + imageId + ", value: " + diceResult[imageId]);
        if (bCol >= 0 & imageId >= 0 & imageId < DM.ROW_ID_STRAIGHT)
        {
            if (bCol == dm.player.play0Col & imageCnt > imageCount[imageId])
            {
                for (int i = imageCnt; i >= 0; i--)
                {
                    int iCol = dm.player.getColFromEntryTable(i, bImageId);
                    if (iCol >= 0)
                    {
                        bCol = iCol;
                        imageId = bImageId;
                        break;
                    }
                }
            }
        }
//Log.i(tag, "7 ??? calcImageEntry(), bCol: " + bCol + ", bRow: " + imageId + ", value: " + diceResult[imageId]);
        if (bCol >= 0)
        {
            bestCol = bCol;
            bestRow = imageId;
            if (dm.isLogging)
                Log.i(TAG, "1a calcImageEntry(), col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
            return;
        }

        if (bestCombiId > 0)
        {
            int xCol = -1;
            for (int i = 0; i < dm.col; ++i)
            {
                Column pl = dm.player.getPlayerColumn(playerId, i);
                if (pl.rowPlay[bestCombiId] >= 0)
                    xCol = i;
            }
            if (xCol >= 0)
            {
                bestCol = xCol;
                bestRow = bestCombiId;
                if (dm.isLogging)
                    Log.i(TAG, "1b calcImageEntry(), set combi, col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
                return;
            }
        }

        bCol = -1;
        int bRow = -1;

        // NEU
        if (bCol < 0)
        {
            if (dm.player.play0Col >= 0)
            {
                int bValue = 0;
                Column pl = dm.player.getPlayerColumn(playerId, dm.player.play0Col);
                for (int j = 0; j <= 5; ++j)
                {
                    if (pl.rowPlay[j] >= 0 & imageCount[j] >= 2 & diceResult[j] > bValue)
                    {
                        bValue = diceResult[j];
                        bCol = dm.player.play0Col;
                        bRow = j;
                    }
                }
                if (bCol >= 0)
                {
                    bestCol = bCol;
                    bestRow = bRow;
                    if (dm.isLogging)
                        Log.i(TAG, "2a calcImageEntry(), weak image entry, col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
                    return;
                }
                else
                {

                    bestCol = dm.player.play0Col;
                    bestRow = dm.player.play0Row;
                    if (dm.isLogging)
                        Log.i(TAG, "2b calcImageEntry(), weak image entry, col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
                    return;
                }
            }
        }

        for (int i = 0; i <= 5; ++i)
        {
            if (bCol < 0)
            {
                bCol = dm.player.getColFromEntryTable(0, i);
                bRow = i;
            }
            else
            {
                Column pl = dm.player.getPlayerColumn(playerId, bCol);
                int bValue = 0;
                for (int j = bRow + 1; j < DM.ROW_ID_STRAIGHT; ++j)
                {
                    if (pl.rowPlay[j] >= 0 & imageCount[j] >= 2 & diceResult[j] > bValue)
                    {
                        bValue = diceResult[j];
                        bRow = i;
                    }
                }
            }
        }
//Log.i(tag, "8 ??? calcImageEntry(), bCol: " + bCol + ", bRow: " + bRow + ", value: " + diceResult[imageId]);
        if (bCol >= 0 & bRow >= 0)
        {
            Column pl = dm.player.getPlayerColumn(playerId, bCol);
            for (int i = 0; i < dm.col; ++i)
            {
                Column plX = dm.player.getPlayerColumn(playerId, i);
                if (plX.rowPlay[bRow] >= 0 & plX.p1Action > pl.p1Action)
                    bCol = i;
            }
        }
//Log.i(tag, "9 ??? calcImageEntry(), bCol: " + bCol + ", bRow: " + bRow + ", value: " + diceResult[imageId]);
        if (bCol >= 0 & bRow >= 0)
        {
            Column pl = dm.player.getPlayerColumn(playerId, bCol);
            int bValue = diceResult[bRow];
            if (!(!pl.isP1 & pl.colId == 0))
            {
                for (int j = bRow + 1; j <= DM.ROW_ID_GRANDE; ++j)
                {
                    if (((dm.player.play0Col == pl.colId & pl.rowPlay[j] >= 0) | pl.rowPlay[j] == 0) & diceResult[j] > bValue)
                    {
                        int cnt = 0;
                        if (j < DM.ROW_ID_STRAIGHT)
                            cnt = imageCount[j];
                        int iCol = -1;
                        if (cnt > 0)
                            iCol = dm.player.getColFromEntryTable(cnt, j);
                        if (iCol == pl.colId | j >= DM.ROW_ID_STRAIGHT)
                        {

                            bValue = diceResult[j];
                            bRow = j;
                        }
                    }
                }
            }
            if (bRow < DM.ROW_ID_STRAIGHT & bValue == 0 & pl.rowPlay[DM.ROW_ID_STRAIGHT] == 0)
                bRow = DM.ROW_ID_STRAIGHT;
        }

        if (bCol >= 0 & bRow >= 0)
        {
            if (diceResult[bRow] == 0)
            {
                int xCol = -1;
                int xRow = -1;
                if (bestCombiId >= 0)
                {
                    for (int i = 0; i < dm.col; ++i)
                    {
                        Column pl = dm.player.getPlayerColumn(playerId, i);
                        if (pl.rowPlay[bestCombiId] >= 0)
                        {
                            xCol = i;
                            xRow = bestCombiId;
                        }
                    }
                }
                if (xCol >= 0)
                {
                    bestCol = xCol;
                    bestRow = xRow;
                    if (dm.isLogging)
                        Log.i(TAG, "3 calcImageEntry(), weak combi entry: 0, col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
                    return;
                }
            }
        }
        if (bCol >= 0)
        {
            bestCol = bCol;
            bestRow = bRow;
            if (diceResult[bestRow] == 0)
            {
                int value = 0;
                for (int j = 0; j <= DM.ROW_ID_STRAIGHT; ++j)
                {
                    int iCol = dm.player.getColFromEntryTable(0, j);
                    if (iCol >= 0 & diceResult[j] > 0 & diceResult[j] > value)
                    {
                        bestCol = iCol;
                        bestRow = j;
                        value = diceResult[j];
                    }
                }
            }

            Column pl = dm.player.getPlayerColumn(playerId, bestCol);
            int nRow = -1;
            for (int j = 0; j < DM.ROW_ID_STRAIGHT; ++j)
            {
                if (pl.rowPlay[j] >= 0 & diceResult[j] > diceResult[bestRow])
                    nRow = j;
            }
            if (nRow >= 0)
                bestRow = nRow;

            if (dm.isLogging)
                Log.i(TAG, "3 calcImageEntry(), image entry: 0, col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);

            return;
        }

        bCol = dm.player.getColFromEntryTable(0, 6);
        bRow = dm.player.getColFromEntryTable(0, 7);
        if (bCol >= 0 & bRow >= 0)
        {
            bestCol = bCol;
            bestRow = bRow;
            if (diceResult[bestRow] == 0)
            {
                Column pl = dm.player.getPlayerColumn(playerId, bCol);
                int xRow = -1;
                for (int j = 0; j <= DM.ROW_ID_GRANDE; ++j)
                {
                    if (j != bRow & pl.rowPlay[j] == 0 & diceResult[j] > 0)
                    {
                        xRow = j;
                        break;
                    }
                }
                for (int j = 0; j <= DM.ROW_ID_GRANDE; ++j)
                {
                    if (j != bRow & diceResult[j] >= pl.rowPlay[j] & pl.rowPlay[j] > 0)
                    {
                        xRow = j;
                        break;
                    }
                }
                if (xRow >= 0)
                    bestRow = xRow;
            }
            if (bestCol >= 0 & bestRow >= 0)
            {
                Column plB = dm.player.getPlayerColumn(playerId, bestCol);
                Column opB = dm.player.getOponentColumn(playerId, bestCol);
                int diffRowB = dm.getRowSum(plB.row, null) - dm.getRowSum(opB.row, null);
                for (int i = 0; i < dm.col; ++i)
                {
                    Column pl = dm.player.getPlayerColumn(playerId, i);
                    Column op = dm.player.getOponentColumn(playerId, i);
                    int diffRow = dm.getRowSum(pl.row, null) - dm.getRowSum(op.row, null);
                    if (dm.player.getMovesToGo(playerId) <= dm.roundCheck4 & diffRowB < 0  & diffRow < 0
                            & pl.rowPlay[bestRow] > 0 & plB.rowPlay[bestRow] > 0 & pl.rowPlay[bestRow] > plB.rowPlay[bestRow])
                    {
                        bestCol = i;
                        break;
                    }
                }
            }
            if (dm.isLogging)
                Log.i(TAG, "3 calcImageEntry(), combi entry: 0, col: " + bestCol + ", row: " + bestRow + ", value: " + diceResult[bestRow]);
            return;
        }

        if (dm.isLogging)
            Log.i(TAG, "calcImageEntry(), NO ENTRY!");
    }

    private boolean setRollHold()
    {
        if (dm.playerIsP1)
            return p1RollHold();
        if (dm.isLogging)
            Log.i(TAG, "setRollHold(), plan1: " + dm.playerIsP1 + "(" + dm.playerRoundesToGo + "), diceAll: " + diceAll);
        if (diceTry == 3)
        {
            if (dm.isLogging)
                Log.i(TAG, " error setRollHold(), try: " + diceTry);
            return false;
        }
        setRollAll();
        if (checkDoubleServ())
            return true;
        if (bestCombiId == DM.ROW_ID_GRANDE)
        {
            if (getRowCnt(bestCombiId, true) == 0 & getRowCnt(bestImageId, true) == 0)
            {
                if (getRowCnt(DM.ROW_ID_POKER, true) >= 0)
                {
                    for (int i = 0; i < diceRoll.length -1; i++)
                    {
                        diceHold[i] = diceRoll[i];
                        diceRoll[i] = -1;
                    }
                    diceRoll[1] = diceHold[1];
                    diceHold[1] = -1;
                    return true;
                }
            }
        }
        if (bestCombiId == DM.ROW_ID_POKER)
        {
            if (getEmtyColCount(bestImageId) > 0 | getEmtyColCount(DM.ROW_ID_GRANDE) > 0)
            {
                setHold(bestImageId);
                return true;
            }
        }
        if (bestCombiId == DM.ROW_ID_FULL)
        {
            if (getEmtyColCount(bestImageId) > 0 | (getEmtyColCount(DM.ROW_ID_POKER) > 0 & diceTry == 1))
            {
                setHold(bestImageId);
                return true;
            }
            else
            {
                for (int i = 0; i < imageCount.length; i++)
                {
                    if (imageCount[i] == DM.ROW_ID_PAIR)
                    {
                        if (getEmtyColCount(i) > 0)
                        {
                            setHold(i);
                            return true;
                        }
                    }
                }
            }
        }
        if (is3)
        {
            if (getEmtyColCount(bestImageId) > 0 | getEmtyColCount(DM.ROW_ID_POKER) > 0)
            {
                setHold(bestImageId);
                return true;
            }
        }
        if (imageCount[bestImageId] <= 2 & !isTwoPair)
        {
            if (holdPosibleStraight(bestImageId))
                return true;
        }
        if (is2 & !isTwoPair)
        {
            if (bestCombiId != DM.ROW_ID_FULL)
            {
                if (getEmtyColCount(bestImageId) > 0)
                {
                    setHold(bestImageId);
                    return true;
                }
            }
        }
        if (isTwoPair)
        {
            if (dm.isLogging) Log.i(TAG, "isTwoPair()");
            int firstPair = 0;
            for (int i = 0; i < imageCount.length; i++)
            {
                if (imageCount[i] == DM.ROW_ID_PAIR & i != bestImageId)
                {
                    firstPair = i;
                    break;
                }
            }
            int bestEmpty = getEmtyColCount(bestImageId);
            int firstEmpty = getEmtyColCount(firstPair);
            setRollAll();
            int bestId = 0;
            if (diceTry == 1)
            {
                if (bestEmpty >= firstEmpty)
                    bestId = bestImageId;
                else
                    bestId = firstPair;
            }
            if (diceTry == 2)
            {
                if (bestEmpty > firstEmpty)
                    bestId = bestImageId;
                else
                    bestId = firstPair;
            }
            if (getEmtyColCount(bestId) > 0)
            {
                setHold(bestId);
                return true;
            }
        }
        return true;
    }

    private boolean p1RollHold()
    {
        setRollAll();

        boolean isServ = false;
        if  (       dm.player.getColFromEntryTable(5, 7) >= 0 | dm.player.getColFromEntryTable(4, 7) >= 0
                |   dm.player.getColFromEntryTable(3, 7) >= 0 | dm.player.getColFromEntryTable(1, 7) >= 0
                )
            isServ = true;

        if (dm.isLogging)
            Log.i(TAG, "p1RollHold(), plan1: " + dm.playerIsP1 + "(" + dm.playerRoundesToGo + "), diceAll: " + diceAll);

        if (checkDoubleServ())
        {
            if (dm.isLogging)  Log.i(TAG, "3: p1RollHold(), checkDoubleServ()");
            return true;
        }
        if (checkServ())
        {
            if (dm.isLogging)  Log.i(TAG, "4: p1RollHold(), checkServ()");
            return true;
        }
        if (bestCombiId == DM.ROW_ID_STRAIGHT)
        {
            if (dm.isLogging)  Log.i(TAG, "5: p1RollHold(), DM.ROW_ID_STRAIGHT");
            setHoldAll();
            return true;
        }
        if (bestCombiId == DM.ROW_ID_FULL)
        {
            if (    !dm.player.isRowInEntryTable(bestImageId)
                    & firstPairId >= 0
                    &
                    (
                            dm.player.getColFromEntryTable(3, firstPairId) >= 0
                        |   dm.player.getColFromEntryTable(4, firstPairId) >= 0)
                    )
            {
                if (dm.isLogging)  Log.i(TAG, "6a: p1RollHold(), DM.ROW_ID_FULL (pair)");
                setHold(firstPairId);
                return true;
            }
            if (dm.player.isRowInEntryTable(bestImageId) | dm.player.getColFromEntryTable(4, 6) >= 0 | dm.player.getColFromEntryTable(5, 6) >= 0)
            {
                if (dm.isLogging)  Log.i(TAG, "6b: p1RollHold(), DM.ROW_ID_FULL (three of kind)");
                setHold(bestImageId);
                return true;
            }
            else
            {
                for (int i = 0; i < imageCount.length; i++)
                {
                    if (imageCount[i] == DM.ROW_ID_PAIR & i != bestImageId)
                    {
                        if (dm.player.isRowInEntryTable(i))
                        {
                            if (dm.isLogging)  Log.i(TAG, "6c: p1RollHold(), DM.ROW_ID_FULL (pair)");
                            setHold(i);
                            return true;
                        }
                    }
                }
            }
        }
        if (firstPairId >= 0  & is2 & !dm.player.isRowInEntryTable(bestImageId))
        {
            if (dm.player.isRowInEntryTable(firstPairId))
                bestImageId = firstPairId;
        }
        if (bestCombiId == DM.ROW_ID_POKER)
        {
            if (dm.player.getColFromEntryTable(5, 6) >= 0 | dm.player.isRowInEntryTable(bestImageId))
            {
                if (dm.isLogging)  Log.i(TAG, "7a: p1RollHold(), DM.ROW_ID_POKER");
                setHold(bestImageId);
                return true;
            }
            if (dm.player.getColFromEntryTable(4, 6) < 0 & dm.player.getColFromEntryTable(5, 6) < 0)
            {
                if (dm.isLogging)  Log.i(TAG, "7b: p1RollHold(), DM.ROW_ID_POKER");
                if (dm.player.getColFromEntryTable(3, 6) >= 0)
                {
                    int cnt = 0;
                    for (int i = 0; i < diceRoll.length; i++)
                    {
                        if (diceRoll[i] == bestImageId & cnt < 2)
                        {
                            if (imageCount[diceRoll[i]] >= 1)
                            {
                                diceHold[i] = bestImageId;
                                diceRoll[i] = -1;
                                cnt++;
                            }
                        }
                    }
                }
                else
                {
                    for (int i = 0; i < diceRoll.length; i++)
                    {
                        if (diceRoll[i] != bestImageId)
                        {
                            diceHold[i] = diceRoll[i];
                            diceRoll[i] = -1;
                        }
                    }
                }
                return true;
            }
        }
        if (bestCombiId == DM.ROW_ID_GRANDE)
        {
            for (int i = 0; i < diceRoll.length; i++)
            {
                diceHold[i] = diceRoll[i];
                diceRoll[i] = -1;
            }
            if (dm.player.getColFromEntryTable(5, 6) < 0 & dm.player.getColFromEntryTable(4, 6) >= 0
                    & dm.player.getColFromEntryTable(4, bestImageId) < 0)
            {
                if (dm.isLogging)  Log.i(TAG, "8: p1RollHold(), DM.ROW_ID_GRANDE");
                diceRoll[0] = diceHold[0];
                diceHold[0] = -1;
                return true;
            }
            else
                return false;
        }
        if (holdPosibleStraight(bestImageId))   // double end straight
            return true;
        if (isStraightBetterFPG())
        {
            if (dm.isLogging)  Log.i(TAG, "9: p1RollHold(), check posible straight");
            int cntImageStraight = 0;
            for (int i = 0; i < imageCount.length; i++)
            {
                if ((i > 0 & i < 5) & imageCount[i] > 0)
                    cntImageStraight++;
            }
            boolean isSetHold = false;
            boolean computeDiceTry2 = false;
            if (dm.player.getColFromEntryTable(1, 6) >= 0)
                computeDiceTry2 = true;

            if (diceTry == 2 & !computeDiceTry2)
            {
                if (!(imageCount[0] > 0 & imageCount[5] > 0))
                {
                    if (cntImageStraight == 3 & (imageCount[0] > 0 | imageCount[5] > 0))
                    {
                        int cnt[] = new int[6];
                        for (int i = 0; i < cnt.length; i++)
                        {
                            cnt[i] = -1;
                        }
                        for (int i = 0; i < diceRoll.length; i++)
                        {
                            if (cnt[diceRoll[i]] == -1)
                            {
                                cnt[diceRoll[i]] = diceRoll[i];
                                diceHold[i] = diceRoll[i];
                                diceRoll[i] = -1;
                                isSetHold = true;
                            }
                        }
                        if (isSetHold)
                            return true;
                    }
                }
                else computeDiceTry2 = true;
            }
            if (diceTry == 1 | computeDiceTry2)
            {
                if (cntImageStraight >= 1)
                {
                    int cnt[] = new int[6];
                    for (int i = 0; i < cnt.length; i++)
                    {
                        cnt[i] = -1;
                    }

                    for (int i = 0; i < diceRoll.length; i++)
                    {
                        if ((diceRoll[i] > 0 & diceRoll[i] < 5))
                        {
                            if (cnt[diceRoll[i]] == -1)
                            {
                                cnt[diceRoll[i]] = diceRoll[i];
                                diceHold[i] = diceRoll[i];
                                diceRoll[i] = -1;
                                isSetHold = true;
                            }
                        }
                    }
                    if (isSetHold)
                        return true;
                }
            }
        }
        if (dm.player.getMovesToGo(playerId) <= dm.roundCheck4)
        {
            int holdRow = -1;
            boolean isPlayImage = false;
            int cCol = dm.player.getColFromEntryTable(4, 6);
            if (!(cCol >= 0 & imageCount[bestImageId] >= 2))
            {
                for (int i = 0; i < dm.col; ++i)
                {
                    Column pl = dm.player.getPlayerColumn(playerId, i);
                    for (int j = 0; j < DM.ROW_ID_STRAIGHT; ++j)
                    {
                        if (pl.p1Action == DM.P1_PLAY_WIN & pl.rowPlay[j] > 0 & imageCount[j] > 0)
                        {
                            holdRow = j;
                            if (imageCount[j] >= 2)
                            {
                                isPlayImage = false;
                                break;
                            }
                        }
                        if (pl.p1Action != DM.P1_PLAY_WIN & pl.rowPlay[j] > 0 & imageCount[j] >= 2)
                            isPlayImage = true;
                    }
                }
                if (!isPlayImage & holdRow >= 0)
                {
                    setHold(holdRow);
                    return true;
                }
            }
        }

        if  ( is3 & (   dm.player.getColFromEntryTable(3, bestImageId) >= 0 | dm.player.getColFromEntryTable(3, 6) >= 0
                        |   dm.player.getColFromEntryTable(4, 6) >= 0  | dm.player.getColFromEntryTable(5, 6) >= 0
                    )
            )
        {
            if (dm.isLogging)  Log.i(TAG, "10: p1RollHold(), three of kind");
            setHold(bestImageId);
            return true;
        }
        if (isTwoPair)
        {
            boolean isP1Full = true;
            if (dm.playerIsP1 & dm.player.getColFromEntryTable(3, 6) < 0)
                isP1Full = false;
            if (getBestP1Action(DM.ROW_ID_POKER, true) >= DM.P1_PLAY_BEST)
                isP1Full = false;

            if  (   (   dm.player.getColFromEntryTable(3, 6) >= 0 & getRowCnt(DM.ROW_ID_FULL, true) > getRowCnt(bestImageId, true)
                        & getRowCnt(DM.ROW_ID_FULL, true) > getRowCnt(firstPairId, true) & dm.player.getMovesToGo(playerId) <= dm.roundCheck12
                    )
                    |   dm.player.getColFromEntryTable(3, 6) >= 0 & dm.player.getMovesToGo(playerId) <= dm.roundCheck8
                )
            {
                if (isP1Full)
                {
                    if (dm.isLogging)  Log.i(TAG, "11a: p1RollHold(), isTwoPair(), isP1Full: true");
                    for (int i = 0; i < imageCount.length; i++)
                    {
                        if (imageCount[i] == 2)
                            setHold(i);
                    }
                    return true;
                }
            }
            if (dm.player.getMovesToGo(playerId) <= dm.roundCheck6)
            {
                int bRow = -1;
                int action = 0;
                for (int i = 0; i < dm.col; ++i)
                {
                    Column pl = dm.player.getPlayerColumn(playerId, i);
                    for (int j = 0; j < DM.ROW_ID_STRAIGHT; j++)
                    {
                        if (pl.rowPlay[j] > 0 & imageCount[j] == 2 & pl.p1Action >= action)
                        {
                            bRow = j;
                            action = pl.p1Action;
                        }
                    }
                }
                if (bRow >= 0)
                {
                    if (dm.isLogging)  Log.i(TAG, "11b: p1RollHold(), isTwoPair(), bestImageId last 6 moves");
                    setHold(bRow);
                    return true;
                }
            }
            if (dm.player.isRowInEntryTable(bestImageId) & getRowCnt(bestImageId, true) >= getRowCnt(firstPairId, true))
            {
                if (dm.isLogging)  Log.i(TAG, "11c: p1RollHold(), isTwoPair(), bestImageId");
                setHold(bestImageId);
                return true;
            }
            else
            {
                if (dm.player.isRowInEntryTable(firstPairId) & firstPairId >= 0 & getRowCnt(firstPairId, true) > 0)
                {
                    if (dm.isLogging)  Log.i(TAG, "11d: p1RollHold(), isTwoPair(), firstPairId");
                    setHold(firstPairId);
                    return true;
                }
            }
            if (isP1Full)
            {
                if (getRowCnt(DM.ROW_ID_FULL, true) > getRowCnt(DM.ROW_ID_POKER, true))
                {
                    if (dm.isLogging)  Log.i(TAG, "11e: p1RollHold(), isTwoPair(), isP1Full: true");
                    for (int i = 0; i < imageCount.length; i++)
                    {
                        if (imageCount[i] == 2)
                            setHold(i);
                    }
                    return true;
                }
            }
            if (dm.player.getColFromEntryTable(4, 6) >= 0)
            {
                if (getRowCnt(bestImageId, true) >= getRowCnt(firstPairId, true) & dm.player.isRowInEntryTable(bestImageId))
                {
                    if (dm.isLogging)  Log.i(TAG, "11f: p1RollHold(), isTwoPair(), isPlayWin(DM.ROW_ID_POKER) 1");
                    setHold(bestImageId);
                    return true;
                }
                else
                {

                    if (dm.isLogging)  Log.i(TAG, "11g: p1RollHold(), isTwoPair(), isPlayWin(DM.ROW_ID_POKER) 2");
                    if (isPlayWin(bestImageId, false, false))
                    {
                        setHold(bestImageId);
                        return true;
                    }
                    setHold(firstPairId);
                    return true;
                }
            }
        }
        int bestSingleRow;
        int bestSingleCnt;
        if (imageCount[bestImageId] == DM.ROW_ID_PAIR | imageCount[bestImageId] == 1)
        {
            if (imageCount[bestImageId] == DM.ROW_ID_PAIR)
            {
                if (dm.player.isRowInEntryTable(bestImageId))
                {
                    if (dm.isLogging)
                        Log.i(TAG, "12a: p1RollHold(), check DM.ROW_ID_PAIR (image in entryTable)");
                    setHold(bestImageId);
                    return true;
                }
                int fullCol = dm.player.getColFromEntryTable(2, 6);
                int pokerCol = dm.player.getColFromEntryTable(4, 6);
                int grandeCol = dm.player.getColFromEntryTable(5, 6);
                if (getRowCnt(bestImageId, true) >= 1 & (fullCol >= 0 | pokerCol >= 0 | grandeCol >= 0))
                {
                    if (dm.isLogging)
                        Log.i(TAG, "12b: p1RollHold(), check DM.ROW_ID_PAIR (image in rowCnt)");
                    setHold(bestImageId);
                    return true;
                }
                int cntPlayImage = 0;
                int cntPlayCombi = 0;
                for (int i = 0; i < dm.col; ++i)
                {
                    Column pl = dm.player.getPlayerColumn(playerId, i);
                    if (pl.p1Action >= DM.P1_PLAY)
                    {
                        for (int j = 0; j <= DM.ROW_ID_GRANDE; ++j)
                        {
                            if (j < DM.ROW_ID_STRAIGHT & pl.rowPlay[j] > 0)
                                cntPlayImage++;
                            if (j > DM.ROW_ID_STRAIGHT & pl.rowPlay[j] > 0 & pl.rowPlay[j] <= DM.ROW_MULTIPLIER[j])
                                cntPlayCombi++;
                        }
                    }
                    if (pl.p1Action >= DM.P1_PLAY_WIN)
                    {
                        if  (       pl.rowPlay[DM.ROW_ID_POKER] == DM.ROW_MULTIPLIER[DM.ROW_ID_POKER]
                                |   pl.rowPlay[DM.ROW_ID_FULL] == DM.ROW_MULTIPLIER[DM.ROW_ID_FULL]
                            )
                        {
                            if (dm.isLogging)
                                Log.i(TAG, "13: p1RollHold(), check DM.ROW_ID_PAIR (P1_PLAY_WIN)");
                            setHold(bestImageId);
                            return true;
                        }
                    }
                }
                if (cntPlayImage == 0 & cntPlayCombi > 0)
                {
                    int bImage = bestImageId;
                    int cnt = imageCount[bestImageId];
                    for (int i = 0; i < dm.col; ++i)
                    {
                        Column pl = dm.player.getPlayerColumn(playerId, i);
                        for (int j = 0; j < DM.ROW_ID_STRAIGHT; ++j)
                        {
                            if (pl.rowPlay[j] >= 0 & imageCount[j] >= 1 & imageCount[j] > cnt)
                            {
                                bImage = j;
                                cnt = imageCount[j];
                            }
                        }
                    }
                    if (dm.isLogging)
                        Log.i(TAG, "14a: p1RollHold(), check DM.ROW_ID_PAIR (play combi: FPG)");
                    setHold(bImage);
                    return true;
                }
                if (dm.playerRoundesToGo <= dm.roundCheck8
                        & (cntPlayCombi > cntPlayImage | (cntPlayCombi > 0 & imageCount[bestImageId] >= 2)))
                {
                    if (dm.isLogging)
                        Log.i(TAG, "14b: p1RollHold(), check DM.ROW_ID_PAIR (play combi: FPG)");
                    setHold(bestImageId);
                    return true;
                }
            }
            bestSingleRow = -1;
            int playWinRow = -1;
            int cntCheck = 99;
            for (int i = 0; i < imageCount.length; i++)
            {
                if  (       dm.playerRoundesToGo <= dm.roundCheck12 & isPlayWin(i, true, false) & imageCount[i] >= 1
                        &   getBestP1Action(i, false) == DM.P1_PLAY_WIN
                    )
                    playWinRow = i;
                int iCol = -1;
                int iCnt = 0;
                if (imageCount[i] >= 1)
                {
                    iCnt = 3;
                    iCol = dm.player.getColFromEntryTable(iCnt, i);
                    if (iCol < 0)
                    {
                        iCnt = 2;
                        iCol = dm.player.getColFromEntryTable(iCnt, i);
                    }
                    if (iCol < 0)
                    {
                        iCnt = 1;
                        iCol = dm.player.getColFromEntryTable(iCnt, i);
                    }
                    if (iCol < 0 & dm.playerRoundesToGo <= dm.roundCheck12)
                    {
                        iCnt = 4;
                        iCol = dm.player.getColFromEntryTable(iCnt, i);
                    }
                }
                if (iCol >= 0 & iCnt < cntCheck)
                {
                    Column pl = dm.player.getPlayerColumn(playerId, iCol);
                    if (pl.p1Action >= DM.P1_PLAY_WEAK)
                    {
                        cntCheck = iCnt;
                        bestSingleRow = i;
                    }
                }
            }

            if (playWinRow >= 0)
                bestSingleRow = playWinRow;
            if (bestSingleRow >= 0)
            {
                if (dm.playerRoundesToGo >= dm.roundCheck12 & imageCount[bestSingleRow] < 2)
                {
                    if (dm.isLogging) Log.i(TAG, "15a: p1RollHold(), roundes >= 12 and single image ---> roll all");
                    return false;
                }

                if (dm.isLogging) Log.i(TAG, "15b: p1RollHold(), isPlayWin() >= 0");
                setHold(bestSingleRow);
                return true;
            }
            if (dm.player.getColFromEntryTable(3, 6) >= 0 | dm.player.getColFromEntryTable(4, 6) >= 0 | dm.player.getColFromEntryTable(5, 6) >= 0)
            {
                if (dm.isLogging)  Log.i(TAG, "16: p1RollHold(), check DM.ROW_ID_PAIR (best FPG)");
                if (imageCount[bestImageId] >= 2)
                {
                    setHold(bestImageId);
                    return true;
                }
                else
                    return false;
            }
            if (!dm.player.isRowInEntryTable(bestImageId))
            {
                for (int i = DM.ROW_ID_STRAIGHT - 1; i >= 0; i--)
                {
                    if (dm.player.isRowInEntryTable(i))
                    {
                        if (dm.isLogging)  Log.i(TAG, "17: p1RollHold(), check DM.ROW_ID_PAIR (best straight)");
                        setHold(i);
                        return true;
                    }
                }
            }
        }
        bestSingleRow = -1;
        bestSingleCnt = 0;
        int cnt2 = 0;
        for (int i = 0; i < imageCount.length; i++)
        {
            if  (       dm.player.isRowInEntryTable(i) & imageCount[i] == 1
                    &   getRowCnt(i, true) >= bestSingleCnt
                )
            {
                bestSingleRow = i;
                bestSingleCnt = getRowCnt(i, true);
            }
            if (imageCount[i] >= 2)
                cnt2++;
        }
        if (bestSingleRow >= 0 & cnt2 == 0 & dm.playerRoundesToGo <= dm.roundCheck8)
        {
            if (dm.isLogging) Log.i(TAG, "18: p1RollHold(), hold best single image");
            setHold(bestSingleRow);
            return true;
        }
        if (cnt2 == 0)
        {
            if (dm.isLogging) Log.i(TAG, "19: p1RollHold(), roll all");
            return true;
        }

        if  (       dm.playerIsP1 & imageCount[bestImageId] >= 2
                &   (dm.player.getColFromEntryTable(3, 6) >= 0 | dm.player.getColFromEntryTable(4, 6) >= 0 | dm.player.getColFromEntryTable(5, 6) >= 0)
            )
        {
            int imageEntry = 0;
            for (int i = 0; i < dm.col; ++i)
            {
                Column pl = dm.player.getPlayerColumn(playerId, i);
                for (int j = 0; j < DM.ROW_ID_STRAIGHT; ++j)
                {
                    if (pl.rowPlay[j] > 0) imageEntry++;
                }
            }
            if (imageEntry == 0)
            {
                if (dm.isLogging) Log.i(TAG, "20: p1RollHold(), no image entry");
                setHold(bestImageId);
                return true;
            }
        }
        int bestNoPairID = -1;
        for (int i = 0; i < diceRoll.length; i++)
        {
            if (diceRoll[i] >= 0)
            {
                if (imageCount[diceRoll[i]] > 0 & dm.player.isRowInEntryTable(diceRoll[i])
                        & getRowCnt(diceRoll[i], true) >= getRowCnt(DM.ROW_ID_STRAIGHT, true))
                    bestNoPairID = diceRoll[i];
            }
        }
        if (bestNoPairID >= 0)
        {
            if (dm.isLogging)  Log.i(TAG, "21: p1RollHold(), check no pair, bestNoPairID: " + bestNoPairID);
            setHold(bestNoPairID);
            return true;
        }

        if (imageCount[bestImageId] >= 2 & (dm.player.isRowInEntryTable(bestImageId)
                | dm.player.getColFromEntryTable(3, 6) >= 0 | dm.player.getColFromEntryTable(4, 6) >= 0))
        {
            if (dm.isLogging)  Log.i(TAG, "22: p1RollHold(), play bestImage if posible full or poker");
            setHold(bestImageId);
            return true;
        }

        boolean isImage = false;
        int imageRow = -1;
        boolean isCombi = false;
        for (int i = 0; i < dm.col; ++i)
        {
            Column pl = dm.player.getPlayerColumn(playerId, i);
            for (int j = 0; j < DM.row; ++j)
            {
                if (j < DM.ROW_ID_STRAIGHT)
                {
                    if (pl.rowPlay[j] > 0 & imageCount[j] > 0)
                    {
                        isImage = true;
                        imageRow = j;
                    }
                }
                if (j > DM.ROW_ID_STRAIGHT & pl.rowPlay[j] > 0 & imageCount[bestImageId] > 1)
                {
                    if (pl.rowPlay[j] == dm.ROW_MULTIPLIER[j])
                        isCombi = true;
                }
            }
        }
        if (isImage)
        {
            if (dm.isLogging)  Log.i(TAG, "23: p1RollHold(), isImage & imageCount[image] > 0");
            setHold(imageRow);
            return true;
        }
        if (isCombi)
        {
            if (dm.isLogging)  Log.i(TAG, "24: p1RollHold(), !isImage & isCombi & bestImageId(count) > 1");
            setHold(bestImageId);
            return true;
        }
        isImage = false;
        imageRow = -1;
        boolean isStraight = false;
        int cnt = 0;
        for (int i = 0; i < dm.col; ++i)
        {
            Column pl = dm.player.getPlayerColumn(playerId, i);
            for (int j = 0; j < DM.row; ++j)
            {
                if (j < DM.ROW_ID_STRAIGHT)
                {
                    if (pl.rowPlay[j] >= 0 & imageCount[j] > cnt & pl.p1Action >= DM.P1_PLAY_WEAK)
                    {
                        isImage = true;
                        imageRow = j;
                        cnt = imageCount[j];
                    }
                }
                if (j == DM.ROW_ID_STRAIGHT & pl.rowPlay[j] >= 0)
                    isStraight = true;
            }
        }
        if (!(!isImage & isStraight))
        {
            if (!isServ & isImage & imageRow >= 0)
            {
                setHold(imageRow);
                if (dm.isLogging)  Log.i(TAG, "25: p1RollHold(), no image, no straight ---> hold bestImageId");
            }
        }
        if (dm.isLogging)  Log.i(TAG, "26: p1RollHold(), no image, no playable combi ---> roll all");
        return false;

    }

    public int getRowCnt(int row, boolean isPlayAction)
    {
        int rowCnt = 0;
        for (int i = 0; i < dm.col; ++i)
        {
            Column pl = dm.player.getPlayerColumn(playerId, i);
            if (isPlayAction)
            {
                if (pl.rowPlay[row] >= 0 & pl.p1Action >= DM.P1_PLAY)
                    rowCnt++;
            }
            else
            {
                if (pl.row[row] < 0)
                    rowCnt++;
            }
        }
        return rowCnt;
    }

    private boolean isPlayWin(int row, boolean isPlay, boolean isGreater0)
    {
        boolean playWin = false;
        int p1Action = DM.P1_PLAY;
        if (!isPlay)
            p1Action = DM.P1_NON;
        for (int i = 0; i < dm.col; ++i)
        {
            Column pl = dm.player.getPlayerColumn(playerId, i);
            if (isGreater0)
            {
                if (pl.rowPlay[row] > 0 & pl.p1Action >= p1Action)
                    playWin = true;
            }
            else
            {
                if (pl.rowPlay[row] >= 0 & pl.p1Action >= p1Action)
                    playWin = true;
            }
        }
        return playWin;
    }

    private int getBestP1Action(int row, boolean greater0)
    {
        int bestAction = 0;
        for (int i = 0; i < dm.col; ++i)
        {
            Column pl = dm.player.getPlayerColumn(playerId, i);
            if (row < 0)
            {
                if (pl.p1Action >= bestAction)
                    bestAction = pl.p1Action;
            }
            else
            {
                if (greater0)
                {
                    if (pl.rowPlay[row] > 0 & pl.p1Action >= bestAction)
                        bestAction = pl.p1Action;
                } else
                {
                    if (pl.rowPlay[row] >= 0 & pl.p1Action >= bestAction)
                        bestAction = pl.p1Action;
                }
            }
        }
        return bestAction;
    }

    private boolean isPlayValue(int rowId, int value, boolean greaterEqual)
    {
        boolean isValue = false;
        for (int i = 0; i < dm.col; ++i)
        {
            Column pl = dm.player.getPlayerColumn(playerId, i);
            if (pl.rowPlay[rowId] >= 0 & pl.p1Action >= DM.P1_PLAY)
            {
                if (greaterEqual & pl.rowPlay[rowId] >= value)
                    isValue = true;
                if (!greaterEqual & pl.rowPlay[rowId] == value)
                    isValue = true;
            }
        }
        return isValue;
    }

    private boolean checkDoubleServ()
    {
        int maxCol = dm.col -1;
        if (dm.roundDouble1 == null | !dm.isDouble)
            return false;
        if (dm.roundDouble1.isServed & diceResult[bestCombiId] > 0)
            return false;
        int col3 = dm.player.getColFromEntryTable(3, bestImageId);
        int col4 = dm.player.getColFromEntryTable(4, bestImageId);
        int colFull = dm.player.getColFromEntryTable(3, 6);
        int colPoker = dm.player.getColFromEntryTable(4, 6);
        int colGrande = dm.player.getColFromEntryTable(5, 6);
        if (dm.roundDouble1.isGrande & imageCount[bestImageId] >= 3 & bestImageId != DM.ROW_ID_A)
            return true;
        if (imageCount[bestImageId] >= 3 & (col3 >= 0 | col4 >= 0 | colFull >= 0 | colPoker >= 0 | colGrande >= 0))
            return false;
        int colStraight = dm.player.getColFromEntryTable(1, 7);
        colFull = dm.player.getColFromEntryTable(3, 7);
        colPoker = dm.player.getColFromEntryTable(4, 7);
        if (!dm.roundDouble1.isGrande & colGrande >= 1 & colStraight < maxCol & colFull < maxCol & colPoker < maxCol)
            return false;
        if (dm.roundDouble1.isPoker | dm.roundDouble1.isGrande & imageCount[bestImageId] < 3)
        {
            if (colStraight >= 0 | colFull >= 0 | colPoker >= 0)
                return true;
        }
        return false;
    }

    private boolean checkServ()
    {
        if (dm.player.getMovesToGo(playerId) == 1 & dm.player.getColFromEntryTable(5, 7) >= 0 & dm.player.getColFromEntryTable(5, 6) < 0)
            return true;

        int col1 = dm.player.getColFromEntryTable(1, bestImageId);
        int col2 = dm.player.getColFromEntryTable(2, bestImageId);
        int colF = -1;
        if (firstPairId >= 0)
        {
            colF = dm.player.getColFromEntryTable(2, firstPairId);
            if (colF < 0)
                colF = dm.player.getColFromEntryTable(3, firstPairId);
            if (colF < 0)
                colF = dm.player.getColFromEntryTable(4, firstPairId);
        }
        int col3 = dm.player.getColFromEntryTable(3, bestImageId);
        int col4 = dm.player.getColFromEntryTable(4, bestImageId);
        int colStraight = dm.player.getColFromEntryTable(1, 6);
        if (colStraight >= 0 & holdPosibleStraight(bestImageId))   // double end straight
            return false;
        int colFull = dm.player.getColFromEntryTable(3, 6);
        int colPoker = dm.player.getColFromEntryTable(4, 6);
        int colGrande = dm.player.getColFromEntryTable(5, 6);
        int colStraightServed = dm.player.getColFromEntryTable(1, 7);
        int colFullServed = dm.player.getColFromEntryTable(3, 7);
        int colPokerServed = dm.player.getColFromEntryTable(4, 7);
        if (colStraightServed == colStraight) colStraightServed = -1;
        if (colFullServed == colFull) colFullServed = -1;
        if (colPokerServed == colPoker) colPokerServed = -1;
        if (col1 >= 0 | col2 >= 0 | colF >= 0 | col3 >= 0 | col4 >= 0 | colStraight >= 0 | colFull >= 0 | colPoker >= 0)
            return false;
        if  (       imageCount[bestImageId] <= 2 & getBestP1Action(-1, true) >= DM.P1_PLAY_BEST
                &   (colStraightServed >= 0 | colFullServed >= 0 | colPokerServed >= 0)
            )
            return true;
        if (imageCount[bestImageId] >= 3 & colGrande >= 0)
            return false;

        if (colStraightServed >= 0 | colFullServed >= 0 | colPokerServed >= 0)
            return true;

        return false;

    }

    private boolean isStraightBetterFPG()
    {
        int col2 = dm.player.getColFromEntryTable(2, bestImageId);
        int col3 = dm.player.getColFromEntryTable(3, bestImageId);
        if (col2 < 0 & col3 < 0 & firstPairId >= 0)
        {
            col2 = dm.player.getColFromEntryTable(2, firstPairId);
            col3 = dm.player.getColFromEntryTable(3, firstPairId);
        }
        int colStraight = dm.player.getColFromEntryTable(1, 6);
        int colFull = dm.player.getColFromEntryTable(3, 6);
        int colPoker = dm.player.getColFromEntryTable(4, 6);
        int colGrande = dm.player.getColFromEntryTable(5, 6);
        if (colGrande >= 0)
        {
            Column pl = dm.player.getPlayerColumn(playerId, colGrande);
            if (pl.rowPlay[DM.ROW_ID_GRANDE] == 0)
                colGrande = -1;
        }
        if (colStraight < 0)
            return false;

        if (imageCount[bestImageId] >= 2)
        {
            if (dm.player.getMovesToGo(playerId) > dm.roundCheck8)
            {
                if (col2 >= 0 | col3 >= 0 | colFull >= 0 | colPoker >= 0 | colGrande >= 0)
                    return false;
            }
            else
            {
                Column pl = dm.player.getPlayerColumn(playerId, colStraight);
                if (colGrande >= 0 & pl.rowPlay[DM.ROW_ID_GRANDE] > 0)
                    return false;
            }
        }

        if (colFull >= 0)
        {
            Column pl = dm.player.getPlayerColumn(playerId, colFull);
            if (pl.p1Action >= DM.P1_PLAY_BEST & dm.player.getMovesToGo(playerId) <= dm.roundCheck8)
                return false;
        }
        if (colPoker >= 0 | colGrande >= 0)
        {
            int colId = colPoker;
            if (colId < 0)
                colId = colGrande;
            Column pl = dm.player.getPlayerColumn(playerId, colId);
            if (pl.p1Action >= DM.P1_PLAY_BEST & dm.player.getMovesToGo(playerId) <= dm.roundCheck8)
                return false;
            if (is3 | imageCount[bestImageId] >= 2 & (colStraight == colPoker | colStraight == colGrande))
                return false;
        }
        for (int i = 0; i < dm.col; ++i)
        {
            Column pl = dm.player.getPlayerColumn(playerId, i);
            if (pl.p1Action >= DM.P1_PLAY_BEST & dm.player.getMovesToGo(playerId) <= dm.roundCheck8)
            {
                boolean isImageBest = false;
                for (int j = 0; j < imageCount.length; j++)
                {
                    if (imageCount[j] >= 1 & pl.rowPlay[j] >= 1)
                        isImageBest = true;
                }
                if (isImageBest)
                    return false;
            }
        }
        if (colStraight >= 0 & !(imageCount[bestImageId] >= 3 & (colPoker >= 0 | colGrande >= 0)))
        {
            Column pl = dm.player.getPlayerColumn(playerId, colStraight);
            if (pl.p1Action >= DM.P1_PLAY_BEST & dm.player.getMovesToGo(playerId) <= dm.roundCheck8)
                return true;
        }
        if (dm.player.getMovesToGo(playerId) > dm.roundCheck8)
        {
            if (col2 >= 0 | col3 >= 0 | colFull >= 0 | colPoker >= 0 | colGrande >= 0)
                return false;
            else
                return true;
        }
        else
        {
            boolean isStraightPlay = true;
            if (col2 >= 0 & col2 != colGrande)
                isStraightPlay = false;
            if (col3 >= 0 & col3 != colGrande)
                isStraightPlay = false;
            if (colFull >= 0 & colFull != colGrande)
                isStraightPlay = false;
            if (colPoker >= 0 & colPoker != colGrande)
                isStraightPlay = false;
            if (col2 < 0 & col3 < 0 & colFull < 0 & colPoker < 0 & colGrande >= 0 & is3)
                isStraightPlay = false;
            return isStraightPlay;
        }
    }

    private boolean setEntryValue(int col, int row)
    {
        entryOK = true;
        entryCol = col;
        entryRow = row;
        entryValue =  diceResult[row];
        if (dm.isLogging & entryCol >= 0)
            Log.i(TAG, "setEntry(), all dice: " + diceAll + ", col: " + entryCol + ", row: " + entryRow + ", value: " + entryValue);
        else
            if (dm.isLogging) Log.i(TAG, "setEntry() NO ENTRY");
        return true;
    }

    private boolean holdPosibleStraight(int bestImageId)
    {
        boolean holdStraight = false;
        int colStraight = dm.player.getColFromEntryTable(1, 6);
        int colStraightServed = dm.player.getColFromEntryTable(1, 7);
        if (colStraight < 0)
            return false;
        if (dm.isDouble)
        {
            if (colStraightServed >= 0 & dm.playerRoundesToGo > dm.roundCheck6)
                return false;
        }
        for (int i = 0; i < dm.col; i++)
        {
            Column pl = dm.player.getPlayerColumn(playerId, i);
            if (pl.rowPlay[DM.ROW_ID_STRAIGHT] == 0)
            {
                for (int j = 0; j < DM.ROW_ID_STRAIGHT; ++j)
                {
                    if (pl.rowPlay[j] > 0)
                        holdStraight = true;
                }
            }
            if (pl.rowPlay[DM.ROW_ID_STRAIGHT] > 0 & dm.player.isAllPlay0)
                holdStraight = true;
        }
        if (!holdStraight)
        {
            if (getEmtyColCount(DM.ROW_ID_STRAIGHT) == 0 | dm.isDouble & !dm.playerIsP1)
                return false;
            if (!dm.playerIsP1 & getEmtyColCount(DM.ROW_ID_STRAIGHT) <= 1)
                return false;
            if (dm.playerIsP1 & !isPlayValue(DM.ROW_ID_STRAIGHT, DM.ROW_MULTIPLIER[DM.ROW_ID_STRAIGHT], false))
                return false;
        }
        for (int i = 0; i < imageCount.length; i++)
        {
            if (imageCount[i] > 2)
                return false;
        }

        int bCol = dm.player.getColFromEntryTable(1, 6);
        if (bCol < 0)
            return false;

        if (imageCount[1] >= 1  & imageCount[2] >= 1 & imageCount[3] >= 1 & imageCount[4] >= 1)
        {
            setRollAll();
            int bestId = 0;
            for (int i = 0; i < diceRoll.length; i++)
            {
                if (diceRoll[i] > 0 & diceRoll[i] < 5)
                {
                    if (diceRoll[i] != bestImageId | diceRoll[i] == bestImageId & bestId == 0)
                    {
                        if (diceRoll[i] == bestImageId)
                            bestId++;
                        diceHold[i] = diceRoll[i];
                        diceRoll[i] = -1;
                    }
                }
            }
            if (dm.isLogging)  Log.i(TAG, "p1RollHold(), hold posible double end straight");
            return true;
        }
        else
            return false;
    }

    private int getEmtyColCount(int rowId)
    {
        int cnt = 0;
        for (int i = 0; i < dm.col; ++i)
        {
            if (dm.player.getEntry(playerId, i , rowId) < 0)
                cnt++;
        }
        return cnt;
    }

    private void setPlan()
    {
        boolean setP1 = false;
        if (dm.playerRoundesToGo < 20)
            setP1 = true;
        for (int i = 0; i < dm.col; ++i)
        {
            Column pl = dm.player.getPlayerColumn(playerId, i);
            Column op = dm.player.getOponentColumn(playerId, i);
            if (pl.cntEmpty <= 4 | pl.isWon | pl.isLost)
                setP1 = true;
            if ((pl.row[DM.ROW_ID_GRANDE] > 0 | op.row[DM.ROW_ID_GRANDE] > 0) & pl.row[DM.ROW_ID_GRANDE] != op.row[DM.ROW_ID_GRANDE])
                setP1 = true;
            if (setP1)
            {
                pl.isP1 = true;
                dm.player.setPlayerColumn(playerId, i, pl);
            }
        }
        if (setP1)
            dm.playerIsP1 = true;
    }

    private void setHold(int bestImageId)
    {
        for (int i = 0; i < diceRoll.length; i++)
        {
            if (diceRoll[i] == bestImageId)
            {
                if (imageCount[diceRoll[i]] >= 1)
                {
                    diceHold[i] = bestImageId;
                    diceRoll[i] = -1;
                }
            }
        }
    }

    private void setRollAll()
    {
        for (int i = 0; i < diceHold.length; i++)
        {
            if (diceHold[i] >= 0)
            {
                diceRoll[i] = diceHold[i];
                diceHold[i] = -1;
            }
        }
    }

    private void setHoldAll()
    {
        for (int i = 0; i < diceRoll.length; i++)
        {
            if (diceRoll[i] >= 0)
            {
                diceHold[i] = diceRoll[i];
                diceRoll[i] = -1;
            }
        }
    }

    String getRollValues()
    {
        String value = "";
        for (int i = 0; i < diceRoll.length; i++)
        {
            if (diceRoll[i] == -1)
                value = value + "-";
            else
                value = value + diceRoll[i];
        }
        return value;
    }

    String getHoldValues()
    {
        String value = "";
        for (int i = 0; i < diceHold.length; i++)
        {
            if (diceHold[i] == -1)
                value = value + "-";
            else
                value = value + diceHold[i];
        }
        return value;
    }

    private static final String TAG = "Round";
    private DM dm;
    private char playerId;
    private int diceTry;

    int diceRoll[] = new int[5];    // the five dices, dice image valus 0...5
    int diceHold[] = new int[5];

    boolean entryOK = false;
    int entryCol;
    int entryRow;
    int entryValue;

    int bestCol = -1;
    int bestRow = -1;
    int bestValue = -1;
    int bestRating = -1;
    int entry0Col = -1;
    int entry0Row = -1;

    String diceAll;

    public int imageCount[] = new int[6];      // count the dice image values 0...5
    public int diceResult[] = new int[10];     // result for all possible images and combinations
    boolean isServed = false;
    private boolean isStraight = false;
    private boolean isFull = false;
    private boolean isPoker = false;
    private boolean isGrande = false;
    private boolean is3 = false;
    private boolean is2 = false;
    private boolean isTwoPair = false;

    private int firstPairId;
    int bestImageId;
    int bestCombiId;
    boolean isBestEntry = false;

}
