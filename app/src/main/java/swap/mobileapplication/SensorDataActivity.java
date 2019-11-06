package swap.mobileapplication;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.st.BlueSTSDK.Feature;
import com.st.BlueSTSDK.Features.FeatureAcceleration;
import com.st.BlueSTSDK.Features.FeatureGyroscope;
import com.st.BlueSTSDK.Manager;
import com.st.BlueSTSDK.Node;

//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

// aktywność wyświetlająca dane z akcelerometru i żyroskopu
public class SensorDataActivity extends AppCompatActivity {

    private TcpClient tcpClient;

    private volatile AtomicBoolean a;
    private volatile AtomicBoolean g;
    private volatile AtomicBoolean t;

    private volatile AtomicBoolean inProgress;

    private FeatureAcceleration  mAccelerator;
    private FeatureGyroscope  mGyroscope;
    Semaphore sem;
    volatile int packCounter;

    // węzeł transmitujący dane
    private Node mNode;

    ArrayList<String> timestampArray;
    ArrayList<String> accDataPacket;
    ArrayList<String> gyroDataPacket;



//    private PrintWriter pw1;
//    private PrintWriter pw2;

    // fragment zarządzający połączeniem z węzłem
    // pozwala uniknąć ponownego połączenia za każdym razem gdy aktywność jest wznawiana
    private NodeContainerFragment mNodeContainer;

    // tag używany w celu otrzymania NodeContainerFragment
    private final static String NODE_FRAGMENT = SensorDataActivity.class.getCanonicalName() + "" +
            ".NODE_FRAGMENT";

    // tag używany do przechowywania id węzła
    private final static String NODE_TAG = SensorDataActivity.class.getCanonicalName() + "" +
            ".NODE_TAG";

    public class ConnectingTask extends AsyncTask<String, String, TcpClient> {

        // metoda uruchamiana w oddzielnym wątku
        @Override
        protected TcpClient doInBackground(String... message) {

            // utworzenie klienta TCP
            tcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
                @Override
                public void messageReceived(String message) {
                    // wywołanie metody onProgressUpdate
                    publishProgress(message);
                }
            });
            tcpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            // odpowiedź otrzymana od serwera
            Log.d("TCP Server",  values[0]);
        }
    }

//    private synchronized void setA (boolean value) {
//        a = value;
//    }
//    private synchronized void setG (boolean value) {
//        g = value;
//    }
//    private synchronized void setInProgress (boolean value) {
//        inProgress = value;
//    }

    private final Feature.FeatureListener mAccelerationListener = new Feature.FeatureListener() {
        @Override
        // aktualizacja danych z akcelerometru
        public void onUpdate(Feature f,Feature.Sample sample) {

            float x = FeatureAcceleration.getAccX(sample);
            float y = FeatureAcceleration.getAccY(sample);
            float z = FeatureAcceleration.getAccZ(sample);

//            final String dataToDisplay = "Akcelerometr\n" + "Timestamp: " + sample.timestamp + "\nX: " + x + "\nY: " + y + "\nZ: " + z;
//
//            SensorDataActivity.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    TextView TextView  = findViewById(R.id.accelerator);
//                    TextView.setText(dataToDisplay);
//                }
//            });

//            // zapis danych z akcelerometru do pliku
//            try {
//                pw1 = new PrintWriter(new FileWriter(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "accelerometer_data_2.txt"), true));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            pw1.write(sample.timestamp + ",  " + x + ", " + y + ", " + z + "\n");
//            pw1.flush();

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
//            System.out.println(timestampArray.size() + " ...");
//            System.out.println(accDataPacket.size() + " ...a");

//            System.out.println("T: " + (timestampArray.size() == 100 && !inProgress) + " " + !inProgress);
//            System.out.println("RK: " + (gyroDataPacket.size() == 100) + " " + !inProgress.get());


//            System.out.println("A: " + (accDataPacket.size() == 100 && !inProgress) + " " + !inProgress);
//            if(accDataPacket.size() == 100 && !inProgress) {
//                a = true;
//            }


        }
    };

    private final Feature.FeatureListener mGyroscopeListener = new Feature.FeatureListener() {
        @Override
        // aktualizacja danych z żyroskopu
        public void onUpdate(Feature f,Feature.Sample sample) {

            float x = FeatureGyroscope.getGyroX(sample);
            float y = FeatureGyroscope.getGyroY(sample);
            float z = FeatureGyroscope.getGyroZ(sample);

//            final String dataToDisplay = "Żyroskop\n" + "Timestamp: " + sample.timestamp + "\nX: " + x + "\nY: " + y + "\nZ: " + z;
//            SensorDataActivity.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    TextView TextView  = findViewById(R.id.gyroscope);
//                    TextView.setText(dataToDisplay);
//                }
//            });

//            // zapis danych z żyroskopu do pliku
//            try {
//                pw2 = new PrintWriter(new FileWriter(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "gyroscope_data.txt"), true));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            pw2.write(sample.timestamp + ",  " + x + ", " + y + ", " + z + "\n");
//            pw2.flush();

            synchronized (this) {
                gyroDataPacket.add(" " + x + " " + y + " " + z + "\n");

//            System.out.println(gyroDataPacket.size() + " ...g");

                //           System.out.println("G: " + (gyroDataPacket.size() == 100 && !inProgress) + " " + !inProgress);

                if (gyroDataPacket.size() == 100 && !inProgress.get()) {
                    g.set(true);
                }
            }

//            System.out.println("........................... " + t + " " + a + " " + g + " " + !inProgress.get());
//            if (a && g && t && !inProgress) {
//                inProgress = true;
//                Log.e("Client", "sendToServer");
//                sendToServer();
//                inProgress = false;
//            }
        }
    };

    // utworzenie intentu odpowiadającego za uruchomienie aktywności
    public static Intent getStartIntent(Context c, @NonNull Node node) {
        Intent i = new Intent(c, SensorDataActivity.class);
        i.putExtra(NODE_TAG, node.getTag());
        i.putExtras(NodeContainerFragment.prepareArguments(node));
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acceleration_menu);
        sem = new Semaphore(1);

        a = new AtomicBoolean(false);
        g = new AtomicBoolean(false);
        t = new AtomicBoolean(false);
        inProgress = new AtomicBoolean(false);

        packCounter = 0;
        TextView TextView  = findViewById(R.id.gyroscope);
        TextView.setText(String.valueOf(packCounter));

//        a = false;
//        g = false;
//        t = false;
//        inProgress = false;

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
        new ConnectingTask().execute("");

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
                            SensorDataActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                 public void run() {
                                    TextView TextView  = findViewById(R.id.gyroscope);
                                    TextView.setText(String.valueOf(packCounter));
                                }
                             });
                            sendToServer();
                            SensorDataActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView server_ans  = findViewById(R.id.server_ans);
                                    server_ans.setText(String.valueOf(tcpClient.serverMessage));
                                }
                            });
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
                SensorDataActivity.this.runOnUiThread(new Runnable() {
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
        if (mNode.isConnected()) {
            enableGyroscopeNotification();
            enableAccelerometerNotification();
        } else {
            mNode.addNodeStateListener(mNodeStatusListener);
        }
    }

    protected void  onDestroy() {
        super.onDestroy();
//       pw1.close();
//       pw2.close();
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

//        timestampArray.clear();
//        accDataPacket.clear();
//        gyroDataPacket.clear();

    }

}