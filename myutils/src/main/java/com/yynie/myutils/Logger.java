/**
 * BSD 2-Clause License
 *
 * Copyright (c) 2018, yynie
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package com.yynie.myutils;

import android.util.Log;

/**
 * An utility class used to print log strings on Android Devices.<br>
 * More powerful level controller provided.
 * Easy to use.
 *
 * @author <a href="mailto:yy_nie@hotmail.com">Yan.Nie</a>
 * */
public class Logger {

    /**
     * Log Level. Priority(high -> low):<br>
     *     ERROR, WARN, INFO, DEBUG <br>
     *     and OFF means close all log output.
     * */
    public enum Level {
        /**
         *  At DEBUG level, all messages would be logged
         * */
        DEBUG ( 0 ),

        /**
         *  At INFO level, {@link Logger#i},{@link Logger#w},{@link Logger#e} would be logged
         * */
        INFO ( 1 ),

        /**
         *  At WARN level, {@link Logger#w},{@link Logger#e} would be logged
         * */
        WARN ( 2 ),

        /**
         *  At ERROR level, only {@link Logger#e} would be logged
         * */
        ERROR ( 3 ),

        /**
         *  log closed
         * */
        OFF ( 4 );

        private int value;
        Level(int value) {
            this.value = value;
        }

        private int value() {
            return value;
        }

        private static Level fromIntValue(int v){
            for (int i = 0; i < values().length; i++) {
                Level lev = Level.values()[i];
                if (v == lev.value()) {
                    return lev;
                }
            }
            return null;
        }

        private static Level getHigher(Level one, Level two){
            if(one.value > two.value)
                return one;
            else
                return two;
        }

        private boolean isAllow(Level allow){
            return this.value <= allow.value;
        }
    }

    /**
     *  Main Level control at the global scope. The default value is ERROR.
     *
     *  @see Logger#setGlobalLevel(Level)
     *  @see Logger#getGlobalLevel()
     */
    private static Level LEVEL_GLOBEL = Level.ERROR;

    private final String TAG;
    private final Level LEVEL;

    /**
     * Create a Logger instance for one Class in which scope you want to use Looger to print log
     * messages with the class's simpleName as the tag.
     *
     * @param clazz The owner class.
     * @param level set to the {@link Level} which would be the lowest level control in this scope.
     *              Pls note that level lower than the global control will be ignored.
     * */
    public static Logger get(Class clazz, Level level){
        return new Logger(clazz.getSimpleName(), level);
    }

    /**
     * Create a Logger instance for a specified TAG.
     *
     * @param tag specify a tag
     * @param level set to the {@link Level} which would be the lowest level control for this instance.
     *              Pls note that level lower than the global control will be ignored.
     * */
    public static Logger get(String tag, Level level){
        return new Logger(tag, level);
    }

    /**
     * get currently global level
     *
     * @return  global level
     * */
    public static Level getGlobalLevel(){
        return LEVEL_GLOBEL;
    }

    /**
     * set global level. It will take effect immediately.
     *
     * @param level
     * */
    public static boolean setGlobalLevel(Level level){
        LEVEL_GLOBEL = level;
        return true;
    }

    /**
     * set global level with the Level's integer value. It will take effect immediately.
     * @param levelValue
     * @return return false if levelValue is illegal. return true if success.
     * */
    public static boolean setGlobalLevel(int levelValue){
        Level set = Level.fromIntValue(levelValue);
        if(set == null) return false;
        LEVEL_GLOBEL = set;
        return true;
    }

    private Logger(String tag, Level level){
        TAG = tag;
        LEVEL = level; //Level.getHigher(level, LEVEL_HIGH);
    }

    private boolean isLevelAllowed(Level check){
        Level higher = Level.getHigher(LEVEL, LEVEL_GLOBEL);
        return higher.isAllow(check);
    }

    /**
     * log a message at DEBUG level
     *
     * @param debug debug message
     * */
    public void d (String debug){
        if(isLevelAllowed(Level.DEBUG)) Log.d(TAG, debug);
    }

    /**
     * log a message at INFO level
     *
     * @param info info message
     * */
    public void i (String info){
        if(isLevelAllowed(Level.INFO)) Log.i(TAG, info);
    }

    /**
     * log a message at WARN level
     *
     * @param warning warning message
     * */
    public void w (String warning){
        if(isLevelAllowed(Level.WARN)) Log.w(TAG, warning);
    }

    /**
     * log a message at ERROR level
     *
     * @param error error message
     * */
    public void e (String error){
        if(isLevelAllowed(Level.ERROR)) Log.e(TAG, error);
    }

    /**
     * log a message at INFO level, additionally with current process info (PID, TID, UID)
     *
     * @param info info message
     * */
    public void printProcessInfo(String info){
        i(info + "@[PID:" + android.os.Process.myPid() + "] [TID:" + android.os.Process.myTid() + "] [UID:" + android.os.Process.myUid() + "]");
    }

}
