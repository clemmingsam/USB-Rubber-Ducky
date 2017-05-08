// File:         Encoder.java
// Created:      8/10/2011
// Original Author:Jason Appelbaum Jason@Hak5.org 
// Author:       Dnucna
// Modified:     8/18/2012
// Modified:	 11/9/2013 midnitesnake "added COMMAND-OPTION"
// Modified:     1/3/2013 midnitesnake "added COMMAND"
// Modified:     1/3/2013 midnitesnake "added REPEAT X"
// Modified:	 2/5/2013 midnitesnake "added ALT-SHIFT"
// Modified:     4/18/2013 midnitesnake "added more user feedback"
// Modified:	 5/2/2013 midnitesnake "added skip over empty lines"
// Modified:     1/12/2014 Benthejunebug "added ALT-TAB"
// Modified:	 9/13/2016 rbeede "added STRING_DELAY n text"
// Modified: 	 05/07/2017 clemmingsam "refactored code"

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;

import java.util.Properties;

public class Encoder {
        /* contains the keyboard configuration */
        private static Properties keyboardProps = new Properties();
        /* contains the language layout */
        private Properties layoutProps = new Properties();
        
        public final String version = "2.6.4";
        public Boolean debug = false;
    
        public void loadProperties (String lang){
            InputStream in;
            ClassLoader loader = ClassLoader.getSystemClassLoader ();
            
            try {
                    in = loader.getResourceAsStream("keyboard.properties");
                    if(in != null) {
                            keyboardProps.load(in);
                            in.close();
                            System.out.println("Loading Keyboard File .....\t[ OK ]");
                    }
                    
                    else {
                            System.out.println("Error with keyboard.properties!");
                            System.exit(0);
                    }
            }
            
            catch (IOException e) {
                    System.out.println("Error with keyboard.properties!");
            }
                    
            try {
                in = loader.getResourceAsStream(lang + ".properties");
                
                if(in != null) {
                        layoutProps.load(in);
                        in.close();
                        System.out.println("Loading Language File .....\t[ OK ]");
                }
                
                else {
                    if(new File(lang).isFile()) {
                            layoutProps.load(new FileInputStream(lang));
                            System.out.println("Loading Language File .....\t[ OK ]");
                    }
                    
                    else {
                            System.out.println("External layout.properties non found!");
                            System.exit(0);
                    }
                }
            } 
            
            catch (IOException e) {
                    System.out.println("Error with layout.properties!");
                    System.exit(0);
            }
        }
        
