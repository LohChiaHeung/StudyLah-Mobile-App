package my.edu.utar.studylah;

import android.app.Application;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PDFBoxResourceLoader.init(getApplicationContext());  // <- Important for PDFBox
    }
}
