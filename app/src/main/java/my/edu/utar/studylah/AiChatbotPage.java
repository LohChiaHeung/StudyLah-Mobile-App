package my.edu.utar.studylah;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.*;

public class AiChatbotPage extends AppCompatActivity {

    private LinearLayout messageContainer;
    private EditText userInput;
    private Button sendBtn;
    private ScrollView scrollView;
    private ImageButton imageButton;
    private ImageView attachedImagePreview;
    private ProgressBar loadingIndicator;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    private LinearLayout imagePreviewContainer;
    private ImageButton clearImageButton;
    private final List<JSONObject> conversationHistory = new ArrayList<>();



    private Bitmap selectedBitmap;
    private static final int IMAGE_PICK_CODE = 1001;
    private static final int CAMERA_CODE = 1002;
    private static final String apiKey = "Bearer sk-proj-"; //need to change to own API Keys
    private static final String apiUrl = "https://api.openai.com/v1/chat/completions";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chatbot);



        messageContainer = findViewById(R.id.messageContainer);
        userInput = findViewById(R.id.userInput);
        sendBtn = findViewById(R.id.sendBtn);
        scrollView = findViewById(R.id.scrollView);
        imageButton = findViewById(R.id.imageSelectBtn);
        attachedImagePreview = findViewById(R.id.attachedImagePreview);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);
        attachedImagePreview = findViewById(R.id.attachedImagePreview);
        clearImageButton = findViewById(R.id.clearImageButton);

        if (savedInstanceState != null) {
            try {
                String savedHistory = savedInstanceState.getString("chat_history");
                if (savedHistory != null) {
                    JSONArray jsonArray = new JSONArray(savedHistory);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String role = obj.getString("role");

                        String messageText = "";
                        Bitmap image = null;

                        Object contentObj = obj.get("content");
                        if (contentObj instanceof JSONArray) {
                            JSONArray contentArray = (JSONArray) contentObj;
                            for (int j = 0; j < contentArray.length(); j++) {
                                JSONObject item = contentArray.getJSONObject(j);
                                if ("text".equals(item.optString("type"))) {
                                    messageText = item.optString("text");
                                }
                            }
                        } else {
                            messageText = obj.optString("content");
                        }

                        addMessageBubble(messageText, "user".equals(role), null);
                        conversationHistory.add(obj);

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        clearImageButton.setOnClickListener(v -> {
            selectedBitmap = null;
            attachedImagePreview.setImageDrawable(null);
            imagePreviewContainer.setVisibility(View.GONE);
        });

        if (savedInstanceState == null) {
            String hello = "👋 Hello! I'm Albert Einstein, your personal AI-Chatbot Lecturer. How can I help you?";
            addMessageBubble(hello, false, null);

            try {
                JSONObject helloMsg = new JSONObject();
                helloMsg.put("role", "assistant");
                helloMsg.put("content", hello);
                conversationHistory.add(helloMsg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        sendBtn.setOnClickListener(v -> {
            String message = userInput.getText().toString().trim();
            if (!message.isEmpty() || selectedBitmap != null) {
                addMessageBubble(message, true, selectedBitmap);
                callChatGPTWithImage(message);
                userInput.setText("");
                selectedBitmap = null;
                attachedImagePreview.setImageDrawable(null);
                imagePreviewContainer.setVisibility(View.GONE);

            }
        });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                        if (bitmap != null) {
                            selectedBitmap = bitmap;
                            attachedImagePreview.setImageBitmap(bitmap);
                            imagePreviewContainer.setVisibility(View.VISIBLE);
                        }
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            selectedBitmap = bitmap;
                            attachedImagePreview.setImageBitmap(bitmap);
                            imagePreviewContainer.setVisibility(View.VISIBLE);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

        imageButton.setOnClickListener(v -> {
            String[] options = {"Take Photo", "Choose from Gallery"};
            new AlertDialog.Builder(this)
                    .setTitle("Select Image")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            cameraLauncher.launch(cameraIntent);
                        } else {
                            Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            galleryLauncher.launch(galleryIntent);
                        }
                    }).show();
        });


        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 100);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_CODE && data != null) {
                selectedBitmap = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
            } else if (requestCode == IMAGE_PICK_CODE && data != null) {
                Uri imageUri = data.getData();
                try {
                    selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (selectedBitmap != null) {
                attachedImagePreview.setImageBitmap(selectedBitmap);
                imagePreviewContainer.setVisibility(View.VISIBLE);
            }

        }
    }


    private void addMessageBubble(String text, boolean isUser, @Nullable Bitmap image) {

        LinearLayout verticalLayout = new LinearLayout(this);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);
        verticalLayout.setPadding(0, 12, 0, 12);

        LinearLayout horizontalLayout = new LinearLayout(this);
        horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);

        ImageView icon = new ImageView(this);
        icon.setImageResource(isUser ? R.drawable.ic_user : R.drawable.ic_bot);
        icon.setLayoutParams(new LinearLayout.LayoutParams(80, 80));

        LinearLayout bubbleLayout = new LinearLayout(this);
        bubbleLayout.setOrientation(LinearLayout.VERTICAL);
        bubbleLayout.setBackgroundResource(isUser ? R.drawable.chat_bubble_user : R.drawable.chat_bubble_bot);
        bubbleLayout.setPadding(24, 16, 24, 16);

        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bubbleParams.setMargins(20, 0, 20, 0);
        bubbleLayout.setLayoutParams(bubbleParams);

        if (text != null && !text.trim().isEmpty()) {
            TextView textView = new TextView(this);
            textView.setText(text);
            textView.setTextColor(getResources().getColor(android.R.color.black));
            textView.setMaxWidth((int)(getResources().getDisplayMetrics().widthPixels * 0.6)); // max 60% of screen
            textView.setLineSpacing(0, 1.2f);
            textView.setTextSize(16f);
            textView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            bubbleLayout.addView(textView);
        }

        if (image != null) {
            ImageView sentImage = new ImageView(this);
            sentImage.setImageBitmap(image);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(400, LinearLayout.LayoutParams.WRAP_CONTENT);
            imgParams.gravity = Gravity.END;
            imgParams.topMargin = 12;
            sentImage.setLayoutParams(imgParams);
            sentImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            sentImage.setAdjustViewBounds(true);
            bubbleLayout.addView(sentImage);
        }

        if (isUser) {
            horizontalLayout.setGravity(Gravity.END);
            horizontalLayout.addView(bubbleLayout);
            horizontalLayout.addView(icon);
        } else {
            horizontalLayout.setGravity(Gravity.START);
            horizontalLayout.addView(icon);
            horizontalLayout.addView(bubbleLayout);
        }

        verticalLayout.addView(horizontalLayout);
        messageContainer.addView(verticalLayout);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));

        if (!isUser) {
            TextView translatePrompt = new TextView(this);
            translatePrompt.setText("🌐 Translate ");
            translatePrompt.setTextSize(14f);
            translatePrompt.setPadding(0, 10, 20, 0);
            translatePrompt.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
            translatePrompt.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Choose Language");
                String[] languages = {"English", "Malay", "Chinese"};
                builder.setItems(languages, (dialog, which) -> {
                    translateText(text, languages[which], verticalLayout);
                });
                builder.show();
            });

            verticalLayout.addView(translatePrompt);
        }
    }



    private void translateText(String originalText, String targetLanguage, LinearLayout container) {
        String apiKey = "Bearer sk-proj-"; //need to change to own API Keys
        String apiUrl = "https://api.openai.com/v1/chat/completions";

        loadingIndicator.setVisibility(View.VISIBLE);

        String languagePrompt;
        switch (targetLanguage) {
            case "Malay":
                languagePrompt = "Translate the following text to Malay:\n" + originalText;
                break;
            case "Chinese":
                languagePrompt = "Translate the following text to Chinese:\n" + originalText;
                break;
            case "English":
                languagePrompt = "Translate the following text to English:\n" + originalText;
                break;
            default:
                languagePrompt = "Translate the following text to English:\n" + originalText;
                break;
        }

        JSONObject jsonBody = new JSONObject();
        try {
            JSONArray messages = new JSONArray();


            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "You are a helpful study assistant. Answer questions based on previous context and image if provided."));



            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", languagePrompt);
            messages.put(userMsg);


            jsonBody.put("model", "gpt-4o");
            jsonBody.put("messages", messages);
            jsonBody.put("max_tokens", 1000);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        RequestBody requestBody = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        OkHttpClient client = new OkHttpClient();
        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String translatedText = jsonResponse.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    translatedText = translatedText
                            .replaceAll("(?s)```.*?```", "")
                            .replaceAll("\\*\\*|__", "")
                            .replaceAll("_", "")
                            .replaceAll("\\*", "")
                            .replaceAll("^#{1,6}\\s*", "")
                            .replaceAll("(?m)^\\s*\\d+\\.\\s*", "• ")
                            .replaceAll("(?m)^\\s*[-*+]\\s*", "• ")
                            .replaceAll("\\r\\n|\\r", "\n")
                            .replaceAll("(?m)^\\s+", "")
                            .replaceAll("(?m)^\\s{2,}", "")
                            .replaceAll("(?<=\\n)(?=\\S)", "\n")
                            .replaceAll("\\n{3,}", "\n\n")
                            .replaceAll(" +", " ")
                            .replaceAll("\\\\\\(", "")
                            .replaceAll("\\\\\\)", "")
                            .replaceAll("\\\\", "")
                            .trim();

                    String finalTranslatedText = translatedText;
                    runOnUiThread(() -> {
                        LinearLayout horizontalLayout = new LinearLayout(this);
                        horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
                        horizontalLayout.setPadding(0, 12, 0, 12);

                        ImageView botIcon = new ImageView(this);
                        botIcon.setImageResource(R.drawable.ic_bot);
                        botIcon.setLayoutParams(new LinearLayout.LayoutParams(80, 80));


                        TextView translationView = new TextView(this);
                        translationView.setText("🌐 Translated to " + targetLanguage + ":\n" + finalTranslatedText.trim());
                        translationView.setBackgroundResource(R.drawable.chat_bubble_bot);
                        translationView.setPadding(24, 16, 24, 16);
                        translationView.setTextColor(getResources().getColor(android.R.color.black));
                        translationView.setTextSize(14f);

                        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                        bubbleParams.setMargins(20, 0, 20, 0);
                        translationView.setLayoutParams(bubbleParams);

                        horizontalLayout.addView(botIcon);
                        horizontalLayout.addView(translationView);


                        container.addView(horizontalLayout);
                        loadingIndicator.setVisibility(View.GONE);

                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Translation failed", Toast.LENGTH_SHORT).show();
                        loadingIndicator.setVisibility(View.GONE);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                loadingIndicator.setVisibility(View.GONE);
            }
        }).start();
    }




    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream);
        byte[] byteArray = stream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }


    private void callChatGPTWithImage(String userMessage) {
        loadingIndicator.setVisibility(View.VISIBLE);
        if (userMessage.isEmpty() && selectedBitmap == null) {
            addMessageBubble("⚠️ Please provide a message or photo.", false,null);
            loadingIndicator.setVisibility(View.GONE);
            return;
        }

        JSONArray contentArray = new JSONArray();

        try {
            if (selectedBitmap != null) {
                String base64Image = encodeImageToBase64(selectedBitmap);
                JSONObject imageObject = new JSONObject();
                imageObject.put("type", "image_url");
                JSONObject imageUrl = new JSONObject();
                imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
                imageObject.put("image_url", imageUrl);
                contentArray.put(imageObject);
            }

            if (!userMessage.isEmpty()) {
                JSONObject textObject = new JSONObject();
                textObject.put("type", "text");
                textObject.put("text", userMessage);
                contentArray.put(textObject);
            }

            JSONArray messages = new JSONArray();


            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", "You are a helpful study assistant. Analyze image and user question.");
            messages.put(systemMsg);


            for (JSONObject historyItem : conversationHistory) {
                messages.put(historyItem);
            }


            JSONObject currentUserMsg = new JSONObject();
            currentUserMsg.put("role", "user");
            currentUserMsg.put("content", contentArray);
            messages.put(currentUserMsg);


            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "gpt-4o");
            jsonBody.put("messages", messages);
            jsonBody.put("max_tokens", 1000);

            RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();

            new Thread(() -> {
                try {
                    Response response = client.newCall(request).execute();
                    String responseText = response.body().string();
                    Log.d("OpenAI_Response", responseText);

                    JSONObject json = new JSONObject(responseText);
                    String reply = json.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    conversationHistory.add(new JSONObject().put("role", "user").put("content", contentArray));


                    conversationHistory.add(new JSONObject().put("role", "assistant").put("content", reply));


                    reply = reply
                            .replaceAll("(?s)```.*?```", "")               // Remove code blocks
                            .replaceAll("\\*\\*|__", "")                  // Remove bold markers
                            .replaceAll("_", "")                          // Remove italic markers
                            .replaceAll("\\*", "")                        // Remove asterisks
                            .replaceAll("^#{1,6}\\s*", "")                // Remove markdown headers
                            .replaceAll("(?m)^\\s*\\d+\\.\\s*", "• ")     // Convert numbered lists to bullets
                            .replaceAll("(?m)^\\s*[-*+]\\s*", "• ")       // Convert bullet markdown to bullets
                            .replaceAll("\\r\\n|\\r", "\n")               // Normalize line breaks
                            .replaceAll("(?m)^\\s+", "")                  // Trim leading spaces on lines
                            .replaceAll("(?m)^\\s{2,}", "")               // Remove excessive line indentation
                            .replaceAll("(?<=\\n)(?=\\S)", "\n")          // Add newlines where needed before text
                            .replaceAll("\\n{3,}", "\n\n")                // Reduce multiple newlines to max 2
                            .replaceAll(" +", " ")                        // Collapse multiple spaces
                            .replaceAll("\\\\\\(", "")  // Remove \(
                            .replaceAll("\\\\\\)", "")  // Remove \)
                            .replaceAll("\\\\", "")     // Remove other backslashes (optional cleanup)
                            .replaceAll("^#{1,6}\\s*", "")
                            .trim();                                      // Final trim


                    String finalReply = reply;
                    runOnUiThread(() -> {
                        addMessageBubble(finalReply, false, null);
                        loadingIndicator.setVisibility(View.GONE);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        addMessageBubble(" Error: " + e.getMessage(), false, null);
                        loadingIndicator.setVisibility(View.GONE);
                    });
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            addMessageBubble(" Error: " + e.getMessage(), false,null);
            loadingIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        JSONArray jsonArray = new JSONArray(conversationHistory);
        outState.putString("chat_history", jsonArray.toString());
    }
}
