package com.opclient.subject.domain

data class SubjectPage(
    val subjectName: String,
    val workCount: Int,
    val works: List<SubjectWork>,
)

data class SubjectWork(
    val key: String,
    val title: String,
    val authorName: String?,
    val coverUrl: String?,
)
