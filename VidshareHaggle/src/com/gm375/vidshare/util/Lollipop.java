package com.gm375.vidshare.util;

public class Lollipop {
    
    private static final int INITIAL_NUMBER = 0x80000001; // -N + 1
    private static final int START_LOLLIPOP_NUMBER = 0; // 0
    private static final int END_LOLLIPOP_NUMBER = 0x7FFFFFFE; // N - 2
    private static final int SEQ_NUMBER_SPACE_DIV_TWO = END_LOLLIPOP_NUMBER;
    
    private int currentNo;
    
    public Lollipop() {
        currentNo = INITIAL_NUMBER - 1;
    }
    
    public int getCurrent() throws Exception {
        if (currentNo == (INITIAL_NUMBER - 1)) {
            throw new Exception("getNext() should be called before getCurrent().");
        }
        return currentNo;
    }
    
    public int getNext() {
        if (currentNo == END_LOLLIPOP_NUMBER) {
            currentNo = START_LOLLIPOP_NUMBER;
        } else {
            currentNo++;
        }
        return currentNo;
    }
    
    public boolean isMoreRecent(int a) {
        if (a < 0) {
            if (a < currentNo) {
                return true;
            }
            return false;
        } else {
            if ((a < currentNo) && ((currentNo - a) < SEQ_NUMBER_SPACE_DIV_TWO)) {
                return true;
            }
            if ((currentNo >= 0) && (a > currentNo) && ((a - currentNo) > SEQ_NUMBER_SPACE_DIV_TWO)) {
                return true;
            }
            return false;
        }
    }
    
}
