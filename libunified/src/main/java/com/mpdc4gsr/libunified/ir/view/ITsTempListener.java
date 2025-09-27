package com.mpdc4gsr.libunified.ir.view;

public interface ITsTempListener {

    default float tempCorrectByTs(Float temp) {
        return temp;
    }

}
