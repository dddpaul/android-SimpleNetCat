package com.github.dddpaul.netcat;

public interface NetCatListener
{
    void netCatIsStarted();
    void netCatIsCompleted();
    void netCatIsFailed( Exception e );
}
