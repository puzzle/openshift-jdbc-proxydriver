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

        String openshiftServer = "broker.openshift-dev.puzzle.ch";
        String application = "amwnode1";
        String domain = "dev";
        String cartridge = "postgresql-9.2";
        String driver = "org.postgresql.Driver";

        String openshiftUser = System.getProperty("openshiftUser");
        String openshiftUserPassword = System.getProperty("openshiftUserPassword");

        if (openshiftUser == null || openshiftUserPassword == null) {
            throw new IllegalArgumentException("Missing user or password systemproperty argument! Define the following arguments as on runconfiguration -DopenshiftUser=<openshiftuser> -DopenshiftUserPassword=<password>");
        }

        connect(openshiftServer, application, domain, cartridge, driver, openshiftUser, openshiftUserPassword);
    }

    private static void connect(String broker, String application, String namespace, String cartridge, String driver, String openshiftUser, String openshiftUserPassword) throws SQLException {
        OpenshiftProxyDriver proxy = new OpenshiftProxyDriver();
        String connectionUrl = createConnectionUrlWithoutPortForwardParameter(broker, application, namespace, cartridge, driver);

        Properties props = createProperties(openshiftUser, openshiftUserPassword);

        proxy.connect(connectionUrl, props);
    }

    private static Properties createProperties(String openshiftUser, String openshiftPassword) {
        Properties properties = new Properties();
        properties.put(OpenshiftProxyDriver.USER_PROPERTY_KEY, openshiftUser);
        properties.put(OpenshiftProxyDriver.PASSWORD_PROPERTY_KEY, openshiftPassword);
        return properties;
    }

    private static String createConnectionUrlWithoutPortForwardParameter(String broker, String application, String namespace, String cartridge, String driver) {
        return OpenshiftProxyDriver.URL_PREFIX + broker + "/" + application + "?" + OpenshiftProxyDriver.DOMAIN_PARAMETER_PREFIX + namespace + OpenshiftProxyDriver.PARAMETER_DELIMITER + OpenshiftProxyDriver.CARTRIDGE_PARAMETER_PREFIX + cartridge + OpenshiftProxyDriver.PARAMETER_DELIMITER + OpenshiftProxyDriver.DRIVER_PARAMETER_PREFIX + driver;
    }

}
