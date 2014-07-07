package com.github.dddpaul.netcat;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static com.github.dddpaul.netcat.Utils.isNotEmpty;

public interface NetCater
{
    enum Op { CONNECT, LISTEN, RECEIVE, SEND, DISCONNECT }
    enum State { IDLE, CONNECTED, LISTENING }

    public void cancel();
    public void execute( String ... params );
    public void executeParallel( String ... params );
    public void setSocket( Socket socket );
    public void setInput( InputStream input );
    public void setOutput( OutputStream input );
    public boolean isConnected();

    class Result
    {
        public Op op;
        public Object object;
        public Exception exception;

        public Result( Op op )
        {
            this.op = op;
        }

        public Socket getSocket()
        {
            if( object instanceof Socket ) {
                return (Socket) object;
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
