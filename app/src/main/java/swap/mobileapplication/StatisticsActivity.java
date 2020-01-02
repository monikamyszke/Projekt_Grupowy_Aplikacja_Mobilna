package swap.mobileapplication;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.st.BlueSTSDK.Feature;
import com.st.BlueSTSDK.Features.FeatureAcceleration;
import com.st.BlueSTSDK.Features.FeatureGyroscope;
import com.st.BlueSTSDK.Manager;
import com.st.BlueSTSDK.Node;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class StatisticsActivity extends AppCompatActivity {

    // klient TCP
    private TcpClient tcpClient;

    // wykorzystywane charakterystyki
    private FeatureAcceleration mAccelerator;
    private FeatureGyroscope mGyroscope;

    // licznik wyslanych pakietow
    volatile int packCounter;

    // wezel transmitujacy dane
    private Node mNode;

    // listy do przechowywania probek wykorzystywanych do uformowania pakietu
    ArrayList<String> timestampArray;
    ArrayList<String> accDataPacket;
    ArrayList<String> gyroDataPacket;

    // zmienne pomocnicze przy zliczaniu probek danych
    private volatile AtomicBoolean a;
    private volatile AtomicBoolean g;
    private volatile AtomicBoolean t;
    private volatile AtomicBoolean inProgress;

    // liczniki aktywnosci
    int jogging_time;
    int staying_time;
    int march_time;
    int running_time;
    int hop_number;

    // zmienne wykorzystywane przy uaktualnianiu wynikow klasyfikacji
    int id_to_update;
    int new_value;

    ConnectingTask connectingTask;
    Semaphore sem;

    // fragment zarzadzajacy polaczeniem z wezlem
    // pozwala uniknac ponownego polaczenia za kazdym razem gdy aktywnosc jest wznawiana
    private NodeContainerFragment mNodeContainer;

    // tag uzywany w celu otrzymania NodeContainerFragment
    private final static String NODE_FRAGMENT = StatisticsActivity.class.getCanonicalName() + "" +
            ".NODE_FRAGMENT";

    // tag uzywany do przechowywania id wezla
    private final static String NODE_TAG = StatisticsActivity.class.getCanonicalName() + "" +
            ".NODE_TAG";

    /**
     * Disconnects using a background task to avoid doing long/network operations on the UI thread
     */
    public class DisconnectTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            tcpClient.stopClient();
            tcpClient = null;
            return null;
        }
    }

    public class ConnectingTask extends AsyncTask<String, String, TcpClient> {

        // metoda uruchamiana w oddzielnym watku
        @Override
        protected TcpClient doInBackground(String... message) {

            // utworzenie klienta TCP
            tcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
                @Override
                public void messageReceived(final String message) {
                    new_value = 0;
                    id_to_update = 0;
                    Log.e("Client", message);

                    // inkrementacja danych w tabeli na podstawie odpowiedzi serwera
                    if (message.equals("Bieg")) {
                        running_time++;
                        new_value = running_time;
                        id_to_update = R.id.running_time;
                    } else if (message.equals("Trucht")) {
                        jogging_time++;
                        new_value = jogging_time;
                        id_to_update = R.id.jogging_time;
                    } else if (message.equals("Stanie")) {
                        staying_time++;
                        new_value = staying_time;
                        id_to_update = R.id.staying_time;
                    } else if (message.equals("Marsz")) {
                        march_time++;
                        new_value = march_time;
                        id_to_update = R.id.march_time;
                    } else if (message.equals("Skok")) {
                        hop_number++;
                        new_value = hop_number;
                        id_to_update = R.id.hop_number;
                    }

                    StatisticsActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView textview;
                            if (id_to_update != 0) {
                                textview = findViewById(id_to_update);
                                textview.setText(String.valueOf(new_value));
                                textview = findViewById(R.id.server_response);
                                textview.setText("Wykryta aktywność: " + message);
                            }
                        }
                    });

                }
            });
            Log.e("Client", "Connecting task");

            // proba ponownego polaczenia z serwerem
            Boolean is_configured;
            int attempt = 3;

            do {
                is_configured = tcpClient.preRun();
                attempt--;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (!is_configured && attempt > 0);
            if (is_configured) {
                while (!this.isCancelled()) {
                    tcpClient.run();
                }
                tcpClient.postRun();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            // odpowiedz otrzymana od serwera
            Log.d("TCP Server",  values[0]);
        }
    }

    private final Feature.FeatureListener mAccelerationListener = new Feature.FeatureListener() {
        @Override
        // aktualizacja danych z akcelerometru
        public void onUpdate(Feature f,Feature.Sample sample) {

            float x = FeatureAcceleration.getAccX(sample);
            float y = FeatureAcceleration.getAccY(sample);
            float z = FeatureAcceleration.getAccZ(sample);

            synchronized (this) {
                timestampArray.add(String.valueOf(sample.timestamp));

                accDataPacket.add(" " + x + " " + y + " " + z);
                if(timestampArray.size() == 100 && !inProgress.get()) {
                    t.set(true);
                }

                if(accDataPacket.size() == 100 && !inProgress.get()) {
                    a.set(true);
                }
            }
        }
    };

    private final Feature.FeatureListener mGyroscopeListener = new Feature.FeatureListener() {
        @Override
        // aktualizacja danych z zyroskopu
        public void onUpdate(Feature f,Feature.Sample sample) {

        float x = FeatureGyroscope.getGyroX(sample);
        float y = FeatureGyroscope.getGyroY(sample);
        float z = FeatureGyroscope.getGyroZ(sample);

            synchronized (this) {
                gyroDataPacket.add(" " + x + " " + y + " " + z + "\n");
                if (gyroDataPacket.size() == 100 && !inProgress.get()) {
                    g.set(true);
                }
            }
        }
    };

    // utworzenie intentu odpowiadajacego za uruchomienie aktywnosci
    public static Intent getStartIntent(Context c, @NonNull Node node) {
        Intent i = new Intent(c, StatisticsActivity.class);
        i.putExtra(NODE_TAG, node.getTag());
        i.putExtras(NodeContainerFragment.prepareArguments(node));
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        final TextView TextView  = findViewById(R.id.tcp_counter);
        final Button disconnectButton = findViewById(R.id.disconnect_button);
        final Button resetButton = findViewById(R.id.reset_button);

        a = new AtomicBoolean(false);
        g = new AtomicBoolean(false);
        t = new AtomicBoolean(false);

        jogging_time = 0;
        staying_time = 0;
        march_time = 0;
        running_time = 0;
        hop_number = 0;

        inProgress = new AtomicBoolean(false);
        sem = new Semaphore(1);
        packCounter = 0;

        TextView.setText(String.valueOf(packCounter));

        // rozlaczanie z serwerem po wcisnieciu przycisku
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeConnection ();
                connectingTask.cancel(true);
            }
        });

        // resetowanie zawartosci tabeli
        resetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                jogging_time = 0;
                TextView textView = findViewById(R.id.jogging_time);
                textView.setText(String.valueOf(jogging_time));

                staying_time = 0;
                textView = findViewById(R.id.staying_time);
                textView.setText(String.valueOf(staying_time));

                march_time = 0;
                textView = findViewById(R.id.march_time);
                textView.setText(String.valueOf(march_time));

                running_time = 0;
                textView = findViewById(R.id.running_time);
                textView.setText(String.valueOf(running_time));

                hop_number = 0;
                textView = findViewById(R.id.hop_number);
                textView.setText(String.valueOf(hop_number));

                packCounter = 0;
                textView = findViewById(R.id.tcp_counter);
                textView.setText(String.valueOf(packCounter));

                TextView textview = findViewById(R.id.server_response);
                textview.setText(null);
            }
        });


        // "odszukanie" wlasciwego wezla
        String nodeTag = getIntent().getStringExtra(NODE_TAG);
        mNode = Manager.getSharedInstance().getNodeWithTag(nodeTag);

        // utworzenie/odzyskanie NodeContainerFragment
        if (savedInstanceState == null) {
            Intent i = getIntent();
            mNodeContainer = new NodeContainerFragment();
            mNodeContainer.setArguments(i.getExtras());

            getFragmentManager().beginTransaction()
                    .add(mNodeContainer, NODE_FRAGMENT).commit();

        } else {
            mNodeContainer = (NodeContainerFragment) getFragmentManager()
                    .findFragmentByTag(NODE_FRAGMENT);
        }

        // utworzenie list przechowujacych probki z czujnikow
        timestampArray = new ArrayList<>();
        accDataPacket = new ArrayList<>();
        gyroDataPacket = new ArrayList<>();

        // watek zwiazany z wysylaniem danych do serwera
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while(true) {
                        // sprawdzanie liczby zebranych probek
                        if (g.get() && a.get() && t.get() && !inProgress.get()) {
                            packCounter ++;
                            inProgress.set(true);
                            a.set(false);
                            g.set(false);
                            t.set(false);
                            Log.e("Client", "sendToServer");

                            // uaktualnianie liczby wyslanych pakietow w GUI
                            StatisticsActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (tcpClient != null) {
                                        TextView TextView = findViewById(R.id.tcp_counter);
                                        TextView.setText(String.valueOf(packCounter));
                                    }
                                }
                            });

                            // wyslanie pakietu do serwera
                            sendToServer();
                            inProgress.set(false);
                        }
                        sleep(1);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }

    // funkcja wlaczajaca notyfikacje (dane z akcelerometru)
    private void enableAccelerometerNotification() {
        mAccelerator = mNode.getFeature(FeatureAcceleration.class);
        mAccelerator.addFeatureListener(mAccelerationListener);
        mNode.enableNotification(mAccelerator);

    }

    // funkcja wlaczajaca notyfikacje (dane z zyroskopu)
    private void enableGyroscopeNotification() {
        mGyroscope = mNode.getFeature(FeatureGyroscope.class);
        mGyroscope.addFeatureListener(mGyroscopeListener);
        mNode.enableNotification(mGyroscope);
    }

    // listener odpowiedzialny za wlaczenie notyfikacji od wezla
    private Node.NodeStateListener mNodeStatusListener = new Node.NodeStateListener() {
        @Override
        public void onStateChange(final Node node, Node.State newState, Node.State prevState) {
            if (newState == Node.State.Connected) {
                StatisticsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enableAccelerometerNotification();
                        enableGyroscopeNotification();
                    }
                });
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // jesli wezel jest polaczony -> aktywacja notyfikacji
        if (mNode.isConnected()) {
            enableGyroscopeNotification();
            enableAccelerometerNotification();
        }
        // jesli wezel nie jest polaczony -> ustawienie listenera
        else {
            mNode.addNodeStateListener(mNodeStatusListener);
        }

        // polaczenie z serwerem
        connectingTask = new ConnectingTask();
        connectingTask.execute("");

    }

    protected void  onDestroy() {
        super.onDestroy();
    }


    @Override
    protected void onPause() {
        super.onPause();
        // rozlaczenie sie z serwerem podczas przejscia do innej aktywnosci
        closeConnection ();
        connectingTask.cancel(true);
    }

    // funkcja zamykajaca polaczenie z serwerem
    private void closeConnection () {
        // ramka wysylana w celu zakonczenia polaczenia
        String message = "END $";

        Log.e("Client", "Closing connection...");
        if (tcpClient != null) {
            // wyslanie ramki
            tcpClient.sendMessage(message);
        }

        if (tcpClient != null) {
            // rozlaczenie
            new DisconnectTask().execute();
        }

        // wyswietlenie komunikatu o rozlaczeniu w aplikacji
        TextView textview = findViewById(R.id.server_response);
        textview.setText(R.string.disconnect);
    }


    // funkcja formujaca pakiet danych i wysylajaca go do serwera
    private void sendToServer(){

        Log.e("Client", timestampArray.size() + " " + accDataPacket.size() + " " + gyroDataPacket.size());

        // naglowek pakietu
        String message = "Klasyfikacja 1 ";

        // dodanie kolejnych probek danych z czujnikow
        for(int i = 0; i < 100; i++) {
            message += timestampArray.get(0) + accDataPacket.get(0) + gyroDataPacket.get(0);
            timestampArray.remove(0);
            accDataPacket.remove(0);
            gyroDataPacket.remove(0);
        }
        // dodanie znaku konca ramki
        message = message + "$";

        // wysłanie danych do serwera
        if (tcpClient != null) {
            tcpClient.sendMessage(message);
        }

    }
}
