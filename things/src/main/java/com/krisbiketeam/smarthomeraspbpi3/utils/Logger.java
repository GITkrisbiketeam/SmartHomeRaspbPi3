package com.krisbiketeam.smarthomeraspbpi3.utils;

import android.app.Activity;
import android.databinding.DataBindingUtil;
import android.util.Log;

import com.krisbiketeam.smarthomeraspbpi3.R;
import com.krisbiketeam.smarthomeraspbpi3.databinding.ActivityHomeBinding;

@SuppressWarnings("unused")
public class Logger {

    private volatile static Logger sInstance;

    private LogConsole logger;

    public static Logger getInstance() {
        if (sInstance == null) {
            synchronized (Logger.class) {
                if (sInstance == null) {
                    sInstance = new Logger();
                }
            }
        }
        return sInstance;
    }

    public void setLogConsole(Activity activity) {
        logger = new LogConsole();
        ActivityHomeBinding binding = DataBindingUtil.setContentView(activity, R.layout
                .activity_home);
        binding.setLogConsole(logger); //This is where we bind the layout with the object*/
    }

    private int printlnInternal(int priority, String tag, String msg) {
        if(priority>Log.VERBOSE && logger != null) {
            String lastConsoleMsg = logger.getConsoleMessage();
            if (lastConsoleMsg == null) {
                lastConsoleMsg = "";
            }
            logger.setConsoleMessage(tag.concat(": ").concat(msg).concat("\n").concat(lastConsoleMsg));
        }
        return Log.println(priority, tag, msg);
    }

    private static int println(int priority, String tag, String msg) {
        return Logger.getInstance().printlnInternal(priority, tag, msg);
    }

    /**
     * Send a #VERBOSE log message.
     *  @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void v(String tag, String msg) {
        println(Log.VERBOSE, tag, msg);
    }

    /**
     * Send a #VERBOSE log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int v(String tag, String msg, Throwable tr) {
        return println(Log.VERBOSE, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    /**
     * Send a #DEBUG} log message.
     *  @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void d(String tag, String msg) {
        println(Log.DEBUG, tag, msg);
    }

    /**
     * Send a #DEBUG} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int d(String tag, String msg, Throwable tr) {
        return println(Log.DEBUG, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    /**
     * Send an #INFO log message.
     *  @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void i(String tag, String msg) {
        println(Log.INFO, tag, msg);
    }

    /**
     * Send a #INFO log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int i(String tag, String msg, Throwable tr) {
        return println(Log.INFO, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    /**
     * Send a #WARN} log message.
     *  @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void w(String tag, String msg) {
        println(Log.WARN, tag, msg);
    }

    /**
     * Send a #WARN} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int w(String tag, String msg, Throwable tr) {
        return println(Log.WARN, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    /*
     * Send a #WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    public static int w(String tag, Throwable tr) {
        return println(Log.WARN, tag, Log.getStackTraceString(tr));
    }

    /**
     * Send an #ERROR} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int e(String tag, String msg) {
        return println(Log.ERROR, tag, msg);
    }

    /**
     * Send a #ERROR} log message and log the exception.
     *  @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void e(String tag, String msg, Throwable tr) {
        println(Log.ERROR, tag, msg + '\n' + Log.getStackTraceString(tr));
    }


}
