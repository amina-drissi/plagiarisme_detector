package model;

import java.util.Date;

public class Submission {

    private int id;
    private int userId;
    private String tpName;
    private String filePath;
    private Date uploadDate;

    public Submission() {}

    public Submission(int id, int userId, String tpName, String filePath, Date uploadDate) {
        this.id = id;
        this.userId = userId;
        this.tpName = tpName;
        this.filePath = filePath;
        this.uploadDate = uploadDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTpName() {
        return tpName;
    }

    public void setTpName(String tpName) {
        this.tpName = tpName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Date getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(Date uploadDate) {
        this.uploadDate = uploadDate;
    }
}