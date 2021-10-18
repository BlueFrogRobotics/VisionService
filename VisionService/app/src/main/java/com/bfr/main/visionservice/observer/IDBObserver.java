package com.bfr.main.visionservice.observer;

import java.io.IOException;

/**
 * Pattern Observer
 */
public interface IDBObserver {
    public void update(String message) throws IOException;
}
