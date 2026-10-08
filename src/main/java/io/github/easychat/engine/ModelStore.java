package io.github.easychat.engine;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.function.Consumer;

public final class ModelStore {
    public enum Model {
        PARAFORMER("paraformer-zh-int8", "Paraformer 中文识别", 217,
                "90bc03034ae1bef9575f8cc798cd1519c8be8aa9e8b458a033e32017ff4d584c");
        public final String id, label, sha256; public final int mib;
        Model(String id, String label, int mib, String hash) { this.id=id; this.label=label; this.mib=mib; sha256=hash; }
        public long maximumBytes() { return 227330205L; }
        public static Model from(String id) { return PARAFORMER; }
    }
    public static final String TOKEN_HASH="6c0e3b35cece259829e6cb5b8d90d13db88f61ea3a2953d11898e4b2bfd7a2e2";
    private final Path directory;
    private volatile String validated;
    private volatile HttpURLConnection active;
    public ModelStore(Path root) { directory=root.resolve("models/paraformer-zh-int8"); }
    public Path file(Model model) { return directory.resolve("model.int8.onnx"); }
    public Path tokensFile(Model model) { return directory.resolve("tokens.txt"); }
    private Path disabledFile() { return directory.resolve(".removed"); }
    private String stamp(Model model) throws IOException {
        return Files.size(file(model))+":"+Files.getLastModifiedTime(file(model)).toMillis()+":"+Files.size(tokensFile(model))+":"+Files.getLastModifiedTime(tokensFile(model)).toMillis();
    }
    public boolean exists(Model model) { return Files.isRegularFile(file(model)); }
    public boolean ready(Model model) {
        try { return !Files.exists(disabledFile()) && stamp(model).equals(validated); } catch(IOException missing) { return false; }
    }
    public List<Model> available() { return ready(Model.PARAFORMER) ? Collections.singletonList(Model.PARAFORMER) : Collections.emptyList(); }
    private String bundledPath(String name) { return "/assets/easychat/models/paraformer-zh-int8/"+name; }
    public boolean bundled(Model model) { return getClass().getResource(bundledPath("model.int8.onnx"))!=null; }
    public boolean autoPrepare(Model model) { return bundled(model)&&!Files.exists(disabledFile()); }
    private boolean valid(Path path,String hash) throws IOException { return Files.isRegularFile(path)&&hash.equals(digest(path,"SHA-256")); }
    public void scan() throws IOException {
        Model model=Model.PARAFORMER;
        validated=valid(file(model),model.sha256)&&valid(tokensFile(model),TOKEN_HASH)?stamp(model):null;
    }
    public synchronized void prepare(Model model,Consumer<String> progress) throws IOException {
        if (Files.exists(disabledFile())) throw new IOException("模型已删除，请在模型管理中恢复");
        progress.accept("正在校验 Paraformer 模型");
        if (!valid(file(model),model.sha256)) release("model.int8.onnx",model.sha256,progress);
        if (!valid(tokensFile(model),TOKEN_HASH)) release("tokens.txt",TOKEN_HASH,progress);
        verify(model);
    }
    private void release(String name,String hash,Consumer<String> progress) throws IOException {
        Files.createDirectories(directory); Path target=directory.resolve(name),part=directory.resolve(name+".part");
        try (InputStream input=getClass().getResourceAsStream(bundledPath(name))) {
            if(input==null)throw new IOException("缺少 Paraformer 模型，请下载或离线导入");
            progress.accept("正在释放内置 Paraformer 模型，首次使用需要稍候");
            try {
                try (OutputStream output=Files.newOutputStream(part)) {
                    byte[] buffer=new byte[65536];int n;
                    while((n=input.read(buffer))!=-1) { if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("已取消");output.write(buffer,0,n); }
                }
                if(!hash.equals(digest(part,"SHA-256")))throw new IOException("内置 Paraformer 模型校验失败");
                move(part,target);
            } finally { Files.deleteIfExists(part); }
        }
    }
    private void move(Path source,Path target) throws IOException {
        try { Files.move(source,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
        catch(AtomicMoveNotSupportedException fallback) { Files.move(source,target,StandardCopyOption.REPLACE_EXISTING); }
    }
    public void verify(Model model) throws IOException {
        if(!valid(file(model),model.sha256)||!valid(tokensFile(model),TOKEN_HASH))throw new IOException("Paraformer 文件不完整或校验失败，请恢复、下载或导入");
        validated=stamp(model);
    }
    public synchronized long clearDownloadCache() throws IOException {
        long bytes=0;
        for(String name:new String[]{"model.int8.onnx.part","tokens.txt.part","import.part"}) {
            Path path=directory.resolve(name);
            if(Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)) { bytes+=Files.size(path);Files.delete(path); }
        }
        return bytes;
    }
    public synchronized void deleteModel(Model model) throws IOException {
        Files.createDirectories(directory);Files.write(disabledFile(),new byte[]{1});
        Files.deleteIfExists(file(model));Files.deleteIfExists(tokensFile(model));clearDownloadCache();validated=null;
    }
    public void download(Model model,Consumer<String> progress) throws IOException { download(model,progress,io.github.easychat.download.DownloadSources.Source.OFFICIAL); }
    public void download(Model model,Consumer<String> progress,io.github.easychat.download.DownloadSources.Source source) throws IOException {
        if(bundled(model)) { Files.deleteIfExists(disabledFile());prepare(model,progress);return; }
        String url=io.github.easychat.download.DownloadSources.selected(io.github.easychat.download.DownloadSources.links(model.id,null,null,null),source).get(0).url;
        fetch(model,url,progress);release("tokens.txt",TOKEN_HASH,progress);Files.deleteIfExists(disabledFile());verify(model);
    }
    private void fetch(Model model,String source,Consumer<String> progress) throws IOException {
        Files.createDirectories(directory);Path part=directory.resolve("model.int8.onnx.part");
        long offset=Files.exists(part)?Files.size(part):0;
        if(offset>model.maximumBytes()) { Files.delete(part);offset=0; }
        if(offset==model.maximumBytes()&&valid(part,model.sha256)) { move(part,file(model));return; }
        HttpURLConnection connection=(HttpURLConnection)new URL(source).openConnection();active=connection;
        connection.setConnectTimeout(20000);connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent","EasyChat/0.1.0");if(offset>0)connection.setRequestProperty("Range","bytes="+offset+"-");
        try {
            int status=connection.getResponseCode();
            if(status!=200&&status!=206)throw new IOException("下载失败（HTTP "+status+"），可切换下载源或离线导入");
            String range=connection.getHeaderField("Content-Range");
            if(status==206&&(offset==0||range==null||!range.startsWith("bytes "+offset+"-")))throw new IOException("续传位置无效");
            if(status==200)offset=0;
            long size=offset,start=System.nanoTime(),updated=0,initial=offset;
            try(InputStream input=connection.getInputStream();OutputStream output=new FileOutputStream(part.toFile(),offset>0)) {
                byte[] buffer=new byte[65536];int n;
                while((n=input.read(buffer))!=-1) {
                    if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("已取消，下次可续传");
                    size+=n;if(size>model.maximumBytes())throw new IOException("模型下载大小异常");output.write(buffer,0,n);
                    if(System.currentTimeMillis()-updated>250) { progress.accept(io.github.easychat.download.DownloadSources.speed(size-initial,start)+" · "+size/1048576+" / "+model.mib+" MiB");updated=System.currentTimeMillis(); }
                }
            }
            if(!valid(part,model.sha256)) { Files.deleteIfExists(part);throw new IOException("模型校验失败，请重新下载"); }
            move(part,file(model));
        } finally { active=null;connection.disconnect(); }
    }
    public void cancel() { HttpURLConnection current=active;if(current!=null)current.disconnect(); }
    public synchronized void importFile(Model model,Path source,Consumer<String> progress) throws IOException {
        if(source.toAbsolutePath().normalize().equals(file(model).toAbsolutePath().normalize())) { verify(model);Files.deleteIfExists(disabledFile());return; }
        Files.createDirectories(directory);Path part=directory.resolve("import.part");
        try {
            if(Files.size(source)!=model.maximumBytes())throw new IOException("请选择 Paraformer 2024-03-09 的 model.int8.onnx 文件");
            progress.accept("正在复制并校验 Paraformer 模型");Files.copy(source,part,StandardCopyOption.REPLACE_EXISTING);
            if(!valid(part,model.sha256))throw new IOException("不是支持的 Paraformer 量化模型");
            if(!valid(tokensFile(model),TOKEN_HASH))release("tokens.txt",TOKEN_HASH,progress);
            move(part,file(model));Files.deleteIfExists(disabledFile());verify(model);
        } finally { Files.deleteIfExists(part); }
    }
    public static String digest(Path file,String algorithm) throws IOException {
        try {
            MessageDigest hash=MessageDigest.getInstance(algorithm);
            try(InputStream input=Files.newInputStream(file)) { byte[] buffer=new byte[65536];int n;while((n=input.read(buffer))!=-1) { if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("已取消");hash.update(buffer,0,n); } }
            StringBuilder result=new StringBuilder();for(byte b:hash.digest())result.append(String.format(Locale.ROOT,"%02x",b&255));return result.toString();
        } catch(NoSuchAlgorithmException impossible) { throw new IOException(impossible); }
    }
}
