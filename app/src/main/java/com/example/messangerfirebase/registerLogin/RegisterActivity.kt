package com.example.messangerfirebase.registerLogin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.messangerfirebase.HomeActivity
import com.example.messangerfirebase.R
import com.example.messangerfirebase.models.User
import com.example.messangerfirebase.utils.Utils
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity(), Utils {

    private lateinit var auth: FirebaseAuth
    private var selectedPhoto: Uri? = null
    private lateinit var name: String
    private lateinit var email: String
    private lateinit var password: String
    private var profileImageUri: String? = null

    companion object{
        val ORIGINAL_USER = "ORIGINAL_USER"
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            closeActivityAndOpenOnther(this, HomeActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        register_signIn_button.setOnClickListener {
            register_circular_progressBar.visibility = View.VISIBLE
            name = register_name_editText.editText!!.text.toString().trim()
            email = register_email_editText.editText!!.text.toString().trim()
            password = register_password_editText.editText!!.text.toString().trim()
            if (validateForm())
                createAccount()
            else
                register_circular_progressBar.visibility = View.INVISIBLE
        }

        // Useful when there is a photo
        register_profile_image_imageView.setOnClickListener {
            selectPhoto()
        }
        register_select_photo_textView.setOnClickListener {
            selectPhoto()
        }
        register_already_haveAccount_button.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            selectedPhoto = data?.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhoto)
//            val b = Bitmap.createScaledBitmap(bitmap, 100, 100, true)
            register_select_photo_textView.visibility = View.INVISIBLE
            register_profile_image_imageView.setImageBitmap(bitmap)
        }
    }

    private fun selectPhoto() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, 0)
    }

    private fun createAccount() {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                register_circular_progressBar.visibility = View.INVISIBLE
                Toast.makeText(this, "${task.exception!!.message}", Toast.LENGTH_LONG).show()
                return@addOnCompleteListener
            }
            saveProfileImageToFirebaseStore()
        }
    }

    private fun saveProfileImageToFirebaseStore() {
        if (selectedPhoto != null) {
            val filename = "${auth.uid}" // use random generated id if you want
            Log.d("RegisterActivity", "UUID for image: $filename")
            val storageRef = FirebaseStorage.getInstance().reference.child("images/$filename")
            val uploadTask = storageRef.putFile(selectedPhoto!!).addOnSuccessListener { taskSnapshot ->
                Log.d("ActivityMain", "The uploaded picture path is: ${taskSnapshot.metadata!!.path}")
            }
            uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    register_circular_progressBar.visibility = View.INVISIBLE
                    task.exception?.let {
                        throw it
                    }
                }
                return@Continuation storageRef.downloadUrl
            }).addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    register_circular_progressBar.visibility = View.INVISIBLE
                    Toast.makeText(this, "${task.exception!!.message}", Toast.LENGTH_LONG).show()
                    return@addOnCompleteListener
                }
                profileImageUri = task.result.toString()
                saveUserToCloudFireStore()
                Log.d("RegisterActivity", "The downloaded Uri: $profileImageUri")
            }
        }

    }

    private fun saveUserToCloudFireStore() {
        if (auth.uid != null && profileImageUri != null) {
            val db = FirebaseFirestore.getInstance()
            val user = User(uid = auth.uid!!, name = name, profileImageUri = profileImageUri!!)
            db.collection("users").document(auth.uid!!).set(user).addOnSuccessListener {
                Log.d("RegisterActivity", "User has been Added Successfully")
                register_circular_progressBar.visibility = View.INVISIBLE
                val b = Bundle()
                b.putParcelable(ORIGINAL_USER, user)
                closeActivityAndOpenOnther(this, HomeActivity::class.java, b)
            }.addOnFailureListener { exception ->
                register_circular_progressBar.visibility = View.INVISIBLE
                Toast.makeText(this, "${exception.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validateForm(): Boolean {
        return validateName() && validateEmail() && validatePassword() && validateProfileImage()
    }

    private fun validateName(): Boolean {
        return when {
            name.isEmpty() -> {
                register_name_editText.error = "This Field can't be empty"
                false
            }
            name.length > 25 -> {
                register_name_editText.error = "The maximum length is 25"
                false
            }
            else -> {
                register_name_editText.error = null
                register_name_editText.isErrorEnabled = false
                true
            }
        }
    }

    private fun validateEmail(): Boolean {
        return if (email.isEmpty()) {
            register_email_editText.error = "This Field can't be empty"
            false
        } else {
            register_email_editText.error = null
            register_email_editText.isErrorEnabled = false
            true
        }
    }

    private fun validatePassword(): Boolean {
        return when {
            password.isEmpty() -> {
                register_password_editText.error = "This Field can't be empty"
                false
            }
            password.length < 6 -> {
                register_password_editText.error = "The Minimum length is 6 characters"
                false
            }
            else -> {
                register_password_editText.error = null
                register_password_editText.isErrorEnabled = false
                true
            }
        }
    }

    private fun validateProfileImage(): Boolean {
        return if (selectedPhoto == null) {
            register_select_photo_textView.text = "You have to select photo"
            register_select_photo_textView.setTextColor(resources.getColor(R.color.red))
            false
        } else {
            true
        }
    }
}
