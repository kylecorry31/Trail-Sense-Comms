package com.kylecorry.trail_sense_comms.ui

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.andromeda.fragments.ColorTheme
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense_comms.R
import com.kylecorry.trail_sense_comms.app.NavigationUtils.setupWithNavController
import com.kylecorry.trail_sense_comms.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AndromedaActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding
        get() = _binding!!

    private val permissions = mutableListOf<String>()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ExceptionHandler.initialize(this)
        setColorTheme(ColorTheme.System, true)
        enableEdgeToEdge(
            navigationBarStyle = if (isDarkTheme()) {
                SystemBarStyle.dark(Resources.androidBackgroundColorSecondary(this))
            } else {
                SystemBarStyle.light(
                    Resources.androidBackgroundColorSecondary(this),
                    Color.BLACK
                )
            }
        )

        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.setupWithNavController(findNavController(), false)

        bindLayoutInsets()

        val previousPermissionStatus = permissions.map {
            Permissions.hasPermission(this, it)
        }
        requestPermissions(permissions) {
            val currentPermissionStatus = permissions.map {
                Permissions.hasPermission(this, it)
            }
            val permissionsChanged =
                previousPermissionStatus.zip(currentPermissionStatus).any { it.first != it.second }
            startApp(permissionsChanged)
        }
    }

    private fun startApp(shouldReload: Boolean) {
        if (!openDesiredTool() && shouldReload) {
            findNavController().navigate(
                findNavController().currentDestination?.id ?: R.id.action_settings
            )
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent ?: return
        setIntent(intent)
        openDesiredTool()
    }

    private fun openDesiredTool(): Boolean {
        val desiredTool = intent?.getStringExtra("tool")
        if (desiredTool == TOOL_TALK) {
            findNavController().navigate(
                R.id.talkFragment,
                null,
                NavOptions.Builder().setPopUpTo(R.id.action_settings, true).build()
            )
            return true
        } else if (desiredTool == TOOL_MESSAGING) {
            findNavController().navigate(
                R.id.action_main,
                null,
                NavOptions.Builder().setPopUpTo(R.id.action_settings, true).build()
            )
            return true
        }
        return false
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding.bottomNavigation.selectedItemId = savedInstanceState.getInt(
            "page",
            R.id.action_settings
        )
        if (savedInstanceState.containsKey("navigation")) {
            tryOrNothing {
                val bundle = savedInstanceState.getBundle("navigation_arguments")
                findNavController().navigate(savedInstanceState.getInt("navigation"), bundle)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("page", binding.bottomNavigation.selectedItemId)
        findNavController().currentBackStackEntry?.arguments?.let {
            outState.putBundle("navigation_arguments", it)
        }
        findNavController().currentDestination?.id?.let {
            outState.putInt("navigation", it)
        }
    }

    private fun bindLayoutInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun findNavController(): NavController {
        return (supportFragmentManager.findFragmentById(R.id.fragment_holder) as NavHostFragment).navController
    }

    private fun isDarkTheme(): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    companion object {
        private const val TOOL_MESSAGING = "4c285dfe-1c8b-45eb-bb79-3f1d2eb6ae48"
        private const val TOOL_TALK = "b76f32bf-6a72-4992-a741-0c9bf19ebd11"
    }
}
