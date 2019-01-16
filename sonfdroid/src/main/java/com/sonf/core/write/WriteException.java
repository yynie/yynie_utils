package com.sonf.core.write;

import java.io.IOException;

public class WriteException extends IOException {

    public WriteException(String message){
        super(message);
    }
}
