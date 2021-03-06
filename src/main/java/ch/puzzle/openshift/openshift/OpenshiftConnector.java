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

import com.openshift.client.ConnectionBuilder;
import com.openshift.client.IOpenShiftConnection;

public class OpenshiftConnector {
    private static final int OPENSHIFT_CONNECTION_TIMEOUT = 90_000;

    public IOpenShiftConnection getConnection(String openshiftServerUrl, String openshiftUser, String openshiftPassword) {
        try {
            ConnectionBuilder builder = new ConnectionBuilder(openshiftServerUrl);
            return builder.credentials(openshiftUser, openshiftPassword).timeout(OPENSHIFT_CONNECTION_TIMEOUT).create();
        } catch (Exception e) {
            throw new RuntimeException("Could not create connection to openshift server", e);
        }
    }
}
