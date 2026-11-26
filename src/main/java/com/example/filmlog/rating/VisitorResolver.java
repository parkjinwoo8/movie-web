package com.example.filmlog.rating;

import java.util.UUID;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
public class VisitorResolver {

    private static final String COOKIE = "uid";

    public String resolve(HttpServletRequest req, HttpServletResponse res) {
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if (COOKIE.equals(c.getName()) && c.getValue().matches("[0-9a-f-]{36}")) {
                    return c.getValue();
                }
            }
        }
        String uid = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(COOKIE, uid);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(60 * 60 * 24 * 365 * 2);
        cookie.setAttribute("SameSite", "Lax");
        res.addCookie(cookie);
        return uid;
    }
}
