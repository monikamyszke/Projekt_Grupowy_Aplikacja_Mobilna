package swap.mobileapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

// pomoc dla uzytkownika
public class HelpActivity extends AppCompatActivity {

    TextView helpTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        helpTextView = findViewById(R.id.help_textview);
        helpTextView.setMovementMethod(new ScrollingMovementMethod());

    }
}
