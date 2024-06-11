package demos.kafka.opensearch;

import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class OpenSearchConsumerWithBulkRequest {
    public static void main(String[] args) throws IOException {
        Logger logger = LoggerFactory.getLogger(OpenSearchConsumerWithBulkRequest.class.getSimpleName());

        // create an OpenSearch Client
        RestHighLevelClient openSearchClient = createOpenSearchClient();

        // create our kafka Client
        KafkaConsumer<String, String> consumer = createKafkaConsumer();

        final Thread main = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Detected a shutdown, let's exit by calling consumer.wakeup()");
            consumer.wakeup(); // wakeup 하면 아래 Poll() 로직에서 컨슈머에 예외가 발생함. 그런 다음 메인 스레드에 합류해서
            // 하위 코드가 전부 완료되고 더 이상 실행되지 않을 때까지 기다림 -> 아래 코드 예외 발생하니까 try catch로 감싸야함
            try {
                main.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));

        // create the index on OpenSearch if it does not exist
        try (openSearchClient; consumer) { // 에러 발생하거나 로직 끝나면 openSearchClient 닫음
            if (!openSearchClient.indices().exists(new GetIndexRequest("wikimedia"), RequestOptions.DEFAULT)) {
                logger.info("create index");
                CreateIndexRequest createIndexRequest = new CreateIndexRequest("wikimedia");
                openSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
            } else {
                logger.info("index exists");
            }

            consumer.subscribe(Collections.singleton("wikimedia.recentchange"));

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(3000));
                int count = records.count();
                logger.info("Received {} records", count);

                BulkRequest bulkRequest = new BulkRequest();

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        // make sure consumer is idempotent
                        // At Least Once + Idempotent -> Exactly Once!
                        String id = extractId(record.value());

                        IndexRequest indexRequest = new IndexRequest("wikimedia").source(record.value(), XContentType.JSON).id(id);
//                        IndexResponse indexResponse = openSearchClient.index(indexRequest, RequestOptions.DEFAULT);
                        bulkRequest.add(indexRequest);
                    } catch (Exception e) {

                    }
                }

                if (bulkRequest.numberOfActions() > 0) {
                    BulkResponse bulkResponse = openSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                    logger.info("Inserted {} records", bulkResponse.getItems().length);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    consumer.commitSync();
                    logger.info("Offsets have been committed");
                }
            }
        }catch (WakeupException e) {
            logger.info("Consumer is starting to shutdown");
        } catch (Exception e) {
            logger.error("Unexpected exception in the consumer", e);
        }finally {
            consumer.close();
            openSearchClient.close();
            logger.info("The consumer is now gracefully shut down");
        }

        // main code logic

        // close things


    }

    private static String extractId(String json) {
        return JsonParser.parseString(json)
                .getAsJsonObject()
                .get("meta")
                .getAsJsonObject()
                .get("id")
                .getAsString();
    }

    private static KafkaConsumer<String, String> createKafkaConsumer() {
        String bootstrapServer = "127.0.0.1:9092";
        String groupId = "consumer-opensearch-demo";

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        // 컨슈머는 오프셋을 자동으로 커밋하지 않음
        // 수동으로 커밋해주지 않으면 컨슈머 다시 시작할 때마다 읽었던 데이터를 다시 읽음!
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        return new KafkaConsumer<>(properties);
    }

    public static RestHighLevelClient createOpenSearchClient() {
        String connString = "http://localhost:9200";
//        String connString = "https://c9p5mwld41:45zeygn9hy@kafka-course-2322630105.eu-west-1.bonsaisearch.net:443";

        // we build a URI from the connection string
        RestHighLevelClient restHighLevelClient;
        URI connUri = URI.create(connString);
        // extract login information if it exists
        String userInfo = connUri.getUserInfo();

        if (userInfo == null) {
            // REST client without security
            restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), "http")));

        } else {
            // REST client with security
            String[] auth = userInfo.split(":");

            CredentialsProvider cp = new BasicCredentialsProvider();
            cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(auth[0], auth[1]));

            restHighLevelClient = new RestHighLevelClient(
                    RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), connUri.getScheme()))
                            .setHttpClientConfigCallback(
                                    httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(cp)
                                            .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())));


        }

        return restHighLevelClient;
    }
}
