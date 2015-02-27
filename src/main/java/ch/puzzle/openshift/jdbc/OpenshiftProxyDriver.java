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
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by bschwaller on 11.02.15.
 */
public class OpenshiftProxyDriver implements Driver {
    static final String URL_PREFIX = "jdbc:openshiftproxy://";
    static final String URL_PROTOCOL_HOST_DELIMITER = "://";
    static final String PARAMETER_DELIMITER = "&";
    static final String DOMAIN_PARAMETER_PREFIX = "domain=";
    static final String CARTRIDGE_PARAMETER_PREFIX = "cartridge=";
    static final String DRIVER_PARAMETER_PREFIX = "driver=";
    static final String FORWARDED_PORT_PARAMETER_PREFIX = "externalforwardedport=";

    static final String USER_PROPERTY_KEY = "user";
    static final String PASSWORD_PROPERTY_KEY = "password";
    static final String SSH_PRIVATE_KEY_PROPERTY_KEY = "privateSshKeyFilePath";

    static final String SERVER = "openshiftServerKey";
    static final String APPLICATION = "applicationKey";
    static final String DOMAIN = "domainKey";
    static final String CARTRIDGE = "cartridgeKey";
    static final String DRIVER = "driverKey";
    static final String EXTERNAL_FORWARDED_PORT = "externalForwardedPortKey";

    static final int MAJOR_VERSION = 1;
    static final int MINOR_VERSION = 0;

    private Logger logger = Logger.getLogger(OpenshiftProxyDriver.class.getName());

    private String openshiftUser;
    private String openshiftPassword;
    private String privateSshKeyFilePath;

    private OpenshiftCommunicationHandler communicator;
    private ConnectionWrapper connectionProxy;

    static {
        registerDriver();
    }

    // TODO implement logging


    public OpenshiftProxyDriver() {
        this.communicator = new OpenshiftCommunicationHandler();
        this.connectionProxy = new ConnectionWrapper(this);
    }


    /**
     * TODO
     *
     * @see com.mysql.jdbc.Driver#connect(String, java.util.Properties)
     */
    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        logger.info("proxy connection request to " + url);
        Properties parameter = extractProxyDriverParametersFromUrl(url);
        verifyAndExtractProperties(info);

