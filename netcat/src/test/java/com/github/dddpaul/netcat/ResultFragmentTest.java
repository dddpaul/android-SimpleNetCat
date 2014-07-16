package com.github.dddpaul.netcat;

import android.widget.Button;
import android.widget.EditText;

import com.github.dddpaul.netcat.ui.ResultFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Mockito.*;
import static org.robolectric.util.FragmentTestUtil.startFragment;

@Config( emulateSdk = 18 )
@RunWith( RobolectricTestRunner.class )
public class ResultFragmentTest
{
    private ResultFragment fragment;

    @Mock
    private NetCater mockNetCat;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks( this );
        fragment = new ResultFragment();
    }

    /**
     * Test send button behaviour
     */
    @Test
    public void testSendButton()
    {
        startFragment( fragment );
        when( mockNetCat.isConnected() ).thenReturn( true );
        fragment.setNetCat( mockNetCat );

        Button sendButton = (Button) fragment.getView().findViewById( R.id.b_send );

        assertThat( sendButton ).isDisabled();

        EditText inputText = (EditText) fragment.getView().findViewById( R.id.et_input );
        inputText.setText( "some text" );

        assertThat( sendButton ).isEnabled();
    }
}
