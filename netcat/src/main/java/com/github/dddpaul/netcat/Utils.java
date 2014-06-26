package com.github.dddpaul.netcat;

import android.widget.EditText;

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

    public static boolean populated( EditText editText )
    {
        return editText.length() > 0;
    }

    public static boolean connected( NetCater netCat )
    {
        return netCat != null && netCat.isConnected();
    }
}
