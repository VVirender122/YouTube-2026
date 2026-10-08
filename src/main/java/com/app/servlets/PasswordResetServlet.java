package com.app.servlets;

import java.io.IOException;

import com.app.db.UserDB;
import com.app.helpers.DataValidations;
import com.app.helpers.PasswordHasher;
import com.app.helpers.PasswordResetTokenService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/PasswordResetServlet")
public class PasswordResetServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String token = request.getParameter("token");
        String password = request.getParameter("password");
        String confirmPassword =
                request.getParameter("confirmPassword");

        if (token == null || token.isBlank()
                || password == null
                || password.length() < 8
                || !DataValidations.passwordValidation(
                        password,
                        confirmPassword)) {

            response.sendRedirect(
                    request.getContextPath()
                    + "/PasswordResetPage.html?error=validation");

            return;
        }

        String tokenHash =
                PasswordResetTokenService.hashToken(token);

        String passwordHash =
                PasswordHasher.hash(password);

        boolean updated =
                UserDB.resetPasswordWithToken(
                        tokenHash,
                        passwordHash,
                        System.currentTimeMillis());

        if (!updated) {
            response.sendRedirect(
                    request.getContextPath()
                    + "/PasswordResetPage.html?error=expired");

            return;
        }

        response.sendRedirect(
                request.getContextPath()
                + "/LoginPage.html?reset=1");
    }
}