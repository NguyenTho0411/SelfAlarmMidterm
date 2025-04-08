package hcmute.edu.vn.selfalarm.fragments;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import hcmute.edu.vn.selfalarm.R;
import hcmute.edu.vn.selfalarm.activities.MyMediaPlayer;
import hcmute.edu.vn.selfalarm.adapter.SongListAdapter;
import hcmute.edu.vn.selfalarm.model.Song;
import hcmute.edu.vn.selfalarm.service.MediaPlayerService;

public class MusicPlayerFragment extends Fragment implements View.OnClickListener, SongListAdapter.OnSongClickListener {

    private RecyclerView recyclerView;
    private TextView noMusicTextView;
    private TextView songTitleTextView;
    private TextView currentTime, totalTime;
    private SeekBar seekBar;
    private ArrayList<Song> songsList = new ArrayList<>();
    private ImageButton playPauseBtn, nextBtn, prevBtn;
    private MediaPlayer mediaPlayer = MyMediaPlayer.getInstance();

    private String SERVICE_PLAY_SONG = "service_play_song";
    private String SERVICE_RESUME_SONG = "service_resume_song";
    private String SERVICE_PAUSE_SONG = "service_pause_song";
    private String SERVICE_NEXT_SONG = "service_next_song";
    private String SERVICE_PREV_SONG = "service_prev_song";
    private String SERVICE_SEEKBAR_SONG = "service_seekbar_song";
    private String SERVICE_SELECT_SONG = "service_select_song";

