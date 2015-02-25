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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 * Created by bschwaller on 11.02.15.
 */
public class ConnectionWrapper {
    private final OpenshiftProxyDriver driver;

    public ConnectionWrapper(OpenshiftProxyDriver driver) {
        this.driver = Objects.requireNonNull(driver, "Driver must not be null");
    }

    /**
     * Tries to establish a (wrapped) connection to the registered driver
     *
     * @see java.sql.DriverManager#getConnection(String, java.util.Properties)
     */
    public Connection wrap(String url, Properties info) throws SQLException {
        final Connection connection = wrapConnection(Objects.requireNonNull(url, "URL must not be null"), Objects.requireNonNull(info, "Properties must not be null"));
        return new ProxyDriverConnection(driver, connection);
    }

    private Connection wrapConnection(String url, Properties info) throws SQLException {
        return DriverManager.getConnection(url, info);
    }


}
