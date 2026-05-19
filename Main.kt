import java.awt.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

val cyrToLat = mapOf(
    'а' to "a",  'б' to "b",  'в' to "w",  'г' to "g",  'д' to "d",
    'е' to "e",  'ё' to "ö",  'ж' to "ž",  'з' to "z",  'и' to "i",
    'й' to "ÿ",  'к' to "k",  'л' to "l",  'м' to "m",  'н' to "n",
    'о' to "o",  'п' to "p",  'р' to "r",  'с' to "s",  'т' to "t",
    'у' to "y",  'ф' to "f",  'х' to "h",  'ц' to "ç",  'ч' to "č",
    'ш' to "š",  'щ' to "šč", 'ъ' to "æ",  'ы' to "ø",  'ь' to "į",
    'э' to "ē",  'ю' to "ů",  'я' to "ja"
)

val latToCyr: List<Pair<String, Char>> = cyrToLat.entries
    .map { (cyr, lat) -> lat to cyr }
    .sortedByDescending { it.first.length }

// Characters that can appear in RysskajáLatiniça words
val RYL = "[a-zA-ZöžÿçčšæøįēůÖŽŸÇČŠÆØĮĒŮ]"
val CYR = "[А-Яа-яёЁ]"

fun capitalize(s: String): String =
    if (s.isEmpty()) s else s[0].toUpperCase() + s.drop(1)

// "или" → "иль" so it transliterates naturally to "ilį"
fun preprocessCyrillic(s: String): String = s
    .replace(Regex("(?<!$CYR)или(?!$CYR)"), "иль")
    .replace(Regex("(?<!$CYR)Или(?!$CYR)"), "Иль")
    .replace(Regex("(?<!$CYR)ИЛИ(?!$CYR)"), "ИЛЬ")

fun applyMorphRules(s: String): String = s
    .replace(Regex("Tįsja(?!$RYL)"), "Ça")   // Ться → Ça
    .replace(Regex("tįsja(?!$RYL)"), "ça")   // ться → ça
    .replace(Regex("Tsja(?!$RYL)"),  "Ça")   // Тся  → Ça
    .replace(Regex("tsja(?!$RYL)"),  "ça")   // тся  → ça
    .replace(Regex("Tį(?!$RYL)"),    "Č")    // Ть   → Č (конец слова)
    .replace(Regex("tį(?!$RYL)"),    "č")    // ть   → č (конец слова)
    .replace(Regex("T(?!$RYL)"),     "Č")    // Т    → Č (конец слова)
    .replace(Regex("t(?!$RYL)"),     "č")    // т    → č (конец слова)

fun toRysskaja(input: String): String = buildString {
    for (ch in preprocessCyrillic(input)) {
        val lat = cyrToLat[ch.toLowerCase()]
        if (lat != null) {
            if (ch.isUpperCase()) append(capitalize(lat))
            else append(lat)
        } else {
            append(ch)
        }
    }
}.let { applyMorphRules(it) }

fun toCyrillic(input: String): String = buildString {
    var i = 0
    outer@ while (i < input.length) {
        for ((lat, cyr) in latToCyr) {
            val upper = capitalize(lat)
            when {
                input.startsWith(lat, i) -> {
                    append(cyr); i += lat.length; continue@outer
                }
                input.startsWith(upper, i) -> {
                    append(cyr.toUpperCase()); i += upper.length; continue@outer
                }
            }
        }
        append(input[i++])
    }
}

fun main() {
    SwingUtilities.invokeLater {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

        val frame = JFrame("RysskajáLatiniça Converter")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.preferredSize = Dimension(520, 420)

        val font = Font("SansSerif", Font.PLAIN, 15)

        val dirCombo = JComboBox(arrayOf(
            "Кириллица  →  RysskajáLatiniça",
            "RysskajáLatiniça  →  Кириллица"
        ))
        dirCombo.font = font

        val inputArea = JTextArea().apply {
            lineWrap = true; wrapStyleWord = true; this.font = font
        }
        val outputArea = JTextArea().apply {
            lineWrap = true; wrapStyleWord = true; this.font = font
            isEditable = false
            background = Color(245, 245, 248)
        }

        fun convert() {
            val text = inputArea.text
            outputArea.text = if (dirCombo.selectedIndex == 0) toRysskaja(text) else toCyrillic(text)
        }

        inputArea.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = convert()
            override fun removeUpdate(e: DocumentEvent) = convert()
            override fun changedUpdate(e: DocumentEvent) = convert()
        })
        dirCombo.addActionListener { convert() }

        val inputScroll  = JScrollPane(inputArea).apply  { border = BorderFactory.createTitledBorder("Ввод") }
        val outputScroll = JScrollPane(outputArea).apply { border = BorderFactory.createTitledBorder("Результат") }

        val centerPanel = JPanel(GridLayout(2, 1, 0, 8)).apply {
            add(inputScroll); add(outputScroll)
        }
        val topPanel = JPanel(BorderLayout(8, 0)).apply {
            add(JLabel("Направление:"), BorderLayout.WEST)
            add(dirCombo, BorderLayout.CENTER)
        }
        val root = JPanel(BorderLayout(0, 10)).apply {
            border = BorderFactory.createEmptyBorder(14, 14, 14, 14)
            add(topPanel, BorderLayout.NORTH)
            add(centerPanel, BorderLayout.CENTER)
        }

        frame.contentPane = root
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }
}
