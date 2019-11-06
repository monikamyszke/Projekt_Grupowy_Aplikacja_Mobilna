package swap.mobileapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MenuActivity extends AppCompatActivity {

    Button connectButton;
    Button statButton;
    Button helpButton;
    Button exitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        connectButton = findViewById(R.id.connect_button);
        statButton = findViewById(R.id.statistics_button);
        helpButton = findViewById(R.id.help_button);
        exitButton = findViewById(R.id.exit_button);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScanActivity();
            }
        });

        statButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startStatisticsActivity();
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

    // funkcja przechodząca do skanowania po wciśnięciu przycisku
    public void startScanActivity() {
        startActivity(new Intent(this, ScanActivity.class));
    }

    // funkcja przechodząca do statystyk
    public void startStatisticsActivity() {
        startActivity(new Intent(this, StatisticsActivity.class));
    }

    // funkcja przechodząca do pomocy dla użytkownika
    public void startHelpActivity() {
        startActivity(new Intent(this, HelpActivity.class));
    }
}
