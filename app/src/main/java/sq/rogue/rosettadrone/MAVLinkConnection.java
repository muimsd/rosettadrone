package sq.rogue.rosettadrone;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MAVLinkConnection {
    DatagramSocket socket;
    private InetAddress targetAddr;
    private int targetPort;
    MainActivity.GCSCommunicatorAsyncTask.Listener listener;
    private final String TAG = this.getClass().getSimpleName();

    public MAVLinkConnection(String host, int port) {
        try {
            Log.e(TAG, "GCS link to " + host + ":" + port + " (unconnected RX, accept any source)");
            socket = new DatagramSocket();
            // NOTE: deliberately do NOT socket.connect(host, port).
            // A *connected* UDP socket only delivers datagrams whose source
            // exactly matches the peer (host:port). The GCS reliably receives
            // our telemetry, but may send its commands back from a *different*
            // source IP (multi-interface / NAT / the OS choosing another local
            // address for the route). With connect(), the OS silently drops
            // those command datagrams — "wire sees the packets, the socket
            // doesn't" — so MISSION_COUNT/commands never reach the parser.
            // Instead: send to an explicit target, and receive() from ANY source.
            targetAddr = InetAddress.getByName(host);
            targetPort = port;
            //socket.setSoTimeout(10);

        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void send(byte[] bytes) throws IOException {
        socket.send(new DatagramPacket(bytes, bytes.length, targetAddr, targetPort));
    }

    public void close() {
        listener.close = true;
        listener.interrupt();
        try {
            Log.i(TAG, "Waiting to close listener...");
            listener.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        socket.close();
    }

    public void listen(MainActivity.GCSCommunicatorAsyncTask.Listener listener) {
        this.listener = listener;
        listener.start();
    }
}
