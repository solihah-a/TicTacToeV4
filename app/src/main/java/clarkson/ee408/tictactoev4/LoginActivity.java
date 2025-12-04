package clarkson.ee408.tictactoev4;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import clarkson.ee408.tictactoev4.client.*;
import clarkson.ee408.tictactoev4.model.*;
import clarkson.ee408.tictactoev4.socket.*;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameField;
    private EditText passwordField;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Getting UI elements
        Button loginButton = findViewById(R.id.buttonLogin);
        Button registerButton = findViewById(R.id.buttonRegister);
        usernameField = findViewById(R.id.editTextUsername);
        passwordField = findViewById(R.id.editTextPassword);

        gson = new GsonBuilder().serializeNulls().create();

        //Adding Handlers
        loginButton.setOnClickListener(view -> handleLogin());
        registerButton.setOnClickListener(view -> gotoRegister());
    }

    /**
     * Process login input and pass it to {@link #submitLogin(User)}
     */
    public void handleLogin() {
        String username = usernameField.getText().toString();
        String password = passwordField.getText().toString();

        if (username.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create User object with username and password and call submitLogin()
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        submitLogin(user);
    }

    /**
     * Sends a LOGIN request to the server
     * @param user User object to login
     */
    public void submitLogin(User user) {
        //Send a LOGIN request, If SUCCESS response, call gotoPairing(), else, Toast the error message from sever
        String serializedUser = gson.toJson(user);
        Request request = new Request(Request.RequestType.LOGIN, serializedUser);

        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Response response = SocketClient.getInstance().sendRequest(request, Response.class);
                AppExecutors.getInstance().mainThread().execute(() -> {
                    if (response != null) {
                        if (response.getStatus() == Response.ResponseStatus.SUCCESS) {
                            gotoPairing(user.getUsername());
                        } else {
                            Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                AppExecutors.getInstance().mainThread().execute(() ->
                        Toast.makeText(this, "Error sending request", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    /**
     * Switch the page to {@link PairingActivity}
     * @param username the data to send
     */
    public void gotoPairing(String username) {
        //start PairingActivity and pass the username
        Intent intent = new Intent(this, PairingActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
    }

    /**
     * Switch the page to {@link RegisterActivity}
     */
    public void gotoRegister() {
        // start RegisterActivity
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }
}