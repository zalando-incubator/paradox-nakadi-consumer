package de.zalando.paradox.nakadi.consumer.sqserrorhandler;

import java.util.Collections;
import java.util.Optional;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.annotations.VisibleForTesting;

import de.zalando.paradox.nakadi.consumer.core.FailedEventSource;
import de.zalando.paradox.nakadi.consumer.core.domain.FailedEvent;

public class SQSFailedEventSource implements FailedEventSource<SQSFailedEvent> {

    @VisibleForTesting
    static final String EVENT_SOURCE_NAME = "SQSFailedEventSource";

    private final AmazonSQS amazonSQS;

    private final ObjectMapper objectMapper;

    private final String queueUrl;

    public SQSFailedEventSource(final SQSConfig sqsConfig, final AmazonSQS amazonSQS, final ObjectMapper objectMapper) {
        this.amazonSQS = amazonSQS;
        this.objectMapper = objectMapper;
        this.queueUrl = sqsConfig.getQueueUrl();
    }

    @Override
    public String getEventSourceName() {
        return EVENT_SOURCE_NAME;
    }

    @Override
    public Optional<SQSFailedEvent> getFailedEvent() {
        return fetchEvent().map(this::mapToSQSFailedEvent);
    }

    @Override
    public void commit(final SQSFailedEvent sqsFailedEvent) {
        amazonSQS.deleteMessage(queueUrl, sqsFailedEvent.getReceiptHandle());
    }

    @Override
    public long getSize() {

        final GetQueueAttributesRequest getQueueAttributesRequest = new GetQueueAttributesRequest(queueUrl,
                Collections.singletonList(QueueAttributeName.ApproximateNumberOfMessages.name()));

        final GetQueueAttributesResult queueAttributes = amazonSQS.getQueueAttributes(getQueueAttributesRequest);

        if (queueAttributes.getAttributes() != null) {
            return Long.valueOf(queueAttributes.getAttributes().getOrDefault(
                        QueueAttributeName.ApproximateNumberOfMessages.name(), "0"));
        } else {
            return 0L;
        }
    }

    private Optional<Message> fetchEvent() {

        final ReceiveMessageResult receiveMessageResult = amazonSQS.receiveMessage(queueUrl);

        if (receiveMessageResult.getMessages() != null && !receiveMessageResult.getMessages().isEmpty()) {
            return Optional.of(receiveMessageResult.getMessages().get(0));
        } else {
            return Optional.empty();
        }
    }

    private SQSFailedEvent mapToSQSFailedEvent(final Message message) {
        try {
            final FailedEvent failedEvent = objectMapper.readValue(message.getBody(), FailedEvent.class);
            final SQSFailedEvent sqsFailedEvent = new SQSFailedEvent(failedEvent);
            sqsFailedEvent.setId(message.getMessageId());
            sqsFailedEvent.setReceiptHandle(message.getReceiptHandle());
            return sqsFailedEvent;
        } catch (final Exception e) {
            throw new IllegalStateException(String.format(
                    "Exception occurred during deserialization. Message id = [%s]", message.getMessageId()), e);
        }
    }

}
