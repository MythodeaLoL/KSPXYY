package com.sth.kspxyy.subtitle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FormatSCC implements TimedTextFileFormat {

    public TimedTextObject parseFile(String fileName, InputStream is) throws IOException {

        TimedTextObject tto = new TimedTextObject();
        Caption newCaption = null;

        //variables to represent a decoder
        String textBuffer = "";
        boolean isChannel1 = false;
        boolean isBuffered = true;

        //to store current style
        boolean underlined = false;
        boolean italics = false;
        String color = null;

        //first lets load the file
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        //the file name is saved
        tto.fileName = fileName;
        tto.title = fileName;

        String line;
        int lineCounter = 0;
        try {

            lineCounter++;
            //the file must start with the type declaration
            if (!br.readLine().trim().equalsIgnoreCase("Scenarist_SCC V1.0")) {
                //this is a fatal parsing error.
                throw new IOException("The fist line should define the file type: \"Scenarist_SCC V1.0\"");

            } else {

                createSCCStyles(tto);

                tto.warnings += "Only data from CC channel 1 will be extracted.\n\n";
                line = br.readLine();

                while (line != null) {
                    line = line.trim();
                    lineCounter++;
                    //if its not an empty line
                    if (!line.isEmpty()) {
                        //we separate the time code from the VANC data
                        String[] data = line.split("\t");
                        Time currentTime = new Time("h:m:s:f/fps", data[0] + "/29.97");
                        //we separate the words
                        data = data[1].split(" ");
                        for (int j = 0; j < data.length; j++) {
                            //we get its hex value stored in a short
                            int word = Integer.parseInt(data[j], 16);

                            // odd parity could be checked here

                            //we eliminate the parity bits before decoding
                            word &= 0x7f7f;

                            // if it is a char:
                            if ((word & 0x6000) != 0) {
                                //if we are in the right channel (1)
                                if (isChannel1) {
                                    //we extract the two chars
                                    byte c1 = (byte) ((word & 0xff00) >>> 8);
                                    byte c2 = (byte) (word & 0x00ff);

                                    if (isBuffered) {
                                        //we decode the byte and add it to the text buffer
                                        textBuffer += decodeChar(c1);
                                        //we decode the second char and add it, this one can be empty.
                                        textBuffer += decodeChar(c2);
                                    } else {
                                        //we decode the byte and add it to the text screen
                                        newCaption.content += decodeChar(c1);
                                        //we decode the second char and add it, this one can be empty.
                                        newCaption.content += decodeChar(c2);
                                    }
                                }

                            } else if (word == 0x0000)
                                // word 8080 is filler to add frames
                                currentTime.mseconds += 1000 / 29.97;
                            else {
                                //it is a control code
                                if (j + 1 < data.length && data[j].equals(data[j + 1]))
                                    //if code is repeated, skip one.
                                    j++;

                                // we check the channel
                                if ((word & 0x0800) == 0) {
                                    //we are on channel 1 or 3

                                    //we parse the code
                                    if ((word & 0x1670) == 0x1420) {
                                        //it is a command code
                                        //we check the channel
                                        if ((word & 0x0100) == 0) {
                                            //it is channel 1
                                            isChannel1 = true;
                                            //the command is decoded
                                            word &= 0x000f;
                                            switch (word) {
                                                case 0:
                                                    //Resume Caption Loading: start pop on captions
                                                    isBuffered = true;
                                                    textBuffer = "";
                                                    break;
                                                case 5:
                                                case 6:
                                                case 7:
                                                    //roll-up caption by number of rows, effect not supported
                                                    //clear text buffer
                                                    textBuffer = "";
                                                    //clear screen text
                                                    if (newCaption != null) {
                                                        newCaption.end = currentTime;
                                                        String style = "";
                                                        style += color;
                                                        if (underlined) style += "U";
                                                        if (italics) style += "I";
                                                        newCaption.style = tto.styling.get(style);
                                                        tto.captions.put(newCaption.start.mseconds, newCaption);
                                                    }
                                                    //new caption starts with roll up style
                                                    newCaption = new Caption();
                                                    newCaption.start = currentTime;
                                                    //all characters and codes will be applied directly to the screen
                                                    isBuffered = false;
                                                    break;
                                                case 9:
                                                    //Resume Direct Captioning: start paint-on captions
                                                    isBuffered = false;
                                                    newCaption = new Caption();
                                                    newCaption.start = currentTime;
                                                    break;
                                                case 12:
                                                    //Erase Displayed Memory: clear screen text
                                                    if (newCaption != null) {
                                                        newCaption.end = currentTime;
                                                        if (newCaption.start != null) {
                                                            //we save the caption
                                                            int key = newCaption.start.mseconds;
                                                            //in case the key is already there, we increase it by a millisecond, since no duplicates are allowed
                                                            while (tto.captions.containsKey(key)) key++;
                                                            //we save the caption
                                                            tto.captions.put(newCaption.start.mseconds, newCaption);
                                                            //and reset the caption builder
                                                            newCaption = new Caption();
                                                        }
                                                    }
                                                    break;
                                                case 14:
                                                    //Erase Non-Displayed Memory: clear the text buffer
                                                    textBuffer = "";
                                                    break;
                                                case 15:
                                                    //End of caption: Swap off-screen buffer with caption screen.
                                                    newCaption = new Caption();
                                                    newCaption.start = currentTime;
                                                    newCaption.content += textBuffer;
                                                    break;
                                                default:
                                                    //unsupported or unrecognized command code
                                            }

                                        } else {
                                            isChannel1 = false;
                                        }

                                    } else if (isChannel1) {
                                        if ((word & 0x1040) == 0x1040) {
                                            //it is a preamble code, format is removed
                                            color = "white";
                                            underlined = false;
                                            italics = false;
                                            //it is a new line
                                            if (isBuffered && !textBuffer.isEmpty())
                                                textBuffer += "<br />";
                                            if (!isBuffered && !newCaption.content.isEmpty())
                                                newCaption.content += "<br />";
                                            if ((word & 0x0001) == 1)
                                                //it is underlined
                                                underlined = true;
                                            //positioning is not supported, rows and columns are ignored
                                            if ((word & 0x0010) != 0x0010) {
                                                //setting style for following text
                                                word &= 0x000e;
                                                word = (short) (word >> 1);
                                                switch (word) {
                                                    case 0:
                                                        color = "white";
                                                        break;
                                                    case 1:
                                                        color = "green";
                                                        break;
                                                    case 2:
                                                        color = "blue";
                                                        break;
                                                    case 3:
                                                        color = "cyan";
                                                        break;
                                                    case 4:
                                                        color = "red";
                                                        break;
                                                    case 5:
                                                        color = "yellow";
                                                        break;
                                                    case 6:
                                                        color = "magenta";
                                                        break;
                                                    case 7:
                                                        italics = true;
                                                        break;
                                                    default:
                                                        //error!
                                                }
                                            } else {
                                                color = "white";
                                            }


                                        } else if ((word & 0x1770) == 0x1120) {
                                            //it is a midrow style code
                                            if ((word & 0x001) == 1)
                                                //it is underlined
                                                underlined = true;
                                            else underlined = false;
                                            //setting style for text
                                            word &= 0x000e;
                                            word = (short) (word >> 1);
                                            switch (word) {
                                                case 0:
                                                    color = "white";
                                                    italics = false;
                                                    break;
                                                case 1:
                                                    color = "green";
                                                    italics = false;
                                                    break;
                                                case 2:
                                                    color = "blue";
                                                    italics = false;
                                                    break;
                                                case 3:
                                                    color = "cyan";
                                                    italics = false;
                                                    break;
                                                case 4:
                                                    color = "red";
                                                    italics = false;
                                                    break;
                                                case 5:
                                                    color = "yellow";
                                                    italics = false;
                                                    break;
                                                case 6:
                                                    color = "magenta";
                                                    italics = false;
                                                    break;
                                                case 7:
                                                    italics = true;
                                                    break;
                                                default:
                                                    //error!
                                            }
                                        } else if ((word & 0x177c) == 0x1720) {
                                            //it is a tab code
                                            //positioning is not supported

                                        } else if ((word & 0x1770) == 0x1130) {
                                            //it is a special character code
                                            word &= 0x000f;
                                            //coded value is extracted
                                            if (isBuffered)
                                                //we decode the special char and add it to the text buffer
                                                textBuffer += decodeSpecialChar(word);
                                            else
                                                //we decode the special char and add it to the text
                                                newCaption.content += decodeSpecialChar(word);
                                        } else if ((word & 0x1660) == 0x1220) {
                                            //it is an extended character code
                                            word &= 0x011f;
                                            //coded value is extracted
                                            if (isBuffered)
                                                //we decode the extended char and add it to the text buffer
                                                decodeXtChar(textBuffer, word);
                                            else
                                                //we decode the extended char and add it to the text
                                                decodeXtChar(newCaption.content, word);

                                        } else {
                                            //non recognized code
                                        }
                                    }
                                } else {
                                    //we are on channel 2 or 4
                                    isChannel1 = false;
                                }

                            }
                        }


                    }
                    // end of while
                    line = br.readLine();

                }

                //we save any last shown caption
                newCaption.end = new Time("h:m:s:f/fps", "99:59:59:29/29.97");
                ;
                if (newCaption.start != null) {
                    //we save the caption
                    int key = newCaption.start.mseconds;
                    //in case the key is already there, we increase it by a millisecond, since no duplicates are allowed
                    while (tto.captions.containsKey(key)) key++;
                    //we save the caption
                    tto.captions.put(newCaption.start.mseconds, newCaption);
                }
                tto.cleanUnusedStyles();
            }

        } catch (NullPointerException e) {
            tto.warnings += "unexpected end of file at line " + lineCounter + ", maybe last caption is not complete.\n\n";
        } finally {
            //we close the reader
            is.close();
        }

        tto.built = true;
        return tto;
    }

    private String decodeChar(byte c) {
        switch (c) {
            case 42:
                return "�";
            case 92:
                return "�";
            case 94:
                return "�";
            case 95:
                return "�";
            case 96:
                return "�";
            case 123:
                return "�";
            case 124:
                return "�";
            case 125:
                return "�";
            case 126:
                return "�";
            case 127:
                return "|";
            case 0:
                //filler code
                return "";
            default:
                return "" + (char) c;
        }
    }


    private String decodeSpecialChar(int word) {
        switch (word) {
            case 15:
                return "�";
            case 14:
                return "�";
            case 13:
                return "�";
            case 12:
                return "�";
            case 11:
                return "�";
            case 10:
                return "�";
            case 9:
                return "\u00A0";
            case 8:
                return "�";
            case 7:
                return "\u266A";
            case 6:
                return "�";
            case 5:
                return "�";
            case 4:
                return "�";
            case 3:
                return "�";
            case 2:
                return "�";
            case 1:
                return "�";
            case 0:
                return "�";
            default:
                //unrecoginzed code
                return "";
        }
    }

    private void decodeXtChar(String textBuffer, int word) {
        switch (word) {

        }
    }

    private void createSCCStyles(TimedTextObject tto) {
        Style style;

        style = new Style("white");
        tto.styling.put(style.iD, style);

        style = new Style("whiteU", style);
        style.underline = true;
        tto.styling.put(style.iD, style);

        style = new Style("whiteUI", style);
        style.italic = true;
        tto.styling.put(style.iD, style);

        style = new Style("whiteI", style);
        style.underline = false;
        tto.styling.put(style.iD, style);

        style = new Style("green");
        style.color = Style.getRGBValue("name", "green");
        tto.styling.put(style.iD, style);

        style = new Style("greenU", style);
        style.underline = true;
        tto.styling.put(style.iD, style);

        style = new Style("greenUI", style);
        style.italic = true;
        tto.styling.put(style.iD, style);

        style = new Style("greenI", style);
        style.underline = false;
        tto.styling.put(style.iD, style);

        style = new Style("blue");
        style.color = Style.getRGBValue("name", "blue");
        tto.styling.put(style.iD, style);

        style = new Style("blueU", style);
        style.underline = true;
        tto.styling.put(style.iD, style);

        style = new Style("blueUI", style);
        style.italic = true;
        tto.styling.put(style.iD, style);

        style = new Style("blueI", style);
        style.underline = false;
        tto.styling.put(style.iD, style);

        style = new Style("cyan");
        style.color = Style.getRGBValue("name", "cyan");
        tto.styling.put(style.iD, style);

        style = new Style("cyanU", style);
        style.underline = true;
        tto.styling.put(style.iD, style);

        style = new Style("cyanUI", style);
        style.italic = true;
        tto.styling.put(style.iD, style);

        style = new Style("cyanI", style);
        style.underline = false;
        tto.styling.put(style.iD, style);

        style = new Style("red");
        style.color = Style.getRGBValue("name", "red");
        tto.styling.put(style.iD, style);

        style = new Style("redU", style);
        style.underline = true;
        tto.styling.put(style.iD, style);

        style = new Style("redUI", style);
        style.italic = true;
        tto.styling.put(style.iD, style);

        style = new Style("redI", style);
        style.underline = false;
        tto.styling.put(style.iD, style);

        style = new Style("yellow");
        style.color = Style.getRGBValue("name", "yellow");
        tto.styling.put(style.iD, style);

        style = new Style("yellowU", style);
        style.underline = true;
        tto.styling.put(style.iD, style);

        style = new Style("yellowUI", style);
        style.italic = true;
        tto.styling.put(style.iD, style);

        style = new Style("yellowI", style);
        style.underline = false;
        tto.styling.put(style.iD, style);

        style = new Style("magenta");
        style.color = Style.getRGBValue("name", "magenta");
        tto.styling.put(style.iD, style);

        style = new Style("magentaU", style);
        style.underline = true;
        tto.styling.put(style.iD, style);

        style = new Style("magentaUI", style);
        style.italic = true;
        tto.styling.put(style.iD, style);

        style = new Style("magentaI", style);
        style.underline = false;
        tto.styling.put(style.iD, style);

    }

}
