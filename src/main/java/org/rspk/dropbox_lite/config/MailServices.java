package org.rspk.dropbox_lite.config;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.rspk.dropbox_lite.utils.exceptions.SomethingWentWrongException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class MailServices {

    @Value("${mail.otp.subject}")
    private String otpSubject;

    @Value("${mail.otp.body}")
    private String otpBody;

    @Value("${mail.account.owner.email}")
    private String ownerMail;

    @Value("${mail.account.owner.password}")
    private String ownerPassword;

    public void sendOtp(
            String to,
            String otp
    ){
        sendMail(
                ownerMail,
                ownerPassword,
                to,
                otpSubject,
                otpBody.replace("{{otp}}", otp)
        );
    }


    private static void sendMail(
            String email,
            String password,
            String to,
            String subject,
            String body
    ) {

        Session session = Session.getInstance(
                mailProperties(),
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                email,
                                password
                        );
                    }
                }
        );


        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(email));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(to)
            );
            message.setSubject(subject);
            message.setContent(body, "text/html; charset=utf-8");
            message.saveChanges();
            Transport.send(message);
        } catch (MessagingException e) {
            throw new SomethingWentWrongException(e.getMessage());
        }
    }


    private static Properties mailProperties(){
        Properties props = new Properties();
        props.put("mail.smtp.host","smtp.gmail.com");
        props.put("mail.smtp.port","587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return props;
    }
}
