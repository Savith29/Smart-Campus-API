package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe singleton in-memory data store.
 * Starts EMPTY — all data is created through API calls during the demonstration.
 * Uses ConcurrentHashMap to prevent race conditions with per-request JAX-RS lifecycle.
 */
public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    private final ConcurrentHashMap<String, Room>               rooms    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Sensor>             sensors  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    private DataStore() {
        // No pre-seeded data — all resources created via API during demo
    }

    public static DataStore getInstance() { return INSTANCE; }

    public ConcurrentHashMap<String, Room>                getRooms()    { return rooms; }
    public ConcurrentHashMap<String, Sensor>              getSensors()  { return sensors; }
    public ConcurrentHashMap<String, List<SensorReading>> getReadings() { return readings; }
}
