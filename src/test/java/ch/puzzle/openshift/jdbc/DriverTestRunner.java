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

import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by bschwaller on 11.02.15.
 */
public class DriverTestRunner {


    /**
     * Start proxy driver.
     * Define user credentials as vm arguments within the ide's runconfiguration: -DopenshiftUser=<openshiftuser> -DopenshiftUserPassword=<password>
     */
    public static void main(String[] args) throws Exception {

        String openshiftServer = "openshift.redhat.com";
        String application = "ref";
        String domain = "rocketknaller";
        String cartridge = "postgresql-9.2";

        String openshiftUser = System.getProperty("openshiftUser");
        String openshiftUserPassword = System.getProperty("openshiftUserPassword");

        if (openshiftUser == null || openshiftUserPassword == null) {
            throw new IllegalArgumentException("Missing user or password systemproperty argument! Define the following arguments as on runconfiguration -DopenshiftUser=<openshiftuser> -DopenshiftUserPassword=<password>");
        }

        connect(openshiftServer, application, domain, cartridge, openshiftUser, openshiftUserPassword);
    }

    private static void connect(String openshiftServer, String application, String domain, String cartridge, String openshiftUser, String openshiftUserPassword) throws SQLException {
        OpenshiftProxyDriver proxy = new OpenshiftProxyDriver();
        String connectionUrl = createConnectionUrlWithoutPortForwardParameter(openshiftServer, application, domain, cartridge);

        Properties props = createProperties(openshiftUser, openshiftUserPassword);

        proxy.connect(connectionUrl, props);
    }

    private static Properties createProperties(String openshiftUser, String openshiftPassword) {
        Properties properties = new Properties();
        properties.put(OpenshiftProxyDriver.USER_PROPERTY_KEY, openshiftUser);
        properties.put(OpenshiftProxyDriver.PASSWORD_PROPERTY_KEY, openshiftPassword);
        properties.put(OpenshiftProxyDriver.SSH_PRIVATE_KEY_PROPERTY_KEY, "~/.ssh/id_rsa");
        return properties;
    }

    private static String createConnectionUrlWithoutPortForwardParameter(String openshiftServer, String application, String domain, String cartridge) {
        return ProxyDriverURLParameter.DRIVER_PROTOCOL_URL_PREFIX + openshiftServer + "/" + application + "?" + ProxyDriverURLParameter.DOMAIN_PARAMETER_PREFIX + domain + ProxyDriverURLParameter.PARAMETER_DELIMITER + ProxyDriverURLParameter.CARTRIDGE_PARAMETER_PREFIX + cartridge;
    }

}
