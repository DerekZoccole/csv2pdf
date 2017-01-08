package csv2pdf;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

// csv2pdf Application
// 
// Progammically converts the last modified (or newest downloaded) file into a form fillable pdf file.
//
// Input
//
// CSV files only

public class csv2pdf {
	public static void main(String[] args) {
		String home = System.getProperty("user.home") + "/Downloads/";
		String[] headers;
		List<String[]> data;
		
		File file = getNewestFile(home);
		
		if (!filenameExtension(file).equals("csv")) {			
			System.out.println("Not a csv file, download the excel file and try again.");
		}
		else {			
			if (file != null) {
				try {
					CSVReader reader = new CSVReader(new FileReader(file.getAbsolutePath()));
					String[] nextLine;
					reader.readNext();
					while ((nextLine = reader.readNext()) != null) {
						for (int i = 0; i < nextLine.length; i++) {
							System.out.println(nextLine[i]);
						}
					}
					reader.close();
				}
				catch (IOException e) {
					System.out.println("CSVReader creation error.");
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
