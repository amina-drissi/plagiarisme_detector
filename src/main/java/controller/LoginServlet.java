package controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import util.DBConnection;
import util.PasswordUtil;

public class LoginServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            Connection conn = DBConnection.getConnection();

            // ✅ Le mot de passe est haché : on récupère le hash correspondant à l'email
            //    puis on le vérifie avec BCrypt (plus de comparaison SQL en clair).
            String sql = "SELECT id, name, password FROM users WHERE email=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if (rs.next() && PasswordUtil.matches(password, rs.getString("password"))) {
                HttpSession session = request.getSession();
                session.setAttribute("user", email);
                session.setAttribute("userId", rs.getInt("id"));     // ✅ nécessaire à l'isolation par utilisateur
                session.setAttribute("userName", rs.getString("name"));

                response.sendRedirect("dashboard.jsp");
            } else {
                response.sendRedirect("login.jsp?error=1");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("login.jsp?error=1");
        }
    }
}
