package org.apache.pulsar.client.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.client.api.transaction.Transaction;

public class ChunkTest {

    public static void main(String[] args) throws PulsarClientException, ExecutionException, InterruptedException {

        String serviceUrl = "localhost";
        String token = "xxx=";


        String topicName = "test-topic";
        String consumerName = "test-java-consumer";
        String sub = "test-sub";
        SubscriptionType subType = SubscriptionType.Shared;

        try (PulsarClient client  = PulsarClient.builder()
                .serviceUrl(serviceUrl)
                .authentication(AuthenticationFactory.token(token))
                .enableTransaction(true)
                .build()) {

            // consumer
            try (Consumer<byte[]> consumer = client
                    .newConsumer()
                    .subscriptionName(sub)
                    .consumerName(consumerName)
                    .topic(topicName)
                    .subscriptionType(subType)
                    .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                    .subscribe()) {

                while (true) {
                    System.out.println("waiting for messages...");
                    Transaction txn = null;
                    Message<byte[]> msg = consumer.receive();
                    System.out.println("Message received: " + msg.getMessageId());
                    try {
                        txn = client
                                .newTransaction()
                                .withTransactionTimeout(500, TimeUnit.SECONDS)
                                .build()
                                .get();
                        System.out.println("consumer transaction created with txnID: " + txn.getTxnID());

                        consumer.acknowledgeAsync(msg.getMessageId(), txn).get();

                        System.out.println("Message acknowledgeAsync: " + msg.getMessageId());
                        txn.commit().get();
                        System.out.println("consumer transaction was committed with txnID: " + txn.getTxnID());

                    } catch (Exception e) {
                        if (txn != null) {
                            txn.abort();
                        }
                        consumer.negativeAcknowledge(msg);
                        System.err.println("Error processing message: " + e.getMessage());
                    }
                }
            }
        }
    }


}