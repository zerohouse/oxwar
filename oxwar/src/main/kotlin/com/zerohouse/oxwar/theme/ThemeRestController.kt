package com.zerohouse.oxwar.theme

import com.zerohouse.oxwar.error.DuplicateException
import com.zerohouse.oxwar.error.ForbiddenException
import com.zerohouse.oxwar.error.InvalidInputException
import com.zerohouse.oxwar.error.NotFoundException
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*

@Transactional
@RestController
@RequestMapping("/api/themes")
class ThemeRestController(
    private val themeRepository: ThemeRepository,
    @Value("\${oxwar.admin-uuid:}") private val adminUuid: String,
) {
    @GetMapping
    fun list(@RequestHeader("X-UUID", required = false) uuid: String?): List<ThemeDto> =
        themeRepository.findAll().map { it.toDto(isAdmin(uuid)) }

    @GetMapping("/{slug}")
    fun get(@PathVariable slug: String, @RequestHeader("X-UUID", required = false) uuid: String?): ThemeDetailDto {
        val theme = themeRepository.findBySlug(slug)
            ?: themeRepository.save(ThemeEntity(slug = slug, title = slug))
        return theme.toDetailDto(isAdmin(uuid))
    }

    @PostMapping
    fun create(@RequestBody req: CreateThemeRequest, @RequestHeader("X-UUID", required = false) uuid: String?): ThemeDto {
        checkAdmin(uuid)
        val slug = req.slug.lowercase().replace(Regex("[^a-z0-9-]"), "")
        if (slug.isBlank()) throw InvalidInputException("테마 ID는 영문, 숫자, -만 사용할 수 있습니다")
        if (themeRepository.findBySlug(slug) != null) throw DuplicateException("이미 존재하는 테마입니다")
        val theme = themeRepository.save(ThemeEntity(slug = slug, title = req.title.ifBlank { slug }))
        return theme.toDto(true)
    }

    @PutMapping("/{slug}")
    fun updateQuizzes(
        @PathVariable slug: String,
        @RequestBody req: UpdateQuizzesRequest,
        @RequestHeader("X-UUID", required = false) uuid: String?,
    ): ThemeDetailDto {
        checkAdmin(uuid)
        val theme = themeRepository.findBySlug(slug)
            ?: throw NotFoundException("테마를 찾을 수 없습니다")

        theme.title = req.title ?: theme.title
        if (req.categories != null) {
            theme.categories = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(req.categories)
        }
        if (req.defaultPrompt != null) {
            theme.defaultPrompt = req.defaultPrompt
        }
        theme.quizzes.clear()
        req.quizzes.forEachIndexed { i, q ->
            theme.quizzes.add(QuizEntity(
                theme = theme,
                question = q.question,
                options = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(q.options),
                answer = q.answer,
                category = q.category,
                sortOrder = i,
            ))
        }
        themeRepository.save(theme)
        return theme.toDetailDto(true)
    }

    @DeleteMapping("/{slug}")
    fun delete(@PathVariable slug: String, @RequestHeader("X-UUID", required = false) uuid: String?) {
        checkAdmin(uuid)
        val theme = themeRepository.findBySlug(slug)
            ?: throw NotFoundException("테마를 찾을 수 없습니다")
        themeRepository.delete(theme)
    }

    private fun isAdmin(uuid: String?): Boolean =
        adminUuid.isNotBlank() && uuid == adminUuid

    private fun checkAdmin(uuid: String?) {
        if (!isAdmin(uuid)) throw ForbiddenException("편집 권한이 없습니다")
    }
}

data class CreateThemeRequest(val slug: String = "", val title: String = "")
data class UpdateQuizzesRequest(val title: String? = null, val categories: List<String>? = null, val defaultPrompt: String? = null, val quizzes: List<QuizDto> = emptyList())
data class QuizDto(val question: String = "", val options: List<String> = listOf("", ""), val answer: Int = 0, val category: String = "")
data class ThemeDto(val slug: String, val title: String, val quizCount: Int, val editable: Boolean)
data class ThemeDetailDto(val slug: String, val title: String, val categories: List<String>, val defaultPrompt: String, val editable: Boolean, val quizzes: List<QuizDto>)

private fun ThemeEntity.parseCategories(): List<String> = try {
    com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().readValue(categories, com.fasterxml.jackson.module.kotlin.jacksonTypeRef<List<String>>())
} catch (_: Exception) { emptyList() }

fun ThemeEntity.toDto(editable: Boolean) = ThemeDto(slug, title, quizzes.size, editable)
fun ThemeEntity.toDetailDto(editable: Boolean) = ThemeDetailDto(slug, title, parseCategories(), defaultPrompt, editable, quizzes.map {
    val opts: List<String> = try {
        com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().readValue(it.options, com.fasterxml.jackson.module.kotlin.jacksonTypeRef<List<String>>())
    } catch (_: Exception) { listOf("O", "X") }
    QuizDto(it.question, opts, it.answer, it.category)
})
