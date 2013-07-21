package com.sth.kspxyy.subtitle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FormatASS implements TimedTextFileFormat {

    public TimedTextObject parseFile(String fileName, InputStream is) throws IOException {

        TimedTextObject tto = new TimedTextObject();
        tto.fileName = fileName;

        Caption caption = new Caption();
        Style style;

        //for the clock timer
        float timer = 100;

        //if the file is .SSA or .ASS
        boolean isASS = false;

        //variables to store the formats
        String[] styleFormat;
        String[] dialogueFormat;

        //first lets load the file
        InputStreamReader in = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(in);

        String line;
        int lineCounter = 0;
        try {
            //we scour the file
            line = br.readLine();
            lineCounter++;
            while (line != null) {
                line = line.trim();
                //we skip any line until we find a section [section name]
                if (line.startsWith("[")) {
                    //now we must identify the section
                    if (line.equalsIgnoreCase("[Script info]")) {
                        //its the script info section section
                        lineCounter++;
                        line = br.readLine().trim();
                        //Each line is scanned for useful info until a new section is detected
                        while (!line.startsWith("[")) {
                            if (line.startsWith("Title:"))
                                //We have found the title
                                tto.title = line.split(":")[1].trim();
                            else if (line.startsWith("Original Script:"))
                                //We have found the author
                                tto.author = line.split(":")[1].trim();
                            else if (line.startsWith("Script Type:")) {
                                //we have found the version
                                if (line.split(":")[1].trim().equalsIgnoreCase("v4.00+")) isASS = true;
                                    //we check the type to set isASS or to warn if it comes from an older version than the studied specs
                                else if (!line.split(":")[1].trim().equalsIgnoreCase("v4.00"))
                                    tto.warnings += "Script version is older than 4.00, it may produce parsing errors.";
                            } else if (line.startsWith("Timer:"))
                                //We have found the timer
                                timer = Float.parseFloat(line.split(":")[1].trim().replace(',', '.'));
                            //we go to the next line
                            lineCounter++;
                            line = br.readLine().trim();
                        }

                    } else if (line.equalsIgnoreCase("[v4 Styles]")
                            || line.equalsIgnoreCase("[v4 Styles+]")
                            || line.equalsIgnoreCase("[v4+ Styles]")) {
                        //its the Styles description section
                        if (line.contains("+") && isASS == false) {
                            //its ASS and it had not been noted
                            isASS = true;
                            tto.warnings += "ScriptType should be set to v4:00+ in the [Script Info] section.\n\n";
                        }
                        lineCounter++;
                        line = br.readLine().trim();
                        //the first line should define the format
                        if (!line.startsWith("Format:")) {
                            //if not, we scan for the format.
                            tto.warnings += "Format: (format definition) expected at line " + line + " for the styles section\n\n";
                            while (!line.startsWith("Format:")) {
                                lineCounter++;
                                line = br.readLine().trim();
                                ;
                            }
                        }
                        // we recover the format's fields
                        styleFormat = line.split(":")[1].trim().split(",");
                        lineCounter++;
                        line = br.readLine().trim();
                        // we parse each style until we reach a new section
                        while (!line.startsWith("[")) {
                            //we check it is a style
                            if (line.startsWith("Style:")) {
                                //we parse the style
                                style = parseStyleForASS(line.split(":")[1].trim().split(","), styleFormat, lineCounter, isASS, tto.warnings);
                                //and save the style
                                tto.styling.put(style.iD, style);
                            }
                            //next line
                            lineCounter++;
                            line = br.readLine().trim();
                        }

                    } else if (line.trim().equalsIgnoreCase("[Events]")) {
                        //its the events specification section
                        lineCounter++;
                        line = br.readLine().trim();
                        tto.warnings += "Only dialogue events are considered, all other events are ignored.\n\n";
                        //the first line should define the format of the dialogues
                        if (!line.startsWith("Format:")) {
                            //if not, we scan for the format.
                            tto.warnings += "Format: (format definition) expected at line " + line + " for the events section\n\n";
                            while (!line.startsWith("Format:")) {
                                lineCounter++;
                                line = br.readLine().trim();
                            }
                        }
                        // we recover the format's fields
                        dialogueFormat = line.split(":")[1].trim().split(",");
                        //next line
                        lineCounter++;
                        line = br.readLine().trim();
                        // we parse each style until we reach a new section
                        while (!line.startsWith("[")) {
                            //we check it is a dialogue
                            //WARNING: all other events are ignored.
                            if (line.startsWith("Dialogue:")) {
                                //we parse the dialogue
                                caption = parseDialogueForASS(line.split(":", 2)[1].trim().split(",", 10), dialogueFormat, timer, tto);
                                //and save the caption
                                int key = caption.start.mseconds;
                                //in case the key is already there, we increase it by a millisecond, since no duplicates are allowed
                                while (tto.captions.containsKey(key)) key++;
                                tto.captions.put(key, caption);
                            }
                            //next line
                            lineCounter++;
                            line = br.readLine().trim();
                        }

                    } else if (line.trim().equalsIgnoreCase("[Fonts]") || line.trim().equalsIgnoreCase("[Graphics]")) {
                        //its the custom fonts or embedded graphics section
                        //these are not supported
                        tto.warnings += "The section " + line.trim() + " is not supported for conversion, all information there will be lost.\n\n";
                        line = br.readLine().trim();
                    } else {
                        tto.warnings += "Unrecognized section: " + line.trim() + " all information there is ignored.";
                        line = br.readLine().trim();
                    }
                } else {
                    line = br.readLine();
                    lineCounter++;
                }
            }
            // parsed styles that are not used should be eliminated
            tto.cleanUnusedStyles();

        } catch (NullPointerException e) {
            tto.warnings += "unexpected end of file, maybe last caption is not complete.\n\n";
        } finally {
            //we close the reader
            is.close();
        }

        tto.built = true;
        return tto;
    }


    private Style parseStyleForASS(String[] line, String[] styleFormat, int index, boolean isASS, String warnings) {
        Style newStyle = new Style(Style.defaultID());
        if (line.length != styleFormat.length) {
            //both should have the same size
            warnings += "incorrectly formated line at " + index + "\n\n";
        } else {
            for (int i = 0; i < styleFormat.length; i++) {
                //we go through every format parameter and save the interesting values
                if (styleFormat[i].trim().equalsIgnoreCase("Name")) {
                    //we save the name
                    newStyle.iD = line[i].trim();
                } else if (styleFormat[i].trim().equalsIgnoreCase("Fontname")) {
                    //we save the font
                    newStyle.font = line[i].trim();
                } else if (styleFormat[i].trim().equalsIgnoreCase("Fontsize")) {
                    //we save the size
                    newStyle.fontSize = line[i].trim();
                } else if (styleFormat[i].trim().equalsIgnoreCase("PrimaryColour")) {
                    //we save the color
                    String color = line[i].trim();
                    if (isASS) {
                        if (color.startsWith("&H")) newStyle.color = Style.getRGBValue("&HAABBGGRR", color);
                        else newStyle.color = Style.getRGBValue("decimalCodedAABBGGRR", color);
                    } else {
                        if (color.startsWith("&H")) newStyle.color = Style.getRGBValue("&HBBGGRR", color);
                        else newStyle.color = Style.getRGBValue("decimalCodedBBGGRR", color);
                    }
                } else if (styleFormat[i].trim().equalsIgnoreCase("BackColour")) {
                    //we save the background color
                    String color = line[i].trim();
                    if (isASS) {
                        if (color.startsWith("&H")) newStyle.backgroundColor = Style.getRGBValue("&HAABBGGRR", color);
                        else newStyle.backgroundColor = Style.getRGBValue("decimalCodedAABBGGRR", color);
                    } else {
                        if (color.startsWith("&H")) newStyle.backgroundColor = Style.getRGBValue("&HBBGGRR", color);
                        else newStyle.backgroundColor = Style.getRGBValue("decimalCodedBBGGRR", color);
                    }
                } else if (styleFormat[i].trim().equalsIgnoreCase("Bold")) {
                    //we save if bold
                    newStyle.bold = Boolean.parseBoolean(line[i].trim());
                } else if (styleFormat[i].trim().equalsIgnoreCase("Italic")) {
                    //we save if italic
                    newStyle.italic = Boolean.parseBoolean(line[i].trim());
                } else if (styleFormat[i].trim().equalsIgnoreCase("Underline")) {
                    //we save if underlined
                    newStyle.underline = Boolean.parseBoolean(line[i].trim());
                } else if (styleFormat[i].trim().equalsIgnoreCase("Alignment")) {
                    //we save the alignment
                    int placement = Integer.parseInt(line[i].trim());
                    if (isASS) {
                        switch (placement) {
                            case 1:
                                newStyle.textAlign = "bottom-left";
                                break;
                            case 2:
                                newStyle.textAlign = "bottom-center";
                                break;
                            case 3:
                                newStyle.textAlign = "bottom-right";
                                break;
                            case 4:
                                newStyle.textAlign = "mid-left";
                                break;
                            case 5:
                                newStyle.textAlign = "mid-center";
                                break;
                            case 6:
                                newStyle.textAlign = "mid-right";
                                break;
                            case 7:
                                newStyle.textAlign = "top-left";
                                break;
                            case 8:
                                newStyle.textAlign = "top-center";
                                break;
                            case 9:
                                newStyle.textAlign = "top-right";
                                break;
                            default:
                                warnings += "undefined alignment for style at line " + index + "\n\n";
                        }
                    } else {
                        switch (placement) {
                            case 9:
                                newStyle.textAlign = "bottom-left";
                                break;
                            case 10:
                                newStyle.textAlign = "bottom-center";
                                break;
                            case 11:
                                newStyle.textAlign = "bottom-right";
                                break;
                            case 1:
                                newStyle.textAlign = "mid-left";
                                break;
                            case 2:
                                newStyle.textAlign = "mid-center";
                                break;
                            case 3:
                                newStyle.textAlign = "mid-right";
                                break;
                            case 5:
                                newStyle.textAlign = "top-left";
                                break;
                            case 6:
                                newStyle.textAlign = "top-center";
                                break;
                            case 7:
                                newStyle.textAlign = "top-right";
                                break;
                            default:
                                warnings += "undefined alignment for style at line " + index + "\n\n";
                        }
                    }
                }
            }
        }

        return newStyle;
    }

    /**
     * This methods transforms a dialogue line from ASS according to a format definition into an Caption object.
     *
     * @param line           the dialogue line without its declaration
     * @param dialogueFormat the list of attributes in this dialogue line
     * @param timer          % to speed or slow the clock, above 100% span of the subtitles is reduced.
     * @return a new Caption object
     */
    private Caption parseDialogueForASS(String[] line, String[] dialogueFormat, float timer, TimedTextObject tto) {
        Caption newCaption = new Caption();

        //all information from fields 10 onwards are the caption text therefore needn't be split
        String captionText = line[9];
        //text is cleaned before being inserted into the caption
        newCaption.content = captionText.replaceAll("\\{.*?\\}", "").replace("\n", "<br />").replace("\\N", "<br />");

        for (int i = 0; i < dialogueFormat.length; i++) {
            //we go through every format parameter and save the interesting values
            if (dialogueFormat[i].trim().equalsIgnoreCase("Style")) {
                //we save the style
                Style s = tto.styling.get(line[i].trim());
                if (s != null)
                    newCaption.style = s;
                else
                    tto.warnings += "undefined style: " + line[i].trim() + "\n\n";
            } else if (dialogueFormat[i].trim().equalsIgnoreCase("Start")) {
                //we save the starting time
                newCaption.start = new Time("h:mm:ss.cs", line[i].trim());
            } else if (dialogueFormat[i].trim().equalsIgnoreCase("End")) {
                //we save the starting time
                newCaption.end = new Time("h:mm:ss.cs", line[i].trim());
            }
        }

        //timer is applied
        if (timer != 100) {
            newCaption.start.mseconds /= (timer / 100);
            newCaption.end.mseconds /= (timer / 100);
        }
        return newCaption;
    }
}