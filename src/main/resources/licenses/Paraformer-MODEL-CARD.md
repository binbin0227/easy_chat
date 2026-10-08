# Paraformer model provenance

Original provider: Alibaba DAMO Academy / iic.
Original model: iic/speech_paraformer-large_asr_nat-zh-cn-16k-common-vocab8358-tensorflow1.
Model card revision reviewed: v1.1.9; declared model license: Apache License 2.0.
Source: https://modelscope.cn/models/iic/speech_paraformer-large_asr_nat-zh-cn-16k-common-vocab8358-tensorflow1

Unmodified int8 ONNX conversion by Fangjun Kuang / k2-fsa:
https://huggingface.co/csukuangfj/sherpa-onnx-paraformer-zh-2024-03-09

model.int8.onnx SHA-256: 90bc03034ae1bef9575f8cc798cd1519c8be8aa9e8b458a033e32017ff4d584c
tokens.txt SHA-256: 6c0e3b35cece259829e6cb5b8d90d13db88f61ea3a2953d11898e4b2bfd7a2e2

Easy Chat packages these files without further modification and adds an independent
Java recognition worker. Retain Paraformer-LICENSE.txt with redistributed weights.
The model license is separate from FunASR's MIT project-code license.
