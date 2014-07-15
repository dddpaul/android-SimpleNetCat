package com.github.dddpaul.netcat;

import android.widget.AutoCompleteTextView;
import android.widget.Button;

import com.github.dddpaul.netcat.ui.MainFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.robolectric.util.FragmentTestUtil.startFragment;

@Config( emulateSdk = 18 )
@RunWith( RobolectricTestRunner.class )
public class MainFragmentTest
{
    private MainFragment fragment;

    @Before
    public void setUp()
    {
        fragment = new MainFragment();
    }

    /**
     * Test connect button behaviour
     */
    @Test
    public void testConnectToButton()
    {
        startFragment( fragment );
        Button connectButton = (Button) fragment.getView().findViewById( R.id.b_connect );

        assertThat( connectButton ).isDisabled();

        AutoCompleteTextView connectToText = (AutoCompleteTextView) fragment.getView().findViewById( R.id.et_connect_to );
        connectToText.setText( "some host" );

        assertThat( connectButton ).isEnabled();
    }
}
