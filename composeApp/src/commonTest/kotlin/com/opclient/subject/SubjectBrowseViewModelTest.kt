package com.opclient.subject

import com.opclient.subject.presentation.FeaturedSubjects
import com.opclient.subject.presentation.SubjectBrowseViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubjectBrowseViewModelTest {

    @Test
    fun initialState_containsAll12Subjects() {
        val vm = SubjectBrowseViewModel()
        assertEquals(12, vm.uiState.value.subjects.size)
    }

    @Test
    fun initialState_matchesFeaturedSubjectsList() {
        val vm = SubjectBrowseViewModel()
        assertEquals(FeaturedSubjects.list, vm.uiState.value.subjects)
    }

    @Test
    fun initialState_containsExpectedSubjects() {
        val vm = SubjectBrowseViewModel()
        val subjects = vm.uiState.value.subjects
        assertTrue(subjects.contains("Science Fiction"))
        assertTrue(subjects.contains("Mystery"))
        assertTrue(subjects.contains("History"))
    }
}
