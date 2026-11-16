package com.bassouda.bahacademy

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val bg = Color.rgb(8, 13, 28)
    private val panel = Color.rgb(18, 26, 47)
    private val panel2 = Color.rgb(24, 35, 60)
    private val mint = Color.rgb(101, 231, 211)
    private val violet = Color.rgb(167, 139, 250)
    private val white = Color.rgb(241, 246, 255)
    private val muted = Color.rgb(154, 169, 195)
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var prefs: android.content.SharedPreferences
    private var page = "Accueil"
    private var selectedLesson = -1
    private val lessons = mutableListOf<JSONObject>()
    private val messages = mutableListOf<JSONObject>()
    private val groups = mutableListOf<JSONObject>()
    private val classes = mutableListOf<JSONObject>()
    private val pickImage = 201
    private val pickDocument = 202
    private val pickVoice = 203
    private var voiceTarget: EditText? = null
    private var pendingOcr = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        prefs = getSharedPreferences("bah_academy", MODE_PRIVATE)
        loadData()
        render()
    }

    private fun loadData() {
        readArray("lessons").let { a -> for (i in 0 until a.length()) lessons.add(a.optJSONObject(i) ?: JSONObject()) }
        readArray("messages").let { a -> for (i in 0 until a.length()) messages.add(a.optJSONObject(i) ?: JSONObject()) }
        readArray("groups").let { a -> for (i in 0 until a.length()) groups.add(a.optJSONObject(i) ?: JSONObject()) }
        readArray("classes").let { a -> for (i in 0 until a.length()) classes.add(a.optJSONObject(i) ?: JSONObject()) }
        if (lessons.isEmpty()) {
            lessons.add(JSONObject().put("title", "Bienvenue dans ton carnet").put("subject", "Guide").put("body", "Ajoute une leçon en écrivant son contenu ou en scannant tes pages. Tu pourras ensuite créer des fiches, réviser et t'évaluer.").put("summary", "Ton espace d'apprentissage personnel.").put("understood", "").put("date", "Aujourd'hui"))
            saveData()
        }
    }

    private fun readArray(k: String) = try { JSONArray(prefs.getString(k, "[]")) } catch (_: Exception) { JSONArray() }
    private fun saveData() {
        prefs.edit().putString("lessons", JSONArray(lessons).toString()).putString("messages", JSONArray(messages).toString())
            .putString("groups", JSONArray(groups).toString()).putString("classes", JSONArray(classes).toString()).apply()
    }

    private fun render() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg); fitsSystemWindows = true }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(12), dp(20), dp(10)) }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        brand.addView(txt("BAH  /  ACADEMY", 17, white, true))
        brand.addView(txt("APPRENDRE • COMPRENDRE • PROGRESSER", 9, muted, true))
        header.addView(brand)
        header.addView(roundButton("⚙", 42, panel2) { page = "Réglages"; render() })
        root.addView(header)
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(4), dp(18), dp(26)) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(bottomBar())
        setContentView(root)
        when (page) {
            "Accueil" -> home()
            "Carnet" -> notebook()
            "Évaluation" -> evaluation()
            "Cours" -> courses()
            "Messages" -> messaging()
            "Services" -> services()
            "Réglages" -> settings()
            else -> home()
        }
    }

    private fun home() {
        content.addView(space(6))
        val hero = card(24, panel2).apply { background = gradient(intArrayOf(Color.rgb(23, 57, 72), Color.rgb(39, 39, 78), Color.rgb(22, 31, 57)), 24) }
        hero.addView(txt("TON ESPACE POUR APPRENDRE", 10, mint, true))
        hero.addView(txt("Chaque idée peut\ndevenir une découverte.", 27, white, true), lp(top = 10))
        hero.addView(txt("Un carnet intelligent pour tes cours, tes révisions et tes projets.", 13, muted), lp(top = 9))
        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        stats.addView(stat("${lessons.size}", "LEÇONS"), LinearLayout.LayoutParams(0, -2, 1f))
        stats.addView(stat("${messages.size}", "MESSAGES"), LinearLayout.LayoutParams(0, -2, 1f))
        stats.addView(stat("${lessons.count { it.optString("understood").isNotBlank() }}", "NOTES"), LinearLayout.LayoutParams(0, -2, 1f))
        hero.addView(stats, lp(top = 20))
        content.addView(hero)
        section("Ton espace de travail", "Tout est organisé pour avancer à ton rythme")
        val tiles = listOf(
            arrayOf("▤", "Carnet numérique", "Scanner, organiser et comprendre tes cours", "Carnet"),
            arrayOf("✦", "Évaluation", "Quiz, exercices et corrections expliquées", "Évaluation"),
            arrayOf("◉", "Cours en ligne", "Espaces de classe et groupes d'étude", "Cours"),
            arrayOf("☷", "Messagerie", "Discussions et partage de documents", "Messages"),
            arrayOf("✧", "Services intelligents", "Assistant, recherche et sciences", "Services")
        )
        tiles.forEach { tile ->
            val ic = tile[0]; val title = tile[1]; val sub = tile[2]; val destination = tile[3]
            val c = card(19)
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(iconBubble(ic), lp(width = 48, height = 48, end = 13))
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
            col.addView(txt(title, 15, white, true)); col.addView(txt(sub, 11, muted), lp(top = 4))
            row.addView(col); row.addView(txt("›", 27, mint, true))
            c.addView(row); c.setOnClickListener { page = destination; render() }; content.addView(c)
        }
        section("Reprendre tes études", "Tes leçons récentes")
        lessons.takeLast(3).asReversed().forEachIndexed { idx, l -> lessonRow(l, lessons.size - 1 - idx) }
        val signature = card(18, Color.rgb(21, 29, 53))
        signature.addView(txt("UNE CRÉATION DE", 9, muted, true))
        signature.addView(txt("Bassouda Haïdara", 18, mint, true), lp(top = 5))
        signature.addView(txt("BAH Academy • Construire le savoir, partager les idées.", 11, muted), lp(top = 3))
        content.addView(signature)
    }

    private fun notebook() {
        pageTitle("Carnet numérique", "Tes cours, soigneusement classés et toujours disponibles.")
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(actionChip("＋ Écrire une leçon") { newLesson() }, LinearLayout.LayoutParams(0, dp(46), 1f))
        actions.addView(spaceW(8))
        actions.addView(actionChip("▧ Scanner") { chooseImage() }, LinearLayout.LayoutParams(0, dp(46), 1f))
        content.addView(actions)
        content.addView(space(14))
        val search = EditText(this).apply { hint = "Rechercher une leçon ou une matière…"; setHintTextColor(muted); setTextColor(white); textSize = 14f; setPadding(dp(16), dp(12), dp(16), dp(12)); background = shape(panel2, 18) }
        content.addView(search, lp(height = 50))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun update(q: String) { list.removeAllViews(); lessons.forEachIndexed { i, l -> if (l.optString("title").contains(q, true) || l.optString("subject").contains(q, true)) list.addView(lessonRow(l, i)) } }
        search.addTextChangedListener(SimpleWatcher { update(it) }); update("")
        content.addView(list, lp(top = 12))
        section("Outils du carnet", "Pour transformer tes notes en connaissances")
        toolRow("✧", "Créer un résumé", "Extraire les idées essentielles", "Choisis une leçon pour créer un résumé.")
        toolRow("✎", "Mon explication", "Écris ce que tu as compris", "Choisis une leçon pour expliquer avec tes mots.")
        toolRow("▣", "Fiches de révision", "Définitions et formules à mémoriser", "Les fiches sont générées à partir du contenu enregistré.")
    }

    private fun lessonRow(l: JSONObject, index: Int): View {
        val c = card(17)
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(iconBubble("▤"), lp(width = 44, height = 44, end = 12))
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        col.addView(txt(l.optString("title", "Leçon sans titre"), 14, white, true))
        col.addView(txt("${l.optString("subject", "Général")}  •  ${l.optString("date", "")}", 10, muted), lp(top = 4))
        row.addView(col); row.addView(txt("›", 24, mint))
        c.addView(row); c.setOnClickListener { selectedLesson = index; lessonDetail(index) }
        return c
    }

    private fun lessonDetail(i: Int) {
        if (i !in lessons.indices) return
        selectedLesson = i
        val l = lessons[i]
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(18), dp(20), dp(20)); background = shape(panel, 22) }
        box.addView(txt(l.optString("subject", "COURS").uppercase(Locale.getDefault()), 10, mint, true))
        box.addView(txt(l.optString("title"), 23, white, true), lp(top = 8))
        box.addView(txt(l.optString("body"), 14, white), lp(top = 16))
        section("Résumé intelligent", "Une synthèse à modifier et conserver")
        box.addView(txt(l.optString("summary", "Aucun résumé pour le moment."), 13, muted), lp(top = 10))
        content.removeAllViews(); content.addView(roundButton("← Retour au carnet", 44, panel2) { page = "Carnet"; render() }, lp(bottom = 12))
        content.addView(box)
        content.addView(actionChip("✧ Générer / actualiser le résumé") { aiForLesson(i, "Fais un résumé fidèle, structuré et facile à comprendre de cette leçon. Garde les formules et définitions.") }, lp(top = 12))
        content.addView(actionChip("Comprendre chaque partie, étape par étape") { aiForLesson(i, "Explique cette leçon partie par partie. Pour chaque partie : reprends son titre, explique les idées en français simple, définis les mots difficiles, donne un exemple concret et explique les formules sans en inventer. Termine par les points à retenir.") }, lp(top = 8))
        section("Ce que j'ai compris", "Explique avec tes propres mots. L'assistant t'aidera à repérer les points oubliés.")
        val own = EditText(this).apply { setText(l.optString("understood")); hint = "Écris ici ton explication personnelle…"; setHintTextColor(muted); setTextColor(white); minLines = 4; gravity = Gravity.TOP; textSize = 14f; setPadding(dp(15), dp(14), dp(15), dp(14)); background = shape(panel2, 18) }
        content.addView(own)
        content.addView(actionChip("🎙 Dicter mon explication") { voiceTarget = own; startVoiceInput() }, lp(top = 8))
        content.addView(actionChip("Enregistrer mon explication") { l.put("understood", own.text.toString()); saveData(); toast("Ton explication est enregistrée.") }, lp(top = 8))
        content.addView(actionChip("Comparer avec la leçon et recevoir un retour") { l.put("understood", own.text.toString()); saveData(); aiForLesson(i, "Voici l'explication personnelle de l'élève : ${own.text}. Compare-la avec le cours. Dis ce qui est correct, ce qui manque et ce qui doit être rectifié. Ne réécris pas tout à sa place. Sois encourageant.") }, lp(top = 8))
        section("Actions", "")
        content.addView(actionChip("Créer un quiz sur cette leçon") { page = "Évaluation"; render() })
        content.addView(actionChip("Partager le cours") { shareText(l.optString("title") + "\n\n" + l.optString("body")) }, lp(top = 8))
        content.addView(actionChip("Modifier le cours") { editLesson(i) }, lp(top = 8))
        content.addView(actionChip("Supprimer cette leçon", danger = true) { confirmDelete(i) }, lp(top = 8))
    }

    private fun evaluation() {
        pageTitle("Évaluation & révision", "Teste tes connaissances. Le but est de comprendre, pas seulement d'avoir une note.")
        content.addView(actionChip("✦ Préparer un quiz avec l'IA") { chooseLessonForQuiz() })
        content.addView(actionChip("＋ Créer mes propres questions") { customQuiz() }, lp(top = 8))
        section("Choisir une leçon", "Les questions doivent s'appuyer sur ton cours")
        lessons.forEachIndexed { i, l ->
            val c = card(16)
            c.addView(txt(l.optString("title"), 14, white, true))
            c.addView(txt(l.optString("subject"), 10, muted), lp(top = 3))
            c.addView(actionChip("Commencer l'évaluation  ›") { startQuiz(i) }, lp(top = 10))
            content.addView(c)
        }
        section("Réviser autrement", "Petites séances régulières")
        toolRow("▤", "Cartes mémoire", "Définitions, dates et formules", "Révise les définitions et formules de tes leçons.")
        toolRow("◷", "Révision espacée", "Revoir au bon moment", "Conseil : revois une notion aujourd'hui, demain, puis dans quelques jours.")
    }

    private fun chooseLessonForQuiz() {
        if (lessons.isEmpty()) { toast("Ajoute d'abord une leçon."); return }
        val names = lessons.map { it.optString("title") }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Choisir une leçon").setItems(names) { _, which ->
            selectedLesson = which
            aiForLesson(which, "Crée un quiz de 8 questions sur ce cours : 3 faciles, 3 intermédiaires et 2 difficiles. Mélange QCM et questions courtes. Donne les réponses et des explications séparément à la fin.")
        }.show()
    }

    private fun startQuiz(i: Int) {
        if (i !in lessons.indices) return
        val l = lessons[i]
        val prompts = listOf("Quelle est l'idée principale de cette leçon ?", "Donne une définition importante du cours.", "Explique une formule ou une règle étudiée.", "Donne un exemple concret lié à cette leçon.")
        var q = 0; var score = 0
        val answers = mutableListOf<String>()
        fun ask() {
            if (q >= prompts.size) {
                val result = "Évaluation terminée : $score / ${prompts.size} réponses renseignées.\n\nTes réponses :\n" + answers.joinToString("\n\n")
                showResult("Ton entraînement", result, "Demander une correction à l'IA") { askAi("Corrige avec bienveillance ces réponses selon le cours ci-dessous. Signale les points justes, les oublis et donne une explication simple.\nCOURS : ${l.optString("body")}\nRÉPONSES : $result") }
                return
            }
            val input = EditText(this).apply { hint = "Ta réponse…"; setHintTextColor(muted); setTextColor(white); minLines = 3; gravity = Gravity.TOP; setPadding(dp(12), dp(10), dp(12), dp(10)); background = shape(panel2, 14) }
            AlertDialog.Builder(this).setTitle("Question ${q + 1} / ${prompts.size}").setMessage(prompts[q]).setView(input)
                .setNegativeButton("Quitter", null).setPositiveButton("Valider") { _, _ -> if (input.text.isNotBlank()) { score++; answers.add("${q + 1}. ${prompts[q]}\n${input.text}") }; q++; ask() }.show()
        }
        ask()
    }

    private fun customQuiz() {
        val title = EditText(this).apply { hint = "Sujet ou matière"; setTextColor(white); setHintTextColor(muted) }
        val count = EditText(this).apply { hint = "Nombre de questions (ex. 5)"; inputType = InputType.TYPE_CLASS_NUMBER; setTextColor(white); setHintTextColor(muted) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(title); addView(count) }
        AlertDialog.Builder(this).setTitle("Créer un entraînement").setView(box).setNegativeButton("Annuler", null).setPositiveButton("Créer") { _, _ ->
            askAi("Crée ${count.text.toString().toIntOrNull()?.coerceIn(1, 20) ?: 5} questions progressives sur ${title.text}. Donne les réponses dans une partie séparée.")
        }.show()
    }

    private fun courses() {
        pageTitle("Cours & communautés", "Organise des séances d'étude, retrouve tes groupes et prépare des cours à distance.")
        content.addView(actionChip("＋ Créer un groupe d'étude") { createGroup() })
        content.addView(actionChip("＋ Ajouter une salle de cours / lien de réunion") { addClass() }, lp(top = 8))
        section("Mes groupes", "Espaces de travail sur tes matières")
        if (groups.isEmpty()) emptyState("Aucun groupe pour le moment", "Crée un groupe pour organiser tes révisions.")
        groups.forEachIndexed { i, g ->
            val c = card(18); c.addView(txt("◉  ${g.optString("name")}", 16, white, true)); c.addView(txt("${g.optString("subject")} • ${g.optString("members", "Moi")}", 11, muted), lp(top = 5)); c.addView(actionChip("Ouvrir le groupe") { groupDetail(i) }, lp(top = 10)); content.addView(c)
        }
        section("Mes salles de cours", "Réunions et cours en ligne")
        if (classes.isEmpty()) emptyState("Pas de salle enregistrée", "Ajoute le lien de ton cours en ligne pour le retrouver ici.")
        classes.forEachIndexed { i, c0 ->
            val c = card(18); c.addView(txt(c0.optString("name"), 15, white, true)); c.addView(txt(c0.optString("link"), 10, muted), lp(top = 4)); c.addView(actionChip("Rejoindre le cours  ↗") { openMeeting(c0.optString("link")) }, lp(top = 10)); content.addView(c)
        }
        val note = card(16, Color.rgb(29, 30, 58)); note.addView(txt("À propos des cours en direct", 13, violet, true)); note.addView(txt("Les salles peuvent ouvrir un lien de réunion compatible (par exemple un service de visioconférence). Les appels multi-utilisateurs et la synchronisation des messages nécessitent un serveur de communication configuré par l'administrateur de la plateforme.", 11, muted), lp(top = 7)); content.addView(note)
    }

    private fun groupDetail(i: Int) {
        if (i !in groups.indices) return
        val g = groups[i]
        content.removeAllViews(); content.addView(roundButton("← Groupes", 42, panel2) { page = "Cours"; render() })
        section(g.optString("name"), g.optString("subject"))
        content.addView(txt(g.optString("description", "Espace d'étude partagé."), 13, muted))
        content.addView(actionChip("Partager l'invitation") { shareText("Rejoins mon groupe d'étude BAH Academy : ${g.optString("name")} (${g.optString("subject")})") }, lp(top = 12))
        content.addView(actionChip("Partager un exercice ou un document") { chooseDocument() }, lp(top = 8))
        section("Discussion du groupe", "Messages enregistrés sur cet appareil")
        val input = EditText(this).apply { hint = "Écrire un message au groupe…"; setHintTextColor(muted); setTextColor(white); setPadding(dp(14), dp(12), dp(14), dp(12)); background = shape(panel2, 16) }
        content.addView(input)
        content.addView(actionChip("Envoyer le message") { if (input.text.isNotBlank()) { messages.add(JSONObject().put("room", g.optString("name")).put("text", input.text.toString()).put("from", "Moi")); saveData(); input.setText(""); toast("Message enregistré localement") } }, lp(top = 8))
        messages.filter { it.optString("room") == g.optString("name") }.forEach { val c = card(14); c.addView(txt(it.optString("from") + "  •  " + it.optString("text"), 13, white)); content.addView(c) }
    }

    private fun messaging() {
        pageTitle("Messagerie", "Prépare tes échanges et partage tes ressources d'apprentissage.")
        content.addView(actionChip("＋ Nouvelle discussion locale") { composeMessage() })
        content.addView(actionChip("Partager une photo ou un document") { chooseDocument() }, lp(top = 8))
        section("Conversations", "Historique stocké sur cet appareil")
        if (messages.isEmpty()) emptyState("Ta messagerie est prête", "Commence par écrire un message ou crée un groupe.")
        messages.asReversed().forEach { m ->
            val c = card(15); c.addView(txt(m.optString("room", "Discussion") + "  •  " + m.optString("from", "Moi"), 10, mint, true)); c.addView(txt(m.optString("text"), 14, white), lp(top = 5)); content.addView(c)
        }
        val info = card(16, Color.rgb(29, 30, 58)); info.addView(txt("Synchronisation entre appareils", 13, violet, true)); info.addView(txt("Cette édition conserve les discussions localement. Pour envoyer et recevoir des messages en temps réel entre plusieurs téléphones, il faut relier un projet serveur sécurisé (Firebase ou équivalent) et activer les règles d'accès. Aucun faux statut « envoyé en ligne » n'est affiché.", 11, muted), lp(top = 7)); content.addView(info)
    }

    private fun composeMessage() {
        val room = EditText(this).apply { hint = "Nom de la discussion"; setTextColor(white); setHintTextColor(muted) }
        val msg = EditText(this).apply { hint = "Ton message"; setTextColor(white); setHintTextColor(muted); minLines = 3 }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(room); addView(msg) }
        AlertDialog.Builder(this).setTitle("Nouvelle discussion").setView(box).setNegativeButton("Annuler", null).setPositiveButton("Enregistrer") { _, _ -> if (msg.text.isNotBlank()) { messages.add(JSONObject().put("room", room.text.toString().ifBlank { "Discussion personnelle" }).put("from", "Moi").put("text", msg.text.toString())); saveData(); render() } }.show()
    }

    private fun services() {
        pageTitle("Services intelligents", "Des outils pour chercher, comprendre, créer et expérimenter.")
        val ai = card(22, panel2)
        ai.addView(txt("BAH • ASSISTANT", 10, mint, true))
        ai.addView(txt("Une question ?\nOn la travaille ensemble.", 23, white, true), lp(top = 8))
        val prompt = EditText(this).apply { hint = "Pose une question en français…"; setHintTextColor(muted); setTextColor(white); minLines = 2; gravity = Gravity.TOP; setPadding(dp(14), dp(13), dp(14), dp(13)); background = shape(bg, 16) }
        ai.addView(prompt, lp(top = 14))
        ai.addView(actionChip("Envoyer  ➤") { if (prompt.text.isNotBlank()) { val q = prompt.text.toString(); prompt.setText(""); askAi(q) } }, lp(top = 9))
        content.addView(ai)
        section("Explorer les outils", "")
        toolRow("∑", "Mathématiques", "Résolution guidée, équations et démonstrations", "Aide-moi à comprendre et résoudre étape par étape : ")
        toolRow("⚗", "Physique & chimie", "Lois, expériences, unités et explications", "Explique cette notion de physique-chimie avec des exemples : ")
        toolRow("⌘", "Informatique & programmation", "Algorithmes, Python et projets", "Aide-moi à comprendre ce code ou ce concept : ")
        toolRow("⌕", "Recherche web", "Trouver des ressources et des explications", "Ouvre une recherche web")
        toolRow("◈", "Idées de schémas scientifiques", "Préparer une description claire pour un visuel", "Aide-moi à concevoir un schéma pédagogique sur : ")
        section("Configuration IA", "Choisis un fournisseur et garde ta clé privée sur ton appareil")
        content.addView(actionChip("Configurer Groq / OpenRouter") { page = "Réglages"; render() })
        content.addView(txt("Les services IA nécessitent une connexion Internet et une clé API valide. Les fonctions de carnet et de révision restent disponibles hors ligne.", 11, muted), lp(top = 9))
    }

    private fun settings() {
        pageTitle("Réglages", "Personnalise ton espace et configure les services connectés.")
        val provider = prefs.getString("provider", "groq") ?: "groq"
        val group = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val g = RadioButton(this).apply { text = "Groq"; setTextColor(white); id = 1 }
        val o = RadioButton(this).apply { text = "OpenRouter"; setTextColor(white); id = 2 }
        group.addView(g); group.addView(o); group.check(if (provider == "openrouter") 2 else 1)
        val key = EditText(this).apply { hint = "Clé API du fournisseur sélectionné"; setHintTextColor(muted); setTextColor(white); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; setText(prefs.getString(if (provider == "openrouter") "openrouter_key" else "groq_key", "")); setPadding(dp(15), dp(12), dp(15), dp(12)); background = shape(panel2, 16) }
        val model = EditText(this).apply { hint = "Modèle (laisser vide pour le modèle par défaut)"; setHintTextColor(muted); setTextColor(white); setText(prefs.getString("model", "") ?: ""); setPadding(dp(15), dp(12), dp(15), dp(12)); background = shape(panel2, 16) }
        content.addView(txt("FOURNISSEUR", 10, mint, true)); content.addView(group, lp(top = 7)); content.addView(key, lp(top = 8)); content.addView(model, lp(top = 8))
        content.addView(actionChip("Enregistrer la configuration") { val p = if (group.checkedRadioButtonId == 2) "openrouter" else "groq"; prefs.edit().putString("provider", p).putString(if (p == "groq") "groq_key" else "openrouter_key", key.text.toString().trim()).putString("model", model.text.toString().trim()).apply(); toast("Configuration enregistrée sur cet appareil") }, lp(top = 10))
        content.addView(actionChip("Ouvrir les clés Groq") { openUrl("https://console.groq.com/keys") }, lp(top = 8))
        content.addView(actionChip("Ouvrir les clés OpenRouter") { openUrl("https://openrouter.ai/settings/keys") }, lp(top = 8))
        section("Identité de la plateforme", "")
        val brand = card(18); brand.addView(txt("BAH ACADEMY", 20, mint, true)); brand.addView(txt("Créée par Bassouda Haïdara", 13, white, true), lp(top = 5)); brand.addView(txt("Version 1.0.0 • Application d'apprentissage", 10, muted), lp(top = 4)); content.addView(brand)
        section("Données", "Tes leçons et messages sont enregistrés sur cet appareil")
        content.addView(actionChip("Exporter mes leçons") { shareText(lessons.joinToString("\n\n") { it.optString("title") + "\n" + it.optString("body") }) })
        content.addView(actionChip("Supprimer toutes les données locales", danger = true) { AlertDialog.Builder(this).setTitle("Effacer les données ?").setMessage("Cette action supprime les leçons, groupes et messages enregistrés sur cet appareil.").setNegativeButton("Annuler", null).setPositiveButton("Effacer") { _, _ -> lessons.clear(); messages.clear(); groups.clear(); classes.clear(); saveData(); render() }.show() }, lp(top = 8))
        content.addView(txt("Confidentialité : ne partage jamais ta clé API. Pour les groupes et cours, échange uniquement avec des personnes que tu connais et ne publie pas tes informations personnelles.", 11, muted), lp(top = 16))
    }

    // ----- Création, modification, importation et OCR -----
    private fun newLesson() { editLesson(-1) }
    private fun editLesson(i: Int) {
        val old = if (i in lessons.indices) lessons[i] else JSONObject()
        val title = EditText(this).apply { hint = "Titre de la leçon"; setText(old.optString("title")); setTextColor(white); setHintTextColor(muted) }
        val subject = EditText(this).apply { hint = "Matière (maths, physique…)"; setText(old.optString("subject")); setTextColor(white); setHintTextColor(muted) }
        val body = EditText(this).apply { hint = "Contenu complet du cours…"; setText(old.optString("body")); setTextColor(white); setHintTextColor(muted); minLines = 6; gravity = Gravity.TOP }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, dp(8), 0); addView(title); addView(subject); addView(body) }
        AlertDialog.Builder(this).setTitle(if (i < 0) "Nouvelle leçon" else "Modifier la leçon").setView(box).setNegativeButton("Annuler", null).setNeutralButton("Scanner une page") { _, _ -> pendingOcr = true; chooseImage() }.setPositiveButton("Enregistrer") { _, _ ->
            val l = JSONObject().put("title", title.text.toString().ifBlank { "Leçon sans titre" }).put("subject", subject.text.toString().ifBlank { "Général" }).put("body", body.text.toString()).put("summary", old.optString("summary")).put("understood", old.optString("understood")).put("date", "Aujourd'hui")
            if (i in lessons.indices) lessons[i] = l else lessons.add(l)
            saveData(); page = "Carnet"; render()
        }.show()
    }

    private fun chooseImage() { val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE); putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) }; startActivityForResult(intent, pickImage) }
    private fun chooseDocument() { val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE) }; startActivityForResult(intent, pickDocument) }
    @Deprecated("Deprecated in Android API; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        if (requestCode == pickImage) {
            val uris = mutableListOf<Uri>(); data.data?.let { uris.add(it) }; data.clipData?.let { c -> for (i in 0 until c.itemCount) uris.add(c.getItemAt(i).uri) }
            if (uris.isNotEmpty()) scanUris(uris.distinct())
        } else if (requestCode == pickDocument) {
            data.data?.let { uri -> val share = Intent(Intent.ACTION_SEND).apply { type = contentResolver.getType(uri) ?: "*/*"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; startActivity(Intent.createChooser(share, "Partager le document")) }
        } else if (requestCode == pickVoice) {
            val spoken = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) { val target = voiceTarget; target?.append(if (target.text.isNullOrBlank()) spoken else " $spoken"); toast("Dictée ajoutée") }
        }
    }

    private fun startVoiceInput() {
        if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 304)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { toast("La reconnaissance vocale n'est pas disponible sur cet appareil."); return }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Parle clairement…")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try { startActivityForResult(intent, pickVoice) } catch (_: Exception) { toast("Impossible de démarrer la dictée vocale.") }
    }

    private fun scanUris(uris: List<Uri>) {
        toast("Lecture des pages en cours…")
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val output = StringBuilder()
        fun next(i: Int) {
            if (i >= uris.size) {
                recognizer.close()
                runOnUiThread {
                    val title = EditText(this).apply { hint = "Titre du cours"; setTextColor(white); setHintTextColor(muted) }
                    val subject = EditText(this).apply { hint = "Matière"; setTextColor(white); setHintTextColor(muted) }
                    val body = EditText(this).apply { setText(output.toString()); setTextColor(white); setHintTextColor(muted); minLines = 7; gravity = Gravity.TOP }
                    val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(title); addView(subject); addView(body) }
                    AlertDialog.Builder(this).setTitle("Texte reconnu • ${uris.size} page(s)").setView(box).setNegativeButton("Annuler", null).setPositiveButton("Enregistrer la leçon") { _, _ -> lessons.add(JSONObject().put("title", title.text.toString().ifBlank { "Cours scanné" }).put("subject", subject.text.toString().ifBlank { "Général" }).put("body", body.text.toString()).put("summary", "").put("understood", "").put("date", "Aujourd'hui")); saveData(); page = "Carnet"; render() }.show()
                }
                return
            }
            try {
                val image = InputImage.fromFilePath(this, uris[i])
                recognizer.process(image).addOnSuccessListener { result -> output.append("\n\n--- PAGE ${i + 1} ---\n").append(result.text).append("\n"); next(i + 1) }
                    .addOnFailureListener { output.append("\n[Page ${i + 1} : lecture impossible]\n"); next(i + 1) }
            } catch (_: Exception) { output.append("\n[Page ${i + 1} : fichier illisible]\n"); next(i + 1) }
        }
        next(0)
    }

    // ----- IA Groq / OpenRouter -----
    private fun aiForLesson(i: Int, instruction: String) { if (i !in lessons.indices) return; val l = lessons[i]; askAi("$instruction\n\nMATIÈRE : ${l.optString("subject")}\nTITRE : ${l.optString("title")}\nCOURS :\n${l.optString("body")}") { answer -> l.put("summary", answer); saveData(); showResult("Résultat de l'analyse", answer, "Copier / partager") { shareText(answer) } } }

    private fun askAi(prompt: String, onAnswer: ((String) -> Unit)? = null) {
        val provider = prefs.getString("provider", "groq") ?: "groq"
        val key = prefs.getString(if (provider == "openrouter") "openrouter_key" else "groq_key", "") ?: ""
        if (key.isBlank()) { AlertDialog.Builder(this).setTitle("Configuration nécessaire").setMessage("Ajoute ta clé Groq ou OpenRouter dans Réglages pour utiliser les services IA. Le carnet reste utilisable sans clé.").setPositiveButton("Ouvrir les réglages") { _, _ -> page = "Réglages"; render() }.setNegativeButton("Annuler", null).show(); return }
        val loading = ProgressBar(this)
        val dialog = AlertDialog.Builder(this).setTitle("BAH Assistant").setMessage("Préparation de la réponse…").setView(loading).setNegativeButton("Fermer", null).create()
        dialog.show()
        thread {
            var answer = "Une erreur est survenue. Vérifie la connexion et ta clé API."
            try {
                val conn = (URL(if (provider == "openrouter") "https://openrouter.ai/api/v1/chat/completions" else "https://api.groq.com/openai/v1/chat/completions").openConnection() as HttpURLConnection)
                conn.requestMethod = "POST"; conn.connectTimeout = 20000; conn.readTimeout = 60000; conn.doOutput = true
                conn.setRequestProperty("Authorization", "Bearer $key"); conn.setRequestProperty("Content-Type", "application/json")
                if (provider == "openrouter") { conn.setRequestProperty("HTTP-Referer", "https://bahacademy.app"); conn.setRequestProperty("X-Title", "BAH Academy") }
                val model = prefs.getString("model", "")?.takeIf { it.isNotBlank() } ?: if (provider == "openrouter") "openrouter/free" else "llama-3.3-70b-versatile"
                val payload = JSONObject().put("model", model).put("messages", JSONArray().put(JSONObject().put("role", "system").put("content", "Tu es un tuteur pédagogique bienveillant. Réponds en français simple, avec des étapes et des exemples. Aide l'élève à réfléchir au lieu de faire tout à sa place quand c'est un exercice.")).put(JSONObject().put("role", "user").put("content", prompt)))
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val response = stream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                if (conn.responseCode !in 200..299) answer = "Erreur du fournisseur (${conn.responseCode}) : " + json.optJSONObject("error")?.optString("message", response) else answer = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content", "Réponse vide.")
                conn.disconnect()
            } catch (e: Exception) { answer = "Impossible de joindre le service : ${e.localizedMessage ?: "erreur réseau"}" }
            runOnUiThread { dialog.dismiss(); if (onAnswer != null && !answer.startsWith("Erreur") && !answer.startsWith("Impossible")) onAnswer(answer) else showResult("BAH Assistant", answer, "Partager la réponse") { shareText(answer) } }
        }
    }

    private fun showResult(title: String, text: String, action: String, onAction: () -> Unit) {
        val body = ScrollView(this); val t = TextView(this).apply { this.text = text; setTextColor(white); textSize = 14f; setPadding(dp(18), dp(16), dp(18), dp(16)) }; body.addView(t)
        AlertDialog.Builder(this).setTitle(title).setView(body).setPositiveButton("Fermer", null).setNeutralButton(action) { _, _ -> onAction() }.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 304 && grantResults.firstOrNull() == android.content.pm.PackageManager.PERMISSION_GRANTED) startVoiceInput()
        else if (requestCode == 304) toast("La permission du micro est nécessaire pour la dictée.")
    }

    // ----- Outils UI -----
    private fun pageTitle(title: String, subtitle: String) { content.addView(txt(title, 25, white, true)); content.addView(txt(subtitle, 12, muted), lp(top = 7, bottom = 16)) }
    private fun section(title: String, subtitle: String) { content.addView(txt(title, 17, white, true), lp(top = 20)); if (subtitle.isNotBlank()) content.addView(txt(subtitle, 11, muted), lp(top = 4, bottom = 8)) }
    private fun toolRow(icon: String, title: String, sub: String, prompt: String) {
        val c = card(16); val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(iconBubble(icon), lp(width = 42, height = 42, end = 11))
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }; col.addView(txt(title, 14, white, true)); col.addView(txt(sub, 10, muted), lp(top = 4)); row.addView(col); c.addView(row)
        c.setOnClickListener { if (prompt == "Ouvre une recherche web") { val q = EditText(this).apply { hint = "Sujet à rechercher"; setTextColor(white); setHintTextColor(muted) }; AlertDialog.Builder(this).setTitle("Recherche web").setView(q).setNegativeButton("Annuler", null).setPositiveButton("Rechercher") { _, _ -> startActivity(Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", q.text.toString())) }.show() } else { val input = EditText(this).apply { hint = "Complète ta question…"; setTextColor(white); setHintTextColor(muted) }; AlertDialog.Builder(this).setTitle(title).setView(input).setNegativeButton("Annuler", null).setPositiveButton("Continuer") { _, _ -> askAi(prompt + input.text) }.show() } }
        content.addView(c)
    }
    private fun bottomBar(): View {
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(5), dp(7), dp(5), dp(9)); setBackgroundColor(Color.rgb(12, 19, 37)) }
        val items = listOf("Accueil" to "⌂", "Carnet" to "▤", "Évaluation" to "✦", "Cours" to "◉", "Services" to "✧")
        items.forEach { (name, symbol) ->
            val active = page == name || (name == "Services" && page == "Réglages")
            val item = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(5), dp(5), dp(5), dp(4)); background = if (active) shape(Color.rgb(29, 55, 66), 16) else null; layoutParams = LinearLayout.LayoutParams(0, dp(54), 1f) }
            item.addView(txt(symbol, 19, if (active) mint else muted, true)); item.addView(txt(name, 9, if (active) mint else muted, active), lp(top = 2)); item.setOnClickListener { page = name; render() }; bar.addView(item)
        }
        return bar
    }
    private fun card(radius: Int = 18, color: Int = panel) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(15), dp(16), dp(15)); background = shape(color, radius); layoutParams = lp(bottom = 10) }
    private fun txt(s: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply { text = s; textSize = size.toFloat(); setTextColor(color); if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD); includeFontPadding = true }
    private fun stat(value: String, label: String) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(txt(value, 22, mint, true)); addView(txt(label, 9, muted, true), lp(top = 2)) }
    private fun iconBubble(s: String) = TextView(this).apply { text = s; gravity = Gravity.CENTER; textSize = 21f; setTextColor(mint); background = shape(Color.rgb(28, 52, 66), 18) }
    private fun roundButton(s: String, h: Int, color: Int, action: () -> Unit) = TextView(this).apply { text = s; gravity = Gravity.CENTER; textSize = 13f; setTextColor(white); background = shape(color, 100); setOnClickListener { action() }; layoutParams = lp(height = h, bottom = 5) }
    private fun actionChip(s: String, danger: Boolean = false, action: () -> Unit) = TextView(this).apply { text = s; gravity = Gravity.CENTER; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(if (danger) Color.rgb(255, 151, 164) else white); setPadding(dp(14), dp(12), dp(14), dp(12)); background = if (danger) shape(Color.rgb(65, 31, 48), 100) else gradient(intArrayOf(Color.rgb(39, 73, 91), Color.rgb(35, 47, 78)), 100); setOnClickListener { action() }; layoutParams = lp(height = 46) }
    private fun shape(color: Int, radius: Int) = android.graphics.drawable.GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), Color.argb(28, 190, 220, 255)) }
    private fun gradient(colors: IntArray, radius: Int) = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR, colors).apply { cornerRadius = dp(radius).toFloat(); setStroke(dp(1), Color.argb(45, 170, 230, 235)) }
    private fun lp(width: Int = -1, height: Int = -2, top: Int = 0, bottom: Int = 0, start: Int = 0, end: Int = 0) = LinearLayout.LayoutParams(width, height).apply { topMargin = dp(top); bottomMargin = dp(bottom); marginStart = dp(start); marginEnd = dp(end) }
    private fun space(h: Int) = View(this).apply { layoutParams = lp(height = h) }
    private fun spaceW(w: Int) = View(this).apply { layoutParams = lp(width = w, height = 1) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    private fun shareText(s: String) { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, s), "Partager")) }
    private fun openUrl(s: String) { try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s))) } catch (_: Exception) { toast("Impossible d'ouvrir le lien") } }
    private fun openMeeting(s: String) { if (s.startsWith("https://") || s.startsWith("http://")) openUrl(s) else toast("Ajoute un lien de réunion valide (https://…)") }
    private fun emptyState(title: String, subtitle: String) { val c = card(18); c.gravity = Gravity.CENTER; c.addView(txt("◌", 28, violet)); c.addView(txt(title, 14, white, true), lp(top = 6)); c.addView(txt(subtitle, 11, muted), lp(top = 4)); content.addView(c) }
    private fun createGroup() {
        val name = EditText(this).apply { hint = "Nom du groupe"; setTextColor(white); setHintTextColor(muted) }; val subject = EditText(this).apply { hint = "Matière"; setTextColor(white); setHintTextColor(muted) }; val desc = EditText(this).apply { hint = "Objectif du groupe"; setTextColor(white); setHintTextColor(muted) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(name); addView(subject); addView(desc) }
        AlertDialog.Builder(this).setTitle("Créer un groupe d'étude").setView(box).setNegativeButton("Annuler", null).setPositiveButton("Créer") { _, _ -> groups.add(JSONObject().put("name", name.text.toString().ifBlank { "Mon groupe" }).put("subject", subject.text.toString().ifBlank { "Études" }).put("description", desc.text.toString()).put("members", "Moi")); saveData(); render() }.show()
    }
    private fun addClass() {
        val name = EditText(this).apply { hint = "Nom du cours"; setTextColor(white); setHintTextColor(muted) }; val link = EditText(this).apply { hint = "Lien HTTPS de la réunion"; setTextColor(white); setHintTextColor(muted) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(name); addView(link) }
        AlertDialog.Builder(this).setTitle("Ajouter une salle de cours").setView(box).setNegativeButton("Annuler", null).setPositiveButton("Enregistrer") { _, _ -> classes.add(JSONObject().put("name", name.text.toString().ifBlank { "Cours en ligne" }).put("link", link.text.toString())); saveData(); render() }.show()
    }
    private fun confirmDelete(i: Int) { AlertDialog.Builder(this).setTitle("Supprimer la leçon ?").setMessage("Cette action ne peut pas être annulée.").setNegativeButton("Annuler", null).setPositiveButton("Supprimer") { _, _ -> lessons.removeAt(i); saveData(); page = "Carnet"; render() }.show() }
}

private class SimpleWatcher(val changed: (String) -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed(s?.toString() ?: "") }
    override fun afterTextChanged(s: android.text.Editable?) {}
}
