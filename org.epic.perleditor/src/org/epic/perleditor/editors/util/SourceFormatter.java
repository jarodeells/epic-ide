package org.epic.perleditor.editors.util;
import java.io.*;import gnu.regexp.RE;import gnu.regexp.REMatch;import gnu.regexp.REException;

public class SourceFormatter {
	public String doConversion(String text) {		String result = "";		try {
			String lineIn;
			int indent = 0;			int tabs = 4;
			BufferedReader br = new BufferedReader(new StringReader(text));
			while ((lineIn = br.readLine()) != null) {
				//Handle POD comments				if (lineIn.startsWith("=")) {					String pod = handlePodComments(lineIn, br);					if(pod.length() > 0) {						result += pod;						continue;					}				}
				lineIn = lineIn.trim();
				// Escape "(" and ")" for regularExpr with "\\"				if (compareOccurrences(lineIn, "}", "{") > 0					|| compareOccurrences(lineIn, "\\)", "\\(") > 0) {					indent--;				}
				String prefix = "";				for (int i = 0; i < indent; i++) {					for (int j = 0; j < tabs; j++) {						prefix += " ";					}				}

				result += prefix + lineIn + "\n";				// Escape "(" and ")" for regularExpr with "\\"				if (compareOccurrences(lineIn, "{", "}") > 0					|| compareOccurrences(lineIn, "\\(", "\\)") > 0) {					indent++;				}
				// Handle HERE script				if (lineIn.indexOf("<") != -1) {					result += handleHereScript(lineIn, br);				}
			}
		} catch (Exception ex) {			ex.printStackTrace();		}
		return result;	}

	private int compareOccurrences(		String line,		String string1,     	String string2) {
		RE re;
		String[] doNotInspectBetween = { "\"", "'", "/", "~", "|" };
		try {
            // Delete comments			re = new RE("#.*$");			line = re.substitute(line, "");
			// Remove backslash characters			re = new RE("\\\\.");			line = re.substitute(line, "");
			for (int i = 0; i < doNotInspectBetween.length; i++) {				re =					new RE(						doNotInspectBetween[i]							+ ".*?"							+ doNotInspectBetween[i]);				line = re.substitute(line, "");			}
			re = new RE(string1);			REMatch[] matches1 = re.getAllMatches(line);
			re = new RE(string2);			REMatch[] matches2 = re.getAllMatches(line);
			return matches1.length - matches2.length;		} catch (REException ex) {			ex.printStackTrace();			return 0;		}
	}
	private String handleHereScript(String line, BufferedReader br) {
		String hereBlock = "";
		try {			RE re = new RE("<<[^a-zA-Z]*?([a-zA-Z]+)");			REMatch[] matches = re.getAllMatches(line);
			if (matches.length == 0) {				return hereBlock;			}
			int found = 0;			String content = "";
			while (found < matches.length				&& (content = br.readLine()) != null) {				hereBlock += content + "\n";				if (matches.length == found					&& matches[found].toString(1).equals("x")) {					break;				}
				if (content.equals(matches[found].toString(1))) {					found++;				}
			}
		} catch (Exception e) {			e.printStackTrace();		}
		return hereBlock;
	}
	private String handlePodComments(String line, BufferedReader br) {		String podBlock = "";
		try {			RE re = new RE("^=[a-zA-Z]+");			REMatch[] matches = re.getAllMatches(line);
			if (matches.length == 0) {				return podBlock;			}
			podBlock = line + "\n";
			String content = "";
			while ((content = br.readLine()) != null) {				podBlock += content + "\n";
				if (content.equals("=cut")) {					break;				}
			}
		} catch (Exception e) {			e.printStackTrace();		}
		return podBlock;	}
}
