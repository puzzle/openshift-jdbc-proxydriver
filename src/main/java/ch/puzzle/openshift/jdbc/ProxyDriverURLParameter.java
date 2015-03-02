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

import java.util.Properties;

/**
 * Created by bschwaller on 28.02.15.
 */
public class ProxyDriverURLParameter {
    static final String DRIVER_PROTOCOL_URL_PREFIX = "jdbc:openshiftproxy://";

    static final String PARAMETER_DELIMITER = "&";
    static final String DOMAIN_PARAMETER_PREFIX = "domain=";
    static final String CARTRIDGE_PARAMETER_PREFIX = "cartridge=";
    static final String DRIVER_PARAMETER_PREFIX = "driver=";
    static final String FORWARDED_PORT_PARAMETER_PREFIX = "externalforwardedport=";

    static final String SERVER = "openshiftServerKey";
    static final String APPLICATION = "applicationKey";
    static final String DOMAIN = "domainKey";
    static final String CARTRIDGE = "cartridgeKey";
    static final String DRIVER = "driverKey";
    static final String EXTERNAL_FORWARDED_PORT = "externalForwardedPortKey";


    private final String server;
    private final String application;
    private final String domain;
    private final String cartridge;
    private final String driver;
    private final Integer externalForwardedPort;

    private ProxyDriverURLParameter(String server, String application, String domain, String cartridge, String driver, String externalForwardedPort) {
        this.server = verifyNotNullAndNotEmpty(server);
        this.application = verifyNotNullAndNotEmpty(application);
        this.domain = verifyNotNullAndNotEmpty(domain);
        this.cartridge = verifyNotNullAndNotEmpty(cartridge);
        this.driver = verifyNotNullAndNotEmpty(driver);
        this.externalForwardedPort = convertIfNotNull(externalForwardedPort);
    }

    private Integer convertIfNotNull(String externalForwardedPort) {
        if (externalForwardedPort != null) {
            return Integer.valueOf(externalForwardedPort);

        }
        return null;
    }

    /**
     * Parses the connectionUrl and creates a {@link ProxyDriverURLParameter} object. Occurrence of all mandatory parameters within the connectionUrl are verified. In case of a missing or invalid (null value) parameter an exception will be thrown.
     *
     * @return valid {@link ProxyDriverURLParameter} object
     */
    public static ProxyDriverURLParameter createValid(String connectionUrl) {
        Properties parameter = extractProxyDriverParametersFromUrl(DRIVER_PROTOCOL_URL_PREFIX, connectionUrl);
        return new ProxyDriverURLParameter(parameter.getProperty(SERVER), parameter.getProperty(APPLICATION), parameter.getProperty(DOMAIN), parameter.getProperty(CARTRIDGE), parameter.getProperty(DRIVER), parameter.getProperty(EXTERNAL_FORWARDED_PORT));
    }

    private static Properties extractProxyDriverParametersFromUrl(String proxyDriverURLPrefix, String url) {

        final String urlWithoutProtocol = url.substring(proxyDriverURLPrefix.length());
        final String[] serverWithAppAndArguments = urlWithoutProtocol.split("\\?");

        if (serverWithAppAndArguments.length == 2) {

            String serverWithApp = serverWithAppAndArguments[0];
            String urlParameter = serverWithAppAndArguments[1];

            return extractParameters(serverWithApp, urlParameter);
        }
        throw new RuntimeException("Invalid URL " + url);
    }

    private static Properties extractParameters(String serverWithApp, String urlParameter) {
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
        return properties;
    }


    public String getServer() {
        return server;
    }

    public String getApplication() {
        return application;
    }

    public String getDomain() {
        return domain;
    }

    public String getCartridge() {
        return cartridge;
    }

    public String getDriver() {
        return driver;
    }

    public Integer getExternalForwardedPort() {
        return externalForwardedPort;
    }

    public boolean hasExternalForwardedPort() {
        return externalForwardedPort != null;
    }

    private String verifyNotNullAndNotEmpty(String argument) {
        if (argument == null || argument.isEmpty()) {
            throw new IllegalArgumentException("Argument " + argument + " must not be null or empty");
        }
        return argument;
    }

    /**
     * Returns true if the url satisfies the proxydriver protocol
     */
    public static boolean acceptProxyDriverProtocol(String url) {
        return url != null && url.regionMatches(true, 0, DRIVER_PROTOCOL_URL_PREFIX, 0, DRIVER_PROTOCOL_URL_PREFIX.length());
    }

}
