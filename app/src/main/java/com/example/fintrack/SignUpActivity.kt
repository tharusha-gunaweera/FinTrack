package com.example.fintrack

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SignUpActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSignUp: Button


    private lateinit var ivValidation1: ImageView
    private lateinit var ivValidation2: ImageView
    private lateinit var ivValidation3: ImageView
    private lateinit var ivValidation4: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()  // Hide the ActionBar
        setContentView(R.layout.activity_signup)

        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSignUp = findViewById(R.id.btnSignUp)
        val tvSignIn = findViewById<TextView>(R.id.tvSignIn)

        tvSignIn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        ivValidation1 = findViewById(R.id.ivValidation1)
        ivValidation2 = findViewById(R.id.ivValidation2)
        ivValidation3 = findViewById(R.id.ivValidation3)
        ivValidation4 = findViewById(R.id.ivValidation4)

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePassword(s.toString())
            }
        })

        btnSignUp.setOnClickListener {
            val username = etUsername.text.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else if (!isPasswordValid(password)) {
                Toast.makeText(this, "Password does not meet the requirements", Toast.LENGTH_SHORT).show()
            } else if (isUserAlreadyRegistered(email)) {
                Toast.makeText(this, "User already exists with this email", Toast.LENGTH_SHORT).show()
            } else {
                saveUserToSharedPreferences(username, email, password)
                Toast.makeText(this, "SignUp Successful", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun isUserAlreadyRegistered(email: String): Boolean {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val usersJson = sharedPref.getString("users", "[]")
        val type = object : TypeToken<List<User>>() {}.type
        val users = Gson().fromJson<List<User>>(usersJson, type)
        return users.any { it.email == email }
    }

    private fun validatePassword(password: String) {
        val hasLowercase = password.matches(Regex(".*[a-z].*"))
        val hasUppercase = password.matches(Regex(".*[A-Z].*"))
        ivValidation1.setImageResource(if (hasLowercase && hasUppercase) R.drawable.ic_green_check else R.drawable.ic_red_cross)

        val hasNumber = password.matches(Regex(".*\\d.*"))
        ivValidation2.setImageResource(if (hasNumber) R.drawable.ic_green_check else R.drawable.ic_red_cross)

        val isLengthValid = password.length >= 6
        ivValidation3.setImageResource(if (isLengthValid) R.drawable.ic_green_check else R.drawable.ic_red_cross)

        val hasNoSpaces = password.trim() == password
        ivValidation4.setImageResource(if (hasNoSpaces) R.drawable.ic_green_check else R.drawable.ic_red_cross)
    }

    private fun isPasswordValid(password: String): Boolean {
        val hasLowercase = password.matches(Regex(".*[a-z].*"))
        val hasUppercase = password.matches(Regex(".*[A-Z].*"))
        val hasNumber = password.matches(Regex(".*\\d.*"))
        val isLengthValid = password.length >= 6
        val hasNoSpaces = password.trim() == password

        return hasLowercase && hasUppercase && hasNumber && isLengthValid && hasNoSpaces
    }

    private fun saveUserToSharedPreferences(username: String, email: String, password: String) {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val usersJson = sharedPref.getString("users", "[]")
        val type = object : TypeToken<List<User>>() {}.type
        val users = Gson().fromJson<List<User>>(usersJson, type).toMutableList()
        
        // Add new user
        users.add(User(username, email, password))
        
        // Save updated users list
        val editor = sharedPref.edit()
        editor.putString("users", Gson().toJson(users))
        editor.apply()
    }

    data class User(
        val username: String,
        val email: String,
        val password: String
    )
}