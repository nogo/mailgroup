package wtf.ctl.mailgroup.database;

import wtf.ctl.mailgroup.MailGroup;

import java.sql.*;
import java.util.Optional;
import java.util.function.Supplier;

public class DbMessageRepository {

  private final Supplier<Connection> connectionSupplier;

  public DbMessageRepository(Supplier<Connection> connectionSupplier) {
    this.connectionSupplier = connectionSupplier;
  }

  public Optional<DbMessage> persist(DbMessage dbMessage) {
    if (dbMessage == null) return Optional.empty();

    try (PreparedStatement stmt = this.connectionSupplier.get().prepareStatement(
      "INSERT INTO messages (list_name, message_uid, message_date, message_from, subject, plain, html) VALUES (?, ?, ?, ?, ?, ?, ?)",
      Statement.RETURN_GENERATED_KEYS
    )) {
      stmt.setString(1, dbMessage.getListName());
      stmt.setString(2, dbMessage.getMessageUid());
      stmt.setTimestamp(3, Timestamp.valueOf(dbMessage.getMessageDate()));
      stmt.setString(4, dbMessage.getFrom());
      stmt.setString(5, dbMessage.getSubject());
      stmt.setString(6, dbMessage.getPlainBody());
      stmt.setString(7, dbMessage.getHtmlBody());
      stmt.execute();

      ResultSet generatedKeys = stmt.getGeneratedKeys();
      if (generatedKeys.next()) {
        return Optional.of(new DbMessage(
          generatedKeys.getInt(1),
          dbMessage.getListName(),
          dbMessage.getMessageUid(),
          dbMessage.getMessageDate(),
          dbMessage.getFrom(),
          dbMessage.getSubject(),
          dbMessage.getPlainBody(),
          dbMessage.getHtmlBody()
        ));
      } else {
        return Optional.of(dbMessage);
      }
    } catch (SQLException ex) {
      MailGroup.logError(ex);
      return Optional.empty();
    }
  }

  public Optional<DbMessage> find(int id) {
    try (PreparedStatement stmt = this.connectionSupplier.get().prepareStatement("SELECT * FROM messages WHERE id = ?")) {
      stmt.setInt(1, id);
      try (ResultSet resultSet = stmt.executeQuery()) {
        resultSet.next();
        return Optional.of(createDbMessageFromResultSet(resultSet));
      }
    } catch (SQLException ex) {
      MailGroup.logError(ex);
      return Optional.empty();
    }
  }

  public Optional<DbMessage> find(String messageUid) {
    if (messageUid == null || messageUid.isBlank()) return Optional.empty();

    try (PreparedStatement stmt = this.connectionSupplier.get().prepareStatement("SELECT * FROM messages WHERE message_uid = ?")) {
      stmt.setString(1, messageUid);
      try (ResultSet resultSet = stmt.executeQuery()) {
        resultSet.next();
        return Optional.of(createDbMessageFromResultSet(resultSet));
      }
    } catch (SQLException ex) {
      MailGroup.logError(ex);
      return Optional.empty();
    }
  }

  private DbMessage createDbMessageFromResultSet(ResultSet resultSet) throws SQLException {
    return new DbMessage(
      resultSet.getInt("id"),
      resultSet.getString("list_name"),
      resultSet.getString("message_uid"),
      resultSet.getTimestamp("message_date").toLocalDateTime(),
      resultSet.getString("message_from"),
      resultSet.getString("subject"),
      resultSet.getString("plain"),
      resultSet.getString("html")
    );
  }
}
