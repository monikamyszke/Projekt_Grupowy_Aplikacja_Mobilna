package swap.mobileapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

// aktywność wyświetlana po starcie aplikacji
public class MainActivity extends AppCompatActivity {

    Button scanningButton;
    Button tcpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanningButton = findViewById(R.id.scanningButton);
        tcpButton = findViewById(R.id.tcpButton);

        scanningButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScanActivity();
            }
        });

        tcpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTcpActivity();
            }
        });

    }

    // funkcja przechodząca do skanowania po wciśnięciu przycisku
    public void startScanActivity() {
        startActivity(new Intent(this, ScanActivity.class));
    }

    // funkcja przechodząca do połączenia po tcp po wciśnięciu przycisku
    public void startTcpActivity() {
        startActivity(new Intent(this, TcpActivity.class));
    }
}