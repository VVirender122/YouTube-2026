package com.app.servlets;

import java.io.IOException;

import com.app.db.UserDB;
import com.app.db.UserSchema;
import com.app.helpers.OtpService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/OTPValidationServlet")
public class OTPValidationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String enteredOtp = request.getParameter("enteredOTP");
        HttpSession session = request.getSession(false);

        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/RegisterPage.html?error=expired");
            return;
        }

        String expectedOtp = (String) session.getAttribute("registrationOtp");
        Long expiry = (Long) session.getAttribute("registrationOtpExpiry");

        if (!OtpService.isValid(expectedOtp, enteredOtp, expiry == null ? 0 : expiry)) {
            response.sendRedirect(request.getContextPath() + "/OTPEnterPage.html?error=invalid");
            return;
        }

        UserSchema pending = (UserSchema) session.getAttribute("pendingUser");
        if (pending == null || !UserDB.addUser(pending)) {
            response.sendRedirect(request.getContextPath() + "/RegisterPage.html?error=account");
            return;
        }

        session.removeAttribute("pendingUser");
        session.removeAttribute("registrationOtp");
        session.removeAttribute("registrationOtpExpiry");

        response.sendRedirect(request.getContextPath() + "/LoginPage.html?registered=1");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/OTPEnterPage.html");
    }
}
