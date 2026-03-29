package com.zerohouse.oxwar.theme

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.zerohouse.oxwar.error.AppException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@RestController
@RequestMapping("/api/ai")
class AiGenerateController(
    @Value("\${openrouter.api-key:}") private val apiKey: String,
    @Value("\${oxwar.admin-uuid:}") private val adminUuid: String,
) {
    private val webClient = WebClient.builder()
        .clientConnector(
            org.springframework.http.client.reactive.ReactorClientHttpConnector(
                reactor.netty.http.client.HttpClient.create()
                    .followRedirect(true)
                    .compress(true)
                    .responseTimeout(java.time.Duration.ofSeconds(30))
            )
        )
        .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
        .build()
    private val mapper = jacksonObjectMapper()

    @PostMapping("/generate")
    suspend fun generate(@RequestBody req: AiGenerateRequest, @RequestHeader("X-UUID", required = false) uuid: String?): List<QuizDto> {
        if (adminUuid.isNotBlank() && uuid != adminUuid) throw com.zerohouse.oxwar.error.ForbiddenException("편집 권한이 없습니다")
        if (apiKey.isBlank()) throw AppException("NO_API_KEY", HttpStatus.SERVICE_UNAVAILABLE, "API 키가 설정되지 않았습니다")

        var context = ""

        val categoryInstruction = if (!req.category.isNullOrBlank())
            "\n세부 카테고리: \"${req.category}\". 이 카테고리에 맞는 문제만 만들어." else ""

        val refInstruction = if (context.isNotBlank())
            "\n반드시 위 참고 자료에 있는 내용만을 기반으로 퀴즈를 만들어. 참고 자료에 없는 내용은 절대 포함하지 마."
        else ""

        val customPart = if (!req.customPrompt.isNullOrBlank()) "\n추가 지시: ${req.customPrompt}" else ""

        val prompt = """"${req.topic}" 주제로 퀴즈 ${req.count}개를 만들어줘.$categoryInstruction$context$refInstruction$customPart
각 문제마다 2지선다 또는 4지선다를 적절히 판단해서 만들어. O/X로 판별 가능한 단순 문제는 2지선다, 여러 보기가 필요한 문제는 4지선다로.
각 문제에 1~2문장의 간단한 해설(explanation)을 포함해.
JSON 배열로만 응답해. 다른 텍스트 없이.
형식: [{"question":"질문","options":["보기1","보기2"] 또는 ["보기1","보기2","보기3","보기4"],"answer":정답인덱스(0부터),"explanation":"해설"}]"""

        val content = callOpenRouter(req.model, prompt)
        val match = Regex("\\[\\s*\\{[\\s\\S]*}\\s*]").find(content)
            ?: throw AppException("PARSE_ERROR", HttpStatus.BAD_GATEWAY, "AI 응답을 파싱할 수 없습니다")

        val parsed: List<QuizDto> = mapper.readValue(match.value)
        return parsed.map {
            val optCount = it.options.size.coerceIn(2, 4)
            QuizDto(
                question = it.question,
                options = it.options.take(optCount),
                answer = it.answer.coerceIn(0, optCount - 1),
                category = req.category ?: "",
            )
        }
    }

    private suspend fun callOpenRouter(model: String, prompt: String): String {
        val body = mapper.writeValueAsString(mapOf(
            "model" to model,
            "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
            "temperature" to 0.7,
        ))

        val response = webClient.post()
            .uri("https://openrouter.ai/api/v1/chat/completions")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .bodyValue(body)
            .retrieve()
            .awaitBody<String>()

        val json = mapper.readTree(response)
        return json.at("/choices/0/message/content").asText("")
    }

}

data class AiGenerateRequest(
    val topic: String = "",
    val category: String? = null,
    val count: Int = 10,
    val model: String = "openai/gpt-5.4-pro",
    val customPrompt: String? = null,
)
