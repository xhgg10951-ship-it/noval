# RH-10 qwen3.7-plus Product-wired Results

Run ID: `rh10_qwen3.7-plus_19322e0_20260823_product`

Product/prompt commit: `19322e0`

Provider: `qwen3.7-plus`, `mock_llm=false`

Execution boundary: public Spring Boot API → Java workflow/context assembly → Python AI service → MySQL persistence.

| Acceptance | Result | Metrics |
|---|---|---|
| AC-101 | PASS | `{"restartHits":[],"logicalOrders":[3,4,5],"plannerText":"林夜在艾琳的陪同下前往冒险者公会，完成入会测试并注册身份，初步展示其现代思维对异世界规则的适应与伪装。 林夜成功获得冒险者徽章（最低等级），确立合法身份，但身体虚弱状态引起公会人员注意，需通过话术化解。\n林夜与艾琳共同接取'幽影森林失踪案'委托，并在出发前进行必要的物资准备与情报分析，展现林夜的逻辑推理能力。 两人正式组队（临时），明确任务目标与报酬，林夜通过提问获取关键线索，艾琳对林夜的'直觉'产生轻微好奇。\n进入幽影森林外围，遭遇初次环境危机与小规模魔物骚扰，林夜在实战中艰难求生，验证其生存策略。 林夜体验真实的魔法世界战斗残酷性，依靠艾琳救援脱险，同时发现第一个关键线索（如异常的植物或痕迹）。"}` |
| AC-103 | PASS | `{"targetCharacters":3000,"actualCharacters":[2749,3460,3559,3794,2937],"inBand":[true,true,true,false,true],"passRate":"4/5","duplicateLongSentences":[]}` |
| AC-104 | PASS | `{"chapterId":1443,"registrationHits":["入会"],"identityViolations":[]}` |
| AC-105 | FAIL | `{"breadCandidates":[{"id":761,"storyId":1595,"sourceChapterId":1442,"type":"TRANSIENT_DETAIL","subject":"林夜","field":"consumed_item","value":"黑面包和干酪，用于恢复体力，随后不再关注","suggestedAction":"IGNORE","evidence":"她从柜子里拿出一块黑面包和一小块干酪，扔在林夜面前的盘子里... 林夜拿起那块硬得像石头一样的黑面包，用力掰下一小块放入口中。","importance":1,"scope":"CHAPTER","processingStatus":"IGNORED","applied":false,"createdAt":"2026-08-23T23:42:08"},{"id":771,"storyId":1595,"sourceChapterId":1442,"type":"TRANSIENT_DETAIL","subject":"林夜","field":null,"value":"食用了一块普通面包填饱肚子","suggestedAction":"IGNORE","evidence":"早餐时，林夜吃掉一块普通面包，只是填饱肚子，随后不再关注它。","importance":1,"scope":"CHAPTER","processingStatus":"IGNORED","applied":false,"createdAt":"2026-08-23T23:42:31"}],"exactClassificationCount":2,"laterBreadMentions":[1,3,1]}` |
| AC-106 | PASS | `{"replanLogicalOrders":[4,5,6],"repeatedOpeningHits":[],"finalChapterNumbers":[1,2,3,4,5,6],"firstThreeUnchanged":true,"duplicateTitles":[],"stoppedJobStatus":"STOPPED","finalJobStatus":"COMPLETED","replanSemanticText":"分析黑雾性质，确定寻找‘钥匙’的方向，并引出掌握古代符文的线索人物。 林夜利用现代化学/物理知识解释黑雾的某种特性（非魔法层面），艾琳通过冒险者公会情报网锁定一名被排挤的落魄学者或老法师。\n从莫尔斯处获取关于黑雾和钥匙的情报，同时揭示失踪案背后的组织雏形。 莫尔斯认出黑雾是‘影蚀教派’的标记，给出半块残缺的符文石板作为‘钥匙’的一部分，并警告危险升级。\n逃脱追捕，整合现有线索，制定下一步针对贵族少年的调查计划，并将冲突从暗处推向明处。 成功甩掉追兵，林夜与艾琳的关系因共患难而加深，但艾琳对林夜的‘运气’和‘知识’产生更深的疑虑。确定下一站：贵族区。"}` |
| AC-109 | PASS | `{"chapterId":1445,"missingFacts":{"characters":[],"location":[],"items":[],"case":[],"ending":[]},"mechanicalStartsBefore":6,"mechanicalStartsAfter":2,"sourceType":"AI_POLISH","memoryExtractionStatus":"COMPLETED"}` |
| AC-114 | PASS | `{"targetChapterCount":600,"currentChapter":5,"arc":"1-60 生存融入","logicalOrders":[6,7,8,9,10],"endgameHits":[],"localProgressHits":["公会","委托","失踪","生存"],"plannerText":"描写前往旧钟楼的夜间潜行过程，营造灰石城夜晚的压抑氛围，并展示林夜在虚弱状态下对环境的敏锐观察。 两人抵达旧钟楼区域，发现环境异常安静，暗示有人提前清场或埋伏。\n在旧钟楼内与见证人接触，获取关于‘黑色粘液’和‘红土’的关键情报，同时暴露潜在的内部威胁。 见证人提供线索后迅速死亡或失踪，留下新的谜题，证实案件涉及公会内部或高层势力。\n紧急撤离旧钟楼，林夜利用地形优势帮助艾琳摆脱可能的追兵，初步建立信任纽带。 成功甩掉尾巴，回到安全屋或临时据点。林夜的‘观察力’特长在实战中首次得到艾琳的真正认可。\n休整与分析阶段。林夜结合之前发现的‘红土’线索与地图碎片，完善推理，并解决生存压力（会费/医药费）。 确定下一步行动目标为南部废弃矿坑。林夜意识到必须提升实力或赚取更多佣金才能购买抗黑雾药剂。\n执行低级委托‘清理变异鼠患’，作为林夜的第一次实战试炼，重点描写他的非战斗贡献和团队协作。 任务完成，获得少量报酬。林夜证明了自己即使在战斗中也能通过策略发挥作用，巩固了在队伍中的位置。"}` |

Final: **FAILED**
