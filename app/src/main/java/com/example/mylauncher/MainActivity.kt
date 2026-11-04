package com.example.mylauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

class MainActivity : Activity() {

    // ===== غيّر هذي القيم حسب مدينتك =====
    private val cityName = "الكويت"
    private val latitude = 29.3759
    private val longitude = 47.9774
    // طريقة حساب الصلاة: 9 = الكويت، 4 = أم القرى (السعودية)
    private val prayerMethod = 9
    // درجة تعتيم الخلفية
    private val wallpaperDim = "#88000000"
    // =====================================

    private val prayerKeys = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    private val prayerNames = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
    private val prayerMinutes = IntArray(5) { -1 }

    private lateinit var apps: List<ResolveInfo>
    private lateinit var weatherView: TextView
    private val prayerBoxes = mutableListOf<LinearLayout>()
    private val prayerTimeViews = mutableListOf<TextView>()
    private val handler = Handler(Looper.getMainLooper())

    private val weatherTask = object : Runnable {
        override fun run() {
            fetchWeather()
            handler.postDelayed(this, 30 * 60 * 1000L)
        }
    }

    private val prayerTask = object : Runnable {
        override fun run() {
            fetchPrayerTimes()
            handler.postDelayed(this, 6 * 60 * 60 * 1000L)
        }
    }

    private val highlightTask = object : Runnable {
        override fun run() {
            highlightNextPrayer()
            handler.postDelayed(this, 60 * 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        apps = packageManager.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString() }

        val match = ViewGroup.LayoutParams.MATCH_PARENT

        val frame = FrameLayout(this)
        val wallId = resources.getIdentifier("wallpaper", "drawable", packageName)
        if (wallId != 0) {
            frame.setBackgroundColor(Color.BLACK)
            frame.addView(ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(wallId)
            }, FrameLayout.LayoutParams(match, match))
            frame.addView(View(this).apply {
                setBackgroundColor(Color.parseColor(wallpaperDim))
            }, FrameLayout.LayoutParams(match, match))
        } else {
            frame.background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#0F2027"), Color.parseColor("#2C5364"))
            )
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(80, 50, 80, 40)
        }
        content.addView(buildHeader())
        content.addView(buildPrayerBar())

        val grid = GridView(this).apply {
            numColumns = 5
            horizontalSpacing = 24
            verticalSpacing = 24
            selector = ColorDrawable(Color.parseColor("#55FFFFFF"))
            adapter = AppAdapter()
            setOnItemClickListener { _, _, position, _ ->
                packageManager.getLaunchIntentForPackage(apps[position].activityInfo.packageName)
                    ?.let { startActivity(it) }
            }
        }
        content.addView(grid, LinearLayout.LayoutParams(match, match))

        frame.addView(content, FrameLayout.LayoutParams(match, match))
        setContentView(frame)
    }

    private fun buildHeader(): View {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 24)
        }

        header.addView(TextView(this).apply {
            text = "واجهتي"
            textSize = 34f
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        weatherView = TextView(this).apply {
            text = "$cityName ..."
            textSize = 22f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 60, 0)
        }
        header.addView(weatherView)

        val clockBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        clockBox.addView(TextClock(this).apply {
            format12Hour = "hh:mm a"
            format24Hour = "HH:mm"
            textSize = 40f
            setTextColor(Color.WHITE)
        })
        clockBox.addView(TextClock(this).apply {
            format12Hour = "EEEE d MMMM"
            format24Hour = "EEEE d MMMM"
            textSize = 16f
            setTextColor(Color.parseColor("#CCFFFFFF"))
        })
        header.addView(clockBox)
        return header
    }

    private fun buildPrayerBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 32)
        }
        for (i in 0 until 5) {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(8, 16, 8, 16)
                background = roundedBg("#22FFFFFF")
            }
            box.addView(TextView(this).apply {
                text = prayerNames[i]
                textSize = 18f
                setTextColor(Color.parseColor("#CCFFFFFF"))
                gravity = Gravity.CENTER
            })
            val time = TextView(this).apply {
                text = "--:--"
                textSize = 24f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            box.addView(time)

            prayerBoxes.add(box)
            prayerTimeViews.add(time)

            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(8, 0, 8, 0)
            bar.addView(box, lp)
        }
        return bar
    }

    private fun roundedBg(color: String) = GradientDrawable().apply {
        cornerRadius = 24f
        setColor(Color.parseColor(color))
    }

    private fun fetchPrayerTimes() {
        Thread {
            try {
                val url = URL(
                    "https://api.aladhan.com/v1/timings" +
                        "?latitude=$latitude&longitude=$longitude&method=$prayerMethod"
