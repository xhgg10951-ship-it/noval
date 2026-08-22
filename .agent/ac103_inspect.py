# -*- coding: utf-8 -*-
import json, urllib.request
chapters = json.loads(urllib.request.urlopen('http://127.0.0.1:8080/api/stages/644/chapters', timeout=30).read())
for c in sorted(chapters, key=lambda x: x['chapterNumber']):
    content = c['content']
    print("--- Ch%s [%s] len=%s ---" % (c['chapterNumber'], c['title'], len(content)))
    print("开头:", content[:120].replace("\n", " "))
    print("结尾:", content[-100:].replace("\n", " "))
    print()
