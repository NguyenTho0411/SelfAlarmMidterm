package hcmute.edu.vn.selfalarm.activities;

import android.media.MediaPlayer;

public class MyMediaPlayer extends MediaPlayer {
    private static MediaPlayer instance;

    // Create and return a singleton instance of the MediaPlayer
    public static MediaPlayer getInstance() {
        if (instance == null) {
            instance = new MediaPlayer();
        }
        return instance;
    }

    // Current track index (if using a playlist)
    public static int currentIndex = 0;

    // Flags to check the media player state
    public static boolean isPaused = true;
    public static boolean isStopped = true;

    // Method to reset the media player to its initial state
    public static void resetPlayer() {
        if (instance != null) {
            instance.reset();
            isPaused = true;
            isStopped = true;
            currentIndex = 0;
        }
    }

    // Override stop method to change state correctly
    @Override
    public void stop() {
        super.stop();
        isStopped = true;
        isPaused = false;
    }

    // Override pause method to change state correctly
    @Override
    public void pause() throws IllegalStateException {
        super.pause();
        isPaused = true;
        isStopped = false;
    }

    // Override start method to change state correctly
    @Override
    public void start() throws IllegalStateException {
        super.start();
        isPaused = false;
        isStopped = false;
    }

    // Release the player
    public static void releasePlayer() {
        if (instance != null) {
            instance.release();
            instance = null;
        }
    }
}
