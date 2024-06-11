package demos.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemo {
    private static final Logger log = LoggerFactory.getLogger(ConsumerDemo.class.getSimpleName());

    public static void main(String[] args) {
        log.info("I am a kafka Consumer!");
        String groupId = "my-java-application";
        String topic = "wikimedia.recentchange.connect";
        // create Consumer Properties
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "127.0.0.1:9092");
        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", StringDeserializer.class.getName());
        properties.setProperty("group.id", groupId);
        // latest -> 지금부터 보낸 메세지만 읽겠다
        properties.setProperty("auto.offset.reset", "earliest");
        // earliest도 완전 처음부터는 아니고 읽지 않은 오프셋부터 읽는듯
        // 파티션에 커밋된 오프셋이 없으면 파티션의 오프셋을 특정 위치로 재설정 (ex. 0) -> 커밋된 오프셋이라는 게..?
        // 아래는 챗 지피티 답변
        /**
         * 새로운 소비자 그룹이 처음 시작할 때 또는 기존 소비자 그룹이 이전에 커밋된 오프셋을 찾지 못할 때, 소비자는 주제의 가장 처음 오프셋부터 메시지를 읽기 시작합니다.
         * 기존에 커밋된 오프셋이 있는 경우에는 이 설정이 적용되지 않습니다. 이 경우 소비자는 커밋된 오프셋 이후의 메시지부터 읽기 시작합니다.
         */

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Arrays.asList(topic));

        while (true) {
            log.info("Polling");

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            for (ConsumerRecord<String, String> record : records) {
                log.info("Key: {} , Value: {} ", record.key(), record.value());
                log.info("Partition: {} , Offset: {} ", record.partition(), record.offset());
            }
        }
    }
}
