package com.example.parallelagent

object ForegroundState {

    @Volatile
    var currentPackage: String? = null
        private set

    @Volatile
    var lastUpdatedAt: Long = 0L
        private set

    fun update(packageName: String) {
        currentPackage = packageName
        lastUpdatedAt = System.currentTimeMillis()
    }
}

