package com.app.servlets;

import java.io.IOException;

import com.app.db.UserDB;
import com.app.db.UserSchema;
import com.app.helpers.DataValidations;
import com.app.helpers.OtpService;
import com.app.helpers.PasswordHasher;
import com.app.services.EmailServices;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String firstName = trim(request.getParameter("fname"));
        String lastName = trim(request.getParameter("lname"));
        String email = normalize(request.getParameter("userEmail"));
        String password = request.getParameter("password");
        String confirm = request.getParameter("cnfPassword");

        if (!DataValidations.nameValidation(firstName, lastName)
                || !DataValidations.emailValidation(email)
                || !DataValidations.passwordValidation(password, confirm)) {
            response.sendRedirect(request.getContextPath() + "/RegisterPage.html?error=validation");
            return;
        }

        if (UserDB.getUserByEmail(email) != null) {
            response.sendRedirect(request.getContextPath() + "/RegisterPage.html?error=exists");
            return;
        }

        String otp = OtpService.generate();
        HttpSession session = request.getSession(true);
        session.setAttribute("pendingUser", new UserSchema(
                firstName, lastName, email, PasswordHasher.hash(password)));
        session.setAttribute("registrationOtp", otp);
        session.setAttribute("registrationOtpExpiry", OtpService.expiryMillis());

        if (!EmailServices.sendOTP(email, firstName, otp)) {
            session.removeAttribute("pendingUser");
            session.removeAttribute("registrationOtp");
            session.removeAttribute("registrationOtpExpiry");
            response.sendRedirect(request.getContextPath() + "/RegisterPage.html?error=mail");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/OTPEnterPage.html");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/RegisterPage.html");
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}
