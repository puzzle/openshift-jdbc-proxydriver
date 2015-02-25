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