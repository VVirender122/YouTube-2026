package com.app.servlets;

import java.io.IOException;

import com.app.db.UserDB;
import com.app.helpers.DataValidations;
import com.app.helpers.PasswordResetTokenService;
import com.app.services.EmailServices;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import shadow.org.bson.Document;

@WebServlet("/PasswordResetRequestServlet")
public class PasswordResetRequestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = normalize(request.getParameter("userEmail"));

        if (email == null || !DataValidations.emailValidation(email)) {
            response.sendRedirect(request.getContextPath() + "/PasswordResetPage.html?error=invalid-email");
            return;
        }

        Document user = UserDB.getUserByEmail(email);

        /*
         * Do not reveal whether the account exists.
         */
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/PasswordResetPage.html?sent=1");
            return;
        }

        String token = PasswordResetTokenService.generateToken();
        String tokenHash = PasswordResetTokenService.hashToken(token);
        
        long expiry = PasswordResetTokenService.expiryMillis();

        if (!UserDB.saveResetToken(email, tokenHash, expiry)) {
            response.sendRedirect(request.getContextPath() + "/PasswordResetPage.html?error=account");
            return;
        }

        String name = user.getString("firstName");

        if (name == null || name.isBlank()) {
            name = "there";
        }

        String resetLink = buildResetLink(request, token);

        if (!EmailServices.sendPasswordResetMail(email, name, resetLink)) {
            /*
             * Do not leave a usable token behind if the email
             * could not be sent.
             */
            UserDB.clearResetToken(email);

            response.sendRedirect(request.getContextPath() + "/PasswordResetPage.html?error=mail");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/PasswordResetPage.html?sent=1");
    }

    private static String buildResetLink(HttpServletRequest request, String token) {

        StringBuilder base = new StringBuilder();

        base.append(request.getScheme())
        			.append("://")
        			.append(request.getServerName());

        int port = request.getServerPort();

        if (("http".equalsIgnoreCase(request.getScheme()) && port != 80) || ("https".equalsIgnoreCase(request.getScheme()) && port != 443)) {
            base.append(":").append(port);
        }

        base.append(request.getContextPath())
                .append("/PasswordResetPage.html")
                .append("?token=")
                .append(token);

        return base.toString();
    }

    private static String normalize(String value) {
        return value == null
                ? null
                : value.trim().toLowerCase();
    }
}