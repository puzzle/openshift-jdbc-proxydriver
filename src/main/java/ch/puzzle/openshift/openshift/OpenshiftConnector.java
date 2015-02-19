package ch.puzzle.openshift.openshift;

import com.openshift.client.ConnectionBuilder;
import com.openshift.client.IOpenShiftConnection;

import java.io.IOException;

public class OpenshiftConnector {

    public IOpenShiftConnection getConnection(String openshiftServerUrl, String openshiftUser, String openshiftPassword) {
        try {
            ConnectionBuilder builder = new ConnectionBuilder(openshiftServerUrl);
            return builder.credentials(openshiftUser, openshiftPassword).create();
        } catch (IOException e) {
            throw new RuntimeException("Could not create connection", e);
        }
    }
}
