import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;


public class Encoder_CLI {
	 public static void main(String[] args) {
		 Encoder encoder = new Encoder();
		 
         String helpStr = "Hak5 Duck Encoder "+ encoder.version + "\n\n"
                 + "Usage: duckencode -i [file ..]\t\t\tencode specified file\n"
                 + "   or: duckencode -i [file ..] -o [file ..]\tencode to specified file\n\n"
                 + "Arguments:\n"
                 + "   -i [file ..] \t\tInput File\n"
                 + "   -o [file ..] \t\tOutput File\n"
                 + "   -l [file ..] \t\tKeyboard Layout (us/fr/pt or a path to a properties file)\n\n"
                 + "Script Commands:\n"
                 + "   ALT [key name] (ex: ALT F4, ALT SPACE)\n"
                 + "   CTRL | CONTROL [key name] (ex: CTRL ESC)\n"
                 + "   CTRL-ALT [key name] (ex: CTRL-ALT DEL)\n"
                 + "   CTRL-SHIFT [key name] (ex: CTRL-SHIFT ESC)\n"
                 + "   DEFAULT_DELAY | DEFAULTDELAY [Time in millisecond] (change the delay between each command)\n"
                 + "   DELAY [Time in millisecond] (used to overide temporary the default delay)\n"
                 + "   GUI | WINDOWS [key name] (ex: GUI r, GUI l)\n"
                 + "   REM [anything] (used to comment your code, no obligation :) )\n"
                 + "   ALT-SHIFT (swap language)\n"
                 + "   SHIFT [key name] (ex: SHIFT DEL)\n"
                 + "   STRING [any character of your layout]\n"
                 + "   STRING_DELAY [Number] [any character of your layout]	(Number is ms delay between each character)\n"
                 + "   REPEAT [Number] (Repeat last instruction N times)\n"
                 + "   [key name] (anything in the keyboard.properties)";                        
		
		 String inputFile = null;
		 String outputFile = null;
		 String layoutFile = null;
		
		 // This code below, is the same as code 92-93
		 if (args.length == 0) {
	         System.out.println(helpStr);
	         System.exit(0);
		 }
		 
		 ArrayList<String> arguments = new ArrayList<String>(Arrays.asList(args));
		 
		 for (String argument : args) {
	         if (argument.equals("--gui") || argument.equals("-g")) {
	             System.out.println("Launch GUI");
	         }
	         
	         else if (argument.equals("--help") || argument.equals("-h")) {
	             System.out.println(helpStr);
	         }
	         
	         else if (argument.equals("-i")) {
	             // encode file
	             inputFile = arguments.get(arguments.indexOf(argument) + 1);
	         }
	         
	         else if (argument.equals("-o")) {
	             // output file
	             outputFile = arguments.get(arguments.indexOf(argument) + 1);
	         }
	         
	         else if (argument.equals("-l")) {
	             // output file
	             layoutFile = arguments.get(arguments.indexOf(argument) + 1);
	         }
	         
	         else if (argument.equals("-d")) {
	             // output file
	             encoder.debug = true;
	         }
	         
	         else {
	     		// As this(refer to lines 67-70
	             System.out.println(helpStr);
	             System.exit(0);
	         }
		 }
		     
		 System.out.println("Hak5 Duck Encoder "+ encoder.version +"\n");
		 
		 if (inputFile != null) {
	         String scriptStr = null;
	
	         if (inputFile.contains(".rtf")) {
                 try {
                     FileInputStream stream = new FileInputStream(inputFile);
                     RTFEditorKit kit = new RTFEditorKit();
                     Document doc = kit.createDefaultDocument();
                     
                     kit.read(stream, doc, 0);
                     scriptStr = doc.getText(0, doc.getLength());
                     
                     System.out.println("Loading RTF .....\t\t[ OK ]");       
                 }
                 
                 catch (IOException e) {
                     System.out.println("Error with input file!");
                 }
                 
                 catch (BadLocationException e) {
                     System.out.println("Error with input file!");
                 }    
	         }
	         
	         else {
	        	 DataInputStream in = null;
	                 
	             try {
	                     File f = new File(inputFile);
	                     byte[] buffer = new byte[(int) f.length()];
	                     
	                     in = new DataInputStream(new FileInputStream(f));
	                     
	                     in.readFully(buffer);
	                     scriptStr = new String(buffer);
	                     
	                     System.out.println("Loading File .....\t\t[ OK ]");
	             } 
	             
	             catch (IOException e) {
	                     System.out.println("Error with input file!");
	             } 
	             
	             finally {
	             	try {
	             		in.close();
	                 } 
	             	
	             	catch (IOException e) { /* ignore it */
	                     
	             	}
	             }
	         }
	         
	         encoder.loadProperties((layoutFile == null) ? "us" : layoutFile);
	         
	         encoder.encodeToFile(scriptStr, (outputFile == null) ? "inject.bin" : outputFile);
		 }	     
	 }	 
}
