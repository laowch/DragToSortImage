package com.laowch.dragtosort;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {


    private static final int REQUEST_CODE_TAKEN_PHOTO_CAMERA = 0x01;

    private static final int REQUEST_CODE_TAKEN_PHOTO_GALLERY = 0x02;

    DraggableImageLayout imageLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.add_picture).setOnClickListener(this);
        imageLayout = (DraggableImageLayout) findViewById(R.id.image_layout);
        imageLayout.setHoverView((ImageView) findViewById(R.id.hover_view));
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_picture:
                onTakenGalleryPhoto();
                break;
        }
    }


    public void onTakenGalleryPhoto() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_CODE_TAKEN_PHOTO_GALLERY);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_TAKEN_PHOTO_CAMERA: {
                    if (data != null) {
                        final Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                        onImageTaken(bitmap);
                        break;
                    }
                }
                case REQUEST_CODE_TAKEN_PHOTO_GALLERY: {
                    if (data != null) {
                        if (data.getData() == null) {
                            ClipData clipData = data.getClipData();
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                ClipData.Item item = clipData.getItemAt(i);
                                new GetBitmapFromUriTask(this, item.getUri(), new GetBitmapFromUriTask.IOnImageTakenListener() {
                                    @Override
                                    public void onImageTaken(final Bitmap pBitmap) {
                                        MainActivity.this.onImageTaken(pBitmap);
                                    }
                                }).execute();
                            }
                        } else {
                            new GetBitmapFromUriTask(this, data.getData(), new GetBitmapFromUriTask.IOnImageTakenListener() {
                                @Override
                                public void onImageTaken(final Bitmap pBitmap) {
                                    MainActivity.this.onImageTaken(pBitmap);
                                }
                            }).execute();
                        }

                        break;
                    }
                }
            }
        }
    }


    private void onImageTaken(Bitmap pBitmap) {
        if (pBitmap == null) {
            Toast.makeText(this, "image decode error", Toast.LENGTH_LONG).show();
            return;
        }
        DisplayMetrics dm = getResources().getDisplayMetrics();

        int width = dm.widthPixels;
        int height = pBitmap.getHeight() * dm.widthPixels / pBitmap.getWidth();

        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(width, height));
        imageView.setImageBitmap(BitmapUtils.resizeBitmap(pBitmap, width, height));

        imageLayout.addView(imageView);
    }


}
