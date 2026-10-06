package com.app.filters;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebFilter(urlPatterns = {"*.jsp"})
public class AuthFilter implements Filter {

    private static final Set<String> PUBLIC_JSP = Set.of();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getServletPath();
        if (PUBLIC_JSP.contains(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        boolean authenticated = session != null
                && Boolean.TRUE.equals(session.getAttribute("authenticated"));

        if (!authenticated) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/LoginPage.html");
            return;
        }

        chain.doFilter(request, response);
    }
}