        public void encodeToFile(String inStr, String fileDest) {
            List<Byte> file = new ArrayList<Byte>();
            
            inStr = inStr.replaceAll("\\r", ""); // CRLF Fix
            
            ArrayList<String> instructions = new ArrayList<String>(Arrays.asList(inStr.split("\n")));
            
            ArrayList<String> prev_instruction = null;
            ArrayList<String> instruction = null;        

            int defaultDelay = 0;
            int loop = 0;
            boolean repeat = false;
            
            System.out.println("Loading DuckyScript .....\t[ OK ]");
            
            if(debug) {
            	System.out.println("\nParsing Commands:");
            }
            
            for (String line : instructions) {
	            try {
                    boolean delayOverride = false;
                    String commentCheck = line.substring(0, 2);
                    
                    if (commentCheck.equals("//")) {
                            continue;
                    }
                    
                    if (line.equals("\n")) {
                    		continue;
                    }
                    
                    instruction = new ArrayList<String>(Arrays.asList(line.split(" ", 2)));
                    
                    if (instructions.indexOf(line) > 0) {
                		prev_instruction = new ArrayList<String>(Arrays.asList(instructions.get(instructions.indexOf(instruction) -1 ).split(" ", 2)));
                    }
                    
                    else {
                		prev_instruction = new ArrayList<String>(Arrays.asList(line.split(" ", 2)));
                    }

            		prev_instruction.get(0).trim();
            		
            		if (prev_instruction.size() == 2) {
                    		prev_instruction.get(1).trim();
            		}
                    		
                    instruction.get(0).trim();

                    if (instruction.size() == 2) {
                            instruction.get(1).trim();
                    }

					if (instruction.get(0).equals("REM")){
						continue;
					}
					
					if (instruction.get(0).equals("REPEAT")){
						loop = Integer.parseInt(instruction.get(1).trim());
						repeat = true;
					}
					
					else {
						repeat=false;
						loop=1;
					}
					
					while(loop > 0){
						if (repeat){
							instruction = prev_instruction;
							//System.out.println(Integer.toString(instruction.length));
						}
						
						if (debug) { 
							System.out.println(instruction.toString());
						}
						
                    	if (instruction.get(0).equals("DEFAULT_DELAY")|| instruction.get(0).equals("DEFAULTDELAY")) {
                    		defaultDelay = Integer.parseInt(instruction.get(1).trim());
                    		delayOverride = true;
                    	}
                    	
                    	else if (instruction.get(0).equals("DELAY")) {
                            int delay = Integer.parseInt(instruction.get(1).trim());
                            
                            while (delay > 0) {
                                file.add((byte) 0x00);
                                if (delay > 255) {
                                        file.add((byte) 0xFF);
                                        delay = delay - 255;
                                } else {
                                        file.add((byte) delay);
                                        delay = 0;
                                }
                            }
                            
                            delayOverride = true;
                    	}
                    	
                    	else if (instruction.get(0).equals("STRING")) {
                            for (char character : instruction.get(1).toCharArray()) {
                            	addBytes(file, charToBytes(character));
                            }
                    	}
                    	
                    	else if (instruction.get(0).equals("STRING_DELAY")) {
                    		final String[] twoOptions = instruction.get(1).split(" ", 2);
                    		final int delayMillis = Integer.parseInt(twoOptions[0].trim());
                    		final String userText = twoOptions[1].trim();
                    		
                    		if(debug) {
                    			System.out.println(delayMillis);
                    			System.out.println(userText);
                    		}
                    		
                            for (char character : userText.toCharArray()) {
                                addBytes(file,charToBytes(character));
                                
                                // Now insert the delay before the next character (and after the last is provided)
                                for(int counter = delayMillis; counter > 0; counter -= 0xFF) {
                                	file.add((byte) 0x00);
                                	if(counter > 0xFF) {
                                		file.add((byte) 0xFF);
                                	} else {
                                		file.add((byte) counter);  // Last one
                                	}
                                }
                            }
                    	}
                    	
                    	else if (instruction.get(0).equals("CONTROL") || instruction.get(0).equals("CTRL")) {
                            if (instruction.size() != 1){
                                    file.add(strInstrToByte(instruction.get(1)));
                                    file.add(strToByte(keyboardProps.getProperty("MODIFIERKEY_CTRL")));
                            } 
                            
                            else {
                                    file.add(strToByte(keyboardProps.getProperty("KEY_LEFT_CTRL")));
                                    file.add((byte) 0x00);
                            }                               
                    	} 
                    	
                    	else if (instruction.get(0).equals("ALT")) {
                            if (instruction.size() != 1){
                                    file.add(strInstrToByte(instruction.get(1)));
                                    file.add(strToByte(keyboardProps.getProperty("MODIFIERKEY_ALT")));
                            } 
                            
                            else {
                                    file.add(strToByte(keyboardProps.getProperty("KEY_LEFT_ALT")));
                                    file.add((byte) 0x00);
                            }
                    	} 
                    	
                    	else if (instruction.get(0).equals("SHIFT")) {
                            if (instruction.size() != 1) {
                                    file.add(strInstrToByte(instruction.get(1)));
                                    file.add(strToByte(keyboardProps.getProperty("MODIFIERKEY_SHIFT")));
                            } 
                            
                            else {
                                    file.add(strToByte(keyboardProps.getProperty("KEY_LEFT_SHIFT")));
                                    file.add((byte) 0x00);
                            }
                    	} 
                    	
                    	else if (instruction.get(0).equals("CTRL-ALT")) {
                            if (instruction.size() != 1) {
                                    file.add(strInstrToByte(instruction.get(1)));
                                    file.add((byte) (strToByte(keyboardProps.getProperty("MODIFIERKEY_CTRL"))
                                                    | strToByte(keyboardProps.getProperty("MODIFIERKEY_ALT"))));
                            } 
                            
                            else {
                                    continue;
                            }
                    	} 
                    	
                    	else if (instruction.get(0).equals("CTRL-SHIFT")) {
                            if (instruction.size() != 1) {
                                    file.add(strInstrToByte(instruction.get(1)));
                                    file.add((byte) (strToByte(keyboardProps.getProperty("MODIFIERKEY_CTRL"))
                                                    | strToByte(keyboardProps.getProperty("MODIFIERKEY_SHIFT"))));
                            }
                            
                            else {
                                    continue;
                            }
                        } 
                    	
                    	else if (instruction.get(0).equals("COMMAND-OPTION")) {
                            if (instruction.size() != 1) {
                                    file.add(strInstrToByte(instruction.get(1)));
                                    file.add((byte) (strToByte(keyboardProps.getProperty("MODIFIERKEY_KEY_LEFT_GUI"))
                                                    | strToByte(keyboardProps.getProperty("MODIFIERKEY_ALT"))));
                            }
                            
                            else {
                                    continue;
                            }
                    	} 
                    	
                    	else if (instruction.get(0).equals("ALT-SHIFT")) {
                            if (instruction.size() != 1) {
                                file.add(strInstrToByte(instruction.get(1)));
                                file.add((byte) (strToByte(keyboardProps.getProperty("MODIFIERKEY_LEFT_ALT"))
                                                    | strToByte(keyboardProps.getProperty("MODIFIERKEY_SHIFT"))
                                                )
                                        );
                            } 
                            
                            else {
                            	file.add(strToByte(keyboardProps.getProperty("KEY_LEFT_ALT")));
                                file.add((byte) (strToByte(keyboardProps.getProperty("MODIFIERKEY_LEFT_ALT")) 
                                					| strToByte(keyboardProps.getProperty("MODIFIERKEY_SHIFT"))
                                				)
                                		);
                            }
                    	} 
                    	
                    	else if (instruction.get(0).equals("ALT-TAB")){
                            if (instruction.size() == 1) {
                                file.add(strToByte(keyboardProps.getProperty("KEY_TAB")));
                                file.add(strToByte(keyboardProps.getProperty("MODIFIERKEY_LEFT_ALT")));
                            } 
                            
                            else{
                                // do something?
                            }
                        } 
                    	
                    	else if (instruction.get(0).equals("REM")) {
                            /* no default delay for the comments */
                            delayOverride = true;
                            continue;
                    	} 
                    	
                    	else if (instruction.get(0).equals("WINDOWS") || instruction.get(0).equals("GUI")) {
                            if (instruction.size() == 1) {
	                            file.add(strToByte(keyboardProps.getProperty("MODIFIERKEY_LEFT_GUI")));
	                            file.add((byte) 0x00);
                            } 
                            
                            else {
                                file.add(strInstrToByte(instruction.get(1)));
                                file.add(strToByte(keyboardProps.getProperty("MODIFIERKEY_LEFT_GUI")));
                            }
                    	} 
                    	
                    	else if (instruction.get(0).equals("COMMAND")){
                            if (instruction.size() == 1) {
                                file.add(strToByte(keyboardProps.getProperty("KEY_COMMAND")));
                                file.add((byte) 0x00);
                            } 
                            
                            else {
	                            file.add(strInstrToByte(instruction.get(1)));
	                            file.add(strToByte(keyboardProps.getProperty("MODIFIERKEY_LEFT_GUI")));
                            }
                    	}
                    	
                    	else {
                            /* treat anything else as a key */
                            file.add(strInstrToByte(instruction.get(0)));
                            file.add((byte) 0x00);
                    	}
                    	
                    	loop--;
					}
                    // Default delay
                    if (!delayOverride & defaultDelay > 0) {
                        int delayCounter = defaultDelay;
                        
                        while (delayCounter > 0) {
                            file.add((byte) 0x00);
                            
                            if (delayCounter > 255) {
                                    file.add((byte) 0xFF);
                                    delayCounter = delayCounter - 255;
                            } 
                            
                            else {
                                    file.add((byte) delayCounter);
                                    delayCounter = 0;
                            }
                        }
                    }
                }
	            
                catch (Exception e) {
                        System.out.println("Error on Line: " + (instructions.indexOf(line) + 1));
                        e.printStackTrace();
                }
            }

            // Write byte array to file
            byte[] data = new byte[file.size()];
            
            for (int i = 0; i < file.size(); i++) {
            	data[i] = file.get(i);
            }
            
            try {
                File someFile = new File(fileDest);
                FileOutputStream fos = new FileOutputStream(someFile);
                
                fos.write(data);
                fos.flush();
                
                fos.close();
                
                System.out.println("DuckyScript Complete.....\t[ OK ]\n");
            } 
            
            catch (Exception e) {
                    System.out.print("Failed to write hex file!");
            }
        }

