package demos.kafka.wikimedia;

import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.MessageEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WikimediaChangeHandler implements EventHandler {
    KafkaProducer<String, String> kafkaProducer;
    String topic;
    private final Logger log = LoggerFactory.getLogger(WikimediaChangeHandler.class.getSimpleName());

    public WikimediaChangeHandler(KafkaProducer<String, String> kafkaProducer, String topic) {
        this.kafkaProducer = kafkaProducer;
        this.topic = topic;
    }

    // when stream is open
    @Override
    public void onOpen() {
        // nothing here
    }

    // when stream is closed
    @Override
    public void onClosed() {
        kafkaProducer.close();
    }

    // http에서 온 메세지를 수신
    @Override
    public void onMessage(String s, MessageEvent messageEvent) {
        log.info("getData: {}", messageEvent.getData());

        // asynchronous
        kafkaProducer.send(new ProducerRecord<>(topic, messageEvent.getData()));
    }

    @Override
    public void onComment(String s) {

    }

    @Override
    public void onError(Throwable throwable) {
        log.error("Error in Stream reading", throwable);
    }
}
