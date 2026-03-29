package com.zerohouse.oxwar

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.zerohouse.oxwar.theme.QuizEntity
import com.zerohouse.oxwar.theme.ThemeEntity
import com.zerohouse.oxwar.theme.ThemeRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

data class SeedQuiz(
    val question: String,
    val options: List<String>,
    val answer: Int,
    val category: String = "",
    val explanation: String = "",
)

@Component
class SeedLoader(private val themeRepository: ThemeRepository) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(SeedLoader::class.java)
    private val mapper = jacksonObjectMapper()

    @Transactional
    override fun run(args: ApplicationArguments) {
        val seedDir = ClassPathResource("seed-quizzes.json")
        if (seedDir.exists()) {
            loadLegacySeed()
        }
        loadDataDirs()
    }

    private fun loadLegacySeed() {
        val resource = ClassPathResource("seed-quizzes.json")
        if (!resource.exists()) return

        data class SeedTheme(
            val slug: String, val title: String,
            val categories: List<String> = emptyList(),
            val defaultPrompt: String = "",
            val quizzes: List<SeedQuiz> = emptyList(),
        )

        val seeds: List<SeedTheme> = mapper.readValue(resource.inputStream)
        for (seed in seeds) {
            if (themeRepository.findBySlug(seed.slug) != null) continue
            saveTheme(seed.slug, seed.title, seed.categories, seed.defaultPrompt, seed.quizzes)
        }
    }

    private fun loadDataDirs() {
        // data/{slug}/categories.json + quizzes.json
        val basePath = "data"
        // scan known slugs from seed-quizzes or by listing
        val categoriesResource = { slug: String -> ClassPathResource("$basePath/$slug/categories.json") }
        val quizzesResource = { slug: String -> ClassPathResource("$basePath/$slug/quizzes.json") }

        // Try known slugs from seed data
        val seedResource = ClassPathResource("seed-quizzes.json")
        val slugs = mutableSetOf<String>()
        if (seedResource.exists()) {
            val seeds: List<Map<String, Any>> = mapper.readValue(seedResource.inputStream)
            seeds.forEach { slugs.add(it["slug"] as String) }
        }

        for (slug in slugs) {
            val catRes = categoriesResource(slug)
            val quizRes = quizzesResource(slug)
            if (!catRes.exists() || !quizRes.exists()) continue
            if (themeRepository.findBySlug(slug) != null) continue

            val categories: List<String> = mapper.readValue(catRes.inputStream)
            val quizzes: List<SeedQuiz> = mapper.readValue(quizRes.inputStream)
            saveTheme(slug, slug, categories, "", quizzes)
        }
    }

    private fun saveTheme(slug: String, title: String, categories: List<String>, defaultPrompt: String, quizzes: List<SeedQuiz>) {
        val theme = ThemeEntity(
            slug = slug,
            title = title,
            categories = mapper.writeValueAsString(categories),
            defaultPrompt = defaultPrompt,
        )
        quizzes.forEachIndexed { i, q ->
            theme.quizzes.add(
                QuizEntity(
                    theme = theme,
                    question = q.question,
                    options = mapper.writeValueAsString(q.options),
                    answer = q.answer,
                    category = q.category,
                    explanation = q.explanation,
                    sortOrder = i,
                )
            )
        }
        themeRepository.save(theme)
        log.info("Seed loaded: '$slug' with ${quizzes.size} quizzes")
    }
}
