package com.opclient.subject.domain

import com.opclient.core.ApiError
import com.opclient.core.Result

interface SubjectRepository {
    suspend fun getSubjectPage(
        subjectName: String,
        limit: Int = 12,
        offset: Int = 0,
    ): Result<SubjectPage, ApiError>
}
