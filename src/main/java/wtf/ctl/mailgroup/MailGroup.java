package wtf.ctl.mailgroup;

import org.yaml.snakeyaml.Yaml;
import wtf.ctl.mailgroup.configuration.MailGroupConfiguration;
import wtf.ctl.mailgroup.configuration.ShutdownHook;
import wtf.ctl.mailgroup.database.DatabaseConnector;
import wtf.ctl.mailgroup.database.DbMessageRepository;
import wtf.ctl.mailgroup.database.DbQueueRepository;
import wtf.ctl.mailgroup.task.Fetch;
import wtf.ctl.mailgroup.task.Send;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * MailGroup is a multi-account mailing list using a standard email account.
 * <p>
 * Multi-accounting
 * IMAP Account connection to read emails
 * Access only for members of the mail group
 * Attachments are not forwared
 * History save email in imap folder
 * <p>
 * Source: https://eclipse-ee4j.github.io/mail/docs/api/
 */
public class MailGroup implements Closeable {

  private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(2);
  private final List<MailGroupConfiguration> mailgroups = new ArrayList<>();
  private final DatabaseConnector databaseConnector;

  private MailGroup(Path databaseFile, Path configurationFile) {
    Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));

    this.databaseConnector = new DatabaseConnector(databaseFile);
    try {
      this.databaseConnector.testDatabaseConnection();
    } catch (Exception ex) {
      MailGroup.logError("Could not database", ex);
      System.exit(1);
    }

    Yaml yaml = new Yaml();
    try (InputStream in = Files.newInputStream(configurationFile)) {
      LinkedHashMap<String, LinkedHashMap<String, Object>> yamlData = yaml.load(in);
      this.mailgroups.addAll(yamlData
        .entrySet()
        .stream()
        .map(item -> new MailGroupConfiguration(item.getKey(), item.getValue()))
        .collect(Collectors.toList())
      );
    } catch (IOException ex) {
      MailGroup.logError("Could not load configuration file", ex);
      System.exit(1);
    }
  }

  void run() {
    this.scheduler.scheduleAtFixedRate(new Fetch(
      this.mailgroups,
      new DbMessageRepository(this.databaseConnector::getConnection),
      new DbQueueRepository(this.databaseConnector::getConnection)
    ), 0, 5, TimeUnit.MINUTES);
    this.scheduler.scheduleAtFixedRate(new Send(
      this.mailgroups,
      new DbQueueRepository(this.databaseConnector::getConnection)
    ) , 1, 5, TimeUnit.MINUTES);
  }

  public static void main(String[] args) {
    if (args == null || args.length <= 1) {
      MailGroup.logError("Database file and configuration file argument missing.");
      System.exit(1);
    }

    new MailGroup(Paths.get(args[0]), Paths.get(args[1])).run();
  }

  @Override
  public void close() throws IOException {
    this.databaseConnector.close();
    this.scheduler.shutdownNow();
  }

  public static void logInfo(String message, Object... params) {
    System.out.println(MessageFormat.format(message, Arrays.stream(params).filter(obj -> !(obj instanceof Throwable)).toArray()));
  }

  public static void logError(String message, Object... params) {
    System.err.println(MessageFormat.format(message, Arrays.stream(params).filter(obj -> !(obj instanceof Throwable)).toArray()));
    Arrays.stream(params).filter(obj -> (obj instanceof Throwable)).map(Throwable.class::cast).findFirst().ifPresent(Throwable::printStackTrace);
  }

  public static void logError(Throwable exception) {
    System.err.println(exception.getMessage());
    exception.printStackTrace();
  }
}
