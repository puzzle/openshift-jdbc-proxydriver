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

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by bschwaller on 28.02.15.
 */
public class TestDriverWithUserAndPasswordAndOtherProperties extends TestDriverWithoutProperties {
    
    public static final String USER = "testUser";
    public static final String PASSWORD = "testPassword";
    public static final String OTHER_DRIVER_PROPERTY = "otherDriverProperty";

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        List<DriverPropertyInfo> driverPropertyInfos = new ArrayList<>();

        DriverPropertyInfo driverpropertyinfo = new DriverPropertyInfo(OpenshiftProxyDriver.USER_PROPERTY_KEY, USER);
        driverpropertyinfo.description = "Openshift user";
        driverpropertyinfo.required = true;
        driverPropertyInfos.add(driverpropertyinfo);

        driverpropertyinfo = new DriverPropertyInfo(OpenshiftProxyDriver.PASSWORD_PROPERTY_KEY, PASSWORD);
        driverpropertyinfo.description = "Openshift password";
        driverpropertyinfo.required = true;
        driverPropertyInfos.add(driverpropertyinfo);

        driverpropertyinfo = new DriverPropertyInfo(OTHER_DRIVER_PROPERTY, OTHER_DRIVER_PROPERTY);
        driverPropertyInfos.add(driverpropertyinfo);

        return driverPropertyInfos.toArray(new DriverPropertyInfo[driverPropertyInfos.size()]);
    }
}

