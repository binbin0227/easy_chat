package io.github.easychat.engine;

import java.nio.file.Paths;

/** Explicit network check, separate from the offline unit tests. */
public final class ModelDownloadIntegration {
    public static void main(String[] args) throws Exception {
        ModelStore store = new ModelStore(Paths.get(args[0]));
        ModelStore.Model model = ModelStore.Model.from(args.length > 1 ? args[1] : "base");
        store.download(model, System.out::println);
        store.verify(model);
        System.out.println("DOWNLOAD_VERIFIED=" + store.file(model));
    }
}
