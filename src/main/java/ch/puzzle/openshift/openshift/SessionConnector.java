package ch.puzzle.openshift.openshift;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Created by bschwaller on 18.02.15.
 */
public class SessionConnector {

    static final String SSH_URL_PREFIX = "ssh://";
    static final String DEFAULT_SSH_KEY_FILE = "~/.ssh/id_rsa";

    private JSch jsch;

    public SessionConnector() {
        this.jsch = new JSch();
    }

    public Session getAndConnectSession(String sshUrl, String keyFile) {
        try {
            String useKeyFile = keyFile != null ? keyFile : DEFAULT_SSH_KEY_FILE;
            Path tempFile = Paths.get(useKeyFile);
            File file = tempFile.toFile();
            jsch.addIdentity(file.getPath());

            String[] userHost = extractApplicationUserAndHost(sshUrl);
            String applicationUser = userHost[0];
            String applicationHost = userHost[1];

            Session session = jsch.getSession(applicationUser, applicationHost);
            session.setConfig("StrictHostKeyChecking", "no");

            session.connect();
            return session;
        } catch (JSchException | NullPointerException e) {
            throw new RuntimeException("Could not open session", e);
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

    void setJsch(JSch jsch) {
        this.jsch = jsch;
    }
}