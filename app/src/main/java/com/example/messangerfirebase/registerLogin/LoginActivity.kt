package com.example.messangerfirebase.registerLogin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.messangerfirebase.HomeActivity
import com.example.messangerfirebase.R
import com.example.messangerfirebase.models.User
import com.example.messangerfirebase.utils.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity(), Utils {

    private lateinit var auth: FirebaseAuth
    private lateinit var email: String
    private lateinit var password: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        login_sigIn_button.setOnClickListener {
            login_progressBar.visibility = View.VISIBLE
            email = login_email_editText.editText!!.text.toString().trim()
            password = login_password_editText.editText!!.text.toString().trim()

            if (validateForm())
                logIn()
            else
                login_progressBar.visibility = View.INVISIBLE
        }

        login_dont_haveAccount_textView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null)
            closeActivityAndOpenOnther(this, HomeActivity::class.java)
    }

    private fun logIn() {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            login_progressBar.visibility = View.INVISIBLE
            if (task.isSuccessful) {
                FirebaseFirestore.getInstance().collection("users").document(auth.uid.toString()).get()
                    .addOnSuccessListener { document ->
                        val b = Bundle()
                        val user = User(
                            name = document.get("name").toString(),
                            uid = document.get("uid").toString(),
                            profileImageUri = document.get("profileImageUri").toString()
                        )
                        b.putParcelable(RegisterActivity.ORIGINAL_USER, user)
                        closeActivityAndOpenOnther(this, HomeActivity::class.java, b)
                    }
            } else {
                Toast.makeText(this, "${task.exception!!.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validateForm(): Boolean {
        return validateEmail() && validatePassword()
    }

    private fun validateEmail(): Boolean {
        return if (email.isEmpty()) {
            login_email_editText.error = "This Field can't be empty"
            false
        } else {
            login_email_editText.error = null
            login_email_editText.isErrorEnabled = false
            true
        }
    }

    private fun validatePassword(): Boolean {
        return when {
            password.isEmpty() -> {
                login_password_editText.error = "This Field can't be empty"
                false
            }
            password.length < 6 -> {
                login_password_editText.error = "The Minimum length is 6 characters"
                false
            }
            else -> {
                login_password_editText.error = null
                login_password_editText.isErrorEnabled = false
                true
            }
        }
    }
}
