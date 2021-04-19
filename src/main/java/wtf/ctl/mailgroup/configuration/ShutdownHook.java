package wtf.ctl.mailgroup.configuration;

import wtf.ctl.mailgroup.MailGroup;

import java.io.IOException;

public class ShutdownHook extends Thread {

  private final MailGroup mailGroup;

  public ShutdownHook(MailGroup mailGroup) {
    this.mailGroup = mailGroup;
  }

  @Override
  public void run() {
    try {
      this.mailGroup.close();
    } catch (IOException ex) {
      MailGroup.logError(ex);
    }
  }
}
