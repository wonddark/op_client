package com.opclient.subject

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.subject.domain.SubjectPage
import com.opclient.subject.domain.SubjectRepository
import com.opclient.subject.presentation.FeaturedSubjects
import com.opclient.subject.presentation.SubjectBrowseViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeSubjectRepository : SubjectRepository {
    override suspend fun getSubjectPage(
        subjectName: String,
        limit: Int,
        offset: Int,
    ): Result<SubjectPage, ApiError> = Result.Success(SubjectPage(subjectName, 0, emptyList()))

    override suspend fun searchSubjects(query: String): Result<List<String>, ApiError> =
        Result.Success(emptyList())
}

class SubjectBrowseViewModelTest {

    @Test
    fun initialState_containsAll12Subjects() {
        val vm = SubjectBrowseViewModel(FakeSubjectRepository())
        assertEquals(12, vm.uiState.value.subjects.size)
    }

    @Test
    fun initialState_matchesFeaturedSubjectsList() {
        val vm = SubjectBrowseViewModel(FakeSubjectRepository())
        assertEquals(FeaturedSubjects.list, vm.uiState.value.subjects)
    }

    @Test
    fun initialState_containsExpectedSubjects() {
        val vm = SubjectBrowseViewModel(FakeSubjectRepository())
        val subjects = vm.uiState.value.subjects
        assertTrue(subjects.contains("Science Fiction"))
        assertTrue(subjects.contains("Mystery"))
        assertTrue(subjects.contains("History"))
    }
}
