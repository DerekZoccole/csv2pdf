package csv2pdf;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// csv2pdf Application
// 
// Progammically converts the last modified (or newest downloaded) file into a form fillable pdf file.
//
// Input
//
// CSV files only

public class csv2pdf {	
	final static int MAXIMUM_NUMBER_PDFS = 100;
	public static void main(String[] args) {
		String home = System.getProperty("user.home") + "/Downloads/";
		String[] headers = new String[22];
		List<String[]> data = new ArrayList<String[]>();
		List<String[]> finalData = new ArrayList<String[]>();
		
		// Find newest file
		File file = getNewestFile(home);
		
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
			
	}		
	
	private static File getNewestFile(String dirPath) {
		File dir = new File(dirPath);
		File[] files = dir.listFiles();
		if (files == null || files.length == 0) {
			return null;
		}
		
		File newestFile = files[0];
		for (int i = 1; i < files.length; i++) {
			if (newestFile.lastModified() < files[i].lastModified()) {
				newestFile = files[i];
			}
		}
		return newestFile;
	}
	
	private static String filenameExtension(File file) {
		int a = file.getAbsolutePath().lastIndexOf('.');
		int b = Math.max(file.getAbsolutePath().lastIndexOf('/'), file.getAbsolutePath().lastIndexOf('\\'));
		
		if (a > b) {
			return file.getAbsolutePath().substring(a + 1);
		}
		return "";
	}
}
