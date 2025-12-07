package br.com.loomi.orders.domain.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an event related to order processing.
 * Events are published to Kafka for asynchronous processing.
 */
public class OrderEvent {

    /**
     * Unique event identifier
     */
    private UUID eventId;

    /**
     * Event type
     */
    private String eventType;

    /**
     * Event timestamp
     */
    private Instant timestamp;

    /**
     * Event payload data
     */
    private Map<String, Object> payload;

    /**
     * Gets the unique event identifier.
     *
     * @return the event ID
     */
    public UUID getEventId() {
        return eventId;
    }

    /**
     * Sets the unique event identifier.
     *
     * @param eventId the event ID to set
     */
    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    /**
     * Gets the event type.
     *
     * @return the event type
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Sets the event type.
     *
     * @param eventType the event type to set
     */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    /**
     * Gets the event timestamp.
     *
     * @return the timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the event timestamp.
     *
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the event payload data.
     *
     * @return the payload map
     */
    public Map<String, Object> getPayload() {
        return payload;
    }

    /**
     * Sets the event payload data.
     *
     * @param payload the payload map to set
     */
    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    /**
     * Creates a new order event with the specified type and payload.
     * Automatically generates a unique event ID and current timestamp.
     *
     * @param type the event type
     * @param payload the event payload data
     * @return a new order event instance
     */
    public static OrderEvent of(String type, Map<String, Object> payload) {
        OrderEvent event = new OrderEvent();
        event.eventId = UUID.randomUUID();
        event.eventType = type;
        event.timestamp = Instant.now();
        event.payload = payload;
        return event;
    }
}
