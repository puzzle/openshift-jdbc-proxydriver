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
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Arrays;
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
    private static final String NAMESPACE_NAME = "openshiftDomainName";
    private static final String CARTRIDGE_NAME = "cartridgeName";
    private static final String ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME = "org.postgresql.Driver";
    private static final String OPENSHIFT_USER_NAME = "Username";
    private static final String OPENSHIFT_PASSWORD = "Password";
    private static final String OPENSHIFT_DB_CONNECTION_URL = "postgresql://$OPENSHIFT_POSTGRESQL_DB_HOST:$OPENSHIFT_POSTGRESQL_DB_PORT";

    private static final String USER_PROPERTY = "userProperty";
    private static final String PASSWORD_PROPERTY = "passwordProperty";
    private static final String OTHER_DRIVER_PROPERTY = "otherDriverProperty";

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
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

    }

    private Properties createProperties(String openshiftUser, String openshiftPassword) {
        Properties properties = new Properties();
        properties.put(OpenshiftProxyDriver.USER_PROPERTY_KEY, openshiftUser);
        properties.put(OpenshiftProxyDriver.PASSWORD_PROPERTY_KEY, openshiftPassword);
        return properties;
    }

    private String createConnectionUrlWithoutPortForwardParameter(String prefix, String server, String application, String namespace, String cartridge, String driver) {
        return prefix + server + "/" + application + "?" + ProxyDriverURL.DOMAIN_PARAMETER_PREFIX + namespace + ProxyDriverURL.PARAMETER_DELIMITER + ProxyDriverURL.CARTRIDGE_PARAMETER_PREFIX + cartridge + ProxyDriverURL.PARAMETER_DELIMITER + ProxyDriverURL.DRIVER_PARAMETER_PREFIX + driver;
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
        String connectionUrl = createConnectionUrlWithoutPortForwardParameter("anyStringOtherThanPrefix", OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        final Connection connection = proxy.connect(connectionUrl, properties);

        // then
        assertNull(connection);
    }


    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenMissingNamespacePrefixInUrl() throws SQLException {
        // given
        connectionUrl = connectionUrl.replace(ProxyDriverURL.DOMAIN_PARAMETER_PREFIX, "");

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenMissingCartridgePrefixInUrl() throws SQLException {
        // given
        connectionUrl = connectionUrl.replace(ProxyDriverURL.CARTRIDGE_PARAMETER_PREFIX, "");

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenMissingDriverPrefixInUrl() throws SQLException {
        // given
        connectionUrl = connectionUrl.replace(ProxyDriverURL.DRIVER_PARAMETER_PREFIX, "");

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoServerIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, "", APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoApplicationIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, "", NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoNamespaceIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, "", CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoCartridgeIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, "", ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoDriverIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, "");

        // when
        proxy.connect(connectionUrl, properties);
    }

    @Test(expected = SQLException.class)
    public void parameterValidationOnConnectShouldThrowExceptionWhenNoLocalPortIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

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

    @Test(expected = SQLException.class)
    public void onConnectShouldThrowExceptionOnInvalidDriverClassArgument() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, "invalidDriver");
        connectionUrl += ProxyDriverURL.PARAMETER_DELIMITER + ProxyDriverURL.FORWARDED_PORT_PARAMETER_PREFIX + 9999;

        // when
        proxy.connect(connectionUrl, properties);
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
        verify(communicatorMock).startPortForwarding(APPLICATION_NAME, NAMESPACE_NAME, OPENSHIFT_DB_CONNECTION_URL, null);
    }

    @Test
    public void onConnectShouldNotStartPortForwardingWhenParameterIsSet() throws SQLException {
        // given

        mockOpenshiftDatabaseDataResponse();
        connectionUrl += ProxyDriverURL.PARAMETER_DELIMITER + ProxyDriverURL.FORWARDED_PORT_PARAMETER_PREFIX + "9999";

        // when
        proxy.connect(connectionUrl, properties);

        // then
        verify(communicatorMock, never()).startPortForwarding(APPLICATION_NAME, NAMESPACE_NAME, OPENSHIFT_DB_CONNECTION_URL, null);
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
        verify(communicatorMock).startPortForwarding(APPLICATION_NAME, NAMESPACE_NAME, OPENSHIFT_DB_CONNECTION_URL, sshKeyPath);
    }

    @Test
    public void onConnectShouldNotStartPortForwardingWhenParameterIsSetAndConnectToGivenPort() throws SQLException {
        // given
        int forwardedPort = 9999;
        mockOpenshiftDatabaseDataResponse();
        connectionUrl += ProxyDriverURL.PARAMETER_DELIMITER + ProxyDriverURL.FORWARDED_PORT_PARAMETER_PREFIX + forwardedPort;

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
        connectionUrl += ProxyDriverURL.PARAMETER_DELIMITER + ProxyDriverURL.FORWARDED_PORT_PARAMETER_PREFIX + forwardedPort;

        // when
        proxy.connect(connectionUrl, properties);
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
        when(communicatorMock.startPortForwarding(APPLICATION_NAME, NAMESPACE_NAME, OPENSHIFT_DB_CONNECTION_URL, null)).thenReturn(forwardedPort);

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
        String connectionUrl = createConnectionUrlWithoutPortForwardParameter("anyStringOtherThanPrefix", OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

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
        String connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        final boolean isUrlAccepted = proxy.acceptsURL(connectionUrl);

        // then
        assertTrue(isUrlAccepted);
    }

    @Test
    public void onAcceptsURLShouldReturnTrueWhenUrlHasValidProxyPrefixButInvalidUrlParameter() throws SQLException {
        // given
        String connectionUrl = OpenshiftProxyDriver.URL_PREFIX + "invalid url parameter";
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

    @Test(expected = RuntimeException.class)
    public void onGetPropertyInfoShouldThrowExceptionWhenDriverParameterIsInvalid() throws SQLException {
        // given
        properties = createProperties(OPENSHIFT_USER_NAME, OPENSHIFT_PASSWORD);
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, "invalidDriverName");

        // when
        proxy.getPropertyInfo(connectionUrl, properties);

    }

    @Test
    public void onGetPropertyInfoShouldReturnPropertiesFromProxyDriver() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, TestDriverWithoutProperties.class.getCanonicalName());
        mockOpenshiftDatabaseDataResponse("dbUser", "dbPwd", "postgresql://$OPENSHIFT_POSTGRESQL_DB_HOST:$OPENSHIFT_POSTGRESQL_DB_PORT", "dbName");

        // when
        final DriverPropertyInfo[] propertyInfo = proxy.getPropertyInfo(connectionUrl, properties);

        // then
        assertEquals("Should contain exactly all properties set by openshiftProxyDriver", propertyInfo.length, 3);
        assertTrue(containsProperty(propertyInfo, OpenshiftProxyDriver.USER_PROPERTY_KEY));
        assertTrue(containsProperty(propertyInfo, OpenshiftProxyDriver.PASSWORD_PROPERTY_KEY));
        assertTrue(containsProperty(propertyInfo, OpenshiftProxyDriver.SSH_PRIVATE_KEY_PROPERTY_KEY));

    }

    private boolean containsProperty(DriverPropertyInfo[] propertyInfo, String propertyName) {
        for (DriverPropertyInfo property : Arrays.asList(propertyInfo)) {
            if (property.name.equals(propertyName)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void onGetPropertyInfoShouldReturnPropertiesFromProxyDriverAndAdditionalPropertyFromOriginDriver() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, TestDriverWithUserAndPasswordAndOtherProperties.class.getCanonicalName());
        mockOpenshiftDatabaseDataResponse("dbUser", "dbPwd", "postgresql://$OPENSHIFT_POSTGRESQL_DB_HOST:$OPENSHIFT_POSTGRESQL_DB_PORT", "dbName");

        // when
        final DriverPropertyInfo[] propertyInfo = proxy.getPropertyInfo(connectionUrl, properties);

        // then
        assertEquals("Should contain all properties set by openshiftProxyDriver and additionally property from origin driver", propertyInfo.length, 4);
        assertTrue(containsProperty(propertyInfo, OpenshiftProxyDriver.USER_PROPERTY_KEY));
        assertTrue(containsProperty(propertyInfo, OpenshiftProxyDriver.PASSWORD_PROPERTY_KEY));
        assertTrue(containsProperty(propertyInfo, OpenshiftProxyDriver.SSH_PRIVATE_KEY_PROPERTY_KEY));
        assertTrue(containsProperty(propertyInfo, TestDriverWithUserAndPasswordAndOtherProperties.OTHER_DRIVER_PROPERTY));
    }


}