package ch.puzzle.openshift.openshift;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.openshift.client.OpenShiftSSHOperationException;

import java.net.BindException;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bschwaller on 11.02.15.
 */
public class ForwardablePort {
    public static final String LOCALHOST = "localhost";

//    The range 49152–65535 (215+214 to 216−1)—above the registered ports—contains dynamic or private ports that cannot be registered with IANA.[174] This range is used for custom or temporary purposes and for automatic allocation of ephemeral ports.

    /**
     * Regex for port forwarding
     */
    private static final Pattern REGEX_FORWARDED_PORT = Pattern.compile("([^ ]+) -> ([^:]+):(\\d+)"); // TODO from ApplicationSSHSession
    private static final int PORT_ITERATION_RANGE = 10;
    private static final int INITIAL_STARTING_PORT = 49152;


    private final String name;
    private final String remoteAddress;
    private final int remotePort;

    private int localPort = -1;


    private ForwardablePort(String name, String remoteAddress, String remotePortString) {
        this.name = Objects.requireNonNull(name, "Name must not be null");
        this.remoteAddress = Objects.requireNonNull(remoteAddress, "Remote address must not be null");
        int remotePort = Integer.parseInt(remotePortString);
        this.remotePort = Objects.requireNonNull(remotePort, "Remote port must not be null");
    }


    public static ForwardablePort createForValidRhcListPortsOutputLine(String rhcListPortsOutputLine) {
        final Matcher matcher = extractPortInformation(rhcListPortsOutputLine);
        if (matcher != null) {
            return new ForwardablePort(matcher.group(1), matcher.group(2), matcher.group(3));
        }
        return null;
    }

    private static Matcher extractPortInformation(String rhcListPortsOutputLine) {
        Matcher matcher = REGEX_FORWARDED_PORT.matcher(rhcListPortsOutputLine);
        if (!matcher.find() || matcher.groupCount() != 3) {
            return null;
        }
        return matcher;
    }


    public String getName() {
        return name;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public Integer getLocalPort() {
        return localPort;
    }

    @Override
    public String toString() {
        return "ForwardablePort ["
                + name + ":" + LOCALHOST + ":" + localPort + " -> " + remoteAddress
                + ":" + remotePort + "]";
    }


    public int startPortForwarding(Session session) {
        if (!isPortforwardingStarted(session)) {
            localPort = doPortForward(session);
        }
        return localPort;
    }


    private int doPortForward(Session session) {
        int startingPort = INITIAL_STARTING_PORT;

        for (int i = 0; i < PORT_ITERATION_RANGE; i++) {
            try {
                return session.setPortForwardingL(startingPort, remoteAddress, remotePort);
            } catch (JSchException e) {
                if (e.getCause() instanceof BindException) {
                    // port already in use, try next port
                    startingPort++;
                } else {
                    throw new RuntimeException("Failed to portforward. Reason: " + e.getMessage(), e);
                }
            }
        }
        throw new RuntimeException("All ports from " + INITIAL_STARTING_PORT + " to " + INITIAL_STARTING_PORT + PORT_ITERATION_RANGE + " are already in use.");
    }

    // TODO copied from applicationportforwarding
    private boolean isPortforwardingStarted(Session session) throws OpenShiftSSHOperationException {
        if (session == null || !session.isConnected()) {
            return false;
        } else if (localPort == -1) {
            return false;
        }
        try {
            // returned format : localPort:remoteHost:remotePort
            final String[] portForwardingL = session.getPortForwardingL();
            final String key = getLocalPort() + ":" + getRemoteAddress() + ":" + getRemotePort();
            Arrays.sort(portForwardingL);
            final int r = Arrays.binarySearch(portForwardingL, key);
            return r >= 0;
        } catch (JSchException e) {
            throw new RuntimeException("Failed to retrieve SSH ports forwarding", e);
        }
    }

}
