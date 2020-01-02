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

// klasa implementujaca kienta TCP
class TcpClient {

    // adres IP serwera
    private static final String SERVER_IP = "swapdomain.ddns.net";

    // numer portu serwera
    private static final int SERVER_PORT = 5005;

    // wiadomosc wysylana przez serwer do klienta
    private String serverMessage;

    // obiekt nasluchujacy wiadomosci od serwera
    private OnMessageReceived messageListener;

    // true, gdy serwer jest uruchomiony
    private volatile boolean serverRunning = false;

    private PrintWriter bufferOut;
    private BufferedReader bufferIn;
    private Socket socket;

    // konstruktor klasy
    TcpClient(OnMessageReceived listener) {
        messageListener = listener;
    }

    // funkcja wysylajaca wiadomosc klienta do serwera
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


    // funkcja zwracajaca true, gdy udalo sie nawiazac polaczenie z serwerem
    Boolean preRun () {
        serverRunning = true;
        Log.e("TCP Client", "Try to connect...");
        try {
            // pobranie adresu IP serwera
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

            // utworzenie gniazda w celu nawiazania polaczenia z serwerem
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
        try {
            serverMessage = bufferIn.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (serverMessage != null && messageListener != null) {
            // otrzymano wiadomosc od serwera
            messageListener.messageReceived(serverMessage);
        }
    }

    // zamkniecie polaczenia
    void postRun () {
        serverRunning = false;
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        stopClient();
    }

    // funkcja konczaca polaczenie
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

    // metoda messageReceived(String message) musi byc zaimplementowana w StatisticsActivity, w doInBackground
    public interface OnMessageReceived {
        void messageReceived(String message);
    }

}