package com.app.servlets;

import java.io.IOException;

import com.app.db.UserDB;
import com.app.helpers.PasswordHasher;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import shadow.org.bson.Document;

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = normalize(request.getParameter("userEmail"));
        String password = request.getParameter("userPass");
        Document user = UserDB.getUserByEmail(email);

        boolean valid = false;
        if (user != null) {
            String hash = user.getString("passwordHash");
            if (hash != null) {
                valid = PasswordHasher.verify(password, hash);
            } else {
                // One-time compatibility path for databases created by the old project.
                String legacyPassword = user.getString("password");
                valid = legacyPassword != null && legacyPassword.equals(password);
                if (valid) {
                    UserDB.migrateLegacyPassword(email, PasswordHasher.hash(password));
                }
            }
        }

        if (valid) {
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }

            HttpSession session = request.getSession(true);
            session.setAttribute("authenticated", true);
            session.setAttribute("userName", user.getString("firstName"));
            session.setAttribute("userEmail", user.getString("email"));

            response.sendRedirect(request.getContextPath() + "/Dashboard.jsp");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/LoginPage.html?error=invalid");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/LoginPage.html");
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}
