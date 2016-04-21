import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by trpet15 - Troels Blicher Petersen <troels@newtec.dk> on 4/17/16.
 */
public class Main {
    static int cores = Runtime.getRuntime().availableProcessors() + 1;
    private static ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(cores);
    //    private static String wordToFind;
    private static List<Result> rList = new ArrayList<>();
    private static List<Result> rListLong = new ArrayList<>();

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
//        wordToFind = "test";
        System.out.println(cores);
        Path path = Paths.get(System.getProperty("user.dir"));
        Path cpath = Paths.get("/home/troels");
//        doAndMeasure("Great!");
        findAll("if", cpath);
//        System.out.println("Working Directory = " +
//                System.getProperty("user.dir"));


        //            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        while (true) {
            if (pool.getActiveCount() < 1) break;
//            else System.out.println(pool.getActiveCount());
        }
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        rList.addAll(rListLong);
        System.out.println(totalTime + " ms");
        System.out.println(rList.size() + " results");
        pool.shutdown();
//        rList.forEach(i -> System.out.println(i.path()))782149 758014 782044 782188;+++
    }

    public static void findAll(String word, Path dir) {
        try (
                DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)
        ) {
            for (Path path : dirStream) {
                if (Files.isDirectory(path)) {
                    findAll(word, path);
                } else {
                    if (path.toString().endsWith(".js") || path.toString().endsWith(".conf")|| path.toString().endsWith(".java")) {
//                        System.out.println(path.toString());
                        fileOperation(word, path);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fileOperation(String word, Path file) throws IOException {
        if ((file.getFileName().toFile().length() / 1048576) > 1) {
            int ln = 0;
            String line;
            BufferedReader reader = Files.newBufferedReader(file);
            while ((line = reader.readLine()) != null) {
                final String currentLine = line;
                final int thisLine = ln;
                pool.submit(() -> wordCounter(thisLine, word, currentLine, file));
                ln++;
            }
        } else {
            pool.submit(() -> {
                try {
                    fileWordCounter(word, file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static void fileWordCounter(String wordToFind, Path file) throws IOException {
        int ln = 0;
        String line;
        ArrayList<Result> localAL = new ArrayList<>();
        BufferedReader reader = Files.newBufferedReader(file);
        while ((line = reader.readLine()) != null) {
            final String currentLine = line;
            final int thisLine = ln;
            String lineWords[] = currentLine.split("\\s+");
            for (String word : lineWords) {
                if (word.equals(wordToFind)) {
                    localAL.add(new Result() {
                        @Override
                        public Path path() {
                            return file;
                        }

                        @Override
                        public int line() {
                            return thisLine;
                        }
                    });
                }

            }
            ln++;
        }
        synchronized (rList) {
            rListLong.addAll(localAL);
        }
    }

    private static void wordCounter(int ln, String wordToFind, String line, Path file) {
        String lineWords[] = line.split("\\s+");
        for (String word : lineWords) {
            if (word.equals(wordToFind)) {
                synchronized (rList) {
                    rList.add(new Result() {
                        @Override
                        public Path path() {
                            return file;
                        }

                        @Override
                        public int line() {
                            return ln;
                        }
                    });
                }
            }
        }
    }

//    public static void init(String word, Path dir) {
//        pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(cores);
//        pool.submit(() -> findAll(word, dir));
//    }

//    public static void doAndMeasure(String caption) {
//        long tStart = System.currentTimeMillis();
//        pool.submit(runnable);
//        System.out.println(caption + " took " + (System.currentTimeMillis() - tStart) + "ms");
//    }
}