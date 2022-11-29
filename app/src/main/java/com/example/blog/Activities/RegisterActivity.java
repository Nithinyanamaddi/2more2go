package com.example.blog.Activities;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.blog.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    //views
    EditText mNameEt, mEmailEt, mPasswordEt;
    Button mRegisterBtn;
    TextView mHaveAccountTV;

    ProgressDialog progressDialog;

    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Create Account");
        actionBar.setDisplayHomeAsUpEnabled(true);



        mNameEt = findViewById(R.id.nameET);
        mEmailEt = findViewById(R.id.emailET);
        mPasswordEt = findViewById(R.id.passwordET);
        mRegisterBtn = findViewById(R.id.registerBtn);
        progressDialog = new ProgressDialog(this);
        mHaveAccountTV = findViewById(R.id.have_account_TV);


        mAuth = FirebaseAuth.getInstance();

        mRegisterBtn.setOnClickListener(view -> {

            String name = mNameEt.getText().toString();
            String email = mEmailEt.getText().toString();
            String password = mPasswordEt.getText().toString().trim();

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

                mEmailEt.setError("Invalid Email");

            } else if (password.length() < 6) {
                mPasswordEt.setError("Password length must be at least 6 characters");
            } else {
                registerUser(name, email, password);
            }

        });

        mHaveAccountTV.setOnClickListener(view -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });


    }

    @Override
    public boolean onSupportNavigateUp() {


        onBackPressed();
        return super.onSupportNavigateUp();

    }

    private void registerUser(String name, String email, String password) {

        progressDialog.setTitle("Registering User...");
        progressDialog.show();
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        progressDialog.dismiss();

                        FirebaseUser user = mAuth.getCurrentUser();
                        assert user != null;
                        String email1 = user.getEmail();
                        String uid = user.getUid();

                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("email", email1);
                        hashMap.put("uid", uid);
                        hashMap.put("name", name);
                        hashMap.put("onlineStatus", "online");
                        hashMap.put("typingTo", "noOne");
                        hashMap.put("phone", "");
                        hashMap.put("profile", "");
                        hashMap.put("cover", "");

                        FirebaseDatabase database = FirebaseDatabase.getInstance();

                        DatabaseReference reference = database.getReference("Users");

                        reference.child(uid).setValue(hashMap);


                        Toast.makeText(RegisterActivity.this, "Registered...\n" + user.getEmail(), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                        finish();

                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }

                }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

        });
    }


}
