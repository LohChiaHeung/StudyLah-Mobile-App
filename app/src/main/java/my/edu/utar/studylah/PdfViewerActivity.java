package my.edu.utar.studylah;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import com.github.chrisbanes.photoview.PhotoView;



public class PdfViewerActivity extends AppCompatActivity {

    private PhotoView imageView;
    private Button btnNext, btnPrev;

    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor fileDescriptor;
    private int pageIndex = 0;
    private int pageCount = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Hide status bar and navigation
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
            getSupportActionBar().show(); // re-enable it
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                returnResultAndFinish();
            }
        });

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());


        imageView = findViewById(R.id.pdfImageView);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);

        Uri uri = Uri.parse(getIntent().getStringExtra("pdf_uri"));
        openPdf(uri);

        btnNext.setOnClickListener(v -> showPage(pageIndex + 1));
        btnPrev.setOnClickListener(v -> showPage(pageIndex - 1));
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

        byte[] buffer = new byte[1024];
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
        imageView.setImageBitmap(null); // Clear previous

        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                currentPage.getWidth(), currentPage.getHeight(),
                android.graphics.Bitmap.Config.ARGB_8888
        );
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        imageView.setImageBitmap(bitmap);

        pageIndex = index;
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
