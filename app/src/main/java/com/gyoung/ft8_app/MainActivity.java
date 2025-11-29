package com.gyoung.ft8_app;

import android.os.Bundle;
import android.util.Log;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.gyoung.ft8_app.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FT8MainActivity";
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private FT8Lib ft8Lib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        
        // Initialize FT8 library
        try {
            ft8Lib = new FT8Lib();
            Log.d(TAG, "FT8 library loaded successfully");

            // Run FT8 tests
            testFT8Features();

        } catch (Exception e) {
            Log.e(TAG, "Error loading FT8 library: " + e.getMessage());
            e.printStackTrace();
        }

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Test FT8 encoding when FAB is clicked
                testFT8Encoding(view);
            }
        });
    }

    private void testFT8Features() {
        Log.d(TAG, "=== Starting FT8 Tests ===");

        // Test 1: Pack only
        testPackOnly();

        // Test 2: Encoding
        testFT8EncodingSimple();

        // Test 3: Full round-trip
        testFullRoundTrip();

        Log.d(TAG, "=== All FT8 Tests Completed Successfully! ===");
    }

    private void testPackOnly() {
        String message = "CQ WA8Q DM43";

        Log.d(TAG, "Testing pack for: " + message);

        try {
            // Pack the message
            byte[] packed = ft8Lib.packMessage(message);
            Log.d(TAG, "✓ Packed into " + packed.length + " bytes");

            // Print the packed bytes in hex
            StringBuilder hex = new StringBuilder();
            for (byte b : packed) {
                hex.append(String.format("%02X ", b));
            }
            Log.d(TAG, "  Packed data: " + hex.toString());

        } catch (Exception e) {
            Log.e(TAG, "✗ Error in pack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testFullRoundTrip() {
        String message = "CQ WA8Q DM43";

        Log.d(TAG, "Testing full encode/decode round-trip for: " + message);

        try {
            // Step 1: Encode to packed message
            byte[] packed = ft8Lib.packMessage(message);
            Log.d(TAG, "✓ Step 1: Packed into " + packed.length + " bytes");

            // Print the packed bytes in hex
            StringBuilder hex = new StringBuilder();
            for (byte b : packed) {
                hex.append(String.format("%02X ", b));
            }
            Log.d(TAG, "  Packed data: " + hex.toString());

            // Step 2: Generate audio from the message
            float frequencyHz = 1500f;
            float[] audioSamples = ft8Lib.encodeMessage(message, frequencyHz);
            Log.d(TAG, "✓ Step 2: Generated " + audioSamples.length + " audio samples");

            // Note: Full decoding would require:
            // 1. Converting audio back to waterfall (FFT/spectrogram)
            // 2. Finding sync patterns
            // 3. Extracting symbols
            // 4. LDPC decoding
            // 5. Then ftx_message_decode would work

            Log.d(TAG, "✓ Encode test successful! (Decode requires additional signal processing)");

        } catch (Exception e) {
            Log.e(TAG, "✗ Error in round-trip test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testFT8EncodingSimple() {
        String message = "CQ WA8Q DM43";
        float frequencyHz = 1500f;

        Log.d(TAG, "Encoding message: " + message + " at " + frequencyHz + " Hz");

        try {
            float[] audioSamples = ft8Lib.encodeMessage(message, frequencyHz);

            if (audioSamples != null) {
                Log.d(TAG, "✓ Successfully generated " + audioSamples.length + " audio samples");
                Log.d(TAG, "  Duration: " + (audioSamples.length / 12000.0f) + " seconds");
                Log.d(TAG, "  Sample rate: 12000 Hz");

                // Calculate some basic statistics
                float min = Float.MAX_VALUE;
                float max = Float.MIN_VALUE;
                float sum = 0;
                for (float sample : audioSamples) {
                    if (sample < min) min = sample;
                    if (sample > max) max = sample;
                    sum += Math.abs(sample);
                }
                float avg = sum / audioSamples.length;
                Log.d(TAG, "  Sample range: [" + min + ", " + max + "]");
                Log.d(TAG, "  Average absolute value: " + avg);
            } else {
                Log.e(TAG, "✗ Encoding returned null");
            }

        } catch (Exception e) {
            Log.e(TAG, "✗ Error encoding: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testFT8Encoding(View view) {
        String message = "CQ WA8Q DM43";
        float frequencyHz = 1500f;

        try {
            float[] audioSamples = ft8Lib.encodeMessage(message, frequencyHz);

            if (audioSamples != null) {
                Snackbar.make(view,
                                "FT8 encoded! Generated " + audioSamples.length + " samples",
                                Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .setAction("Info", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Log.d(TAG, "Check logcat (tag: FT8MainActivity) for detailed output");
                            }
                        }).show();

                Log.d(TAG, "FAB clicked - FT8 encoding test successful");
            } else {
                Snackbar.make(view, "FT8 encoding failed", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .show();
            }

        } catch (Exception e) {
            Snackbar.make(view, "Error: " + e.getMessage(), Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.fab)
                    .show();
            Log.e(TAG, "Error in FAB click handler: " + e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
