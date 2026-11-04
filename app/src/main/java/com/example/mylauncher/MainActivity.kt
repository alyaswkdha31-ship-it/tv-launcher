package com.example.mylauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONObject
import java.net.URL
import java.util.Calendar

class MainActivity : Activity() {

    // غيّر المدينة والإحداثيات (method: 9 الكويت، 4 السعودية)
    private val cityName = "الكويت"
    private val lat = 29.3759
    private val lon = 47.9774
    private val method = 9

    private val keys = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    private val names = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
    private val mins = IntArray(5) { -1 }
    private val boxes = mutableListOf<LinearLayout>()
    private val times = mutableListOf<TextView>()
    private val h = Handler(Looper.getMainLooper())
    private val match = ViewGroup.LayoutParams.MATCH_PARENT
    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
    private var lastFetch = 0L
    private lateinit var apps: List<ResolveInfo>
    private lateinit var weather: TextView

    private val tick = object : Runnable {
        override fun run() {
            highlight()
            if (System.currentTimeMillis() - lastFetch > 1800000L) {
                lastFetch = System.currentTimeMillis()
                load()
            }
            h.postDelayed(this, 60000L)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        apps = packageManager.queryIntentActivities(q, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString() }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(80, 50, 80, 40)
        }
        val wall = resources.getIdentifier("wallpaper", "drawable", packageName)
        root.background = if (wall != 0) {
            LayerDrawable(arrayOf<Drawable>(getDrawable(wall)!!, ColorDrawable(Color.parseColor("#88000000"))))
        } else {
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#0F2027"), Color.parseColor("#2C5364"))
            )
        }

        val head = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 24)
        }
        head.addView(tv("واجهتي", 34f), LinearLayout.LayoutParams(0, wrap, 1f))
        weather = tv("$cityName ...", 22f).apply { setPadding(0, 0, 60, 0) }
        head.addView(weather)
        val clock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        clock.addView(TextClock(this).apply {
            format12Hour = "hh:mm a"
            format24Hour = "HH:mm"
            textSize = 40f
            setTextColor(Color.WHITE)
        })
        clock.addView(TextClock(this).apply {
            format12Hour = "EEEE d MMMM"
            format24Hour = "EEEE d MMMM"
            textSize = 16f
            setTextColor(Color.WHITE)
        })
        head.addView(clock)
        root.addView(head)

        val bar = LinearLayout(this).apply { setPadding(0, 0, 0, 32) }
        for (i in 0..4) {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(8, 16, 8, 16)
                background = bg("#22FFFFFF")
            }
            box.addView(tv(names[i], 18f).apply { gravity = Gravity.CENTER })
            val t = tv("--:--", 24f).apply { gravity = Gravity.CENTER }
            box.addView(t)
            boxes.add(box)
            times.add(t)
            bar.addView(box, LinearLayout.LayoutParams(0, wrap, 1f).apply { setMargins(8, 0, 8, 0) })
        }
        root.addView(bar)

        val grid = GridView(this).apply {
            numColumns = 5
            horizontalSpacing = 24
            verticalSpacing = 24
            selector = ColorDrawable(Color.parseColor("#55FFFFFF"))
            adapter = object : BaseAdapter() {
                override fun getCount() = apps.size
                override fun getItem(p: Int) = apps[p]
                override fun getItemId(p: Int) = p.toLong()
                override fun getView(p: Int, v: View?, parent: ViewGroup): View {
                    val a = apps[p]
                    val pm = packageManager
                    return LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setPadding(16, 24, 16, 24)
                        addView(ImageView(context).apply {
                            setImageDrawable(a.activityInfo.loadBanner(pm) ?: a.loadIcon(pm))
                        }, LinearLayout.LayoutParams(220, 124))
                        addView(tv(a.loadLabel(pm).toString(), 16f).apply {
                            gravity = Gravity.CENTER
                            maxLines = 1
                        })
                    }
                }
            }
            setOnItemClickListener { _, _, p, _ ->
                packageManager.getLaunchIntentForPackage(apps[p].activityInfo.packageName)
                    ?.let { startActivity(it) }
            }
        }
        root.addView(grid, LinearLayout.LayoutParams(match, match))
        setContentView(root)
    }

    private fun tv(s: String, size: Float) = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(Color.WHITE)
    }

    private fun bg(c: String) = GradientDrawable().apply {
        cornerRadius = 24f
        setColor(Color.parseColor(c))
    }

    private fun fmt(m: Int): String {
        val hr = m / 60
        val h12 = if (hr % 12 == 0) 12 else hr % 12
        return "%d:%02d %s".format(h12, m % 60, if (hr >= 12) "م" else "ص")
    }

    private fun highlight() {
        if (mins.any { it < 0 }) return
        val c = Calendar.getInstance()
        val now = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        val next = mins.indexOfFirst { it > now }.let { if (it < 0) 0 else it }
        for (i in 0..4) boxes[i].background = bg(if (i == next) "#887FDBFF" else "#22FFFFFF")
    }

    private fun load() {
        Thread {
            try {
                val cur = JSONObject(
                    URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code").readText()
                ).getJSONObject("current")
                val t = Math.round(cur.getDouble("temperature_2m"))
                val icon = when (cur.getInt("weather_code")) {
                    0 -> "☀️"
                    1, 2 -> "🌤️"
                    3 -> "☁️"
                    45, 48 -> "🌫️"
                    in 51..67, in 80..82 -> "🌧️"
                    in 71..77 -> "❄️"
                    in 95..99 -> "⛈️"
                    else -> "🌡️"
                }
                runOnUiThread { weather.text = "$icon $t°\n$cityName" }
            } catch (e: Exception) {
                runOnUiThread { weather.text = "الطقس غير متوفر" }
            }
            try {
                val tm = JSONObject(
                    URL("https://api.aladhan.com/v1/timings?latitude=$lat&longitude=$lon&method=$method").readText()
                ).getJSONObject("data").getJSONObject("timings")
                for (i in 0..4) {
                    val p = tm.getString(keys[i]).take(5).split(":")
                    mins[i] = p[0].toInt() * 60 + p[1].toInt()
                }
                runOnUiThread {
                    for (i in 0..4) times[i].text = fmt(mins[i])
                    highlight()
                }
            } catch (e: Exception) {
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        h.post(tick)
    }

    override fun onPause() {
        super.onPause()
        h.removeCallbacks(tick)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {}
}
