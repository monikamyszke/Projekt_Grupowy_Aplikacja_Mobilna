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

// aktywnosc pokazujaca liste urzadzen wspieranych przez BlueSTSDK
public class ScanActivity extends NodeScanActivity implements AbsListView.OnItemClickListener {

    // czas skanowania w poszukiwaniu nowego węzła (10 s)
    private final static int SCAN_TIME_MS = 10 * 1000;

    // adapter budujacy GUI dla kazdego wykrytego wezła - przechowuje wykryte wezly
    private NodeArrayAdapter mAdapter;

    // listener nasluchujacy wykrycia wezla
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

        // metoda wywolywana w momencie wykrycia nowego wezla
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

        // utworzenie adaptera i powiazanie go z lista, w ktorej bedzie uzyty do reprezentacji danych
        mAdapter = new NodeArrayAdapter(this);
        listView.setAdapter(mAdapter);

        // rejestracja wybrania wezla z listy
        listView.setOnItemClickListener(this);

        // dodanie wszystkich wykrytych wezlow do adaptera
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

        // rozlaczenie wszystkich wykrytych wezlow
        mAdapter.disconnectAllNodes();

        // dodanie listenera dla nowych wezlow
        mManager.addListener(mAdapter);

        resetNodeList();
        startNodeDiscovery();
    }

    // zatrzymanie skanowania i usuniecie listenerow zwiazanych z maganerem
    @Override
    protected void onStop() {
        if (mManager.isDiscovering())
            mManager.stopDiscovery();
        Log.e("5", "onStop");
        mManager.removeListener(mUpdateDiscoverGui);
        mManager.removeListener(mAdapter);
        super.onStop();
    }

    // wyczyszczenie listy wezlow powiazanych z managerem i adapterem
    private void resetNodeList() {
        mManager.resetDiscovery();
        mAdapter.clear();
        // urzadzenia sparowane beda nadal widoczne
        mAdapter.addAll(mManager.getNodes());
    }

    // rozpoczecie skanowania + zapytanie o aktualizacje GUI
    public void startNodeDiscovery() {
        super.startNodeDiscovery(SCAN_TIME_MS);
        invalidateOptionsMenu();
    }

    // zatrzymanie skanowania + zapytanie o aktualizacje GUI
    public void stopNodeDiscovery() {
        super.stopNodeDiscovery();
        invalidateOptionsMenu();
    }

    // wybor węzła -> inicjacja polaczenia
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Node n = mAdapter.getItem(position);
        if(n == null)
            return;

        // przejscie do aktywnosci wizualizujacej sklasyfikowane dane
        Intent i = StatisticsActivity.getStartIntent(this, n);
        startActivity(i);
    }

}
