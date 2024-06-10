package demos.kafka;

import java.io.*;
import java.util.*;

public class Main {
    static List<List<Integer>>  list = new ArrayList<>();
    static Queue<int[]> queue = new LinkedList<>();
    static boolean[] visited ;

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out));
        StringTokenizer tokenizer = new StringTokenizer(br.readLine());
        int N = Integer.parseInt(tokenizer.nextToken());
        int M = Integer.parseInt(tokenizer.nextToken());
        visited = new boolean[N + 1];

        list.add(null);
        for (int i = 0; i < N; i++) {
            list.add(new ArrayList<>());
        }
        for (int i = 0; i < M; i++) {
            tokenizer = new StringTokenizer(br.readLine());
            int x = Integer.parseInt(tokenizer.nextToken());
            int y = Integer.parseInt(tokenizer.nextToken());
            if (!list.get(x).contains(y)) {
                list.get(x).add(y);
            }
            if (!list.get(y).contains(x)) {
                list.get(y).add(x);
            }
        }
        br.close();

        int minUser = Integer.MAX_VALUE;
        int minSum = Integer.MAX_VALUE;

        for (int i = 0; i < N; i++) {

            queue = new LinkedList<>();
            visited = new boolean[N + 1];
            int sum = bfs(i + 1);
            if (sum < minSum) {
                minSum = sum;
                minUser = i + 1;
            } else if (sum == minSum) {
                minUser = Math.min(minUser, i + 1);
            }
        }

        bw.write(String.valueOf(minUser));
        bw.flush();
        bw.close();
    }

    static int bfs(int index) {
        int sum = 0;
        int depth = 0;
        queue.offer(new int[] {index, depth});
        visited[index] = true;

        while (!queue.isEmpty()) {
            int[] polled = queue.poll();
            int user = polled[0];
            List<Integer> friends = list.get(user);

            for (int i : friends) {
                if (!visited[i]) {
                    sum += polled[1] + 1;
                    visited[i] = true;
                    queue.offer(new int[] {i, polled[1] + 1});
                }
            }
        }
        return sum;
    }
}
