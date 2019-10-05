package swap.mobileapplication;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.st.BlueSTSDK.Feature;
import com.st.BlueSTSDK.Features.FeatureAcceleration;
import com.st.BlueSTSDK.Features.FeatureGyroscope;
import com.st.BlueSTSDK.Manager;
import com.st.BlueSTSDK.Node;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

// aktywność wyświetlająca dane z akcelerometru i żyroskopu
public class AccelerationActivity extends AppCompatActivity {

    private TcpClient tcpClient;

    private FeatureAcceleration  mAccelerator;
    private FeatureGyroscope  mGyroscope;

    // węzeł transmitujący dane
    private Node mNode;

    private PrintWriter pw1;
    private PrintWriter pw2;

    // fragment zarządzający połączeniem z węzłem
    // pozwala uniknąć ponownego połączenia za każdym razem gdy aktywność jest wznawiana
    private NodeContainerFragment mNodeContainer;

    // tag używany w celu otrzymania NodeContainerFragment
    private final static String NODE_FRAGMENT = AccelerationActivity.class.getCanonicalName() + "" +
            ".NODE_FRAGMENT";

    // tag używany do przechowywania id węzła
    private final static String NODE_TAG = AccelerationActivity.class.getCanonicalName() + "" +
            ".NODE_TAG";

    public class ConnectingTask extends AsyncTask<String, String, TcpClient> {

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
            // Odpowiedź otrzymana od serwera
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

            final String dataToDisplay = "Akcelerometr\n" + "Timestamp: " + sample.timestamp + "\nX: " + x + "\nY: " + y + "\nZ: " + z;

            AccelerationActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView TextView  = findViewById(R.id.accelerator);
                    TextView.setText(dataToDisplay);
                }
            });

            // zapis danych z akcelerometru do pliku
            try {
                pw1 = new PrintWriter(new FileWriter(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "accelerometer_data.txt"), true));
            } catch (IOException e) {
                e.printStackTrace();
            }
            pw1.write(x + ", " + y + ", " + z + "\n");

            // wysłanie danych do serwera
            if (tcpClient != null) {
                tcpClient.sendMessage(x + ", " + y + ", " + z + "\n");
            }

            pw1.flush();
    }
    };

    private final Feature.FeatureListener mGyroscopeListener = new Feature.FeatureListener() {
        @Override
        // aktualizacja danych z żyroskopu
        public void onUpdate(Feature f,Feature.Sample sample) {

            float x = FeatureGyroscope.getGyroX(sample);
            float y = FeatureGyroscope.getGyroY(sample);
            float z = FeatureGyroscope.getGyroZ(sample);

            final String dataToDisplay = "Żyroskop\n" + "Timestamp: " + sample.timestamp + "\nX: " + x + "\nY: " + y + "\nZ: " + z;
            AccelerationActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView TextView  = findViewById(R.id.gyroscope);
                    TextView.setText(dataToDisplay);
                }
            });

            // zapis danych z żyroskopu do pliku
            try {
                pw2 = new PrintWriter(new FileWriter(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "gyroscope_data.txt"), true));
            } catch (IOException e) {
                e.printStackTrace();
            }
            pw2.write(x + ", " + y + ", " + z + "\n");
            pw2.flush();

        }
    };

    // utworzenie intentu odpowiadającego za uruchomienie aktywności
    public static Intent getStartIntent(Context c, @NonNull Node node) {
        Intent i = new Intent(c, AccelerationActivity.class);
        i.putExtra(NODE_TAG, node.getTag());
        i.putExtras(NodeContainerFragment.prepareArguments(node));
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acceleration_menu);

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

        // połączenie z serwerem
        new ConnectingTask().execute("");
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
                AccelerationActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enableAccelerometerNotification();
                        enableGyroscopeNotification();
                    }
                });
            }
        }
    };

    // jeśli węzeł jest połączony -> aktywacja GUI
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
       pw1.close();
       pw2.close();
    }

}