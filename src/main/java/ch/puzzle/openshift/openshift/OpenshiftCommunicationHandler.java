package ch.puzzle.openshift.openshift;

import com.jcraft.jsch.*;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;
import com.openshift.client.cartridge.IEmbeddedCartridge;
import com.openshift.internal.client.response.CartridgeResourceProperties;
import com.openshift.internal.client.utils.StreamUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Created by bschwaller on 11.02.15.
 */
public class OpenshiftCommunicationHandler {

    static final String USERNAME_KEY = "username";
    static final String PASSWORD_KEY = "password";
    static final String DATABASE_NAME_KEY = "database_name";
    static final String CONNECTION_URL_KEY = "connection_url";

    private static final String SSH_URL_PREFIX = "ssh://";


    private Logger logger = Logger.getLogger(OpenshiftCommunicationHandler.class.getName());

    private String openshiftUser;
    private String openshiftPassword;

    private IOpenShiftConnection connection;
    private Session session;

    private OpenshiftConnector connectionCreator;
    // TODO implement logging

    public OpenshiftCommunicationHandler() {
        this.connectionCreator = new OpenshiftConnector();
    }

    public void connect(String broker, String openshiftUser, String openshiftPassword) {
        connection = connectionCreator.getConnection(broker, openshiftUser, openshiftPassword);
        this.openshiftUser = openshiftUser;
        this.openshiftPassword = openshiftPassword;
    }


    private boolean isConnected() {
        return connection != null;
    }

    /**
     * @param applicationName
     * @param namespace
     * @param connectionUrl
     * @return
     */
    public int startPortForwarding(String applicationName, String namespace, String connectionUrl) {
        final IApplication application = getApplication(applicationName, namespace);
        final String sshUrl = application.getSshUrl();
        session = getSession(sshUrl);

        final List<String> rhcListPortsOutput = executeRhcListPorts(session);

        ForwardablePort port = extractForwardableDatabasePort(rhcListPortsOutput, connectionUrl);
        port.startPortForwarding(session);
        logger.info("Started port forwarding " + port.toString());
        return port.getLocalPort();
    }

    private ForwardablePort extractForwardableDatabasePort(List<String> rhcListPortsOutput, String connectionUrl) {
        for (String line : rhcListPortsOutput) {
            ForwardablePort port = ForwardablePort.createForValidRhcListPortsOutputLine(line);

            if (port != null && connectionUrl.startsWith(port.getName())) {
                return port;
            }
        }
        throw new RuntimeException("No forwardable port found!");
    }

    private List<String> executeRhcListPorts(Session session) {
        List<String> forwardablePorts = new ArrayList<>();
        InputStream in = null;
        OutputStream out = null;
        Channel channel = null;
        try {
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand("rhc-list-ports");
            ((ChannelExec) channel).setPty(true);
            in = channel.getInputStream();
            out = channel.getOutputStream();
            ((ChannelExec) channel).setErrStream(System.err);
            channel.connect();
            out.flush();
            forwardablePorts = readLines(in);
        } catch (Exception e) {
            logger.warning("Error while executing rhc-list-ports on session. Reason: " + e.getMessage());
            throw new RuntimeException("Error while executing rhc-list-ports on session", e);
        } finally {
            try {
                StreamUtils.close(in);
                StreamUtils.close(out);
                if (channel != null) {
                    channel.disconnect();
                }
            } catch (IOException e) {
                logger.warning("Could not close channel to ssh server");
            }
        }
        return forwardablePorts;
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


    private Session getSession(String sshUrl) {
        try {
            JSch jsch = new JSch();
            String keyFile = "~/.ssh/id_rsa";
            Path tempFile = Paths.get(keyFile);
            File file = tempFile.toFile();
            jsch.addIdentity(file.getPath());

            String[] userHost = extractApplicationUserAndHost(sshUrl);
            String applicationUser = userHost[0];
            String applicationHost = userHost[1];

            Session session = jsch.getSession(applicationUser, applicationHost);
            session.setConfig("StrictHostKeyChecking", "no");

            session.connect();
            return session;
        } catch (JSchException e) {
            throw new RuntimeException("Could not open session");
        }
    }

    private String[] extractApplicationUserAndHost(String sshUrl) {
        Objects.requireNonNull(sshUrl, "SshUrl must not be empty");

        if (sshUrl.startsWith(SSH_URL_PREFIX)) {
            sshUrl = sshUrl.substring(SSH_URL_PREFIX.length());
        }
        String[] userHost = sshUrl.split("@");
        if (userHost.length != 2) {
            throw new RuntimeException("Could not extract application user and host from sshUrl " + sshUrl);
        }
        return userHost;
    }

    private IApplication getApplication(String applicationName, String namespace) {
        if (isConnected()) {
            IUser user = connection.getUser();
            if (user != null) {
                IDomain domain = user.getDomain(namespace);
                if (domain != null) {
                    final IApplication application = domain.getApplicationByName(applicationName);
                    if (application != null) {
                        return application;
                    }
                }
            }
        }
        throw new RuntimeException("Could not open application " + applicationName + " on namespace " + namespace);
    }


    public DatabaseData readDatabaseData(String applicationName, String namespace, String cartridgeName) {
        final IApplication application = getApplication(applicationName, namespace);
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


    void setOpenshiftConnector(OpenshiftConnector connectionCreator) {
        this.connectionCreator = connectionCreator;
    }

}
