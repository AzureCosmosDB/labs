package com.azure.cosmosdb.cassandra;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.JdkSSLOptions;
import com.datastax.driver.core.RemoteEndpointAwareJdkSSLOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;

public class Util {
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    private static String PROPERTY_FILE = "config.properties";
    private static Properties prop = null;

    private static Session session = null;
    private static String DEFAULT_KEYSTORE_PASSWORD = "changeit";

    private static String DEFAULT_KEYSTORE_LOCATION = System.getenv("JAVA_HOME") + File.separator + "jre"
            + File.separator + "lib" + File.separator + "security" + File.separator + "cacerts";

    static Invocation.Builder builder = null;

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

    public static Invocation.Builder getPowerBIRestClient() {
        String powerBIUrl = prop.getProperty("power_bi_url");

        if (builder == null) {
            ClientConfig config = new ClientConfig();
            config.register(JacksonJsonProvider.class);

            builder = ClientBuilder.newClient(config).target(powerBIUrl).request().header("Content-Type",
                    "application/json");
        }
        return builder;
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

    private static Session _getSession() throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
            IOException, UnrecoverableKeyException, KeyManagementException {

        final KeyStore keyStore = KeyStore.getInstance("JKS");

        String ssl_keystore_file_config = prop.getProperty("ssl_keystore_file_path");
        String sslKeystoreFile = (ssl_keystore_file_config == null) ? DEFAULT_KEYSTORE_LOCATION
                : ssl_keystore_file_config;

        LOGGER.info("Keystore location: {}", sslKeystoreFile);

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

        final SSLContext sc = SSLContext.getInstance("TLSv1.2");
        sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());

        JdkSSLOptions sslOptions = RemoteEndpointAwareJdkSSLOptions.builder().withSSLContext(sc).build();

        SocketOptions options = new SocketOptions();
        options.setConnectTimeoutMillis(60000);
        options.setReadTimeoutMillis(60000);

        String contactPoints = prop.getProperty("cassandra_host");
        int port = Integer.parseInt(prop.getProperty("cassandra_port"));
        String cassandraUsername = prop.getProperty("cassandra_username");
        String cassandraPassword = prop.getProperty("cassandra_password");

        LOGGER.info("Contact Points: {}", contactPoints);
        LOGGER.info("Port: {}", port);
        LOGGER.info("Cassandra username: {}", cassandraUsername);

        Cluster cluster = Cluster.builder().addContactPoints(new String[] { contactPoints }).withPort(port)
                .withCredentials(cassandraUsername, cassandraPassword).withSSL(sslOptions).withSocketOptions(options)
                .build();

        Session s = cluster.connect();
        LOGGER.info("Connected to cluster: {}", cluster.getClusterName());
        return s;
    }
}