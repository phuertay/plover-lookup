package com.phuertay.ploverlookup

import android.app.Application

class PloverLookupApp : Application() {
    lateinit var repository: LookupRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = LookupRepository(this, LookupDatabase.create(this))
    }
}
