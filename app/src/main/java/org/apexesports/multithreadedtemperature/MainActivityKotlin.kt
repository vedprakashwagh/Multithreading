package org.apexesports.multithreadedtemperature

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class MainActivityKotlin : AppCompatActivity() {
    private var DELAY = 100L
    private var currentTemperature = "--°C"
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val updateCycle = AtomicInteger(0)
    private val displayUpdates = AtomicInteger(0)
    private val mutex = Mutex()

    private lateinit var displayContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = "Multithreaded Kotlin"
        displayContainer = findViewById(R.id.display_container)
        startTemperatureMonitoring()
    }

    private fun startTemperatureMonitoring() {
        coroutineScope.launch {
            while (isActive) {
                val newTemp = readThermometer()
                withContext(Dispatchers.Main) {
                    findViewById<TextView>(R.id.mainTemperatureDisplay).text =
                        "Outside Temperature: $newTemp"
                }
                displayUpdates.set(0)
                mutex.withLock {
                    currentTemperature = newTemp
                    updateCycle.incrementAndGet()
                }
                while (displayUpdates.get() < 7) {
                    delay(DELAY)
                }
                delay(DELAY)
            }
        }

        repeat(7) { index ->
            val displayIndex = index + 1
            coroutineScope.launch {
                var lastUpdateCycle = 0

                while (isActive) {
                    var temp: String
                    var currentCycle: Int
                    do {
                        delay(5)
                        mutex.withLock {
                            currentCycle = updateCycle.get()
                            temp = currentTemperature
                        }
                    } while (currentCycle == lastUpdateCycle)

                    lastUpdateCycle = currentCycle
                    withContext(Dispatchers.Main) {
                        displayContainer.findViewWithTag<TextView>("display$displayIndex").text =
                            "Teller $displayIndex: $temp"
                    }
                    displayUpdates.incrementAndGet()
                }
            }
        }
    }

    private fun readThermometer(): String {
        val temperature = 15 + Random.nextInt(20)
        return "$temperature°C"
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}