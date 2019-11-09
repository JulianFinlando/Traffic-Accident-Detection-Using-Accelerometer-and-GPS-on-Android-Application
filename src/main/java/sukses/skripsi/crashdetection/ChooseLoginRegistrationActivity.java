package sukses.skripsi.crashdetection;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ChooseLoginRegistrationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_login_registration);
        Button mLogin = (Button)findViewById(R.id.btnlogin);
        Button mSignup = (Button)findViewById(R.id.btnsignup);

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplication(), LoginActivity.class);
                startActivity(intent);
                return;
            }
        });
        mSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplication(), RegistrationActivity.class);
                startActivity(intent);
                return;
            }
        });
    }
}
