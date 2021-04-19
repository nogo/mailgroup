package wtf.ctl.mailgroup.database;

import wtf.ctl.mailgroup.MailGroup;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector implements Closeable {

  private final String url;
  private Connection connection;

  public DatabaseConnector(Path databasePath) {
    this.url = "jdbc:sqlite:" + databasePath.toAbsolutePath();
  }

  public Connection getConnection()  {
    try {
      if (this.connection == null || this.connection.isClosed()) {
        this.connection = DriverManager.getConnection(this.url);
      }
    } catch (SQLException ex) {
      MailGroup.logError(ex);
    }

    return this.connection;
  }

  public void testDatabaseConnection() throws SQLException {
    try (Connection connection = DriverManager.getConnection(this.url)) {
      // Create database if necessary
    }
  }

  @Override
  public void close() throws IOException {
    if (this.connection != null) {
      try {
        this.connection.close();
      } catch (SQLException ex) {
        MailGroup.logError(ex);
      }
    }
  }
}
