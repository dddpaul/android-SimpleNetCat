package com.github.dddpaul.netcat;

import org.mockito.Mockito;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * This module needs to override {@link NetCatModule} when using from tests
 */
@Module(
    injects = {
        ResultFragmentTest.class
    },
    overrides = true,
    library = true,
    complete = false
)
public class MockNetCatModule
{
    @Provides @Singleton
    NetCater provideNetCat()
    {
        return Mockito.mock( NetCat.class );
    }
}
