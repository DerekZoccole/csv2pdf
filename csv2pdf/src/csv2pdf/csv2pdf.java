package csv2pdf;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.adobe.fdf.FDFDoc;
import com.adobe.fdf.exceptions.FDFException;

// csv2pdf Application
// 
// Programmatically converts the last modified (or newest downloaded) file into a form fillable pdf file.
//
// Input
//
// CSV files only
//
// Output
//
// PDF file with filled in form fields
//
// Function
//
// Takes a csv file from the OZHI web interface and converts it to fdf file format then auto fills a certain pdf form

public class csv2pdf {	
	// Names of pdf files
	final static String[] pdfNames = {"ERP_progress_inspection_form_bilingual.pdf", "Retrofit_progress_inspection_form_bilingual.pdf", "rrap_progress_inspection_form_bilingual.pdf"};

	// Names of directories most used for downloading
	final static String[] dirNames = {System.getProperty("user.home") + "/Downloads/", System.getProperty("user.home") + "/Documents/", System.getProperty("user.home") + "/Desktop/"};
	
	// Name of directory the program is run in
	final static String directory = "C://Program Files//OZHI-PDF-CREATOR//";

	// Variable names for populating pdf forms
	static String finalCity, streetNumber;
	
	public static void main(String[] args) {
		
		// Local variables
		String[] headers = new String[22];
		List<String[]> data = new ArrayList<String[]>();
		List<String[]> finalData = new ArrayList<String[]>();
		String filename = "temp";
		
		String[] buttonText = {"OK"};
		JPanel panel = new JPanel();
		JLabel label = new JLabel("No error");
		
		if (!checkDirectoryLocation(directory)) {			
			label.setText("C:/Program Files/OZHI-PDF-CREATOR/ directory does not exist.  Please rerun setup.");
			panel.add(label);
			JOptionPane.showOptionDialog(null, panel, "Error Message", JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, buttonText, buttonText[0]);
			return;
		}
		
		// Find newest file
		File file = findDirectory();
		
		// Check to see if file has extension of CSV, otherwise do nothing
		if (!filenameExtension(file).equals("csv")) {			
			label.setText("Not a csv file, download the excel file and rerun this program.");
			panel.add(label);
			JOptionPane.showOptionDialog(null, panel, "Error Message", JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, buttonText, buttonText[0]);
			return;
		}
		else {			
			if (file != null) {
				try {
					// Reader parses csv file into an array of String[]
					CSVReader reader = new CSVReader(new FileReader(file.getAbsolutePath()));
					String[] nextLine;
					
					// Store headers
					headers = reader.readNext();
					
					// Store the rest of the data 
					while ((nextLine = reader.readNext()) != null) {
						data.add(nextLine);
					}
					reader.close();
				}
				catch (IOException e) {			
					e.printStackTrace();	
					label.setText("CSVReader creation error.");
					panel.add(label);
					JOptionPane.showOptionDialog(null, panel, "Error Message", JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, buttonText, buttonText[0]);
					return;
				}
			}
		}
		
		if (headers != null && data != null) {
			
			for (int i = 0; i < headers.length; i++) {		
				
				// Checks for key header names
				if (headers[i].equals("Region") || 
					headers[i].equals("Service") ||
					headers[i].equals("Address") || 
					headers[i].equals("CMHC Number")) {
					
					String[] s = new String[data.size() + 1];
					
					// Stores header
					s[0] = headers[i];
					
					// Stores the column under the header
					for (int j = 0; j < data.size(); j++) {
						s[j + 1] = data.get(j)[i];
					}
					
					// Stores the final column into a final data set
					finalData.add(s);
				}
			}
		}
		
		// Creates FDF file
		try {
			FDFDoc doc = null;
			doc = new FDFDoc();
			
			for (int i = 1; i < finalData.get(0).length ; i++) {
				// Creates form values and sets them
				doc.SetValue("Province", getProvince(finalData.get(0)[i]));
				doc.SetValue("Address", checkForComma(finalData.get(2)[i]) ? parseAddress(finalData.get(2)[i]) : parseStreet(finalData.get(2)[i]));
				doc.SetValue("City", checkForComma(finalData.get(2)[i]) ? finalCity : "");
				doc.SetValue("Street No", streetNumber != null ? streetNumber : "");
				doc.SetValue("CMHC Account No", parseCMHC(finalData.get(3)[i]));
				
				if(!checkPDFLocation(directory + getService(finalData.get(1)[i]))) {						
					label.setText("PDF file not found.  Please reinstall.");
					panel.add(label);
					JOptionPane.showOptionDialog(null, panel, "Error Message", JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, buttonText, buttonText[0]);
					return;
				}
				
				// Uses a pdf to fill forms
				doc.SetFile(directory + getService(finalData.get(1)[i]));
				
				// Filename of fdf file
				filename += Integer.toString(i);
				filename += ".fdf";
				
				// Saves fdf file
				doc.Save(directory + "temp/" + filename);
				
				// Opens a new instance of each pdf with new acrobat window
				String s = "acrobat /n " + directory + "temp/" + filename;
				@SuppressWarnings("unused")
				Process pr = Runtime.getRuntime().exec(s);

				filename = "temp";
			}
		}
		catch (FDFException e) {		
			e.printStackTrace();		
			label.setText("FDF creation error");
			panel.add(label);
			JOptionPane.showOptionDialog(null, panel, "Error Message", JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, buttonText, buttonText[0]);
			return;
		}
		catch (IOException e) {			
			e.printStackTrace();	
			label.setText("IO error");
			panel.add(label);
			JOptionPane.showOptionDialog(null, panel, "Error Message", JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, buttonText, buttonText[0]);
			return;
		}
		
/*		try {
			@SuppressWarnings("unused")
			Process pr = Runtime.getRuntime().exec("del *.fdf");
		}
		catch (IOException e) {			
			e.printStackTrace();
			label.setText("IO error");
			panel.add(label);
			JOptionPane.showOptionDialog(null, panel, "Error Message", JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, buttonText, buttonText[0]);
			return;
		}*/
	}	

