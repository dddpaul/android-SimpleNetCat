package com.github.dddpaul.netcat;

import android.support.v4.view.ViewPager;

import com.github.dddpaul.netcat.ui.MainActivity;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config( emulateSdk = 18 )
@RunWith( RobolectricTestRunner.class )
public class ActivityTest extends Assert
{
    @Test
    public void test()
    {
        MainActivity activity = Robolectric.buildActivity( MainActivity.class ).create().get();
        assertNotNull( activity );

        ViewPager pager = (ViewPager) activity.findViewById( R.id.pager );
        assertNotNull( pager );
    }
}
