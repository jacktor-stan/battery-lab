package com.jacktor.batterylab.views

class TeamModel {
    var name: String? = null
    var username: String? = null
    private var inProject: Boolean? = null
    private var imgURL: String? = null

    fun getNames(): String {
        return name.toString()
    }

    fun setNames(name: String) {
        this.name = name
    }

    fun getUsernames(): String {
        return username.toString()
    }

    fun setUsernames(username: String) {
        this.username = username
    }

    fun getStatus(): Boolean? {
        return inProject
    }

    fun setStatus(inProject: Boolean) {
        this.inProject = inProject
    }

    fun getimgURLs(): String {
        return imgURL.toString()
    }

    fun setimgURLs(imgURL: String) {
        this.imgURL = imgURL
    }
}