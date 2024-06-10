package demos.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.*;

import java.util.Properties;

public class ProducerDemo {
    private static final Logger log = LoggerFactory.getLogger(ProducerDemo.class.getSimpleName());

    public static void main(String[] args) {
        log.info("I am a kafka producer!");
        // create Producer Properties
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "127.0.0.1:9092");
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());

        // create the producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        /**
         *
         * demo_java 토픽 만들어야함 !!!
         */
        ProducerRecord<String, String> record = new ProducerRecord<>("demo_java", "hello world");

        producer.send(record);
        producer.flush(); // 카프카 메세지 전송은 비동기 방식이기 때문에 플러시 안해주면 전송하기 전에 프로그램 끝나버림
        producer.close();
    }
}
