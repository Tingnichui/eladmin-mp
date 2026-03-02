package me.zhengjie.utils;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class SnUtil {

    private static final Pattern PATTERN = Pattern.compile("^(.*?)(\\d+)$");

    public static void main(String[] args) {
        System.err.println(String.format("%.0f", Double.valueOf("0")));

    }





    public static String mergeToString(List<String> sns) {
        return String.join(",", mergeToList(sns));
    }

    public static List<String> mergeToList(List<String> sns) {

        if (sns == null || sns.isEmpty()) {
            return Collections.emptyList();
        }

        // 去重
        Set<String> unique = new HashSet<>(sns);

        // 分组
        Map<String, List<String>> group = new HashMap<>();

        for (String sn : unique) {
            Matcher m = PATTERN.matcher(sn);
            if (!m.matches()) {
                // 不符合规则的直接单独保存
                group.computeIfAbsent(sn, k -> new ArrayList<>()).add(sn);
                continue;
            }

            String prefix = m.group(1);
            String numStr = m.group(2);

            group.computeIfAbsent(prefix, k -> new ArrayList<>()).add(numStr);
        }

        List<String> result = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : group.entrySet()) {
            String prefix = entry.getKey();
            List<String> numsStr = entry.getValue();

            // 不规则的直接放入
            if (numsStr.size() == 1 && numsStr.get(0).equals(prefix)) {
                result.add(prefix);
                continue;
            }

            int len = numsStr.get(0).length();
            List<Long> nums = numsStr.stream()
                    .map(Long::parseLong)
                    .sorted().collect(Collectors.toList());

            long start = nums.get(0);
            long prev = start;

            for (int i = 1; i < nums.size(); i++) {
                long cur = nums.get(i);
                if (cur == prev + 1) {
                    prev = cur;
                } else {
                    result.add(build(prefix, start, prev, len));
                    start = cur;
                    prev = cur;
                }
            }
            result.add(build(prefix, start, prev, len));
        }

        return result;
    }

    private static String build(String prefix, long start, long end, int len) {
        if (start == end) {
            return prefix + pad(start, len);
        }
        return prefix + pad(start, len) + "-" + prefix + pad(end, len);
    }

    private static String pad(long num, int len) {
        return String.format("%0" + len + "d", num);
    }
}
