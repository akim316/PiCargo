package com.aw.picargo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.ImageView;

/**
 * Created by Alex on 6/18/2015.
 */
public class Thumbnail implements Parcelable {

    private Bitmap img;
    private int col;
    private int row;

    public Thumbnail(Bitmap img, int col, int row) {
        this.img = img;
        this.col = col;
        this.row = row;
    }

    public Thumbnail(Bitmap img) {
        this.img = img;
    }

    public Thumbnail(Parcel in) {
        img = in.readParcelable(null);
        col = in.readInt();
        row = in.readInt();
    }
    @Override
    public int describeContents() {
        Log.d("msg", "describeContents");
        return 0;

    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(img, 0);
        dest.writeInt(col);
        dest.writeInt(row);
    }

    public static final Parcelable.Creator<Thumbnail> CREATOR
            = new Parcelable.Creator<Thumbnail>() {
        public Thumbnail createFromParcel(Parcel in) {
            return new Thumbnail(in);
        }

        public Thumbnail[] newArray(int size) {
            return new Thumbnail[size];
        }
    };

    public Bitmap getImg() {
        return img;
    }

    public void setImg(Bitmap img) {
        this.img = img;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }
}
