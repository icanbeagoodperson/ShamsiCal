package com.example.shamsical
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** date = Gregorian ISO "2026-10-17"; alarm: 0 none, 1 sound, 2 silent notification; lead = minutes before */
data class Item(val id: Long, val date: String, val title: String, val event: Boolean, val start: Int, val end: Int, val alarm: Int, val lead: Int)
data class Th(val name: String, val primary: Int, val bg: Int, val text: Int)

object Store {
    private fun p(c: Context) = c.getSharedPreferences("d", 0)
    val defaults = listOf(
        Th("روشن", 0xFF1565C0.toInt(), 0xFFFFFFFF.toInt(), 0xFF111111.toInt()),
        Th("تیره", 0xFF90CAF9.toInt(), 0xFF121212.toInt(), 0xFFEEEEEE.toInt()),
        Th("سبز", 0xFF2E7D32.toInt(), 0xFFF1F8E9.toInt(), 0xFF1B1B1B.toInt()))

    fun items(c: Context): List<Item> {
        val a = JSONArray(p(c).getString("items", "[]"))
        return (0 until a.length()).map { val o = a.getJSONObject(it)
            Item(o.getLong("id"), o.getString("date"), o.getString("title"), o.getBoolean("event"), o.getInt("start"), o.getInt("end"), o.getInt("alarm"), o.getInt("lead")) }
    }
    fun saveItems(c: Context, l: List<Item>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("date", it.date).put("title", it.title).put("event", it.event)
            .put("start", it.start).put("end", it.end).put("alarm", it.alarm).put("lead", it.lead)) }
        p(c).edit().putString("items", a.toString()).apply()
    }
    fun themes(c: Context): List<Th> {
        val a = JSONArray(p(c).getString("themes", "[]"))
        return defaults + (0 until a.length()).map { val o = a.getJSONObject(it); Th(o.getString("n"), o.getInt("p"), o.getInt("b"), o.getInt("t")) }
    }
    fun saveThemes(c: Context, l: List<Th>) {
        val a = JSONArray()
        l.filter { t -> defaults.none { it.name == t.name } }.forEach { a.put(JSONObject().put("n", it.name).put("p", it.primary).put("b", it.bg).put("t", it.text)) }
        p(c).edit().putString("themes", a.toString()).apply()
    }
    fun current(c: Context): Th = themes(c).firstOrNull { it.name == p(c).getString("cur", "") } ?: defaults[0]
    fun setCurrent(c: Context, name: String) = p(c).edit().putString("cur", name).apply()
}
