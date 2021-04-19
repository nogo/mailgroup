package wtf.ctl.mailgroup.configuration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

public class MailGroupConfiguration {

  private final String id;
  private final String name;
  private final String email;

  private final String imapHost;
  private final String smtpHost;

  private final String user;
  private final String password;

  private final String search;
  private final String receivedFolder;
  private final String errorsFolder;

  private final String bounce;

  private final Map<String, String> mailingList = new LinkedHashMap<>();

  public MailGroupConfiguration(String id, LinkedHashMap<String, Object> items) {
    this.id = id;
    this.name = this.get(MailGroupConfigurationKeys.NAME, items).orElse("");
    this.email = this.get(MailGroupConfigurationKeys.MAIL, items).orElse("");

    this.imapHost = this.get(MailGroupConfigurationKeys.IMAP, items).orElse("");
    this.smtpHost = this.get(MailGroupConfigurationKeys.SMTP, items).orElse("");

    this.user = this.get(MailGroupConfigurationKeys.USER, items).orElse("");
    this.password = this.get(MailGroupConfigurationKeys.PASSWORD, items).orElse("");

    this.search = this.get(MailGroupConfigurationKeys.SEARCH, items).orElse("");
    this.receivedFolder = this.get(MailGroupConfigurationKeys.RECEIVED, items).orElse("INBOX.received");
    this.errorsFolder = this.get(MailGroupConfigurationKeys.ERRORS, items).orElse("INBOX.errors");

    this.bounce = this.get(MailGroupConfigurationKeys.BOUNCE, items).orElse("");

    //noinspection unchecked
    this.mailingList.putAll((LinkedHashMap<String, String>) items.getOrDefault(MailGroupConfigurationKeys.LIST.toString(), new LinkedHashMap<>()));
  }

  public Properties createSessionProperties() {
    Properties properties = new Properties();

    properties.setProperty("mail.user", this.user);
    properties.setProperty("mail.imap.host", this.imapHost);
    properties.setProperty("mail.imap.port", "993");

    properties.setProperty("mail.smtp.auth", "true");
    properties.setProperty("mail.smtp.host", this.smtpHost);
    properties.setProperty("mail.smtp.port", "465");
    properties.setProperty("mail.smtp.ssl.enable", "true");

    return properties;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getImapHost() {
    return imapHost;
  }

  public String getSmtpHost() {
    return smtpHost;
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }

  public String getSearch() {
    return search;
  }

  public String getReceivedFolder() {
    return receivedFolder;
  }

  public String getErrorsFolder() {
    return errorsFolder;
  }

  public String getBounce() {
    return bounce;
  }

  public boolean isValidRecipient(String email) {
    return this.mailingList.containsKey(email);
  }

  public Stream<String> streamTo() {
    return this.mailingList.entrySet().stream()
      .filter(item -> !item.getValue().equals("IGNORE"))
      .map(Map.Entry::getKey);
  }

  private Optional<String> get(
    MailGroupConfigurationKeys key,
    LinkedHashMap<String, Object> items
  ) {
    if (items == null || key == null)
      return Optional.empty();

    Object obj = items.get(key.toString());
    return obj instanceof String ? Optional.of((String) obj) : Optional.empty();
  }

  @Override
  public String toString() {
    return "MailGroupConfiguration{" +
      "id='" + id + '\'' +
      ", email='" + email + '\'' +
      ", imapHost='" + imapHost + '\'' +
      ", smtpHost='" + smtpHost + '\'' +
      '}';
  }
}
