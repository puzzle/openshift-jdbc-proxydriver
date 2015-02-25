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

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;
import com.openshift.client.cartridge.IEmbeddedCartridge;
import com.openshift.internal.client.response.CartridgeResourceProperties;
import com.openshift.internal.client.response.CartridgeResourceProperty;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OpenshiftCommunicationHandlerTest {

    private static final String COMMANDLINE_NEWLINE_CHAR = "\n";

    private static final String APPLICATION_NAME = "applicationName";
    private static final String DOMAIN_NAME = "domainName";
    private static final String CONNECTION_URL = "connectionUrl";
    private static final String CARTRIDGE_NAME = "cartridgeName";


    private OpenshiftCommunicationHandler communicator;

    @Mock
    private OpenshiftConnector openshiftConnectorMock;

    @Mock
    private IOpenShiftConnection connectionMock;

    @Mock
    private Session sessionMock;

    @Mock
    private SessionConnector sessionConnectorMock;

    @Before
    public void setUp() {
        communicator = new OpenshiftCommunicationHandler();
        communicator.setOpenshiftConnector(openshiftConnectorMock);
        communicator.setSessionConnector(sessionConnectorMock);
    }

    @Test
    public void connectShouldDelegateConnectCall() {
        // given
        String openshiftServer = "openshiftServer";
        String openshiftUser = "openshiftUser";
        String openshiftPassword = "openshiftPassword";

        // when
        communicator.connect(openshiftServer, openshiftUser, openshiftPassword);

        // then
        verify(openshiftConnectorMock).getConnection(openshiftServer, openshiftUser, openshiftPassword);
    }

    @Test(expected = RuntimeException.class)
    public void startPortForwardingWhenNotConnectedShouldThrowException() {
        // given
        assertFalse(communicator.isConnectedToOpenshiftServer());

        // when
        communicator.startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, CONNECTION_URL, null);
    }

    @Test(expected = RuntimeException.class)
    public void startPortForwardingShouldThrowExceptionWhenConnectionGetUserIsNull() {
        // given
        mockConnectToOpenshift();
        when(connectionMock.getUser()).thenReturn(null);

        // when
        communicator.startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, CONNECTION_URL, null);
    }


    private void mockConnectToOpenshift() {
        String openshiftServer = "openshiftServer";
        String openshiftUser = "openshiftUser";
        String openshiftPassword = "openshiftPassword";
        Mockito.when(openshiftConnectorMock.getConnection(anyString(), anyString(), anyString())).thenReturn(connectionMock);
        communicator.connect(openshiftServer, openshiftUser, openshiftPassword);
    }

    @Test(expected = RuntimeException.class)
    public void startPortForwardingShouldThrowExceptionWhenConnectionGetUserGetDomainIsNull() {
        // given
        mockConnectToOpenshift();
        IUser userMock = mock(IUser.class);
        when(connectionMock.getUser()).thenReturn(userMock);
        when(userMock.getDomain(DOMAIN_NAME)).thenReturn(null);

        // when
        communicator.startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, CONNECTION_URL, null);
    }

    @Test(expected = RuntimeException.class)
    public void startPortForwardingShouldThrowExceptionWhenConnectionGetUserGetDomainGetApplicationIsNull() {
        // given
        mockConnectToOpenshift();
        mockGetApplicationFor(null);

        // when
        communicator.startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, CONNECTION_URL, null);
    }

    @Test
    public void startPortForwardingShouldStartSshSessionToUrl() {
        // given
        mockConnectToOpenshift();
        IApplication applicationMock = mock(IApplication.class);
        mockGetApplicationFor(applicationMock);
        String sshUrl = "sshUrl";

        when(applicationMock.getSshUrl()).thenReturn(sshUrl);

        // when
        startPortForwardingAcceptingMockException(null);

        // then
        verify(sessionConnectorMock).getAndConnectSession(sshUrl, null);
    }

    @Test
    public void startPortForwardingShouldStartSshSessionUsingSshKeyPath() {
        // given
        mockConnectToOpenshift();
        IApplication applicationMock = mock(IApplication.class);
        mockGetApplicationFor(applicationMock);
        String sshKeyPath = "sshKeyFilePath";

        // when
        startPortForwardingAcceptingMockException(sshKeyPath);

        // then
        verify(sessionConnectorMock).getAndConnectSession(anyString(), eq(sshKeyPath));
    }

    private void mockGetApplicationFor(IApplication applicationMock) {
        IUser userMock = mock(IUser.class);
        IDomain domainMock = mock(IDomain.class);
        when(connectionMock.getUser()).thenReturn(userMock);
        when(userMock.getDomain(DOMAIN_NAME)).thenReturn(domainMock);
        when(domainMock.getApplicationByName(APPLICATION_NAME)).thenReturn(applicationMock);
    }

    private void mockGetApplication() {
        mockGetApplicationFor(mock(IApplication.class));
    }

    @Test
    public void startPortForwardingShouldOpenExecChannelOnSession() throws JSchException, IOException {
        // given
        mockConnectToOpenshift();
        mockGetApplication();
        when(sessionConnectorMock.getAndConnectSession(anyString(), anyString())).thenReturn(sessionMock);

        // when
        startPortForwardingAcceptingMockException(null);

        // then
        verify(sessionMock).openChannel("exec");
    }

    @Test
    public void startPortForwardingOnExecuteRhcListPortsCommandShouldInvokeRhcListPortsChannelExecCommand() throws JSchException, IOException {
        // given
        mockConnectToOpenshift();
        mockGetApplication();
        when(sessionConnectorMock.getAndConnectSession(anyString(), anyString())).thenReturn(sessionMock);
        ChannelExec channelMock = mock(ChannelExec.class);
        when(sessionMock.openChannel(anyString())).thenReturn(channelMock);
        ByteArrayInputStream in = new ByteArrayInputStream("".getBytes());
        when(channelMock.getInputStream()).thenReturn(in);

        // when
        startPortForwardingAcceptingMockException(null);

        // then
        verify(channelMock).setCommand(OpenshiftCommunicationHandler.RHC_LIST_PORT_COMMAND);
    }

    @Test
    public void startPortForwardingOnExecuteWakeUpGearCommandShouldInvokeWakeUpChannelExecCommand() throws JSchException, IOException {
        // given
        mockConnectToOpenshift();
        mockGetApplication();
        when(sessionConnectorMock.getAndConnectSession(anyString(), anyString())).thenReturn(sessionMock);
        ChannelExec channelMock = mock(ChannelExec.class);
        when(sessionMock.openChannel(anyString())).thenReturn(channelMock);
        ByteArrayInputStream in = new ByteArrayInputStream("".getBytes());
        when(channelMock.getInputStream()).thenReturn(in);

        // when
        startPortForwardingAcceptingMockException(null);

        // then
        verify(channelMock).setCommand(OpenshiftCommunicationHandler.WAKE_UP_GEAR_COMMAND);
    }


    @Test
    public void startPortForwardingOnExecuteRhcListPortsShouldInvokeChannelExecCommands() throws JSchException, IOException {
        // given
        mockConnectToOpenshift();
        mockGetApplication();
        when(sessionConnectorMock.getAndConnectSession(anyString(), anyString())).thenReturn(sessionMock);
        ChannelExec channelMock = mock(ChannelExec.class);
        when(sessionMock.openChannel(anyString())).thenReturn(channelMock);

        // when
        startPortForwardingAcceptingMockException(null);

        // then
        verify(channelMock).getInputStream();
        verify(channelMock).connect(anyInt());
        verify(channelMock).disconnect();
    }

    @Test
    public void startPortForwardingOnExecuteRhcListPortsShouldDisconnectChannelExecCommandOnFailure() throws JSchException, IOException {
        // given
        mockConnectToOpenshift();
        mockGetApplication();
        when(sessionConnectorMock.getAndConnectSession(anyString(), anyString())).thenReturn(sessionMock);
        ChannelExec channelMock = mock(ChannelExec.class);
        when(sessionMock.openChannel(anyString())).thenReturn(channelMock);
        when(channelMock.getInputStream()).thenThrow(new RuntimeException("Exception on inputstream"));

        // when
        startPortForwardingAcceptingMockException(null);

        // then
        verify(channelMock).disconnect();
    }

    @Test(expected = RuntimeException.class)
    public void startPortForwardingOnExecuteRhcListPortsShouldThrowExceptionOnChannelExecCommandFailure() throws JSchException, IOException {
        // given
        mockConnectToOpenshift();
        mockGetApplication();
        when(sessionConnectorMock.getAndConnectSession(anyString(), anyString())).thenReturn(sessionMock);
        ChannelExec channelMock = mock(ChannelExec.class);
        when(sessionMock.openChannel(anyString())).thenReturn(channelMock);
        when(channelMock.getInputStream()).thenThrow(new RuntimeException("Exception on inputstream"));

        // when
        communicator.startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, CONNECTION_URL, null);
    }

    private void startPortForwardingAcceptingMockException(String sshKeyPath) {
        try {
            communicator.startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, CONNECTION_URL, sshKeyPath);
        } catch (RuntimeException e) {
            // exception thrown because not everything is mocked!
        }
    }

    @Test(expected = RuntimeException.class)
    public void startPortForwardingOnExtractForwardableDatabasePortShouldThrowExceptionOnInvalidCommandlineOutputString() throws JSchException, IOException {
        // given
        mockConnectToOpenshift();
        mockGetApplication();
        mockExecuteRhcListPortCommand("Any arbitary not valid command line output");

        // when
        communicator.startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, CONNECTION_URL, null);
    }

    @Test(expected = RuntimeException.class)
    public void startPortForwardingOnExtractForwardableDatabasePortShouldThrowExceptionOnEmptyCommandlineOutputString() throws JSchException, IOException {
        // given
        mockConnectToOpenshift();
        mockGetApplication();
        mockExecuteRhcListPortCommand("");

        // when
        communicator.startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, CONNECTION_URL, null);
    }

    @Test(expected = RuntimeException.class)
    public void startPortForwardingOnExtractForwardableDatabasePortShouldThrowExceptionOnValidCommandlineOutputStringButNotContainingPortServiceNameInConnectionUrl() throws JSchException, IOException {
        // given
        mockConnectToOpenshift();
        mockGetApplication();
        String portServiceName = "portserviceName";
        String validRhcListPortOutputLine = createValidOutputline(portServiceName, "host", "1234");
        String connectionUrl = "connection URL does not contains port service name";
        assertFalse(connectionUrl.contains(portServiceName));

        mockExecuteRhcListPortCommand(validRhcListPortOutputLine);

        // when
        communicator.startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, connectionUrl, null);
    }

    @Test
    public void startPortForwardingOnExtractForwardableDatabasePortShouldReturnPortForValidCommandlineOutputStringWithPortServiceNameInConnectionUrl() throws JSchException, IOException {
        // given
        mockConnectToOpenshift();
        mockGetApplication();
        String portServiceName = "portserviceName";
        String host = "host";
        int port = 12345;
        String validRhcListPortOutputLine = createValidOutputline(portServiceName, host, String.valueOf(port));
        String connectionUrl = portServiceName + " service name is within the connection URL";
        assertTrue(connectionUrl.contains(portServiceName));

        mockExecuteRhcListPortCommand(validRhcListPortOutputLine);

        // when
        communicator.startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, connectionUrl, null);

        // then
        verify(sessionMock).setPortForwardingL(anyInt(), eq(host), eq(port));
    }

    @Test
    public void startPortForwardingOnExtractForwardableDatabasePortShouldReturnPortForValidCommandlineOutputStringWithPortServiceNameInConnectionUrlAndIgnoreInvalidCommandLine() throws JSchException, IOException {
        // given
        mockConnectToOpenshift();
        mockGetApplication();
        String portServiceName = "portserviceName";
        String host = "host";
        int port = 12345;
        String validRhcListPortOutputLine = createValidOutputline(portServiceName, host, String.valueOf(port));
        String connectionUrl = portServiceName + " service name is within the connection URL";
        assertTrue(connectionUrl.contains(portServiceName));

        mockExecuteRhcListPortCommand("invalid commandline to be ignored", validRhcListPortOutputLine);

        // when
        communicator.startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, connectionUrl, null);

        // then
        verify(sessionMock).setPortForwardingL(anyInt(), eq(host), eq(port));
    }

    @Test
    public void startPortForwardingOnExtractForwardableDatabasePortShouldReturnPortForFirstValidCommandlineOutputStringWithPortServiceNameInConnectionUrl() throws JSchException, IOException {
        // given
        mockConnectToOpenshift();
        mockGetApplication();
        String portServiceName = "portserviceName";
        String firstHost = "host";
        int port = 12345;
        String firstValidRhcListPortOutputLine = createValidOutputline(portServiceName, firstHost, String.valueOf(port));
        String connectionUrl = portServiceName + " service name is within the connection URL";
        assertTrue(connectionUrl.contains(portServiceName));

        String secondHost = "host";
        String secondValidRhcListPortOutputLine = createValidOutputline(portServiceName, secondHost, String.valueOf(port));

        mockExecuteRhcListPortCommand(firstValidRhcListPortOutputLine, secondValidRhcListPortOutputLine);

        // when
        communicator.startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, connectionUrl, null);

        // then
        verify(sessionMock).setPortForwardingL(anyInt(), eq(firstHost), eq(port));
    }

    @Test
    public void startPortForwardingShouldReturnPortNumberReturnedByPortforwarding() throws JSchException, IOException {
        // given
        mockConnectToOpenshift();
        mockGetApplication();

        String portServiceName = "portserviceName";
        String validRhcListPortOutputLine = createValidOutputline(portServiceName, "host", "1234");
        String connectionUrl = portServiceName + " service name is within the connection URL";
        assertTrue(connectionUrl.contains(portServiceName));
        mockExecuteRhcListPortCommand("invalid commandline to be ignored", validRhcListPortOutputLine);

        int localPortReturnedByPortForwardingL = 987654;
        when(sessionMock.setPortForwardingL(anyInt(), anyString(), anyInt())).thenReturn(localPortReturnedByPortForwardingL);

        // when
        final int portForwarding = communicator.startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, connectionUrl, null);

        // then
        assertEquals(localPortReturnedByPortForwardingL, portForwarding);
    }

    @Test(expected = RuntimeException.class)
    public void startPortForwardingShouldThrowExceptionWhenPortforwardingFails() throws JSchException, IOException {
        // given
        mockConnectToOpenshift();
        mockGetApplication();

        String portServiceName = "portserviceName";
        String validRhcListPortOutputLine = createValidOutputline(portServiceName, "host", "1234");
        String connectionUrl = portServiceName + " service name is within the connection URL";
        assertTrue(connectionUrl.contains(portServiceName));
        mockExecuteRhcListPortCommand("invalid commandline to be ignored", validRhcListPortOutputLine);

        when(sessionMock.setPortForwardingL(anyInt(), anyString(), anyInt())).thenThrow(new RuntimeException("Exception thrown by portforwarding"));

        // when
        communicator.startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, connectionUrl, null);

    }

    // TODO add test with keyfile

    private String createValidOutputline(String name, String remoteHost, String remotePortString) {
        return name + " -> " + remoteHost + ":" + remotePortString;
    }


    private void mockExecuteRhcListPortCommand(String... rhcListPortOutputLines) throws JSchException, IOException {
        when(sessionConnectorMock.getAndConnectSession(anyString(), anyString())).thenReturn(sessionMock);
        ChannelExec channelMock = mock(ChannelExec.class);
        when(sessionMock.openChannel(anyString())).thenReturn(channelMock);
        when(channelMock.getInputStream()).thenAnswer(new InputStreamAnswer(rhcListPortOutputLines));
    }


    @Test(expected = RuntimeException.class)
    public void readDatabaseDataShouldThrowExceptionWhenNotConnected() {

        // when
        communicator.readDatabaseData(APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);
    }

    @Test(expected = RuntimeException.class)
    public void readDatabaseDataShouldThrowExceptionWhenConnectedButErrorAccessingApplication() {
        // given
        mockConnectToOpenshift();

        // when
        communicator.readDatabaseData(APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);
    }

    @Test(expected = RuntimeException.class)
    public void readDatabaseDataShouldThrowExceptionWhenConnectedApplicationGetEmbeddedCartridgeIsNull() {
        // given
        mockConnectToOpenshift();
        mockGetEmbeddedCartridgeFor(null);

        // when
        communicator.readDatabaseData(APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);
    }


    @Test
    public void readDatabaseDataShouldReadEmbeddedCartridgeProperties() {
        // given
        mockConnectToOpenshift();
        IEmbeddedCartridge cartridgeMock = mock(IEmbeddedCartridge.class);
        mockGetEmbeddedCartridgeFor(cartridgeMock);

        // when
        readDatabaseDataAcceptingMockException();

        // then
        verify(cartridgeMock).getProperties();
    }

    private void readDatabaseDataAcceptingMockException() {
        try {
            communicator.readDatabaseData(APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);
        } catch (RuntimeException e) {
            // exception thrown because not everything is mocked!
        }
    }

    @Test
    public void readDatabaseDataShouldReadDbUsernameProperty() {
        // given
        mockConnectToOpenshift();
        IEmbeddedCartridge cartridgeMock = mock(IEmbeddedCartridge.class);
        mockGetEmbeddedCartridgeFor(cartridgeMock);

        CartridgeResourceProperties cartridgePropertiesMock = mock(CartridgeResourceProperties.class);
        when(cartridgeMock.getProperties()).thenReturn(cartridgePropertiesMock);
        CartridgeResourceProperty propertyMock = mock(CartridgeResourceProperty.class);
        when(cartridgePropertiesMock.getProperty(anyString())).thenReturn(propertyMock);

        // when
        readDatabaseDataAcceptingMockException();

        // then
        verify(cartridgePropertiesMock).getProperty(OpenshiftCommunicationHandler.USERNAME_KEY);
    }

    @Test
    public void readDatabaseDataShouldReadDbPasswordProperty() {
        // given
        mockConnectToOpenshift();
        IEmbeddedCartridge cartridgeMock = mock(IEmbeddedCartridge.class);
        mockGetEmbeddedCartridgeFor(cartridgeMock);

        CartridgeResourceProperties cartridgePropertiesMock = mock(CartridgeResourceProperties.class);
        when(cartridgeMock.getProperties()).thenReturn(cartridgePropertiesMock);
        CartridgeResourceProperty propertyMock = mock(CartridgeResourceProperty.class);
        when(cartridgePropertiesMock.getProperty(anyString())).thenReturn(propertyMock);

        // when
        readDatabaseDataAcceptingMockException();

        // then
        verify(cartridgePropertiesMock).getProperty(OpenshiftCommunicationHandler.PASSWORD_KEY);
    }

    @Test
    public void readDatabaseDataShouldReadDbConnectionUrlProperty() {
        // given
        mockConnectToOpenshift();
        IEmbeddedCartridge cartridgeMock = mock(IEmbeddedCartridge.class);
        mockGetEmbeddedCartridgeFor(cartridgeMock);

        CartridgeResourceProperties cartridgePropertiesMock = mock(CartridgeResourceProperties.class);
        when(cartridgeMock.getProperties()).thenReturn(cartridgePropertiesMock);
        CartridgeResourceProperty propertyMock = mock(CartridgeResourceProperty.class);
        when(cartridgePropertiesMock.getProperty(anyString())).thenReturn(propertyMock);

        // when
        readDatabaseDataAcceptingMockException();

        // then
        verify(cartridgePropertiesMock).getProperty(OpenshiftCommunicationHandler.CONNECTION_URL_KEY);
    }

    @Test
    public void readDatabaseDataShouldReadDbNameProperty() {
        // given
        mockConnectToOpenshift();
        IEmbeddedCartridge cartridgeMock = mock(IEmbeddedCartridge.class);
        mockGetEmbeddedCartridgeFor(cartridgeMock);

        CartridgeResourceProperties cartridgePropertiesMock = mock(CartridgeResourceProperties.class);
        when(cartridgeMock.getProperties()).thenReturn(cartridgePropertiesMock);
        CartridgeResourceProperty propertyMock = mock(CartridgeResourceProperty.class);
        when(cartridgePropertiesMock.getProperty(anyString())).thenReturn(propertyMock);

        // when
        readDatabaseDataAcceptingMockException();

        // then
        verify(cartridgePropertiesMock).getProperty(OpenshiftCommunicationHandler.DATABASE_NAME_KEY);
    }

    @Test(expected = RuntimeException.class)
    public void readDatabaseDataShouldThrowExceptionPropertyValuesAreNull() {
        // given
        mockConnectToOpenshift();
        IEmbeddedCartridge cartridgeMock = mock(IEmbeddedCartridge.class);
        mockGetEmbeddedCartridgeFor(cartridgeMock);

        CartridgeResourceProperties cartridgePropertiesMock = mock(CartridgeResourceProperties.class);
        when(cartridgeMock.getProperties()).thenReturn(cartridgePropertiesMock);
        CartridgeResourceProperty propertyMock = mock(CartridgeResourceProperty.class);
        when(cartridgePropertiesMock.getProperty(anyString())).thenReturn(propertyMock);
        when(propertyMock.getValue()).thenReturn(null);

        // when
        communicator.readDatabaseData(APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);

        // then
        verify(cartridgePropertiesMock).getProperty(OpenshiftCommunicationHandler.DATABASE_NAME_KEY);
    }

    @Test
    public void readDatabaseDataShouldCreateDatabaseDataWithPropertyValues() {
        // given
        mockConnectToOpenshift();
        IEmbeddedCartridge cartridgeMock = mock(IEmbeddedCartridge.class);
        mockGetEmbeddedCartridgeFor(cartridgeMock);

        CartridgeResourceProperties cartridgePropertiesMock = mock(CartridgeResourceProperties.class);
        when(cartridgeMock.getProperties()).thenReturn(cartridgePropertiesMock);
        CartridgeResourceProperty propertyMock = mock(CartridgeResourceProperty.class);
        when(cartridgePropertiesMock.getProperty(anyString())).thenReturn(propertyMock);

        String propertyValue = "propertyValue";
        when(propertyMock.getValue()).thenReturn(propertyValue);

        // when
        final DatabaseData databaseData = communicator.readDatabaseData(APPLICATION_NAME, DOMAIN_NAME, CARTRIDGE_NAME);

        // then
        assertNotNull(databaseData);
        assertEquals(propertyValue, databaseData.getDbUser());
        assertEquals(propertyValue, databaseData.getDbUserPassword());
        assertEquals(propertyValue, databaseData.getConnectionUrl());
        assertEquals(propertyValue, databaseData.getDatabaseName());
    }


    private void mockGetEmbeddedCartridgeFor(IEmbeddedCartridge cartridgeMock) {
        IApplication applicationMock = mock(IApplication.class);
        mockGetApplicationFor(applicationMock);
        when(applicationMock.getEmbeddedCartridge(CARTRIDGE_NAME)).thenReturn(cartridgeMock);
    }

    @Test
    public void disconnectShouldDoNothingWhenNoSessionActive() {
        //when
        communicator.disconnect();

        // then
        verifyZeroInteractions(sessionMock, connectionMock);
    }

    @Test
    public void disconnectShouldDisconnectSessionWhenActive() throws IOException, JSchException {
        // given
        connectAndStartPortForwarding();

        //when
        communicator.disconnect();

        // then
        verify(sessionMock).disconnect();
    }

    private void connectAndStartPortForwarding() throws IOException, JSchException {
        mockConnectToOpenshift();
        mockGetApplication();
        when(sessionConnectorMock.getAndConnectSession(anyString(), anyString())).thenReturn(sessionMock);

        String portServiceName = "portserviceName";
        String validRhcListPortOutputLine = createValidOutputline(portServiceName, "host", "1234");
        String connectionUrl = portServiceName + " service name is within the connection URL";
        assertTrue(connectionUrl.contains(portServiceName));
        mockExecuteRhcListPortCommand(validRhcListPortOutputLine);

        int localPortReturnedByPortForwardingL = 987654;
        when(sessionMock.setPortForwardingL(anyInt(), anyString(), anyInt())).thenReturn(localPortReturnedByPortForwardingL);

        communicator.startPortForwarding(APPLICATION_NAME, DOMAIN_NAME, connectionUrl, null);
    }

    private class InputStreamAnswer implements Answer<InputStream> {
        String rhcListPortOutputLine;

        public InputStreamAnswer(String[] rhcListPortOutputLines) {
            this.rhcListPortOutputLine = createOutputLine(rhcListPortOutputLines);
        }

        @Override
        public InputStream answer(InvocationOnMock invocationOnMock) throws Throwable {
            return new ByteArrayInputStream(rhcListPortOutputLine.getBytes());
        }

        private String createOutputLine(String[] rhcListPortOutputLines) {
            String rhcListPortOutputLine = "";
            for (String line : rhcListPortOutputLines) {
                rhcListPortOutputLine += line + COMMANDLINE_NEWLINE_CHAR;
            }
            if (rhcListPortOutputLine.endsWith(COMMANDLINE_NEWLINE_CHAR)) {
                rhcListPortOutputLine = rhcListPortOutputLine.substring(0, rhcListPortOutputLine.length() - COMMANDLINE_NEWLINE_CHAR.length());
            }
            return rhcListPortOutputLine;
        }

    }

}