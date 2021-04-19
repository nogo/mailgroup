package wtf.ctl.mailgroup.task;

import com.sun.mail.imap.IMAPMessage;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;
import wtf.ctl.mailgroup.MailGroup;
import wtf.ctl.mailgroup.configuration.MailGroupConfiguration;
import wtf.ctl.mailgroup.database.DbMessage;
import wtf.ctl.mailgroup.database.DbMessageRepository;
import wtf.ctl.mailgroup.database.DbQueue;
import wtf.ctl.mailgroup.database.DbQueueRepository;
import wtf.ctl.mailgroup.mailing.Imap;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public class Fetch implements Runnable {

  private final Collection<MailGroupConfiguration> mailGroupConfigurationList;
  private final DbMessageRepository messageRepository;
  private final DbQueueRepository queueRepository;
  private int newMessageSuccessCount = 0;
  private int newMessageErrorCount = 0;

  public Fetch(
    Collection<MailGroupConfiguration> mailGroupConfigurationList,
    DbMessageRepository messageRepository,
    DbQueueRepository queueRepository
  ) {
    this.mailGroupConfigurationList = mailGroupConfigurationList;
    this.messageRepository = messageRepository;
    this.queueRepository = queueRepository;
  }

  @Override
  public void run() {
    this.newMessageSuccessCount = 0;
    this.newMessageErrorCount = 0;
    this.mailGroupConfigurationList.forEach(this::fetch);
    MailGroup.logInfo("Fetch run successfully. (New message(s) found [Success: {0}, Failure: {1}])", this.newMessageSuccessCount, this.newMessageErrorCount);
  }

  private void fetch(MailGroupConfiguration mailGroupConfiguration) {
    try (Imap imap = new Imap(mailGroupConfiguration)) {
      for (Message message : imap.getInbox().getMessages()) {
        Optional<String> fromAddress = Arrays.stream(message.getFrom())
          .map(InternetAddress.class::cast)
          .map(InternetAddress::getAddress)
          .findFirst();

        if (fromAddress.isPresent() && mailGroupConfiguration.isValidRecipient(fromAddress.get())) {
          String subject = message.getSubject();

          String plainBody = null;
          String htmlBody = null;

          if (message.isMimeType("text/html")) {
            htmlBody = (String) message.getContent();
          } else if (message.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) message.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
              BodyPart bodyPart = mp.getBodyPart(i);
              if (bodyPart.isMimeType("text/html")) {
                htmlBody = (String) bodyPart.getContent();
              } else {
                plainBody = (String) bodyPart.getContent();
              }
            }
          } else {
            plainBody = (String) message.getContent();
          }

          this.messageRepository
            .persist(new DbMessage(
                null,
                mailGroupConfiguration.getId(),
                ((IMAPMessage) message).getMessageID(),
                message.getSentDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                fromAddress.get(),
                subject,
                plainBody,
                htmlBody
              )
            )
            .filter(dbMessage -> dbMessage.getId() != null)
            .ifPresentOrElse(
              value -> mailGroupConfiguration.streamTo().forEach(recipient -> this.queueRepository.persist(new DbQueue(
                null,
                recipient,
                false,
                value
              ))),
              () -> MailGroup.logInfo("Message could not processed {0} - {1}", fromAddress.get(), subject)
            );

          imap.moveMessageToReceived(message);
          this.newMessageSuccessCount++;
        } else {
          imap.moveMessageToErrors(message);
          this.newMessageErrorCount++;
        }

      }
    } catch (Exception e) {
      MailGroup.logError(e);
    }
  }
}

