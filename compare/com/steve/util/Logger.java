package com.steve.util;

import org.apache.log4j.*;
//import org.apache.log4j.xml.DOMConfigurator;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;


public class Logger implements Log{
    private Log log;
    public static Logger logger;
    private Logger(Log _log)
    {
        try
        {
           BasicConfigurator.configure(new FileAppender(new PatternLayout("%d [%t] %-5p %c - %m%n"), "d:\\test.log"));

        }catch(Exception e)
        {
            e.printStackTrace();
        }
        this.log = _log;
    }

    public static Logger getLogger(Class clazz)
    {
//        DOMConfigurator.configure("c:\\log4j-conf.xml");
        Log log = LogFactory.getLog(clazz);
        return new Logger(log);
    	//return Logger.getLogger(clazz);
    }

    public boolean isDebugEnabled()
    {
        return log.isDebugEnabled();
    }
    public boolean isErrorEnabled()
    {
        return log.isErrorEnabled();
    }
    public boolean isFatalEnabled()
    {
        return log.isFatalEnabled();
    }

    public boolean isInfoEnabled()
    {
        return log.isInfoEnabled();
    }

    public boolean isTraceEnabled()
    {
        return log.isTraceEnabled();
    }

    public boolean isWarnEnabled()
    {
        return log.isWarnEnabled();
    }

    public void trace(Object object)
    {
        log.trace(object);
    }

    public void trace(Object object, Throwable throwable)
    {
        log.trace(object, throwable);
    }

    public void debug(Object object)
    {
        log.debug(object);
    }

    public void debug(Object object, Throwable throwable)
    {
        log.debug(object, throwable);
    }

    public void info(Object object)
    {
    }

    public void info(Object object, Throwable throwable)
    {
    }

    public void warn(Object object)
    {
        log.warn(object);
    }

    public void warn(Object object, Throwable throwable)
    {
        log.warn(object,throwable );
    }

    public void error(Object object)
    {
        log.error(object);
    }

    public void error(Object object, Throwable throwable)
    {
        log.error(object, throwable);
    }

    public void fatal(Object object)
    {
        log.fatal(object);
    }

    public void fatal(Object object, Throwable throwable)
    {
        log.fatal(object, throwable);
    }
    public static void main(String[] args)
    {
        Logger log = Logger.getLogger(Logger.class);
        log.error("aaaaatest");
    }
}
