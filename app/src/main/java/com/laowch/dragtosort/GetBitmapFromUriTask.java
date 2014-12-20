package com.laowch.dragtosort;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;

import java.io.FileDescriptor;

/**
 * Created by lao on 14-6-20.
 */
public class GetBitmapFromUriTask extends AsyncTask<Void, Void, Bitmap> {
    Activity mContext;

    Uri mUri;

    IOnImageTakenListener mOnImageTakenListener;


    public GetBitmapFromUriTask(Activity pContext, Uri pUri, IOnImageTakenListener pOnImageTakenListener) {
        super();
        this.mContext = pContext;
        this.mUri = pUri;
        this.mOnImageTakenListener = pOnImageTakenListener;
    }

    @Override
    protected Bitmap doInBackground(final Void... pParams) {
        try {
            ParcelFileDescriptor parcelFileDescriptor = mContext.getContentResolver().openFileDescriptor(mUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return image;

        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(final Bitmap pBitmap) {
        super.onPostExecute(pBitmap);

        if (mContext != null) {
            if (mOnImageTakenListener != null) {
                mOnImageTakenListener.onImageTaken(pBitmap);
            }
        }
    }

    public interface IOnImageTakenListener {
        void onImageTaken(Bitmap pBitmap);
    }
}
