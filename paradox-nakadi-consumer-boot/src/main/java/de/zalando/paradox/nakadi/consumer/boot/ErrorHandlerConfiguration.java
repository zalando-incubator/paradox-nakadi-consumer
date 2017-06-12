package de.zalando.paradox.nakadi.consumer.boot;

import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.zalando.paradox.nakadi.consumer.sqserrorhandler.SQSConfig;
import de.zalando.paradox.nakadi.consumer.sqserrorhandler.SQSErrorHandler;
import de.zalando.paradox.nakadi.consumer.sqserrorhandler.SQSFailedEventSource;

@ConditionalOnProperty(value = "enabled", prefix = SQSConfiguration.DEFAULT_SQS_PROPERTIES_PREFIX, havingValue = "true")
@Configuration
public class ErrorHandlerConfiguration {

    @Bean
    public AmazonSQS amazonSQS(final SQSConfiguration sqsConfiguration) {
        return AmazonSQSClientBuilder.standard().withRegion(sqsConfiguration.getRegion()).build();
    }

    @Bean
    public SQSConfig sqsConfig(final SQSConfiguration sqsConfiguration) {
        return new SQSConfig.Builder().queueUrl(sqsConfiguration.getQueueUrl()).build();
    }

    @Bean
    public ObjectMapper sqsErrorHandlerObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public SQSErrorHandler sqsErrorHandler(final SQSConfig sqsConfig, final AmazonSQS amazonSQS,
            @Qualifier("sqsErrorHandlerObjectMapper") final ObjectMapper objectMapper) {
        return new SQSErrorHandler(sqsConfig, amazonSQS, objectMapper);
    }

    @Bean
    public SQSFailedEventSource sqsFailedEventSource(final SQSConfig sqsConfig, final AmazonSQS amazonSQS,
            @Qualifier("sqsErrorHandlerObjectMapper") final ObjectMapper objectMapper) {
        return new SQSFailedEventSource(sqsConfig, amazonSQS, objectMapper);
    }

}
