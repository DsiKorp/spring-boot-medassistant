package com.dsikorp.iamedassistan.agent;

import com.dsikorp.iamedassistan.dto.agent.AppointmentSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class PendingBookingStore {

    private static final Logger log = LoggerFactory.getLogger(PendingBookingStore.class);

    private final ConcurrentHashMap<Long, AppointmentSelection> pending = new ConcurrentHashMap<>();

    public void store(Long userId, AppointmentSelection selection){

        pending.put(userId, selection);
        log.info("store: pending={}", pending);
    }

    public AppointmentSelection retrieve(Long userId){

        log.info("retrieve: pending={}", pending);
        return pending.get(userId);
    }

    public AppointmentSelection remove(Long userId){

        log.info("remove: pending={}", pending);
        return pending.remove(userId);
    }
}
