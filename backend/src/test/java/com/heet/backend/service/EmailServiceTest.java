package com.heet.backend.service;

import com.heet.backend.exception.EmailDeliveryException;
import jakarta.mail.Address;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(
                mailSender,
                "Nivara <nivara.app@gmail.com>",
                "nivara.app@gmail.com",
                "smtp.gmail.com",
                465
        );
    }

    @Test
    void sendVerificationEmail_SetsExactRecipientAndSender() throws Exception {
        JavaMailSenderImpl dummySender = new JavaMailSenderImpl();
        MimeMessage mimeMessage = dummySender.createMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String testRecipient = "test-recipient-1@example.com";
        emailService.sendVerificationEmail(testRecipient, "Test User", "123456");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);

        // Recipient MUST strictly match the exact email supplied, never fallback or hardcoded
        Address[] recipients = sentMessage.getRecipients(MimeMessage.RecipientType.TO);
        assertNotNull(recipients);
        assertEquals(1, recipients.length);
        assertEquals(testRecipient, ((InternetAddress) recipients[0]).getAddress());

        // Verify sender
        Address[] fromAddresses = sentMessage.getFrom();
        assertNotNull(fromAddresses);
        assertEquals(1, fromAddresses.length);
        assertEquals("nivara.app@gmail.com", ((InternetAddress) fromAddresses[0]).getAddress());

        // Verify subject
        assertEquals("Verify your email - Nivara", sentMessage.getSubject());
    }

    @Test
    void sendVerificationEmail_SecondRecipient_SetsExactSecondRecipient() throws Exception {
        JavaMailSenderImpl dummySender = new JavaMailSenderImpl();
        MimeMessage mimeMessage = dummySender.createMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String secondRecipient = "user.second@company.org";
        emailService.sendVerificationEmail(secondRecipient, "Second User", "654321");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        Address[] recipients = sentMessage.getRecipients(MimeMessage.RecipientType.TO);
        assertEquals(secondRecipient, ((InternetAddress) recipients[0]).getAddress());
    }

    @Test
    void sendVerificationEmail_WhenMailSenderThrows_ThrowsEmailDeliveryException() {
        JavaMailSenderImpl dummySender = new JavaMailSenderImpl();
        MimeMessage mimeMessage = dummySender.createMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("550 You can only send testing emails to your own email address"))
                .when(mailSender).send(any(MimeMessage.class));

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () ->
                emailService.sendVerificationEmail("unauthorized@example.com", "Test", "112233"));

        assertEquals("Verification email could not be sent. Please try again.", ex.getMessage());
        assertNotNull(ex.getCause());
    }
}
