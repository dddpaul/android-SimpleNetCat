package com.github.dddpaul.netcat;

/**
 * Send the event to the host activity
 **/
public interface OnFragmentInteractionListener
{
    public void onFragmentInteraction( int position );

    public void onFragmentInteraction( int position, NetCat.Op op, String data );
}
