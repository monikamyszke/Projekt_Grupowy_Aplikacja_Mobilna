package swap.mobileapplication;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.st.BlueSTSDK.Manager;
import com.st.BlueSTSDK.Node;

// klasa wykorzystywana do przechowywania polaczonych wezlow
public class NodeContainerFragment extends Fragment {

    final static String NODE_TAG = NodeContainerFragment.class.getCanonicalName()+".NODE_TAG";

    private boolean userAskToKeepConnection = false;

    // okienko (dialog) wyswietlane podczas czekania na polaczenie z wezlem
    private ProgressDialog mConnectionWait;

    private Node mNode = null;

    // listener zwiazany z dialogiem
    private Node.NodeStateListener mNodeStateListener = new Node.NodeStateListener() {
        @Override
        public void onStateChange(@NonNull final Node node, @NonNull Node.State newState, @NonNull Node.State prevState) {
            final Activity activity = NodeContainerFragment.this.getActivity();
            // ukrycie dialogu po polaczeniu
            if ((newState == Node.State.Connected) && activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // zamkniecie dialogu
                        mConnectionWait.dismiss();
                        mConnectionWait  = null;
                    }
                });
                // blad
            } else if ((newState == Node.State.Unreachable ||
                    newState == Node.State.Dead ||
                    newState == Node.State.Lost) && activity != null) {
                final String msg = null;
                // obsluga bledow nie jest implementowana
                switch (newState) {
                    case Dead:
                        break;
                    case Unreachable:
                        break;
                    case Lost:
                    default:
                        break;
                }

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(mConnectionWait == null) {
                            setUpProgressDialog();
                            mConnectionWait.show();
                        }
                        else if (!mConnectionWait.isShowing())
                            mConnectionWait.show();
                        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                        mNode.connect(getActivity());
                    }
                });
            }

        }
    };

    // przygotowanie argumentow, ktore podaje sie do fragmentu
    public static Bundle prepareArguments(Node n) {
        Bundle args = new Bundle();
        args.putString(NODE_TAG, n.getTag());
        return args;
    }

    // przygotowanie dialogu
    private void setUpProgressDialog(){
        mConnectionWait = new ProgressDialog(getActivity(),ProgressDialog.STYLE_SPINNER);
        mConnectionWait.setTitle("Trwa łączenie...");
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        String nodeTag = getArguments().getString(NODE_TAG);
        mNode = Manager.getSharedInstance().getNodeWithTag(nodeTag);
        if(mNode != null)
            setUpProgressDialog();
    }

    @Override
    public void onResume(){
        super.onResume();
        if (mNode != null && !mNode.isConnected()) {
            mConnectionWait.show();
            mNode.addNodeStateListener(mNodeStateListener);
            mNode.connect(getActivity());
        }
    }

    @Override
    public void onPause(){
        if(mConnectionWait != null && mConnectionWait.isShowing()){
            mConnectionWait.dismiss();
        }

        super.onPause();
    }

    // metoda niewykorzystywana
    public void keepConnectionOpen(boolean doIt){
        userAskToKeepConnection = doIt;
    }

    @Override
    public void onDestroy(){

        if(mNode != null && mNode.isConnected()){
            if(!userAskToKeepConnection) {
                mNode.removeNodeStateListener(mNodeStateListener);
                mNode.disconnect();
            }
        }

        super.onDestroy();
    }

}

