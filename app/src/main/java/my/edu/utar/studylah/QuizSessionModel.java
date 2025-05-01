package my.edu.utar.studylah;

import java.util.List;

public class QuizSessionModel {
    private String date;
    private String pdfTitle; // Optional
    private List<QuestionModel> questions;

    public QuizSessionModel(String date, String pdfTitle, List<QuestionModel> questions) {
        this.date = date;
        this.pdfTitle = pdfTitle;
        this.questions = questions;
    }

    public String getDate() { return date; }
    public String getPdfTitle() { return pdfTitle; }
    public List<QuestionModel> getQuestions() { return questions; }
}
