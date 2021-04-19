package wtf.ctl.mailgroup.database;

import java.time.LocalDateTime;

public class DbMessage {

  private Integer id;
  private String listName;
  private String messageUid;
  private LocalDateTime messageDate;
  private String from;
  private String subject;
  private String plainBody;
  private String htmlBody;

  public DbMessage(Integer id, String listName, String messageUid, LocalDateTime messageDate, String from, String subject, String plainBody, String htmlBody) {
    this.id = id;
    this.listName = listName;
    this.messageUid = messageUid;
    this.messageDate = messageDate;
    this.from = from;
    this.subject = subject;
    this.plainBody = plainBody;
    this.htmlBody = htmlBody;
  }

  public Integer getId() {
    return id;
  }

  public String getListName() {
    return listName;
  }

  public String getMessageUid() {
    return messageUid;
  }

  public LocalDateTime getMessageDate() {
    return messageDate;
  }

  public String getFrom() {
    return from;
  }

  public String getSubject() {
    return subject;
  }

  public String getPlainBody() {
    return plainBody;
  }

  public String getHtmlBody() {
    return htmlBody;
  }
}
