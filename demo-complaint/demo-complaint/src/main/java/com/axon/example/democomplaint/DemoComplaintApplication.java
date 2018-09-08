package com.axon.example.democomplaint;

import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.model.AggregateIdentifier;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.axonframework.commandhandling.model.AggregateLifecycle.apply;

@SpringBootApplication
public class DemoComplaintApplication {

    @Bean
    public Exchange exchange()
    {
        return ExchangeBuilder.fanoutExchange("ComplientEvent").build();
    }
    @Bean
    public Queue queue()
    {
        return QueueBuilder.durable("ComplientEvent").build();
    }
    @Bean
    public Binding binding()
    {
        return BindingBuilder.bind(queue()).to(exchange()).with("*").noargs();
    }
    @Autowired
    public void configure(AmqpAdmin admin){
       admin.declareExchange(exchange());
       admin.declareQueue(queue());
       admin.declareBinding(binding());
    }
	public static void main(String[] args) {
		SpringApplication.run(DemoComplaintApplication.class, args);
	}
	@RestController
	public static class ComplainAPI{
	    private final ComplaintQueryObjectRepository repository;
	    private final CommandGateway commandGateway;

        public ComplainAPI(ComplaintQueryObjectRepository repository,CommandGateway commandGateway) {
            this.repository = repository;
            this.commandGateway = commandGateway;
        }
        @PostMapping
        public void createCommand(@RequestBody Map<String,String> request){
            String id=UUID.randomUUID().toString();
             commandGateway.send(new FileComplintCommand(id,request.get("company"),request.get("description")),new CommandCallback<FileComplintCommand, Object>() {
                @Override
                public void onSuccess(CommandMessage<? extends FileComplintCommand> commandMessage, Object result) {
                    System.out.println("Success");
                }

                @Override
                public void onFailure(CommandMessage<? extends FileComplintCommand> commandMessage, Throwable cause) {
                   // commandGateway.send(new CancelMoneyTransferCommand(event.getTransferId()));
                    System.out.println("error=====>"+cause);
                }
            });
        }
        @GetMapping
	    public List<ComplaintQueryObject> findAll()
        {
              return repository.findAll();
        }
        @Aggregate
        public static class Complaint{
            @AggregateIdentifier
            private String complaintId;

            @CommandHandler
            public Complaint(FileComplintCommand cmd) {
               // this.complaintId = complaintId;
                Assert.hasLength(cmd.getCompany(),"company require");
                apply(new ComplaintFileEvent(cmd.getId(),cmd.getCompany(),cmd.getDescription()));
            }

            public Complaint() {
            }
            @EventSourcingHandler
            public void on(ComplaintFileEvent event)
            {
                this.complaintId=event.getId();
            }
        }
        @Component
        public static class ComplaintQueryObjectUpdater{
            private final ComplaintQueryObjectRepository repository;

            public ComplaintQueryObjectUpdater(ComplaintQueryObjectRepository repository) {
                this.repository = repository;
            }
            @EventHandler
            public void on(ComplaintFileEvent event)
            {
                this.repository.save(new ComplaintQueryObject(event.getId(),event.getCompany(),event.getDescription()));
            }
        }
        public static class FileComplintCommand {
            private String id;
            private  String company;
            private String description;


            public FileComplintCommand(String id, String company, String description) {
                this.id = id;
                this.company = company;
                this.description = description;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getCompany() {
                return company;
            }

            public void setCompany(String company) {
                this.company = company;
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }
        }
    }
}
