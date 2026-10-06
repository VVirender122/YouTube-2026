package com.app.servlets;

import java.io.IOException;

import com.app.db.UserDB;
import com.app.helpers.OtpService;
import com.app.services.EmailServices;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/PasswordResetRequestServlet")
public class PasswordResetRequestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = normalize(request.getParameter("userEmail"));
        if (email == null || !com.app.helpers.DataValidations.emailValidation(email)) {
            response.sendRedirect(request.getContextPath() + "/PasswordResetPage.html?error=invalid-email");
            return;
        }

        if (UserDB.getUserByEmail(email) == null) {
            // Do not reveal whether an account exists.
            response.sendRedirect(request.getContextPath() + "/PasswordResetPage.html?sent=1");
            return;
        }

        String otp = OtpService.generate();
        HttpSession session = request.getSession(true);
        session.setAttribute("resetEmail", email);
        session.setAttribute("resetOtp", otp);
        session.setAttribute("resetOtpExpiry", OtpService.expiryMillis());

        String name = "there";
        var user = UserDB.getUserByEmail(email);
        if (user != null) {
            name = user.getString("firstName");
        }

        if (!EmailServices.sendPasswordResetMail(email, name, otp)) {
            session.removeAttribute("resetOtp");
            session.removeAttribute("resetOtpExpiry");
            response.sendRedirect(request.getContextPath() + "/PasswordResetPage.html?error=mail");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/PasswordResetPage.html?sent=1");
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}
