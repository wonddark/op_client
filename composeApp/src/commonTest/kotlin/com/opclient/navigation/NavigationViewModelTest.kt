package com.opclient.navigation

import com.opclient.ui.navigation.Destination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun initialStack_isSearch() = runTest {
        val vm = NavigationViewModel()
        assertEquals(listOf(Screen.Search), vm.stack.value)
    }

    @Test
    fun navigateTo_pushesScreen() = runTest {
        val vm = NavigationViewModel()
        vm.navigateTo(Screen.BookDetail("/works/OL1W"))
        assertEquals(listOf(Screen.Search, Screen.BookDetail("/works/OL1W")), vm.stack.value)
    }

    @Test
    fun navigateBack_popsScreen() = runTest {
        val vm = NavigationViewModel()
        vm.navigateTo(Screen.BookDetail("/works/OL1W"))
        vm.navigateBack()
        assertEquals(listOf(Screen.Search), vm.stack.value)
    }

    @Test
    fun navigateBack_onRoot_isNoOp() = runTest {
        val vm = NavigationViewModel()
        vm.navigateBack()
        assertEquals(listOf(Screen.Search), vm.stack.value)
    }

    @Test
    fun navigateToTab_search_resetsToSearch() = runTest {
        val vm = NavigationViewModel()
        vm.navigateTo(Screen.BookDetail("/works/OL1W"))
        vm.navigateToTab(Destination.SEARCH)
        assertEquals(listOf(Screen.Search), vm.stack.value)
    }

    @Test
    fun navigateToTab_browse_resetsToSubjectList() = runTest {
        val vm = NavigationViewModel()
        vm.navigateTo(Screen.BookDetail("/works/OL1W"))
        vm.navigateToTab(Destination.BROWSE)
        assertEquals(listOf(Screen.SubjectList), vm.stack.value)
    }

    @Test
    fun navigateToTab_library_resetsToSearch() = runTest {
        val vm = NavigationViewModel()
        vm.navigateTo(Screen.BookDetail("/works/OL1W"))
        vm.navigateToTab(Destination.LIBRARY)
        assertEquals(listOf(Screen.Search), vm.stack.value)
    }

    @Test
    fun deepStack_multipleBack() = runTest {
        val vm = NavigationViewModel()
        vm.navigateTo(Screen.BookDetail("/works/OL1W"))
        vm.navigateTo(Screen.AuthorDetail("/authors/OL1A"))
        vm.navigateBack()
        assertEquals(listOf(Screen.Search, Screen.BookDetail("/works/OL1W")), vm.stack.value)
    }
}
