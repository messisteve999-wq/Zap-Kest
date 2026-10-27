package com.steve.zapkest

import java.time.LocalDate

object QuestionRepository {
    const val ROMANCE = "Romantisme, compliments et drague amicale"
    const val CONNAISSANCE = "Apprendre à connaître une fille"
    const val COPINE = "Faire durer une conversation avec ma copine"

    val categories = listOf(ROMANCE, CONNAISSANCE, COPINE)

    private val romance = listOf(
        "J'aime beaucoup ton sourire.",
        "Ton humour rend nos conversations agréables.",
        "J'apprécie ta sincérité et ta façon d'être.",
        "J'aime découvrir ta personnalité petit à petit.",
        "Ta gentillesse est une belle qualité.",
        "C'est toujours agréable de discuter avec toi.",
        "Tu sais rendre une conversation intéressante.",
        "J'aime ton naturel, reste comme tu es.",
        "Tu as une énergie positive qui fait plaisir.",
        "J'aime la façon dont tu exprimes tes idées.",
        "Ton sourire peut illuminer une journée ordinaire.",
        "J'aime quand on peut rire de choses simples.",
        "J'apprécie les échanges où chacun peut être lui-même.",
        "J'aime apprendre à te connaître sans précipitation.",
        "Quelle qualité apprécies-tu le plus chez une personne ?",
        "Quel compliment te fait vraiment plaisir ?",
        "Préfères-tu les mots doux ou les petites attentions ?",
        "Pour toi, qu'est-ce qu'une belle complicité ?",
        "Qu'est-ce qui te donne envie de mieux connaître quelqu'un ?",
        "Quelle petite attention te met de bonne humeur ?"
    )

    private val connaissance = listOf(
        "Qu'est-ce qui te rend heureuse pendant tes moments de repos ?",
        "Comment imagines-tu ta vie dans quelques années ?",
        "Quel souvenir te fait toujours sourire ?",
        "Qu'aimerais-tu découvrir ou apprendre cette année ?",
        "Quelle place donnes-tu à l'amitié dans ta vie ?",
        "Qu'est-ce qui compte le plus pour toi dans une relation ?",
        "Qu'est-ce qui te donne confiance en une personne ?",
        "Quelle petite attention apprécies-tu particulièrement ?",
        "Quel endroit aimerais-tu visiter un jour ?",
        "Quel rêve aimerais-tu réaliser en priorité ?",
        "Quelles qualités admires-tu chez les autres ?",
        "Comment préfères-tu régler un désaccord ?",
        "Qu'est-ce qui te motive dans une période difficile ?",
        "Quelle activité te fait perdre la notion du temps ?",
        "Quelle musique correspond à ton humeur actuelle ?",
        "Quel est le meilleur conseil qu'on t'ait donné ?",
        "Qu'est-ce qui te fait rire à coup sûr ?",
        "Qu'aimerais-tu que les gens comprennent mieux à ton sujet ?",
        "Quelle tradition aimerais-tu garder toute ta vie ?",
        "Qu'est-ce qui te fait te sentir respectée et écoutée ?"
    )

    private val copine = listOf(
        "Raconte-moi le meilleur moment de ta journée.",
        "Quel projet aimerais-tu qu'on réalise ensemble ?",
        "Si on pouvait voyager demain, où aimerais-tu aller ?",
        "Quelle musique te touche particulièrement en ce moment ?",
        "Qu'est-ce qui t'a fait rire aujourd'hui ?",
        "Quels rêves aimerais-tu partager avec moi ?",
        "Comment puis-je te soutenir quand tu passes une journée difficile ?",
        "Quelle petite tradition pourrait-on créer ensemble ?",
        "Quelle attention te ferait plaisir cette semaine ?",
        "Qu'aimerais-tu apprendre ou essayer bientôt ?",
        "Quel souvenir de nous te fait sourire ?",
        "Quel film aimerais-tu regarder ensemble ?",
        "Quel repas aimerais-tu qu'on prépare à deux ?",
        "Quelle activité simple te ferait plaisir ce week-end ?",
        "Qu'est-ce qui te donne le sentiment qu'on est une bonne équipe ?",
        "Comment préfères-tu qu'on se réconcilie après un désaccord ?",
        "Quelle chose aimerais-tu faire plus souvent avec moi ?",
        "Qu'est-ce qui rend une journée ordinaire spéciale pour toi ?",
        "Quelle nouvelle activité pourrait-on découvrir ensemble ?",
        "De quoi aimerais-tu parler davantage avec moi ?"
    )

    private fun expand(category: String, prefix: String, source: List<String>, day: Long): List<Question> {
        val result = mutableListOf<Question>()
        val unique = source.distinct()
        var round = 0
        while (result.size < 50) {
            unique.forEachIndexed { i, text ->
                if (result.size < 50) {
                    val suffix = if (round == 0) "" else when (round % 4) {
                        1 -> " Et toi, qu'en penses-tu ?"
                        2 -> " J'aimerais connaître ton avis."
                        3 -> " Tu peux m'en dire davantage ?"
                        else -> " Pourquoi est-ce important pour toi ?"
                    }
                    result.add(Question("${prefix}_${round}_$i", category, 1, text + suffix))
                }
            }
            round++
        }
        return result.sortedBy { Math.floorMod(it.id.hashCode().toLong() * 31L + day * 17L, Long.MAX_VALUE) }
    }

    fun questionsFor(category: String, day: Long = LocalDate.now().toEpochDay()): List<Question> =
        when (category) {
            ROMANCE -> expand(ROMANCE, "rom", romance, day)
            CONNAISSANCE -> expand(CONNAISSANCE, "know", connaissance, day)
            COPINE -> expand(COPINE, "couple", copine, day)
            else -> emptyList()
        }

    fun dailyQuestions(day: Long = LocalDate.now().toEpochDay()): List<Question> =
        categories.flatMap { questionsFor(it, day) }

    val all: List<Question> get() = dailyQuestions()
}
