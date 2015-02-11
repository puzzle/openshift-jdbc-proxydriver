package ch.puzzle.openshift.jdbc;

/**
 * Created by bschwaller on 11.02.15.
 */
public class ConnectionWrapper {

    public static Connection wrap(Connection con) {
        return (Connection) Proxy.newProxyInstance(con.getClass().getClassLoader(), new Class[]{Connection.class}, new GenericLoggingHandler(con));
    }
}
