package com.sth.kspxyy.subtitle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class FormatSRT implements TimedTextFileFormat {

    public TimedTextObject parseFile(String fileName, InputStream is) throws IOException {

        TimedTextObject tto = new TimedTextObject();
        Caption caption = new Caption();
        int captionNumber = 1;
        boolean allGood;

        //first lets load the file
        InputStreamReader in = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(in);

        //the file name is saved
        tto.fileName = fileName;

        String line = br.readLine();
        int lineCounter = 0;
        try {
            while (line != null) {
                line = line.trim();
                lineCounter++;
                //if its a blank line, ignore it, otherwise...
                if (!line.isEmpty()) {
                    allGood = false;
                    //the first thing should be an increasing number
                    try {
                        int num = Integer.parseInt(line);
                        if (num != captionNumber)
                            throw new Exception();
                        else {
                            captionNumber++;
                            allGood = true;
                        }
                    } catch (Exception e) {
                        tto.warnings += captionNumber + " expected at line " + lineCounter;
                        tto.warnings += "\n skipping to next line\n\n";
                    }
                    if (allGood) {
                        //we go to next line, here the begin and end time should be found
                        try {
                            lineCounter++;
                            line = br.readLine().trim();
                            String start = line.substring(0, 12);
                            String end = line.substring(line.length() - 12, line.length());
                            Time time = new Time("hh:mm:ss,ms", start);
                            caption.start = time;
                            time = new Time("hh:mm:ss,ms", end);
                            caption.end = time;
                        } catch (Exception e) {
                            tto.warnings += "incorrect time format at line " + lineCounter;
                            allGood = false;
                        }
                    }
                    if (allGood) {
                        //we go to next line where the caption text starts
                        lineCounter++;
                        line = br.readLine().trim();
                        String text = "";
                        while (!line.isEmpty()) {
                            text += line + "<br />";
                            line = br.readLine().trim();
                            lineCounter++;
                        }
                        caption.content = text;
                        int key = caption.start.mseconds;
                        //in case the key is already there, we increase it by a millisecond, since no duplicates are allowed
                        while (tto.captions.containsKey(key)) key++;
                        if (key != caption.start.mseconds)
                            tto.warnings += "caption with same start time found...\n\n";
                        //we add the caption.
                        tto.captions.put(key, caption);
                    }
                    //we go to next blank
                    while (!line.isEmpty()) {
                        line = br.readLine().trim();
                        lineCounter++;
                    }
                    caption = new Caption();
                }
                line = br.readLine();
            }

        } catch (NullPointerException e) {
            tto.warnings += "unexpected end of file, maybe last caption is not complete.\n\n";
        } finally {
            //we close the reader
            is.close();
        }

        tto.built = true;
        return tto;
    }
}
