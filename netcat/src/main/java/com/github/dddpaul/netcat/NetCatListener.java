package com.github.dddpaul.netcat;

import static com.github.dddpaul.netcat.NetCater.Result;

public interface NetCatListener
{
    void netCatIsStarted();
    void netCatIsCompleted( Result result );
    void netCatIsFailed( Result result );
}
