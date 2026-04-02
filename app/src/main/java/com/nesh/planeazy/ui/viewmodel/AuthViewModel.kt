package com.nesh.planeazy.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.nesh.planeazy.util.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _user = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isDarkMode = MutableStateFlow(preferenceManager.isDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val _currency = MutableStateFlow(preferenceManager.getCurrency())
    val currency: StateFlow<String> = _currency

    private val _isBiometricAuthenticated = MutableStateFlow(false)
    val isBiometricAuthenticated: StateFlow<Boolean> = _isBiometricAuthenticated

    private val _useBiometrics = MutableStateFlow(preferenceManager.useBiometrics())
    val useBiometrics: StateFlow<Boolean> = _useBiometrics

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        preferenceManager.setDarkMode(enabled)
    }

    fun setCurrency(newCurrency: String) {
        _currency.value = newCurrency
        preferenceManager.setCurrency(newCurrency)
    }

    fun setBiometricAuthenticated(authenticated: Boolean) {
        _isBiometricAuthenticated.value = authenticated
    }

    fun setUseBiometrics(enabled: Boolean) {
        _useBiometrics.value = enabled
        preferenceManager.setUseBiometrics(enabled)
    }

    fun signIn(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _error.value = "Email and password cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                _user.value = auth.currentUser
            } catch (e: Exception) {
                _error.value = mapFirebaseException(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUp(email: String, pass: String) {
        val validationError = validatePassword(pass)
        if (validationError != null) {
            _error.value = validationError
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                auth.createUserWithEmailAndPassword(email, pass).await()
                _user.value = auth.currentUser
            } catch (e: Exception) {
                _error.value = mapFirebaseException(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun validatePassword(pass: String): String? {
        if (pass.length < 8) return "Password must be at least 8 characters long"
        if (!pass.any { it.isUpperCase() }) return "Password must contain at least one uppercase letter"
        if (!pass.any { it.isLowerCase() }) return "Password must contain at least one lowercase letter"
        if (!pass.any { it.isDigit() }) return "Password must contain at least one number"
        if (!pass.any { !it.isLetterOrDigit() }) return "Password must contain at least one special character (e.g. @, #, $, %)"
        return null
    }

    private fun mapFirebaseException(e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidUserException -> "No account found with this email address."
            is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password. Please try again."
            is FirebaseAuthUserCollisionException -> "An account already exists with this email address."
            is FirebaseAuthWeakPasswordException -> "The password provided is too weak."
            is FirebaseNetworkException -> "Network error. Please check your internet connection."
            else -> e.localizedMessage ?: "An unexpected error occurred. Please try again."
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _error.value = "Please enter your email address to reset password"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                auth.sendPasswordResetEmail(email).await()
                _error.value = "Reset email sent to $email"
            } catch (e: Exception) {
                _error.value = mapFirebaseException(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _user.value = null
        _isBiometricAuthenticated.value = false
    }

    fun deleteAccount(onComplete: (Boolean) -> Unit) {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Delete Firestore Data
                val userRef = db.collection("users").document(currentUser.uid)
                
                // Delete subcollections first
                val collections = listOf("transactions", "goals", "budgets")
                collections.forEach { col ->
                    val snapshot = userRef.collection(col).get().await()
                    snapshot.documents.forEach { it.reference.delete().await() }
                }
                userRef.delete().await()

                // 2. Delete Auth Account
                currentUser.delete().await()
                
                _user.value = null
                onComplete(true)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Account deletion failed", e)
                _error.value = "Failed to delete account. You may need to re-authenticate first."
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
