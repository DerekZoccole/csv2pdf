package csv2pdf;

import java.awt.Desktop;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
	// Maximum number of pdfs that can be populated
	final static int MAXIMUM_NUMBER_PDFS = 100;
	
	// Names of pdf files
	final static String[] pdfNames = {"ERP_progress_inspection_form_bilingual.pdf", "Retrofit_progress_inspection_form_bilingual.pdf"};

	// Names of directories most used for downloading
	final static String[] dirNames = {System.getProperty("user.home") + "/Downloads/", System.getProperty("user.home") + "/Documents/", System.getProperty("user.home") + "/Desktop/"};

	// Variable names for populating pdf forms
	static String finalCity, streetNumber;
	
	public static void main(String[] args) {
		
		// Local variables
		String[] headers = new String[22];
		List<String[]> data = new ArrayList<String[]>();
		List<String[]> finalData = new ArrayList<String[]>();
		
		// Find newest file
		File file = findDirectory();
		
		// Check to see if file has extension of CSV, otherwise do nothing
		if (!filenameExtension(file).equals("csv")) {			
			System.out.println("Not a csv file, download the excel file and try again.");
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
					System.out.println("CSVReader creation error.");
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
					
					String[] s = new String[MAXIMUM_NUMBER_PDFS];
					
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
		
		try {
			FDFDoc doc = null;
			doc = new FDFDoc();
//			for (int i = 0; i < finalData.get(0).length; i++) {
//				for (int j = 1; j < finalData.size(); j++) {
					doc.SetValue("Province", getProvince(finalData.get(0)[1]));
					doc.SetValue("Address", checkForComma(finalData.get(2)[1]) ? parseAddress(finalData.get(2)[1]) : finalData.get(2)[1]);
					doc.SetValue("City", checkForComma(finalData.get(2)[1]) ? finalCity : "");
					doc.SetValue("Street No", streetNumber);
					doc.SetFile(dirNames[1] + getService(finalData.get(1)[1]));
					doc.Save("temp.fdf");
					File f = new File("temp.fdf");
					Desktop.getDesktop().open(f);
//				}
//			}
		}
		catch (FDFException e) {
			
		}
		catch (IOException e) {
			
		}
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
			case "Retrofit Final":
				return pdfNames[1];
			default:
				return pdfNames[0];
		}
	}
	
	// Function - findDirectory
	//
	// Input - none
	// Output - File - newest file in directory path
	//
	// Finds the directory of the newest file downloaded and returns the file
	private static File findDirectory() {
		File[] files = {getNewestFile(dirNames[0]), getNewestFile(dirNames[1]), getNewestFile(dirNames[2])};
		
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
		String temp = address;
		
		for (int i = 0; i < address.length(); i++) {
			if (address.charAt(i) == comma) {
				count++;
			}
		}
		
		if (count != 0) {
			if (count == 1) {
				finalCity = address.substring(address.lastIndexOf(comma) + 1).replaceAll("\\s+", "");
				return parseStreet(address.substring(0, address.lastIndexOf(comma)));
			}
			else if (count == 2) {
				temp = address.substring(address.lastIndexOf(comma));
			}
		}
		
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
		
		for (int i = 0; i < address.length(); i++) {
			if (address.charAt(i) == comma) {
				return true;
			}
		}
		return false;
	}
	
	// Function - parseStreet
	//
	// Input - String - address without city or province
	// Output - String - street number
	//
	// Parses street number from address
	private static String parseStreet(String address) {
		System.out.println(address.substring(0, address.indexOf(' ')));
		if (address.substring(0, address.indexOf(' ')) == "House" ||
		    address.substring(0, address.indexOf(' ')) == "Lot" ||
		    address.substring(0, address.indexOf(' ')) == "Unit") {
			streetNumber = address.substring(0, ordinalIndexOf(address, " ", 2));
			return address.substring(ordinalIndexOf(address, " ", 2) + 1);
		}
		else {
			streetNumber = address.substring(0, address.indexOf(' '));
			return address.substring(address.indexOf(' ') + 1);
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
		int pos = str.indexOf(substr);
		while (--n > 0 && pos != -1) 
			pos = str.indexOf(substr, pos + 1);
		return pos;
	}
}
