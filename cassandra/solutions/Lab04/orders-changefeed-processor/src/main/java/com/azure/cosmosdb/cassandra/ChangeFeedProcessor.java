package com.azure.cosmosdb.cassandra;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;

import com.datastax.driver.core.JdkSSLOptions;
import com.datastax.driver.core.RemoteEndpointAwareJdkSSLOptions;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeFeedProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChangeFeedProcessor.class);

	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static String CHANGE_FEED_QUERY = "SELECT * FROM ordersapp.orders where COSMOS_CHANGEFEED_START_TIME()='"
			+ dtf.format(LocalDateTime.now()) + "'";

	public static void main(String[] s) throws Exception {
		final Session cassandraSession = Util.getSession();
		Invocation.Builder builder = Util.getPowerBIRestClient();
		ExecutorService pool = Executors.newFixedThreadPool(5);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			cassandraSession.close();
			LOGGER.info("Cassandra session closed");
			try {
				pool.shutdown();
				LOGGER.info("Thread pool shutdown");
			} catch (Exception e) {
				LOGGER.error("Failed to shutdown thread pool {}", e.getMessage());
			}

		}));

		byte[] token = null;

		SimpleStatement st = new SimpleStatement(CHANGE_FEED_QUERY);
		st.setFetchSize(100);
		while (true) {
			LOGGER.info("starting change feed loop");

			if (token != null) {
				st.setPagingStateUnsafe(token);
				LOGGER.debug("set paging token to " + new String(token));
			}
			System.out.println("executing query " + CHANGE_FEED_QUERY);
			ResultSet result = null;
			try {
				result = cassandraSession.execute(st);
				token = result.getExecutionInfo().getPagingState().toBytes();
				System.out.println("NEW paging token " + new String(token));
			} catch (Exception e) {
				throw e;
			}
			try {
				for (Row row : result) {

					ChangeFeedEvent cfe = ChangeFeedEvent.fromOrder(row);
					ChangeFeedEvents events = new ChangeFeedEvents();
					events.addEvent(cfe);

					pool.execute(new Runnable() {
						public void run() {
							try {
								LOGGER.info("Posting to PBI in thread {}", Thread.currentThread().getName());
								int postStatus = builder.post(Entity.json(events)).getStatus();

								LOGGER.info("PBI invocation result for order ID {} is {}", cfe.getId(), postStatus);
							} catch (Exception e) {
								LOGGER.error("Failed to send order info to Power BI {}", e.getMessage());
							}
						}
					});
				}
			} catch (Exception e) {
				// swallowed
				// row iteration will throw
				// com.datastax.driver.core.exceptions.DriverInternalError
				// if session is closed (when the program is stopped)
			}
		}
	}
}
