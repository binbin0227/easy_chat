# Third-party components

Easy Chat original code is MIT licensed (root LICENSE). The derived C API ABI
mapping in src/asrWorker/java/io/github/easychat/worker/SherpaAsr.java retains
Apache-2.0, Xiaomi attribution and a modification notice. Third-party licenses
are retained under licenses/ in both the mod and isolated worker JAR.

## sherpa-onnx 1.13.8 - Apache-2.0

Source: https://github.com/k2-fsa/sherpa-onnx/tree/v1.13.8
Unmodified official Windows x64 C API runtime:
sherpa-onnx-v1.13.8-win-x64-shared-MT-Release-no-tts-lib.tar.bz2
SHA256: a1253e665c4f236119c443c8932a8acfca32a546c78c05962d483c9a0eae21b7
The official build disables TTS and PortAudio. The previous TTS-enabled Java JNI
library, eSpeak-NG and Piper are excluded. License: licenses/sherpa-onnx-LICENSE.txt.
Native dependencies, copyrights and source availability: licenses/asr-runtime-NOTICES.txt.

## JNA 5.17.0 - Apache-2.0 chosen

https://github.com/java-native-access/jna/tree/5.17.0
Upstream offers Apache-2.0 OR LGPL-2.1-or-later; we choose Apache-2.0.
Unmodified Java classes and Windows x64 jnidispatch.dll only.
Licenses: jna-LICENSE, jna-AL2.0.txt and jna-libffi-LICENSE.txt (libffi MIT).

## Paraformer model - Apache-2.0

Unmodified int8 conversion sherpa-onnx-paraformer-zh-2024-03-09 by Fangjun Kuang/k2-fsa,
from Alibaba DAMO Academy / FunASR model
https://modelscope.cn/models/iic/speech_paraformer-large_asr_nat-zh-cn-16k-common-vocab8358-tensorflow1
Conversion: https://huggingface.co/csukuangfj/sherpa-onnx-paraformer-zh-2024-03-09
Model license/card: licenses/Paraformer-LICENSE.txt, licenses/Paraformer-MODEL-CARD.md.
FunASR-LICENSE.txt covers FunASR code (MIT), not these weights.

## OPUS-MT / Marian models - direction-specific licenses

Unmodified Helsinki-NLP models, quantized ONNX conversions distributed by Xenova:
https://huggingface.co/Xenova/opus-mt-en-zh
https://huggingface.co/Xenova/opus-mt-zh-en
Both directions include checksum manifests. Original/conversion model notices:
licenses/opus-mt-LICENSE.txt and licenses/opus-mt-MODEL-NOTICE.txt.
English to Chinese: Apache-2.0, with the retained original model card.
Chinese to English: CC-BY-4.0, developed by the University of Helsinki Language
Technology Research Group. Full license and model card are retained as
licenses/opus-mt-zh-en-CC-BY-4.0.txt and licenses/opus-mt-zh-en-MODEL-CARD.md.
Xenova converted and quantized the original models to ONNX; Easy Chat redistributes
those converted files without further modification. These licenses are separate
from the application's MIT license; no upstream endorsement is implied.
Easy Chat implements tokenizer handling and beam-search inference.

## ONNX Runtime - MIT and accompanying third-party terms

Copyright Microsoft Corporation. https://github.com/microsoft/onnxruntime
OPUS uses 1.17.3 (onnxruntime-LICENSE.txt and onnxruntime-ThirdPartyNotices.txt).
The isolated ASR worker uses 1.28.2 from the pinned sherpa release
(onnxruntime-asr-1.28.2-LICENSE.txt and onnxruntime-asr-1.28.2-ThirdPartyNotices.txt).
Versions run in separate processes. Eigen MPL-2.0 source availability is recorded
in licenses/asr-runtime-NOTICES.txt.

Whisper, Qwen/llama and their weights are excluded. Minecraft, Forge, LWJGL and
Gson are supplied by the game/loader, not copied into the mod. Gradle Wrapper
Apache-2.0 LICENSE/NOTICE are retained in the repository licenses/ directory.
