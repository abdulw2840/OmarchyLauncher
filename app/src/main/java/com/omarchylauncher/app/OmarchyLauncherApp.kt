package com.omarchylauncher.app

import android.app.Application
import com.omarchylauncher.app.data.AppRepository
import com.omarchylauncher.app.data.LauncherPreferences
import com.omarchylauncher.app.privilege.PrivilegeController
import com.topjohnwu.superuser.Shell

class OmarchyLauncherApp : Application() {

    lateinit var preferences: LauncherPreferences
        private set

    lateinit var privilegeController: PrivilegeController
        private set

    lateinit var appRepository: AppRepository
        private set

    override fun onCreate() {
        super.onCreate()
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(5)
        )

        preferences = LauncherPreferences(this)
        privilegeController = PrivilegeController(this)
        appRepository = AppRepository(this)
    }
}
