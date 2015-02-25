package ch.puzzle.openshift.openshift;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;
import com.openshift.client.cartridge.IEmbeddedCartridge;
import com.openshift.internal.client.response.CartridgeResourceProperties;
import com.openshift.internal.client.utils.StreamUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by bschwaller on 11.02.15.
 */
public class OpenshiftCommunicationHandler {

    static final String USERNAME_KEY = "username";
    static final String PASSWORD_KEY = "password";
    static final String DATABASE_NAME_KEY = "database_name";
    static final String CONNECTION_URL_KEY = "connection_url";

    static final String WAKE_UP_GEAR_COMMAND = "curl $OPENSHIFT_GEAR_DNS > /dev/null 2>&1";
    static final String RHC_LIST_PORT_COMMAND = "rhc-list-ports";


    private Logger logger = Logger.getLogger(OpenshiftCommunicationHandler.class.getName());
    // TODO implement logging

    private OpenshiftConnector connectionCreator;
    private SessionConnector sessionConnector;
    private IOpenShiftConnection connection;
    private Session session;

    private ForwardablePort port;

    public OpenshiftCommunicationHandler() {
        this.connectionCreator = new OpenshiftConnector();
        this.sessionConnector = new SessionConnector();
    }

    public void connect(String openshiftServer, String openshiftUser, String openshiftPassword) {
        if (!isConnectedToOpenshiftServer()) {
            connection = connectionCreator.getConnection(openshiftServer, openshiftUser, openshiftPassword);
        }
    }


    boolean isConnectedToOpenshiftServer() {
        return connection != null;
    }


    public int startPortForwarding(String applicationName, String domainName, String connectionUrl, String privateSshKeyFilePath) {
        final IApplication application = getApplication(applicationName, domainName);
        final String sshUrl = application.getSshUrl();
        session = sessionConnector.getAndConnectSession(sshUrl, privateSshKeyFilePath);
        port = requestForwardablePort(connectionUrl);
        port.startPortForwarding(session);
        logger.info("Started port forwarding " + port.toString());
        return port.getLocalPort();
    }

    private ForwardablePort requestForwardablePort(String connectionUrl) {
        int timeoutInMillis = 30_000;
        logger.info("Wakeup gear");
        executeCommand(WAKE_UP_GEAR_COMMAND, session, timeoutInMillis);
        logger.info("Execute list-port-forward command");
        List<String> rhcListPortsOutput = executeCommand(RHC_LIST_PORT_COMMAND, session, timeoutInMillis);
        return extractForwardableDatabasePort(rhcListPortsOutput, connectionUrl);
    }


    private IApplication getApplication(String applicationName, String domainName) {
        if (isConnectedToOpenshiftServer()) {
            IUser user = connection.getUser();
            if (user != null) {
                IDomain domain = user.getDomain(domainName);
                if (domain != null) {
                    final IApplication application = domain.getApplicationByName(applicationName);
                    if (application != null) {
                        return application;
                    }
                }
            }
        }
        throw new RuntimeException("Could not open application " + applicationName + " on domainName " + domainName);
    }

    private List<String> executeCommand(String command, Session session, int timeout) {
        List<String> forwardablePorts = new ArrayList<>();
        InputStream in = null;
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setPty(true);
            in = channel.getInputStream();
            channel.connect(timeout);

            forwardablePorts = readLines(in);
        } catch (Exception e) {
            logger.warning("Error while executing rhc-list-ports on session. Reason: " + e.getMessage());
            throw new RuntimeException("Error while executing rhc-list-ports on session", e);
        } finally {
            try {
                StreamUtils.close(in);
                if (channel != null) {
                    channel.disconnect();
                }
            } catch (IOException e) {
                logger.warning("Could not disconnect channel to ssh server");
            }
        }
        return forwardablePorts;
    }

    private ForwardablePort extractForwardableDatabasePort(List<String> rhcListPortsOutput, String connectionUrl) {
        boolean hasValidPortOutput = false;
        for (String line : rhcListPortsOutput) {
            ForwardablePort port = ForwardablePort.createForValidRhcListPortsOutputLine(line);

            if (port != null) {
                hasValidPortOutput = true;
                if (connectionUrl.startsWith(port.getName())) {
                    return port;
                } else {
                    logger.info("Output line " + line + " is not matching the port service");
                }
            }
        }

        if (hasValidPortOutput) {
            throw new RuntimeException("No forwardable port found matching the required service defined by " + connectionUrl);
        } else {
            throw new RuntimeException("No forwardable port found!");
        }
    }


    public List<String> readLines(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        List<String> lines = new ArrayList<String>();
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }


    public DatabaseData readDatabaseData(String applicationName, String domainName, String cartridgeName) {
        final IApplication application = getApplication(applicationName, domainName);
        final IEmbeddedCartridge databaseCartridge = application.getEmbeddedCartridge(cartridgeName);

        if (databaseCartridge != null) {
            final CartridgeResourceProperties databaseCartridgeProperties = databaseCartridge.getProperties();
            String userName = databaseCartridgeProperties.getProperty(USERNAME_KEY).getValue();
            String password = databaseCartridgeProperties.getProperty(PASSWORD_KEY).getValue();
            String connectionUrl = databaseCartridgeProperties.getProperty(CONNECTION_URL_KEY).getValue();
            String databaseName = databaseCartridgeProperties.getProperty(DATABASE_NAME_KEY).getValue();

            return new DatabaseData(userName, password, connectionUrl, databaseName);
        } else {
            throw new RuntimeException("Could not open embedded cardridge " + cartridgeName);
        }
    }

    public void disconnect() {
        if (hasSession()) {
            stopPortforwarding();
            session.disconnect();
            session = null;
            logger.info("Session closed");
        }

        if (isConnectedToOpenshiftServer()) {
            connection = null;
        }
    }

    private void stopPortforwarding() {
        if (hasForwardedPort()) {
            try {
                port.stopPortForwarding(session);
            } catch (RuntimeException e) {
                logger.info("Error stopping port forwarding");
            } finally {
                port = null;
            }
        }
    }

    private boolean hasForwardedPort() {
        return port != null;
    }

    private boolean hasSession() {
        return session != null;
    }

    void setOpenshiftConnector(OpenshiftConnector connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

    void setSessionConnector(SessionConnector sessionConnector) {
        this.sessionConnector = sessionConnector;
    }
}