        private void addBytes(List<Byte> file, byte[] byteTab){
            for(int i=0;i<byteTab.length;i++) {
            	file.add(byteTab[i]);
            }
            
            if(byteTab.length % 2 != 0) {
            	file.add((byte) 0x00);
            }
        }
        
        private byte[] charToBytes (char c){
            return codeToBytes(charToCode(c));
        }
        
        private String charToCode (char c){
            String code;
            
            if(c<128) {
                code = "ASCII_" + Integer.toHexString(c).toUpperCase();
            }
            
            else if (c<256) {
                code = "ISO_8859_1_" + Integer.toHexString(c).toUpperCase();
            }
            
            else {
                code = "UNICODE_" + Integer.toHexString(c).toUpperCase();
            }
            
            return code;
        }
        
        private byte[] codeToBytes (String str){
        	byte[] byteTab = null;
        	
            if(layoutProps.getProperty(str) != null){
                ArrayList<String> keys = new ArrayList<String>(Arrays.asList(layoutProps.getProperty(str).split(",")));
                byteTab = new byte[keys.size()];
                
                for(String key : keys){
                    if(keyboardProps.getProperty(key) != null) {
                    	byteTab[keys.indexOf(key)] = strToByte(keyboardProps.getProperty(key.trim()).trim());
                    }
                    
                    else if(layoutProps.getProperty(key) != null) {
                    	byteTab[keys.indexOf(key)] = strToByte(layoutProps.getProperty(key.trim()).trim());
                    }
                    
                    else {
                        System.out.println("Key not found:"+ key.trim());
                        byteTab[keys.indexOf(key)] = (byte) 0x00;
                    }
                }
                
                return byteTab;
            }
            
            else {
                System.out.println("Char not found:"+str);
                
                byteTab = new byte[1];
                byteTab[0] = (byte) 0x00;
                
                return byteTab;
            }
        }
        
