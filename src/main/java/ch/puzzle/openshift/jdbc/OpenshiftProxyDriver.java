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

    private Logger logger = Logger.getLogger(OpenshiftProxyDriver.class.getName());

    private String openshiftUser;
    private String openshiftPassword;
    private String privateSshKeyFilePath;
    private String openshiftServer;
    private String applicationName;
    private String domain;
    private String cardridge;
    private String driver;
    private Integer externalForwardedPort;

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
        verifyUrlAndExtractUrlParameters(url);
        verifyAndExtractProperties(info);

        try {
            communicator.connect(openshiftServer, openshiftUser, openshiftPassword);
            final DatabaseData databaseData = communicator.readDatabaseData(applicationName, domain, cardridge);
            int port;
            if (!hasForwardedPortParameter()) {
                logger.info("Start port forwarding");
                port = communicator.startPortForwarding(applicationName, domain, databaseData.getConnectionUrl(), privateSshKeyFilePath);
            } else {
                logger.info("Use external portforwarding on port " + externalForwardedPort);
                port = externalForwardedPort;
            }

            return connectToDriver(port, databaseData);
        } catch (RuntimeException e) {
            throw new SQLException("Error occurred while communicating with openshift. Reason: " + e.getMessage(), e);
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

    private boolean hasForwardedPortParameter() {
        return externalForwardedPort != null;
    }


    private void verifyUrlAndExtractUrlParameters(String url) throws SQLException {
        if (acceptsURL(url)) {
            readParameterFromUrl(url);
            verifyThatAllParametersAreSet();
        } else {
            throw new SQLException("Invalid URL " + url);
        }
    }

    private void verifyAndExtractProperties(Properties info) throws SQLException {
        if (info == null) {
            throw new SQLException("No properties set. At least user and password must be set!");
        }

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

    private Connection connectToDriver(int forwardedPort, DatabaseData connectionData) throws SQLException {
        try {
            Class.forName(driver);

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


    private void readParameterFromUrl(String url) {
        final String urlWithoutProtocol = url.substring(URL_PREFIX.length());
        final String[] serverWithAppAndParameters = urlWithoutProtocol.split("\\?");

        if (serverWithAppAndParameters.length == 2) {

            String serverWithApp = serverWithAppAndParameters[0];
            String urlParameter = serverWithAppAndParameters[1];

            extractAndSetServerAndApp(serverWithApp);
            extractAndSetUrlParameter(urlParameter);
        }
    }

    private void extractAndSetServerAndApp(String serverWithApp) {
        final String[] serverAndApp = serverWithApp.split("/");
        if (serverAndApp.length == 2) {
            openshiftServer = serverAndApp[0];
            applicationName = serverAndApp[1];
        }
    }

    private void extractAndSetUrlParameter(String urlParameter) {
        for (String parameterValues : urlParameter.split(PARAMETER_DELIMITER)) {
            if (parameterValues.startsWith(DOMAIN_PARAMETER_PREFIX)) {
                domain = parameterValues.substring(DOMAIN_PARAMETER_PREFIX.length());
            }
            if (parameterValues.startsWith(CARTRIDGE_PARAMETER_PREFIX)) {
                cardridge = parameterValues.substring(CARTRIDGE_PARAMETER_PREFIX.length());
            }
            if (parameterValues.startsWith(DRIVER_PARAMETER_PREFIX)) {
                driver = parameterValues.substring(DRIVER_PARAMETER_PREFIX.length());
            }
            if (parameterValues.startsWith(FORWARDED_PORT_PARAMETER_PREFIX)) {
                externalForwardedPort = Integer.valueOf(parameterValues.substring(FORWARDED_PORT_PARAMETER_PREFIX.length()));
            }
        }
    }

    private void verifyThatAllParametersAreSet() throws SQLException {
        StringBuilder sb = new StringBuilder("Missing parameter in URL for");
        boolean hasMissingParameter = false;

        if (!isParameterValid(openshiftServer)) {
            sb.append(" openshiftServerUrl");
            hasMissingParameter = true;
        }
        if (!isParameterValid(applicationName)) {
            sb.append(" applicationName");
            hasMissingParameter = true;
        }
        if (!isParameterValid(domain)) {
            sb.append(" ").append(DOMAIN_PARAMETER_PREFIX);
            hasMissingParameter = true;
        }
        if (!isParameterValid(cardridge)) {
            sb.append(" ").append(CARTRIDGE_PARAMETER_PREFIX);
            hasMissingParameter = true;
        }
        if (!isParameterValid(driver)) {
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
        // TODO  should test url prefix for returning result and if the requested properties are not set then a exception will be thrown???
        return url != null
                && url.regionMatches(true, 0, URL_PREFIX, 0, URL_PREFIX.length())
                // TODO ab hier in verify auslagern!
                && url.contains(DOMAIN_PARAMETER_PREFIX)
                && url.contains(CARTRIDGE_PARAMETER_PREFIX)
                && url.contains(DRIVER_PARAMETER_PREFIX);
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
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMinorVersion() {
        return 0;
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
}
