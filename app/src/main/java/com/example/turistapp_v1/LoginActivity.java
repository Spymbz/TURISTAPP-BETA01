package com.example.turistapp_v1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils; // Para revisar si los campos estan vacios
import android.util.Patterns; // Para validar emails
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast; // Para mostrar mensajes emergentes

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    // 1. Declarar las variables de la interfaz y Firebase
    TextInputEditText etEmailLogin, etPasswordLogin;
    Button btnLogin;
    TextView tvGoToRegister;
    ImageButton btnBackLogin;
    FirebaseAuth mAuth; // El objeto de autenticacion de Firebase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 2. Inicializar las variables
        mAuth = FirebaseAuth.getInstance(); // Conectamos con Firebase Auth

        // Conectamos las variables de Java con los IDs del XML
        etEmailLogin = findViewById(R.id.et_email_login);
        etPasswordLogin = findViewById(R.id.et_password_login);
        btnLogin = findViewById(R.id.btn_login);
        tvGoToRegister = findViewById(R.id.tv_go_to_register);
        btnBackLogin = findViewById(R.id.btn_back_login);

        // 3. Configurar los Listeners

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });
        tvGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Creamos una intención (Intent) para abrir RegisterActivity
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
        btnBackLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Cierra esta actividad y vuelve a la anterior (MainActivity)
            }
        });
    }

    // 4. Funcion para procesar el Login
    private void loginUser() {
        // Obtenemos el texto de los campos
        String email = etEmailLogin.getText().toString().trim();
        String password = etPasswordLogin.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmailLogin.setError("El email es requerido");
            etEmailLogin.requestFocus();
            return; // Detiene la ejecución si hay error
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmailLogin.setError("Por favor, ingresa un email válido");
            etEmailLogin.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPasswordLogin.setError("La contraseña es requerida");
            etPasswordLogin.requestFocus();
            return;
        }

        // --- Proceso de Firebase ---
        // (Mostramos un Toast para que el usuario sepa que está cargando)
        Toast.makeText(LoginActivity.this, "Iniciando sesión...", Toast.LENGTH_SHORT).show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // ¡Login exitoso!
                            Toast.makeText(LoginActivity.this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();

                            // Lo mandamos a la pantalla principal
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish(); // Cerramos LoginActivity

                        } else {
                            // Error en el login
                            Toast.makeText(LoginActivity.this, "Error. Revisa tus credenciales.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}