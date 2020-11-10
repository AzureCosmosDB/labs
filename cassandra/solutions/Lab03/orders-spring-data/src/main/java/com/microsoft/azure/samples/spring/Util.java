package com.microsoft.azure.samples.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.JdkSSLOptions;
import com.datastax.driver.core.LatencyTracker;
import com.datastax.driver.core.RemoteEndpointAwareJdkSSLOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.Statement;
import com.microsoft.azure.cosmos.cassandra.CosmosLoadBalancingPolicy;
import com.microsoft.azure.cosmos.cassandra.CosmosRetryPolicy;

public class Util {
    // private static final Logger LOGGER =
    // LoggerFactory.getLogger(Configuration.class);
    private static String PROPERTY_FILE = "application.properties";
    private static Properties prop = null;

    // retry policy setting
    private static final int FIXED_BACK_OFF_TIME = 5000;
    private static final int GROWING_BACK_OFF_TIME = 1000;
    private static final int MAX_RETRY_COUNT = 20;

    private static Session session = null;
    private static String DEFAULT_KEYSTORE_LOCATION = System.getenv("JAVA_HOME") + File.separator + "jre"
            + File.separator + "lib" + File.separator + "security" + File.separator + "cacerts";

    static {
        InputStream input = Util.class.getClassLoader().getResourceAsStream(PROPERTY_FILE);
        if (input == null) {
            throw new RuntimeException("Unable to find configuration file " + PROPERTY_FILE);
        }
        prop = new Properties();
        try {
            prop.load(input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Session getSession() throws Exception {
        if (session == null) {
            try {
                session = _getSession();
            } catch (Exception e) {
                throw e;
            }
        }
        return session;
    }

    private static String DEFAULT_KEYSTORE_PASSWORD = "changeit";
    private static String DEFAULT_COSMOS_READ_DC = "West US";
    private static String DEFAULT_COSMOS_WRITE_DC = "West US";

    private static Session _getSession() throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
            IOException, UnrecoverableKeyException, KeyManagementException {

        final KeyStore keyStore = KeyStore.getInstance("JKS");

        String ssl_keystore_file_config = prop.getProperty("ssl_keystore_file_path");
        String sslKeystoreFile = (ssl_keystore_file_config == null) ? DEFAULT_KEYSTORE_LOCATION
                : ssl_keystore_file_config;

        System.out.println("Keystore location:" + sslKeystoreFile);

        String ssl_keystore_password_config = prop.getProperty("ssl_keystore_password");

        String sslKeyStorePassword = (ssl_keystore_password_config != null && !ssl_keystore_password_config.isEmpty())
                ? ssl_keystore_password_config
                : DEFAULT_KEYSTORE_PASSWORD;

        try (final InputStream is = new FileInputStream(new File(sslKeystoreFile))) {
            keyStore.load(is, sslKeyStorePassword.toCharArray());
        }

        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, sslKeyStorePassword.toCharArray());
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        // Creates a socket factory for HttpsURLConnection using JKS contents.
        final SSLContext sc = SSLContext.getInstance("TLSv1.2");
        sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());

        JdkSSLOptions sslOptions = RemoteEndpointAwareJdkSSLOptions.builder().withSSLContext(sc).build();

        SocketOptions options = new SocketOptions();
        options.setConnectTimeoutMillis(60000);
        options.setReadTimeoutMillis(60000);

        String contactPoints = prop.getProperty("spring.data.cassandra.contact-points");
        int port = Integer.parseInt(prop.getProperty("spring.data.cassandra.port"));
        String cassandraUsername = prop.getProperty("spring.data.cassandra.username");
        String cassandraPassword = prop.getProperty("spring.data.cassandra.password");
        String cassandraKeyspace = prop.getProperty("spring.data.cassandra.keyspace-name");

        System.out.println("Contact Points:" + contactPoints);
        System.out.println("Port:" + port);
        System.out.println("Cassandra username:" + cassandraUsername);
        System.out.println("Cassandra keyspace:" + cassandraKeyspace);

        // set Cosmos Retry Policy
        CosmosRetryPolicy retryPolicy = new CosmosRetryPolicy(MAX_RETRY_COUNT, FIXED_BACK_OFF_TIME,
                GROWING_BACK_OFF_TIME);

        String cosmosReadDC = prop.getProperty("cosmos_retry_read_dc") == null ? DEFAULT_COSMOS_READ_DC
                : prop.getProperty("cosmos_retry_read_dc");

        String cosmosWriteDC = prop.getProperty("cosmos_retry_write_dc") == null ? DEFAULT_COSMOS_WRITE_DC
                : prop.getProperty("cosmos_retry_write_dc");

        System.out.println("Cassandra read DC:" + cosmosReadDC);
        System.out.println("Cassandra write DC:" + cosmosWriteDC);

        // CosmosLoadBalancingPolicy loadBalancingPolicy =
        // CosmosLoadBalancingPolicy.builder().withWriteDC(cosmosWriteDC)
        // .withReadDC(cosmosReadDC).build();

        // CosmosLoadBalancingPolicy loadBalancingPolicy =
        // CosmosLoadBalancingPolicy.builder().withGlobalEndpoint(contactPoints).build();

        Cluster cluster = Cluster.builder().addContactPoints(new String[] { contactPoints }).withPort(port)
                .withCredentials(cassandraUsername, cassandraPassword).withRetryPolicy(retryPolicy)
                // .withLoadBalancingPolicy(loadBalancingPolicy)
                .withSSL(sslOptions).withSocketOptions(options).build();

        // cluster.register(new StatsLogger());

        System.out.println("Connected to cluster: " + cluster.getClusterName());
        return cluster.connect(cassandraKeyspace);
    }

    public static class StatsLogger implements LatencyTracker {

        @Override
        public void update(Host host, Statement statement, Exception exception, long newLatencyNanos) {
            System.out.println("*******Stats START********");
            System.out.println("data center - " + host.getDatacenter());
            System.out.println("address - " + new String(host.getAddress().getHostAddress()));
            System.out.println("query - " + statement.toString());
            System.out.println(
                    "latency (in ms) - " + TimeUnit.MILLISECONDS.convert(newLatencyNanos, TimeUnit.NANOSECONDS));
            System.out.println("*******Stats END********");

        }

        @Override
        public void onRegister(Cluster cluster) {
            // no-op
        }

        @Override
        public void onUnregister(Cluster cluster) {
            // no-op
        }
    }
}