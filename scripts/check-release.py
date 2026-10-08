import pathlib,zipfile,json,hashlib,xml.etree.ElementTree as ET
import argparse
parser=argparse.ArgumentParser(description='Verify bundled JAR and public release readiness')
parser.add_argument('--allow-license-blockers',action='store_true',help='Internal verification only; does not clear public distribution')
args=parser.parse_args()
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
    assert '20261008-release-audit' in archive.read('META-INF/MANIFEST.MF').decode()
    assert 'sherpa-onnx 1.13.8' in archive.read('THIRD_PARTY_NOTICES.md').decode()
    for name in ['licenses/sherpa-onnx-LICENSE.txt','licenses/FunASR-LICENSE.txt','licenses/Paraformer-MODEL-CARD.md','licenses/Paraformer-LICENSE.txt','licenses/espeak-ng-COPYING.txt']:
        assert name in names
reports=[ET.parse(p).getroot() for p in (root/'build/test-results/test').glob('TEST-*.xml')]
assert reports, 'missing test reports'
assert all(int(r.get('failures','0'))==0 and int(r.get('errors','0'))==0 for r in reports)
with jar.open('rb') as stream:digest=hashlib.file_digest(stream,'sha256').hexdigest()
result=dict(public_release_cleared=False,license_blockers=['Bundled sherpa JNI contains GPLv3 eSpeak-NG; corresponding-source obligations unresolved'],bytes=jar.stat().st_size,MiB=round(jar.stat().st_size/1048576,1),sha256=digest,unit_tests=sum(int(r.get('tests','0')) for r in reports),failures=0,engines='Paraformer + OPUS zh-en/en-zh')
(root/'outputs/paraformer-release-check.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(result,ensure_ascii=False))

if not args.allow_license_blockers:
    raise SystemExit('PUBLIC RELEASE BLOCKED: see docs/LICENSE_REVIEW.md; GPLv3 native dependency requires resolution.')
