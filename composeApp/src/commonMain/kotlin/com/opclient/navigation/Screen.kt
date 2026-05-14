package com.opclient.navigation

sealed class Screen {
    data object Search : Screen()
    data class BookDetail(val workKey: String) : Screen()
    data class AuthorDetail(val authorKey: String) : Screen()
}
