package com.example.turistapp_v1;

// --- Imports necesarios ---
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser; // Para obtener el ID del usuario
import com.google.firebase.firestore.FirebaseFirestore; // Para guardar el nombre

import java.util.HashMap; // Para crear el objeto de usuario
import java.util.Map; // Para crear el objeto de usuario

public class RegisterActivity extends AppCompatActivity {

    // 1. Declarar las variables
    TextInputEditText etNameRegister, etEmailRegister, etPasswordRegister, etConfirmPasswordRegister;
    Button btnRegister;
    TextView tvGoToLogin;
    ImageButton btnBackRegister;
    FirebaseAuth mAuth;
    FirebaseFirestore mFirestore; // Base de datos para guardar el nombre

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 2. Inicializar las variables
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance(); // Conectamos con Firestore

        // Conectamos con los IDs del XML
        etNameRegister = findViewById(R.id.et_name_register);
        etEmailRegister = findViewById(R.id.et_email_register);
        etPasswordRegister = findViewById(R.id.et_password_register);
        etConfirmPasswordRegister = findViewById(R.id.et_confirm_password_register);
        btnRegister = findViewById(R.id.btn_register);
        tvGoToLogin = findViewById(R.id.tv_go_to_login);
        btnBackRegister = findViewById(R.id.btn_back_register);

        // 3. Configurar los "oyentes" de clics

        // ¿Qué pasa al hacer clic en el botón "Registrarme"?
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser(); // Llamamos a nuestra función de registro
            }
        });

        // ¿Qué pasa al hacer clic en el texto "Inicia Sesión"?
        tvGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Simplemente cerramos esta actividad para volver al Login
                // (Asumiendo que siempre vienes desde Login)
                // O mejor, iniciamos LoginActivity por si acaso.
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Cerramos esta para que no se acumulen
            }
        });

        btnBackRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Cierra esta actividad y vuelve a la anterior (LoginActivity)
            }
        });
    }

    // 4. Función para procesar el Registro
    private void registerUser() {
        // Obtenemos el texto de los campos
        String name = etNameRegister.getText().toString().trim();
        String email = etEmailRegister.getText().toString().trim();
        String password = etPasswordRegister.getText().toString().trim();
        String confirmPassword = etConfirmPasswordRegister.getText().toString().trim();

        // --- Validaciones ---
        if (TextUtils.isEmpty(name)) {
            etNameRegister.setError("El nombre es requerido");
            etNameRegister.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmailRegister.setError("El email es requerido");
            etEmailRegister.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmailRegister.setError("Ingresa un email válido");
            etEmailRegister.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPasswordRegister.setError("La contraseña es requerida");
            etPasswordRegister.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPasswordRegister.setError("La contraseña debe tener al menos 6 caracteres");
            etPasswordRegister.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPasswordRegister.setError("Confirma la contraseña");
            etConfirmPasswordRegister.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPasswordRegister.setError("Las contraseñas no coinciden");
            etConfirmPasswordRegister.requestFocus();
            return;
        }

        // --- Proceso de Firebase ---
        Toast.makeText(RegisterActivity.this, "Creando cuenta...", Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // ¡Cuenta creada en Auth!
                            // Ahora guardamos el nombre en Firestore
                            FirebaseUser user = mAuth.getCurrentUser();
                            String userId = user.getUid();

                            // Creamos un "mapa" (objeto) para guardar los datos
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("nombre", name);
                            userData.put("email", email);
                            userData.put("rol", "usuario"); // ¡Importante para el futuro!

                            // Guardamos en la colección "usuarios" con el ID del usuario
                            mFirestore.collection("usuarios").document(userId)
                                    .set(userData)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // ¡Todo salió bien!
                                            Toast.makeText(RegisterActivity.this, "¡Cuenta creada exitosamente!", Toast.LENGTH_SHORT).show();

                                            // Mandamos al MainActivity
                                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Error al guardar en Firestore
                                            Toast.makeText(RegisterActivity.this, "Error al guardar datos de usuario.", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                        } else {
                            // Error al crear cuenta (ej: email ya existe)
                            Toast.makeText(RegisterActivity.this, "Error al crear la cuenta. Puede que el email ya esté en uso.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}