package com.github.dddpaul.netcat;

import com.github.dddpaul.netcat.ui.ResultFragment;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
    injects = {
        ResultFragment.class
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
