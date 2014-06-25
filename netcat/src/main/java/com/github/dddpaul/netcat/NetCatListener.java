package com.github.dddpaul.netcat;

public interface NetCatListener
{
    void netCatIsStarted();
    void netCatIsCompleted( NetCat.Op op );
    void netCatIsFailed( Exception e );
}
