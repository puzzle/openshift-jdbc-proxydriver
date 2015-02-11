package ch.puzzle.openshift.openshift;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OpenshiftCommunicationHandlerTest {
    private OpenshiftCommunicationHandler communicator;
    private OpenshiftConnector openshiftConnectorMock;

    @Before
    public void setUp() {
        communicator = new OpenshiftCommunicationHandler();
        openshiftConnectorMock = mock(OpenshiftConnector.class);
        communicator.setOpenshiftConnector(openshiftConnectorMock);
    }

    @Test
    public void connectShouldDelegateConnectCall() {
        // given
        String broker = "broker";
        String user = "user";
        String password = "password";

        // when
        communicator.connect(broker, user, password);

        // then
        verify(openshiftConnectorMock).getConnection(broker, user, password);
    }

//    @Test
//    public void test() {
//        // given
//
//        // when
//
//        // then
//    }

}