package io.github.yusukeiwaki.ta_lesson5;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 100;

    private static final int SLIDESHOW_INTERVAL_MS = 2000; //2秒

    private static final String KEY_POSITION = "position";
    private static final int POSITION_UNSPECIFIED = -1;
    private static final String KEY_PLAYING = "playing";

    private Timer mTimer;
    private Cursor mCursor;
    private boolean mPlaying;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int position = POSITION_UNSPECIFIED;
        mPlaying = false;

        if (savedInstanceState != null) {
            position = savedInstanceState.getInt(KEY_POSITION, POSITION_UNSPECIFIED);
            mPlaying = savedInstanceState.getBoolean(KEY_PLAYING, false);
        }


        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                initializeCursor(position);
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            }
        } else { // Android 5系以下の場合
            initializeCursor(position);
        }

        findViewById(R.id.btn_previous).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moveCursorToPrevious();
                updateImageView();
            }
        });

        findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moveCursorToNext();
                updateImageView();
            }
        });

        View btnPlay = findViewById(R.id.btn_play);
        View btnPause = findViewById(R.id.btn_pause);

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mPlaying) {
                    startSlideshow();
                }
                mPlaying = true;
            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPlaying) {
                    stopSlideshow();
                }
                mPlaying = false;
            }
        });

        btnPlay.setVisibility(View.VISIBLE);
        btnPause.setVisibility(View.GONE);

        if (mPlaying) {
            startSlideshow();
        }
    }

    private void moveCursorToNext() {
        if (mCursor.getCount() > 0) {
            if (!mCursor.moveToNext()) {
                mCursor.moveToFirst();
            }
        }
    }

    private void moveCursorToPrevious() {
        if (mCursor.getCount() > 0) {
            if (!mCursor.moveToPrevious()) {
                mCursor.moveToLast();
            }
        }
    }

    private void updateImageView() {
        if (mCursor.getCount() > 0) {
            int fieldIndex = mCursor.getColumnIndex(MediaStore.Images.Media._ID);
            Long id = mCursor.getLong(fieldIndex);
            Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

            ImageView imageVIew = (ImageView) findViewById(R.id.imageView);
            imageVIew.setImageURI(imageUri);
        }
    }

    private void startSlideshow() {
        findViewById(R.id.btn_play).setVisibility(View.GONE);
        findViewById(R.id.btn_pause).setVisibility(View.VISIBLE);

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                moveCursorToNext();

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateImageView();
                    }
                });
            }
        }, SLIDESHOW_INTERVAL_MS, SLIDESHOW_INTERVAL_MS);
    }

    private void stopSlideshow() {
        findViewById(R.id.btn_play).setVisibility(View.VISIBLE);
        findViewById(R.id.btn_pause).setVisibility(View.GONE);

        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCursor(POSITION_UNSPECIFIED);
            } else {
                Toast.makeText(this, "許可してくれないと使えないよ", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void initializeCursor(int position) {
        // 画像の情報を取得する
        ContentResolver resolver = getContentResolver();
        mCursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
                null, // 項目(null = 全項目)
                null, // フィルタ条件(null = フィルタなし)
                null, // フィルタ用パラメータ
                null // ソート (null ソートなし)
        );

        if (position != POSITION_UNSPECIFIED) {
            mCursor.moveToPosition(position);
        } else {
            mCursor.moveToFirst();
        }

        updateImageView();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mCursor != null) {
            outState.putInt(KEY_POSITION, mCursor.getPosition());
        }
        outState.putBoolean(KEY_PLAYING, mPlaying);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        stopSlideshow();
        if (mCursor != null && mCursor.isClosed()) {
            mCursor.close();
            mCursor = null;
        }
        super.onDestroy();
    }
}
