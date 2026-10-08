package io.github.easychat.engine;

import io.github.easychat.chat.TextTranslationService;
import io.github.easychat.config.ChatSettings;
import java.nio.file.*;

/** Opt-in final-JAR test: actual Java worker, both OPUS directions and model lifecycle. */
public final class ParaformerOpusIntegration {
    public static void main(String[] args) throws Exception {
        Path root=Paths.get(args[0]),audio=Paths.get(args[1]);Files.createDirectories(root);
        ModelStore models=new ModelStore(root);ModelStore.Model model=ModelStore.Model.PARAFORMER;
        ChatSettings settings=new ChatSettings(root.resolve("settings.properties"));
        models.prepare(model,s->{});long modified=Files.getLastModifiedTime(models.file(model)).toMillis();
        Path nativeRoot=root.resolve("runtime/paraformer-1.13.8/native");
        Files.createDirectories(nativeRoot);
        Path retired=nativeRoot.resolve("sherpa-onnx-jni.dll");
        Files.write(retired,new byte[]{1}); // Simulate the owned old TTS-enabled runtime cache.
        try(TextTranslationService text=new TextTranslationService(root,settings)) {
            text.scan();
            ParaformerEngine engine=new ParaformerEngine(root,models);
            try {
                engine.ensureStarted(model,System.out::println);
                if(Files.exists(retired))throw new AssertionError("retired JNI cache remains");
                Path dispatch=nativeRoot.resolve("jnidispatch.dll");
                long nativeModified=Files.getLastModifiedTime(dispatch).toMillis();
                for(String id:new String[]{"who_defends","bed","emeralds","not_blue","pearl"}) {
                    long start=System.nanoTime();String chinese=engine.transcribe(Files.readAllBytes(audio.resolve(id+".wav")));
                    String english=text.translate(chinese,true,settings.speechTextModel);
                    if(chinese.isEmpty()||!english.matches("(?s).*[A-Za-z].*"))throw new AssertionError("empty speech output");
                    if("who_defends".equals(id)&&!chinese.equals("谁来防守"))throw new AssertionError(chinese);
                    System.out.println("SPEECH "+id+" | "+chinese+" => "+english+" | "+(System.nanoTime()-start)/1000000+" ms");
                }
                // Cancellation must not leave an unusable protocol reader or locked native DLLs.
                engine.close();engine.ensureStarted(model,s->{});
                if(nativeModified!=Files.getLastModifiedTime(dispatch).toMillis())throw new AssertionError("valid native rewritten");
                if(!"谁来防守".equals(engine.transcribe(Files.readAllBytes(audio.resolve("who_defends.wav")))))throw new AssertionError("restart protocol broken");
            } finally {engine.shutdown();}
            for(String source:new String[]{"Defend bed","rush blue","I need blocks"}) {
                String result=text.translate(source,false,settings.incomingTextModel);
                if(!result.matches("(?s).*[\\u4e00-\\u9fff].*"))throw new AssertionError("missing Chinese: "+result);
                System.out.println("CHAT "+source+" => "+result);
            }
        }
        models.prepare(model,s->{});
        if(modified!=Files.getLastModifiedTime(models.file(model)).toMillis())throw new AssertionError("valid weight rewritten");
        models.deleteModel(model);
        if(models.ready(model)||Files.exists(models.file(model))||Files.exists(models.tokensFile(model)))throw new AssertionError("delete failed");
        try{models.prepare(model,s->{});throw new AssertionError("deleted model auto-restored");}catch(java.io.IOException expected){}
        models.download(model,s->{});if(!models.ready(model))throw new AssertionError("explicit restore failed");
        System.out.println("PARAFORMER_OPUS_INTEGRATION=PASS: Java8 worker, speech zh-en, chat en-zh, cancellation/restart, native and model cache reuse, retired JNI cleanup, persistent deletion and explicit restore");
    }
}
