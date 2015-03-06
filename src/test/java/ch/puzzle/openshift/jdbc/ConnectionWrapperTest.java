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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionWrapperTest {


    private String protocol = "protocol_supported_by_driver";

    private ConnectionWrapper wrapper;
    private TestDriverStub testDriver;

    @Mock
    private OpenshiftProxyDriver driverMock;

    @Mock
    private Connection connectionMock;

    @Before
    public void setUp() throws SQLException {
        testDriver = new TestDriverStub(protocol, connectionMock);
        DriverManager.registerDriver(testDriver);

        wrapper = new ConnectionWrapper(driverMock);
    }

    @After
    public void tearDown() throws SQLException {
        if (testDriver != null) {
            DriverManager.deregisterDriver(testDriver);
        }
    }

    @Test
    public void wrapShouldReturnWrappedConnectionFromDriverWithMatchingUrl() throws SQLException {
        // given
        String cunnectionUrlWithAcceptedProtocol = protocol;

        // when
        final Connection wrappedConnection = wrapper.wrap(cunnectionUrlWithAcceptedProtocol, new Properties());

        // then
        assertNotNull(wrappedConnection);
        assertTrue(wrappedConnection instanceof ProxyDriverConnection);
    }

    @Test(expected = SQLException.class)
    public void wrapShouldThrowExceptionWhenNoSuitableDriverCanBeFound() throws SQLException {
        // given
        String cunnectionUrlWithUnknownProtocol = "Unknown protocol not supported by any driver";

        // when
        wrapper.wrap(cunnectionUrlWithUnknownProtocol, new Properties());
    }

    @Test(expected = NullPointerException.class)
    public void wrapShouldThrowExceptionWhenCalledWithNullURL() throws SQLException {
        // given
        String cunnectionUrlWithUnknownProtocol = null;

        // when
        wrapper.wrap(cunnectionUrlWithUnknownProtocol, new Properties());
    }

    @Test(expected = NullPointerException.class)
    public void wrapShouldThrowExceptionWhenCalledWithNullProperties() throws SQLException {
        // given
        String cunnectionUrlWithUnknownProtocol = "anyUrl";
        Properties properties = null;

        // when
        wrapper.wrap(cunnectionUrlWithUnknownProtocol, properties);
    }
}