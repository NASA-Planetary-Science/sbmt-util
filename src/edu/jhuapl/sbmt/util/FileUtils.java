package edu.jhuapl.sbmt.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.Vector;

import javax.swing.filechooser.FileSystemView;

public class FileUtils {
	public static boolean areSame(String s1, String s2)
	{
		boolean result = false;
		
		if ((s1 != null) && (s2 != null))
		{
			String sLeft = FileUtils.stripSpaces(s1.toUpperCase());
			String sRight = FileUtils.stripSpaces(s2.toUpperCase());
			if (sLeft.equals(sRight))
			{
				result = true;
			}
		}
		
		return result;
	}
	public static String getLabelString(String imgStr)
	{
		String result = null;
		
		if ((imgStr != null) && (imgStr.length() > 0))
		{
			String candidate1 = FileUtils.getFilenameBeforeExtension(imgStr) + ".lbl";
			String candidate2 = FileUtils.getFilenameBeforeExtension(imgStr) + ".LBL";
			if (new File(candidate1).exists())
			{
				result = candidate1;
			}
			else if (new File(candidate2).exists())
			{	
				result = candidate2;
			}
			else if (imgStr.endsWith(".IMG"))
			{
				result = FileUtils.getFilenameBeforeExtension(imgStr) + ".LBL";
			}
			else
			{
				result = FileUtils.getFilenameBeforeExtension(imgStr) + ".lbl";
			}
		}
		
		return result;
	}
	public static String stripSpaces(String inStr)
	{
		String result = new String("");
		
		if (inStr != null)
		{
			int i;
			char c;
			int n = inStr.length();
			if (n > 0)
			{
				int startPt = 0;
				int stopPt = n - 1;
				
				for (i = 0; i < n; i++){
					c = inStr.charAt(i);
					if (c > (char)32)
					{
						startPt = i;
						break;
					}
				}
				
				for (i = n - 1; i >= 0; i--){
					c = inStr.charAt(i);
					if (c > (char)32)
					{
						stopPt = i;
						break;
					}
				}
				
				for (i = startPt; i <= stopPt; i++){
					result += inStr.charAt(i);
				}
			}
		}
		
		return result;
	}
	public static boolean inRange(double x, double xmin, double xmax)
	{
		if ((x > xmin) && (x < xmax))
			return true;
		return false;
	}
	static int findChar(String str, char c)
	{
		int result = -1;
		
		for (int i = 0; (i < str.length()) && (result < 0); i++)
			if (str.charAt(i) == c)
				result = i;
		
		return result;
	}
	static String removeChar(String str, int pos)
	{
		String result = "";
		
		for (int i = 0; i < str.length(); i++){
			if (i != pos)
			{
				result += str.charAt(i);
			}
		}
		
		return result;
	}
	static String copyStr(String src)
	{
		String result = "";
		
		for (int i = 0; i < src.length(); i++)
			result += src.charAt(i);
		
		return result;
	}
	static char[] makeStringArray(String str)
	{
		char result[] = new char[str.length()];
		
		for (int i = 0; i < str.length(); i++)
			result[i] = str.charAt(i);
		
		return result;
	}
	static char[] addLeft(char array[], char fillChar)
	{
		char result[] = new char[array.length + 1];
		
		result[0] = fillChar;
		for (int i = 0; i < array.length; i++)
			result[i+1] = array[i];
		
		return result;
	}
	static char[] addRight(char array[], char fillChar)
	{
		char result[] = new char[array.length + 1];
		
		for (int i = 0; i < array.length; i++)
			result[i] = array[i];
		result[array.length] = fillChar;
		
		return result;
	}
	static String moveChar(String str, int pos, int places, char fillChar)
	{
		String result = "";
		
		int i;
		char array[] = makeStringArray(str);
		
		int absPlaces = Math.abs(places);
		boolean right = true;
		if (places < 0) right = false;
		
		for (i = 0; i < absPlaces; i++){
			char c1 = array[pos];
			int length = array.length;
			if (right)
			{
				if (pos == (length-1))
				{
					array = addRight(array, fillChar);
				}
				char c2 = array[pos+1];
				array[pos] = c2;
				array[pos+1] = c1;
				pos++;
			}
			else
			{
				if (pos == 0)
				{
					array = addLeft(array, fillChar);
					pos++;
				}
				char c2 = array[pos-1];
				array[pos] = c2;
				array[pos-1] = c1;
				pos--;
			}
		}
		
		for (i = 0; i < array.length; i++)
			result += array[i];
		
		return result;
	}
	public static void main(String args[])
	{
		String s = formatDouble(123456, 5);
		System.out.println("String = "+s);
	}
	public static String formatDouble(double x, int places)
	{
		String result = new String("");
		
		x = roundOff(x, places);
		
		String s = Double.toString(x);
		
		if (s.toUpperCase().contains("E"))
		{
			String parts[] = s.toUpperCase().split("E");
			if (parts.length == 2)
			{
				String exStr = parts[1];
				String numStr = parts[0];
				if (!numStr.contains("."))
				{
					numStr += ".";
				}
				int pos = findChar(numStr, '.');
				int exponent = Integer.parseInt(exStr);
				s = moveChar(numStr, pos, exponent, '0');
			}
		}
		if (s.contains("."))
			s += "000000000000000000000000";
		else
			s += ".000000000000000000000000";
		
		int length = s.length();
		
		boolean finished = false;
		int decimalPlace = -1;
		
		for (int i = 0; (i < length) && !finished; i++){
			char c = s.charAt(i);
			if (c == '.')
			{
				decimalPlace = i;
			}
			else
			{
				if (decimalPlace >= 0)
				{
					if ((i-decimalPlace) >= places)
					{
						finished = true;
					}
				}
			}
			result += c;
		}
		
		return result;
	}
	public static String formatDouble(double x, int placesBefore, int placesAfter)
	{
		String result = formatDouble(x, placesAfter);
		while (findChar(result,'.') < placesBefore)
			result = "0" + result;
		
		return result;
	}
	public static String formatInteger(int v, int places)
	{
		String result = new String("");
	
		String s = Integer.toString(v);
		int length = s.length();
		
		if (length < places)
		{
			int diff = (places - length);
			for (int i = 0; i < diff; i++){
				result += '0';
			}
		}
		
		result += s;
	
		return result;
	}
	public static String formatInteger(int v)
	{
		return Integer.toString(v);
	}
	public static String formatHex(int v, int places)
	{
		String fmt = "%0" + places + "x";
		
		return String.format(fmt, v);
	}
	public static double roundOff(double x, int places)
	{
		double factor = Math.pow(10.0, (double)places);
		return Math.floor(0.50 + x * factor)/factor;
	}
	public static double fractionalPart(double x)
	{
		return (x - Math.floor(x));
	}
	public static boolean isInteger(String s)
	{
		boolean result = false;

		int length = s.length();
		
		for (int i = 0; i < length; i++){
			char c = s.charAt(i);
			if (i == 0)
			{
				if ((c == '-') || (c == '+'))
				{
				}
				else if ((c >= 48) && (c <= 57))
				{
					result = true;
				}
				else
				{	
					break;
				}
			}
			else
			{
				if ((c >= 48) && (c <= 57))
				{
					result = true;
				}
				else
				{	
					result = false;
					break;
				}
			}
		}

		return result;
	}
	public static boolean isDouble(String s)
	{
		boolean result = false;
		
		boolean gotDot = false;

		int length = s.length();
		
		String str = s.toUpperCase();

		boolean expOk = true;

		if (str.contains("D"))
		{
			expOk = false;

			String args[] = str.split("D");
			if (args.length > 0)
			{
				str = args[0];
			}
			if ((args.length == 2) && isInteger(args[1]))
			{
				expOk = true;
			}
		}
		else if (str.contains("E"))
		{
			expOk = false;

			String args[] = str.split("E");
			if (args.length > 0)
			{
				str = args[0];
			}
			if ((args.length == 2) && isInteger(args[1]))
			{
				expOk = true;
			}
		}
		
		length = str.length();
		
		for (int i = 0; i < length; i++){
			char c = str.charAt(i);
			if (i == 0)
			{
				if (c == '-')
				{
				}
				else if ((c >= 48) && (c <= 57))
				{
					result = true;
				}
				else if (c == '.')
				{
					gotDot = true;
				}
				else
				{
					result = false;
					break;
				}
			}
			else
			{
				if ((c >= '0' && c <= '9'))
				{
					result = true;
				}
				else if (c == '.')
				{
					if (gotDot)
					{
						result = false;
						break;
					}
					gotDot = true;
				}
				else
				{	
					result = false;
					break;
				}
			}
		}

		return (result&&expOk);
	}
	public static void writeTextToFile(String filename, Vector<String> lines) throws IOException
	{
		FileOutputStream outputFile = new FileOutputStream(filename);
		PrintStream outputStream = new PrintStream(outputFile);
		
		int size = lines.size();
		for (int i = 0; i < size; i++){
			outputStream.println(lines.get(i));
		}
		
		outputFile.flush();
		outputFile.close();
	}
	public static void appendTextToFile(String filename, String line) throws IOException
	{
		//
		//	write the encoded records out to the file
		//
		try {
			FileOutputStream strm = new FileOutputStream(filename, true);
			PrintStream outputStream = new PrintStream(strm);
			//strm.write(recData, 0, length);
			outputStream.println(line);
			strm.flush();
			strm.close();
		}
		catch (IOException ioEx)
		{
			System.out.println(ioEx);
			ioEx.printStackTrace();
		}
	}
	public static boolean readAsciiFile(String filename, Vector<String> results) throws IOException
	{
		boolean result = false;
		
		File f = new File(filename);
		if (f.exists())
		{
			result = true;
			FileInputStream fileInput = new FileInputStream(filename);
			BufferedReader fileRdr = new BufferedReader(new InputStreamReader(fileInput));
			
			results.clear();
			
			String s = new String(fileRdr.readLine());
			
			while (s != null){
				s.trim();
				
				results.add(s);
				
				s = fileRdr.readLine();
			}
			
			fileRdr.close();
			fileInput.close();
		}
		
		return result;
	}
	public static byte[] readFileData(String filename) throws IOException
	{
		byte results[] = null;
		
		File f = new File(filename);
		if (f.exists())
		{
			long fileLength = f.length();
			byte rawData[] = new byte[(int)fileLength];
			
			FileInputStream fileInput = new FileInputStream(filename);
			int readLength = fileInput.read(rawData);
			if (readLength == (int)fileLength)
			{
				results = rawData;
			}
			else if (readLength > 0)
			{
				results = new byte[readLength];
				for (int i = 0; i < readLength; i++){
					results[i] = rawData[i];
				}
			}
			fileInput.close();
			rawData = null;
		}
		
		return results;
	}
	public static void writeFileData(String filename, byte data[]) throws IOException
	{
		FileOutputStream fos = new FileOutputStream(new File(filename));
		fos.write(data);
		fos.close();
	}
	public static void writeFileData(String filename, byte data[], int length) throws IOException
	{
		FileOutputStream fos = new FileOutputStream(new File(filename));
		fos.write(data, 0, length);
		fos.close();
	}
	public static Vector<String> readAsciiFile(String filename)
	{
		Vector<String> results = null;
		
		try {
			File f = new File(filename);
			if (f.exists())
			{
				FileInputStream fileInput = new FileInputStream(filename);
				BufferedReader fileRdr = new BufferedReader(new InputStreamReader(fileInput));
				
				String s = new String(fileRdr.readLine());
				
				while (s != null){
					s.trim();
					
					if (results == null)
						results = new Vector<String>();
					results.add(s);
					
					s = fileRdr.readLine();
				}
				
				fileRdr.close();
				fileInput.close();
				
				fileInput = null;
				fileRdr = null;
			}
		}
		catch (IOException ioEx)
		{
			System.out.println("Caught IO Exception in FileUtils.readAsciiFile(String): "+ioEx);
			ioEx.printStackTrace();
		}
		
		return results;
	}
	public static void getFilenamesInFolder(String folder, Vector<String> results)
	{
		results.clear();
		FileSystemView fsView = FileSystemView.getFileSystemView();
		File[] files = fsView.getFiles(new File(folder), false);
		int num = files.length;
		for (int i = 0; i < num; i++){
			results.add(files[i].getName());
		}
	}
	public static void getFilenamesInFolderWithEnding(String folder, String endStr, Vector<String> results)
	{
		Vector<String> tempStrings = new Vector<String>();
		getFilenamesInFolder(folder, tempStrings);
		
		int i, n;
		
		String upEndStr = endStr.toUpperCase();
		
		n = tempStrings.size();
		for (i = 0; i < n; i++){
			if (tempStrings.get(i).toUpperCase().endsWith(upEndStr))
			{
				String str = folder;
				str += tempStrings.get(i);
				results.add(str);
			}
		}
	}
	public static void getFilesInFolder(String folder, Vector<File> results)
	{
		results.clear();
		FileSystemView fsView = FileSystemView.getFileSystemView();
		File[] files = fsView.getFiles(new File(folder), false);
		int num = files.length;
		for (int i = 0; i < num; i++){
			results.add(files[i]);
		}
	}
	public static void getFilesInFolder(String folder, String fileStart, Vector<File> results)
	{
		results.clear();
		String upFileStart = fileStart.toUpperCase();
		FileSystemView fsView = FileSystemView.getFileSystemView();
		File[] files = fsView.getFiles(new File(folder), false);
		int num = files.length;
		for (int i = 0; i < num; i++){
			String fStr = FileUtils.getFilenameOnly(files[i].getAbsolutePath());
			if (fStr.toUpperCase().startsWith(upFileStart))
			{
				results.add(files[i]);
			}
		}
	}
	public static void getFoldersInFolder(String folder, Vector<File> results)
	{
		results.clear();
		FileSystemView fsView = FileSystemView.getFileSystemView();
		File[] files = fsView.getFiles(new File(folder), false);
		int num = files.length;
		for (int i = 0; i < num; i++){
			if (files[i].isDirectory())
			{
				//System.out.println("Adding: "+files[i].getAbsolutePath());
				results.add(files[i]);
			}
		}
	}
	public static void getFilesInFolderRecursively(String folder, Vector<File> results)
	{
		FileSystemView fsView = FileSystemView.getFileSystemView();
		File[] files = fsView.getFiles(new File(folder), false);
		int num = files.length;
		for (int i = 0; i < num; i++){
			if (files[i].isDirectory())
			{
				String dirStr = new String(folder + (folder.endsWith(File.separator) ? "" : File.separator) + files[i].getName());
				getFilesInFolderRecursively(dirStr, results);
			}
			else
			{
				results.add(files[i]);
			}
		}
	}
	public static boolean readEntireFile(String filename, Vector<Byte> fileContents)
	{
		Boolean result = false;
		
		fileContents.clear();
		
		File f = new File(filename);
		if (f.exists() && f.isFile())
		{
			long fileSize = f.length();
			if (fileSize > 0)
			{
				int i;
				int buffLength = 4096;
				byte buff[] = new byte[buffLength];
				
				try {
					RandomAccessFile raf = new RandomAccessFile(filename, "r");
					
					int n = raf.read(buff, 0, buffLength);
					while (n > 0){
						for (i = 0; i < n; i++){
							fileContents.add(buff[i]);
						}
						n = raf.read(buff, 0, buffLength);
					}
					
					result = true;
					
					raf.close();
				}
				catch (IOException ioEx)
				{
					System.out.println(ioEx);
					ioEx.printStackTrace();
				}
			}
		}
		
		return result;
	}
	public static byte[] readEntireFile(String filename)
	{
		byte result[] = null;
		
		File f = new File(filename);
		if (f.exists() && f.isFile())
		{
			long fileSize = f.length();
			if (fileSize > 0)
			{	
				try {
					RandomAccessFile raf = new RandomAccessFile(filename, "r");
					
					byte buff[] = new byte[(int)fileSize];
					int n = raf.read(buff, 0, (int)fileSize);
					
					if (n == (int)fileSize)
					{
						result = buff;
					}
					
					raf.close();
				}
				catch (IOException ioEx)
				{
					System.out.println(ioEx);
					ioEx.printStackTrace();
				}
			}
		}
		
		return result;
	}
	public static byte[] readDataFromFile(String filename, int offset, int nBytes)
	{
		byte result[] = null;
		
		if ((offset >= 0) && (nBytes > 0))
		{
			File f = new File(filename);
			if (f.exists() && f.isFile())
			{
				long fileSize = f.length();
				if (fileSize >= (offset+nBytes))
				{	
					byte buff[] = new byte[(int)nBytes];
					
					try {
						RandomAccessFile raf = new RandomAccessFile(filename, "r");
						
						raf.seek(offset);
						
						int n = raf.read(buff, 0, (int)nBytes);
						if (n == nBytes)
						{
							result = buff;
						}
						
						raf.close();
					}
					catch (IOException ioEx)
					{
						System.out.println(ioEx);
						ioEx.printStackTrace();
					}
				}
			}
		}
		
		return result;
	}
	public static String getFilenameOnly(String filename)
	{
		String result = new String("");
		
		char sepChar = File.separatorChar;
		
		int i, length;
		int sepPt = 0;
		boolean foundSep = false;
		
		length = filename.length();
		
		if (length > 0)
		{
			for (i = length - 1; (i >= 0) && (sepPt == 0); i--){
				char c = filename.charAt(i);
				if (c == sepChar)
				{
					sepPt = i;
					foundSep = true;
				}
			}
			if (foundSep)
			{
				for (i = sepPt + 1; i < length; i++){
					result += filename.charAt(i);
				}
			}
			else
			{
				for (i = 0; i < length; i++){
					result += filename.charAt(i);
				}
			}
		}
		
		return result;
	}
	public static String getFilenameOnlyNoExt(String filename)
	{
		String result = new String("");
		
		char sepChar = File.separatorChar;
		
		int i, length;
		int sepPt = 0;
		boolean foundSep = false;
		
		length = filename.length();
		
		if (length > 0)
		{
			for (i = length - 1; (i >= 0) && (sepPt == 0); i--){
				char c = filename.charAt(i);
				if (c == sepChar)
				{
					sepPt = i;
					foundSep = true;
				}
			}
			if (foundSep)
			{
				for (i = sepPt + 1; i < length; i++){
					result += filename.charAt(i);
				}
			}
			else
			{
				for (i = 0; i < length; i++){
					result += filename.charAt(i);
				}
			}
		}
		
		return FileUtils.getFilenameBeforeExtension(result);
	}
	public static String[] getPathParts(String filename)
	{
		String results[] = null;
		
		char sepChar = File.separatorChar;
		
		int i, length;
		
		Vector<String> parts = new Vector<String>();
		String arg = "";
		
		length = filename.length();
		
		if (length > 0)
		{
			for (i = 0; i < length; i++){
				char c = filename.charAt(i);
				if (c == sepChar)
				{
					if (arg.length() > 0)
						parts.add(arg);
				}
				else
				{
					arg += c;
				}
			}
			
			if (arg.length() > 0)
				parts.add(arg);
		}
		
		results = (String[])parts.toArray();
		
		return results;
	}
	public static String getFolderOnly(String filename)
	{
		String result = new String("");
		
		char sepChar = File.separatorChar;
		
		int i, length;
		int sepPt = 0;
		boolean foundSep = false;
		
		length = filename.length();
		
		if (length > 0)
		{
			for (i = length - 1; (i >= 0) && (sepPt == 0); i--){
				char c = filename.charAt(i);
				if (c == sepChar)
				{
					sepPt = i;
					foundSep = true;
				}
			}
			if (foundSep)
			{
				for (i = 0; i <= sepPt; i++){
					result += filename.charAt(i);
				}
			}
			else
			{
				for (i = 0; i < length; i++){
					result += filename.charAt(i);
				}
			}
		}
		
		if (!result.endsWith(File.separator))
			result += File.separator;
		
		return result;
	}
	public static String getFilenameBeforeExtension(String filename)
	{
		String result = new String("");
		
		String s = new String(filename);
		
		int length = s.length();
		if (length > 0)
		{
			for (int i = length-1; i >= 0; i--){
				char c = s.charAt(i);
				if (c == '.')
				{
					for (int j = 0; j < i; j++){
						result += s.charAt(j);
					}
					break;
				}
			}
		}
		
		return result;
	}
	public static String getFilenameExtension(String filename)
	{
		String result = new String("");
		
		String s = new String(filename);
		
		int length = s.length();
		if (length > 0)
		{
			for (int i = length-1; i >= 0; i--){
				char c = s.charAt(i);
				if (c == '.')
				{
					for (int j = i+1; j < length; j++){
						result += s.charAt(j);
					}
					break;
				}
			}
		}
		
		return result;
	}
	public static String stripSingleQuotes(String s)
	{
		String result = new String("");
		
		if (s != null)
		{
			int i, n;
			char c;
			String quoteStr = "'";
			
			n = s.length();
			
			int iStart = 0;
			int iStop = n - 1;
			
			for (i = 0; i < n; i++){
				c = s.charAt(i);
				if (c == quoteStr.charAt(0))
					iStart++;
				else if (c > (char)31)
					break;
			}
			for (i = n - 1; i >= 0; i--){
				c = s.charAt(i);
				if (c == quoteStr.charAt(0))
					iStop--;
				else if (c > (char)31)
					break;
			}
			
			for (i = iStart; i <= iStop; i++){
				result += s.charAt(i);
			}
		}
		
		return result;
	}
	public static String stripDoubleQuotes(String s)
	{
		String result = new String("");
		
		if (s != null)
		{
			int i, n;
			char c;
			char quoteChar = '"';
			
			n = s.length();
			
			int iStart = 0;
			int iStop = n - 1;
			
			for (i = 0; i < n; i++){
				c = s.charAt(i);
				if (c == quoteChar)
					iStart++;
				else if (c > (char)31)
					break;
			}
			for (i = n - 1; i >= 0; i--){
				c = s.charAt(i);
				if (c == quoteChar)
					iStop--;
				else if (c > (char)31)
					break;
			}
			
			for (i = iStart; i <= iStop; i++){
				result += s.charAt(i);
			}
		}
		
		return result;
	}
	public static void splitOnChar(String s, char splitChar, Vector<String> parts)
	{
		int i, n;
		char c;
		
		parts.clear();
		
		n = s.length();
		String part = new String("");
		
		for (i = 0; i < n; i++){
			c = s.charAt(i);
			if (c == splitChar)
			{
				if (part.length() > 0)
				{
					parts.add(part);
					part = new String("");
				}
			}
			else
			{
				part += c;
			}
		}
		
		if (part.length() > 0)
			parts.add(part);
	}
	public static Vector<String> splitOnChar(String s, char splitChar)
	{
		Vector<String> results = new Vector<String>();
		
		splitOnChar(s, splitChar, results);
		
		return results;
	}
	public static boolean areFilesTheSame(String file1, String file2)
	{
		boolean result = false;
		
		try {
			File f1 = new File(file1);
			File f2 = new File(file2);
			
			if (f1.exists() && f2.exists() && (f1.length() == f2.length()) && (f1.length() > 0))
			{
				RandomAccessFile raf1 = new RandomAccessFile(file1, "r");
				RandomAccessFile raf2 = new RandomAccessFile(file2, "r");
				
				result = true;
				
				int i;//, ndx=0;
				//long filePointer = -1;
				int blockSize = 8192;
				byte data1[] = new byte[blockSize];
				byte data2[] = new byte[blockSize];
				
				boolean finished = false;
				while (!finished){
					int readLength1 = raf1.read(data1);
					//int readLength2 = raf2.read(data2);
					//ndx++;
					if (readLength1 > 0)
					{
						for (i = 0; (i < readLength1) && !finished; i++){
							if (data1[i] != data2[i])
							{
								result = false;
								finished = true;
								//filePointer = raf1.getFilePointer();
							}
						}
					}
					else
					{
						finished = true;
					}
				}
				
				raf1.close();
				raf2.close();
				data1 = null;
				data2 = null;
			}
		}
		catch (IOException ioEx)
		{
		}
		
		return result;
	}
	public static String getNonBracketedString(String s)
	{
		String result = "";
		
		String str = FileUtils.stripSpaces(s);
		
		int n = str.length();
		
		boolean inBracket = false;
		
		for (int i = 0; i < n; i++){
			char c = str.charAt(i);
			if (c == '<')
			{
				inBracket = true;
			}
			else if (c == '>')
			{
				inBracket = false;
			}
			else if (!inBracket)
			{
				result += c;
			}
		}
		
		return result;
	}
	public static String getStringWithinBrackets(String s, char bracketChar)
	{
		String result = "";
		
		String str = FileUtils.stripSpaces(s);
		
		int n = str.length();
		
		boolean inBracket = false;
		
		for (int i = 0; i < n; i++){
			char c = str.charAt(i);
			if (c == bracketChar)
			{
				inBracket = true;
			}
			else if (c == bracketChar)
			{
				inBracket = false;
			}
			else if (!inBracket)
			{
				result += c;
			}
		}
		
		return result;
	}
	public static String getStringAfterString(String line, String match)
	{
		String result = "";
		
		int matchLen = match.length();
		int n = line.length();
		
		boolean done = false;
		
		for (int i = 0; (i < (n-matchLen)) && !done; i++){
			if (line.substring(i).startsWith(match))
			{
				for (int j = i + matchLen; j < n; j++){
					result += line.charAt(j);
				}
				done = true;
			}
		}
		
		return result;
	}
	public static String getStringBefore(String line, char match)
	{
		String result = "";
		
		int n = line.length();
		
		boolean done = false;
		
		for (int i = 0; (i < n) && !done; i++){
			char c = line.charAt(i);
			if (c == match)
			{
				done = true;
			}
			else
			{
				result += c;
			}
		}
		
		return result;
	}
	public static String getStringAfter(String s, String pattern)
	{
		String result = "";
		
		int i, n;
		
		n = s.length();
		for (i = 0; i < n; i++){
			if (s.substring(i).startsWith(pattern))
			{
				result = s.substring(i+pattern.length());
				break;
			}
		}
		
		return result;
	}
	public static String getStringBefore(String s, String pattern)
	{
		String result = "";
		
		int i, n;
		
		n = s.length();
		for (i = 0; i < n; i++){
			if (s.substring(i).startsWith(pattern))
			{
				result = s.substring(0,i);
				break;
			}
		}
		
		return result;
	}
}
