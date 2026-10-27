package com.example.frontend

import android.app.Application
import com.example.frontend.data.Dependencias

class CampusBitesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Dependencias.iniciar(this)
    }
}
