package com.statix.localupdater.history;

import android.util.Log;

import com.statix.localupdater.misc.Constants;
import com.statix.localupdater.model.ABUpdate;
import com.statix.localupdater.model.HistoryCard;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class HistoryUtils {

    public static synchronized void writeUpdateToJson(File historyFile, ABUpdate update) throws IOException, JSONException {
        boolean updateSuccessful = update.state() == Constants.UPDATE_SUCCEEDED;
        String updateName = update.update().getName();
        ArrayList<HistoryCard> cards = readFromJson(historyFile);
        cards.add(new HistoryCard(updateName, updateSuccessful));
        Collections.sort(cards);
        HashMap<String, Boolean> cardMap = new HashMap<>();
        // convert cards to a map
        for (HistoryCard card : cards) {
            cardMap.put(card.getUpdateName(), card.updateSucceeded());
        }
        JSONObject toWrite = new JSONObject(cardMap);
        String write = toWrite.toString();
        Log.d("HistoryUtils", write);
        FileWriter fileWriter = new FileWriter(historyFile);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(write);
        bufferedWriter.close();
    }

    public static synchronized ArrayList<HistoryCard> readFromJson(File historyFile) throws IOException, JSONException {
        if (historyFile.exists()) {
            FileReader fr = new FileReader(historyFile);
            BufferedReader bufferedReader = new BufferedReader(fr);
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null){
                stringBuilder.append(line).append("\n");
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
            String updates = stringBuilder.toString();
            if (updates == null || updates.isEmpty()) {
                return new ArrayList<HistoryCard>();
            }
            JSONObject historyPairs = new JSONObject(updates);
            JSONArray updateNames = historyPairs.names();
            ArrayList<HistoryCard> ret = new ArrayList<>();
            for (int i = 0; i < updateNames.length(); i++) {
                boolean success = historyPairs.getBoolean(updateNames.getString(i));
                ret.add(new HistoryCard(updateNames.getString(i), success));
            }
            Collections.sort(ret);
            Collections.reverse(ret);
            return ret;
        } else {
            historyFile.createNewFile();
            return new ArrayList<HistoryCard>();
        }
    }
}
