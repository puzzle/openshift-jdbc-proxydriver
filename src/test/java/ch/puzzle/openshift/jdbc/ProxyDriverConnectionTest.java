package ch.puzzle.openshift.jdbc;

import com.mysql.jdbc.Connection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.SQLException;

@RunWith(MockitoJUnitRunner.class)
public class ProxyDriverConnectionTest {

    private ProxyDriverConnection proxyDriverConnection;

    @Mock
    private OpenshiftProxyDriver proxyDriverMock;
    @Mock
    private Connection wrappedConnectionMock;

    @Before
    public void setUp() {
        proxyDriverConnection = new ProxyDriverConnection(proxyDriverMock, wrappedConnectionMock);
    }

    @Test
    public void onCloseShouldCallCloseOnDriver() throws SQLException {

        // when
        proxyDriverConnection.close();

        //then
        Mockito.verify(proxyDriverMock).close();
        Mockito.verify(wrappedConnectionMock).close();
    }

    @Test
    public void onCloseShouldDelegateCloseOnWrappedConnection() throws SQLException {

        // when
        proxyDriverConnection.close();

        //then
        Mockito.verify(wrappedConnectionMock).close();
    }

}