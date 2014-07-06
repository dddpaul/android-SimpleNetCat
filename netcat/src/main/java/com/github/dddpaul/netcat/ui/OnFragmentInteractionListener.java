package com.github.dddpaul.netcat.ui;

import com.github.dddpaul.netcat.NetCater;

/**
 * Send the event to the host activity
 **/
public interface OnFragmentInteractionListener
{
    public void onFragmentInteraction( int position );

    public void onFragmentInteraction( int position, NetCater.Op op, String data );
}
