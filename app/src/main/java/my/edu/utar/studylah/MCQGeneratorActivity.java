package my.edu.utar.studylah;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MCQGeneratorActivity extends AppCompatActivity {

    private static final int PICK_PDF_CODE = 1001;
    private TextView txtMcqOutput;
    private OkHttpClient client;
    private List<String> allSummaries = new ArrayList<>();
    private AlertDialog progressDialog;


    private final String OPENAI_API_KEY = "sk-proj-"; //need to change to own API Keys

    Uri pdfUri = null;
    String quizTitle = "Untitled Quiz";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mcq_generator);

        PDFBoxResourceLoader.init(getApplicationContext());
        client = new OkHttpClient();

        txtMcqOutput = findViewById(R.id.txtMcqOutput);

        // Get data passed from MainActivity
        Intent intent = getIntent();
        if (intent != null) {
            String uriStr = intent.getStringExtra("pdf_uri");
            quizTitle = intent.getStringExtra("quiz_title");

            if (uriStr != null) {
                pdfUri = Uri.parse(uriStr);
                try {
                    File file = FileUtil.from(this, pdfUri);
                    String extractedText = extractTextFromPdf(file);
                    List<String> chunks = splitTextIntoChunks(extractedText, 1000);
                    summarizeChunks(chunks, quizTitle);
                } catch (IOException e) {
                    Toast.makeText(this, "Failed to read selected PDF", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }
    }

    private void openPdfPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), PICK_PDF_CODE);
    }

    private void summarizeChunks(List<String> chunks, String title) {
        allSummaries.clear();
        final int[] completed = {0};

        for (int i = 0; i < chunks.size(); i++) {
            int index = i;
            String chunk = chunks.get(i);

            JSONObject json = new JSONObject();
            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();

            try {
                message.put("role", "user");
                message.put("content", "Summarize this for MCQ generation:\n" + chunk);
                messages.put(message);

                json.put("model", "gpt-3.5-turbo");
                json.put("messages", messages);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(MCQGeneratorActivity.this, "Summary API failed", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String result = parseOpenAiResponse(response);
                    synchronized (allSummaries) {
                        allSummaries.add(result);
                        completed[0]++;

                        if (completed[0] == chunks.size()) {
                            // All summaries done
                            String combinedSummary = String.join("\n", allSummaries);
                            generateMcqsFromSummary(combinedSummary, title);
                        }
                    }
                }
            });
        }
    }

    private String extractTextFromPdf(File pdfFile) {
        String parsedText = "";
        try {
            PDDocument document = PDDocument.load(pdfFile);
            PDFTextStripper stripper = new PDFTextStripper();
            parsedText = stripper.getText(document);
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return parsedText;
    }

    private List<String> splitTextIntoChunks(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        for (int i = 0; i < length; i += maxLength) {
            chunks.add(text.substring(i, Math.min(length, i + maxLength)));
        }
        return chunks;
    }

    private void generateMcqsFromSummary(String summary, String title) {
        // Show progress dialog
        runOnUiThread(() -> {
            progressDialog = new AlertDialog.Builder(MCQGeneratorActivity.this)
                    .setTitle("Generating Quiz")
                    .setMessage("Please wait while we generate your questions...")
                    .setCancelable(false)
                    .create();
            progressDialog.show();
        });

        JSONObject json = new JSONObject();
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();

        try {
            message.put("role", "user");
            message.put("content",
                    "Generate exactly 10 multiple choice questions based on the content below.\n" +
                            "Each question must have 4 options (A, B, C, D).\n" +
                            "After each question, write the correct answer in this format: Answer: X\n\n" +
                            "Content:\n" + summary);
            messages.put(message);
            json.put("model", "gpt-3.5-turbo");
            json.put("messages", messages);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                    Toast.makeText(MCQGeneratorActivity.this, "MCQ generation failed", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = parseOpenAiResponse(response);
                List<QuestionModel> questionList = parseMcqResult(result);

                runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();

                    if (questionList.isEmpty()) {
                        Toast.makeText(MCQGeneratorActivity.this, "Failed to generate valid questions. Please try another PDF.", Toast.LENGTH_LONG).show();
                    } else {
                        Intent intent = new Intent(MCQGeneratorActivity.this, QuizActivity.class);
                        intent.putExtra("generated_questions", new Gson().toJson(questionList));
                        intent.putExtra("pdf_title", title);
                        startActivity(intent);
                    }
                });
            }
        });
    }

    private List<QuestionModel> parseMcqResult(String gptOutput) {
        List<QuestionModel> questions = new ArrayList<>();

        String[] lines = gptOutput.split("\n");
        String questionText = null;
        String[] options = new String[4];
        int optionIndex = 0;
        String correctAnswer = null;

        for (String line : lines) {
            line = line.trim();

            // Start of a new question
            if (line.matches("^\\d+\\.\\s+.*")) {
                if (questionText != null && optionIndex == 4 && correctAnswer != null) {
                    questions.add(new QuestionModel(questionText, options, correctAnswer));
                }

                questionText = line.replaceFirst("^\\d+\\.\\s+", "").trim();
                options = new String[4];
                optionIndex = 0;
                correctAnswer = null;

            } else if (line.matches("^[A-Da-d][\\).:]\\s?.*") && optionIndex < 4) {
                // Handles A. / A) / A:
                options[optionIndex++] = line.substring(2).trim();

            } else if (line.toLowerCase().startsWith("answer:")) {
                correctAnswer = line.substring(7).trim().toUpperCase();
                if (correctAnswer.length() > 1) {
                    correctAnswer = correctAnswer.substring(0, 1); // Ensure it's just 'A', 'B', etc.
                }
            }
        }

        // Add last question
        if (questionText != null && optionIndex == 4 && correctAnswer != null) {
            questions.add(new QuestionModel(questionText, options, correctAnswer));
        }
        return questions;
    }

    private String parseOpenAiResponse(Response response) throws IOException {
        String body = response.body().string();

        try {
            JSONObject jsonObject = new JSONObject(body);

            // Check for choices array first
            if (jsonObject.has("choices")) {
                return jsonObject.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
            } else if (jsonObject.has("error")) {
                // Show error from OpenAI
                return "API Error: " + jsonObject.getJSONObject("error").getString("message");
            } else {
                return "Unexpected response format.";
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return "Error parsing response: " + body;
        }
    }

}

