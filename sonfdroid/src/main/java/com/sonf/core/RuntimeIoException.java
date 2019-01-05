package com.sonf.core;


/**
 * different from IOException in that doesn't trigger force session close,
 *
 *  @author <a href="mailto:yy_nie@hotmail.com">Yan.Nie</a>
 */
public class RuntimeIoException extends RuntimeException {

    public RuntimeIoException() {
        super();
    }

    public RuntimeIoException(String message) {
        super(message);
    }

    public RuntimeIoException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuntimeIoException(Throwable cause) {
        super(cause);
    }
}
