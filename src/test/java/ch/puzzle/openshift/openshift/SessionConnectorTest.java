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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SessionConnectorTest {

    private static final String USER = "user";
    private static final String HOST = "host";

    private SessionConnector connector;

    @Mock
    private JSch jSchMock;

    @Mock
    private Session sessionMock;

    @Before
    public void setUp() throws JSchException {
        connector = new SessionConnector();
        connector.setJsch(jSchMock);

        when(jSchMock.getSession(USER, HOST)).thenReturn(sessionMock);
    }


    @Test
    public void getAndConnectSessionShouldUseDefaultPrivateSSHKeyFileWhenParameterIsNull() throws JSchException {
        // given
        String sshUrl = createSshUrl(USER, HOST);
        String keyFile = null;

        // when
        connector.getAndConnectSession(sshUrl, keyFile);

        // then
        verify(jSchMock).addIdentity(SessionConnector.DEFAULT_PRIVATE_SSH_KEY_FILE);
    }

    private String createSshUrl(String user, String host) {
        return SessionConnector.SSH_URL_PREFIX + user + "@" + host;
    }

    @Test
    public void getAndConnectSessionShouldUseSSHKeyFileFromParameter() throws JSchException {
        // given
        String sshUrl = createSshUrl(USER, HOST);
        String keyFile = "keyFile";

        // when
        connector.getAndConnectSession(sshUrl, keyFile);

        // then
        verify(jSchMock).addIdentity(keyFile);
    }

    @Test(expected = RuntimeException.class)
    public void getAndConnectSessionShouldThrowExceptionOnJschAddIdentityFailure() throws JSchException {
        // given
        String sshUrl = createSshUrl(USER, HOST);
        String keyFile = "keyFile";

        doThrow(new JSchException("JSch failure on adding identity key file")).when(jSchMock).addIdentity(keyFile);

        // when
        connector.getAndConnectSession(sshUrl, keyFile);
    }

    @Test
    public void getAndConnectSessionShouldGetSessionForUserAndHost() throws JSchException {
        // given
        String sshUrl = createSshUrl(USER, HOST);
        String keyFile = "keyFile";

        // when
        connector.getAndConnectSession(sshUrl, keyFile);

        // then
        verify(jSchMock).getSession(USER, HOST);
    }

    @Test(expected = RuntimeException.class)
    public void getAndConnectSessionShouldThrowExceptionOnJschGetSessionFailure() throws JSchException {
        // given
        String sshUrl = createSshUrl(USER, HOST);
        String keyFile = "keyFile";

        doThrow(new JSchException("JSch failure on adding identity key file")).when(jSchMock).getSession(USER, HOST);

        // when
        connector.getAndConnectSession(sshUrl, keyFile);
    }

    @Test
    public void getAndConnectSessionShouldSetNoStrictHostKeyCheckingConfig() throws JSchException {
        // given
        String sshUrl = createSshUrl(USER, HOST);
        String keyFile = "keyFile";
        Session sessionMock = mock(Session.class);
        when(jSchMock.getSession(USER, HOST)).thenReturn(sessionMock);

        // when
        connector.getAndConnectSession(sshUrl, keyFile);

        // then
        verify(sessionMock).setConfig("StrictHostKeyChecking", "no");
    }

    @Test
    public void getAndConnectSessionShouldCallConnectOnSession() throws JSchException {
        // given
        String sshUrl = createSshUrl(USER, HOST);
        String keyFile = "keyFile";
        Session sessionMock = mock(Session.class);
        when(jSchMock.getSession(USER, HOST)).thenReturn(sessionMock);

        // when
        connector.getAndConnectSession(sshUrl, keyFile);

        // then
        verify(sessionMock).connect();
    }

    @Test(expected = RuntimeException.class)
    public void getAndConnectSessionShouldThrowExceptionOnSessionConnectFailure() throws JSchException {
        // given
        String sshUrl = createSshUrl(USER, HOST);
        String keyFile = "keyFile";
        Session sessionMock = mock(Session.class);
        when(jSchMock.getSession(USER, HOST)).thenReturn(sessionMock);

        doThrow(new JSchException("Failure on session connect")).when(sessionMock).connect();

        // when
        connector.getAndConnectSession(sshUrl, keyFile);
    }


    @Test(expected = RuntimeException.class)
    public void getAndConnectSessionShouldThrowExceptionWhenSshUrlIsNull() throws JSchException {
        // given
        String keyFile = "keyFile";
        String sshUrl = null;

        // when
        connector.getAndConnectSession(sshUrl, keyFile);

        // then
        verify(jSchMock).getSession(USER, HOST);
    }


    @Test(expected = RuntimeException.class)
    public void getAndConnectSessionShouldThrowExceptionOnInvalidSshUrlMissingAt() throws JSchException {
        // given
        String keyFile = "keyFile";
        String sshUrl = "invalid ssh url because of missing at";

        // when
        connector.getAndConnectSession(sshUrl, keyFile);

        // then
        verify(jSchMock).getSession(USER, HOST);
    }

    @Test(expected = RuntimeException.class)
    public void getAndConnectSessionShouldThrowExceptionOnInvalidSshUrlToManyAt() throws JSchException {
        // given
        String keyFile = "keyFile";
        String sshUrl = "invalid@sshUrl@because of to many @";

        // when
        connector.getAndConnectSession(sshUrl, keyFile);

        // then
        verify(jSchMock).getSession(USER, HOST);
    }

}