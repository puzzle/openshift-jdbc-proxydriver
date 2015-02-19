package ch.puzzle.openshift.jdbc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by bschwaller on 11.02.15.
 */
public class DriverTestRunner {

    private static final String BROKER_NAME = "serverUrl";
    private static final String APPLICATION_NAME = "applicationName";
    private static final String NAMESPACE_NAME = "openshiftDomainName";
    private static final String CARTRIDGE_NAME = "cartridgeName";
    private static final String ORIGINAL_JDBC_DRIVER_FULL_CLASS_NAME = "org.postgresql.Driver";

    private static final String OPENSHIFT_USER_NAME = "Username";
    private static final String OPENSHIFT_PASSWORD = "Password";

    /**
     * User ide vm argument to set user and password
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        String broker = "broker.openshift-dev.puzzle.ch";
        String application = "sonar";
        String namespace = "strubiapp";
        String cartridge = "postgresql-9.2";
        String driver = "org.postgresql.Driver";

        String openshiftUser = System.getProperty("openshiftUser");
        String openshiftUserPassword = System.getProperty("openshiftUserPassword");

        if (openshiftUser == null || openshiftUserPassword == null) {
            throw new IllegalArgumentException("Missing user or password systemproperty argument! Define the following arguments as on runconfiguration -DopenshiftUser=<openshiftuser> -DopenshiftUserPassword=<password>");
        }

        connect(broker, application, namespace, cartridge, driver, openshiftUser, openshiftUserPassword);

    }

    private static String readInputParameter(String parameterName) throws IOException {
        System.out.print(parameterName + ": ");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String s;
        while ((s = in.readLine()) != null && s.length() != 0) {
            return s;
        }
        // An empty line or Ctrl-Z terminates the program
        return null;
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
        return OpenshiftProxyDriver.URL_PREFIX + broker + "/" + application + "?" + OpenshiftProxyDriver.NAMESPACE_PARAMETER_PREFIX + namespace + OpenshiftProxyDriver.PARAMETER_DELIMITER + OpenshiftProxyDriver.CARTRIDGE_PARAMETER_PREFIX + cartridge + OpenshiftProxyDriver.PARAMETER_DELIMITER + OpenshiftProxyDriver.DRIVER_PARAMETER_PREFIX + driver;
    }

//    private String createConnectionUrlWithPortForwardParameter(String broker, String application, String namespace, String cartridge, String driver, int port) {
//        String connectionUrl = createConnectionUrlWithoutPortForwardParameter(broker, application, namespace, cartridge, driver);
//        connectionUrl += OpenshiftDriverProxy.PARAMETER_DELIMITER + OpenshiftDriverProxy.FORWARDED_PORT_PARAMETER_PREFIX + port;
//        return connectionUrl;
//    }

}
