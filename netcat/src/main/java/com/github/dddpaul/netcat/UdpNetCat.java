package com.github.dddpaul.netcat;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class UdpNetCat extends NetCat
{
    private final String CLASS_NAME = getClass().getSimpleName();

    private NetCatTask task;
    private DatagramChannel channel;

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
        return channel != null && channel.isOpen();
    }

    @Override
    public boolean isConnected()
    {
        return channel != null && channel.isConnected();
    }

    public class NetCatTask extends Task
    {
        @Override
        protected Result doInBackground( String... params )
        {
            Op op = Op.valueOf( params[0] );
            Result result = new Result( op, Proto.UDP );
            try {
                Log.d( CLASS_NAME, String.format( "Executing %s operation", op ) );
                int port;
                switch( op ) {
                    case CONNECT:
                        String host = params[1];
                        port = Integer.parseInt( params[2] );
                        Log.d( CLASS_NAME, String.format( "Connecting to %s:%d (UDP)", host, port ) );
                        channel = DatagramChannel.open();
                        channel.connect( new InetSocketAddress( host, port ) );
                        result.object = channel.socket();
                        break;
                    case LISTEN:
                        port = Integer.parseInt( params[1] );
                        Log.d( CLASS_NAME, String.format( "Listening on %d (UDP)", port ) );
                        channel = DatagramChannel.open();
                        channel.socket().bind( new InetSocketAddress( port ) );
                        result.object = channel.socket();
                        break;
                    case RECEIVE:
                        if( isListening() ) {
                            // Connect after receive is necessary for further sending
                            SocketAddress remoteSocketAddress = receiveFromChannel();
                            Log.d( CLASS_NAME, String.format( "Received data from %s (UDP)", remoteSocketAddress ) );
                            channel.connect( remoteSocketAddress );
                            Log.d( CLASS_NAME, String.format( "Connected to %s (UDP)", channel.socket().getRemoteSocketAddress() ) );
                        }
                        break;
                    case SEND:
                        if( isConnected() ) {
                            Log.d( CLASS_NAME, String.format( "Sending to %s (UDP)", channel.socket().getRemoteSocketAddress() ) );
                            sendToChannel();
                        }
                        break;
                    case DISCONNECT:
                        if( isConnected() ) {
                            Log.d( CLASS_NAME, String.format( "Disconnecting from %s (UDP)", channel.socket().getRemoteSocketAddress() ) );
                        }
                        if( isListening() ) {
                            Log.d( CLASS_NAME, String.format( "Stop listening on %d (UDP)", channel.socket().getLocalPort() ) );
                        }
                        if( isConnected() || isListening() ) {
                            channel.close();
                            channel = null;
                        }
                        break;
                }
            } catch( Exception e ) {
                e.printStackTrace();
                result.exception = e;
            }
            return result;
        }

        private SocketAddress receiveFromChannel() throws IOException
        {
            ByteBuffer buf = ByteBuffer.allocate( 1024 );
            buf.clear();
            SocketAddress remoteSocketAddress = channel.receive( buf );
            output.write( buf.array(), 0, buf.position() );
            return remoteSocketAddress;
        }

        private void sendToChannel() throws IOException
        {
            byte[] buf = new byte[1024];
            int bytesRead = input.read( buf, 0, buf.length );
            if( bytesRead > 0 ) {
                channel.send( ByteBuffer.wrap( buf ), channel.socket().getRemoteSocketAddress() );
            }
        }
    }
}
