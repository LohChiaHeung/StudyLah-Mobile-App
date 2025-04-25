// PdfViewerActivity.java
package my.edu.utar.studylah;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PdfViewerActivity extends AppCompatActivity implements GestureDetector.OnGestureListener {

    private PhotoView imageView;
    private TextView tvPageCounter, tvFileName;

    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor fileDescriptor;
    private int pageIndex = 0;
    private int pageCount = 0;
    private GestureDetectorCompat gestureDetector;

    // For smoother transitions between pages
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set fullscreen mode
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        if (getSupportActionBar() != null) {
            getSupportActionBar().show();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Handle back button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                returnResultAndFinish();
            }
        });

        // Initialize views
        imageView = findViewById(R.id.pdfImageView);
        tvPageCounter = findViewById(R.id.tvPageCounter);
        tvFileName = findViewById(R.id.tvFileName);
        View btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Initialize gesture detector
        gestureDetector = new GestureDetectorCompat(this, this);

        // Get PDF URI and open PDF
        Uri uri = Uri.parse(getIntent().getStringExtra("pdf_uri"));
        displayFileName(uri);
        openPdf(uri);
    }

    private void displayFileName(Uri uri) {
        String fileName = getFileNameFromUri(uri);
        if (fileName != null && !fileName.isEmpty()) {
            tvFileName.setText(fileName);
        } else {
            tvFileName.setText("Document");
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void openPdf(Uri uri) {
        try {
            File file = copyFileFromUri(uri);
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(fileDescriptor);
            pageCount = pdfRenderer.getPageCount();
            showPage(0);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private File copyFileFromUri(Uri uri) throws Exception {
        ContentResolver resolver = getContentResolver();
        InputStream input = resolver.openInputStream(uri);
        File outputFile = new File(getCacheDir(), "temp.pdf");
        FileOutputStream output = new FileOutputStream(outputFile);

        byte[] buffer = new byte[4096]; // Larger buffer for faster copying
        int len;
        while ((len = input.read(buffer)) > 0) {
            output.write(buffer, 0, len);
        }

        input.close();
        output.close();
        return outputFile;
    }

    private void showPage(int index) {
        if (pdfRenderer == null || index < 0 || index >= pageCount) return;

        if (currentPage != null) currentPage.close();

        currentPage = pdfRenderer.openPage(index);

        // Create bitmap with appropriate resolution
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                currentPage.getWidth(), currentPage.getHeight(),
                android.graphics.Bitmap.Config.ARGB_8888
        );

        // Render PDF page
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        imageView.setImageBitmap(bitmap);

        pageIndex = index;
        updatePageCounter();
    }

    private void updatePageCounter() {
        tvPageCounter.setText(String.format("Page %d of %d", pageIndex + 1, pageCount));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // Not used
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        // Not used
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        boolean result = false;
        try {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            // Check if horizontal swipe meets threshold criteria
            if (Math.abs(diffX) > Math.abs(diffY) &&
                    Math.abs(diffX) > SWIPE_THRESHOLD &&
                    Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                if (diffX > 0) {
                    // Right to left swipe, go to previous page
                    showPage(pageIndex - 1);
                } else {
                    // Left to right swipe, go to next page
                    showPage(pageIndex + 1);
                }
                result = true;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return result;
    }

    @Override
    protected void onDestroy() {
        try {
            if (currentPage != null) currentPage.close();
            if (pdfRenderer != null) pdfRenderer.close();
            if (fileDescriptor != null) fileDescriptor.close();
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    private void returnResultAndFinish() {
        Intent result = new Intent();
        // Make sure we're passing back the same folder ID that was passed to us
        result.putExtra(
                "current_folder_id",
                getIntent().getIntExtra("current_folder_id", -1)
        );
        setResult(RESULT_OK, result);
        finish();
    }
}