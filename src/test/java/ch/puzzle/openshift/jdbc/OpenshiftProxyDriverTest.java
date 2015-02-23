package ch.puzzle.openshift.jdbc;

import ch.puzzle.openshift.openshift.DatabaseData;
import ch.puzzle.openshift.openshift.OpenshiftCommunicationHandler;
import com.openshift.client.OpenShiftException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OpenshiftProxyDriverTest {

    private static final String BROKER_NAME = "serverUrl";
    private static final String APPLICATION_NAME = "applicationName";
    private static final String NAMESPACE_NAME = "openshiftDomainName";
    private static final String CARTRIDGE_NAME = "cartridgeName";
    private static final String ORIGINAL_JDBC_DRIVER_FULL_CLASS_NAME = "org.postgresql.Driver";
    private static final String OPENSHIFT_USER_NAME = "Username";
    private static final String OPENSHIFT_PASSWORD = "Password";
    private static final String OPENSHIFT_DB_CONNECTION_URL = "postgresql://$OPENSHIFT_POSTGRESQL_DB_HOST:$OPENSHIFT_POSTGRESQL_DB_PORT";

    private Properties properties;
    private String connectionUrl;
    private OpenshiftProxyDriver proxy;

    @Mock
    private OpenshiftCommunicationHandler communicatorMock;
    @Mock
    private ConnectionWrapper connectionProxyMock;

    @Before
    public void setUp() {
        proxy = new OpenshiftProxyDriver();
        proxy.setOpenshiftCommunicator(communicatorMock);
        proxy.setConnectionProxy(connectionProxyMock);

        properties = createProperties(OPENSHIFT_USER_NAME, OPENSHIFT_PASSWORD);
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, BROKER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_JDBC_DRIVER_FULL_CLASS_NAME);

    }

    private Properties createProperties(String openshiftUser, String openshiftPassword) {
        Properties properties = new Properties();
        properties.put(OpenshiftProxyDriver.USER_PROPERTY_KEY, openshiftUser);
        properties.put(OpenshiftProxyDriver.PASSWORD_PROPERTY_KEY, openshiftPassword);
        return properties;
    }

    private String createConnectionUrlWithoutPortForwardParameter(String prefix, String broker, String application, String namespace, String cartridge, String driver) {
        return prefix + broker + "/" + application + "?" + OpenshiftProxyDriver.NAMESPACE_PARAMETER_PREFIX + namespace + OpenshiftProxyDriver.PARAMETER_DELIMITER + OpenshiftProxyDriver.CARTRIDGE_PARAMETER_PREFIX + cartridge + OpenshiftProxyDriver.PARAMETER_DELIMITER + OpenshiftProxyDriver.DRIVER_PARAMETER_PREFIX + driver;
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenUrlIsNull() throws SQLException {
        // given
        connectionUrl = null;

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNotStartingWithUrlPrefix() throws SQLException {
        // given
        String connectionUrl = createConnectionUrlWithoutPortForwardParameter("anyStringOtherThanPrefix", BROKER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxy.connect(connectionUrl, properties);
    }


    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenMissingNamespacePrefixInUrl() throws SQLException {
        // given
        connectionUrl = connectionUrl.replace(OpenshiftProxyDriver.NAMESPACE_PARAMETER_PREFIX, "");

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenMissingCartridgePrefixInUrl() throws SQLException {
        // given
        connectionUrl = connectionUrl.replace(OpenshiftProxyDriver.CARTRIDGE_PARAMETER_PREFIX, "");

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenMissingDriverPrefixInUrl() throws SQLException {
        // given
        connectionUrl = connectionUrl.replace(OpenshiftProxyDriver.DRIVER_PARAMETER_PREFIX, "");

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoBrokerIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, "", APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoApplicationIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, BROKER_NAME, "", NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoNamespaceIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, BROKER_NAME, APPLICATION_NAME, "", CARTRIDGE_NAME, ORIGINAL_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoCartridgeIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, BROKER_NAME, APPLICATION_NAME, NAMESPACE_NAME, "", ORIGINAL_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoDriverIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, BROKER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, "");

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoLocalPortIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, BROKER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoUserPropertyIsSet() throws SQLException {
        // given
        properties = createProperties("", OPENSHIFT_PASSWORD);
        // when
        proxy.connect(connectionUrl, properties);
    }


    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoPasswordPropertyIsSet() throws SQLException {
        // given
        properties = createProperties(OPENSHIFT_USER_NAME, "");
        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenPropertyIsNull() throws SQLException {
        // given
        properties = null;
        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test
    public void parameterValidationOnConnectShouldNotThrowExceptionWhenValidUrl() {
        // given
        mockOpenshiftDatabaseDataResponse();

        // when
        try {
            proxy.connect(connectionUrl, properties);
        } catch (SQLException e) {
            fail("Valid url should not throw exception");
        }
    }

    @Test
    public void onConnectShouldOpenConnectionToOpenshiftBroker() throws SQLException {
        // given
        mockOpenshiftDatabaseDataResponse();

        // when
        proxy.connect(connectionUrl, properties);

        // then
        verify(communicatorMock).connect(BROKER_NAME, OPENSHIFT_USER_NAME, OPENSHIFT_PASSWORD);
    }

    @Test(expected = SQLException.class)
    public void onConnectWhenOpenshiftConnectionErrorOccurrsShouldThrowException() throws SQLException {
        // given
        doThrow(new OpenShiftException("")).when(communicatorMock).connect(BROKER_NAME, OPENSHIFT_USER_NAME, OPENSHIFT_PASSWORD);

        // when
        proxy.connect(connectionUrl, properties);

        // then
        verify(communicatorMock).connect(BROKER_NAME, OPENSHIFT_USER_NAME, OPENSHIFT_PASSWORD);
    }

    @Test
    public void onConnectShouldReadDatabaseDataOnOpenshiftBroker() throws SQLException {
        // given
        mockOpenshiftDatabaseDataResponse();
        // when
        proxy.connect(connectionUrl, properties);

        // then
        verify(communicatorMock).readDatabaseData(APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME);
    }

    @Test
    public void onConnectShouldExtractAndDelegateWithUserAndPasswordProperties() throws SQLException {
        // given
        String dbUser = "dbUser";
        String dbPwd = "dbPwd";
        String dbConnectionUrl = "postgresql://$OPENSHIFT_POSTGRESQL_DB_HOST:$OPENSHIFT_POSTGRESQL_DB_PORT";
        String dbName = "dbName";
        mockOpenshiftDatabaseDataResponse(dbUser, dbPwd, dbConnectionUrl, dbName);

        // when
        proxy.connect(connectionUrl, properties);

        // then
        ArgumentCaptor<Properties> argCapt = ArgumentCaptor.forClass(Properties.class);
        verify(connectionProxyMock).wrap(anyString(), argCapt.capture());

        final Properties properties = argCapt.getAllValues().get(0);
        assertEquals(dbUser, properties.get(OpenshiftProxyDriver.USER_PROPERTY_KEY));
        assertEquals(dbPwd, properties.get(OpenshiftProxyDriver.PASSWORD_PROPERTY_KEY));
    }

    private void mockOpenshiftDatabaseDataResponse(String dbUser, String dbPwd, String dbConnectionUrl, String dbName) {
        DatabaseData databaseTO = new DatabaseData(dbUser, dbPwd, dbConnectionUrl, dbName);
        when(communicatorMock.readDatabaseData(APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME)).thenReturn(databaseTO);
    }


    private void mockOpenshiftDatabaseDataResponse() {
        String dbUser = "dbUser";
        String dbPwd = "dbPwd";
        String dbConnectionUrl = OPENSHIFT_DB_CONNECTION_URL;
        String dbName = "dbName";

        mockOpenshiftDatabaseDataResponse(dbUser, dbPwd, dbConnectionUrl, dbName);
    }

    @Test(expected = SQLException.class)
    public void onConnectShouldThrowExceptionWhenReceiveInvalidConnectionUrl() throws SQLException {
        // given
        String dbUser = "dbUser";
        String dbPwd = "dbPwd";
        String protocol = "protocol";
        String dbConnectionUrl = "invalidConnectionUrl";
        String dbName = "dbName";
        mockOpenshiftDatabaseDataResponse(dbUser, dbPwd, dbConnectionUrl, dbName);

        // when
        proxy.connect(connectionUrl, properties);
    }


    @Test
    public void onConnectShouldExtractAndDelegateWithConnectionUrlUsingProtocol() throws SQLException {
        // given
        String dbUser = "dbUser";
        String dbPwd = "dbPwd";
        String protocol = "protocol";
        String dbConnectionUrl = protocol + "://anyHostAndPort";
        String dbName = "dbName";
        mockOpenshiftDatabaseDataResponse(dbUser, dbPwd, dbConnectionUrl, dbName);
        // when
        proxy.connect(connectionUrl, properties);

        // then
        verify(connectionProxyMock).wrap(startsWith("jdbc:" + protocol), any(Properties.class));

    }

    @Test
    public void onConnectShouldExtractAndDelegateWithConnectionUrlUsingLocalhost() throws SQLException {
        // given
        String dbUser = "dbUser";
        String dbPwd = "dbPwd";
        String dbConnectionUrl = "postgresql://$OPENSHIFT_POSTGRESQL_DB_HOST:$OPENSHIFT_POSTGRESQL_DB_PORT";
        String dbName = "dbName";
        mockOpenshiftDatabaseDataResponse(dbUser, dbPwd, dbConnectionUrl, dbName);
        // when
        proxy.connect(connectionUrl, properties);

        // then
        verify(connectionProxyMock).wrap(contains("localhost"), any(Properties.class));
    }

    @Test
    public void onConnectShouldStartPortforwardingWhenParameterIsNotSet() throws SQLException {
        // given
        mockOpenshiftDatabaseDataResponse();

        // when
        proxy.connect(connectionUrl, properties);

        // then
        verify(communicatorMock).startPortForwarding(APPLICATION_NAME, NAMESPACE_NAME, OPENSHIFT_DB_CONNECTION_URL);
    }

    @Test
    public void onConnectShouldNotStartPortForwardingWhenParameterIsSet() throws SQLException {
        // given

        mockOpenshiftDatabaseDataResponse();
        connectionUrl += OpenshiftProxyDriver.PARAMETER_DELIMITER + OpenshiftProxyDriver.FORWARDED_PORT_PARAMETER_PREFIX + "9999";

        // when
        proxy.connect(connectionUrl, properties);

        // then
        verify(communicatorMock, never()).startPortForwarding(APPLICATION_NAME, NAMESPACE_NAME, OPENSHIFT_DB_CONNECTION_URL);
    }

    @Test
    public void onConnectShouldNotStartPortForwardingWhenParameterIsSet2() throws SQLException {
        // given
        int forwardedPort = 9999;
        mockOpenshiftDatabaseDataResponse();
        connectionUrl += OpenshiftProxyDriver.PARAMETER_DELIMITER + OpenshiftProxyDriver.FORWARDED_PORT_PARAMETER_PREFIX + forwardedPort;

        // when
        proxy.connect(connectionUrl, properties);

        // then
        verify(connectionProxyMock).wrap(contains(String.valueOf(forwardedPort)), any(Properties.class));
    }

    @Test
    public void onConnectShouldExtractAndDelegateWithConnectionUrlUsingPort() throws SQLException {
        // given
        String dbUser = "dbUser";
        String dbPwd = "dbPwd";
        String dbConnectionUrl = "postgresql://$OPENSHIFT_POSTGRESQL_DB_HOST:$OPENSHIFT_POSTGRESQL_DB_PORT";
        String dbName = "dbName";
        int forwardedPort = 9999;
        mockOpenshiftDatabaseDataResponse(dbUser, dbPwd, dbConnectionUrl, dbName);
        when(communicatorMock.startPortForwarding(APPLICATION_NAME, NAMESPACE_NAME, OPENSHIFT_DB_CONNECTION_URL)).thenReturn(forwardedPort);

        // when
        proxy.connect(connectionUrl, properties);

        // then
        verify(connectionProxyMock).wrap(contains(String.valueOf(forwardedPort)), any(Properties.class));
    }

    @Test
    public void onConnectShouldExtractAndDelegateWithConnectionUrlUsingDbName() throws SQLException {
        // given
        String dbUser = "dbUser";
        String dbPwd = "dbPwd";
        String dbConnectionUrl = "postgresql://$OPENSHIFT_POSTGRESQL_DB_HOST:$OPENSHIFT_POSTGRESQL_DB_PORT";
        String dbName = "dbName";
        mockOpenshiftDatabaseDataResponse(dbUser, dbPwd, dbConnectionUrl, dbName);
        // when
        proxy.connect(connectionUrl, properties);

        // then
        verify(connectionProxyMock).wrap(contains(dbName), any(Properties.class));
    }

    @Test
    public void onCloseShouldDisconnectCommunicator() throws SQLException {
        // when
        proxy.close();

        // then
        verify(communicatorMock).disconnect();
    }

}