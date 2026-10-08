// SPDX-License-Identifier: Apache-2.0
// ABI declarations: Copyright (c) 2023 Xiaomi Corporation.
// Modified: Java/JNA ASR-only mapping by Easy Chat contributors, 2026-10-08.
package io.github.easychat.worker;

import com.sun.jna.*;
import java.util.*;

/** C ABI mapping derived from sherpa-onnx v1.13.8 c-api.h (Apache-2.0).
 * Only ASR functions are bound. Field order includes all ABI fields, even unused models.
 */
public final class SherpaAsr {
    public interface Api extends Library {
        String SherpaOnnxGetVersionStr();
        String SherpaOnnxGetOnnxruntimeVersionStr();
        Pointer SherpaOnnxCreateOfflineRecognizer(SherpaOnnxOfflineRecognizerConfig config);
        void SherpaOnnxDestroyOfflineRecognizer(Pointer recognizer);
        Pointer SherpaOnnxCreateOfflineStream(Pointer recognizer);
        void SherpaOnnxDestroyOfflineStream(Pointer stream);
        void SherpaOnnxAcceptWaveformOffline(Pointer stream, int sampleRate, float[] samples, int length);
        void SherpaOnnxDecodeOfflineStream(Pointer recognizer, Pointer stream);
        Pointer SherpaOnnxGetOfflineStreamResult(Pointer stream);
        void SherpaOnnxDestroyOfflineRecognizerResult(Pointer result);
    }
    public static class Utf8Structure extends Structure {
        public Utf8Structure() { setStringEncoding("UTF-8"); }
    }
    @Structure.FieldOrder({"sample_rate", "feature_dim"})
    public static class SherpaOnnxFeatureConfig extends Utf8Structure {
        public int sample_rate;
        public int feature_dim;
    }
    @Structure.FieldOrder({"encoder", "decoder", "joiner"})
    public static class SherpaOnnxOfflineTransducerModelConfig extends Utf8Structure {
        public String encoder;
        public String decoder;
        public String joiner;
    }
    @Structure.FieldOrder({"model"})
    public static class SherpaOnnxOfflineParaformerModelConfig extends Utf8Structure {
        public String model;
    }
    @Structure.FieldOrder({"model"})
    public static class SherpaOnnxOfflineNemoEncDecCtcModelConfig extends Utf8Structure {
        public String model;
    }
    @Structure.FieldOrder({"encoder", "decoder", "language", "task", "tail_paddings", "enable_token_timestamps", "enable_segment_timestamps"})
    public static class SherpaOnnxOfflineWhisperModelConfig extends Utf8Structure {
        public String encoder;
        public String decoder;
        public String language;
        public String task;
        public int tail_paddings;
        public int enable_token_timestamps;
        public int enable_segment_timestamps;
    }
    @Structure.FieldOrder({"model"})
    public static class SherpaOnnxOfflineTdnnModelConfig extends Utf8Structure {
        public String model;
    }
    @Structure.FieldOrder({"model", "language", "use_itn"})
    public static class SherpaOnnxOfflineSenseVoiceModelConfig extends Utf8Structure {
        public String model;
        public String language;
        public int use_itn;
    }
    @Structure.FieldOrder({"preprocessor", "encoder", "uncached_decoder", "cached_decoder", "merged_decoder"})
    public static class SherpaOnnxOfflineMoonshineModelConfig extends Utf8Structure {
        public String preprocessor;
        public String encoder;
        public String uncached_decoder;
        public String cached_decoder;
        public String merged_decoder;
    }
    @Structure.FieldOrder({"encoder", "decoder"})
    public static class SherpaOnnxOfflineFireRedAsrModelConfig extends Utf8Structure {
        public String encoder;
        public String decoder;
    }
    @Structure.FieldOrder({"model"})
    public static class SherpaOnnxOfflineDolphinModelConfig extends Utf8Structure {
        public String model;
    }
    @Structure.FieldOrder({"model"})
    public static class SherpaOnnxOfflineZipformerCtcModelConfig extends Utf8Structure {
        public String model;
    }
    @Structure.FieldOrder({"encoder", "decoder", "src_lang", "tgt_lang", "use_pnc"})
    public static class SherpaOnnxOfflineCanaryModelConfig extends Utf8Structure {
        public String encoder;
        public String decoder;
        public String src_lang;
        public String tgt_lang;
        public int use_pnc;
    }
    @Structure.FieldOrder({"model"})
    public static class SherpaOnnxOfflineWenetCtcModelConfig extends Utf8Structure {
        public String model;
    }
    @Structure.FieldOrder({"model"})
    public static class SherpaOnnxOfflineOmnilingualAsrCtcModelConfig extends Utf8Structure {
        public String model;
    }
    @Structure.FieldOrder({"model"})
    public static class SherpaOnnxOfflineMedAsrCtcModelConfig extends Utf8Structure {
        public String model;
    }
    @Structure.FieldOrder({"encoder_adaptor", "llm", "embedding", "tokenizer", "system_prompt", "user_prompt", "max_new_tokens", "temperature", "top_p", "seed", "language", "itn", "hotwords"})
    public static class SherpaOnnxOfflineFunASRNanoModelConfig extends Utf8Structure {
        public String encoder_adaptor;
        public String llm;
        public String embedding;
        public String tokenizer;
        public String system_prompt;
        public String user_prompt;
        public int max_new_tokens;
        public float temperature;
        public float top_p;
        public int seed;
        public String language;
        public int itn;
        public String hotwords;
    }
    @Structure.FieldOrder({"model"})
    public static class SherpaOnnxOfflineFireRedAsrCtcModelConfig extends Utf8Structure {
        public String model;
    }
    @Structure.FieldOrder({"conv_frontend", "encoder", "decoder", "tokenizer", "max_total_len", "max_new_tokens", "temperature", "top_p", "seed", "hotwords"})
    public static class SherpaOnnxOfflineQwen3ASRModelConfig extends Utf8Structure {
        public String conv_frontend;
        public String encoder;
        public String decoder;
        public String tokenizer;
        public int max_total_len;
        public int max_new_tokens;
        public float temperature;
        public float top_p;
        public int seed;
        public String hotwords;
    }
    @Structure.FieldOrder({"encoder", "decoder", "language", "use_punct", "use_itn"})
    public static class SherpaOnnxOfflineCohereTranscribeModelConfig extends Utf8Structure {
        public String encoder;
        public String decoder;
        public String language;
        public int use_punct;
        public int use_itn;
    }
    @Structure.FieldOrder({"transducer", "paraformer", "nemo_ctc", "whisper", "tdnn", "tokens", "num_threads", "debug", "provider", "model_type", "modeling_unit", "bpe_vocab", "telespeech_ctc", "sense_voice", "moonshine", "fire_red_asr", "dolphin", "zipformer_ctc", "canary", "wenet_ctc", "omnilingual", "medasr", "funasr_nano", "fire_red_asr_ctc", "qwen3_asr", "cohere_transcribe"})
    public static class SherpaOnnxOfflineModelConfig extends Utf8Structure {
        public SherpaOnnxOfflineTransducerModelConfig transducer = new SherpaOnnxOfflineTransducerModelConfig();
        public SherpaOnnxOfflineParaformerModelConfig paraformer = new SherpaOnnxOfflineParaformerModelConfig();
        public SherpaOnnxOfflineNemoEncDecCtcModelConfig nemo_ctc = new SherpaOnnxOfflineNemoEncDecCtcModelConfig();
        public SherpaOnnxOfflineWhisperModelConfig whisper = new SherpaOnnxOfflineWhisperModelConfig();
        public SherpaOnnxOfflineTdnnModelConfig tdnn = new SherpaOnnxOfflineTdnnModelConfig();
        public String tokens;
        public int num_threads;
        public int debug;
        public String provider;
        public String model_type;
        public String modeling_unit;
        public String bpe_vocab;
        public String telespeech_ctc;
        public SherpaOnnxOfflineSenseVoiceModelConfig sense_voice = new SherpaOnnxOfflineSenseVoiceModelConfig();
        public SherpaOnnxOfflineMoonshineModelConfig moonshine = new SherpaOnnxOfflineMoonshineModelConfig();
        public SherpaOnnxOfflineFireRedAsrModelConfig fire_red_asr = new SherpaOnnxOfflineFireRedAsrModelConfig();
        public SherpaOnnxOfflineDolphinModelConfig dolphin = new SherpaOnnxOfflineDolphinModelConfig();
        public SherpaOnnxOfflineZipformerCtcModelConfig zipformer_ctc = new SherpaOnnxOfflineZipformerCtcModelConfig();
        public SherpaOnnxOfflineCanaryModelConfig canary = new SherpaOnnxOfflineCanaryModelConfig();
        public SherpaOnnxOfflineWenetCtcModelConfig wenet_ctc = new SherpaOnnxOfflineWenetCtcModelConfig();
        public SherpaOnnxOfflineOmnilingualAsrCtcModelConfig omnilingual = new SherpaOnnxOfflineOmnilingualAsrCtcModelConfig();
        public SherpaOnnxOfflineMedAsrCtcModelConfig medasr = new SherpaOnnxOfflineMedAsrCtcModelConfig();
        public SherpaOnnxOfflineFunASRNanoModelConfig funasr_nano = new SherpaOnnxOfflineFunASRNanoModelConfig();
        public SherpaOnnxOfflineFireRedAsrCtcModelConfig fire_red_asr_ctc = new SherpaOnnxOfflineFireRedAsrCtcModelConfig();
        public SherpaOnnxOfflineQwen3ASRModelConfig qwen3_asr = new SherpaOnnxOfflineQwen3ASRModelConfig();
        public SherpaOnnxOfflineCohereTranscribeModelConfig cohere_transcribe = new SherpaOnnxOfflineCohereTranscribeModelConfig();
    }
    @Structure.FieldOrder({"model", "scale"})
    public static class SherpaOnnxOfflineLMConfig extends Utf8Structure {
        public String model;
        public float scale;
    }
    @Structure.FieldOrder({"dict_dir", "lexicon", "rule_fsts"})
    public static class SherpaOnnxHomophoneReplacerConfig extends Utf8Structure {
        public String dict_dir;
        public String lexicon;
        public String rule_fsts;
    }
    @Structure.FieldOrder({"feat_config", "model_config", "lm_config", "decoding_method", "max_active_paths", "hotwords_file", "hotwords_score", "rule_fsts", "rule_fars", "blank_penalty", "hr"})
    public static class SherpaOnnxOfflineRecognizerConfig extends Utf8Structure {
        public SherpaOnnxFeatureConfig feat_config = new SherpaOnnxFeatureConfig();
        public SherpaOnnxOfflineModelConfig model_config = new SherpaOnnxOfflineModelConfig();
        public SherpaOnnxOfflineLMConfig lm_config = new SherpaOnnxOfflineLMConfig();
        public String decoding_method;
        public int max_active_paths;
        public String hotwords_file;
        public float hotwords_score;
        public String rule_fsts;
        public String rule_fars;
        public float blank_penalty;
        public SherpaOnnxHomophoneReplacerConfig hr = new SherpaOnnxHomophoneReplacerConfig();
    }
    private SherpaAsr() { }
}
