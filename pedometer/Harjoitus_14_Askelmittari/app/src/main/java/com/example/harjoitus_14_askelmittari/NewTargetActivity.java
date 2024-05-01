package com.example.harjoitus_14_askelmittari;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class NewTargetActivity extends AppCompatActivity {
    public static final String EXTRA_REPLY = "com.example.android.roomtesti3112022.REPLY";

    private EditText mEditNumeroView;
    private FloatingActionButton mFobTakaisin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_target_activity);

        mEditNumeroView = findViewById(R.id.edit_numero);
        mFobTakaisin = findViewById(R.id.floatingActionButtonPrevious);

        mFobTakaisin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Asetetaan aseta-napille kuuntelija
        final Button button = findViewById(R.id.button_save);
        button.setOnClickListener(view -> {
            Intent replyIntent = new Intent();
            if (TextUtils.isEmpty(mEditNumeroView.getText())) {
                setResult(RESULT_CANCELED, replyIntent);
                Toast.makeText(
                        getApplicationContext(),
                        "Lisää lukuarvon asettaaksesi tavoitteen",
                        Toast.LENGTH_LONG).show();
            } else {
                try
                {
                    // Tarkastetaan onko syötetty arvo laillinen, jos on niin palataan MainActivityyn
                    Integer i = Integer.parseInt(mEditNumeroView.getText().toString());
                    if (i >= 10 && i <= 1000000) {
                        replyIntent.putExtra(EXTRA_REPLY, i);
                        setResult(RESULT_OK, replyIntent);
                        finish();
                    } else {
                        Toast.makeText(
                                getApplicationContext(),
                                "Lukuarvon tulee olla välillä 10 - 1000000",
                                Toast.LENGTH_LONG).show();
                    }
                }
                catch (NumberFormatException e)
                {
                    Toast.makeText(
                            getApplicationContext(),
                            "Lukuarvon tulee olla välillä 10 - 1000000",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
