import pathlib,zipfile,json,hashlib,io,xml.etree.ElementTree as ET
root=pathlib.Path(__file__).resolve().parent.parent
jar=root/'outputs/easy-chat-forge-1.8.9-0.1.0.jar'
with zipfile.ZipFile(jar) as archive:
    names=archive.namelist()
    assert len(names)==len(set(names)),'duplicate JAR entries'
    assert not any(n.endswith('.gguf') or n.endswith('whisper-bin-x64.zip') or n.endswith('llama-cpu-x64.zip') or n.endswith('llama-vulkan-x64.zip') for n in names)
    assert not any('/WhisperEngine' in n or '/GgufTranslator' in n or '/QwenTranslator' in n for n in names)
    models=json.loads(archive.read('easychat/text-model-catalog.json'));assert {m['id'] for m in models}=={'opus-en-zh','opus-zh-en'}
    for folder,manifest in [('easychat/text-model/','easychat/text-model.properties'),('easychat/text-model-zh-en/','easychat/text-model-zh-en.properties')]:
        for line in archive.read(manifest).decode().splitlines():
            if '=' not in line or line.startswith('#'):continue
            name,digest=line.split('=',1)
            with archive.open(folder+name) as stream:assert hashlib.file_digest(stream,'sha256').hexdigest()==digest
    with archive.open('assets/easychat/models/paraformer-zh-int8/model.int8.onnx') as stream:
        assert hashlib.file_digest(stream,'sha256').hexdigest()=='90bc03034ae1bef9575f8cc798cd1519c8be8aa9e8b458a033e32017ff4d584c'
    assert '20261008-chat-clean' in archive.read('META-INF/MANIFEST.MF').decode()
    assert 'sherpa-onnx 1.13.8' in archive.read('THIRD_PARTY_NOTICES.md').decode()
    for name in ['licenses/sherpa-onnx-LICENSE.txt','licenses/FunASR-LICENSE.txt','licenses/Paraformer-MODEL-CARD.md','licenses/Paraformer-LICENSE.txt']:
        assert name in names
    assert not any('sherpa-onnx-jni' in n or 'espeak-ng-COPYING' in n for n in names)
    audit=json.loads((root/'docs/native-license-audit.json').read_text(encoding='utf-8'))
    artifact=root/'work/libs'/audit['component']
    with artifact.open('rb') as stream:
        assert hashlib.file_digest(stream,'sha256').hexdigest()=='a1253e665c4f236119c443c8932a8acfca32a546c78c05962d483c9a0eae21b7'
    license_files=[p.name for p in (root/'src/main/resources/licenses').iterdir() if p.is_file()]
    for name in license_files:
        assert archive.read('licenses/'+name)==(root/'src/main/resources/licenses'/name).read_bytes(),name
    with zipfile.ZipFile(io.BytesIO(archive.read('easychat/paraformer-worker.jar'))) as worker:
        entries=worker.namelist();assert len(entries)==len(set(entries))
        assert not any('com/k2fsa/' in n or 'sherpa-onnx-jni' in n for n in entries)
        dlls={n for n in entries if n.endswith('.dll')}
        expected={'easychat/native/win-x64/'+name:digest for name,digest in audit['native_files'].items()}
        expected['com/sun/jna/win32-x86-64/jnidispatch.dll']=audit['jnidispatch_sha256']
        assert dlls==set(expected),(dlls,set(expected))
        for name,digest in expected.items():
            data=worker.read(name);assert hashlib.sha256(data).hexdigest()==digest,name
            for forbidden in audit['forbidden_symbols']:
                assert forbidden.encode() not in data,(name,forbidden)
        assert 'io/github/easychat/worker/SherpaAsr.class' in entries
        for name in license_files:assert worker.read('licenses/'+name)==archive.read('licenses/'+name)
        assert worker.read('THIRD_PARTY_NOTICES.md')==archive.read('THIRD_PARTY_NOTICES.md')
reports=[ET.parse(p).getroot() for p in (root/'build/test-results/test').glob('TEST-*.xml')]
assert reports, 'missing test reports'
assert all(int(r.get('failures','0'))==0 and int(r.get('errors','0'))==0 for r in reports)
with jar.open('rb') as stream:digest=hashlib.file_digest(stream,'sha256').hexdigest()
result=dict(public_release_cleared=True,license_blockers=[],license_scope='Pinned component audit; gameplay acceptance and future dependency audits still required',bytes=jar.stat().st_size,MiB=round(jar.stat().st_size/1048576,1),sha256=digest,unit_tests=sum(int(r.get('tests','0')) for r in reports),failures=0,engines='Paraformer + OPUS zh-en/en-zh')
(root/'outputs/paraformer-release-check.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(result,ensure_ascii=False))
