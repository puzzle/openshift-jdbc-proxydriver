package ch.puzzle.openshift.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by bschwaller on 11.02.15.
 */
public class ConnectionWrapper {
    public Connection wrap(String url, Properties info) throws SQLException {
        return DriverManager.getConnection(url, info);
    }
}
