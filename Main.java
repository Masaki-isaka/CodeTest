import java.util.List;

public class Main {
    public static void main(String[] args) {
        // ユーザID
        Integer userId = 23356;
        CodeTest service = new CodeTest();

        System.out.println("--- 1ページ目 ---");
        List<JobPost> result1Page = service.interleaving(userId, 1, 10);
        System.out.println(result1Page);
        System.out.println("--- 2ページ目 ---");
        List<JobPost> result2Page = service.interleaving(userId, 2, 10);
        System.out.println(result2Page);
        System.out.println("--- 3ページ目 ---");
        List<JobPost> result3Page = service.interleaving(userId, 3, 10);
        System.out.println(result3Page);
        System.out.println("--- 4ページ目 ---");
        List<JobPost> result4Page = service.interleaving(userId, 4, 10);
        System.out.println(result4Page);
    }
}
