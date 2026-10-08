package io.github.easychat.chat;
import io.github.easychat.config.ChatSettings;
import java.nio.file.*;

/** Opt-in final-JAR check: bundled preparation, persistent deletion and cache reuse. */
public final class TextCatalogIntegration {
    private static void waitFor(TextTranslationService service)throws Exception{long until=System.nanoTime()+30000000000L;while(service.busy&&System.nanoTime()<until)Thread.sleep(25);if(service.busy||service.status.contains("失败"))throw new AssertionError(service.status);}
    public static void main(String[] args)throws Exception{
        Path root=Paths.get(args[0]);Files.createDirectories(root);ChatSettings settings=new ChatSettings(root.resolve("settings.properties"));
        Path cache=root.resolve("text-model/opus-zh-en/encoder_model_quantized.onnx");
        try(TextTranslationService service=new TextTranslationService(root,settings)){
            service.scan();service.prepare("opus-zh-en");waitFor(service);
            if(!service.exists("opus-zh-en")||!service.ready("opus-zh-en"))throw new AssertionError("bundled prepare");
            System.out.println("ZH_EN="+service.translate("你好",true,settings.speechTextModel));
        }
        long modified=Files.getLastModifiedTime(cache).toMillis();settings.load();
        try(TextTranslationService service=new TextTranslationService(root,settings)){
            service.scan();service.prepare("opus-zh-en");waitFor(service);if(modified!=Files.getLastModifiedTime(cache).toMillis())throw new AssertionError("valid cache rewritten across restart");
            service.delete("opus-zh-en");waitFor(service);if(service.ready("opus-zh-en")||!settings.translateIncoming||!settings.speechTextModel.isEmpty())throw new AssertionError("delete affected other direction or failed to disable");
        }
        settings.load();try(TextTranslationService service=new TextTranslationService(root,settings)){
            service.scan();if(service.ready("opus-zh-en")||Files.exists(cache))throw new AssertionError("deleted model restored on startup");
            service.prepare("opus-zh-en");waitFor(service);if(!service.ready("opus-zh-en")||!Files.exists(cache))throw new AssertionError("explicit restore failed");
            if(!"opus-zh-en".equals(settings.speechTextModel))throw new AssertionError("default direction not restored");service.select("opus-zh-en",true);
        }
        System.out.println("EASYCHAT_TEXT_CATALOG=PASS: prepare Chinese to English, inference, restart reuse, delete/disable, persistent removal, explicit restore, selection separate");
    }
}
