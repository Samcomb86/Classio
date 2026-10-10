package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.viewmodel.TimetableViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Class Flow", appName)
  }

  @Test
  fun `test email sign up and sign in flow`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = TimetableViewModel(app)

    var resultSuccess = false
    var resultMsg = ""

    // 1. Invalid email
    viewModel.authenticateWithEmail("invalidemail", "123456", isSignUp = true) { success, msg ->
      resultSuccess = success
      resultMsg = msg
    }
    assertFalse(resultSuccess)
    assertTrue(resultMsg.contains("valid email", ignoreCase = true))

    // 2. Short password
    viewModel.authenticateWithEmail("student@tu.edu.pk", "123", isSignUp = true) { success, msg ->
      resultSuccess = success
      resultMsg = msg
    }
    assertFalse(resultSuccess)
    assertTrue(resultMsg.contains("6 characters", ignoreCase = true))

    // 3. Successful Sign Up
    viewModel.authenticateWithEmail("student@tu.edu.pk", "pass12345", isSignUp = true) { success, msg ->
      resultSuccess = success
      resultMsg = msg
    }
    assertTrue(resultSuccess)
    assertNotNull(viewModel.currentUser.value)
    assertEquals("student@tu.edu.pk", viewModel.currentUser.value?.email)

    // 4. Sign Out
    viewModel.signOutUser()
    assertNull(viewModel.currentUser.value)

    // 5. Successful Sign In
    viewModel.authenticateWithEmail("student@tu.edu.pk", "pass12345", isSignUp = false) { success, msg ->
      resultSuccess = success
      resultMsg = msg
    }
    assertTrue(resultSuccess)
    assertNotNull(viewModel.currentUser.value)
    assertEquals("student@tu.edu.pk", viewModel.currentUser.value?.email)
  }

  @Test
  fun `test continue with google flow`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = TimetableViewModel(app)

    var resultSuccess = false
    viewModel.continueWithGoogle("sameedjaved86@gmail.com") { success, _ ->
      resultSuccess = success
    }
    assertTrue(resultSuccess)
    assertNotNull(viewModel.currentUser.value)
    assertEquals("sameedjaved86@gmail.com", viewModel.currentUser.value?.email)
    assertTrue(viewModel.currentUser.value?.isGoogleAccount == true)

    viewModel.signOutUser()
    assertNull(viewModel.currentUser.value)
  }

  @Test
  fun `test send password reset flow`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = TimetableViewModel(app)

    var resultSuccess = false
    var resultMsg = ""

    // Invalid email
    viewModel.sendPasswordReset("notanemail") { success, msg ->
      resultSuccess = success
      resultMsg = msg
    }
    assertFalse(resultSuccess)
    assertTrue(resultMsg.contains("valid email", ignoreCase = true))

    // Valid email
    viewModel.sendPasswordReset("sameedjaved86@gmail.com") { success, msg ->
      resultSuccess = success
      resultMsg = msg
    }
    assertTrue(resultSuccess)
    assertTrue(resultMsg.contains("sameedjaved86@gmail.com", ignoreCase = true))
  }
}