    public MusicPlayerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_player, container, false);

        noMusicTextView = view.findViewById(R.id.no_music_available);
        songTitleTextView = view.findViewById(R.id.currentSongTitle);
        currentTime = view.findViewById(R.id.current_time);
        totalTime = view.findViewById(R.id.total_time);
        seekBar = view.findViewById(R.id.seekbar);
        seekBar.setProgress(0);
        recyclerView = view.findViewById(R.id.recycler_view);
        playPauseBtn = view.findViewById(R.id.play_pause_btn);
        nextBtn = view.findViewById(R.id.next_btn);
        prevBtn = view.findViewById(R.id.prev_btn);
        playPauseBtn.setOnClickListener(this);
        nextBtn.setOnClickListener(this);
        prevBtn.setOnClickListener(this);

        checkExternalStoragePermission();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Touch and update the application UI
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    currentTime.setText(convertToMMS(mediaPlayer.getCurrentPosition() + ""));

                    if (songTitleTextView.getText() != songsList.get(MyMediaPlayer.currentIndex).getTitle()) {
                        songTitleTextView.setSelected(true);
                        songTitleTextView.setText(songsList.get(MyMediaPlayer.currentIndex).getTitle());
                    }

                    if (seekBar.getMax() != mediaPlayer.getDuration()) {
                        seekBar.setMax(mediaPlayer.getDuration());
                    }

                    playPauseBtn.setImageResource(R.drawable.baseline_pause_45);
                } else if (MyMediaPlayer.isStopped || MyMediaPlayer.isPaused) {
                    playPauseBtn.setImageResource(R.drawable.baseline_play_arrow_50);
                    songTitleTextView.setSelected(false);
                }
                new Handler().postDelayed(this, 50);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    Intent seekBarIntent = new Intent(getActivity(), MediaPlayerService.class);
                    seekBarIntent.setAction(SERVICE_SEEKBAR_SONG);
                    seekBarIntent.putExtra("current position", progress);
                    seekBarIntent.putExtra("current song", songsList.get(MyMediaPlayer.currentIndex));
                    getActivity().startService(seekBarIntent);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view.equals(playPauseBtn)) {
            if (MyMediaPlayer.isStopped && !songsList.isEmpty()) {
                playAudio();
            } else if (MyMediaPlayer.isPaused && !songsList.isEmpty()) {
                resumeAudio();
            } else if (!MyMediaPlayer.isPaused && !songsList.isEmpty()) {
                pauseAudio();
            }
        } else if (view.equals(prevBtn)) {
            if (!MyMediaPlayer.isStopped) {
                prevSong();
            }
        } else if (view.equals(nextBtn)) {
            if (!MyMediaPlayer.isStopped) {
                nextSong();
            }
        }
    }

    void playAudio() {
        MyMediaPlayer.isPaused = false;
        MyMediaPlayer.isStopped = false;

        Intent playInt = new Intent(getActivity(), MediaPlayerService.class);
        playInt.setAction(SERVICE_PLAY_SONG);
        playInt.putExtra("media", songsList.get(MyMediaPlayer.currentIndex));
        totalTime.setText(convertToMMS(songsList.get(MyMediaPlayer.currentIndex).getDuration()));
        getActivity().startService(playInt);
    }

    void resumeAudio() {
        MyMediaPlayer.isPaused = false;

        Intent resumeInt = new Intent(getActivity(), MediaPlayerService.class);
        resumeInt.setAction(SERVICE_RESUME_SONG);
        getActivity().startService(resumeInt);
    }

    void pauseAudio() {
        MyMediaPlayer.isPaused = true;

        Intent stopInt = new Intent(getActivity(), MediaPlayerService.class);
        stopInt.setAction(SERVICE_PAUSE_SONG);
        getActivity().startService(stopInt);
    }

    void prevSong() {
        MyMediaPlayer.isPaused = false;
        if (MyMediaPlayer.currentIndex == 0){
            MyMediaPlayer.currentIndex = songsList.size() - 1;
        }else {
            MyMediaPlayer.currentIndex--;
        }

        Intent prevInt = new Intent(getActivity(), MediaPlayerService.class);
        prevInt.setAction(SERVICE_PREV_SONG);
        prevInt.putExtra("media", songsList.get(MyMediaPlayer.currentIndex));
        totalTime.setText(convertToMMS(songsList.get(MyMediaPlayer.currentIndex).getDuration()));
        getActivity().startService(prevInt);
    }

    void nextSong() {
        MyMediaPlayer.isPaused = false;
        if (MyMediaPlayer.currentIndex == songsList.size() - 1){
            MyMediaPlayer.currentIndex = 0;
        }else {
            MyMediaPlayer.currentIndex++;
        }

        Intent nextInt = new Intent(getActivity(), MediaPlayerService.class);
        nextInt.setAction(SERVICE_NEXT_SONG);
        nextInt.putExtra("media", songsList.get(MyMediaPlayer.currentIndex));
        totalTime.setText(convertToMMS(songsList.get(MyMediaPlayer.currentIndex).getDuration()));
        getActivity().startService(nextInt);
    }

    void loadAudioFiles() {
        songsList = new ArrayList<>();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION};
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        ContentResolver contentResolver = getActivity().getContentResolver();
        Cursor cursor = contentResolver.query(uri, projection, selection, null, sortOrder);
        while (cursor != null && cursor.moveToNext()) {
            Song songData = new Song(cursor.getString(1), cursor.getString(0), cursor.getString(2));
            if (new File(songData.getPath()).exists())
                songsList.add(songData);
        }

        if (cursor != null) {
            cursor.close();
        }

        if (songsList.isEmpty()) {
            noMusicTextView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            recyclerView.setAdapter(new SongListAdapter(songsList, getActivity(), this));
        }
    }

    @Override
    public void onSongClick(Song song, int position) {
        MyMediaPlayer.isPaused = false;
        MyMediaPlayer.isStopped = false;
        MyMediaPlayer.currentIndex = position;

        Intent songSelectIntent = new Intent(getActivity(), MediaPlayerService.class);
        songSelectIntent.putExtra("media", song);
        songSelectIntent.setAction(SERVICE_SELECT_SONG);

        getActivity().startService(songSelectIntent);

        updateUI(song);
    }

    private void updateUI(Song song) {
        songTitleTextView.setText(song.getTitle());
        totalTime.setText(convertToMMS(song.getDuration()));
    }

    void checkExternalStoragePermission() {
        int selfPermission;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            selfPermission = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_MEDIA_AUDIO);
        else
            selfPermission = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);

        if (selfPermission == PackageManager.PERMISSION_GRANTED) {
            loadAudioFiles();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_MEDIA_AUDIO}, 123);
            else
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 123) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadAudioFiles();
            } else {
                ShowMessage("Permission denied...");
            }
        }
    }

    private void ShowMessage(String message) {
        Toast.makeText(getActivity(), "Service: " + message, Toast.LENGTH_LONG).show();
    }

    @SuppressLint("DefaultLocale")
    public static String convertToMMS(String duration) {
        long millis = Long.parseLong(duration);

        int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(millis);
        int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        if (minutes > 60) {
            int hours = minutes / 60;
            minutes %= 60;
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }

        return String.format("%02d:%02d", minutes, seconds);
    }
}
