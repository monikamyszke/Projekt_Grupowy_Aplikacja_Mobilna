package swap.mobileapplication;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.st.BlueSTSDK.Manager;
import com.st.BlueSTSDK.Node;

public class NodeArrayAdapter extends ArrayAdapter<Node> implements Manager.ManagerListener {

    // aktywność do której przywiązany jest adapter (ScanActivity)
    private Activity mActivity;

    NodeArrayAdapter(Activity context) {
        super(context, R.layout.node_view_item);
        mActivity = context;
    }

    // metoda niewykorzystywana
    @Override
    public void onDiscoveryChange(Manager m, boolean enabled) {
    }

    // dodanie wykrytego węzła do adaptera
    @Override
    public void onNodeDiscovered(Manager m, final Node node) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                add(node);
            }
        });
    }

    // rozłączenie węzłów związanych z adapterem
    void disconnectAllNodes() {
        for (int i = 0; i < getCount(); i++) {
            Node n = getItem(i);
            if (n != null && n.isConnected())
                n.disconnect();
        }
    }

    // utworzenie view opisującego pojedynczy węzeł
    @NonNull
    @Override
    public View getView(int position, View v, @NonNull ViewGroup parent) {

        ViewHolderItem viewHolder;

        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.node_view_item, parent, false);
            viewHolder = new ViewHolderItem();
            viewHolder.sensorName = v.findViewById(R.id.nodeName);
            viewHolder.sensorTag = v.findViewById(R.id.nodeTag);
            v.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolderItem) v.getTag();
        }

        Node sensor = getItem(position);

        viewHolder.sensorName.setText(sensor.getName());
        viewHolder.sensorTag.setText(sensor.getTag());

        return v;
    }

    private static class ViewHolderItem {
        TextView sensorName;
        TextView sensorTag;
    }

}

