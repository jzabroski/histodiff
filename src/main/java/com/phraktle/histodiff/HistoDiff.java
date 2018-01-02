package com.phraktle.histodiff;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HistoDiff {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out
                    .println("Usage: HistoDiff file1/url1 file2/url2 [sortBy] [threshold]");
            System.exit(1);
        }
        File file1 = parseFileArg(args, 0);
        File file2 = parseFileArg(args, 1);
        int sortBy = parseArg(args, 2);
        int threshold = parseArg(args, 3);

        dump(sort(sortBy,
                filter(sortBy, threshold, diff(parse(file1), parse(file2)))));
    }

    static File parseFileArg(String[] args, int idx) throws IOException {
        if (args[idx].startsWith("http://")) {
            File targetFile = File.createTempFile("histodiff-temp-file-", ".tmp");
            targetFile.deleteOnExit();
            URL httpTarget = new URL(args[idx]);
            try (InputStream in = httpTarget.openStream()) {
                Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return targetFile;
        } else {
            return new File(args[idx]);
        }
    }

    static int parseArg(String[] args, int idx) {
        return args.length <= idx ? 0 : Integer.parseInt(args[idx]);
    }

    static List<Entry<String, int[]>> filter(int byIndex, int threshold,
            List<Entry<String, int[]>> histo) {
        List<Entry<String, int[]>> filtered = new ArrayList<>();
        for (Entry<String, int[]> e : histo) {
            if (Math.abs(e.getValue()[byIndex]) > threshold) {
                filtered.add(e);
            }
        }
        return filtered;
    }

    static List<Entry<String, int[]>> sort(final int byIndex,
            List<Entry<String, int[]>> histo) {
        Collections.sort(histo, new Comparator<Entry<String, int[]>>() {
            public int compare(Entry<String, int[]> a, Entry<String, int[]> b) {
                return -Integer.compare(a.getValue()[byIndex],
                        b.getValue()[byIndex]);
            }

        });
        return histo;
    }

    static void dump(List<Entry<String, int[]>> diff) {
        System.out.println("  #instances       #bytes  class name");
        System.out.println("-------------------------------------");
        for (Entry<String, int[]> e : diff) {
            int[] v = e.getValue();
            System.out.printf("%+12d %+12d  %s%n", v[0], v[1], e.getKey());
        }
    }

    static Map<String, int[]> parse(File file) throws IOException {
        Pattern p = Pattern.compile(" *(\\d+): +([\\d]+) +([\\d]+) +(.+)");
        Map<String, int[]> map = new HashMap<>();
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                Matcher matcher = p.matcher(line);
                if (matcher.matches()) {

                    int instances = Integer.parseInt(matcher.group(2));
                    int bytes = Integer.parseInt(matcher.group(3));
                    String className = matcher.group(4);
                    map.put(className, new int[] { instances, bytes });

                }
            }
        }
        return map;
    }

    static List<Entry<String, int[]>> diff(Map<String, int[]> histo1,
            Map<String, int[]> histo2) {

        List<Entry<String, int[]>> diff = new ArrayList<>();

        for (Entry<String, int[]> e : histo1.entrySet()) {
            String c = e.getKey();
            int[] a = e.getValue();
            int[] b = histo2.remove(c);
            diff.add(newEntry(c, diff(a, b)));
        }

        // remaining in histo2 were not in histo1
        for (Entry<String, int[]> e : histo2.entrySet()) {
            String c = e.getKey();
            int[] b = e.getValue();
            diff.add(newEntry(c, diff(null, b)));
        }

        return diff;
    }

    static Entry<String, int[]> newEntry(String c, int[] diff) {
        return new AbstractMap.SimpleEntry<>(c, diff);
    }

    static int[] diff(int[] a, int[] b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            b = new int[a.length];
        }
        int[] diff = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            diff[i] = b[i] - a[i];
        }
        return diff;
    }

}
