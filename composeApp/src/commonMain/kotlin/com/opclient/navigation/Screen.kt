package com.opclient.navigation

sealed class Screen {
    data object Search : Screen()
    data class BookDetail(val workKey: String) : Screen()
    data class AuthorDetail(val authorKey: String) : Screen()
    data object SubjectList : Screen()
    data class SubjectDetail(val subjectName: String) : Screen()
    data object Library : Screen()
    data object Profile : Screen()
}
