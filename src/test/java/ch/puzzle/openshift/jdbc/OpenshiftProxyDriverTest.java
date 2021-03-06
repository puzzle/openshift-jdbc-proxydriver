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

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OpenshiftProxyDriverTest {

    private static final String OPENSHIFT_SERVER_NAME = "serverUrl";
    private static final String APPLICATION_NAME = "applicationName";
    private static final String DOMAIN_NAME = "openshiftDomainName";
    private static final String CARTRIDGE_NAME = "cartridgeName";
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
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);

    }

    private Properties createProperties(String openshiftUser, String openshiftPassword) {
        Properties properties = new Properties();
        properties.put(OpenshiftProxyDriver.USER_PROPERTY_KEY, openshiftUser);
        properties.put(OpenshiftProxyDriver.PASSWORD_PROPERTY_KEY, openshiftPassword);
        return properties;
    }

    private String createConnectionUrlWithoutPortForwardParameter(String prefix, String server, String application, String domain, String cartridge) {
        return prefix + server + "/" + application + "?" + ProxyDriverURLParameter.DOMAIN_PARAMETER_PREFIX + domain + ProxyDriverURLParameter.PARAMETER_DELIMITER + ProxyDriverURLParameter.CARTRIDGE_PARAMETER_PREFIX + cartridge;
    }

    @Test
    public void onConnectShouldReturnNullWhenUrlIsNull() throws SQLException {
        // given
        connectionUrl = null;

        // when
        final Connection connection = proxy.connect(connectionUrl, properties);

        // then
        assertNull(connection);
    }

    @Test
    public void onConnectShouldReturnNullWhenNotStartingWithUrlPrefix() throws SQLException {
        // given
        String connectionUrl = createConnectionUrlWithoutPortForwardParameter("anyStringOtherThanPrefix", OPENSHIFT_SERVER_NAME, APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);

        // when
        final Connection connection = proxy.connect(connectionUrl, properties);

        // then
        assertNull(connection);
    }


    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenMissingNamespacePrefixInUrl() throws SQLException {
        // given
        connectionUrl = connectionUrl.replace(ProxyDriverURLParameter.DOMAIN_PARAMETER_PREFIX, "");

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenMissingCartridgePrefixInUrl() throws SQLException {
        // given
        connectionUrl = connectionUrl.replace(ProxyDriverURLParameter.CARTRIDGE_PARAMETER_PREFIX, "");

        // when
        proxy.connect(connectionUrl, properties);
    }


    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoServerIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, "", APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoApplicationIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, "", DOMAIN_NAME, CARTRIDGE_NAME);

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoNamespaceIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, "", CARTRIDGE_NAME);

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoCartridgeIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, DOMAIN_NAME, "");

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoLocalPortIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);

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
    public void onConnectShouldOpenConnectionToOpenshiftServer() throws SQLException {
        // given
        mockOpenshiftDatabaseDataResponse();

        // when
        proxy.connect(connectionUrl, properties);

        // then
        verify(communicatorMock).connect(OPENSHIFT_SERVER_NAME, OPENSHIFT_USER_NAME, OPENSHIFT_PASSWORD);
    }

    @Test(expected = SQLException.class)
    public void onConnectWhenOpenshiftConnectionErrorOccurrsShouldThrowException() throws SQLException {
        // given
        doThrow(new OpenShiftException("")).when(communicatorMock).connect(OPENSHIFT_SERVER_NAME, OPENSHIFT_USER_NAME, OPENSHIFT_PASSWORD);

        // when
        proxy.connect(connectionUrl, properties);

        // then
        verify(communicatorMock).connect(OPENSHIFT_SERVER_NAME, OPENSHIFT_USER_NAME, OPENSHIFT_PASSWORD);
    }

    @Test
    public void onConnectShouldReadDatabaseDataOnOpenshiftServer() throws SQLException {
        // given
        mockOpenshiftDatabaseDataResponse();
        // when
        proxy.connect(connectionUrl, properties);

        // then
        verify(communicatorMock).readDatabaseData(APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);
    }

    @Test
    public void onConnectShouldExtractAndDelegateWithUserAndPasswordProperties() throws SQLException {
        // given
        String dbUser = "dbUser";
        String dbPwd = "dbPwd";
        String dbConnectionUrl = "postgresql://$OPENSHIFT_POSTGRESQL_DB_HOST:$OPENSHIFT_POSTGRESQL_DB_PORT";
        String dbName = "dbName";
        mockOpenshiftDatabaseDataResponse(dbUser, dbPwd, dbConnectionUrl, dbName);

        properties.put(OpenshiftProxyDriver.SSH_PRIVATE_KEY_PROPERTY_KEY, "");

        // when
        proxy.connect(connectionUrl, properties);

        // then
        ArgumentCaptor<Properties> argCapt = ArgumentCaptor.forClass(Properties.class);
        verify(connectionProxyMock).wrap(anyString(), argCapt.capture());

        final Properties properties = argCapt.getAllValues().get(0);
        assertEquals(dbUser, properties.get(OpenshiftProxyDriver.USER_PROPERTY_KEY));
        assertEquals(dbPwd, properties.get(OpenshiftProxyDriver.PASSWORD_PROPERTY_KEY));
    }

    @Test
    public void onConnectShouldRemoveProxyDriverSshKeyProperty() throws SQLException {
        // given
        mockOpenshiftDatabaseDataResponse();

        properties.put(OpenshiftProxyDriver.SSH_PRIVATE_KEY_PROPERTY_KEY, "");

        // when
        proxy.connect(connectionUrl, properties);

        // then
        ArgumentCaptor<Properties> argCapt = ArgumentCaptor.forClass(Properties.class);
        verify(connectionProxyMock).wrap(anyString(), argCapt.capture());

        final Properties properties = argCapt.getAllValues().get(0);
        assertFalse(properties.containsKey(OpenshiftProxyDriver.SSH_PRIVATE_KEY_PROPERTY_KEY));
    }

    @Test
    public void onConnectShouldDelegateTargetDriverProperties() throws SQLException {
        // given
        mockOpenshiftDatabaseDataResponse();

        String targetDriverProperty1 = "targetDriverProperty1";
        String targetDriverProperty2 = "targetDriverProperty2";

        properties.put(targetDriverProperty1, "any value");
        properties.put(targetDriverProperty2, "any value");

        // when
        proxy.connect(connectionUrl, properties);

        // then
        ArgumentCaptor<Properties> argCapt = ArgumentCaptor.forClass(Properties.class);
        verify(connectionProxyMock).wrap(anyString(), argCapt.capture());

        final Properties properties = argCapt.getAllValues().get(0);
        assertTrue(properties.containsKey(targetDriverProperty1));
        assertTrue(properties.containsKey(targetDriverProperty2));
    }


    private void mockOpenshiftDatabaseDataResponse(String dbUser, String dbPwd, String dbConnectionUrl, String dbName) {
        DatabaseData databaseTO = new DatabaseData(dbUser, dbPwd, dbConnectionUrl, dbName);
        when(communicatorMock.readDatabaseData(APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME)).thenReturn(databaseTO);
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
        mockOpenshiftDatabaseDataResponse();

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
        verify(communicatorMock).startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, OPENSHIFT_DB_CONNECTION_URL, null);
    }

    @Test
    public void onConnectShouldNotStartPortForwardingWhenParameterIsSet() throws SQLException {
        // given

        mockOpenshiftDatabaseDataResponse();
        connectionUrl += ProxyDriverURLParameter.PARAMETER_DELIMITER + ProxyDriverURLParameter.FORWARDED_PORT_PARAMETER_PREFIX + "9999";

        // when
        proxy.connect(connectionUrl, properties);

        // then
        verify(communicatorMock, never()).startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, OPENSHIFT_DB_CONNECTION_URL, null);
    }

    @Test
    public void onConnectShouldStartPortforwardingWithSshKeyPathFromProperty() throws SQLException {
        // given
        mockOpenshiftDatabaseDataResponse();
        String sshKeyPath = "sshKeyPath";
        properties.put(OpenshiftProxyDriver.SSH_PRIVATE_KEY_PROPERTY_KEY, sshKeyPath);

        // when
        proxy.connect(connectionUrl, properties);

        // then
        verify(communicatorMock).startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, OPENSHIFT_DB_CONNECTION_URL, sshKeyPath);
    }

    @Test
    public void onConnectShouldNotStartPortForwardingWhenParameterIsSetAndConnectToGivenPort() throws SQLException {
        // given
        int forwardedPort = 9999;
        mockOpenshiftDatabaseDataResponse();
        connectionUrl += ProxyDriverURLParameter.PARAMETER_DELIMITER + ProxyDriverURLParameter.FORWARDED_PORT_PARAMETER_PREFIX + forwardedPort;

        // when
        proxy.connect(connectionUrl, properties);

        // then
        verify(connectionProxyMock).wrap(contains(String.valueOf(forwardedPort)), any(Properties.class));
    }

    @Test(expected = SQLException.class)
    public void onConnectShouldThrowExceptionWhenPortForwardingParameterIsSetWithInvalidValue() throws SQLException {
        // given
        String forwardedPort = "Invalid port number";
        mockOpenshiftDatabaseDataResponse();
        connectionUrl += ProxyDriverURLParameter.PARAMETER_DELIMITER + ProxyDriverURLParameter.FORWARDED_PORT_PARAMETER_PREFIX + forwardedPort;

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test
    public void onConnectShouldExtractAndDelegateWithConnectionUrlUsingPort() throws SQLException {
        // given
        int forwardedPort = 9999;
        mockOpenshiftDatabaseDataResponse();
        when(communicatorMock.startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, OPENSHIFT_DB_CONNECTION_URL, null)).thenReturn(forwardedPort);

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

    @Test
    public void onAcceptsURLShouldReturnFalseWhenUrlWithoutProxyDriverPrefix() throws SQLException {
        // given
        String connectionUrl = createConnectionUrlWithoutPortForwardParameter("anyStringOtherThanPrefix", OPENSHIFT_SERVER_NAME, APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);

        // when
        final boolean isUrlAccepted = proxy.acceptsURL(connectionUrl);

        // then
        assertFalse(isUrlAccepted);
    }

    @Test
    public void onAcceptsURLShouldReturnFalseWhenUrlIsNull() throws SQLException {
        // given
        String connectionUrl = null;
        // when
        final boolean isUrlAccepted = proxy.acceptsURL(connectionUrl);

        // then
        assertFalse(isUrlAccepted);
    }

    @Test
    public void onAcceptsURLShouldReturnTrueWhenUrlHasValidProxyPrefixAndValidParameter() throws SQLException {
        // given
        String connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);

        // when
        final boolean isUrlAccepted = proxy.acceptsURL(connectionUrl);

        // then
        assertTrue(isUrlAccepted);
    }

    @Test
    public void onAcceptsURLShouldReturnTrueWhenUrlHasValidProxyPrefixButInvalidUrlParameter() throws SQLException {
        // given
        String connectionUrl = ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX + "invalid url parameter";
        // when
        final boolean isUrlAccepted = proxy.acceptsURL(connectionUrl);

        // then
        assertTrue(isUrlAccepted);
    }

    @Test
    public void onGetMajorVersionShouldReturnValueDefinedInConstant() throws SQLException {
        // when
        final int majorVersion = proxy.getMajorVersion();

        // then
        assertEquals(OpenshiftProxyDriver.MAJOR_VERSION, majorVersion);
    }

    @Test
    public void onGetMinorVersionShouldReturnValueDefinedInConstant() throws SQLException {
        // when
        final int minorVersion = proxy.getMinorVersion();

        // then
        assertEquals(OpenshiftProxyDriver.MINOR_VERSION, minorVersion);
    }

}