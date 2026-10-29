package br.com.proximaera.boletorush

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Local-only troubleshooting. Nothing is uploaded automatically.
 * Device model and last crash report leave the phone only when the player
 * explicitly chooses a target in Android's share sheet.
 */
internal object DeviceDiagnostics {
    fun snapshot(context: Context): String {
        val prefs = context.getSharedPreferences("boleto_rush", Context.MODE_PRIVATE)
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memory = ActivityManager.MemoryInfo()
        runCatching { manager?.getMemoryInfo(memory) }
            .onFailure { Log.w("BoletoRush", "Memory summary unavailable", it) }
        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "desconhecida"
        val mb = 1024L * 1024L
        val heap = Runtime.getRuntime()
        val crash = prefs.getString("last_crash", null)
        val lastFrame = prefs.getLong("last_first_frame_at", 0L)
        val trim = prefs.getInt("last_memory_warning", -1)
        return buildString {
            appendLine("BOLETO RUSH — DIAGNÓSTICO LOCAL")
            appendLine("Versão: $versionName")
            appendLine("Pacote: ${context.packageName}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Aparelho: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Memória livre do sistema: ${memory.availMem / mb} MB")
            appendLine("Memória total: ${memory.totalMem / mb} MB")
            appendLine("Sistema em pouca memória: ${memory.lowMemory}")
            appendLine("Memória usada pela JVM: ${(heap.totalMemory() - heap.freeMemory()) / mb} MB")
            appendLine("Limite de memória da JVM: ${heap.maxMemory() / mb} MB")
            appendLine("Primeira tela renderizada: ${if (lastFrame > 0L) java.util.Date(lastFrame).toString() else "sem registro"}")
            appendLine("Último aviso de memória: ${if (trim < 0) "nenhum registrado" else trim}")
            appendLine()
            appendLine("ÚLTIMA EXCEÇÃO SALVA:")
            appendLine(crash ?: "Nenhuma exceção não tratada registrada. Fechamentos pelo sistema podem não gerar um relatório.")
        }
    }

    fun share(context: Context, report: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Boleto Rush — diagnóstico")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar diagnóstico"))
    }
}
