package swap.mobileapplication;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

class TcpClient {

    // adres IP i numer portu serwera
    //private static final String SERVER_IP = "83.20.119.105";
    private static final String SERVER_IP = "swapdomain.ddns.net";
    private static final int SERVER_PORT = 5005;

    // wiadomość wysyłana przez serwer do klienta
    public String serverMessage;

    // obiekt nasłuchujący wiadomości od serwera
    private OnMessageReceived messageListener = null;

    // true, gdy serwer jest uruchomiony
    private boolean serverRunning = false;

    private PrintWriter bufferOut;
    private BufferedReader bufferIn;

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

    void run() {
        serverRunning = true;
        try {
            // pobranie adresu IP serwera
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

            // utworzenie gniazda w celu nawiązania połączenia z serwerem
            Socket socket = new Socket(serverAddr, SERVER_PORT);
            Log.e("TCP Client", "Connecting...");

            try {

                bufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                bufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // nasłuchiwanie przyjścia wiadomości zwrotnej
                while (serverRunning) {

                    serverMessage = bufferIn.readLine();

                    if (serverMessage != null && messageListener != null) {
                        // otrzymano wiadomość od serwera
                        messageListener.messageReceived(serverMessage);
                    }
                }

            } catch (Exception e) {
                Log.e("TCP Client", "Error", e);
            } finally {
                socket.close();
            }

        } catch (Exception e) {
            Log.e("TCP Client", "Error", e);
        }

    }

    // metoda messageReceived(String message) musi być zaimplementowana w SensorDataActivity, w doInBackground
    public interface OnMessageReceived {
        void messageReceived(String message);
    }

}