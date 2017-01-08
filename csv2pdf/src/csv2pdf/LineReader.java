package csv2pdf;

import java.io.BufferedReader;
import java.io.IOException;

public class LineReader {
	private final BufferedReader reader;
	private final boolean keepCarriageReturns;
	
	public LineReader(BufferedReader reader, boolean keepCarriageReturns) {
		this.reader = reader;
		this.keepCarriageReturns = keepCarriageReturns;
	}
	
	public String readLine() throws IOException {
		return keepCarriageReturns ? readUntilNewLine() : reader.readLine();
	}
	
	private String readUntilNewLine() throws IOException {
		StringBuilder sb = new StringBuilder(ICSVParser.INITIAL_READ_SIZE);
		for (int c = reader.read(); c > -1 && c != '\n'; c = reader.read()) {
			sb.append((char) c);
		}
		
		return sb.length() > 0 ? sb.toString() : null;
	}
}
