package ch.puzzle.openshift.openshift;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by bschwaller on 11.02.15.
 */
public class OpenshiftCommunicationHandler {

    protected static class SshCommandResponse {

        private InputStream inputStream;

        SshCommandResponse(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public List<String> getLines() throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            List<String> lines = new ArrayList<String>();
            String line = null;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        }
    }

    /** Regex for port forwarding */
    private static final Pattern REGEX_FORWARDED_PORT = Pattern.compile("([^ ]+) -> ([^:]+):(\\d+)");

    private IApplication application;

    RhcListPortsCommandResponse(IApplication application, InputStream inputStream) {
        super(inputStream);
        this.application = application;
    }

    public List<IApplicationPortForwarding> getPortForwardings() throws IOException {
        List<IApplicationPortForwarding> ports = new ArrayList<IApplicationPortForwarding>();
        for (String line : getLines()) {
            ApplicationPortForwarding port = extractForwardablePortFrom(line);
            if (port != null) {
                ports.add(port);
            }
        }
        return ports;
    }
}
