package wtf.ctl.mailgroup.database;

public class DbQueue {

  private Integer id;
  private String sendTo;
  private boolean sent;
  private DbMessage message;

  public DbQueue(Integer id, String sendTo, boolean sent, DbMessage message) {
    this.id = id;
    this.sendTo = sendTo;
    this.sent = sent;
    this.message = message;
  }

  public Integer getId() {
    return id;
  }

  public String getSendTo() {
    return sendTo;
  }

  public boolean isSent() {
    return sent;
  }

  public DbMessage getMessage() {
    return message;
  }
}
