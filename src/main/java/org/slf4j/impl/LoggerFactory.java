package org.slf4j.impl;


import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;


public class LoggerFactory implements ILoggerFactory {

    private MavenLoggerAdapter logger;

    public LoggerFactory() {
        logger = new MavenLoggerAdapter("");
    }

    public Logger getLogger(String name) {
        return logger;
    }
}