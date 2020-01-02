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
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class StatisticsActivity extends AppCompatActivity {

    private TcpClient tcpClient;

    private volatile AtomicBoolean a;
    private volatile AtomicBoolean g;
    private volatile AtomicBoolean t;

    private volatile AtomicBoolean inProgress;

    private FeatureAcceleration mAccelerator;
    private FeatureGyroscope mGyroscope;
    Semaphore sem;
    volatile int packCounter;

    // węzeł transmitujący dane
    private Node mNode;

    ArrayList<String> timestampArray;
    ArrayList<String> accDataPacket;
    ArrayList<String> gyroDataPacket;

    Integer trucht_czas;
    Integer stanie_czas;
    Integer marsz_czas;
    Integer bieg_czas;
    Integer skakanie_czas;

    int id_to_update;
    int new_value;

    ConnectingTask connectingTask;


    // fragment zarządzający połączeniem z węzłem
    // pozwala uniknąć ponownego połączenia za każdym razem gdy aktywność jest wznawiana
    private NodeContainerFragment mNodeContainer;

    // tag używany w celu otrzymania NodeContainerFragment
    private final static String NODE_FRAGMENT = StatisticsActivity.class.getCanonicalName() + "" +
            ".NODE_FRAGMENT";

    // tag używany do przechowywania id węzła
    private final static String NODE_TAG = StatisticsActivity.class.getCanonicalName() + "" +
            ".NODE_TAG";



    /**
     * Disconnects using a background task to avoid doing long/network operations on the UI thread
     */
    public class DisconnectTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            // disconnect
            tcpClient.stopClient();
            tcpClient = null;

            return null;
        }
    }



    public class ConnectingTask extends AsyncTask<String, String, TcpClient> {

        // metoda uruchamiana w oddzielnym wątku
        @Override
        protected TcpClient doInBackground(String... message) {

            // utworzenie klienta TCP
            tcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
                @Override
                public void messageReceived(final String message) {
                    // wywołanie metody onProgressUpdate
//                    publishProgress(message);
//                    Log.e("DUPA1", message);
                    new_value = 0;
                    id_to_update = 0;
                    Log.e("Client", message);

                    if (message.equals("Bieg")) {
                        bieg_czas++;
                        new_value = bieg_czas;
                        id_to_update = R.id.bieg;
                    } else if (message.equals("Trucht")) {
                        trucht_czas++;
                        new_value = trucht_czas;
                        id_to_update = R.id.trucht;
                    } else if (message.equals("Stanie")) {
                        stanie_czas++;
                        new_value = stanie_czas;
                        id_to_update = R.id.stanie;
                    } else if (message.equals("Marsz")) {
                        marsz_czas++;
                        new_value = marsz_czas;
                        id_to_update = R.id.marsz;
                    } else if (message.equals("Skok")) {
                        skakanie_czas++;
                        new_value = skakanie_czas;
                        id_to_update = R.id.skakanie;
                    }

                    if (message.equals("END")){
                        System.out.println("xxxxxxxxxxxxxxxxxxx");
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
            Log.e("Client", "connecting task");

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
            } while (is_configured == false && attempt > 0);
            if (is_configured) {
                while (this.isCancelled() == false) {
                    tcpClient.run();
                }
                tcpClient.postRun();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            // odpowiedź otrzymana od serwera
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
        // aktualizacja danych z żyroskopu
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

    // utworzenie intentu odpowiadającego za uruchomienie aktywności
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

        sem = new Semaphore(1);

        a = new AtomicBoolean(false);
        g = new AtomicBoolean(false);
        t = new AtomicBoolean(false);

        trucht_czas = new Integer(0);
        stanie_czas = new Integer(0);
        marsz_czas = new Integer(0);
        bieg_czas = new Integer(0);
        skakanie_czas = new Integer(0);

        inProgress = new AtomicBoolean(false);

        packCounter = 0;
        final TextView TextView  = findViewById(R.id.tcp_counter);
        TextView.setText(String.valueOf(packCounter));

//        a = false;
//        g = false;
//        t = false;
//        inProgress = false;

        final Button button = findViewById(R.id.button2);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeConnection ();
                connectingTask.cancel(true);
            }
        });

        final Button button2 = findViewById(R.id.reset_button);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                trucht_czas = 0;
                TextView textView = findViewById(R.id.trucht);
                textView.setText(String.valueOf(trucht_czas));

                stanie_czas = 0;
                textView = findViewById(R.id.stanie);
                textView.setText(String.valueOf(stanie_czas));


                marsz_czas = 0;
                textView = findViewById(R.id.marsz);
                textView.setText(String.valueOf(marsz_czas));

                bieg_czas = 0;
                textView = findViewById(R.id.bieg);
                textView.setText(String.valueOf(bieg_czas));

                skakanie_czas = 0;
                textView = findViewById(R.id.skakanie);
                textView.setText(String.valueOf(skakanie_czas));

                packCounter = 0;
                textView = findViewById(R.id.tcp_counter);
                textView.setText(String.valueOf(packCounter));

                TextView textview = findViewById(R.id.server_response);
                textview.setText(null);
            }
        });





        // "odszukanie" właściwego węzła
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

        timestampArray = new ArrayList<>();
        accDataPacket = new ArrayList<>();
        gyroDataPacket = new ArrayList<>();

        // połączenie z serwerem
