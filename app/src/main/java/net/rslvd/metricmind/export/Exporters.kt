package net.rslvd.metricmind.export

import android.content.Context
import android.net.Uri
import net.rslvd.metricmind.domain.model.Habit
import net.rslvd.metricmind.domain.model.MetricEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import javax.inject.Inject

/**
 * Local-only exporters. Callers obtain a [Uri] via the Storage Access Framework
 * (ACTION_CREATE_DOCUMENT) so no storage permission is needed and data never leaves the device
 * unless the user explicitly chooses a destination.
 */
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun export(uri: Uri, metrics: List<MetricEntry>) {
        context.contentResolver.openOutputStream(uri)?.use { out -> write(out, metrics) }
    }

    private fun write(out: OutputStream, metrics: List<MetricEntry>) {
        out.bufferedWriter().use { w ->
            w.appendLine("day,type,value,note")
            metrics.forEach { e ->
                val note = e.note?.replace("\"", "\"\"").orEmpty()
                w.appendLine("${e.day},${e.type.name},${e.value},\"$note\"")
            }
        }
    }
}

class JsonExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Full structured dump; also the basis for future restore. Carries a schema version. */
    fun export(uri: Uri, metrics: List<MetricEntry>, habits: List<Habit>) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            val root = JSONObject()
            root.put("schemaVersion", 1)
            root.put("exportedAt", System.currentTimeMillis())
            root.put("metrics", JSONArray().apply {
                metrics.forEach { e ->
                    put(JSONObject().apply {
                        put("day", e.day.toString())
                        put("type", e.type.name)
                        put("value", e.value.toDouble())
                        put("note", e.note ?: JSONObject.NULL)
                    })
                }
            })
            root.put("habits", JSONArray().apply {
                habits.forEach { h ->
                    put(JSONObject().apply {
                        put("title", h.title)
                        put("template", h.template.name)
                        put("reminderMode", h.reminderMode.name)
                        put("reminderMinute", h.reminderMinute ?: JSONObject.NULL)
                        put("active", h.active)
                    })
                }
            })
            out.bufferedWriter().use { it.write(root.toString(2)) }
        }
    }
}