        private byte strToByte(String str) {
            if(str.startsWith("0x")) {
            	return (byte)Integer.parseInt(str.substring(2,16));
            }
            
            else {
            	return (byte)Integer.parseInt(str);
            }
        }
        
        private byte strInstrToByte(String instruction){
            instruction = instruction.trim();
            
            if(keyboardProps.getProperty("KEY_"+instruction)!=null)
                    return strToByte(keyboardProps.getProperty("KEY_" + instruction));
            
            /* instruction different from the key name */
            if(instruction.equals("ESCAPE"))
                    return strInstrToByte("ESC");
            if(instruction.equals("DEL"))
                    return strInstrToByte("DELETE");
            if(instruction.equals("BREAK"))
                    return strInstrToByte("PAUSE");
            if(instruction.equals("CONTROL"))
                    return strInstrToByte("CTRL");
            if(instruction.equals("DOWNARROW"))
                    return strInstrToByte("DOWN");
            if(instruction.equals("UPARROW"))
                    return strInstrToByte("UP");
            if(instruction.equals("LEFTARROW"))
                    return strInstrToByte("LEFT");
            if(instruction.equals("RIGHTARROW"))
                    return strInstrToByte("RIGHT");
            if(instruction.equals("MENU"))
                    return strInstrToByte("APP");
            if(instruction.equals("WINDOWS"))
                    return strInstrToByte("GUI");
            if(instruction.equals("PLAY") || instruction.equals("PAUSE"))
                    return strInstrToByte("MEDIA_PLAY_PAUSE");
            if(instruction.equals("STOP"))
                    return strInstrToByte("MEDIA_STOP");
            if(instruction.equals("MUTE"))
                    return strInstrToByte("MEDIA_MUTE");
            if(instruction.equals("VOLUMEUP"))
                    return strInstrToByte("MEDIA_VOLUME_INC");
            if(instruction.equals("VOLUMEDOWN"))
                    return strInstrToByte("MEDIA_VOLUME_DEC");
            if(instruction.equals("SCROLLLOCK"))
                    return strInstrToByte("SCROLL_LOCK");
            if(instruction.equals("NUMLOCK"))
                    return strInstrToByte("NUM_LOCK");
            if(instruction.equals("CAPSLOCK"))
                    return strInstrToByte("CAPS_LOCK");
            /* else take first letter */
            return charToBytes(instruction.charAt(0))[0];
        }
}
