package csv2pdf;

import java.io.IOException;

import csv2pdf.CSVReaderNullFieldIndicator;
import static csv2pdf.CSVReaderNullFieldIndicator.NEITHER;

public interface ICSVParser {
	char DEFAULT_SEPARATOR = ',';
	int INITIAL_READ_SIZE = 1024;
	int READ_BUFFER_SIZE = 128;
	char DEFAULT_QUOTE_CHARACTER = '"';
	char DEFAULT_ESCAPE_CHARACTER = '\\';
	boolean DEFAULT_STRICT_QUOTES = false;
	boolean DEFAULT_IGNORE_LEADING_WHITESPACE = true;
	boolean DEFAULT_IGNORE_QUOTATIONS = false;
	char NULL_CHARACTER = '\0';
	CSVReaderNullFieldIndicator DEFAULT_NULL_FIELD_INDICATOR = NEITHER;
	
	char getSeparator();
	char getQuotechar();
	boolean isPending();
	String[] parseLineMulti(String nextLine) throws IOException;
	String[] parseLine(String nextLine) throws IOException;
	CSVReaderNullFieldIndicator nullFieldIndicator();
}
