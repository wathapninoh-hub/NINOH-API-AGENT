package com.ninoh.apiagent

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {
    private lateinit var base: EditText
    private lateinit var key: EditText
    private lateinit var model: Spinner
    private lateinit var prompt: EditText
    private lateinit var output: TextView
    private lateinit var status: TextView
    private val models = mutableListOf<String>()
    private val prefs by lazy { getSharedPreferences("ninoh_settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 22, 20, 20)
            setBackgroundColor(0xFF101827.toInt())
        }
        val scroll = ScrollView(this)
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(page)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val title = TextView(this).apply {
            text = "NINOH API AGENT"
            textSize = 25f
            setTextColor(0xFF63D9FF.toInt())
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 18)
        }
        page.addView(title)
        page.addView(label("API PROVIDER / BASE URL"))
        base = edit("https://openrouter.ai/api/v1").also { it.setText(prefs.getString("base", "https://openrouter.ai/api/v1")) }
        page.addView(base)
        page.addView(label("API KEY (voatahiry eto amin'ny finday)"))
        key = edit("Ampidiro eto ny API key").also { it.setText(prefs.getString("key", "")); it.inputType = 129 }
        page.addView(key)
        val load = button("Haka ny lisitry ny modely")
        page.addView(load)
        page.addView(label("SAFIDIO NY MODEL"))
        model = Spinner(this)
        models.add("Tsindrio aloha Haka ny lisitry ny modely")
        model.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, models)
        page.addView(model)
        page.addView(label("HAFATRA / CODING PROMPT"))
        prompt = edit("Soraty eto ny fanontaniana na asa coding...")
        prompt.minLines = 3
        page.addView(prompt)
        val send = button("ALEFA")
        page.addView(send)
        status = label("Vonona. Mila connexion internet sy API key.")
        page.addView(status)
        page.addView(label("VALIN'NY AI"))
        output = TextView(this).apply {
            text = "Eto no hisehoan'ny valiny."
            textSize = 15f
            setTextColor(0xFFE5EDF7.toInt())
            setPadding(14, 14, 14, 24)
            setBackgroundColor(0xFF1D293B.toInt())
        }
        page.addView(output)

        load.setOnClickListener { save(); loadModels() }
        send.setOnClickListener { save(); sendPrompt() }
        setContentView(root)
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(0xFF9FB4CC.toInt())
        setPadding(2, 13, 2, 6)
    }

    private fun edit(hintText: String) = EditText(this).apply {
        hint = hintText
        textSize = 15f
        setTextColor(0xFFFFFFFF.toInt())
        setHintTextColor(0xFF8494A8.toInt())
        setPadding(12, 10, 12, 10)
        setBackgroundColor(0xFF253449.toInt())
    }

    private fun button(textValue: String) = Button(this).apply {
        text = textValue
        isAllCaps = false
        setTextColor(0xFFFFFFFF.toInt())
        setBackgroundColor(0xFF087FA5.toInt())
    }

    private fun save() {
        prefs.edit().putString("base", base.text.toString().trim())
            .putString("key", key.text.toString().trim()).apply()
    }

    private fun endpoint(path: String): String {
        var b = base.text.toString().trim().trimEnd('/')
        if (b.isEmpty()) b = "https://openrouter.ai/api/v1"
        return if (b.endsWith(path)) b else "$b/$path"
    }

    private fun request(urlText: String, method: String, body: String? = null): String {
        val conn = (URL(urlText).openConnection() as HttpURLConnection)
        conn.requestMethod = method
        conn.connectTimeout = 20000
        conn.readTimeout = 60000
        conn.setRequestProperty("Authorization", "Bearer ${key.text.toString().trim()}")
        conn.setRequestProperty("Content-Type", "application/json")
        if (body != null) {
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val result = stream?.bufferedReader()?.use { it.readText() } ?: ""
        conn.disconnect()
        if (code !in 200..299) throw Exception("HTTP $code: $result")
        return result
    }

    private fun loadModels() {
        if (key.text.toString().isBlank()) { status.text = "Ampidiro aloha ny API key."; return }
        status.text = "Maka modely..."
        Thread {
            try {
                val json = JSONObject(request(endpoint("models"), "GET"))
                val data = json.optJSONArray("data") ?: JSONArray()
                val found = mutableListOf<String>()
                for (i in 0 until data.length()) {
                    val id = data.optJSONObject(i)?.optString("id").orEmpty()
                    if (id.isNotBlank()) found.add(id)
                }
                runOnUiThread {
                    models.clear()
                    models.addAll(found)
                    if (models.isEmpty()) models.add("Tsy nisy modely hita")
                    model.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, models)
                    status.text = if (found.isEmpty()) "Tsy nisy modely tao amin'ny valiny." else "Modely ${found.size} hita."
                }
            } catch (e: Exception) { runOnUiThread { status.text = "Hadisoana: ${e.message}" } }
        }.start()
    }

    private fun sendPrompt() {
        val text = prompt.text.toString().trim()
        val selected = model.selectedItem?.toString().orEmpty()
        if (key.text.toString().isBlank()) { status.text = "Ampidiro aloha ny API key."; return }
        if (text.isBlank()) { status.text = "Soraty aloha ny hafatra."; return }
        if (selected.isBlank() || selected.startsWith("Tsindrio") || selected.startsWith("Tsy nisy")) {
            status.text = "Fidio aloha ny modely."; return
        }
        status.text = "Miandry ny valin'ny AI..."
        output.text = ""
        Thread {
            try {
                val messages = JSONArray().put(JSONObject().put("role", "user").put("content", text))
                val body = JSONObject().put("model", selected).put("messages", messages).toString()
                val json = JSONObject(request(endpoint("chat/completions"), "POST", body))
                val answer = json.optJSONArray("choices")?.optJSONObject(0)
                    ?.optJSONObject("message")?.optString("content") ?: "Tsy nisy valiny."
                runOnUiThread { output.text = answer; status.text = "Vita." }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Hadisoana: ${e.message}" }
            }
        }.start()
    }
}
