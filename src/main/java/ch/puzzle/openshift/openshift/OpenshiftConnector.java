package ch.puzzle.openshift.openshift;

import com.openshift.client.ConnectionBuilder;
import com.openshift.client.IOpenShiftConnection;

import java.io.IOException;

public class OpenshiftConnector {

    public IOpenShiftConnection getConnection(String brokerUrl, String openshiftUser, String openshiftPassword) {
        ConnectionBuilder builder = null;
        try {
            builder = new com.openshift.client.ConnectionBuilder(brokerUrl);
            final IOpenShiftConnection iOpenShiftConnection = builder.credentials(openshiftUser, openshiftPassword).create();
            return iOpenShiftConnection;
        } catch (IOException e) {
            throw new RuntimeException("Could not create connection", e);
        }
    }
}
