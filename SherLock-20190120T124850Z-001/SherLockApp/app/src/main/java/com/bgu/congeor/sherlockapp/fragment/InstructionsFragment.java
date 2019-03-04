package com.bgu.congeor.sherlockapp.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.bgu.congeor.sherlockapp.R;

/**
 * Created by stan on 12/02/14.
 */
public class InstructionsFragment extends Fragment
{

    private String[] instructions = {"על האפליקציה לרוץ בכל זמן","יש לוודא כי בכל עת מופיע אייקון זכוכית המגדלת בשורת ההודעות","על הwifi והgps להיות דלוקים כל הזמן","במידה והאפליקציה קורסת יש לוודא כי היא עולה מחדש או להפעילה ידנית"};


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        final View rootView = inflater.inflate(R.layout.instructions, container, false);
        Context context = rootView.getContext();
        ListView instList = (ListView) rootView.findViewById(R.id.instructionList);
        instList.setAdapter(new InstructionListAdapter(getActivity(), R.id.instructionList, instructions));





        return rootView;
    }

    private class InstructionListAdapter extends ArrayAdapter<String>
    {

        private Context context;
        String[] data;

        public InstructionListAdapter(Context context, int textViewResourceId, String[] data)
        {

            super(context, textViewResourceId, data);
            this.data = data;
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if (convertView == null)
            {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.instruction_item, parent, false);
            }
            ((TextView) convertView.findViewById(R.id.instText)).setText(this.data[position]);
            return convertView;
        }

    }
}
