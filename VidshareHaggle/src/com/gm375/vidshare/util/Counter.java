package com.gm375.vidshare.util;

public class Counter {
    
    public static final int INITIAL_NUMBER = Integer.MIN_VALUE + 2;
    public static final int MAXIMUM_NUMBER = Integer.MAX_VALUE - 1;
    
    private int currentNo;
    
    public Counter() {
        currentNo = INITIAL_NUMBER - 1;
    }
    
    public int getCurrent() throws IllegalStateException {
        if (currentNo == (INITIAL_NUMBER - 1)) {
            throw new IllegalStateException("getNext() should be called before getCurrent().");
        }
        return currentNo;
    }
    
    public int getNext() throws RuntimeException {
        if (currentNo == MAXIMUM_NUMBER) {
            throw new RuntimeException("Counter overflow.");
        } else {
            return ++currentNo;
        }
    }
    
}
