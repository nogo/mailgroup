package wtf.ctl.mailgroup.mailing;

import com.sun.mail.imap.IMAPFolder;
import jakarta.mail.*;
import wtf.ctl.mailgroup.MailGroup;
import wtf.ctl.mailgroup.configuration.MailGroupConfiguration;

public class Imap implements AutoCloseable {

  private static final String INBOX = "INBOX";
  private final String receivedFolder;
  private final String errorFolder;
  private final Store store;
  private IMAPFolder inbox;

  public Imap(MailGroupConfiguration mailGroupConfiguration) throws NoSuchProviderException {
    Session session = Session.getDefaultInstance(mailGroupConfiguration.createSessionProperties(), null);

    this.receivedFolder = mailGroupConfiguration.getReceivedFolder();
    this.errorFolder = mailGroupConfiguration.getErrorsFolder();

    this.store = session.getStore("imaps");
    try {
      this.store.connect(mailGroupConfiguration.getImapHost(), mailGroupConfiguration.getUser(), mailGroupConfiguration.getPassword());
      this.inbox = (IMAPFolder) store.getFolder(INBOX);
      this.inbox.open(Folder.READ_WRITE);
    } catch (MessagingException ex) {
      MailGroup.logError("Cannot connect to mailgroup {0} ", mailGroupConfiguration.toString(), ex);
    }
  }

  @Override
  public void close() throws Exception {
    this.inbox.close(true);
    this.store.close();
  }

  public IMAPFolder getInbox() {
    return this.inbox;
  }

  public Imap moveMessageToReceived(Message... message) {
    if (!this.store.isConnected()) return this;

    if (message == null || message.length <= 0) return this;

    try {
      try (Folder received = store.getFolder(this.receivedFolder)) {
        received.open(Folder.READ_WRITE);
        this.inbox.copyMessages(message, received);
      }
      Flags flags = new Flags(Flags.Flag.DELETED);
      this.inbox.setFlags(message, flags, true);
      this.inbox.expunge();
    } catch (MessagingException ex) {
      MailGroup.logError("Cannot move messages", ex);
    }
    return this;
  }

  public Imap moveMessageToErrors(Message... message) {
    if (!this.store.isConnected() || message == null || message.length <= 0) return this;

    try {
      this.inbox.open(Folder.READ_WRITE);
      try (Folder errors = store.getFolder(this.errorFolder)) {
        errors.open(Folder.READ_WRITE);
        this.inbox.copyMessages(message, errors);
      }
      Flags flags = new Flags(Flags.Flag.DELETED);
      this.inbox.setFlags(message, flags, true);
      this.inbox.expunge();
    } catch (MessagingException ex) {
      MailGroup.logError("Cannot move messages", ex);
    }
    return this;
  }
}
