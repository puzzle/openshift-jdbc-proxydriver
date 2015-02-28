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

public class ProxyDriverURLTest {

    private static final String OPENSHIFT_SERVER_NAME = "serverUrl";
    private static final String APPLICATION_NAME = "applicationName";
    private static final String NAMESPACE_NAME = "openshiftDomainName";
    private static final String CARTRIDGE_NAME = "cartridgeName";
    private static final String ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME = "org.postgresql.Driver";
    private static final String OPENSHIFT_USER_NAME = "Username";
    private static final String OPENSHIFT_PASSWORD = "Password";
    private static final String OPENSHIFT_DB_CONNECTION_URL = "postgresql://$OPENSHIFT_POSTGRESQL_DB_HOST:$OPENSHIFT_POSTGRESQL_DB_PORT";

    private ProxyDriverURL proxyDriverURL;

    private String connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);


    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenMissingNamespacePrefixInUrl() throws SQLException {
        // given
        connectionUrl = connectionUrl.replace(ProxyDriverURL.DOMAIN_PARAMETER_PREFIX, "");

        // when
        proxyDriverURL = ProxyDriverURL.createValid(OpenshiftProxyDriver.URL_PREFIX, connectionUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenMissingCartridgePrefixInUrl() throws SQLException {
        // given
        connectionUrl = connectionUrl.replace(ProxyDriverURL.CARTRIDGE_PARAMETER_PREFIX, "");

        // when
        proxyDriverURL = ProxyDriverURL.createValid(OpenshiftProxyDriver.URL_PREFIX, connectionUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenMissingDriverPrefixInUrl() throws SQLException {
        // given
        connectionUrl = connectionUrl.replace(ProxyDriverURL.DRIVER_PARAMETER_PREFIX, "");

        // when
        proxyDriverURL = ProxyDriverURL.createValid(OpenshiftProxyDriver.URL_PREFIX, connectionUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenNoServerIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, "", APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxyDriverURL = ProxyDriverURL.createValid(OpenshiftProxyDriver.URL_PREFIX, connectionUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenNoApplicationIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, "", NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxyDriverURL = ProxyDriverURL.createValid(OpenshiftProxyDriver.URL_PREFIX, connectionUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenNoNamespaceIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, "", CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxyDriverURL = ProxyDriverURL.createValid(OpenshiftProxyDriver.URL_PREFIX, connectionUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenNoCartridgeIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, "", ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxyDriverURL = ProxyDriverURL.createValid(OpenshiftProxyDriver.URL_PREFIX, connectionUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenNoDriverIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, "");

        // when
        proxyDriverURL = ProxyDriverURL.createValid(OpenshiftProxyDriver.URL_PREFIX, connectionUrl);
    }

    @Test(expected = NumberFormatException.class)
    public void onCreateValidShouldThrowExceptionWhenExternalPortIsNotANumber() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);
        connectionUrl += ProxyDriverURL.PARAMETER_DELIMITER + ProxyDriverURL.FORWARDED_PORT_PARAMETER_PREFIX + "not a number";

        // when
        proxyDriverURL = ProxyDriverURL.createValid(OpenshiftProxyDriver.URL_PREFIX, connectionUrl);
    }

    @Test(expected = RuntimeException.class)
    public void onCreateValidShouldThrowExceptionWhenMissingQuestionmarkDelimiterInUrl() throws SQLException {
        // given
        connectionUrl = connectionUrl.replace("?", "");

        // when
        proxyDriverURL = ProxyDriverURL.createValid(OpenshiftProxyDriver.URL_PREFIX, connectionUrl);
    }

    @Test
    public void onCreateValidShouldCreateProxyDriverUrlOnValidUrl() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);

        // when
        proxyDriverURL = ProxyDriverURL.createValid(OpenshiftProxyDriver.URL_PREFIX, connectionUrl);
    }

    @Test
    public void onCreateValidShouldCreateProxyDriverUrlOnValidUrlWithExternalPortForward() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(OpenshiftProxyDriver.URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, NAMESPACE_NAME, CARTRIDGE_NAME, ORIGINAL_POSTGRES_JDBC_DRIVER_FULL_CLASS_NAME);
        connectionUrl += ProxyDriverURL.PARAMETER_DELIMITER + ProxyDriverURL.FORWARDED_PORT_PARAMETER_PREFIX + "123456";

        // when
        proxyDriverURL = ProxyDriverURL.createValid(OpenshiftProxyDriver.URL_PREFIX, connectionUrl);
    }


    private String createConnectionUrlWithoutPortForwardParameter(String prefix, String server, String application, String namespace, String cartridge, String driver) {
        return prefix + server + "/" + application + "?" + ProxyDriverURL.DOMAIN_PARAMETER_PREFIX + namespace + ProxyDriverURL.PARAMETER_DELIMITER + ProxyDriverURL.CARTRIDGE_PARAMETER_PREFIX + cartridge + ProxyDriverURL.PARAMETER_DELIMITER + ProxyDriverURL.DRIVER_PARAMETER_PREFIX + driver;
    }

}