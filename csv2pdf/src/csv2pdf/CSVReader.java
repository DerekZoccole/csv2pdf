package csv2pdf;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CSVReader implements Closeable, Iterable<String[]>{
	public static final boolean DEFAULT_KEEP_CR = false;
	public static final boolean DEFAULT_VERIFY_READER = true;
	
	public static final int DEFAULT_SKIP_LINES = 0;
	public static final int READ_AHEAD_LIMIT = Character.SIZE / Byte.SIZE;
	protected ICSVParser parser;
	protected int skipLines;
	protected BufferedReader br;
	protected LineReader lineReader;
	protected boolean hasNext = true;
	protected boolean linesSkiped;
	protected boolean keepCR;
	protected boolean verifyReader;
	
	protected long linesRead = 0;
	protected long recordsRead = 0;
	
	public CSVReader(Reader reader) {
		this(reader, ICSVParser.DEFAULT_SEPARATOR, ICSVParser.DEFAULT_QUOTE_CHARACTER, ICSVParser.DEFAULT_ESCAPE_CHARACTER);
	}
	
	public CSVReader(Reader reader, char separator) {
		this(reader, separator, ICSVParser.DEFAULT_QUOTE_CHARACTER, ICSVParser.DEFAULT_ESCAPE_CHARACTER);
	}
	
	public CSVReader(Reader reader, char separator, char quotechar) {
		this(reader, separator, quotechar, ICSVParser.DEFAULT_ESCAPE_CHARACTER, DEFAULT_SKIP_LINES, ICSVParser.DEFAULT_STRICT_QUOTES);
	}
	
	public CSVReader(Reader reader, char separator, char quotechar, boolean strictQuotes) {
		this(reader, separator, quotechar, ICSVParser.DEFAULT_ESCAPE_CHARACTER, DEFAULT_SKIP_LINES, strictQuotes);
	}
	
	public CSVReader(Reader reader, char separator, char quotechar, char escape) {
		this(reader, separator, quotechar, escape, DEFAULT_SKIP_LINES, ICSVParser.DEFAULT_STRICT_QUOTES);
	}
	
	public CSVReader(Reader reader, char separator, char quotechar, int line) {
		this(reader, separator, quotechar, ICSVParser.DEFAULT_ESCAPE_CHARACTER, line, ICSVParser.DEFAULT_STRICT_QUOTES);
	}
	
	public CSVReader(Reader reader, char separator, char quotechar, char escape, int line) {
		this(reader, separator, quotechar, escape, line, ICSVParser.DEFAULT_STRICT_QUOTES);
	}
	
	public CSVReader(Reader reader, char separator, char quotechar, char escape, int line, boolean strictQuotes) {
		this(reader, separator, quotechar, escape, line, strictQuotes, ICSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE);
	}
	
	public CSVReader(Reader reader, char separator, char quotechar, char escape, int line, boolean strictQuotes, boolean ignoreLeadingWhiteSpace) {
		this(reader, line, new CSVParser(separator, quotechar, escape, strictQuotes, ignoreLeadingWhiteSpace));
	}
	
	public CSVReader(Reader reader, char separator, char quotechar, char escape, int line, boolean strictQuotes, boolean ignoreLeadingWhiteSpace, boolean keepCR) {
		this(reader, line, new CSVParser(separator, quotechar, escape, strictQuotes, ignoreLeadingWhiteSpace), keepCR, DEFAULT_VERIFY_READER);
	}
	
	public CSVReader(Reader reader, int line, ICSVParser icsvParser) {
		this(reader, line, icsvParser, DEFAULT_KEEP_CR, DEFAULT_VERIFY_READER);
	}
	
	CSVReader(Reader reader, int line, ICSVParser icsvParser, boolean keepCR, boolean verifyReader) {
		this.br = (reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader));
		this.lineReader = new LineReader(br, keepCR);
		this.skipLines = line;
		this.parser = icsvParser;
		this.keepCR = keepCR;
		this.verifyReader = verifyReader;
	}
	
	public ICSVParser getParser() {
		return parser;
	}
	
	public int getSkipLines() {
		return skipLines;
	}
	
	public boolean keepCarriageReturns() {
		return keepCR;
	}
	
	public List<String[]> readAll() throws IOException {
		List<String[]> allElements = new ArrayList<String[]>();
		while (hasNext) {
			String[] nextLineAsTokens = readNext();
			if (nextLineAsTokens != null) {
				allElements.add(nextLineAsTokens);
			}
		}
		return allElements;
	}
	
	public String[] readNext() throws IOException {
		String[] result = null;
		do {
			String nextLine = getNextLine();
			if (!hasNext) {
				return validateResult(result);
			}
			String[] r = parser.parseLineMulti(nextLine);
			if (r.length > 0) {
				if (result == null) {
					result = r;
				}
				else {
					result = combineResultsFromMultpleReads(result, r);
				}
			}
		} while (parser.isPending());
		return validateResult(result);
	}
	
	protected String[] validateResult(String[] result) {
		if (result != null) {
			recordsRead++;
		}
		return result;
	}
	
	protected String[] combineResultsFromMultpleReads(String[] buffer, String[] lastRead) {
		String[] t = new String[buffer.length + lastRead.length];
		System.arraycopy(buffer, 0, t, 0, buffer.length);
		System.arraycopy(lastRead, 0, t, buffer.length, lastRead.length);
		return t;
	}

	protected String getNextLine() throws IOException{
		if (isClosed()) {
			hasNext = false;
			return null;
		}
		
		if (!this.linesSkiped) {
			for (int i = 0; i < skipLines; i++) {
				lineReader.readLine();
				linesRead++;
			}
			this.linesSkiped = true;
		}
		String nextLine = lineReader.readLine();
		if (nextLine == null) {
			hasNext = false;
		}
		else {
			linesRead++;
		}
		
		return hasNext ? nextLine : null;
	}
	
	protected boolean isClosed() {
		if (!verifyReader) {
			return false;
		}
		try {
			br.mark(READ_AHEAD_LIMIT);
			int nextByte = br.read();
			br.reset();
			return nextByte == -1;
		}
		catch (IOException e) {
			return true;
		}
	}
	
	@Override
	public void close() throws IOException {
		br.close();
	}
	
	public Iterator<String[]> iterator() {
		try {
			return new CSVIterator(this);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public boolean verifyReader() {
		return this.verifyReader;
	}
	
	public long getLinesRead() {
		return linesRead;
	}
	
	public long getRecordsRead() {
		return recordsRead;
	}
}
