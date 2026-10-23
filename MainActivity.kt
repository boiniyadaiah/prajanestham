package com.prajanestham.shortnews

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Html
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.gms.ads.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.*
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var root: LinearLayout
    private lateinit var refresh: SwipeRefreshLayout
    private lateinit var card: LinearLayout
    private lateinit var titleView: TextView
    private lateinit var bodyView: TextView
    private lateinit var imageView: ImageView
    private lateinit var infoView: TextView
    private lateinit var districtSpinner: Spinner
    private lateinit var tts: TextToSpeech
    private var news = listOf<WpPost>()
    private var index = 0
    private var ttsReady = false
    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private val savedIds by lazy { prefs.getStringSet("bookmarks", emptySet())!!.toMutableSet() }
    private val districts = listOf("అన్ని జిల్లాలు", "హైదరాబాద్", "రంగారెడ్డి", "మేడ్చల్–మల్కాజిగిరి", "వరంగల్", "ఖమ్మం", "నల్గొండ", "కరీంనగర్", "నిజామాబాద్", "మహబూబ్‌నగర్", "ఇతర జిల్లాలు")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dark = prefs.getBoolean("dark", false)
        AppCompatDelegate.setDefaultNightMode(if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        tts = TextToSpeech(this, this)
        buildUi()
        MobileAds.initialize(this) {}
        loadNews()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }

    private fun buildUi() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(12, 8, 12, 8) }
        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = TextView(this).apply { text = "ప్రజానేస్తం"; textSize = 22f; setTextColor(0xFFB5121B.toInt()); setTypeface(null, 1) }
        toolbar.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        toolbar.addView(button("బుక్‌మార్క్‌లు") { showBookmarks() })
        toolbar.addView(button(if (prefs.getBoolean("dark", false)) "☀" else "☾") { toggleDark() })
        root.addView(toolbar)
        districtSpinner = Spinner(this)
        districtSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, districts)
        districtSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) { renderCurrent() }
        }
        root.addView(districtSpinner)
        refresh = SwipeRefreshLayout(this)
        card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(4, 8, 4, 8) }
        imageView = ImageView(this).apply { adjustViewBounds = true; minimumHeight = 220; scaleType = ImageView.ScaleType.CENTER_CROP }
        titleView = TextView(this).apply { textSize = 23f; setTypeface(null, 1); setPadding(0, 12, 0, 8) }
        bodyView = TextView(this).apply { textSize = 17f; setPadding(0, 4, 0, 8) }
        infoView = TextView(this).apply { textSize = 12f; setPadding(0, 4, 0, 8) }
        card.addView(imageView); card.addView(titleView); card.addView(infoView); card.addView(bodyView)
        val actions = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        actions.addView(button("🔊 వినండి") { speakCurrent() })
        actions.addView(button("షేర్") { shareCurrent() })
        actions.addView(button("☆ సేవ్") { bookmarkCurrent() })
        card.addView(actions)
        val swipeHint = TextView(this).apply { text = "తదుపరి వార్త కోసం పైకి/కిందికి స్వైప్ చేయండి"; gravity = Gravity.CENTER; setPadding(0, 10, 0, 10) }
        card.addView(swipeHint)
        var startY = 0f
        card.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> { startY = event.y; true }
                android.view.MotionEvent.ACTION_UP -> {
                    val delta = event.y - startY
                    if (kotlin.math.abs(delta) > 70) { if (delta < 0) nextNews(1) else nextNews(-1); true } else false
                }
                else -> true
            }
        }
        refresh.addView(card)
        refresh.setOnRefreshListener { loadNews() }
        root.addView(refresh, LinearLayout.LayoutParams(-1, 0, 1f))
        val ad = AdView(this).apply {
            setAdSize(AdSize.BANNER)
            // Official Google test banner ID. Replace with your own AdMob unit ID before release.
            adUnitId = "ca-app-pub-3940256099942544/6300978111"
            loadAd(AdRequest.Builder().build())
        }
        root.addView(ad)
        setContentView(root)
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply { text = label; textSize = 12f; setOnClickListener { action() } }

    private fun loadNews() {
        refresh.isRefreshing = true
        scope.launch {
            try {
                val category = ApiClient.SHORT_NEWS_CATEGORY_ID.takeIf { it > 0 }
                news = withContext(Dispatchers.IO) { ApiClient.api.posts(perPage = 50, category = category) }
                index = 0
                renderCurrent()
            } catch (e: Exception) {
                titleView.text = "వార్తలు లోడ్ కాలేదు"
                bodyView.text = "ఇంటర్నెట్‌ను తనిఖీ చేయండి. WordPress API మరియు షార్ట్ న్యూస్ కేటగిరీ ID సరిచూడండి.\n\nవివరం: ${e.localizedMessage ?: "తెలియని లోపం"}"
            } finally { refresh.isRefreshing = false }
        }
    }

    private fun renderCurrent() {
        if (!::titleView.isInitialized || news.isEmpty()) return
        val post = news.getOrNull(index) ?: return
        titleView.text = clean(post.title?.rendered ?: "శీర్షిక లేదు")
        bodyView.text = clean(post.excerpt?.rendered ?: post.content?.rendered ?: "")
        infoView.text = "ప్రజానేస్తం • ${post.date ?: ""} • ${districts.getOrElse(districtSpinner.selectedItemPosition) { "తెలంగాణ" }}"
        // Featured image is retrieved by _embed. A fallback placeholder is used until API image extraction is configured.
        val image = post.imageUrl
        if (!image.isNullOrBlank()) Glide.with(this).load(image).into(imageView)
        else imageView.setImageResource(android.R.drawable.ic_menu_report_image)
    }

    private fun clean(value: String): String = Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString().trim()

    private fun nextNews(direction: Int) {
        if (news.isEmpty()) return
        index = (index + direction + news.size) % news.size
        renderCurrent()
    }

    private fun shareCurrent() {
        val p = news.getOrNull(index) ?: return
        val text = clean(p.title?.rendered ?: "ప్రజానేస్తం వార్త") + "\n" + (p.link ?: "")
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "వార్తను షేర్ చేయండి"))
    }

    private fun bookmarkCurrent() {
        val p = news.getOrNull(index) ?: return
        val ids = savedIds
        if (!ids.add(p.id.toString())) ids.remove(p.id.toString())
        prefs.edit().putStringSet("bookmarks", ids).apply()
        Toast.makeText(this, if (p.id.toString() in ids) "బుక్‌మార్క్‌లో సేవ్ అయింది" else "బుక్‌మార్క్ తొలగించబడింది", Toast.LENGTH_SHORT).show()
    }

    private fun showBookmarks() {
        val ids = savedIds
        val filtered = news.filter { it.id.toString() in ids }
        if (filtered.isEmpty()) { Toast.makeText(this, "సేవ్ చేసిన వార్తలు లేవు", Toast.LENGTH_SHORT).show(); return }
        news = filtered; index = 0; renderCurrent()
    }

    private fun toggleDark() {
        val next = !prefs.getBoolean("dark", false)
        prefs.edit().putBoolean("dark", next).apply()
        AppCompatDelegate.setDefaultNightMode(if (next) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        recreate()
    }

    private fun speakCurrent() {
        val p = news.getOrNull(index) ?: return
        val text = clean(p.title?.rendered ?: "") + ". " + clean(p.excerpt?.rendered ?: p.content?.rendered ?: "")
        if (ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "prajanestham_news")
        else Toast.makeText(this, "వాయిస్ సిద్ధమవుతోంది", Toast.LENGTH_SHORT).show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("te", "IN"))
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onDestroy() {
        scope.cancel()
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
}
