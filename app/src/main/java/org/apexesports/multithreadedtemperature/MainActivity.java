package org.apexesports.multithreadedtemperature;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {
    private final int DELAY = 100;

    private final AtomicReference<String> currentTemperature = new AtomicReference<>("--°C");

    //Thanks to this -> https://www.geeksforgeeks.org/java-util-concurrent-cyclicbarrier-java/
    private CyclicBarrier updateBarrier;
    private ScheduledExecutorService thermometerExecutor;
    private ScheduledExecutorService[] displayExecutors = new ScheduledExecutorService[7];

    private LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setTitle("Multithreaded Java");
        linearLayout = findViewById(R.id.display_container);
        updateBarrier = new CyclicBarrier(8);
        startThreads();
    }

    private void startThreads() {
        thermometerExecutor = Executors.newSingleThreadScheduledExecutor();
        thermometerExecutor.scheduleWithFixedDelay(() -> {
            try {
                String newTemperature = readThermometer();
                currentTemperature.set(newTemperature);
                runOnUiThread(() -> {
                    TextView mainDisplay = findViewById(R.id.mainTemperatureDisplay);
                    mainDisplay.setText("Outside Temperature: " + newTemperature);
                });
                updateBarrier.await();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, DELAY, TimeUnit.MILLISECONDS);

        for (int i = 0; i < 7; i++) {
            final int displayIndex = i;
            displayExecutors[i] = Executors.newSingleThreadScheduledExecutor();
            displayExecutors[i].execute(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        updateBarrier.await();
                        String temp = currentTemperature.get();
                        runOnUiThread(() -> {
                            ((TextView) linearLayout.findViewWithTag("display" + (displayIndex + 1))).setText("Teller " + (displayIndex + 1) + ": " + temp);
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private String readThermometer() {
        Random random = new Random();
        int temperature = 15 + random.nextInt(20);
        return temperature + "°C";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        thermometerExecutor.shutdownNow();
        for (ScheduledExecutorService executor : displayExecutors) {
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }
}