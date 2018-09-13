package com.example.democomplaintstat;

import com.axon.example.democomplaint.ComplaintFileEvent;
import com.mongodb.MongoClient;
import com.rabbitmq.client.Channel;
import org.axonframework.amqp.eventhandling.DefaultAMQPMessageConverter;
import org.axonframework.amqp.eventhandling.spring.SpringAMQPMessageSource;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.mongo.DefaultMongoTemplate;
import org.axonframework.mongo.eventsourcing.eventstore.MongoEventStorageEngine;
import org.axonframework.serialization.Serializer;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
@SpringBootApplication
public class DemoComplaintStatApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoComplaintStatApplication.class, args);
	}
    @ProcessingGroup("statistics")
    @RestController
    public static class StatisticsAPI {

        private final ConcurrentMap<String, AtomicLong> statistics = new ConcurrentHashMap<>();

        @EventHandler
        public void on(ComplaintFileEvent event) {
            statistics.computeIfAbsent(event.getCompany(), k -> new AtomicLong()).incrementAndGet();
        }

        @GetMapping
        public ConcurrentMap<String, AtomicLong> showStatistics() {
            return statistics;
        }
    }
    @Bean
    public SpringAMQPMessageSource statisticsQueue(Serializer serializer) {
        return new SpringAMQPMessageSource(new DefaultAMQPMessageConverter(serializer)) {

            @RabbitListener(queues = "ComplientEvent")
            @Override
            public void onMessage(Message message, Channel channel)  {
                super.onMessage(message, channel);
            }
        };
    }
    @Bean
    public EventStorageEngine eventStore(MongoClient client) {
        return new MongoEventStorageEngine (new DefaultMongoTemplate (client));
    }
}
