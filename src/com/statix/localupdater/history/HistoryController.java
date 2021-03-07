package com.statix.localupdater.history;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.statix.localupdater.R;
import com.statix.localupdater.misc.Constants;
import com.statix.localupdater.model.HistoryCard;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class HistoryController extends BaseAdapter {

    private ArrayList<HistoryCard> mCards;
    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private Resources mResources;

    private static final String TAG = "HistoryController";

    public HistoryController(Context ctx, Resources res) {
        mContext = ctx;
        mLayoutInflater = LayoutInflater.from(ctx);
        mCards = new ArrayList<>();
        mResources = res;
    }

    public void getUpdates() {
        File historyFile = new File(Constants.HISTORY_PATH);
        try {
            mCards = HistoryUtils.readFromJson(historyFile);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Unable to find previous updates");
        }
    }

    @Override
    public int getCount() {
        return mCards.size();
    }

    @Override
    public Object getItem(int position) {
        return mCards.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.update_cardview, parent, false);
        }
        HistoryCard card = mCards.get(position);
        convertView.setBackgroundColor(card.updateSucceeded()
                ? ResourcesCompat.getColor(mResources, R.color.update_successful, null)
                : ResourcesCompat.getColor(mResources, R.color.update_unsuccessful, null));
        TextView title = convertView.findViewById(R.id.title);
        title.setText(card.getUpdateName());
        String placeholder = mResources.getString(card.updateSucceeded() ? R.string.succeeded : R.string.failed);
        TextView updateWasSuccessful = convertView.findViewById(R.id.update_status);
        updateWasSuccessful.setText(mResources.getString(R.string.update_status, placeholder));
        return convertView;
    }
}
