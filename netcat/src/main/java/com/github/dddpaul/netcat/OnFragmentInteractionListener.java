package com.github.dddpaul.netcat;

import android.support.v4.app.Fragment;

/**
 * Send the event to the host activity
 **/
public interface OnFragmentInteractionListener
{
    public void onFragmentInteraction( int position );

    public void onFragmentInteraction( int position, Fragment caller, NetCat.Op op, String data );
}
