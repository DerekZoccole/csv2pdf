package csv2pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVParser implements ICSVParser {
	
	private final char separator;
	private final char quotechar;
	private final char escape;
	private final boolean strictQuotes;
	private final boolean ignoreLeadingWhiteSpace;
	private final boolean ignoreQuotations;
	private final CSVReaderNullFieldIndicator nullFieldIndicator;
	private String pending;
	private boolean inField = false;
	
	public CSVParser() {
		this(DEFAULT_SEPARATOR, DEFAULT_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER);
	}
	
	public CSVParser(char separator) {
		this(separator, DEFAULT_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER);
	}
	
	public CSVParser(char separator, char quotechar) {
		this(separator, quotechar, DEFAULT_ESCAPE_CHARACTER);
	}
	
	public CSVParser(char separator, char quotechar, char escape) {
		this(separator, quotechar, escape, DEFAULT_STRICT_QUOTES);
	}
	
	public CSVParser(char separator, char quotechar, char escape, boolean strictQuotes) {
		this(separator, quotechar, escape, strictQuotes, DEFAULT_IGNORE_LEADING_WHITESPACE);
	}
	
	public CSVParser(char separator, char quotechar, char escape, boolean strictQuotes, boolean ignoreLeadingWhiteSpace) {
		this(separator, quotechar, escape, strictQuotes, ignoreLeadingWhiteSpace, DEFAULT_IGNORE_QUOTATIONS);
	}
	
	public CSVParser(char separator, char quotechar, char escape, boolean strictQuotes, boolean ignoreLeadingWhiteSpace, boolean ignoreQuotations) {
		this(separator, quotechar, escape, strictQuotes, ignoreLeadingWhiteSpace, ignoreQuotations, DEFAULT_NULL_FIELD_INDICATOR);
	}
	
	CSVParser(char separator, char quotechar, char escape, boolean strictQuotes, boolean ignoreLeadingWhiteSpace, boolean ignoreQuotations, CSVReaderNullFieldIndicator nullFieldIndicator) {
		if (anyCharacterAreTheSame(separator, quotechar, escape)) {
			throw new UnsupportedOperationException("The separator, quote, and escape character must be different");
		}
		if (separator == NULL_CHARACTER) {
			throw new UnsupportedOperationException("The separator character must be defined!");
		}
		this.separator = separator;
		this.quotechar = quotechar;
		this.escape = escape;
		this.strictQuotes = strictQuotes;
		this.ignoreLeadingWhiteSpace = ignoreLeadingWhiteSpace;
		this.ignoreQuotations = ignoreQuotations;
		this.nullFieldIndicator = nullFieldIndicator;
	}

	@Override
	public char getSeparator() {
		// TODO Auto-generated method stub
		return separator;
	}

	@Override
	public char getQuotechar() {
		// TODO Auto-generated method stub
		return quotechar;
	}
	
	public char getEscape() {
		return escape;
	}
	
	public boolean isStrictQuotes() {
		return strictQuotes;
	}
	
	public boolean isIgnoreLeadingWhiteSpace() {
		return ignoreLeadingWhiteSpace;
	}
	
	public boolean isIgnoreQuotations() {
		return ignoreQuotations;
	}
	
	private boolean anyCharacterAreTheSame(char separator, char quotechar, char escape) {
		return isSameCharacter(separator, quotechar) || isSameCharacter(separator, escape) || isSameCharacter(quotechar, escape);
	}
	
	private boolean isSameCharacter(char c1, char c2) {
		return c1 != NULL_CHARACTER && c1 == c2;
	}

	@Override
	public boolean isPending() {
		return pending != null;
	}

	@Override
	public String[] parseLineMulti(String nextLine) throws IOException {
		return parseLine(nextLine, true);
	}

	@Override
	public String[] parseLine(String nextLine) throws IOException {
		return parseLine(nextLine, false);
	}
	
	protected String[] parseLine(String nextLine, boolean multi) throws IOException {
		if (!multi && pending != null) {
			pending = null;
		}
		
		if (nextLine == null) {
			if (pending != null) {
				String s = pending;
				pending = null;
				return new String[]{s};
			}
			return null;
		}
		
		List<String> tokensOnThisLine = new ArrayList<String>();
		StringBuilder sb = new StringBuilder(nextLine.length() + READ_BUFFER_SIZE);
		boolean inQuotes = false;
		boolean fromQuotedField = false;
		if (pending != null) {
			sb.append(pending);
			pending = null;
			inQuotes = !this.ignoreQuotations;
		}
		for (int i = 0; i < nextLine.length(); i++) {
			char c = nextLine.charAt(i);
			if (c == this.escape) {
				if (isNextCharacterEscapable(nextLine, inQuotes(inQuotes), i)) {
					i = appendNextCharacterAndAdvanceLoop(nextLine, sb, i);
				}
			}
			else if (c == quotechar) {
				if (isNextCharacterEscapeQuote(nextLine, inQuotes(inQuotes), i)) {
					i = appendNextCharacterAndAdvanceLoop(nextLine, sb, i);
				}
				else {
					inQuotes = !inQuotes;
					if (atStartOfField(sb)) {
						fromQuotedField = true;
					}
					
					if (!strictQuotes) {
						if (i > 2 && nextLine.charAt(i - 1) != this.separator && nextLine.length() > (i + 1) && nextLine.charAt(i + 1) != this.separator) {
							if (ignoreLeadingWhiteSpace && sb.length() > 0 && StringUtils.isWhitespace(sb)) {
								sb.setLength(0);
							}
							else {
								sb.append(c);
							}
						}
					}
				}
				inField = !inField;
			}
			else if (c == separator && !(inQuotes && !ignoreQuotations)) {
				tokensOnThisLine.add(convertEmptyToNullIfNeeded(sb.toString(), fromQuotedField));
				fromQuotedField = false;
				sb.setLength(0);
				inField = false;
			}
			else {
				if (!strictQuotes || (inQuotes && !ignoreQuotations)) {
					sb.append(c);
					inField = true;
					fromQuotedField = true;
				}
			}
		}
		
		if (inQuotes && !ignoreQuotations) {
			if (multi) {
				sb.append('\n');
				pending = sb.toString();
				sb = null;
			}
			else {
				throw new IOException("Un-terminated quoted field at the end of CSV line");
			}
			
			if (inField) {
				fromQuotedField = true;
			}
		}
		else {
			inField = true;
		}
		
		if (sb != null) {
			tokensOnThisLine.add(convertEmptyToNullIfNeeded(sb.toString(), fromQuotedField));
		}
		return tokensOnThisLine.toArray(new String[tokensOnThisLine.size()]);
	}
	
	private boolean atStartOfField(StringBuilder sb) {
		return sb.length() == 0;
	}
	
	private String convertEmptyToNullIfNeeded(String s, boolean fromQuotedField) {
		if (s.isEmpty() && shouldConvertEmptyToNull(fromQuotedField)) {
			return null;
		}
		return s;
	}
	
	private boolean shouldConvertEmptyToNull(boolean fromQuotedField) {
		switch (nullFieldIndicator) {
			case BOTH:
				return true;
			case EMPTY_SEPARATORS:
				return !fromQuotedField;
			case EMPTY_QUOTES:
				return fromQuotedField;
			default:
				return false;
		}
	}
	
	private int appendNextCharacterAndAdvanceLoop(String line, StringBuilder sb, int i) {
		sb.append(line.charAt(i + 1));
		i++;
		return i;
	}
	
	private boolean inQuotes(boolean inQuotes) {
		return (inQuotes && !ignoreQuotations) || inField;
	}
	
	private boolean isNextCharacterEscapeQuote(String nextLine, boolean inQuotes, int i) {
		return inQuotes && nextLine.length() > (i + 1) && isCharacterQuoteCharacter(nextLine.charAt(i + 1));
	}
	
	private boolean isCharacterQuoteCharacter(char c) {
		return c == quotechar;
	}
	
	private boolean isCharacterEscapeCharacter(char c) {
		return c == escape;
	}
	
	private boolean isCharacterEscapable(char c) {
		return isCharacterQuoteCharacter(c) || isCharacterEscapeCharacter(c);
	}
	
	private boolean isNextCharacterEscapable(String nextLine, boolean inQuotes, int i) {
		return inQuotes && nextLine.length() > (i + 1) && isCharacterEscapable(nextLine.charAt(i + 1));
	}

	@Override
	public CSVReaderNullFieldIndicator nullFieldIndicator() {
		return nullFieldIndicator;
	}
}
