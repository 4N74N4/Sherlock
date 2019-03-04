package com.bgu.congeor.sherlockapp.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.bgu.congeor.Constants;
import com.bgu.congeor.sherlockapp.fragment.InstructionsFragment;


/**
 * Created with IntelliJ IDEA.
 * User: stan
 * Date: 26/12/13
 * Time: 11:56
 * To change this template use File | Settings | File Templates.
 */
public class TabsAdapter extends FragmentStatePagerAdapter
{

   /* private Fragment CouponsPouchFragment;
    private Fragment SettingsFragment;
    private Fragment FBLoginFragment;*/

    public TabsAdapter(FragmentManager fm)
    {
        super(fm);
        /*this.CouponsPouchFragment = new CouponsPouchFragment();
        this.SettingsFragment = new SettingsFragment();
        this.FBLoginFragment = new FBLoginFragment();*/
    }

    @Override
    public Fragment getItem(int i)
    {
        switch (i)
        {
            case 0:
                return new InstructionsFragment();
            case 1:


            default:
                return null;
        }
    }

    @Override
    public int getCount()
    {
        return Constants.TABS_STRINGS_NETMONITOR.length;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public CharSequence getPageTitle(int position)
    {
        return "Tab " + (position + 1);
    }

}
