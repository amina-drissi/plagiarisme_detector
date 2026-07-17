package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;

import util.DBConnection;

public class SubmissionDAO {

    public boolean saveSubmission(int userId, String tpName, String filePath) {

        try {
            Connection conn = DBConnection.getConnection();

            String sql = "INSERT INTO submissions(user_id,tp_name,file_path) VALUES(?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, userId);
            ps.setString(2, tpName);
            ps.setString(3, filePath);

            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}