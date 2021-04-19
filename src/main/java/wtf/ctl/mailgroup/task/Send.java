package wtf.ctl.mailgroup.task;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import wtf.ctl.mailgroup.MailGroup;
import wtf.ctl.mailgroup.configuration.MailGroupConfiguration;
import wtf.ctl.mailgroup.database.DbMessage;
import wtf.ctl.mailgroup.database.DbQueue;
import wtf.ctl.mailgroup.database.DbQueueRepository;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

public class Send implements Runnable {

  private final Collection<MailGroupConfiguration> mailGroupConfigurationList;
  private final DbQueueRepository dbQueueRepository;

  public Send(
    Collection<MailGroupConfiguration> mailGroupConfigurationList,
    DbQueueRepository dbQueueRepository
  ) {
    this.mailGroupConfigurationList = mailGroupConfigurationList;
    this.dbQueueRepository = dbQueueRepository;
  }

  @Override
  public void run() {
    this.dbQueueRepository.findAllToSend().forEach(this::send);
    MailGroup.logInfo("Send run successfully.");
  }

  private void send(DbQueue queueItem) {
    DbMessage queueMessage = queueItem.getMessage();

    Optional<MailGroupConfiguration> configurationOptional = this.getMailGroupConfiguration(queueMessage.getListName());

    if (configurationOptional.isPresent()) {
      MailGroupConfiguration mailGroupConfiguration = configurationOptional.get();
      Session session = Session.getDefaultInstance(mailGroupConfiguration.createSessionProperties());

      try {
        InternetAddress fromAddress = new InternetAddress(mailGroupConfiguration.getEmail());
        fromAddress.setPersonal(mailGroupConfiguration.getName());

        Message message = new MimeMessage(session);
        message.setFrom(fromAddress);
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(queueItem.getSendTo()));
        message.setSentDate(new Date());

        message.setSubject(queueMessage.getSubject());

        boolean sendable = false;
        if (queueMessage.getHtmlBody() != null && !queueMessage.getHtmlBody().isBlank()) {
          message.setContent(queueMessage.getHtmlBody(), "text/html");
          sendable = true;
        }

        if (queueMessage.getPlainBody() != null && !queueMessage.getPlainBody().isBlank()) {
          message.setText(queueMessage.getPlainBody());
          sendable = true;
        }

        if (sendable) {
          Transport.send(message, mailGroupConfiguration.getUser(), mailGroupConfiguration.getPassword());
          this.dbQueueRepository.queueItemWasSent(queueItem.getId());
        } else {
          MailGroup.logError("Message could not be sent, body was empty");
        }
      } catch (Exception ex) {
        MailGroup.logError(ex);
      }
    }
  }

  private Optional<MailGroupConfiguration> getMailGroupConfiguration(String mailgroupId) {
    return this.mailGroupConfigurationList
      .stream()
      .filter(mgc -> mgc.getId().equals(mailgroupId))
      .findFirst();
  }

}
