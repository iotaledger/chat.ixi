package org.iota.ixi.utils;

import java.io.*;
import java.util.StringJoiner;

public final class FileOperations {

    public static void writeToFile(File file, String data) {
        try (PrintStream out = new PrintStream(new FileOutputStream(file))) {
            out.print(data);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readFromFile(File file) throws IOException {
        if(!file.exists())
            throw new IllegalArgumentException("file "+file.getAbsolutePath()+" does not exist");
        BufferedReader br = new BufferedReader(new FileReader(file));
        try {
            StringJoiner sj = new StringJoiner(System.lineSeparator());
            String line = br.readLine();
            while (line != null) {
                sj.add(line);
                line = br.readLine();
            }
            return sj.toString();
        } finally {
            br.close();
        }
    }
}
