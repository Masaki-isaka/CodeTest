package openlogicodetest;

import java.util.HashSet;

public class App {

    /** 開始URL */
    private static final String START_URL = "https://ja.wikipedia.org/wiki/%E3%82%A8%E3%83%AA%E3%82%A6%E3%83%89%E3%83%BB%E3%82%AD%E3%83%97%E3%83%81%E3%83%A7%E3%82%B2";

    /** 開始キーワード */
    private static final String START_KEY_WORD = "エリウド・キプチョゲ";

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        OpenLogiCodeTest openLogiCodeTest = new OpenLogiCodeTest();

        // 幅優先的に探索する
        openLogiCodeTest.searchAsBfs(START_URL, START_KEY_WORD, 0);

        // 探索したものを深さ優先的に出力する
        openLogiCodeTest.printTreeAsDfs(START_KEY_WORD, 0, new HashSet<>());

        System.out.println("\nExecution Time: " + (System.currentTimeMillis() - startTime) + "ms");

    }

}