package com.github.dddpaul.netcat;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import static com.github.dddpaul.netcat.NetCater.State.CONNECTED;
import static com.github.dddpaul.netcat.NetCater.State.IDLE;

public class UdpNetCat extends NetCat
{
    private final String CLASS_NAME = getClass().getSimpleName();

    private NetCatTask task;
    private DatagramSocket socket;

    public UdpNetCat( NetCatListener listener )
    {
        super( listener );
    }

    @Override
    public void cancel()
    {
        if( task != null ) {
            task.cancel( false );
        }
    }

    @Override
    public void execute( String... params )
    {
        task = new NetCatTask();
        task.execute( params );
    }

    @Override
    public void executeParallel( String... params )
    {
        task = new NetCatTask();
        task.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, params );
    }

    @Override
    public boolean isListening()
    {
        return socket != null && socket.isBound();
    }

    @Override
    public boolean isConnected()
    {
        return socket != null && socket.isConnected();
    }

    public class NetCatTask extends Task
    {
        @Override
        protected Result doInBackground( String... params )
        {
            Op op = Op.valueOf( params[0] );
            Result result = new Result( op );
            try {
                Log.d( CLASS_NAME, String.format( "Executing %s operation", op ) );
                int port;
                switch( op ) {
                    case CONNECT:
                        Proto proto = Proto.valueOf( params[1] );
                        String host = params[2];
                        port = Integer.parseInt( params[3] );
                        Log.d( CLASS_NAME, String.format( "Connecting to %s:%d (%s)", host, port, proto ) );
                        socket = new DatagramSocket();
                        socket.connect( new InetSocketAddress( host, port ) );
                        publishProgress( CONNECTED.toString() );
                        result.object = socket;
                        break;
                    case LISTEN:
                        proto = Proto.valueOf( params[1] );
                        port = Integer.parseInt( params[2] );
                        Log.d( CLASS_NAME, String.format( "Listening on %d (%s)", port, proto ) );
                        socket = new DatagramSocket( port );
                        result.object = socket;
                        break;
                    case RECEIVE:
                        if( isListening() ) {
                            // Connect after receive is necessary for further sending
                            DatagramPacket packet = receiveFromDatagramSocket();
                            Log.d( CLASS_NAME, String.format( "Received data from %s (UDP)", packet.getSocketAddress() ) );
                            socket.connect( packet.getSocketAddress() );
                            Log.d( CLASS_NAME, String.format( "Connected to %s (UDP)", packet.getSocketAddress() ) );
                        }
                        break;
                    case SEND:
                        if( isConnected() ) {
                            Log.d( CLASS_NAME, String.format( "Sending to %s (UDP)", socket.getRemoteSocketAddress() ) );
                            sendToDatagramSocket();
                        }
                        break;
                    case DISCONNECT:
                        if( isConnected() ) {
                            Log.d( CLASS_NAME, String.format( "Disconnecting from %s (UDP)", socket.getRemoteSocketAddress() ) );
                        }
                        if( isListening() ) {
                            Log.d( CLASS_NAME, String.format( "Stop listening on %d (UDP)", socket.getLocalPort() ) );
                        }
                        if( isConnected() || isListening() ) {
                            socket.close();
                            socket = null;
                            publishProgress( IDLE.toString() );
                        }
                        break;
                }
            } catch( Exception e ) {
                e.printStackTrace();
                result.exception = e;
            }
            return result;
        }

        private DatagramPacket receiveFromDatagramSocket() throws IOException
        {
            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket( buf, buf.length );
            socket.receive( packet );
            output.write( packet.getData(), 0, packet.getLength() );
            return packet;
        }

        private void sendToDatagramSocket() throws IOException
        {
            byte[] buf = new byte[1024];
            int bytesRead = input.read( buf, 0, buf.length );
            if( bytesRead > 0 ) {
                DatagramPacket packet = new DatagramPacket( buf, bytesRead );
                socket.send( packet );
            }
        }
    }
}
