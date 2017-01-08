package csv2pdf;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CSVIterator implements Iterator<String[]> {
	private final CSVReader reader;
	private String[] nextLine;
	
	public CSVIterator(CSVReader reader) throws IOException {
		this.reader = reader;
		nextLine = reader.readNext();
	}

	@Override
	public boolean hasNext() {
		return nextLine != null;
	}

	@Override
	public String[] next() {
		String[] temp = nextLine;
		try {
			nextLine = reader.readNext();
		}
		catch (IOException e) {
			throw new NoSuchElementException();
		}
		return temp;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("This is a ready only iterator.");
	}
}
