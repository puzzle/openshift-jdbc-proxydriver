/*
 * Copyright 2015 Puzzle ITC GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.puzzle.openshift.jdbc;

import ch.puzzle.openshift.openshift.DatabaseData;
import ch.puzzle.openshift.openshift.OpenshiftCommunicationHandler;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by bschwaller on 11.02.15.
 */
public class OpenshiftProxyDriver implements Driver {
    static final String URL_PROTOCOL_HOST_DELIMITER = "://";

    static final String USER_PROPERTY_KEY = "user";
    static final String PASSWORD_PROPERTY_KEY = "password";
    static final String SSH_PRIVATE_KEY_PROPERTY_KEY = "privateSshKeyFilePath";

    static final int MAJOR_VERSION = 1;
    static final int MINOR_VERSION = 0;

    private Logger logger = Logger.getLogger(OpenshiftProxyDriver.class.getName());

    private OpenshiftCommunicationHandler communicator;
    private ConnectionWrapper connectionProxy;

    static {
        registerDriver();
    }

    public OpenshiftProxyDriver() {
        this.communicator = new OpenshiftCommunicationHandler();
        this.connectionProxy = new ConnectionWrapper(this);
    }


    /**
     * TODO javadoc
     *
     * @see Driver#connect(String, java.util.Properties)
     */
    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        logger.info("proxy connection request to " + url);

        if (!acceptsURL(url)) {
            logger.fine("This driver is the wrong kind of driver to connect to the given URL " + url);
            return null;
        }

