package com.appblish.filora

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Empty Hilt-enabled host activity for compose/navigation instrumented tests that set
 * their own content (via the compose test rule). Unlike [MainActivity] it does not call
 * `setContent` in `onCreate`, so a test is free to host its own NavHost + TestNavHostController
 * while still resolving `hiltViewModel()` for the real feature screens.
 */
@AndroidEntryPoint
class HiltComponentActivity : ComponentActivity()
