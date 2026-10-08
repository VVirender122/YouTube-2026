package com.app.services;

import java.util.Locale;
import java.util.Properties;

import com.app.config.SecretsReader;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public final class EmailServices {

    private static final Locale LOCALE = Locale.US;

    private EmailServices() {
    }

    private static Session createSession() {
        String from = SecretsReader.readData("secrets", LOCALE, "MAIL_ID");
        String password = SecretsReader.readData("secrets", LOCALE, "MAIL_PASS");

        Properties properties = new Properties();
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");

        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        };

        return Session.getInstance(properties, authenticator);
    }

    public static boolean sendOTP(String to, String userName, String otp) {
        return send(to, "Your verification code",
                "Hello " + userName + ",\n\n"
                + "Your verification code is: " + otp + "\n\n"
                + "This code expires in 5 minutes. Do not share it with anyone.\n\n"
                + "Regards,\nTeam B");
    }

    public static boolean sendPasswordResetMail(String to, String userName, String resetLink) {
        return send(to,
                "Reset your YouTube-2026 password",
                "Hello " + userName + ",\n\n"
                + "We received a request to reset your password.\n\n"
                + "Click the link below to choose a new password:\n\n"
                + resetLink + "\n\n"
                + "This link expires in "
                + com.app.helpers.PasswordResetTokenService
                        .lifetimeMinutes()
                + " minutes and can only be used once.\n\n"
                + "If you did not request a password reset, "
                + "you can safely ignore this email.\n\n"
                + "Regards,\n"
                + "Team B"
        );
    }

    private static boolean send(String to, String subject, String body) {
        try {
            String from = SecretsReader.readData("secrets", LOCALE, "MAIL_ID");
            Message message = new MimeMessage(createSession());
            message.setFrom(new InternetAddress(from));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
