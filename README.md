## Mailgroup

Mailgroup is a easy to manage multi-account mailing list written in Java.

## Features

* IMAP account connection to read emails
* Multi-accounting
* Access only for members of the mail group
* Attachments are not forwarded for security reasons
* History save email in imap folder
* Docker ready

## FUNCTIONALITY

1. A member of the mailing list sends an email to the mailing list address.
2. The application connects to the IMAP account reads the email.
3. The mail get saved into a sqlite database.
4. A copy of the mail now gets prepared for every member of the list.
5. The application send with the mailing list address the copy of the mail to each member of the list.
