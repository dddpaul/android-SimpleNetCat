package com.github.dddpaul.netcat;

import java.net.Socket;

import static com.github.dddpaul.netcat.Utils.isNotEmpty;

public interface NetCater
{
    enum Op { CONNECT, LISTEN, RECEIVE, SEND }

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
