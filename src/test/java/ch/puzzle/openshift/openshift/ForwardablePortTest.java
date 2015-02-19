package ch.puzzle.openshift.openshift;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.BindException;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ForwardablePortTest {

    private static final String PORT_NAME = "name";
    private static final String REMOTE_HOST = "remoteHost";
    private static final int REMOTE_PORT = 5432;

    private static final int FORWARDED_PORT = 123456;

    private ForwardablePort forwardablePort;

    @Mock
    private Session sessionMock;

    @Test
    public void createForValidRhcListPortsOutputLineOnValidOutputLineShouldCreateForwardablePort() throws Exception {
        // given
        String validOutputLine = createOutputline("name", "remoteHost", "5432");
        // when
        forwardablePort = ForwardablePort.createForValidRhcListPortsOutputLine(validOutputLine);

        // then
        assertNotNull(forwardablePort);
    }

    @Test(expected = RuntimeException.class)
    public void createForValidRhcListPortsOutputLineOnNullOutputLineShouldThrowException() throws Exception {
        // given
        String validOutputLine = null;
        // when
        forwardablePort = ForwardablePort.createForValidRhcListPortsOutputLine(validOutputLine);
    }

    @Test
    public void createForValidRhcListPortsOutputLineOnInValidOutputLineShouldReturnNull() throws Exception {
        // given
        String validOutputLine = "Any invalid outputline";
        // when
        forwardablePort = ForwardablePort.createForValidRhcListPortsOutputLine(validOutputLine);

        // then
        assertNull(forwardablePort);
    }

    @Test
    public void createForValidRhcListPortsOutputLineOnInValidPortOnOutputLineShouldReturnNull() throws Exception {
        // given
        String validOutputLine = createOutputline("name", "remoteHost", "invalidPort");
        // when
        forwardablePort = ForwardablePort.createForValidRhcListPortsOutputLine(validOutputLine);

        // then
        assertNull(forwardablePort);
    }


    @Test
    public void getNameShouldReturnForwardablePortName() throws Exception {
        // given
        forwardablePort = createForwardablePort();

        // when
        final String forwardablePortName = forwardablePort.getName();

        // then
        assertEquals(PORT_NAME, forwardablePortName);
    }

    @Test
    public void getRemoteHostShouldReturnForwardablePortHost() throws Exception {
        // given
        forwardablePort = createForwardablePort();

        // when
        final String forwardablePortRemoteHost = forwardablePort.getRemoteHost();

        // then
        assertEquals(REMOTE_HOST, forwardablePortRemoteHost);
    }

    @Test
    public void getRemotePortShouldReturnForwardablePortRemotePort() throws Exception {
        // given
        forwardablePort = createForwardablePort();

        // when
        final int forwardablePortRemotePort = forwardablePort.getRemotePort();

        // then
        assertEquals(REMOTE_PORT, forwardablePortRemotePort);
    }

    @Test
    public void getLocalPortOnNotStartedPortForwardingShouldReturnMinusOne() throws Exception {
        // given
        forwardablePort = createForwardablePort();
        assertFalse(forwardablePort.isPortforwardingStarted(sessionMock));

        // when
        final int forwardablePortLocalPort = forwardablePort.getLocalPort();

        // then
        assertEquals(-1, forwardablePortLocalPort);
    }

    @Test
    public void isPortforwardingStartedShouldReturnFalseWhenNoForwardingIsPreviouslyStarted() throws Exception {
        // given
        forwardablePort = createForwardablePort();

        // when
        final boolean isPortForwardingStarded = forwardablePort.isPortforwardingStarted(sessionMock);

        // then
        assertFalse(isPortForwardingStarded);
        Mockito.verifyZeroInteractions(sessionMock);
    }

    @Test
    public void isPortforwardingStartedShouldReturnFalseWhenSessionIsNull() throws Exception {
        // given
        forwardablePort = createForwardablePort();
        mockStartPortForwardingTo(FORWARDED_PORT);
        sessionMock = null;

        // when
        final boolean isPortForwardingStarded = forwardablePort.isPortforwardingStarted(sessionMock);

        // then
        assertFalse(isPortForwardingStarded);
    }

    private void mockStartPortForwardingTo(int forwardedPort) throws JSchException {
        Mockito.when(sessionMock.setPortForwardingL(ForwardablePort.INITIAL_STARTING_PORT, REMOTE_HOST, REMOTE_PORT)).thenReturn(forwardedPort);
        forwardablePort.startPortForwarding(sessionMock);
    }

    @Test
    public void isPortforwardingStartedShouldReturnFalseWhenSessionIsNoMoreConnected() throws Exception {
        // given
        forwardablePort = createForwardablePort();
        mockStartPortForwardingTo(FORWARDED_PORT);
        Mockito.when(sessionMock.isConnected()).thenReturn(false);

        // when
        final boolean isPortForwardingStarded = forwardablePort.isPortforwardingStarted(sessionMock);

        // then
        assertFalse(isPortForwardingStarded);
    }

    @Test(expected = RuntimeException.class)
    public void isPortforwardingStartedShouldThrowExceptionOnSessionGetPortForwardingLFailure() throws Exception {
        // given
        forwardablePort = createForwardablePort();
        mockStartPortForwardingTo(FORWARDED_PORT);
        Mockito.when(sessionMock.isConnected()).thenReturn(true);
        Mockito.when(sessionMock.getPortForwardingL()).thenThrow(new JSchException("Exception occurred on getPortForwardingL call"));

        // when
        final boolean isPortForwardingStarded = forwardablePort.isPortforwardingStarted(sessionMock);
    }

    @Test
    public void isPortforwardingStartedShouldReturnFalseWhenPortIsNotInSessionPortForwardingLList() throws Exception {
        // given
        forwardablePort = createForwardablePort();
        mockStartPortForwardingTo(FORWARDED_PORT);
        Mockito.when(sessionMock.isConnected()).thenReturn(true);
        Mockito.when(sessionMock.getPortForwardingL()).thenReturn(new String[0]);

        // when
        final boolean isPortForwardingStarded = forwardablePort.isPortforwardingStarted(sessionMock);

        // then
        assertFalse(isPortForwardingStarded);
    }

    @Test
    public void isPortforwardingStartedShouldReturnTrueWhenPortIsInSessionPortForwardingLList() throws Exception {
        // given
        forwardablePort = createForwardablePort();
        mockStartPortForwardingTo(FORWARDED_PORT);
        Mockito.when(sessionMock.isConnected()).thenReturn(true);
        Mockito.when(sessionMock.getPortForwardingL()).thenReturn(new String[]{FORWARDED_PORT + ":" + REMOTE_HOST + ":" + REMOTE_PORT});

        // when
        final boolean isPortForwardingStarded = forwardablePort.isPortforwardingStarted(sessionMock);

        // then
        assertTrue(isPortForwardingStarded);
    }


    @Test
    public void startPortForwardingWhenNotYetStartedShouldCallPortForwardOnSession() throws Exception {
        // given
        forwardablePort = createForwardablePort();

        // when
        forwardablePort.startPortForwarding(sessionMock);

        // then
        Mockito.verify(sessionMock).setPortForwardingL(ForwardablePort.INITIAL_STARTING_PORT, REMOTE_HOST, REMOTE_PORT);
    }


    @Test(expected = RuntimeException.class)
    public void startPortForwardingWhenNotYetStartedShouldThrowExceptionOnSessionPortForwardFailure() throws Exception {
        // given
        forwardablePort = createForwardablePort();
        int forwardingPort = ForwardablePort.INITIAL_STARTING_PORT;
        Mockito.when(sessionMock.setPortForwardingL(forwardingPort, REMOTE_HOST, REMOTE_PORT)).thenThrow(new JSchException("Portforwarding exception", new RuntimeException("Some exception other than bindingexception")));

        // when
        forwardablePort.startPortForwarding(sessionMock);
    }

    @Test
    public void startPortForwardingWhenNotYetStartedShouldRetryPortForwardingWithIncrementedPortnumberWhenPortIsAlreadyInUse() throws Exception {
        // given
        forwardablePort = createForwardablePort();
        int forwardingPort = ForwardablePort.INITIAL_STARTING_PORT;
        Mockito.when(sessionMock.setPortForwardingL(forwardingPort, REMOTE_HOST, REMOTE_PORT)).thenThrow(new JSchException("Portforwarding exception", new BindException("Port allready in use")));

        // when
        forwardablePort.startPortForwarding(sessionMock);

        // then
        Mockito.verify(sessionMock).setPortForwardingL((forwardingPort + 1), REMOTE_HOST, REMOTE_PORT);
    }

    @Test(expected = RuntimeException.class)
    public void startPortForwardingWhenNotYetStartedShouldThrowExceptionWhenAllPortsAreAlreadyInUse() throws Exception {
        // given
        forwardablePort = createForwardablePort();
        int forwardingPort = ForwardablePort.INITIAL_STARTING_PORT;
        for (; forwardingPort < ForwardablePort.INITIAL_STARTING_PORT + ForwardablePort.PORT_ITERATION_RANGE; forwardingPort++) {
            Mockito.when(sessionMock.setPortForwardingL(forwardingPort, REMOTE_HOST, REMOTE_PORT)).thenThrow(new JSchException("Portforwarding exception", new BindException("Port allready in use")));
        }

        // when
        forwardablePort.startPortForwarding(sessionMock);
    }


    @Test
    public void stopPortForwardingOnStartedPortForwardingShouldCallDelPortForwardingOnSession() throws Exception {
        // given
        forwardablePort = createForwardablePort();
        mockStartPortForwardingTo(FORWARDED_PORT);
        Mockito.when(sessionMock.isConnected()).thenReturn(true);
        Mockito.when(sessionMock.getPortForwardingL()).thenReturn(new String[]{FORWARDED_PORT + ":" + REMOTE_HOST + ":" + REMOTE_PORT});

        // when
        forwardablePort.stopPortForwarding(sessionMock);

        // then
        Mockito.verify(sessionMock).delPortForwardingL(FORWARDED_PORT);
    }

    @Test
    public void stopPortForwardingOnStartedPortForwardingShouldResetLocalPortToMinusOne() throws Exception {
        // given
        forwardablePort = createForwardablePort();
        mockStartPortForwardingTo(FORWARDED_PORT);
        Mockito.when(sessionMock.isConnected()).thenReturn(true);
        Mockito.when(sessionMock.getPortForwardingL()).thenReturn(new String[]{FORWARDED_PORT + ":" + REMOTE_HOST + ":" + REMOTE_PORT});

        // when
        forwardablePort.stopPortForwarding(sessionMock);

        // then
        final int localPort = forwardablePort.getLocalPort();
        assertEquals(-1, localPort);
    }

    @Test(expected = RuntimeException.class)
    public void stopPortForwardingOnStartedPortForwardingShouldThrowExceptionWhenDelPortForwardingLFails() throws Exception {
        // given
        forwardablePort = createForwardablePort();
        mockStartPortForwardingTo(FORWARDED_PORT);
        Mockito.when(sessionMock.isConnected()).thenReturn(true);
        Mockito.when(sessionMock.getPortForwardingL()).thenReturn(new String[]{FORWARDED_PORT + ":" + REMOTE_HOST + ":" + REMOTE_PORT});

        Mockito.doThrow(new JSchException("")).when(sessionMock).delPortForwardingL(FORWARDED_PORT);

        // when
        forwardablePort.stopPortForwarding(sessionMock);
    }


    private ForwardablePort createForwardablePort() {
        return createForwardablePortFor(PORT_NAME, REMOTE_HOST, REMOTE_PORT);
    }

    private ForwardablePort createForwardablePortFor(String name, String remoteHost, int remotePort) {
        return ForwardablePort.createForValidRhcListPortsOutputLine(createOutputline(name, remoteHost, String.valueOf(remotePort)));
    }

    private String createOutputline(String name, String remoteHost, String remotePortString) {
        return name + " -> " + remoteHost + ":" + remotePortString;
    }
}