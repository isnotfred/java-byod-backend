package com.pup.byod.javabyodbackend.service;

import com.pup.byod.javabyodbackend.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class ResendEmailService {

    @Value("${RESEND_API_KEY:}")
    private String apiKey;

    @Value("${RESEND_FROM_EMAIL:onboarding@resend.dev}")
    private String fromEmail;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void sendWelcomeEmail(String toEmail, String fullName, String password) {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("Resend API Key is not set. Simulated Email sending:");
            System.out.println("To: " + toEmail);
            System.out.println("Subject: Welcome to BYOD Device Management System");
            System.out.println("Password: " + password);
            return;
        }

        String subject = "Welcome to BYOD Device Management System";
        String htmlContent = String.format(
                "<h3>Welcome %s,</h3>" +
                "<p>An account has been created for you in the BYOD Device Management System.</p>" +
                "<p>Here are your credentials for your first login:</p>" +
                "<ul>" +
                "  <li><b>Email:</b> %s</li>" +
                "  <li><b>Password:</b> %s</li>" +
                "</ul>" +
                "<p><b>Important:</b> Please reset your password immediately after logging in for security purposes.</p>" +
                "<p>Best regards,<br>BYOD System Admin</p>",
                fullName, toEmail, password
        );

        // Escape JSON quotes/newlines
        String escapedHtml = htmlContent.replace("\"", "\\\"").replace("\n", "\\n");

        String jsonPayload = String.format(
                "{\"from\":\"%s\",\"to\":\"%s\",\"subject\":\"%s\",\"html\":\"%s\"}",
                fromEmail, toEmail, subject, escapedHtml
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 300) {
                throw new BusinessRuleException("Failed to send welcome email via Resend. Status code: " 
                        + response.statusCode() + ", Response: " + response.body());
            }
        } catch (Exception e) {
            if (e instanceof BusinessRuleException) {
                throw (BusinessRuleException) e;
            }
            throw new BusinessRuleException("Error occurred while sending welcome email: " + e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String toEmail, String fullName, String token) {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("Resend API Key is not set. Simulated Email sending:");
            System.out.println("To: " + toEmail);
            System.out.println("Subject: Password Reset Request");
            System.out.println("Token: " + token);
            return;
        }

        String subject = "Password Reset Request";
        String htmlContent = String.format(
                "<h3>Hello %s,</h3>" +
                "<p>A password reset was requested for your account in the BYOD Device Management System.</p>" +
                "<p>Please use the following token to reset your password:</p>" +
                "<p style=\"font-size: 18px; font-weight: bold; background-color: #f3f4f6; padding: 10px; display: inline-block;\">%s</p>" +
                "<p>This token will expire in 15 minutes.</p>" +
                "<p>If you did not make this request, you can safely ignore this email.</p>" +
                "<p>Best regards,<br>BYOD System Admin</p>",
                fullName, token
        );

        // Escape JSON quotes/newlines
        String escapedHtml = htmlContent.replace("\"", "\\\"").replace("\n", "\\n");

        String jsonPayload = String.format(
                "{\"from\":\"%s\",\"to\":\"%s\",\"subject\":\"%s\",\"html\":\"%s\"}",
                fromEmail, toEmail, subject, escapedHtml
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 300) {
                throw new BusinessRuleException("Failed to send password reset email via Resend. Status code: " 
                        + response.statusCode() + ", Response: " + response.body());
            }
        } catch (Exception e) {
            if (e instanceof BusinessRuleException) {
                throw (BusinessRuleException) e;
            }
            throw new BusinessRuleException("Error occurred while sending password reset email: " + e.getMessage());
        }
    }
}
