package com.opclient.subject.data

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.core.map
import com.opclient.subject.domain.SubjectPage
import com.opclient.subject.domain.SubjectRepository

class SubjectRepositoryImpl(
    private val apiClient: SubjectApiClient,
    private val cache: SubjectCache,
) : SubjectRepository {

    override suspend fun getSubjectPage(
        subjectName: String,
        limit: Int,
        offset: Int,
    ): Result<SubjectPage, ApiError> {
        val normalized = subjectName.replace(" ", "_").lowercase()

        if (offset == 0) {
            val cacheKey = "subject:$normalized"
            cache.get(cacheKey)?.let { return Result.Success(it) }
            return when (val result = apiClient.getSubject(subjectName, limit, offset)) {
                is Result.Success -> {
                    val page = result.value.toDomain()
                    cache.put(cacheKey, page)
                    Result.Success(page)
                }
                is Result.Failure -> result
            }
        }

        return when (val result = apiClient.getSubject(subjectName, limit, offset)) {
            is Result.Success -> Result.Success(result.value.toDomain())
            is Result.Failure -> result
        }
    }

    override suspend fun searchSubjects(query: String): Result<List<String>, ApiError> =
        apiClient.searchSubjects(query).map { dtos -> dtos.map { it.name } }
}
