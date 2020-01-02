package swap.mobileapplication;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

class TcpClient {

    // adres IP i numer portu serwera
//    private static final String SERVER_IP = "192.168.0.120";
    private static final String SERVER_IP = "swapdomain.ddns.net";
    private static final int SERVER_PORT = 5005;

    // wiadomość wysyłana przez serwer do klienta
    public String serverMessage;

    // obiekt nasłuchujący wiadomości od serwera
    private OnMessageReceived messageListener = null;

    // true, gdy serwer jest uruchomiony
    private volatile boolean serverRunning = false;

    private PrintWriter bufferOut;
    private BufferedReader bufferIn;
    private Socket socket;

    // konstruktor klasy
    TcpClient(OnMessageReceived listener) {
        messageListener = listener;
    }

    // funkcja wysyłająca wiadomość klienta do serwera
    void sendMessage(final String message) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (bufferOut != null) {
                    Log.d("TCP Client", "Sent: " + message);
                    bufferOut.println(message);
                    bufferOut.flush();
                }
            }
        };

        Thread sendingThread = new Thread(runnable);
        sendingThread.start();
    }


    // zwróć true, jeśli sukces, w przeciwnym wypadku false
    Boolean preRun () {
        serverRunning = true;
        Log.e("TCP Client", "Connecting1...");
        try {
            // pobranie adresu IP serwera
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

            // utworzenie gniazda w celu nawiązania połączenia z serwerem
            socket = new Socket(serverAddr, SERVER_PORT);
            Log.e("TCP Client", "Connecting...");

            bufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            bufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return true;
        }  catch (Exception e) {
            Log.e("TCP Client", "Error", e);
            return false;
        }
    }


    void run() {
        Log.e("TCP Client", "JESTEM!!...");
        try {
            serverMessage = bufferIn.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (serverMessage != null && messageListener != null) {
            // otrzymano wiadomość od serwera
            messageListener.messageReceived(serverMessage);
        }
    }
    void postRun () {

        serverRunning = false;
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        stopClient();
    }

    // funkcja do zamykania połączenia
    public void stopClient() {
        serverRunning = false;

        if (bufferOut != null) {
            bufferOut.flush();
            bufferOut.close();
        }

        bufferIn = null;
        bufferOut = null;
        messageListener = null;
        serverMessage = null;
    }

    // metoda messageReceived(String message) musi być zaimplementowana w SensorDataActivity, w doInBackground
    public interface OnMessageReceived {
        void messageReceived(String message);
    }

}