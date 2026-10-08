package io.github.easychat.chat;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;

/** Small Java implementation for this model's Unigram + WhitespaceSplit/Metaspace vocabulary. */
public final class UnigramTokenizer {
    private static final class Node {
        final Map<Character, Node> next = new HashMap<>();
        int id = -1; float score;
    }
    private final Node root = new Node();
    private final String[] pieces;
    private int chinese = -1;
    public UnigramTokenizer(Path file) throws IOException {
        JsonArray vocabulary;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            vocabulary = new JsonParser().parse(reader).getAsJsonObject().getAsJsonObject("model").getAsJsonArray("vocab");
        }
        pieces = new String[vocabulary.size()];
        for (int id = 0; id < pieces.length; id++) {
            JsonArray item = vocabulary.get(id).getAsJsonArray();
            String piece = item.get(0).getAsString(); pieces[id] = piece;
            if (piece.equals(">>cmn_Hans<<")) chinese = id;
            if (piece.startsWith("<")) continue;
            Node node = root;
            for (int i = 0; i < piece.length(); i++) node = node.next.computeIfAbsent(piece.charAt(i), c -> new Node());
            node.id = id; node.score = item.get(1).getAsFloat();
        }
    }
    public long[] encode(String source) {
        List<Long> ids = new ArrayList<>();
        if (chinese >= 0) ids.add((long) chinese);
        String normalized = Normalizer.normalize(source, Normalizer.Form.NFKC).replaceAll("\\p{Cc}", " ").trim();
        for (String word : normalized.split("\\s+")) {
            if (word.isEmpty()) continue;
            String text = "▁" + word;
            float[] scores = new float[text.length() + 1]; Arrays.fill(scores, Float.NEGATIVE_INFINITY); scores[0] = 0;
            int[] previous = new int[scores.length], tokens = new int[scores.length];
            for (int start = 0; start < text.length(); start++) {
                if (scores[start] == Float.NEGATIVE_INFINITY) continue;
                Node node = root;
                for (int end = start; end < text.length(); end++) {
                    node = node.next.get(text.charAt(end)); if (node == null) break;
                    if (node.id >= 0 && scores[start] + node.score > scores[end + 1]) {
                        scores[end + 1] = scores[start] + node.score; previous[end + 1] = start; tokens[end + 1] = node.id;
                    }
                }
                int end = text.offsetByCodePoints(start, 1);
                if (scores[start] - 100 > scores[end]) { scores[end] = scores[start] - 100; previous[end] = start; tokens[end] = 1; }
            }
            List<Long> reversed = new ArrayList<>();
            for (int end = text.length(); end > 0; end = previous[end]) reversed.add((long) tokens[end]);
            Collections.reverse(reversed); ids.addAll(reversed);
        }
        ids.add(0L);
        if (ids.size() > 256) throw new IllegalArgumentException("聊天内容过长");
        long[] result = new long[ids.size()]; for (int i = 0; i < result.length; i++) result[i] = ids.get(i);
        return result;
    }
    public String decode(List<Long> ids) {
        StringBuilder text = new StringBuilder();
        for (long id : ids) if (id > 1 && id < pieces.length && !pieces[(int) id].startsWith("<")) text.append(pieces[(int) id]);
        return text.toString().replace('▁', ' ').replaceAll("\\s+", " ").trim();
    }
}
