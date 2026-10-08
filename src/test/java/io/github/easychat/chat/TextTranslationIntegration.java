package io.github.easychat.chat;
import java.nio.file.*;
public final class TextTranslationIntegration {
    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args[0]);
        Path directory = root.resolve("text-model/opus-zh-en");
        try (LocalTextTranslator translator = new LocalTextTranslator(root, true)) {
            for (String text : new String[] {"你好", "你守床，我去中间。", "给我一点铁，我需要方块。"}) {
                String english = translator.translate(text);
                System.out.println(text + " => " + english);
                if (!english.matches(".*[A-Za-z].*")) throw new AssertionError("missing English translation");
            }
            Path cached = directory.resolve("encoder_model_quantized.onnx");
            long modified = Files.getLastModifiedTime(cached).toMillis();
            Path unfinished = directory.resolve("tokenizer.json.part"), unrelated = directory.resolve("keep-me.txt");
            Files.write(unfinished, new byte[] {1, 2, 3}); Files.write(unrelated, new byte[] {4});
            if (translator.clearDownloadCache() != 3 || Files.exists(unfinished) || !Files.exists(unrelated)) throw new AssertionError("unsafe cache cleanup");
            if (Files.getLastModifiedTime(cached).toMillis() != modified) throw new AssertionError("cache cleanup replaced valid model");
            Files.delete(unrelated);
        }
        Files.write(directory.resolve("tokenizer.json"), new byte[] {0});
        try (LocalTextTranslator translator = new LocalTextTranslator(root, true)) {
            if (!translator.translate("你好").matches(".*[A-Za-z].*")) throw new AssertionError("corrupted resource not restored");
            translator.deleteCachedModel();
            if (Files.exists(directory.resolve("encoder_model_quantized.onnx")) || Files.exists(directory.resolve("decoder_model_quantized.onnx")) || Files.exists(directory.resolve("tokenizer.json"))) throw new AssertionError("text model deletion failed");
        }
        System.out.println("EASYCHAT_TEXT_RESOURCES=PASS: offline extraction, partial cleanup, preserve valid/unrelated files, restore corrupted tokenizer, delete text model");
    }
}
