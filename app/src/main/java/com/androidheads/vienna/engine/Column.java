package com.androidheads.vienna.engine;
class Column
{
    Column(int colId, int colRanking)
    {
        this.colId = colId;
        this.colRanking = colRanking;
        for (int i = 0; i < rowCnt; ++i)
        {
            row[i] = -1;
        }
    }

    // entry values
    int colId = 0;
    int rowCnt = 10;
    int colRanking;                 // 3 columns: 1 = weak, 2 = middle, 3 = best
    int colValue;                   // column value
    int row[] = new int[rowCnt];    // images: 0...5, combinations: 6...9; empty: -1
    int sum = 0;                    // isSummation from row
    int sumMax = 0;                 // maximum isSummation from row
    int cntEmpty = 10;              // row: count empty
    int cntEntry = 0;               // row: count entries
    boolean isWon = false;          // column is won
    boolean isLost = false;         // column is lost
    boolean isP1 = false;           // plan 1: true, plan 3: false

    // round values
    int rowResult[] = new int[rowCnt];      // the possible results from the last dice; cell with entry: -1
    int rowPlay[] = new int[rowCnt];            // P1: the best player play
    int rowOpo[] = new int[rowCnt];             // P1: the best oponent play

    int p1Action = DM.P1_NON;

    int diffPlayImage;
    int diffPlayCombo;
    int diffPlay;
    int playCounter;
    int rankingCounter;
    boolean isPlayLess = false;
    boolean isPlayMore = false;
    boolean isOr = false;   // cntEmpty == 2; only one entry needed
}
