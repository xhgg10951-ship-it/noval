# RH-10 qwen3.7-plus Product-wired Results

Run ID: `rh10_qwen3.7-plus_a00279d_20260824_product`

Product/prompt commit: `a00279d`

Provider: `qwen3.7-plus`, `mock_llm=false`

Execution boundary: public Spring Boot API → Java workflow/context assembly → Python AI service → MySQL persistence.

| Acceptance | Result | Metrics |
|---|---|---|
| AC-101 | PASS | `{"restartHits":[],"logicalOrders":[3,4,5],"plannerText":"描写林夜在艾琳住所的清晨准备过程，强化‘失忆者’人设的细节打磨，并前往冒险者公会。 林夜整理仪容，与艾琳进行简短互动以巩固信任，两人抵达灰石城冒险者公会门口。\n完成冒险者注册流程，并通过基础能力测试，正式获得见习冒险者资格。 林夜填写表格（利用失忆设定规避细节），进行魔力/体能基础检测，成功拿到徽章。\n接取‘幽影森林失踪案’委托，了解任务详情，并采购必要物资，准备出发。 详细阅读委托书，分析风险，购买简易装备，结束于出发前的最后准备。"}` |
| AC-103 | PASS | `{"targetCharacters":3000,"actualCharacters":[3211,2637,3140,3426,3298],"inBand":[true,true,true,true,true],"passRate":"5/5","duplicateLongSentences":[]}` |
| AC-104 | PASS | `{"chapterId":1578,"registrationHits":["登记","注册","手续"],"identityViolations":[]}` |
| AC-105 | PASS | `{"breadCandidates":[{"id":1117,"storyId":1729,"sourceChapterId":1577,"type":"WORLD_RULE","subject":"灰石城","field":"purchasing_power","value":"劣质黑面包汤=2铜便士；破旅馆一晚=5铜便士；冒险者入门测试费=10银先令","suggestedAction":"REVIEW","evidence":"一顿最劣质的黑面包汤需要两个铜便士。住这种破旅馆的一晚，需要五个铜便士。如果你想注册成为冒险者，入门测试费是十个银先令。","importance":3,"scope":"STORY","processingStatus":"PENDING","applied":false,"createdAt":"2026-08-24T00:42:40"},{"id":1121,"storyId":1729,"sourceChapterId":1577,"type":"TRANSIENT_DETAIL","subject":"林夜","field":"consumed_item","value":"黑面包（干硬，口感粗糙）","suggestedAction":"IGNORE","evidence":"拿起那块黑面包。面包硬得像石头... 粗糙的口感摩擦着口腔黏膜","importance":1,"scope":"CHAPTER","processingStatus":"IGNORED","applied":false,"createdAt":"2026-08-24T00:42:40"},{"id":1132,"storyId":1729,"sourceChapterId":1577,"type":"TRANSIENT_DETAIL","subject":"普通面包","field":null,"value":"早餐时食用，仅用于填饱肚子","suggestedAction":"IGNORE","evidence":"早餐时，林夜吃掉一块普通面包，只是填饱肚子，随后不再关注它。","importance":1,"scope":"CHAPTER","processingStatus":"IGNORED","applied":false,"createdAt":"2026-08-24T00:43:06"}],"exactClassificationCount":2,"laterBreadMentions":[0,0,0]}` |
| AC-106 | PASS | `{"replanLogicalOrders":[4,5,6],"repeatedOpeningHits":[],"finalChapterNumbers":[1,2,3,4,5,6],"firstThreeUnchanged":true,"duplicateTitles":[],"stoppedJobStatus":"STOPPED","finalJobStatus":"COMPLETED","replanSemanticText":"深入矿区核心区域，发现人为改造的地下设施痕迹，确认失踪者被囚禁而非杀害，并遭遇第一波非自然生物的伏击。 两人进入旧矿区废弃的主井口，林夜利用现代物理知识识破陷阱，发现地下存在人工照明的迹象。\n绕过正门封锁，通过通风管道或废弃排水渠潜入内部设施，目睹‘实验’或‘仪式’的一角，证实阴谋的残酷性。 找到侧翼入口，潜入地下设施外围，林夜透过缝隙观察到内部情况，确认失踪者还活着但状态异常。\n带着关键情报撤离矿区，途中遭遇追踪者，利用地形和环境陷阱摆脱追兵，回到城镇边缘，决定下一步行动策略。 成功逃离地下设施，但在返回地面途中被守卫察觉，经历一场紧张的追逐战，最终甩掉尾巴。"}` |
| AC-109 | PASS | `{"chapterId":1580,"missingFacts":{"characters":[],"location":[],"items":[],"case":[],"ending":[]},"mechanicalStartsBefore":6,"mechanicalStartsAfter":1,"sourceType":"AI_POLISH","memoryExtractionStatus":"COMPLETED"}` |
| AC-114 | PASS | `{"targetChapterCount":600,"currentChapter":5,"arc":"1-60 生存融入","logicalOrders":[6,7,8,9,10],"endgameHits":[],"localProgressHits":["森林","失踪","调查","生存"],"plannerText":"午夜旧钟楼会面，获取关键目击线索并建立林夜与艾琳的初步战术默契 3000字左右\n进入幽影森林外围，遭遇低阶魔物试探，展现林夜的生存本能与艾琳的战斗风格 3200字左右\n调查汤姆失踪地点，发现诡异陷阱，林夜利用现代逻辑破解简易魔法机关 3000字左右\n黑雾弥漫中遭遇心理压迫，林夜克服恐惧，发现第二个失踪者玛丽亚的踪迹 3300字左右\n山洞内的对峙与逃脱，确认失踪者已遇害或转化，确立阶段性调查结果 3500字左右"}` |

Final: **7/7 PASSED**