        try {
            communicator.connect(parameter.getProperty(SERVER), openshiftUser, openshiftPassword);
            final DatabaseData databaseData = communicator.readDatabaseData(parameter.getProperty(APPLICATION), parameter.getProperty(DOMAIN), parameter.getProperty(CARTRIDGE));
            int port;
            if (!parameter.containsKey(EXTERNAL_FORWARDED_PORT)) {
                logger.info("Start port forwarding");
                port = communicator.startPortForwarding(parameter.getProperty(APPLICATION), parameter.getProperty(DOMAIN), databaseData.getConnectionUrl(), privateSshKeyFilePath);
            } else {
                logger.info("Use external portforwarding on port " + parameter.getProperty(EXTERNAL_FORWARDED_PORT));
                port = getIntValueOfExternalForwardedPort(parameter);
            }

            return connectToDriver(port, databaseData, parameter.getProperty(DRIVER));
        } catch (RuntimeException e) {
            throw new SQLException("Error occurred while communicating with openshift. Reason: " + e.getMessage(), e);
        }
    }

    private Properties extractProxyDriverParametersFromUrl(String url) throws SQLException {
        if (acceptsURL(url)) {

            final String urlWithoutProtocol = url.substring(URL_PREFIX.length());
            final String[] serverWithAppAndArguments = urlWithoutProtocol.split("\\?");

            if (serverWithAppAndArguments.length == 2) {

                String serverWithApp = serverWithAppAndArguments[0];
                String urlParameter = serverWithAppAndArguments[1];

                return extractAndVerifyParameters(serverWithApp, urlParameter);
            }
        }
        throw new SQLException("Invalid URL " + url);
    }

    private int getIntValueOfExternalForwardedPort(Properties parameter) throws SQLException {
        final String port = parameter.getProperty(EXTERNAL_FORWARDED_PORT);
        try {
            return Integer.valueOf(port);
        } catch (NumberFormatException e) {
            throw new SQLException("Invalid port number " + port);
        }
    }

    /**
     * Close driver is a callback method to clean up open connections to openshift.
     * This method is ment to be called by sql connection close.
     */
    public void close() {
        logger.info("Disconnect openshift communicator");
        communicator.disconnect();
    }

    private void verifyAndExtractProperties(Properties info) throws SQLException {
        if (info == null) {
            throw new SQLException("No properties set. At least user and password must be set!");
        }
        // TODO pass other arguments to driver

        openshiftUser = info.getProperty(USER_PROPERTY_KEY);
        openshiftPassword = info.getProperty(PASSWORD_PROPERTY_KEY);
        if (info.containsKey(SSH_PRIVATE_KEY_PROPERTY_KEY)) {
            privateSshKeyFilePath = info.getProperty(SSH_PRIVATE_KEY_PROPERTY_KEY);
        } else {
            privateSshKeyFilePath = null;
        }

        if (openshiftUser == null || openshiftUser.isEmpty()
                || openshiftPassword == null || openshiftPassword.isEmpty()) {
            throw new SQLException("Invalid properties set! At least user and password must be set!");
        }
    }

    private Connection connectToDriver(int forwardedPort, DatabaseData connectionData, String driverClassName) throws SQLException {
        try {
            Class.forName(driverClassName);

            String url = createConnectionUrl(connectionData, forwardedPort);
            Properties userPassword = createProperties(connectionData.getDbUser(), connectionData.getDbUserPassword());
            return connectionProxy.wrap(url, userPassword);
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

    private Properties createProperties(String dbUser, String dbUserPassword) {
        Properties properties = new Properties();

        properties.setProperty(USER_PROPERTY_KEY, dbUser);
        properties.setProperty(PASSWORD_PROPERTY_KEY, dbUserPassword);

        return properties;
    }


    private Properties extractAndVerifyParameters(String serverWithApp, String urlParameter) throws SQLException {
        Properties properties = new Properties();

        final String[] serverAndApp = serverWithApp.split("/");
        if (serverAndApp.length == 2) {
            properties.put(SERVER, serverAndApp[0]);
            properties.put(APPLICATION, serverAndApp[1]);
        }
        for (String parameterValues : urlParameter.split(PARAMETER_DELIMITER)) {

            if (parameterValues.startsWith(DOMAIN_PARAMETER_PREFIX)) {
                properties.put(DOMAIN, parameterValues.substring(DOMAIN_PARAMETER_PREFIX.length()));
            }
            if (parameterValues.startsWith(CARTRIDGE_PARAMETER_PREFIX)) {
                properties.put(CARTRIDGE, parameterValues.substring(CARTRIDGE_PARAMETER_PREFIX.length()));
            }
            if (parameterValues.startsWith(DRIVER_PARAMETER_PREFIX)) {
                properties.put(DRIVER, parameterValues.substring(DRIVER_PARAMETER_PREFIX.length()));
            }
            if (parameterValues.startsWith(FORWARDED_PORT_PARAMETER_PREFIX)) {
                properties.put(EXTERNAL_FORWARDED_PORT, parameterValues.substring(FORWARDED_PORT_PARAMETER_PREFIX.length()));
            }
        }

        verifyMandatoryParameters(properties);
        return properties;
    }

    private void verifyMandatoryParameters(Properties properties) throws SQLException {
        StringBuilder sb = new StringBuilder("Missing mandatory parameter in URL for");
        boolean hasMissingParameter = false;

        if (!properties.containsKey(SERVER)) {
            sb.append(" ").append("openshiftServerUrl");
            hasMissingParameter = true;
        }
        if (!properties.containsKey(APPLICATION)) {
            sb.append(" ").append("applicationName");
            hasMissingParameter = true;
        }
        if (!properties.containsKey(DOMAIN)) {
            sb.append(" ").append(DOMAIN_PARAMETER_PREFIX);
            hasMissingParameter = true;
        }
        if (!properties.containsKey(CARTRIDGE)) {
            sb.append(" ").append(CARTRIDGE_PARAMETER_PREFIX);
            hasMissingParameter = true;
        }
        if (!properties.containsKey(DRIVER)) {
            sb.append(" ").append(DRIVER_PARAMETER_PREFIX);
            hasMissingParameter = true;
        }
        if (hasMissingParameter) {
            throw new SQLException(sb.toString());
        }
    }

    private boolean isParameterValid(String parameter) {
        return parameter != null && !parameter.isEmpty();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url != null && url.regionMatches(true, 0, URL_PREFIX, 0, URL_PREFIX.length());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties properties) throws SQLException {
        DriverPropertyInfo driverPropertyInfos[] = new DriverPropertyInfo[3];
        DriverPropertyInfo driverpropertyinfo = new DriverPropertyInfo(USER_PROPERTY_KEY, null);
        driverpropertyinfo.value = properties.getProperty(USER_PROPERTY_KEY);
        driverpropertyinfo.description = "Openshift user";
        driverpropertyinfo.required = true;
        driverPropertyInfos[0] = driverpropertyinfo;

        driverpropertyinfo = new DriverPropertyInfo(PASSWORD_PROPERTY_KEY, null);
        driverpropertyinfo.value = properties.getProperty(PASSWORD_PROPERTY_KEY);
        driverpropertyinfo.description = "Openshift password";
        driverpropertyinfo.required = true;
        driverPropertyInfos[1] = driverpropertyinfo;

        driverpropertyinfo = new DriverPropertyInfo(SSH_PRIVATE_KEY_PROPERTY_KEY, null);
        driverpropertyinfo.value = properties.getProperty(SSH_PRIVATE_KEY_PROPERTY_KEY);
        driverpropertyinfo.description = "Absolute file path of private ssh key";
        driverpropertyinfo.required = false;
        driverPropertyInfos[2] = driverpropertyinfo;
        return driverPropertyInfos;
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
        // TODO what?
        return false;
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        // TODO
        return logger;
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

    protected java.sql.Driver getDriverByClassName(String className) {
        try {
            Class<?> c = Class.forName(className);
            Object o = c.newInstance();
            if (o instanceof java.sql.Driver) {
                return (java.sql.Driver) o;
            } else {
                throw new RuntimeException("JDBCMetrics could cast " + className + " to java.sql.Driver");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("JDBCMetrics could not find driver class", e);
        } catch (InstantiationException e) {
            throw new RuntimeException("JDBCMetrics could not instantiate driver class", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("JDBCMetrics could not instantiate driver class", e);
        }
    }
}
