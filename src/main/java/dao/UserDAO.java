package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import util.DBConnection;
import util.PasswordUtil;

public class UserDAO {

    public boolean register(String name, String email, String password) {
        try {
            Connection conn = DBConnection.getConnection();

            String sql = "INSERT INTO users(name,email,password) VALUES(?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, PasswordUtil.hash(password)); // ✅ mot de passe haché avec BCrypt

            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean login(String email, String password) {
        try {
            Connection conn = DBConnection.getConnection();

            // ✅ On ne peut plus comparer le mot de passe directement en SQL puisqu'il est
            //    désormais haché : on récupère le hash puis on le vérifie avec BCrypt.
            String sql = "SELECT password FROM users WHERE email=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return PasswordUtil.matches(password, rs.getString("password"));
            }
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
