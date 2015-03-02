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

import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProxyDriverURLParameterTest {

    private static final String OPENSHIFT_SERVER_NAME = "serverUrl";
    private static final String APPLICATION_NAME = "applicationName";
    private static final String NAMESPACE_NAME = "openshiftDomainName";
    private static final String CARTRIDGE_NAME = "cartridgeName";
    private static final String ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME = "org.postgresql.Driver";
    private static final String OPENSHIFT_USER_NAME = "Username";
    private static final String OPENSHIFT_PASSWORD = "Password";
    private static final String OPENSHIFT_DB_CONNECTION_URL = "postgresql://$OPENSHIFT_POSTGRESQL_DB_HOST:$OPENSHIFT_POSTGRESQL_DB_PORT";

    private ProxyDriverURLParameter proxyDriverURLParameter;

    private String connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);


    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenMissingNamespacePrefixInUrl() throws SQLException {
        // given
        connectionUrl = connectionUrl.replace(ProxyDriverURLParameter.DOMAIN_PARAMETER_PREFIX, "");

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenMissingCartridgePrefixInUrl() throws SQLException {
        // given
        connectionUrl = connectionUrl.replace(ProxyDriverURLParameter.CARTRIDGE_PARAMETER_PREFIX, "");

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenMissingDriverPrefixInUrl() throws SQLException {
        // given
        connectionUrl = connectionUrl.replace(ProxyDriverURLParameter.DRIVER_PARAMETER_PREFIX, "");

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenNoServerIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, "", APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenNoApplicationIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, "", NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenNoNamespaceIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, "", CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenNoCartridgeIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, "", ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenNoDriverIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, "");

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);
    }

    @Test(expected = NumberFormatException.class)
    public void onCreateValidShouldThrowExceptionWhenExternalPortIsNotANumber() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);
        connectionUrl += ProxyDriverURLParameter.PARAMETER_DELIMITER + ProxyDriverURLParameter.FORWARDED_PORT_PARAMETER_PREFIX + "not a number";

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);
    }

    @Test(expected = RuntimeException.class)
    public void onCreateValidShouldThrowExceptionWhenMissingQuestionmarkDelimiterInUrl() throws SQLException {
        // given
        connectionUrl = connectionUrl.replace("?", "");

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);
    }

    @Test
    public void onCreateValidShouldCreateProxyDriverUrlOnValidUrl() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);
    }

    @Test
    public void onCreateValidShouldCreateProxyDriverUrlOnValidUrlWithExternalPortForward() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);
        connectionUrl += ProxyDriverURLParameter.PARAMETER_DELIMITER + ProxyDriverURLParameter.FORWARDED_PORT_PARAMETER_PREFIX + "123456";

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);
    }

    @Test
    public void onAcceptProxyDriverProtocolWithProxyDriverProtocolShouldReturnTrue() {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        boolean isAccepted = ProxyDriverURLParameter.acceptProxyDriverProtocol(connectionUrl);

        // then
        assertTrue(isAccepted);
    }

    @Test
    public void onAcceptProxyDriverProtocolWithOtherThenProxyDriverProtocolShouldReturnFalse() {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter("other than proxydriver protocol", OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        boolean isAccepted = ProxyDriverURLParameter.acceptProxyDriverProtocol(connectionUrl);

        // then
        assertFalse(isAccepted);
    }


    private String createConnectionUrlWithoutPortForwardParameter(String prefix, String server, String application, String namespace, String cartridge, String driver) {
        return prefix + server + "/" + application + "?" + ProxyDriverURLParameter.DOMAIN_PARAMETER_PREFIX + namespace + ProxyDriverURLParameter.PARAMETER_DELIMITER + ProxyDriverURLParameter.CARTRIDGE_PARAMETER_PREFIX + cartridge + ProxyDriverURLParameter.PARAMETER_DELIMITER + ProxyDriverURLParameter.DRIVER_PARAMETER_PREFIX + driver;
    }

}