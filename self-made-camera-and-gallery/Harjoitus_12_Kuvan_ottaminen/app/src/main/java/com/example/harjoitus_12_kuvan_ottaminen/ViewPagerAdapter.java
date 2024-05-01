package com.example.harjoitus_12_kuvan_ottaminen;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.Objects;

public class ViewPagerAdapter extends PagerAdapter {

    Context context;

    ArrayList<String> imagePaths = new ArrayList<>();

    LayoutInflater mLayoutInlfater;

    public ViewPagerAdapter(Context context, ArrayList<String> imagePaths) {
        this.context = context;
        this.imagePaths = imagePaths;
        mLayoutInlfater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return imagePaths.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Log.d(MainActivity.TAG, "Ollaan initializestate");
        View itemView = mLayoutInlfater.inflate(R.layout.galleryitem, container, false);
        ImageView imageView = itemView.findViewById(R.id.imageViewmain);

        Bitmap myBitMap = BitmapFactory.decodeFile(imagePaths.get(position));
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap rotatedBitmap = Bitmap.createBitmap(myBitMap, 0, 0, myBitMap.getWidth(), myBitMap.getHeight(), matrix, true);
        imageView.setImageBitmap(rotatedBitmap);

        Objects.requireNonNull(container).addView(itemView);

        return itemView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((LinearLayout) object);
    }

    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}
