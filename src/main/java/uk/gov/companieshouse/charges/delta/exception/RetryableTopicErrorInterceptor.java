package uk.gov.companieshouse.charges.delta.exception;

import static java.lang.String.format;
import static org.springframework.kafka.support.KafkaHeaders.EXCEPTION_CAUSE_FQCN;
import static org.springframework.kafka.support.KafkaHeaders.EXCEPTION_STACKTRACE;
import static uk.gov.companieshouse.charges.delta.ChargesDeltaConsumerApplication.NAMESPACE;

import java.util.Map;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import uk.gov.companieshouse.charges.delta.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

/**
 * Retryable Topic Error Interceptor.
 */
public class RetryableTopicErrorInterceptor implements ProducerInterceptor<String, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        String nextTopic = record.topic().contains("-error") ? getNextErrorTopic(record)
                : record.topic();
        LOGGER.info(format("Moving record into new topic: %s with value: %s",
                nextTopic, record.value()), DataMapHolder.getLogMap());
        if (nextTopic.contains("-invalid")) {
            return new ProducerRecord<>(nextTopic, record.key(), record.value());
        }

        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata recordMetadata, Exception ex) {
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> map) {
    }

    private String getNextErrorTopic(ProducerRecord<String, Object> record) {
        Header header1 = record.headers().lastHeader(EXCEPTION_CAUSE_FQCN);
        Header header2 = record.headers().lastHeader(EXCEPTION_STACKTRACE);
        return ((header1 != null
                && new String(header1.value()).contains(NonRetryableErrorException.class.getName()))
                || (header2 != null
                && new String(header2.value()).contains(
                        NonRetryableErrorException.class.getName())))
                ? record.topic().replace("-error", "-invalid") : record.topic();
    }
}
