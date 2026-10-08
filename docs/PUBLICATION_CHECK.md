# Public source and distribution check — 2026-10-08

Source repository: https://github.com/binbin0227/easy_chat (public).
Version: 0.1.0. This record does not announce a binary release.

## Findings corrected before upload

- OPUS-MT Chinese-to-English was incorrectly labeled Apache-2.0. The current
  Helsinki-NLP model card declares CC-BY-4.0. The model catalog, notices and
  review now reflect that license; the complete license and original model card
  accompany the distribution, with University of Helsinki attribution and
  Xenova conversion/quantization disclosure. English-to-Chinese remains Apache-2.0.
- Both ONNX Runtime versions now identify their own exact Eigen source revision
  for MPL-2.0 source availability, alongside their third-party notices.
- The pixel-art project icon is identified as AI-generated, not a gameplay image.

## Verification

- Existing test suite: 39 tests, zero failures or errors after the model license
  catalog correction. Subsequent changes only add license/source notices.
- Release inspection validates pinned model and DLL hashes, direction-specific
  licenses, full notices in the main JAR and worker, and exclusion of retired
  Whisper/Qwen/TTS-enabled JNI components. No known license blockers found.
- All 120 pre-publication Git-history blobs checked for common credential
  patterns, runtime/build files and large blobs: no findings. Current upload
  additionally checked before commit. Work files, old models, logs and output
  JARs are excluded from source control. Historical GPL license text is retained
  only in old Git commits; no GPL binary or corresponding old runtime is uploaded.
- Final local distribution: `easy-chat-forge-1.8.9-0.1.0.jar`, 379098217 bytes.
  SHA-256: `d436b89a1a9f380d7a46fa1dbc4d6ef0b95dd42b2e028b8ee1602529831f648d`.
- No game/client restart was needed for these notice changes. The earlier
  gameplay acceptance remains recorded separately in VERIFICATION.md.

Original code is MIT except the explicitly attributed Apache-2.0 C ABI mapping.
Third-party models/libraries retain their individual terms. This is a check of
the identified, pinned components, not an unconditional legal guarantee; future
dependency changes and any additional publication assets need a new review.
