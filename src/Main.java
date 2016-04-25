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
//    private static ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(cores);
    //    private static String wordToFind;
    private static List<Result> rList = new ArrayList<>();
    private static List<String> sList = new ArrayList<>();
    private static Stats searchStats;
    private static int freq;
    private static String word;

    private static Result anyResult;
    private static boolean tick = false;

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
//        System.out.println(cores);
        Path path = Paths.get(System.getProperty("user.dir"));
        Path cpath = Paths.get("/home/troels/Documents/test2");
word = WordFinder.stats(cpath).leastFrequent();
//        rList = WordFinder.findAll("for", cpath);
//        System.out.println("stats");
//        searchStats = WordFinder.stats(cpath);
//        rList = WordFinder.stats(cpath).mostFrequent("HEJ");
//        while (true) {
//            if (pool.getActiveCount() < 1) break;
//            else if (tick) pool.shutdownNow();
////            else System.out.println(pool.getActiveCount());
//        }
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
//        rList.addAll(rListLong);
        System.out.println(totalTime + " ms");
        System.out.println(rList.size() + " results");
//        System.out.println(anyResult.path()+": line " + anyResult.line());
        System.out.println("------------" + word);
        sList.forEach(i-> System.out.println(i));
//        System.out.println(searchStats.occurrences("for"));
//        rList.forEach(i -> System.out.println(i.path()+":"+i.line()));
//        pool.shutdown();
//        rList.forEach(i -> System.out.println(i.path()))782149 758014 782044 782188;+++
    }
}