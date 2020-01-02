package swap.mobileapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

// menu glowne aplikacji
public class MenuActivity extends AppCompatActivity {

    // przycisk polaczenia
    Button connectButton;

    // przycisk pomocy dla uzytkownika
    Button helpButton;

    // przycisk wyjscia z aplikacji
    Button exitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        connectButton = findViewById(R.id.connect_button);
        helpButton = findViewById(R.id.help_button);
        exitButton = findViewById( R.id.exit_button);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScanActivity();
            }
        });

        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startHelpActivity();
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                System.exit(0);
            }
        });
    }

    // funkcja przechodzaca do wyszukiwania urzadzen
    public void startScanActivity() {
        startActivity(new Intent(this, ScanActivity.class));
    }


    // funkcja przechodząaa do pomocy dla uzytkownika
    public void startHelpActivity() {
        startActivity(new Intent(this, HelpActivity.class));
    }
}
