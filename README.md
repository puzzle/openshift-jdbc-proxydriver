OpenShift JDBC Proxy Driver
===========================

JDBC Proxy Driver for OpenShift

This driver proxy reads the db connection parameter from the applications database cartridge starts portforwarding and opens a connection to the database. 

Usage
-----
Database client arguments:

Proxy driver: 
jdbc-proxydriver-[version]-jar-with-dependencies.jar

Driver class:
ch.puzzle.openshift.jdbc.OpenshiftProxyDriver

Connection URL:
jdbc:openshiftproxy://[serverURL]/[app]?domain=[domain]&cartridge=[cartridge]&driver=[driver]
* serverURL: openshift server
* app: application name
* domain: application domain name
* cartridge: database cartridge name. (ex. postgresql-9.2)
* driver: driver class for database type (ex. org.postgresql.Driver)
* using the optional argument: &externalforwardedport=[Port] will try to connect to the given port. In this case the proxy driver does not do any port forwarding!

Mandatory properties:

* User: OpenShift Online user account.
* Password: OpenShift Online account password.

Optional properties:
* privateSshKeyFilePath: Absolute file path of private ssh key
If this property is not set then the key stored under "~/.ssh/id_rsa" is used by default

Prerequisite
------------
You will need to have an openshift user account and uploaded valid ssh keys.

Currently supported database driver
-------------------------
* Postgresql: org.postgresql:postgresql:9.3-1102-jdbc4
* MySql: mysql:mysql-connector-java:5.1.9


Development/Testrunner
----------------------
For runing the proxy driver use "ch.puzzle.openshift.jdbc.DriverTestRunner" and define vm arguments -DopenshiftUser=<openshiftuser> -DopenshiftUserPassword=<password> within the runconfiguration

