package com.example.harjoitus_12_kuvan_ottaminen;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import java.io.File;
import java.util.ArrayList;

public class CustomGalleryActivity extends AppCompatActivity {

    ArrayList<String> f = new ArrayList<>();
    File[] listFile;
    private String folderName = "MyPhotoDir";

    ViewPager mViewPager;
    ViewPagerAdapter mViewPagerAdapter;
    private static int currentPage = 0;
    PageListener pL = new PageListener();

    Button btnDelete;
    Button btnDeleteAll;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        getFromSdcard();
        mViewPager = findViewById(R.id.viewPagerMain);
        mViewPager.setOnPageChangeListener(pL);
        mViewPagerAdapter = new ViewPagerAdapter(this, f);
        mViewPager.setAdapter(mViewPagerAdapter);

        btnDelete = findViewById(R.id.btnDelete);
        btnDeleteAll = findViewById(R.id.btnDeleteAll);

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(MainActivity.TAG, "Painettu Poista kuva");
                 if (deleteFile(mViewPagerAdapter.imagePaths.get(currentPage))) {
                     Toast.makeText(getApplicationContext(), "Kuva poistettu onnistuneesti", Toast.LENGTH_LONG).show();
                 } else {
                     if (f.size() == 0) {
                         Toast.makeText(getApplicationContext(), "Ei Kuvia poistettavaksi", Toast.LENGTH_LONG).show();
                     }
                     Toast.makeText(getApplicationContext(), "Kuvaa ei saatu poistettua", Toast.LENGTH_LONG).show();
                 }
            }
        });

        btnDeleteAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(MainActivity.TAG, "Painettu Poista kaikki");
                if (deleteFileAll(f)) {
                    Toast.makeText(getApplicationContext(), "Kuvat poistettu onnistuneesti", Toast.LENGTH_LONG).show();
                } else {
                    if (f.size() == 0) {
                        Toast.makeText(getApplicationContext(), "Ei Kuvia poistettavaksi", Toast.LENGTH_LONG).show();
                    }
                    Toast.makeText(getApplicationContext(), "Kuvia ei saatu poistettua", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void getFromSdcard() {
        File file = new File(getExternalFilesDir(folderName), "/");
        Log.d(MainActivity.TAG, "Ollaan getFromSdcard");
        if (file.isDirectory()) {
            listFile = file.listFiles();
            for (int i = 0; i < listFile.length; i++) {
                Log.d(MainActivity.TAG, "" + listFile[i].getName());
                f.add(listFile[i].getAbsolutePath());
            }
        }
    }

    public boolean deleteFile(String filePath) {
        if (f.size() == 0) {
            return false;
        }
        try {
            File file = new File(filePath);
            if (file.delete()) {
                f.clear();
                getFromSdcard();
                mViewPagerAdapter.notifyDataSetChanged();
                Log.d(MainActivity.TAG, "Tiedosto poistettu");
                return true;
            } else {
                Log.d(MainActivity.TAG, "Tiedoston poisto epäonnistui");
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteFileAll(ArrayList<String> filePaths) {
        if (f.size() == 0) {
            return false;
        }
        try {
            for (String s : filePaths) {
                File file = new File(s);
                if (file.delete()) {
                    Log.d(MainActivity.TAG, "Tiedosto poistettu");
                } else {
                    Log.d(MainActivity.TAG, "Tiedoston poisto epäonnistui");
                    return false;
                }
            }
            f.clear();
            getFromSdcard();
            mViewPagerAdapter.notifyDataSetChanged();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static class PageListener extends ViewPager.SimpleOnPageChangeListener {
        public PageListener() {
            currentPage = 0;
        }

        public void onPageSelected(int position) {
            Log.i(MainActivity.TAG, "page selected " + position);
            currentPage = position;
        }
    }
}
