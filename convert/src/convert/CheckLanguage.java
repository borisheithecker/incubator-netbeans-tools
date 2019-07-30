/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package convert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author boris.heithecker
 */
public class CheckLanguage {

    private final static String CHARSET = "ISO-8859-1";
    private final static DecimalFormat HEX = (DecimalFormat) DecimalFormat.getInstance();

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Use: CheckLanguage <source-directory>");
            return;
        }
        HEX.setMinimumIntegerDigits(4);
//        final String locale = "src/vi";
//        final int sr = 0x00C0;
//        final int se = 0x1EF9;
//        
//        final String locale = "src/ar_EG";
//        final int sr = 0x0600;
//        final int se = 0x06FF;
//        
        
//                final String locale = "src/ar_SA";
//        final int sr = 0x0600;
//        final int se = 0x06FF;
        
//        final String locale = "src/de";
//        final int sr = 0x00f6;
//        final int se = 0x00f7;

//                final String locale = "src/el";
//        final int sr = 0x03B1;
//        final int se = 0x03C9;


//        final String locale = "src/hi_IN";
//        final int sr = 0x0900;
//        final int se = 0x097F;
//        
                final String locale = "src/ta_IN";
        final int sr = 0x0B80;
        final int se = 0x0BFA;
        
        final Path root = Paths.get(args[0], locale);
        final List<Path> found = new ArrayList<>();
        Files.find(root, Integer.MAX_VALUE, (p, attr) -> attr.isRegularFile() && isPropertiesFile(p))
                .forEach(file -> {
                    int c = sr;
                    try {
                        while (c != se) {
                            final String content = new String(Files.readAllBytes(file), CHARSET);
                            final String unicode = String.format("\\u%04X", c++);
//                            if (content.contains(String.valueOf(c++))) {
                            if (content.contains(unicode) || content.contains(unicode.toLowerCase())) {
                                found.add(file);
                                break;
                            }
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
        System.out.println("Count: " + found.size());
        if (args.length > 1) {
            Path log = Paths.get(args[1]);
            if (Files.isDirectory(log)) {
                log = log.resolve("files-found.txt");
            }
            Files.write(log, found.stream().map(Path::toString).collect(Collectors.toList()));
        }
    }

    private static boolean isPropertiesFile(Path p) {
        return p.getName(p.getNameCount() - 1).toString().endsWith(".properties");
    }

}
