package com.opclient.auth

import com.opclient.auth.domain.AuthRepository
import com.opclient.auth.presentation.AuthIntent
import com.opclient.auth.presentation.AuthViewModel
import com.opclient.core.ApiError
import com.opclient.core.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun fakeRepo(
        loginResult: Result<Unit, ApiError> = Result.Success(Unit),
        loggedIn: Boolean = false,
        username: String? = null,
    ) = object : AuthRepository {
        override suspend fun login(u: String, p: String) = loginResult
        override suspend fun logout() = Unit
        override suspend fun isLoggedIn() = loggedIn
        override suspend fun getLoggedInUsername() = username
    }

    @Test
    fun init_checksLoginState() = runTest {
        val vm = AuthViewModel(fakeRepo(loggedIn = true, username = "oz"))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isLoggedIn)
        assertEquals("oz", vm.uiState.value.username)
    }

    @Test
    fun login_success_updatesState() = runTest {
        val vm = AuthViewModel(fakeRepo())
        vm.onIntent(AuthIntent.UsernameChanged("oz"))
        vm.onIntent(AuthIntent.PasswordChanged("secret"))
        vm.onIntent(AuthIntent.Login)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isLoggedIn)
        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun login_failure_setsError() = runTest {
        val vm = AuthViewModel(fakeRepo(loginResult = Result.Failure(ApiError.HttpError(401, "Invalid credentials"))))
        vm.onIntent(AuthIntent.UsernameChanged("oz"))
        vm.onIntent(AuthIntent.PasswordChanged("wrong"))
        vm.onIntent(AuthIntent.Login)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoggedIn)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun logout_clearsState() = runTest {
        val vm = AuthViewModel(fakeRepo(loggedIn = true, username = "oz"))
        advanceUntilIdle()
        vm.onIntent(AuthIntent.Logout)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoggedIn)
        assertNull(vm.uiState.value.username)
    }
}