//        new ConnectingTask().execute("");

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while(true) {
//                        System.out.println("RK2: g =" + g.get() + " t=" + t.get() + " !inprogress=" + !inProgress.get());
                        if (g.get() && a.get() && t.get() && !inProgress.get()) {
                            packCounter ++;
                            inProgress.set(true);
                            a.set(false);
                            g.set(false);
                            t.set(false);
                            Log.e("Client", "sendToServer");
                            StatisticsActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (tcpClient != null) {
                                        TextView TextView = findViewById(R.id.tcp_counter);
                                        TextView.setText(String.valueOf(packCounter));
                                    }
                                }
                            });
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

    private void enableAccelerometerNotification() {
        mAccelerator = mNode.getFeature(FeatureAcceleration.class);
        mAccelerator.addFeatureListener(mAccelerationListener);
        mNode.enableNotification(mAccelerator);

    }

    private void enableGyroscopeNotification() {
        mGyroscope = mNode.getFeature(FeatureGyroscope.class);
        mGyroscope.addFeatureListener(mGyroscopeListener);
        mNode.enableNotification(mGyroscope);
    }

    // listener odpowiedzialny za włączenie notyfikacji od węzła
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

    // jeśli węzeł jest połączony -> aktywacja notyfikacji
    // jeśli nie -> ustawienie listenera
    @Override
    protected void onResume() {
        super.onResume();
        Log.e("Client", "Resume...");
        if (mNode.isConnected()) {
            enableGyroscopeNotification();
            enableAccelerometerNotification();
        } else {
            mNode.addNodeStateListener(mNodeStatusListener);
        }

        // połączenie z serwerem

        connectingTask = new ConnectingTask();
        connectingTask.execute("");

    }

    protected void  onDestroy() {
        super.onDestroy();
//       pw1.close();
//       pw2.close();
    }

    @Override
    protected void onPause() {
        super.onPause();

        closeConnection ();
        connectingTask.cancel(true);
    }

    private void closeConnection () {
        String message = "END $";
        // wysłanie danych do serwera

        Log.e("Client", "Close connection...");
        if (tcpClient != null) {
            tcpClient.sendMessage(message);
        }

        if (tcpClient != null) {
            // disconnect
            new DisconnectTask().execute();
        }


        TextView textview = findViewById(R.id.server_response);
        textview.setText("Brak połączenia z serwerem!");
    }


    private void sendToServer(){

        Log.e("Client", timestampArray.size() + " " + accDataPacket.size() + " " + gyroDataPacket.size());

        String message = "Klasyfikacja 1 ";

        for(int i = 0; i < 100; i++) {
            message = message + timestampArray.get(0) + accDataPacket.get(0) + gyroDataPacket.get(0);
            timestampArray.remove(0);
            accDataPacket.remove(0);
            gyroDataPacket.remove(0);
        }
        message = message + "$";
        // wysłanie danych do serwera
        if (tcpClient != null) {
            tcpClient.sendMessage(message);
        }
//        else {
//            new ConnectingTask().execute("");
//        }

//        timestampArray.clear();
//        accDataPacket.clear();
//        gyroDataPacket.clear();

    }
}
