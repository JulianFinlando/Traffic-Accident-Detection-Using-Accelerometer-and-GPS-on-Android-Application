package sukses.skripsi.crashdetection;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegistrationActivity extends AppCompatActivity {
    private Button mSignup;
    private EditText mEmail,mPassword,mName;
    private FirebaseAuth mAuth;
    private TextView mLogin;
    private DatabaseReference users;
    private EditText edtNoHp;
    private FirebaseAuth.AuthStateListener firebaseAuthStateListener;
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        firebaseAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null){
                    Intent intent = new Intent(getApplication(), MapsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };
        mAuth = FirebaseAuth.getInstance();

        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.passwd);
        mSignup = findViewById(R.id.signup);
        mName =  findViewById(R.id.name);
        mLogin = findViewById(R.id.masuk);
        edtNoHp = findViewById(R.id.nohp);

        users = FirebaseDatabase.getInstance().getReference("users");

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplication(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        mSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String email = mEmail.getText().toString();
                final String name = mName.getText().toString();
                final String password = mPassword.getText().toString();
                final String noHp = edtNoHp.getText().toString();
                mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(RegistrationActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(RegistrationActivity.this,"Email sudah dipakai", Toast.LENGTH_LONG).show();
                        }
                        else {
                            final String userId = mAuth.getCurrentUser().getUid();
                            final DatabaseReference currentUserDb = FirebaseDatabase.getInstance().getReference().child("users").child(userId);

                            Map userInfo = new HashMap<>();
                            userInfo.put("Email",email);
                            userInfo.put("Password",password);
                            userInfo.put("Name",name);
                            userInfo.put("NoHp",noHp);

                            currentUserDb.updateChildren(userInfo);
                        }
                    }
                });

            }
        });
    }

    protected void onStart(){
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthStateListener);
    }
    protected void onStop(){
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthStateListener);
    }
}

