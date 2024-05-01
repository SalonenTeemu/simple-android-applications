package com.example.harjoitus_14_askelmittari;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "Askel softa: ";
    private static final int PERMISSION_CODE = 100;
    private static final int NEW_TARGET_ACTIVITY_REQUEST_CODE = 1;

    private SensorManager sensorManager;

    // Apumuuttujat
    private boolean running = false;
    private double totalSteps = 0;
    private double previousTotalSteps = 0;
    private double max = 0;
    private String date = "";

    TextView stepsTaken;
    TextView stepsMax;
    Button changeTarget;
    FloatingActionButton infoButton;
    CircularProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        stepsTaken = findViewById(R.id.stepsTaken);
        stepsMax = findViewById(R.id.stepsMax);
        progressBar = findViewById(R.id.progress_bar);

        // Ladataan mahdollinen vanha data
        loadData();
        // Asetetaan klikin kuuntelijat
        resetSteps();
        // Kysytään käyttäjältä lupa käyttää liikkumisdataa, jos tätä ei ole myönnetty aiemmin
        checkPermissions();

        // Asetetaan edistymispalkin arvo oikein
        progressBar.setProgressWithAnimation((float) previousTotalSteps);

        // Nappi tavoitteen vaihdosta avaa uuden aktiviteetin
        changeTarget = findViewById(R.id.buttonChangeTarget);
        changeTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NewTargetActivity.class);
                startActivityForResult(intent, NEW_TARGET_ACTIVITY_REQUEST_CODE);
            }
        });

        // Info-nappi tulostaa tiedon, koska askeleet on viimeksi nollattu
        infoButton = findViewById(R.id.floatingActionButton2);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (date.equals("")) {
                    Toast.makeText(MainActivity.this, "Ei aikaisempaa nollausta", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Edellinen nollaus: " + date, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // NewTargetActivityn palauttama arvo asetetaan uudeksi tavoitearvoksi ja päivitetään tiedot tämän pohjalta
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NEW_TARGET_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            Integer i = data.getIntExtra(NewTargetActivity.EXTRA_REPLY, 1);
            if (i != 1) {
                max = (float) i;
                saveData();
                loadData();
            }
        }
    }

    // Tarkastaa onko oikeudet annettu, jos ei, niin kysyy niitä checkPermission-metodissa
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACTIVITY_RECOGNITION)
                == PackageManager.PERMISSION_GRANTED) {
        } else {
            checkPermission(Manifest.permission.ACTIVITY_RECOGNITION, PERMISSION_CODE);
        }
    }

    // Kysyy oikeuksia, jos niitä ei annettu, muuten ilmoittaa, että oikeudet jo annettu
    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { permission }, requestCode);
        }
        else {
            Toast.makeText(MainActivity.this, "Liikkumisdataoikeudet jo annettu", Toast.LENGTH_SHORT).show();
        }
    }

    // Tekee menun MainActivityyn
    @Override
    public boolean onCreatePanelMenu(int featureId, @NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    // Reagoi menun painalluksiin
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_permission:
                checkPermission(Manifest.permission.ACTIVITY_RECOGNITION, PERMISSION_CODE);
                return true;
            case R.id.menu_istructions:
                Toast.makeText(MainActivity.this, "Pitkä painallus keskeltä nollaa askeleet", Toast.LENGTH_LONG).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Sensorikuuntelija, joka huomatessaan tapahtuman päivittää palkin arvoa
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (running) {
            totalSteps = event.values[0];
            int currentSteps = (int) (totalSteps - previousTotalSteps);
            stepsTaken.setText(String.valueOf(currentSteps));

            progressBar.setProgressWithAnimation((float) currentSteps);
            Log.d(TAG, "SensorChanged: " + currentSteps);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        running = true;
        Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepSensor == null) {
            Toast.makeText(this, "Askelsensoria ei ole käytettävissä kyseisellä laitteella", Toast.LENGTH_SHORT).show();
        }
        else {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    private void resetSteps() {
        stepsTaken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Paina keskeltä pitkään nollataksesi askeleet", Toast.LENGTH_SHORT).show();
            }
        });

        // Kun keskeltä painetaan pitkään, nollataan otetut askeleet, tallennetaan data ja päiväys ja päivitetään edistymispalkki
        stepsTaken.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                previousTotalSteps = totalSteps;
                stepsTaken.setText("0");
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    LocalDateTime dt = LocalDateTime.now();
                    DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                    date = dt.format(myFormatObj);
                }
                saveData();
                progressBar.setProgressWithAnimation(0);
                return true;
            }
        });
    }

    // Tallentaa SharedPreferences avulla askelmäärän ja asetetun tavoitearvon
    private void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("key1", (float) previousTotalSteps);
        editor.putFloat("key2", (float) max);
        editor.putString("key3", date);
        editor.apply();
    }

    // Lataa SharedPreferences avulla askelmäärän ja asetetun tavoitteen ja vaihtaa nämä tarvittaessa
    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        float savedNumber = sharedPreferences.getFloat("key1", 0f);
        float savedMax = sharedPreferences.getFloat("key2", 10000f);
        String savedDate = sharedPreferences.getString("key3", "");
        Log.d(TAG, "loadData: " + savedNumber + " ja max: " + savedMax + " ja date: " + savedDate);
        previousTotalSteps = savedNumber;
        max = savedMax;
        date = savedDate;
        changeTargetScore((int) max);
    }

    // Metodi vaihtaa tavoitearvon, sekä päivittää edistymispalkin tämän pohjalta
    private void changeTargetScore(Integer i) {
        stepsMax.setText("/" + i.toString());
        progressBar.setProgressMax(i.floatValue());
        progressBar.setProgressWithAnimation((int) (totalSteps - previousTotalSteps));
    }
}