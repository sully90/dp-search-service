package com.github.onsdigital.search.searchstats;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.github.onsdigital.search.search.models.SearchStat;
import com.github.onsdigital.search.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author sullid (David Sullivan) on 05/03/2018
 * @project dp-search-service
 */
public class SQSSearchStatLoader implements SearchStatsLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQSSearchStatLoader.class);
    private static final JsonUtils<SearchStat> UNMARSHALLER;

    private final String queueName;
    private final AmazonSQS sqs;

    static {
        UNMARSHALLER = new JsonUtils<>(SearchStat.class);
    }

    public SQSSearchStatLoader(String queueName) {
        this.queueName = queueName;
        this.sqs = getClient();
    }

    @Override
    public List<SearchStat> getSearchStats() {
        // Fetches searchStats from an AWS SQS queue

        List<SearchStat> searchStats = new LinkedList<>();

        final String queueUrl = this.sqs.getQueueUrl(this.queueName).getQueueUrl();

        // Receive messages from the queue
        List<Message> messages = sqs.receiveMessage(queueUrl).getMessages();

        LOGGER.info(String.format("Got %d message(s)", messages.size()));

        for (Message message : messages) {
            // Get body as JSON string
            final String body = message.getBody();

            // Unmarshall as a SearchStat object
            try {
                SearchStat searchStat = UNMARSHALLER.fromJsonString(body);
                searchStats.add(searchStat);
            } catch (IOException e) {
                LOGGER.error("Error unmarshalling SQS message to type SearchStat", e);
            }
        }
        return searchStats;
    }

    private static AmazonSQS getClient() {
        // Set up creds
        EnvironmentVariableCredentialsProvider credentialsProvider = new EnvironmentVariableCredentialsProvider();
        AmazonSQS client = AmazonSQSClientBuilder.standard().withCredentials(credentialsProvider)
                .withRegion(Regions.EU_WEST_1)
                .build();

        return client;
    }

    public static void main(String[] args) {
        final String queueName = "iankent-search-stats-test";

        SQSSearchStatLoader searchStatLoader = new SQSSearchStatLoader(queueName);
        System.out.println(searchStatLoader.getSearchStats().size());
    }
}
