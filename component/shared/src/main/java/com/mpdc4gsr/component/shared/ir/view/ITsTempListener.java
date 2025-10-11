package com.mpdc4gsr.component.shared.ir.view;

public interface ITsTempListener {

    default float tempCorrectByTs(Float temp) {
        return temp;
    }

}


