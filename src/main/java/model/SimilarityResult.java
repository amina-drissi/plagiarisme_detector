package model;

public class SimilarityResult {

    private int id;
    private int analysisId;
    private String student1;
    private String student2;
    private double similarity;
    private String source;   // student or github
    private String details;

    public SimilarityResult() {}

    public SimilarityResult(int id, int analysisId, String student1, String student2,
                            double similarity, String source, String details) {
        this.id = id;
        this.analysisId = analysisId;
        this.student1 = student1;
        this.student2 = student2;
        this.similarity = similarity;
        this.source = source;
        this.details = details;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(int analysisId) {
        this.analysisId = analysisId;
    }

    public String getStudent1() {
        return student1;
    }

    public void setStudent1(String student1) {
        this.student1 = student1;
    }

    public String getStudent2() {
        return student2;
    }

    public void setStudent2(String student2) {
        this.student2 = student2;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}