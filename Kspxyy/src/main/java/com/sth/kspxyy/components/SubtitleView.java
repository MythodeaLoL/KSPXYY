package com.sth.kspxyy.components;

import android.R;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.sth.kspxyy.subtitle.Caption;
import com.sth.kspxyy.subtitle.FormatSRT;
import com.sth.kspxyy.subtitle.TimedTextObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

public class SubtitleView extends ListView {

    private TimedTextObject timedTextObject;

    public SubtitleView(Context context) {
        super(context);
    }

    public SubtitleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SubtitleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void loadSubtitle(String path) {
        try {
            FormatSRT formatSRT = new FormatSRT();
            File dir = Environment.getExternalStorageDirectory();

            timedTextObject = formatSRT.parseFile("", new FileInputStream(dir.getPath() + path));
            setAdapter(new SubtitleAdapter(filterByLanguage(timedTextObject.captions)));
        } catch (IOException e) {
            Toast.makeText(getContext(), "Failed to load subtitle.", Toast.LENGTH_LONG).show();
        }
    }

    private ArrayList<Caption> filterByLanguage(TreeMap<Integer, Caption> captions) {
        ArrayList<Caption> results = new ArrayList<Caption>();
        for (Caption caption : captions.values()) {
            results.add(caption);
        }
        return results;
    }

    private class SubtitleAdapter implements ListAdapter {
        private ArrayList<Caption> captions;

        private SubtitleAdapter(ArrayList<Caption> captions) {
            this.captions = captions;
        }



        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public int getCount() {
            return captions.size();
        }

        @Override
        public Object getItem(int position) {
            return captions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new SubTitleTextView(getContext());
            }

            ((SubTitleTextView) convertView).setCaption(captions.get(position));
            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
