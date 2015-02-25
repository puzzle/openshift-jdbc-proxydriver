/*
 * Copyright 2015 Puzzle ITC GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.puzzle.openshift.openshift;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.util.Objects;

/**
 * Created by bschwaller on 18.02.15.
 */
public class SessionConnector {

    static final String SSH_URL_PREFIX = "ssh://";
    static final String DEFAULT_PRIVATE_SSH_KEY_FILE = "~/.ssh/id_rsa";

    private JSch jsch;

    public SessionConnector() {
        this.jsch = new JSch();
    }

    public Session getAndConnectSession(String sshUrl, String privateSshKeyFilePath) {
        try {
            String usedPrivateSshKeyFilePath = privateSshKeyFilePath != null ? privateSshKeyFilePath : DEFAULT_PRIVATE_SSH_KEY_FILE;
            jsch.addIdentity(usedPrivateSshKeyFilePath);

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
