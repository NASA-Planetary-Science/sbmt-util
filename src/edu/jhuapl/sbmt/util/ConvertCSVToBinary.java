package edu.jhuapl.sbmt.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;


public class ConvertCSVToBinary {
	static String path;
	static final int lineLength = 121;

	public static void main(String[] args) {
		System.setProperty("java.awt.headless", "true");

		System.out.println(args.length);

		if(args.length != 2){
			usage();
		}
		run(args);
	}

	private static void usage(){
        String usage = "This program takes a CSV time history file and converts it to binary. \n"
        		     + "Takes two arguements, the first is a CSV file and the second is the  \n"
        		     + "location of where to save the binary file.\n\n"
                     + "Usage: Convert CSV to binary.\n";
        System.out.println(usage);

        System.exit(0);
	}

	private static void run(String[] args){
        String csvFile = args[0];
        path = args[1];
        BufferedReader brCsv = null;
        BufferedReader brPath = null;
        String line = "";
        String csvSplitBy = ",";
        int lineNum = 0;

        //
        // converts csv file to binary and writes to new file if file is empty
        //
        System.out.println(path);
		File binary = new File(path);
        try {
			binary.createNewFile();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        long startTime = System.currentTimeMillis();
        try
        {
            brCsv = new BufferedReader(new FileReader(csvFile));
            brPath = new BufferedReader(new FileReader(binary));
        	if (brPath.readLine() == null) {
                while ((line = brCsv.readLine()) != null){
                	if(!line.substring(0, 4).equals("#UTC")){
                        String[] info = line.split(csvSplitBy);
                        writeString(info[0], (lineLength * lineNum));
                        for(int i = 1; i<info.length; i++){
                        	writeBinary(info[i], (lineLength * lineNum) + 25 + (i-1)*8);
                        }
                        System.out.println("Processed line: " + lineNum);
                	}
                	lineNum++;
                }
        	}
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime)/1000);

        //
        // reads binary file and displays all time data
        //

//        try {
//        	int i = 1;
//			while(i<59864){
//				System.out.print(i + ".   : " + readString(i*150) + " ");
//				dates.add(readString(i*150));
//				for(int j = 0; j < 12; j++){
//					System.out.print(readBinary((i*150)+ 25 + (j * 8) ) + " ");
//				}
//				i++;
//				System.out.println();
//			}
//		} catch (Exception e) {
//			//e.printStackTrace();
//			//return;
//			System.exit(1);
//		}

        //
        // binary searches csv file and prints out data from the search
        //

//        int position = binarySearch(1, (int) getBinaryFileLength(), "2001-02-12T19:20:35.040");
//        int position2 = binarySearch(1, (int) getBinaryFileLength(), "2001-02-12T09:20:35.040");
//        System.out.print(readString(position));
//        for(int i = 0; i<12; i++){
//        	System.out.print(" " + readBinary(position + 25 + (i * 8)));
//        }
//        System.out.println();
//        System.out.print(readString(position2));
//        for(int i = 0; i<12; i++){
//        	System.out.print(" " + readBinary(position2 + 25 + (i * 8)));
//        }

	}

//	public static int binarySearch(int first, int last, String target){
//		if(first > last){
//			System.out.println(first + " " + last);
//			System.out.println("!");
//			return (last) * lineLength;
//		}else{
//			int middle = (first+last)/2;
//			int compResult = target.compareTo(readString((middle - 1) * lineLength));
//			if(compResult == 0)
//				return (middle - 1) * lineLength;
//			else if(compResult < 0)
//				return binarySearch(first, middle - 1, target);
//			else
//				return binarySearch(middle + 1, last, target);
//		}
//	}

//	public static long getBinaryFileLength(){
//		long length = 0;
//		try {
//			RandomAccessFile fileStore = new RandomAccessFile(path, "r");
//			length = fileStore.length()/lineLength;
//			System.out.println(length);
//			fileStore.close();
//		} catch (Exception e) {
//			return length;
//		}
//		return length;
//	}

//	public static String readString(int postion){
//		String string = "";
//		try {
//			RandomAccessFile fileStore = new RandomAccessFile(path, "r");
//				fileStore.seek(postion);
//				string = fileStore.readUTF();
//				fileStore.close();
//		} catch (Exception e) {
//			return "";
//		}
//		return string;
//	}
//
//	public static double readBinary(int postion){
//		double num = 0;
//		try {
//			RandomAccessFile fileStore = new RandomAccessFile(path, "r");
//			fileStore.seek(postion);
//			num = fileStore.readDouble();
//			fileStore.close();
//		} catch (Exception e) {
//			return 0;
//		}
//		return  num;
//	}

	public static void writeString(String text, int position){
		try {
			RandomAccessFile fileStore = new RandomAccessFile(path, "rw");

			fileStore.seek(position);

			fileStore.writeUTF(text);

			fileStore.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void writeBinary(String text, int position){
		try {
			RandomAccessFile fileStore = new RandomAccessFile(path, "rw");

			fileStore.seek(position);

			fileStore.writeDouble(Double.parseDouble(text));

			fileStore.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
