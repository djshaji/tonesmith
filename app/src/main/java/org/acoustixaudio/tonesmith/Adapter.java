package org.acoustixaudio.tonesmith;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.acoustixaudio.tonesmith.MainActivity;

import java.util.ArrayList;

public class Adapter extends BaseAdapter {
    MainActivity mainActivity = null ;
    public ArrayList<Integer> plugins = new ArrayList<>();
    public ArrayList<View> layouts = new ArrayList<View>();

    @Override
    public int getCount() {
        return plugins.size();
    }

    @Override
    public Object getItem(int position) {
        return layouts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mainActivity.getLayoutInflater().inflate(R.layout.plugin, null);
            layouts.add(convertView);
        }

        return convertView;
    }

    Adapter (MainActivity _mainActivity) {
        mainActivity = _mainActivity;
    }
}
