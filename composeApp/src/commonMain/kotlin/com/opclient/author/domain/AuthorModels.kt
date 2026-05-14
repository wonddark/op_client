// composeApp/src/commonMain/kotlin/com/opclient/author/domain/AuthorModels.kt
package com.opclient.author.domain

data class AuthorDetail(
    val key: String,
    val name: String,
    val bio: String?,
    val birthDate: String?,
    val deathDate: String?,
    val photoUrl: String?,
    val works: List<AuthorWork>,
)

data class AuthorWork(
    val key: String,
    val title: String,
    val coverUrl: String?,
)
