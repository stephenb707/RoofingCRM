package com.roofingcrm.realtime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for ActivityEventCreatedAppEvent and publishes to WebSocket.
 * Runs AFTER_COMMIT so clients never see events that roll back.
 */
@Component
public class ActivityEventCreatedListener {

    private final ActivityRealtimePublisher publisher;

    public ActivityEventCreatedListener(ActivityRealtimePublisher publisher) {
        this.publisher = publisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onActivityEventCreated(ActivityEventCreatedAppEvent event) {
        publisher.publish(
                event.getTenantId(),
                event.getEntityType(),
                event.getEntityId(),
                event.getActivityEventId());
    }
}
