package my.edu.utar.studylah;

public class QuestionModel {
    private String question;
    private String[] options;
    private String correctAnswer;
    private String selectedAnswer; // Optional

    public QuestionModel(String question, String[] options, String correctAnswer) {
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    public String getQuestion() { return question; }
    public String[] getOptions() { return options; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getSelectedAnswer() { return selectedAnswer; }

    public void setSelectedAnswer(String selectedAnswer) {
        this.selectedAnswer = selectedAnswer;
    }
}

