package com.example.planitout.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.planitout.activities.LoginActivity;
import com.example.planitout.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {
    Button forgotPasswordButton, backButton;
    EditText userEmail;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        forgotPasswordButton = findViewById(R.id.forgotPasswordButton);
        backButton = findViewById(R.id.backButton);
        userEmail = findViewById(R.id.forgotPasswordEmail);

        mAuth = FirebaseAuth.getInstance();

        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = userEmail.getText().toString().trim();
                if (validateEmail(email)) {
                    resetPassword(email);
                } else {
                    userEmail.setError("Email field cannot be empty!");
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            userEmail.setError("Email field cannot be empty!");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            userEmail.setError("Enter a valid email address!");
            return false;
        }
        return true;
    }

    private void resetPassword(String email) {
        forgotPasswordButton.setEnabled(false);

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(ForgotPasswordActivity.this, "Reset Password Link Sent to the Email", Toast.LENGTH_SHORT).show();
                        forgotPasswordButton.setEnabled(true);
                        Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ForgotPasswordActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        forgotPasswordButton.setEnabled(true);
                    }
                });
    }
}
