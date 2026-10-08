package io.github.easychat.chat;

import io.github.easychat.config.ChatSettings;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Two independent OPUS directions. Serialized inference keeps native sessions bounded. */
public final class TextTranslationService implements AutoCloseable {
    public static final String OPUS="opus";
    private final Path root;
    private final ChatSettings settings;
    private final Map<String,LocalTextTranslator> local=new HashMap<>();
    private final Set<String> damaged=ConcurrentHashMap.newKeySet();
    private final ExecutorService resources=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"easychat-text-resources");t.setDaemon(true);return t;});
    private volatile Future<?> job;
    public volatile boolean busy;
    public volatile String operatingModel="",status="OPUS 中译英和英译中均已内置";
    public TextTranslationService(Path root,ChatSettings settings) {
        this.root=root;this.settings=settings;
        for(TextModelCatalog.Model model:TextModelCatalog.all())local.put(model.id,new LocalTextTranslator(root,model));
    }
    private Path removed(TextModelCatalog.Model model) { return root.resolve(model.directory).resolve(".removed"); }
    public boolean ready(String id) {
        if(OPUS.equals(id))id="opus-en-zh";
        TextModelCatalog.Model model=TextModelCatalog.find(id);
        return model!=null&&!Files.exists(removed(model))&&!damaged.contains(id);
    }
    public boolean exists(String id) { return local.containsKey(id)&&local.get(id).cacheExists(); }
    public String modelState(String id) {
        TextModelCatalog.Model model=TextModelCatalog.find(id);if(model==null)return "未支持的模型";
        if(busy&&id.equals(operatingModel))return status;
        return Files.exists(removed(model))?"本地副本已删除":damaged.contains(id)?"副本损坏 · 可恢复":exists(id)?"可用 · 已释放到游戏目录":"可用 · 首次使用自动释放";
    }
    public void scan() throws IOException {
        for(TextModelCatalog.Model model:TextModelCatalog.all()) {
            if(exists(model.id)&&!local.get(model.id).cacheValid())damaged.add(model.id);
        }
        reconcile();
    }
    public String fallback(boolean toEnglish) { TextModelCatalog.Model model=TextModelCatalog.resolve(OPUS,toEnglish);return ready(model.id)?model.id:""; }
    private boolean usable(String id,boolean toEnglish) { TextModelCatalog.Model model=TextModelCatalog.resolve(id,toEnglish);return model!=null&&model.supports(toEnglish)&&ready(model.id); }
    private void reconcile() throws IOException {
        settings.speechTextModel=usable(settings.speechTextModel,true)?TextModelCatalog.resolve(settings.speechTextModel,true).id:fallback(true);
        settings.incomingTextModel=usable(settings.incomingTextModel,false)?TextModelCatalog.resolve(settings.incomingTextModel,false).id:fallback(false);
        if(settings.incomingTextModel.isEmpty())settings.translateIncoming=false;
        settings.save();
    }
    public static String label(String id) { TextModelCatalog.Model model=TextModelCatalog.find(id);return model!=null?model.name:OPUS.equals(id)?"OPUS-MT":"无可用模型"; }
    public void select(String id,boolean speech) throws IOException {
        if(busy||!usable(id,speech))return;
        String selected=TextModelCatalog.resolve(id,speech).id;
        if(speech)settings.speechTextModel=selected;else settings.incomingTextModel=selected;
        settings.save();
    }
    public synchronized String translate(String source,boolean toEnglish,String id) throws Exception {
        if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("翻译已取消");
        TextModelCatalog.Model model=TextModelCatalog.resolve(id,toEnglish);
        if(model==null||!model.supports(toEnglish)||!ready(model.id))throw new IOException("OPUS 模型不可用，请在模型管理中恢复");
        return local.get(model.id).translate(source);
    }
    public String phase() { TextModelCatalog.Model model=TextModelCatalog.resolve(settings.incomingTextModel,false);return model==null?"等待消息":local.get(model.id).phase; }
    public String backendStatus() { return "Paraformer 和 OPUS · CPU 本地推理"; }
    public void prepare(String id) { if(!local.containsKey(id))return;task(id,()->{local.get(id).prepare();Files.deleteIfExists(removed(TextModelCatalog.find(id)));damaged.remove(id);reconcile();status=label(id)+"已准备";}); }
    // OPUS consists of three curated bundled files; arbitrary external formats are not accepted.
    public void importModel(String id,Path source) { status="OPUS 已内置，请使用恢复按钮"; }
    public void delete(String id) {
        if(!local.containsKey(id))return;
        task(id,()->{synchronized(this){local.get(id).deleteCachedModel();Path marker=removed(TextModelCatalog.find(id));Files.createDirectories(marker.getParent());Files.write(marker,new byte[]{1});reconcile();status=label(id)+"本地副本已删除";}});
    }
    private interface Task { void run() throws Exception; }
    private synchronized void task(String id,Task task) {
        if(busy)return;busy=true;operatingModel=id;status="正在准备 "+label(id);
        job=resources.submit(()->{try{task.run();}catch(Exception error){status="模型操作失败："+error.getMessage();}finally{try{io.github.easychat.engine.ModelFileRegistry.at(root).refresh();}catch(IOException error){status="文件登记失败："+error.getMessage();}busy=false;operatingModel="";}});
    }
    public synchronized long clearDownloadCache() throws IOException {
        if(busy)throw new IOException("请等待模型操作完成");long bytes=0;for(LocalTextTranslator translator:local.values())bytes+=translator.clearDownloadCache();return bytes;
    }
    public void cancel() { Future<?> current=job;if(current!=null)current.cancel(true); }
    public void cancelInference() { /* Worker interruption is checked between OPUS decoder steps. */ }
    @Override public void close() { cancel();resources.shutdownNow();synchronized(this){for(LocalTextTranslator translator:local.values())translator.close();} }
}
