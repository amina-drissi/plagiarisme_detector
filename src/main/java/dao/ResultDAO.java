package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import util.DBConnection;

public class ResultDAO {

    public void saveResult(int analysisId, String student1, String student2,
                           double similarity, String source, String details) {

        try {
            Connection conn = DBConnection.getConnection();

            String sql = "INSERT INTO similarity_results(analysis_id,student1,student2,similarity,source,details)"
                       + " VALUES(?,?,?,?,?,?)";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, analysisId);
            ps.setString(2, student1);
            ps.setString(3, student2);
            ps.setDouble(4, similarity);
            ps.setString(5, source);
            ps.setString(6, details);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}