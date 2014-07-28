package com.github.dddpaul.netcat;

import com.github.dddpaul.netcat.ui.NetCatFragment;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
    injects = {
        NetCatFragment.class
    },
    complete = false
)
public class NetCatModule
{
    @Provides @Singleton
    NetCater provideNetCat( NetCat netCat )
    {
        return netCat;
    }
}
