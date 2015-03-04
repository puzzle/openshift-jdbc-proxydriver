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

import static org.junit.Assert.*;

public class ProxyDriverURLParameterTest {

    private static final String OPENSHIFT_SERVER_NAME = "serverUrl";
    private static final String APPLICATION_NAME = "applicationName";
    private static final String DOMAIN_NAME = "openshiftDomainName";
    private static final String CARTRIDGE_NAME = "cartridgeName";

    private ProxyDriverURLParameter proxyDriverURLParameter;

    private String connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);


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
    public void onCreateValidShouldThrowExceptionWhenNoServerIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, "", APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenNoApplicationIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, "", DOMAIN_NAME, CARTRIDGE_NAME);

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenNoNamespaceIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, "", CARTRIDGE_NAME);

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onCreateValidShouldThrowExceptionWhenNoCartridgeIsSet() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, DOMAIN_NAME, "");

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);
    }


    @Test(expected = NumberFormatException.class)
    public void onCreateValidShouldThrowExceptionWhenExternalPortIsNotANumber() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);
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
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);

        // then
        assertNotNull(proxyDriverURLParameter);
        assertEquals(OPENSHIFT_SERVER_NAME, proxyDriverURLParameter.getServer());
        assertEquals(APPLICATION_NAME, proxyDriverURLParameter.getApplication());
        assertEquals(DOMAIN_NAME, proxyDriverURLParameter.getDomain());
        assertEquals(CARTRIDGE_NAME, proxyDriverURLParameter.getCartridge());
        assertFalse(proxyDriverURLParameter.hasExternalForwardedPort());
    }

    @Test
    public void onCreateValidShouldCreateProxyDriverUrlOnValidUrlWithExternalPortForward() throws SQLException {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);
        connectionUrl += ProxyDriverURLParameter.PARAMETER_DELIMITER + ProxyDriverURLParameter.FORWARDED_PORT_PARAMETER_PREFIX + "123456";

        // when
        proxyDriverURLParameter = ProxyDriverURLParameter.createValid(connectionUrl);

        // then
        assertNotNull(proxyDriverURLParameter);
        assertEquals(OPENSHIFT_SERVER_NAME, proxyDriverURLParameter.getServer());
        assertEquals(APPLICATION_NAME, proxyDriverURLParameter.getApplication());
        assertEquals(DOMAIN_NAME, proxyDriverURLParameter.getDomain());
        assertEquals(CARTRIDGE_NAME, proxyDriverURLParameter.getCartridge());
        assertTrue(proxyDriverURLParameter.hasExternalForwardedPort());
    }

    @Test
    public void onAcceptProxyDriverProtocolWithProxyDriverProtocolShouldReturnTrue() {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter(ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX, OPENSHIFT_SERVER_NAME, APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);

        // when
        boolean isAccepted = ProxyDriverURLParameter.acceptProxyDriverProtocol(connectionUrl);

        // then
        assertTrue(isAccepted);
    }

    @Test
    public void onAcceptProxyDriverProtocolWithOtherThenProxyDriverProtocolShouldReturnFalse() {
        // given
        connectionUrl = createConnectionUrlWithoutPortForwardParameter("other than proxydriver protocol", OPENSHIFT_SERVER_NAME, APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);

        // when
        boolean isAccepted = ProxyDriverURLParameter.acceptProxyDriverProtocol(connectionUrl);

        // then
        assertFalse(isAccepted);
    }


    private String createConnectionUrlWithoutPortForwardParameter(String prefix, String server, String application, String namespace, String cartridge) {
        return prefix + server + "/" + application + "?" + ProxyDriverURLParameter.DOMAIN_PARAMETER_PREFIX + namespace + ProxyDriverURLParameter.PARAMETER_DELIMITER + ProxyDriverURLParameter.CARTRIDGE_PARAMETER_PREFIX + cartridge + ProxyDriverURLParameter.PARAMETER_DELIMITER;
    }

}