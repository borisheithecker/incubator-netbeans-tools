/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package convert;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CategorizeLicenses {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Use: CategorizeLicenses <source-directory> <target-directory>");
            return;
        }
        Path root = Paths.get(args[0]);
        int[] recognizedCount = new int[1];
        Map<String, List<String>> licenses = new HashMap<>();
        Map<String, List<String>> paragraphs = new HashMap<>();
        Set<String> noCDDLNoSun = new HashSet<>();
        Set<String> cddlNotRecognized = new HashSet<>();
        Set<String> sun = new HashSet<>();
        Files.find(root, Integer.MAX_VALUE, (p, attr) -> attr.isRegularFile())
                .forEach(p -> {
                    try {
                        String path = root.relativize(p).toString();
                        String code = new String(Files.readAllBytes(p));

                        if (code.contains("CDDL")) {
                            Description lic = snipUnifiedLicenseOrNull(code, p);

                            if (lic != null) {
                                recognizedCount[0]++;
                                licenses.computeIfAbsent(lic.getInfo(), l -> new ArrayList<>()).add(path);
                                for (String par : lic.header.split("\n")) {
                                    paragraphs.computeIfAbsent(par, l -> new ArrayList<>()).add(path);
                                }
                                return;
                            }

                            cddlNotRecognized.add(path);
                            return;
                        }

                        if (code.contains("Sun")) {
                            Description lic = snipUnifiedLicenseOrNull(code, p);

                            if (lic != null) {
                                recognizedCount[0]++;
                                licenses.computeIfAbsent(lic.getInfo(), l -> new ArrayList<>()).add(path);
                                for (String par : lic.header.split("\n")) {
                                    paragraphs.computeIfAbsent(par, l -> new ArrayList<>()).add(path);
                                }
                                return;
                            }

                            sun.add(path);
                            return;
                        }

                        noCDDLNoSun.add(path);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });

        Path target = Paths.get(args[1]);

        int i = 0;
        for (Map.Entry<String, List<String>> e : licenses.entrySet()) {
            try ( Writer w = Files.newBufferedWriter(target.resolve("lic" + i++))) {
                w.write(e.getKey());
                w.write("\n\n");
                for (String file : e.getValue()) {
                    w.write(file);
                    w.write("\n");
                }
            }
        }
        System.err.println("files with recognized license headers: " + recognizedCount[0]);
        System.err.println("licenses count: " + licenses.size());
        System.err.println("paragraphs count: " + paragraphs.size());

        System.err.println("cddl, unrecognized file: " + cddlNotRecognized.size());
        System.err.println("sun, unrecognized file: " + sun.size());
        System.err.println("no cddl or sun license: " + noCDDLNoSun.size());

        dump(licenses, target, "lic");
        dump(paragraphs, target, "par");
        dump(Collections.singletonMap("Files which contain string CDDL, but their comment structure is not (yet) recognized.", cddlNotRecognized), target, "have-cddl-not-recognized-filetype");
        dump(Collections.singletonMap("Files which contain string Sun, but their comment structure is not (yet) recognized.", sun), target, "have-sun-not-recognized-filetype");
        dump(Collections.singletonMap("Files which do not contain string CDDL or Sun", noCDDLNoSun), target, "do-not-have-cddl-or-sun");
    }
    private static final Pattern YEARS_PATTERN = Pattern.compile("[12][019][0-9][0-9]([ \t]*[-,/][ \t]*[12][019][0-9][0-9])?");

    private static void dump(Map<String, ? extends Collection<String>> cat, Path target, String name) throws IOException {
        int i = 0;
        for (Map.Entry<String, ? extends Collection<String>> e : cat.entrySet()) {
            try ( Writer w = Files.newBufferedWriter(target.resolve(name + i++))) {
                w.write(e.getKey());
                w.write("\n\n");
                w.write("files:\n");
                e.getValue().stream().sorted().forEach(file -> {
                    try {
                        w.write(file);
                        w.write("\n");
                    } catch (IOException ex) {
                        throw new IllegalStateException(ex);
                    }
                });
            }
        }
    }

    private static final Map<String, Collection<Function<String, Description>>> extension2Convertor = new HashMap<>();

    static {
        enterExtensions(code -> snipLicense(code, "/\\*+", "\\*+/", "^[ \t]*\\**[ \t]*", CommentType.JAVA),
                "javx", "c", "h", "cpp", "pass", "hint", "css", "java", "js", "jj");
        enterExtensions(code -> snipLicense(code, "<!--+", "-+->", "^[ \t]*(-[ \t]*)?", CommentType.XML),
                "html", "xsd", "xsl", "dtd", "settings", "wstcgrp", "wstcref",
                "wsgrp", "xml", "xslt", "fxml", "wsdl", "sun-resource");
        enterExtensions(code -> snipLicenseBundle(code, "#!.*", "#", CommentType.PROPERTIES), "sh");
        enterExtensions(code -> snipLicenseBundle(code, null, "#", CommentType.PROPERTIES), "properties");
        enterExtensions(code -> snipLicenseBundle(code, null, "rem", CommentType.BAT1), "bat");
        enterExtensions(code -> snipLicenseBundle(code, null, "@rem", CommentType.BAT2), "bat");
    }

    private static void enterExtensions(Function<String, Description> convertor, String... extensions) {
        for (String ext : extensions) {
            extension2Convertor.computeIfAbsent(ext, x -> new ArrayList<>()).add(convertor);
        }
    }

    public static Description snipUnifiedLicenseOrNull(String code, Path file) {
        String fn = file.getFileName().toString();
        String ext = fn.substring(fn.lastIndexOf('.') + 1);
        return Stream.concat(extension2Convertor.getOrDefault(ext, Collections.emptyList())
                .stream(),
                extension2Convertor.values()
                        .stream()
                        .flatMap(c -> c.stream()))
                .map(c -> c.apply(code))
                .filter(desc -> desc != null)
                .findFirst()
                .orElse(null);
    }

    public static Description snipLicense(String code, String commentStart, String commentEnd, String normalizeLines, CommentType commentType) {
        Matcher startM = Pattern.compile(commentStart).matcher(code);
        if (!startM.find() || startM.start() > LIMIT) //only first 150 characters
        {
            return null;
        }
        Matcher endM = Pattern.compile(commentEnd).matcher(code);
        if (!endM.find(startM.end())) {
            return null;
        }
        String lic = code.substring(startM.end(), endM.start());
        if (!isLicenseText(lic)) {
            startM = Pattern.compile(commentStart).matcher(code);
            if (!startM.find(endM.end()) || startM.start() > LIMIT) //only first 150 characters
            {
                return null;
            }
            endM = Pattern.compile(commentEnd).matcher(code);
            if (!endM.find(startM.end())) {
                return null;
            }
            lic = code.substring(startM.end(), endM.start());
        }
        if (normalizeLines != null) {
            lic = Arrays.stream(lic.split("\n"))
                    .map(l -> l.replaceAll(normalizeLines, ""))
                    .collect(Collectors.joining("\n"));
        }
        return createUnifiedDescriptionOrNull(startM.start(), endM.end(), lic, commentType);
    }

    private static final int LIMIT = 300;

    public static Description snipLicenseBundle(String code, String firstLinePattern, String commentMarker, CommentType commentType) {
        StringBuilder res = new StringBuilder();
        boolean firstLine = true;
        int start = -1;
        int pos;
        int next = 0;
        int end = 0;
        String[] lines = code.split("\n");
        boolean startOfLic = false;
        int compromisingLineNumber = -1;
        String compromisingLine = null;
        boolean possiblyCodeBeforeLic = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            pos = next;
            next += line.length() + ((i + 1) < lines.length ? 1 : 0);
            line = line.trim();
            if (firstLine && firstLinePattern != null && Pattern.compile(firstLinePattern).matcher(line).matches()) {
                continue;
            }
            if (firstLine && line.trim().isEmpty()) {
                continue;
            }
            if (line.startsWith(commentMarker)) {
                String part = line.substring(commentMarker.length()).trim();
                if (firstLine && part.isEmpty()) {
                    continue;
                }
                if (firstLine) {
                    //Start collecting line of the license only if this line is really the beginning of the license text.
                    if (!isStartOfLicense(part)) {
                        continue;
                    }
                    start = pos;
                    startOfLic = true;
                }
                firstLine = false;

                res.append(part);
                res.append("\n");
                if (!part.isEmpty()) {
                    end = next;
                }

//                final Description ret = createUnifiedDescriptionOrNull(start, end, res.toString(), commentType);
//                if (Convert.isValidLicenseHeader(ret)) {
//                    ret.valid = true;
//                    ret.possiblyCodeBeforeLic = possiblyCodeBeforeLic;
//                    return ret;
//                }
                final boolean before = startOfLic;
                startOfLic = startOfLic && isStartOfLicense(res.toString());
                if (before && !startOfLic) {
                    compromisingLine = line;
                    compromisingLineNumber = i;
                }
            } else {
                if (line.trim().isEmpty()) {
                    //Skip empty lines
                    continue;
                } else if (firstLine) {
                    //Handle cases where lines of code are inserted before the license header
                    possiblyCodeBeforeLic = true;
                    continue;
                }
                final Description ret = createUnifiedDescriptionOrNull(start, end, res.toString(), commentType);
                if (Convert.isValidLicenseHeader(ret)) {
                    ret.valid = true;
                    ret.possiblyCodeBeforeLic = possiblyCodeBeforeLic;
                } else if (compromisingLine != null) {
                    ret.compromisingLine = compromisingLine;
                    ret.compromisingLineNumber = compromisingLineNumber;
                }
                return ret;
            }
        }
        final Description ret = createUnifiedDescriptionOrNull(start, end, res.toString(), commentType);
        if (Convert.isValidLicenseHeader(ret)) {
            ret.valid = true;
            ret.possiblyCodeBeforeLic = possiblyCodeBeforeLic;
        } else if (compromisingLine != null) {
            ret.compromisingLine = compromisingLine;
            ret.compromisingLineNumber = compromisingLineNumber;
        }
        return ret;
    }

    static boolean isStartOfLicense(String part) {
        final Matcher m1 = Convert.headerPattern1.matcher(normalize(part));
        final Matcher m2 = Convert.headerPattern2.matcher(normalize(part));
        final Matcher m3 = Convert.headerPatternSPN.matcher(normalize(part));
        final boolean isStartOfLicense = !m1.matches() && m1.hitEnd()
                || !m2.matches() && m2.hitEnd()
                || !m3.matches() && m3.hitEnd();
        return isStartOfLicense;
    }

    private static Description createUnifiedDescriptionOrNull(int start, int end, String lic, CommentType commentType) {
        if (lic != null && isLicenseText(lic)) {
            if (start == (-1)) {
                throw new IllegalStateException();
            }
            lic = normalize(lic);
            return new Description(start, end, lic, commentType);
        }
        return null;
    }

    private static String normalize(String lic) {
        lic = YEARS_PATTERN.matcher(lic).replaceAll(Matcher.quoteReplacement("<YEARS>"));
        lic = lic.replaceAll("\\Q<p/>\\E", "\n"); //normalize <p/> to newlines
        lic = lic.replaceAll("([^\n])\n([^\n])", "$1 $2");
        lic = lic.replaceAll("[ \t]+", " ");
        lic = lic.replaceAll("\n+", "\n");
        lic = lic.replaceAll("^\n+", "");
        lic = lic.replaceAll("\n+$", "");
        return lic;
    }

    private static boolean isLicenseText(String text) {
        return text.contains("CDDL")
                || text.contains("Sun Public License")
                || text.contains("Redistribution")
                || text.contains("Apache License");
    }

    public static class Description {

        public final int start;
        public final int end;
        public final String header;
        public final CommentType commentType;

        public int compromisingLineNumber = -1;
        public String compromisingLine = null;
        boolean valid;

        boolean possiblyCodeBeforeLic = false;

        public Description(int start, int end, String header, CommentType commentType) {
            this.start = start;
            this.end = end;
            this.header = header;
            this.commentType = commentType;
        }

        String getInfo() {
            final StringJoiner sj = new StringJoiner("\n\n");
            sj.add(header);
            if (compromisingLine != null) {
                final String msg = "The beginning lines up to line " + this.compromisingLineNumber + " correspond to the beginning of a valid header. The compromising line is: ";
                sj.add(msg);
                sj.add(compromisingLine);
            }
            if (valid) {
                if (possiblyCodeBeforeLic) {
                    final String msg = "Possibly contains lines of code before the start of the header at line " + this.start;
                    sj.add(msg);
                }
                sj.add("This is a valid license.");
            }
            return sj.toString();
        }
    }

    public enum CommentType {
        JAVA,
        XML,
        PROPERTIES,
        BAT1,
        BAT2;
    }
}
