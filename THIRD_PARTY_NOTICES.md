# Third-party components

Easy Chat is MIT licensed. Third-party licenses are retained under `licenses/` in the JAR.

## sherpa-onnx 1.13.8 - Apache-2.0

Copyright k2-fsa contributors. Source: https://github.com/k2-fsa/sherpa-onnx/tree/v1.13.8

The Java API and official Windows x64 JNI runtime run inside a separate Java worker.
The upstream Java API/native DLLs are unmodified. Easy Chat supplies its own worker protocol.
License: `licenses/sherpa-onnx-LICENSE.txt`.

Official release checksums:
- sherpa-onnx-jvm-1.13.8.jar: `77b7b047fade4eadada96b568eb92615049aaf1dc317c7244e46c1ea38b9a63b`
- sherpa-onnx-native-lib-win-x64-1.13.8.jar: `33fbdbd5410e9ba9bdda94aa164ec8f7825bb49246420d8ce9bdd88219d97039`

## Paraformer model - Apache-2.0

The default Chinese/English speech recognizer is the int8 ONNX conversion
`sherpa-onnx-paraformer-zh-2024-03-09`, exported by Fangjun Kuang/k2-fsa from
Alibaba DAMO Academy / FunASR's
`iic/speech_paraformer-large_asr_nat-zh-cn-16k-common-vocab8358-tensorflow1`.

Original model: https://modelscope.cn/models/iic/speech_paraformer-large_asr_nat-zh-cn-16k-common-vocab8358-tensorflow1
Conversion: https://huggingface.co/csukuangfj/sherpa-onnx-paraformer-zh-2024-03-09
License text for the model: `licenses/Paraformer-LICENSE.txt` (Apache-2.0).
`licenses/FunASR-LICENSE.txt` covers FunASR project code (MIT), not these model weights.
Conversion weights are unmodified.

## OPUS-MT / Marian translation models - Apache-2.0

Helsinki-NLP models, quantized ONNX conversions distributed by Xenova:
- English to Chinese: https://huggingface.co/Xenova/opus-mt-en-zh
- Chinese to English: https://huggingface.co/Xenova/opus-mt-zh-en

Both directions are bundled with checksum manifests. Copyright and model notices:
`licenses/opus-mt-LICENSE.txt`, `licenses/opus-mt-MODEL-NOTICE.txt`.
No modifications are made to model weights. Easy Chat implements local tokenizer
handling and beam-search inference around the exported models.

## ONNX Runtime - MIT

Copyright Microsoft Corporation. https://github.com/microsoft/onnxruntime
The mod uses the 1.17.3 Java runtime for OPUS. The isolated ASR worker uses the
ONNX Runtime DLL included in the official sherpa release; it is not loaded into
Minecraft's process. Retained notices: `licenses/onnxruntime-LICENSE.txt` and
`licenses/onnxruntime-ThirdPartyNotices.txt`.

Whisper and Qwen/llama engines and weights are not part of this distribution.
Historical notices may remain in source archives for previously built versions.
Gson, Minecraft, Forge and LWJGL are supplied by the game/loader and are not copied
into this mod. Minecraft itself is not distributed by this project.

## Release audit finding: GPLv3 TTS code in the ASR native runtime

The official Windows JNI library above also contains statically linked eSpeak-NG
and piper-phonemize text-to-speech code, although Easy Chat uses ASR only.
Its distribution must not be described as wholly MIT/Apache licensed.
Pinned eSpeak-NG source: https://github.com/csukuangfj/espeak-ng/tree/ed530aa113046142eb5115cf2fc9157854d0ffe1
GPL text: `licenses/espeak-ng-COPYING.txt`.
The official build enables TTS by default; disabling the executable alone does
not remove its library code. Details and unresolved redistribution requirements
are documented in `docs/LICENSE_REVIEW.md`. This build is NOT cleared for public
binary release. Adding license text alone does not discharge source obligations.
