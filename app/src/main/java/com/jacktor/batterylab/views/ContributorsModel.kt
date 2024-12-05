package com.jacktor.batterylab.views

data class ContributorsModel(
    var name: String = "",
    var username: String = "",
    var avatarUrl: String = "",
    var contributions: Int = 0,
    var htmlUrl: String = ""
)