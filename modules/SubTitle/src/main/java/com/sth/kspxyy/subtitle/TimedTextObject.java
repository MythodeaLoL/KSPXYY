package com.sth.kspxyy.subtitle;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeMap;

public class TimedTextObject {

    public String title = "";
    public String description = "";
    public String copyrigth = "";
    public String author = "";
    public String fileName = "";

    //list of styles (id, reference)
    public Hashtable<String, Style> styling;

    //list of layouts (id, reference)
    public Hashtable<String, Region> layout;

    //list of captions (begin time, reference)
    //represented by a tree map to maintain order
    public TreeMap<Integer, Caption> captions;

    //to store non fatal errors produced during parsing
    public String warnings;

    //to know if a parsing method has been applied
    public boolean built = false;


    /**
     * Protected constructor so it can't be created from outside
     */
    public TimedTextObject() {
        styling = new Hashtable<String, Style>();
        layout = new Hashtable<String, Region>();
        captions = new TreeMap<Integer, Caption>();
    }

    /**
     * This method simply checks the style list and eliminate any style not referenced by any caption
     * This might come useful when default styles get created and cover too much.
     * It require a unique iteration through all captions.
     */
    protected void cleanUnusedStyles() {
        //here all used styles will be stored
        Hashtable<String, Style> usedStyles = new Hashtable<String, Style>();
        //we iterate over the captions
        Iterator<Caption> itrC = captions.values().iterator();
        while (itrC.hasNext()) {
            //new caption
            Caption current = itrC.next();
            //if it has a style
            if (current.style != null) {
                String iD = current.style.iD;
                //if we haven't saved it yet
                if (!usedStyles.containsKey(iD))
                    usedStyles.put(iD, current.style);
            }
        }
        //we saved the used styles
        this.styling = usedStyles;
    }

    public int getNextLineIndex(int currentPosition) {
        Iterator<Caption> iterator = captions.values().iterator();
        int index = 0;
        while (iterator.hasNext()) {
            Caption current = iterator.next();
            index++;
            if (currentPosition > current.start.mseconds && currentPosition < current.end.mseconds) {
                return index;
            }
        }
        return index;
    }


    public long getFirstLineStartTime() {
        return captions.firstKey();
    }
}
