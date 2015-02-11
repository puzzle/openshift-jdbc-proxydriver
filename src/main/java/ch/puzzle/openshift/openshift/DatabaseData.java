package ch.puzzle.openshift.openshift;


import java.util.Objects;

public class DatabaseData {

    private final String dbUser;
    private final String dbUserPassword;
    private final String connectionUrl;
    private final String databaseName;

    public DatabaseData(String dbUser, String dbUserPassword, String connectionUrl, String databaseName) {
        this.dbUser = Objects.requireNonNull(dbUser, "dbUser must not be null!");
        this.dbUserPassword = Objects.requireNonNull(dbUserPassword, "dbUserPassword must not be null!");
        this.connectionUrl = Objects.requireNonNull(connectionUrl, "connectionUrl must not be null!");
        this.databaseName = Objects.requireNonNull(databaseName, "databaseName must not be null!");
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbUserPassword() {
        return dbUserPassword;
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public String getDatabaseName() {
        return databaseName;
    }
}
