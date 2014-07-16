package com.github.dddpaul.netcat;

import android.widget.Button;

import com.github.dddpaul.netcat.ui.ResultFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import dagger.ObjectGraph;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.robolectric.util.FragmentTestUtil.startFragment;

@Config( emulateSdk = 18 )
@RunWith( RobolectricTestRunner.class )
public class ResultFragmentTest
{
    private ResultFragment fragment;

    @Before
    public void setUp()
    {
        ObjectGraph.create( new MockNetCatModule() ).inject( this );
        fragment = new ResultFragment();
    }

    /**
     * Test send button behaviour
     */
    @Test
    public void testSendButton()
    {
        startFragment( fragment );
        Button sendButton = (Button) fragment.getView().findViewById( R.id.b_send );

        assertThat( sendButton ).isDisabled();
    }
}
