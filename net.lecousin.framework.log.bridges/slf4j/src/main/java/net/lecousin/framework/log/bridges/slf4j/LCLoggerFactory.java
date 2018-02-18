package net.lecousin.framework.log.bridges.slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.lecousin.framework.application.LCCore;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class LCLoggerFactory implements ILoggerFactory {

    private ConcurrentMap<String, Logger> loggerMap;

    public LCLoggerFactory() {
        loggerMap = new ConcurrentHashMap<String, Logger>();
    }

    /**
     * Return an appropriate {@link SimpleLogger} instance by name.
     */
    @Override
    public Logger getLogger(String name) {
        Logger simpleLogger = loggerMap.get(name);
        if (simpleLogger != null) {
            return simpleLogger;
        }
        Logger newInstance = new LCLogger(name, LCCore.getApplication().getLoggerFactory().getLogger(name));
        Logger oldInstance = loggerMap.putIfAbsent(name, newInstance);
        return oldInstance == null ? newInstance : oldInstance;
    }

    /**
     * Clear the internal logger cache.
     *
     * This method is intended to be called by classes (in the same package) for
     * testing purposes. This method is internal. It can be modified, renamed or
     * removed at any time without notice.
     *
     * You are strongly discouraged from calling this method in production code.
     */
    void reset() {
        loggerMap.clear();
    }

}
