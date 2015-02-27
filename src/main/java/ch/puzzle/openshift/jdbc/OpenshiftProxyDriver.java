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
     * TODO javadoc
     *
     * @see com.mysql.jdbc.Driver#connect(String, java.util.Properties)
     */
    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        logger.info("proxy connection request to " + url);
        Properties parameter = extractAndValidateProxyDriverParametersFromUrl(url);
        verifyUserProperties(info);

        try {
            communicator.connect(parameter.getProperty(SERVER), info.getProperty(USER_PROPERTY_KEY), info.getProperty(PASSWORD_PROPERTY_KEY));
            final DatabaseData databaseData = communicator.readDatabaseData(parameter.getProperty(APPLICATION), parameter.getProperty(DOMAIN), parameter.getProperty(CARTRIDGE));
            int port;
            if (!parameter.containsKey(EXTERNAL_FORWARDED_PORT)) {
                logger.info("Start port forwarding");
                port = communicator.startPortForwarding(parameter.getProperty(APPLICATION), parameter.getProperty(DOMAIN), databaseData.getConnectionUrl(), info.getProperty(SSH_PRIVATE_KEY_PROPERTY_KEY));
            } else {
                logger.info("Use external portforwarding on port " + parameter.getProperty(EXTERNAL_FORWARDED_PORT));
                port = getIntValueOfExternalForwardedPort(parameter);
            }

            String connectionUrl = createConnectionUrl(databaseData, port);
            Properties driverConnectionProperties = replaceUserPasswordProperties(info, databaseData.getDbUser(), databaseData.getDbUserPassword());

            return connectToDriver(parameter.getProperty(DRIVER), connectionUrl, driverConnectionProperties);
        } catch (RuntimeException e) {
            throw new SQLException("Error occurred while communicating with openshift. Reason: " + e.getMessage(), e);
        }
    }

    private Properties replaceUserPasswordProperties(Properties proxyDriverProperties, String dbUser, String dbUserPassword) throws SQLException {
        proxyDriverProperties.remove(USER_PROPERTY_KEY);
        proxyDriverProperties.remove(PASSWORD_PROPERTY_KEY);

        proxyDriverProperties.put(USER_PROPERTY_KEY, dbUser);
        proxyDriverProperties.put(PASSWORD_PROPERTY_KEY, dbUserPassword);

        return verifyUserProperties(proxyDriverProperties);
    }

    private Properties extractAndValidateProxyDriverParametersFromUrl(String url) throws SQLException {
        if (acceptsURL(url)) {

            final String urlWithoutProtocol = url.substring(URL_PREFIX.length());
            final String[] serverWithAppAndArguments = urlWithoutProtocol.split("\\?");

            if (serverWithAppAndArguments.length == 2) {

                String serverWithApp = serverWithAppAndArguments[0];
                String urlParameter = serverWithAppAndArguments[1];

                return extractAndValidateParameters(serverWithApp, urlParameter);
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

    private Properties verifyUserProperties(Properties info) throws SQLException {

        if (info != null) {
            String openshiftUser = info.getProperty(USER_PROPERTY_KEY);
            String openshiftPassword = info.getProperty(PASSWORD_PROPERTY_KEY);

            if (openshiftUser != null && !openshiftUser.isEmpty()
                    && openshiftPassword != null && !openshiftPassword.isEmpty()) {
                return info;
            }
        }
        throw new SQLException("Invalid user properties! At least user and password must be set!");
    }

    private Connection connectToDriver(String driverClassName, String url, Properties info) throws SQLException {
        try {
            getDriverByClassName(driverClassName);
            return connectionProxy.wrap(url, info);
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

    private Properties extractAndValidateParameters(String serverWithApp, String urlParameter) throws SQLException {
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

        verifyExistenceOfMandatoryParameters(properties);
        return properties;
    }

    private void verifyExistenceOfMandatoryParameters(Properties properties) throws SQLException {
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

        driverPropertyInfos.addAll(getTargetDriverPropertiesWithoutUserAndPassword(url, properties));

        return driverPropertyInfos.toArray(new DriverPropertyInfo[driverPropertyInfos.size()]);
    }

    private List<DriverPropertyInfo> getTargetDriverPropertiesWithoutUserAndPassword(String url, Properties properties) throws SQLException {
        List<DriverPropertyInfo> driverPropertiesWithoutUserPassword = new ArrayList<>();

        Properties parameter = extractAndValidateProxyDriverParametersFromUrl(url);
        Driver driver = getDriverByClassName(parameter.getProperty(DRIVER));

        if (driver != null) {
            for (DriverPropertyInfo driverpropertyinfo : driver.getPropertyInfo(url, properties)) {
                if (!isUserOrPasswordProperty(driverpropertyinfo)) {
                    driverPropertiesWithoutUserPassword.add(driverpropertyinfo);
                }
            }
        }

        return driverPropertiesWithoutUserPassword;
    }

    private boolean isUserOrPasswordProperty(DriverPropertyInfo driverpropertyinfo) {
        return USER_PROPERTY_KEY.equals(driverpropertyinfo.name) || PASSWORD_PROPERTY_KEY.equals(driverpropertyinfo.name);
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

    private java.sql.Driver getDriverByClassName(String className) {
        try {
            Class<?> c = Class.forName(className);
            Object o = c.newInstance();
            if (o instanceof java.sql.Driver) {
                return (java.sql.Driver) o;
            } else {
                throw new RuntimeException("Could not cast " + className + " to java.sql.Driver");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find driver class", e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Could not instantiate driver class", e);
        }
    }
}
