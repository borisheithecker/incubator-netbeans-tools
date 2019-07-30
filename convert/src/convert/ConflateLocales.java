/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package convert;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author boris.heithecker
 */
public class ConflateLocales {

    private final static String CHARSET = "ISO-8859-1";

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Use: CategorizeLicenses <source-directory> <log-directory>");
            return;
        }
        final Path root = Paths.get(args[0], "src");
        final Path log = Paths.get(args[1]);
        if (!Files.isDirectory(log)) {
            System.err.println("Use: CategorizeLicenses <source-directory> <log-directory>");
            return;
        }
        Files.createDirectories(log.resolve("conflated"));
        Files.newDirectoryStream(root, (p) -> Files.isDirectory(p))
                .forEach(locDir -> {
                    final String p = locDir.toString();
                    try {
                        final List<String> conflated = conflateLocale(locDir);
                        if (!conflated.isEmpty()) {
                            final Path logLocale = log.resolve("conflated").resolve(locDir.getFileName());
                            Files.write(logLocale, conflated);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(ConflateLocales.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });

    }

    static List<String> conflateLocale(final Path locDir) throws IOException {
        final List<String> lines = new ArrayList<>();
        Files.find(locDir, Integer.MAX_VALUE, (p, attr) -> attr.isRegularFile() && isPropertiesFile(p))
                .forEach(file -> {
                    try {
                        Files.readAllLines(file, Charset.forName(CHARSET)).stream()
                                .filter(l -> !l.trim().isEmpty())
                                .filter(l -> !l.trim().startsWith("#"))
                                .forEach(lines::add);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
        return lines;
    }

    private static boolean isPropertiesFile(Path p) {
        return p.getName(p.getNameCount() - 1).toString().endsWith(".properties");
    }

}
