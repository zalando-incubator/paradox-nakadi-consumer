package de.zalando.paradox.nakadi.consumer.core.domain;

public class FailedEvent {

    private String id;

    private String consumerName;

    private String offset;

    private EventType eventType;

    private String partition;

    private Throwable throwable;

    private String rawEvent;

    private long failedTimeInMilliSeconds;

    public FailedEvent() { }

    public FailedEvent(final FailedEvent failedEvent) {
        this.id = failedEvent.getId();
        this.consumerName = failedEvent.getConsumerName();
        this.offset = failedEvent.getOffset();
        this.eventType = failedEvent.getEventType();
        this.partition = failedEvent.getPartition();
        this.throwable = failedEvent.getThrowable();
        this.rawEvent = failedEvent.getRawEvent();
        this.failedTimeInMilliSeconds = failedEvent.getFailedTimeInMilliSeconds();
    }

    public String getConsumerName() {
        return consumerName;
    }

    public String getOffset() {
        return offset;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getPartition() {
        return partition;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public String getRawEvent() {
        return rawEvent;
    }

    public long getFailedTimeInMilliSeconds() {
        return failedTimeInMilliSeconds;
    }

    public void setConsumerName(final String consumerName) {
        this.consumerName = consumerName;
    }

    public void setOffset(final String offset) {
        this.offset = offset;
    }

    public void setEventType(final EventType eventType) {
        this.eventType = eventType;
    }

    public void setPartition(final String partition) {
        this.partition = partition;
    }

    public void setThrowable(final Throwable throwable) {
        this.throwable = throwable;
    }

    public void setRawEvent(final String rawEvent) {
        this.rawEvent = rawEvent;
    }

    public void setFailedTimeInMilliSeconds(final long failedTimeInMilliSeconds) {
        this.failedTimeInMilliSeconds = failedTimeInMilliSeconds;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }
}
