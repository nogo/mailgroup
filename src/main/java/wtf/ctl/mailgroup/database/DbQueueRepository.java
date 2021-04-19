package wtf.ctl.mailgroup.database;

import wtf.ctl.mailgroup.MailGroup;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class DbQueueRepository {

  private final Supplier<Connection> connectionSupplier;
  private DbMessageRepository dbMessageRepository;

  public DbQueueRepository(Supplier<Connection> connectionSupplier) {
    this.connectionSupplier = connectionSupplier;
    this.dbMessageRepository = new DbMessageRepository(connectionSupplier);
  }

  public DbQueue persist(DbQueue queueItem) {
    if (queueItem == null || queueItem.getMessage() == null) return null;

    try (PreparedStatement stmt = this.connectionSupplier.get().prepareStatement(
      "INSERT INTO queue (message_id, send_to, sent) VALUES (?, ?, ?)",
      Statement.RETURN_GENERATED_KEYS
    )) {
      stmt.setInt(1, queueItem.getMessage().getId());
      stmt.setString(2, queueItem.getSendTo());
      stmt.setBoolean(3, queueItem.isSent());
      stmt.execute();

      ResultSet generatedKeys = stmt.getGeneratedKeys();
      if (generatedKeys.next()) {
        return new DbQueue(
          generatedKeys.getInt(1),
          queueItem.getSendTo(),
          queueItem.isSent(),
          queueItem.getMessage()
        );
      } else {
        return queueItem;
      }
    } catch (SQLException ex) {
      MailGroup.logError(ex);
      return null;
    }
  }

  public List<DbQueue> findAllToSend() {
    List<DbQueue> result = new ArrayList<>();
    try (Statement stmt = this.connectionSupplier.get().createStatement()) {
      try (ResultSet resultSet = stmt.executeQuery("SELECT id, message_id, send_to, sent FROM queue WHERE sent = false")) {        Map<Integer, DbMessage> messages = new HashMap<>();
        while (resultSet.next()) {
          result.add(new DbQueue(
            resultSet.getInt(1),
            resultSet.getString(3),
            resultSet.getBoolean(4),
            messages.computeIfAbsent(resultSet.getInt(2), messageId -> this.dbMessageRepository.find(messageId).orElse(null))
          ));
        }
      }
    } catch (SQLException ex) {
      MailGroup.logError(ex);
    }
    return result;
  }

  public DbQueueRepository queueItemWasSent(int id) {
    try (PreparedStatement stmt = this.connectionSupplier.get().prepareStatement("UPDATE queue SET sent = 1 WHERE id = ?")) {
      stmt.setInt(1, id);
      stmt.execute();
    } catch (SQLException ex) {
      MailGroup.logError(ex);
    }
    return this;
  }
}
