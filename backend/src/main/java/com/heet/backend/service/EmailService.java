package com.heet.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String smtpHost;
    private final int smtpPort;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:}") String fromEmail,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.host:smtp.gmail.com}") String smtpHost,
            @Value("${spring.mail.port:465}") int smtpPort) {
        this.mailSender = mailSender;
        String resolvedFrom = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : mailUsername;
        if (resolvedFrom == null || resolvedFrom.isBlank()) {
            resolvedFrom = "nivara.app@gmail.com";
        }
        if (!resolvedFrom.contains("<") && resolvedFrom.contains("@")) {
            this.fromEmail = "Nivara <" + resolvedFrom.trim() + ">";
        } else {
            this.fromEmail = resolvedFrom.trim();
        }
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
    }

    /**
     * Sends a 6-digit OTP verification email to the user.
     * The recipient address is always the exact address provided during registration or resend.
     *
     * @param toEmail       recipient email address
     * @param recipientName recipient name
     * @param otp           raw 6-digit verification code
     */
    public void sendVerificationEmail(String toEmail, String recipientName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify your email - Nivara");

            String plainText = buildPlainTextEmail(recipientName, otp);
            String htmlText = buildHtmlEmail(recipientName, otp);

            helper.setText(plainText, htmlText);

            mailSender.send(message);
            log.info("Verification email successfully dispatched to '{}' via {}:{}",
                    maskEmail(toEmail), smtpHost, smtpPort);
        } catch (Exception e) {
            logSmtpDiagnostics(toEmail, e);
            throw new com.heet.backend.exception.EmailDeliveryException(
                    "Verification email could not be sent. Please try again.", e);
        }
    }

    private void logSmtpDiagnostics(String toEmail, Exception exception) {
        Throwable rootCause = findRootCause(exception);
        String failureType = classifyFailureType(exception, rootCause);
        String recipientDomain = extractDomain(toEmail);

        log.error("""
                ======================= SMTP DELIVERY FAILURE =======================
                SMTP Host:             {}
                SMTP Port:             {}
                Configured Sender:     {}
                Recipient Domain:      {}
                Failure Type:          {}
                Deepest Exception:     {}
                Root Cause Message:    {}
                =====================================================================""",
                smtpHost,
                smtpPort,
                fromEmail,
                recipientDomain,
                failureType,
                rootCause.getClass().getSimpleName(),
                rootCause.getMessage()
        );
    }

    private Throwable findRootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    private String classifyFailureType(Exception exception, Throwable rootCause) {
        String msg = (rootCause.getMessage() != null ? rootCause.getMessage().toLowerCase() : "");
        String exName = rootCause.getClass().getName().toLowerCase();

        if (exName.contains("authentication") || msg.contains("535") || msg.contains("authentication failed")) {
            return "AUTHENTICATION_FAILED";
        }
        if (msg.contains("550") || msg.contains("testing emails to your own email address") || msg.contains("recipient rejected")) {
            return "RECIPIENT_REJECTED";
        }
        if (msg.contains("553") || msg.contains("sender rejected")) {
            return "SENDER_REJECTED";
        }
        if (exName.contains("ssl") || exName.contains("handshake") || msg.contains("ssl") || msg.contains("handshake")) {
            return "SSL_TLS_FAILURE";
        }
        if (exName.contains("timeout") || msg.contains("timed out") || msg.contains("timeout")) {
            return "CONNECTION_TIMEOUT";
        }
        if (exName.contains("connectexception") || msg.contains("connection refused")) {
            return "CONNECTION_FAILED";
        }
        return "UNKNOWN_MAIL_ERROR";
    }


    private String extractDomain(String email) {
        if (email != null && email.contains("@")) {
            return email.substring(email.indexOf('@') + 1);
        }
        return "unknown";
    }

    private String buildPlainTextEmail(String recipientName, String otp) {
        return "Nivara\n\n" +
                "Verify your email\n\n" +
                "Your verification code is:\n\n" +
                otp + "\n\n" +
                "This code expires in 5 minutes.\n\n" +
                "If you didn't create a Nivara account, you can ignore this email.\n";
    }

    private String buildHtmlEmail(String recipientName, String otp) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Verify your email</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f8fafc; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b;">
                  <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #f8fafc; padding: 40px 16px;">
                    <tr>
                      <td align="center">
                        <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 480px; background-color: #ffffff; border-radius: 16px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);">
                          <!-- Header -->
                          <tr>
                            <td style="padding: 32px 32px 24px 32px; text-align: center; border-bottom: 1px solid #f1f5f9;">
                              <div style="display: inline-block; padding: 10px 18px; border-radius: 12px; background-color: #eef2ff; color: #4f46e5; font-weight: 700; font-size: 20px; letter-spacing: -0.5px;">
                                Nivara
                              </div>
                            </td>
                          </tr>
                          <!-- Content -->
                          <tr>
                            <td style="padding: 32px;">
                              <h1 style="margin: 0 0 12px 0; font-size: 20px; font-weight: 700; color: #0f172a; text-align: center;">
                                Verify your email
                              </h1>
                              <p style="margin: 0 0 28px 0; font-size: 14px; line-height: 1.6; color: #64748b; text-align: center;">
                                Use the 6-digit code below to complete your registration and activate your Nivara personal finance workspace.
                              </p>
                              <!-- Code Display Box -->
                              <div style="background-color: #f1f5f9; border: 1px solid #cbd5e1; border-radius: 12px; padding: 20px; text-align: center; margin-bottom: 24px;">
                                <span style="font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace; font-size: 32px; font-weight: 800; letter-spacing: 6px; color: #1e293b;">
                                  %s
                                </span>
                              </div>
                              <p style="margin: 0 0 20px 0; font-size: 13px; color: #64748b; text-align: center;">
                                ⏱️ This code expires in <strong>5 minutes</strong>.
                              </p>
                              <p style="margin: 0; font-size: 12px; line-height: 1.5; color: #94a3b8; text-align: center;">
                                If you didn't create a Nivara account, you can safely ignore this email.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(otp);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }
}
