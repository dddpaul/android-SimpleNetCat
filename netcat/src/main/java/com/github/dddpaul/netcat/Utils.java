package com.github.dddpaul.netcat;

import android.widget.TextView;

public class Utils
{
    public static boolean isEmpty( String str )
    {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty( String str )
    {
        return !isEmpty( str );
    }

    public static boolean populated( TextView view )
    {
        return view.length() > 0;
    }

    public static boolean isActive( NetCater netCat )
    {
        return netCat != null && ( netCat.isConnected() || netCat.isListening() );
    }
}
