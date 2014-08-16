package com.github.dddpaul.netcat;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.Socket;

import static com.github.dddpaul.netcat.Utils.isNotEmpty;

public interface NetCater
{
    enum Op { CONNECT, LISTEN, RECEIVE, SEND, DISCONNECT, CLEAR_OUTPUT_VIEW }
    enum State { IDLE, CONNECTED, LISTENING, OUTPUT_VIEW_CLEARED }
    enum Proto { TCP, UDP }

    public void cancel();
    public void execute( String ... params );
    public void executeParallel( String ... params );
    public void setListener( NetCatListener listener );
    public void setInput( InputStream input );
    public void createOutput();
    public void closeOutput();
    public OutputStream getOutput();
    public String getOutputString();
    public boolean isConnected();
    public boolean isListening();

    class Result
    {
        public Op op;
        public Object object;
        public Exception exception;

        public Result( Op op )
        {
            this.op = op;
        }

        public Closeable getSocket()
        {
            if( object instanceof Socket || object instanceof DatagramSocket ) {
                return (Closeable) object;
            }
            return null;
        }

        public String getErrorMessage()
        {
            String message = "";
            if( exception != null && isNotEmpty( exception.getMessage() )) {
                message = exception.getMessage();
            }
            return message;
        }
    }
}
