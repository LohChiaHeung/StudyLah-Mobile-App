package my.edu.utar.studylah;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PomodoroActivity extends AppCompatActivity {

    private ImageButton btnLoopToggle;
    private boolean isLooping = true;

    private TextView timerText, titleText, taskText, tvStartTime, tvEndTime;
    private CountDownTimer countDownTimer;
    private boolean isStudy = true;
    private boolean isTimerRunning = false;
    //10 sec for demo
    private long studyTimeInMillis = 10 * 1000;
    private long breakTimeInMillis = 10 * 1000;

    //25 minutes for working session, 5 minutes for break session
//    private long studyTimeInMillis = 25 * 60 * 1000;
//    private long breakTimeInMillis = 5 * 60 * 1000;
    private long timeLeftInMillis = studyTimeInMillis;
    private int completedTasks = 0, totalTasks = 10; // Task Tracking
    private int musicResumePosition = 0;
    private boolean isUsingCustomTrack = false;
    private boolean isPlaying = false;
    private static final int SESSION_STUDY = 0;
    private static final int SESSION_BREAK = 1;
    private static final int SESSION_COMPLETED = 2;

    private final int[] musicTracks = {R.raw.studying_bgm, R.raw.productivity_bgm};
    private final String[] musicNames = {"Calm", "Productivity"};

    private List<Uri> customTrackUris = new ArrayList<>();
    private List<String> customTrackNames = new ArrayList<>();
    private List<Uri> allTrackUris = new ArrayList<>();
    private List<String> allTrackNames = new ArrayList<>();
    private int currentTrackGlobalIndex = 0;

    private MediaPlayer mediaPlayer;

    private ImageButton btnPrev, btnPlayPause, btnNext, btnVolume, btnPomodoroToggle;

    //SeekBar Update Thread
    private SeekBar musicSeekBar;
    private Handler seekBarHandler;
    private Runnable seekBarRunnable;
    private int currentTrackIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro);

        showAddTasksDialog(); //Prompt user to input the number of tasks first
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(PomodoroActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish(); //
        });

        timerText = findViewById(R.id.timerText);
        titleText = findViewById(R.id.titleText);
        taskText = findViewById(R.id.todaysTaskText);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvEndTime = findViewById(R.id.tvEndTime);
        musicSeekBar = findViewById(R.id.musicSeekBar);

        btnPrev = findViewById(R.id.btnPrev);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnVolume = findViewById(R.id.btnVolume);
        btnPomodoroToggle = findViewById(R.id.btnPomodoroToggle);
        btnLoopToggle = findViewById(R.id.btnLoopToggle);

        findViewById(R.id.musicButton).setOnClickListener(v -> openMusicDialog());
        findViewById(R.id.btnAdjustTime).setOnClickListener(v -> showTimeAdjustmentDialog());

        ImageButton btnAddTask = findViewById(R.id.btnAddTask);
        btnAddTask.setOnClickListener(v -> showAddTasksDialog());


        btnPomodoroToggle.setOnClickListener(v -> {
            // Check if the timer is NOT currently running
            if (!isTimerRunning) {
                // If all tasks are completed, reset everything before starting new session
                if (completedTasks >= totalTasks) {
                    completedTasks = 0;
                    timeLeftInMillis = studyTimeInMillis;
                    isStudy = true; // Reset to study mode
                    updateSessionVisual(SESSION_STUDY); //Change Pic back so 'Study Pic'
                    updateTaskText(); // Update task count display
                    updateUI();  // Update session image and title
                    updateTimerText(); // Reset timer label
                }

                //Start the countdown timer
                startTimer();

                //Change the 'Play' to 'Stop' icon
                btnPomodoroToggle.setImageResource(R.drawable.stop_button);
                //Show the feedback to the user
                Toast.makeText(this, "Timer started!", Toast.LENGTH_SHORT).show();

                // Start background music from last position if not already playing
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(musicResumePosition); //Resume from last position
                    mediaPlayer.start();
                    isPlaying = true;
                    startSeekBarUpdate(); //Start the timeline updates
                }
            } else {
                //If timer is already running, stop it
                stopTimer();
                btnPomodoroToggle.setImageResource(R.drawable.playbutton);    // Switch back to play icon
                Toast.makeText(this, "Timer stopped!", Toast.LENGTH_SHORT).show();

                // Pause the background music and save current position
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    musicResumePosition = mediaPlayer.getCurrentPosition(); //Save position
                    mediaPlayer.pause();
                    isPlaying = false;
                    stopSeekBarUpdate(); //Stop updating the music timeline
                }
            }
        });

        btnLoopToggle.setOnClickListener(v -> {
            isLooping = !isLooping;

            if (mediaPlayer != null) {
                mediaPlayer.setLooping(isLooping);
            }

            // Change the button image based on the loop state
            if (isLooping) {
                btnLoopToggle.setImageResource(R.drawable.looping_button); // enabled image
                Toast.makeText(this, "Looping enabled", Toast.LENGTH_SHORT).show();
            } else {
                btnLoopToggle.setImageResource(R.drawable.disablelooping_button); // disabled image
                Toast.makeText(this, "Looping disabled", Toast.LENGTH_SHORT).show();
            }
        });

        loadCustomTracksFromLocal();

        // Listen for user interactions on the music SeekBar (progress slider)
        musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            // Called when SeekBar progress changes
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // If the user is manually dragging the SeekBar
                if (fromUser && mediaPlayer != null) {
                    // Move the music playback to the selected position
                    mediaPlayer.seekTo(progress);
                    // Save current position so it resumes correctly if paused later
                    musicResumePosition = progress;
                    // Convert progress (in milliseconds) to minutes and seconds
                    int minutes = (progress / 1000) / 60;
                    int seconds = (progress / 1000) % 60;
                    // Update the start time TextView
                    tvStartTime.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}   // called when user starts touching the SeekBar
            @Override public void onStopTrackingTouch(SeekBar seekBar) {} // called when user stops touching the SeekBar
        });

        seekBarHandler = new Handler(); // Used to update the SeekBar regularly every 0.5 seconds while music is playing
        updateTimerText(); // Update the Pomodoro timer display (e.g., 25:00 → 24:59)
        updateTaskText(); // Update the "Today's Task" progress text (e.g., 2 / 5)
        setUpDefaultTrack(currentTrackIndex); // Set up default background music track (e.g., Calm)

        // Play or Pause Music
        btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    musicResumePosition = mediaPlayer.getCurrentPosition(); // Save position
                    mediaPlayer.pause();
                    isPlaying = false;
                    stopSeekBarUpdate(); // Stop SeekBar UI updates
                } else {
                    mediaPlayer.seekTo(musicResumePosition); // Resume from last position
                    mediaPlayer.start();
                    isPlaying = true;
                    startSeekBarUpdate(); //Resume SeekBar UI updates
                }
            }
        });

        // Next Music Track
        btnNext.setOnClickListener(v -> {
            if (!allTrackUris.isEmpty()) {
                // Move to next track index in the global playlist (loop back to 0 if at the end)
                currentTrackGlobalIndex = (currentTrackGlobalIndex + 1) % allTrackUris.size();
                musicResumePosition = 0; // Reset playback to beginning (new song)
                playFromGlobalPlaylist();  // Load and play new track
            }
        });

        // Previous Music Track
        btnPrev.setOnClickListener(v -> {
            if (!allTrackUris.isEmpty()) {
                // Move to previous track index (loop to end if at beginning)
                currentTrackGlobalIndex = (currentTrackGlobalIndex - 1 + allTrackUris.size()) % allTrackUris.size();
                musicResumePosition = 0;
                playFromGlobalPlaylist(); // Load and play previous track
            }
        });

        // Open System Volume Control UI
        btnVolume.setOnClickListener(v -> {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                // Triggers the system’s volume adjustment overlay without changing the volume
                audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
            }
        });
    }

    // Set the total duration of the current playing track (displayed on the right side of the seek bar)
    private void updateEndTime() {
        if (mediaPlayer != null) {
            int duration = mediaPlayer.getDuration(); // ⏱ Get total length in milliseconds
            int minutes = (duration / 1000) / 60;
            int seconds = (duration / 1000) % 60;
            tvEndTime.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)); // Display in mm:ss format
        }
    }

    //Continuously updates the seek bar and time labels when user play the music
    private void startSeekBarUpdate() {
        if (mediaPlayer == null) return;
        // Set SeekBar maximum to match music track length
        musicSeekBar.setMax(mediaPlayer.getDuration());
        // Set total duration label once (right side of bar)
        updateEndTime();

        // Define a runnable task that updates the seek bar and current time (left side of bar)
        seekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    musicSeekBar.setProgress(currentPosition);
                    int minutes = (currentPosition / 1000) / 60; // Current time in ms
                    int seconds = (currentPosition / 1000) % 60;  // Update SeekBar progress
                    tvStartTime.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)); // Format current time to mm:ss and display
                    seekBarHandler.postDelayed(this, 500); // Schedule next update after 500 milliseconds (0.5seconds) [Low Battery use] [100ms require higher battery usage]
                    //If doesn't use the postDelayed, the runnable will only run once (not continuously)
                }
            }
        };
        // Start updating
        seekBarHandler.post(seekBarRunnable);
    }

    // Stops updating seek bar
    private void stopSeekBarUpdate() {
        seekBarHandler.removeCallbacks(seekBarRunnable); // Remove all scheduled updates
    }

    //Start Timer
    private void startTimer() {
        updateSessionVisual(isStudy ? SESSION_STUDY : SESSION_BREAK);

        if (countDownTimer != null) {
            countDownTimer.cancel(); // cancel existing timer if any
        }

        updateTimerText(); // Refresh time display
        isTimerRunning = true;

        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(musicResumePosition); // Resume from saved position (so that it will not start from 0.00)
            mediaPlayer.start();
            isPlaying = true;
            startSeekBarUpdate();
        }

        //CountDown Timer (by 1 second)
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            // Called every second
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText(); //Update timer display
            }

            // Called when time finishes
            public void onFinish() {
                if (!isTimerRunning) return;

                final boolean wasStudySession = isStudy;

                isStudy = !isStudy; // Switch session (study <-> break)

                updateUI();
                updateTaskText();

                // Pause music and save position
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    musicResumePosition = mediaPlayer.getCurrentPosition();
                    mediaPlayer.pause();
                    isPlaying = false;
                    stopSeekBarUpdate();
                }

                // If session was study, increase completed task count
                if (wasStudySession) {
                    completedTasks++;
                    updateTaskText();

                    // All tasks completed!
                    if (completedTasks >= totalTasks) {
                        updateSessionVisual(SESSION_COMPLETED);
                        Toast.makeText(PomodoroActivity.this, "Congratulations on completing all the tasks!", Toast.LENGTH_LONG).show();

                        // Play success sound
                        MediaPlayer congratSound = MediaPlayer.create(PomodoroActivity.this, R.raw.celebration_sound);
                        congratSound.start();
                        congratSound.setOnCompletionListener(MediaPlayer::release);

                        // Stop music
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            mediaPlayer.pause();
                            mediaPlayer.seekTo(0);
                            stopSeekBarUpdate();
                        }

                        // Update UI
                        titleText.setText("Completed Tasks!");
                        btnPomodoroToggle.setImageResource(R.drawable.playbutton);
                        isTimerRunning = false;

                        // Reset timer state for new start
                        timeLeftInMillis = studyTimeInMillis;
                        isStudy = true; // So that next time user clicks play, it starts a Study session again
                        updateTimerText();

                        timerText.setText("00:00");
                        return; // Don't proceed to next session
                    }
                }

                // Play ticking sound and transition
                playClockEffect(wasStudySession);

                new Handler().postDelayed(() -> {
                    timeLeftInMillis = isStudy ? studyTimeInMillis : breakTimeInMillis;
                    updateTimerText();

                    // Auto-resume for study session
                    if (isStudy && mediaPlayer != null) {
                        mediaPlayer.seekTo(musicResumePosition);
                        mediaPlayer.start();
                        isPlaying = true;
                        startSeekBarUpdate();
                    }

                    startTimer(); // Start next session
                }, 5000);
            }
        }.start();
    }

    private void stopTimer() {
        if (countDownTimer != null) countDownTimer.cancel(); // Cancel running timer
        isTimerRunning = false;

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            musicResumePosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
            isPlaying = false;
            stopSeekBarUpdate();
        }
    }

    //Update the countdown label in mm:ss format
    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    // Update UI based on study/break mode (Change the Image)
    private void updateUI() {
        titleText.setText(isStudy ? "Study Time" : "Break Time");

        ImageView timerIcon = findViewById(R.id.timerIcon);
        if (isStudy) {
            timerIcon.setImageResource(R.drawable.study_image_removebg); //Show study image
        } else {
            timerIcon.setImageResource(R.drawable.break_image); //Show break image
        }
    }


    //Show the tasks completed and the total tasks that needed to be completed
    private void updateTaskText() {
        taskText.setText(completedTasks + " / " + totalTasks);
    }

    //Adjust study/break duration dialog
    private void showTimeAdjustmentDialog() {
        // Inflate the custom layout containing two input fields
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_adjust_time, null);
        final EditText inputStudy = dialogView.findViewById(R.id.inputStudy); // Input for study time
        final EditText inputBreak = dialogView.findViewById(R.id.inputBreak); // Input for break time

        // Create a custom title
        TextView title = new TextView(this);
        title.setText("Adjust Timer");
        title.setTextSize(24);
        title.setTypeface(ResourcesCompat.getFont(this, R.font.lexend_regular), Typeface.BOLD);
        title.setPadding(65, 32, 32, 16);
        title.setTextColor(Color.BLACK);

        // Create the AlertDialog with the custom view and title
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCustomTitle(title)
                .setView(dialogView) // Set the body layout (EditTexts)
                .setPositiveButton("Save", (dialogInterface, which) -> {
                    try {
                        // Get input values and convert to milliseconds
                        int studyMinutes = Integer.parseInt(inputStudy.getText().toString());
                        int breakMinutes = Integer.parseInt(inputBreak.getText().toString());
                        studyTimeInMillis = studyMinutes * 60 * 1000;
                        breakTimeInMillis = breakMinutes * 60 * 1000;
                        // If the timer is not currently running, update the remaining time
                        if (!isTimerRunning) {
                            timeLeftInMillis = isStudy ? studyTimeInMillis : breakTimeInMillis;
                            updateTimerText();
                        }
                        Toast.makeText(this, "Timer updated!", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        // Handle invalid input
                        Toast.makeText(this, "Please enter valid numbers / Fill-in all the required field.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();  // store it in a variable

        // Customize the button styles after dialog is created
        dialog.setOnShowListener(dlg -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            if (positive != null) {
                positive.setTextSize(18); // Change font size
                positive.setTextColor(Color.parseColor("#7DA0FF"));
                positive.setTypeface(ResourcesCompat.getFont(this, R.font.lexend_regular));
            }

            if (negative != null) {
                negative.setTextSize(18);
                negative.setTextColor(Color.parseColor("#7DA0FF"));
                negative.setTypeface(ResourcesCompat.getFont(this, R.font.lexend_regular));
            }
        });

        dialog.show(); // Show the dialog on screen
    }

    //Play the clock sound effect
    private void playClockEffect(boolean wasStudySession) {
        ImageView clock = findViewById(R.id.timerIcon);

        // Choose sound based on session that just ended
        int soundResId = wasStudySession ? R.raw.break_sound : R.raw.continuestudying_sound;

        // Create and start looping sound
        MediaPlayer tick = MediaPlayer.create(this, soundResId);
        tick.setLooping(true);
        tick.start();

        // Stop the sound after 5 seconds using a Handler
        new Handler().postDelayed(() -> {
            if (tick.isPlaying()) {
                tick.stop();
                tick.release();
            }
        }, 5000); // Play for 5 seconds

        // Toast message
        String message = wasStudySession ? "Break Time!" : "Back to Work!";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    // Asks user to enter a custom name when uploading custom music
    private void openNameInputDialog(Uri musicUri) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_music_name, null);
        EditText editMusicName = dialogView.findViewById(R.id.editMusicName);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancel);
        TextView btnSave = dialogView.findViewById(R.id.btnSave);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = editMusicName.getText().toString().trim();
            if (!name.isEmpty()) {
                uploadCustomMusicToLocal(musicUri, name);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }


    //Save the selected music file to internal storage
    private void uploadCustomMusicToLocal(Uri musicUri, String customName) {
        try {
            //Create the custom_music folder
            File musicDir = new File(getFilesDir(), "custom_music");
            if (!musicDir.exists()) musicDir.mkdirs();

            //Create a new file inside the folder (mp3 format)
            File outFile = new File(musicDir, customName + ".mp3");

            //Read the selected file using its uri and save it into internal storage
            InputStream inputStream = getContentResolver().openInputStream(musicUri); //read the binary data of the file
            FileOutputStream outputStream = new FileOutputStream(outFile); //write mode

            byte[] buffer = new byte[1024]; //create buffer (temporary memory space) for data
            //Read from input and write to output
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }

            //Close them (so that the system resources are released properly)
            inputStream.close();
            outputStream.close();

            //Add this file to the track list
            Uri fileUri = Uri.fromFile(outFile);
            customTrackNames.add(customName);
            customTrackUris.add(fileUri);
            //Add it into the global playlist
            allTrackNames.add(customName);
            allTrackUris.add(fileUri);

            Toast.makeText(this, "Saved locally: " + customName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    //Load saved custom music files from storage
    private void loadCustomTracksFromLocal() {
        //clear all the existing music data (refresh the list)
        customTrackNames.clear();
        customTrackUris.clear();
        allTrackNames.clear();
        allTrackUris.clear();

        //Add the 2 default tracks into the global list
        allTrackNames.addAll(Arrays.asList(musicNames)); //'Calm', 'Productivity'
        allTrackUris.add(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.studying_bgm));
        allTrackUris.add(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.productivity_bgm));

        //Locate the custom music folder inside the internal storage
        File musicDir = new File(getFilesDir(), "custom_music");

        //If the folder exists, read the file
        if (musicDir.exists()) {
            File[] files = musicDir.listFiles(); //get all files inside the folder

            if (files != null) {
                //Loop through each file and filter only mp3 files
                for (File file : files) {
                    if (file.getName().endsWith(".mp3")) {
                        //Extract the name without the "mp3"
                        String name = file.getName().replace(".mp3", "");
                        // Convert the file into a URI so it can be played
                        Uri uri = Uri.fromFile(file);
                        //Add this file to the custom lists
                        customTrackNames.add(name); // e.g., "Ocean"
                        customTrackUris.add(uri); // actual file path to the mp3
                        //Add to the global lists (default + custom)
                        allTrackNames.add(name);
                        allTrackUris.add(uri);
                    }
                }
            }
        }
    }

    // Dialog to select, add or remove music
    private void openMusicDialog() {
        // Prepare the dialog view
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_music_selection, null);
        ListView musicListView = dialogView.findViewById(R.id.musicListView);
        TextView btnAdd = dialogView.findViewById(R.id.btnAdd);
        TextView btnRemove = dialogView.findViewById(R.id.btnRemove);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Prepare and number all tracks
        List<String> numberedOptions = new ArrayList<>();
        List<String> allTracks = new ArrayList<>(); // Keep track of actual names

        // Default tracks
        List<String> defaultTracks = Arrays.asList("Calm", "Productivity");
        for (int i = 0; i < defaultTracks.size(); i++) {
            numberedOptions.add((i + 1) + ". " + defaultTracks.get(i));
            allTracks.add(defaultTracks.get(i));
        }

        // Custom tracks
        for (int i = 0; i < customTrackNames.size(); i++) {
            String customName = customTrackNames.get(i);
            numberedOptions.add((i + 3) + ". " + customName + " - Custom");
            allTracks.add(customName);
        }

        // Set up a list adapter with custom styling (font, size, color)
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, numberedOptions) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextSize(18); // Set your desired font size
                textView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.lexend_regular)); // Optional: set font
                textView.setTextColor(Color.BLACK); // Optional: text color
                return textView;
            }
        };
        musicListView.setAdapter(adapter);

        // Create and show the dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // When a user selects a track
        musicListView.setOnItemClickListener((parent, view, position, id) -> {
            musicResumePosition = 0; // Always start from the beginning
            if (position < 2) { // Default tracks
                currentTrackIndex = position;
                isUsingCustomTrack = false;
                playSessionMusic();
            } else {
                isUsingCustomTrack = true;
                playCustomMusic(position - 2); // Adjust for custom index
            }
            dialog.dismiss();
        });

        // Add new custom music
        btnAdd.setOnClickListener(v -> {
            dialog.dismiss();
            openFilePicker(); // Launch file picker
        });

        // Remove existing custom music
        btnRemove.setOnClickListener(v -> {
            dialog.dismiss();
            removeCustomMusicDialog(); // Launch delete dialog
        });

        // Cancel button closes the dialog
        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    private void openFilePicker() {
        // Launch file picker for audio files only
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*"); // Filter for audio files
        intent.addCategory(Intent.CATEGORY_OPENABLE); // Only files the user can open
        startActivityForResult(Intent.createChooser(intent, "Select Music"), 1234); // Launch picker
    }

    //Handle the result from the file picker
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1234 && resultCode == RESULT_OK && data != null) {
            Uri musicUri = data.getData(); // Get the selected file URI
            openNameInputDialog(musicUri);  // Ask user to name the uploaded track
        }
    }

    //Play the default built-in music tracks (calm, productivity)
    private void playSessionMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + musicTracks[currentTrackIndex]);
        mediaPlayer = MediaPlayer.create(this, uri);
        mediaPlayer.setLooping(isLooping);
        mediaPlayer.seekTo(0);
        mediaPlayer.start();

        mediaPlayer.setOnCompletionListener(mp -> {
            if (!isLooping) {
                currentTrackGlobalIndex = (currentTrackGlobalIndex + 1) % allTrackUris.size();
                musicResumePosition = 0;
                playFromGlobalPlaylist();
            }
        });

        isPlaying = true;
        startSeekBarUpdate();
        updateEndTime();
        tvStartTime.setText("0:00");
    }


    //Play user-uploaded custom music tracks
    private void playCustomMusic(int index) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        Uri uri = customTrackUris.get(index);
        mediaPlayer = MediaPlayer.create(this, uri);
        mediaPlayer.setLooping(isLooping);
        mediaPlayer.seekTo(0); // Always start from 0 for new track
        mediaPlayer.start();

        mediaPlayer.setOnCompletionListener(mp -> {
            if (!isLooping) {
                currentTrackGlobalIndex = (currentTrackGlobalIndex + 1) % allTrackUris.size();
                musicResumePosition = 0;
                playFromGlobalPlaylist();
            }
        });

        isPlaying = true;
        startSeekBarUpdate();
        updateEndTime();
        tvStartTime.setText("0:00");
    }

    //Method to play music by URI
    private void playUri(Uri uri) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        mediaPlayer = MediaPlayer.create(this, uri);
        if (mediaPlayer == null) {
            Toast.makeText(this, "Failed to play track", Toast.LENGTH_SHORT).show();
            return;
        }

        mediaPlayer.setLooping(isLooping);
        mediaPlayer.seekTo(musicResumePosition);
        mediaPlayer.start();
        isPlaying = true;

        mediaPlayer.setOnCompletionListener(mp -> {
            if (!isLooping) {
                currentTrackGlobalIndex = (currentTrackGlobalIndex + 1) % allTrackUris.size();
                musicResumePosition = 0;
                playFromGlobalPlaylist();
            }
        });

        startSeekBarUpdate();
        updateEndTime();
        tvStartTime.setText("0:00");
    }

    //Go through all the music tracks (default + user custom music)
    private void playFromGlobalPlaylist() {
        if (allTrackUris.isEmpty()) return;
        isUsingCustomTrack = currentTrackGlobalIndex >= musicTracks.length;
        playUri(allTrackUris.get(currentTrackGlobalIndex));
    }

    // Dialog to remove uploaded music
    private void removeCustomMusicDialog() {
        if (customTrackNames.isEmpty()) {
            Toast.makeText(this, "No custom tracks to remove.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Inflate custom layout for dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_remove_music, null);
        ListView listView = dialogView.findViewById(R.id.removeMusicListView);

        // Create numbered list for display
        List<String> numberedTracks = new ArrayList<>();
        for (int i = 0; i < customTrackNames.size(); i++) {
            numberedTracks.add((i + 1) + ". " + customTrackNames.get(i));
        }

        // Set custom adapter with font and size
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, numberedTracks) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextSize(18);
                textView.setTextColor(Color.BLACK);
                textView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.lexend_regular));
                return textView;
            }
        };

        listView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Remove music track on click
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String name = customTrackNames.get(position);
            Uri uri = customTrackUris.get(position);
            File file = new File(uri.getPath());

            boolean deleted = file.exists() && file.delete();
            if (deleted) {
                customTrackNames.remove(position);
                customTrackUris.remove(position);

                int globalIndex = allTrackUris.indexOf(uri);
                if (globalIndex != -1) {
                    allTrackUris.remove(globalIndex);
                    allTrackNames.remove(globalIndex);
                }

                Toast.makeText(this, "Deleted " + name, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setUpDefaultTrack(int trackIndex) {
        isUsingCustomTrack = false;
        currentTrackIndex = trackIndex;
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + musicTracks[trackIndex]);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(this, uri);
        mediaPlayer.setLooping(isLooping);
        musicSeekBar.setProgress(0);
        musicSeekBar.setMax(mediaPlayer.getDuration());
        updateEndTime();
        tvStartTime.setText("0:00");
        isPlaying = false;
    }


    //Prompt the user to input the total tasks to complete
    private void showAddTasksDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_total_tasks, null);
        EditText inputTotalTasks = dialogView.findViewById(R.id.inputTotalTasks);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancel);
        TextView btnNext = dialogView.findViewById(R.id.btnNext);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnNext.setOnClickListener(v -> {
            try {
                int total = Integer.parseInt(inputTotalTasks.getText().toString());
                totalTasks = total;
                completedTasks = 0;
                updateTaskText();
                dialog.dismiss();

                // Show confirmation message
                Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    // Show appropriate image and label for each session type
    private void updateSessionVisual(int sessionType) {
        ImageView timerIcon = findViewById(R.id.timerIcon);
        switch (sessionType) {
            case SESSION_STUDY:
                titleText.setText("Study Time");
                timerIcon.setImageResource(R.drawable.study_image_removebg);
                break;
            case SESSION_BREAK:
                titleText.setText("Break Time");
                timerIcon.setImageResource(R.drawable.break_image);
                break;
            case SESSION_COMPLETED:
                titleText.setText("Completed Tasks!");
                timerIcon.setImageResource(R.drawable.congrats_image);
                break;
        }
    }
}

