package ch.puzzle.openshift.openshift;

/**
 * Created by bschwaller on 11.02.15.
 */
public class ForwardablePort {

    public boolean isPortFowardingStarted() throws OpenShiftSSHOperationException {
        try {
            return isConnected()
                    && session.getPortForwardingL().length > 0;
        } catch (JSchException e) {
            throw new OpenShiftSSHOperationException(e,
                    "Unable to verify if port-forwarding has been started for application \"{0}\"",
                    application.getName());
        }
    }

    /**
     * Start forwarding available ports to this application
     *
     * @return Current list of ports
     * @throws OpenShiftSSHOperationException
     */
    public List<IApplicationPortForwarding> startPortForwarding() throws OpenShiftSSHOperationException {
        assertLiveSSHSession();

        for (IApplicationPortForwarding port : ports) {
            try {
                port.start(session);
            } catch (OpenShiftSSHOperationException oss) {
                /*
                 * ignore for now FIXME: should store this error on the forward
				 * to let user know why it could not start/stop
				 */
            }
        }
        return ports;
    }

    public boolean isStarted(final Session session) throws OpenShiftSSHOperationException {
        if (session == null || !session.isConnected()) {
            return false;
        }
        try {
            // returned format : localPort:remoteHost:remotePort
            final String[] portForwardingL = session.getPortForwardingL();
            final String key = this.localPort + ":" + this.remoteAddress + ":" + this.remotePort;
            Arrays.sort(portForwardingL);
            final int r = Arrays.binarySearch(portForwardingL, key);
            return r >= 0;
        } catch (JSchException e) {
            throw new OpenShiftSSHOperationException(e, "Failed to retrieve SSH ports forwarding");
        }
    }
}

}
