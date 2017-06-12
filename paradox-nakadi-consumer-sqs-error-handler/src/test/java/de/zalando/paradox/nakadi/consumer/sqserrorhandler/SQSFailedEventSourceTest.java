package de.zalando.paradox.nakadi.consumer.sqserrorhandler;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomUtils.nextLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static de.zalando.paradox.nakadi.consumer.sqserrorhandler.SQSFailedEventSource.EVENT_SOURCE_NAME;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.http.SdkHttpMetadata;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageResult;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.zalando.paradox.nakadi.consumer.core.domain.EventType;
import de.zalando.paradox.nakadi.consumer.core.domain.FailedEvent;

public class SQSFailedEventSourceTest {

    private SQSFailedEventSource sqsFailedEventSource;

    private ObjectMapper objectMapper;

    private SQSConfig sqsConfig;

    private AmazonSQS amazonSQS;

    @Before
    public void setUp() {
        amazonSQS = mock(AmazonSQS.class);
        sqsConfig = mock(SQSConfig.class);
        objectMapper = new ObjectMapper();

        final String queueName = randomAlphabetic(10);
        when(sqsConfig.getQueueName()).thenReturn(queueName);

        final GetQueueUrlResult getQueueUrlResult = new GetQueueUrlResult();
        final SdkHttpMetadata responseMetadata = mock(SdkHttpMetadata.class);
        when(responseMetadata.getHttpStatusCode()).thenReturn(200);
        getQueueUrlResult.setSdkHttpMetadata(responseMetadata);
        getQueueUrlResult.setQueueUrl(randomAlphabetic(10));

        when(amazonSQS.getQueueUrl(queueName)).thenReturn(getQueueUrlResult);

        sqsFailedEventSource = new SQSFailedEventSource(sqsConfig, amazonSQS, objectMapper);
    }

    @Test
    public void testShouldReturnSQSEventSourceName() {
        assertThat(sqsFailedEventSource.getEventSourceName()).isEqualTo(EVENT_SOURCE_NAME);
        verify(amazonSQS).getQueueUrl(anyString());
    }

    @Test
    public void testShouldFailWhenQueueUrlIsNotRetrieved() {

        reset(amazonSQS, sqsConfig);

        final String queueName = randomAlphabetic(10);
        when(sqsConfig.getQueueName()).thenReturn(queueName);

        final GetQueueUrlResult getQueueUrlResult = new GetQueueUrlResult();
        final SdkHttpMetadata responseMetadata = mock(SdkHttpMetadata.class);
        when(responseMetadata.getHttpStatusCode()).thenReturn(400);
        getQueueUrlResult.setSdkHttpMetadata(responseMetadata);

        when(amazonSQS.getQueueUrl(anyString())).thenReturn(getQueueUrlResult);

        assertThatThrownBy(() -> new SQSFailedEventSource(sqsConfig, amazonSQS, objectMapper)).isInstanceOf(
            NullPointerException.class).hasMessageContaining("The queue url was not retrieved. Queue name =");
        verify(amazonSQS).getQueueUrl(anyString());
    }

    @Test
    public void testShouldReturnTotalNumberOfFailedEvents() {

        final SdkHttpMetadata responseMetadata = mock(SdkHttpMetadata.class);
        when(responseMetadata.getHttpStatusCode()).thenReturn(200);

        final String totalNumberOfFailedEvents = RandomStringUtils.randomNumeric(4);
        final Map<String, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.ApproximateNumberOfMessages.name(), totalNumberOfFailedEvents);

        final GetQueueAttributesResult getQueueAttributesResult = new GetQueueAttributesResult();
        getQueueAttributesResult.setSdkHttpMetadata(responseMetadata);
        getQueueAttributesResult.setAttributes(attributes);

        when(amazonSQS.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenReturn(getQueueAttributesResult);

        assertThat(sqsFailedEventSource.getSize()).isEqualTo(Long.valueOf(totalNumberOfFailedEvents));
    }

