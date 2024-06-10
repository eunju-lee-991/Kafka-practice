package demos.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemoKeys {
    private static final Logger log = LoggerFactory.getLogger(ProducerDemoKeys.class.getSimpleName());

    public static void main(String[] args) {
        log.info("I am a kafka producer!");
        // create Producer Properties
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "127.0.0.1:9092");
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());

        // create the producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < 10; i++) {
                String topic = "demo_java";
                String key = "id_" + i;
                String value = "hello world " + i;

                ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
                producer.send(record,
                        (metadata, exception) -> {
                            if(exception == null) {
                                log.info("key: {}", key);
                                log.info("partition: {}", metadata.partition());
                            } else {
                                log.error("Error!! ", exception);
                            }
                        });
            }
        }

        producer.flush(); // 카프카 메세지 전송은 비동기 방식이기 때문에 플러시 안해주면 전송하기 전에 프로그램 끝나버림
        producer.close();
    }
}
