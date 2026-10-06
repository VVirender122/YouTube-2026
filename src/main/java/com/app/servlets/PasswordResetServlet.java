package com.app.servlets;

import java.io.IOException;

import com.app.db.UserDB;
import com.app.helpers.DataValidations;
import com.app.helpers.OtpService;
import com.app.helpers.PasswordHasher;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/PasswordResetServlet")
public class PasswordResetServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = normalize(request.getParameter("userEmail"));
        String otp = request.getParameter("otp");
        String password = request.getParameter("password");
        String confirm = request.getParameter("confirmPassword");

        if (email == null || !DataValidations.emailValidation(email)
                || password == null || password.length() < 8
                || !DataValidations.passwordValidation(password, confirm)) {
            response.sendRedirect(request.getContextPath() + "/PasswordResetPage.html?error=validation");
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/PasswordResetPage.html?error=expired");
            return;
        }

        String resetEmail = (String) session.getAttribute("resetEmail");
        String resetOtp = (String) session.getAttribute("resetOtp");
        Long expiry = (Long) session.getAttribute("resetOtpExpiry");

        if (!email.equals(resetEmail) || !OtpService.isValid(resetOtp, otp, expiry == null ? 0 : expiry)) {
            response.sendRedirect(request.getContextPath() + "/PasswordResetPage.html?error=otp");
            return;
        }

        if (!UserDB.updatePassword(email, PasswordHasher.hash(password))) {
            response.sendRedirect(request.getContextPath() + "/PasswordResetPage.html?error=account");
            return;
        }

        session.removeAttribute("resetEmail");
        session.removeAttribute("resetOtp");
        session.removeAttribute("resetOtpExpiry");

        response.sendRedirect(request.getContextPath() + "/LoginPage.html?reset=1");
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}
