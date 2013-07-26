package com.sth.kspxyy;

import android.app.Activity;
import android.database.DataSetObserver;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.sth.kspxyy.components.SubTitleTextView;
import com.sth.kspxyy.subtitle.Caption;
import com.sth.kspxyy.subtitle.FormatSRT;
import com.sth.kspxyy.subtitle.TimedTextObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

public class MediaPlayerActivity extends Activity implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnVideoSizeChangedListener, SurfaceHolder.Callback
        , MediaController.MediaPlayerControl {

    private int mVideoWidth;
    private int mVideoHeight;
    private MediaPlayer mMediaPlayer;
    private SurfaceView mPreview;
    private SurfaceHolder holder;
    private String path;
    private boolean mIsVideoSizeKnown = false;
    private boolean mIsVideoReadyToBePlayed = false;
    private ListView subTitleListView;

    private MediaController mMediaController;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.main);
        mPreview = (SurfaceView) findViewById(R.id.surface);
        subTitleListView = (ListView) findViewById(R.id.subtitle_list);
        holder = mPreview.getHolder();
        holder.addCallback(this);

        mMediaController = new MediaController(this);

        mPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mMediaController.isShowing()) {
                    mMediaController.hide();
                } else {
                    mMediaController.show();
                }
                return false;
            }
        });


    }

    private void playVideo() {
        doCleanUp();

        path = Environment.getExternalStorageDirectory() + "/Download/The.Big.Bang.Theory.S06E24.HDTV.x264-LOL.mp4";

        try {
            // Create a new media player and set the listeners
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(path);
            mMediaPlayer.setDisplay(holder);
            mMediaPlayer.prepare();
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        } catch (Exception e) {
            Log.e("", "error: " + e.getMessage(), e);
        }

        try {
            loadSubtitle();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to load subtitle.", Toast.LENGTH_LONG).show();
        }

        subTitleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Caption caption = ((SubTitleTextView) view).getCaption();
                mMediaPlayer.seekTo(caption.start.mseconds);
            }
        });
    }

    private void loadSubtitle() throws IOException {
        FormatSRT formatSRT = new FormatSRT();
        File dir = Environment.getExternalStorageDirectory();

        TimedTextObject vegas = formatSRT.parseFile("Vegas", new FileInputStream(dir.getPath() + "/Download/The.Big.Bang.Theory.S06E24.HDTV.x264-LOL.srt"));
        subTitleListView.setAdapter(new SubtitleAdapter(filterByLanguage(vegas.captions, "eng")));
    }

    private ArrayList<Caption> filterByLanguage(TreeMap<Integer, Caption> captions, String language) {
        ArrayList<Caption> results = new ArrayList<Caption>();
        for (Caption caption : captions.values()) {
            results.add(caption);
        }
        return results;
    }

    public void onBufferingUpdate(MediaPlayer arg0, int percent) {
        Log.d("", "onBufferingUpdate percent:" + percent);
    }

    public void onCompletion(MediaPlayer arg0) {
        Log.d("", "onCompletion called");
    }

    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.v("", "onVideoSizeChanged called");
        if (width == 0 || height == 0) {
            Log.e("", "invalid video width(" + width + ") or height(" + height + ")");
            return;
        }
        mIsVideoSizeKnown = true;
        mVideoWidth = width;
        mVideoHeight = height;
        if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
            startVideoPlayback();
        }
    }

    public void onPrepared(MediaPlayer mediaplayer) {
        Log.d("", "onPrepared called");
        mIsVideoReadyToBePlayed = true;
        if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
            startVideoPlayback();
        }
        mMediaController.setMediaPlayer(this);
        mMediaController.setAnchorView(mPreview);
        mMediaController.setEnabled(true);
        mMediaController.show();
    }

    public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
        Log.d("", "surfaceChanged called");
    }

    public void surfaceDestroyed(SurfaceHolder surfaceholder) {
        Log.d("", "surfaceDestroyed called");
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("", "surfaceCreated called");
        playVideo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaPlayer();
        doCleanUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
        doCleanUp();
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void doCleanUp() {
        mVideoWidth = 0;
        mVideoHeight = 0;
        mIsVideoReadyToBePlayed = false;
        mIsVideoSizeKnown = false;
    }

    private void startVideoPlayback() {
        Log.v("", "startVideoPlayback");
        holder.setFixedSize(mVideoWidth, mVideoHeight);
        mMediaPlayer.start();
    }

    @Override
    public void start() {
        mMediaPlayer.start();
    }

    @Override
    public void pause() {
        mMediaPlayer.pause();
    }

    @Override
    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        mMediaPlayer.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    private class SubtitleAdapter implements ListAdapter {
        private ArrayList<Caption> captions;

        private SubtitleAdapter(ArrayList<Caption> captions) {
            this.captions = captions;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public int getCount() {
            return captions.size();
        }

        @Override
        public Object getItem(int position) {
            return captions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new SubTitleTextView(MediaPlayerActivity.this);
            }

            ((SubTitleTextView) convertView).setCaption(captions.get(position));
            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
