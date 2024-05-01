package com.example.harjoitus_15_16_gps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GPS-softa: ";
    private final int ACCESS_FINE_PERMISSION_CODE = 123;
    private final int ACCESS_COARSE_PERMISSION_CODE = 321;
    private final int ACCESS_INTERNET_PERMISSION_CODE = 999;

    private Context ctx;

    // Access fine, Access coarse, Internet
    private int[] permissions = {0, 0, 0};
    private boolean allGranted = false;

    // Apumuuttujat aikaisimmille arvoille
    private double previousLong = 0;
    private double previousLati = 0;
    private Set<String> previousLocations;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mLocation;

    private MapView mMapView = null;
    private MapController mMapController;
    private Marker currentPositionMarker;
    private List<Marker> savedMarkers;

    private Button saveLocationButton;
    private Button clearLocationsButton;
    private Button locateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ctx = this.getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        checkPermissions();
        confirmPermissions();
        Log.d(TAG, "Oikeudet: " + permissions[0] + permissions[1] + permissions[2] + allGranted);

        loadData();

        mMapView = findViewById(R.id.mapview);
        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(18);

        savedMarkers = new ArrayList<>();
        setSavedMarkers();

        saveLocationButton = findViewById(R.id.button_save_location);
        saveLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestLocationUpdate(false);
            }
        });

        clearLocationsButton = findViewById(R.id.button_clear_locations);
        clearLocationsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });

        locateButton = findViewById(R.id.button_locate);
        locateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLocation();
            }
        });

        // Asetetaan kuuntelija sijainnille ja päivitetään arvoja ja kartan tämänhetkistä merkkiä sen mukaan
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                Log.d(TAG, "Paikkatieto muuttui, nyt: " + location.getLatitude() + ", " + location.getLongitude());
                previousLati = location.getLatitude();
                previousLong = location.getLongitude();

                mLocation = location;
                GeoPoint gPt = new GeoPoint(location.getLatitude(), location.getLongitude());

                Marker newPositionMarker = new Marker(mMapView);
                newPositionMarker.setPosition(gPt);
                newPositionMarker.setIcon(getResources().getDrawable(R.drawable.ic_baseline_fmd_good_24_blue));
                newPositionMarker.setTitle("OLET TÄSSÄ");
                newPositionMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);
                mMapView.getOverlays().remove(currentPositionMarker);
                currentPositionMarker = newPositionMarker;
                mMapView.getOverlays().add(newPositionMarker);
            }
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
                Toast.makeText(MainActivity.this, "Puhelimen sijaintitieto kytketty päälle", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderDisabled(String s) {
                Toast.makeText(MainActivity.this, "Puhelimen sijaintitieto tulee kytkeä päälle", Toast.LENGTH_SHORT).show();
            }
        };
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
        updateLocation();
    }

    // Päivittää tämänhetkisen sijainnin kartalle
    private void updateLocation() {
        if (previousLati == 0 && previousLong == 0) {
            requestLocationUpdate(true);
            if (mLocation != null) {
                GeoPoint gPt = new GeoPoint(previousLati, previousLong);
                mMapView.getController().setCenter(gPt);
                Marker newPositionMarker = new Marker(mMapView);
                newPositionMarker.setPosition(gPt);
                newPositionMarker.setIcon(getResources().getDrawable(R.drawable.ic_baseline_fmd_good_24_blue));
                newPositionMarker.setTitle("OLET TÄSSÄ");
                newPositionMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);
                mMapView.getOverlays().remove(currentPositionMarker);
                currentPositionMarker = newPositionMarker;
                mMapView.getOverlays().add(newPositionMarker);
            } else {
                Toast.makeText(MainActivity.this, "Paikkatieto ei vielä valmis... yritä uudelleen", Toast.LENGTH_SHORT).show();
            }
        } else {
            GeoPoint gPt = new GeoPoint(previousLati, previousLong);
            mMapView.getController().setCenter(gPt);
            Marker newPositionMarker = new Marker(mMapView);
            newPositionMarker.setPosition(gPt);
            newPositionMarker.setIcon(getResources().getDrawable(R.drawable.ic_baseline_fmd_good_24_blue));
            newPositionMarker.setTitle("OLET TÄSSÄ");
            newPositionMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);
            mMapView.getOverlays().remove(currentPositionMarker);
            currentPositionMarker = newPositionMarker;
            mMapView.getOverlays().add(newPositionMarker);
        }
    }

    // Pyytää paikannuspäivityksiä ja kutsuu sijainnin tallentavaa funktiota, jos def == false
    private void requestLocationUpdate(boolean def) {
        try {
            if (!allGranted) {
                confirmPermissions();
            }
            if (!def) {
                // mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
                if (mLocation != null) {
                    setNewSavedMarker(mLocation.getLatitude(), mLocation.getLongitude());
                    Toast.makeText(MainActivity.this, "Sijainti tallennettu onnistuneesti: " + mLocation.getLatitude() + ", " + mLocation.getLongitude(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Paikkatieto ei vielä valmis... yritä uudelleen", Toast.LENGTH_SHORT).show();
                }
            } else {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
            }
        } catch (SecurityException e) {
            Log.d(TAG, "Virhe: Sovelluksella ei ollut oikeuksia lokaatioon");
        }
    }

    // Asettaa mahdolliset aikaisemmilla käytöllä asetetut merkit kartalle
    private void setSavedMarkers() {
        if (previousLocations != null && previousLocations.size() > 0) {
            Log.d(TAG, "Löydettiin tallennettuja sijainteja");
            Iterator<String> it = previousLocations.iterator();
            while (it.hasNext()) {
                String[] loc = it.next().split(";");
                GeoPoint gPt = new GeoPoint(Double.parseDouble(loc[0]), Double.parseDouble(loc[1]));
                Marker newLocationMarker = new Marker(mMapView);
                newLocationMarker.setPosition(gPt);
                newLocationMarker.setIcon(getResources().getDrawable(R.drawable.ic_baseline_fmd_good_24_red));
                newLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);
                mMapView.getOverlays().add(newLocationMarker);
                savedMarkers.add(newLocationMarker);
            }
        } else {
            Log.d(TAG, "Ei löydetty tallennettuja sijainteja");
        }
    }

    // Tallentaa ja asettaa uuden tallennetun sijainnin merkin kartalle
    private void setNewSavedMarker(double latitude, double longitude) {
        String content = latitude + ";" + longitude;
        previousLocations.add(content);

        GeoPoint gPt = new GeoPoint(latitude, longitude);
        Marker newLocationMarker = new Marker(mMapView);
        newLocationMarker.setPosition(gPt);
        newLocationMarker.setIcon(getResources().getDrawable(R.drawable.ic_baseline_fmd_good_24_red));
        newLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);
        mMapView.getOverlays().add(newLocationMarker);
        savedMarkers.add(newLocationMarker);
    }

    // Poistaa kaikki tallennetut sijaintimerkit kartalta
    private void deleteSavedMarkers() {
        previousLocations.clear();
        for (Marker m : savedMarkers) {
            mMapView.getOverlays().remove(m);
        }
        savedMarkers.clear();
    }

    // Tarkastaa oikeudet ja pyytää niitä tarvittaessa
    public void checkPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            permissions[0] = 1;
        } else {
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, ACCESS_FINE_PERMISSION_CODE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            permissions[1] = 1;
        } else {
            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_COARSE_PERMISSION_CODE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET)
                == PackageManager.PERMISSION_GRANTED) {
            permissions[2] = 1;
        } else {
            checkPermission(Manifest.permission.INTERNET, ACCESS_INTERNET_PERMISSION_CODE);
        }
    }

    // Apufunktio oikeuden pyytämiselle
    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { permission }, requestCode);
            Log.d(TAG, "Pyydetty oikeus: "+ permission);
        }
    }

    // Asettaa tiedon oikeuden saamisesta ja pyytää tarvittaessa sitä
    public void confirmPermissions() {
        if (permissions[0] == 0) {
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, ACCESS_FINE_PERMISSION_CODE);
        }
        if (permissions[1] == 0)
        {
            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_COARSE_PERMISSION_CODE);
        }
        if (permissions[2] == 0)
        {
            checkPermission(Manifest.permission.INTERNET, ACCESS_INTERNET_PERMISSION_CODE);
        }
        checkPermissions();
        if (permissions[0] == 1 && permissions[1] == 1 && permissions[2] == 1){
            allGranted = true;
        }
    }

    // Luo Pop-up vahvistuksen siitä, haluaako käyttäjä poistaa kaikki tallennetut sijaintimerkit kartalta
    public void confirmDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(true);
        builder.setTitle("Vahvista sijaintien poisto");
        builder.setMessage("Oletko varma, että haluat poistaa kaikki tallennetut sijainnit?");
        builder.setPositiveButton("Vahvista",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteSavedMarkers();
                    }
                });
        builder.setNegativeButton("Peruuta", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Hakee mahdolliset aikaisemmat datat sijainnista ja talletetuista sijainneista
    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        float savedLati = sharedPreferences.getFloat("lati", 0f);
        float savedLong = sharedPreferences.getFloat("long", 0f);
        Set<String> savedLocs = sharedPreferences.getStringSet("locs", null);
        Log.d(TAG, "loadData lati:" + savedLati + " ja long: " + savedLong);
        previousLong = savedLong;
        previousLati = savedLati;
        if (savedLocs == null) {
            previousLocations = new HashSet<>();
        } else {
            previousLocations = savedLocs;
        }
    }

    // Tallentaa tämänhetkisen sijainnin ja uudet talletetut sijainnit
    private void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("lati", (float) previousLati);
        editor.putFloat("long", (float) previousLong);
        editor.putStringSet("locs", previousLocations);
        editor.apply();
    }

}