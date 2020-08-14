package de.spicysources.bubblepenetration.database;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class

DataConnection {

    public static String[] getHighscoreData(String username){
        String result = "";
        try {
            HttpURLConnection httpConn = getHttpPostConnection("http://188.68.55.198:8080/BubbleHighscores/GetHighscores");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(httpConn.getOutputStream()));
            writer.write(username+"\n");
            writer.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            result = reader.readLine();
            writer.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.split("\\|");
    }

    public static boolean putHighscoreData(String name, String score){
        String inputString = name+"|"+score+"\n";
        boolean better = false;
        try {
            HttpURLConnection httpConn = getHttpPostConnection("http://188.68.55.198:8080/BubbleHighscores/PutHighscores");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(httpConn.getOutputStream()));
            writer.write(inputString);
            writer.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            char result = (char)reader.read();
            if(result == '1')
                better = true;
            writer.close();
            reader.close();
            httpConn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return better;
    }

    public static boolean getUsernameExists(String username){
        boolean exists = false;
        try {
            HttpURLConnection httpConn = getHttpPostConnection("http://188.68.55.198:8080/BubbleHighscores/CheckUsername");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(httpConn.getOutputStream()));
            writer.write(username+"\n");
            writer.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            char result = (char)reader.read();
            if(result == '1')
                exists = true;
            writer.close();
            reader.close();
            httpConn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return exists;
    }

    private static HttpURLConnection getHttpPostConnection(String url){
        HttpURLConnection httpConn = null;
        try {
            URLConnection conn = new URL(url).openConnection();
            httpConn = (HttpURLConnection)conn;
            httpConn.setRequestMethod("POST");
            httpConn.setDoOutput(true);
            httpConn.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return httpConn;
    }
}