    @Test
    public void testShouldReturnDefaultTotalNumberOfFailedEvents() {
        final SdkHttpMetadata responseMetadata = mock(SdkHttpMetadata.class);
        when(responseMetadata.getHttpStatusCode()).thenReturn(200);

        final GetQueueAttributesResult getQueueAttributesResult = new GetQueueAttributesResult();
        getQueueAttributesResult.setSdkHttpMetadata(responseMetadata);
        getQueueAttributesResult.setAttributes(new HashMap<>());

        when(amazonSQS.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenReturn(getQueueAttributesResult);
        assertThat(sqsFailedEventSource.getSize()).isEqualTo(Long.valueOf(0L));
    }

    @Test
    public void testShouldReturnDefaultTotalNumberOfFailedEventsWhenThereIsNoQueueAttributes() {
        final SdkHttpMetadata responseMetadata = mock(SdkHttpMetadata.class);
        when(responseMetadata.getHttpStatusCode()).thenReturn(200);

        final GetQueueAttributesResult getQueueAttributesResult = new GetQueueAttributesResult();
        getQueueAttributesResult.setSdkHttpMetadata(responseMetadata);
        when(amazonSQS.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenReturn(getQueueAttributesResult);
        assertThat(sqsFailedEventSource.getSize()).isEqualTo(Long.valueOf(0L));
    }

    @Test
    public void testShouldCommitTheMessageSuccessfully() {
        final SdkHttpMetadata responseMetadata = mock(SdkHttpMetadata.class);
        when(responseMetadata.getHttpStatusCode()).thenReturn(200);

        final DeleteMessageResult deleteMessageResult = new DeleteMessageResult();
        deleteMessageResult.setSdkHttpMetadata(responseMetadata);

        when(amazonSQS.deleteMessage(anyString(), anyString())).thenReturn(deleteMessageResult);

        sqsFailedEventSource.commit(new SQSFailedEvent(new FailedEvent()));

        verify(amazonSQS).deleteMessage(anyString(), anyString());
    }

    @Test
    public void testShouldFetchEmptyMessageFromSQS() {
        when(amazonSQS.receiveMessage(anyString())).thenReturn(new ReceiveMessageResult());
        assertThat(sqsFailedEventSource.getFailedEvent()).isEmpty();
    }

    @Test
    public void testShouldFailWhileDeserializationOfFailedEvent() {

        final ReceiveMessageResult receiveMessageResult = new ReceiveMessageResult();
        receiveMessageResult.setMessages(Collections.singleton(new Message()));

        when(amazonSQS.receiveMessage(anyString())).thenReturn(receiveMessageResult);
        assertThatThrownBy(() -> sqsFailedEventSource.getFailedEvent()).isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("Exception occurred during deserialization. Message id =");
    }

    @Test
    public void testShouldFetchFailedEventFromSQS() throws JsonProcessingException {

        final FailedEvent failedEvent = new FailedEvent();
        failedEvent.setRawEvent(randomAlphabetic(10));
        failedEvent.setOffset(randomAlphabetic(10));
        failedEvent.setConsumerName(randomAlphabetic(10));
        failedEvent.setFailedTimeInMilliSeconds(nextLong(1, 10));
        failedEvent.setEventType(new EventType(randomAlphabetic(10)));
        failedEvent.setPartition(randomAlphabetic(10));
        failedEvent.setStackTrace(randomAlphabetic(10));

        final Message message = new Message();
        message.setMessageId(randomAlphabetic(10));
        message.setReceiptHandle(randomAlphabetic(10));
        message.setBody(objectMapper.writeValueAsString(failedEvent));

        final ReceiveMessageResult receiveMessageResult = new ReceiveMessageResult();
        receiveMessageResult.setMessages(Collections.singleton(message));

        when(amazonSQS.receiveMessage(anyString())).thenReturn(receiveMessageResult);

        final Optional<SQSFailedEvent> sqsFailedEventOptional = sqsFailedEventSource.getFailedEvent();
        assertThat(sqsFailedEventOptional).isPresent();

        final SQSFailedEvent sqsFailedEvent = sqsFailedEventOptional.get();
        assertThat(sqsFailedEvent.getRawEvent()).isEqualTo(failedEvent.getRawEvent());
        assertThat(sqsFailedEvent.getReceiptHandle()).isEqualTo(message.getReceiptHandle());
        assertThat(sqsFailedEvent.getConsumerName()).isEqualTo(failedEvent.getConsumerName());
        assertThat(sqsFailedEvent.getEventType()).isEqualTo(failedEvent.getEventType());
        assertThat(sqsFailedEvent.getId()).isEqualTo(message.getMessageId());
        assertThat(sqsFailedEvent.getOffset()).isEqualTo(failedEvent.getOffset());
        assertThat(sqsFailedEvent.getPartition()).isEqualTo(failedEvent.getPartition());
        assertThat(sqsFailedEvent.getStackTrace()).isEqualTo(failedEvent.getStackTrace());
        assertThat(sqsFailedEvent.getFailedTimeInMilliSeconds()).isEqualTo(failedEvent.getFailedTimeInMilliSeconds());
    }

    @Test
    public void testShouldIgnoreUnknownFields() throws Exception {
        final Message message = new Message();
        message.setBody("{\"unknown_field\": 1}");

        final ReceiveMessageResult receiveMessageResult = new ReceiveMessageResult();
        receiveMessageResult.setMessages(Collections.singleton(message));

        when(amazonSQS.receiveMessage(anyString())).thenReturn(receiveMessageResult);

        final Optional<SQSFailedEvent> sqsFailedEventOptional = sqsFailedEventSource.getFailedEvent();
        assertThat(sqsFailedEventOptional).isPresent();
    }

}
