package swap.mobileapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import com.st.BlueSTSDK.Node;
import com.st.BlueSTSDK.Manager;
import android.widget.AdapterView;
import android.content.Intent;

import com.st.BlueSTSDK.Utils.NodeScanActivity;

// aktywność pokazującą listę urządzeń wspieranych przez BlueSTSDK
public class ScanActivity extends NodeScanActivity implements AbsListView.OnItemClickListener {

    // czas skanowania w poszukiwaniu nowego węzła (10 s)
    private final static int SCAN_TIME_MS = 10 * 1000;

    // adapter budujący GUI dla każdego wykrytego węzła - przechowuje wykryte węzły
    private NodeArrayAdapter mAdapter;

    // listener nasłuchujący wykrycia węzła
    private Manager.ManagerListener mUpdateDiscoverGui = new Manager.ManagerListener() {
        // zatrzymanie skanowania w celu uaktualnienia informacji w GUI
        @Override
        public void onDiscoveryChange(Manager m, boolean enabled) {
            if (!enabled)
                ScanActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("1", "onDiscoveryChange");
                        stopNodeDiscovery();
                    }
                });
        }

        // metoda wywoływana w momencie wykrycia nowego węzła
        @Override
        public void onNodeDiscovered(Manager m, Node node) {
            Log.e("2", "onNodeDiscovered");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        AbsListView listView = findViewById(R.id.nodeListView);

        // utworzenie adaptera i powiązanie go z listą, w której będzie użyty do reprezentacji danych
        mAdapter = new NodeArrayAdapter(this);
        listView.setAdapter(mAdapter);

        // rejestracja wybrania węzła z listy
        listView.setOnItemClickListener(this);

        // dodanie wszystkich wykrytych węzłów do adaptera
        mAdapter.addAll(mManager.getNodes());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e("3", "onStart");
    }

    protected void onResume() {
        super.onResume();
        Log.e("4", "onResume");

        mManager.addListener(mUpdateDiscoverGui);

        // rozłączenie wszystkich wykrytych węzłów
        mAdapter.disconnectAllNodes();

        // dodanie listenera dla nowych węzłów
        mManager.addListener(mAdapter);

        resetNodeList();
        startNodeDiscovery();
    }

    // zatrzymanie skanowania i usunięcie listenerów związanych z maganerem
    @Override
    protected void onStop() {
        if (mManager.isDiscovering())
            mManager.stopDiscovery();
        Log.e("5", "onStop");
        mManager.removeListener(mUpdateDiscoverGui);
        mManager.removeListener(mAdapter);
        super.onStop();
    }

    // wyczyszczenie listy węzłów powiązanych z managerem i adapterem
    private void resetNodeList() {
        mManager.resetDiscovery();
        mAdapter.clear();
        // urządzenia sparowane będą nadal widoczne
        mAdapter.addAll(mManager.getNodes());
    }

    // rozpoczęcie skanowania + zapytanie o aktualizację GUI
    public void startNodeDiscovery() {
        super.startNodeDiscovery(SCAN_TIME_MS);
        invalidateOptionsMenu();
    }

    // zatrzymanie skanowania + zapytanie o aktualizację GUI
    public void stopNodeDiscovery() {
        super.stopNodeDiscovery();
        invalidateOptionsMenu();
    }

    // wybór węzła -> rozpoczęcie połączenia
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Node n = mAdapter.getItem(position);
        if(n == null)
            return;

        // przejście do aktywności wyświetlającej dane z czujników
        Intent i = SensorDataActivity.getStartIntent(this, n);
        startActivity(i);
    }

}
