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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author boris.heithecker
 */
public class AddLicenseToUnlicensedPropertiesFiles {

    private final static String CHARSET = "ISO-8859-1";
    private final static String[] KEYWORDS = {"License", "license", "CDDL", "Oracle", "Sun", "Apache"};

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Use: CategorizeLicenses <source-directory>");
            return;
        }
        final Path root = Paths.get(args[0]);
        final List<Path> changed = new ArrayList<>();
        Files.find(root, Integer.MAX_VALUE, (p, attr) -> attr.isRegularFile() && isPropertiesFile(p))
                .forEach(file -> {
                    try {
                        final String content = new String(Files.readAllBytes(file), CHARSET);
                        final boolean noCommentLine = Arrays.stream(content.split("\n"))
                                .map(String::trim)
                                .noneMatch(s -> s.startsWith("#"));
                        final boolean noCommentWithLicense = Arrays.stream(content.split("\n"))
                                .map(String::trim)
                                .filter(s -> s.startsWith("#"))
                                .noneMatch(AddLicenseToUnlicensedPropertiesFiles::commentContainsLicenseKeyword);
                        if (noCommentLine || noCommentWithLicense) {
                            final String newContent = Convert.BUNDLE_OUTPUT + content;
                            Files.write(file, newContent.getBytes(CHARSET));
                            final Path rel = root.relativize(file);
                            changed.add(rel);
                            System.out.println(rel.toString());
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
        System.out.println("Count: " + changed.size());
        if (args.length > 1) {
            Path log = Paths.get(args[1]);
            if (Files.isDirectory(log)) {
                log = log.resolve("license-added.txt");
            }
            Files.write(log, changed.stream().map(Path::toString).collect(Collectors.toList()));
        }
    }

    static boolean commentContainsLicenseKeyword(String s) {
        return Arrays.stream(KEYWORDS)
                .anyMatch(s::contains);
    }

    private static boolean isPropertiesFile(Path p) {
        return p.getName(p.getNameCount() - 1).toString().endsWith(".properties");
    }

}