	// Function - getNewestFile
	//
	// Input - String - directory path
	// Output - File - newest file in directory path
	//
	// Finds newest file that has been downloaded in a specific directory path by comparing each file's last modified entry
	private static File getNewestFile(String dirPath) {
		
		// Makes file from directory path
		File dir = new File(dirPath);
		
		// List of files from directory path
		File[] files = dir.listFiles();
		
		// Checks if file is empty or does not exist
		if (files == null || files.length == 0) {
			return null;
		}
		
		// Compares each file using last modified date of file
		File newestFile = files[0];
		for (int i = 1; i < files.length; i++) {
			if (newestFile.lastModified() < files[i].lastModified()) {
				newestFile = files[i];
			}
		}
		return newestFile;
	}
	
	// Function - filenameExtension
	//
	// Input - File - file to find extension
	// Output - String - filename extension
	//
	// Finds the filename extension of a given file
	private static String filenameExtension(File file) {
		// Finds last index of .
		int a = file.getAbsolutePath().lastIndexOf('.');
		
		// Just in case the . is not the filename extension - For example ../test.test/test.txt
		int b = Math.max(file.getAbsolutePath().lastIndexOf('/'), file.getAbsolutePath().lastIndexOf('\\'));
		
		// Checks if the last . is not before any other .
		if (a > b) {
			return file.getAbsolutePath().substring(a + 1);
		}
		return "";
	}
	
	// Function - getProvince
	//
	// Input - String - name of province
	// Output - String - abbreviation of province
	//
	// Abbreviates long form of province into an abbreviated form
	private static String getProvince(String prov) {
		switch (prov) {
			case "Alberta":
				return "AB";
			case "British Columbia":
				return "BC";
			case "Manitoba":
				return "MB";
			case "New Brunswick":
				return "NB";
			case "Newfoundland":
				return "NL";
			case "Nova Scotia":
				return "NS";
			case "Ontario":
				return "ON";
			case "Prince Edward Island":
				return "PE";
			case "Quebec":
				return "QC";
			case "Saskatchewan":
				return "SK";
			case "Yukon":
				return "YK";
			default:
				return "";
		}
	}
	
	// Function - getService
	//
	// Input - String - type of service requested
	// Output - String - filename of service requested
	//
	// Finds which service is being requested and returns the appropriate file
	private static String getService(String service) {
		switch (service) {
			case "ERP Final":
				return pdfNames[0];
			case "Retrofit Final":
				return pdfNames[1];
			case "RRAP Final":
				return pdfNames[2];
			default:
				return pdfNames[1];
		}
	}
	
	// Function - findDirectory
	//
	// Input - none
	// Output - File - newest file in directory path
	//
	// Finds the directory of the newest file downloaded and returns the file
	private static File findDirectory() {
		// Stores all download directories in array
		File[] files = {getNewestFile(dirNames[0]), getNewestFile(dirNames[1]), getNewestFile(dirNames[2])};
		
		// Checks all last modified files against each other to see which one is newest.
		File newestFile = files[0];
		for (int i = 1; i < files.length; i++) {
			if (newestFile.lastModified() < files[i].lastModified()) {
				newestFile = files[i];
			}
		}
		
		return newestFile;
	}
	