        try {
            verifyUserPasswordProperties(info);

            ProxyDriverURLParameter proxyDriverURLParameter = ProxyDriverURLParameter.createValid(url);
            final DatabaseData databaseData = connectToOpenshiftAndGetDatabaseData(proxyDriverURLParameter, info);

            int port;

            if (proxyDriverURLParameter.hasExternalForwardedPort()) {
                logger.info("Use external portforwarding on port " + proxyDriverURLParameter.getExternalForwardedPort());
                port = proxyDriverURLParameter.getExternalForwardedPort();
            } else {
                logger.info("Start port forwarding");
                port = communicator.startPortForwarding(proxyDriverURLParameter.getApplication(), proxyDriverURLParameter.getDomain(), databaseData.getConnectionUrl(), info.getProperty(SSH_PRIVATE_KEY_PROPERTY_KEY));
            }

            String connectionUrl = createConnectionUrl(databaseData, port);
            Properties targetDriverPropertiesInfo = createTargetDriverPropertiesInfo(info, databaseData);

            return connectToDriver(connectionUrl, targetDriverPropertiesInfo);
        } catch (RuntimeException e) {
            throw new SQLException("Error occurred while communicating with openshift. Reason: " + e.getMessage(), e);
        }

    }

    private Properties createTargetDriverPropertiesInfo(Properties allPropertyInfos, DatabaseData databaseData) {
        removeProxyDriverSpecificProperties(allPropertyInfos);
        addTargetDriverUserPasswordProperties(allPropertyInfos, databaseData.getDbUser(), databaseData.getDbUserPassword());

        return allPropertyInfos;
    }

    private void removeProxyDriverSpecificProperties(Properties allPropertyInfos) {
        allPropertyInfos.remove(USER_PROPERTY_KEY);
        allPropertyInfos.remove(PASSWORD_PROPERTY_KEY);
        allPropertyInfos.remove(SSH_PRIVATE_KEY_PROPERTY_KEY);
    }

    private DatabaseData connectToOpenshiftAndGetDatabaseData(ProxyDriverURLParameter proxyDriverURLParameter, Properties info) {
        try {
            logger.fine("Connect to openshift server");
            communicator.connect(proxyDriverURLParameter.getServer(), info.getProperty(USER_PROPERTY_KEY), info.getProperty(PASSWORD_PROPERTY_KEY));
            logger.fine("Read database data from cartridge");
            return communicator.readDatabaseData(proxyDriverURLParameter.getApplication(), proxyDriverURLParameter.getDomain(), proxyDriverURLParameter.getCartridge());
        } catch (Exception e) {
            logger.warning("Could not connect to openshift and read database data! Reason: " + e.getMessage());
            throw new RuntimeException("Error connecting and reading database data from openshift server", e);
        }
    }

    private void addTargetDriverUserPasswordProperties(Properties proxyDriverProperties, String dbUser, String dbUserPassword) {
        proxyDriverProperties.put(USER_PROPERTY_KEY, dbUser);
        proxyDriverProperties.put(PASSWORD_PROPERTY_KEY, dbUserPassword);
    }


    /**
     * Close driver is a callback method to clean up open connections to openshift.
     * This method is ment to be called by sql connection close.
     */
    public void close() {
        logger.info("Disconnect openshift communicator");
        communicator.disconnect();
    }

    private Properties verifyUserPasswordProperties(Properties info) throws SQLException {

        if (info != null) {
            String user = info.getProperty(USER_PROPERTY_KEY);
            String password = info.getProperty(PASSWORD_PROPERTY_KEY);

            if (user != null && !user.isEmpty()
                    && password != null && !password.isEmpty()) {
                return info;
            }
        }
        throw new SQLException("Invalid user properties! At least user and password must be set!");
    }

    private Connection connectToDriver(String url, Properties info) throws SQLException {
        try {
            final Connection wrappedConnection = connectionProxy.wrap(url, info);
            logger.info("Successfully established connection to " + url);
            return wrappedConnection;
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        }
    }


    private String createConnectionUrl(DatabaseData connectionData, int forwardedPort) throws SQLException {
        String connectionUrl = connectionData.getConnectionUrl();
        final String[] protocolHost = connectionUrl.split(URL_PROTOCOL_HOST_DELIMITER);

        if (protocolHost.length < 2) {
            throw new SQLException("Error extracting connection url");
        } else {
            String protocol = protocolHost[0];
            return "jdbc:" + protocol + URL_PROTOCOL_HOST_DELIMITER + "localhost:" + forwardedPort + "/" + connectionData.getDatabaseName();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return ProxyDriverURLParameter.acceptProxyDriverProtocol(url);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties properties) throws SQLException {

        List<DriverPropertyInfo> driverPropertyInfos = new ArrayList<>();

        DriverPropertyInfo driverpropertyinfo = new DriverPropertyInfo(USER_PROPERTY_KEY, properties.getProperty(USER_PROPERTY_KEY));
        driverpropertyinfo.description = "Openshift user";
        driverpropertyinfo.required = true;
        driverPropertyInfos.add(driverpropertyinfo);

        driverpropertyinfo = new DriverPropertyInfo(PASSWORD_PROPERTY_KEY, properties.getProperty(PASSWORD_PROPERTY_KEY));
        driverpropertyinfo.description = "Openshift password";
        driverpropertyinfo.required = true;
        driverPropertyInfos.add(driverpropertyinfo);

        driverpropertyinfo = new DriverPropertyInfo(SSH_PRIVATE_KEY_PROPERTY_KEY, properties.getProperty(SSH_PRIVATE_KEY_PROPERTY_KEY));
        driverpropertyinfo.description = "Absolute file path of private ssh key";
        driverPropertyInfos.add(driverpropertyinfo);

        return driverPropertyInfos.toArray(new DriverPropertyInfo[driverPropertyInfos.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMajorVersion() {
        return MAJOR_VERSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMinorVersion() {
        return MINOR_VERSION;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    void setOpenshiftCommunicator(OpenshiftCommunicationHandler communicator) {
        this.communicator = Objects.requireNonNull(communicator, "OpenshiftCommunicator must not be null");
    }

    void setConnectionProxy(ConnectionWrapper connectionProxy) {
        this.connectionProxy = connectionProxy;
    }


    private static void registerDriver() {
        try {
            DriverManager.registerDriver(new OpenshiftProxyDriver());
        } catch (SQLException exception) {
            throw new RuntimeException("Error registering driver", exception);
        }
    }

}
