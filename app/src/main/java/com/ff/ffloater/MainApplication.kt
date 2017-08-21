package com.ff.ffloater

import android.app.Application
import android.content.Context

/**
 * Created by feifan on 2017/8/16.
 * Contacts me:404619986@qq.com
 */
class MainApplication : Application() {
    companion object {
        @Volatile var instance: MainApplication? = null
            private set
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        instance = this
    }


}