	// Function - parseAddress
	//
	// Input - String - unformatted address
	// Output - String - formatted address
	//
	// Parses the address returning the parsed address without number and city and province
	private static String parseAddress(String address) {
		final char comma = ',';
		int count = 0;
		
		// Counts how many commas
		for (int i = 0; i < address.length(); i++) {
			if (address.charAt(i) == comma) {
				count++;
			}
		}
		
		if (count != 0) {
			// If there is one comma, parse the city and return the street
			if (count == 1) {
				finalCity = address.substring(address.lastIndexOf(comma) + 1).replaceAll("\\s+", "");
				return parseStreet(address.substring(0, address.lastIndexOf(comma)));
			}
			// If there is two commas, parse the city, return the street and ignore the province
			else if (count == 2) {
				finalCity = address.substring(address.indexOf(comma) + 1, address.lastIndexOf(comma)).replaceAll("\\s+", "");
				return parseStreet(address.substring(0, address.indexOf(comma)));
			}
		}
		
		// If there is no comma's return an empty string
		return "";
	}
	
	// Function - checkForComma
	//
	// Input - String - address
	// Output - boolean - returns answer
	//
	// Checks a string for an appearing comma, if found returns true
	private static boolean checkForComma(String address) {
		final char comma = ',';
		
		// Checks the string for any occurrences of a comma and returns true
		for (int i = 0; i < address.length(); i++) {
			if (address.charAt(i) == comma) {
				return true;
			}
		}
		// No commas found return false
		return false;
	}
	
	// Function - parseStreet
	//
	// Input - String - address without city or province
	// Output - String - street number
	//
	// Parses street number from address
	private static String parseStreet(String address) {
		// Checks for keywords to parse address from second occurrence of a space
		if (address.substring(0, address.indexOf(' ')).equals("House") ||
		    address.substring(0, address.indexOf(' ')).equals("Lot") ||
		    address.substring(0, address.indexOf(' ')).equals("Unit") ||
		    address.substring(0, address.indexOf(' ')).equals("Mile")) {
			streetNumber = address.substring(0, ordinalIndexOf(address, " ", 2));
			return (streetNumber != null) ? address.substring(ordinalIndexOf(address, " ", 2) + 1) : "";
		}
		// Checks to see if the first character is a digit to insure address has a street number infront of it
		else if (Character.isDigit(address.substring(0, address.indexOf(' ')).charAt(0))) {
			streetNumber = address.substring(0, address.indexOf(' '));
			return address.substring(address.indexOf(' ') + 1);
		}
		// Otherwise just return the address
		else {
			return address;
		}
	}
	
	// Function - ordinalIndexOf
	//
	// Input - String - string to find index of	
	// 		   String - substring to find within string
	//		   int - number of occurrences to find before stopping
	// Output - int - position of ordinal index of substring
	//
	// Finds the ordinal index of a substring within another string
	// For example, find the second , in a string --> ordinalIndexOf(someString, ",", 2)
	private static int ordinalIndexOf(String str, String substr, int n) {
		// Stores first occurrence of substring
		int pos = str.indexOf(substr);
		
		// Loops until the last occurrence is found or until the end of the string is found
		while (--n > 0 && pos != -1) 
			pos = str.indexOf(substr, pos + 1);
		return pos;
	}
	
	// Function - parseCMHC
	//
	// Input - String - CMHC 8 digit number
	//
	// Output - String - CMHC 8 digit number without special characters
	//
	// Takes a CMHC number and removes spaces and/or dashes for a number only CMHC number
	// NOTE:  Some CMHC numbers are longer than 8 digits but will only print out 8 digits on the pdf form
	private static String parseCMHC(String number) {
		// Checks CMHC number for any occurrence of a dash and replaces with nothing
		if (number.contains("-")) {
			number = number.replaceAll("-", "");
		}
		
		// Checks CMHC number for any occurrence of a space and replaces with nothing
		if (number.contains(" ")) {
			number = number.replaceAll(" ", "");
		}
		
		// Returns a number only format
		return number;
	}		
	
	// Function - checkPDFLocation
	//
	// Input - String - file directory path
	//
	// Output - boolean
	//
	// Checks to see if file is in location. Returns true if found, otherwise returns false.
	
	private static boolean checkPDFLocation(String location) {	
		return new File(location).exists() && !(new File(location).isDirectory());
	}
	
	// Function - checkDirectoryLocation
	//
	// Input - String - directory path
	//
	// Output - boolean
	//
	// Checks to see if directory is in location. Returns true if found, otherwise returns false.
	
	private static boolean checkDirectoryLocation(String location) {	
		return new File(location).exists() && new File(location).isDirectory();
	}
}
